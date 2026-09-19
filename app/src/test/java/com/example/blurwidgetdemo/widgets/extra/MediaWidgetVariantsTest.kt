package dev.revilodev.onewidget.widgets.extra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaWidgetVariantsTest {
    @Test
    fun `gallery sizes resolve to distinct provider identities`() {
        assertEquals(MediaPlayer3x1Widget::class.java, MediaWidgetVariants.providerFor(3, 1))
        assertEquals(MediaPlayer2x2Widget::class.java, MediaWidgetVariants.providerFor(2, 2))
        assertEquals(MediaPlayer3x2Widget::class.java, MediaWidgetVariants.providerFor(3, 2))
    }

    @Test
    fun `all media provider identities are recognized`() {
        assertTrue(MediaWidgetVariants.providerClasses.all { provider ->
            MediaWidgetVariants.isMediaProvider(provider.name)
        })
    }
}
