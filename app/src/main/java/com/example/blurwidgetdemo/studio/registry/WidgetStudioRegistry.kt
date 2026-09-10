package dev.revilodev.onewidget.studio.registry

import dev.revilodev.onewidget.studio.clock.ClockStudioDescriptor
import dev.revilodev.onewidget.studio.battery.BatteryStudioDescriptor
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor
import dev.revilodev.onewidget.studio.calendar.CalendarStudioDescriptor
import dev.revilodev.onewidget.studio.universal.UniversalWidgetStudioCatalog
import dev.revilodev.onewidget.studio.model.ElementCapability
import dev.revilodev.onewidget.studio.model.WidgetDesignSpec
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.rendering.StudioRemoteViewsRenderer

enum class StudioElementVisualType { TEXT, ICON, PROGRESS, PANEL, SHAPE }

data class StudioElementDescriptor(
    val elementId: String,
    val displayName: String,
    val capabilities: Set<ElementCapability>,
    val required: Boolean = false,
    val visualType: StudioElementVisualType = StudioElementVisualType.TEXT,
    val sampleText: String? = null,
    val wrapWidthDp: Float = 64f,
    val wrapHeightDp: Float = 32f
)

/** Reserved sample-data seam; runtime values remain outside WidgetDesignSpec. */
fun interface StudioSampleDataProvider {
    fun sampleValues(): Map<String, String>
}

data class StudioWidgetDescriptor(
    val widgetId: String,
    val displayName: String,
    val supportedVariants: Set<WidgetVariantId>,
    val elements: List<StudioElementDescriptor>,
    val defaultDesign: WidgetDesignSpec,
    val renderer: StudioRemoteViewsRenderer? = null,
    val sampleDataProvider: StudioSampleDataProvider? = null
) {
    val capabilities: Set<ElementCapability> = elements.flatMapTo(linkedSetOf()) { it.capabilities }
    val requiredElementIds: Set<String> = elements.filter { it.required }.mapTo(linkedSetOf()) { it.elementId }
}

sealed class RegistryRegistrationResult {
    data object Registered : RegistryRegistrationResult()
    data class Rejected(val message: String) : RegistryRegistrationResult()
}

class WidgetStudioRegistry {
    private val descriptors = linkedMapOf<String, StudioWidgetDescriptor>()

    fun register(descriptor: StudioWidgetDescriptor): RegistryRegistrationResult {
        if (descriptor.widgetId.isBlank()) return RegistryRegistrationResult.Rejected("Widget ID must not be empty")
        if (descriptor.widgetId in descriptors) {
            return RegistryRegistrationResult.Rejected("Duplicate Studio widget ID: ${descriptor.widgetId}")
        }
        val duplicateElements = descriptor.elements.groupBy { it.elementId }.filterValues { it.size > 1 }.keys
        if (duplicateElements.isNotEmpty()) {
            return RegistryRegistrationResult.Rejected(
                "Duplicate Studio element IDs: ${duplicateElements.sorted().joinToString()}"
            )
        }
        descriptors[descriptor.widgetId] = descriptor
        return RegistryRegistrationResult.Registered
    }

    fun descriptor(widgetId: String): StudioWidgetDescriptor? = descriptors[widgetId]

    fun allDescriptors(): List<StudioWidgetDescriptor> = descriptors.values.toList()

    fun registeredWidgetCount(): Int = descriptors.size
}

object DefaultWidgetStudioRegistry {
    fun create(): WidgetStudioRegistry = WidgetStudioRegistry().apply {
        check(register(ClockStudioDescriptor.create()) == RegistryRegistrationResult.Registered)
        check(register(BatteryStudioDescriptor.create()) == RegistryRegistrationResult.Registered)
        check(register(StorageStudioDescriptor.create()) == RegistryRegistrationResult.Registered)
        check(register(CalendarStudioDescriptor.create()) == RegistryRegistrationResult.Registered)
        UniversalWidgetStudioCatalog.definitions.forEach { definition ->
            check(register(definition.createDescriptor()) == RegistryRegistrationResult.Registered)
        }
    }
}
