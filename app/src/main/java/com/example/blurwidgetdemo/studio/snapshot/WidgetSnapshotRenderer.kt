package dev.revilodev.onewidget.studio.snapshot

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.registry.WidgetStudioRegistry
import dev.revilodev.onewidget.studio.rendering.WidgetRenderSize
import kotlin.math.roundToInt

data class SnapshotRenderRequest(
    val widgetId: String,
    val variantId: WidgetVariantId,
    val designRevision: Long,
    val widthDp: Float,
    val heightDp: Float,
    val density: Float
)

data class SnapshotPixelSize(
    val widthPx: Int,
    val heightPx: Int,
    val estimatedBytes: Long
)

sealed class SnapshotSizeValidation {
    data class Valid(val pixelSize: SnapshotPixelSize) : SnapshotSizeValidation()
    data class Invalid(val message: String) : SnapshotSizeValidation()
}

object SnapshotSizeValidator {
    const val MAX_BITMAP_BYTES = 64L * 1024L * 1024L
    const val MAX_PIXEL_DIMENSION = 8192

    fun validate(widthDp: Float, heightDp: Float, density: Float): SnapshotSizeValidation {
        if (!widthDp.isFinite() || widthDp <= 0f) {
            return SnapshotSizeValidation.Invalid("Snapshot width must be positive and finite")
        }
        if (!heightDp.isFinite() || heightDp <= 0f) {
            return SnapshotSizeValidation.Invalid("Snapshot height must be positive and finite")
        }
        if (!density.isFinite() || density <= 0f) {
            return SnapshotSizeValidation.Invalid("Snapshot density must be positive and finite")
        }
        val widthDouble = widthDp.toDouble() * density.toDouble()
        val heightDouble = heightDp.toDouble() * density.toDouble()
        if (
            !widthDouble.isFinite() || !heightDouble.isFinite() ||
            widthDouble > Int.MAX_VALUE || heightDouble > Int.MAX_VALUE
        ) {
            return SnapshotSizeValidation.Invalid("Snapshot pixel dimensions are too large")
        }
        val widthPx = widthDouble.roundToInt().coerceAtLeast(1)
        val heightPx = heightDouble.roundToInt().coerceAtLeast(1)
        if (widthPx > MAX_PIXEL_DIMENSION || heightPx > MAX_PIXEL_DIMENSION) {
            return SnapshotSizeValidation.Invalid(
                "Snapshot exceeds the ${MAX_PIXEL_DIMENSION}px per-side safety limit"
            )
        }
        val estimatedBytes = widthPx.toLong() * heightPx.toLong() * ARGB_BYTES_PER_PIXEL
        if (estimatedBytes > MAX_BITMAP_BYTES) {
            return SnapshotSizeValidation.Invalid(
                "Snapshot needs ${estimatedBytes / (1024L * 1024L)} MB; the safety limit is " +
                    "${MAX_BITMAP_BYTES / (1024L * 1024L)} MB"
            )
        }
        return SnapshotSizeValidation.Valid(
            SnapshotPixelSize(widthPx, heightPx, estimatedBytes)
        )
    }

    private const val ARGB_BYTES_PER_PIXEL = 4L
}

sealed class SnapshotCaptureResult {
    data class Success(
        val bitmap: Bitmap,
        val request: SnapshotRenderRequest,
        val pixelSize: SnapshotPixelSize,
        val rendererVersion: Int
    ) : SnapshotCaptureResult()
    data class Failure(val message: String) : SnapshotCaptureResult()
}

/** Applies the registered ACTUAL RemoteViews hierarchy and draws it at exact requested bounds. */
class WidgetSnapshotRenderer(
    private val registry: WidgetStudioRegistry,
    private val loadDesign: (String) -> StudioRepositoryResult<WidgetDesignSpec>
) {
    @SuppressLint("UseKtx") // This project deliberately excludes AndroidX Core KTX.
    fun capture(context: Context, request: SnapshotRenderRequest): SnapshotCaptureResult {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return SnapshotCaptureResult.Failure("RemoteViews capture must run on the main thread")
        }
        val pixelSize = when (
            val validation = SnapshotSizeValidator.validate(
                request.widthDp,
                request.heightDp,
                request.density
            )
        ) {
            is SnapshotSizeValidation.Valid -> validation.pixelSize
            is SnapshotSizeValidation.Invalid -> return SnapshotCaptureResult.Failure(validation.message)
        }
        val descriptor = registry.descriptor(request.widgetId)
            ?: return SnapshotCaptureResult.Failure("Missing widget descriptor: ${request.widgetId}")
        val renderer = descriptor.renderer
            ?: return SnapshotCaptureResult.Failure("No snapshot renderer: ${request.widgetId}")
        if (request.variantId !in descriptor.supportedVariants) {
            return SnapshotCaptureResult.Failure("Unsupported variant: ${request.variantId.stableId}")
        }
        val storedDesign = when (val loaded = loadDesign(request.widgetId)) {
            is StudioRepositoryResult.Success -> loaded.value
            is StudioRepositoryResult.Failure -> return SnapshotCaptureResult.Failure(
                "Unable to load saved design: ${loaded.error.message}"
            )
        }
        if (storedDesign.revision != request.designRevision) {
            return SnapshotCaptureResult.Failure(
                "Saved revision changed: expected ${request.designRevision}, found ${storedDesign.revision}"
            )
        }
        val resolution = WidgetDesignResolver.resolve(storedDesign, request.variantId)
        val design = resolution.design
            ?: return SnapshotCaptureResult.Failure(
                resolution.issues.firstOrNull()?.message ?: "Unable to resolve selected variant"
            )
        return try {
            val remoteViews = renderer.buildForSnapshot(
                context,
                design,
                WidgetRenderSize(request.widthDp, request.heightDp)
            )
            val parent = FrameLayout(context)
            val applied = remoteViews.apply(context, parent)
            parent.addView(
                applied,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            val widthSpec = View.MeasureSpec.makeMeasureSpec(
                pixelSize.widthPx,
                View.MeasureSpec.EXACTLY
            )
            val heightSpec = View.MeasureSpec.makeMeasureSpec(
                pixelSize.heightPx,
                View.MeasureSpec.EXACTLY
            )
            parent.measure(widthSpec, heightSpec)
            parent.layout(0, 0, pixelSize.widthPx, pixelSize.heightPx)
            val bitmap = Bitmap.createBitmap(
                pixelSize.widthPx,
                pixelSize.heightPx,
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(Color.TRANSPARENT)
            parent.draw(Canvas(bitmap))
            SnapshotCaptureResult.Success(
                bitmap = bitmap,
                request = request,
                pixelSize = pixelSize,
                rendererVersion = renderer.snapshotRendererVersion
            )
        } catch (error: OutOfMemoryError) {
            SnapshotCaptureResult.Failure("Not enough memory for this snapshot size")
        } catch (error: RuntimeException) {
            SnapshotCaptureResult.Failure(error.message ?: error.javaClass.simpleName)
        }
    }
}
