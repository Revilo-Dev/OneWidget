package com.example.blurwidgetdemo.studio.verification

import android.content.Context
import com.example.blurwidgetdemo.studio.data.StudioRepositoryError
import com.example.blurwidgetdemo.studio.data.StudioRepositoryErrorKind
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.data.safeStudioId
import com.example.blurwidgetdemo.studio.data.writeTextAtomically
import com.example.blurwidgetdemo.studio.json.JsonParseException
import com.example.blurwidgetdemo.studio.json.JsonValueException
import com.example.blurwidgetdemo.studio.json.SimpleJson
import com.example.blurwidgetdemo.studio.json.asBoolean
import com.example.blurwidgetdemo.studio.json.asInt
import com.example.blurwidgetdemo.studio.json.asLong
import com.example.blurwidgetdemo.studio.json.asObject
import com.example.blurwidgetdemo.studio.json.asString
import com.example.blurwidgetdemo.studio.json.jsonBoolean
import com.example.blurwidgetdemo.studio.json.jsonNumber
import com.example.blurwidgetdemo.studio.json.jsonObject
import com.example.blurwidgetdemo.studio.json.jsonString
import com.example.blurwidgetdemo.studio.json.required
import com.example.blurwidgetdemo.studio.json.optional
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import java.io.File
import java.io.IOException

/** Human-readable, revision-addressed Samsung device checks in app-private storage. */
class WidgetParityVerificationRepository(private val directory: File) {
    constructor(context: Context) : this(File(context.filesDir, DIRECTORY))

    fun save(value: WidgetParityVerification): StudioRepositoryResult<WidgetParityVerification> {
        validationError(value)?.let { return failure(StudioRepositoryErrorKind.INVALID_DESIGN, it) }
        return when (val write = writeTextAtomically(file(value), encode(value))) {
            is StudioRepositoryResult.Success -> StudioRepositoryResult.Success(value)
            is StudioRepositoryResult.Failure -> write
        }
    }

    fun load(
        widgetId: String,
        variantId: WidgetVariantId,
        designRevision: Long
    ): StudioRepositoryResult<WidgetParityVerification> {
        if (!validIdentity(widgetId, variantId, designRevision)) return invalidIdentity()
        val target = file(widgetId, variantId, designRevision)
        if (!target.isFile) return failure(StudioRepositoryErrorKind.NOT_FOUND, "Verification not found")
        return try {
            val decoded = decode(target.readText(Charsets.UTF_8))
            if (
                decoded.widgetId != widgetId || decoded.variantId != variantId ||
                decoded.designRevision != designRevision
            ) {
                failure(StudioRepositoryErrorKind.CORRUPT_DATA, "Verification identity does not match its file")
            } else {
                StudioRepositoryResult.Success(decoded)
            }
        } catch (error: IOException) {
            failure(StudioRepositoryErrorKind.IO_ERROR, error.message ?: "Unable to read verification")
        } catch (error: JsonParseException) {
            failure(StudioRepositoryErrorKind.CORRUPT_DATA, error.message ?: "Corrupt verification JSON")
        } catch (error: JsonValueException) {
            failure(StudioRepositoryErrorKind.CORRUPT_DATA, error.message ?: "Corrupt verification JSON")
        }
    }

    fun delete(widgetId: String, variantId: WidgetVariantId, designRevision: Long): StudioRepositoryResult<Boolean> {
        if (!validIdentity(widgetId, variantId, designRevision)) return invalidIdentity()
        val target = file(widgetId, variantId, designRevision)
        return when {
            !target.exists() -> StudioRepositoryResult.Success(false)
            target.delete() -> StudioRepositoryResult.Success(true)
            else -> failure(StudioRepositoryErrorKind.IO_ERROR, "Unable to delete verification")
        }
    }

