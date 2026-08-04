package com.damonjess.doomsdayassistant

data class CapturedHero(
    val id: String,
    val heroId: String,
    val name: String,
    val rarity: HeroRarity,
    val role: HeroRole,
    val faction: Faction,
    val level: Int,
    val stars: Int,
    val power: Long,
    val stats: HeroStats,
    val skillLevels: List<Int>,
    val capturedAt: Long = System.currentTimeMillis(),
    val isMaxed: Boolean = false
) {
    fun combatScore(): Double {
        var score = 0.0
        score += stats.attack * 0.5
        score += stats.defense * 0.3
        score += stats.hp * 0.001
        score += stats.speed * 2.0
        score += stats.critRate * 1000
        score += stats.critDamage * 500
        score *= (1 + level * 0.01)
        score *= (1 + stars * 0.15)
        score *= when (rarity) {
            HeroRarity.COMMON -> 1.0
            HeroRarity.UNCOMMON -> 1.1
            HeroRarity.RARE -> 1.25
            HeroRarity.EPIC -> 1.5
            HeroRarity.LEGENDARY -> 2.0
            HeroRarity.MYTHIC -> 2.5
        }
        val avgSkill = if (skillLevels.isNotEmpty()) skillLevels.average() else 0.0
        score *= (1 + avgSkill * 0.05)
        return score
    }

    fun displayString(): String {
        val rarityEmoji = when (rarity) {
            HeroRarity.LEGENDARY -> "🟠"
            HeroRarity.EPIC -> "🟣"
            HeroRarity.RARE -> "🔵"
            else -> "⚪"
        }
        val powerStr = power.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")
        return "$rarityEmoji $name | Lv$level | ${stars}★ | $powerStr"
    }
}

data class ArenaTeam(
    val tile1: TileAssignment,
    val tile2: TileAssignment,
    val tile3: TileAssignment,
    val tile4: TileAssignment,
    val tile5: TileAssignment,
    val teamScore: Double,
    val synergyScore: Double,
    val coverageScore: Double,
    val pairScore: Double
) {
    fun allHeroes(): List<CapturedHero> {
        return listOf(tile1, tile2, tile3, tile4, tile5)
            .flatMap { it.heroes }
    }

    fun bestPair(): Pair<CapturedHero, CapturedHero>? {
        val heroes = allHeroes()
        if (heroes.size < 2) return null
        var bestScore = -1.0
        var bestPair: Pair<CapturedHero, CapturedHero>? = null
        for (i in heroes.indices) {
            for (j in i + 1 until heroes.size) {
                val score = PairingEngine.calculatePairScore(heroes[i], heroes[j])
                if (score > bestScore) {
                    bestScore = score
                    bestPair = Pair(heroes[i], heroes[j])
                }
            }
        }
        return bestPair
    }
}

data class TileAssignment(
    val tileNumber: Int,
    val heroes: List<CapturedHero>,
    val isPaired: Boolean = heroes.size == 2
) {
    init {
        require(heroes.size in 1..2) { "Tile must have 1 or 2 heroes" }
    }
    fun tilePower(): Long = heroes.sumOf { it.power }
    fun tileScore(): Double = heroes.sumOf { it.combatScore() }
}

data class ArenaOptimizationResult(
    val savedRoster: List<CapturedHero>,
    val topTeams: List<ArenaTeam>,
    val bestOverallTeam: ArenaTeam,
    val bestPair: Pair<CapturedHero, CapturedHero>,
    val pairAnalysis: String,
    val roleCoverage: Map<HeroRole, Int>,
    val factionBonus: Map<Faction, Int>
)
