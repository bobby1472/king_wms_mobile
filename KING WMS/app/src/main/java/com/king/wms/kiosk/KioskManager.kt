package com.king.wms.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/**
 * Controls single-app kiosk (Lock Task) mode.
 *
 * Two levels of lockdown:
 *  1. App pinning  — works without Device Owner, but the user can exit by holding
 *                    Back + Recents. Fine for testing.
 *  2. Device Owner — true kiosk. The app is whitelisted and the user CANNOT leave.
 *                    Requires provisioning the device as Device Owner first:
 *                      adb shell dpm set-device-owner com.king.wms/.kiosk.AdminReceiver
 *                    (Device must be freshly reset with no accounts added.)
 */
class KioskManager(private val context: Context) {

    private val dpm =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = AdminReceiver.componentName(context)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /** Call once after becoming Device Owner to harden the device into a kiosk. */
    fun applyKioskPolicies() {
        if (!isDeviceOwner()) return

        // Only KING WMS may run in lock task mode.
        dpm.setLockTaskPackages(admin, arrayOf(context.packageName))

        // Auto-relaunch this app as the home screen.
        val filter = android.content.IntentFilter(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_HOME)
            addCategory(android.content.Intent.CATEGORY_DEFAULT)
        }
        dpm.addPersistentPreferredActivity(
            admin, filter,
            android.content.ComponentName(context, "com.king.wms.MainActivity")
        )

        // Lock down disruptive features.
        runCatching { dpm.setStatusBarDisabled(admin, true) }
        runCatching { dpm.setKeyguardDisabled(admin, true) }
        runCatching {
            dpm.addUserRestriction(admin, "no_safe_boot")
            dpm.addUserRestriction(admin, "no_factory_reset")
            dpm.addUserRestriction(admin, "no_add_user")
            dpm.addUserRestriction(admin, "no_install_unknown_sources")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Stay awake while plugged into the charging dock at the workstation.
            runCatching {
                dpm.setGlobalSetting(admin, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "7")
            }
        }
    }

    /** Enter the kiosk. Safe to call whether or not we are Device Owner. */
    fun startKiosk(activity: Activity) {
        try {
            activity.startLockTask()
        } catch (_: Exception) {
            // Lock task not permitted (e.g. not whitelisted) — ignore for testing builds.
        }
    }

    /** Exit kiosk (use for an authenticated supervisor exit). */
    fun stopKiosk(activity: Activity) {
        runCatching { activity.stopLockTask() }
    }
}
