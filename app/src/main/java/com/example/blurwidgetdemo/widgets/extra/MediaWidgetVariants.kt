package dev.revilodev.onewidget.widgets.extra

import android.appwidget.AppWidgetProvider

/**
 * A pin request identifies a provider, not one of that provider's preview sizes. These provider
 * identities give the launcher an unambiguous default size while sharing the media renderer.
 */
class MediaPlayer3x1Widget : MediaPlayerWidget()
class MediaPlayer2x2Widget : MediaPlayerWidget()
class MediaPlayer3x2Widget : MediaPlayerWidget()

object MediaWidgetVariants {
    val providerClasses: List<Class<out AppWidgetProvider>> = listOf(
        MediaPlayerWidget::class.java,
        MediaPlayer3x1Widget::class.java,
        MediaPlayer2x2Widget::class.java,
        MediaPlayer3x2Widget::class.java
    )

    private val providerNames = providerClasses.mapTo(hashSetOf()) { it.name }

    fun isMediaProvider(className: String?): Boolean =
        className != null && providerNames.contains(className)

    fun providerFor(columns: Int, rows: Int): Class<out AppWidgetProvider> = when (columns to rows) {
        3 to 1 -> MediaPlayer3x1Widget::class.java
        2 to 2 -> MediaPlayer2x2Widget::class.java
        3 to 2 -> MediaPlayer3x2Widget::class.java
        else -> MediaPlayer3x1Widget::class.java
    }
}
