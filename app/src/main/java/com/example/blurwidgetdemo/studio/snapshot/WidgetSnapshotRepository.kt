package dev.revilodev.onewidget.studio.snapshot

import android.content.Context
import dev.revilodev.onewidget.studio.data.StudioRepositoryError
import dev.revilodev.onewidget.studio.data.StudioRepositoryErrorKind
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.data.safeStudioId
import dev.revilodev.onewidget.studio.data.writeTextAtomically
import dev.revilodev.onewidget.studio.json.JsonParseException
import dev.revilodev.onewidget.studio.json.JsonObjectValue
import dev.revilodev.onewidget.studio.json.JsonValueException
import dev.revilodev.onewidget.studio.json.SimpleJson
import dev.revilodev.onewidget.studio.json.asFloat
import dev.revilodev.onewidget.studio.json.asInt
import dev.revilodev.onewidget.studio.json.asLong
import dev.revilodev.onewidget.studio.json.asObject
import dev.revilodev.onewidget.studio.json.asString
import dev.revilodev.onewidget.studio.json.jsonNumber
import dev.revilodev.onewidget.studio.json.jsonObject
import dev.revilodev.onewidget.studio.json.jsonString
import dev.revilodev.onewidget.studio.json.required
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

enum class SnapshotScope { DEVELOPER, PRODUCTION }

enum class SnapshotRenderMode { LOCAL_REMOTEVIEWS }

data class WidgetSnapshotMetadata(
    val widgetId: String,
    val variantId: WidgetVariantId,
    val widthDp: Float,
    val heightDp: Float,
    val density: Float,
    val designRevision: Long,
    val createdAt: Long,
    val snapshotPath: String,
    val widthPx: Int = (widthDp * density).roundToInt().coerceAtLeast(1),
    val heightPx: Int = (heightDp * density).roundToInt().coerceAtLeast(1),
    val scope: SnapshotScope = SnapshotScope.DEVELOPER,
    val renderMode: SnapshotRenderMode = SnapshotRenderMode.LOCAL_REMOTEVIEWS,
    val rendererVersion: Int = 1
)

sealed class SnapshotLookupResult {
    data class Available(val metadata: WidgetSnapshotMetadata, val file: File) : SnapshotLookupResult()
    data class MissingFile(val metadata: WidgetSnapshotMetadata) : SnapshotLookupResult()
    data class CorruptMetadata(val message: String) : SnapshotLookupResult()
    data object NotFound : SnapshotLookupResult()
}

sealed class SnapshotStatus {
    data class Current(val snapshot: SnapshotLookupResult.Available) : SnapshotStatus()
    data class Stale(
        val snapshot: SnapshotLookupResult.Available,
        val currentDesignRevision: Long
    ) : SnapshotStatus()
    data object Missing : SnapshotStatus()
}

data class SnapshotCleanupResult(
    val removedMetadata: Int,
    val removedTemporaryFiles: Int,
    val removedOrphanImages: Int
)

object SnapshotFileNames {
    fun image(widgetId: String, variantId: WidgetVariantId, designRevision: Long): String {
        require(safeStudioId(widgetId)) { "Invalid snapshot widget ID" }
        require(variantId.isValid()) { "Invalid snapshot variant ID" }
        require(designRevision >= 0L) { "Snapshot revision must not be negative" }
        return "${widgetId}__${variantSegment(variantId)}__rev_$designRevision.png"
    }

    fun metadata(widgetId: String, variantId: WidgetVariantId, designRevision: Long): String =
        image(widgetId, variantId, designRevision).removeSuffix(".png") + ".json"

    fun prefix(widgetId: String, variantId: WidgetVariantId): String {
        require(safeStudioId(widgetId)) { "Invalid snapshot widget ID" }
        require(variantId.isValid()) { "Invalid snapshot variant ID" }
        return "${widgetId}__${variantSegment(variantId)}"
    }

    private fun variantSegment(variantId: WidgetVariantId): String =
        variantId.stableId.replace(':', '_')
}

/** Revision-aware metadata and PNG lifecycle for app-private Studio snapshots. */
class WidgetSnapshotRepository(private val metadataDirectory: File) {
    constructor(context: Context) : this(File(context.filesDir, METADATA_DIRECTORY))

    private val snapshotRoot: File = metadataDirectory.parentFile
        ?: error("Snapshot metadata directory requires a parent")
    val imagesDirectory: File = File(snapshotRoot, IMAGES_DIRECTORY_NAME)

