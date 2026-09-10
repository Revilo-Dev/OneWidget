package dev.revilodev.onewidget.studio.production

import android.content.Context
import dev.revilodev.onewidget.studio.data.StudioRepositoryError
import dev.revilodev.onewidget.studio.data.StudioRepositoryErrorKind
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.snapshot.SnapshotLookupResult
import dev.revilodev.onewidget.studio.snapshot.SnapshotScope
import dev.revilodev.onewidget.studio.snapshot.SnapshotStatus
import dev.revilodev.onewidget.studio.snapshot.WidgetSnapshotMetadata
import dev.revilodev.onewidget.studio.snapshot.WidgetSnapshotRepository
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class ProductionSnapshotPromotionReport(
    val promotedVariants: List<WidgetVariantId>,
    val missingVariants: List<WidgetVariantId>,
    val staleVariants: List<WidgetVariantId>
)

/** Production PNGs are copied out of mutable developer snapshot storage. */
class ProductionWidgetSnapshotRepository(
    private val repository: WidgetSnapshotRepository
) {
    constructor(context: Context) : this(
        WidgetSnapshotRepository(File(context.filesDir, METADATA_DIRECTORY))
    )

    fun status(widgetId: String, variantId: WidgetVariantId, activeRevision: Long): SnapshotStatus =
        repository.status(widgetId, variantId, activeRevision)

    fun promoteCurrent(
        widgetId: String,
        designRevision: Long,
        variants: Collection<WidgetVariantId>,
        developerRepository: WidgetSnapshotRepository
    ): StudioRepositoryResult<ProductionSnapshotPromotionReport> {
        val promoted = mutableListOf<WidgetVariantId>()
        val missing = mutableListOf<WidgetVariantId>()
        val stale = mutableListOf<WidgetVariantId>()
        variants.distinct().forEach { variant ->
            when (val status = developerRepository.status(widgetId, variant, designRevision)) {
                is SnapshotStatus.Current -> when (val copied = copy(status.snapshot)) {
                    is StudioRepositoryResult.Success -> promoted += variant
                    is StudioRepositoryResult.Failure -> return copied
                }
                is SnapshotStatus.Stale -> stale += variant
                SnapshotStatus.Missing -> missing += variant
            }
        }
        return StudioRepositoryResult.Success(
            ProductionSnapshotPromotionReport(promoted, missing, stale)
        )
    }

    private fun copy(source: SnapshotLookupResult.Available): StudioRepositoryResult<WidgetSnapshotMetadata> {
        val sourceMetadata = source.metadata
        val destinationName = dev.revilodev.onewidget.studio.snapshot.SnapshotFileNames.image(
            sourceMetadata.widgetId,
            sourceMetadata.variantId,
            sourceMetadata.designRevision
        )
        val destinationDirectory = repository.imagesDirectory
        val destination = File(destinationDirectory, destinationName)
        val temporary = File(destinationDirectory, ".$destinationName.tmp")
        return try {
            if (!destinationDirectory.exists() && !destinationDirectory.mkdirs()) {
                throw IOException("Unable to create production snapshot directory")
            }
            Files.copy(source.file.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            val productionMetadata = sourceMetadata.copy(
                snapshotPath = repository.relativeImagePath(destinationName),
                scope = SnapshotScope.PRODUCTION
            )
            when (val registered = repository.register(productionMetadata)) {
                is StudioRepositoryResult.Success -> registered
                is StudioRepositoryResult.Failure -> {
                    destination.delete()
                    registered
                }
            }
        } catch (error: IOException) {
            temporary.delete()
            StudioRepositoryResult.Failure(
                StudioRepositoryError(
                    StudioRepositoryErrorKind.IO_ERROR,
                    error.message ?: "Unable to copy production snapshot"
                )
            )
        }
    }

    companion object {
        const val ROOT_DIRECTORY = "widget_designs/production_snapshots"
        const val METADATA_DIRECTORY = "$ROOT_DIRECTORY/metadata"
    }
}
