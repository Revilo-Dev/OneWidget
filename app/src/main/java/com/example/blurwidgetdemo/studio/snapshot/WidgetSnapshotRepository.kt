package com.example.blurwidgetdemo.studio.snapshot

import android.content.Context
import com.example.blurwidgetdemo.studio.data.StudioRepositoryError
import com.example.blurwidgetdemo.studio.data.StudioRepositoryErrorKind
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.data.safeStudioId
import com.example.blurwidgetdemo.studio.data.writeTextAtomically
import com.example.blurwidgetdemo.studio.json.JsonParseException
import com.example.blurwidgetdemo.studio.json.JsonValueException
import com.example.blurwidgetdemo.studio.json.SimpleJson
import com.example.blurwidgetdemo.studio.json.asFloat
import com.example.blurwidgetdemo.studio.json.asInt
import com.example.blurwidgetdemo.studio.json.asLong
import com.example.blurwidgetdemo.studio.json.asObject
import com.example.blurwidgetdemo.studio.json.asString
import com.example.blurwidgetdemo.studio.json.jsonNumber
import com.example.blurwidgetdemo.studio.json.jsonObject
import com.example.blurwidgetdemo.studio.json.jsonString
import com.example.blurwidgetdemo.studio.json.required
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import java.io.File
import java.io.IOException

data class WidgetSnapshotMetadata(
    val widgetId: String,
    val variantId: WidgetVariantId,
    val widthDp: Float,
    val heightDp: Float,
    val density: Float,
    val designRevision: Long,
    val createdAt: Long,
    val snapshotPath: String
)

sealed class SnapshotLookupResult {
    data class Available(val metadata: WidgetSnapshotMetadata, val file: File) : SnapshotLookupResult()
    data class MissingFile(val metadata: WidgetSnapshotMetadata) : SnapshotLookupResult()
    data class CorruptMetadata(val message: String) : SnapshotLookupResult()
    data object NotFound : SnapshotLookupResult()
}

/** Metadata-only snapshot foundation. PNG creation is intentionally deferred to a later phase. */
class WidgetSnapshotRepository(private val metadataDirectory: File) {
    constructor(context: Context) : this(File(context.filesDir, METADATA_DIRECTORY))

    fun register(metadata: WidgetSnapshotMetadata): StudioRepositoryResult<WidgetSnapshotMetadata> {
        validationError(metadata)?.let { message ->
            return StudioRepositoryResult.Failure(
                StudioRepositoryError(StudioRepositoryErrorKind.INVALID_DESIGN, message)
            )
        }
        return when (val write = writeTextAtomically(metadataFile(metadata.widgetId, metadata.variantId), encode(metadata))) {
            is StudioRepositoryResult.Success -> StudioRepositoryResult.Success(metadata)
            is StudioRepositoryResult.Failure -> write
        }
    }

    fun findLatest(widgetId: String, variantId: WidgetVariantId): SnapshotLookupResult {
        if (!safeStudioId(widgetId) || !variantId.isValid()) return SnapshotLookupResult.NotFound
        val metadataFile = metadataFile(widgetId, variantId)
        if (!metadataFile.isFile) return SnapshotLookupResult.NotFound
        val metadata = try {
            decode(metadataFile.readText(Charsets.UTF_8))
        } catch (error: IOException) {
            return SnapshotLookupResult.CorruptMetadata(error.message ?: "Unable to read snapshot metadata")
        } catch (error: JsonParseException) {
            return SnapshotLookupResult.CorruptMetadata(error.message ?: "Corrupt snapshot metadata")
        } catch (error: JsonValueException) {
            return SnapshotLookupResult.CorruptMetadata(error.message ?: "Corrupt snapshot metadata")
        }
        val image = resolveSnapshotFile(metadata.snapshotPath)
        return if (image.isFile) SnapshotLookupResult.Available(metadata, image)
        else SnapshotLookupResult.MissingFile(metadata)
    }

    fun removeMetadata(widgetId: String, variantId: WidgetVariantId): StudioRepositoryResult<Boolean> {
        if (!safeStudioId(widgetId) || !variantId.isValid()) {
            return StudioRepositoryResult.Failure(
                StudioRepositoryError(StudioRepositoryErrorKind.INVALID_ID, "Invalid snapshot identity")
            )
        }
        val file = metadataFile(widgetId, variantId)
        if (!file.exists()) return StudioRepositoryResult.Success(false)
        return if (file.delete()) StudioRepositoryResult.Success(true)
        else StudioRepositoryResult.Failure(
            StudioRepositoryError(StudioRepositoryErrorKind.IO_ERROR, "Unable to remove snapshot metadata")
        )
    }

