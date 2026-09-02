package com.example.blurwidgetdemo.studio.production

import android.content.Context
import com.example.blurwidgetdemo.studio.data.StudioRepositoryError
import com.example.blurwidgetdemo.studio.data.StudioRepositoryErrorKind
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.data.safeStudioId
import com.example.blurwidgetdemo.studio.data.writeTextAtomically
import com.example.blurwidgetdemo.studio.json.JsonParseException
import com.example.blurwidgetdemo.studio.json.JsonValueException
import com.example.blurwidgetdemo.studio.json.SimpleJson
import com.example.blurwidgetdemo.studio.json.asInt
import com.example.blurwidgetdemo.studio.json.asLong
import com.example.blurwidgetdemo.studio.json.asObject
import com.example.blurwidgetdemo.studio.json.asString
import com.example.blurwidgetdemo.studio.json.jsonNumber
import com.example.blurwidgetdemo.studio.json.jsonObject
import com.example.blurwidgetdemo.studio.json.jsonString
import com.example.blurwidgetdemo.studio.json.required
import com.example.blurwidgetdemo.studio.model.DesignDecodeResult
import com.example.blurwidgetdemo.studio.model.WidgetDesignJsonCodec
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetDesignValidator
import com.example.blurwidgetdemo.studio.verification.WidgetVerificationStatus
import java.io.File
import java.io.IOException
import java.security.MessageDigest

const val PROMOTED_DESIGN_SCHEMA_VERSION = 1

data class PromotedWidgetDesignMetadata(
    val schemaVersion: Int = PROMOTED_DESIGN_SCHEMA_VERSION,
    val widgetId: String,
    val designRevision: Long,
    val promotedAt: Long,
    val verificationStatus: WidgetVerificationStatus,
    val sourceDesignHash: String
)

data class PromotedWidgetDesign(
    val metadata: PromotedWidgetDesignMetadata,
    val design: WidgetDesignSpec
)

