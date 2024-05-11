package com.wj.androidm3.business.ui.camera

import android.Manifest
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.wj.androidm3.R
import com.wj.androidm3.databinding.ActivityCameraXBinding
import com.wj.basecomponent.ui.BaseMVVMActivity
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.vm.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

/**
 *@Create by H.W.J 2024/5/6/006
 */
class CameraXActivity : BaseMVVMActivity<BaseViewModel, ActivityCameraXBinding>() {

    val mExecutors = Executors.newFixedThreadPool(2)

    private var mCameraProvider: ProcessCameraProvider? = null

    private var mTakeOneYuv = true

    private var mRunning = false

    private val requestTakePictureMission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { hasGrantedPermission ->
        if (hasGrantedPermission) {
            startCamera()
        }
    }

    private val mImageAnalysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(720, 1280))
        .setOutputImageRotationEnabled(true)
        .setTargetRotation(Surface.ROTATION_0)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

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

    override fun getLayoutId(): Int {
        return R.layout.activity_camera_x
    }

    private var outputCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding?.run {
            enableAna.setOnClickListener {
                outputCount++
                WJLog.d("启用分析器 outputCount:$outputCount")
                mImageAnalysis.setAnalyzer(mExecutors) { imageProxy ->
                    if (mTakeOneYuv) {
                        mTakeOneYuv = false
                        WJLog.d("旋转角度:${imageProxy.imageInfo.rotationDegrees}")
                        ImageHelper.useYuvImageSaveFile(this@CameraXActivity, imageProxy, outputCount % 2 == 0)
                        lifecycleScope.launch(Dispatchers.Main) {
                            WJLog.i("截取一帧")
                        }
                    }
                    imageProxy.close()
                }
            }

            start.setOnClickListener {
                mCameraProvider?.let { cameraProvider ->
                    if (!mRunning) {
                        bindPreView(cameraProvider, previewView)
                    }
                }
            }

            end.setOnClickListener {
                mCameraProvider?.let {
                    it.unbindAll()
                    mRunning = false
                }
            }

            clrAna.setOnClickListener {
                mImageAnalysis.clearAnalyzer()
                WJLog.d("mImageAnalysis.clearAnalyzer()")
            }

            takeOneAnalyse.setOnClickListener {
                mTakeOneYuv = true
                WJLog.d("获取一帧，输出图片旋转：${mImageAnalysis.isOutputImageRotationEnabled}")
            }

            snapshot.setOnClickListener {
                takePhoto()
            }

            requestTakePictureMission.launch(Manifest.permission.CAMERA)


        }
    }

    private var imageCapture: ImageCapture? = null
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
                    WJLog.e("Photo capture failed: ${exc.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    WJLog.d(msg)
                }
            }
        )
    }

    private fun startCamera() {
        val previewView = mViewBinding?.previewView ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@CameraXActivity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreView(cameraProvider, previewView)
        }, ContextCompat.getMainExecutor(this@CameraXActivity))
    }

    private fun bindPreView(cameraProvider: ProcessCameraProvider, previewView: PreviewView) {
        val preView = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        preView.setSurfaceProvider(previewView.surfaceProvider)
        cameraProvider.bindToLifecycle(this@CameraXActivity, cameraSelector, preView, mImageAnalysis)
        mCameraProvider = cameraProvider
        mRunning = true
        imageCapture = ImageCapture.Builder().build()
    }

}