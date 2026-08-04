package com.damonjess.doomsdayassistant

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var storage: HeroCaptureStorage

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updateUI()
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            initCaptureService(result.resultCode, result.data!!)
            startFloatingButtonService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storage = HeroCaptureStorage(this)
        statusText = findViewById(R.id.status_text)

        findViewById<Button>(R.id.permission_button).setOnClickListener {
            requestOverlayPermission()
        }

        findViewById<Button>(R.id.start_button).setOnClickListener {
            if (hasOverlayPermission()) {
                requestMediaProjection()
            } else {
                requestOverlayPermission()
            }
        }

        findViewById<Button>(R.id.arena_button).setOnClickListener {
            startActivity(Intent(this, ArenaOfDoomActivity::class.java))
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val hasPermission = hasOverlayPermission()
        findViewById<Button>(R.id.permission_button).isEnabled = !hasPermission
        findViewById<Button>(R.id.start_button).isEnabled = hasPermission
        
        val rosterSize = storage.getRosterSize()
        findViewById<Button>(R.id.arena_button).text = "🏟️ Arena of Doom ($rosterSize heroes)"
        
        statusText.text = if (hasPermission) "✅ Permissions granted" else "⏳ Permission required"
    }

    private fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun requestMediaProjection() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun initCaptureService(resultCode: Int, data: Intent) {
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "ACTION_INIT"
            putExtra("EXTRA_RESULT_CODE", resultCode)
            putExtra("EXTRA_DATA", data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun startFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
