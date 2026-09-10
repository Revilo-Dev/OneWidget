package dev.revilodev.onewidget.studio.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteViewsPreviewHostTest {
    @Test fun `apply guard returns successful value`() {
        val result = RemoteViewsApplyGuard.run { "applied" }
        assertTrue(result is RemoteViewsApplyAttempt.Success)
        assertEquals("applied", (result as RemoteViewsApplyAttempt.Success).value)
    }

    @Test fun `apply guard converts runtime failure to diagnostics`() {
        val result = RemoteViewsApplyGuard.run<String> { throw IllegalStateException("inflate failed") }
        assertTrue(result is RemoteViewsApplyAttempt.Failure)
        assertEquals("inflate failed", (result as RemoteViewsApplyAttempt.Failure).message)
    }
}
