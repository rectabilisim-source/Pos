package com.rectapos.notifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Yedek BroadcastReceiver — WatcherService kapalıyken devreye girer.
 * Asıl numara okuma WatcherService > PhoneStateListener üzerinden yapılır.
 */
class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return
        if (!Prefs.isEnabled(context) || !Prefs.isConfigured(context)) return

        // EXTRA_INCOMING_NUMBER bazı cihazlarda broadcast'te hâlâ gelir
        @Suppress("DEPRECATION")
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            ?.takeIf { it.isNotBlank() } ?: return  // boşsa WatcherService halledecek

        Log.d("RectaPOS", "CallReceiver (yedek) - Arama: $number")
        val url    = Prefs.getUrl(context)
        val secret = Prefs.getSecret(context)
        Thread { HttpClient.postCall(url, secret, number) }.start()
    }
}
