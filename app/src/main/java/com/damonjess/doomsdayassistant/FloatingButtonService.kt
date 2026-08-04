package com.damonjess.doomsdayassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
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
        const val ACTION_STOP = "com.damonjess.doomsdayassistant.STOP_FLOATING_BUTTON"
        private const val NOTIF_ID = 42
        private const val CHANNEL_ID = "doomsday_overlay"
    }

    override fun onCreate() {
        super.onCreate()

        // MUST happen before anything else, and within ~5s of the service starting
        startForegroundCompat()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_button, null)
        val button = floatingView?.findViewById<ImageButton>(R.id.floating_button)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
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
        var longPressTriggered = false
        var lastTapTime = 0L
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
                    longPressTriggered = false

                    longPressRunnable = Runnable {
                        longPressTriggered = true
                        mode = MODE_SAVE_ARENA
                        Toast.makeText(this, "💾 Saving hero…", Toast.LENGTH_SHORT).show()
                        triggerCapture()
                    }
                    longPressHandler.postDelayed(longPressRunnable!!, 600)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY
                    if (!longPressTriggered && !isDragging &&
                        (kotlin.math.abs(dx) > 50 || kotlin.math.abs(dy) > 50)) {
                        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                        isDragging = true
                    }
                    if (isDragging) {
                        params!!.x = initialX + dx.toInt()
                        params!!.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                    if (!isDragging && !longPressTriggered) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300) {
                            showModeMenu(button)
                        } else {
                            mode = MODE_ANALYZE
                            triggerCapture()
                        }
                        lastTapTime = now
                    }
                    isDragging = false
                }
            }
            true
        }

        windowManager.addView(floatingView, params)
    }

    private fun startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Doomsday Overlay", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, FloatingButtonService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle("Doomsday Assistant")
            .setContentText("Overlay active — tap Stop to close")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Special use wasn't a type before 34, so we just use 0 or none if it fits.
            // Actually, for API 29-33, we can't use specialUse.
            // If we don't need a specific type, we just pass the notification.
            startForeground(NOTIF_ID, notification)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun showModeMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("🏟️ Open Arena of Doom").setOnMenuItemClickListener {
            startActivity(Intent(this, ArenaOfDoomActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
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
        Log.d("DoomsdayCapture", "Triggering capture. StateCode: $stateCode, Data present: ${stateData != null}")
        
        // resultCode -1 is Activity.RESULT_OK. 
        if (stateCode != -1 || stateData == null) {
            Toast.makeText(this, "⚠️ Start the assistant from the app first!", Toast.LENGTH_SHORT).show()
            return
        }

        val captureIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_CAPTURE
            putExtra(ScreenCaptureService.EXTRA_MODE, mode)
        }
        startService(captureIntent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            val stopCaptureIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_STOP
            }
            startService(stopCaptureIntent)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        floatingView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
