package com.damonjess.doomsdayassistant

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_OVERLAY = 1001
    private val REQUEST_SCREEN_CAPTURE = 1002

    private lateinit var statusText: TextView
    private lateinit var permissionButton: Button
    private lateinit var startButton: Button
    private lateinit var arenaButton: Button

    private lateinit var storage: HeroCaptureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.status_text)
        permissionButton = findViewById(R.id.permission_button)
        startButton = findViewById(R.id.start_button)
        arenaButton = findViewById(R.id.arena_button)

        storage = HeroCaptureStorage(this)

        updateUI()

        permissionButton.setOnClickListener {
            requestOverlayPermission()
        }

        startButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                statusText.text = "❌ Overlay permission required first!"
                return@setOnClickListener
            }
            startScreenCapture()
        }

        arenaButton.setOnClickListener {
            startActivity(Intent(this, ArenaOfDoomActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val hasOverlay = Settings.canDrawOverlays(this)
        statusText.text = if (hasOverlay) "✅ Ready to start" else "⏳ Grant overlay permission"
        permissionButton.text = if (hasOverlay) "Overlay Permission ✅" else "Grant Overlay Permission"
        arenaButton.text = "🏟️ Arena of Doom (${storage.getRosterSize()} heroes)"
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY)
        }
    }

    private fun startScreenCapture() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQUEST_SCREEN_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY) {
            updateUI()
        } else if (requestCode == REQUEST_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            Log.d("DoomsdayCapture", "Screen capture permission granted. ResultCode: $resultCode")
            
            // Initialize MediaProjection IMMEDIATELY. Android 14+ only allows using the Intent data once.
            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mgr.getMediaProjection(resultCode, data)
            
            ScreenCaptureState.mediaProjection?.stop()
            ScreenCaptureState.mediaProjection = projection
            ScreenCaptureState.resultCode = resultCode
            ScreenCaptureState.data = data
            
            // Start the ScreenCaptureService immediately so it stays alive
            val captureServiceIntent = Intent(this, ScreenCaptureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(captureServiceIntent)
            } else {
                startService(captureServiceIntent)
            }
            
            val serviceIntent = Intent(this, FloatingButtonService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            statusText.text = "🟢 Assistant is active!"
        }
    }
}
