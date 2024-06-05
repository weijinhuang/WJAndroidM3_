package com.wj.androidm3.business.ui.camera

import android.media.MediaFormat

class CameraUtil {
}



public fun idToStr(videoCodecId: Int): String = when (videoCodecId) {
    EncoderWrapper.VIDEO_CODEC_ID_HEVC -> "HEVC"
    EncoderWrapper.VIDEO_CODEC_ID_H264 -> "H264"
    EncoderWrapper.VIDEO_CODEC_ID_AV1 -> "AV1"
    else -> throw RuntimeException("Unexpected video codec id " + videoCodecId)
}

public fun idToType(videoCodecId: Int): String = when (videoCodecId) {
    EncoderWrapper.VIDEO_CODEC_ID_H264 -> MediaFormat.MIMETYPE_VIDEO_AVC
    EncoderWrapper.VIDEO_CODEC_ID_HEVC -> MediaFormat.MIMETYPE_VIDEO_HEVC
    EncoderWrapper.VIDEO_CODEC_ID_AV1 -> MediaFormat.MIMETYPE_VIDEO_AV1
    else -> throw RuntimeException("Unexpected video codec id " + videoCodecId)
}