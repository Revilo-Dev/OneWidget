package com.example.blurwidgetdemo.widgets.extra

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds

open class SamsungBrowserSearchWidget : NamedActionWidget() {
    override val title = "Search"
    override val icon = R.drawable.samsung_browser
    override val studioWidgetId = UniversalWidgetIds.SAMSUNG_BROWSER_SEARCH
    override val layoutResource = R.layout.widget_google_search
    override fun backgroundResource() = R.drawable.widget_search_background

    override fun action(context: Context, id: Int): Intent {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q="))
            .setPackage(SAMSUNG_BROWSER_PACKAGE)
        return if (browserIntent.resolveActivity(context.packageManager) != null) browserIntent
        else Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", "")
    }

    override fun startLabelPadding(context: Context) = (56 * context.resources.displayMetrics.density).toInt()

    private companion object { const val SAMSUNG_BROWSER_PACKAGE = "com.sec.android.app.sbrowser" }
}

open class YouTubeSearchWidget : NamedActionWidget() {
    override val title = "Search YouTube"
    override val icon = R.drawable.youtube
    override val studioWidgetId = UniversalWidgetIds.YOUTUBE_SEARCH
    override val layoutResource = R.layout.widget_google_search
    override fun backgroundResource() = R.drawable.widget_search_background

    override fun action(context: Context, id: Int): Intent {
        val youtubeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com/results?search_query="))
            .setPackage(YOUTUBE_PACKAGE)
        return if (youtubeIntent.resolveActivity(context.packageManager) != null) youtubeIntent
        else Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", "")
    }

    override fun startLabelPadding(context: Context) = (56 * context.resources.displayMetrics.density).toInt()

    private companion object { const val YOUTUBE_PACKAGE = "com.google.android.youtube" }
}
