package dev.revilodev.onewidget.studio.production

import dev.revilodev.onewidget.studio.clock.ClockStudioDescriptor
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor
import dev.revilodev.onewidget.studio.calendar.CalendarStudioDescriptor
import dev.revilodev.onewidget.studio.model.WidgetElementSpec
import dev.revilodev.onewidget.studio.verification.WidgetVerificationStatus
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds
import dev.revilodev.onewidget.studio.universal.UniversalWidgetStudioCatalog
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WidgetPromotionCoordinatorTest {
    private lateinit var root: File
    private lateinit var repository: ProductionWidgetDesignRepository
    private lateinit var coordinator: WidgetPromotionCoordinator

    @Before fun setUp() {
        root = Files.createTempDirectory("promotion-coordinator").toFile()
        repository = ProductionWidgetDesignRepository(root)
        coordinator = WidgetPromotionCoordinator(repository) { 500L }
    }

    @After fun tearDown() {
        root.deleteRecursively()
    }

    @Test fun `verified promotion with current snapshot succeeds`() {
        val result = coordinator.promote(
            request(WidgetVerificationStatus.VERIFIED, PromotionSnapshotStatus.CURRENT),
            allowIncompleteVerification = false,
            allowSnapshotWarning = false
        )
        assertTrue(result is PromotionResult.Success)
    }

    @Test fun `incomplete verification requires explicit override`() {
        val request = request(WidgetVerificationStatus.PARTIALLY_VERIFIED, PromotionSnapshotStatus.CURRENT)
        assertTrue(coordinator.promote(request, false, false) is PromotionResult.Rejected)
        assertTrue(coordinator.promote(request, true, false) is PromotionResult.Success)
    }

    @Test fun `missing and stale snapshots each require explicit override`() {
        val missing = request(WidgetVerificationStatus.VERIFIED, PromotionSnapshotStatus.MISSING)
        val stale = request(WidgetVerificationStatus.VERIFIED, PromotionSnapshotStatus.STALE)
        assertTrue(coordinator.promote(missing, false, false) is PromotionResult.Rejected)
        assertTrue(coordinator.promote(missing, false, true) is PromotionResult.Success)
        assertTrue(coordinator.promote(stale, false, false) is PromotionResult.Rejected)
        assertTrue(coordinator.promote(stale, false, true) is PromotionResult.Success)
    }

    @Test fun `invalid design is rejected regardless of override`() {
        val invalid = request(WidgetVerificationStatus.VERIFIED, PromotionSnapshotStatus.CURRENT).copy(
            design = ClockStudioDescriptor.defaultDesign().copy(widgetId = "", revision = 1L)
        )
        assertTrue(coordinator.promote(invalid, true, true) is PromotionResult.Rejected)
    }

    @Test fun `mapped element without renderer binding is rejected`() {
        val base = request(WidgetVerificationStatus.VERIFIED, PromotionSnapshotStatus.CURRENT)
        val unsupported = base.copy(
            design = base.design.copy(
                baseVariant = base.design.baseVariant.copy(
                    elements = base.design.baseVariant.elements + WidgetElementSpec("clock.unsupported")
                )
            )
        )

        assertTrue(coordinator.promote(unsupported, true, true) is PromotionResult.Rejected)
    }

    @Test fun `storage is promotable and reports its pre 31 radius fallback`() {
        val design = StorageStudioDescriptor.defaultDesign().copy(revision = 2L)
        val review = WidgetPromotionReviewer.review(
            PromotionRequest(
                descriptor = StorageStudioDescriptor.create(),
                design = design,
                verificationStatus = WidgetVerificationStatus.VERIFIED,
                snapshotStatus = PromotionSnapshotStatus.CURRENT,
                previousProductionDesign = null,
                androidSdkInt = 30
            )
        )

        assertTrue(review.blockingErrors.isEmpty())
        assertTrue(review.compatibilityWarnings.any { it.contains("Storage progress corner radius") })
    }

    @Test fun `calendar renderer and production variants pass promotion review`() {
        val design = CalendarStudioDescriptor.defaultDesign().copy(revision = 3L)
        val review = WidgetPromotionReviewer.review(
            PromotionRequest(
                descriptor = CalendarStudioDescriptor.create(),
                design = design,
                verificationStatus = WidgetVerificationStatus.VERIFIED,
                snapshotStatus = PromotionSnapshotStatus.CURRENT,
                previousProductionDesign = null,
                androidSdkInt = 36
            )
        )

        assertTrue(review.blockingErrors.isEmpty())
        assertTrue(review.mapping.isNotEmpty())
    }

    @Test fun `universal home widgets promote while system ui widgets are explicitly blocked`() {
        val weather = requireNotNull(UniversalWidgetStudioCatalog.definition(UniversalWidgetIds.WEATHER))
        val weatherReview = WidgetPromotionReviewer.review(
            PromotionRequest(
                descriptor = weather.createDescriptor(),
                design = weather.defaultDesign().copy(revision = 4L),
                verificationStatus = WidgetVerificationStatus.VERIFIED,
                snapshotStatus = PromotionSnapshotStatus.CURRENT,
                previousProductionDesign = null,
                androidSdkInt = 36
            )
        )
        assertTrue(weatherReview.blockingErrors.isEmpty())

        val lock = requireNotNull(
            UniversalWidgetStudioCatalog.definition(UniversalWidgetIds.LOCKSCREEN_MESSAGE_SMALL)
        )
        val lockReview = WidgetPromotionReviewer.review(
            PromotionRequest(
                descriptor = lock.createDescriptor(),
                design = lock.defaultDesign().copy(revision = 4L),
                verificationStatus = WidgetVerificationStatus.VERIFIED,
                snapshotStatus = PromotionSnapshotStatus.CURRENT,
                previousProductionDesign = null,
                androidSdkInt = 36
            )
        )
        assertTrue(lockReview.blockingErrors.any { it.contains("System UI-hosted") })
    }

    private fun request(
        verification: WidgetVerificationStatus,
        snapshot: PromotionSnapshotStatus
    ) = PromotionRequest(
        descriptor = ClockStudioDescriptor.create(),
        design = ClockStudioDescriptor.defaultDesign().copy(revision = 1L),
        verificationStatus = verification,
        snapshotStatus = snapshot,
        previousProductionDesign = null,
        androidSdkInt = 36
    )
}
