package dev.revilodev.onewidget.studio.production

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

enum class WidgetProductionDesignMode { LEGACY_DEFAULT, PROMOTED_DEFAULT }

interface ProductionDesignModeStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

@SuppressLint("UseKtx") // AndroidX Core KTX is deliberately excluded by this project.
class SharedPreferencesProductionDesignModeStore(
    private val preferences: SharedPreferences
) : ProductionDesignModeStore {
    override fun getString(key: String): String? = preferences.getString(key, null)
    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)
    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

/** Per-instance production geometry mode; deliberately separate from all Studio preferences. */
class WidgetProductionDesignModeRepository(private val store: ProductionDesignModeStore) {
    constructor(context: Context) : this(
        SharedPreferencesProductionDesignModeStore(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        )
    )

    fun mode(widgetId: String, appWidgetId: Int): WidgetProductionDesignMode =
        store.getString(modeKey(widgetId, appWidgetId))
            ?.let { value -> WidgetProductionDesignMode.entries.firstOrNull { it.name == value } }
            ?: WidgetProductionDesignMode.LEGACY_DEFAULT

    fun setMode(widgetId: String, appWidgetId: Int, mode: WidgetProductionDesignMode) {
        store.putString(modeKey(widgetId, appWidgetId), mode.name)
    }

    fun initializeNewWidget(widgetId: String, appWidgetId: Int, promotedDesignExists: Boolean): WidgetProductionDesignMode {
        val key = modeKey(widgetId, appWidgetId)
        store.getString(key)?.let { existing ->
            return WidgetProductionDesignMode.entries.firstOrNull { it.name == existing }
                ?: WidgetProductionDesignMode.LEGACY_DEFAULT
        }
        val initial = if (promotedDesignExists && usePromotedForNewWidgets(widgetId)) {
            WidgetProductionDesignMode.PROMOTED_DEFAULT
        } else {
            WidgetProductionDesignMode.LEGACY_DEFAULT
        }
        store.putString(key, initial.name)
        return initial
    }

    fun delete(widgetId: String, appWidgetId: Int) = store.remove(modeKey(widgetId, appWidgetId))

    fun usePromotedForNewWidgets(widgetId: String): Boolean =
        store.getBoolean(newWidgetKey(widgetId), false)

    fun setUsePromotedForNewWidgets(widgetId: String, enabled: Boolean) =
        store.putBoolean(newWidgetKey(widgetId), enabled)

    private fun modeKey(widgetId: String, appWidgetId: Int) = "mode_${widgetId}_$appWidgetId"
    private fun newWidgetKey(widgetId: String) = "new_widgets_$widgetId"

    companion object {
        const val PREFERENCES_NAME = "widget_production_design_modes"
    }
}
