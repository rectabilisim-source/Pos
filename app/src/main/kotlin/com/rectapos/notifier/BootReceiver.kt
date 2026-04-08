package com.rectapos.notifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Telefon yeniden başladığında veya uygulama güncellendiğinde servisi başlatır. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" // HTC / bazı cihazlar
        )
        if (intent.action !in validActions) return
        if (Prefs.isEnabled(context) && Prefs.isConfigured(context)) {
            WatcherService.start(context)
        }
    }
}
