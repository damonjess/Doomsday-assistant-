package com.damonjess.doomsdayassistant

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
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

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        storage = HeroCaptureStorage(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            if (selectedHero == null) {
                Toast.makeText(this, "Tap a hero name first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveHero(selectedHero!!, gs)
            closeOverlay()
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
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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

    private fun closeOverlay() {
        overlayView?.let { wm.removeView(it) }
        overlayView = null
        stopSelf()
    }

    override fun onDestroy() {
        closeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
