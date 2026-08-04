package com.damonjess.doomsdayassistant

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArenaOfDoomActivity : AppCompatActivity() {

    private lateinit var storage: HeroCaptureStorage
    private lateinit var adapter: CapturedHeroAdapter
    private lateinit var rosterRecycler: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var rosterCount: TextView
    private lateinit var resultContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arena_of_doom)

        storage = HeroCaptureStorage(this)
        
        rosterRecycler = findViewById(R.id.roster_recycler)
        emptyState = findViewById(R.id.empty_state)
        rosterCount = findViewById(R.id.roster_count)
        resultContainer = findViewById(R.id.result_container)

        adapter = CapturedHeroAdapter(storage.getAllHeroes()) { hero ->
            storage.removeHero(hero.id)
            refreshData()
        }

        rosterRecycler.layoutManager = LinearLayoutManager(this)
        rosterRecycler.adapter = adapter

        findViewById<Button>(R.id.clear_button).setOnClickListener {
            storage.clearRoster()
            refreshData()
        }

        findViewById<Button>(R.id.optimize_button).setOnClickListener {
            performOptimization()
        }

        refreshData()
    }

    private fun refreshData() {
        val heroes = storage.getAllHeroes()
        adapter.updateData(heroes)
        
        rosterCount.text = "Roster: ${heroes.size}/50"
        
        if (heroes.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rosterRecycler.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rosterRecycler.visibility = View.VISIBLE
        }
    }

    private fun performOptimization() {
        val heroes = storage.getAllHeroes()
        if (heroes.size < 5) {
            // Toast: Need at least 5 heroes
            return
        }

        val optimizer = ArenaOfDoomOptimizer()
        val result = optimizer.optimize(heroes)

        resultContainer.removeAllViews()
        resultContainer.visibility = View.VISIBLE

        val titleView = TextView(this).apply {
            text = "Best Overall Team (Score: ${result.bestOverallTeam.teamScore.toInt()})"
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.white))
            setPadding(0, 16, 0, 8)
        }
        resultContainer.addView(titleView)

        result.bestOverallTeam.allHeroes().forEach { hero ->
            val heroView = TextView(this).apply {
                text = "• ${hero.name} (${hero.role})"
                setTextColor(resources.getColor(android.R.color.darker_gray))
                setPadding(16, 4, 0, 4)
            }
            resultContainer.addView(heroView)
        }

        val analysisView = TextView(this).apply {
            text = "\nPair Analysis:\n${result.pairAnalysis}"
            setTextColor(resources.getColor(android.R.color.holo_orange_light))
            setPadding(0, 8, 0, 8)
        }
        resultContainer.addView(analysisView)
    }
}
