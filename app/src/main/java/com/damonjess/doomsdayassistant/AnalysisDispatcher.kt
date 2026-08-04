package com.damonjess.doomsdayassistant

import android.content.Context
import android.graphics.Bitmap

class AnalysisDispatcher(private val context: Context) {

    private val screenDetector = ScreenTypeDetector()

    data class UnifiedResult(
        val screenType: ScreenType,
        val title: String,
        val priorityScore: Int,
        val sections: List<ResultSection>
    )

    data class ResultSection(val header: String, val items: List<String>)

    fun analyze(bitmap: Bitmap): UnifiedResult {
        val detection = screenDetector.detectScreen(bitmap)
        return when (detection.screenType) {
            ScreenType.HERO_PROFILE -> analyzeProfile(detection.fullBitmap, detection.rawText)
            ScreenType.TALENT_TREE -> analyzeTalent(detection.fullBitmap, detection.rawText)
            ScreenType.SKILLS_DETAIL -> analyzeSkills(detection.fullBitmap, detection.rawText)
            ScreenType.ARMAMENTS -> analyzeArmaments(detection.fullBitmap, detection.rawText)
            ScreenType.ARENA_FORMATION -> analyzeArena(detection.fullBitmap, detection.rawText)
            ScreenType.UNKNOWN -> createUnknownResult()
        }
    }

    private fun analyzeProfile(bitmap: Bitmap, rawText: String): UnifiedResult {
        val analyzer = ProfileScreenAnalyzer()
        val result = analyzer.analyze(bitmap, rawText)
        val sections = mutableListOf<ResultSection>()

        sections.add(ResultSection("📊 OVERVIEW", listOf(
            "Hero: ${result.heroName} (${result.heroTitle})",
            "Level: ${result.level} ${if (result.isMaxLevel) "✅ MAXED" else "⚠️ NOT MAXED"}",
            "Stars: ${result.stars}★",
            "Power: ${result.power.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}"
        )))

        sections.add(ResultSection("📈 STATS", listOf(
            "DMG: ${result.stats.dmg.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}",
            "HP: ${result.stats.hp.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}",
            "DEF: ${result.stats.def.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}",
            "Squad: ${result.stats.squad.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}"
        )))

        if (result.skillLevels.isNotEmpty()) {
            val skillItems = result.skillLevels.map { skill ->
                "Skill ${skill.slot}: ${skill.current}/${skill.max} ${if (skill.isMaxed) "✅" else "⚠️"}"
            }
            sections.add(ResultSection("⚔️ SKILLS", skillItems))
        }

        sections.add(ResultSection("🎯 RECOMMENDATIONS", result.recommendations))

        return UnifiedResult(
            screenType = ScreenType.HERO_PROFILE,
            title = "⚔️ ${result.heroName}",
            priorityScore = calculatePriorityScore(result),
            sections = sections
        )
    }

    private fun analyzeTalent(bitmap: Bitmap, rawText: String): UnifiedResult {
        val analyzer = TalentScreenAnalyzer()
        val result = analyzer.analyze(bitmap, rawText)
        val sections = mutableListOf<ResultSection>()

        sections.add(ResultSection("🌳 TALENT OVERVIEW", listOf(
            "Points Available: ${result.pointsAvailable}",
            "Trees: ${result.trees.size}"
        )))

        result.trees.forEach { tree ->
            val percent = (tree.completionPercent * 100).toInt()
            val items = mutableListOf("Completion: $percent%")
            val incomplete = tree.nodes.filter { !it.isMaxed }
            if (incomplete.isNotEmpty()) items.add("Incomplete nodes: ${incomplete.size}")
            sections.add(ResultSection("📌 ${tree.name} Tree", items))
        }

        sections.add(ResultSection("🎯 RECOMMENDATIONS", result.recommendations))

        return UnifiedResult(
            screenType = ScreenType.TALENT_TREE,
            title = "🌳 Talent Tree",
            priorityScore = if (result.pointsAvailable > 0) 90 else 50,
            sections = sections
        )
    }

