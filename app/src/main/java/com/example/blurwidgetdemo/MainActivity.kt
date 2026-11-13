package dev.revilodev.onewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionSet
import android.transition.TransitionManager
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import dev.revilodev.onewidget.ui.PaletteActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.revilodev.onewidget.widgets.battery.BatteryWidget
import dev.revilodev.onewidget.studio.clock.ClockStudioDescriptor
import dev.revilodev.onewidget.studio.battery.BatteryStudioDescriptor
import dev.revilodev.onewidget.studio.storage.StorageStudioDescriptor
import dev.revilodev.onewidget.studio.calendar.CalendarStudioDescriptor
import dev.revilodev.onewidget.studio.universal.UniversalWidgetIds
import dev.revilodev.onewidget.studio.data.StudioRepositoryResult
import dev.revilodev.onewidget.studio.data.WidgetDesignRepository
import dev.revilodev.onewidget.studio.developer.DeveloperModeRepository
import dev.revilodev.onewidget.studio.gallery.GalleryPreviewRequest
import dev.revilodev.onewidget.studio.gallery.GalleryPreviewSource
import dev.revilodev.onewidget.studio.gallery.SnapshotCacheKey
import dev.revilodev.onewidget.studio.gallery.WidgetGalleryBitmapCache
import dev.revilodev.onewidget.studio.gallery.WidgetGalleryPreviewResolver
import dev.revilodev.onewidget.studio.model.WidgetVariantId
import dev.revilodev.onewidget.studio.production.ProductionWidgetDesignRepository
import dev.revilodev.onewidget.studio.production.ProductionWidgetSnapshotRepository
import dev.revilodev.onewidget.studio.production.WidgetPinResultReceiver
import dev.revilodev.onewidget.studio.snapshot.WidgetSnapshotRepository
import dev.revilodev.onewidget.widgets.calendar.CalendarDisplayWidget
import dev.revilodev.onewidget.widgets.calendar.CalendarWidget
import dev.revilodev.onewidget.widgets.clock.DigitalClockWidget
import dev.revilodev.onewidget.widgets.extra.AlarmWidget
import dev.revilodev.onewidget.widgets.extra.CounterWidget
import dev.revilodev.onewidget.widgets.extra.GoogleSearchWidget
import dev.revilodev.onewidget.widgets.extra.MediaPlayerWidget
import dev.revilodev.onewidget.widgets.extra.OpenAppWidget
import dev.revilodev.onewidget.widgets.extra.StepCounterWidget
import dev.revilodev.onewidget.widgets.extra.TextWidget
import dev.revilodev.onewidget.widgets.extra.TodoWidget
import dev.revilodev.onewidget.widgets.extra.SamsungBrowserSearchWidget
import dev.revilodev.onewidget.widgets.extra.ToggleKind
import dev.revilodev.onewidget.widgets.extra.ToggleWidget
import dev.revilodev.onewidget.widgets.extra.YouTubeSearchWidget
import dev.revilodev.onewidget.widgets.hydration.HydrationWidget
import dev.revilodev.onewidget.widgets.storage.StorageWidget
import dev.revilodev.onewidget.widgets.weather.WeatherWidget
import dev.revilodev.onewidget.widgets.WidgetInitialSizeSpec
import dev.revilodev.onewidget.widgets.variants.ExactWidgetVariants
import dev.oneuiproject.oneui.widget.RoundedLinearLayout
import dev.revilodev.onewidget.ui.FlowToolbarLayout
import dev.revilodev.onewidget.ui.WidgetSearchBar
import dev.revilodev.onewidget.ui.applyCardHighlight

/** A launcher-like list that previews the same layouts supplied to the widget host. */
class MainActivity : PaletteActivity() {
    private val snapshotRepository by lazy { WidgetSnapshotRepository(this) }
    private val designRepository by lazy { WidgetDesignRepository(this) }
    private val developerModeRepository by lazy { DeveloperModeRepository(this) }
    private val productionDesignRepository by lazy { ProductionWidgetDesignRepository(this) }
    private val productionSnapshotRepository by lazy { ProductionWidgetSnapshotRepository(this) }
    private val galleryPreviewResolver by lazy {
        WidgetGalleryPreviewResolver(
            developerSnapshotStatus = snapshotRepository::status,
            productionSnapshotStatus = productionSnapshotRepository::status
        )
    }
    private val studioRevisions = mutableMapOf<String, Long?>()
    private val productionRevisions = mutableMapOf<String, Long?>()

