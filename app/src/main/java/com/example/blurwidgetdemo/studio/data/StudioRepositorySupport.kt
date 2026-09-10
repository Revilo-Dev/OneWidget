package dev.revilodev.onewidget.studio.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

enum class StudioRepositoryErrorKind {
    NOT_FOUND,
    INVALID_ID,
    INVALID_DESIGN,
    CORRUPT_DATA,
    UNSUPPORTED_SCHEMA,
    IO_ERROR
}

data class StudioRepositoryError(
    val kind: StudioRepositoryErrorKind,
    val message: String
)

sealed class StudioRepositoryResult<out T> {
    data class Success<T>(val value: T) : StudioRepositoryResult<T>()
    data class Failure(val error: StudioRepositoryError) : StudioRepositoryResult<Nothing>()
}

interface StudioStateStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String?)
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
}

@SuppressLint("UseKtx") // This project deliberately excludes AndroidX Core KTX.
class SharedPreferencesStudioStateStore(
    private val preferences: SharedPreferences
) : StudioStateStore {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String?) {
        preferences.edit().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        preferences.getLong(key, defaultValue)

    override fun putLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    companion object {
        const val PREFERENCES_NAME = "widget_studio_state"
    }
}

internal val SAFE_STUDIO_ID = Regex("^[A-Za-z0-9._-]+$")

internal fun safeStudioId(value: String): Boolean = value.isNotBlank() && SAFE_STUDIO_ID.matches(value)

internal fun writeTextAtomically(target: File, contents: String): StudioRepositoryResult<Unit> {
    return try {
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, ".${target.name}.tmp")
        temporary.writeText(contents, Charsets.UTF_8)
        try {
            Files.move(
                temporary.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        StudioRepositoryResult.Success(Unit)
    } catch (error: IOException) {
        StudioRepositoryResult.Failure(
            StudioRepositoryError(
                StudioRepositoryErrorKind.IO_ERROR,
                error.message ?: "Unable to write ${target.name}"
            )
        )
    } catch (error: SecurityException) {
        StudioRepositoryResult.Failure(
            StudioRepositoryError(
                StudioRepositoryErrorKind.IO_ERROR,
                error.message ?: "Unable to access ${target.name}"
            )
        )
    }
}
