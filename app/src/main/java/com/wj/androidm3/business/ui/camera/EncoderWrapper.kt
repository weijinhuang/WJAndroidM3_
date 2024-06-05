package com.wj.androidm3.business.ui.camera

import android.hardware.camera2.params.DynamicRangeProfiles
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore.Audio.Media
import android.view.Surface
import com.wj.basecomponent.util.log.WJLog
import java.io.File
import java.lang.ref.WeakReference
import java.nio.ByteBuffer

class EncoderWrapper(
    val width: Int,
    val height: Int,
    val bitRate: Int,
    val frameRate: Int,
    val dynamicRange: Long,
    val orientationHint: Int,
    val outputFile: File,
    val useMediaRecorder: Boolean,
    val videoCodec: Int
) {
    companion object {
        val TAG = javaClass::class.java.simpleName
        val VERBOSE = false
        val IFRAME_INTERVAL = 1 //I帧间隔


        public const val VIDEO_CODEC_ID_HEVC: Int = 0
        public const val VIDEO_CODEC_ID_H264: Int = 1
        public const val VIDEO_CODEC_ID_AV1: Int = 2

    }

    private val mMimeType = idToType(videoCodec)

    private val mEncoder: MediaCodec? by lazy {
        if (useMediaRecorder) {
            null
        } else {
            MediaCodec.createEncoderByType(mMimeType)
        }
    }

    private val mEncoderThread: EncoderThread? by lazy {
        if (useMediaRecorder) {
            null
        } else {
            mEncoder?.let { encoder ->
                EncoderThread(encoder, outputFile, orientationHint)
            }
        }
    }

    private val mInputSurface: Surface by lazy {
        mEncoder?.createInputSurface() ?: throw RuntimeException("createInputSurface error")
    }

    init {
        val codecProfile = when (videoCodec) {
            VIDEO_CODEC_ID_HEVC -> when (dynamicRange) {
                DynamicRangeProfiles.HLG10 -> MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
                DynamicRangeProfiles.HDR10 -> MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
                DynamicRangeProfiles.HDR10_PLUS -> MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
                else -> -1

            }

            VIDEO_CODEC_ID_AV1 -> when {
                dynamicRange == DynamicRangeProfiles.HLG10 ->
                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10

                dynamicRange == DynamicRangeProfiles.HDR10 ->
                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10

                dynamicRange == DynamicRangeProfiles.HDR10_PLUS ->
                    MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus

                else -> -1
            }

            else -> -1
        }

        val format = MediaFormat.createVideoFormat(mMimeType, width, height)

        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)

        if (codecProfile != -1) {
            format.setInteger(MediaFormat.KEY_PROFILE, codecProfile)
            format.setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
            format.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL)
            format.setInteger(MediaFormat.KEY_COLOR_TRANSFER, getTransferFunction())
            format.setFeatureEnabled(MediaCodecInfo.CodecCapabilities.FEATURE_HdrEditing, true)
        }

        WJLog.d("FORMAT :$format")
        mEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    private fun getTransferFunction() = when (dynamicRange) {
        DynamicRangeProfiles.HLG10 -> MediaFormat.COLOR_TRANSFER_HLG
        DynamicRangeProfiles.HDR10 -> MediaFormat.COLOR_TRANSFER_ST2084
        DynamicRangeProfiles.HDR10_PLUS -> MediaFormat.COLOR_TRANSFER_ST2084
        else -> MediaFormat.COLOR_TRANSFER_SDR_VIDEO

    }

    fun getInputSurface(): Surface {
        return mInputSurface
    }

    fun start() {
        mEncoder?.start()
        mEncoderThread?.start()
        mEncoderThread?.waitUntilReady()
    }

    fun shutdown(): Boolean {
        WJLog.d("release encoder ")
        mEncoderThread?.let { encoderThread ->
            encoderThread.getHandler()?.let { handler ->
                handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SHUTDOWN))
                try {
                    encoderThread.join()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return true
    }

    fun frameAvailable() {
        mEncoderThread?.let { encoderThread ->
            encoderThread?.getHandler()?.let { handler ->
                handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_FRAME_AVALIBLE))
            }
        }
    }

    fun waitForFirstFrame() {
        mEncoderThread?.waitForFirstFrame()
    }


    private class EncoderThread(mediaCodec: MediaCodec, outputFile: File, orientationHint: Int) : Thread() {

        val mEncoder = mediaCodec
        var mEncodedFormat: MediaFormat? = null
        val mBufferInfo = MediaCodec.BufferInfo()
        val mMuxer = MediaMuxer(outputFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val mOrientationHint = orientationHint
        var mVideoTrack: Int = -1

        var mHandler: EncoderHandler? = null
        var mFrameNum = 0

        val mLock: Object = Object()

        @Volatile
        var mReady = false

        override fun run() {

            Looper.prepare()

            mHandler = EncoderHandler(this)
            WJLog.d("Encoder thread ready")

            synchronized(mLock) {

                mReady = false
                mHandler = null

            }

            WJLog.d("Looper quit")

        }

        fun waitUntilReady() {
            WJLog.d("waitUntilReady()")
            synchronized(mLock) {
                while (!mReady) {
                    try {
                        mLock.wait()
                    } catch (e: Exception) {
                        WJLog.e(e.message ?: "")
                    }
                }
            }
        }

        fun waitForFirstFrame() {
            WJLog.d("waitForFirstFrame()")
            synchronized(mLock) {
                while (mFrameNum < 1) {
                    try {
                        mLock.wait()
                    } catch (e: Exception) {
                        WJLog.d(e.message ?: "")
                    }
                }
            }
        }

        fun getHandler(): EncoderHandler? {
            synchronized(mLock) {
                if (!mReady) {
                    throw RuntimeException("Not ready")
                }
            }
            return mHandler
        }

        fun drainEncoder(): Boolean {
            val TIMEOUT_USEC: Long = 0L
            var encodedFrame = false

            while (true) {
                var encoderStatus: Int = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC)
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mEncodedFormat = mEncoder.getOutputFormat() ?: throw RuntimeException("格式错误")
                    WJLog.d("output format changed: $mEncodedFormat")
                } else if (encoderStatus < 0) {
                    WJLog.e("unexpected result from encoder.dequequeOutputBuffer:$encoderStatus")
                } else {
                    var encodedData: ByteBuffer = mEncoder.getOutputBuffer(encoderStatus) ?: throw RuntimeException(
                        "encoderOutputBuffer " + encoderStatus +
                                " was null"
                    )
                    if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        WJLog.d("ignoring buffer_flag_codec_config")
                        mBufferInfo.size = 0
                    }

                    if (mBufferInfo.size != 0) {
                        encodedData.position(mBufferInfo.offset)
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size)

                        if (mVideoTrack == -1) {
                            mEncodedFormat?.let { encodedFormat ->
                                mVideoTrack = mMuxer.addTrack(encodedFormat)
                                mMuxer.setOrientationHint(mOrientationHint)
                                mMuxer.start()
                                WJLog.d("Muxer start()")
                            }
                        }
                        mMuxer.writeSampleData(mVideoTrack, encodedData, mBufferInfo)
                        encodedFrame = true

                        WJLog.d("sent ${mBufferInfo.size} bytes to muxer, ts=${mBufferInfo.presentationTimeUs}")
                    }

                    mEncoder.releaseOutputBuffer(encoderStatus, false)
                    if ((mBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        WJLog.e("reached end of stream unexpectedly")
                        break
                    }
                }
            }

            return encodedFrame
        }

        fun frameAvailable() {
            WJLog.d("frameAvailable()")
            if (drainEncoder()) {
                synchronized(mLock) {
                    mFrameNum++
                    mLock.notify()
                }

            }
        }

        fun shutdown() {
            Looper.myLooper()?.quit()
            mMuxer.stop()
            mMuxer.release()
        }


        public class EncoderHandler(et: EncoderThread) : Handler() {
            companion object {
                val MSG_FRAME_AVALIBLE: Int = 0
                val MSG_SHUTDOWN: Int = 1
            }

            private val mWeakEncoderThread = WeakReference<EncoderThread>(et)

            override fun handleMessage(msg: Message) {

                val what: Int = msg.what

                WJLog.d("EncoderHandler:what = $what")

                val encoderThread = mWeakEncoderThread.get()

                if (encoderThread == null) {
                    WJLog.d("EncoderHandler.handleMessage: weak ref is null")
                    return
                }

                when (what) {
                    MSG_FRAME_AVALIBLE -> encoderThread.frameAvailable()
                    MSG_SHUTDOWN -> encoderThread.shutdown()
                    else -> throw RuntimeException("unknown message $what")
                }
            }

        }
    }

}