/** Immutable production copies. This repository never reads mutable Widget Studio storage. */
class ProductionWidgetDesignRepository(
    private val rootDirectory: File,
    private val codec: WidgetDesignJsonCodec = WidgetDesignJsonCodec(),
    private val historyLimit: Int = DEFAULT_HISTORY_LIMIT
) {
    init {
        require(historyLimit > 0) { "Production history limit must be positive" }
    }

    constructor(context: Context) : this(File(context.filesDir, DIRECTORY))

    fun promote(
        design: WidgetDesignSpec,
        verificationStatus: WidgetVerificationStatus,
        promotedAt: Long
    ): StudioRepositoryResult<PromotedWidgetDesign> {
        if (!safeStudioId(design.widgetId) || design.revision <= 0L || promotedAt < 0L) {
            return failure(StudioRepositoryErrorKind.INVALID_ID, "Invalid production promotion identity")
        }
        val validation = WidgetDesignValidator.validate(design)
        if (!validation.isValid) {
            return failure(
                StudioRepositoryErrorKind.INVALID_DESIGN,
                validation.issues.joinToString("; ") { "${it.field}: ${it.message}" }
            )
        }
        val designJson = codec.encode(design)
        val hash = ProductionDesignIntegrity.sha256(designJson)
        val revisionDirectory = revisionDirectory(design.widgetId, design.revision)
        val designFile = File(revisionDirectory, DESIGN_FILE)
        if (designFile.exists()) {
            val existingHash = runCatching {
                ProductionDesignIntegrity.sha256(designFile.readText(Charsets.UTF_8))
            }.getOrNull()
            if (existingHash != hash) {
                return failure(
                    StudioRepositoryErrorKind.CORRUPT_DATA,
                    "Immutable production revision ${design.revision} already has different content"
                )
            }
        }
        val metadata = PromotedWidgetDesignMetadata(
            widgetId = design.widgetId,
            designRevision = design.revision,
            promotedAt = promotedAt,
            verificationStatus = verificationStatus,
            sourceDesignHash = hash
        )
        when (val write = writeTextAtomically(designFile, designJson)) {
            is StudioRepositoryResult.Failure -> return write
            is StudioRepositoryResult.Success -> Unit
        }
        when (val write = writeTextAtomically(File(revisionDirectory, METADATA_FILE), encodeMetadata(metadata))) {
            is StudioRepositoryResult.Failure -> return write
            is StudioRepositoryResult.Success -> Unit
        }
        when (val activated = writeActive(design.widgetId, design.revision)) {
            is StudioRepositoryResult.Failure -> return activated
            is StudioRepositoryResult.Success -> Unit
        }
        pruneHistory(design.widgetId)
        return StudioRepositoryResult.Success(PromotedWidgetDesign(metadata, design))
    }

    fun loadActive(widgetId: String): StudioRepositoryResult<PromotedWidgetDesign?> {
        if (!safeStudioId(widgetId)) return failure(StudioRepositoryErrorKind.INVALID_ID, "Invalid widget ID")
        val active = activeFile(widgetId)
        if (!active.isFile) return StudioRepositoryResult.Success(null)
        val revision = try {
            decodeActive(active.readText(Charsets.UTF_8))
        } catch (error: Exception) {
            return decodeFailure(error, "active production revision")
        }
        return when (val loaded = loadRevision(widgetId, revision)) {
            is StudioRepositoryResult.Success -> StudioRepositoryResult.Success(loaded.value)
            is StudioRepositoryResult.Failure -> loaded
        }
    }

    fun loadRevision(widgetId: String, revision: Long): StudioRepositoryResult<PromotedWidgetDesign> {
        if (!safeStudioId(widgetId) || revision <= 0L) {
            return failure(StudioRepositoryErrorKind.INVALID_ID, "Invalid production revision identity")
        }
        val directory = revisionDirectory(widgetId, revision)
        val designFile = File(directory, DESIGN_FILE)
        val metadataFile = File(directory, METADATA_FILE)
        if (!designFile.isFile || !metadataFile.isFile) {
            return failure(StudioRepositoryErrorKind.NOT_FOUND, "Production revision not found")
        }
        return try {
            val designJson = designFile.readText(Charsets.UTF_8)
            val metadata = decodeMetadata(metadataFile.readText(Charsets.UTF_8))
            val design = when (val decoded = codec.decode(designJson)) {
                is DesignDecodeResult.Success -> decoded.design
                is DesignDecodeResult.Failure -> return failure(
                    StudioRepositoryErrorKind.CORRUPT_DATA,
                    decoded.error.message
                )
            }
            val hash = ProductionDesignIntegrity.sha256(designJson)
            if (
                metadata.widgetId != widgetId || metadata.designRevision != revision ||
                design.widgetId != widgetId || design.revision != revision
            ) {
                failure(StudioRepositoryErrorKind.CORRUPT_DATA, "Production revision identity mismatch")
            } else if (hash != metadata.sourceDesignHash) {
                failure(StudioRepositoryErrorKind.CORRUPT_DATA, "Production design integrity hash mismatch")
            } else {
                StudioRepositoryResult.Success(PromotedWidgetDesign(metadata, design))
            }
        } catch (error: Exception) {
            decodeFailure(error, "production revision")
        }
    }

    fun history(widgetId: String): StudioRepositoryResult<List<PromotedWidgetDesignMetadata>> {
        if (!safeStudioId(widgetId)) return failure(StudioRepositoryErrorKind.INVALID_ID, "Invalid widget ID")
        val revisions = File(widgetDirectory(widgetId), REVISIONS_DIRECTORY)
        if (!revisions.isDirectory) return StudioRepositoryResult.Success(emptyList())
        val metadata = revisions.listFiles { file -> file.isDirectory && file.name.startsWith(REVISION_PREFIX) }
            .orEmpty()
            .mapNotNull { directory ->
                runCatching { decodeMetadata(File(directory, METADATA_FILE).readText(Charsets.UTF_8)) }.getOrNull()
            }
            .sortedWith(compareByDescending<PromotedWidgetDesignMetadata> { it.promotedAt }
                .thenByDescending { it.designRevision })
        return StudioRepositoryResult.Success(metadata)
    }

    fun activateRevision(widgetId: String, revision: Long): StudioRepositoryResult<PromotedWidgetDesign> =
        when (val loaded = loadRevision(widgetId, revision)) {
            is StudioRepositoryResult.Failure -> loaded
            is StudioRepositoryResult.Success -> when (val active = writeActive(widgetId, revision)) {
                is StudioRepositoryResult.Failure -> active
                is StudioRepositoryResult.Success -> loaded
            }
        }

    fun revertToLegacy(widgetId: String): StudioRepositoryResult<Boolean> {
        if (!safeStudioId(widgetId)) return failure(StudioRepositoryErrorKind.INVALID_ID, "Invalid widget ID")
        val active = activeFile(widgetId)
        return when {
            !active.exists() -> StudioRepositoryResult.Success(false)
            active.delete() -> StudioRepositoryResult.Success(true)
            else -> failure(StudioRepositoryErrorKind.IO_ERROR, "Unable to clear active production design")
        }
    }

    private fun writeActive(widgetId: String, revision: Long): StudioRepositoryResult<Unit> =
        writeTextAtomically(
            activeFile(widgetId),
            SimpleJson.stringify(
                jsonObject(
                    "schemaVersion" to jsonNumber(PROMOTED_DESIGN_SCHEMA_VERSION),
                    "activeRevision" to jsonNumber(revision)
                )
            )
        )

    private fun encodeMetadata(value: PromotedWidgetDesignMetadata): String = SimpleJson.stringify(
        jsonObject(
            "schemaVersion" to jsonNumber(value.schemaVersion),
            "widgetId" to jsonString(value.widgetId),
            "designRevision" to jsonNumber(value.designRevision),
            "promotedAt" to jsonNumber(value.promotedAt),
            "verificationStatus" to jsonString(value.verificationStatus.name),
            "sourceDesignHash" to jsonString(value.sourceDesignHash)
        )
    )

    private fun decodeMetadata(contents: String): PromotedWidgetDesignMetadata {
        val json = SimpleJson.parse(contents).asObject("root")
        val schema = json.required("schemaVersion").asInt("schemaVersion")
        if (schema != PROMOTED_DESIGN_SCHEMA_VERSION) {
            throw JsonValueException("Unsupported production metadata schema: $schema")
        }
        val statusText = json.required("verificationStatus").asString("verificationStatus")
        val status = WidgetVerificationStatus.entries.firstOrNull { it.name == statusText }
            ?: throw JsonValueException("Invalid verification status: $statusText")
        return PromotedWidgetDesignMetadata(
            schemaVersion = schema,
            widgetId = json.required("widgetId").asString("widgetId"),
            designRevision = json.required("designRevision").asLong("designRevision"),
            promotedAt = json.required("promotedAt").asLong("promotedAt"),
            verificationStatus = status,
            sourceDesignHash = json.required("sourceDesignHash").asString("sourceDesignHash")
        )
    }

    private fun decodeActive(contents: String): Long {
        val json = SimpleJson.parse(contents).asObject("root")
        val schema = json.required("schemaVersion").asInt("schemaVersion")
        if (schema != PROMOTED_DESIGN_SCHEMA_VERSION) {
            throw JsonValueException("Unsupported active production schema: $schema")
        }
        return json.required("activeRevision").asLong("activeRevision")
    }

    private fun pruneHistory(widgetId: String) {
        val history = when (val result = history(widgetId)) {
            is StudioRepositoryResult.Success -> result.value
            is StudioRepositoryResult.Failure -> return
        }
        history.drop(historyLimit).forEach { metadata ->
            revisionDirectory(widgetId, metadata.designRevision).deleteRecursively()
        }
    }

    private fun widgetDirectory(widgetId: String) = File(rootDirectory, widgetId)
    private fun revisionDirectory(widgetId: String, revision: Long) =
        File(File(widgetDirectory(widgetId), REVISIONS_DIRECTORY), "$REVISION_PREFIX$revision")
    private fun activeFile(widgetId: String) = File(widgetDirectory(widgetId), ACTIVE_FILE)

    private fun <T> decodeFailure(error: Exception, label: String): StudioRepositoryResult<T> = when (error) {
        is IOException -> failure(StudioRepositoryErrorKind.IO_ERROR, error.message ?: "Unable to read $label")
        is JsonParseException, is JsonValueException -> failure(
            StudioRepositoryErrorKind.CORRUPT_DATA,
            error.message ?: "Corrupt $label"
        )
        else -> failure(StudioRepositoryErrorKind.CORRUPT_DATA, error.message ?: "Corrupt $label")
    }

    private fun <T> failure(kind: StudioRepositoryErrorKind, message: String): StudioRepositoryResult<T> =
        StudioRepositoryResult.Failure(StudioRepositoryError(kind, message))

    companion object {
        const val DIRECTORY = "widget_designs/production"
        const val DEFAULT_HISTORY_LIMIT = 10
        private const val REVISIONS_DIRECTORY = "revisions"
        private const val REVISION_PREFIX = "rev_"
        private const val DESIGN_FILE = "design.json"
        private const val METADATA_FILE = "promotion.json"
        private const val ACTIVE_FILE = "active.json"
    }
}

object ProductionDesignIntegrity {
    fun sha256(contents: String): String = MessageDigest.getInstance("SHA-256")
        .digest(contents.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
