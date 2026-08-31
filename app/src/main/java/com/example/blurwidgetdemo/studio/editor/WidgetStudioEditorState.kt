package com.example.blurwidgetdemo.studio.editor

import com.example.blurwidgetdemo.studio.model.ResolvedWidgetDesign
import com.example.blurwidgetdemo.studio.model.ValidationIssue
import com.example.blurwidgetdemo.studio.model.WidgetDesignSpec
import com.example.blurwidgetdemo.studio.model.WidgetVariantId

data class WidgetStudioEditorState(
    val selectedWidgetId: String,
    val selectedVariantId: WidgetVariantId,
    val storedDesign: WidgetDesignSpec,
    val workingDesign: WidgetDesignSpec,
    val resolvedDesign: ResolvedWidgetDesign?,
    val selectedElementId: String?,
    val dirty: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val validationIssues: List<ValidationIssue> = emptyList()
)

sealed class EditorActionResult {
    data class Success(val changed: Boolean = true) : EditorActionResult()
    data class Failure(
        val message: String,
        val issues: List<ValidationIssue> = emptyList()
    ) : EditorActionResult()
}
