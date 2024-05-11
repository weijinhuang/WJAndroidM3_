package com.wj.androidm3.business.ui.camera

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Environment
import androidx.camera.core.ImageProxy
import com.wj.basecomponent.util.log.WJLog
import java.io.File
import java.io.FileOutputStream

/**
 *@Create by H.W.J 2024/5/6/006
 */
class ImageHelper {

    companion object {

        fun useYuvImageSaveFile(context: Context, imageProxy: ImageProxy, outputYOnly: Boolean) {
            val yuvImage: YuvImage = toYuvImage(imageProxy, outputYOnly)
            context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.let { DCIM ->
                val file = File("${DCIM.absolutePath}${File.separator}_${if (outputYOnly) "Y" else "YUV"}_${System.currentTimeMillis()}.png")
                saveYuvToFile(file, imageProxy.width, imageProxy.height, yuvImage)
                WJLog.d("存储图片：${file.absolutePath}")
            }
        }

        private fun toYuvImage(imageProxy: ImageProxy, onlyY: Boolean): YuvImage {
            if (imageProxy.format != ImageFormat.YUV_420_888) {
                WJLog.e("无效格式：${imageProxy.format}")
            }

            val numPixels = (imageProxy.width * imageProxy.height * 1.5f).toInt()

            WJLog.d("numPixels:$numPixels width:${imageProxy.width} height:${imageProxy.height}")

            val yBuffer = imageProxy.planes[0].buffer
            val nv21 = ByteArray(numPixels)
            var index = 0

            val yRowStride = imageProxy.planes[0].rowStride
            val yPixelStride = imageProxy.planes[0].pixelStride
            WJLog.d("yRowStride:$yRowStride yPixelStride:$yPixelStride")
            try {
                //复制Y数据
                for (y in 0 until imageProxy.height) {
                    for (x in 0 until imageProxy.width) {
                        val desPos = y * yRowStride + x * yPixelStride
                        nv21[index++] = yBuffer.get(desPos)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                WJLog.e("${e.message} 复制Y分量数据出错，index:$index ${nv21.size}")
            }


            if (onlyY) {
                WJLog.d("Y分量复制完毕，仅保存Y分量")
                return YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            }

            //复制U/V数据
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val uvRowStride = imageProxy.planes[1].rowStride
            val uvPixelStride = imageProxy.planes[1].pixelStride

            val uvWidth = imageProxy.width / 2
            val uvHeight = imageProxy.height / 2
            for (y in 0 until uvHeight) {
                for (x in 0 until uvWidth) {
                    val bufferIndex = y * uvRowStride + x * uvPixelStride
                    nv21[index++] = vBuffer.get(bufferIndex)
                    nv21[index++] = uBuffer.get(bufferIndex)
                }
            }
            WJLog.d("YUV分量复制完毕")
            return YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        }

        fun saveYuvToFile(file: File, width: Int, height: Int, yuvImage: YuvImage) {
            try {
                FileOutputStream(file).use { fos ->
                    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, fos)
                }
            } catch (e: Exception) {
                WJLog.e(e.message ?: "save YUV File error")
            }
        }
    }


}