    private val categories by lazy { pickerCategories() }
    private val expandedCategories = mutableSetOf<String>()
    private val expandedWidgets = mutableSetOf<String>()
    private lateinit var searchBar: WidgetSearchBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbarLayout = findViewById<FlowToolbarLayout>(R.id.main_toolbar_layout)
        bindSearch(toolbarLayout, savedInstanceState)
        toolbarLayout.post {
            supportActionBar?.setDisplayShowTitleEnabled(false)
            toolbarLayout.hideCollapsedTitle()
        }
    }

    override fun onResume() {
        super.onResume()
        studioRevisions.clear()
        productionRevisions.clear()
        addWidgetPicker()
    }

    private fun bindSearch(toolbarLayout: FlowToolbarLayout, savedInstanceState: Bundle?) {
        searchBar = findViewById(R.id.widget_search)
        positionSearchAboveSystemBars()
        val searchBack = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = searchBar.setOpen(false)
        }
        onBackPressedDispatcher.addCallback(this, searchBack)
        val appBar = toolbarLayout.appBarLayout
        var restoreExpandedHeader = false
        searchBar.onOpenChanged = { open ->
            searchBack.isEnabled = open
            if (open) {
                restoreExpandedHeader = appBar.top == 0
                appBar.setExpanded(false, true)
            } else if (restoreExpandedHeader) {
                appBar.setExpanded(true, true)
            }
        }
        searchBar.onQueryChanged = {
            addWidgetPicker()
            findViewById<View>(R.id.widget_picker_scroll).scrollTo(0, 0)
        }
        savedInstanceState?.getStringArrayList("expanded_categories")?.let(expandedCategories::addAll)
        savedInstanceState?.getStringArrayList("expanded_widgets")?.let(expandedWidgets::addAll)
        savedInstanceState?.getString("widget_search_query")?.let(searchBar::restoreQuery)
    }

    private fun positionSearchAboveSystemBars() {
        val container = searchBar.parent as View
        val scroll = findViewById<View>(R.id.widget_picker_scroll)
        val galleryPadding = scroll.paddingBottom
        val location = IntArray(2)
        fun update(insets: WindowInsetsCompat) {
            val inset = insets.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime()).bottom
            val overlap = if (Build.VERSION.SDK_INT >= 30) {
                container.getLocationInWindow(location)
                val windowHeight = windowManager.currentWindowMetrics.bounds.height()
                (inset - (windowHeight - location[1] - container.height)).coerceAtLeast(0)
            } else 0
            val params = searchBar.layoutParams as FrameLayout.LayoutParams
            val bottom = dp(16) + overlap
            if (params.bottomMargin != bottom) {
                params.bottomMargin = bottom
                searchBar.layoutParams = params
            }
            val padding = galleryPadding + overlap
            if (scroll.paddingBottom != padding) {
                scroll.setPadding(scroll.paddingLeft, scroll.paddingTop, scroll.paddingRight, padding)
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(container) { _, insets ->
            update(insets)
            insets
        }
        container.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            ViewCompat.getRootWindowInsets(container)?.let(::update)
        }
        ViewCompat.requestApplyInsets(container)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (searchBar.isOpen) outState.putString("widget_search_query", searchBar.query)
        outState.putStringArrayList("expanded_categories", ArrayList(expandedCategories))
        outState.putStringArrayList("expanded_widgets", ArrayList(expandedWidgets))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_settings -> {
            startActivity(Intent(this, AppearanceSettingsActivity::class.java))
            true
        }
        R.id.menu_about -> {
            startActivity(Intent(this, AboutActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun addWidgetPicker() {
        val container = findViewById<LinearLayout>(R.id.widget_picker_list)
        container.removeAllViews()
        val query = searchBar.query.trim()
        val results = if (query.isEmpty()) categories else categories.mapNotNull { category ->
            val matches = if (category.title.contains(query, ignoreCase = true)) category.widgets
                else category.widgets.filter { it.title.contains(query, ignoreCase = true) }
            if (matches.isEmpty()) null else category.copy(widgets = matches)
        }
        if (results.isEmpty()) {
            container.addView(TextView(this).apply {
                setText(R.string.no_widgets_found)
                gravity = Gravity.CENTER
                setTextColor(themeColor(android.R.attr.textColorSecondary))
                setPadding(0, dp(24), 0, dp(24))
            })
        }
        results.forEachIndexed { index, category ->
            container.addView(categoryCard(category, query.isNotEmpty()), LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { if (index > 0) topMargin = dp(10) })
        }
    }

    private fun pickerCategories() = listOf(
        PickerCategory("Glass", R.drawable.category_blur, listOf(widget(
            "Glass", R.drawable.ic_home_24, BlurWidget::class.java,
            sizes("2 × 1" to R.layout.widget_preview_strip,
                "2 × 2" to R.layout.widget_preview_square, "4 × 2" to R.layout.widget_preview_large,
                "4 × 4" to R.layout.widget_preview_large, "4 × 5" to R.layout.widget_preview_large),
            studioWidgetId = UniversalWidgetIds.GLASS
        ))),
        PickerCategory("Clock", R.drawable.category_clock, listOf(
            widget(
            "Digital clock", R.drawable.time, DigitalClockWidget::class.java,
            sizes("2 × 1" to R.layout.widget_digital_clock_compact,
                "3 × 2" to R.layout.widget_digital_clock_medium,
                "4 × 2" to R.layout.widget_digital_clock_large,
                "4 × 4" to R.layout.widget_digital_clock_large),
            studioWidgetId = ClockStudioDescriptor.WIDGET_ID
            ),
            widget("Alarm", R.drawable.category_clock, AlarmWidget::class.java,
                sizes("2 × 1" to R.layout.widget_alarm_compact, "3 × 2" to R.layout.widget_alarm_large,
                    "4 × 2" to R.layout.widget_alarm_large),
                studioWidgetId = UniversalWidgetIds.ALARM)
        )),
        PickerCategory("Battery", R.drawable.category_battery, listOf(widget(
            "Battery", R.drawable.battery, BatteryWidget::class.java,
            sizes("2 × 1" to R.layout.widget_battery_compact,
                "4 × 1" to R.layout.widget_battery_compact,
                "2 × 2" to R.layout.widget_battery_square, "4 × 2" to R.layout.widget_battery_large),
            studioWidgetId = BatteryStudioDescriptor.WIDGET_ID,
            artwork = mapOf(
                (2 to 1) to R.drawable.battery_2x1,
                (4 to 1) to R.drawable.battery4x1,
                (2 to 2) to R.drawable.battery_2x2,
                (4 to 2) to R.drawable.battery_4x2
            )
        ))),
        PickerCategory("Weather", R.drawable.category_weather, listOf(widget(
            "Weather", R.drawable.weather_sunny, WeatherWidget::class.java,
            sizes("2 × 1" to R.layout.widget_weather_compact,
                "3 × 1" to R.layout.widget_weather_compact, "4 × 1" to R.layout.widget_weather_compact,
                "4 × 2" to R.layout.widget_weather_large),
            studioWidgetId = UniversalWidgetIds.WEATHER
        ))),
        PickerCategory("Calendar", R.drawable.category_calander, listOf(
            widget("Next event", R.drawable.calendar_today, CalendarWidget::class.java,
                sizes("2 × 1" to R.layout.widget_calendar_compact,
                    "3 × 1" to R.layout.widget_calendar_compact, "4 × 1" to R.layout.widget_calendar_compact,
                    "2 × 2" to R.layout.widget_calendar_large, "4 × 2" to R.layout.widget_calendar_large),
                studioWidgetId = CalendarStudioDescriptor.WIDGET_ID),
            widget("Calendar display", R.drawable.calendar_today, CalendarDisplayWidget::class.java,
                sizes("2 × 2" to R.layout.widget_calendar_display, "4 × 2" to R.layout.widget_calendar_display),
                studioWidgetId = UniversalWidgetIds.CALENDAR_DISPLAY)
        )),
        PickerCategory("Storage", R.drawable.category_storage, listOf(widget(
            "Storage", R.drawable.manage_storage, StorageWidget::class.java,
            sizes("2 × 1" to R.layout.widget_storage_compact, "4 × 1" to R.layout.widget_storage_compact,
                "2 × 2" to R.layout.widget_storage_large, "4 × 2" to R.layout.widget_storage_large),
            studioWidgetId = StorageStudioDescriptor.WIDGET_ID,
            artwork = mapOf(
                (2 to 1) to R.drawable.storage_2x1,
                (4 to 1) to R.drawable.storage_4x1,
                (2 to 2) to R.drawable.storage_2x2,
                (4 to 2) to R.drawable.storage_4x2_1
            )
        ))),
        PickerCategory("Health", R.drawable.category_health, listOf(
            stepCounterWidget(),
            widget("Hydration", R.drawable.water_drop, HydrationWidget::class.java,
                sizes("2 × 1" to R.layout.widget_hydration_compact,
                    "3 × 1" to R.layout.widget_hydration_compact, "4 × 1" to R.layout.widget_hydration_compact,
                    "4 × 2" to R.layout.widget_hydration_large),
                studioWidgetId = UniversalWidgetIds.HYDRATION)
        )),
        PickerCategory("Search", R.drawable.category_search, listOf(
            widget("Google Search", R.drawable.google, GoogleSearchWidget::class.java,
                sizes("3 × 1" to R.layout.widget_google_search, "4 × 1" to R.layout.widget_google_search,
                    "5 × 1" to R.layout.widget_google_search, "6 × 1" to R.layout.widget_google_search,
                    "7 × 1" to R.layout.widget_google_search), studioWidgetId = UniversalWidgetIds.GOOGLE_SEARCH,
                artwork = mapOf((4 to 1) to R.drawable.google_search_4x1)),
            widget("Samsung Browser search", R.drawable.samsung_browser, SamsungBrowserSearchWidget::class.java,
                sizes("3 × 1" to R.layout.widget_google_search, "4 × 1" to R.layout.widget_google_search,
                    "5 × 1" to R.layout.widget_google_search, "6 × 1" to R.layout.widget_google_search,
                    "7 × 1" to R.layout.widget_google_search), studioWidgetId = UniversalWidgetIds.SAMSUNG_BROWSER_SEARCH,
                artwork = mapOf((4 to 1) to R.drawable.samsung_search_4x1)),
            widget("YouTube search", R.drawable.youtube, YouTubeSearchWidget::class.java,
                sizes("3 × 1" to R.layout.widget_google_search, "4 × 1" to R.layout.widget_google_search,
                    "5 × 1" to R.layout.widget_google_search, "6 × 1" to R.layout.widget_google_search,
                    "7 × 1" to R.layout.widget_google_search), studioWidgetId = UniversalWidgetIds.YOUTUBE_SEARCH,
                artwork = mapOf((4 to 1) to R.drawable.youtube_4x1))
        )),
        PickerCategory("Media player", R.drawable.category_media, listOf(
            widget("Media player", R.drawable.music_alt, MediaPlayerWidget::class.java,
                sizes("3 × 1" to R.layout.widget_media_compact, "2 × 2" to R.layout.widget_media_large,
                    "3 × 2" to R.layout.widget_media_large),
                studioWidgetId = UniversalWidgetIds.MEDIA_PLAYER)
        )),
        PickerCategory("Other", R.drawable.category_more, listOf(
            actionWidget("Open app", R.drawable.phone, OpenAppWidget::class.java, 4, UniversalWidgetIds.OPEN_APP),
            actionWidget("Text", R.drawable.text_check_on, TextWidget::class.java, 4, UniversalWidgetIds.TEXT),
            widget("To do list", R.drawable.checkbox_unchecked, TodoWidget::class.java,
                sizes("2 × 2" to R.layout.widget_todo, "4 × 2" to R.layout.widget_todo),
                studioWidgetId = "todo_list"),
            widget("Counter", R.drawable.list_add, CounterWidget::class.java,
                sizes("2 × 1" to R.layout.widget_counter_compact, "2 × 2" to R.layout.widget_counter_large,
                    "4 × 2" to R.layout.widget_counter_large),
                studioWidgetId = UniversalWidgetIds.COUNTER)
        )),
        PickerCategory("Toggles", R.drawable.category_settings, ToggleKind.entries.map { kind ->
            widget(kind.label, kind.icon, ToggleWidget::class.java,
                toggleSizes("1 × 1" to R.layout.widget_toggle_square, "2 × 1" to R.layout.widget_toggle), kind,
                studioWidgetId = UniversalWidgetIds.TOGGLE)
        })
    )

    private fun actionWidget(
        title: String,
        icon: Int,
        provider: Class<out AppWidgetProvider>,
        count: Int,
        studioWidgetId: String
    ): PickerWidget {
        val variants = if (count == 2) {
            sizes("2 × 1" to R.layout.widget_action)
        } else {
            sizes("2 × 1" to R.layout.widget_action,
                "2 × 2" to R.layout.widget_action, "4 × 2" to R.layout.widget_action)
        }
        return widget(title, icon, provider, variants, studioWidgetId = studioWidgetId)
    }

    private fun stepCounterWidget() = widget(
        "Step counter", R.drawable.running, StepCounterWidget::class.java,
        sizes(
            "2 × 1" to R.layout.widget_steps_compact,
            "3 × 1" to R.layout.widget_steps_compact,
            "4 × 1" to R.layout.widget_steps_compact
        ),
        studioWidgetId = UniversalWidgetIds.STEP_COUNTER,
        artwork = mapOf((4 to 1) to R.drawable.steps_4x1)
    )

    private fun sizes(vararg definitions: Pair<String, Int>): List<WidgetSize> = definitions
        .map { (name, layout) ->
        val parts = name.split(" × ")
        WidgetSize(name, layout, parts[0].toInt(), parts[1].toInt())
    }

    private fun toggleSizes(vararg definitions: Pair<String, Int>): List<WidgetSize> = definitions.map { (name, layout) ->
        val parts = name.split(" × ")
        WidgetSize(name, layout, parts[0].toInt(), parts[1].toInt())
    }

    private fun widget(
        title: String,
        icon: Int,
        provider: Class<out AppWidgetProvider>,
        sizes: List<WidgetSize>,
        toggleKind: ToggleKind? = null,
        studioWidgetId: String? = null,
        artwork: Map<Pair<Int, Int>, Int> = emptyMap()
    ) = PickerWidget(title, icon, provider, sizes, toggleKind, studioWidgetId, artwork)

    private fun categoryCard(category: PickerCategory, searching: Boolean): View {
        val card = RoundedLinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            applyCardHighlight()
        }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row = pickerRow(category.title, category.icon, category.widgetCountText())
        fun populate() {
            if (content.childCount > 0) return
            category.widgets.forEachIndexed { index, item ->
                if (category.widgets.size == 1) content.addView(previewSection(item, false))
                else content.addView(widgetRow(item, index != 0, searching))
            }
        }
        bindDropdown(row, content, searching || category.title in expandedCategories) { expanded ->
            if (!searching) updateExpansion(expandedCategories, category.title, expanded)
            if (expanded) populate()
        }
        if (content.visibility == View.VISIBLE) populate()
        card.addView(row)
        card.addView(content)
        return card
    }

    private fun widgetRow(item: PickerWidget, showDivider: Boolean, searching: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        if (showDivider) addView(divider())
        val previews = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        val row = pickerRow(item.title, item.subcategoryIcon(), item.sizeCountText())
        bindDropdown(row, previews, !searching && item.title in expandedWidgets) { expanded ->
            if (!searching) updateExpansion(expandedWidgets, item.title, expanded)
            if (expanded && previews.childCount == 0) previews.addView(previewSection(item, true))
        }
        if (previews.visibility == View.VISIBLE) previews.addView(previewSection(item, true))
        addView(row)
        addView(previews)
    }

    private fun updateExpansion(expanded: MutableSet<String>, title: String, open: Boolean) {
        if (open) expanded.add(title) else expanded.remove(title)
    }

    private fun previewSection(item: PickerWidget, inset: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(if (inset) dp(24) else dp(16), 0, if (inset) dp(24) else dp(16), dp(16))
        if (item.provider == BlurWidget::class.java) {
            addView(pickerRow("Custom size", R.drawable.category_blur, "2 × 1 to 7 × 7", expandable = false).apply {
                setOnClickListener { showGlassSizePicker(item) }
            })
        }
        var index = 0
        while (index < item.sizes.size) {
            val current = item.sizes[index]
            val next = item.sizes.getOrNull(index + 1)
            if (index > 0) addView(divider(dp(8)))
            if (current.isSingleCell && next?.isWideSingleRow == true) {
                addView(inlinePreviewRow(item, current, next))
                index += 2
            } else {
                addView(previewTile(item, current))
                index++
            }
        }
    }

    private fun showGlassSizePicker(item: PickerWidget) {
        val columns = android.widget.NumberPicker(this).apply { minValue = 2; maxValue = 7; value = 2; wrapSelectorWheel = false }
        val rows = android.widget.NumberPicker(this).apply { minValue = 1; maxValue = 7; value = 1; wrapSelectorWheel = false }
        columns.contentDescription = "Columns"
        rows.contentDescription = "Rows"
        val content = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            addView(columns)
            addView(TextView(this@MainActivity).apply { text = " × "; textSize = 22f })
            addView(rows)
        }
        AlertDialog.Builder(this).setTitle("Glass size").setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("Continue") { _, _ ->
                val c = columns.value
                val r = rows.value
                showPlacementPrompt(item, WidgetSize("${c} × ${r}", R.layout.widget_preview_large, c, r))
            }.show()
    }

    private fun inlinePreviewRow(item: PickerWidget, square: WidgetSize, wide: WidgetSize): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.TOP
        addView(previewTile(item, square), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(View(this@MainActivity), LinearLayout.LayoutParams(dp(8), 1))
        addView(previewTile(item, wide), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f))
    }

    private fun previewTile(item: PickerWidget, size: WidgetSize): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = selectableBackground()
        isClickable = true
        isFocusable = true
        contentDescription = getString(R.string.place_widget_preview, item.title, size.label)
        setPadding(dp(4), dp(6), dp(4), dp(6))
        setOnClickListener { showPlacementPrompt(item, size) }
        addView(TextView(this@MainActivity).apply {
            text = size.label
            textSize = 13f
            setTextColor(themeColor(android.R.attr.textColorSecondary))
            gravity = Gravity.CENTER_HORIZONTAL
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(
            galleryPreview(item, size),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun galleryPreview(item: PickerWidget, size: WidgetSize): View {
        val variant = WidgetVariantId.Standard(size.columns, size.rows)
        val developerModeEnabled = developerModeRepository.isEnabled()
        val revision = item.studioWidgetId?.takeIf { developerModeEnabled }?.let { widgetId ->
            studioRevisions.getOrPut(widgetId) {
                when (val loaded = designRepository.load(widgetId)) {
                    is StudioRepositoryResult.Success -> loaded.value.revision
                    is StudioRepositoryResult.Failure -> null
                }
            }
        }
        val productionRevision = item.studioWidgetId?.let { widgetId ->
            productionRevisions.getOrPut(widgetId) {
                when (val loaded = productionDesignRepository.loadActive(widgetId)) {
                    is StudioRepositoryResult.Success -> loaded.value?.metadata?.designRevision
                    is StudioRepositoryResult.Failure -> null
                }
            }
        }
        val source = galleryPreviewResolver.resolve(
            GalleryPreviewRequest(
                developerModeEnabled = developerModeEnabled,
                widgetId = item.studioWidgetId,
                variantId = variant,
                currentDesignRevision = revision,
                productionDesignRevision = productionRevision,
                packagedPreviewResId = size.layout,
                legacyPreviewAvailable = true
            )
        )
        if (source is GalleryPreviewSource.GeneratedSnapshot) {
            generatedSnapshotView(source)?.let { return it }
        }
        item.artwork[size.columns to size.rows]?.let { resourceId ->
            val compact = size.columns == 2 && size.rows in 1..2
            return object : ImageView(this) {
                override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                    if (compact) {
                        val verticalPadding = paddingTop + paddingBottom
                        val artworkHeight = (measuredHeight - verticalPadding).coerceAtLeast(0)
                        setMeasuredDimension(measuredWidth, artworkHeight / 2 + verticalPadding)
                    }
                }
            }.apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(0, dp(4), 0, dp(4))
                setImageResource(resourceId)
                contentDescription = getString(R.string.widget_preview)
            }
        }
        return when (source) {
            is GalleryPreviewSource.PackagedPreview -> ScaledWidgetPreview(
                this,
                source.resourceId,
                size.columns,
                size.rows,
                item.title,
                item.icon
            )
            is GalleryPreviewSource.GeneratedSnapshot,
            GalleryPreviewSource.LegacyScaledPreview -> ScaledWidgetPreview(
                this,
                size.layout,
                size.columns,
                size.rows,
                item.title,
                item.icon
            )
            GalleryPreviewSource.Fallback -> ImageView(this).apply {
                minimumHeight = dp(72)
                setImageResource(item.icon)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                contentDescription = getString(R.string.widget_preview)
            }
        }
    }

    private fun generatedSnapshotView(source: GalleryPreviewSource.GeneratedSnapshot): View? {
        val metadata = source.snapshot.metadata
        val key = SnapshotCacheKey(
            metadata.widgetId,
            metadata.variantId,
            metadata.designRevision,
            metadata.snapshotPath,
            metadata.scope
        )
        val bitmap = WidgetGalleryBitmapCache.get(key)
            ?: BitmapFactory.decodeFile(source.snapshot.file.absolutePath)?.also { decoded ->
                WidgetGalleryBitmapCache.put(key, decoded)
            }
            ?: return null
        return ImageView(this).apply {
            minimumHeight = dp(72)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(0, dp(4), 0, dp(4))
            setImageBitmap(bitmap)
            contentDescription = getString(R.string.widget_preview)
        }
    }

    private fun pickerRow(title: String, icon: Int, count: String, expandable: Boolean = true): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(72)
        setPadding(dp(20), dp(10), dp(14), dp(10))
        background = selectableBackground()
        isClickable = true
        isFocusable = true
        addView(ImageView(this@MainActivity).apply {
            setImageResource(icon)
            imageTintList = null
            contentDescription = null
        }, LinearLayout.LayoutParams(dp(28), dp(28)))
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 17f
                setTypeface(typeface, Typeface.NORMAL)
                setTextColor(themeColor(android.R.attr.textColorPrimary))
                maxLines = 1
            })
            addView(TextView(this@MainActivity).apply {
                text = count
                textSize = 14f
                setTextColor(themeColor(android.R.attr.textColorSecondary))
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(2) })
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(18) })
        if (expandable) addView(ImageView(this@MainActivity).apply {
            setImageResource(R.drawable.ic_widget_picker_expand)
            imageTintList = android.content.res.ColorStateList.valueOf(themeColor(android.R.attr.textColorSecondary))
            contentDescription = getString(R.string.expand_widget_category)
        }, LinearLayout.LayoutParams(dp(24), dp(24)))
    }

    private fun bindDropdown(row: LinearLayout, content: View, open: Boolean, onChanged: (Boolean) -> Unit) {
        val chevron = row.getChildAt(row.childCount - 1) as ImageView
        fun update(expanded: Boolean, animate: Boolean) {
            content.visibility = if (expanded) View.VISIBLE else View.GONE
            if (animate) chevron.animate().rotation(if (expanded) 180f else 0f).setDuration(180).start()
            else chevron.rotation = if (expanded) 180f else 0f
            chevron.contentDescription = getString(
                if (expanded) R.string.collapse_widget_category else R.string.expand_widget_category
            )
        }
        update(open, false)
        row.setOnClickListener {
            val expanded = content.visibility != View.VISIBLE
            val list = findViewById<LinearLayout>(R.id.widget_picker_list)
            TransitionManager.beginDelayedTransition(list, TransitionSet()
                .addTransition(ChangeBounds())
                .addTransition(Fade().addTarget(content))
                .setDuration(180))
            onChanged(expanded)
            update(expanded, true)
        }
    }

    private fun showPlacementPrompt(item: PickerWidget, size: WidgetSize) {
        AlertDialog.Builder(this)
            .setTitle(R.string.place_widget_title)
            .setMessage(getString(R.string.place_widget_message, item.title, size.label))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.place_widget_action) { _, _ -> requestPlacement(item, size) }
            .show()
    }

    private fun requestPlacement(item: PickerWidget, size: WidgetSize) {
        // Runtime permissions are requested by the launcher configuration activity.
        // Keeping them there avoids duplicate prompts and still allows explicit
        // permission-required widget states when access is denied.
        val provider = ExactWidgetVariants.providerFor(item.provider, size.columns, size.rows)
        if (provider == null) {
            AlertDialog.Builder(this)
                .setMessage("This widget size is not available.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        requestWidgetPin(provider, size, item.toggleKind, item.studioWidgetId)
    }

    private fun requestWidgetPin(
        provider: Class<out AppWidgetProvider>,
        size: WidgetSize,
        toggleKind: ToggleKind? = null,
        selectedStudioWidgetId: String? = null
    ) {
        val manager = getSystemService(AppWidgetManager::class.java)
        if (manager.isRequestPinAppWidgetSupported) {
            val baseProvider = ExactWidgetVariants.baseFor(provider)
            val studioWidgetId = selectedStudioWidgetId ?: when (baseProvider) {
                DigitalClockWidget::class.java -> ClockStudioDescriptor.WIDGET_ID
                BatteryWidget::class.java -> BatteryStudioDescriptor.WIDGET_ID
                StorageWidget::class.java -> StorageStudioDescriptor.WIDGET_ID
                CalendarWidget::class.java -> CalendarStudioDescriptor.WIDGET_ID
                else -> dev.revilodev.onewidget.studio.universal.UniversalWidgetStudioCatalog
                    .definitionForProvider(baseProvider.name)?.widgetId
            }
            val successCallback = studioWidgetId?.let { widgetId ->
                PendingIntent.getBroadcast(
                    this,
                    (provider.name.hashCode() * 31 + size.columns * 7 + size.rows),
                    Intent(this, WidgetPinResultReceiver::class.java)
                        .putExtra(WidgetPinResultReceiver.EXTRA_STUDIO_WIDGET_ID, widgetId)
                        .putExtra(WidgetPinResultReceiver.EXTRA_PROVIDER_CLASS_NAME, provider.name)
                        .putExtra(WidgetInitialSizeSpec.EXTRA_COLUMNS, size.spec.columns)
                        .putExtra(WidgetInitialSizeSpec.EXTRA_ROWS, size.spec.rows)
                        .putExtra(WidgetPinResultReceiver.EXTRA_TOGGLE_KIND, toggleKind?.name),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            }
            manager.requestPinAppWidget(
                ComponentName(this, provider),
                widgetOptions(toggleKind),
                successCallback
            )
        } else {
            AlertDialog.Builder(this)
                .setMessage(R.string.add_widget_not_supported)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    /** Request extras belonging to the app, not host-owned widget option keys. */
    private fun widgetOptions(toggleKind: ToggleKind?) = Bundle().apply {
        toggleKind?.let { putString(ToggleWidget.OPTION_TOGGLE_KIND, it.name) }
    }

    private fun divider(height: Int = dp(12)): View = FrameLayout(this).apply {
        addView(View(this@MainActivity).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(1).toFloat()
                setColor(if (isDarkTheme()) 0x33FFFFFF else 0x11000000)
            }
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1), Gravity.CENTER_VERTICAL))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }

    private fun isDarkTheme() = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun selectableBackground() = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).run {
        getDrawable(0).also { recycle() }
    }

    private fun themeColor(attribute: Int): Int {
        val value = android.util.TypedValue()
        theme.resolveAttribute(attribute, value, true)
        return if (value.resourceId != 0) ContextCompat.getColor(this, value.resourceId) else value.data
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private data class PickerCategory(val title: String, val icon: Int, val widgets: List<PickerWidget>) {
        fun widgetCountText(): String {
            val count = widgets.sumOf { it.sizes.size }
            return "$count widget${if (count == 1) "" else "s"}"
        }
    }
    private data class PickerWidget(
        val title: String,
        val icon: Int,
        val provider: Class<out AppWidgetProvider>,
        val sizes: List<WidgetSize>,
        val toggleKind: ToggleKind? = null,
        val studioWidgetId: String? = null,
        val artwork: Map<Pair<Int, Int>, Int> = emptyMap()
    ) {
        fun subcategoryIcon() = when (toggleKind) {
            ToggleKind.BLUETOOTH -> R.drawable.sub_toggle_bluetooth
            ToggleKind.CAMERA -> R.drawable.sub_toggle_camera
            ToggleKind.DARK_MODE -> R.drawable.sub_toggle_darkmode
            ToggleKind.DATA -> R.drawable.sub_toggle_data
            ToggleKind.DO_NOT_DISTURB -> R.drawable.sub_toggle_dnd
            ToggleKind.HOTSPOT -> R.drawable.sub_toggle_hotspot
            ToggleKind.LOCATION -> R.drawable.sub_toggle_location
            ToggleKind.TORCH -> R.drawable.sub_toggle_torch
            ToggleKind.WIFI -> R.drawable.sub_toggle_wifi
            null -> when (provider) {
                OpenAppWidget::class.java -> R.drawable.sub_category_apps
                TodoWidget::class.java -> R.drawable.sub_category_check
                CounterWidget::class.java -> R.drawable.sub_category_counter
                DigitalClockWidget::class.java -> R.drawable.sub_category_digital_clock
                GoogleSearchWidget::class.java -> R.drawable.sub_category_google
                HydrationWidget::class.java -> R.drawable.sub_category_hydration
                CalendarWidget::class.java -> R.drawable.sub_category_next_event
                CalendarDisplayWidget::class.java -> R.drawable.category_calander
                SamsungBrowserSearchWidget::class.java -> R.drawable.sub_category_samsung_browser
                StepCounterWidget::class.java -> R.drawable.sub_category_steps
                TextWidget::class.java -> R.drawable.sub_category_text
                YouTubeSearchWidget::class.java -> R.drawable.sub_category_yotube
                else -> icon
            }
        }

        fun sizeCountText() = "${sizes.size} size${if (sizes.size == 1) "" else "s"}"
    }
    private data class WidgetSize(val label: String, val layout: Int, val columns: Int, val rows: Int) {
        val spec = WidgetInitialSizeSpec.grid(columns, rows)
        val isSingleCell get() = columns == 1 && rows == 1
        val isWideSingleRow get() = columns == 2 && rows == 1
    }

}
