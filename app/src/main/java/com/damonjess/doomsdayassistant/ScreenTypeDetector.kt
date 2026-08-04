package com.damonjess.doomsdayassistant

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch

enum class ScreenType {
    HERO_PROFILE, TALENT_TREE, SKILLS_DETAIL, ARMAMENTS, ARENA_FORMATION, UNKNOWN
}

class ScreenTypeDetector {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun detectScreen(bitmap: Bitmap): ScreenDetectionResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        var result: ScreenDetectionResult? = null
        val latch = CountDownLatch(1)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text
                val screenType = identifyByLandmarks(rawText)
                result = ScreenDetectionResult(screenType, rawText, bitmap)
                latch.countDown()
            }
            .addOnFailureListener {
                result = ScreenDetectionResult(ScreenType.UNKNOWN, "", bitmap)
                latch.countDown()
            }

        latch.await()
        return result!!
    }

    private fun identifyByLandmarks(text: String): ScreenType {
        val lower = text.lowercase()
        val scores = mutableMapOf<ScreenType, Int>()

        scores[ScreenType.HERO_PROFILE] = countMatches(lower, listOf(
            "might", "infantry squad", "general", "skills",
            "dmg", "hp", "def", "squad", "exp", "lv", "level",
            "hero talent", "skin", "chronicles", "comment", "strategy"
        ))

        scores[ScreenType.TALENT_TREE] = countMatches(lower, listOf(
            "talent points", "recommended", "infantry squad", "general", "skills",
            "reset", "/6", "/5", "/4", "/3", "/2"
        ))

        scores[ScreenType.SKILLS_DETAIL] = countMatches(lower, listOf(
            "passive skill", "active skill", "hero skill info",
            "upgrade preview", "fragment", "exchange", "level up",
            "skills reset", "battle", "adventure", "when leading"
        ))

        scores[ScreenType.ARMAMENTS] = countMatches(lower, listOf(
            "hero armaments", "armament attribute", "basic attributes",
            "quick equip", "unequip all", "skill effects",
            "squad atk", "squad def", "lethality", "gathering"
        ))

        scores[ScreenType.ARENA_FORMATION] = countMatches(lower, listOf(
            "formation complete", "formation", "deploy",
            "total power", "might", "battle", "attack"
        ))

        val best = scores.maxByOrNull { it.value }
        return if (best != null && best.value >= 3) best.key else ScreenType.UNKNOWN
    }

    private fun countMatches(text: String, keywords: List<String>): Int {
        return keywords.count { text.contains(it) }
    }

    data class ScreenDetectionResult(
        val screenType: ScreenType,
        val rawText: String,
        val fullBitmap: Bitmap
    )
}
