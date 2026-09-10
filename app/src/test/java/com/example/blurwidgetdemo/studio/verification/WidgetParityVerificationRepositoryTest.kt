package dev.revilodev.onewidget.studio.verification

import dev.revilodev.onewidget.studio.battery.BatteryStudioDescriptor
import dev.revilodev.onewidget.studio.clock.ClockStudioDescriptor
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor
import dev.revilodev.onewidget.studio.calendar.CalendarStudioDescriptor
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class WidgetParityVerificationRepositoryTest {
    private lateinit var root: File
    private lateinit var repository: WidgetParityVerificationRepository
    private val variant = WidgetVariantId.Standard(2, 1)

    @Before fun setUp() {
        root = Files.createTempDirectory("parity-verifications").toFile()
        repository = WidgetParityVerificationRepository(root)
    }

    @After fun tearDown() {
        root.deleteRecursively()
    }

    @Test fun `save and load preserve revision-bound human readable data`() {
        val value = completeClock(revision = 12L).copy(notes = "One UI Home checked")

        assertTrue(repository.save(value) is StudioRepositoryResult.Success)
        assertEquals(value, success(repository.load(value.widgetId, variant, 12L)))
        assertTrue(root.listFiles().orEmpty().single().readText().contains("One UI Home checked"))
    }

    @Test fun `revision match is current and edit makes old verification stale`() {
        repository.save(completeClock(revision = 12L))

        assertTrue(repository.status(ClockStudioDescriptor.WIDGET_ID, variant, 12L) is VerificationRevisionStatus.Current)
        val stale = repository.status(ClockStudioDescriptor.WIDGET_ID, variant, 13L)
        assertTrue(stale is VerificationRevisionStatus.Stale)
        assertEquals(12L, (stale as VerificationRevisionStatus.Stale).verification.designRevision)
    }

    @Test fun `schema one verification migrates with calendar content check unset`() {
        val value = completeClock(revision = 14L)
        repository.save(value)
        val file = root.listFiles().orEmpty().single()
        file.writeText(
            file.readText()
                .replace("\"schemaVersion\": 3", "\"schemaVersion\": 1")
                .replace("  \"contentStatesVerified\": false,\n", "")
                .replace("  \"systemUiHostVerified\": false,\n", "")
        )

        val loaded = success(repository.load(value.widgetId, variant, 14L))
        assertEquals(PARITY_VERIFICATION_SCHEMA_VERSION, loaded.schemaVersion)
        assertEquals(false, loaded.contentStatesVerified)
        assertEquals(false, loaded.systemUiHostVerified)
    }

    @Test fun `schema two verification migrates with system ui host check unset`() {
        val value = completeClock(revision = 15L).copy(contentStatesVerified = true)
        repository.save(value)
        val file = root.listFiles().orEmpty().single()
        file.writeText(
            file.readText()
                .replace("\"schemaVersion\": 3", "\"schemaVersion\": 2")
                .replace("  \"systemUiHostVerified\": false,\n", "")
        )

        val loaded = success(repository.load(value.widgetId, variant, 15L))
        assertEquals(PARITY_VERIFICATION_SCHEMA_VERSION, loaded.schemaVersion)
        assertTrue(loaded.contentStatesVerified)
        assertEquals(false, loaded.systemUiHostVerified)
    }

    @Test fun `complete clock battery and storage policies require renderer-specific checks`() {
        assertEquals(
            WidgetVerificationStatus.VERIFIED,
            WidgetParityVerificationPolicy.assess(completeClock(1L)).status
        )
        val battery = completeClock(1L).copy(
            widgetId = BatteryStudioDescriptor.WIDGET_ID,
            progressVerified = true,
            radiusVerified = true,
            androidSdkInt = 36
        )
        assertEquals(WidgetVerificationStatus.VERIFIED, WidgetParityVerificationPolicy.assess(battery).status)
        assertEquals(
            WidgetVerificationStatus.PARTIALLY_VERIFIED,
            WidgetParityVerificationPolicy.assess(battery.copy(radiusVerified = false)).status
        )
        val storage = battery.copy(widgetId = StorageStudioDescriptor.WIDGET_ID)
        assertEquals(WidgetVerificationStatus.VERIFIED, WidgetParityVerificationPolicy.assess(storage).status)
        assertEquals(
            WidgetVerificationStatus.PARTIALLY_VERIFIED,
            WidgetParityVerificationPolicy.assess(storage.copy(progressVerified = false)).status
        )
        val calendar = completeClock(1L).copy(
            widgetId = CalendarStudioDescriptor.WIDGET_ID,
            contentStatesVerified = true
        )
        assertEquals(WidgetVerificationStatus.VERIFIED, WidgetParityVerificationPolicy.assess(calendar).status)
        assertEquals(
            WidgetVerificationStatus.PARTIALLY_VERIFIED,
            WidgetParityVerificationPolicy.assess(calendar.copy(contentStatesVerified = false)).status
        )
        val weather = completeClock(1L).copy(
            widgetId = UniversalWidgetIds.WEATHER,
            contentStatesVerified = true
        )
        assertEquals(WidgetVerificationStatus.VERIFIED, WidgetParityVerificationPolicy.assess(weather).status)
        assertEquals(
            WidgetVerificationStatus.PARTIALLY_VERIFIED,
            WidgetParityVerificationPolicy.assess(weather.copy(contentStatesVerified = false)).status
        )
        val lockScreen = completeClock(1L).copy(
            widgetId = UniversalWidgetIds.LOCKSCREEN_MESSAGE_SMALL,
            nativeBlurVerified = false,
            systemUiHostVerified = true
        )
        assertEquals(WidgetVerificationStatus.VERIFIED, WidgetParityVerificationPolicy.assess(lockScreen).status)
        assertEquals(
            WidgetVerificationStatus.PARTIALLY_VERIFIED,
            WidgetParityVerificationPolicy.assess(lockScreen.copy(systemUiHostVerified = false)).status
        )
    }

    @Test fun `partial failed and not tested statuses remain distinct`() {
        val blank = WidgetParityVerification(
            widgetId = ClockStudioDescriptor.WIDGET_ID,
            variantId = variant,
            designRevision = 4L
        )
        assertEquals(WidgetVerificationStatus.NOT_TESTED, WidgetParityVerificationPolicy.assess(blank).status)
        assertEquals(
            WidgetVerificationStatus.PARTIALLY_VERIFIED,
            WidgetParityVerificationPolicy.assess(blank.copy(geometryVerified = true)).status
        )
        assertEquals(
            WidgetVerificationStatus.FAILED,
            WidgetParityVerificationPolicy.assess(blank.copy(failed = true)).status
        )
    }

    @Test fun `delete and verified revision listing are supported`() {
        repository.save(completeClock(2L))
        repository.save(completeClock(3L).copy(snapshotVerified = false))

        assertEquals(listOf(2L), repository.listVerifiedRevisions(ClockStudioDescriptor.WIDGET_ID, variant))
        assertEquals(true, success(repository.delete(ClockStudioDescriptor.WIDGET_ID, variant, 2L)))
    }

    private fun completeClock(revision: Long) = WidgetParityVerification(
        widgetId = ClockStudioDescriptor.WIDGET_ID,
        variantId = variant,
        designRevision = revision,
        androidSdkInt = 36,
        developerWidgetVerified = true,
        nativeBlurVerified = true,
        geometryVerified = true,
        visibilityVerified = true,
        paddingVerified = true,
        alignmentVerified = true,
        snapshotVerified = true,
        verifiedAt = 100L
    )

    private fun <T> success(result: StudioRepositoryResult<T>): T {
        assertTrue(result is StudioRepositoryResult.Success)
        return (result as StudioRepositoryResult.Success).value
    }
}
