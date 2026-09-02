package com.example.blurwidgetdemo.studio.developer

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.blurwidgetdemo.widgets.developer.DeveloperWidgetProvider

interface DeveloperWidgetComponentGateway {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}
class AndroidDeveloperWidgetComponentGateway(
    context: Context
) : DeveloperWidgetComponentGateway {
    private val appContext = context.applicationContext
    private val component = ComponentName(appContext, DeveloperWidgetProvider::class.java)

    override fun isEnabled(): Boolean =
        appContext.packageManager.getComponentEnabledSetting(component) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    override fun setEnabled(enabled: Boolean) {
        appContext.packageManager.setComponentEnabledSetting(
            component,
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP
        )
    }
}

class DeveloperWidgetComponentController(
    private val gateway: DeveloperWidgetComponentGateway
) {
    constructor(context: Context) : this(AndroidDeveloperWidgetComponentGateway(context))

    fun isEnabled(): Boolean = gateway.isEnabled()

    fun enable() = gateway.setEnabled(true)

    fun disable() = gateway.setEnabled(false)

    fun synchronizeWithDeveloperMode(developerModeEnabled: Boolean) {
        if (developerModeEnabled != gateway.isEnabled()) {
            gateway.setEnabled(developerModeEnabled)
        }
    }
}

/** Keeps the developer flag and provider component transition in one testable operation. */
class DeveloperModeCoordinator(
    private val repository: DeveloperModeRepository,
    private val componentController: DeveloperWidgetComponentController
) {
    fun setEnabled(enabled: Boolean) {
        repository.setEnabled(enabled)
        componentController.synchronizeWithDeveloperMode(enabled)
    }

    fun synchronize() {
        componentController.synchronizeWithDeveloperMode(repository.isEnabled())
    }
}
