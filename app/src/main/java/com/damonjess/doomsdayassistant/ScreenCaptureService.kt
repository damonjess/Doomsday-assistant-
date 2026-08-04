package com.damonjess.doomsdayassistant

import android.app.*
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
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var windowManager: WindowManager
    
    private var resultCode: Int = 0
    private lateinit var resultData: Intent

    companion object {
        var staticResultCode: Int = 0
        var staticResultData: Intent? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        val action = intent.action
        if (action == "ACTION_INIT") {
            resultCode = intent.getIntExtra("EXTRA_RESULT_CODE", 0)
            resultData = intent.getParcelableExtra("EXTRA_DATA") ?: return START_NOT_STICKY
            staticResultCode = resultCode
            staticResultData = resultData
            
            createNotificationChannel()
            startForeground(2, createNotification())
        } else if (action == "ACTION_ANALYZE" || action == "ACTION_SAVE_HERO") {
            captureAndProcess(action == "ACTION_SAVE_HERO")
        }

        return START_NOT_STICKY
    }

    private fun captureAndProcess(saveToArena: Boolean) {
        val metrics = DisplayMetrics()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)
        
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(staticResultCode, staticResultData!!)

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width
                
                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                val analyzer = HeroAnalyzer()
                val result = analyzer.analyze(bitmap)

                if (result != null) {
                    if (saveToArena) {
                        saveHeroToRoster(result)
                    } else {
                        showResults(result)
                    }
                }

                stopCapture()
            }
        }, 500)
    }

    private fun saveHeroToRoster(result: AnalysisResult) {
        val hero = result.hero ?: return
        val storage = HeroCaptureStorage(this)
        val capturedHero = CapturedHero(
            id = java.util.UUID.randomUUID().toString(),
            heroId = hero.id,
            name = hero.name,
            rarity = hero.rarity,
            role = hero.role,
            faction = hero.faction,
            level = result.detectedLevel ?: 1,
            stars = 0,
            power = 0,
            stats = hero.baseStats,
            skillLevels = emptyList()
        )
        storage.saveHero(capturedHero)
        
        // Broadcast or toast?
    }

    private fun showResults(result: AnalysisResult) {
        val intent = Intent(this, ResultsOverlayService::class.java).apply {
            // In a real app, we'd pass data via a singleton or parcelable
            ResultsOverlayService.currentResult = result
        }
        startService(intent)
    }

    private fun stopCapture() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "capture_service",
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "capture_service")
            .setContentTitle("Doomsday Screen Capture")
            .setContentText("Capturing hero data...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }
}
