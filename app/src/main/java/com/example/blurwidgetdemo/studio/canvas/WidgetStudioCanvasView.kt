package dev.revilodev.onewidget.studio.canvas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import dev.revilodev.onewidget.studio.editor.AnchorPositioning
import dev.revilodev.onewidget.studio.model.DimensionMode
import dev.revilodev.onewidget.studio.model.ResolvedWidgetDesign
import dev.revilodev.onewidget.studio.model.WidgetElementSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.registry.StudioWidgetDescriptor
import dev.revilodev.onewidget.studio.registry.StudioElementVisualType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@SuppressLint("UseKtx") // AndroidX Core KTX is deliberately excluded by this project.
class WidgetStudioCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var onElementSelected: ((String) -> Unit)? = null
    var onDragStarted: ((String) -> Unit)? = null
    var onDragPositionChanged: ((String, Float, Float) -> Unit)? = null
    var onDragFinished: (() -> Unit)? = null
    var onZoomChanged: ((Int) -> Unit)? = null

    var snapMode: StudioSnapMode = StudioSnapMode.ONE_DP
    var showHiddenElements: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val metrics = StudioPreviewMetrics()
    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 100, 181, 246)
        style = Paint.Style.STROKE
        strokeWidth = 1f
        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
    }

    private var descriptor: StudioWidgetDescriptor? = null
    private var design: ResolvedWidgetDesign? = null
    private var selectedElementId: String? = null
    private var canvasSize = StudioCanvasSize(152f, 72f)
    private var showApproximateGridGaps = true
    private var currentVariantId: WidgetVariantId? = null
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downTouchX = 0f
    private var downTouchY = 0f
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var draggingElementId: String? = null
    private var gestureMode = GestureMode.NONE
    private var activeGuides = emptySet<Guide>()

    private enum class GestureMode { NONE, ELEMENT, PAN }
    private enum class Guide { HORIZONTAL_CENTER, VERTICAL_CENTER, LEFT, RIGHT, TOP, BOTTOM }

    init {
        isClickable = true
        isFocusable = true
    }

    fun setEditorData(
        resolvedDesign: ResolvedWidgetDesign?,
        widgetDescriptor: StudioWidgetDescriptor,
        selectedElement: String?,
        requestedCanvasSize: StudioCanvasSize? = null,
        showGridGaps: Boolean = true
    ) {
        val variantChanged = currentVariantId != resolvedDesign?.variantId
        val nextCanvasSize = requestedCanvasSize
            ?: (resolvedDesign?.variantId as? WidgetVariantId.Standard)?.let(metrics::canvasSize)
            ?: StudioCanvasSize(152f, 72f)
        val canvasSizeChanged = nextCanvasSize != canvasSize
        descriptor = widgetDescriptor
        design = resolvedDesign
        selectedElementId = selectedElement
        currentVariantId = resolvedDesign?.variantId
        canvasSize = nextCanvasSize
        showApproximateGridGaps = showGridGaps
        if (variantChanged || canvasSizeChanged) post(::fitToView)
        invalidate()
    }

    fun canvasSizeDp(): StudioCanvasSize = canvasSize
    fun zoomPercent(): Int = (zoom * 100f).toInt()
    fun setZoomPercent(percent: Int) = setZoom(percent / 100f)

    fun zoomIn() = setZoom(zoom + ZOOM_STEP)
    fun zoomOut() = setZoom(zoom - ZOOM_STEP)
    fun actualSize() = setZoom(1f, resetPan = true)

    fun fitToView() {
        if (width <= 0 || height <= 0) return
        val availableWidth = max(1f, width - FIT_PADDING_PX * density)
        val availableHeight = max(1f, height - FIT_PADDING_PX * density)
        val fitted = min(
            availableWidth / (canvasSize.widthDp * density),
            availableHeight / (canvasSize.heightDp * density)
        )
        setZoom(fitted, resetPan = true)
    }

    private fun setZoom(value: Float, resetPan: Boolean = false) {
        zoom = value.coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (resetPan) {
            panX = 0f
            panY = 0f
        }
        onZoomChanged?.invoke(zoomPercent())
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        fillPaint.color = Color.rgb(38, 40, 45)
        canvas.drawColor(fillPaint.color)
        val scale = density * zoom
        val originX = (width - canvasSize.widthDp * scale) / 2f + panX
        val originY = (height - canvasSize.heightDp * scale) / 2f + panY

        canvas.save()
        canvas.translate(originX, originY)
        canvas.scale(scale, scale)
        drawCanvasSurface(canvas)
        val radius = min(WIDGET_CORNER_RADIUS_DP, min(canvasSize.widthDp, canvasSize.heightDp) / 2f)
        canvas.clipToWidgetBounds(
            RectF(0f, 0f, canvasSize.widthDp, canvasSize.heightDp),
            radius
        )
        drawGuides(canvas)
        drawElements(canvas)
        canvas.restore()
    }

    private fun drawCanvasSurface(canvas: Canvas) {
        val surface = RectF(0f, 0f, canvasSize.widthDp, canvasSize.heightDp)
        val radius = min(WIDGET_CORNER_RADIUS_DP, min(canvasSize.widthDp, canvasSize.heightDp) / 2f)
        fillPaint.color = Color.rgb(65, 68, 75)
        canvas.drawRoundRect(surface, radius, radius, fillPaint)
        canvas.save()
        canvas.clipToWidgetBounds(surface, radius)
        drawCellGaps(canvas)
        strokePaint.color = Color.rgb(190, 194, 201)
        strokePaint.strokeWidth = 1.5f / zoom
        strokePaint.pathEffect = null
        canvas.drawRoundRect(surface, radius, radius, strokePaint)
        canvas.restore()
    }

    private fun Canvas.clipToWidgetBounds(bounds: RectF, radius: Float) {
        val path = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }
        clipPath(path)
    }

    private fun drawCellGaps(canvas: Canvas) {
        if (!showApproximateGridGaps) return
        val variant = currentVariantId as? WidgetVariantId.Standard ?: return
        fillPaint.color = Color.argb(45, 0, 0, 0)
        repeat(variant.columns - 1) { index ->
            val left = (index + 1) * metrics.approximateCellWidthDp + index * metrics.cellGapDp
            canvas.drawRect(left, 0f, left + metrics.cellGapDp, canvasSize.heightDp, fillPaint)
        }
        repeat(variant.rows - 1) { index ->
            val top = (index + 1) * metrics.approximateCellHeightDp + index * metrics.cellGapDp
            canvas.drawRect(0f, top, canvasSize.widthDp, top + metrics.cellGapDp, fillPaint)
        }
    }

    private fun drawGuides(canvas: Canvas) {
        guidePaint.strokeWidth = 1f / zoom
        guidePaint.color = Color.argb(75, 100, 181, 246)
        canvas.drawLine(canvasSize.widthDp / 2f, 0f, canvasSize.widthDp / 2f, canvasSize.heightDp, guidePaint)
        canvas.drawLine(0f, canvasSize.heightDp / 2f, canvasSize.widthDp, canvasSize.heightDp / 2f, guidePaint)
        guidePaint.color = Color.rgb(255, 193, 7)
        activeGuides.forEach { guide ->
            when (guide) {
                Guide.HORIZONTAL_CENTER -> canvas.drawLine(
                    0f,
                    canvasSize.heightDp / 2f,
                    canvasSize.widthDp,
                    canvasSize.heightDp / 2f,
                    guidePaint
                )
                Guide.VERTICAL_CENTER -> canvas.drawLine(
                    canvasSize.widthDp / 2f,
                    0f,
                    canvasSize.widthDp / 2f,
                    canvasSize.heightDp,
                    guidePaint
                )
                Guide.LEFT -> canvas.drawLine(0f, 0f, 0f, canvasSize.heightDp, guidePaint)
                Guide.RIGHT -> canvas.drawLine(
                    canvasSize.widthDp,
                    0f,
                    canvasSize.widthDp,
                    canvasSize.heightDp,
                    guidePaint
                )
                Guide.TOP -> canvas.drawLine(0f, 0f, canvasSize.widthDp, 0f, guidePaint)
                Guide.BOTTOM -> canvas.drawLine(
                    0f,
                    canvasSize.heightDp,
                    canvasSize.widthDp,
                    canvasSize.heightDp,
                    guidePaint
                )
            }
        }
    }

    private fun drawElements(canvas: Canvas) {
        val resolved = design ?: return
        resolved.elements.sortedBy { it.zIndex }.forEach { element ->
            if (!element.visible && !showHiddenElements) return@forEach
            val bounds = elementBounds(element)
            val selected = element.elementId == selectedElementId
            fillPaint.color = when {
                !element.visible -> Color.argb(35, 255, 255, 255)
                selected -> Color.argb(145, 33, 150, 243)
                else -> Color.argb(90, 33, 150, 243)
            }
            canvas.drawRoundRect(bounds, 6f, 6f, fillPaint)
            strokePaint.color = when {
                selected -> Color.rgb(255, 193, 7)
                !element.visible -> Color.argb(120, 255, 255, 255)
                else -> Color.argb(190, 144, 202, 249)
            }
            strokePaint.strokeWidth = if (selected) 2.5f / zoom else 1f / zoom
            strokePaint.pathEffect = if (!element.visible) DashPathEffect(floatArrayOf(4f, 4f), 0f) else null
            canvas.drawRoundRect(bounds, 6f, 6f, strokePaint)
            if (element.visible) drawElementPreview(canvas, element, bounds)
        }
        strokePaint.pathEffect = null
    }

    private fun drawElementPreview(canvas: Canvas, element: WidgetElementSpec, bounds: RectF) {
        val elementDescriptor = descriptor?.elements?.firstOrNull { it.elementId == element.elementId }
        when (elementDescriptor?.visualType ?: StudioElementVisualType.TEXT) {
            StudioElementVisualType.TEXT -> {
                val text = elementDescriptor?.sampleText ?: elementDescriptor?.displayName ?: element.elementId
                textPaint.textSize = if ((elementDescriptor?.wrapHeightDp ?: 32f) >= 44f) 22f else 13f
                val baseline = bounds.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
                canvas.drawText(text, bounds.centerX(), baseline, textPaint)
            }
            StudioElementVisualType.ICON -> {
                textPaint.textSize = min(bounds.width(), bounds.height()) * 0.55f
                val baseline = bounds.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
                canvas.drawText("⚡", bounds.centerX(), baseline, textPaint)
            }
            StudioElementVisualType.PROGRESS -> {
                val radius = element.cornerRadiusDp.coerceAtMost(bounds.height() / 2f)
                fillPaint.color = Color.argb(70, 255, 255, 255)
                canvas.drawRoundRect(bounds, radius, radius, fillPaint)
                fillPaint.color = Color.rgb(49, 206, 138)
                val fill = RectF(bounds.left, bounds.top, bounds.left + bounds.width() * 0.72f, bounds.bottom)
                canvas.drawRoundRect(fill, min(radius, fill.width() / 2f), min(radius, fill.width() / 2f), fillPaint)
                elementDescriptor?.sampleText?.let { text ->
                    textPaint.textSize = min(12f, bounds.height() * 0.42f)
                    val baseline = bounds.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
                    canvas.save()
                    canvas.clipRect(bounds)
                    canvas.drawText(text, bounds.centerX(), baseline, textPaint)
                    canvas.restore()
                }
            }
            StudioElementVisualType.PANEL,
            StudioElementVisualType.SHAPE -> Unit
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                downTouchX = event.x
                downTouchY = event.y
                lastTouchX = event.x
                lastTouchY = event.y
                val point = viewToCanvas(event.x, event.y)
                val hit = hitElement(point.x, point.y)
                if (hit != null) {
                    gestureMode = GestureMode.ELEMENT
                    draggingElementId = hit.elementId
                    dragStartX = hit.xDp
                    dragStartY = hit.yDp
                    onElementSelected?.invoke(hit.elementId)
                    onDragStarted?.invoke(hit.elementId)
                } else {
                    gestureMode = GestureMode.PAN
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                if (gestureMode == GestureMode.PAN) {
                    panX += dx
                    panY += dy
                    invalidate()
                } else if (gestureMode == GestureMode.ELEMENT) {
                    val totalXDp = (event.x - downTouchX) / (density * zoom)
                    val totalYDp = (event.y - downTouchY) / (density * zoom)
                    val id = draggingElementId
                    val element = design?.elements?.firstOrNull { it.elementId == id }
                    if (id != null && element != null) {
                        val snappedX = snapMode.snap(dragStartX + totalXDp)
                        val snappedY = snapMode.snap(dragStartY + totalYDp)
                        val constrained = constrainPosition(element, snappedX, snappedY)
                        updateActiveGuides(element.copy(xDp = constrained.x, yDp = constrained.y))
                        onDragPositionChanged?.invoke(id, constrained.x, constrained.y)
                    }
                }
                lastTouchX = event.x
                lastTouchY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val moved = abs(event.x - downTouchX) > touchSlop || abs(event.y - downTouchY) > touchSlop
                if (gestureMode == GestureMode.ELEMENT) onDragFinished?.invoke()
                if (!moved) performClick()
                finishGesture()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (gestureMode == GestureMode.ELEMENT) onDragFinished?.invoke()
                finishGesture()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun finishGesture() {
        gestureMode = GestureMode.NONE
        draggingElementId = null
        activeGuides = emptySet()
        parent.requestDisallowInterceptTouchEvent(false)
        invalidate()
    }

    private fun viewToCanvas(x: Float, y: Float): StudioPoint {
        val scale = density * zoom
        val originX = (width - canvasSize.widthDp * scale) / 2f + panX
        val originY = (height - canvasSize.heightDp * scale) / 2f + panY
        return StudioPoint((x - originX) / scale, (y - originY) / scale)
    }

    private fun hitElement(xDp: Float, yDp: Float): WidgetElementSpec? = design?.elements
        ?.asSequence()
        ?.filter { it.visible || showHiddenElements }
        ?.sortedByDescending { it.zIndex }
        ?.firstOrNull { elementBounds(it).contains(xDp, yDp) }

    private fun elementBounds(element: WidgetElementSpec): RectF {
        val size = elementSize(element)
        val anchor = AnchorPositioning.anchorPoint(element.anchor, canvasSize.widthDp, canvasSize.heightDp)
        val centerX = anchor.x + element.xDp
        val centerY = anchor.y + element.yDp
        return RectF(
            centerX - size.widthDp / 2f,
            centerY - size.heightDp / 2f,
            centerX + size.widthDp / 2f,
            centerY + size.heightDp / 2f
        )
    }

    private fun elementSize(element: WidgetElementSpec): StudioCanvasSize {
        val elementDescriptor = descriptor?.elements?.firstOrNull { it.elementId == element.elementId }
        val sampleWidth = elementDescriptor?.wrapWidthDp ?: 64f
        val sampleHeight = elementDescriptor?.wrapHeightDp ?: 32f
        val width = when (element.widthMode) {
            DimensionMode.WRAP -> sampleWidth + element.paddingStartDp + element.paddingEndDp
            DimensionMode.MATCH -> max(24f, canvasSize.widthDp - element.paddingStartDp - element.paddingEndDp)
            DimensionMode.FIXED -> max(1f, element.widthDp)
        }
        val height = when (element.heightMode) {
            DimensionMode.WRAP -> sampleHeight + element.paddingTopDp + element.paddingBottomDp
            DimensionMode.MATCH -> max(24f, canvasSize.heightDp - element.paddingTopDp - element.paddingBottomDp)
            DimensionMode.FIXED -> max(1f, element.heightDp)
        }
        return StudioCanvasSize(width, height)
    }

    private fun constrainPosition(element: WidgetElementSpec, xDp: Float, yDp: Float): StudioPoint {
        val size = elementSize(element)
        val anchor = AnchorPositioning.anchorPoint(element.anchor, canvasSize.widthDp, canvasSize.heightDp)
        val minimumVisible = min(MINIMUM_VISIBLE_DP, min(size.widthDp, size.heightDp))
        val centerX = (anchor.x + xDp).coerceIn(
            minimumVisible - size.widthDp / 2f,
            canvasSize.widthDp - minimumVisible + size.widthDp / 2f
        )
        val centerY = (anchor.y + yDp).coerceIn(
            minimumVisible - size.heightDp / 2f,
            canvasSize.heightDp - minimumVisible + size.heightDp / 2f
        )
        return StudioPoint(centerX - anchor.x, centerY - anchor.y)
    }

    private fun updateActiveGuides(element: WidgetElementSpec) {
        val bounds = elementBounds(element)
        activeGuides = buildSet {
            if (abs(bounds.centerX() - canvasSize.widthDp / 2f) <= GUIDE_TOLERANCE_DP) {
                add(Guide.VERTICAL_CENTER)
            }
            if (abs(bounds.centerY() - canvasSize.heightDp / 2f) <= GUIDE_TOLERANCE_DP) {
                add(Guide.HORIZONTAL_CENTER)
            }
            if (abs(bounds.left) <= GUIDE_TOLERANCE_DP) add(Guide.LEFT)
            if (abs(bounds.right - canvasSize.widthDp) <= GUIDE_TOLERANCE_DP) add(Guide.RIGHT)
            if (abs(bounds.top) <= GUIDE_TOLERANCE_DP) add(Guide.TOP)
            if (abs(bounds.bottom - canvasSize.heightDp) <= GUIDE_TOLERANCE_DP) add(Guide.BOTTOM)
        }
        invalidate()
    }

    private data class StudioPoint(val x: Float, val y: Float)

    companion object {
        private const val MIN_ZOOM = 0.25f
        private const val MAX_ZOOM = 3f
        private const val ZOOM_STEP = 0.25f
        private const val FIT_PADDING_PX = 32f
        private const val MINIMUM_VISIBLE_DP = 16f
        private const val GUIDE_TOLERANCE_DP = 4f
        private const val WIDGET_CORNER_RADIUS_DP = 24f
    }
}