    fun register(metadata: WidgetSnapshotMetadata): StudioRepositoryResult<WidgetSnapshotMetadata> {
        validationError(metadata)?.let { message ->
            return failure(StudioRepositoryErrorKind.INVALID_DESIGN, message)
        }
        if (resolveSnapshotFile(metadata.snapshotPath) == null) {
            return failure(StudioRepositoryErrorKind.INVALID_ID, "Snapshot path escapes private storage")
        }
        return when (
            val write = writeTextAtomically(
                metadataFile(metadata.widgetId, metadata.variantId, metadata.designRevision),
                encode(metadata)
            )
        ) {
            is StudioRepositoryResult.Success -> StudioRepositoryResult.Success(metadata)
            is StudioRepositoryResult.Failure -> write
        }
    }

    fun latest(widgetId: String, variantId: WidgetVariantId): SnapshotLookupResult =
        findLatest(widgetId, variantId)

    fun findLatest(widgetId: String, variantId: WidgetVariantId): SnapshotLookupResult {
        val candidates = candidates(widgetId, variantId)
        candidates.available.maxWithOrNull(snapshotOrder)?.let { return it }
        candidates.missing.maxWithOrNull(missingOrder)?.let { return it }
        candidates.corrupt.firstOrNull()?.let { return SnapshotLookupResult.CorruptMetadata(it) }
        return SnapshotLookupResult.NotFound
    }

    fun status(
        widgetId: String,
        variantId: WidgetVariantId,
        currentDesignRevision: Long
    ): SnapshotStatus {
        if (currentDesignRevision < 0L) return SnapshotStatus.Missing
        val available = candidates(widgetId, variantId).available
        val current = available.filter { it.metadata.designRevision == currentDesignRevision }
            .maxByOrNull { it.metadata.createdAt }
        if (current != null) return SnapshotStatus.Current(current)
        val latest = available.maxWithOrNull(snapshotOrder) ?: return SnapshotStatus.Missing
        return SnapshotStatus.Stale(latest, currentDesignRevision)
    }

    /** Deletes both the selected PNG and its revision-specific metadata. */
    fun delete(metadata: WidgetSnapshotMetadata): StudioRepositoryResult<Boolean> {
        validationError(metadata)?.let { return failure(StudioRepositoryErrorKind.INVALID_ID, it) }
        val image = resolveSnapshotFile(metadata.snapshotPath)
            ?: return failure(StudioRepositoryErrorKind.INVALID_ID, "Snapshot path escapes private storage")
        val matchingMetadata = metadataFiles(metadata.widgetId, metadata.variantId).filter { file ->
            runCatching {
                decode(file.readText(Charsets.UTF_8)).designRevision == metadata.designRevision
            }.getOrDefault(false)
        }
        if (image.exists() && !image.delete()) {
            return failure(StudioRepositoryErrorKind.IO_ERROR, "Unable to remove snapshot PNG")
        }
        if (matchingMetadata.any { it.exists() && !it.delete() }) {
            return failure(StudioRepositoryErrorKind.IO_ERROR, "Unable to remove snapshot metadata")
        }
        return StudioRepositoryResult.Success(true)
    }

    /** Phase 1 compatibility: removes every metadata revision for an identity, not its images. */
    fun removeMetadata(widgetId: String, variantId: WidgetVariantId): StudioRepositoryResult<Boolean> {
        if (!safeStudioId(widgetId) || !variantId.isValid()) {
            return failure(StudioRepositoryErrorKind.INVALID_ID, "Invalid snapshot identity")
        }
        val files = metadataFiles(widgetId, variantId)
        if (files.isEmpty()) return StudioRepositoryResult.Success(false)
        if (files.any { it.exists() && !it.delete() }) {
            return failure(StudioRepositoryErrorKind.IO_ERROR, "Unable to remove snapshot metadata")
        }
        return StudioRepositoryResult.Success(true)
    }

