package com.example.blurwidgetdemo.studio.developer

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

object DeveloperAccessCode {
    const val REQUIRED_LENGTH = 8
    private const val CURRENT_CODE = "41244110"

    fun hasValidFormat(input: String): Boolean =
        input.length == REQUIRED_LENGTH && input.all(Char::isDigit)

    fun isCorrect(input: String): Boolean = hasValidFormat(input) && input == CURRENT_CODE
}

interface DeveloperModeStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

@SuppressLint("UseKtx") // This project deliberately excludes AndroidX Core KTX.
private class SharedPreferencesDeveloperModeStore(
    private val preferences: SharedPreferences
) : DeveloperModeStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        preferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }
}

/** Developer-only state, intentionally separate from per-instance widget_prefs. */
class DeveloperModeRepository(private val store: DeveloperModeStore) {
    constructor(context: Context) : this(
        SharedPreferencesDeveloperModeStore(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        )
    )

    fun isEnabled(): Boolean = store.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        store.putBoolean(KEY_ENABLED, enabled)
    }

    companion object {
        const val PREFERENCES_NAME = "widget_studio_developer_mode"
        const val KEY_ENABLED = "developer_mode_enabled"
    }
}
