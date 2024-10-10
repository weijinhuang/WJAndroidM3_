package com.wj.androidm3.business.ui.camera

import android.media.MediaFormat
import com.wj.androidm3.business.ui.camera.EncoderWrapper.Companion.VIDEO_CODEC_ID_AV1
import com.wj.androidm3.business.ui.camera.EncoderWrapper.Companion.VIDEO_CODEC_ID_H264
import com.wj.androidm3.business.ui.camera.EncoderWrapper.Companion.VIDEO_CODEC_ID_HEVC

class CameraUtil {
}



public fun idToStr(videoCodecId: Int): String = when (videoCodecId) {
    VIDEO_CODEC_ID_HEVC -> "HEVC"
    VIDEO_CODEC_ID_H264 -> "H264"
    VIDEO_CODEC_ID_AV1 -> "AV1"
    else -> throw RuntimeException("Unexpected video codec id " + videoCodecId)
}

public fun idToType(videoCodecId: Int): String = when (videoCodecId) {
    VIDEO_CODEC_ID_H264 -> MediaFormat.MIMETYPE_VIDEO_AVC
    VIDEO_CODEC_ID_HEVC -> MediaFormat.MIMETYPE_VIDEO_HEVC
    VIDEO_CODEC_ID_AV1 -> MediaFormat.MIMETYPE_VIDEO_AV1
    else -> throw RuntimeException("Unexpected video codec id " + videoCodecId)
}