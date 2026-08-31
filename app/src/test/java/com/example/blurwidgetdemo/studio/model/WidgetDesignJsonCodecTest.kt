package com.example.blurwidgetdemo.studio.model

import com.example.blurwidgetdemo.studio.studioTestDesign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDesignJsonCodecTest {
    private val codec = WidgetDesignJsonCodec()

    @Test fun `schema version 1 design round trips`() {
        val variant = WidgetVariantId.Standard(2, 1)
        val source = studioTestDesign().copy(
            variants = mapOf(
                variant to WidgetVariantOverride(
                    mapOf(
                        "test.primary" to WidgetElementOverride(
                            visible = false,
                            xDp = 18f,
                            anchor = WidgetAnchor.BOTTOM_CENTER,
                            widthMode = DimensionMode.MATCH
                        )
                    )
                )
            )
        )
        val encoded = codec.encode(source)
        val decoded = codec.decode(encoded)
        assertTrue(decoded is DesignDecodeResult.Success)
        assertEquals(source, (decoded as DesignDecodeResult.Success).design)
        assertTrue(encoded.contains("\"schemaVersion\": 1"))
        assertTrue(encoded.contains("\"anchor\": \"BOTTOM_CENTER\""))
        assertTrue(encoded.contains("\"widthMode\": \"MATCH\""))
    }

    @Test fun `malformed JSON returns a decode error`() {
        val result = codec.decode("{not-json")
        assertTrue(result is DesignDecodeResult.Failure)
        assertEquals(
            DesignDecodeErrorKind.MALFORMED_JSON,
            (result as DesignDecodeResult.Failure).error.kind
        )
    }

    @Test fun `newer schema is rejected without reinterpretation`() {
        val json = codec.encode(studioTestDesign())
            .replaceFirst("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        val result = codec.decode(json)
        assertTrue(result is DesignDecodeResult.Failure)
        assertEquals(
            DesignDecodeErrorKind.UNSUPPORTED_SCHEMA,
            (result as DesignDecodeResult.Failure).error.kind
        )
    }

    @Test fun `unknown fields are ignored`() {
        val json = codec.encode(studioTestDesign())
            .replaceFirst("{", "{\n  \"futureField\": \"future-value\",")
        val result = codec.decode(json)
        assertTrue(result is DesignDecodeResult.Success)
        assertEquals(studioTestDesign(), (result as DesignDecodeResult.Success).design)
    }

    @Test fun `invalid persisted enum returns a decode error`() {
        val json = codec.encode(studioTestDesign())
            .replaceFirst("\"anchor\": \"CENTER\"", "\"anchor\": \"NOT_AN_ANCHOR\"")
        val result = codec.decode(json)
        assertTrue(result is DesignDecodeResult.Failure)
        assertEquals(
            DesignDecodeErrorKind.INVALID_VALUE,
            (result as DesignDecodeResult.Failure).error.kind
        )
    }
}
