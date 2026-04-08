package com.rectapos.notifier

import android.content.Context
import android.content.SharedPreferences

/**
 * Uygulama ayarlarını SharedPreferences'ta saklar.
 * Thread-safe okuma/yazma.
 */
object Prefs {

    private const val FILE = "rectapos"
    private const val KEY_URL     = "url"
    private const val KEY_SECRET  = "secret"
    private const val KEY_ENABLED = "enabled"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getUrl(ctx: Context): String =
        prefs(ctx).getString(KEY_URL, "") ?: ""

    fun getSecret(ctx: Context): String =
        prefs(ctx).getString(KEY_SECRET, "") ?: ""

    fun isEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ENABLED, true)

    fun save(ctx: Context, url: String, secret: String, enabled: Boolean) {
        prefs(ctx).edit()
            .putString(KEY_URL, url.trim().trimEnd('/'))
            .putString(KEY_SECRET, secret.trim())
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun isConfigured(ctx: Context): Boolean = getUrl(ctx).isNotEmpty()
}
