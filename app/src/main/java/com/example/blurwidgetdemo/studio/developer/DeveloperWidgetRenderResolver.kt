package com.example.blurwidgetdemo.studio.developer

import com.example.blurwidgetdemo.studio.data.StudioRepositoryErrorKind
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.model.WidgetDesignResolver
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.registry.StudioWidgetDescriptor
import com.example.blurwidgetdemo.studio.registry.WidgetStudioRegistry
import com.example.blurwidgetdemo.studio.rendering.WidgetRenderSize

sealed class DeveloperWidgetRenderResolution {
    data object NoActiveDesign : DeveloperWidgetRenderResolution()
    data class Error(val message: String) : DeveloperWidgetRenderResolution()
    data class Ready(
        val activeSpec: ActiveDeveloperWidgetSpec,
        val descriptor: StudioWidgetDescriptor,
        val design: ResolvedWidgetDesign,
        val renderSize: WidgetRenderSize
    ) : DeveloperWidgetRenderResolution()
}

class DeveloperWidgetRenderResolver(
    private val registry: WidgetStudioRegistry,
    private val loadDesign: (String) -> StudioRepositoryResult<WidgetDesignSpec>
) {
    fun resolve(
        activeState: DeveloperStateResult<ActiveDeveloperWidgetSpec>,
        hostSize: DeveloperWidgetHostSize
    ): DeveloperWidgetRenderResolution {
        val active = when (activeState) {
            DeveloperStateResult.Missing -> return DeveloperWidgetRenderResolution.NoActiveDesign
            is DeveloperStateResult.Corrupt -> {
                return DeveloperWidgetRenderResolution.Error(activeState.reason)
            }
            is DeveloperStateResult.Value -> activeState.value
        }
        val descriptor = registry.descriptor(active.widgetId)
            ?: return DeveloperWidgetRenderResolution.Error(
                "Missing widget descriptor: ${active.widgetId}"
            )
        if (descriptor.renderer == null) {
            return DeveloperWidgetRenderResolution.Error(
                "No developer renderer: ${active.widgetId}"
            )
        }
        if (active.variantId !in descriptor.supportedVariants) {
            return DeveloperWidgetRenderResolution.Error(
                "Unsupported variant: ${active.variantId.stableId}"
            )
        }
        val storedDesign = when (val loaded = loadDesign(active.widgetId)) {
            is StudioRepositoryResult.Success -> loaded.value
            is StudioRepositoryResult.Failure -> {
                val prefix = if (loaded.error.kind == StudioRepositoryErrorKind.NOT_FOUND) {
                    "Missing design"
                } else {
                    "Unable to load design"
                }
                return DeveloperWidgetRenderResolution.Error("$prefix: ${active.widgetId}")
            }
        }
        if (storedDesign.revision != active.designRevision) {
            return DeveloperWidgetRenderResolution.Error(
                "Design revision changed: loaded ${active.designRevision}, saved ${storedDesign.revision}. " +
                    "Load it again from Widget Studio."
            )
        }
        val resolved = WidgetDesignResolver.resolve(storedDesign, active.variantId)
        if (!resolved.isSuccess || resolved.design == null) {
            return DeveloperWidgetRenderResolution.Error(
                resolved.issues.firstOrNull()?.message ?: "Unable to resolve ${active.variantId.stableId}"
            )
        }
        return DeveloperWidgetRenderResolution.Ready(
            activeSpec = active,
            descriptor = descriptor,
            design = resolved.design,
            renderSize = WidgetRenderSize(
                hostSize.preferredWidthDp,
                hostSize.preferredHeightDp
            )
        )
    }
}

enum class DeveloperWidgetUpdateReason {
    INITIAL,
    LOAD_FROM_STUDIO,
    RESIZE,
    PROVIDER_UPDATE,
    BOOT,
    APP_UPDATE,
    DEVELOPER_MODE_ENABLED
}
