package com.damonjess.doomsdayassistant

object PairingEngine {

    fun calculatePairScore(hero1: CapturedHero, hero2: CapturedHero): Double {
        var score = 0.0
        score += roleSynergy(hero1.role, hero2.role)
        score += factionSynergy(hero1.faction, hero2.faction)
        score += databaseSynergy(hero1, hero2)
        score += statComplement(hero1, hero2)
        score += powerBalance(hero1, hero2)
        return score.coerceIn(0.0, 100.0)
    }

    fun analyzePair(hero1: CapturedHero, hero2: CapturedHero): String {
        val reasons = mutableListOf<String>()
        when {
            hero1.role == HeroRole.TANK && hero2.role == HeroRole.SUPPORT ->
                reasons.add("${hero1.name} tanks while ${hero2.name} keeps them alive")
            hero1.role == HeroRole.SUPPORT && hero2.role == HeroRole.TANK ->
                reasons.add("${hero2.name} tanks while ${hero1.name} keeps them alive")
            hero1.role == HeroRole.DAMAGE && hero2.role == HeroRole.CONTROL ->
                reasons.add("${hero2.name} locks enemies down so ${hero1.name} can burst them")
            hero1.role == HeroRole.CONTROL && hero2.role == HeroRole.DAMAGE ->
                reasons.add("${hero1.name} locks enemies down so ${hero2.name} can burst them")
            hero1.role == HeroRole.TANK && hero2.role == HeroRole.DAMAGE ->
                reasons.add("${hero1.name} draws aggro while ${hero2.name} deals damage from safety")
            hero1.role == HeroRole.DAMAGE && hero2.role == HeroRole.TANK ->
                reasons.add("${hero2.name} draws aggro while ${hero1.name} deals damage from safety")
            hero1.role == hero2.role && hero1.role == HeroRole.TANK ->
                reasons.add("Double tank = nearly unkillable frontline")
            hero1.role == hero2.role && hero1.role == HeroRole.DAMAGE ->
                reasons.add("Double DPS = massive burst damage potential")
            else -> reasons.add("Balanced role coverage")
        }

        if (hero1.faction == hero2.faction) {
            reasons.add("Same faction (${hero1.faction}) = faction bonuses stack")
        }

        val dbHero1 = HeroDatabase.findHeroByName(hero1.heroId)
        val dbHero2 = HeroDatabase.findHeroByName(hero2.heroId)

        if (dbHero1?.synergies?.contains(hero2.heroId) == true) {
            reasons.add("${hero1.name} has built-in synergy with ${hero2.name}")
        }
        if (dbHero2?.synergies?.contains(hero1.heroId) == true) {
            reasons.add("${hero2.name} has built-in synergy with ${hero1.name}")
        }

        val maxPower = maxOf(hero1.power, hero2.power)
        val minPower = minOf(hero1.power, hero2.power)
        val ratio = if (maxPower > 0) minPower.toDouble() / maxPower else 0.0
        if (ratio > 0.8) {
            reasons.add("Well-balanced power levels (ratio: ${(ratio * 100).toInt()}%)")
        } else {
            reasons.add("⚠️ Power gap is large — upgrade the weaker hero")
        }

        return reasons.joinToString("\n• ", "• ")
    }

    private fun roleSynergy(role1: HeroRole, role2: HeroRole): Double {
        return when {
            (role1 == HeroRole.TANK && role2 == HeroRole.SUPPORT) ||
            (role1 == HeroRole.SUPPORT && role2 == HeroRole.TANK) -> 30.0
            (role1 == HeroRole.TANK && role2 == HeroRole.DAMAGE) ||
            (role1 == HeroRole.DAMAGE && role2 == HeroRole.TANK) -> 28.0
            (role1 == HeroRole.CONTROL && role2 == HeroRole.DAMAGE) ||
            (role1 == HeroRole.DAMAGE && role2 == HeroRole.CONTROL) -> 27.0
            (role1 == HeroRole.SUPPORT && role2 == HeroRole.DAMAGE) ||
            (role1 == HeroRole.DAMAGE && role2 == HeroRole.SUPPORT) -> 25.0
            role1 == role2 && role1 == HeroRole.TANK -> 22.0
            role1 == role2 && role1 == HeroRole.DAMAGE -> 20.0
            role1 == role2 && role1 == HeroRole.SUPPORT -> 18.0
            role1 == role2 && role1 == HeroRole.CONTROL -> 15.0
            role1 == role2 && role1 == HeroRole.ASSASSIN -> 16.0
            (role1 == HeroRole.TANK && role2 == HeroRole.ASSASSIN) ||
            (role1 == HeroRole.ASSASSIN && role2 == HeroRole.TANK) -> 24.0
            (role1 == HeroRole.SUPPORT && role2 == HeroRole.CONTROL) ||
            (role1 == HeroRole.CONTROL && role2 == HeroRole.SUPPORT) -> 20.0
            else -> 15.0
        }
    }

    private fun factionSynergy(f1: Faction, f2: Faction): Double {
        return if (f1 == f2) 20.0 else 8.0
    }

    private fun databaseSynergy(h1: CapturedHero, h2: CapturedHero): Double {
        val db1 = HeroDatabase.findHeroByName(h1.heroId)
        val db2 = HeroDatabase.findHeroByName(h2.heroId)
        var score = 0.0
        if (db1?.synergies?.contains(h2.heroId) == true) score += 15.0
        if (db2?.synergies?.contains(h1.heroId) == true) score += 15.0
        if (db1?.counters?.contains(h2.heroId) == true) score -= 10.0
        if (db2?.counters?.contains(h1.heroId) == true) score -= 10.0
        if (db1?.weakAgainst?.any { db2?.counters?.contains(it) == true } == true) score += 10.0
        if (db2?.weakAgainst?.any { db1?.counters?.contains(it) == true } == true) score += 10.0
        return score.coerceIn(0.0, 25.0)
    }

    private fun statComplement(h1: CapturedHero, h2: CapturedHero): Double {
        var score = 0.0
        if (h1.stats.hp > h2.stats.hp * 2 && h2.stats.attack > h1.stats.attack) score += 8.0
        if (h2.stats.hp > h1.stats.hp * 2 && h1.stats.attack > h2.stats.attack) score += 8.0
        if (h1.stats.defense > h2.stats.defense && h2.stats.speed > h1.stats.speed) score += 7.0
        return score.coerceIn(0.0, 15.0)
    }

    private fun powerBalance(h1: CapturedHero, h2: CapturedHero): Double {
        val maxPower = maxOf(h1.power, h2.power)
        val minPower = minOf(h1.power, h2.power)
        val ratio = if (maxPower > 0) minPower.toDouble() / maxPower else 0.0
        return (ratio * 10).coerceIn(0.0, 10.0)
    }
}
