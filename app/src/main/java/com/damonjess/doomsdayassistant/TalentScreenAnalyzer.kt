package com.damonjess.doomsdayassistant

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch

class TalentScreenAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class TalentAnalysis(val pointsAvailable: Int, val trees: List<TalentTree>, val recommendations: List<String>)
    data class TalentTree(val name: String, val nodes: List<TalentNode>, val completionPercent: Float)
    data class TalentNode(val current: Int, val max: Int, val isMaxed: Boolean)

    fun analyze(bitmap: Bitmap, rawText: String): TalentAnalysis {
        val extractor = ScreenRegionExtractor(bitmap)
        val regions = extractor.getTalentTreeRegions()

        val pointsRegion = regions.find { it.name == "talent_points" }!!
        val pointsText = ocr(extractor.crop(pointsRegion))
        val pointsAvailable = extractTalentPoints(pointsText)

        val infantryRegion = regions.find { it.name == "infantry_tree" }!!
        val infantryTree = parseTree("Infantry", ocr(extractor.crop(infantryRegion)))

        val generalRegion = regions.find { it.name == "general_tree" }!!
        val generalTree = parseTree("General", ocr(extractor.crop(generalRegion)))

        val skillsRegion = regions.find { it.name == "skills_tree" }!!
        val skillsTree = parseTree("Skills", ocr(extractor.crop(skillsRegion)))

        val recommendations = generateTalentRecommendations(pointsAvailable, listOf(infantryTree, generalTree, skillsTree))

        return TalentAnalysis(pointsAvailable, listOf(infantryTree, generalTree, skillsTree), recommendations)
    }

    private fun extractTalentPoints(text: String): Int {
        val regex = Regex("""Talent Points:\s*(\d+)""")
        return regex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun parseTree(name: String, text: String): TalentTree {
        val nodes = mutableListOf<TalentNode>()
        val regex = Regex("""(\d+)/(\d+)""")
        regex.findAll(text).forEach { match ->
            val current = match.groupValues[1].toInt()
            val max = match.groupValues[2].toInt()
            nodes.add(TalentNode(current, max, current >= max))
        }
        val totalPoints = nodes.sumOf { it.max }
        val investedPoints = nodes.sumOf { it.current }
        val completion = if (totalPoints > 0) investedPoints.toFloat() / totalPoints else 0f
        return TalentTree(name, nodes, completion)
    }

    private fun generateTalentRecommendations(pointsAvailable: Int, trees: List<TalentTree>): List<String> {
        val recs = mutableListOf<String>()
        recs.add("🌳 TALENT TREE ANALYSIS")
        recs.add("")
        if (pointsAvailable > 0) {
            recs.add("⚡ You have $pointsAvailable talent points to spend!")
            recs.add("")
        }

        trees.forEach { tree ->
            val percent = (tree.completionPercent * 100).toInt()
            val maxedNodes = tree.nodes.count { it.isMaxed }
            val totalNodes = tree.nodes.size
            recs.add("📌 ${tree.name} Tree: $percent% complete ($maxedNodes/$totalNodes nodes maxed)")
            val incomplete = tree.nodes.filter { !it.isMaxed }
            if (incomplete.isNotEmpty()) {
                val lowest = incomplete.minByOrNull { it.current.toFloat() / it.max }
                lowest?.let { recs.add("   → Priority: ${it.current}/${it.max} node") }
            }
        }

        recs.add("")
        recs.add("🎯 RECOMMENDED PATH:")
        val leastComplete = trees.minByOrNull { it.completionPercent }
        leastComplete?.let {
            recs.add("1. Finish ${it.name} tree first (only ${(it.completionPercent * 100).toInt()}% done)")
        }
        val infantryTree = trees.find { it.name == "Infantry" }
        if (infantryTree != null && infantryTree.completionPercent < 1.0f) {
            recs.add("2. Max Infantry ATK and DEF nodes for squad bonuses")
        }
        recs.add("3. General tree: Focus Speed → HP → DEF")
        recs.add("4. Skills tree: Last priority unless skill-dependent hero")

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
