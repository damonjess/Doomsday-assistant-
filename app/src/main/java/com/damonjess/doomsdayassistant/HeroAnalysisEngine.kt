package com.damonjess.doomsdayassistant

import android.content.Context
import android.widget.Toast

class HeroAnalysisEngine(private val context: Context) {

    fun analyzeScreen(rawText: String): AnalysisResult {
        val detectedHero = identifyHero(rawText)
        val detectedLevel = extractLevel(rawText)
        val detectedSkills = extractSkillNames(rawText, detectedHero)

        return if (detectedHero != null) {
            val recommendations = generateUpgradeRecommendations(detectedHero, detectedLevel)
            val arenaPairs = generateArenaPairs(detectedHero)
            val priorityScore = calculatePriorityScore(detectedHero, detectedLevel)

            AnalysisResult(
                heroName = detectedHero.name,
                hero = detectedHero,
                detectedLevel = detectedLevel,
                detectedSkills = detectedSkills,
                recommendations = recommendations,
                arenaPairs = arenaPairs,
                priorityScore = priorityScore
            )
        } else {
            AnalysisResult(
                heroName = "Unknown", hero = null, detectedLevel = detectedLevel,
                detectedSkills = detectedSkills,
                recommendations = listOf(
                    "⚠️ Could not identify hero. Make sure you're on the hero details screen.",
                    "💡 Tip: Center the hero name in your screen before tapping the assistant."
                ),
                arenaPairs = emptyList(), priorityScore = 0.0
            )
        }
    }

    private fun identifyHero(text: String): Hero? {
        val allHeroes = HeroDatabase.getAllHeroes()
        for (hero in allHeroes) {
            if (text.contains(hero.name, ignoreCase = true) || text.contains(hero.id, ignoreCase = true)) {
                return hero
            }
        }
        for (hero in allHeroes) {
            val skillMatches = hero.skills.count { skill -> text.contains(skill.name, ignoreCase = true) }
            if (skillMatches >= 2) return hero
        }
        return null
    }

