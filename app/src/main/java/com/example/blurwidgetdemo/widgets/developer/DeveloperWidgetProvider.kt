package com.example.blurwidgetdemo.widgets.developer

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.example.blurwidgetdemo.BlurWidget
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.data.WidgetDesignRepository
import com.example.blurwidgetdemo.studio.developer.DeveloperStateResult
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetHostSizeResolver
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetMeasurement
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetRenderResolution
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetRenderResolver
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetStateRepository
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetUpdateReason
import com.example.blurwidgetdemo.studio.registry.DefaultWidgetStudioRegistry

/** Generic, developer-only launcher host for the active Widget Studio design. */
class DeveloperWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val state = DeveloperWidgetStateRepository(context)
        appWidgetIds.forEach { appWidgetId ->
            val reason = if (state.loadMeasurement(appWidgetId) is DeveloperStateResult.Missing) {
                DeveloperWidgetUpdateReason.INITIAL
            } else {
                DeveloperWidgetUpdateReason.PROVIDER_UPDATE
            }
            updateWidget(context, appWidgetManager, appWidgetId, reason)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(
            context,
            appWidgetManager,
            appWidgetId,
            DeveloperWidgetUpdateReason.RESIZE,
            newOptions
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val reason = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> DeveloperWidgetUpdateReason.BOOT
            Intent.ACTION_MY_PACKAGE_REPLACED -> DeveloperWidgetUpdateReason.APP_UPDATE
            else -> null
        }
        if (reason != null) updateAll(context, reason)
    }

    companion object {
        private const val LOG_TAG = "DeveloperWidgetDebug"

        fun appWidgetIds(context: Context): IntArray = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, DeveloperWidgetProvider::class.java))

        fun updateAll(context: Context, reason: DeveloperWidgetUpdateReason): Int {
            val manager = AppWidgetManager.getInstance(context)
            val ids = appWidgetIds(context)
            ids.forEach { updateWidget(context, manager, it, reason) }
            return ids.size
        }

        fun updateWidget(
            context: Context,
            appWidgetId: Int,
            reason: DeveloperWidgetUpdateReason
        ) {
            updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId, reason)
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            reason: DeveloperWidgetUpdateReason,
            optionsOverride: Bundle? = null
        ) {
            val options = optionsOverride ?: manager.getAppWidgetOptions(appWidgetId)
            val hostRead = DeveloperWidgetHostSizeResolver.fromOptions(
                options,
                context.resources.configuration.orientation
            )
            val stateRepository = DeveloperWidgetStateRepository(context)
            if (hostRead.launcherValuesPresent) {
                stateRepository.saveMeasurement(
                    DeveloperWidgetMeasurement(
                        appWidgetId = appWidgetId,
                        hostSize = hostRead.size,
                        measuredAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
            val resolver = DeveloperWidgetRenderResolver(
                registry = DefaultWidgetStudioRegistry.create(),
                loadDesign = WidgetDesignRepository(context)::load
            )
            val resolution = resolver.resolve(stateRepository.loadActive(), hostRead.size)
            val views = when (resolution) {
                DeveloperWidgetRenderResolution.NoActiveDesign -> placeholder(
                    context,
                    context.getString(R.string.developer_widget_placeholder_title),
                    context.getString(R.string.developer_widget_placeholder_message)
                )
                is DeveloperWidgetRenderResolution.Error -> placeholder(
                    context,
                    context.getString(R.string.developer_widget_error_title),
                    resolution.message
                )
                is DeveloperWidgetRenderResolution.Ready -> runCatching {
                    requireNotNull(resolution.descriptor.renderer).buildForDeveloperWidget(
                        context = context,
                        design = resolution.design,
                        renderSize = resolution.renderSize,
                        appWidgetId = appWidgetId
                    )
                }.getOrElse { error ->
                    placeholder(
                        context,
                        context.getString(R.string.developer_widget_error_title),
                        error.message ?: error.javaClass.simpleName
                    )
                }
            }
            manager.updateAppWidget(appWidgetId, views)
            logUpdate(context, appWidgetId, resolution, hostRead.size, reason)
        }

        private fun placeholder(context: Context, title: String, message: String): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_developer_placeholder).apply {
                BlurWidget.applySurfaceColor(
                    this,
                    BlurWidget.tintColor(
                        BlurWidget.DEFAULT_TINT_HUE,
                        BlurWidget.DEFAULT_TINT_SATURATION,
                        BlurWidget.DEFAULT_TINT_VALUE,
                        BlurWidget.DEFAULT_TINT_ALPHA
                    )
                )
                setTextViewText(R.id.developer_widget_title, title)
                setTextViewText(R.id.developer_widget_message, message)
            }

        private fun logUpdate(
            context: Context,
            appWidgetId: Int,
            resolution: DeveloperWidgetRenderResolution,
            hostSize: com.example.blurwidgetdemo.studio.developer.DeveloperWidgetHostSize,
            reason: DeveloperWidgetUpdateReason
        ) {
            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
            val active = (resolution as? DeveloperWidgetRenderResolution.Ready)?.activeSpec
            Log.d(
                LOG_TAG,
                "appWidgetId=$appWidgetId widget=${active?.widgetId ?: "none"} " +
                    "variant=${active?.variantId?.stableId ?: "none"} " +
                    "revision=${active?.designRevision ?: 0L} " +
                    "host=${hostSize.preferredWidthDp}x${hostSize.preferredHeightDp} reason=$reason"
            )
        }
    }
}
