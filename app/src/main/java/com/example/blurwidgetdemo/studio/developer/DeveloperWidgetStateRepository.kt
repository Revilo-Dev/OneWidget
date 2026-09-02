package com.example.blurwidgetdemo.studio.developer

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.example.blurwidgetdemo.studio.model.WidgetVariantId

data class ActiveDeveloperWidgetSpec(
    val widgetId: String,
    val variantId: WidgetVariantId,
    val designRevision: Long,
    val loadedAtEpochMillis: Long,
    val customWidthDp: Float? = null,
    val customHeightDp: Float? = null
)

data class DeveloperWidgetMeasurement(
    val appWidgetId: Int,
    val hostSize: DeveloperWidgetHostSize,
    val measuredAtEpochMillis: Long
)

sealed class DeveloperStateResult<out T> {
    data class Value<T>(val value: T) : DeveloperStateResult<T>()
    data object Missing : DeveloperStateResult<Nothing>()
    data class Corrupt(val reason: String) : DeveloperStateResult<Nothing>()
}

interface DeveloperWidgetStateStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

@SuppressLint("UseKtx") // AndroidX Core KTX is deliberately excluded by this project.
private class SharedPreferencesDeveloperWidgetStateStore(
    private val preferences: SharedPreferences
) : DeveloperWidgetStateStore {
    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

/** Developer-only persistence. This never reads or writes production widget_prefs. */
class DeveloperWidgetStateRepository(
    private val store: DeveloperWidgetStateStore
) {
    constructor(context: Context) : this(
        SharedPreferencesDeveloperWidgetStateStore(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        )
    )

    fun saveActive(spec: ActiveDeveloperWidgetSpec) {
        require(validSpec(spec)) { "Invalid active developer widget specification" }
        store.putString(KEY_ACTIVE_SPEC, encodeActive(spec))
    }

    fun loadActive(): DeveloperStateResult<ActiveDeveloperWidgetSpec> {
        val encoded = store.getString(KEY_ACTIVE_SPEC) ?: return DeveloperStateResult.Missing
        val parts = encoded.split(FIELD_SEPARATOR)
        if (parts.size != ACTIVE_FIELD_COUNT || parts[0] != ACTIVE_VERSION) {
            return DeveloperStateResult.Corrupt("Unsupported active developer widget state")
        }
        val variant = WidgetVariantId.parse(parts[2])
            ?: return DeveloperStateResult.Corrupt("Invalid active variant: ${parts[2]}")
        val revision = parts[3].toLongOrNull()
            ?: return DeveloperStateResult.Corrupt("Invalid active design revision")
        val loadedAt = parts[4].toLongOrNull()
            ?: return DeveloperStateResult.Corrupt("Invalid active load timestamp")
        val width = optionalFloat(parts[5])
            ?: return DeveloperStateResult.Corrupt("Invalid custom render width")
        val height = optionalFloat(parts[6])
            ?: return DeveloperStateResult.Corrupt("Invalid custom render height")
        val spec = ActiveDeveloperWidgetSpec(
            widgetId = parts[1],
            variantId = variant,
            designRevision = revision,
            loadedAtEpochMillis = loadedAt,
            customWidthDp = width.value,
            customHeightDp = height.value
        )
        return if (validSpec(spec)) {
            DeveloperStateResult.Value(spec)
        } else {
            DeveloperStateResult.Corrupt("Active developer widget values are invalid")
        }
    }

    fun clearActive() {
        store.remove(KEY_ACTIVE_SPEC)
    }

    fun saveMeasurement(measurement: DeveloperWidgetMeasurement) {
        require(measurement.appWidgetId > 0) { "appWidgetId must be positive" }
        require(measurement.measuredAtEpochMillis >= 0L) { "measuredAt must not be negative" }
        store.putString(measurementKey(measurement.appWidgetId), encodeMeasurement(measurement))
        val ids = measurementIds().toMutableSet().apply { add(measurement.appWidgetId) }
        store.putString(KEY_MEASUREMENT_IDS, ids.sorted().joinToString(","))
    }

    fun loadMeasurement(appWidgetId: Int): DeveloperStateResult<DeveloperWidgetMeasurement> {
        val encoded = store.getString(measurementKey(appWidgetId))
            ?: return DeveloperStateResult.Missing
        val parts = encoded.split(FIELD_SEPARATOR)
        if (parts.size != MEASUREMENT_FIELD_COUNT || parts[0] != MEASUREMENT_VERSION) {
            return DeveloperStateResult.Corrupt("Unsupported developer widget measurement")
        }
        val storedId = parts[1].toIntOrNull()
            ?: return DeveloperStateResult.Corrupt("Invalid measured widget ID")
        if (storedId <= 0 || storedId != appWidgetId) {
            return DeveloperStateResult.Corrupt("Measurement widget ID mismatch")
        }
        val minWidth = parts[2].toIntOrNull()
        val minHeight = parts[3].toIntOrNull()
        val maxWidth = parts[4].toIntOrNull()
        val maxHeight = parts[5].toIntOrNull()
        val preferredWidth = parts[6].toFloatOrNull()
        val preferredHeight = parts[7].toFloatOrNull()
        val measuredAt = parts[8].toLongOrNull()
        if (
            minWidth == null || minHeight == null || maxWidth == null || maxHeight == null ||
            preferredWidth == null || preferredHeight == null || measuredAt == null || measuredAt < 0L
        ) {
            return DeveloperStateResult.Corrupt("Invalid developer widget measurement")
        }
        val hostSize = DeveloperWidgetHostSize.fromPersisted(
            minWidthDp = minWidth,
            minHeightDp = minHeight,
            maxWidthDp = maxWidth,
            maxHeightDp = maxHeight,
            preferredWidthDp = preferredWidth,
            preferredHeightDp = preferredHeight
        )
        return DeveloperStateResult.Value(
            DeveloperWidgetMeasurement(
                appWidgetId = appWidgetId,
                hostSize = hostSize,
                measuredAtEpochMillis = measuredAt
            )
        )
    }

    fun mostRecentMeasurement(): DeveloperWidgetMeasurement? = measurementIds()
        .mapNotNull { id ->
            (loadMeasurement(id) as? DeveloperStateResult.Value)?.value
        }
        .maxByOrNull { it.measuredAtEpochMillis }

    private fun measurementIds(): Set<Int> = store.getString(KEY_MEASUREMENT_IDS)
        ?.split(',')
        ?.mapNotNull(String::toIntOrNull)
        ?.filter { it > 0 }
        ?.toSet()
        .orEmpty()

    private data class OptionalFloat(val value: Float?)

    private fun optionalFloat(encoded: String): OptionalFloat? = if (encoded.isEmpty()) {
        OptionalFloat(null)
    } else {
        encoded.toFloatOrNull()?.let(::OptionalFloat)
    }

    private fun validSpec(spec: ActiveDeveloperWidgetSpec): Boolean {
        val customValuesValid = when {
            spec.customWidthDp == null && spec.customHeightDp == null -> true
            spec.customWidthDp == null || spec.customHeightDp == null -> false
            else -> spec.customWidthDp.isFinite() && spec.customWidthDp > 0f &&
                spec.customHeightDp.isFinite() && spec.customHeightDp > 0f
        }
        return spec.widgetId.matches(SAFE_ID) &&
            spec.variantId.isValid() &&
            spec.designRevision >= 0L &&
            spec.loadedAtEpochMillis >= 0L &&
            customValuesValid
    }

    private fun encodeActive(spec: ActiveDeveloperWidgetSpec): String = listOf(
        ACTIVE_VERSION,
        spec.widgetId,
        spec.variantId.stableId,
        spec.designRevision,
        spec.loadedAtEpochMillis,
        spec.customWidthDp ?: "",
        spec.customHeightDp ?: ""
    ).joinToString(FIELD_SEPARATOR)

    private fun encodeMeasurement(measurement: DeveloperWidgetMeasurement): String = listOf(
        MEASUREMENT_VERSION,
        measurement.appWidgetId,
        measurement.hostSize.minWidthDp,
        measurement.hostSize.minHeightDp,
        measurement.hostSize.maxWidthDp,
        measurement.hostSize.maxHeightDp,
        measurement.hostSize.preferredWidthDp,
        measurement.hostSize.preferredHeightDp,
        measurement.measuredAtEpochMillis
    ).joinToString(FIELD_SEPARATOR)

    private fun measurementKey(appWidgetId: Int) = "measurement_$appWidgetId"

    companion object {
        const val PREFERENCES_NAME = "widget_studio_developer_widget"
        private const val KEY_ACTIVE_SPEC = "active_spec"
        private const val KEY_MEASUREMENT_IDS = "measurement_ids"
        private const val ACTIVE_VERSION = "active-v1"
        private const val MEASUREMENT_VERSION = "measurement-v2"
        private const val ACTIVE_FIELD_COUNT = 7
        private const val MEASUREMENT_FIELD_COUNT = 9
        private const val FIELD_SEPARATOR = "|"
        private val SAFE_ID = Regex("^[A-Za-z0-9._-]+$")
    }
}
