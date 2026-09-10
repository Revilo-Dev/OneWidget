package dev.revilodev.onewidget.studio.preview

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import dev.revilodev.onewidget.R
import dev.revilodev.onewidget.WidgetPreviewEnvironment

data class RemoteViewsApplyResult(
    val successful: Boolean,
    val error: String? = null
)

sealed class RemoteViewsApplyAttempt<out T> {
    data class Success<T>(val value: T) : RemoteViewsApplyAttempt<T>()
    data class Failure(val message: String) : RemoteViewsApplyAttempt<Nothing>()
}

/** Pure failure boundary used by the Android host and unit tests. */
object RemoteViewsApplyGuard {
    fun <T> run(apply: () -> T): RemoteViewsApplyAttempt<T> = try {
        RemoteViewsApplyAttempt.Success(apply())
    } catch (error: RuntimeException) {
        RemoteViewsApplyAttempt.Failure(error.message ?: error.javaClass.simpleName)
    }
}

/** Reusable local host for real RemoteViews hierarchies. */
open class RemoteViewsPreviewHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    init {
        WidgetPreviewEnvironment.apply(this)
    }

    fun show(remoteViews: RemoteViews): RemoteViewsApplyResult {
        removeAllViews()
        return when (val attempt = RemoteViewsApplyGuard.run { remoteViews.apply(context, this) }) {
            is RemoteViewsApplyAttempt.Success -> {
                addView(
                    attempt.value,
                    LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                RemoteViewsApplyResult(successful = true)
            }
            is RemoteViewsApplyAttempt.Failure -> {
                addView(
                    TextView(context).apply {
                        gravity = Gravity.CENTER
                        setPadding(24, 24, 24, 24)
                        text = context.getString(R.string.studio_actual_apply_error, attempt.message)
                    },
                    LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                RemoteViewsApplyResult(successful = false, error = attempt.message)
            }
        }
    }
}

/** Backwards-compatible Studio XML name; the behavior now lives in the shared host. */
class ActualRemoteViewsPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RemoteViewsPreviewHost(context, attrs)
