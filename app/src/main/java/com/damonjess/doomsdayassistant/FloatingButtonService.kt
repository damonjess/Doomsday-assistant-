package com.damonjess.doomsdayassistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var mode = MODE_ANALYZE

    companion object {
        const val MODE_ANALYZE = "analyze"
        const val MODE_SAVE_ARENA = "save_arena"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
        val button = floatingView?.findViewById<ImageButton>(R.id.floating_button)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        var isDragging = false
        val longPressHandler = Handler(Looper.getMainLooper())
        var longPressRunnable: Runnable? = null

        button?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params!!.x
                    initialY = params!!.y
                    touchX = event.rawX
                    touchY = event.rawY
                    isDragging = false

                    longPressRunnable = Runnable {
                        isDragging = true
                        showModeMenu(button)
                    }
                    longPressHandler.postDelayed(longPressRunnable!!, 600)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDragging) {
                        params!!.x = initialX + (event.rawX - touchX).toInt()
                        params!!.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                    if (!isDragging) {
                        triggerCapture()
                    }
                }
            }
            true
        }

        windowManager.addView(floatingView, params)
    }

    private fun showModeMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("🔴 Analyze Screen").setOnMenuItemClickListener {
            mode = MODE_ANALYZE
            anchor.setBackgroundResource(R.drawable.floating_button_bg)
            Toast.makeText(this, "Mode: Analyze", Toast.LENGTH_SHORT).show()
            true
        }
        popup.menu.add("🟢 Save Hero to Arena").setOnMenuItemClickListener {
            mode = MODE_SAVE_ARENA
            anchor.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
            Toast.makeText(this, "Mode: Save to Arena", Toast.LENGTH_SHORT).show()
            true
        }
        popup.menu.add("🏟️ Open Arena of Doom").setOnMenuItemClickListener {
            val intent = Intent(this, ArenaOfDoomActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            true
        }
        popup.menu.add("❌ Close Assistant").setOnMenuItemClickListener {
            stopSelf()
            true
        }
        popup.show()
    }

    private fun triggerCapture() {
        val stateCode = ScreenCaptureState.resultCode
        val stateData = ScreenCaptureState.data
        if (stateCode == -1 || stateData == null) {
            Toast.makeText(this, "⚠️ Start the assistant from the app first!", Toast.LENGTH_SHORT).show()
            return
        }

        val captureIntent = Intent(this, ScreenCaptureService::class.java).apply {
            putExtra(ScreenCaptureService.EXTRA_MODE, mode)
            putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, stateCode)
            putExtra(ScreenCaptureService.EXTRA_DATA, stateData)
        }
        startService(captureIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        floatingView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}