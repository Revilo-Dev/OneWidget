package dev.revilodev.onewidget.widgets.extra

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
import android.util.LruCache
import android.view.View
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.widgets.WidgetProgressBarRenderer
import dev.revilodev.onewidget.studio.universal.UniversalElementContent
import dev.revilodev.onewidget.studio.universal.UniversalProductionWidgetRenderer
import dev.revilodev.onewidget.studio.universal.UniversalWidgetActions
import dev.revilodev.onewidget.studio.universal.UniversalWidgetContent
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds
import kotlin.math.roundToInt

/** Shows the active media session when notification-listener access has been enabled. */
open class MediaPlayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = ids.forEach { updateWidget(context, manager, it) }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) = updateWidget(context, manager, id)
    override fun onDeleted(context: Context, ids: IntArray) {
        UniversalProductionWidgetRenderer.deleteModes(context, UniversalWidgetIds.MEDIA_PLAYER, ids)
        val editor = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE).edit()
        ids.forEach { id ->
            editor.remove(backgroundTrackKey(id))
        }
        editor.apply()
    }

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
        private const val ACTION_PREVIOUS = "dev.revilodev.onewidget.action.MEDIA_PREVIOUS"
        private const val ACTION_TOGGLE = "dev.revilodev.onewidget.action.MEDIA_TOGGLE"
        private const val ACTION_NEXT = "dev.revilodev.onewidget.action.MEDIA_NEXT"
        private const val MEDIA_PROGRESS_HEIGHT_DP = 10f
        private const val MEDIA_PROGRESS_RESERVED_WIDTH_DP = 106
        private const val MAX_PROGRESS_BITMAP_WIDTH = 1_200

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            MediaWidgetVariants.providerClasses.forEach { provider ->
                manager.getAppWidgetIds(ComponentName(context, provider))
                    .forEach { updateWidget(context, manager, it) }
            }
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
            val controller = activeController(context)
            val state = controller?.playbackState
            val metadata = controller?.metadata
            val isPlaying = state?.state == PlaybackState.STATE_PLAYING
            val options = manager.getAppWidgetOptions(id)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)
            val singleCell = width <= 100 && height <= 100
            val large = height > 100
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "No media"
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Nothing is playing"
            val art = artwork(metadata)
            val hasMedia = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null
            val duration = duration(metadata)
            val (progressWidth, progressHeight) = progressBitmapSize(
                width,
                context.resources.displayMetrics.density
            )
            val progressBitmap = WidgetProgressBarRenderer.renderMedia(
                progress(state, metadata),
                artworkColour(art),
                progressWidth,
                progressHeight
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
                when {
                    singleCell -> R.layout.widget_media_square
                    large -> R.layout.widget_media_large
                    else -> R.layout.widget_media_compact
                }
            ).apply {
                BlurWidget.applyMediaSurfaceTint(this, prefs, id, fullyRounded = !large)
                setTextViewText(R.id.media_title, title)
                setTextViewText(R.id.media_artist, artist)
                setViewVisibility(R.id.media_artist, if (large) View.VISIBLE else View.GONE)
                bindAnimatedBackground(this, prefs, id, metadata, art)
                setImageViewResource(R.id.media_idle_icon, R.drawable.music_idle_bob)
                setViewVisibility(R.id.media_idle_icon, if (art == null) View.VISIBLE else View.GONE)
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

        private fun bindAnimatedBackground(
            views: RemoteViews,
            prefs: android.content.SharedPreferences,
            appWidgetId: Int,
            metadata: MediaMetadata?,
            art: Bitmap?
        ) {
            val trackKey = trackKey(metadata)
            val previousTrackKey = prefs.getString(backgroundTrackKey(appWidgetId), null)
            // RemoteViews starts with child zero on every update. Keep that child populated
            // so progress updates cannot replay a ViewFlipper transition or flash the surface.
            if (art == null) {
                views.setImageViewResource(R.id.media_art, 0)
                views.setImageViewResource(R.id.media_art_next, 0)
            } else {
                views.setImageViewBitmap(R.id.media_art, art)
                views.setImageViewBitmap(R.id.media_art_next, art)
                val previousArt = previousTrackKey?.let(backgroundArtworkCache::get)
                if (previousTrackKey != null && previousTrackKey != trackKey && previousArt != null) {
                    views.setImageViewBitmap(R.id.media_art, previousArt)
                    views.setDisplayedChild(R.id.media_background, 1)
                }
                backgroundArtworkCache.put(trackKey, art)
            }
            if (previousTrackKey != trackKey) {
                prefs.edit().putString(backgroundTrackKey(appWidgetId), trackKey).apply()
            }
        }

        private fun trackKey(metadata: MediaMetadata?): String = if (metadata == null) {
            "no-media"
        } else {
            listOf(
                metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),
                metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),
                duration(metadata).toString()
            ).joinToString("\u001F")
        }

        private fun progressBitmapSize(widgetWidthDp: Int, density: Float): Pair<Int, Int> {
            val progressWidthDp = (widgetWidthDp - MEDIA_PROGRESS_RESERVED_WIDTH_DP).coerceAtLeast(40)
            val ratio = progressWidthDp / MEDIA_PROGRESS_HEIGHT_DP
            var height = (MEDIA_PROGRESS_HEIGHT_DP * density).roundToInt().coerceIn(10, 48)
            var width = (height * ratio).roundToInt().coerceAtLeast(1)
            if (width > MAX_PROGRESS_BITMAP_WIDTH) {
                width = MAX_PROGRESS_BITMAP_WIDTH
                height = (width / ratio).roundToInt().coerceAtLeast(2)
            }
            return width to height
        }

        private fun backgroundTrackKey(appWidgetId: Int) = "media_background_track_$appWidgetId"
        private val backgroundArtworkCache = object : LruCache<String, Bitmap>(6) {}

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