    fun status(widgetId: String, variantId: WidgetVariantId, currentDesignRevision: Long): VerificationRevisionStatus {
        when (val exact = load(widgetId, variantId, currentDesignRevision)) {
            is StudioRepositoryResult.Success -> return VerificationRevisionStatus.Current(
                exact.value,
                WidgetParityVerificationPolicy.assess(exact.value)
            )
            is StudioRepositoryResult.Failure -> Unit
        }
        val latest = list(widgetId, variantId).maxByOrNull { it.designRevision }
            ?: return VerificationRevisionStatus.NotTested
        return VerificationRevisionStatus.Stale(latest, currentDesignRevision)
    }

    fun listVerifiedRevisions(widgetId: String, variantId: WidgetVariantId): List<Long> =
        list(widgetId, variantId)
            .filter { WidgetParityVerificationPolicy.assess(it).status == WidgetVerificationStatus.VERIFIED }
            .map { it.designRevision }
            .sortedDescending()

    private fun list(widgetId: String, variantId: WidgetVariantId): List<WidgetParityVerification> {
        if (!safeStudioId(widgetId) || !variantId.isValid() || !directory.isDirectory) return emptyList()
        val prefix = prefix(widgetId, variantId)
        return directory.listFiles { candidate ->
            candidate.isFile && candidate.extension == "json" && candidate.name.startsWith(prefix)
        }.orEmpty().mapNotNull { candidate ->
            runCatching { decode(candidate.readText(Charsets.UTF_8)) }.getOrNull()
                ?.takeIf { it.widgetId == widgetId && it.variantId == variantId }
        }
    }

    private fun encode(value: WidgetParityVerification): String = SimpleJson.stringify(
        jsonObject(
            "schemaVersion" to jsonNumber(value.schemaVersion),
            "widgetId" to jsonString(value.widgetId),
            "variantId" to jsonString(value.variantId.stableId),
            "designRevision" to jsonNumber(value.designRevision),
            "deviceManufacturer" to jsonString(value.deviceManufacturer),
            "deviceModel" to jsonString(value.deviceModel),
            "androidVersion" to jsonString(value.androidVersion),
            "androidSdkInt" to jsonNumber(value.androidSdkInt),
            "oneUiVersion" to jsonString(value.oneUiVersion),
            "launcherVersion" to jsonString(value.launcherVersion),
            "actualPreviewVerified" to jsonBoolean(value.actualPreviewVerified),
            "developerWidgetVerified" to jsonBoolean(value.developerWidgetVerified),
            "nativeBlurVerified" to jsonBoolean(value.nativeBlurVerified),
            "geometryVerified" to jsonBoolean(value.geometryVerified),
            "visibilityVerified" to jsonBoolean(value.visibilityVerified),
            "paddingVerified" to jsonBoolean(value.paddingVerified),
            "alignmentVerified" to jsonBoolean(value.alignmentVerified),
            "radiusVerified" to jsonBoolean(value.radiusVerified),
            "progressVerified" to jsonBoolean(value.progressVerified),
            "contentStatesVerified" to jsonBoolean(value.contentStatesVerified),
            "systemUiHostVerified" to jsonBoolean(value.systemUiHostVerified),
            "snapshotVerified" to jsonBoolean(value.snapshotVerified),
            "failed" to jsonBoolean(value.failed),
            "notes" to jsonString(value.notes),
            "verifiedAt" to jsonNumber(value.verifiedAt)
        )
    )

