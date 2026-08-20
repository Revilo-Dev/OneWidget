package com.example.blurwidgetdemo.widgets.extra
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
class OpenAppWidget : NamedActionWidget() { override val title = "Open app"; override val icon = R.drawable.phone; override fun action(context: Context, id: Int): Intent { val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE); val pkg = prefs.getString("open_app_package_$id", null); val cls = prefs.getString("open_app_class_$id", null); return if (pkg != null && cls != null) Intent().setComponent(ComponentName(pkg, cls)) else Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER) }; override fun displayText(context: Context, id: Int) = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getString("open_app_label_$id", title) ?: title; override fun iconBitmap(context: Context, id: Int): Bitmap? = runCatching { val pkg = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getString("open_app_package_$id", null) ?: return null; drawableBitmap(context.packageManager.getApplicationIcon(pkg)) }.getOrNull() }
private fun drawableBitmap(drawable: Drawable): Bitmap { val width = drawable.intrinsicWidth.coerceAtLeast(1); val height = drawable.intrinsicHeight.coerceAtLeast(1); return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { drawable.setBounds(0, 0, width, height); drawable.draw(Canvas(it)) } }
