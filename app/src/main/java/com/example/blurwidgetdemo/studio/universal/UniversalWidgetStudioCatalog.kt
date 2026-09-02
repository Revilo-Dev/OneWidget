package com.example.blurwidgetdemo.studio.universal

import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.model.DimensionMode
import com.example.blurwidgetdemo.studio.model.ElementCapability
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.model.WidgetAnchor
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetElementOverride
import com.example.blurwidgetdemo.studio.model.WidgetElementSpec
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import com.example.blurwidgetdemo.studio.model.WidgetVariantOverride
import com.example.blurwidgetdemo.studio.model.WidgetVariantSpec
import com.example.blurwidgetdemo.studio.registry.StudioElementDescriptor
import com.example.blurwidgetdemo.studio.registry.StudioElementVisualType
import com.example.blurwidgetdemo.studio.registry.StudioWidgetDescriptor

enum class UniversalSizeProfile { ADAPTIVE, ROW, GRID, FIXED_SMALL, FIXED_WIDE }

enum class UniversalVisibilityRule { ALWAYS, HIDE_ONE_BY_ONE, HIDE_SINGLE_ROW }

data class UniversalElementDefinition(
    val elementId: String,
    val displayName: String,
    val visualType: StudioElementVisualType,
    val sampleText: String? = null,
    val sampleImageResId: Int? = null,
    val xDp: Float,
    val yDp: Float,
    val widthDp: Float,
    val heightDp: Float,
    val textSizeSp: Float = 16f,
    val required: Boolean = true,
    val visibilityRule: UniversalVisibilityRule = UniversalVisibilityRule.ALWAYS,
    val cornerRadiusDp: Float = 0f,
    val centerInOneByOne: Boolean = false
) {
    val capabilities: Set<ElementCapability> = buildSet {
        add(ElementCapability.VISIBILITY)
        add(ElementCapability.POSITION)
        add(ElementCapability.SIZE)
        add(ElementCapability.PADDING)
        if (visualType == StudioElementVisualType.TEXT) add(ElementCapability.ALIGNMENT)
        if (visualType == StudioElementVisualType.PROGRESS ||
            visualType == StudioElementVisualType.PANEL ||
            visualType == StudioElementVisualType.SHAPE
        ) {
            add(ElementCapability.CORNER_RADIUS)
        }
    }
}

data class UniversalWidgetDefinition(
    val widgetId: String,
    val displayName: String,
    val providerClassNames: Set<String>,
    val sizeProfile: UniversalSizeProfile,
    val elements: List<UniversalElementDefinition>,
    val requiresContentStateVerification: Boolean = false,
    val requiresProgressVerification: Boolean = false,
    val systemUiHosted: Boolean = false
) {
    init {
        require(elements.isNotEmpty()) { "$widgetId must expose at least one element" }
        require(elements.size <= UniversalRemoteViewsGeometry.SLOT_COUNT) {
            "$widgetId exceeds the universal renderer slot limit"
        }
    }

    fun createDescriptor(): StudioWidgetDescriptor = StudioWidgetDescriptor(
        widgetId = widgetId,
        displayName = displayName,
        supportedVariants = WidgetVariantId.standardVariants.toSet(),
        elements = elements.map { element ->
            StudioElementDescriptor(
                elementId = element.elementId,
                displayName = element.displayName,
                capabilities = element.capabilities,
                required = element.required,
                visualType = element.visualType,
                sampleText = element.sampleText,
                wrapWidthDp = element.widthDp,
                wrapHeightDp = element.heightDp
            )
        },
        defaultDesign = defaultDesign(),
        renderer = UniversalStudioRemoteViewsRenderer(this),
        sampleDataProvider = {
            elements.mapNotNull { element ->
                element.sampleText?.let { element.elementId to it }
            }.toMap()
        }
    )

    fun defaultDesign(): WidgetDesignSpec = WidgetDesignSpec(
        widgetId = widgetId,
        baseVariant = WidgetVariantSpec(
            elements.mapIndexed { index, element ->
                WidgetElementSpec(
                    elementId = element.elementId,
                    xDp = element.xDp,
                    yDp = element.yDp,
                    widthMode = DimensionMode.FIXED,
                    heightMode = DimensionMode.FIXED,
                    widthDp = element.widthDp,
                    heightDp = element.heightDp,
                    anchor = WidgetAnchor.CENTER,
                    horizontalAlignment = if (element.visualType == StudioElementVisualType.TEXT) {
                        HorizontalAlignment.CENTER
                    } else {
                        HorizontalAlignment.START
                    },
                    verticalAlignment = if (element.visualType == StudioElementVisualType.TEXT) {
                        VerticalAlignment.CENTER
                    } else {
                        VerticalAlignment.TOP
                    },
                    zIndex = index,
                    cornerRadiusDp = element.cornerRadiusDp
                )
            }
        ),
        variants = buildMap {
            WidgetVariantId.standardVariants.forEach { variant ->
                val availableWidthDp = 56f + (variant.columns - 1) * 90f
                val availableHeightDp = 60f + (variant.rows - 1) * 100f
                val xScale = (availableWidthDp / DEFAULT_CONTENT_WIDTH_DP).coerceAtMost(1f)
                val yScale = (availableHeightDp / DEFAULT_CONTENT_HEIGHT_DP).coerceAtMost(1f)
                val overrides = elements.associate { element ->
                    val hidden = when (element.visibilityRule) {
                        UniversalVisibilityRule.ALWAYS -> false
                        UniversalVisibilityRule.HIDE_ONE_BY_ONE -> variant.columns == 1 && variant.rows == 1
                        UniversalVisibilityRule.HIDE_SINGLE_ROW -> variant.rows == 1
                    }
                    val widthDp = element.widthDp.coerceAtMost(availableWidthDp)
                    val heightDp = element.heightDp.coerceAtMost(availableHeightDp)
                    val xLimit = ((availableWidthDp - widthDp) / 2f).coerceAtLeast(0f)
                    val yLimit = ((availableHeightDp - heightDp) / 2f).coerceAtLeast(0f)
                    val preferredX = if (
                        variant.columns == 1 && variant.rows == 1 && element.centerInOneByOne
                    ) 0f else element.xDp * xScale
                    element.elementId to WidgetElementOverride(
                        visible = false.takeIf { hidden },
                        xDp = preferredX.coerceIn(-xLimit, xLimit),
                        yDp = (element.yDp * yScale).coerceIn(-yLimit, yLimit),
                        widthDp = widthDp,
                        heightDp = heightDp
                    )
                }
                put(variant, WidgetVariantOverride(overrides))
            }
        }
    )

    private companion object {
        // The authored base geometry occupies a centered 4 x 2 content area.
        const val DEFAULT_CONTENT_WIDTH_DP = 326f
        const val DEFAULT_CONTENT_HEIGHT_DP = 160f
    }
}

