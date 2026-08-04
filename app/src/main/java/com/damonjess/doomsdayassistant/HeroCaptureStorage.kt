package com.damonjess.doomsdayassistant

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
        Log.d("DoomsdayCapture", "Saving hero to storage: ${hero.name}")
        val roster = getAllHeroes().toMutableList()
        roster.removeAll { it.heroId == hero.heroId }
        roster.add(hero)
        while (roster.size > MAX_ROSTER_SIZE) {
            roster.removeAt(0)
        }
        val success = saveRoster(roster)
        Log.d("DoomsdayCapture", "Hero save status: $success. Roster size: ${roster.size}")
        return success
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
        prefs.edit().remove(KEY_ROSTER).commit()
    }

    fun hasHero(heroId: String): Boolean {
        return getAllHeroes().any { it.heroId == heroId }
    }

    private fun saveRoster(roster: List<CapturedHero>): Boolean {
        val jsonArray = JSONArray()
        roster.forEach { hero ->
            jsonArray.put(heroToJson(hero))
        }
        prefs.edit().putString(KEY_ROSTER, jsonArray.toString()).apply()
        return true
    }

    private fun parseRoster(jsonStr: String): List<CapturedHero> {
        val roster = mutableListOf<CapturedHero>()
        if (jsonStr == "[]") return roster
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                try {
                    roster.add(jsonToHero(obj))
                } catch (e: Exception) {
                    Log.e("DoomsdayCapture", "Failed to parse hero at index $i: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("DoomsdayCapture", "Failed to parse roster JSON: ${e.message}")
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
            rarity = safeValueOf(obj.getString("rarity"), HeroRarity.COMMON),
            role = safeValueOf(obj.getString("role"), HeroRole.DAMAGE),
            faction = safeValueOf(obj.getString("faction"), Faction.SURVIVORS),
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

    private inline fun <reified T : Enum<T>> safeValueOf(value: String, default: T): T {
        return try {
            java.lang.Enum.valueOf(T::class.java, value.uppercase())
        } catch (e: Exception) {
            default
        }
    }
}
