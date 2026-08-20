package com.example.blurwidgetdemo.widgets.extra
import android.content.Context
import android.content.Intent
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
class TextWidget : NamedActionWidget() { override val title = "Text"; override val icon = R.drawable.text_check_on; override fun action(context: Context, id: Int) = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME); override fun displayText(context: Context, id: Int) = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).getString("text_widget_$id", "Text") ?: "Text" }
