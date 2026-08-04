package com.damonjess.doomsdayassistant

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class HeroCaptureStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "DoomsdayArenaRoster"
        private const val KEY_ROSTER = "captured_heroes"
        private const val MAX_ROSTER_SIZE = 50
    }

    fun saveHero(hero: CapturedHero): Boolean {
        val roster = getAllHeroes().toMutableList()
        roster.removeAll { it.heroId == hero.heroId }
        roster.add(hero)
        while (roster.size > MAX_ROSTER_SIZE) {
            roster.removeAt(0)
        }
        saveRoster(roster)
        return true
    }

    fun removeHero(captureId: String) {
        val roster = getAllHeroes().filter { it.id != captureId }
        saveRoster(roster)
    }

    fun getAllHeroes(): List<CapturedHero> {
        val jsonStr = prefs.getString(KEY_ROSTER, "[]") ?: "[]"
        return parseRoster(jsonStr).sortedByDescending { it.power }
    }

    fun getHeroesByRole(role: HeroRole): List<CapturedHero> {
        return getAllHeroes().filter { it.role == role }
    }

    fun getRosterSize(): Int = getAllHeroes().size

    fun clearRoster() {
        prefs.edit().remove(KEY_ROSTER).apply()
    }

    fun hasHero(heroId: String): Boolean {
        return getAllHeroes().any { it.heroId == heroId }
    }

    private fun saveRoster(roster: List<CapturedHero>) {
        val jsonArray = JSONArray()
        roster.forEach { hero ->
            jsonArray.put(heroToJson(hero))
        }
        prefs.edit().putString(KEY_ROSTER, jsonArray.toString()).apply()
    }

    private fun parseRoster(jsonStr: String): List<CapturedHero> {
        val roster = mutableListOf<CapturedHero>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                roster.add(jsonToHero(obj))
            }
        } catch (e: Exception) {
        }
        return roster
    }

    private fun heroToJson(hero: CapturedHero): JSONObject {
        return JSONObject().apply {
            put("id", hero.id)
            put("heroId", hero.heroId)
            put("name", hero.name)
            put("rarity", hero.rarity.name)
            put("role", hero.role.name)
            put("faction", hero.faction.name)
            put("level", hero.level)
            put("stars", hero.stars)
            put("power", hero.power)
            put("attack", hero.stats.attack)
            put("defense", hero.stats.defense)
            put("hp", hero.stats.hp)
            put("speed", hero.stats.speed)
            put("critRate", hero.stats.critRate)
            put("critDamage", hero.stats.critDamage)
            put("skillLevels", JSONArray(hero.skillLevels))
            put("capturedAt", hero.capturedAt)
            put("isMaxed", hero.isMaxed)
        }
    }

    private fun jsonToHero(obj: JSONObject): CapturedHero {
        val skillArray = obj.getJSONArray("skillLevels")
        val skillLevels = mutableListOf<Int>()
        for (i in 0 until skillArray.length()) {
            skillLevels.add(skillArray.getInt(i))
        }

        return CapturedHero(
            id = obj.getString("id"),
            heroId = obj.getString("heroId"),
            name = obj.getString("name"),
            rarity = HeroRarity.valueOf(obj.getString("rarity")),
            role = HeroRole.valueOf(obj.getString("role")),
            faction = Faction.valueOf(obj.getString("faction")),
            level = obj.getInt("level"),
            stars = obj.getInt("stars"),
            power = obj.getLong("power"),
            stats = HeroStats(
                attack = obj.getInt("attack"),
                defense = obj.getInt("defense"),
                hp = obj.getInt("hp"),
                speed = obj.getInt("speed"),
                critRate = obj.getDouble("critRate").toFloat(),
                critDamage = obj.getDouble("critDamage").toFloat()
            ),
            skillLevels = skillLevels,
            capturedAt = obj.getLong("capturedAt"),
            isMaxed = obj.optBoolean("isMaxed", false)
        )
    }
}
