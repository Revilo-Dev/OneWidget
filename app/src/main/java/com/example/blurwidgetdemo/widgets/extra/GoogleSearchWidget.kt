package com.example.blurwidgetdemo.widgets.extra
import android.content.Context
import android.content.Intent
import com.example.blurwidgetdemo.R
class GoogleSearchWidget : NamedActionWidget() { override val title = "Search"; override val icon = R.drawable.google; override val layoutResource = R.layout.widget_google_search; override fun action(context: Context, id: Int) = Intent(Intent.ACTION_WEB_SEARCH).putExtra("query", "") }
