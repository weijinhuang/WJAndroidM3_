//package com.wj.androidm3.business.ui.camera
//
//import android.graphics.Rect
//import android.graphics.SurfaceTexture
//import android.hardware.DataSpace
//import android.hardware.HardwareBuffer
//import android.hardware.camera2.CameraCaptureSession
//import android.hardware.camera2.CameraCharacteristics
//import android.hardware.camera2.CameraDevice
//import android.hardware.camera2.CaptureRequest
//import android.hardware.camera2.params.DynamicRangeProfiles
//import android.opengl.EGL14
//import android.opengl.EGLConfig
//import android.opengl.EGLExt
//import android.opengl.EGLSurface
//import android.opengl.GLES11Ext
//import android.opengl.GLES20
//import android.opengl.GLES30
//import android.os.Build
//import android.os.ConditionVariable
//import android.os.Handler
//import android.os.HandlerThread
//import android.os.Looper
//import android.util.Log
//import android.util.Range
//import android.util.Size
//import android.view.Surface
//import android.view.SurfaceControl
//import androidx.opengl.EGLImageKHR
//import com.wj.basecomponent.util.log.WJLog
//import com.wj.basecomponent.util.media.AutoFitSurfaceView
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.IntBuffer
//import javax.microedition.khronos.egl.EGL
//
//const val PQ_STR = "PQ"
//const val LINEAR_STR = "LINEAR"
//const val HLG_STR = "HLG (Android 14 or above)"
//const val HLG_WORKAROUND_STR = "HLG (Android 13)"
//const val PQ_ID: Int = 0
//const val LINEAR_ID: Int = 1
//const val HLG_ID: Int = 2
//const val HLG_WORKAROUND_ID: Int = 3
//
///** Generates a fullscreen quad to cover the entire viewport. Applies the transform set on the
//camera surface to adjust for orientation and scaling when used for copying from the camera
//surface to the render surface. We will pass an identity matrix when copying from the render
//surface to the recording / preview surfaces. */
//private val TRANSFORM_VSHADER = """
//attribute vec4 vPosition;
//uniform mat4 texMatrix;
//varying vec2 vTextureCoord;
//void main() {
//    gl_Position = vPosition;
//    vec4 texCoord = vec4((vPosition.xy + vec2(1.0, 1.0)) / 2.0, 0.0, 1.0);
//    vTextureCoord = (texMatrix * texCoord).xy;
//}
//"""
//
///**
// * Fragment shaders
// */
//private val INCLUDE_HLG_EOTF = """
//// BT.2100 / BT.2020 HLG EOTF for one channel.
//highp float hlgEotfSingleChannel(highp float hlgChannel) {
//  // Specification:
//  // https://www.khronos.org/registry/DataFormat/specs/1.3/dataformat.1.3.inline.html#TRANSFER_HLG
//  // Reference implementation:
//  // https://cs.android.com/android/platform/superproject/+/master:frameworks/native/libs/renderengine/gl/ProgramCache.cpp;l=265-279;drc=de09f10aa504fd8066370591a00c9ff1cafbb7fa
//  const highp float a = 0.17883277;
//  const highp float b = 0.28466892;
//  const highp float c = 0.55991073;
//  return hlgChannel <= 0.5 ? hlgChannel * hlgChannel / 3.0 :
//      (b + exp((hlgChannel - c) / a)) / 12.0;
//}
//
//// BT.2100 / BT.2020 HLG EOTF.
//highp vec3 hlgEotf(highp vec3 hlgColor) {
//  return vec3(
//      hlgEotfSingleChannel(hlgColor.r),
//      hlgEotfSingleChannel(hlgColor.g),
//      hlgEotfSingleChannel(hlgColor.b)
//  );
//}
//"""
//
//private val INCLUDE_YUV_TO_RGB = """
//vec3 yuvToRgb(vec3 yuv) {
//  const mat3 yuvToRgbColorTransform = mat3(
//    1.1689f, 1.1689f, 1.1689f,
//    0.0000f, -0.1881f, 2.1502f,
//    1.6853f, -0.6530f, 0.0000f
//  );
//  const vec3 yuvOffset = vec3(0.0625, 0.5, 0.5);
//  yuv = yuv - yuvOffset;
//  return clamp(yuvToRgbColorTransform * yuv, 0.0, 1.0);
//}
//"""
//
//private val TRANSFORM_HDR_VSHADER = """#version 300 es
//in vec4 vPosition;
//uniform mat4 texMatrix;
//out vec2 vTextureCoord;
//out vec4 outPosition;
//void main() {
//    outPosition = vPosition;
//    vec4 texCoord = vec4((vPosition.xy + vec2(1.0, 1.0)) / 2.0, 0.0, 1.0);
//    vTextureCoord = (texMatrix * texCoord).xy;
//    gl_Position = vPosition;
//}
//"""
//
///** Passthrough fragment shader, simply copies from the source texture */
//private val PASSTHROUGH_FSHADER = """
//#extension GL_OES_EGL_image_external : require
//precision mediump float;
//varying vec2 vTextureCoord;
//uniform samplerExternalOES sTexture;
//void main() {
//    gl_FragColor = texture2D(sTexture, vTextureCoord);
//}
//"""
//
//private val PASSTHROUGH_HDR_FSHADER = """#version 300 es
//#extension GL_OES_EGL_image_external_essl3 : require
//precision mediump float;
//in vec2 vTextureCoord;
//uniform samplerExternalOES sTexture;
//out vec4 outColor;
//void main() {
//    outColor = texture(sTexture, vTextureCoord);
//}
//"""
//
//private val YUV_TO_RGB_PASSTHROUGH_HDR_FSHADER = """#version 300 es
//#extension GL_EXT_YUV_target : require
//#extension GL_OES_EGL_image_external_essl3 : require
//precision mediump float;
//uniform __samplerExternal2DY2YEXT sTexture;
//in vec2 vTextureCoord;
//out vec4 outColor;
//""" + INCLUDE_YUV_TO_RGB +
//        """
//void main() {
//    vec4 color = texture(sTexture, vTextureCoord);
//    color.rgb = yuvToRgb(color.rgb);
//    outColor = color;
//}
//"""
//
//private val YUV_TO_RGB_PORTRAIT_HDR_FSHADER = """#version 300 es
//#extension GL_EXT_YUV_target : require
//#extension GL_OES_EGL_image_external_essl3 : require
//precision mediump float;
//uniform __samplerExternal2DY2YEXT sTexture;
//in vec2 vTextureCoord;
//out vec4 outColor;
//""" + INCLUDE_YUV_TO_RGB +
//        """
//// BT.2100 / BT.2020 HLG OETF for one channel.
//highp float hlgOetfSingleChannel(highp float linearChannel) {
//  // Specification:
//  // https://www.khronos.org/registry/DataFormat/specs/1.3/dataformat.1.3.inline.html#TRANSFER_HLG
//  // Reference implementation:
//  // https://cs.android.com/android/platform/superproject/+/master:frameworks/native/libs/renderengine/gl/ProgramCache.cpp;l=529-543;drc=de09f10aa504fd8066370591a00c9ff1cafbb7fa
//  const highp float a = 0.17883277;
//  const highp float b = 0.28466892;
//  const highp float c = 0.55991073;
//
//  return linearChannel <= 1.0 / 12.0 ? sqrt(3.0 * linearChannel) :
//      a * log(12.0 * linearChannel - b) + c;
//}
//
//// BT.2100 / BT.2020 HLG OETF.
//highp vec3 hlgOetf(highp vec3 linearColor) {
//  return vec3(
//      hlgOetfSingleChannel(linearColor.r),
//      hlgOetfSingleChannel(linearColor.g),
//      hlgOetfSingleChannel(linearColor.b)
//  );
//}
//""" + INCLUDE_HLG_EOTF +
//        """
//void main() {
//    vec4 color = texture(sTexture, vTextureCoord);
//
//    // Convert from YUV to RGB
//    color.rgb = yuvToRgb(color.rgb);
//
//    // Convert from HLG to linear
//    color.rgb = hlgEotf(color.rgb);
//
//    // Apply the portrait effect. Use gamma 2.4, roughly equivalent to what we expect in sRGB
//    float x = vTextureCoord.x * 2.0 - 1.0, y = vTextureCoord.y * 2.0 - 1.0;
//    float r = sqrt(x * x + y * y);
//    color.rgb *= pow(1.0f - r, 2.4f);
//
//    // Convert back to HLG
//    color.rgb = hlgOetf(color.rgb);
//    outColor = color;
//}
//"""
//
//private val HLG_TO_LINEAR_HDR_FSHADER = """#version 300 es
//#extension GL_OES_EGL_image_external_essl3 : require
//precision mediump float;
//uniform samplerExternalOES sTexture;
//in vec2 vTextureCoord;
//out vec4 outColor;
//""" + INCLUDE_HLG_EOTF +
//        """
//void main() {
//    vec4 color = texture(sTexture, vTextureCoord);
//
//    // Convert from HLG electrical to linear optical [0.0, 1.0]
//    color.rgb = hlgEotf(color.rgb);
//
//    outColor = color;
//}
//"""
//
//private val HLG_TO_PQ_HDR_FSHADER = """#version 300 es
//#extension GL_OES_EGL_image_external_essl3 : require
//precision mediump float;
//uniform samplerExternalOES sTexture;
//in vec2 vTextureCoord;
//out vec4 outColor;
//""" + INCLUDE_HLG_EOTF +
//        """
//// BT.2100 / BT.2020, PQ / ST2084 OETF.
//highp vec3 pqOetf(highp vec3 linearColor) {
//  // Specification:
//  // https://registry.khronos.org/DataFormat/specs/1.3/dataformat.1.3.inline.html#TRANSFER_PQ
//  // Reference implementation:
//  // https://cs.android.com/android/platform/superproject/+/master:frameworks/native/libs/renderengine/gl/ProgramCache.cpp;l=514-527;drc=de09f10aa504fd8066370591a00c9ff1cafbb7fa
//  const highp float m1 = (2610.0 / 16384.0);
//  const highp float m2 = (2523.0 / 4096.0) * 128.0;
//  const highp float c1 = (3424.0 / 4096.0);
//  const highp float c2 = (2413.0 / 4096.0) * 32.0;
//  const highp float c3 = (2392.0 / 4096.0) * 32.0;
//
//  highp vec3 temp = pow(linearColor, vec3(m1));
//  temp = (c1 + c2 * temp) / (1.0 + c3 * temp);
//  return pow(temp, vec3(m2));
//}
//
//void main() {
//    vec4 color = texture(sTexture, vTextureCoord);
//
//    // Convert from HLG electrical to linear optical [0.0, 1.0]
//    color.rgb = hlgEotf(color.rgb);
//
//    // HLG has a different L = 1 than PQ, which is 10,000 cd/m^2.
//    color.rgb /= 40.0f;
//
//    // Convert from linear optical [0.0, 1.0] to PQ electrical
//    color.rgb = pqOetf(color.rgb);
//
//    outColor = color;
//}
//"""
//
//private val PORTRAIT_FSHADER = """
//#extension GL_OES_EGL_image_external : require
//precision mediump float;
//varying vec2 vTextureCoord;
//uniform samplerExternalOES sTexture;
//void main() {
//    float x = vTextureCoord.x * 2.0 - 1.0, y = vTextureCoord.y * 2.0 - 1.0;
//    vec4 color = texture2D(sTexture, vTextureCoord);
//    float r = sqrt(x * x + y * y);
//    gl_FragColor = color * (1.0 - r);
//}
//"""
//
//private val IDENTITY_MATRIX = floatArrayOf(
//    1.0f, 0.0f, 0.0f, 0.0f,
//    0.0f, 1.0f, 0.0f, 0.0f,
//    0.0f, 0.0f, 1.0f, 0.0f,
//    0.0f, 0.0f, 0.0f, 1.0f
//)
//
//private val FULLSCREEN_QUAD = floatArrayOf(
//    -1.0f, -1.0f,  // 0 bottom left
//    1.0f, -1.0f,  // 1 bottom right
//    -1.0f, 1.0f,  // 2 top left
//    1.0f, 1.0f,  // 3 top right
//)
//
//private val EGL_GL_COLORSPACE_KHR = 0x309D
//private val EGL_GL_COLORSPACE_BT2020_LINEAR_EXT = 0x333F
//private val EGL_GL_COLORSPACE_BT2020_PQ_EXT = 0x3340
//private val EGL_GL_COLORSPACE_BT2020_HLG_EXT = 0x3540
//private val EGL_SMPTE2086_DISPLAY_PRIMARY_RX_EXT = 0x3341
//private val EGL_SMPTE2086_DISPLAY_PRIMARY_RY_EXT = 0x3342
//private val EGL_SMPTE2086_DISPLAY_PRIMARY_GX_EXT = 0x3343
//private val EGL_SMPTE2086_DISPLAY_PRIMARY_GY_EXT = 0x3344
//private val EGL_SMPTE2086_DISPLAY_PRIMARY_BX_EXT = 0x3345
//private val EGL_SMPTE2086_DISPLAY_PRIMARY_BY_EXT = 0x3346
//private val EGL_SMPTE2086_WHITE_POINT_X_EXT = 0x3347
//private val EGL_SMPTE2086_WHITE_POINT_Y_EXT = 0x3348
//private val EGL_SMPTE2086_MAX_LUMINANCE_EXT = 0x3349
//private val EGL_SMPTE2086_MIN_LUMINANCE_EXT = 0x334A
//
//class HardwarePipeline(
//    width: Int,
//    height: Int,
//    fps: Int,
//    filterOp: Boolean,
//    transfer: Int,
//    dynamicRange: Long,
//    characteristices: CameraCharacteristics,
//    encoder: EncoderWrapper,
//    viewFinder: AutoFitSurfaceView
//) : Pipeline(width, height, fps, filterOp, dynamicRange, characteristices, encoder, viewFinder) {
//
//    companion object {
//
//        fun checkGlError(errorMsg: String) {
//            val error = GLES30.glGetError()
//            if (error != GLES30.GL_NO_ERROR) {
//                val msg = errorMsg + ":gleError 0x" + Integer.toHexString(error)
//                WJLog.e(msg)
//                throw RuntimeException(msg)
//            }
//
//        }
//
//
//        private fun checkEglError(op: String) {
//            val eglError = EGL14.eglGetError()
//            if (eglError != EGL14.EGL_SUCCESS) {
//                val msg = op + ": eglError 0x" + Integer.toHexString(eglError)
//                WJLog.e(msg)
//                throw RuntimeException(msg);
//            }
//        }
//    }
//
//
//    private val renderThread: HandlerThread by lazy {
//        val renderThread = HandlerThread("HWJ.RenderThread")
//        renderThread.start()
//        renderThread
//    }
//
//
//    override fun createRecordRequest(session: CameraCaptureSession, previewStabilization: Boolean): CaptureRequest {
//        TODO("Not yet implemented")
//    }
//
//    override fun getPreviewTargets(): List<Surface> {
//        TODO("Not yet implemented")
//    }
//
//    override fun getRecordTargets(): List<Surface> {
//        TODO("Not yet implemented")
//    }
//
//
//    private class RenderHandler(
//        looper: Looper,
//        val width: Int,
//        val height: Int,
//        val fps: Int,
//        val filterOn: Boolean,
//        val transfer: Int,
//        val dynamicRange: Long,
//        val characteristices: CameraCharacteristics,
//        val encoder: EncoderWrapper,
//        val viewFinder: AutoFitSurfaceView
//    ) : Handler(looper), SurfaceTexture.OnFrameAvailableListener {
//
//        companion object {
//            val MSG_CREATE_RESOURCES = 0
//            val MSG_DESTROY_WINDOW_SURFACE = 1
//            val MSG_ACTION_DOWN = 2
//            val MSG_CLEAR_FRAME_LISTENER = 3
//            val MSG_CLEANUP = 4
//            val MSG_ON_FRAME_AVAILABLE = 5
//        }
//
//
//        private var previewSize = Size(0, 0)
//
//        /**
//         * 提供给camera的surface texture的OpenGL texture
//         */
//        private var cameraTexId: Int = 0
//
//        /**
//         * 提供给相机进行捕捉的 SurfaceTexture
//         *
//         */
//        private lateinit var cameraTexture: SurfaceTexture
//
//        /**
//         * 上面的 SurfaceTexture 转换为 Surface
//         *
//         */
//        private lateinit var cameraSurface: Surface
//
//        /**
//         * 将相机输出与渲染结合起来的 OpenGL 纹理
//         */
//        private var renderTexId: Int = 0
//
//        /**
//         * 我们要渲染的 SurfaceTexture
//         */
//        private lateinit var renderTexture: SurfaceTexture
//
//
//        /**
//         * 上面的 SurfaceTexture 转换为 Surface
//         */
//        private lateinit var renderSurface: Surface
//
//        /**
//         *通过 SurfaceControl 显示 HLG 所需的东西
//         *
//         */
//        private var contentSurfaceControl: SurfaceControl? = null
//
//        private var windowTexId: Int = 0
//        private var windowFboId: Int = 0
//
//        private var supportsNativeFences = false
//
//        /**
//         *设置texMatrix统一的存储空间
//         */
//        private val texMatrix = FloatArray(16)
//
//        /**
//         * 相机方向为 0、90、180 或 270 度
//         *
//         */
//        private val orientation: Int by lazy {
//            characteristices.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
//        }
//
//
//        @Volatile
//        private var currentlyRecording = false
//
//
//        /**EGL / OPENGL 数据*/
//        private var eglDisplay = EGL14.EGL_NO_DISPLAY
//        private var eglContext = EGL14.EGL_NO_CONTEXT
//        private var eglConfig: EGLConfig? = null
//        private var eglRenderSurface: EGLSurface? = EGL14.EGL_NO_SURFACE
//        private var eglEncoderSurface: EGLSurface? = EGL14.EGL_NO_SURFACE
//        private var eglWindowSurface: EGLSurface? = EGL14.EGL_NO_SURFACE
//        private var vertexShader = 0
//        private var cameraToRenderFragmentShader = 0
//        private var renderToPreviewFragmentShader = 0
//        private var renderToEncodeFragmentShader = 0
//
//        private var cameraToRenderShaderProgram: ShaderProgram? = null
//        private var renderToPreviewShaderProgram: ShaderProgram? = null
//        private var renderToEncodeShaderProgram: ShaderProgram? = null
//
//        private val cvResourcesCreated = ConditionVariable(false)
//        private val cvDestroyWindowSurface = ConditionVariable(false)
//        private val cvClearFramedListener = ConditionVariable(false)
//        private val cvCleanup = ConditionVariable(false)
//
//        fun startRecording() {
//            currentlyRecording = true
//        }
//
//        fun stopRecording() {
//            currentlyRecording = false
//        }
//
//        fun createRequest(session: CameraCaptureSession, previewStabilization: Boolean): CaptureRequest {
//            cvResourcesCreated.block()
//            return session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
//                addTarget(cameraSurface)
//                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(fps, fps))
//                if (previewStabilization) {
//                    set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
//                }
//            }.build()
//        }
//
//        fun setPreviewSize(previewSize: Size) {
//            this.previewSize = previewSize
//        }
//
//        fun getTargets(): List<Surface> {
//            cvResourcesCreated.block()
//            return listOf(cameraSurface)
//        }
//
//        private fun initEGL() {
//            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
//            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
//                throw RuntimeException("unable to get EGL14 display")
//            }
//            checkEglError("eglGetDisplay")
//
//            val version = intArrayOf(0, 0)
//            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
//                eglDisplay = null
//                throw RuntimeException("Unable to initialize EGL14")
//            }
//            checkEglError("eglInitialize")
//            val eglVersion = version[0] * 10 + version[1]
//            WJLog.i("eglVersion:$eglVersion")
//
//
//
//            if (isHDR()) {
//                val requiredExtensionsList = mutableListOf("EGL_KHR_gl_colorspace")
//                if (transfer == PQ_ID) {
//                    requiredExtensionsList.add("EGL_EXT_gl_colorspace_bt2020_pq")
//                } else if (transfer == LINEAR_ID) {
//                    requiredExtensionsList.add("EGL_EXT_gl_colorspace_bt2020_linear")
//                } else if (transfer == HLG_ID) {
//                    requiredExtensionsList.add("EGL_EXT_gl_colorspace_bt2020_hlg")
//                }
//
//                val eglExtensions = EGL14.eglQueryString(eglDisplay, EGL14.EGL_EXTENSIONS)
//                for (requiredExtension in requiredExtensionsList) {
//                    if (!eglExtensions.contains(requiredExtension)) {
//                        WJLog.e("EGL extension not supported: " + requiredExtension)
//                        WJLog.e("Supported extensions: ")
//                        WJLog.e(eglExtensions)
//                        throw RuntimeException("EGL extension not supported: " + requiredExtension)
//                    }
//                }
//
//                supportsNativeFences = eglVersion >= 15 && eglExtensions.contains("EGL_ANDROID_native_fence_sync")
//            }
//
//            WJLog.i("isHDR: ${isHDR()}")
//
//            if (isHDR()) {
//                WJLog.d("Preview transfer:${idToStr(transfer)}")
//            }
//
//            var renderableType = EGL14.EGL_OPENGL_ES2_BIT
//            if (isHDR()) {
//                renderableType = EGLExt.EGL_OPENGL_ES3_BIT_KHR
//            }
//
//            var rgbBits = 8
//            var alphaBits = 8
//            if (isHDR()) {
//                rgbBits = 10
//                alphaBits = 2
//            }
//
//            val configAttribList = intArrayOf(
//                EGL14.EGL_RENDERABLE_TYPE, renderableType,
//
//                EGL14.EGL_RED_SIZE, rgbBits,
//                EGL14.EGL_GREEN_SIZE, rgbBits,
//                EGL14.EGL_BLUE_SIZE, rgbBits,
//                EGL14.EGL_ALPHA_SIZE, alphaBits,
//                EGL14.EGL_NONE
//            )
//
//            val configs = arrayOfNulls<EGLConfig>(1)
//            val numConfigs = intArrayOf(1)
//            EGL14.eglChooseConfig(eglDisplay, configAttribList, 0, configs, 0, configs.size, numConfigs, 0)
//            eglConfig = configs[0]
//
//            var requestedVersion = 2
//            if (isHDR()) {
//                requestedVersion = 3
//            }
//
//            val contextAttribList = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, requestedVersion, EGL14.EGL_NONE)
//
//            eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribList, 0)
//            if (eglContext == EGL14.EGL_NO_CONTEXT) {
//                throw RuntimeException("Failed to create EGL context")
//            }
//
//            val clientVersion = intArrayOf(0)
//            EGL14.eglQueryContext(eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion, 0)
//            WJLog.d("EGLContext created , client version: ${clientVersion[0]}")
//
//            val tmpSurfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
//            val tmpSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, tmpSurfaceAttribs, 0)
//            EGL14.eglMakeCurrent(eglDisplay, tmpSurface, tmpSurface, eglContext)
//        }
//
//        fun createResources(surface: Surface) {
//            if (eglContext == EGL14.EGL_NO_CONTEXT) {
//                initEGL()
//            }
//
//            var windowSurfaceAttribs = intArrayOf(EGL14.EGL_NONE)
//            if (isHDR()) {
//                windowSurfaceAttribs = when (transfer) {
//                    PQ_ID -> intArrayOf(
//                        EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_PQ_EXT, EGL14.EGL_NONE
//                    )
//
//                    LINEAR_ID -> intArrayOf(
//                        EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_LINEAR_EXT,
//                        EGL14.EGL_NONE
//                    )
//                    // We configure HLG below
//                    HLG_ID -> intArrayOf(
//                        EGL_GL_COLORSPACE_KHR, EGL_GL_COLORSPACE_BT2020_HLG_EXT,
//                        EGL14.EGL_NONE
//                    )
//
//                    HLG_WORKAROUND_ID -> intArrayOf(EGL14.EGL_NONE)
//                    else -> throw RuntimeException("Unexpected transfer " + transfer)
//                }
//            }
//            if (!isHDR() or (transfer != HLG_WORKAROUND_ID)) {
//                eglWindowSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, surface, windowSurfaceAttribs, 0)
//                if (eglWindowSurface == EGL14.EGL_NO_SURFACE) {
//                    throw RuntimeException("ailed to create EGL texture view surface")
//                }
//            }
//            if (eglWindowSurface != EGL14.EGL_NO_SURFACE) {
//                if (isHDR() and (transfer == PQ_ID)) {
//                    val SMPTE2086_MULTIPLIER = 50000
//                    EGL14.eglSurfaceAttrib(eglDisplay, eglWindowSurface, EGL_SMPTE2086_MAX_LUMINANCE_EXT, 10000 * SMPTE2086_MULTIPLIER)
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_MIN_LUMINANCE_EXT, 0
//                    )
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_DISPLAY_PRIMARY_RX_EXT,
//                        (0.708f * SMPTE2086_MULTIPLIER).toInt()
//                    )
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_DISPLAY_PRIMARY_RY_EXT,
//                        (0.292f * SMPTE2086_MULTIPLIER).toInt()
//                    )
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_DISPLAY_PRIMARY_GX_EXT,
//                        (0.170f * SMPTE2086_MULTIPLIER).toInt()
//                    )
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_DISPLAY_PRIMARY_GY_EXT,
//                        (0.797f * SMPTE2086_MULTIPLIER).toInt()
//                    )
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_DISPLAY_PRIMARY_BX_EXT,
//                        (0.131f * SMPTE2086_MULTIPLIER).toInt()
//                    )
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_DISPLAY_PRIMARY_BY_EXT,
//                        (0.046f * SMPTE2086_MULTIPLIER).toInt()
//                    )
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_WHITE_POINT_X_EXT,
//                        (0.3127f * SMPTE2086_MULTIPLIER).toInt()
//                    )
//                    EGL14.eglSurfaceAttrib(
//                        eglDisplay, eglWindowSurface,
//                        EGL_SMPTE2086_WHITE_POINT_Y_EXT,
//                        (0.3290f * SMPTE2086_MULTIPLIER).toInt()
//                    )
//                }
//            }
//            cameraTexId = createTexture()
//            cameraTexture = SurfaceTexture(cameraTexId)
//            cameraTexture.setOnFrameAvailableListener(this)
//            cameraTexture.setDefaultBufferSize(width, height)
//            cameraSurface = Surface(cameraTexture)
//
//            if (isHDR() and (transfer == HLG_WORKAROUND_ID)) {
//                if (Build.VERSION.SDK_INT >= 29) {
//                    val surfaceControlBuilder = SurfaceControl.Builder().setName("HardwarePipeline")
//                        .setParent(viewFinder.surfaceControl)
//                    if (Build.VERSION.SDK_INT >= 33) {
//                        surfaceControlBuilder.setHidden(false)
//                    }
//                    contentSurfaceControl = surfaceControlBuilder.build()
//                }
//            }
//            renderTexId = createTexture()
//            renderTexture = SurfaceTexture(renderTexId)
//            renderTexture.setDefaultBufferSize(width, height)
//            renderSurface = Surface(renderTexture)
//
//            var renderSurfaceAttribs = intArrayOf(EGL14.EGL_NONE)
//            eglRenderSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, renderSurface, renderSurfaceAttribs, 0)
//            if (eglRenderSurface == EGL14.EGL_NO_SURFACE) {
//                throw RuntimeException("Failed to create EGL render surface")
//            }
//            createShaderResource()
//            cvResourcesCreated.open()
//
//        }
//
//        private fun createShaderResource() {
//            if (isHDR()) {
//                val extensions = GLES30.glGetString(GLES30.GL_EXTENSIONS)
//                if (!extensions.contains("GL_EXT_YUV_target")) {
//                    throw RuntimeException("Device does not support GL_EXT_YUV_target")
//                }
//
//                vertexShader = createShader(GLES30.GL_VERTEX_SHADER, TRANSFORM_HDR_VSHADER)
//
//                cameraToRenderFragmentShader = when (filterOn) {
//                    false -> createShader(
//                        GLES30.GL_FRAGMENT_SHADER,
//                        YUV_TO_RGB_PASSTHROUGH_HDR_FSHADER
//                    )
//
//                    true -> createShader(
//                        GLES30.GL_FRAGMENT_SHADER,
//                        YUV_TO_RGB_PORTRAIT_HDR_FSHADER
//                    )
//                }
//
//                cameraToRenderShaderProgram = createShaderProgram(cameraToRenderFragmentShader)
//
//                renderToPreviewFragmentShader = when (transfer) {
//                    PQ_ID -> createShader(
//                        GLES30.GL_FRAGMENT_SHADER,
//                        HLG_TO_PQ_HDR_FSHADER
//                    )
//
//                    LINEAR_ID -> createShader(
//                        GLES30.GL_FRAGMENT_SHADER,
//                        HLG_TO_LINEAR_HDR_FSHADER
//                    )
//
//                    HLG_ID,
//                    HLG_WORKAROUND_ID -> createShader(
//                        GLES30.GL_FRAGMENT_SHADER,
//                        PASSTHROUGH_HDR_FSHADER
//                    )
//
//                    else -> throw RuntimeException("Unexpected transfer " + transfer)
//                }
//
//                renderToPreviewShaderProgram = createShaderProgram(
//                    renderToPreviewFragmentShader
//                )
//
//                renderToEncodeFragmentShader = createShader(
//                    GLES30.GL_FRAGMENT_SHADER,
//                    PASSTHROUGH_HDR_FSHADER
//                )
//                renderToEncodeShaderProgram = createShaderProgram(renderToEncodeFragmentShader)
//            } else {
//                vertexShader = createShader(GLES30.GL_VERTEX_SHADER, TRANSFORM_VSHADER)
//
//                val passthroughFragmentShader = createShader(
//                    GLES30.GL_FRAGMENT_SHADER,
//                    PASSTHROUGH_FSHADER
//                )
//                val passthroughShaderProgram = createShaderProgram(passthroughFragmentShader)
//
//                cameraToRenderShaderProgram = when (filterOn) {
//                    false -> passthroughShaderProgram
//                    true -> createShaderProgram(
//                        createShader(
//                            GLES30.GL_FRAGMENT_SHADER,
//                            PORTRAIT_FSHADER
//                        )
//                    )
//                }
//
//                renderToPreviewShaderProgram = passthroughShaderProgram
//                renderToEncodeShaderProgram = passthroughShaderProgram
//            }
//
//        }
//
//        /** Creates the shader program used to copy data from one texture to another */
//        private fun createShaderProgram(fragmentShader: Int): ShaderProgram {
//            var shaderProgram = GLES30.glCreateProgram()
//            checkGlError("glCreateProgram")
//
//            GLES30.glAttachShader(shaderProgram, vertexShader)
//            checkGlError("glAttachShader")
//            GLES30.glAttachShader(shaderProgram, fragmentShader)
//            checkGlError("glAttachShader")
//            GLES30.glLinkProgram(shaderProgram)
//            checkGlError("glLinkProgram")
//
//            val linkStatus = intArrayOf(0)
//            GLES30.glGetProgramiv(shaderProgram, GLES30.GL_LINK_STATUS, linkStatus, 0)
//            checkGlError("glGetProgramiv")
//            if (linkStatus[0] == 0) {
//                val msg = "Could not link program: " + GLES30.glGetProgramInfoLog(shaderProgram)
//                GLES30.glDeleteProgram(shaderProgram)
//                throw RuntimeException(msg)
//            }
//
//            var vPositionLoc = GLES30.glGetAttribLocation(shaderProgram, "vPosition")
//            checkGlError("glGetAttribLocation")
//            var texMatrixLoc = GLES30.glGetUniformLocation(shaderProgram, "texMatrix")
//            checkGlError("glGetUniformLocation")
//
//            return ShaderProgram(shaderProgram, vPositionLoc, texMatrixLoc)
//        }
//
//        /** Create a shader given its type and source string */
//        private fun createShader(type: Int, source: String): Int {
//            val shader = GLES30.glCreateShader(type)
//            GLES30.glShaderSource(shader, source)
//            checkGlError("glShaderSource")
//            GLES30.glCompileShader(shader)
//            checkGlError("glCompileShader")
//            val compiled = intArrayOf(0)
//            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
//            checkGlError("glGetShaderiv")
//            if (compiled[0] == 0) {
//                val msg = "Could not compile shader " + type + ": " + GLES30.glGetShaderInfoLog(shader)
//                GLES30.glDeleteShader(shader)
//                throw RuntimeException(msg)
//            }
//            return shader
//        }
//
//        private fun createTexture(): Int {
//            if (eglDisplay == null) {
//                throw IllegalStateException("EGL not initialized before call to createTexture")
//            }
//            val texId = createTexId()
//            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
//            GLES30.glTexParameteri(
//                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER,
//                GLES30.GL_LINEAR
//            )
//            GLES30.glTexParameteri(
//                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER,
//                GLES30.GL_LINEAR
//            )
//            GLES30.glTexParameteri(
//                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S,
//                GLES30.GL_CLAMP_TO_EDGE
//            )
//            GLES30.glTexParameteri(
//                GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T,
//                GLES30.GL_CLAMP_TO_EDGE
//            )
//            return texId
//        }
//
//        private fun destroyWindowSurface() {
//            if (eglWindowSurface != EGL14.EGL_NO_SURFACE && eglDisplay != EGL14.EGL_NO_DISPLAY) {
//                EGL14.eglDestroySurface(eglDisplay, eglWindowSurface)
//            }
//            eglWindowSurface = EGL14.EGL_NO_SURFACE
//            cvDestroyWindowSurface.open()
//        }
//
//        fun waitDestroyWindowSurface() {
//            cvDestroyWindowSurface.block()
//        }
//
//        private fun copyTexture(
//            texId: Int, texture: SurfaceTexture, viewportRect: Rect,
//            shaderProgram: ShaderProgram, outputIsFramebuffer: Boolean
//        ) {
//            GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
//            checkGlError("glClearColor")
//            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
//            checkGlError("glClear")
//
//            shaderProgram.useProgram()
//            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
//            checkGlError("glActiveTexture")
//            GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
//            checkGlError("glBindTexture")
//
//            texture.getTransformMatrix(texMatrix)
//
//            // HardwareBuffer coordinates are flipped relative to what GLES expects
//            if (outputIsFramebuffer) {
//                val flipMatrix = floatArrayOf(
//                    1f, 0f, 0f, 0f,
//                    0f, -1f, 0f, 0f,
//                    0f, 0f, 1f, 0f,
//                    0f, 1f, 0f, 1f
//                )
//                android.opengl.Matrix.multiplyMM(texMatrix, 0, flipMatrix, 0, texMatrix.clone(), 0)
//            }
//            shaderProgram.setTexMatrix(texMatrix)
//
//            shaderProgram.setVertexAttribArray(FULLSCREEN_QUAD)
//
//            GLES30.glViewport(
//                viewportRect.left, viewportRect.top, viewportRect.right,
//                viewportRect.bottom
//            )
//            checkGlError("glViewport")
//            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
//            checkGlError("glDrawArrays")
//        }
//
//        private fun createTexId(): Int {
//            val buffer = IntBuffer.allocate(1)
//            GLES30.glGenTextures(1, buffer)
//            return buffer.get(0)
//        }
//
//        private fun destroyTexId(id: Int) {
//            val buffer = IntBuffer.allocate(1)
//            buffer.put(0, id)
//            GLES30.glDeleteTextures(1, buffer)
//        }
//
//        private fun createFboId(): Int {
//            val buffer = IntBuffer.allocate(1)
//            GLES30.glGenFramebuffers(1, buffer)
//            return buffer.get(0)
//        }
//
//        fun isHDR(): Boolean {
//            return dynamicRange != DynamicRangeProfiles.STANDARD
//        }
//
//
//        override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
//
//
//        }
//
//        private fun copyCameraToRender() {
//            EGL14.eglMakeCurrent(eglDisplay, eglRenderSurface, eglRenderSurface, eglContext)
//
//            copyTexture(
//                cameraTexId, cameraTexture, Rect(0, 0, width, height),
//                cameraToRenderShaderProgram!!, false
//            )
//
//            EGL14.eglSwapBuffers(eglDisplay, eglRenderSurface)
//            renderTexture.updateTexImage()
//        }
//
//        private fun copyRenderToPreview() {
//
//            var hardwareBuffer: HardwareBuffer? = null
//            var eglImage: EGLImageKHR? = null
//            if (transfer == HLG_WORKAROUND_ID) {
//                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, eglContext)
//
//                // TODO: use GLFrameBufferRenderer to optimize the performance
//                // Note that pooling and reusing HardwareBuffers will have significantly better
//                // memory utilization so the HardwareBuffers do not have to be allocated every frame
//                hardwareBuffer = HardwareBuffer.create(
//                    previewSize.width, previewSize.height,
//                    HardwareBuffer.RGBA_1010102, 1,
//                    HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
//                            or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
//                            or HardwareBuffer.USAGE_COMPOSER_OVERLAY
//                )
//
//                // If we're sending output buffers to a SurfaceControl we cannot render to an
//                // EGLSurface. We need to render to a HardwareBuffer instead by importing the
//                // HardwareBuffer into EGL, associating it with a texture, and framebuffer, and
//                // drawing directly into the HardwareBuffer.
//                eglImage = androidx.opengl.EGLExt.eglCreateImageFromHardwareBuffer(
//                    eglDisplay, hardwareBuffer
//                )
//                checkGlError("eglCreateImageFromHardwareBuffer")
//
//                GLES30.glBindTexture(GLES20.GL_TEXTURE_2D, windowTexId)
//                checkGlError("glBindTexture")
//                androidx.opengl.EGLExt.glEGLImageTargetTexture2DOES(GLES20.GL_TEXTURE_2D, eglImage!!)
//                checkGlError("glEGLImageTargetTexture2DOES")
//
//                GLES30.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, windowFboId);
//                checkGlError("glBindFramebuffer")
//                GLES30.glFramebufferTexture2D(
//                    GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, windowTexId, 0
//                );
//                checkGlError("glFramebufferTexture2D")
//            } else {
//                EGL14.eglMakeCurrent(eglDisplay, eglWindowSurface, eglRenderSurface, eglContext)
//            }
//
//            val cameraAspectRatio = width.toFloat() / height.toFloat()
//            val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()
//            var viewportWidth = previewSize.width
//            var viewportHeight = previewSize.height
//            var viewportX = 0
//            var viewportY = 0
//
//            /** The camera display is not the same size as the video. Letterbox the preview so that
//             * we can see exactly how the video will turn out. */
//            if (previewAspectRatio < cameraAspectRatio) {
//                /** Avoid vertical stretching */
//                viewportHeight = ((viewportHeight.toFloat() / previewAspectRatio) * cameraAspectRatio).toInt()
//                viewportY = (previewSize.height - viewportHeight) / 2
//            } else {
//                /** Avoid horizontal stretching */
//                viewportWidth = ((viewportWidth.toFloat() / cameraAspectRatio) * previewAspectRatio).toInt()
//                viewportX = (previewSize.width - viewportWidth) / 2
//            }
//
//            copyTexture(
//                renderTexId, renderTexture,
//                Rect(viewportX, viewportY, viewportWidth, viewportHeight),
//                renderToPreviewShaderProgram!!, hardwareBuffer != null
//            )
//
//            if (hardwareBuffer != null) {
//                if (contentSurfaceControl == null) {
//                    throw RuntimeException("Forgot to set up SurfaceControl for HLG preview!")
//                }
//
//                // When rendering to HLG, send each camera frame to the display and communicate the
//                // HLG colorspace here.
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    val fence = createSyncFence()
//                    if (fence == null) {
//                        GLES20.glFinish()
//                        checkGlError("glFinish")
//                    }
//                    SurfaceControl.Transaction()
//                        .setBuffer(
//                            contentSurfaceControl!!,
//                            hardwareBuffer,
//                            fence
//                        )
//                        .setDataSpace(
//                            contentSurfaceControl!!,
//                            DataSpace.pack(
//                                DataSpace.STANDARD_BT2020,
//                                DataSpace.TRANSFER_HLG,
//                                DataSpace.RANGE_FULL
//                            )
//                        )
//                        .apply()
//                    hardwareBuffer.close()
//                }
//            } else {
//                EGL14.eglSwapBuffers(eglDisplay, eglWindowSurface)
//            }
//
//            if (eglImage != null) {
//                GLES30.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//                androidx.opengl.EGLExt.eglDestroyImageKHR(eglDisplay, eglImage)
//            }
//
//        }
//    }
//
//    private class ShaderProgram(val id: Int, val vPositionLoc: Int, val texMatrixLoc: Int) {
//
//        public fun setVertexAttribArray(vertexCoords: FloatArray) {
//            val nativeBuffer = ByteBuffer.allocateDirect(vertexCoords.size * 4)
//            nativeBuffer.order(ByteOrder.nativeOrder())
//            val vertexBuffer = nativeBuffer.asFloatBuffer()
//            vertexBuffer.put(vertexCoords)
//            nativeBuffer.position(0)
//            vertexBuffer.position(0)
//
//            GLES30.glEnableVertexAttribArray(vPositionLoc)
//            checkGlError("glEnableVertexAttribArray")
//            GLES30.glVertexAttribPointer(vPositionLoc, 2, GLES30.GL_FLOAT, false, 8, vertexBuffer)
//            checkGlError("glVertexAttribPointer")
//        }
//
//        public fun setTexMatrix(texMatrix: FloatArray) {
//            GLES30.glUniformMatrix4fv(texMatrixLoc, 1, false, texMatrix, 0)
//            checkGlError("glUniformMatrix4fv")
//        }
//
//        public fun useProgram() {
//            GLES30.glUseProgram(id)
//            checkGlError("glUseProgram")
//        }
//    }
//}