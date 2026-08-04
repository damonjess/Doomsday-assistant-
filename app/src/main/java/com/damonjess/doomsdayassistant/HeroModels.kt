package com.damonjess.doomsdayassistant

enum class HeroRarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHIC }
enum class HeroRole { TANK, DAMAGE, SUPPORT, CONTROL, ASSASSIN }
enum class Faction { WANDERERS, BROTHERHOOD, OUTLAWS, SURVIVORS, SCIENTISTS }

data class HeroSkill(
    val name: String,
    val description: String,
    val type: SkillType,
    val priority: Int
) {
    enum class SkillType { ACTIVE, PASSIVE, ULTIMATE, AWAKENED }
}

data class Hero(
    val id: String,
    val name: String,
    val rarity: HeroRarity,
    val role: HeroRole,
    val faction: Faction,
    val skills: List<HeroSkill>,
    val baseStats: HeroStats,
    val synergies: List<String>,
    val counters: List<String>,
    val weakAgainst: List<String>
)

data class HeroStats(
    val attack: Int,
    val defense: Int,
    val hp: Int,
    val speed: Int,
    val critRate: Float,
    val critDamage: Float
)

data class AnalysisResult(
    val heroName: String,
    val hero: Hero?,
    val detectedLevel: Int?,
    val detectedSkills: List<String>,
    val recommendations: List<String>,
    val arenaPairs: List<String>,
    val priorityScore: Double
)
