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

/**
 * Ön plan (foreground) servisi.
 *
 * Xiaomi / Huawei / Samsung gibi telefonlar agresif pil optimizasyonu
 * yaparak arka plan uygulamalarını öldürür. Foreground service bu
 * kısıtlamadan muaf tutulur ve sistem bildirimi gösterir.
 *
 * START_STICKY: sistem zorla öldürse bile yeniden başlatılır.
 */
class WatcherService : Service() {

    companion object {
        private const val CHANNEL_ID = "rectapos_watcher"
        private const val NOTIF_ID   = 1

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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

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
