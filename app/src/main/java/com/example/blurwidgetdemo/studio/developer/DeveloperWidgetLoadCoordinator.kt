package dev.revilodev.onewidget.studio.developer

import dev.revilodev.onewidget.studio.model.WidgetVariantId

data class DeveloperWidgetLoadResult(
    val activeSpec: ActiveDeveloperWidgetSpec,
    val updatedWidgetIds: List<Int>
)

class DeveloperWidgetLoadCoordinator(
    private val stateRepository: DeveloperWidgetStateRepository,
    private val currentTimeMillis: () -> Long,
    private val listWidgetIds: () -> IntArray,
    private val updateWidget: (Int, DeveloperWidgetUpdateReason) -> Unit
) {
    fun load(
        widgetId: String,
        variantId: WidgetVariantId,
        designRevision: Long,
        customWidthDp: Float? = null,
        customHeightDp: Float? = null
    ): DeveloperWidgetLoadResult {
        val spec = ActiveDeveloperWidgetSpec(
            widgetId = widgetId,
            variantId = variantId,
            designRevision = designRevision,
            loadedAtEpochMillis = currentTimeMillis(),
            customWidthDp = customWidthDp,
            customHeightDp = customHeightDp
        )
        stateRepository.saveActive(spec)
        val ids = listWidgetIds().distinct().sorted()
        ids.forEach { updateWidget(it, DeveloperWidgetUpdateReason.LOAD_FROM_STUDIO) }
        return DeveloperWidgetLoadResult(spec, ids)
    }
}
