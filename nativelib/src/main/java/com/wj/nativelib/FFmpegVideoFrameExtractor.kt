package com.wj.nativelib

/**
 * FFmpeg 精确帧桥接。输出由 FFmpeg PNG encoder 直接按解码帧宽高编码，因此不会受到
 * Surface、PlayerView、屏幕分辨率或设备像素密度影响。
 */
object FFmpegVideoFrameExtractor {
    /** [width, height, durationMs, averageFrameRateNumerator, denominator] */
    external fun probeVideo(inputPath: String): LongArray

    /** @return 0 成功；负数为 native 解封装/解码/编码阶段错误码。 */
    external fun extractFrameToPng(inputPath: String, outputPath: String, timestampUs: Long): Int

    init {
        System.loadLibrary("nativelib")
    }
}
