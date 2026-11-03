package com.github.jimmy90109.geoalarm.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewItem
import com.github.jimmy90109.geoalarm.ui.components.MediaPreviewType
import com.github.jimmy90109.geoalarm.ui.components.resolveAspectRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

internal object MediaRatioFixtures {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    fun image(name: String, width: Int, height: Int, rotated: Boolean = false): MediaPreviewItem {
        val file = File(context.cacheDir, "ratio-$name.jpg")
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.LTGRAY)
        val paint = Paint().apply { color = Color.GREEN }
        canvas.drawRect(0f, 0f, width / 2f, height / 2f, paint)
        paint.color = Color.BLUE
        canvas.drawRect(width / 2f, height / 2f, width.toFloat(), height.toFloat(), paint)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        if (rotated) ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        return MediaPreviewItem(name, file.path, name, MediaPreviewType.IMAGE)
    }
    fun video(rotated: Boolean): MediaPreviewItem {
        val name = if (rotated) "rotated" else "landscape"
        val file = File(context.cacheDir, "ratio-$name.mp4")
        InstrumentationRegistry.getInstrumentation().context.assets.open("media/$name.mp4").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return MediaPreviewItem(name, file.path, name, MediaPreviewType.VIDEO, durationMillis = 300L)
    }
}

class MediaAspectRatioTest {
    @Test fun readsActualImageDimensionsIncludingExifRotation() {
        for ((width, height) in listOf(320 to 180, 180 to 320, 240 to 240, 40 to 800)) {
            val item = MediaRatioFixtures.image("${width}x$height", width, height)
            assertEquals(width.toFloat() / height, item.resolveAspectRatio()!!, 0.001f)
        }
        assertEquals(180f / 320, MediaRatioFixtures.image("rotated", 320, 180, true).resolveAspectRatio()!!, 0.001f)
    }
    @Test fun readsVideoDisplayRotation() {
        assertEquals(320f / 180, MediaRatioFixtures.video(false).resolveAspectRatio()!!, 0.001f)
        assertEquals(180f / 320, MediaRatioFixtures.video(true).resolveAspectRatio()!!, 0.001f)
    }
    @Test fun validStoredDimensionsTakePrecedenceAndMissingFilesAreSafe() {
        val item = MediaPreviewItem("missing", "/missing", "missing", MediaPreviewType.IMAGE, 300, 200)
        assertEquals(1.5f, item.resolveAspectRatio()!!, 0.001f)
        assertNull(item.copy(width = 0).resolveAspectRatio())
        assertNull(item.copy(type = MediaPreviewType.VIDEO, width = null, height = null).resolveAspectRatio())
    }
}
