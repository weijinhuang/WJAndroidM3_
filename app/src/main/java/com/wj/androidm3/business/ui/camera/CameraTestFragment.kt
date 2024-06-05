package com.wj.androidm3.business.ui.camera

import android.Manifest
import android.content.ContentValues
import android.graphics.Point
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaSpec
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Start
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.encoder.Encoder
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import com.wj.androidm3.R
import com.wj.androidm3.databinding.FragmentCameraTestBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel
import kotlinx.coroutines.async
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraTestFragment : BaseMVVMActivity<BaseViewModel, FragmentCameraTestBinding>() {

    private var mBitRate = 2000000

    private var FRAME_RATE = 24

    private var IFRAME_INTERVAL = 1

    private var MIME_TYPE = "video/avc"

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private val cameraCapabilities = mutableMapOf<CameraSelector, List<Quality>>()

    private var mEncoder: MediaCodec? = null

    private var mMuxer: MediaMuxer? = null

    private var mInputSurface: Surface? = null

    private fun initMuxer(outputFile: String, format: Int, onInitSuccess: (MediaMuxer) -> Unit) {
        if (mMuxer == null) {
            mMuxer = MediaMuxer(outputFile, format)
        }
        onInitSuccess.invoke(mMuxer!!)
    }

    private fun prepareEncoder() {
        mQuality?.let { quality ->
            val width: Int
            val height: Int
            when (quality) {
                Quality.FHD -> {
                    width = 1920
                    height = 1080
                }

                Quality.HD -> {
                    width = 1280
                    height = 720
                }

                Quality.SD -> {
                    width = 720
                    height = 480
                }

                Quality.UHD -> {
                    width = 3840
                    height = 2160
                }

                else -> {
                    width = 720
                    height = 480
                }

            }
            val mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                setInteger(MediaFormat.KEY_BIT_RATE, mBitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL)
            }

            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE)?.apply {
                configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                mInputSurface = createInputSurface()
                start()
            }

        }

    }

    private fun initQualityList() {
        val provider = ProcessCameraProvider.getInstance(this).get()
        provider.unbindAll()
        for (camSelector in arrayOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)) {
            try {
                // just get the camera.cameraInfo to query capabilities
                // we are not binding anything here.
                if (provider.hasCamera(camSelector)) {
                    val camera = provider.bindToLifecycle(this, camSelector)
                    QualitySelector
                        .getSupportedQualities(camera.cameraInfo)
                        .filter { quality ->
                            listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)
                                .contains(quality)
                        }.also {
                            cameraCapabilities[camSelector] = it
                        }
                    cameraCapabilities.values.last().let { cameraSelector ->
                        mCameraSelector = camSelector
                        mQuality = cameraCapabilities[mCameraSelector]?.last()
                    }
                }
            } catch (exc: java.lang.Exception) {
                WJLog.e("Camera Face $camSelector is not supported")
            }
        }
    }


    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        initQualityList()
        mViewBinding?.run {
            imageCaptureButton.setOnClickListener { takePhoto() }
            videoCaptureButton.setOnClickListener { captureVideo() }
            switchCamera.setOnClickListener {
                if (mCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                }
                mQuality = cameraCapabilities[mCameraSelector]?.last()
                bindCamera()
            }
        }
        cameraExecutor = Executors.newSingleThreadExecutor()

        requestTakePictureMission.launch(Manifest.permission.CAMERA)
    }

    private val requestTakePictureMission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { hasGrantedPermission ->
        if (hasGrantedPermission) {
            startCamera()
        }
    }


    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun captureVideoByMediacodec() {
        initMediaCodec()
    }

    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        mViewBinding?.videoCaptureButton?.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }
        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues).build()
        recording = videoCapture.output.prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(
                        this@CameraTestFragment,
                        Manifest.permission.RECORD_AUDIO
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        mViewBinding?.videoCaptureButton?.apply {
                            text = "Stop Capture"
                            isEnabled = true
                        }
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "视频录制成功:${recordEvent.outputResults.outputUri}"
                        } else {
                            recording?.close()
                            recording = null
                        }
                        mViewBinding?.videoCaptureButton?.apply {
                            text = "Start Capture"
                            isEnabled = true
                        }
                    }
                }
            }
    }

    fun test() {
        val input: Encoder.EncoderInput
//        val encoderFinder = EncoderFinder()
    }

    private var mCameraProvider: ProcessCameraProvider? = null
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            mCameraProvider = cameraProvider
            // Preview


            // Select back camera as a default
            try {
                bindCamera()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private var mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var mQuality: Quality? = null

    private fun getMediaSize(): Point {
        return when (mQuality) {
            Quality.SD -> Point(720, 480)
            Quality.HD -> Point(1280, 720)
            Quality.FHD -> Point(1920, 1080)
            Quality.UHD -> Point(3840, 2160)
            else -> Point(0, 0)
        }
    }

    private fun bindCamera() {
        mCameraProvider?.let { processCameraProvider ->

            mViewBinding?.viewFinder?.let { previewView ->
                mQuality?.let { quality ->
                    val screenXY = getMediaSize()
                    WJLog.d("分辨率：${screenXY.x} x ${screenXY.y}")
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(quality))).build()
                    videoCapture = VideoCapture.withOutput(recorder)

                    imageCapture = ImageCapture.Builder()
                        .build()

                    val imageAnalyzer = ImageAnalysis.Builder().build().also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            val bufferData = imageProxy.planes[0].buffer.toByteArray()
                            mMediaCodec?.let { mediaCodec ->
//                                val inputBuffers = mediaCodec.inputBuffers
//                                mediaCodec.dequeueInputBuffer()
                                val inputBufferIndex = mediaCodec.dequeueInputBuffer(10000)
                                if(inputBufferIndex >= 0 ){
                                    val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
                                    val toByteArray = inputBuffer?.toByteArray()
//                                    mediaCodec.queueInputBuffer(inputBufferIndex,0,)
                                }

                            }
                        }
                    }

                    val preview = Preview.Builder().setTargetAspectRatio(quality.getAspectRatio()).build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }

                    processCameraProvider.unbindAll()
                    processCameraProvider.bindToLifecycle(this@CameraTestFragment, mCameraSelector, preview, imageCapture, imageAnalyzer)

                }
            }
        }


    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_camera_test
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }


    private var mMediaCodec: MediaCodec? = null

    fun initMediaCodec() {
        mQuality?.let { quality ->

            val mediaSize = getMediaSize()
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                setCallback(object : MediaCodec.Callback() {
                    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {

                    }

                    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {

                    }

                    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                        WJLog.e("MediaCodec onError : ${e.message}")
                    }

                    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                        WJLog.e("MediaCodec onOutputFormatChanged ")

                    }
                })
                val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mediaSize.x, mediaSize.y)
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mediaSize.x * mediaSize.y * 5)
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24)
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)
                mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                mediaFormat.setInteger(MediaFormat.KEY_PROFILE,MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
                mediaFormat.setInteger(MediaFormat.KEY_LEVEL,MediaCodecInfo.CodecProfileLevel.HEVCHighTierLevel31)
                configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
        }
//        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, )
    }
}