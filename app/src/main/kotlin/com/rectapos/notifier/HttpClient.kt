package com.rectapos.notifier

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

object HttpClient {

    private const val TAG = "RectaPOS"

    /**
     * Sunucuya POST atar.
     * @return true = başarılı (2xx), false = hata
     */
    fun postCall(baseUrl: String, secret: String, phone: String): Boolean {
        return try {
            val url = if (secret.isNotEmpty()) "$baseUrl?secret=$secret" else baseUrl
            Log.d(TAG, "POST → $url  phone=$phone")

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "RectaPOS-Notifier/1.0")
                doOutput = true
                connectTimeout = 8_000
                readTimeout    = 8_000
            }

            val body = """{"phone":"$phone"}""".toByteArray(Charsets.UTF_8)
            conn.outputStream.use { it.write(body) }

            val code = conn.responseCode
            conn.disconnect()

            Log.d(TAG, "Yanıt: HTTP $code")
            code in 200..299

        } catch (e: Exception) {
            Log.e(TAG, "HTTP hatası: ${e.message}")
            false
        }
    }
}
