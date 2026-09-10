package dev.revilodev.onewidget.widgets.extra

import android.content.Context
import android.content.Intent
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds

open class GoogleSearchWidget : NamedActionWidget() {
    override val title = "Search"
    override val icon = R.drawable.google
    override val studioWidgetId = UniversalWidgetIds.GOOGLE_SEARCH
    override val layoutResource = R.layout.widget_google_search
    override fun backgroundResource() = R.drawable.widget_search_background

    override fun action(context: Context, id: Int) = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", "")
    override fun startLabelPadding(context: Context) = (56 * context.resources.displayMetrics.density).toInt()
}
