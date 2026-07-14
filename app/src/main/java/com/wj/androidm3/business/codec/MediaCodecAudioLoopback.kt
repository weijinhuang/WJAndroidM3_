package com.wj.androidm3.business.codec

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 用 MediaCodec 手写一条“音频编解码闭环”：
 *
 * AudioRecord 采集 PCM
 * -> MediaCodec AAC Encoder 压缩成 AAC access unit
 * -> 内存队列模拟网络传输
 * -> MediaCodec AAC Decoder 还原成 PCM
 * -> AudioTrack 播放
 *
 * 真实通话里更推荐 Opus，因为它更适合低延迟语音；AAC 在 Android MediaCodec 里更容易演示。
 * 这个类的目标是帮助理解“PCM 原始音频”和“编码后压缩音频”之间的转换关系。
 */
class MediaCodecAudioLoopback(
    private val context: Context,
    private val onLog: (String) -> Unit
) {
    private val running = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var encoder: MediaCodec? = null
    private var decoder: MediaCodec? = null
    private var encodedFrames = 0
    private var decodedFrames = 0
    private var droppedFrames = 0
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var previousSpeakerphone = false
    private val transportQueue = ArrayDeque<EncodedAudioSample>()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        executor.execute {
            try {
                prepareAudioRoute(true)
                prepareCodecs()
                runAudioLoop()
            } catch (t: Throwable) {
                onLog("Audio loop failed: ${t.message ?: t.javaClass.simpleName}")
            } finally {
                releaseAll()
            }
        }
    }

    fun stop() {
        running.set(false)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun prepareCodecs() {
        encodedFrames = 0
        decodedFrames = 0
        droppedFrames = 0
        transportQueue.clear()

        val minRecordBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minRecordBuffer.coerceAtLeast(AUDIO_FRAME_BYTES * 2)
        )

        val minPlayBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minPlayBuffer.coerceAtLeast(AUDIO_FRAME_BYTES * 4),
            AudioTrack.MODE_STREAM
        )

        val encoderFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            SAMPLE_RATE,
            CHANNEL_COUNT
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, AAC_BIT_RATE)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_FRAME_BYTES)
        }
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }

        audioRecord?.startRecording()
        audioTrack?.play()
        onLog("Audio AAC encoder started: ${SAMPLE_RATE}Hz mono, bitRate=$AAC_BIT_RATE")
    }

    private fun runAudioLoop() {
        val record = audioRecord ?: return
        val encoderCodec = encoder ?: return
        while (running.get()) {
            val inputIndex = encoderCodec.dequeueInputBuffer(CODEC_TIMEOUT_US)
            if (inputIndex >= 0) {
                val inputBuffer = encoderCodec.getInputBuffer(inputIndex)
                if (inputBuffer != null) {
                    inputBuffer.clear()
                    // 这里读到的是 PCM：未压缩的原始音频采样，16bit 单声道。
                    val read = record.read(inputBuffer, AUDIO_FRAME_BYTES)
                    if (read > 0) {
                        encoderCodec.queueInputBuffer(
                            inputIndex,
                            0,
                            read,
                            System.nanoTime() / 1000,
                            0
                        )
                    } else {
                        // dequeueInputBuffer 取出的 buffer 必须通过 queueInputBuffer 还回 MediaCodec。
                        // 读麦克风失败时排一个 0 字节输入，避免编码器输入队列被耗尽。
                        encoderCodec.queueInputBuffer(inputIndex, 0, 0, System.nanoTime() / 1000, 0)
                    }
                }
            }
            drainEncoder()
            drainDecoder()
        }
    }

    private fun drainEncoder() {
        val codec = encoder ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        while (running.get()) {
            when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // AAC 解码器同样需要 codec-specific data，MediaCodec 会放在 outputFormat 的 csd-0。
                    startDecoder(codec.outputFormat)
                }
                else -> {
                    if (outputIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                            if (!isConfig) {
                                // 这里模拟“传输层”：编码后的 AAC access unit 先进入队列，再由接收端解码。
                                enqueueTransportSample(outputBuffer, bufferInfo)
                                drainTransportQueueToDecoder()
                                encodedFrames++
                                if (encodedFrames % 100 == 0) {
                                    onLog("Audio stats: enc=$encodedFrames dec=$decodedFrames drop=$droppedFrames")
                                }
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
        decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(format, null, null, 0)
            start()
        }
        onLog("Audio AAC decoder started with encoder output format")
    }

    private fun enqueueTransportSample(encodedData: ByteBuffer, info: MediaCodec.BufferInfo) {
        val data = ByteArray(info.size)
        encodedData.get(data)
        transportQueue.addLast(
            EncodedAudioSample(
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

    private fun feedDecoder(sample: EncodedAudioSample): Boolean {
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
        val player = audioTrack ?: return
        val bufferInfo = MediaCodec.BufferInfo()
        while (running.get()) {
            when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)) {
                MediaCodec.INFO_TRY_AGAIN_LATER -> return
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> onLog("Audio decoder format: ${codec.outputFormat}")
                else -> {
                    if (outputIndex >= 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            val pcm = ByteArray(bufferInfo.size)
                            outputBuffer.get(pcm)
                            // 解码器输出重新变成 PCM，AudioTrack 只能播放 PCM，不播放 AAC 压缩数据。
                            player.write(pcm, 0, pcm.size)
                            decodedFrames++
                        }
                        codec.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        }
    }

    private fun prepareAudioRoute(enabled: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (enabled) {
            previousAudioMode = audioManager.mode
            previousSpeakerphone = audioManager.isSpeakerphoneOn
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
        } else {
            audioManager.mode = previousAudioMode
            audioManager.isSpeakerphoneOn = previousSpeakerphone
        }
    }

    private fun releaseAll() {
        try {
            audioRecord?.stop()
        } catch (_: Throwable) {
        }
        audioRecord?.release()
        audioRecord = null

        try {
            audioTrack?.stop()
        } catch (_: Throwable) {
        }
        audioTrack?.release()
        audioTrack = null

        releaseCodec(encoder)
        releaseCodec(decoder)
        encoder = null
        decoder = null
        transportQueue.clear()
        prepareAudioRoute(false)
        executor.shutdown()
        onLog("Audio stopped. encoded=$encodedFrames decoded=$decodedFrames dropped=$droppedFrames")
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
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_COUNT = 1
        private const val AAC_BIT_RATE = 32_000
        private const val AUDIO_FRAME_BYTES = 2048
        private const val CODEC_TIMEOUT_US = 10_000L
        private const val MAX_TRANSPORT_QUEUE = 12
    }

    private data class EncodedAudioSample(
        val data: ByteArray,
        val presentationTimeUs: Long,
        val flags: Int
    )
}