    private fun analyzeSkills(bitmap: Bitmap, rawText: String): UnifiedResult {
        val analyzer = SkillsScreenAnalyzer()
        val result = analyzer.analyze(bitmap, rawText)
        val sections = mutableListOf<ResultSection>()

        sections.add(ResultSection("⚔️ SKILL INFO", listOf(
            "Name: ${result.skillName}",
            "Type: ${result.skillType}",
            "Level: ${result.currentLevel}/${result.maxLevel} ${if (result.currentLevel >= result.maxLevel) "✅" else "⚠️"}"
        )))

        sections.add(ResultSection("📦 FRAGMENTS", listOf(
            "Progress: ${result.fragments.current}/${result.fragments.needed}",
            "Owned: ${result.fragments.owned}",
            if (result.fragments.canExchange) "✅ Can exchange now!" else "Need ${result.fragments.needed - result.fragments.current} more"
        )))

        sections.add(ResultSection("📝 DESCRIPTION", listOf(result.description)))
        sections.add(ResultSection("🎯 RECOMMENDATIONS", result.recommendations))

        return UnifiedResult(
            screenType = ScreenType.SKILLS_DETAIL,
            title = "⚔️ ${result.skillName}",
            priorityScore = if (result.currentLevel < result.maxLevel) 85 else 30,
            sections = sections
        )
    }

    private fun analyzeArmaments(bitmap: Bitmap, rawText: String): UnifiedResult {
        val analyzer = ArmamentsScreenAnalyzer()
        val result = analyzer.analyze(bitmap, rawText)
        val sections = mutableListOf<ResultSection>()

        sections.add(ResultSection("📊 ATTRIBUTES", listOf(
            "Squad ATK: ${result.attributes.squadAtk}%",
            "Squad DEF: ${result.attributes.squadDef}%",
            "Lethality: ${result.attributes.lethality}"
        )))

        val gearItems = result.gearSlots.map { slot ->
            val icon = when (slot.rarity) {
                ArmamentsScreenAnalyzer.GearRarity.ORANGE -> "🟠"
                ArmamentsScreenAnalyzer.GearRarity.PURPLE -> "🟣"
                ArmamentsScreenAnalyzer.GearRarity.BLUE -> "🔵"
                ArmamentsScreenAnalyzer.GearRarity.GREEN -> "🟢"
                else -> "⚪"
            }
            "$icon ${slot.position}: ${slot.rarity}"
        }
        sections.add(ResultSection("🛡️ GEAR", gearItems))
        sections.add(ResultSection("🎯 RECOMMENDATIONS", result.recommendations))

        val purpleCount = result.gearSlots.count { it.rarity == ArmamentsScreenAnalyzer.GearRarity.PURPLE }
        return UnifiedResult(
            screenType = ScreenType.ARMAMENTS,
            title = "🛡️ ${result.heroName} Gear",
            priorityScore = if (purpleCount > 0) 75 else 50,
            sections = sections
        )
    }

    private fun analyzeArena(bitmap: Bitmap, rawText: String): UnifiedResult {
        val analyzer = ArenaScreenAnalyzer()
        val result = analyzer.analyze(bitmap, rawText)
        val sections = mutableListOf<ResultSection>()

        sections.add(ResultSection("⚔️ TEAM", result.deployedHeroes.map { hero ->
            "${hero.name} — ${hero.power.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")} [${hero.position}]"
        }))

        sections.add(ResultSection("📊 COMPOSITION", listOf(
            "Infantry: ${result.teamComposition.infantryCount}",
            "Riders: ${result.teamComposition.riderCount}",
            "Hunters: ${result.teamComposition.hunterCount}",
            "Has Tank: ${if (result.teamComposition.hasTank) "✅" else "❌"}",
            "Has Support: ${if (result.teamComposition.hasSupport) "✅" else "❌"}"
        )))

        sections.add(ResultSection("🎯 RECOMMENDATIONS", result.recommendations))

        return UnifiedResult(
            screenType = ScreenType.ARENA_FORMATION,
            title = "🏟️ Arena Formation",
            priorityScore = 70,
            sections = sections
        )
    }

    private fun createUnknownResult(): UnifiedResult {
        return UnifiedResult(
            screenType = ScreenType.UNKNOWN,
            title = "❓ Unknown Screen",
            priorityScore = 0,
            sections = listOf(ResultSection("⚠️ ERROR", listOf(
                "Could not identify the current screen.",
                "Make sure you're on one of these screens:",
                "• Hero Profile", "• Talent Tree", "• Skills Detail",
                "• Armaments", "• Arena Formation"
            )))
        )
    }

    private fun calculatePriorityScore(profile: ProfileScreenAnalyzer.ProfileAnalysis): Int {
        var score = 50
        if (!profile.isMaxLevel) score += 20
        val incompleteSkills = profile.skillLevels.count { !it.isMaxed }
        score += incompleteSkills * 10
        return score.coerceIn(0, 100)
    }
}
