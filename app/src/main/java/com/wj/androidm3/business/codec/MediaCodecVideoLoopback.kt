package com.wj.androidm3.business.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.SystemClock
import android.view.Surface
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * 用 MediaCodec 手写一条“视频编解码闭环”：
 *
 * CameraX ImageAnalysis 输出 YUV_420_888
 * -> 转成编码器能吃的 I420/NV12
 * -> MediaCodec H.264 Encoder 输出压缩后的 H.264 access unit
 * -> 这里用内存队列模拟“网络传输”
 * -> MediaCodec H.264 Decoder 解码
 * -> 渲染到 SurfaceView
 *
 * 这不是为了替代 WebRTC。WebRTC 仍然负责正式局域网视频聊天。
 * 这个类是教学用途，让你能看见 WebRTC 内部“采集/编码/传输/解码/渲染”的核心步骤。
 */
class MediaCodecVideoLoopback(
    private val outputSurface: Surface,
    private val onLog: (String) -> Unit
) {
    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    private var running = false
    private var width = 0
    private var height = 0
    private var colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
    private var yuvBuffer: ByteArray? = null
    private var encodedFrames = 0
    private var decodedFrames = 0
    private var droppedFrames = 0
    private var startMs = 0L
    private val transportQueue = ArrayDeque<EncodedVideoSample>()

    fun start(width: Int = VIDEO_WIDTH, height: Int = VIDEO_HEIGHT) {
        if (running) {
            return
        }
        this.width = width
        this.height = height
        colorFormat = chooseEncoderColorFormat()
        yuvBuffer = ByteArray(width * height * 3 / 2)
        encodedFrames = 0
        decodedFrames = 0
        droppedFrames = 0
        transportQueue.clear()
        startMs = SystemClock.elapsedRealtime()
        configureEncoder(width, height)
        running = true
    }

    private fun configureEncoder(width: Int, height: Int) {
        this.width = width
        this.height = height
        colorFormat = chooseEncoderColorFormat()
        yuvBuffer = ByteArray(width * height * 3 / 2)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            // ByteBuffer 输入模式必须声明 YUV 颜色格式。
            // 生产级实时通话更常用 COLOR_FormatSurface，让摄像头通过 Surface/EGL 直接进编码器。
            // 这里为了教学，故意使用 ByteBuffer，这样能看到 YUV 数据如何进入编码器。
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS)
        }
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }
        onLog("Video encoder started: ${width}x$height, color=${colorFormatName(colorFormat)}")
    }

    fun stop() {
        running = false
        releaseCodec(encoder)
        releaseCodec(decoder)
        encoder = null
        decoder = null
        yuvBuffer = null
        transportQueue.clear()
        onLog("Video stopped. encoded=$encodedFrames decoded=$decodedFrames dropped=$droppedFrames")
    }

    fun queueCameraFrame(imageProxy: ImageProxy) {
        try {
            if (!running) {
                return
            }
            val frameWidth = imageProxy.width
            val frameHeight = imageProxy.height
            if (frameWidth != width || frameHeight != height) {
                if (encodedFrames == 0 && decodedFrames == 0) {
                    reconfigureForCameraSize(frameWidth, frameHeight)
                } else {
                    droppedFrames++
                    onLog("Drop video frame: camera=${frameWidth}x$frameHeight, codec=${width}x$height")
                    return
                }
            }
            val yuv = yuvBuffer ?: return

            // CameraX 给出的 YUV_420_888 是三平面格式，而且每一行可能有 padding。
            // 编码器输入 ByteBuffer 需要紧凑连续的数据，所以这里先拷贝并重新排列。
            if (colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                Yuv420FrameConverter.imageProxyToNv12(imageProxy, yuv)
            } else {
                Yuv420FrameConverter.imageProxyToI420(imageProxy, yuv)
            }

            feedEncoder(yuv)
            drainEncoder()
            drainDecoder()
            reportStatsIfNeeded()
        } finally {
            // ImageProxy 必须及时 close，否则 CameraX 后续帧会被背压卡住。
            imageProxy.close()
        }
    }

    private fun feedEncoder(yuv: ByteArray) {
        val codec = encoder ?: return
        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
        if (inputIndex < 0) {
            droppedFrames++
            return
        }
        val inputBuffer = codec.getInputBuffer(inputIndex) ?: return
        inputBuffer.clear()
        inputBuffer.put(yuv)
        codec.queueInputBuffer(
            inputIndex,
            0,
            yuv.size,
            System.nanoTime() / 1000,
            0
        )
    }

    private fun reconfigureForCameraSize(frameWidth: Int, frameHeight: Int) {
        onLog("Camera selected ${frameWidth}x$frameHeight; reconfigure codec")
        releaseCodec(encoder)
        releaseCodec(decoder)
        encoder = null
        decoder = null
        transportQueue.clear()
        configureEncoder(frameWidth, frameHeight)
    }

    private fun drainEncoder() {
        val codec = encoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        while (running) {
            when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // H.264 解码器需要 SPS/PPS 等 codec-specific data。
                    // Android 编码器会在 outputFormat 里放 csd-0/csd-1，直接拿它配置解码器最稳。
                    startDecoder(codec.outputFormat)
                }
                else -> {
                    if (outputIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val flags = bufferInfo.flags
                            val isConfig = flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            if (!isConfig) {
                                // 这里就是“传输层”的入口。正式局域网实现可以把 sample 放进 UDP/TCP/RTP。
                                // 教学版为了专注编解码，先放进内存队列，模拟“网络传输后的接收队列”。
                                enqueueTransportSample(outputBuffer, bufferInfo)
                                drainTransportQueueToDecoder()
                                encodedFrames++
                            }
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        }
    }

    private fun startDecoder(format: MediaFormat) {
        if (decoder != null) {
            return
        }
        decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, outputSurface, null, 0)
            start()
        }
        onLog("Video decoder started with encoder output format")
    }

    private fun enqueueTransportSample(encodedData: ByteBuffer, info: MediaCodec.BufferInfo) {
        val data = ByteArray(info.size)
        encodedData.get(data)
        transportQueue.addLast(
            EncodedVideoSample(
                data = data,
                presentationTimeUs = info.presentationTimeUs,
                flags = info.flags
            )
        )
        while (transportQueue.size > MAX_TRANSPORT_QUEUE) {
            transportQueue.removeFirst()
            droppedFrames++
        }
    }

    private fun drainTransportQueueToDecoder() {
        while (transportQueue.isNotEmpty()) {
            if (!feedDecoder(transportQueue.first())) {
                return
            }
            transportQueue.removeFirst()
        }
    }

    private fun feedDecoder(sample: EncodedVideoSample): Boolean {
        val codec = decoder ?: return false
        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
        if (inputIndex < 0) {
            droppedFrames++
            return false
        }
        val inputBuffer = codec.getInputBuffer(inputIndex) ?: return false
        inputBuffer.clear()
        inputBuffer.put(sample.data)
        codec.queueInputBuffer(
            inputIndex,
            0,
            sample.data.size,
            sample.presentationTimeUs,
            sample.flags
        )
        return true
    }

    private fun drainDecoder() {
        val codec = decoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        while (running) {
            when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> onLog("Video decoder format: ${codec.outputFormat}")
                else -> {
                    if (outputIndex >= 0) {
                        // 解码器配置了 Surface 输出，releaseOutputBuffer(..., true) 才会把这帧画到 SurfaceView。
                        codec.releaseOutputBuffer(outputIndex, bufferInfo.size > 0)
                        if (bufferInfo.size > 0) {
                            decodedFrames++
                        }
                    }
                }
            }
        }
    }

    private fun reportStatsIfNeeded() {
        if (encodedFrames > 0 && encodedFrames % 30 == 0) {
            val seconds = ((SystemClock.elapsedRealtime() - startMs).coerceAtLeast(1)) / 1000f
            onLog("Video stats: enc=$encodedFrames dec=$decodedFrames drop=$droppedFrames fps=${"%.1f".format(decodedFrames / seconds)}")
        }
    }

    private fun chooseEncoderColorFormat(): Int {
        val codecInfo = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS)
            .codecInfos
            .firstOrNull { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
            } ?: return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        val capabilities = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
        val formats = capabilities.colorFormats.toSet()
        return when {
            formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
            formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) ->
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            else -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        }
    }

    private fun colorFormatName(format: Int): String {
        return when (format) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible -> "YUV420Flexible(I420 input)"
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> "YUV420SemiPlanar(NV12 input)"
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> "YUV420Planar(I420 input)"
            else -> "unknown($format)"
        }
    }

    private fun releaseCodec(codec: MediaCodec?) {
        if (codec == null) {
            return
        }
        try {
            codec.stop()
        } catch (_: Throwable) {
        }
        try {
            codec.release()
        } catch (_: Throwable) {
        }
    }

    companion object {
        const val VIDEO_WIDTH = 480
        const val VIDEO_HEIGHT = 640
        private const val VIDEO_FPS = 15
        private const val VIDEO_BIT_RATE = 900_000
        private const val I_FRAME_INTERVAL_SECONDS = 1
        private const val CODEC_TIMEOUT_US = 10_000L
        private const val MAX_TRANSPORT_QUEUE = 6
    }

    private data class EncodedVideoSample(
        val data: ByteArray,
        val presentationTimeUs: Long,
        val flags: Int
    )
}

