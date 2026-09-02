package com.example.blurwidgetdemo.studio.snapshot

import android.graphics.Bitmap
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

sealed class SnapshotSaveResult {
    data class Success(val metadata: WidgetSnapshotMetadata, val file: File) : SnapshotSaveResult()
    data class Failure(val message: String) : SnapshotSaveResult()
}

/** PNG compression and atomic file/metadata commit. Call from a background thread. */
class WidgetSnapshotStorage(
    private val repository: WidgetSnapshotRepository,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    fun save(capture: SnapshotCaptureResult.Success): SnapshotSaveResult {
        val request = capture.request
        val createdAt = currentTimeMillis()
        val fileName = try {
            SnapshotFileNames.image(request.widgetId, request.variantId, request.designRevision)
        } catch (error: IllegalArgumentException) {
            return SnapshotSaveResult.Failure(error.message ?: "Invalid snapshot identity")
        }
        val imagesDirectory = repository.imagesDirectory
        if (!imagesDirectory.exists() && !imagesDirectory.mkdirs()) {
            return SnapshotSaveResult.Failure("Unable to create snapshot image directory")
        }
        val destination = File(imagesDirectory, fileName)
        val temporary = File(imagesDirectory, ".$fileName.$createdAt.tmp")
        val backup = File(imagesDirectory, ".$fileName.$createdAt.backup.tmp")
        return try {
            val compressed = FileOutputStream(temporary).use { output ->
                capture.bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)
            }
            if (!compressed) throw IOException("PNG encoder rejected the snapshot")
            if (destination.exists()) move(destination, backup)
            move(temporary, destination)
            val metadata = WidgetSnapshotMetadata(
                widgetId = request.widgetId,
                variantId = request.variantId,
                widthDp = request.widthDp,
                heightDp = request.heightDp,
                density = request.density,
                designRevision = request.designRevision,
                createdAt = createdAt,
                snapshotPath = repository.relativeImagePath(fileName),
                widthPx = capture.pixelSize.widthPx,
                heightPx = capture.pixelSize.heightPx,
                scope = SnapshotScope.DEVELOPER,
                renderMode = SnapshotRenderMode.LOCAL_REMOTEVIEWS,
                rendererVersion = capture.rendererVersion
            )
            when (val registered = repository.register(metadata)) {
                is StudioRepositoryResult.Success -> {
                    backup.delete()
                    SnapshotSaveResult.Success(metadata, destination)
                }
                is StudioRepositoryResult.Failure -> {
                    destination.delete()
                    if (backup.exists()) move(backup, destination)
                    SnapshotSaveResult.Failure(registered.error.message)
                }
            }
        } catch (error: IOException) {
            temporary.delete()
            if (!destination.exists() && backup.exists()) runCatching { move(backup, destination) }
            SnapshotSaveResult.Failure(error.message ?: "Unable to save snapshot PNG")
        } catch (error: SecurityException) {
            temporary.delete()
            if (!destination.exists() && backup.exists()) runCatching { move(backup, destination) }
            SnapshotSaveResult.Failure(error.message ?: "Unable to access snapshot storage")
        } finally {
            temporary.delete()
        }
    }

    private fun move(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private companion object {
        const val PNG_QUALITY = 100
    }
}
