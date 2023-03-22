package com.wj.nativelib


const val IMAGE_FORMAT_RGBA = 0x01
const val IMAGE_FORMAT_NV21 = 0x02
const val IMAGE_FORMAT_NV12 = 0x03
const val IMAGE_FORMAT_I420 = 0x04

const val RECORDER_TYPE_SINGLE_VIDEO = 0 //仅录制视频
const val RECORDER_TYPE_SINGLE_AUDIO = 1 //仅录制音频
const val RECORDER_TYPE_AV = 2 //同时录制音频和视频,打包成 MP4 文件

abstract class MediaRecorderContext {

    private val mNativeContextHandle = 0L



    external fun CreateContext();

    external fun DestroyContext()

    external fun Init():Int

    external fun StartRecord(recorderType: Int, outUrl: String, frameWidth: Int, frameHeight: Int, videoBitRate: Long, fps: Int):Int

    external fun OnAudioData(data: ByteArray, len: Int)

    external fun OnPreviewFrame(format: Int, data: ByteArray, width: Int, height: Int)

    external fun StopRecord():Int

    external fun SetTransformMatrix(translateX: Float, transLateY: Float, scaleX: Float, scaleY: Float, degree: Int, mirror: Int)

    external fun OnSurfaceCreated()

    external fun OnSurfaceChanged(width: Int, height: Int)

    external fun OnDrawFrame()

    external fun SetFilterData(index: Int, format: Int, width: Int, height: Int, bytes: ByteArray)

    external fun SetFragShader(index: Int, str: String)

    external fun UnInit():Int

}