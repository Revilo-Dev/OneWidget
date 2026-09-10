package dev.revilodev.onewidget.studio.data

import android.content.Context
import dev.revilodev.onewidget.studio.model.DesignDecodeErrorKind
import dev.revilodev.onewidget.studio.model.DesignDecodeResult
import dev.revilodev.onewidget.studio.model.WidgetDesignJsonCodec
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetDesignValidator
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import java.io.File
import java.io.IOException

/** App-private JSON storage for developer geometry; never writes to widget_prefs. */
class WidgetDesignRepository(
    private val designsDirectory: File,
    private val stateStore: StudioStateStore,
    private val codec: WidgetDesignJsonCodec = WidgetDesignJsonCodec()
) {
    constructor(context: Context) : this(
        File(context.filesDir, DESIGNS_DIRECTORY),
        SharedPreferencesStudioStateStore(context)
    )

    fun save(design: WidgetDesignSpec): StudioRepositoryResult<WidgetDesignSpec> {
        if (!safeStudioId(design.widgetId)) return invalidId(design.widgetId)
        val validation = WidgetDesignValidator.validate(design)
        if (!validation.isValid) {
            return StudioRepositoryResult.Failure(
                StudioRepositoryError(
                    StudioRepositoryErrorKind.INVALID_DESIGN,
                    validation.issues.joinToString("; ") { "${it.field}: ${it.message}" }
                )
            )
        }

        val existingRevision = when (val existing = load(design.widgetId)) {
            is StudioRepositoryResult.Success -> existing.value.revision
            is StudioRepositoryResult.Failure -> 0L
        }
        val savedDesign = design.copy(revision = maxOf(existingRevision, design.revision) + 1L)
        return when (val write = writeTextAtomically(designFile(design.widgetId), codec.encode(savedDesign))) {
            is StudioRepositoryResult.Success -> {
                stateStore.putString(KEY_SELECTED_WIDGET_ID, savedDesign.widgetId)
                stateStore.putLong(KEY_ACTIVE_DESIGN_REVISION, savedDesign.revision)
                StudioRepositoryResult.Success(savedDesign)
            }
            is StudioRepositoryResult.Failure -> write
        }
    }

    fun load(widgetId: String): StudioRepositoryResult<WidgetDesignSpec> {
        if (!safeStudioId(widgetId)) return invalidId(widgetId)
        val file = designFile(widgetId)
        if (!file.isFile) {
            return StudioRepositoryResult.Failure(
                StudioRepositoryError(StudioRepositoryErrorKind.NOT_FOUND, "No Studio design for $widgetId")
            )
        }
        val contents = try {
            file.readText(Charsets.UTF_8)
        } catch (error: IOException) {
            return StudioRepositoryResult.Failure(
                StudioRepositoryError(StudioRepositoryErrorKind.IO_ERROR, error.message ?: "Unable to read $widgetId")
            )
        }
        return when (val decoded = codec.decode(contents)) {
            is DesignDecodeResult.Success -> {
                val validation = WidgetDesignValidator.validate(decoded.design)
                if (validation.isValid) StudioRepositoryResult.Success(decoded.design)
                else StudioRepositoryResult.Failure(
                    StudioRepositoryError(
                        StudioRepositoryErrorKind.INVALID_DESIGN,
                        validation.issues.joinToString("; ") { "${it.field}: ${it.message}" }
                    )
                )
            }
            is DesignDecodeResult.Failure -> StudioRepositoryResult.Failure(
                StudioRepositoryError(
                    if (decoded.error.kind == DesignDecodeErrorKind.UNSUPPORTED_SCHEMA) {
                        StudioRepositoryErrorKind.UNSUPPORTED_SCHEMA
                    } else {
                        StudioRepositoryErrorKind.CORRUPT_DATA
                    },
                    decoded.error.message
                )
            )
        }
    }

    fun listAvailableDesigns(): StudioRepositoryResult<List<WidgetDesignSpec>> {
        if (!designsDirectory.exists()) return StudioRepositoryResult.Success(emptyList())
        val designs = designsDirectory.listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .sortedBy { it.name }
            .mapNotNull { file ->
                when (val result = load(file.nameWithoutExtension)) {
                    is StudioRepositoryResult.Success -> result.value
                    is StudioRepositoryResult.Failure -> null
                }
            }
        return StudioRepositoryResult.Success(designs)
    }

    fun delete(widgetId: String): StudioRepositoryResult<Boolean> {
        if (!safeStudioId(widgetId)) return invalidId(widgetId)
        val file = designFile(widgetId)
        if (!file.exists()) return StudioRepositoryResult.Success(false)
        return if (file.delete()) StudioRepositoryResult.Success(true)
        else StudioRepositoryResult.Failure(
            StudioRepositoryError(StudioRepositoryErrorKind.IO_ERROR, "Unable to delete Studio design for $widgetId")
        )
    }

    fun selectedWidgetId(): String? = stateStore.getString(KEY_SELECTED_WIDGET_ID)

    fun setSelectedWidgetId(widgetId: String?): StudioRepositoryResult<Unit> {
        if (widgetId != null && !safeStudioId(widgetId)) return invalidId(widgetId)
        stateStore.putString(KEY_SELECTED_WIDGET_ID, widgetId)
        return StudioRepositoryResult.Success(Unit)
    }

    fun selectedVariantId(): WidgetVariantId? =
        stateStore.getString(KEY_SELECTED_VARIANT_ID)?.let(WidgetVariantId::parse)

    fun setSelectedVariantId(variantId: WidgetVariantId?): StudioRepositoryResult<Unit> {
        if (variantId != null && !variantId.isValid()) {
            return StudioRepositoryResult.Failure(
                StudioRepositoryError(StudioRepositoryErrorKind.INVALID_ID, "Invalid variant ID: ${variantId.stableId}")
            )
        }
        stateStore.putString(KEY_SELECTED_VARIANT_ID, variantId?.stableId)
        return StudioRepositoryResult.Success(Unit)
    }

    fun activeDesignRevision(): Long = stateStore.getLong(KEY_ACTIVE_DESIGN_REVISION, 0L)

    private fun designFile(widgetId: String) = File(designsDirectory, "$widgetId.json")

    private fun <T> invalidId(id: String): StudioRepositoryResult<T> = StudioRepositoryResult.Failure(
        StudioRepositoryError(StudioRepositoryErrorKind.INVALID_ID, "Invalid Studio ID: $id")
    )

    companion object {
        const val DESIGNS_DIRECTORY = "widget_studio/designs"
        const val KEY_SELECTED_WIDGET_ID = "selected_widget_id"
        const val KEY_SELECTED_VARIANT_ID = "selected_variant_id"
        const val KEY_ACTIVE_DESIGN_REVISION = "active_design_revision"
    }
}
