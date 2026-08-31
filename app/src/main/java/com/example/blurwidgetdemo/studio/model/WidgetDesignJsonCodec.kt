package com.example.blurwidgetdemo.studio.model

import com.example.blurwidgetdemo.studio.json.JsonArrayValue
import com.example.blurwidgetdemo.studio.json.JsonObjectValue
import com.example.blurwidgetdemo.studio.json.JsonParseException
import com.example.blurwidgetdemo.studio.json.JsonValue
import com.example.blurwidgetdemo.studio.json.JsonValueException
import com.example.blurwidgetdemo.studio.json.SimpleJson
import com.example.blurwidgetdemo.studio.json.asArray
import com.example.blurwidgetdemo.studio.json.asBoolean
import com.example.blurwidgetdemo.studio.json.asFloat
import com.example.blurwidgetdemo.studio.json.asInt
import com.example.blurwidgetdemo.studio.json.asLong
import com.example.blurwidgetdemo.studio.json.asObject
import com.example.blurwidgetdemo.studio.json.asString
import com.example.blurwidgetdemo.studio.json.jsonArray
import com.example.blurwidgetdemo.studio.json.jsonBoolean
import com.example.blurwidgetdemo.studio.json.jsonNumber
import com.example.blurwidgetdemo.studio.json.jsonObject
import com.example.blurwidgetdemo.studio.json.jsonString
import com.example.blurwidgetdemo.studio.json.optional
import com.example.blurwidgetdemo.studio.json.required

enum class DesignDecodeErrorKind {
    MALFORMED_JSON,
    INVALID_VALUE,
    UNSUPPORTED_SCHEMA
}

data class DesignDecodeError(
    val kind: DesignDecodeErrorKind,
    val message: String
)

sealed class DesignDecodeResult {
    data class Success(val design: WidgetDesignSpec) : DesignDecodeResult()
    data class Failure(val error: DesignDecodeError) : DesignDecodeResult()
}

/** Versioned, human-readable JSON for developer design files. Unknown fields are ignored. */
class WidgetDesignJsonCodec {
    fun encode(design: WidgetDesignSpec): String = SimpleJson.stringify(
        jsonObject(
            "schemaVersion" to jsonNumber(design.schemaVersion),
            "widgetId" to jsonString(design.widgetId),
            "revision" to jsonNumber(design.revision),
            "baseVariant" to encodeVariantSpec(design.baseVariant),
            "variants" to jsonArray(
                design.variants.entries.sortedBy { it.key.stableId }.map { (variantId, variantOverride) ->
                    jsonObject(
                        "variantId" to jsonString(variantId.stableId),
                        "elements" to jsonArray(
                            variantOverride.elementOverrides.entries.sortedBy { it.key }
                                .map { (elementId, elementOverride) ->
                                    encodeElementOverride(elementId, elementOverride)
                                }
                        )
                    )
                }
            )
        )
    )

    fun decode(json: String): DesignDecodeResult {
        val root = try {
            SimpleJson.parse(json).asObject("root")
        } catch (error: JsonParseException) {
            return failure(DesignDecodeErrorKind.MALFORMED_JSON, error)
        } catch (error: JsonValueException) {
            return failure(DesignDecodeErrorKind.INVALID_VALUE, error)
        }

        val schemaVersion = try {
            root.required("schemaVersion").asInt("schemaVersion")
        } catch (error: JsonValueException) {
            return failure(DesignDecodeErrorKind.INVALID_VALUE, error)
        }
        if (schemaVersion != WIDGET_DESIGN_SCHEMA_VERSION) {
            return DesignDecodeResult.Failure(
                DesignDecodeError(
                    DesignDecodeErrorKind.UNSUPPORTED_SCHEMA,
                    "Unsupported widget design schema version: $schemaVersion"
                )
            )
        }

        return try {
            val variants = linkedMapOf<WidgetVariantId, WidgetVariantOverride>()
            val variantsJson = root.optional("variants")?.asArray("variants") ?: JsonArrayValue(emptyList())
            variantsJson.values.forEachIndexed { index, value ->
                val variantJson = value.asObject("variants[$index]")
                val stableId = variantJson.required("variantId").asString("variants[$index].variantId")
                val variantId = WidgetVariantId.parse(stableId)
                    ?: throw JsonValueException("Invalid variant ID: $stableId")
                if (variants.containsKey(variantId)) {
                    throw JsonValueException("Duplicate variant ID: $stableId")
                }
                val overrides = linkedMapOf<String, WidgetElementOverride>()
                val elementsJson = variantJson.optional("elements")?.asArray("variants[$index].elements")
                    ?: JsonArrayValue(emptyList())
                elementsJson.values.forEachIndexed { elementIndex, elementValue ->
                    val field = "variants[$index].elements[$elementIndex]"
                    val elementJson = elementValue.asObject(field)
                    val elementId = elementJson.required("elementId").asString("$field.elementId")
                    if (overrides.containsKey(elementId)) {
                        throw JsonValueException("Duplicate element override ID: $elementId")
                    }
                    overrides[elementId] = decodeElementOverride(elementJson, field)
                }
                variants[variantId] = WidgetVariantOverride(overrides)
            }
            DesignDecodeResult.Success(
                WidgetDesignSpec(
                    schemaVersion = schemaVersion,
                    widgetId = root.required("widgetId").asString("widgetId"),
                    revision = root.optional("revision")?.asLong("revision") ?: 0L,
                    baseVariant = decodeVariantSpec(root.required("baseVariant").asObject("baseVariant")),
                    variants = variants
                )
            )
        } catch (error: JsonValueException) {
            failure(DesignDecodeErrorKind.INVALID_VALUE, error)
        } catch (error: IllegalArgumentException) {
            failure(DesignDecodeErrorKind.INVALID_VALUE, error)
        }
    }

