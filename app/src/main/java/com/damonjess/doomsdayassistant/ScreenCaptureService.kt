package com.damonjess.doomsdayassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.util.Log
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import java.util.UUID

class ScreenCaptureService : Service() {

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var storage: HeroCaptureStorage
    private var isInitialized = false

    companion object {
        const val EXTRA_MODE = "capture_mode"
        const val ACTION_CAPTURE = "com.damonjess.doomsdayassistant.CAPTURE"
        const val ACTION_STOP = "com.damonjess.doomsdayassistant.STOP_CAPTURE"
        const val MODE_ANALYZE = "analyze"
        const val MODE_SAVE_ARENA = "save_arena"
        private const val NOTIF_ID = 1337
        private const val CHANNEL_ID = "doomsday_capture"
    }

    override fun onCreate() {
        super.onCreate()
        storage = HeroCaptureStorage(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMe()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }

        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_ANALYZE
        Log.d("DoomsdayCapture", "Service command received. Action: ${intent?.action}, Mode: $mode")

        // Lazy-init MediaProjection once per session
        if (ScreenCaptureState.mediaProjection == null) {
            val rc = ScreenCaptureState.resultCode
            val d = ScreenCaptureState.data
            if (rc == 0 || d == null) {
                toast("⚠️ Start the assistant from the app first!")
                stopMe()
                return START_NOT_STICKY
            }
            try {
                val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                ScreenCaptureState.mediaProjection = mgr.getMediaProjection(rc, d)
                toast("🎥 Screen capture ready")
            } catch (e: Exception) {
                Log.e("DoomsdayCapture", "Failed to get MediaProjection: ${e.message}", e)
                // Token already used, stale, or SecurityException (missing FGS type)
                ScreenCaptureState.mediaProjection = null
                ScreenCaptureState.resultCode = 0
                ScreenCaptureState.data = null
                toast("⚠️ Session expired — restart the assistant from the app")
                stopMe()
                return START_NOT_STICKY
            }
        }

        if (!isInitialized) {
            initProjection()
        }

        if (intent?.action == ACTION_CAPTURE) {
            val worker = HandlerThread("DoomsdayCapture", Process.THREAD_PRIORITY_BACKGROUND)
            worker.start()
            val bg = Handler(worker.looper)
            bg.post {
                try {
                    val bitmap = captureScreen()
                    if (bitmap != null) {
                        Log.d("DoomsdayCapture", "Bitmap captured: ${bitmap.width}x${bitmap.height}")
                        
                        when (mode) {
                            MODE_SAVE_ARENA -> {
                                toast("🔍 Reading stats…")
                                val extractor = StatsExtractor(bitmap)
                                val stats = extractor.extract()
                                
                                mainHandler.post {
                                    toast("📊 Lv${stats.level} | ${stats.power} power")
                                    val overlayIntent = Intent(this@ScreenCaptureService, HeroPickerOverlay::class.java).apply {
                                        putExtra("level", stats.level)
                                        putExtra("power", stats.power)
                                        putExtra("stars", stats.stars)
                                        putExtra("skills", stats.skillLevels.toIntArray())
                                        putExtra("dmg", stats.dmg)
                                        putExtra("hp", stats.hp)
                                        putExtra("def", stats.def)
                                        putExtra("squad", stats.squad)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(overlayIntent)
                                    } else {
                                        startService(overlayIntent)
                                    }
                                }
                            }
                            else -> {
                                val dispatcher = AnalysisDispatcher(this@ScreenCaptureService)
                                val result = dispatcher.analyze(bitmap)
                                mainHandler.post {
                                    val overlayIntent = Intent(this@ScreenCaptureService, ResultsOverlayService::class.java).apply {
                                        putExtra("screen_type", result.screenType.name)
                                        putExtra("title", result.title)
                                        putExtra("priority_score", result.priorityScore)
                                        putStringArrayListExtra("sections_headers", ArrayList(result.sections.map { it.header }))
                                        val itemsList = result.sections.map { it.items.joinToString("\n") }
                                        putStringArrayListExtra("sections_items", ArrayList(itemsList))
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(overlayIntent)
                                    } else {
                                        startService(overlayIntent)
                                    }
                                }
                            }
                        }
                    } else {
                        Log.e("DoomsdayCapture", "Bitmap capture failed")
                        toast("❌ Failed to capture screen")
                    }
                } catch (e: Exception) {
                    Log.e("DoomsdayCapture", "Capture error", e)
                    toast("❌ Error: ${e.message}")
                } finally {
                    worker.quitSafely()
                }
            }
        }

        return START_STICKY
    }

    private fun initProjection() {
        val projection = ScreenCaptureState.mediaProjection
        if (projection == null) {
            Log.e("DoomsdayCapture", "Cannot init: MediaProjection is null")
            return
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        val density = metrics.densityDpi

        // One-time registration of the mandatory callback for Android 14+
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d("DoomsdayCapture", "MediaProjection session stopped")
                
                // The OS killed this session — fully clear state so the
                // next attempt shows "start the assistant" instead of
                // trying to reuse a dead token.
                ScreenCaptureState.mediaProjection = null
                ScreenCaptureState.resultCode = 0
                ScreenCaptureState.data = null

                isInitialized = false
                virtualDisplay?.release()
                virtualDisplay = null
                imageReader?.close()
                imageReader = null
            }
        }, mainHandler)

        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection.createVirtualDisplay(
            "DoomsdayCapture",
            w, h, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
        
        isInitialized = true
        Log.d("DoomsdayCapture", "Projection and VirtualDisplay initialized")
    }

    private fun captureScreen(): Bitmap? {
        // We use acquireLatestImage to get the most recent frame. 
        // We MUST close it immediately after copying to avoid "image is already closed" or memory leaks.
        val image = imageReader?.acquireLatestImage() ?: return null
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } finally {
            image.close()
        }
    }


    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Screen Capture", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Doomsday Assistant")
            .setContentText("Active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun stopMe() {
        isInitialized = false
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        ScreenCaptureState.mediaProjection?.stop()
        ScreenCaptureState.mediaProjection = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun toast(message: String) {
        mainHandler.post {
            Toast.makeText(this@ScreenCaptureService, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
