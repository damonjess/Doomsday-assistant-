package com.damonjess.doomsdayassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
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

    companion object {
        const val EXTRA_MODE = "capture_mode"
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
        startForeground(NOTIF_ID, buildNotification())

        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_ANALYZE

        // Lazy-init MediaProjection once per session
        if (ScreenCaptureState.mediaProjection == null) {
            val rc = ScreenCaptureState.resultCode
            val d = ScreenCaptureState.data
            if (rc == -1 || d == null) {
                toast("⚠️ Start the assistant from the app first!")
                stopMe()
                return START_NOT_STICKY
            }
            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            ScreenCaptureState.mediaProjection = mgr.getMediaProjection(rc, d)
            toast("🎥 Screen capture ready")
        }

        val worker = HandlerThread("DoomsdayCapture", Process.THREAD_PRIORITY_BACKGROUND)
        worker.start()
        val bg = Handler(worker.looper)

        bg.post {
            try {
                doCapture(mode)
            } catch (e: Exception) {
                toast("❌ Error: ${e.message}")
                e.printStackTrace()
            } finally {
                // Only release VirtualDisplay + ImageReader, NOT MediaProjection
                try { virtualDisplay?.release() } catch (_: Exception) {}
                try { imageReader?.close() } catch (_: Exception) {}
                worker.quitSafely()
                stopForeground(true)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun doCapture(mode: String) {
        val projection = ScreenCaptureState.mediaProjection
        if (projection == null) {
            toast("❌ MediaProjection lost. Restart the assistant.")
            return
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)

        val w = metrics.widthPixels
        val h = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection.createVirtualDisplay(
            "DoomsdayCapture",
            w, h, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        Thread.sleep(600)

        val bitmap = captureScreen()
        if (bitmap == null) {
            toast("❌ Failed to capture screen")
            return
        }

        when (mode) {
            MODE_SAVE_ARENA -> saveHero(bitmap)
            else -> analyzeScreen(bitmap)
        }
    }

    private fun captureScreen(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
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
        image.close()

        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun analyzeScreen(bitmap: Bitmap) {
        val dispatcher = AnalysisDispatcher(this)
        val result = dispatcher.analyze(bitmap)

        val overlayIntent = Intent(this, ResultsOverlayService::class.java).apply {
            putExtra("screen_type", result.screenType.name)
            putExtra("title", result.title)
            putExtra("priority_score", result.priorityScore)
            putStringArrayListExtra("sections_headers", ArrayList(result.sections.map { it.header }))
            val items = result.sections.map { it.items.joinToString("\n") }
            putStringArrayListExtra("sections_items", ArrayList(items))
        }
        startService(overlayIntent)
    }

    private fun saveHero(bitmap: Bitmap) {
        toast("🔍 Reading stats…")

        val extractor = StatsExtractor(bitmap)
        val stats = extractor.extract()

        toast("📊 Lv${stats.level} | ${stats.power} power")

        val intent = Intent(this, HeroPickerOverlay::class.java).apply {
            putExtra("level", stats.level)
            putExtra("power", stats.power)
            putExtra("stars", stats.stars)
            putExtra("skills", stats.skillLevels.toIntArray())
            putExtra("dmg", stats.dmg)
            putExtra("hp", stats.hp)
            putExtra("def", stats.def)
            putExtra("squad", stats.squad)
        }
        startService(intent)
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
        stopForeground(true)
        stopSelf()
    }

    private fun toast(message: String) {
        mainHandler.post {
            Toast.makeText(this@ScreenCaptureService, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
