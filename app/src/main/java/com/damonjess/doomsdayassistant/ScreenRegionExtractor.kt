package com.damonjess.doomsdayassistant

import android.graphics.Bitmap
import android.graphics.Rect

class ScreenRegionExtractor(private val bitmap: Bitmap) {

    private val width = bitmap.width
    private val height = bitmap.height

    data class Region(val name: String, val rect: Rect, val description: String)

    fun getHeroProfileRegions(): List<Region> = listOf(
        Region("hero_name", pctRect(0.62, 0.08, 0.95, 0.18), "Hero name + title"),
        Region("hero_class", pctRect(0.62, 0.18, 0.95, 0.25), "Class badges"),
        Region("stars", pctRect(0.62, 0.25, 0.85, 0.32), "Star rating"),
        Region("level_exp", pctRect(0.62, 0.32, 0.95, 0.42), "Level badge + EXP"),
        Region("stats_block", pctRect(0.62, 0.42, 0.95, 0.58), "DMG / HP / DEF / Squad"),
        Region("power_score", pctRect(0.35, 0.82, 0.55, 0.88), "Total power number"),
        Region("skill_icons", pctRect(0.62, 0.68, 0.95, 0.82), "Skill icon row"),
        Region("hero_roster", pctRect(0.02, 0.12, 0.22, 0.95), "Left sidebar hero list")
    )

    fun getTalentTreeRegions(): List<Region> = listOf(
        Region("talent_points", pctRect(0.70, 0.02, 0.95, 0.08), "Talent Points"),
        Region("infantry_tree", pctRect(0.35, 0.05, 0.65, 0.50), "Red Infantry nodes"),
        Region("general_tree", pctRect(0.55, 0.45, 0.85, 0.95), "Blue General nodes"),
        Region("skills_tree", pctRect(0.05, 0.45, 0.40, 0.95), "Grey Skills nodes"),
        Region("tree_tabs", pctRect(0.40, 0.88, 0.60, 0.95), "Bottom tabs")
    )

    fun getSkillsDetailRegions(): List<Region> = listOf(
        Region("skill_name", pctRect(0.55, 0.05, 0.95, 0.12), "Skill name + type"),
        Region("skill_description", pctRect(0.55, 0.25, 0.95, 0.55), "Description text"),
        Region("upgrade_preview", pctRect(0.55, 0.55, 0.95, 0.68), "Upgrade Preview"),
        Region("fragment_info", pctRect(0.55, 0.72, 0.95, 0.82), "Fragment count"),
        Region("skill_levels", pctRect(0.15, 0.15, 0.55, 0.85), "Center skill icons"),
        Region("action_buttons", pctRect(0.55, 0.85, 0.95, 0.95), "EXCHANGE / LEVEL UP")
    )

    fun getArmamentsRegions(): List<Region> = listOf(
        Region("hero_info", pctRect(0.55, 0.02, 0.95, 0.20), "Hero name + badges"),
        Region("gear_slots", pctRect(0.15, 0.10, 0.55, 0.90), "8 equipment slots"),
        Region("basic_attributes", pctRect(0.55, 0.28, 0.95, 0.48), "ATK% / DEF% / Lethality"),
        Region("skill_effects", pctRect(0.55, 0.50, 0.95, 0.70), "Skill effects"),
        Region("equip_buttons", pctRect(0.55, 0.88, 0.95, 0.98), "UNEQUIP ALL / QUICK EQUIP")
    )

    fun getArenaFormationRegions(): List<Region> = listOf(
        Region("total_power", pctRect(0.45, 0.01, 0.65, 0.06), "Total power"),
        Region("deployed_heroes", pctRect(0.20, 0.30, 0.50, 0.85), "Deployed heroes"),
        Region("available_roster", pctRect(0.00, 0.10, 0.18, 0.80), "Available heroes"),
        Region("formation_button", pctRect(0.55, 0.82, 0.85, 0.90), "FORMATION COMPLETE")
    )

    private fun pctRect(x1: Double, y1: Double, x2: Double, y2: Double): Rect {
        return Rect(
            (x1 * width).toInt(), (y1 * height).toInt(),
            (x2 * width).toInt(), (y2 * height).toInt()
        )
    }

    fun crop(region: Region): Bitmap {
        return Bitmap.createBitmap(bitmap, region.rect.left, region.rect.top,
            region.rect.width(), region.rect.height())
    }
}
