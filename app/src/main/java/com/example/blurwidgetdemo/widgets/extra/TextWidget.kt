package com.example.blurwidgetdemo.widgets.extra

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.universal.UniversalElementContent
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetContent
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds

open class TextWidget : NamedActionWidget() {
    override val title = "Text"
    override val icon = R.drawable.text_check_on
    override val studioWidgetId = UniversalWidgetIds.TEXT

    override fun action(context: Context, id: Int) =
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    override fun displayText(context: Context, id: Int): String =
        context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            .getString("text_widget_$id", "Text") ?: "Text"

    override fun iconBitmap(context: Context, id: Int): Bitmap? {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("text_icon_enabled_$id", true)) return null
        val emoji = prefs.getString("text_emoji_$id", null)?.trim().orEmpty()
        return emoji.takeIf(String::isNotEmpty)?.let(WidgetAssetBitmaps::emoji)
    }

    override fun applyContent(views: RemoteViews, context: Context, id: Int) {
        super.applyContent(views, context, id)
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        views.setViewVisibility(
            R.id.action_icon,
            if (prefs.getBoolean("text_icon_enabled_$id", true)) View.VISIBLE else View.GONE
        )
        val background = WidgetAssetBitmaps.fromUri(
            context,
            prefs.getString("text_background_uri_$id", null)
        )
        views.setViewVisibility(R.id.action_background, if (background == null) View.GONE else View.VISIBLE)
        background?.let { views.setImageViewBitmap(R.id.action_background, it) }
    }

    override fun studioContent(context: Context, id: Int): UniversalWidgetContent {
        val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("text_icon_enabled_$id", true)
        val bitmap = iconBitmap(context, id)
        return UniversalWidgetContent(
            mapOf(
                "universal.icon" to UniversalElementContent(
                    bitmap = bitmap,
                    imageResId = if (enabled && bitmap == null) icon else null,
                    visible = enabled
                ),
                "universal.label" to UniversalElementContent(text = displayText(context, id))
            )
        )
    }
}