object UniversalWidgetIds {
    const val GLASS = "glass"
    const val ALARM = "alarm"
    const val WEATHER = "weather"
    const val CALENDAR_DISPLAY = "calendar_display"
    const val HYDRATION = "hydration"
    const val STEP_COUNTER = "step_counter"
    const val GOOGLE_SEARCH = "google_search"
    const val SAMSUNG_BROWSER_SEARCH = "samsung_browser_search"
    const val YOUTUBE_SEARCH = "youtube_search"
    const val MEDIA_PLAYER = "media_player"
    const val OPEN_APP = "open_app"
    const val TEXT = "text"
    const val COUNTER = "counter"
    const val TOGGLE = "toggle"
    const val LOCKSCREEN_MESSAGE_SMALL = "lockscreen_message_small"
    const val LOCKSCREEN_MESSAGE_WIDE = "lockscreen_message_wide"
}

/**
 * Definitions for every widget not covered by a purpose-built Clock, Battery,
 * Storage, or Next-event renderer. Live values and PendingIntents are supplied by
 * production providers and never stored in WidgetDesignSpec.
 */
object UniversalWidgetStudioCatalog {
    val definitions: List<UniversalWidgetDefinition> = listOf(
        definition(UniversalWidgetIds.GLASS, "Glass", ".BlurWidget", UniversalSizeProfile.ADAPTIVE,
            panel("surface", "Glass surface", 0f, 0f, 190f, 90f, 24f)),
        definition(UniversalWidgetIds.ALARM, "Alarm", ".widgets.extra.AlarmWidget", UniversalSizeProfile.ADAPTIVE,
            icon("toggle", "Alarm toggle", R.drawable.category_clock, -94f, 0f, 44f),
            text("time", "Alarm time", "7:30 AM", -18f, -15f, 138f, 38f, 25f),
            text("status", "Alarm status", "Daily • On", -18f, 16f, 138f, 26f, 13f),
            text("snooze", "Snooze", "Snooze today", 91f, 0f, 92f, 34f, 13f, UniversalVisibilityRule.HIDE_SINGLE_ROW),
            states = true),
        definition(UniversalWidgetIds.WEATHER, "Weather", ".widgets.weather.WeatherWidget", UniversalSizeProfile.ADAPTIVE,
            icon("icon", "Weather icon", R.drawable.weather_partly_sunny, -104f, 0f, 52f),
            text("temperature", "Temperature", "24°C", -43f, -15f, 72f, 38f, 28f),
            text("location", "Location", "Brisbane", -43f, 17f, 96f, 24f, 13f),
            text("condition", "Condition", "Partly cloudy", 73f, 0f, 120f, 38f, 16f),
            states = true),
        definition(UniversalWidgetIds.CALENDAR_DISPLAY, "Calendar display", ".widgets.calendar.CalendarDisplayWidget", UniversalSizeProfile.GRID,
            text("month", "Month", "September 2026", 0f, -54f, 250f, 32f, 18f),
            text("grid", "Three-week grid", "M  T  W  T  F  S  S\n31  1  2  3  4  5  6\n 7  8  9 10 11 12 13\n14 15 16 17 18 19 20", 0f, 16f, 250f, 100f, 13f)),
        definition(UniversalWidgetIds.HYDRATION, "Hydration", ".widgets.hydration.HydrationWidget", UniversalSizeProfile.ADAPTIVE,
            icon("icon", "Water icon", R.drawable.ic_water_glass, -104f, 0f, 42f),
            text("count", "Glass count", "4", -46f, -10f, 54f, 42f, 30f),
            text("goal", "Daily goal", "of 8 glasses", -38f, 21f, 110f, 25f, 14f, UniversalVisibilityRule.HIDE_SINGLE_ROW),
            icon("minus", "Remove", R.drawable.minus, 53f, 0f, 42f),
            icon("plus", "Add", R.drawable.plus, 106f, 0f, 42f),
            states = true),
        definition(UniversalWidgetIds.STEP_COUNTER, "Step counter", ".widgets.extra.StepCounterWidget", UniversalSizeProfile.ROW,
            icon("icon", "Activity icon", R.drawable.running, -78f, 0f, 36f),
            text("count", "Step count", "6,842", 5f, -10f, 112f, 40f, 28f),
            text("goal", "Step goal", "/ 10,000 steps", 10f, 21f, 130f, 24f, 13f, UniversalVisibilityRule.HIDE_SINGLE_ROW),
            states = true),
        actionDefinition(UniversalWidgetIds.GOOGLE_SEARCH, "Google Search", ".widgets.extra.GoogleSearchWidget", R.drawable.google, "Search Google"),
        actionDefinition(UniversalWidgetIds.SAMSUNG_BROWSER_SEARCH, "Samsung Browser search", ".widgets.extra.SamsungBrowserSearchWidget", R.drawable.samsung_browser, "Search the web"),
        actionDefinition(UniversalWidgetIds.YOUTUBE_SEARCH, "YouTube search", ".widgets.extra.YouTubeSearchWidget", R.drawable.youtube, "Search YouTube"),
        definition(UniversalWidgetIds.MEDIA_PLAYER, "Media player", ".widgets.extra.MediaPlayerWidget", UniversalSizeProfile.ADAPTIVE,
            icon("art", "Artwork", R.drawable.music_idle_bob, -113f, 0f, 56f),
            text("title", "Track title", "Midnight Drive", -30f, -19f, 142f, 32f, 20f),
            text("artist", "Artist", "Studio Sessions", -30f, 10f, 142f, 24f, 13f, UniversalVisibilityRule.HIDE_SINGLE_ROW),
            icon("previous", "Previous", R.drawable.control_previous, 61f, -10f, 36f),
            icon("play_pause", "Play or pause", R.drawable.control_pause, 105f, -10f, 42f),
            icon("next", "Next", R.drawable.control_next, 149f, -10f, 36f),
            progress("progress", "Playback progress", 18f, 47f, 218f, 10f, UniversalVisibilityRule.HIDE_SINGLE_ROW),
            text("time", "Playback time", "1:42/3:58", 135f, 47f, 78f, 22f, 11f, UniversalVisibilityRule.HIDE_SINGLE_ROW),
            states = true, progress = true),
        actionDefinition(UniversalWidgetIds.OPEN_APP, "Open app", ".widgets.extra.OpenAppWidget", R.drawable.phone, "Open app"),
        actionDefinition(UniversalWidgetIds.TEXT, "Text", ".widgets.extra.TextWidget", R.drawable.text_check_on, "Remember this"),
        definition(UniversalWidgetIds.COUNTER, "Counter", ".widgets.extra.CounterWidget", UniversalSizeProfile.ADAPTIVE,
            icon("minus", "Remove", R.drawable.minus, -92f, 0f, 42f),
            text("value", "Value", "12", 0f, -9f, 86f, 44f, 32f),
            icon("plus", "Add", R.drawable.plus, 92f, 0f, 42f),
            text("reset", "Reset time", "Clears 12:00 AM", 0f, 33f, 150f, 24f, 13f, UniversalVisibilityRule.HIDE_SINGLE_ROW),
            states = true),
        definition(UniversalWidgetIds.TOGGLE, "Toggle", ".widgets.extra.ToggleWidget", UniversalSizeProfile.ROW,
            panel("state", "Enabled state", 0f, 0f, 72f, 72f, 36f),
            icon("icon", "Toggle icon", R.drawable.wifi_2, 0f, 0f, 40f),
            states = true),
        definition(UniversalWidgetIds.LOCKSCREEN_MESSAGE_SMALL, "Lock-screen message 1×1", ".LockScreenMessageSmallWidget", UniversalSizeProfile.FIXED_SMALL,
            text("message", "Message", "Calm", 0f, 0f, 72f, 40f, 18f), systemUi = true),
        definition(UniversalWidgetIds.LOCKSCREEN_MESSAGE_WIDE, "Lock-screen message 2×1", ".LockScreenMessageWideWidget", UniversalSizeProfile.FIXED_WIDE,
            text("message", "Message", "Breathe", 0f, 0f, 150f, 40f, 18f), systemUi = true)
    )

