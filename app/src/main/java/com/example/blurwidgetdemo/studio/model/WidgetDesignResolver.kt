package dev.revilodev.onewidget.studio.model

data class ResolutionIssue(
    val field: String,
    val message: String
)

data class ResolutionResult(
    val design: ResolvedWidgetDesign?,
    val issues: List<ResolutionIssue> = emptyList()
) {
    val isSuccess: Boolean get() = design != null && issues.isEmpty()
}

object WidgetDesignResolver {
    fun resolve(design: WidgetDesignSpec, variantId: WidgetVariantId): ResolutionResult {
        if (!variantId.isValid()) {
            return ResolutionResult(
                design = null,
                issues = listOf(ResolutionIssue("variantId", "Invalid variant ID: ${variantId.stableId}"))
            )
        }

        val baseById = design.baseVariant.elements.associateBy { it.elementId }
        val selectedOverride = design.variants[variantId]
        val unknownIds = selectedOverride?.elementOverrides?.keys.orEmpty() - baseById.keys
        if (unknownIds.isNotEmpty()) {
            return ResolutionResult(
                design = null,
                issues = unknownIds.sorted().map { elementId ->
                    ResolutionIssue(
                        field = "variants.${variantId.stableId}.elements.$elementId",
                        message = "Variant override references unknown element ID: $elementId"
                    )
                }
            )
        }

        val resolvedElements = design.baseVariant.elements.map { element ->
            selectedOverride?.elementOverrides?.get(element.elementId)?.let { element.apply(it) } ?: element
        }
        return ResolutionResult(
            design = ResolvedWidgetDesign(
                schemaVersion = design.schemaVersion,
                widgetId = design.widgetId,
                revision = design.revision,
                variantId = variantId,
                elements = resolvedElements
            )
        )
    }

    private fun WidgetElementSpec.apply(override: WidgetElementOverride): WidgetElementSpec = copy(
        visible = override.visible ?: visible,
        xDp = override.xDp ?: xDp,
        yDp = override.yDp ?: yDp,
        widthMode = override.widthMode ?: widthMode,
        heightMode = override.heightMode ?: heightMode,
        widthDp = override.widthDp ?: widthDp,
        heightDp = override.heightDp ?: heightDp,
        paddingStartDp = override.paddingStartDp ?: paddingStartDp,
        paddingTopDp = override.paddingTopDp ?: paddingTopDp,
        paddingEndDp = override.paddingEndDp ?: paddingEndDp,
        paddingBottomDp = override.paddingBottomDp ?: paddingBottomDp,
        anchor = override.anchor ?: anchor,
        horizontalAlignment = override.horizontalAlignment ?: horizontalAlignment,
        verticalAlignment = override.verticalAlignment ?: verticalAlignment,
        zIndex = override.zIndex ?: zIndex,
        cornerRadiusDp = override.cornerRadiusDp ?: cornerRadiusDp
    )
}
