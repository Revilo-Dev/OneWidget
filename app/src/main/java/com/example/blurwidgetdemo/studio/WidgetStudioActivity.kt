package com.example.blurwidgetdemo.studio

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.blurwidgetdemo.R
import com.example.blurwidgetdemo.studio.canvas.StudioPreviewMetrics
import com.example.blurwidgetdemo.studio.canvas.StudioSnapMode
import com.example.blurwidgetdemo.studio.canvas.WidgetStudioCanvasView
import com.example.blurwidgetdemo.studio.data.WidgetDesignRepository
import com.example.blurwidgetdemo.studio.developer.DeveloperModeRepository
import com.example.blurwidgetdemo.studio.editor.EditorActionResult
import com.example.blurwidgetdemo.studio.editor.WidgetStudioEditCommand
import com.example.blurwidgetdemo.studio.editor.WidgetStudioEditorController
import com.example.blurwidgetdemo.studio.editor.WidgetStudioEditorState
import com.example.blurwidgetdemo.studio.model.DesignDecodeResult
import com.example.blurwidgetdemo.studio.model.DimensionMode
import com.example.blurwidgetdemo.studio.model.ElementCapability
import com.example.blurwidgetdemo.studio.model.HorizontalAlignment
import com.example.blurwidgetdemo.studio.model.VerticalAlignment
import com.example.blurwidgetdemo.studio.model.WidgetAnchor
import com.example.blurwidgetdemo.studio.model.WidgetDesignJsonCodec
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetVariantId
import com.example.blurwidgetdemo.studio.registry.DefaultWidgetStudioRegistry
import com.example.blurwidgetdemo.studio.registry.StudioWidgetDescriptor
import java.util.Locale

/** Developer-only Phase 2 editor. It does not participate in production widget rendering. */
class WidgetStudioActivity : AppCompatActivity() {
    private lateinit var descriptor: StudioWidgetDescriptor
    private lateinit var repository: WidgetDesignRepository
    private lateinit var controller: WidgetStudioEditorController
    private val codec = WidgetDesignJsonCodec()
    private val previewMetrics = StudioPreviewMetrics()
    private val variants: List<WidgetVariantId.Standard> = WidgetVariantId.standardVariants
    private val snapModes = StudioSnapMode.entries.toList()
    private val anchors = WidgetAnchor.entries.toList()
    private val dimensionModes = DimensionMode.entries.toList()
    private val horizontalAlignments = HorizontalAlignment.entries.toList()
    private val verticalAlignments = VerticalAlignment.entries.toList()

    private lateinit var canvas: WidgetStudioCanvasView
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
    private lateinit var resetElementButton: Button
    private var rendering = false
    private var linkPadding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!DeveloperModeRepository(this).isEnabled()) {
            finish()
            return
        }
        setContentView(R.layout.activity_widget_studio)
        descriptor = requireNotNull(
            DefaultWidgetStudioRegistry.create().descriptor("digital_clock")
        )
        repository = WidgetDesignRepository(this)
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
        savedInstanceState?.getString(STATE_SNAP_MODE)?.let { name ->
            canvas.snapMode = snapModes.firstOrNull { it.name == name } ?: StudioSnapMode.ONE_DP
        }
        renderState()
        savedInstanceState?.getInt(STATE_ZOOM_PERCENT, 0)?.takeIf { it > 0 }?.let { percent ->
            canvas.post { canvas.setZoomPercent(percent) }
        }
    }

    private fun bindViews() {
        canvas = findViewById(R.id.studio_canvas)
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
        resetElementButton = findViewById(R.id.studio_reset_element)
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
            val canvasSize = previewMetrics.canvasSize(standard)
            canvasDiagnostics.text = getString(
                R.string.studio_canvas_diagnostics,
                standard.stableId,
                canvasSize.widthDp,
                canvasSize.heightDp
            )
            saveState.text = getString(
                if (state.dirty) R.string.studio_unsaved_state else R.string.studio_saved_state,
                state.workingDesign.revision
            )
            undoButton.isEnabled = state.canUndo
            redoButton.isEnabled = state.canRedo
            saveButton.isEnabled = state.dirty
            canvas.showHiddenElements = showHidden.isChecked
            canvas.setEditorData(state.resolvedDesign, descriptor, state.selectedElementId)
            zoomValue.text = getString(R.string.studio_zoom_percent, canvas.zoomPercent())
            if (rebuildElements) rebuildElementList(state)
            renderInspector(state)
        } finally {
            rendering = false
        }
    }

    private fun renderDragState() {
        val state = controller.state
        canvas.setEditorData(state.resolvedDesign, descriptor, state.selectedElementId)
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
        visibilitySection.visibility = capabilityVisibility(capabilities, ElementCapability.VISIBILITY)
        positionSection.visibility = capabilityVisibility(capabilities, ElementCapability.POSITION)
        sizeSection.visibility = capabilityVisibility(capabilities, ElementCapability.SIZE)
        paddingSection.visibility = capabilityVisibility(capabilities, ElementCapability.PADDING)
        alignmentSection.visibility = capabilityVisibility(capabilities, ElementCapability.ALIGNMENT)
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
    }

    private fun capabilityVisibility(
        capabilities: Set<ElementCapability>,
        capability: ElementCapability
    ): Int = if (capability in capabilities) View.VISIBLE else View.GONE

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
        outState.putString(STATE_STORED_DESIGN, codec.encode(state.storedDesign))
        outState.putString(STATE_WORKING_DESIGN, codec.encode(state.workingDesign))
        outState.putString(STATE_VARIANT_ID, state.selectedVariantId.stableId)
        outState.putString(STATE_ELEMENT_ID, state.selectedElementId)
        outState.putBoolean(STATE_SHOW_HIDDEN, showHidden.isChecked)
        outState.putBoolean(STATE_LINK_PADDING, linkPadding)
        outState.putString(STATE_SNAP_MODE, canvas.snapMode.name)
        outState.putInt(STATE_ZOOM_PERCENT, canvas.zoomPercent())
        super.onSaveInstanceState(outState)
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
        private const val STATE_STORED_DESIGN = "studio_stored_design"
        private const val STATE_WORKING_DESIGN = "studio_working_design"
        private const val STATE_VARIANT_ID = "studio_variant_id"
        private const val STATE_ELEMENT_ID = "studio_element_id"
        private const val STATE_SHOW_HIDDEN = "studio_show_hidden"
        private const val STATE_LINK_PADDING = "studio_link_padding"
        private const val STATE_SNAP_MODE = "studio_snap_mode"
        private const val STATE_ZOOM_PERCENT = "studio_zoom_percent"
    }
}