    val widgetIds: Set<String> = definitions.mapTo(linkedSetOf()) { it.widgetId }
    val contentStateWidgetIds: Set<String> = definitions.filter { it.requiresContentStateVerification }
        .mapTo(linkedSetOf()) { it.widgetId }
    val progressWidgetIds: Set<String> = definitions.filter { it.requiresProgressVerification }
        .mapTo(linkedSetOf()) { it.widgetId }
    val systemUiWidgetIds: Set<String> = definitions.filter { it.systemUiHosted }
        .mapTo(linkedSetOf()) { it.widgetId }

    fun definition(widgetId: String): UniversalWidgetDefinition? = definitions.firstOrNull { it.widgetId == widgetId }

    fun definitionForProvider(className: String?): UniversalWidgetDefinition? {
        if (className == null) return null
        return definitions.firstOrNull { definition ->
            definition.providerClassNames.any { suffix -> className.endsWith(suffix) }
        }
    }

    private fun actionDefinition(
        widgetId: String,
        displayName: String,
        provider: String,
        imageResId: Int,
        label: String
    ) = definition(widgetId, displayName, provider, UniversalSizeProfile.ROW,
        icon("icon", "Icon", imageResId, -54f, 0f, 38f, centerInOneByOne = true),
        text("label", "Label", label, 24f, 0f, 120f, 36f, 18f, UniversalVisibilityRule.HIDE_ONE_BY_ONE))

