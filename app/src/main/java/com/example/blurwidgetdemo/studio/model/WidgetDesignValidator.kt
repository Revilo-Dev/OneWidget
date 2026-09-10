package dev.revilodev.onewidget.studio.model

enum class ValidationSeverity { ERROR, WARNING }

data class ValidationIssue(
    val severity: ValidationSeverity,
    val field: String,
    val message: String
)

data class ValidationResult(val issues: List<ValidationIssue>) {
    val isValid: Boolean get() = issues.none { it.severity == ValidationSeverity.ERROR }
}

object WidgetDesignValidator {
    fun validate(
        design: WidgetDesignSpec,
        requiredElementIds: Set<String> = emptySet()
    ): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        fun error(field: String, message: String) {
            issues += ValidationIssue(ValidationSeverity.ERROR, field, message)
        }

        if (design.schemaVersion != WIDGET_DESIGN_SCHEMA_VERSION) {
            error("schemaVersion", "Unsupported schema version: ${design.schemaVersion}")
        }
        if (design.widgetId.isBlank()) error("widgetId", "Widget ID must not be empty")
        if (design.revision < 0L) error("revision", "Revision must not be negative")

        val elements = design.baseVariant.elements
        elements.groupBy { it.elementId }.filterValues { it.size > 1 }.keys.sorted().forEach { duplicate ->
            error("baseVariant.elements", "Duplicate element ID: $duplicate")
        }
        elements.forEachIndexed { index, element ->
            validateElement(element, "baseVariant.elements[$index]", ::error)
        }

        val elementIds = elements.mapTo(mutableSetOf()) { it.elementId }
        requiredElementIds.sorted().filterNot(elementIds::contains).forEach { missing ->
            error("baseVariant.elements", "Missing required element ID: $missing")
        }

        design.variants.entries.sortedBy { it.key.stableId }.forEach { (variantId, variantOverride) ->
            val variantField = "variants.${variantId.stableId}"
            if (!variantId.isValid()) error(variantField, "Invalid variant ID: ${variantId.stableId}")
            variantOverride.elementOverrides.entries.sortedBy { it.key }.forEach { (elementId, elementOverride) ->
                val elementField = "$variantField.elements.$elementId"
                val baseElement = elements.firstOrNull { it.elementId == elementId }
                if (baseElement == null) {
                    error(elementField, "Variant override references unknown element ID: $elementId")
                } else {
                    validateOverride(baseElement, elementOverride, elementField, ::error)
                }
            }
        }
        return ValidationResult(issues)
    }

    private fun validateElement(
        element: WidgetElementSpec,
        field: String,
        error: (String, String) -> Unit
    ) {
        if (element.elementId.isBlank()) error("$field.elementId", "Element ID must not be empty")
        validateFiniteValues(
            field,
            listOf(
                "xDp" to element.xDp,
                "yDp" to element.yDp,
                "widthDp" to element.widthDp,
                "heightDp" to element.heightDp,
                "paddingStartDp" to element.paddingStartDp,
                "paddingTopDp" to element.paddingTopDp,
                "paddingEndDp" to element.paddingEndDp,
                "paddingBottomDp" to element.paddingBottomDp,
                "cornerRadiusDp" to element.cornerRadiusDp
            ),
            error
        )
        if (element.widthMode == DimensionMode.FIXED && element.widthDp < 0f) {
            error("$field.widthDp", "Fixed width must not be negative")
        }
        if (element.heightMode == DimensionMode.FIXED && element.heightDp < 0f) {
            error("$field.heightDp", "Fixed height must not be negative")
        }
        validateNonNegativeLayoutValues(
            field,
            element.paddingStartDp,
            element.paddingTopDp,
            element.paddingEndDp,
            element.paddingBottomDp,
            element.cornerRadiusDp,
            element.zIndex,
            error
        )
    }

    private fun validateOverride(
        base: WidgetElementSpec,
        override: WidgetElementOverride,
        field: String,
        error: (String, String) -> Unit
    ) {
        val merged = base.copy(
            visible = override.visible ?: base.visible,
            xDp = override.xDp ?: base.xDp,
            yDp = override.yDp ?: base.yDp,
            widthMode = override.widthMode ?: base.widthMode,
            heightMode = override.heightMode ?: base.heightMode,
            widthDp = override.widthDp ?: base.widthDp,
            heightDp = override.heightDp ?: base.heightDp,
            paddingStartDp = override.paddingStartDp ?: base.paddingStartDp,
            paddingTopDp = override.paddingTopDp ?: base.paddingTopDp,
            paddingEndDp = override.paddingEndDp ?: base.paddingEndDp,
            paddingBottomDp = override.paddingBottomDp ?: base.paddingBottomDp,
            anchor = override.anchor ?: base.anchor,
            horizontalAlignment = override.horizontalAlignment ?: base.horizontalAlignment,
            verticalAlignment = override.verticalAlignment ?: base.verticalAlignment,
            zIndex = override.zIndex ?: base.zIndex,
            cornerRadiusDp = override.cornerRadiusDp ?: base.cornerRadiusDp
        )
        validateElement(merged, field, error)
    }

    private fun validateFiniteValues(
        field: String,
        values: List<Pair<String, Float>>,
        error: (String, String) -> Unit
    ) {
        values.forEach { (name, value) ->
            if (!value.isFinite()) error("$field.$name", "$name must be finite")
        }
    }

    private fun validateNonNegativeLayoutValues(
        field: String,
        paddingStart: Float,
        paddingTop: Float,
        paddingEnd: Float,
        paddingBottom: Float,
        cornerRadius: Float,
        zIndex: Int,
        error: (String, String) -> Unit
    ) {
        listOf(
            "paddingStartDp" to paddingStart,
            "paddingTopDp" to paddingTop,
            "paddingEndDp" to paddingEnd,
            "paddingBottomDp" to paddingBottom
        ).forEach { (name, value) ->
            if (value < 0f) error("$field.$name", "$name must not be negative")
        }
        if (cornerRadius < 0f) error("$field.cornerRadiusDp", "Corner radius must not be negative")
        if (zIndex < 0) error("$field.zIndex", "Z-index must not be negative")
    }
}
