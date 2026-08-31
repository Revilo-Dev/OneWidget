package com.example.blurwidgetdemo.studio.model

/** A stable design variant identity. Grid dimensions do not imply a fixed dp canvas size. */
sealed class WidgetVariantId {
    abstract val stableId: String

    data class Standard(val columns: Int, val rows: Int) : WidgetVariantId() {
        override val stableId: String = "${columns}x$rows"
    }

    data class Custom(val id: String) : WidgetVariantId() {
        override val stableId: String = "custom:$id"
    }

    fun isValid(): Boolean = when (this) {
        is Standard -> columns in MIN_GRID_SIZE..MAX_GRID_SIZE && rows in MIN_GRID_SIZE..MAX_GRID_SIZE
        is Custom -> id.isNotBlank() && CUSTOM_ID.matches(id)
    }

    companion object {
        const val MIN_GRID_SIZE = 1
        const val MAX_GRID_SIZE = 5
        private val STANDARD_ID = Regex("^([1-5])x([1-5])$")
        private val CUSTOM_ID = Regex("^[A-Za-z0-9._-]+$")

        val standardVariants: List<Standard> = (MIN_GRID_SIZE..MAX_GRID_SIZE).flatMap { rows ->
            (MIN_GRID_SIZE..MAX_GRID_SIZE).map { columns -> Standard(columns, rows) }
        }

        fun parse(stableId: String): WidgetVariantId? {
            STANDARD_ID.matchEntire(stableId)?.let { match ->
                return Standard(match.groupValues[1].toInt(), match.groupValues[2].toInt())
            }
            if (stableId.startsWith("custom:")) {
                return Custom(stableId.removePrefix("custom:")).takeIf { it.isValid() }
            }
            return null
        }
    }
}
