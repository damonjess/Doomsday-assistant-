package com.damonjess.doomsdayassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.os.IBinder
import android.view.*
import android.widget.*
import java.util.UUID

class HeroPickerOverlay : Service() {

    private lateinit var wm: WindowManager
    private var overlayView: View? = null
    private lateinit var storage: HeroCaptureStorage
    private var selectedHero: Hero? = null
    private var gameStats: StatsExtractor.GameStats? = null

    companion object {
        private const val NOTIF_ID = 101
        private const val CHANNEL_ID = "doomsday_picker"
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        storage = HeroCaptureStorage(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
        
        val level = intent?.getIntExtra("level", 0) ?: 0
        val power = intent?.getLongExtra("power", 0) ?: 0
        val stars = intent?.getIntExtra("stars", 0) ?: 0
        val skills = intent?.getIntArrayExtra("skills")?.toList() ?: emptyList()
        val dmg = intent?.getIntExtra("dmg", 0) ?: 0
        val hp = intent?.getIntExtra("hp", 0) ?: 0
        val def = intent?.getIntExtra("def", 0) ?: 0
        val squad = intent?.getIntExtra("squad", 0) ?: 0

        gameStats = StatsExtractor.GameStats(level, power, stars, skills, dmg, hp, def, squad)
        showOverlay()
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        overlayView?.let { wm.removeView(it); overlayView = null }

        val view = LayoutInflater.from(this).inflate(R.layout.hero_picker_overlay, null)
        overlayView = view

        val gs = gameStats!!
        view.findViewById<TextView>(R.id.picker_stats_text).text = """
            Level: ${gs.level} | Stars: ${gs.stars}⭐
            Power: ${gs.power.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}
            DMG: ${gs.dmg} | HP: ${gs.hp} | DEF: ${gs.def}
            Skills: ${gs.skillLevels.joinToString(", ")}
        """.trimIndent()

        val grid = view.findViewById<GridLayout>(R.id.hero_grid)
        val heroes = HeroDatabase.getAllHeroes().sortedBy { it.name }

        heroes.forEach { hero ->
            val btn = Button(this).apply {
                text = "${hero.name}\n${hero.role} | ${hero.faction}"
                textSize = 11f
                setBackgroundColor(0xFF1A1A2E.toInt())
                setTextColor(0xFFE0E0E0.toInt())
                setPadding(12, 12, 12, 12)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(6, 6, 6, 6)
                }
                setOnClickListener {
                    Log.d("DoomsdayCapture", "Hero selected: ${hero.name}")
                    Toast.makeText(this@HeroPickerOverlay, "Selected: ${hero.name}", Toast.LENGTH_SHORT).show()
                    selectedHero = hero
                    for (i in 0 until grid.childCount) {
                        (grid.getChildAt(i) as Button).apply {
                            setBackgroundColor(0xFF1A1A2E.toInt())
                            setTextColor(0xFFE0E0E0.toInt())
                        }
                    }
                    setBackgroundColor(0xFFE94560.toInt())
                    setTextColor(0xFFFFFFFF.toInt())
                }
            }
            grid.addView(btn)
        }

        view.findViewById<Button>(R.id.picker_save_btn).setOnClickListener {
            Log.d("DoomsdayCapture", "Save button clicked. Selected hero: ${selectedHero?.name}")
            if (selectedHero == null) {
                Toast.makeText(this, "⚠️ Select a hero first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                saveHero(selectedHero!!, gs)
                Toast.makeText(this, "✅ ${selectedHero?.name} saved!", Toast.LENGTH_SHORT).show()
                closeOverlay()
            } catch (e: Exception) {
                Log.e("DoomsdayCapture", "Error saving hero", e)
                Toast.makeText(this, "❌ Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        view.findViewById<Button>(R.id.picker_cancel_btn).setOnClickListener {
            closeOverlay()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        wm.addView(view, params)
    }

    private fun saveHero(hero: Hero, stats: StatsExtractor.GameStats) {
        val captured = CapturedHero(
            id = UUID.randomUUID().toString(),
            heroId = hero.id,
            name = hero.name,
            rarity = hero.rarity,
            role = hero.role,
            faction = hero.faction,
            level = stats.level,
            stars = stats.stars,
            power = stats.power,
            stats = HeroStats(
                attack = stats.dmg,
                defense = stats.def,
                hp = stats.hp,
                speed = 100,
                critRate = 0.1f,
                critDamage = 1.5f
            ),
            skillLevels = stats.skillLevels
        )
        storage.saveHero(captured)
        Toast.makeText(this, "✅ ${hero.name} saved!\nRoster: ${storage.getRosterSize()} heroes", Toast.LENGTH_LONG).show()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Hero Picker", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Doomsday Hero Picker")
            .setContentText("Select a hero to save")
            .setSmallIcon(android.R.drawable.ic_menu_add)
            .build()
    }

    private fun closeOverlay() {
        overlayView?.let { 
            try { wm.removeView(it) } catch (_: Exception) {}
        }
        overlayView = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        closeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
