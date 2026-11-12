package dev.revilodev.onewidget.widgets.extra

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds

open class OpenAppWidget : NamedActionWidget() {
    override val title = "Open app"
    override val icon = R.drawable.phone
    override val studioWidgetId = UniversalWidgetIds.OPEN_APP

    override fun action(context: Context, id: Int): Intent {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        val packageName = prefs.getString("open_app_package_$id", null)
        val className = prefs.getString("open_app_class_$id", null)
        return if (packageName != null && className != null) Intent().setComponent(ComponentName(packageName, className))
        else Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    }

    override fun displayText(context: Context, id: Int): String {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        return prefs.getString("open_app_custom_label_$id", null)
            ?: prefs.getString("open_app_label_$id", title) ?: title
    }

    override fun iconBitmap(context: Context, id: Int): Bitmap? {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        WidgetAssetBitmaps.fromUri(context, prefs.getString("open_app_icon_uri_$id", null))?.let { return it }
        val packageName = prefs.getString("open_app_package_$id", null) ?: return null
        return runCatching { drawableBitmap(context.packageManager.getApplicationIcon(packageName)) }.getOrNull()
    }


}

private fun drawableBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(it))
    }
}
