package dev.revilodev.onewidget.studio.universal

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import dev.revilodev.onewidget.BlurWidget
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.studio.editor.AnchorPositioning
import dev.revilodev.onewidget.studio.model.DimensionMode
import dev.revilodev.onewidget.studio.model.HorizontalAlignment
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.model.VerticalAlignment
import dev.revilodev.onewidget.studio.model.WidgetElementSpec
import dev.revilodev.onewidget.studio.registry.StudioElementVisualType
import dev.revilodev.onewidget.studio.rendering.RemoteViewsGeometryCompat
import dev.revilodev.onewidget.studio.rendering.StudioPreviewRenderResult
import dev.revilodev.onewidget.studio.rendering.StudioRemoteViewsRenderer
import dev.revilodev.onewidget.studio.rendering.WidgetRenderSize

enum class UniversalRenderMode {
    CONFIG_PREVIEW,
    STUDIO_ACTUAL_PREVIEW,
    DEVELOPER_WIDGET,
    SNAPSHOT,
    PRODUCTION
}

data class UniversalWidgetStyle(
    val surfaceColor: Int,
    val textColor: Int,
    val textScalePercent: Int = 100
)

data class UniversalElementContent(
    val text: CharSequence? = null,
    val imageResId: Int? = null,
    val bitmap: Bitmap? = null,
    val visible: Boolean = true,
    val imageAlpha: Int = 255
)

data class UniversalWidgetContent(
    val elements: Map<String, UniversalElementContent>
)

data class UniversalWidgetActions(
    val root: PendingIntent? = null,
    val elements: Map<String, PendingIntent> = emptyMap()
)

data class UniversalElementBinding(
    val wrapperId: Int,
    val textId: Int,
    val imageId: Int
)

data class UniversalElementGeometry(
    val elementId: String,
    val binding: UniversalElementBinding,
    val visible: Boolean,
    val widthDp: Float,
    val heightDp: Float,
    val startMarginDp: Float,
    val topMarginDp: Float,
    val paddingPx: IntArray,
    val gravity: Int,
    val cornerRadiusDp: Float,
    val dynamicGeometrySupported: Boolean
)

data class UniversalRenderPlan(
    val mode: UniversalRenderMode,
    val elements: List<UniversalElementGeometry>,
    val diagnostics: List<String>,
    val installActions: Boolean
)

data class UniversalRemoteViewsRenderResult(
    val remoteViews: RemoteViews,
    val plan: UniversalRenderPlan
)

object UniversalRemoteViewsGeometry {
    const val SLOT_COUNT = 8

    private val bindings = listOf(
        UniversalElementBinding(R.id.universal_slot_1, R.id.universal_text_1, R.id.universal_image_1),
        UniversalElementBinding(R.id.universal_slot_2, R.id.universal_text_2, R.id.universal_image_2),
        UniversalElementBinding(R.id.universal_slot_3, R.id.universal_text_3, R.id.universal_image_3),
        UniversalElementBinding(R.id.universal_slot_4, R.id.universal_text_4, R.id.universal_image_4),
        UniversalElementBinding(R.id.universal_slot_5, R.id.universal_text_5, R.id.universal_image_5),
        UniversalElementBinding(R.id.universal_slot_6, R.id.universal_text_6, R.id.universal_image_6),
        UniversalElementBinding(R.id.universal_slot_7, R.id.universal_text_7, R.id.universal_image_7),
        UniversalElementBinding(R.id.universal_slot_8, R.id.universal_text_8, R.id.universal_image_8)
    )

    fun plan(
        definition: UniversalWidgetDefinition,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        density: Float,
        apiLevel: Int
    ): Pair<List<UniversalElementGeometry>, List<String>> {
        val diagnostics = mutableListOf<String>()
        val definitions = definition.elements.associateBy { it.elementId }
        val elements = design.elements.mapNotNull { element ->
            val descriptor = definitions[element.elementId]
            if (descriptor == null) {
                diagnostics += "${element.elementId}.mapping: no universal definition"
                return@mapNotNull null
            }
            val index = definition.elements.indexOfFirst { it.elementId == element.elementId }
            val binding = bindings.getOrNull(index)
            if (binding == null) {
                diagnostics += "${element.elementId}.mapping: no universal slot"
                return@mapNotNull null
            }
            val width = resolveDimension(element.widthMode, element.widthDp, renderSize.widthDp, descriptor.widthDp)
            val height = resolveDimension(element.heightMode, element.heightDp, renderSize.heightDp, descriptor.heightDp)
            val anchor = AnchorPositioning.anchorPoint(element.anchor, renderSize.widthDp, renderSize.heightDp)
            val supported = apiLevel >= RemoteViewsGeometryCompat.MIN_DYNAMIC_GEOMETRY_API
            if (!supported) diagnostics += "${element.elementId}.geometry: XML-safe fallback below API 31"
            UniversalElementGeometry(
                elementId = element.elementId,
                binding = binding,
                visible = element.visible,
                widthDp = width,
                heightDp = height,
                startMarginDp = anchor.x + element.xDp - width / 2f - (renderSize.widthDp - width) / 2f,
                topMarginDp = anchor.y + element.yDp - height / 2f - (renderSize.heightDp - height) / 2f,
                paddingPx = intArrayOf(
                    (element.paddingStartDp * density).toInt(),
                    (element.paddingTopDp * density).toInt(),
                    (element.paddingEndDp * density).toInt(),
                    (element.paddingBottomDp * density).toInt()
                ),
                gravity = element.gravity(),
                cornerRadiusDp = element.cornerRadiusDp,
                dynamicGeometrySupported = supported
            )
        }
        return elements to diagnostics
    }

