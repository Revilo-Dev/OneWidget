package dev.revilodev.onewidget.widgets

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

object WidgetBackgroundStore {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun import(context: Context, source: Uri, complete: (String?) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val result = runCatching {
                val bitmap = WidgetSurface.loadImage(app, source.toString()) ?: return@runCatching null
                val directory = File(app.filesDir, "widget_backgrounds").apply { mkdirs() }
                val file = File(directory, UUID.randomUUID().toString() + ".png")
                try {
                    file.outputStream().use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
                    Uri.fromFile(file).toString()
                } catch (error: Exception) {
                    file.delete()
                    throw error
                }
            }.getOrNull()
            main.post { complete(result) }
        }
    }

    fun removeIfUnused(context: Context, uri: String?) {
        if (uri == null) return
        val prefs = context.getSharedPreferences(dev.revilodev.onewidget.BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        if (prefs.all.values.any { it == uri }) return
        runCatching {
            val parsed = Uri.parse(uri)
            if (parsed.scheme != "file") return
            val path = parsed.path ?: return
            val file = File(path).canonicalFile
            val directory = File(context.filesDir, "widget_backgrounds").canonicalFile
            if (file.parentFile == directory) file.delete()
        }
    }
}
