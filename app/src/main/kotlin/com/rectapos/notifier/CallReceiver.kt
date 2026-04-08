package com.rectapos.notifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CallLog
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Gelen arama BroadcastReceiver.
 *
 * Android < 9  → Numara doğrudan EXTRA_INCOMING_NUMBER'dan gelir.
 * Android 9+   → EXTRA_INCOMING_NUMBER kaldırıldı. Kısa bekleme sonrası
 *                CallLog'dan son gelen arama numarasını okuruz.
 *
 * goAsync() ile BroadcastReceiver zaman aşımına girmiyor.
 */
class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        if (!Prefs.isEnabled(context) || !Prefs.isConfigured(context)) return

        // goAsync: arka plan thread'i için zamanaşımını uzatır
        val pending = goAsync()

        Thread {
            try {
                val phone = resolvePhoneNumber(context, intent)
                Log.d("RectaPOS", "Arama algılandı: $phone")

                val url    = Prefs.getUrl(context)
                val secret = Prefs.getSecret(context)
                HttpClient.postCall(url, secret, phone)

            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun resolvePhoneNumber(context: Context, intent: Intent): String {
        // Android 8.1 (API 27) ve altı: numara doğrudan intent içinde
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val num = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
            if (!num.isNullOrBlank()) return num
        }

        // Android 9+ : CallLog'dan son gelen çağrı numarasını al
        // Çağrı başladığında CallLog kaydı hemen oluşur (MISSED olarak, sonra güncellenir)
        Thread.sleep(500) // kaydın oluşması için kısa bekleme
        return readLatestIncomingFromLog(context)
    }

    private fun readLatestIncomingFromLog(context: Context): String {
        return try {
            val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE)
            val selection  = "${CallLog.Calls.TYPE} IN (${CallLog.Calls.INCOMING_TYPE}, ${CallLog.Calls.MISSED_TYPE})"
            val sortOrder  = "${CallLog.Calls.DATE} DESC LIMIT 1"

            context.contentResolver
                .query(CallLog.Calls.CONTENT_URI, projection, selection, null, sortOrder)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                            ?.takeIf { it.isNotBlank() } ?: "bilinmeyen"
                    } else "bilinmeyen"
                } ?: "bilinmeyen"

        } catch (e: Exception) {
            Log.w("RectaPOS", "CallLog okunamadı: ${e.message}")
            "bilinmeyen"
        }
    }
}
