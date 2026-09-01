package com.example.blurwidgetdemo.studio

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.canvas.StudioPreviewMetrics
import com.example.blurwidgetdemo.studio.canvas.StudioCanvasSize
import com.example.blurwidgetdemo.studio.canvas.StudioCanvasSizeSource
import com.example.blurwidgetdemo.studio.canvas.StudioCanvasSizingController
import com.example.blurwidgetdemo.studio.canvas.StudioSnapMode
import com.example.blurwidgetdemo.studio.canvas.WidgetStudioCanvasView
import com.example.blurwidgetdemo.studio.data.WidgetDesignRepository
import com.example.blurwidgetdemo.studio.data.StudioRepositoryResult
import com.example.blurwidgetdemo.studio.developer.DeveloperModeRepository
import com.example.blurwidgetdemo.studio.developer.DeveloperStateResult
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetComponentController
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetLoadCoordinator
import com.example.blurwidgetdemo.studio.developer.DeveloperWidgetStateRepository
import com.example.blurwidgetdemo.studio.editor.EditorActionResult
import com.example.blurwidgetdemo.studio.editor.WidgetStudioEditCommand
import com.example.blurwidgetdemo.studio.editor.WidgetStudioEditorController
import com.example.blurwidgetdemo.studio.editor.WidgetStudioEditorState
import com.example.blurwidgetdemo.studio.editor.StudioInspectorSections
import com.example.blurwidgetdemo.studio.model.DesignDecodeResult
import com.example.blurwidgetdemo.studio.model.DimensionMode
import com.example.blurwidgetdemo.studio.model.ElementCapability
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.model.WidgetAnchor
import com.example.blurwidgetdemo.studio.model.WidgetDesignJsonCodec
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetDesignValidator
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import com.example.blurwidgetdemo.studio.preview.ActualRemoteViewsPreview
import com.example.blurwidgetdemo.studio.production.ProductionSizeCategory
import com.example.blurwidgetdemo.studio.production.ProductionVariantResolver
import com.example.blurwidgetdemo.studio.production.ProductionWidgetDesignRepository
import com.example.blurwidgetdemo.studio.production.ProductionWidgetSnapshotRepository
import com.example.blurwidgetdemo.studio.production.PromotionRequest
import com.example.blurwidgetdemo.studio.production.PromotionResult
import com.example.blurwidgetdemo.studio.production.PromotionSnapshotStatus
import com.example.blurwidgetdemo.studio.production.WidgetDesignDiff
import com.example.blurwidgetdemo.studio.production.WidgetProductionDesignModeRepository
import com.example.blurwidgetdemo.studio.production.WidgetPromotionCoordinator
import com.example.blurwidgetdemo.studio.production.WidgetPromotionReviewer
import com.example.blurwidgetdemo.studio.registry.DefaultWidgetStudioRegistry
import com.example.blurwidgetdemo.studio.registry.StudioWidgetDescriptor
import com.example.blurwidgetdemo.studio.gallery.SnapshotCacheKey
import com.example.blurwidgetdemo.studio.gallery.WidgetGalleryBitmapCache
import com.example.blurwidgetdemo.studio.snapshot.SnapshotCaptureResult
import com.example.blurwidgetdemo.studio.snapshot.SnapshotLookupResult
import com.example.blurwidgetdemo.studio.snapshot.SnapshotRenderRequest
import com.example.blurwidgetdemo.studio.snapshot.SnapshotSaveResult
import com.example.blurwidgetdemo.studio.snapshot.SnapshotStatus
import com.example.blurwidgetdemo.studio.snapshot.WidgetSnapshotRenderer
import com.example.blurwidgetdemo.studio.snapshot.WidgetSnapshotRepository
import com.example.blurwidgetdemo.studio.snapshot.WidgetSnapshotStorage
import com.example.blurwidgetdemo.widgets.developer.DeveloperWidgetProvider
import com.example.blurwidgetdemo.studio.verification.DeviceTechnicalInfoCapture
import com.example.blurwidgetdemo.studio.verification.VerificationRevisionStatus
import com.example.blurwidgetdemo.studio.verification.WidgetParityVerification
import com.example.blurwidgetdemo.studio.verification.WidgetParityVerificationPolicy
import com.example.blurwidgetdemo.studio.verification.WidgetParityVerificationRepository
import com.example.blurwidgetdemo.studio.verification.WidgetVerificationStatus
import java.util.Locale
import java.util.concurrent.Executors

/** Developer-only Phase 2 editor. It does not participate in production widget rendering. */
class WidgetStudioActivity : AppCompatActivity() {
    private lateinit var descriptor: StudioWidgetDescriptor
    private lateinit var registry: com.example.blurwidgetdemo.studio.registry.WidgetStudioRegistry
    private lateinit var repository: WidgetDesignRepository
    private lateinit var controller: WidgetStudioEditorController
    private lateinit var developerWidgetStateRepository: DeveloperWidgetStateRepository
    private lateinit var developerWidgetLoadCoordinator: DeveloperWidgetLoadCoordinator
    private lateinit var snapshotRepository: WidgetSnapshotRepository
    private lateinit var snapshotRenderer: WidgetSnapshotRenderer
    private lateinit var snapshotStorage: WidgetSnapshotStorage
    private lateinit var verificationRepository: WidgetParityVerificationRepository
    private lateinit var productionDesignRepository: ProductionWidgetDesignRepository
    private lateinit var productionSnapshotRepository: ProductionWidgetSnapshotRepository
    private lateinit var productionModeRepository: WidgetProductionDesignModeRepository
    private lateinit var promotionCoordinator: WidgetPromotionCoordinator
    private val snapshotIo = Executors.newSingleThreadExecutor()
    private val codec = WidgetDesignJsonCodec()
    private val previewMetrics = StudioPreviewMetrics()
    private val canvasSizing = StudioCanvasSizingController(previewMetrics)
    private val variants: List<WidgetVariantId.Standard> = WidgetVariantId.standardVariants
    private val snapModes = StudioSnapMode.entries.toList()
    private val anchors = WidgetAnchor.entries.toList()
    private val dimensionModes = DimensionMode.entries.toList()
    private val horizontalAlignments = HorizontalAlignment.entries.toList()
    private val verticalAlignments = VerticalAlignment.entries.toList()

