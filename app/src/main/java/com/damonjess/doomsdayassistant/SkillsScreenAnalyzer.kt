package com.damonjess.doomsdayassistant

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch

class SkillsScreenAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class SkillsAnalysis(
        val skillName: String, val skillType: String, val currentLevel: Int, val maxLevel: Int,
        val description: String, val upgradePreview: String,
        val fragments: FragmentStatus, val recommendations: List<String>
    )

    data class FragmentStatus(val current: Int, val needed: Int, val owned: Int, val canExchange: Boolean)

    fun analyze(bitmap: Bitmap, rawText: String): SkillsAnalysis {
        val extractor = ScreenRegionExtractor(bitmap)
        val regions = extractor.getSkillsDetailRegions()

        val nameText = ocr(extractor.crop(regions.find { it.name == "skill_name" }!!))
        val skillName = extractSkillName(nameText)
        val skillType = extractSkillType(nameText)

        val description = ocr(extractor.crop(regions.find { it.name == "skill_description" }!!))
        val previewText = ocr(extractor.crop(regions.find { it.name == "upgrade_preview" }!!))
        val fragmentText = ocr(extractor.crop(regions.find { it.name == "fragment_info" }!!))
        val fragments = extractFragments(fragmentText)

        val levelsText = ocr(extractor.crop(regions.find { it.name == "skill_levels" }!!))
        val (currentLevel, maxLevel) = extractSkillLevel(levelsText, skillName)

        val recommendations = generateSkillRecommendations(skillName, skillType, currentLevel, maxLevel, fragments, description)

        return SkillsAnalysis(skillName, skillType, currentLevel, maxLevel, description.take(200), previewText, fragments, recommendations)
    }

    private fun extractSkillName(text: String): String {
        val lines = text.lines().filter { it.isNotBlank() }
        return lines.firstOrNull { it.length > 3 && !it.contains("skill", ignoreCase = true) } ?: "Unknown Skill"
    }

    private fun extractSkillType(text: String): String {
        return when {
            text.contains("Passive Skill", ignoreCase = true) -> "Passive"
            text.contains("Active Skill", ignoreCase = true) -> "Active"
            text.contains("Ultimate", ignoreCase = true) -> "Ultimate"
            else -> "Unknown"
        }
    }

    private fun extractSkillLevel(text: String, skillName: String): Pair<Int, Int> {
        val regex = Regex("""(\d+)[/\s](\d+)""")
        val matches = regex.findAll(text).toList()
        return if (matches.isNotEmpty()) {
            Pair(matches.first().groupValues[1].toInt(), matches.first().groupValues[2].toInt())
        } else Pair(0, 5)
    }

    private fun extractFragments(text: String): FragmentStatus {
        val currentRegex = Regex("""(\d+)[/\s](\d+)""")
        val ownedRegex = Regex("""Owned:\s*(\d+)""")
        val match = currentRegex.find(text)
        val current = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val needed = match?.groupValues?.get(2)?.toIntOrNull() ?: 80
        val ownedMatch = ownedRegex.find(text)
        val owned = ownedMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return FragmentStatus(current, needed, owned, owned > 0 && current < needed)
    }

    private fun generateSkillRecommendations(skillName: String, skillType: String, currentLevel: Int,
        maxLevel: Int, fragments: FragmentStatus, description: String): List<String> {
        val recs = mutableListOf<String>()
        recs.add("⚔️ $skillName [$skillType]")
        recs.add("Level: $currentLevel/$maxLevel")
        recs.add("")

        if (currentLevel < maxLevel) {
            recs.add("⚠️ NOT MAXED — Priority upgrade!")
            recs.add("")
            if (fragments.canExchange) {
                recs.add("✅ You have ${fragments.owned} fragments — EXCHANGE NOW!")
            } else {
                val remaining = fragments.needed - fragments.current
                recs.add("📦 Need $remaining more fragments (${fragments.current}/${fragments.needed})")
                recs.add("💡 Farm from: Hero Trials / Events / Shop")
            }
            recs.add("")
            recs.add("📈 UPGRADE PREVIEW:")
            val percentRegex = Regex("""(\d+)%""")
            val percents = percentRegex.findAll(description).map { it.groupValues[1] }.toList()
            if (percents.size >= 2) {
                recs.add("• Current: ${percents[0]}% → Next: ${percents[1]}%")
            }
        } else {
            recs.add("✅ MAXED — This skill is complete!")
        }

        recs.add("")
        recs.add("📝 EFFECT:")
        recs.add(description.take(150))

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
