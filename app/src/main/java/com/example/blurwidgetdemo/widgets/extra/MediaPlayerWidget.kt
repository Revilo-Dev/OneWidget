package com.example.blurwidgetdemo.widgets.extra
import android.content.Context
import android.content.Intent
import com.example.blurwidgetdemo.R
class MediaPlayerWidget : NamedActionWidget() { override val title = "Media player"; override val icon = R.drawable.music_alt; override fun action(context: Context, id: Int) = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC) }
