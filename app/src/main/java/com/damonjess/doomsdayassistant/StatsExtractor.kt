package com.damonjess.doomsdayassistant

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch

class StatsExtractor(private val bitmap: Bitmap) {

    data class GameStats(
        val level: Int,
        val power: Long,
        val stars: Int,
        val skillLevels: List<Int>,
        val dmg: Int,
        val hp: Int,
        val def: Int,
        val squad: Int
    )

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun extract(): GameStats {
        val extractor = ScreenRegionExtractor(bitmap)

        val levelText = ocr(extractor.crop(extractor.getHeroProfileRegions().find { it.name == "level_exp" }!!))
        val level = Regex("""LV\s*(\d+)""", RegexOption.IGNORE_CASE).find(levelText)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        val powerText = ocr(extractor.crop(extractor.getHeroProfileRegions().find { it.name == "power_score" }!!))
        val power = extractLargestNumber(powerText)

        val skillText = ocr(extractor.crop(extractor.getHeroProfileRegions().find { it.name == "skill_icons" }!!))
        val skills = extractSkillDigits(skillText)

        val statsText = ocr(extractor.crop(extractor.getHeroProfileRegions().find { it.name == "stats_block" }!!))
        val dmg = extractStat(statsText, "DMG")
        val hp = extractStat(statsText, "HP")
        val def = extractStat(statsText, "DEF")
        val squad = extractStat(statsText, "Squad")

        val nameText = ocr(extractor.crop(extractor.getHeroProfileRegions().find { it.name == "hero_name" }!!))
        val stars = Regex("""(\d+)\s*[★⭐]""").find(nameText)?.groupValues?.get(1)?.toIntOrNull() ?: 6

        return GameStats(level, power, stars, skills, dmg, hp, def, squad)
    }

    private fun extractLargestNumber(text: String): Long {
        return Regex("""[\d,]+""").findAll(text)
            .mapNotNull { it.value.replace(",", "").toLongOrNull() }
            .maxOrNull() ?: 0
    }

    private fun extractSkillDigits(text: String): List<Int> {
        return Regex("""\b([1-5])\b""").findAll(text)
            .map { it.groupValues[1].toInt() }
            .take(5)
            .toList()
    }

    private fun extractStat(text: String, label: String): Int {
        val pattern = Regex("""$label\s*[:|\s]*([\d,]+)""", RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
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
