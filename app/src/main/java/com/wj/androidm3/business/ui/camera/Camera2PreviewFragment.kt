package com.wj.androidm3.business.ui.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.ColorSpace
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ColorSpaceProfiles
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.MediaCodecList
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.ConditionVariable
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.wj.androidm3.BuildConfig
import com.wj.androidm3.R
import com.wj.androidm3.business.ui.camera.EncoderWrapper.Companion.VIDEO_CODEC_ID_H264
import com.wj.androidm3.databinding.FragmentCamera2PreviewBinding
import com.wj.basecomponent.ui.BaseMVVMFragment
import com.wj.basecomponent.util.log.WJLog
import com.wj.basecomponent.util.media.getPreviewOutputSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Camera2PreviewFragment : BaseMVVMFragment<Camera2ViewModel, FragmentCamera2PreviewBinding>() {

    private class HandlerExecutor(val mHandler: Handler) : Executor {
        override fun execute(command: Runnable) {
            if (!mHandler.post(command)) {
                throw RejectedExecutionException("$mHandler is shutting down")
            }
        }
    }

    var videoCodec: Int = 0
    var useMediaRecorder: Boolean = false
    var useHardware: Boolean = true
    var colorSpace: Int = 0
    var width = Int.MAX_VALUE
    var height = 0
    var fps = 0
    var filterOn = false
    var transfer = 0
    var dynamicRange = 1L


    private var mCamera: CameraDevice? = null

    private var mCameraId: String? = null

    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)

    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var mCharacteristics: CameraCharacteristics? = null

//    by lazy {
//        cameraManager.getCameraCharacteristics(cameraId)
//    }

    private var encoder: WJEncoderWrapper? = null
    //by lazy { createEncoder() }

    /** Captures frames from a [CameraDevice] for our video recording */
    private var mSession: CameraCaptureSession? = null

    private var mPipeLine: Pipeline? = null
//    by lazy {
//        run {
//            HardwarePipeline(width, height, fps, filterOn, transfer, dynamicRange, characteristics, encoder, mViewBinding?.viewFinder!!)
//        }
//
//    }

    /**
     * Setup a [Surface] for the encoder
     */
    private var encoderSurface: Surface? = null
//    by lazy {
//        encoder.getInputSurface()
//    }

    /** Condition variable for blocking until the recording completes */
    private val cvRecordingStarted = ConditionVariable(false)
    private val cvRecordingComplete = ConditionVariable(false)

    @Volatile
    private var recordingStarted = false

    @Volatile
    private var recordingComplete = false

    private var previewRequest: CaptureRequest? = null
//    by lazy {
//        mPipeLine.createPreviewRequest(mSession, false)
//    }

    /** Requests used for preview and recording in the [CameraCaptureSession] */
    private var recordRequest: CaptureRequest? = null
//    by lazy {
//        mPipeLine.createRecordRequest(mSession, false)
//    }

    private var recordingStartMillis: Long = 0L

    /** Orientation of the camera as 0, 90, 180, or 270 degrees */
    private val orientation: Int? = null
