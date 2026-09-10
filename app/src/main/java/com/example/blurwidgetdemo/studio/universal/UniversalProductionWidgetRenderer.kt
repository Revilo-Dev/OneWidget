package dev.revilodev.onewidget.studio.universal

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.production.ProductionVariantResolver
import dev.revilodev.onewidget.studio.production.ProductionWidgetDesignRepository
import dev.revilodev.onewidget.studio.production.ProductionWidgetDesignResolver
import dev.revilodev.onewidget.studio.production.WidgetProductionDesignModeRepository
import dev.revilodev.onewidget.studio.rendering.WidgetRenderSize
import dev.revilodev.onewidget.widgets.WidgetAppearance

object UniversalProductionWidgetRenderer {
    fun buildIfPromoted(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        widgetId: String,
        content: UniversalWidgetContent,
        actions: UniversalWidgetActions = UniversalWidgetActions(),
        style: UniversalWidgetStyle = style(context, appWidgetId)
    ): RemoteViews? {
        val definition = UniversalWidgetStudioCatalog.definition(widgetId) ?: return null
        val renderSize = renderSize(manager, appWidgetId)
        val mode = WidgetProductionDesignModeRepository(context).mode(widgetId, appWidgetId)
        val promoted = when (val result = ProductionWidgetDesignRepository(context).loadActive(widgetId)) {
            is StudioRepositoryResult.Success -> result.value
            is StudioRepositoryResult.Failure -> null
        }
        val design = ProductionWidgetDesignResolver.resolve(
            mode = mode,
            promotedDesign = promoted,
            widgetId = widgetId,
            widthDp = renderSize.widthDp,
            heightDp = renderSize.heightDp
        ) ?: return null
        return UniversalRemoteViewsRenderer.build(
            context = context,
            definition = definition,
            style = style,
            content = content.copy(
                elements = content.elements.mapKeys { (elementId, _) ->
                    normalizedElementId(widgetId, elementId)
                }
            ),
            renderSize = renderSize,
            design = design,
            mode = UniversalRenderMode.PRODUCTION,
            actions = actions.copy(
                elements = actions.elements.mapKeys { (elementId, _) ->
                    normalizedElementId(widgetId, elementId)
                }
            )
        ).remoteViews
    }

    fun buildConfigurationSample(
        context: Context,
        providerClassName: String?,
        renderSize: WidgetRenderSize,
        style: UniversalWidgetStyle
    ): RemoteViews? {
        val definition = UniversalWidgetStudioCatalog.definitionForProvider(providerClassName) ?: return null
        val variant = ProductionVariantResolver.resolve(
            definition.widgetId,
            renderSize.widthDp,
            renderSize.heightDp
        ).variantId
        val design = WidgetDesignResolver.resolve(definition.defaultDesign(), variant).design ?: return null
        return UniversalRemoteViewsRenderer.build(
            context = context,
            definition = definition,
            style = style,
            content = UniversalRemoteViewsRenderer.sampleContent(definition),
            renderSize = renderSize,
            design = design,
            mode = UniversalRenderMode.CONFIG_PREVIEW
        ).remoteViews
    }

    fun initializeConfiguredWidget(context: Context, widgetId: String, appWidgetId: Int) {
        val promotedExists = when (val result = ProductionWidgetDesignRepository(context).loadActive(widgetId)) {
            is StudioRepositoryResult.Success -> result.value != null
            is StudioRepositoryResult.Failure -> false
        }
        WidgetProductionDesignModeRepository(context).initializeNewWidget(
            widgetId,
            appWidgetId,
            promotedExists
        )
    }

    fun deleteModes(context: Context, widgetId: String, appWidgetIds: IntArray) {
        val repository = WidgetProductionDesignModeRepository(context)
        appWidgetIds.forEach { repository.delete(widgetId, it) }
    }

    fun renderSize(manager: AppWidgetManager, appWidgetId: Int): WidgetRenderSize {
        val options = manager.getAppWidgetOptions(appWidgetId)
        return WidgetRenderSize(
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180).coerceAtLeast(1).toFloat(),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80).coerceAtLeast(1).toFloat()
        )
    }

    fun style(context: Context, appWidgetId: Int): UniversalWidgetStyle {
        val preferences = context.getSharedPreferences(BlurWidget.WIDGET_PREFS, Context.MODE_PRIVATE)
        return UniversalWidgetStyle(
            surfaceColor = BlurWidget.tintColor(preferences, appWidgetId),
            textColor = WidgetAppearance.textColor(preferences, appWidgetId),
            textScalePercent = WidgetAppearance.textScale(preferences, appWidgetId)
        )
    }

    private fun normalizedElementId(widgetId: String, elementId: String): String =
        if (elementId.startsWith("universal.")) {
            "$widgetId.${elementId.removePrefix("universal.")}"
        } else {
            elementId
        }
}