    fun cleanup(): StudioRepositoryResult<SnapshotCleanupResult> {
        var removedMetadata = 0
        var removedTemporaryFiles = 0
        var removedOrphanImages = 0
        val referencedImages = linkedSetOf<String>()
        metadataDirectory.listFiles { file -> file.isFile && file.extension == "json" }
            .orEmpty()
            .forEach { metadataFile ->
                val image = runCatching {
                    resolveSnapshotFile(decode(metadataFile.readText(Charsets.UTF_8)).snapshotPath)
                }.getOrNull()
                if (image == null || !image.isFile) {
                    if (metadataFile.delete()) removedMetadata++
                } else {
                    referencedImages += image.canonicalPath
                }
            }
        imagesDirectory.listFiles().orEmpty().forEach { file ->
            when {
                file.name.endsWith(TEMPORARY_SUFFIX) && file.delete() -> removedTemporaryFiles++
                file.extension == "png" && file.canonicalPath !in referencedImages && file.delete() -> {
                    removedOrphanImages++
                }
            }
        }
        return StudioRepositoryResult.Success(
            SnapshotCleanupResult(removedMetadata, removedTemporaryFiles, removedOrphanImages)
        )
    }

    fun deleteStaleMetadata(): StudioRepositoryResult<Int> = when (val result = cleanup()) {
        is StudioRepositoryResult.Success -> StudioRepositoryResult.Success(result.value.removedMetadata)
        is StudioRepositoryResult.Failure -> result
    }

    fun relativeImagePath(fileName: String): String = "$IMAGES_DIRECTORY_NAME/$fileName"

    fun resolveImage(metadata: WidgetSnapshotMetadata): File? =
        resolveSnapshotFile(metadata.snapshotPath)?.takeIf(File::isFile)

    private data class Candidates(
        val available: List<SnapshotLookupResult.Available>,
        val missing: List<SnapshotLookupResult.MissingFile>,
        val corrupt: List<String>
    )

    private fun candidates(widgetId: String, variantId: WidgetVariantId): Candidates {
        if (!safeStudioId(widgetId) || !variantId.isValid()) {
            return Candidates(emptyList(), emptyList(), emptyList())
        }
        val available = mutableListOf<SnapshotLookupResult.Available>()
        val missing = mutableListOf<SnapshotLookupResult.MissingFile>()
        val corrupt = mutableListOf<String>()
        metadataFiles(widgetId, variantId).forEach { file ->
            val metadata = try {
                decode(file.readText(Charsets.UTF_8))
            } catch (error: IOException) {
                corrupt += error.message ?: "Unable to read snapshot metadata"
                return@forEach
            } catch (error: JsonParseException) {
                corrupt += error.message ?: "Corrupt snapshot metadata"
                return@forEach
            } catch (error: JsonValueException) {
                corrupt += error.message ?: "Corrupt snapshot metadata"
                return@forEach
            }
            if (metadata.widgetId != widgetId || metadata.variantId != variantId) {
                corrupt += "Snapshot metadata identity does not match its file"
                return@forEach
            }
            val image = resolveSnapshotFile(metadata.snapshotPath)
            if (image == null) {
                corrupt += "Snapshot path escapes private storage"
            } else if (image.isFile) {
                available += SnapshotLookupResult.Available(metadata, image)
            } else {
                missing += SnapshotLookupResult.MissingFile(metadata)
            }
        }
        return Candidates(available, missing, corrupt)
    }

    private fun metadataFiles(widgetId: String, variantId: WidgetVariantId): List<File> {
        if (!metadataDirectory.isDirectory) return emptyList()
        val prefix = SnapshotFileNames.prefix(widgetId, variantId)
        return metadataDirectory.listFiles { file ->
            file.isFile && file.extension == "json" &&
                (file.name == "$prefix.json" || file.name.startsWith("${prefix}__rev_"))
        }.orEmpty().sortedBy(File::getName)
    }

    private fun resolveSnapshotFile(path: String): File? {
        val candidate = File(path).let { if (it.isAbsolute) it else File(snapshotRoot, path) }
        val rootPath = snapshotRoot.canonicalFile.toPath()
        val candidatePath = candidate.canonicalFile.toPath()
        return candidate.takeIf { candidatePath.startsWith(rootPath) }
    }

    private fun metadataFile(
        widgetId: String,
        variantId: WidgetVariantId,
        designRevision: Long
    ): File = File(
        metadataDirectory,
        SnapshotFileNames.metadata(widgetId, variantId, designRevision)
    )

