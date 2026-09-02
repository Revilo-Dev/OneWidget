package com.example.blurwidgetdemo.widgets.extra

import android.content.Context
import android.content.Intent
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds

class GoogleSearchWidget : NamedActionWidget() {
    override val title = "Search"
    override val icon = R.drawable.google
    override val studioWidgetId = UniversalWidgetIds.GOOGLE_SEARCH
    override val layoutResource = R.layout.widget_google_search

    override fun action(context: Context, id: Int) = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", "")
    override fun startLabelPadding(context: Context) = (56 * context.resources.displayMetrics.density).toInt()
}
