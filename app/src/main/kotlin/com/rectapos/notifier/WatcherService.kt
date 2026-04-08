package com.rectapos.notifier

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Ön plan (foreground) servisi.
 *
 * PhoneStateListener burada kayıt edilir — BroadcastReceiver'a göre çok
 * daha güvenilir numara alımı sağlar (Android 10+ dahil tüm sürümler).
 *
 * START_STICKY: sistem zorla öldürse bile yeniden başlatılır.
 */
class WatcherService : Service() {

    companion object {
        private const val CHANNEL_ID = "rectapos_watcher"
        private const val NOTIF_ID   = 1
        private const val TAG        = "RectaPOS"

        fun start(ctx: Context) {
            val i = Intent(ctx, WatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, WatcherService::class.java))
        }
    }

    private var lastReportedNumber = ""

    @Suppress("DEPRECATION")
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    val number = phoneNumber?.takeIf { it.isNotBlank() } ?: return
                    // Aynı arama için tekrar gönderme
                    if (number == lastReportedNumber) return
                    lastReportedNumber = number
                    Log.d(TAG, "Gelen arama: $number")
                    if (!Prefs.isEnabled(this@WatcherService) || !Prefs.isConfigured(this@WatcherService)) return
                    val url    = Prefs.getUrl(this@WatcherService)
                    val secret = Prefs.getSecret(this@WatcherService)
                    Thread { HttpClient.postCall(url, secret, number) }.start()
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    lastReportedNumber = ""
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
        registerPhoneListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterPhoneListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    @Suppress("DEPRECATION")
    private fun registerPhoneListener() {
        val tm = getSystemService(TelephonyManager::class.java)
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        Log.d(TAG, "PhoneStateListener kayıt edildi")
    }

    @Suppress("DEPRECATION")
    private fun unregisterPhoneListener() {
        val tm = getSystemService(TelephonyManager::class.java)
        tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "RectaPOS Arama İzleyici",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Gelen aramaları kasaya iletir"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("RectaPOS Aktif")
            .setContentText("Gelen aramalar kasaya iletiliyor…")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(openApp)
            .setOngoing(true)
            .also {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    it.setPriority(Notification.PRIORITY_LOW)
                }
            }
            .build()
    }
}
