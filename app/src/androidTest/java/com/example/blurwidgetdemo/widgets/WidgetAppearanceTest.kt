package dev.revilodev.onewidget.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.revilodev.onewidget.AboutActivity
import dev.revilodev.onewidget.AppearanceSettingsActivity
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.MainActivity
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.WidgetConfigActivity
import dev.revilodev.onewidget.widgets.variants.Glass2x1Widget
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetAppearanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun editingAPresetUpdatesLinkedColourAndBorders() {
        val prefs = context.getSharedPreferences("appearance_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            val initial = WidgetPreset("test", "First name", alpha = 80)
            WidgetPresets.save(prefs, listOf(initial))
            prefs.edit().putString("appearance_preset_42", initial.id).commit()
            assertEquals(80, Color.alpha(BlurWidget.tintColor(prefs, 42)))
            val edited = initial.copy(name = "Renamed", hue = 120f, saturation = 1f, alpha = 180, border = WidgetBorderStyle(true, 8, 50))
            WidgetPresets.save(prefs, listOf(edited))
            assertEquals("Renamed", WidgetPresets.selected(prefs, 42)?.name)
            assertEquals(edited.color(), BlurWidget.tintColor(prefs, 42))
            assertEquals(edited.border, WidgetAppearance.border(prefs, 42))
            prefs.edit().remove("appearance_preset_42").putInt("global_border_thickness_px", 6).commit()
            assertEquals(6, WidgetAppearance.border(prefs, 42).thicknessPx)
            WidgetAppearance.saveBorder(prefs, 42, WidgetBorderStyle(false, 3, 10))
            assertFalse(WidgetAppearance.border(prefs, 42).enabled)
            WidgetAppearance.saveBorder(prefs, 42, null)
            assertEquals(6, WidgetAppearance.border(prefs, 42).thicknessPx)
        } finally { prefs.edit().clear().commit() }
    }

    @Test fun homeSettingsAndAboutOpenWithSharedControls() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertNotNull(it.findViewById<android.view.View>(R.id.widget_picker_list)) }
            capture("home")
            swipeHome()
            scenario.onActivity { assertTrue(it.findViewById<dev.revilodev.onewidget.ui.FlowToolbarLayout>(R.id.main_toolbar_layout).toolbar.title.isNullOrEmpty()) }
            capture("home-collapsed")
        }
        ActivityScenario.launch(AppearanceSettingsActivity::class.java).use { scenario ->
            scenario.onActivity {
                assertNotNull(it.findViewById<android.view.View>(R.id.global_border_style))
                assertNotNull(it.findViewById<android.view.View>(R.id.appearance_presets))
            }
            capture("settings")
        }
        ActivityScenario.launch(AboutActivity::class.java).use { scenario ->
            scenario.onActivity {
                assertTrue(it.findViewById<android.widget.TextView>(R.id.about_header_version).text.contains("1.0.0"))
                assertNotNull(it.findViewById<android.view.View>(R.id.about_x))
                assertNotNull(it.findViewById<android.view.View>(R.id.about_original_repo))
            }
            capture("about")
        }
    }

    private fun swipeHome() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val metrics = context.resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val start = metrics.heightPixels * 0.8f
        val finish = metrics.heightPixels * 0.3f
        val down = android.os.SystemClock.uptimeMillis()
        for (step in 0..16) {
            val action = when (step) { 0 -> android.view.MotionEvent.ACTION_DOWN; 16 -> android.view.MotionEvent.ACTION_UP; else -> android.view.MotionEvent.ACTION_MOVE }
            val event = android.view.MotionEvent.obtain(down, android.os.SystemClock.uptimeMillis(), action, x, start + (finish - start) * step / 16f, 0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.sendPointerSync(event)
            event.recycle()
            android.os.SystemClock.sleep(16)
        }
    }

    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        android.os.SystemClock.sleep(150)
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val directory = java.io.File(context.getExternalFilesDir(null), "verification").apply { mkdirs() }
        java.io.File(directory, name + ".png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun placedGlassReadsResizeOptionsAndOpensSharedSettings() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val manager = AppWidgetManager.getInstance(context)
        val host = AppWidgetHost(context, 9023)
        instrumentation.uiAutomation.adoptShellPermissionIdentity(android.Manifest.permission.BIND_APPWIDGET)
        try {
            val id = host.allocateAppWidgetId()
            assertTrue(manager.bindAppWidgetIdIfAllowed(id, ComponentName(context, Glass2x1Widget::class.java)))
            for ((width, height) in listOf(110 to 40, 250 to 110, 460 to 460)) {
                manager.updateAppWidgetOptions(id, Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
                })
                assertEquals(width to height, WidgetInitialSizeSpec.resolvedBounds(manager, id, 180, 80))
                BlurWidget.updateWidget(context, manager, id)
            }
            val intent = Intent(context, WidgetConfigActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            ActivityScenario.launch<WidgetConfigActivity>(intent).use { scenario ->
                scenario.onActivity {
                    assertNotNull(it.findViewById<android.view.View>(R.id.widget_background_image))
                    assertNotNull(it.findViewById<android.view.View>(R.id.widget_border_style))
                    assertNotNull(it.findViewById<android.view.View>(R.id.widget_preset))
                    assertNotNull(it.findViewById<android.view.View>(R.id.widget_opacity_slider))
                }
            }
        } finally {
            host.deleteHost()
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }
}
