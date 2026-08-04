package com.damonjess.doomsdayassistant

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.CountDownLatch

class ArmamentsScreenAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class ArmamentsAnalysis(val heroName: String, val gearSlots: List<GearSlot>,
        val attributes: GearAttributes, val recommendations: List<String>)

    data class GearSlot(val position: String, val rarity: GearRarity, val isEquipped: Boolean)
    enum class GearRarity { EMPTY, GREEN, BLUE, PURPLE, ORANGE, RED }

    data class GearAttributes(val squadAtk: Float, val squadDef: Float, val lethality: Int, val skillEffects: List<String>)

    fun analyze(bitmap: Bitmap, rawText: String): ArmamentsAnalysis {
        val extractor = ScreenRegionExtractor(bitmap)
        val regions = extractor.getArmamentsRegions()

        val heroText = ocr(extractor.crop(regions.find { it.name == "hero_info" }!!))
        val heroName = extractHeroName(heroText)

        val attrText = ocr(extractor.crop(regions.find { it.name == "basic_attributes" }!!))
        val attributes = extractAttributes(attrText)

        val gearBitmap = extractor.crop(regions.find { it.name == "gear_slots" }!!)
        val gearSlots = analyzeGearSlots(gearBitmap)

        val recommendations = generateGearRecommendations(heroName, gearSlots, attributes)

        return ArmamentsAnalysis(heroName, gearSlots, attributes, recommendations)
    }

    private fun extractHeroName(text: String): String {
        val known = listOf("Miyamoto Doichi", "Norah", "Ammara", "Park Dong-wook", "Lynne")
        for (name in known) {
            if (text.contains(name, ignoreCase = true)) return name
        }
        return text.lines().firstOrNull { it.length > 3 } ?: "Unknown"
    }

    private fun extractAttributes(text: String): GearAttributes {
        val atkRegex = Regex("""Squad ATK\s*([\d.]+)%""")
        val defRegex = Regex("""Squad DEF\s*([\d.]+)%""")
        val lethRegex = Regex("""Lethality\s*([\d,]+)""")
        val atk = atkRegex.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val def = defRegex.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        val leth = lethRegex.find(text)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0
        val effects = text.lines().filter {
            it.contains("Increases", ignoreCase = true) || it.contains("Gain", ignoreCase = true)
        }
        return GearAttributes(atk, def, leth, effects)
    }

    private fun analyzeGearSlots(bitmap: Bitmap): List<GearSlot> {
        val slots = mutableListOf<GearSlot>()
        val positions = listOf("Helmet", "Vest", "Weapon", "Backpack", "Boots", "Pants", "Mask", "Gadget")
        positions.forEachIndexed { index, pos ->
            val sampleX = bitmap.width / 2
            val sampleY = (bitmap.height * (index + 1) / 9)
            val color = bitmap.getPixel(sampleX.coerceIn(0, bitmap.width - 1), sampleY.coerceIn(0, bitmap.height - 1))
            val rarity = classifyRarityByColor(color)
            slots.add(GearSlot(pos, rarity, rarity != GearRarity.EMPTY))
        }
        return slots
    }

    private fun classifyRarityByColor(color: Int): GearRarity {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return when {
            r > 200 && g > 100 && g < 180 -> GearRarity.ORANGE
            r > 150 && g < 100 && b > 150 -> GearRarity.PURPLE
            b > 150 && r < 100 -> GearRarity.BLUE
            g > 150 && r < 100 -> GearRarity.GREEN
            r > 200 && g < 50 -> GearRarity.RED
            else -> GearRarity.EMPTY
        }
    }

    private fun generateGearRecommendations(heroName: String, gearSlots: List<GearSlot>,
        attributes: GearAttributes): List<String> {
        val recs = mutableListOf<String>()
        recs.add("🛡️ ARMAMENTS: $heroName")
        recs.add("")
        val equipped = gearSlots.count { it.isEquipped }
        recs.add("Equipped: $equipped/8 slots")
        recs.add("")

        recs.add("📦 GEAR BREAKDOWN:")
        gearSlots.forEach { slot ->
            val icon = when (slot.rarity) {
                GearRarity.ORANGE -> "🟠"
                GearRarity.PURPLE -> "🟣"
                GearRarity.BLUE -> "🔵"
                GearRarity.GREEN -> "🟢"
                else -> "⚪"
            }
            recs.add("$icon ${slot.position}: ${slot.rarity}")
        }

        recs.add("")
        recs.add("📊 ATTRIBUTES:")
        recs.add("• Squad ATK: ${attributes.squadAtk}%")
        recs.add("• Squad DEF: ${attributes.squadDef}%")
        recs.add("• Lethality: ${attributes.lethality}")

        if (attributes.skillEffects.isNotEmpty()) {
            recs.add("")
            recs.add("✨ SKILL EFFECTS:")
            attributes.skillEffects.forEach { recs.add("• $it") }
        }

        recs.add("")
        recs.add("🎯 UPGRADE PRIORITY:")
        val purpleSlots = gearSlots.filter { it.rarity == GearRarity.PURPLE }
        if (purpleSlots.isNotEmpty()) {
            recs.add("1. Upgrade ${purpleSlots.size} Purple slots to Orange:")
            purpleSlots.forEach { recs.add("   → ${it.position}") }
        }
        val emptySlots = gearSlots.filter { !it.isEquipped }
        if (emptySlots.isNotEmpty()) {
            recs.add("2. Equip missing slots: ${emptySlots.joinToString { it.position }}")
        }
        recs.add("3. Priority order: Weapon → Helmet → Boots → Vest → Rest")

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
