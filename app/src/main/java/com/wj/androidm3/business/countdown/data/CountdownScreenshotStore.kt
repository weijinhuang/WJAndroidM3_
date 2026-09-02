package com.wj.androidm3.business.countdown.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.util.UUID

class CountdownScreenshotStore(context: Context) {
    private val rootDirectory = File(context.applicationContext.cacheDir, DIRECTORY_NAME)

    fun createOutputFile(): File {
        check(rootDirectory.exists() || rootDirectory.mkdirs()) {
            "Unable to create screenshot cache directory"
        }
        return File(rootDirectory, "countdown_${UUID.randomUUID()}.png")
    }

    fun deleteSafely(path: String): Boolean {
        val file = validatedFile(path) ?: return false
        return !file.exists() || file.delete()
    }

    fun decodeSampled(path: String, requestedWidth: Int, requestedHeight: Int): Bitmap? {
        val file = validatedFile(path)?.takeIf { it.isFile } ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= requestedWidth &&
            bounds.outHeight / (sampleSize * 2) >= requestedHeight
        ) {
            sampleSize *= 2
        }
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        )
    }

    private fun validatedFile(path: String): File? = runCatching {
        val root = rootDirectory.canonicalFile
        val candidate = File(path).canonicalFile
        candidate.takeIf { it.parentFile == root }
    }.getOrNull()

    companion object {
        private const val DIRECTORY_NAME = "countdown_screenshots"
    }
}