    fun apply(views: RemoteViews, elements: List<UniversalElementGeometry>) {
        elements.forEach { element ->
            views.setViewVisibility(element.binding.wrapperId, if (element.visible) View.VISIBLE else View.GONE)
            views.setViewPadding(
                element.binding.wrapperId,
                element.paddingPx[0],
                element.paddingPx[1],
                element.paddingPx[2],
                element.paddingPx[3]
            )
            views.setInt(element.binding.textId, "setGravity", element.gravity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && element.dynamicGeometrySupported) {
                RemoteViewsGeometryCompat.applySizeAndPosition(
                    views,
                    element.binding.wrapperId,
                    element.widthDp,
                    element.heightDp,
                    element.startMarginDp,
                    element.topMarginDp
                )
                RemoteViewsGeometryCompat.applyOutlineRadius(
                    views,
                    element.binding.wrapperId,
                    element.cornerRadiusDp
                )
            }
        }
    }

    private fun resolveDimension(mode: DimensionMode, fixed: Float, canvas: Float, wrap: Float): Float =
        when (mode) {
            DimensionMode.WRAP -> wrap
            DimensionMode.MATCH -> canvas
            DimensionMode.FIXED -> fixed
        }.coerceAtLeast(1f)

    private fun WidgetElementSpec.gravity(): Int {
        val horizontal = when (horizontalAlignment) {
            HorizontalAlignment.START -> Gravity.START
            HorizontalAlignment.CENTER -> Gravity.CENTER_HORIZONTAL
            HorizontalAlignment.END -> Gravity.END
        }
        val vertical = when (verticalAlignment) {
            VerticalAlignment.TOP -> Gravity.TOP
            VerticalAlignment.CENTER -> Gravity.CENTER_VERTICAL
            VerticalAlignment.BOTTOM -> Gravity.BOTTOM
        }
        return horizontal or vertical
    }
}

object UniversalRemoteViewsRenderer {
    fun plan(
        definition: UniversalWidgetDefinition,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        mode: UniversalRenderMode,
        density: Float = 1f,
        apiLevel: Int = Build.VERSION.SDK_INT
    ): UniversalRenderPlan {
        val (elements, geometryDiagnostics) = UniversalRemoteViewsGeometry.plan(
            definition,
            design,
            renderSize,
            density,
            apiLevel
        )
        return UniversalRenderPlan(
            mode = mode,
            elements = elements,
            diagnostics = buildList {
                if (design.widgetId != definition.widgetId) {
                    add("widget-id: expected ${definition.widgetId}, received ${design.widgetId}")
                }
                addAll(geometryDiagnostics)
                if (definition.systemUiHosted) {
                    add("host: Samsung System UI verification required")
                }
            },
            installActions = mode == UniversalRenderMode.PRODUCTION
        )
    }