    private fun encodeVariantSpec(spec: WidgetVariantSpec): JsonObjectValue = jsonObject(
        "elements" to jsonArray(spec.elements.map(::encodeElement))
    )

    private fun encodeElement(element: WidgetElementSpec): JsonObjectValue = jsonObject(
        "elementId" to jsonString(element.elementId),
        "visible" to jsonBoolean(element.visible),
        "xDp" to jsonNumber(element.xDp),
        "yDp" to jsonNumber(element.yDp),
        "widthMode" to jsonString(element.widthMode.name),
        "heightMode" to jsonString(element.heightMode.name),
        "widthDp" to jsonNumber(element.widthDp),
        "heightDp" to jsonNumber(element.heightDp),
        "paddingStartDp" to jsonNumber(element.paddingStartDp),
        "paddingTopDp" to jsonNumber(element.paddingTopDp),
        "paddingEndDp" to jsonNumber(element.paddingEndDp),
        "paddingBottomDp" to jsonNumber(element.paddingBottomDp),
        "anchor" to jsonString(element.anchor.name),
        "horizontalAlignment" to jsonString(element.horizontalAlignment.name),
        "verticalAlignment" to jsonString(element.verticalAlignment.name),
        "zIndex" to jsonNumber(element.zIndex),
        "cornerRadiusDp" to jsonNumber(element.cornerRadiusDp)
    )

    private fun decodeVariantSpec(json: JsonObjectValue): WidgetVariantSpec {
        val elements = json.required("elements").asArray("baseVariant.elements").values.mapIndexed { index, value ->
            decodeElement(value.asObject("baseVariant.elements[$index]"), "baseVariant.elements[$index]")
        }
        return WidgetVariantSpec(elements)
    }

    private fun decodeElement(json: JsonObjectValue, field: String): WidgetElementSpec = WidgetElementSpec(
        elementId = json.required("elementId").asString("$field.elementId"),
        visible = json.required("visible").asBoolean("$field.visible"),
        xDp = json.required("xDp").asFloat("$field.xDp"),
        yDp = json.required("yDp").asFloat("$field.yDp"),
        widthMode = enumValue(json.required("widthMode").asString("$field.widthMode"), "$field.widthMode"),
        heightMode = enumValue(json.required("heightMode").asString("$field.heightMode"), "$field.heightMode"),
        widthDp = json.required("widthDp").asFloat("$field.widthDp"),
        heightDp = json.required("heightDp").asFloat("$field.heightDp"),
        paddingStartDp = json.required("paddingStartDp").asFloat("$field.paddingStartDp"),
        paddingTopDp = json.required("paddingTopDp").asFloat("$field.paddingTopDp"),
        paddingEndDp = json.required("paddingEndDp").asFloat("$field.paddingEndDp"),
        paddingBottomDp = json.required("paddingBottomDp").asFloat("$field.paddingBottomDp"),
        anchor = enumValue(json.required("anchor").asString("$field.anchor"), "$field.anchor"),
        horizontalAlignment = enumValue(
            json.required("horizontalAlignment").asString("$field.horizontalAlignment"),
            "$field.horizontalAlignment"
        ),
        verticalAlignment = enumValue(
            json.required("verticalAlignment").asString("$field.verticalAlignment"),
            "$field.verticalAlignment"
        ),
        zIndex = json.required("zIndex").asInt("$field.zIndex"),
        cornerRadiusDp = json.required("cornerRadiusDp").asFloat("$field.cornerRadiusDp")
    )

