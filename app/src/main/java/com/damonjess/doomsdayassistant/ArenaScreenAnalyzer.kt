package com.damonjess.doomsdayassistant

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch

class ArenaScreenAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class ArenaAnalysis(val deployedHeroes: List<DeployedHero>, val totalPower: Long,
        val teamComposition: TeamComposition, val recommendations: List<String>)

    data class DeployedHero(val name: String, val power: Long, val position: String)
    data class TeamComposition(val infantryCount: Int, val riderCount: Int, val hunterCount: Int,
        val hasSupport: Boolean, val hasTank: Boolean)

    fun analyze(bitmap: Bitmap, rawText: String): ArenaAnalysis {
        val extractor = ScreenRegionExtractor(bitmap)
        val regions = extractor.getArenaFormationRegions()

        val powerText = ocr(extractor.crop(regions.find { it.name == "total_power" }!!))
        val totalPower = extractTotalPower(powerText)

        val deployedText = ocr(extractor.crop(regions.find { it.name == "deployed_heroes" }!!))
        val deployedHeroes = extractDeployedHeroes(deployedText)

        val rosterText = ocr(extractor.crop(regions.find { it.name == "available_roster" }!!))
        val availableHeroes = extractAvailableHeroes(rosterText)

        val composition = analyzeComposition(deployedHeroes)
        val recommendations = generateArenaRecommendations(deployedHeroes, availableHeroes, composition, totalPower)

        return ArenaAnalysis(deployedHeroes, totalPower, composition, recommendations)
    }

    private fun extractTotalPower(text: String): Long {
        val regex = Regex("""([\d,]+)""")
        val match = regex.find(text.replace(" ", ""))
        return match?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0
    }

    private fun extractDeployedHeroes(text: String): List<DeployedHero> {
        val heroes = mutableListOf<DeployedHero>()
        val knownHeroes = listOf("Miyamoto Doichi", "Norah", "Ammara", "Park Dong-wook", "Lynne",
            "Maddie", "Sarge", "Ghost", "Reaper", "Jeb", "Chef", "Doc", "Witch", "Ash")
        val lines = text.lines()
        knownHeroes.forEach { name ->
            val nameLine = lines.indexOfFirst { it.contains(name, ignoreCase = true) }
            if (nameLine != -1) {
                val powerLine = lines.getOrNull(nameLine + 1) ?: ""
                val powerMatch = Regex("""([\d,]+)""").find(powerLine)
                val power = powerMatch?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 0
                val position = when {
                    nameLine < lines.size / 3 -> "Front"
                    nameLine < lines.size * 2 / 3 -> "Mid"
                    else -> "Back"
                }
                heroes.add(DeployedHero(name, power, position))
            }
        }
        return heroes
    }

    private fun extractAvailableHeroes(text: String): List<String> {
        val knownHeroes = listOf("Miyamoto Doichi", "Norah", "Ammara", "Park Dong-wook", "Lynne",
            "Maddie", "Sarge", "Ghost", "Reaper", "Jeb", "Chef", "Doc", "Witch", "Ash")
        return knownHeroes.filter { text.contains(it, ignoreCase = true) }
    }

    private fun analyzeComposition(heroes: List<DeployedHero>): TeamComposition {
        var infantry = 0
        var riders = 0
        var hunters = 0
        var hasSupport = false
        var hasTank = false
        heroes.forEach { hero ->
            when {
                listOf("Miyamoto Doichi", "Sarge", "Maddie", "Park Dong-wook").any {
                    hero.name.contains(it, ignoreCase = true)
                } -> { infantry++; hasTank = true }
                listOf("Ammara", "Ghost", "Shadow", "Jeb").any {
                    hero.name.contains(it, ignoreCase = true)
                } -> riders++
                listOf("Norah", "Lynne", "Chef", "Doc", "Witch").any {
                    hero.name.contains(it, ignoreCase = true)
                } -> { hunters++; hasSupport = true }
            }
        }
        return TeamComposition(infantry, riders, hunters, hasSupport, hasTank)
    }

    private fun generateArenaRecommendations(deployed: List<DeployedHero>, available: List<String>,
        comp: TeamComposition, totalPower: Long): List<String> {
        val recs = mutableListOf<String>()
        recs.add("🏟️ ARENA FORMATION ANALYSIS")
        recs.add("Total Power: ${totalPower.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")}")
        recs.add("")

        recs.add("⚔️ DEPLOYED TEAM:")
        deployed.forEach { hero ->
            val powerStr = hero.power.toString().replace(Regex("""(\d)(?=(\d{3})+$)"""), "$1,")
            recs.add("• ${hero.name} — $powerStr [${hero.position}]")
        }

        recs.add("")
        recs.add("📊 COMPOSITION:")
        recs.add("• Infantry: ${comp.infantryCount} | Riders: ${comp.riderCount} | Hunters: ${comp.hunterCount}")
        if (!comp.hasTank) recs.add("⚠️ NO TANK — Your team will fold to burst damage!")
        if (!comp.hasSupport) recs.add("⚠️ NO SUPPORT — No heals or buffs for sustain")

        recs.add("")
        recs.add("🎯 RECOMMENDATIONS:")
        if (!comp.hasSupport && available.any { it.contains("Chef") || it.contains("Doc") }) {
            recs.add("1. Swap a DPS for Chef or Doc (healer)")
        }
        if (!comp.hasTank && available.any { it.contains("Maddie") || it.contains("Sarge") }) {
            recs.add("2. Add Maddie or Sarge as frontline tank")
        }
        val powers = deployed.map { it.power }
        val avgPower = powers.average()
        val weakest = deployed.minByOrNull { it.power }
        weakest?.let {
            if (it.power < avgPower * 0.8) {
                recs.add("3. ${it.name} is underpowered — upgrade or replace")
            }
        }
        recs.add("4. Position tankiest hero in FRONT, DPS in BACK")
        recs.add("")
        recs.add("⭐ META COMPS:")
        recs.add("• Balanced: Tank + 2 DPS + Support + Control")
        recs.add("• Burst: 3 DPS + 1 Tank + 1 Support (fast wins)")
        recs.add("• Turtle: 2 Tank + 2 Support + 1 DPS (outlast)")

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
