package dev.revilodev.onewidget.widgets.extra

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper

class MediaNotificationListener : NotificationListenerService() {
    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private val handler = Handler(Looper.getMainLooper())
    private val progressTicker = object : Runnable {
        override fun run() {
            if (hasPlayingSession()) {
                MediaPlayerWidget.updateAll(this@MediaNotificationListener)
                handler.postDelayed(this, 1_000L)
            }
        }
    }
    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        syncControllerCallbacks(controllers.orEmpty())
        MediaPlayerWidget.updateAll(this)
        scheduleProgressRefresh()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val manager = getSystemService(MediaSessionManager::class.java)
        manager.addOnActiveSessionsChangedListener(sessionListener, listenerComponent)
        syncControllerCallbacks(manager.getActiveSessions(listenerComponent))
        MediaPlayerWidget.updateAll(this)
        scheduleProgressRefresh()
    }

    override fun onDestroy() {
        handler.removeCallbacks(progressTicker)
        getSystemService(MediaSessionManager::class.java).removeOnActiveSessionsChangedListener(sessionListener)
        controllerCallbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        controllerCallbacks.clear()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        MediaPlayerWidget.updateAll(this)
        scheduleProgressRefresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        MediaPlayerWidget.updateAll(this)
        scheduleProgressRefresh()
    }

    private fun syncControllerCallbacks(controllers: List<MediaController>) {
        controllerCallbacks.keys.filter { it !in controllers }.forEach { controller ->
            controllerCallbacks.remove(controller)?.let(controller::unregisterCallback)
        }
        controllers.filter { it !in controllerCallbacks }.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    MediaPlayerWidget.updateAll(this@MediaNotificationListener)
                    scheduleProgressRefresh()
                }
                override fun onMetadataChanged(metadata: android.media.MediaMetadata?) = MediaPlayerWidget.updateAll(this@MediaNotificationListener)
            }
            controller.registerCallback(callback)
            controllerCallbacks[controller] = callback
        }
    }

    private fun scheduleProgressRefresh() {
        handler.removeCallbacks(progressTicker)
        if (hasPlayingSession()) handler.post(progressTicker)
    }

    private fun hasPlayingSession(): Boolean = runCatching {
        getSystemService(MediaSessionManager::class.java).getActiveSessions(listenerComponent)
            .any { it.playbackState?.state == PlaybackState.STATE_PLAYING }
    }.getOrDefault(false)

    private val listenerComponent: ComponentName
        get() = ComponentName(this, MediaNotificationListener::class.java)
}