    private fun validationError(metadata: WidgetSnapshotMetadata): String? = when {
        !safeStudioId(metadata.widgetId) -> "Invalid snapshot widget ID"
        !metadata.variantId.isValid() -> "Invalid snapshot variant ID"
        !metadata.widthDp.isFinite() || metadata.widthDp <= 0f -> {
            "Snapshot width must be positive and finite"
        }
        !metadata.heightDp.isFinite() || metadata.heightDp <= 0f -> {
            "Snapshot height must be positive and finite"
        }
        !metadata.density.isFinite() || metadata.density <= 0f -> {
            "Snapshot density must be positive and finite"
        }
        metadata.widthPx <= 0 || metadata.heightPx <= 0 -> "Snapshot pixels must be positive"
        metadata.designRevision < 0L -> "Snapshot design revision must not be negative"
        metadata.createdAt < 0L -> "Snapshot creation time must not be negative"
        metadata.snapshotPath.isBlank() -> "Snapshot path must not be empty"
        metadata.rendererVersion <= 0 -> "Snapshot renderer version must be positive"
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
            "widthPx" to jsonNumber(metadata.widthPx),
            "heightPx" to jsonNumber(metadata.heightPx),
            "designRevision" to jsonNumber(metadata.designRevision),
            "createdAt" to jsonNumber(metadata.createdAt),
            "snapshotPath" to jsonString(metadata.snapshotPath),
            "scope" to jsonString(metadata.scope.name),
            "renderMode" to jsonString(metadata.renderMode.name),
            "rendererVersion" to jsonNumber(metadata.rendererVersion)
        )
    )

    private fun decode(contents: String): WidgetSnapshotMetadata {
        val json = SimpleJson.parse(contents).asObject("root")
        return when (val schemaVersion = json.required("schemaVersion").asInt("schemaVersion")) {
            1 -> metadataCommon(json)
            SNAPSHOT_SCHEMA_VERSION -> decodeVersion2(json)
            else -> throw JsonValueException(
                "Unsupported snapshot metadata schema version: $schemaVersion"
            )
        }.also { metadata ->
            validationError(metadata)?.let { throw JsonValueException(it) }
        }
    }

    private fun decodeVersion2(json: JsonObjectValue): WidgetSnapshotMetadata {
        val common = metadataCommon(json)
        return common.copy(
            widthPx = json.required("widthPx").asInt("widthPx"),
            heightPx = json.required("heightPx").asInt("heightPx"),
            scope = enumValue(json.required("scope").asString("scope"), "scope"),
            renderMode = enumValue(
                json.required("renderMode").asString("renderMode"),
                "renderMode"
            ),
            rendererVersion = json.required("rendererVersion").asInt("rendererVersion")
        )
    }

    private fun metadataCommon(json: JsonObjectValue): WidgetSnapshotMetadata {
        val variantStableId = json.required("variantId").asString("variantId")
        val variantId = WidgetVariantId.parse(variantStableId)
            ?: throw JsonValueException("Invalid snapshot variant ID: $variantStableId")
        return WidgetSnapshotMetadata(
            widgetId = json.required("widgetId").asString("widgetId"),
            variantId = variantId,
            widthDp = json.required("widthDp").asFloat("widthDp"),
            heightDp = json.required("heightDp").asFloat("heightDp"),
            density = json.required("density").asFloat("density"),
            designRevision = json.required("designRevision").asLong("designRevision"),
            createdAt = json.required("createdAt").asLong("createdAt"),
            snapshotPath = json.required("snapshotPath").asString("snapshotPath")
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw JsonValueException("Invalid $field: $value")

    private fun <T> failure(
        kind: StudioRepositoryErrorKind,
        message: String
    ): StudioRepositoryResult<T> = StudioRepositoryResult.Failure(StudioRepositoryError(kind, message))

    companion object {
        const val SNAPSHOT_SCHEMA_VERSION = 2
        const val SNAPSHOT_ROOT_DIRECTORY = "widget_studio/snapshots"
        const val METADATA_DIRECTORY = "$SNAPSHOT_ROOT_DIRECTORY/metadata"
        const val IMAGES_DIRECTORY_NAME = "images"
        private val snapshotOrder = compareBy<SnapshotLookupResult.Available>(
            { it.metadata.designRevision },
            { it.metadata.createdAt }
        )
        private val missingOrder = compareBy<SnapshotLookupResult.MissingFile>(
            { it.metadata.designRevision },
            { it.metadata.createdAt }
        )
        private const val TEMPORARY_SUFFIX = ".tmp"
    }
}
