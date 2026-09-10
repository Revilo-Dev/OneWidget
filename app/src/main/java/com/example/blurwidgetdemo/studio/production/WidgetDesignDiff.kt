package dev.revilodev.onewidget.studio.production

import dev.revilodev.onewidget.studio.model.WidgetDesignResolver
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetElementSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantId

data class WidgetDesignDifference(
    val variantId: WidgetVariantId,
    val elementId: String,
    val property: String,
    val before: String,
    val after: String
)

object WidgetDesignDiff {
    fun compare(
        previous: WidgetDesignSpec?,
        current: WidgetDesignSpec,
        variants: Collection<WidgetVariantId>
    ): List<WidgetDesignDifference> {
        if (previous == null) return emptyList()
        return variants.sortedBy { it.stableId }.flatMap { variant ->
            val before = WidgetDesignResolver.resolve(previous, variant).design?.elements
                ?.associateBy { it.elementId }.orEmpty()
            val after = WidgetDesignResolver.resolve(current, variant).design?.elements
                ?.associateBy { it.elementId }.orEmpty()
            (before.keys + after.keys).sorted().flatMap { elementId ->
                elementDifferences(variant, elementId, before[elementId], after[elementId])
            }
        }
    }

    fun summary(differences: List<WidgetDesignDifference>): String = if (differences.isEmpty()) {
        "No production geometry differences."
    } else {
        differences.groupBy { it.variantId }.entries.joinToString("\n") { (variant, values) ->
            buildString {
                append(variant.stableId).append(':')
                values.forEach { difference ->
                    append("\n  ")
                    append(difference.elementId).append(' ')
                    append(difference.property).append(": ")
                    append(difference.before).append(" -> ").append(difference.after)
                }
            }
        }
    }

    private fun elementDifferences(
        variant: WidgetVariantId,
        elementId: String,
        before: WidgetElementSpec?,
        after: WidgetElementSpec?
    ): List<WidgetDesignDifference> {
        if (before == null || after == null) {
            return listOf(change(variant, elementId, "element", before?.elementId ?: "missing", after?.elementId ?: "missing"))
        }
        val values = listOf(
            "visible" to (before.visible.toString() to after.visible.toString()),
            "xDp" to (format(before.xDp) to format(after.xDp)),
            "yDp" to (format(before.yDp) to format(after.yDp)),
            "widthMode" to (before.widthMode.name to after.widthMode.name),
            "heightMode" to (before.heightMode.name to after.heightMode.name),
            "widthDp" to (format(before.widthDp) to format(after.widthDp)),
            "heightDp" to (format(before.heightDp) to format(after.heightDp)),
            "paddingStartDp" to (format(before.paddingStartDp) to format(after.paddingStartDp)),
            "paddingTopDp" to (format(before.paddingTopDp) to format(after.paddingTopDp)),
            "paddingEndDp" to (format(before.paddingEndDp) to format(after.paddingEndDp)),
            "paddingBottomDp" to (format(before.paddingBottomDp) to format(after.paddingBottomDp)),
            "anchor" to (before.anchor.name to after.anchor.name),
            "horizontalAlignment" to (before.horizontalAlignment.name to after.horizontalAlignment.name),
            "verticalAlignment" to (before.verticalAlignment.name to after.verticalAlignment.name),
            "zIndex" to (before.zIndex.toString() to after.zIndex.toString()),
            "cornerRadiusDp" to (format(before.cornerRadiusDp) to format(after.cornerRadiusDp))
        )
        return values.filter { (_, pair) -> pair.first != pair.second }
            .map { (property, pair) -> change(variant, elementId, property, pair.first, pair.second) }
    }

    private fun change(
        variant: WidgetVariantId,
        elementId: String,
        property: String,
        before: String,
        after: String
    ) = WidgetDesignDifference(variant, elementId, property, before, after)

    private fun format(value: Float): String = if (value % 1f == 0f) value.toInt().toString() else value.toString()
}
