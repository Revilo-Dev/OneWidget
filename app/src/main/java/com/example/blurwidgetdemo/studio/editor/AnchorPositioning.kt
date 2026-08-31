package com.example.blurwidgetdemo.studio.editor

import com.example.blurwidgetdemo.studio.model.WidgetAnchor

data class StudioPoint(val x: Float, val y: Float)

/** x/y are offsets from the anchor's canvas point to the element centre. */
object AnchorPositioning {
    fun anchorPoint(anchor: WidgetAnchor, widthDp: Float, heightDp: Float): StudioPoint {
        val x = when (anchor) {
            WidgetAnchor.TOP_START, WidgetAnchor.CENTER_START, WidgetAnchor.BOTTOM_START -> 0f
            WidgetAnchor.TOP_CENTER, WidgetAnchor.CENTER, WidgetAnchor.BOTTOM_CENTER -> widthDp / 2f
            WidgetAnchor.TOP_END, WidgetAnchor.CENTER_END, WidgetAnchor.BOTTOM_END -> widthDp
        }
        val y = when (anchor) {
            WidgetAnchor.TOP_START, WidgetAnchor.TOP_CENTER, WidgetAnchor.TOP_END -> 0f
            WidgetAnchor.CENTER_START, WidgetAnchor.CENTER, WidgetAnchor.CENTER_END -> heightDp / 2f
            WidgetAnchor.BOTTOM_START, WidgetAnchor.BOTTOM_CENTER, WidgetAnchor.BOTTOM_END -> heightDp
        }
        return StudioPoint(x, y)
    }

    fun preservePosition(
        oldAnchor: WidgetAnchor,
        newAnchor: WidgetAnchor,
        xDp: Float,
        yDp: Float,
        canvasWidthDp: Float,
        canvasHeightDp: Float
    ): StudioPoint {
        val oldPoint = anchorPoint(oldAnchor, canvasWidthDp, canvasHeightDp)
        val newPoint = anchorPoint(newAnchor, canvasWidthDp, canvasHeightDp)
        return StudioPoint(oldPoint.x + xDp - newPoint.x, oldPoint.y + yDp - newPoint.y)
    }
}
