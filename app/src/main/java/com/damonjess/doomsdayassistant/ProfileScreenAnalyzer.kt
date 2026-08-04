package com.damonjess.doomsdayassistant

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch

class ProfileScreenAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class ProfileAnalysis(
        val heroName: String,
        val heroTitle: String,
        val level: Int,
        val isMaxLevel: Boolean,
        val stars: Int,
        val power: Long,
        val stats: HeroStatsExtracted,
        val skillLevels: List<SkillLevelInfo>,
        val recommendations: List<String>
    )

    data class HeroStatsExtracted(val dmg: Int, val hp: Int, val def: Int, val squad: Int)
    data class SkillLevelInfo(val slot: Int, val current: Int, val max: Int, val isMaxed: Boolean)

    fun analyze(bitmap: Bitmap, rawText: String): ProfileAnalysis {
        val extractor = ScreenRegionExtractor(bitmap)
        val regions = extractor.getHeroProfileRegions()

        val nameRegion = regions.find { it.name == "hero_name" }!!
        val nameText = ocr(extractor.crop(nameRegion))
        val heroName = extractHeroName(nameText)
        val heroTitle = extractHeroTitle(nameText)

        val levelRegion = regions.find { it.name == "level_exp" }!!
        val levelText = ocr(extractor.crop(levelRegion))
        val level = extractLevel(levelText)
        val isMaxLevel = levelText.contains("3,100,000/3,100,000") || levelText.contains("max", ignoreCase = true)

        val statsRegion = regions.find { it.name == "stats_block" }!!
        val statsText = ocr(extractor.crop(statsRegion))
        val stats = extractStats(statsText)

        val powerRegion = regions.find { it.name == "power_score" }!!
        val powerText = ocr(extractor.crop(powerRegion))
        val power = extractPower(powerText)

        val skillRegion = regions.find { it.name == "skill_icons" }!!
        val skillText = ocr(extractor.crop(skillRegion))
        val skillLevels = extractSkillLevels(skillText)

        val recommendations = generateProfileRecommendations(heroName, level, isMaxLevel, stats, skillLevels, power)

        return ProfileAnalysis(heroName, heroTitle, level, isMaxLevel, extractStars(rawText), power, stats, skillLevels, recommendations)
    }

    private fun extractHeroName(text: String): String {
        val knownHeroes = listOf(
            "Miyamoto Doichi", "Norah", "Ammara", "Park Dong-wook", "Lynne",
            "Maddie", "Sarge", "Ghost", "Reaper", "Jeb", "Chef", "Doc", 
            "Witch", "Ash", "Nikola", "Shadow",
            "Claude Le Blanc", "Peggy"
        )
        for (name in knownHeroes) {
            if (text.contains(name, ignoreCase = true)) return name
        }
        val lines = text.lines().filter { it.length > 3 }
        return lines.maxByOrNull { it.length } ?: "Unknown"
    }

    private fun extractHeroTitle(text: String): String {
        val titlePatterns = listOf(
            "Scorched Earth Wanderer", "Reborn Guardian", 
            "Greenfield Mentor", "Ray of Hope"
        )
        for (title in titlePatterns) {
            if (text.contains(title, ignoreCase = true)) return title
        }
        return ""
    }

    private fun extractLevel(text: String): Int {
        val regex = Regex("""[Ll][Vv]\s*(\d+)""")
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun extractStars(text: String): Int {
        val starRegex = Regex("""(\d+)\s*[★⭐]""")
        return starRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun extractStats(text: String): HeroStatsExtracted {
        val dmgRegex = Regex("""DMG\s*[:|\s]*([\d,]+)""")
        val hpRegex = Regex("""HP\s*[:|\s]*([\d,]+)""")
        val defRegex = Regex("""DEF\s*[:|\s]*([\d,]+)""")
        val squadRegex = Regex("""Squad\s*[:|\s]*([\d,]+)""")
        fun parse(regex: Regex): Int {
            return regex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
        }
        return HeroStatsExtracted(parse(dmgRegex), parse(hpRegex), parse(defRegex), parse(squadRegex))
    }

    private fun extractPower(text: String): Long {
        val regex = Regex("""([\d,]+)""")
        val matches = regex.findAll(text.replace(" ", ""))
        // Power is the largest number in the region
        return matches.mapNotNull { 
            it.groupValues[1].replace(",", "").toLongOrNull() 
        }.maxOrNull() ?: 0
    }

    private fun extractSkillLevels(text: String): List<SkillLevelInfo> {
        val levels = mutableListOf<SkillLevelInfo>()
        // Match both "5/5" and standalone digits like "5", "4"
        val regex = Regex("""(\d+)(?:/(\d+))?""")
        val matches = regex.findAll(text).toList()
        matches.forEachIndexed { index, match ->
            if (index >= 5) return@forEachIndexed // Max 5 skills
            val current = match.groupValues[1].toIntOrNull() ?: 0
            val max = match.groupValues[2].toIntOrNull() ?: 5
            levels.add(SkillLevelInfo(index + 1, current, max, current >= max))
        }
        return levels
    }

    private fun generateProfileRecommendations(heroName: String, level: Int, isMaxLevel: Boolean,
        stats: HeroStatsExtracted, skillLevels: List<SkillLevelInfo>, power: Long): List<String> {
        val recs = mutableListOf<String>()
        recs.add("🎯 $heroName Analysis")
        recs.add("Power: ${power.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}")
        recs.add("")

        if (!isMaxLevel) recs.add("⚠️ Level $level — NOT maxed. Priority: Level up first!")
        else recs.add("✅ Level $level — MAXED")

        val incompleteSkills = skillLevels.filter { !it.isMaxed }
        if (incompleteSkills.isNotEmpty()) {
            recs.add("")
            recs.add("📋 SKILL PRIORITY:")
            incompleteSkills.sortedBy { it.current }.forEach { skill ->
                recs.add("• Skill ${skill.slot}: ${skill.current}/${skill.max} ← Upgrade this!")
            }
        } else {
            recs.add("✅ All skills maxed")
        }

        recs.add("")
        recs.add("📊 STATS:")
        recs.add("• DMG: ${stats.dmg.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}")
        recs.add("• HP: ${stats.hp.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}")
        recs.add("• DEF: ${stats.def.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}")
        recs.add("• Squad: ${stats.squad.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}")

        val hero = HeroDatabase.findHeroByName(heroName)
        hero?.let {
            recs.add("")
            recs.add("🎭 ROLE: ${it.role} | ${it.faction}")
            recs.add("💡 ${it.skills.firstOrNull()?.description ?: ""}")
        }

        return recs
    }

    private fun ocr(bitmap: Bitmap): String {
        val input = InputImage.fromBitmap(bitmap, 0)
        var result = ""
        val latch = CountDownLatch(1)
        recognizer.process(input)
            .addOnSuccessListener { result = it.text; latch.countDown() }
            .addOnFailureListener { latch.countDown() }
        latch.await()
        return result
    }
}
