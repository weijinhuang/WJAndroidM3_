package com.wj.androidm3.business.ui.camera

import android.Manifest
import android.content.ContentValues
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraTestFragment : BaseMVVMActivity<BaseViewModel, FragmentCameraTestBinding>() {

    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService

    private val cameraCapabilities = mutableMapOf<CameraSelector, List<Quality>>()

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
                    cameraCapabilities.values.first().let { cameraSelector->
                        mCameraSelector = camSelector
                        mQuality = cameraCapabilities[mCameraSelector]?.first()
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
        initQualityList()
        mViewBinding?.run {
            imageCaptureButton.setOnClickListener { takePhoto() }
            videoCaptureButton.setOnClickListener { captureVideo() }
            switchCamera.setOnClickListener {
                if (mCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    mCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                }else{
                    mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                }
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
    private fun bindCamera() {
        mCameraProvider?.let { processCameraProvider ->

            mViewBinding?.viewFinder?.let { previewView ->
                mQuality?.let { quality ->
                    val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.HD))).build()
                    videoCapture = VideoCapture.withOutput(recorder)

                    imageCapture = ImageCapture.Builder()
                        .build()

                    val imageAnalyzer = ImageAnalysis.Builder().build().also {
                        it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                            WJLog.d("LuminosityAnalyzer:$luma")
                        })
                    }

                    val preview = Preview.Builder().setTargetAspectRatio(quality.getAspectRatio()).build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }

                    processCameraProvider.unbindAll()
                    processCameraProvider.bindToLifecycle(this@CameraTestFragment, mCameraSelector, preview, imageCapture, videoCapture)

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

    private class LuminosityAnalyzer(private val listener: (Double) -> Unit) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }


    private var mMediaCodec: MediaCodec? = null

    fun initMediaCodec() {
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
//            setInputSurface(mViewBinding?.viewFinder?.surfaceProvider?)

        }

//        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, )
    }
}