    private fun extractLevel(text: String): Int? {
        val levelRegex = Regex("""[Ll]evel[:\s]*(\d+)""")
        return levelRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractSkillNames(text: String, hero: Hero?): List<String> {
        val found = mutableListOf<String>()
        hero?.skills?.forEach { skill ->
            if (text.contains(skill.name, ignoreCase = true)) found.add(skill.name)
        }
        return found
    }

    private fun generateUpgradeRecommendations(hero: Hero, level: Int?): List<String> {
        val recs = mutableListOf<String>()
        recs.add("🎯 ${hero.name} [${hero.rarity}] - ${hero.role} | ${hero.faction}")
        recs.add("")

        recs.add("📋 SKILL PRIORITY:")
        val sortedSkills = hero.skills.sortedBy { it.priority }
        sortedSkills.forEachIndexed { index, skill ->
            val emoji = when (index) { 0 -> "1️⃣"; 1 -> "2️⃣"; 2 -> "3️⃣"; else -> "4️⃣" }
            recs.add("$emoji ${skill.name} (${skill.type}) - ${skill.description}")
        }
        recs.add("")

        recs.add("⚡ UPGRADE PATH:")
        when (hero.role) {
            HeroRole.TANK -> {
                recs.add("• Max HP & Defense gear first")
                recs.add("• Skill 1 (taunt/shield) → Skill 3 (ultimate)")
                recs.add("• Focus on damage reduction substats")
            }
            HeroRole.DAMAGE, HeroRole.ASSASSIN -> {
                recs.add("• Max Attack & Crit gear first")
                recs.add("• Skill 1 (main damage) → Skill 3 (ultimate)")
                recs.add("• Focus on crit rate > crit damage > attack speed")
            }
            HeroRole.SUPPORT -> {
                recs.add("• Max Speed & HP gear first")
                recs.add("• Skill 1 (heal/buff) → Skill 3 (ultimate cleanse)")
                recs.add("• Focus on speed to cycle skills faster")
            }
            HeroRole.CONTROL -> {
                recs.add("• Max Speed & Accuracy gear first")
                recs.add("• Skill 1 (CC) → Skill 3 (mass CC)")
                recs.add("• Focus on effect hit rate for reliable CC")
            }
        }

        if (level != null) {
            when {
                level < 30 -> recs.add("🔰 EARLY GAME: Level up to 30 first")
                level < 60 -> recs.add("⚔️ MID GAME: Start farming epic gear sets")
                level < 100 -> recs.add("🛡️ LATE GAME: Min-max substats, aim for legendary gear")
                else -> recs.add("👑 END GAME: Perfect your gear rolls")
            }
        }

        recs.add("")
        recs.add("🎭 SYNERGIES:")
        hero.synergies.forEach { synergyId ->
            HeroDatabase.findHeroByName(synergyId)?.let {
                recs.add("• ${it.name} (${it.role}) - ${it.faction}")
            }
        }

        return recs
    }

    private fun generateArenaPairs(hero: Hero): List<String> {
        val pairs = mutableListOf<String>()
        val allCompositions = HeroDatabase.getArenaCompositions()
        pairs.add("🏟️ ARENA COMPOSITIONS featuring ${hero.name}:")
        pairs.add("")

        val relevantComps = allCompositions.filter { comp -> comp.heroes.contains(hero.id) }
        if (relevantComps.isEmpty()) {
            pairs.add("No meta compositions found. Try these general tips:")
            when (hero.role) {
                HeroRole.TANK -> pairs.add("Pair with: Healer + 2 DPS + 1 Control")
                HeroRole.DAMAGE, HeroRole.ASSASSIN -> pairs.add("Pair with: Tank + Healer + 1 DPS + 1 Control")
                HeroRole.SUPPORT -> pairs.add("Pair with: 2 Tanks + 2 DPS for sustain")
                HeroRole.CONTROL -> pairs.add("Pair with: Burst DPS to capitalize on CC windows")
            }
        } else {
            relevantComps.forEach { comp ->
                pairs.add("⭐ ${comp.name}")
                pairs.add("   Strategy: ${comp.strategy}")
                pairs.add("   ✅ Strong against: ${comp.strength}")
                pairs.add("   ❌ Weak against: ${comp.weakness}")
                pairs.add("   Team: ${comp.heroes.mapNotNull { HeroDatabase.findHeroByName(it)?.name }.joinToString(", ")}")
                pairs.add("")
            }
        }

        pairs.add("🛡️ COUNTERS:")
        hero.counters.forEach { counterId ->
            HeroDatabase.findHeroByName(counterId)?.let { pairs.add("• Strong vs: ${it.name} (${it.role})") }
        }
        pairs.add("")
        pairs.add("⚠️ WATCH OUT FOR:")
        hero.weakAgainst.forEach { weakId ->
            HeroDatabase.findHeroByName(weakId)?.let { pairs.add("• Weak vs: ${it.name} (${it.role})") }
        }

        return pairs
    }

    private fun calculatePriorityScore(hero: Hero, level: Int?): Double {
        var score = 50.0
        score += when (hero.rarity) {
            HeroRarity.COMMON -> 0.0; HeroRarity.UNCOMMON -> 5.0
            HeroRarity.RARE -> 10.0; HeroRarity.EPIC -> 20.0
            HeroRarity.LEGENDARY -> 30.0; HeroRarity.MYTHIC -> 40.0
        }
        score += when (hero.role) {
            HeroRole.TANK -> 5.0; HeroRole.SUPPORT -> 5.0
            HeroRole.DAMAGE -> 3.0; HeroRole.CONTROL -> 3.0
            HeroRole.ASSASSIN -> 2.0
        }
        if (level != null && level > 80) score -= 15.0
        return score.coerceIn(0.0, 100.0)
    }
}
