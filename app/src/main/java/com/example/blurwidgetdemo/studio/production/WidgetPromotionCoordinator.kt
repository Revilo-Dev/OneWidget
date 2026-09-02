package com.example.blurwidgetdemo.studio.production

import com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor
import com.example.blurwidgetdemo.studio.storage.StorageStudioDescriptor
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.model.WidgetDesignResolver
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetDesignValidator
import com.example.blurwidgetdemo.studio.model.ElementCapability
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.registry.StudioWidgetDescriptor
import com.example.blurwidgetdemo.studio.verification.WidgetVerificationStatus
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetStudioCatalog

enum class PromotionSnapshotStatus { CURRENT, MISSING, STALE }

data class PromotionReview(
    val widgetId: String,
    val revision: Long,
    val verificationStatus: WidgetVerificationStatus,
    val snapshotStatus: PromotionSnapshotStatus,
    val mapping: Map<ProductionSizeCategory, com.example.blurwidgetdemo.studio.model.WidgetVariantId.Standard>,
    val differences: List<WidgetDesignDifference>,
    val blockingErrors: List<String>,
    val compatibilityWarnings: List<String>
) {
    val requiresVerificationOverride: Boolean
        get() = verificationStatus != WidgetVerificationStatus.VERIFIED
    val requiresSnapshotOverride: Boolean
        get() = snapshotStatus != PromotionSnapshotStatus.CURRENT
}

data class PromotionRequest(
    val descriptor: StudioWidgetDescriptor,
    val design: WidgetDesignSpec,
    val verificationStatus: WidgetVerificationStatus,
    val snapshotStatus: PromotionSnapshotStatus,
    val previousProductionDesign: WidgetDesignSpec?,
    val androidSdkInt: Int
)

object WidgetPromotionReviewer {
    fun review(request: PromotionRequest): PromotionReview {
        val validation = WidgetDesignValidator.validate(
            request.design,
            request.descriptor.requiredElementIds
        )
        val mapping = ProductionVariantResolver.mappingFor(request.design.widgetId)
        val errors = mutableListOf<String>()
        errors += validation.issues.filter { it.severity == com.example.blurwidgetdemo.studio.model.ValidationSeverity.ERROR }
            .map { "${it.field}: ${it.message}" }
        if (request.design.revision <= 0L) errors += "The current design must be saved before promotion."
        if (request.design.widgetId != request.descriptor.widgetId) errors += "Descriptor and design widget IDs do not match."
        if (request.descriptor.renderer == null) errors += "No production-capable RemoteViews renderer is registered."
        if (mapping.isEmpty()) errors += "No production size mapping is registered."
        if (request.design.widgetId in UniversalWidgetStudioCatalog.systemUiWidgetIds) {
            errors += "Samsung System UI-hosted widgets require a dedicated lock/AOD promotion path."
        }
        val descriptorElements = request.descriptor.elements.associateBy { it.elementId }
        mapping.values.forEach { variant ->
            if (variant !in request.descriptor.supportedVariants) {
                errors += "Mapped variant ${variant.stableId} is not supported."
            } else {
                val resolved = WidgetDesignResolver.resolve(request.design, variant).design
                if (resolved == null) {
                    errors += "Mapped variant ${variant.stableId} cannot be resolved."
                } else {
                    resolved.elements.filter { it.elementId !in descriptorElements }.forEach { element ->
                        errors += "${variant.stableId}.${element.elementId} has no production renderer binding."
                    }
                }
            }
        }
        val warnings = mutableListOf<String>()
        if (request.androidSdkInt < 31) {
            warnings += "Dynamic RemoteViews geometry requires API 31+; production uses the documented XML-safe fallback."
        }
        mapping.values.forEach { variant ->
            WidgetDesignResolver.resolve(request.design, variant).design?.elements.orEmpty().forEach { element ->
                val supported = descriptorElements[element.elementId]?.capabilities.orEmpty()
                if (element.cornerRadiusDp > 0f && ElementCapability.CORNER_RADIUS !in supported) {
                    warnings += "${variant.stableId}.${element.elementId}.cornerRadius uses the XML fallback."
                }
                if (
                    ElementCapability.ALIGNMENT !in supported &&
                    (element.horizontalAlignment != HorizontalAlignment.START ||
                        element.verticalAlignment != VerticalAlignment.TOP)
                ) {
                    warnings += "${variant.stableId}.${element.elementId}.alignment uses the XML fallback."
                }
            }
        }
        if (request.design.widgetId == BatteryStudioDescriptor.WIDGET_ID && request.androidSdkInt < 31) {
            val usesRadius = mapping.values.any { variant ->
                WidgetDesignResolver.resolve(request.design, variant).design?.elements
                    ?.any { it.elementId == BatteryStudioDescriptor.PROGRESS && it.cornerRadiusDp > 0f } == true
            }
            if (usesRadius) {
                warnings += "Battery progress corner radius requires API 31+; the documented XML fallback is used here."
            }
        }
        if (request.design.widgetId == StorageStudioDescriptor.WIDGET_ID && request.androidSdkInt < 31) {
            val usesRadius = mapping.values.any { variant ->
                WidgetDesignResolver.resolve(request.design, variant).design?.elements
                    ?.any { it.elementId.startsWith("storage.") && it.cornerRadiusDp > 0f } == true
            }
            if (usesRadius) {
                warnings += "Storage progress corner radius requires API 31+; the documented XML fallback is used here."
            }
        }
        return PromotionReview(
            widgetId = request.design.widgetId,
            revision = request.design.revision,
            verificationStatus = request.verificationStatus,
            snapshotStatus = request.snapshotStatus,
            mapping = mapping,
            differences = WidgetDesignDiff.compare(
                request.previousProductionDesign,
                request.design,
                mapping.values
            ),
            blockingErrors = errors.distinct(),
            compatibilityWarnings = warnings.distinct()
        )
    }
}

sealed class PromotionResult {
    data class Success(val promoted: PromotedWidgetDesign) : PromotionResult()
    data class Rejected(val reasons: List<String>) : PromotionResult()
    data class Failed(val message: String) : PromotionResult()
}

class WidgetPromotionCoordinator(
    private val repository: ProductionWidgetDesignRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    fun promote(
        request: PromotionRequest,
        allowIncompleteVerification: Boolean,
        allowSnapshotWarning: Boolean
    ): PromotionResult {
        val review = WidgetPromotionReviewer.review(request)
        val reasons = review.blockingErrors.toMutableList()
        if (review.requiresVerificationOverride && !allowIncompleteVerification) {
            reasons += "Verification is ${review.verificationStatus}. Explicit override required."
        }
        if (review.requiresSnapshotOverride && !allowSnapshotWarning) {
            reasons += "Snapshot is ${review.snapshotStatus}. Explicit override required."
        }
        if (reasons.isNotEmpty()) return PromotionResult.Rejected(reasons)
        return when (
            val promoted = repository.promote(
                request.design,
                request.verificationStatus,
                currentTimeMillis()
            )
        ) {
            is StudioRepositoryResult.Success -> PromotionResult.Success(promoted.value)
            is StudioRepositoryResult.Failure -> PromotionResult.Failed(promoted.error.message)
        }
    }
}
