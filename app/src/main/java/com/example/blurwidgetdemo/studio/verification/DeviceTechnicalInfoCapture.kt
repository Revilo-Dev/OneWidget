package com.example.blurwidgetdemo.studio.verification

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

data class DeviceTechnicalInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val androidSdkInt: Int,
    val oneUiVersion: String,
    val launcherVersion: String
)

object DeviceTechnicalInfoCapture {
    private const val ONE_UI_HOME_PACKAGE = "com.sec.android.app.launcher"

    fun capture(context: Context, manualOneUiVersion: String): DeviceTechnicalInfo = DeviceTechnicalInfo(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        androidVersion = Build.VERSION.RELEASE.orEmpty(),
        androidSdkInt = Build.VERSION.SDK_INT,
        oneUiVersion = manualOneUiVersion.trim(),
        launcherVersion = launcherVersion(context.packageManager)
    )

    @Suppress("DEPRECATION")
    private fun launcherVersion(packageManager: PackageManager): String = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            packageManager.getPackageInfo(
                ONE_UI_HOME_PACKAGE,
                PackageManager.PackageInfoFlags.of(0L)
            ).versionName
        } else {
            packageManager.getPackageInfo(ONE_UI_HOME_PACKAGE, 0).versionName
        }
    }.getOrNull().orEmpty()
}