    private fun encodeElementOverride(elementId: String, override: WidgetElementOverride): JsonObjectValue {
        val values = linkedMapOf<String, JsonValue>("elementId" to jsonString(elementId))
        override.visible?.let { values["visible"] = jsonBoolean(it) }
        override.xDp?.let { values["xDp"] = jsonNumber(it) }
        override.yDp?.let { values["yDp"] = jsonNumber(it) }
        override.widthMode?.let { values["widthMode"] = jsonString(it.name) }
        override.heightMode?.let { values["heightMode"] = jsonString(it.name) }
        override.widthDp?.let { values["widthDp"] = jsonNumber(it) }
        override.heightDp?.let { values["heightDp"] = jsonNumber(it) }
        override.paddingStartDp?.let { values["paddingStartDp"] = jsonNumber(it) }
        override.paddingTopDp?.let { values["paddingTopDp"] = jsonNumber(it) }
        override.paddingEndDp?.let { values["paddingEndDp"] = jsonNumber(it) }
        override.paddingBottomDp?.let { values["paddingBottomDp"] = jsonNumber(it) }
        override.anchor?.let { values["anchor"] = jsonString(it.name) }
        override.horizontalAlignment?.let { values["horizontalAlignment"] = jsonString(it.name) }
        override.verticalAlignment?.let { values["verticalAlignment"] = jsonString(it.name) }
        override.zIndex?.let { values["zIndex"] = jsonNumber(it) }
        override.cornerRadiusDp?.let { values["cornerRadiusDp"] = jsonNumber(it) }
        return JsonObjectValue(values)
    }

    private fun decodeElementOverride(json: JsonObjectValue, field: String): WidgetElementOverride =
        WidgetElementOverride(
            visible = json.optionalBoolean("visible", field),
            xDp = json.optionalFloat("xDp", field),
            yDp = json.optionalFloat("yDp", field),
            widthMode = json.optionalEnum<DimensionMode>("widthMode", field),
            heightMode = json.optionalEnum<DimensionMode>("heightMode", field),
            widthDp = json.optionalFloat("widthDp", field),
            heightDp = json.optionalFloat("heightDp", field),
            paddingStartDp = json.optionalFloat("paddingStartDp", field),
            paddingTopDp = json.optionalFloat("paddingTopDp", field),
            paddingEndDp = json.optionalFloat("paddingEndDp", field),
            paddingBottomDp = json.optionalFloat("paddingBottomDp", field),
            anchor = json.optionalEnum<WidgetAnchor>("anchor", field),
            horizontalAlignment = json.optionalEnum<HorizontalAlignment>("horizontalAlignment", field),
            verticalAlignment = json.optionalEnum<VerticalAlignment>("verticalAlignment", field),
            zIndex = json.optionalInt("zIndex", field),
            cornerRadiusDp = json.optionalFloat("cornerRadiusDp", field)
        )

    private fun JsonObjectValue.optionalBoolean(name: String, field: String): Boolean? =
        optional(name)?.asBoolean("$field.$name")

    private fun JsonObjectValue.optionalFloat(name: String, field: String): Float? =
        optional(name)?.asFloat("$field.$name")

    private fun JsonObjectValue.optionalInt(name: String, field: String): Int? =
        optional(name)?.asInt("$field.$name")

    private inline fun <reified T : Enum<T>> JsonObjectValue.optionalEnum(name: String, field: String): T? =
        optional(name)?.asString("$field.$name")?.let { enumValue<T>(it, "$field.$name") }

    private inline fun <reified T : Enum<T>> enumValue(value: String, field: String): T =
        enumValues<T>().firstOrNull { it.name == value }
            ?: throw JsonValueException("Invalid $field enum value: $value")

    private fun failure(kind: DesignDecodeErrorKind, error: Exception): DesignDecodeResult.Failure =
        DesignDecodeResult.Failure(
            DesignDecodeError(kind, error.message ?: "Invalid JSON value")
        )
}
