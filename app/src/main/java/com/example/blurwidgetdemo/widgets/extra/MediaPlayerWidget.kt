package com.example.blurwidgetdemo.widgets.extra

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.widgets.WidgetProgressBarRenderer
import com.example.blurwidgetdemo.studio.universal.UniversalElementContent
import com.example.blurwidgetdemo.studio.universal.UniversalProductionWidgetRenderer
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetActions
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetContent
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetIds

/** Shows the active media session when notification-listener access has been enabled. */
class MediaPlayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidget(context, manager, id)
    override fun onDeleted(context: Context, ids: IntArray) =
        UniversalProductionWidgetRenderer.deleteModes(context, UniversalWidgetIds.MEDIA_PLAYER, ids)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        activeController(context)?.let { controller ->
            when (intent.action) {
                ACTION_PREVIOUS -> controller.transportControls.skipToPrevious()
                ACTION_TOGGLE -> if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) controller.transportControls.pause() else controller.transportControls.play()
                ACTION_NEXT -> controller.transportControls.skipToNext()
                else -> return
            }
        }
        updateWidget(context, AppWidgetManager.getInstance(context), id)
    }

    companion object {
        private const val ACTION_PREVIOUS = "com.example.blurwidgetdemo.action.MEDIA_PREVIOUS"
        private const val ACTION_TOGGLE = "com.example.blurwidgetdemo.action.MEDIA_TOGGLE"
        private const val ACTION_NEXT = "com.example.blurwidgetdemo.action.MEDIA_NEXT"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, MediaPlayerWidget::class.java)).forEach { updateWidget(context, manager, it) }
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val controller = activeController(context)
            val state = controller?.playbackState
            val metadata = controller?.metadata
            val isPlaying = state?.state == PlaybackState.STATE_PLAYING
            val large = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80) > 100
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "No media"
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Nothing is playing"
            val art = artwork(metadata)
            val hasMedia = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null
            val duration = duration(metadata)
            val progressBitmap = WidgetProgressBarRenderer.renderMedia(
                progress(state, metadata),
                artworkColour(art)
            )
            val previousAction = command(context, id, ACTION_PREVIOUS, 0)
            val toggleAction = command(context, id, ACTION_TOGGLE, 1)
            val nextAction = command(context, id, ACTION_NEXT, 2)
            val rootAction = controller?.sessionActivity ?: PendingIntent.getActivity(
                context,
                id,
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val promoted = UniversalProductionWidgetRenderer.buildIfPromoted(
                context,
                manager,
                id,
                UniversalWidgetIds.MEDIA_PLAYER,
                UniversalWidgetContent(
                    mapOf(
                        "universal.art" to UniversalElementContent(
                            bitmap = art,
                            imageResId = if (art == null) R.drawable.music_idle_bob else null
                        ),
                        "universal.title" to UniversalElementContent(text = title),
                        "universal.artist" to UniversalElementContent(text = artist),
                        "universal.previous" to UniversalElementContent(imageResId = R.drawable.control_previous),
                        "universal.play_pause" to UniversalElementContent(
                            imageResId = if (isPlaying) R.drawable.control_pause else R.drawable.control_play
                        ),
                        "universal.next" to UniversalElementContent(imageResId = R.drawable.control_next),
                        "universal.progress" to UniversalElementContent(
                            bitmap = progressBitmap,
                            visible = hasMedia && duration > 0L
                        ),
                        "universal.time" to UniversalElementContent(
                            text = playbackTime(state, duration),
                            visible = hasMedia && duration > 0L
                        )
                    )
                ),
                UniversalWidgetActions(
                    root = rootAction,
                    elements = mapOf(
                        "universal.previous" to previousAction,
                        "universal.play_pause" to toggleAction,
                        "universal.next" to nextAction
                    )
                )
            )
            val views = promoted ?: RemoteViews(
                context.packageName,
                if (large) R.layout.widget_media_large else R.layout.widget_media_compact
            ).apply {
                BlurWidget.applySurfaceTint(this, prefs, id)
                setTextViewText(R.id.media_title, title)
                setTextViewText(R.id.media_artist, artist)
                setViewVisibility(R.id.media_artist, if (large) View.VISIBLE else View.GONE)
                art?.let {
                    setImageViewBitmap(R.id.media_art, it)
                    setViewVisibility(R.id.media_art, View.VISIBLE)
                    setViewVisibility(R.id.media_idle_icon, View.GONE)
                } ?: run {
                    setViewVisibility(R.id.media_art, View.GONE)
                    setImageViewResource(R.id.media_idle_icon, R.drawable.music_idle_bob)
                    setViewVisibility(R.id.media_idle_icon, View.VISIBLE)
                }
                setImageViewResource(R.id.media_previous, R.drawable.control_previous)
                setImageViewResource(R.id.media_play_pause, if (isPlaying) R.drawable.control_pause else R.drawable.control_play)
                setImageViewResource(R.id.media_next, R.drawable.control_next)
                setImageViewBitmap(R.id.media_progress, progressBitmap)
                setTextViewText(R.id.media_time, playbackTime(state, duration))
                setViewVisibility(R.id.media_progress, if (large && hasMedia && duration > 0L) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.media_time, if (large && hasMedia && duration > 0L) View.VISIBLE else View.GONE)
                setOnClickPendingIntent(R.id.media_previous, previousAction)
                setOnClickPendingIntent(R.id.media_play_pause, toggleAction)
                setOnClickPendingIntent(R.id.media_next, nextAction)
                setOnClickPendingIntent(android.R.id.background, rootAction)
            }
            manager.updateAppWidget(id, views)
        }

        private fun activeController(context: Context): MediaController? = runCatching {
            context.getSystemService(MediaSessionManager::class.java)
                .getActiveSessions(ComponentName(context, MediaNotificationListener::class.java))
                .firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: context.getSystemService(MediaSessionManager::class.java)
                    .getActiveSessions(ComponentName(context, MediaNotificationListener::class.java)).firstOrNull()
        }.getOrNull()

        private fun artwork(metadata: MediaMetadata?): Bitmap? = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)

        private fun duration(metadata: MediaMetadata?): Long =
            metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0 } ?: 0L

        private fun progress(state: PlaybackState?, metadata: MediaMetadata?): Int {
            val duration = duration(metadata).takeIf { it > 0 } ?: return 0
            return (currentPosition(state, duration) * 100 / duration).toInt().coerceIn(0, 100)
        }

        private fun playbackTime(state: PlaybackState?, duration: Long): String =
            "${formatTime(currentPosition(state, duration))}/${formatTime(duration)}"

        private fun currentPosition(state: PlaybackState?, duration: Long): Long {
            val playbackState = state ?: return 0L
            val position = playbackState.position
            if (playbackState.state != PlaybackState.STATE_PLAYING || playbackState.lastPositionUpdateTime <= 0L) {
                return position.coerceIn(0L, duration)
            }
            val elapsed = (SystemClock.elapsedRealtime() - playbackState.lastPositionUpdateTime).coerceAtLeast(0L)
            return (position + (elapsed * playbackState.playbackSpeed).toLong()).coerceIn(0L, duration)
        }

        private fun formatTime(milliseconds: Long): String {
            val seconds = milliseconds / 1_000L
            return "%d:%02d".format(seconds / 60L, seconds % 60L)
        }

        /** Selects a saturated artwork colour so the bar belongs to the currently playing song. */
        private fun artworkColour(art: Bitmap?): Int {
            if (art == null) return 0xFF8E99FF.toInt()
            val buckets = HashMap<Int, Int>()
            val stepX = (art.width / 28).coerceAtLeast(1)
            val stepY = (art.height / 28).coerceAtLeast(1)
            var bestBucket = 0
            var bestCount = 0
            for (y in 0 until art.height step stepY) for (x in 0 until art.width step stepX) {
                val pixel = art.getPixel(x, y)
                if (Color.alpha(pixel) < 160) continue
                val max = maxOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
                val min = minOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
                if (max < 55 || max - min < 28) continue
                val bucket = (Color.red(pixel) / 32 shl 16) or (Color.green(pixel) / 32 shl 8) or (Color.blue(pixel) / 32)
                val count = (buckets[bucket] ?: 0) + 1
                buckets[bucket] = count
                if (count > bestCount) {
                    bestBucket = bucket
                    bestCount = count
                }
            }
            return if (bestCount == 0) 0xFF8E99FF.toInt() else Color.rgb(
                ((bestBucket shr 16 and 0xFF) * 32 + 16).coerceAtMost(255),
                ((bestBucket shr 8 and 0xFF) * 32 + 16).coerceAtMost(255),
                ((bestBucket and 0xFF) * 32 + 16).coerceAtMost(255)
            )
        }

        private fun command(context: Context, id: Int, action: String, offset: Int) = PendingIntent.getBroadcast(context, id * 10 + offset,
            Intent(context, MediaPlayerWidget::class.java).setAction(action).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
