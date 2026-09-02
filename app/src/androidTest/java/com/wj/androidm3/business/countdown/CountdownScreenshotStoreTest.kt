package com.wj.androidm3.business.countdown

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wj.androidm3.business.countdown.data.CountdownScreenshotStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CountdownScreenshotStoreTest {
    @Test
    fun deletesOnlyFilesInsideScreenshotCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = CountdownScreenshotStore(context)
        val screenshot = store.createOutputFile().apply { writeText("test") }
        val outsideFile = File(context.cacheDir, "outside_screenshot_test.png").apply { writeText("test") }

        try {
            assertTrue(store.deleteSafely(screenshot.absolutePath))
            assertFalse(screenshot.exists())
            assertFalse(store.deleteSafely(outsideFile.absolutePath))
            assertTrue(outsideFile.exists())
        } finally {
            outsideFile.delete()
        }
    }
}
