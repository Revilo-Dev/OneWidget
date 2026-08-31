package com.example.blurwidgetdemo.studio.editor

import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetElementOverride
import com.example.blurwidgetdemo.studio.model.WidgetElementSpec
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import com.example.blurwidgetdemo.studio.model.WidgetVariantOverride

/** Maps resolved element edits back to normalized, sparse per-variant overrides. */
object WidgetVariantOverrideEditor {
    fun updateElement(
        design: WidgetDesignSpec,
        variantId: WidgetVariantId,
        resolvedElement: WidgetElementSpec
    ): WidgetDesignSpec {
        val base = design.baseVariant.elements.firstOrNull { it.elementId == resolvedElement.elementId }
            ?: return design
        val overrides = design.variants[variantId]?.elementOverrides.orEmpty().toMutableMap()
        val normalized = difference(base, resolvedElement)
        if (normalized.isEmpty()) overrides.remove(resolvedElement.elementId)
        else overrides[resolvedElement.elementId] = normalized
        return design.withVariantOverrides(variantId, overrides)
    }

    fun resetElement(
        design: WidgetDesignSpec,
        variantId: WidgetVariantId,
        elementId: String
    ): WidgetDesignSpec {
        val overrides = design.variants[variantId]?.elementOverrides.orEmpty().toMutableMap()
        if (overrides.remove(elementId) == null) return design
        return design.withVariantOverrides(variantId, overrides)
    }

    fun resetVariant(design: WidgetDesignSpec, variantId: WidgetVariantId): WidgetDesignSpec {
        if (variantId !in design.variants) return design
        return design.copy(variants = design.variants.toMutableMap().apply { remove(variantId) })
    }

    fun copyResolvedVariant(
        design: WidgetDesignSpec,
        source: ResolvedWidgetDesign,
        targetVariantId: WidgetVariantId
    ): WidgetDesignSpec {
        val baseById = design.baseVariant.elements.associateBy { it.elementId }
        val overrides = linkedMapOf<String, WidgetElementOverride>()
        source.elements.forEach { element ->
            val base = baseById[element.elementId] ?: return@forEach
            difference(base, element).takeUnless { it.isEmpty() }?.let {
                overrides[element.elementId] = it
            }
        }
        return design.withVariantOverrides(targetVariantId, overrides)
    }

    private fun WidgetDesignSpec.withVariantOverrides(
        variantId: WidgetVariantId,
        elementOverrides: Map<String, WidgetElementOverride>
    ): WidgetDesignSpec {
        val updated = variants.toMutableMap()
        if (elementOverrides.isEmpty()) updated.remove(variantId)
        else updated[variantId] = WidgetVariantOverride(elementOverrides.toMap())
        return copy(variants = updated)
    }

    private fun difference(base: WidgetElementSpec, value: WidgetElementSpec) = WidgetElementOverride(
        visible = value.visible.takeIf { it != base.visible },
        xDp = value.xDp.takeIf { it != base.xDp },
        yDp = value.yDp.takeIf { it != base.yDp },
        widthMode = value.widthMode.takeIf { it != base.widthMode },
        heightMode = value.heightMode.takeIf { it != base.heightMode },
        widthDp = value.widthDp.takeIf { it != base.widthDp },
        heightDp = value.heightDp.takeIf { it != base.heightDp },
        paddingStartDp = value.paddingStartDp.takeIf { it != base.paddingStartDp },
        paddingTopDp = value.paddingTopDp.takeIf { it != base.paddingTopDp },
        paddingEndDp = value.paddingEndDp.takeIf { it != base.paddingEndDp },
        paddingBottomDp = value.paddingBottomDp.takeIf { it != base.paddingBottomDp },
        anchor = value.anchor.takeIf { it != base.anchor },
        horizontalAlignment = value.horizontalAlignment.takeIf { it != base.horizontalAlignment },
        verticalAlignment = value.verticalAlignment.takeIf { it != base.verticalAlignment },
        zIndex = value.zIndex.takeIf { it != base.zIndex },
        cornerRadiusDp = value.cornerRadiusDp.takeIf { it != base.cornerRadiusDp }
    )

    private fun WidgetElementOverride.isEmpty(): Boolean =
        visible == null && xDp == null && yDp == null && widthMode == null && heightMode == null &&
            widthDp == null && heightDp == null && paddingStartDp == null && paddingTopDp == null &&
            paddingEndDp == null && paddingBottomDp == null && anchor == null &&
            horizontalAlignment == null && verticalAlignment == null && zIndex == null && cornerRadiusDp == null
}
