package com.damonjess.doomsdayassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ArenaOfDoomActivity : AppCompatActivity() {

    private lateinit var rosterRecycler: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var rosterCount: TextView
    private lateinit var optimizeButton: Button
    private lateinit var clearButton: Button
    private lateinit var resultContainer: LinearLayout

    private lateinit var storage: HeroCaptureStorage
    private lateinit var adapter: CapturedHeroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arena_of_doom)

        rosterRecycler = findViewById(R.id.roster_recycler)
        emptyState = findViewById(R.id.empty_state)
        rosterCount = findViewById(R.id.roster_count)
        optimizeButton = findViewById(R.id.optimize_button)
        clearButton = findViewById(R.id.clear_button)
        resultContainer = findViewById(R.id.result_container)

        storage = HeroCaptureStorage(this)
        adapter = CapturedHeroAdapter { hero -> removeHero(hero) }

        rosterRecycler.layoutManager = LinearLayoutManager(this)
        rosterRecycler.adapter = adapter

        clearButton.setOnClickListener {
            storage.clearRoster()
            refreshRoster()
        }

        optimizeButton.setOnClickListener {
            runOptimization()
        }

        refreshRoster()
    }

    override fun onResume() {
        super.onResume()
        refreshRoster()
    }

    private fun refreshRoster() {
        val heroes = storage.getAllHeroes()
        rosterCount.text = "Roster: ${heroes.size}/50"
        adapter.submitList(heroes)

        if (heroes.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            rosterRecycler.visibility = View.GONE
            resultContainer.visibility = View.GONE
        } else {
            emptyState.visibility = View.GONE
            rosterRecycler.visibility = View.VISIBLE
        }
    }

    private fun removeHero(hero: CapturedHero) {
        storage.removeHero(hero.id)
        refreshRoster()
    }

    private fun runOptimization() {
        val roster = storage.getAllHeroes()
        if (roster.isEmpty()) {
            return
        }

        val optimizer = ArenaOfDoomOptimizer()
        val result = optimizer.optimize(roster)

        resultContainer.visibility = View.VISIBLE
        resultContainer.removeAllViews()

        val bestTeam = result.bestOverallTeam
        val pair = result.bestPair

        addResultHeader("🏆 BEST 5-TILE TEAM")
        addResultText("Score: ${bestTeam.teamScore.toInt()}/100 | Synergy: ${bestTeam.synergyScore.toInt()} | Coverage: ${bestTeam.coverageScore.toInt()}")

        val tiles = listOf(bestTeam.tile1, bestTeam.tile2, bestTeam.tile3, bestTeam.tile4, bestTeam.tile5)
        tiles.forEachIndexed { index, tile ->
            val heroes = tile.heroes.joinToString(" + ") { it.name }
            val pairIndicator = if (tile.isPaired) "🔗 PAIR" else "Solo"
            addResultText("Tile ${index + 1}: $heroes ($pairIndicator)")
        }

        addResultHeader("🔗 BEST PAIR")
        addResultText("${pair.first.name} + ${pair.second.name}")
        addResultText(result.pairAnalysis)

        addResultHeader("📊 ROLE COVERAGE")
        result.roleCoverage.forEach { (role, count) ->
            addResultText("• $role: $count")
        }

        addResultHeader("🎭 FACTION BONUS")
        result.factionBonus.forEach { (faction, count) ->
            addResultText("• $faction: $count")
        }

        if (result.topTeams.size > 1) {
            addResultHeader("🥈 ALTERNATIVE TEAMS")
            result.topTeams.drop(1).take(3).forEachIndexed { index, team ->
                addResultText("Alt ${index + 1}: Score ${team.teamScore.toInt()} | Best pair: ${team.bestPair()?.first?.name} + ${team.bestPair()?.second?.name}")
            }
        }
    }

    private fun addResultHeader(text: String) {
        val tv = TextView(this).apply {
            this.text = "\n$text"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#e94560"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        resultContainer.addView(tv)
    }

    private fun addResultText(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#e0e0e0"))
            setPadding(0, 4, 0, 4)
        }
        resultContainer.addView(tv)
    }
}

class CapturedHeroAdapter(private val onRemove: (CapturedHero) -> Unit) :
    RecyclerView.Adapter<CapturedHeroAdapter.ViewHolder>() {

    private var heroes: List<CapturedHero> = emptyList()

    fun submitList(list: List<CapturedHero>) {
        heroes = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_captured_hero, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val hero = heroes[position]
        holder.name.text = hero.name
        holder.info.text = "${hero.role} | ${hero.faction} | Lv${hero.level} | ${hero.stars}★"
        val powerStr = hero.power.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")
        holder.power.text = "⚡ $powerStr"
        holder.remove.setOnClickListener { onRemove(hero) }
    }

    override fun getItemCount() = heroes.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.hero_name)
        val info: TextView = view.findViewById(R.id.hero_info)
        val power: TextView = view.findViewById(R.id.hero_power)
        val remove: android.widget.ImageButton = view.findViewById(R.id.remove_button)
    }
}