    fun build(
        context: Context,
        definition: UniversalWidgetDefinition,
        style: UniversalWidgetStyle,
        content: UniversalWidgetContent,
        renderSize: WidgetRenderSize,
        design: ResolvedWidgetDesign,
        mode: UniversalRenderMode,
        actions: UniversalWidgetActions = UniversalWidgetActions()
    ): UniversalRemoteViewsRenderResult {
        require(design.widgetId == definition.widgetId) {
            "Universal renderer for ${definition.widgetId} received ${design.widgetId}"
        }
        val plan = plan(
            definition,
            design,
            renderSize,
            mode,
            context.resources.displayMetrics.density
        )
        val views = RemoteViews(context.packageName, R.layout.widget_studio_universal)
        BlurWidget.applySurfaceColor(views, style.surfaceColor)
        val definitions = definition.elements.associateBy { it.elementId }
        UniversalRemoteViewsGeometry.apply(views, plan.elements)
        plan.elements.forEach { geometry ->
            val element = requireNotNull(definitions[geometry.elementId])
            val value = content.elements[geometry.elementId] ?: element.sampleContent()
            val visible = geometry.visible && value.visible
            views.setViewVisibility(geometry.binding.wrapperId, if (visible) View.VISIBLE else View.GONE)
            if (value.text != null) {
                views.setViewVisibility(geometry.binding.textId, View.VISIBLE)
                views.setViewVisibility(geometry.binding.imageId, View.GONE)
                views.setTextViewText(geometry.binding.textId, value.text)
                views.setTextColor(geometry.binding.textId, style.textColor)
                views.setFloat(
                    geometry.binding.textId,
                    "setTextSize",
                    element.textSizeSp * style.textScalePercent.coerceIn(50, 200) / 100f
                )
            } else {
                views.setViewVisibility(geometry.binding.textId, View.GONE)
                views.setViewVisibility(geometry.binding.imageId, View.VISIBLE)
                when {
                    value.bitmap != null -> views.setImageViewBitmap(geometry.binding.imageId, value.bitmap)
                    value.imageResId != null -> views.setImageViewResource(geometry.binding.imageId, value.imageResId)
                    else -> views.setImageViewBitmap(
                        geometry.binding.imageId,
                        placeholderBitmap(element, geometry, style)
                    )
                }
                views.setInt(geometry.binding.imageId, "setImageAlpha", value.imageAlpha.coerceIn(0, 255))
            }
        }
        if (plan.installActions) {
            views.setOnClickPendingIntent(android.R.id.background, actions.root)
            plan.elements.forEach { geometry ->
                views.setOnClickPendingIntent(
                    geometry.binding.wrapperId,
                    actions.elements[geometry.elementId]
                )
            }
        } else {
            views.setOnClickPendingIntent(android.R.id.background, null)
            plan.elements.forEach { geometry ->
                views.setOnClickPendingIntent(geometry.binding.wrapperId, null)
            }
        }
        return UniversalRemoteViewsRenderResult(views, plan)
    }

    fun sampleContent(definition: UniversalWidgetDefinition): UniversalWidgetContent =
        UniversalWidgetContent(definition.elements.associate { it.elementId to it.sampleContent() })

    private fun UniversalElementDefinition.sampleContent(): UniversalElementContent =
        UniversalElementContent(text = sampleText, imageResId = sampleImageResId)

    private fun placeholderBitmap(
        definition: UniversalElementDefinition,
        geometry: UniversalElementGeometry,
        style: UniversalWidgetStyle
    ): Bitmap {
        val width = geometry.widthDp.toInt().coerceIn(1, 512)
        val height = geometry.heightDp.toInt().coerceIn(1, 512)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radius = definition.cornerRadiusDp.coerceAtMost(height / 2f)
            if (definition.visualType == StudioElementVisualType.PROGRESS) {
                paint.color = Color.argb(70, 255, 255, 255)
                canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
                paint.color = Color.rgb(49, 206, 138)
                canvas.drawRoundRect(0f, 0f, width * 0.64f, height.toFloat(), radius, radius, paint)
            } else {
                paint.color = Color.argb(48, Color.red(style.textColor), Color.green(style.textColor), Color.blue(style.textColor))
                canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
            }
        }
    }
}

class UniversalStudioRemoteViewsRenderer(
    private val definition: UniversalWidgetDefinition
) : StudioRemoteViewsRenderer {
    override fun buildForDeveloperWidget(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        appWidgetId: Int
    ): RemoteViews = build(context, design, renderSize, UniversalRenderMode.DEVELOPER_WIDGET).remoteViews

    override fun buildForStudioPreview(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): StudioPreviewRenderResult {
        val rendered = build(context, design, renderSize, UniversalRenderMode.STUDIO_ACTUAL_PREVIEW)
        return StudioPreviewRenderResult(
            rendered.remoteViews,
            buildList {
                add("widget=${definition.widgetId}")
                add("geometry=${rendered.plan.elements.size}")
                addAll(rendered.plan.diagnostics)
            }
        )
    }

    override fun buildForSnapshot(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize
    ): RemoteViews = build(context, design, renderSize, UniversalRenderMode.SNAPSHOT).remoteViews

    private fun build(
        context: Context,
        design: ResolvedWidgetDesign,
        renderSize: WidgetRenderSize,
        mode: UniversalRenderMode
    ) = UniversalRemoteViewsRenderer.build(
        context = context,
        definition = definition,
        style = UniversalWidgetStyle(
            surfaceColor = BlurWidget.tintColor(
                BlurWidget.DEFAULT_TINT_HUE,
                BlurWidget.DEFAULT_TINT_SATURATION,
                BlurWidget.DEFAULT_TINT_VALUE,
                BlurWidget.DEFAULT_TINT_ALPHA
            ),
            textColor = Color.WHITE
        ),
        content = UniversalRemoteViewsRenderer.sampleContent(definition),
        renderSize = renderSize,
        design = design,
        mode = mode
    )
}