    private fun definition(
        widgetId: String,
        displayName: String,
        provider: String,
        sizeProfile: UniversalSizeProfile,
        vararg elements: UniversalElementDefinition,
        states: Boolean = false,
        progress: Boolean = false,
        systemUi: Boolean = false
    ) = UniversalWidgetDefinition(
        widgetId = widgetId,
        displayName = displayName,
        providerClassNames = setOf(provider),
        sizeProfile = sizeProfile,
        elements = elements.map { element ->
            element.copy(
                elementId = "$widgetId.${element.elementId.substringAfter('.', element.elementId)}"
            )
        },
        requiresContentStateVerification = states,
        requiresProgressVerification = progress,
        systemUiHosted = systemUi
    )

    private fun text(
        suffix: String,
        name: String,
        sample: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        size: Float,
        visibility: UniversalVisibilityRule = UniversalVisibilityRule.ALWAYS
    ) = UniversalElementDefinition("universal.$suffix", name, StudioElementVisualType.TEXT,
        sampleText = sample, xDp = x, yDp = y, widthDp = width, heightDp = height,
        textSizeSp = size, visibilityRule = visibility)

    private fun icon(
        suffix: String,
        name: String,
        imageResId: Int,
        x: Float,
        y: Float,
        size: Float,
        centerInOneByOne: Boolean = false
    ) = UniversalElementDefinition("universal.$suffix", name, StudioElementVisualType.ICON,
        sampleImageResId = imageResId, xDp = x, yDp = y, widthDp = size, heightDp = size,
        centerInOneByOne = centerInOneByOne)

    private fun progress(
        suffix: String,
        name: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        visibility: UniversalVisibilityRule
    ) = UniversalElementDefinition("universal.$suffix", name, StudioElementVisualType.PROGRESS,
        xDp = x, yDp = y, widthDp = width, heightDp = height,
        visibilityRule = visibility, cornerRadiusDp = height / 2f)

    private fun panel(
        suffix: String,
        name: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float
    ) = UniversalElementDefinition("universal.$suffix", name, StudioElementVisualType.PANEL,
        xDp = x, yDp = y, widthDp = width, heightDp = height, cornerRadiusDp = radius)
}