    fun deleteStaleMetadata(): StudioRepositoryResult<Int> {
        if (!metadataDirectory.exists()) return StudioRepositoryResult.Success(0)
        var deleted = 0
        metadataDirectory.listFiles { file -> file.isFile && file.extension == "json" }.orEmpty().forEach { file ->
            val stale = try {
                !resolveSnapshotFile(decode(file.readText(Charsets.UTF_8)).snapshotPath).isFile
            } catch (_: Exception) {
                true
            }
            if (stale && file.delete()) deleted++
        }
        return StudioRepositoryResult.Success(deleted)
    }

    private fun resolveSnapshotFile(path: String): File {
        val file = File(path)
        return if (file.isAbsolute) file else File(metadataDirectory.parentFile, path)
    }

    private fun metadataFile(widgetId: String, variantId: WidgetVariantId): File {
        val variantSegment = variantId.stableId.replace(':', '_')
        return File(metadataDirectory, "${widgetId}__${variantSegment}.json")
    }

    private fun validationError(metadata: WidgetSnapshotMetadata): String? = when {
        !safeStudioId(metadata.widgetId) -> "Invalid snapshot widget ID"
        !metadata.variantId.isValid() -> "Invalid snapshot variant ID"
        !metadata.widthDp.isFinite() || metadata.widthDp <= 0f -> "Snapshot width must be positive and finite"
        !metadata.heightDp.isFinite() || metadata.heightDp <= 0f -> "Snapshot height must be positive and finite"
        !metadata.density.isFinite() || metadata.density <= 0f -> "Snapshot density must be positive and finite"
        metadata.designRevision < 0L -> "Snapshot design revision must not be negative"
        metadata.createdAt < 0L -> "Snapshot creation time must not be negative"
        metadata.snapshotPath.isBlank() -> "Snapshot path must not be empty"
        else -> null
    }

    private fun encode(metadata: WidgetSnapshotMetadata): String = SimpleJson.stringify(
        jsonObject(
            "schemaVersion" to jsonNumber(SNAPSHOT_SCHEMA_VERSION),
            "widgetId" to jsonString(metadata.widgetId),
            "variantId" to jsonString(metadata.variantId.stableId),
            "widthDp" to jsonNumber(metadata.widthDp),
            "heightDp" to jsonNumber(metadata.heightDp),
            "density" to jsonNumber(metadata.density),
            "designRevision" to jsonNumber(metadata.designRevision),
            "createdAt" to jsonNumber(metadata.createdAt),
            "snapshotPath" to jsonString(metadata.snapshotPath)
        )
    )

    private fun decode(contents: String): WidgetSnapshotMetadata {
        val json = SimpleJson.parse(contents).asObject("root")
        val schemaVersion = json.required("schemaVersion").asInt("schemaVersion")
        if (schemaVersion != SNAPSHOT_SCHEMA_VERSION) {
            throw JsonValueException("Unsupported snapshot metadata schema version: $schemaVersion")
        }
        val variantStableId = json.required("variantId").asString("variantId")
        val variantId = WidgetVariantId.parse(variantStableId)
            ?: throw JsonValueException("Invalid snapshot variant ID: $variantStableId")
        val metadata = WidgetSnapshotMetadata(
            widgetId = json.required("widgetId").asString("widgetId"),
            variantId = variantId,
            widthDp = json.required("widthDp").asFloat("widthDp"),
            heightDp = json.required("heightDp").asFloat("heightDp"),
            density = json.required("density").asFloat("density"),
            designRevision = json.required("designRevision").asLong("designRevision"),
            createdAt = json.required("createdAt").asLong("createdAt"),
            snapshotPath = json.required("snapshotPath").asString("snapshotPath")
        )
        validationError(metadata)?.let { throw JsonValueException(it) }
        return metadata
    }

    companion object {
        const val SNAPSHOT_SCHEMA_VERSION = 1
        const val METADATA_DIRECTORY = "widget_studio/snapshots/metadata"
    }
}