    private fun decode(contents: String): WidgetParityVerification {
        val json = SimpleJson.parse(contents).asObject("root")
        val schema = json.required("schemaVersion").asInt("schemaVersion")
        if (schema !in 1..PARITY_VERIFICATION_SCHEMA_VERSION) {
            throw JsonValueException("Unsupported verification schema version: $schema")
        }
        val variantText = json.required("variantId").asString("variantId")
        val variant = WidgetVariantId.parse(variantText)
            ?: throw JsonValueException("Invalid verification variant: $variantText")
        return WidgetParityVerification(
            schemaVersion = PARITY_VERIFICATION_SCHEMA_VERSION,
            widgetId = json.required("widgetId").asString("widgetId"),
            variantId = variant,
            designRevision = json.required("designRevision").asLong("designRevision"),
            deviceManufacturer = json.required("deviceManufacturer").asString("deviceManufacturer"),
            deviceModel = json.required("deviceModel").asString("deviceModel"),
            androidVersion = json.required("androidVersion").asString("androidVersion"),
            androidSdkInt = json.required("androidSdkInt").asInt("androidSdkInt"),
            oneUiVersion = json.required("oneUiVersion").asString("oneUiVersion"),
            launcherVersion = json.required("launcherVersion").asString("launcherVersion"),
            actualPreviewVerified = json.required("actualPreviewVerified").asBoolean("actualPreviewVerified"),
            developerWidgetVerified = json.required("developerWidgetVerified").asBoolean("developerWidgetVerified"),
            nativeBlurVerified = json.required("nativeBlurVerified").asBoolean("nativeBlurVerified"),
            geometryVerified = json.required("geometryVerified").asBoolean("geometryVerified"),
            visibilityVerified = json.required("visibilityVerified").asBoolean("visibilityVerified"),
            paddingVerified = json.required("paddingVerified").asBoolean("paddingVerified"),
            alignmentVerified = json.required("alignmentVerified").asBoolean("alignmentVerified"),
            radiusVerified = json.required("radiusVerified").asBoolean("radiusVerified"),
            progressVerified = json.required("progressVerified").asBoolean("progressVerified"),
            contentStatesVerified = if (schema >= 2) {
                json.required("contentStatesVerified").asBoolean("contentStatesVerified")
            } else {
                json.optional("contentStatesVerified")?.asBoolean("contentStatesVerified") ?: false
            },
            systemUiHostVerified = if (schema >= 3) {
                json.required("systemUiHostVerified").asBoolean("systemUiHostVerified")
            } else {
                json.optional("systemUiHostVerified")?.asBoolean("systemUiHostVerified") ?: false
            },
            snapshotVerified = json.required("snapshotVerified").asBoolean("snapshotVerified"),
            failed = json.required("failed").asBoolean("failed"),
            notes = json.required("notes").asString("notes"),
            verifiedAt = json.required("verifiedAt").asLong("verifiedAt")
        ).also { validationError(it)?.let { message -> throw JsonValueException(message) } }
    }

    private fun validationError(value: WidgetParityVerification): String? = when {
        value.schemaVersion != PARITY_VERIFICATION_SCHEMA_VERSION -> "Unsupported verification schema"
        !validIdentity(value.widgetId, value.variantId, value.designRevision) -> "Invalid verification identity"
        value.androidSdkInt < 0 -> "Android SDK must not be negative"
        value.verifiedAt < 0L -> "Verification time must not be negative"
        else -> null
    }

    private fun validIdentity(widgetId: String, variantId: WidgetVariantId, revision: Long): Boolean =
        safeStudioId(widgetId) && variantId.isValid() && revision >= 0L

    private fun file(value: WidgetParityVerification): File =
        file(value.widgetId, value.variantId, value.designRevision)

    private fun file(widgetId: String, variantId: WidgetVariantId, revision: Long): File =
        File(directory, "${prefix(widgetId, variantId)}$revision.json")

    private fun prefix(widgetId: String, variantId: WidgetVariantId): String =
        "${widgetId}__${variantId.stableId.replace(':', '_')}__rev_"

    private fun <T> invalidIdentity(): StudioRepositoryResult<T> =
        failure(StudioRepositoryErrorKind.INVALID_ID, "Invalid verification identity")

    private fun <T> failure(kind: StudioRepositoryErrorKind, message: String): StudioRepositoryResult<T> =
        StudioRepositoryResult.Failure(StudioRepositoryError(kind, message))

    companion object {
        const val DIRECTORY = "widget_studio/verifications"
    }
}
