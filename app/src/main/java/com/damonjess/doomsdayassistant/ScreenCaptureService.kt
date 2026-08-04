package com.damonjess.doomsdayassistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import java.nio.ByteBuffer
import java.util.UUID

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var storage: HeroCaptureStorage

    companion object {
        const val EXTRA_MODE = "capture_mode"
        const val MODE_ANALYZE = "analyze"
        const val MODE_SAVE_ARENA = "save_arena"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
    }

    override fun onCreate() {
        super.onCreate()
        storage = HeroCaptureStorage(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_ANALYZE
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data = intent?.getParcelableExtra<android.content.Intent>(EXTRA_DATA)

        if (resultCode == -1 || data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "DoomsdayCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        handler.postDelayed({
            val bitmap = captureScreen()
            if (bitmap != null) {
                when (mode) {
                    MODE_SAVE_ARENA -> saveHeroToArena(bitmap)
                    else -> analyzeScreen(bitmap)
                }
            }
            stopSelf()
        }, 800)

        return START_NOT_STICKY
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

    private fun saveHeroToArena(bitmap: Bitmap) {
        val dispatcher = AnalysisDispatcher(this)
        val result = dispatcher.analyze(bitmap)

        if (result.screenType != ScreenType.HERO_PROFILE) {
            handler.post {
                Toast.makeText(this, "❌ Not a hero profile screen!", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val profileAnalyzer = ProfileScreenAnalyzer()
        val profile = profileAnalyzer.analyze(bitmap, "")

        val hero = HeroDatabase.findHeroByName(profile.heroName)
        if (hero == null) {
            handler.post {
                Toast.makeText(this, "❌ Could not identify hero: ${profile.heroName}", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val captured = CapturedHero(
            id = UUID.randomUUID().toString(),
            heroId = hero.id,
            name = hero.name,
            rarity = hero.rarity,
            role = hero.role,
            faction = hero.faction,
            level = profile.level,
            stars = profile.stars,
            power = profile.power,
            stats = HeroStats(
                attack = profile.stats.dmg,
                defense = profile.stats.def,
                hp = profile.stats.hp,
                speed = 100,
                critRate = 0.1f,
                critDamage = 1.5f
            ),
            skillLevels = profile.skillLevels.map { it.current }
        )

        storage.saveHero(captured)
        handler.post {
            Toast.makeText(this, "✅ ${hero.name} saved to Arena roster!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