    private lateinit var canvas: WidgetStudioCanvasView
    private lateinit var previewModeGroup: RadioGroup
    private lateinit var previewHost: View
    private lateinit var actualPreview: ActualRemoteViewsPreview
    private lateinit var actualDiagnosticsSection: View
    private lateinit var editZoomControls: View
    private lateinit var editCanvasControls: View
    private lateinit var renderDiagnosticsSummary: TextView
    private lateinit var renderDiagnosticsToggle: CheckBox
    private lateinit var renderDiagnosticsDetails: TextView
    private lateinit var testWidgetStatus: TextView
    private lateinit var testWidgetLoaded: TextView
    private lateinit var canvasSizeSource: TextView
    private lateinit var deviceSizeStatus: TextView
    private lateinit var useDeviceSizeButton: Button
    private lateinit var snapshotStatus: TextView
    private lateinit var snapshotButton: Button
    private lateinit var exportSnapshotButton: Button
    private lateinit var deleteSnapshotButton: Button
    private lateinit var snapshotPreview: ImageView
    private lateinit var widgetValue: TextView
    private lateinit var variantSpinner: Spinner
    private lateinit var canvasDiagnostics: TextView
    private lateinit var saveState: TextView
    private lateinit var undoButton: Button
    private lateinit var redoButton: Button
    private lateinit var saveButton: Button
    private lateinit var zoomValue: TextView
    private lateinit var showHidden: CheckBox
    private lateinit var snapSpinner: Spinner
    private lateinit var elementsContainer: LinearLayout
    private lateinit var selectedElement: TextView
    private lateinit var visibilitySection: View
    private lateinit var positionSection: View
    private lateinit var sizeSection: View
    private lateinit var paddingSection: View
    private lateinit var alignmentSection: View
    private lateinit var cornerRadiusSection: View
    private lateinit var visibleInput: CheckBox
    private lateinit var xInput: EditText
    private lateinit var yInput: EditText
    private lateinit var anchorSpinner: Spinner
    private lateinit var widthModeSpinner: Spinner
    private lateinit var heightModeSpinner: Spinner
    private lateinit var widthInput: EditText
    private lateinit var heightInput: EditText
    private lateinit var linkPaddingInput: CheckBox
    private lateinit var paddingStartInput: EditText
    private lateinit var paddingTopInput: EditText
    private lateinit var paddingEndInput: EditText
    private lateinit var paddingBottomInput: EditText
    private lateinit var horizontalSpinner: Spinner
    private lateinit var verticalSpinner: Spinner
    private lateinit var cornerRadiusInput: EditText
    private lateinit var resetElementButton: Button
    private lateinit var verificationStatus: TextView
    private lateinit var deviceInfo: TextView
    private lateinit var oneUiVersion: EditText
    private lateinit var verifyActualPreview: CheckBox
    private lateinit var verifyDeveloperWidget: CheckBox
    private lateinit var verifyBlur: CheckBox
    private lateinit var verifyGeometry: CheckBox
    private lateinit var verifyVisibility: CheckBox
    private lateinit var verifyPadding: CheckBox
    private lateinit var verifyAlignment: CheckBox
    private lateinit var verifyRadius: CheckBox
    private lateinit var verifyProgress: CheckBox
    private lateinit var verifySnapshot: CheckBox
    private lateinit var verificationFailed: CheckBox
    private lateinit var verificationNotes: EditText
    private lateinit var productionStatus: TextView
    private lateinit var productionMapping: TextView
    private lateinit var usePromotedForNew: CheckBox
    private lateinit var promoteProduction: Button
    private lateinit var revertProduction: Button
    private var rendering = false
    private var linkPadding = false
    private var previewMode = PreviewMode.EDIT
    private var snapshotCreating = false
    private var snapshotOperationVariant: WidgetVariantId? = null
    private var snapshotStatusOverride: String? = null
    private var snapshotStatusOverrideVariant: WidgetVariantId? = null
    private var verificationDraft: WidgetParityVerification? = null
    private var verificationDraftKey: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val developerModeRepository = DeveloperModeRepository(this)
        if (!developerModeRepository.isEnabled()) {
            finish()
            return
        }
        DeveloperWidgetComponentController(this).synchronizeWithDeveloperMode(true)
        setContentView(R.layout.activity_widget_studio)
        registry = DefaultWidgetStudioRegistry.create()
        val restoredWidgetId = savedInstanceState?.getString(STATE_WIDGET_ID)
        descriptor = requireNotNull(
            registry.descriptor(restoredWidgetId ?: "digital_clock")
                ?: registry.descriptor("digital_clock")
        )
        repository = WidgetDesignRepository(this)
        snapshotRepository = WidgetSnapshotRepository(this)
        snapshotRenderer = WidgetSnapshotRenderer(
            registry = registry,
            loadDesign = repository::load
        )
        snapshotStorage = WidgetSnapshotStorage(snapshotRepository)
        verificationRepository = WidgetParityVerificationRepository(this)
        productionDesignRepository = ProductionWidgetDesignRepository(this)
        productionSnapshotRepository = ProductionWidgetSnapshotRepository(this)
        productionModeRepository = WidgetProductionDesignModeRepository(this)
        promotionCoordinator = WidgetPromotionCoordinator(productionDesignRepository)
        developerWidgetStateRepository = DeveloperWidgetStateRepository(this)
        developerWidgetLoadCoordinator = DeveloperWidgetLoadCoordinator(
            stateRepository = developerWidgetStateRepository,
            currentTimeMillis = System::currentTimeMillis,
            listWidgetIds = { DeveloperWidgetProvider.appWidgetIds(this) },
            updateWidget = { appWidgetId, reason ->
                DeveloperWidgetProvider.updateWidget(this, appWidgetId, reason)
            }
        )
        controller = WidgetStudioEditorController(
            descriptor = descriptor,
            loadDesign = repository::load,
            saveDesign = repository::save
        )
        bindViews()
        setupAdapters()
        setupListeners()
        val restored = restoreEditorState(savedInstanceState)
        if (!restored) {
            showInitializationResult(controller.initialize(repository.selectedVariantId()))
        }
        showHidden.isChecked = savedInstanceState?.getBoolean(STATE_SHOW_HIDDEN) ?: false
        linkPadding = savedInstanceState?.getBoolean(STATE_LINK_PADDING) ?: false
        linkPaddingInput.isChecked = linkPadding
        previewMode = savedInstanceState?.getString(STATE_PREVIEW_MODE)
            ?.let { stored -> PreviewMode.entries.firstOrNull { it.name == stored } }
            ?: PreviewMode.EDIT
        previewModeGroup.check(
            if (previewMode == PreviewMode.EDIT) R.id.studio_mode_edit else R.id.studio_mode_actual
        )
        renderDiagnosticsToggle.isChecked =
            savedInstanceState?.getBoolean(STATE_RENDER_DIAGNOSTICS_EXPANDED) ?: false
        savedInstanceState?.getString(STATE_SNAP_MODE)?.let { name ->
            canvas.snapMode = snapModes.firstOrNull { it.name == name } ?: StudioSnapMode.ONE_DP
        }
        restoreCanvasSizing(savedInstanceState)
        renderState()
        savedInstanceState?.getInt(STATE_ZOOM_PERCENT, 0)?.takeIf { it > 0 }?.let { percent ->
            canvas.post { canvas.setZoomPercent(percent) }
        }
    }

    private fun bindViews() {
        canvas = findViewById(R.id.studio_canvas)
        previewModeGroup = findViewById(R.id.studio_preview_mode)
        previewHost = findViewById(R.id.studio_preview_host)
        actualPreview = findViewById(R.id.studio_actual_preview)
        actualDiagnosticsSection = findViewById(R.id.studio_actual_diagnostics_section)
        editZoomControls = findViewById(R.id.studio_edit_zoom_controls)
        editCanvasControls = findViewById(R.id.studio_edit_canvas_controls)
        renderDiagnosticsSummary = findViewById(R.id.studio_render_diagnostics_summary)
        renderDiagnosticsToggle = findViewById(R.id.studio_render_diagnostics_toggle)
        renderDiagnosticsDetails = findViewById(R.id.studio_render_diagnostics_details)
        testWidgetStatus = findViewById(R.id.studio_test_widget_status)
        testWidgetLoaded = findViewById(R.id.studio_test_widget_loaded)
        canvasSizeSource = findViewById(R.id.studio_canvas_size_source)
        deviceSizeStatus = findViewById(R.id.studio_device_size_status)
        useDeviceSizeButton = findViewById(R.id.studio_use_device_size)
        snapshotStatus = findViewById(R.id.studio_snapshot_status)
        snapshotButton = findViewById(R.id.studio_snapshot)
        exportSnapshotButton = findViewById(R.id.studio_export_snapshot)
        deleteSnapshotButton = findViewById(R.id.studio_delete_snapshot)
        snapshotPreview = findViewById(R.id.studio_snapshot_preview)
        widgetValue = findViewById(R.id.studio_widget_value)
        variantSpinner = findViewById(R.id.studio_variant_spinner)
        canvasDiagnostics = findViewById(R.id.studio_canvas_diagnostics)
        saveState = findViewById(R.id.studio_save_state)
        undoButton = findViewById(R.id.studio_undo)
        redoButton = findViewById(R.id.studio_redo)
        saveButton = findViewById(R.id.studio_save)
        zoomValue = findViewById(R.id.studio_zoom_value)
        showHidden = findViewById(R.id.studio_show_hidden)
        snapSpinner = findViewById(R.id.studio_snap_spinner)
        elementsContainer = findViewById(R.id.studio_elements_container)
        selectedElement = findViewById(R.id.studio_selected_element)
        visibilitySection = findViewById(R.id.studio_visibility_section)
        positionSection = findViewById(R.id.studio_position_section)
        sizeSection = findViewById(R.id.studio_size_section)
        paddingSection = findViewById(R.id.studio_padding_section)
        alignmentSection = findViewById(R.id.studio_alignment_section)
        cornerRadiusSection = findViewById(R.id.studio_corner_radius_section)
        visibleInput = findViewById(R.id.studio_visible)
        xInput = findViewById(R.id.studio_x_input)
        yInput = findViewById(R.id.studio_y_input)
        anchorSpinner = findViewById(R.id.studio_anchor_spinner)
        widthModeSpinner = findViewById(R.id.studio_width_mode_spinner)
        heightModeSpinner = findViewById(R.id.studio_height_mode_spinner)
        widthInput = findViewById(R.id.studio_width_input)
        heightInput = findViewById(R.id.studio_height_input)
        linkPaddingInput = findViewById(R.id.studio_link_padding)
        paddingStartInput = findViewById(R.id.studio_padding_start_input)
        paddingTopInput = findViewById(R.id.studio_padding_top_input)
        paddingEndInput = findViewById(R.id.studio_padding_end_input)
        paddingBottomInput = findViewById(R.id.studio_padding_bottom_input)
        horizontalSpinner = findViewById(R.id.studio_horizontal_spinner)
        verticalSpinner = findViewById(R.id.studio_vertical_spinner)
        cornerRadiusInput = findViewById(R.id.studio_corner_radius_input)
        resetElementButton = findViewById(R.id.studio_reset_element)
        verificationStatus = findViewById(R.id.studio_verification_status)
        deviceInfo = findViewById(R.id.studio_device_info)
        oneUiVersion = findViewById(R.id.studio_one_ui_version)
        verifyActualPreview = findViewById(R.id.studio_verify_actual_preview)
        verifyDeveloperWidget = findViewById(R.id.studio_verify_developer_widget)
        verifyBlur = findViewById(R.id.studio_verify_blur)
        verifyGeometry = findViewById(R.id.studio_verify_geometry)
        verifyVisibility = findViewById(R.id.studio_verify_visibility)
        verifyPadding = findViewById(R.id.studio_verify_padding)
        verifyAlignment = findViewById(R.id.studio_verify_alignment)
        verifyRadius = findViewById(R.id.studio_verify_radius)
        verifyProgress = findViewById(R.id.studio_verify_progress)
        verifySnapshot = findViewById(R.id.studio_verify_snapshot)
        verificationFailed = findViewById(R.id.studio_verification_failed)
        verificationNotes = findViewById(R.id.studio_verification_notes)
        productionStatus = findViewById(R.id.studio_production_status)
        productionMapping = findViewById(R.id.studio_production_mapping)
        usePromotedForNew = findViewById(R.id.studio_use_promoted_for_new)
        promoteProduction = findViewById(R.id.studio_promote_production)
        revertProduction = findViewById(R.id.studio_revert_production)
    }

    private fun setupAdapters() {
        variantSpinner.adapter = textAdapter(variants.map { it.stableId })
        snapSpinner.adapter = textAdapter(
            listOf(
                getString(R.string.studio_snap_off),
                getString(R.string.studio_snap_1dp),
                getString(R.string.studio_snap_4dp),
                getString(R.string.studio_snap_8dp)
            )
        )
        anchorSpinner.adapter = textAdapter(anchors.map { it.name })
        widthModeSpinner.adapter = textAdapter(dimensionModes.map { it.name })
        heightModeSpinner.adapter = textAdapter(dimensionModes.map { it.name })
        horizontalSpinner.adapter = textAdapter(horizontalAlignments.map { it.name })
        verticalSpinner.adapter = textAdapter(verticalAlignments.map { it.name })
    }

    private fun textAdapter(values: List<String>) = ArrayAdapter(
        this,
        android.R.layout.simple_spinner_item,
        values
    ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    private fun setupListeners() {
        previewModeGroup.setOnCheckedChangeListener { _, checkedId ->
            if (!rendering) {
                previewMode = if (checkedId == R.id.studio_mode_actual) {
                    PreviewMode.ACTUAL
                } else {
                    PreviewMode.EDIT
                }
                renderState(rebuildElements = false)
            }
        }
        widgetValue.setOnClickListener { showWidgetSelector() }
        renderDiagnosticsToggle.setOnCheckedChangeListener { _, checked ->
            renderDiagnosticsDetails.visibility = if (checked) View.VISIBLE else View.GONE
        }
        variantSpinner.onItemSelectedListener = selectionListener { position ->
            if (!rendering && controller.selectVariant(variants[position])) {
                repository.setSelectedVariantId(variants[position])
                renderState()
            }
        }
        snapSpinner.onItemSelectedListener = selectionListener { position ->
            if (!rendering) canvas.snapMode = snapModes[position]
        }
        showHidden.setOnCheckedChangeListener { _, checked ->
            if (!rendering) canvas.showHiddenElements = checked
        }
        linkPaddingInput.setOnCheckedChangeListener { _, checked ->
            if (!rendering) linkPadding = checked
        }
        undoButton.setOnClickListener {
            if (controller.undo()) renderState()
        }
        redoButton.setOnClickListener {
            if (controller.redo()) renderState()
        }
        saveButton.setOnClickListener { saveDesign(showToast = true) }
        findViewById<Button>(R.id.studio_load_test_widget).setOnClickListener {
            loadToTestWidget()
        }
        snapshotButton.setOnClickListener { createSnapshot() }
        exportSnapshotButton.setOnClickListener { exportSnapshot() }
        deleteSnapshotButton.setOnClickListener { confirmDeleteSnapshot() }
        findViewById<Button>(R.id.studio_capture_device_info).setOnClickListener {
            captureDeviceInfo()
        }
        findViewById<Button>(R.id.studio_save_verification).setOnClickListener {
            saveVerification()
        }
        usePromotedForNew.setOnCheckedChangeListener { _, checked ->
            if (!rendering) productionModeRepository.setUsePromotedForNewWidgets(
                descriptor.widgetId,
                checked
            )
        }
        promoteProduction.setOnClickListener { reviewPromotion() }
        revertProduction.setOnClickListener { showProductionHistory() }
        findViewById<Button>(R.id.studio_use_grid_size).setOnClickListener {
            canvasSizing.useApproximateGrid()
            renderState(rebuildElements = false)
        }
        findViewById<Button>(R.id.studio_custom_size).setOnClickListener {
            showCustomSizeDialog()
        }
        useDeviceSizeButton.setOnClickListener {
            val measurement = developerWidgetStateRepository.mostRecentMeasurement()
            if (measurement == null) {
                Toast.makeText(this, R.string.studio_no_device_size, Toast.LENGTH_SHORT).show()
            } else {
                canvasSizing.useDeviceSize(measurement.hostSize)
                renderState(rebuildElements = false)
            }
        }
        findViewById<Button>(R.id.studio_zoom_out).setOnClickListener { canvas.zoomOut() }
        findViewById<Button>(R.id.studio_zoom_in).setOnClickListener { canvas.zoomIn() }
        findViewById<Button>(R.id.studio_zoom_actual).setOnClickListener { canvas.actualSize() }
        findViewById<Button>(R.id.studio_zoom_fit).setOnClickListener { canvas.fitToView() }
        canvas.onZoomChanged = { zoomValue.text = getString(R.string.studio_zoom_percent, it) }
        canvas.onElementSelected = { elementId ->
            controller.selectElement(elementId)
            renderState()
        }
        canvas.onDragStarted = { controller.beginDrag(it) }
        canvas.onDragPositionChanged = { _, xDp, yDp ->
            if (controller.updateDrag(xDp, yDp) is EditorActionResult.Success) renderDragState()
        }
        canvas.onDragFinished = {
            controller.endDrag()
            renderState()
        }

        visibleInput.setOnCheckedChangeListener { _, checked ->
            if (!rendering) controller.state.selectedElementId?.let {
                executeAndRender(WidgetStudioEditCommand.SetVisibility(it, checked))
            }
        }
        anchorSpinner.onItemSelectedListener = selectionListener { position ->
            if (!rendering) controller.state.selectedElementId?.let { elementId ->
                val size = canvas.canvasSizeDp()
                executeAndRender(
                    WidgetStudioEditCommand.SetAnchor(
                        elementId,
                        anchors[position],
                        size.widthDp,
                        size.heightDp
                    )
                )
            }
        }
        widthModeSpinner.onItemSelectedListener = selectionListener { position ->
            if (!rendering) controller.state.selectedElementId?.let {
                executeAndRender(WidgetStudioEditCommand.SetWidthMode(it, dimensionModes[position]))
            }
        }
        heightModeSpinner.onItemSelectedListener = selectionListener { position ->
            if (!rendering) controller.state.selectedElementId?.let {
                executeAndRender(WidgetStudioEditCommand.SetHeightMode(it, dimensionModes[position]))
            }
        }
        horizontalSpinner.onItemSelectedListener = selectionListener { position ->
            if (!rendering) controller.selectedElement()?.let { element ->
                executeAndRender(
                    WidgetStudioEditCommand.SetAlignment(
                        element.elementId,
                        horizontalAlignments[position],
                        element.verticalAlignment
                    )
                )
            }
        }
        verticalSpinner.onItemSelectedListener = selectionListener { position ->
            if (!rendering) controller.selectedElement()?.let { element ->
                executeAndRender(
                    WidgetStudioEditCommand.SetAlignment(
                        element.elementId,
                        element.horizontalAlignment,
                        verticalAlignments[position]
                    )
                )
            }
        }

        bindNumberInput(xInput) { value ->
            controller.selectedElement()?.let {
                WidgetStudioEditCommand.MoveElement(it.elementId, value, it.yDp)
            }
        }
        bindNumberInput(yInput) { value ->
            controller.selectedElement()?.let {
                WidgetStudioEditCommand.MoveElement(it.elementId, it.xDp, value)
            }
        }
        bindNumberInput(widthInput, positive = true) { value ->
            controller.state.selectedElementId?.let { WidgetStudioEditCommand.SetFixedWidth(it, value) }
        }
        bindNumberInput(heightInput, positive = true) { value ->
            controller.state.selectedElementId?.let { WidgetStudioEditCommand.SetFixedHeight(it, value) }
        }
        bindNumberInput(cornerRadiusInput) { value ->
            controller.state.selectedElementId?.let { WidgetStudioEditCommand.SetCornerRadius(it, value) }
        }
        bindPaddingInput(paddingStartInput, PaddingSide.START)
        bindPaddingInput(paddingTopInput, PaddingSide.TOP)
        bindPaddingInput(paddingEndInput, PaddingSide.END)
        bindPaddingInput(paddingBottomInput, PaddingSide.BOTTOM)

        findViewById<Button>(R.id.studio_copy_variant).setOnClickListener { showCopyVariantDialog() }
        resetElementButton.setOnClickListener {
            controller.state.selectedElementId?.let {
                executeAndRender(WidgetStudioEditCommand.ResetElement(it))
            }
        }
        findViewById<Button>(R.id.studio_reset_variant).setOnClickListener { confirmResetVariant() }
        findViewById<Button>(R.id.studio_reset_widget).setOnClickListener { confirmResetWidget() }
    }

    private fun selectionListener(onSelected: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onSelected(position)
        }
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
    }

    private fun bindNumberInput(
        input: EditText,
        positive: Boolean = false,
        command: (Float) -> WidgetStudioEditCommand?
    ) {
        fun commit(): Boolean {
            if (rendering) return false
            val value = input.text.toString().toFloatOrNull()
            if (value == null || !value.isFinite()) {
                input.error = getString(R.string.studio_invalid_number)
                return false
            }
            if (positive && value <= 0f) {
                input.error = getString(R.string.studio_positive_number)
                return false
            }
            input.error = null
            command(value)?.let(::executeAndRender)
            return true
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) commit()
            else false
        }
        input.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) commit()
        }
    }

    private enum class PaddingSide { START, TOP, END, BOTTOM }

    private fun bindPaddingInput(input: EditText, side: PaddingSide) {
        bindNumberInput(input) { value ->
            val element = controller.selectedElement() ?: return@bindNumberInput null
            if (linkPadding) {
                WidgetStudioEditCommand.SetPadding(element.elementId, value, value, value, value)
            } else {
                WidgetStudioEditCommand.SetPadding(
                    element.elementId,
                    if (side == PaddingSide.START) value else element.paddingStartDp,
                    if (side == PaddingSide.TOP) value else element.paddingTopDp,
                    if (side == PaddingSide.END) value else element.paddingEndDp,
                    if (side == PaddingSide.BOTTOM) value else element.paddingBottomDp
                )
            }
        }
    }

    private fun executeAndRender(command: WidgetStudioEditCommand) {
        when (val result = controller.execute(command)) {
            is EditorActionResult.Success -> renderState()
            is EditorActionResult.Failure -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                renderState()
            }
        }
    }

    private fun renderState(rebuildElements: Boolean = true) {
        val state = controller.state
        rendering = true
        try {
            widgetValue.text = descriptor.displayName
            variantSpinner.setSelection(variants.indexOf(state.selectedVariantId).coerceAtLeast(0))
            snapSpinner.setSelection(snapModes.indexOf(canvas.snapMode).coerceAtLeast(0))
            showHidden.isChecked = canvas.showHiddenElements || showHidden.isChecked
            linkPaddingInput.isChecked = linkPadding
            val standard = state.selectedVariantId as WidgetVariantId.Standard
            val canvasSize = canvasSizing.sizeFor(standard)
            canvasDiagnostics.text = getString(
                R.string.studio_canvas_diagnostics,
                standard.stableId,
                canvasSize.widthDp,
                canvasSize.heightDp,
                canvasSizing.source.name
            )
            saveState.text = getString(
                if (state.dirty) R.string.studio_unsaved_state else R.string.studio_saved_state,
                state.workingDesign.revision
            )
            undoButton.isEnabled = state.canUndo
            redoButton.isEnabled = state.canRedo
            saveButton.isEnabled = state.dirty
            canvas.showHiddenElements = showHidden.isChecked
            canvas.setEditorData(
                state.resolvedDesign,
                descriptor,
                state.selectedElementId,
                requestedCanvasSize = canvasSize,
                showGridGaps = canvasSizing.source == StudioCanvasSizeSource.APPROXIMATE_GRID
            )
            zoomValue.text = getString(R.string.studio_zoom_percent, canvas.zoomPercent())
            if (rebuildElements) rebuildElementList(state)
            renderInspector(state)
            renderDeveloperTestPanel(canvasSize)
            renderSnapshotPanel(state)
            renderVerificationAndProduction(state)
            renderPreview(state)
        } finally {
            rendering = false
        }
    }

    private fun renderPreview(state: WidgetStudioEditorState) {
        val actual = previewMode == PreviewMode.ACTUAL
        val variant = state.selectedVariantId as WidgetVariantId.Standard
        val canvasSize = canvasSizing.sizeFor(variant)
        previewModeGroup.check(if (actual) R.id.studio_mode_actual else R.id.studio_mode_edit)
        canvas.visibility = if (actual) View.GONE else View.VISIBLE
        actualPreview.visibility = if (actual) View.VISIBLE else View.GONE
        actualDiagnosticsSection.visibility = if (actual) View.VISIBLE else View.GONE
        editZoomControls.visibility = if (actual) View.GONE else View.VISIBLE
        editCanvasControls.visibility = if (actual) View.GONE else View.VISIBLE
        previewHost.layoutParams = previewHost.layoutParams.apply {
            height = dpToPx(if (actual) canvasSize.heightDp else EDIT_PREVIEW_HEIGHT_DP)
        }
        actualPreview.layoutParams = (actualPreview.layoutParams as android.widget.FrameLayout.LayoutParams).apply {
            width = dpToPx(canvasSize.widthDp)
            height = dpToPx(canvasSize.heightDp)
            gravity = android.view.Gravity.CENTER_HORIZONTAL
        }
        renderDiagnosticsDetails.visibility = if (renderDiagnosticsToggle.isChecked) {
            View.VISIBLE
        } else {
            View.GONE
        }
        if (actual) renderActualPreview(state)
    }

    private fun renderActualPreview(state: WidgetStudioEditorState) {
        val design = state.resolvedDesign ?: return
        val variant = state.selectedVariantId as WidgetVariantId.Standard
        val canvasSize = canvasSizing.sizeFor(variant)
        val renderer = descriptor.renderer
        if (renderer == null) {
            renderDiagnosticsSummary.text = getString(R.string.studio_actual_no_renderer)
            renderDiagnosticsDetails.text = getString(
                R.string.studio_actual_widget_diagnostic,
                descriptor.widgetId
            )
            return
        }
        val result = renderer.buildForStudioPreview(
            context = this,
            design = design,
            renderSize = com.example.blurwidgetdemo.studio.rendering.WidgetRenderSize(
                canvasSize.widthDp,
                canvasSize.heightDp
            )
        )
        val applyResult = actualPreview.show(result.remoteViews)
        renderDiagnosticsSummary.text = buildString {
            append("widget=${design.widgetId}  variant=${variant.stableId}\n")
            append("size=${canvasSize.widthDp.toInt()}x${canvasSize.heightDp.toInt()}dp  ")
            append("api=${android.os.Build.VERSION.SDK_INT}  apply=")
            append(if (applyResult.successful) "SUCCESS" else "FAILED")
            append("  diagnostics=${result.diagnostics.size}")
        }
        renderDiagnosticsDetails.text = buildString {
            append("mode=STUDIO_ACTUAL_PREVIEW\n")
            if (result.diagnostics.isEmpty()) {
                append("diagnostics=none")
            } else {
                result.diagnostics.forEach { append(it).append('\n') }
            }
            applyResult.error?.let { append("applyError=$it") }
        }
    }

    private fun showWidgetSelector() {
        val descriptors = registry.allDescriptors()
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_select_widget)
            .setItems(descriptors.map { it.displayName }.toTypedArray()) { _, which ->
                requestWidgetSwitch(descriptors[which])
            }
            .show()
    }

    private fun requestWidgetSwitch(target: StudioWidgetDescriptor) {
        if (target.widgetId == descriptor.widgetId) return
        if (!controller.state.dirty) {
            switchWidget(target)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_switch_save_title)
            .setMessage(getString(R.string.studio_switch_save_message, target.displayName))
            .setPositiveButton(R.string.studio_save) { _, _ ->
                if (saveDesign(showToast = false)) switchWidget(target)
            }
            .setNegativeButton(R.string.studio_discard) { _, _ -> switchWidget(target) }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
    }

    private fun switchWidget(target: StudioWidgetDescriptor) {
        descriptor = target
        controller = WidgetStudioEditorController(
            descriptor = target,
            loadDesign = repository::load,
            saveDesign = repository::save
        )
        snapshotStatusOverride = null
        snapshotStatusOverrideVariant = null
        snapshotOperationVariant = null
        verificationDraft = null
        verificationDraftKey = null
        canvasSizing.useApproximateGrid()
        showInitializationResult(controller.initialize(repository.selectedVariantId()))
        renderState()
    }

    private fun dpToPx(dp: Float): Int = (dp * resources.displayMetrics.density).toInt()

    private fun renderDeveloperTestPanel(canvasSize: StudioCanvasSize) {
        val widgetCount = DeveloperWidgetProvider.appWidgetIds(this).size
        testWidgetStatus.text = if (widgetCount == 0) {
            getString(R.string.studio_test_widget_none)
        } else {
            resources.getQuantityString(
                R.plurals.studio_test_widget_count,
                widgetCount,
                widgetCount
            )
        }
        testWidgetLoaded.text = when (val active = developerWidgetStateRepository.loadActive()) {
            DeveloperStateResult.Missing -> getString(R.string.studio_test_widget_not_loaded)
            is DeveloperStateResult.Corrupt -> getString(
                R.string.studio_test_widget_corrupt,
                active.reason
            )
            is DeveloperStateResult.Value -> getString(
                R.string.studio_test_widget_loaded,
                if (active.value.widgetId == descriptor.widgetId) {
                    descriptor.displayName
                } else {
                    active.value.widgetId
                },
                active.value.variantId.stableId,
                active.value.designRevision
            )
        }
        canvasSizeSource.text = getString(
            R.string.studio_canvas_source,
            canvasSizing.source.name,
            canvasSize.widthDp,
            canvasSize.heightDp
        )
        val measurement = developerWidgetStateRepository.mostRecentMeasurement()
        useDeviceSizeButton.isEnabled = measurement != null
        deviceSizeStatus.text = if (measurement == null) {
            getString(R.string.studio_no_device_size)
        } else {
            getString(
                R.string.studio_device_size_details,
                measurement.hostSize.preferredWidthDp,
                measurement.hostSize.preferredHeightDp,
                measurement.hostSize.minWidthDp,
                measurement.hostSize.minHeightDp,
                measurement.hostSize.maxWidthDp,
                measurement.hostSize.maxHeightDp,
                measurement.appWidgetId
            )
        }
    }

    private fun renderSnapshotPanel(state: WidgetStudioEditorState) {
        val variant = state.selectedVariantId
        val status = snapshotRepository.status(
            descriptor.widgetId,
            variant,
            state.storedDesign.revision
        )
        val available = availableSnapshot(status)
        val creatingThisVariant = snapshotCreating && snapshotOperationVariant == variant
        snapshotStatus.text = when {
            creatingThisVariant -> getString(R.string.studio_snapshot_creating)
            snapshotStatusOverrideVariant == variant && snapshotStatusOverride != null -> {
                snapshotStatusOverride
            }
            status is SnapshotStatus.Current -> getString(
                R.string.studio_snapshot_current,
                status.snapshot.metadata.designRevision
            )
            status is SnapshotStatus.Stale -> getString(
                R.string.studio_snapshot_stale,
                status.snapshot.metadata.designRevision,
                status.currentDesignRevision
            )
            else -> getString(R.string.studio_snapshot_not_created)
        }
        snapshotButton.text = getString(
            if (snapshotCreating) R.string.studio_snapshot_creating
            else R.string.studio_snapshot_action
        )
        val workingDesignValid = state.resolvedDesign != null &&
            WidgetDesignValidator.validate(state.workingDesign).isValid
        snapshotButton.isEnabled = !snapshotCreating && workingDesignValid && variant.isValid()
        exportSnapshotButton.isEnabled = !snapshotCreating && available != null
        deleteSnapshotButton.isEnabled = !snapshotCreating && available != null
        val bitmap = available?.let { snapshot ->
            val key = snapshotCacheKey(snapshot.metadata)
            WidgetGalleryBitmapCache.get(key)
                ?: BitmapFactory.decodeFile(snapshot.file.absolutePath)?.also { decoded ->
                    WidgetGalleryBitmapCache.put(key, decoded)
                }
        }
        snapshotPreview.setImageBitmap(bitmap)
        snapshotPreview.visibility = if (bitmap == null) View.GONE else View.VISIBLE
    }

    private fun createSnapshot() {
        if (snapshotCreating) return
        val initial = controller.state
        if (!WidgetDesignValidator.validate(initial.workingDesign).isValid) {
            saveDesign(showToast = false)
            return
        }
        val designExists = repository.load(descriptor.widgetId) is StudioRepositoryResult.Success
        if ((initial.dirty || !designExists) && !saveDesign(showToast = false)) return
        val state = controller.state
        val variant = state.selectedVariantId
        val standard = variant as? WidgetVariantId.Standard ?: return
        val size = canvasSizing.sizeFor(standard)
        val request = SnapshotRenderRequest(
            widgetId = descriptor.widgetId,
            variantId = variant,
            designRevision = state.storedDesign.revision,
            widthDp = size.widthDp,
            heightDp = size.heightDp,
            density = resources.displayMetrics.density
        )
        snapshotCreating = true
        snapshotOperationVariant = variant
        snapshotStatusOverride = null
        snapshotStatusOverrideVariant = null
        renderState(rebuildElements = false)
        when (val capture = snapshotRenderer.capture(this, request)) {
            is SnapshotCaptureResult.Failure -> finishSnapshotFailure(variant, capture.message)
            is SnapshotCaptureResult.Success -> snapshotIo.execute {
                val saved = snapshotStorage.save(capture)
                when (saved) {
                    is SnapshotSaveResult.Success -> {
                        WidgetGalleryBitmapCache.invalidate(
                            saved.metadata.widgetId,
                            saved.metadata.variantId
                        )
                        WidgetGalleryBitmapCache.put(
                            snapshotCacheKey(saved.metadata),
                            capture.bitmap
                        )
                    }
                    is SnapshotSaveResult.Failure -> capture.bitmap.recycle()
                }
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) finishSnapshotSave(variant, saved)
                }
            }
        }
    }

    private fun finishSnapshotSave(variant: WidgetVariantId, result: SnapshotSaveResult) {
        snapshotCreating = false
        snapshotOperationVariant = null
        snapshotStatusOverrideVariant = variant
        snapshotStatusOverride = when (result) {
            is SnapshotSaveResult.Success -> getString(
                R.string.studio_snapshot_created,
                result.metadata.widgetId,
                result.metadata.variantId.stableId,
                result.metadata.designRevision
            )
            is SnapshotSaveResult.Failure -> getString(
                R.string.studio_snapshot_failed,
                result.message
            )
        }
        renderState(rebuildElements = false)
        Toast.makeText(this, snapshotStatusOverride, Toast.LENGTH_LONG).show()
    }

    private fun finishSnapshotFailure(variant: WidgetVariantId, message: String) {
        snapshotCreating = false
        snapshotOperationVariant = null
        snapshotStatusOverrideVariant = variant
        snapshotStatusOverride = getString(R.string.studio_snapshot_failed, message)
        renderState(rebuildElements = false)
        Toast.makeText(this, snapshotStatusOverride, Toast.LENGTH_LONG).show()
    }

    private fun exportSnapshot() {
        val snapshot = availableSnapshotForCurrent()
        if (snapshot == null) {
            Toast.makeText(this, R.string.studio_snapshot_export_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        val shared = runCatching {
            val uri = FileProvider.getUriForFile(
                this,
                "$packageName.snapshotfiles",
                snapshot.file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(contentResolver, "widget snapshot", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.studio_snapshot_export_chooser)))
        }.isSuccess
        if (!shared) {
            Toast.makeText(this, R.string.studio_snapshot_export_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteSnapshot() {
        val snapshot = availableSnapshotForCurrent() ?: return
        val metadata = snapshot.metadata
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_snapshot_delete_title)
            .setMessage(
                getString(
                    R.string.studio_snapshot_delete_message,
                    metadata.widgetId,
                    metadata.variantId.stableId,
                    metadata.designRevision
                )
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.studio_delete_snapshot) { _, _ ->
                snapshotIo.execute {
                    val result = snapshotRepository.delete(metadata)
                    if (result is StudioRepositoryResult.Success) {
                        WidgetGalleryBitmapCache.invalidate(metadata.widgetId, metadata.variantId)
                    }
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            snapshotStatusOverrideVariant = metadata.variantId
                            snapshotStatusOverride = when (result) {
                                is StudioRepositoryResult.Success -> getString(
                                    R.string.studio_snapshot_deleted
                                )
                                is StudioRepositoryResult.Failure -> getString(
                                    R.string.studio_snapshot_delete_failed,
                                    result.error.message
                                )
                            }
                            renderState(rebuildElements = false)
                            Toast.makeText(
                                this,
                                snapshotStatusOverride,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun availableSnapshotForCurrent(): SnapshotLookupResult.Available? {
        val state = controller.state
        return availableSnapshot(
            snapshotRepository.status(
                descriptor.widgetId,
                state.selectedVariantId,
                state.storedDesign.revision
            )
        )
    }

    private fun availableSnapshot(status: SnapshotStatus): SnapshotLookupResult.Available? =
        when (status) {
            is SnapshotStatus.Current -> status.snapshot
            is SnapshotStatus.Stale -> status.snapshot
            SnapshotStatus.Missing -> null
        }

    private fun snapshotCacheKey(metadata: com.example.blurwidgetdemo.studio.snapshot.WidgetSnapshotMetadata) =
        SnapshotCacheKey(
            metadata.widgetId,
            metadata.variantId,
            metadata.designRevision,
            metadata.snapshotPath,
            metadata.scope
        )

    private fun loadToTestWidget() {
        val beforeSave = controller.state
        val designExists = repository.load(descriptor.widgetId) is StudioRepositoryResult.Success
        if ((beforeSave.dirty || !designExists) && !saveDesign(showToast = false)) return
        val state = controller.state
        val variant = state.selectedVariantId as WidgetVariantId.Standard
        val canvasSize = canvasSizing.sizeFor(variant)
        val explicitCanvas = canvasSizing.source != StudioCanvasSizeSource.APPROXIMATE_GRID
        val result = developerWidgetLoadCoordinator.load(
            widgetId = descriptor.widgetId,
            variantId = variant,
            designRevision = state.storedDesign.revision,
            customWidthDp = canvasSize.widthDp.takeIf { explicitCanvas },
            customHeightDp = canvasSize.heightDp.takeIf { explicitCanvas }
        )
        repository.setSelectedVariantId(variant)
        renderState(rebuildElements = false)
        val message = if (result.updatedWidgetIds.isEmpty()) {
            getString(R.string.studio_loaded_add_test_widget)
        } else {
            getString(
                R.string.studio_loaded_test_widget_count,
                descriptor.displayName,
                variant.stableId,
                result.updatedWidgetIds.size
            )
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showCustomSizeDialog() {
        val variant = controller.state.selectedVariantId as WidgetVariantId.Standard
        val current = canvasSizing.sizeFor(variant)
        val widthInput = EditText(this).apply {
            hint = getString(R.string.studio_custom_width)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(formatNumber(current.widthDp))
        }
        val heightInput = EditText(this).apply {
            hint = getString(R.string.studio_custom_height)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(formatNumber(current.heightDp))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24f), dpToPx(8f), dpToPx(24f), 0)
            addView(widthInput)
            addView(heightInput)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.studio_custom_size_title)
            .setView(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val width = widthInput.text.toString().toFloatOrNull()
                val height = heightInput.text.toString().toFloatOrNull()
                if (width == null || !width.isFinite() || width <= 0f) {
                    widthInput.error = getString(R.string.studio_positive_number)
                    return@setOnClickListener
                }
                if (height == null || !height.isFinite() || height <= 0f) {
                    heightInput.error = getString(R.string.studio_positive_number)
                    return@setOnClickListener
                }
                canvasSizing.useCustomSize(width, height)
                dialog.dismiss()
                renderState(rebuildElements = false)
            }
        }
        dialog.show()
    }

    private fun restoreCanvasSizing(savedInstanceState: Bundle?) {
        val source = savedInstanceState?.getString(STATE_CANVAS_SIZE_SOURCE)
            ?.let { stored -> StudioCanvasSizeSource.entries.firstOrNull { it.name == stored } }
            ?: StudioCanvasSizeSource.APPROXIMATE_GRID
        val width = savedInstanceState?.getFloat(STATE_CANVAS_WIDTH_DP, Float.NaN)
            ?.takeIf(Float::isFinite)
        val height = savedInstanceState?.getFloat(STATE_CANVAS_HEIGHT_DP, Float.NaN)
            ?.takeIf(Float::isFinite)
        canvasSizing.restore(source, width, height)
    }

    private fun renderDragState() {
        val state = controller.state
        val variant = state.selectedVariantId as WidgetVariantId.Standard
        canvas.setEditorData(
            state.resolvedDesign,
            descriptor,
            state.selectedElementId,
            requestedCanvasSize = canvasSizing.sizeFor(variant),
            showGridGaps = canvasSizing.source == StudioCanvasSizeSource.APPROXIMATE_GRID
        )
        saveState.text = getString(R.string.studio_unsaved_state, state.workingDesign.revision)
        controller.selectedElement()?.let { element ->
            setNumberText(xInput, element.xDp)
            setNumberText(yInput, element.yDp)
        }
    }

    private fun rebuildElementList(state: WidgetStudioEditorState) {
        elementsContainer.removeAllViews()
        descriptor.elements.forEach { elementDescriptor ->
            val element = state.resolvedDesign?.elements?.firstOrNull {
                it.elementId == elementDescriptor.elementId
            } ?: return@forEach
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }
            val selectButton = Button(this).apply {
                text = elementDescriptor.displayName
                isAllCaps = false
                alpha = if (element.elementId == state.selectedElementId) 1f else 0.65f
                setOnClickListener {
                    controller.selectElement(element.elementId)
                    renderState()
                }
            }
            val visibility = CheckBox(this).apply {
                isChecked = element.visible
                contentDescription = getString(
                    R.string.studio_element_visibility_description,
                    elementDescriptor.displayName
                )
                visibility = if (ElementCapability.VISIBILITY in elementDescriptor.capabilities) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
                setOnCheckedChangeListener { _, checked ->
                    if (!rendering) {
                        controller.selectElement(element.elementId)
                        executeAndRender(WidgetStudioEditCommand.SetVisibility(element.elementId, checked))
                    }
                }
            }
            row.addView(
                selectButton,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
            row.addView(
                visibility,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            elementsContainer.addView(row)
        }
    }

    private fun renderInspector(state: WidgetStudioEditorState) {
        val element = state.resolvedDesign?.elements?.firstOrNull { it.elementId == state.selectedElementId }
        val elementDescriptor = descriptor.elements.firstOrNull { it.elementId == state.selectedElementId }
        selectedElement.text = element?.elementId.orEmpty()
        val capabilities = elementDescriptor?.capabilities.orEmpty()
        val sections = StudioInspectorSections.from(capabilities)
        visibilitySection.visibility = sectionVisibility(sections.visibility)
        positionSection.visibility = sectionVisibility(sections.position)
        sizeSection.visibility = sectionVisibility(sections.size)
        paddingSection.visibility = sectionVisibility(sections.padding)
        alignmentSection.visibility = sectionVisibility(sections.alignment)
        cornerRadiusSection.visibility = sectionVisibility(sections.cornerRadius)
        resetElementButton.isEnabled = element != null
        if (element == null) return
        visibleInput.isChecked = element.visible
        setNumberText(xInput, element.xDp)
        setNumberText(yInput, element.yDp)
        anchorSpinner.setSelection(anchors.indexOf(element.anchor))
        widthModeSpinner.setSelection(dimensionModes.indexOf(element.widthMode))
        heightModeSpinner.setSelection(dimensionModes.indexOf(element.heightMode))
        widthInput.isEnabled = element.widthMode == DimensionMode.FIXED
        heightInput.isEnabled = element.heightMode == DimensionMode.FIXED
        setNumberText(widthInput, element.widthDp)
        setNumberText(heightInput, element.heightDp)
        setNumberText(paddingStartInput, element.paddingStartDp)
        setNumberText(paddingTopInput, element.paddingTopDp)
        setNumberText(paddingEndInput, element.paddingEndDp)
        setNumberText(paddingBottomInput, element.paddingBottomDp)
        horizontalSpinner.setSelection(horizontalAlignments.indexOf(element.horizontalAlignment))
        verticalSpinner.setSelection(verticalAlignments.indexOf(element.verticalAlignment))
        setNumberText(cornerRadiusInput, element.cornerRadiusDp)
    }

    private fun sectionVisibility(visible: Boolean): Int = if (visible) View.VISIBLE else View.GONE

    private fun setNumberText(input: EditText, value: Float) {
        if (!input.hasFocus()) input.setText(formatNumber(value))
    }

    private fun formatNumber(value: Float): String = if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }

    private fun showCopyVariantDialog() {
        val sources = variants.filter { it != controller.state.selectedVariantId }
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_copy_title)
            .setItems(sources.map { it.stableId }.toTypedArray()) { _, which ->
                val source = sources[which]
                if (controller.targetVariantHasOverrides()) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.studio_copy_confirm_title)
                        .setMessage(getString(R.string.studio_copy_confirm_message, source.stableId))
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            executeAndRender(WidgetStudioEditCommand.CopyVariant(source))
                        }
                        .show()
                } else {
                    executeAndRender(WidgetStudioEditCommand.CopyVariant(source))
                }
            }
            .show()
    }

    private fun confirmResetVariant() {
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_reset_variant_title)
            .setMessage(
                getString(R.string.studio_reset_variant_message, controller.state.selectedVariantId.stableId)
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                executeAndRender(WidgetStudioEditCommand.ResetVariant)
            }
            .show()
    }

    private fun renderVerificationAndProduction(state: WidgetStudioEditorState) {
        val variant = state.selectedVariantId
        val revision = state.workingDesign.revision
        val key = "${descriptor.widgetId}|${variant.stableId}|$revision"
        if (verificationDraftKey != key) {
            verificationDraftKey = key
            verificationDraft = when (
                val loaded = verificationRepository.load(descriptor.widgetId, variant, revision)
            ) {
                is StudioRepositoryResult.Success -> loaded.value
                is StudioRepositoryResult.Failure -> WidgetParityVerification(
                    widgetId = descriptor.widgetId,
                    variantId = variant,
                    designRevision = revision
                )
            }
            bindVerificationDraft(requireNotNull(verificationDraft))
        }

        verificationStatus.text = when (
            val status = verificationRepository.status(descriptor.widgetId, variant, revision)
        ) {
            is VerificationRevisionStatus.Current -> buildString {
                append(status.assessment.status.name.replace('_', ' '))
                append(" · ").append(variant.stableId).append(" · rev ").append(revision)
                if (status.assessment.missingRequirements.isNotEmpty()) {
                    append("\nMissing: ").append(status.assessment.missingRequirements.joinToString())
                }
            }
            is VerificationRevisionStatus.Stale ->
                "STALE · verified rev ${status.verification.designRevision}, current rev $revision"
            VerificationRevisionStatus.NotTested -> "NOT TESTED · ${variant.stableId} · rev $revision"
        }
        renderDeviceInfo(requireNotNull(verificationDraft))

        val battery = descriptor.widgetId == com.example.blurwidgetdemo.studio.battery.BatteryStudioDescriptor.WIDGET_ID
        verifyProgress.visibility = if (battery) View.VISIBLE else View.GONE
        verifyRadius.visibility = if (battery) View.VISIBLE else View.GONE

        val active = when (val loaded = productionDesignRepository.loadActive(descriptor.widgetId)) {
            is StudioRepositoryResult.Success -> loaded.value
            is StudioRepositoryResult.Failure -> null
        }
        productionStatus.text = buildString {
            append("Current Studio: rev ").append(revision).append('\n')
            append("Verification: ").append(promotionVerificationStatus(state.workingDesign).name.replace('_', ' '))
            append('\n')
            if (active == null) {
                append("Production: legacy built-in design")
            } else {
                append("Production: rev ").append(active.metadata.designRevision)
                append(" · ").append(active.metadata.verificationStatus.name.replace('_', ' '))
                append("\nHash: ").append(active.metadata.sourceDesignHash.take(12))
                append("…\nStatus: ")
                append(
                    when {
                        active.metadata.designRevision == revision -> "Production matches current Studio revision"
                        revision > active.metadata.designRevision -> "Studio newer than production"
                        else -> "Production revision differs from Studio"
                    }
                )
            }
        }
        productionMapping.text = productionMappingSummary(descriptor.widgetId)
        usePromotedForNew.isChecked = productionModeRepository.usePromotedForNewWidgets(descriptor.widgetId)
        promoteProduction.isEnabled = !state.dirty && state.workingDesign.revision > 0L
        revertProduction.isEnabled = active != null || productionHistory().isNotEmpty()
    }

    private fun bindVerificationDraft(value: WidgetParityVerification) {
        oneUiVersion.setText(value.oneUiVersion)
        verifyActualPreview.isChecked = value.actualPreviewVerified
        verifyDeveloperWidget.isChecked = value.developerWidgetVerified
        verifyBlur.isChecked = value.nativeBlurVerified
        verifyGeometry.isChecked = value.geometryVerified
        verifyVisibility.isChecked = value.visibilityVerified
        verifyPadding.isChecked = value.paddingVerified
        verifyAlignment.isChecked = value.alignmentVerified
        verifyRadius.isChecked = value.radiusVerified
        verifyProgress.isChecked = value.progressVerified
        verifySnapshot.isChecked = value.snapshotVerified
        verificationFailed.isChecked = value.failed
        verificationNotes.setText(value.notes)
    }

    private fun renderDeviceInfo(value: WidgetParityVerification) {
        deviceInfo.text = if (
            value.deviceManufacturer.isBlank() && value.deviceModel.isBlank() && value.androidVersion.isBlank()
        ) {
            getString(R.string.studio_device_info_not_captured)
        } else {
            buildString {
                append("Device: ").append(value.deviceManufacturer).append(' ').append(value.deviceModel)
                append("\nAndroid: ").append(value.androidVersion).append(" (API ").append(value.androidSdkInt).append(')')
                append("\nOne UI: ").append(value.oneUiVersion.ifBlank { "manual entry required" })
                append("\nOne UI Home: ").append(value.launcherVersion.ifBlank { "unavailable" })
            }
        }
    }

    private fun captureDeviceInfo() {
        val current = readVerificationInputs(verifiedAt = verificationDraft?.verifiedAt ?: 0L)
        val captured = DeviceTechnicalInfoCapture.capture(this, oneUiVersion.text.toString())
        verificationDraft = current.copy(
            deviceManufacturer = captured.manufacturer,
            deviceModel = captured.model,
            androidVersion = captured.androidVersion,
            androidSdkInt = captured.androidSdkInt,
            oneUiVersion = captured.oneUiVersion,
            launcherVersion = captured.launcherVersion
        )
        renderDeviceInfo(requireNotNull(verificationDraft))
    }

    private fun saveVerification() {
        if (controller.state.dirty || controller.state.workingDesign.revision <= 0L) {
            Toast.makeText(this, R.string.studio_verification_unsaved, Toast.LENGTH_LONG).show()
            return
        }
        val value = readVerificationInputs(verifiedAt = System.currentTimeMillis())
        when (val result = verificationRepository.save(value)) {
            is StudioRepositoryResult.Success -> {
                verificationDraft = result.value
                Toast.makeText(this, R.string.studio_verification_saved, Toast.LENGTH_SHORT).show()
                renderState(rebuildElements = false)
            }
            is StudioRepositoryResult.Failure -> Toast.makeText(this, result.error.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun readVerificationInputs(verifiedAt: Long): WidgetParityVerification {
        val state = controller.state
        val technical = verificationDraft ?: WidgetParityVerification(
            widgetId = descriptor.widgetId,
            variantId = state.selectedVariantId,
            designRevision = state.workingDesign.revision
        )
        return technical.copy(
            widgetId = descriptor.widgetId,
            variantId = state.selectedVariantId,
            designRevision = state.workingDesign.revision,
            oneUiVersion = oneUiVersion.text.toString().trim(),
            actualPreviewVerified = verifyActualPreview.isChecked,
            developerWidgetVerified = verifyDeveloperWidget.isChecked,
            nativeBlurVerified = verifyBlur.isChecked,
            geometryVerified = verifyGeometry.isChecked,
            visibilityVerified = verifyVisibility.isChecked,
            paddingVerified = verifyPadding.isChecked,
            alignmentVerified = verifyAlignment.isChecked,
            radiusVerified = verifyRadius.isChecked,
            progressVerified = verifyProgress.isChecked,
            snapshotVerified = verifySnapshot.isChecked,
            failed = verificationFailed.isChecked,
            notes = verificationNotes.text.toString().trim(),
            verifiedAt = verifiedAt
        )
    }

    private fun promotionVerificationStatus(design: WidgetDesignSpec): WidgetVerificationStatus {
        val variants = ProductionVariantResolver.mappingFor(design.widgetId).values.distinct()
        if (variants.isEmpty()) return WidgetVerificationStatus.NOT_TESTED
        val statuses = variants.map { variant ->
            verificationRepository.status(design.widgetId, variant, design.revision)
        }
        if (statuses.all {
                it is VerificationRevisionStatus.Current &&
                    it.assessment.status == WidgetVerificationStatus.VERIFIED
            }
        ) return WidgetVerificationStatus.VERIFIED
        if (statuses.any {
                it is VerificationRevisionStatus.Current &&
                    it.assessment.status == WidgetVerificationStatus.FAILED
            }
        ) return WidgetVerificationStatus.FAILED
        val hasEvidence = statuses.any { status ->
            status is VerificationRevisionStatus.Stale ||
                status is VerificationRevisionStatus.Current &&
                status.assessment.status != WidgetVerificationStatus.NOT_TESTED
        }
        return if (hasEvidence) WidgetVerificationStatus.PARTIALLY_VERIFIED
        else WidgetVerificationStatus.NOT_TESTED
    }

    private fun promotionSnapshotStatus(design: WidgetDesignSpec): PromotionSnapshotStatus {
        val statuses = ProductionVariantResolver.mappingFor(design.widgetId).values.distinct().map { variant ->
            snapshotRepository.status(design.widgetId, variant, design.revision)
        }
        return when {
            statuses.isNotEmpty() && statuses.all { it is SnapshotStatus.Current } -> PromotionSnapshotStatus.CURRENT
            statuses.any { it is SnapshotStatus.Stale } -> PromotionSnapshotStatus.STALE
            else -> PromotionSnapshotStatus.MISSING
        }
    }

    private fun promotionRequest(): PromotionRequest {
        val design = controller.state.workingDesign
        val previous = when (val active = productionDesignRepository.loadActive(design.widgetId)) {
            is StudioRepositoryResult.Success -> active.value?.design
            is StudioRepositoryResult.Failure -> null
        }
        return PromotionRequest(
            descriptor = descriptor,
            design = design,
            verificationStatus = promotionVerificationStatus(design),
            snapshotStatus = promotionSnapshotStatus(design),
            previousProductionDesign = previous,
            androidSdkInt = android.os.Build.VERSION.SDK_INT
        )
    }

    private fun reviewPromotion() {
        if (controller.state.dirty || controller.state.workingDesign.revision <= 0L) {
            Toast.makeText(this, R.string.studio_verification_unsaved, Toast.LENGTH_LONG).show()
            return
        }
        val request = promotionRequest()
        val review = WidgetPromotionReviewer.review(request)
        if (review.blockingErrors.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.studio_promotion_blocked_title)
                .setMessage(review.blockingErrors.joinToString("\n"))
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val message = buildString {
            append("Widget: ").append(descriptor.displayName)
            append("\nRevision: ").append(review.revision)
            append("\nVerification: ").append(review.verificationStatus.name.replace('_', ' '))
            append("\nVariants in Studio: ").append(descriptor.supportedVariants.size)
            append("\n\n").append(productionMappingSummary(descriptor.widgetId))
            append("\n\nSnapshot: ").append(review.snapshotStatus.name)
            if (review.compatibilityWarnings.isNotEmpty()) {
                append("\n\nProduction compatibility warning:\n")
                append(review.compatibilityWarnings.joinToString("\n"))
            }
            if (review.requiresVerificationOverride) {
                append("\n\nVerification is incomplete. Continuing records this promotion as ")
                append(review.verificationStatus.name.replace('_', ' ')).append('.')
            }
            if (review.requiresSnapshotOverride) {
                append("\n\nNo current snapshot exists for every production-mapped variant. ")
                append("No stale snapshot will be promoted silently.")
            }
            append("\n\nDesign changes:\n").append(WidgetDesignDiff.summary(review.differences))
        }
        val hasWarnings = review.requiresVerificationOverride || review.requiresSnapshotOverride ||
            review.compatibilityWarnings.isNotEmpty()
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_promotion_review_title)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(
                when {
                    review.requiresVerificationOverride -> R.string.studio_promotion_incomplete_override
                    review.requiresSnapshotOverride -> R.string.studio_promotion_snapshot_override
                    hasWarnings -> R.string.studio_promotion_override
                    else -> R.string.studio_promotion_confirm
                }
            ) { _, _ -> completePromotion(request) }
            .show()
    }

    private fun completePromotion(request: PromotionRequest) {
        when (
            val result = promotionCoordinator.promote(
                request,
                allowIncompleteVerification = true,
                allowSnapshotWarning = true
            )
        ) {
            is PromotionResult.Success -> {
                val variants = ProductionVariantResolver.mappingFor(request.design.widgetId).values
                val snapshotCopy = productionSnapshotRepository.promoteCurrent(
                    request.design.widgetId,
                    request.design.revision,
                    variants,
                    snapshotRepository
                )
                productionModeRepository.setUsePromotedForNewWidgets(request.design.widgetId, true)
                val message = if (snapshotCopy is StudioRepositoryResult.Failure) {
                    getString(R.string.studio_promotion_success) + "\n" + snapshotCopy.error.message
                } else {
                    getString(R.string.studio_promotion_success)
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                renderState(rebuildElements = false)
            }
            is PromotionResult.Rejected -> AlertDialog.Builder(this)
                .setTitle(R.string.studio_promotion_blocked_title)
                .setMessage(result.reasons.joinToString("\n"))
                .setPositiveButton(android.R.string.ok, null)
                .show()
            is PromotionResult.Failed -> Toast.makeText(
                this,
                getString(R.string.studio_promotion_failed, result.message),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun productionMappingSummary(widgetId: String): String = buildString {
        append("Production size mapping")
        ProductionSizeCategory.entries.forEach { category ->
            val variant = ProductionVariantResolver.mappingFor(widgetId)[category] ?: return@forEach
            append("\n").append(category.name.lowercase().replaceFirstChar { it.uppercase() })
            append(" -> ").append(variant.stableId)
        }
    }

    private fun productionHistory() = when (val result = productionDesignRepository.history(descriptor.widgetId)) {
        is StudioRepositoryResult.Success -> result.value
        is StudioRepositoryResult.Failure -> emptyList()
    }

    private fun showProductionHistory() {
        val history = productionHistory()
        val labels = buildList {
            add(getString(R.string.studio_revert_legacy))
            history.forEach { metadata ->
                add(
                    "rev ${metadata.designRevision} · ${metadata.verificationStatus.name.replace('_', ' ')} · " +
                        metadata.sourceDesignHash.take(10)
                )
            }
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_revert_production)
            .setItems(labels.toTypedArray()) { _, which ->
                val revision = history.getOrNull(which - 1)?.designRevision
                confirmProductionActivation(revision)
            }
            .show()
    }

    private fun confirmProductionActivation(revision: Long?) {
        val label = revision?.let { "revision $it" } ?: getString(R.string.studio_revert_legacy)
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_revert_confirm_title)
            .setMessage(label)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val success = if (revision == null) {
                    productionModeRepository.setUsePromotedForNewWidgets(descriptor.widgetId, false)
                    productionDesignRepository.revertToLegacy(descriptor.widgetId) is StudioRepositoryResult.Success
                } else {
                    productionDesignRepository.activateRevision(descriptor.widgetId, revision) is StudioRepositoryResult.Success
                }
                if (success) {
                    Toast.makeText(this, R.string.studio_revert_success, Toast.LENGTH_SHORT).show()
                    renderState(rebuildElements = false)
                }
            }
            .show()
    }

    private fun confirmResetWidget() {
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_reset_widget_title)
            .setMessage(R.string.studio_reset_widget_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                executeAndRender(WidgetStudioEditCommand.ResetWidget)
            }
            .show()
    }

    private fun saveDesign(showToast: Boolean): Boolean = when (val result = controller.save()) {
        is EditorActionResult.Success -> {
            repository.setSelectedVariantId(controller.state.selectedVariantId)
            renderState()
            if (showToast) Toast.makeText(this, R.string.studio_saved_message, Toast.LENGTH_SHORT).show()
            true
        }
        is EditorActionResult.Failure -> {
            val message = result.issues.takeIf { it.isNotEmpty() }
                ?.joinToString("\n") { "${it.field}: ${it.message}" }
                ?: result.message
            AlertDialog.Builder(this)
                .setTitle(R.string.studio_validation_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            renderState()
            false
        }
    }

    private fun showInitializationResult(result: EditorActionResult) {
        if (result is EditorActionResult.Failure) {
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreEditorState(savedInstanceState: Bundle?): Boolean {
        savedInstanceState ?: return false
        val stored = decodeDesign(savedInstanceState.getString(STATE_STORED_DESIGN)) ?: return false
        val working = decodeDesign(savedInstanceState.getString(STATE_WORKING_DESIGN)) ?: return false
        val variant = savedInstanceState.getString(STATE_VARIANT_ID)?.let(WidgetVariantId::parse) ?: return false
        val element = savedInstanceState.getString(STATE_ELEMENT_ID)
        return controller.restore(stored, working, variant, element) is EditorActionResult.Success
    }

    private fun decodeDesign(json: String?): WidgetDesignSpec? = when (val result = json?.let(codec::decode)) {
        is DesignDecodeResult.Success -> result.design
        else -> null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val state = controller.state
        val canvasSize = canvasSizing.sizeFor(state.selectedVariantId as WidgetVariantId.Standard)
        outState.putString(STATE_STORED_DESIGN, codec.encode(state.storedDesign))
        outState.putString(STATE_WIDGET_ID, descriptor.widgetId)
        outState.putString(STATE_WORKING_DESIGN, codec.encode(state.workingDesign))
        outState.putString(STATE_VARIANT_ID, state.selectedVariantId.stableId)
        outState.putString(STATE_ELEMENT_ID, state.selectedElementId)
        outState.putBoolean(STATE_SHOW_HIDDEN, showHidden.isChecked)
        outState.putBoolean(STATE_LINK_PADDING, linkPadding)
        outState.putString(STATE_SNAP_MODE, canvas.snapMode.name)
        outState.putInt(STATE_ZOOM_PERCENT, canvas.zoomPercent())
        outState.putString(STATE_PREVIEW_MODE, previewMode.name)
        outState.putBoolean(
            STATE_RENDER_DIAGNOSTICS_EXPANDED,
            renderDiagnosticsToggle.isChecked
        )
        outState.putString(STATE_CANVAS_SIZE_SOURCE, canvasSizing.source.name)
        outState.putFloat(STATE_CANVAS_WIDTH_DP, canvasSize.widthDp)
        outState.putFloat(STATE_CANVAS_HEIGHT_DP, canvasSize.heightDp)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (::controller.isInitialized && ::testWidgetStatus.isInitialized) {
            renderState(rebuildElements = false)
        }
    }

    override fun onDestroy() {
        snapshotIo.shutdown()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingSuperCall") // Super is called only after Save/Discard resolves.
    override fun onBackPressed() {
        if (!controller.state.dirty) {
            leaveStudio()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.studio_save_changes_title)
            .setMessage(R.string.studio_save_changes_message)
            .setPositiveButton(R.string.studio_save) { _, _ ->
                if (saveDesign(showToast = false)) leaveStudio()
            }
            .setNegativeButton(R.string.studio_discard) { _, _ -> leaveStudio() }
            .setNeutralButton(android.R.string.cancel, null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun leaveStudio() {
        super.onBackPressed()
    }

    companion object {
        private enum class PreviewMode { EDIT, ACTUAL }

        private const val STATE_STORED_DESIGN = "studio_stored_design"
        private const val STATE_WIDGET_ID = "studio_widget_id"
        private const val STATE_WORKING_DESIGN = "studio_working_design"
        private const val STATE_VARIANT_ID = "studio_variant_id"
        private const val STATE_ELEMENT_ID = "studio_element_id"
        private const val STATE_SHOW_HIDDEN = "studio_show_hidden"
        private const val STATE_LINK_PADDING = "studio_link_padding"
        private const val STATE_SNAP_MODE = "studio_snap_mode"
        private const val STATE_ZOOM_PERCENT = "studio_zoom_percent"
        private const val STATE_PREVIEW_MODE = "studio_preview_mode"
        private const val STATE_RENDER_DIAGNOSTICS_EXPANDED =
            "studio_render_diagnostics_expanded"
        private const val EDIT_PREVIEW_HEIGHT_DP = 360f
        private const val STATE_CANVAS_SIZE_SOURCE = "studio_canvas_size_source"
        private const val STATE_CANVAS_WIDTH_DP = "studio_canvas_width_dp"
        private const val STATE_CANVAS_HEIGHT_DP = "studio_canvas_height_dp"
    }
}
