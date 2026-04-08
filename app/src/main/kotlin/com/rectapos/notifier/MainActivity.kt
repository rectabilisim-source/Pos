package com.rectapos.notifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.rectapos.notifier.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    // ── İzin listesi ──────────────────────────────────────────────────────
    private val permissions = buildList {
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CALL_LOG)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        updatePermissionUi()
        val all = results.values.all { it }
        toast(if (all) "✓ Tüm izinler verildi" else "Bazı izinler eksik — uygulama tam çalışmayabilir")
    }

    // ── Yaşam döngüsü ─────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Kayıtlı ayarları yükle
        b.etUrl.setText(Prefs.getUrl(this))
        b.etSecret.setText(Prefs.getSecret(this))
        b.switchEnabled.isChecked = Prefs.isEnabled(this)

        b.btnSave.setOnClickListener { onSave() }
        b.btnTest.setOnClickListener { onTest() }
        b.btnPermissions.setOnClickListener { permLauncher.launch(permissions) }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUi()
        updateServiceStatus()
    }

    // ── Kaydet & başlat ───────────────────────────────────────────────────
    private fun onSave() {
        val url    = b.etUrl.text.toString().trim()
        val secret = b.etSecret.text.toString().trim()
        val enabled = b.switchEnabled.isChecked

        if (url.isEmpty())              { b.etUrl.error = "URL boş bırakılamaz"; return }
        if (!url.startsWith("http"))    { b.etUrl.error = "http:// veya https:// ile başlamalı"; return }
        b.etUrl.error = null

        Prefs.save(this, url, secret, enabled)

        if (enabled) WatcherService.start(this) else WatcherService.stop(this)

        toast("✓ Kaydedildi")
        updateServiceStatus()
    }

    // ── Test isteği ───────────────────────────────────────────────────────
    private fun onTest() {
        val url    = b.etUrl.text.toString().trim()
        val secret = b.etSecret.text.toString().trim()
        if (url.isEmpty()) { b.etUrl.error = "URL zorunlu"; return }

        b.btnTest.isEnabled = false
        b.btnTest.text = "Gönderiliyor…"

        Thread {
            val ok = HttpClient.postCall(url, secret, "05001234567")
            runOnUiThread {
                b.btnTest.isEnabled = true
                b.btnTest.text = "Test Gönder"
                toast(if (ok) "✓ Test başarılı! Bilgisayarınızda popup çıktı mı?" else "✗ Bağlantı hatası — URL ve secret'ı kontrol edin")
            }
        }.start()
    }

    // ── UI yardımcıları ───────────────────────────────────────────────────
    private fun updatePermissionUi() {
        val allOk = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allOk) {
            b.tvPermStatus.text = "✓ Tüm izinler verildi"
            b.tvPermStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            b.btnPermissions.visibility = View.GONE
        } else {
            b.tvPermStatus.text = "⚠ İzin gerekiyor"
            b.tvPermStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            b.btnPermissions.visibility = View.VISIBLE
        }
    }

    private fun updateServiceStatus() {
        val running = Prefs.isEnabled(this) && Prefs.isConfigured(this)
        b.tvServiceStatus.text = if (running) "● Servis çalışıyor" else "○ Servis kapalı"
        b.tvServiceStatus.setTextColor(
            ContextCompat.getColor(this,
                if (running) android.R.color.holo_green_dark else android.R.color.darker_gray)
        )
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
