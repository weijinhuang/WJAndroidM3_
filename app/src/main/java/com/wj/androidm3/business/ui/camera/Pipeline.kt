package com.wj.androidm3.business.ui.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Size
import android.view.Surface
import com.wj.basecomponent.util.media.AutoFitSurfaceView

abstract class Pipeline(
    val width: Int,
    val height: Int,
    val fps: Int,
    val filterOn: Boolean,
    val dynamicRange: Long,
    val characteristics: CameraCharacteristics,
    encoder: WJEncoderWrapper,
    viewFinder: AutoFitSurfaceView
) {
    open fun createPreviewRequest(session: CameraCaptureSession, previewStabilization: Boolean): CaptureRequest? {
        return null
    }

    abstract fun createRecordRequest(session: CameraCaptureSession, previewStabilization: Boolean): CaptureRequest

    open fun destroyWindowSurface() {}

    open fun setPreviewSize(previewSize: Size) {}

    open fun createResources(surface: Surface) {}

    abstract fun getPreviewTargets(): List<Surface>

    abstract fun getRecordTargets(): List<Surface>

    open fun actionDown(encoderSurface: Surface) {}

    open fun clearFrameListener() {}

    open fun cleanup() {}

    open fun startRecording() {}

    open fun stopRecording() {}


}