/**
 * 把 CameraX 的 YUV_420_888 图像转换为编码器常用的紧凑 YUV420。
 *
 * YUV_420_888 是 Android 相机层的“通用描述格式”，它只保证有 Y/U/V 三个 plane，
 * 但每个 plane 的 rowStride/pixelStride 可能不同，也可能有 padding。
 * MediaCodec ByteBuffer 输入通常要求一整块连续内存，所以必须逐像素按目标格式重新排布。
 */
private object Yuv420FrameConverter {
    fun imageProxyToI420(image: ImageProxy, out: ByteArray) {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val chromaWidth = width / 2
        val chromaHeight = height / 2
        copyPlane(image.planes[0], width, height, out, 0, width)
        copyPlane(image.planes[1], chromaWidth, chromaHeight, out, ySize, chromaWidth)
        copyPlane(image.planes[2], chromaWidth, chromaHeight, out, ySize + ySize / 4, chromaWidth)
    }

    fun imageProxyToNv12(image: ImageProxy, out: ByteArray) {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val chromaWidth = width / 2
        val chromaHeight = height / 2
        copyPlane(image.planes[0], width, height, out, 0, width)

        val uBuffer = image.planes[1].buffer.duplicate()
        val vBuffer = image.planes[2].buffer.duplicate()
        val uRowStride = image.planes[1].rowStride
        val vRowStride = image.planes[2].rowStride
        val uPixelStride = image.planes[1].pixelStride
        val vPixelStride = image.planes[2].pixelStride
        var dst = ySize
        for (row in 0 until chromaHeight) {
            val uRow = row * uRowStride
            val vRow = row * vRowStride
            for (col in 0 until chromaWidth) {
                // NV12 是 U/V 交错；NV21 是 V/U 交错。MediaCodec 半平面编码器通常期望 NV12。
                out[dst++] = uBuffer.get(uRow + col * uPixelStride)
                out[dst++] = vBuffer.get(vRow + col * vPixelStride)
            }
        }
    }

    private fun copyPlane(
        plane: ImageProxy.PlaneProxy,
        width: Int,
        height: Int,
        out: ByteArray,
        outOffset: Int,
        outRowStride: Int
    ) {
        val buffer = plane.buffer.duplicate()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        for (row in 0 until height) {
            val srcRow = row * rowStride
            val dstRow = outOffset + row * outRowStride
            for (col in 0 until width) {
                out[dstRow + col] = buffer.get(srcRow + col * pixelStride)
            }
        }
    }
}
