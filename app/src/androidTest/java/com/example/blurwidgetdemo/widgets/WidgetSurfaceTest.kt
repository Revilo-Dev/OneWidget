package dev.revilodev.onewidget.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.revilodev.onewidget.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class WidgetSurfaceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun remote(border: WidgetBorderStyle, uri: String? = null): RemoteViews = WidgetSurface.wrap(context, 98765, RemoteViews(context.packageName, R.layout.widget_blur),
            200f, 120f, color = Color.TRANSPARENT, border = border, backgroundUri = uri, useStoredImage = false)

    private fun apply(border: WidgetBorderStyle, uri: String? = null): View = remote(border, uri).apply(context, FrameLayout(context))

    @Test fun borderWrapsRoundedEdgesAndUsesPhysicalPixelDefaults() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val root = apply(WidgetBorderStyle())
            val bitmap = (root.findViewById<ImageView>(R.id.widget_surface_border).drawable as BitmapDrawable).bitmap
            assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
            assertEquals(51, Color.alpha(bitmap.getPixel(bitmap.width / 2, 1)))
            assertEquals(Color.WHITE and 0x00ffffff, bitmap.getPixel(bitmap.width / 2, 1) and 0x00ffffff)
            assertEquals(0, Color.alpha(bitmap.getPixel(bitmap.width / 2, 5)))
        }
    }

    @Test fun changingOrDisablingBorderReplacesThePreviousStyle() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val root = apply(WidgetBorderStyle(false))
            remote(WidgetBorderStyle(true, 8, 50)).reapply(context, root)
            val bitmap = (root.findViewById<ImageView>(R.id.widget_surface_border).drawable as BitmapDrawable).bitmap
            assertEquals(128, Color.alpha(bitmap.getPixel(bitmap.width / 2, 6)))
            assertEquals(0, Color.alpha(bitmap.getPixel(bitmap.width / 2, 9)))
            remote(WidgetBorderStyle(false)).reapply(context, root)
            assertEquals(View.GONE, root.findViewById<View>(R.id.widget_surface_border).visibility)
            assertEquals(1, root.findViewById<FrameLayout>(R.id.widget_surface_content).childCount)
        }
    }

    @Test fun customImageIsClippedAndLeavesTheBorderAboveContent() {
        val file = File(context.cacheDir, "surface-test.png")
        val source = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val root = apply(WidgetBorderStyle(), Uri.fromFile(file).toString())
                val bitmap = (root.findViewById<ImageView>(R.id.widget_surface_decoration).drawable as BitmapDrawable).bitmap
                assertEquals(Color.GREEN, bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
                assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
                assertTrue((root as android.view.ViewGroup).indexOfChild(root.findViewById(R.id.widget_surface_border)) > root.indexOfChild(root.findViewById(R.id.widget_surface_content)))
            }
        } finally {
            file.delete()
            source.recycle()
        }
    }
    @Test fun everyClockFontUsesALiveTextClock() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dev.revilodev.onewidget.widgets.clock.ClockFont.entries.forEach { font ->
                for ((width, height) in listOf(180f to 80f, 280f to 130f, 360f to 180f)) {
                    val content = dev.revilodev.onewidget.widgets.clock.ClockRemoteViewsRenderer.build(context,
                        dev.revilodev.onewidget.widgets.clock.ClockWidgetSettings(font = font, dateAboveTime = true),
                        dev.revilodev.onewidget.widgets.clock.ClockRenderSize(width, height),
                        mode = dev.revilodev.onewidget.widgets.clock.ClockRenderMode.CONFIG_PREVIEW).remoteViews
                    val view = content.apply(context, FrameLayout(context))
                    assertTrue(view.findViewById<View>(R.id.clock_time) is android.widget.TextClock)
                    assertEquals(View.VISIBLE, view.findViewById<View>(R.id.clock_date_above).visibility)
                    assertNotEquals(0, view.findViewById<View>(R.id.clock_date_above).layoutParams.height)
                }
            }
        }
    }

    @Test fun mediaLayoutsUseNativeElapsedTimeViews() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (layout in listOf(R.layout.widget_media_compact, R.layout.widget_media_large, R.layout.widget_media_square)) {
                val content = RemoteViews(context.packageName, layout).apply { setChronometer(R.id.media_time, 0, "%s", false) }
                val view = content.apply(context, FrameLayout(context))
                assertTrue(view.findViewById<View>(R.id.media_time) is android.widget.Chronometer)
            }
        }
    }

    @Test fun cameraPhotosRespectTheirSavedOrientation() {
        val file = File(context.cacheDir, "rotated-surface-test.jpg")
        val bitmap = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            android.media.ExifInterface(file.absolutePath).apply {
                setAttribute(android.media.ExifInterface.TAG_ORIENTATION, "6")
                saveAttributes()
            }
            val loaded = WidgetSurface.loadImage(context, Uri.fromFile(file).toString())
            assertNotNull(loaded)
            assertEquals(10, loaded!!.width)
            assertEquals(20, loaded.height)
        } finally {
            file.delete()
            bitmap.recycle()
        }
    }

    @Test fun sharedTextControlsAffectLegacyComplications() {
        val prefs = context.getSharedPreferences(dev.revilodev.onewidget.BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt("text_color_98765", Color.RED).putInt("text_scale_98765", 150).putString("text_alignment_98765", "END").commit()
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val original = RemoteViews(context.packageName, R.layout.widget_counter_large).apply(context, FrameLayout(context))
                    .findViewById<android.widget.TextView>(R.id.counter_value)
                val content = RemoteViews(context.packageName, R.layout.widget_counter_large)
                WidgetTextAppearance.apply(context, 98765, content)
                val label = content.apply(context, FrameLayout(context)).findViewById<android.widget.TextView>(R.id.counter_value)
                assertEquals(Color.RED, label.currentTextColor)
                assertEquals(original.textSize * 1.5f, label.textSize, 1f)
                assertEquals(android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL, label.gravity)
            }
        } finally {
            prefs.edit().remove("text_color_98765").remove("text_scale_98765").remove("text_alignment_98765").commit()
        }
    }

}