//    by lazy {
//        characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
//    }

    /** File where the recording will be saved */
    private val outputFile: File by lazy { createFile(requireContext(), "mp4") }


    companion object {
        private data class CameraInfo(
            val name: String, val cameraId: String, val size: Size, val fps: Int
        )

        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000
        private const val MIN_REQUIRED_RECORDING_TIME_MILLIS: Long = 1000L

        /** Milliseconds used for UI animations */
        const val ANIMATION_FAST_MILLIS = 50L
        const val ANIMATION_SLOW_MILLIS = 100L
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
        private fun createFile(context: Context, extension: String): File {
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            return File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "VID_${sdf.format(Date())}.$extension")
        }
    }

    override fun firstCreateView() {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewBinding?.run {
            captureButton.setOnClickListener {
                if (mViewModel.mRecording) {
                    lifecycleScope.launch {
                        stopRecording()
                    }
                } else {
                    startRecording()
                }
            }
            enumerateVideoCameras { cameraId ->
                initCodec(cameraId) { characteristics, encoder, pipeLine, videoCodec ->
                    initViewFinderHolderCallback()

                }

            }

        }
    }

    private fun initCodec(
        cameraId: String, callback: (characteristics: CameraCharacteristics, encoder: WJEncoderWrapper, pipeLine: Pipeline, videoCodec: Int) -> Unit
    ) {

        cameraManager.getCameraCharacteristics(cameraId).let { characteristics ->
            val videoCodecIdList = listOf(VIDEO_CODEC_ID_H264)
            val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)

            for (codecInfo in mediaCodecList.getCodecInfos()) {
                if (!codecInfo.isEncoder()) {
                    continue
                }

                val types = codecInfo.getSupportedTypes()
                for (type in types) {
                    for (videoCodecId in videoCodecIdList) {
                        if (type.equals(idToType(videoCodecId), ignoreCase = true)) {
                            videoCodec = videoCodecId
                            break
                        }
                    }
                }
            }
            var width1 = width
            var height1 = height
            var orientationHint = orientation
            if (orientation == 90 || orientation == 270) {
                width1 = height
                height1 = width
            }
            orientationHint = 0

            encoder = WJEncoderWrapper(
                width1, height1, RECORDER_VIDEO_BITRATE, fps, dynamicRange, orientationHint, outputFile, useMediaRecorder, videoCodec
            ).apply {
                mPipeLine = HardwarePipeline(width, height, fps, filterOn, transfer, dynamicRange, characteristics, this, mViewBinding?.viewFinder!!)
            }
            mCharacteristics = characteristics
            callback.invoke(characteristics, encoder!!, mPipeLine!!, videoCodec)
        }

    }

    private fun enumerateVideoCameras(callback: (cameraId: String) -> Unit) {
        val availableCameras: MutableList<CameraInfo> = mutableListOf()
        val cameraIdList = cameraManager.cameraIdList
        cameraIdList.forEach { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val orientation = lensOrientationString(
                characteristics.get(CameraCharacteristics.LENS_FACING)!!
            )
            if (orientation == "Front") {
                // Query the available capabilities and output formats
                characteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
                )?.let { capabilities ->
                    characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                    )?.let { cameraConfig ->
                        if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
                            // Recording should always be done in the most efficient format, which is
                            //  the format native to the camera framework
                            val targetClass = MediaRecorder::class.java

                            // For each size, list the expected FPS
                            cameraConfig.getOutputSizes(targetClass).forEach { size ->
                                // Get the number of seconds that each frame will take to process
                                val secondsPerFrame = cameraConfig.getOutputMinFrameDuration(targetClass, size) / 1_000_000_000.0
                                // Compute the frames per second to let user select a configuration
                                if (size.width < width) {
                                    fps = if (secondsPerFrame > 0) (1.0 / secondsPerFrame).toInt() else 0
                                    mCameraId = id
                                    width = size.width
                                    height = size.height
                                    fps = fps
                                }
                                val fpsLabel = if (fps > 0) "$fps" else "N/A"

                                availableCameras.add(
                                    CameraInfo(
                                        "$orientation ($id) $size $fpsLabel FPS", id, size, fps
                                    )
                                )
                            }
                        }
                    }
                }
            }


        }
        mCameraId?.let(callback)
    }

    private fun lensOrientationString(value: Int) = when (value) {
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
        else -> "Unknown"
    }

    private fun initViewFinderHolderCallback() {
        WJLog.d("initViewFinderHolderCallback")
        mViewBinding?.viewFinder?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mPipeLine?.destroyWindowSurface()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                WJLog.d("surfaceCreated")
                mCharacteristics?.let { characteristics ->
                    mViewBinding?.run {
                        val previewSize = getPreviewOutputSize(viewFinder.display, characteristics, SurfaceHolder::class.java)
                        viewFinder.setAspectRatio(previewSize.width, previewSize.height)
                        mPipeLine?.setPreviewSize(previewSize)
                        lifecycleScope.launch {
                            mPipeLine?.createResources(holder.surface)
                            initializeCamera()
                        }
                    }
                }
            }
        })
    }

    private fun initializeCamera() {
        WJLog.d("initializeCamera")
        lifecycleScope.launch {
            mCameraId?.let { cameraId ->
                mPipeLine?.let { pipeLine ->
                    cameraManager.cameraIdList
                    mCamera = openCamera(cameraManager, cameraId, cameraHandler).apply {
                        mPipeLine?.let { pipeLine ->
                            val previewTarget = pipeLine.getPreviewTargets()
                            mSession = createCaptureSession(this, previewTarget, cameraHandler, recordingCompleteOnClose = true).let { session ->
                                recordRequest = pipeLine.createRecordRequest(session, false)
                                previewRequest = pipeLine.createPreviewRequest(session, false)
                                if (previewRequest == null) {
                                    recordRequest?.let { request ->
                                        session.setRepeatingRequest(request, null, cameraHandler)
                                    }
                                } else {
                                    session.setRepeatingRequest(previewRequest!!, null, cameraHandler)
                                }
                                session
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startRecording() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (!recordingStarted) {
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                mCamera?.let { camera ->
                    mPipeLine?.let { pipeLine ->
                        encoder?.let { encoder ->
                            mSession?.let { session ->
                                recordRequest?.let { recordRequest ->
                                    encoderSurface = encoder.getInputSurface()
                                    pipeLine.actionDown(encoderSurface!!)
                                    recordingStarted = true
                                    encoder.start()
                                    cvRecordingStarted.open()
                                    pipeLine.startRecording()

                                    if (previewRequest != null) {
                                        val recordTargets = pipeLine.getRecordTargets()
                                        session.close()
                                        mSession = createCaptureSession(camera, recordTargets, cameraHandler, true)
                                        session.setRepeatingRequest(recordRequest, object : CameraCaptureSession.CaptureCallback() {
                                            override fun onCaptureCompleted(
                                                session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult
                                            ) {
                                                if (isCurrentlyRecording()) {
                                                    encoder.frameAvailable()
                                                }
                                            }
                                        }, cameraHandler)
                                    }
                                    mViewModel.mRecording = true
                                    lifecycleScope.launch(Dispatchers.Main){
                                        mViewBinding?.captureButton?.background = activity?.getDrawable( R.drawable.ic_shutter_pressed)
                                    }
                                    recordingStartMillis = System.currentTimeMillis()
                                    WJLog.d("开始录制")
                                }

                            }
                        }
                    }

                }

            }
        }
    }

    private suspend fun stopRecording() {
        mViewModel.mRecording = false

        lifecycleScope.launch(Dispatchers.Main){
            mViewBinding?.captureButton?.background = activity?.getDrawable( R.drawable.ic_shutter_normal)
        }
        cvRecordingStarted.block()
        encoder?.waitForFirstFrame()
        mSession?.stopRepeating()
        mSession?.close()
        mPipeLine?.clearFrameListener()
        cvRecordingComplete.block()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        val elapsedTimeMillis = System.currentTimeMillis() - recordingStartMillis
        if (elapsedTimeMillis < MIN_REQUIRED_RECORDING_TIME_MILLIS) {
            delay(MIN_REQUIRED_RECORDING_TIME_MILLIS)
        }
        delay(ANIMATION_SLOW_MILLIS)
        mPipeLine?.cleanup()
        if (encoder?.shutdown() == true) {
            MediaScannerConnection.scanFile(requireView().context, arrayOf(outputFile.absolutePath), null, null)
            if (outputFile.exists()) {
                WJLog.d("录制完毕：${outputFile.absolutePath}")
//                startActivity(Intent().apply {
//                    action = Intent.ACTION_VIEW
//                    type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(outputFile.extension)
//                    val authority = "${BuildConfig.APPLICATION_ID}.provider"
//                    data = FileProvider.getUriForFile(requireActivity(), authority, outputFile)
//                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
//                })
            } else {
                // TODO:
                //  1. Move the callback to ACTION_DOWN, activating it on the second press
                //  2. Add an animation to the button before the user can press it again
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        activity, "文件不存在", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun createCaptureSession(
        device: CameraDevice, targets: List<Surface>, handler: Handler, recordingCompleteOnClose: Boolean
    ): CameraCaptureSession = suspendCoroutine {
        WJLog.d("createCaptureSession")
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                it.resume(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                WJLog.e(exc.message.toString())
//                it.resumeWithException(exc)
            }

            override fun onClosed(session: CameraCaptureSession) {
                if (!recordingCompleteOnClose or !isCurrentlyRecording()) {
                    return
                }
                recordingComplete = true
                mPipeLine?.stopRecording()
                cvRecordingComplete.open()
            }
        }
        setupSessionWithDynamicRageProfile(device, targets, handler, stateCallback)

    }

    private fun isCurrentlyRecording(): Boolean {
        return recordingStarted && !recordingComplete
    }

    private fun setupSessionWithDynamicRageProfile(
        device: CameraDevice, targets: List<Surface>, handler: Handler, stateCallback: CameraCaptureSession.StateCallback
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val outputConfigs = mutableListOf<OutputConfiguration>()
            for (target in targets) {
                val outputConfig = OutputConfiguration(target)
                outputConfig.dynamicRangeProfile = dynamicRange
                outputConfigs.add(outputConfig)
            }
            val sessionConfig = SessionConfiguration(SessionConfiguration.SESSION_REGULAR, outputConfigs, HandlerExecutor(handler), stateCallback)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && colorSpace != ColorSpaceProfiles.UNSPECIFIED) {
                sessionConfig.setColorSpace(ColorSpace.Named.values()[colorSpace])
            }
            device.createCaptureSession(sessionConfig)
            return true
        } else {
            device.createCaptureSession(targets, stateCallback, handler)
            return false
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(manager: CameraManager, cameraId: String, handler: Handler? = null): CameraDevice = suspendCancellableCoroutine {
//        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

        WJLog.d("openCamera")
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {

            override fun onOpened(device: CameraDevice) = it.resume(device)

            override fun onDisconnected(camera: CameraDevice) {
                requireActivity().finish()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                WJLog.e(exc.message.toString())
                if (it.isActive) {
                    it.resumeWithException(exc)
                }
            }

        }, handler)
//        }

    }


    override fun getLayoutId(): Int {
        return R.layout.fragment_camera_2_preview
    }

    override fun onDestroy() {
        super.onDestroy()
        mPipeLine?.clearFrameListener()
        mPipeLine?.cleanup()
        cameraThread.quitSafely()
        encoderSurface?.release()
    }

}