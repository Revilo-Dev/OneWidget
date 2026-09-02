package com.example.blurwidgetdemo.studio.verification

import com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor
import com.example.blurwidgetdemo.studio.clock.ClockStudioDescriptor
import com.example.blurwidgetdemo.studio.storage.StorageStudioDescriptor
import com.example.blurwidgetdemo.studio.calendar.CalendarStudioDescriptor
import com.example.blurwidgetdemo.studio.universal.UniversalWidgetStudioCatalog
import com.example.blurwidgetdemo.studio.model.WidgetVariantId

const val PARITY_VERIFICATION_SCHEMA_VERSION = 3

enum class WidgetVerificationStatus {
    NOT_TESTED,
    PARTIALLY_VERIFIED,
    VERIFIED,
    FAILED
}

data class WidgetParityVerification(
    val schemaVersion: Int = PARITY_VERIFICATION_SCHEMA_VERSION,
    val widgetId: String,
    val variantId: WidgetVariantId,
    val designRevision: Long,
    val deviceManufacturer: String = "",
    val deviceModel: String = "",
    val androidVersion: String = "",
    val androidSdkInt: Int = 0,
    val oneUiVersion: String = "",
    val launcherVersion: String = "",
    val actualPreviewVerified: Boolean = false,
    val developerWidgetVerified: Boolean = false,
    val nativeBlurVerified: Boolean = false,
    val geometryVerified: Boolean = false,
    val visibilityVerified: Boolean = false,
    val paddingVerified: Boolean = false,
    val alignmentVerified: Boolean = false,
    val radiusVerified: Boolean = false,
    val progressVerified: Boolean = false,
    val contentStatesVerified: Boolean = false,
    val systemUiHostVerified: Boolean = false,
    val snapshotVerified: Boolean = false,
    val failed: Boolean = false,
    val notes: String = "",
    val verifiedAt: Long = 0L
)

data class VerificationAssessment(
    val status: WidgetVerificationStatus,
    val missingRequirements: List<String>
)

object WidgetParityVerificationPolicy {
    fun assess(verification: WidgetParityVerification): VerificationAssessment {
        if (verification.failed) {
            return VerificationAssessment(WidgetVerificationStatus.FAILED, emptyList())
        }
        val requirements = linkedMapOf(
            "Developer Test Widget" to verification.developerWidgetVerified,
            "geometry" to verification.geometryVerified,
            "visibility" to verification.visibilityVerified,
            "padding" to verification.paddingVerified,
            "alignment" to verification.alignmentVerified,
            "snapshot" to verification.snapshotVerified
        )
        if (verification.widgetId in UniversalWidgetStudioCatalog.systemUiWidgetIds) {
            requirements["Samsung System UI lock/AOD host"] = verification.systemUiHostVerified
        } else {
            requirements["native One UI blur"] = verification.nativeBlurVerified
        }
        if (verification.widgetId == BatteryStudioDescriptor.WIDGET_ID ||
            verification.widgetId == StorageStudioDescriptor.WIDGET_ID ||
            verification.widgetId in UniversalWidgetStudioCatalog.progressWidgetIds
        ) {
            requirements["progress rendering"] = verification.progressVerified
            if (verification.androidSdkInt == 0 || verification.androidSdkInt >= 31) {
                requirements["progress corner radius"] = verification.radiusVerified
            }
        }
        if (verification.widgetId == CalendarStudioDescriptor.WIDGET_ID ||
            verification.widgetId in UniversalWidgetStudioCatalog.contentStateWidgetIds
        ) {
            requirements["runtime content states"] = verification.contentStatesVerified
        }
        val missing = requirements.filterValues { !it }.keys.toList()
        if (missing.isEmpty() && verification.widgetId in setOf(
                ClockStudioDescriptor.WIDGET_ID,
                BatteryStudioDescriptor.WIDGET_ID,
                StorageStudioDescriptor.WIDGET_ID,
                CalendarStudioDescriptor.WIDGET_ID
            ) + UniversalWidgetStudioCatalog.widgetIds
        ) {
            return VerificationAssessment(WidgetVerificationStatus.VERIFIED, emptyList())
        }
        val anyChecked = listOf(
            verification.actualPreviewVerified,
            verification.developerWidgetVerified,
            verification.nativeBlurVerified,
            verification.geometryVerified,
            verification.visibilityVerified,
            verification.paddingVerified,
            verification.alignmentVerified,
            verification.radiusVerified,
            verification.progressVerified,
            verification.contentStatesVerified,
            verification.systemUiHostVerified,
            verification.snapshotVerified
        ).any { it }
        return VerificationAssessment(
            if (anyChecked) WidgetVerificationStatus.PARTIALLY_VERIFIED
            else WidgetVerificationStatus.NOT_TESTED,
            missing
        )
    }
}

sealed class VerificationRevisionStatus {
    data class Current(
        val verification: WidgetParityVerification,
        val assessment: VerificationAssessment
    ) : VerificationRevisionStatus()

    data class Stale(
        val verification: WidgetParityVerification,
        val currentDesignRevision: Long
    ) : VerificationRevisionStatus()

    data object NotTested : VerificationRevisionStatus()
}
