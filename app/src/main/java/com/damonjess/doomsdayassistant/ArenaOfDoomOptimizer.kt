package com.damonjess.doomsdayassistant

import kotlin.math.min

class ArenaOfDoomOptimizer {

    companion object {
        const val MAX_TEAM_CANDIDATES = 100
        const val MAX_RESULTS = 5
    }

    fun optimize(roster: List<CapturedHero>): ArenaOptimizationResult {
        if (roster.isEmpty()) {
            return createEmptyResult()
        }

        val candidates = roster.sortedByDescending { it.combatScore() }
            .take(min(roster.size, MAX_TEAM_CANDIDATES))

        val teams = generateTeams(candidates)
        val scoredTeams = teams.map { scoreTeam(it) }
            .sortedByDescending { it.teamScore }

        val bestTeam = scoredTeams.firstOrNull() ?: createEmptyTeam()
        val bestPair = bestTeam.bestPair()

        val pairAnalysis = if (bestPair != null) {
            PairingEngine.analyzePair(bestPair.first, bestPair.second)
        } else {
            "Not enough heroes for pairing analysis"
        }

        return ArenaOptimizationResult(
            savedRoster = roster,
            topTeams = scoredTeams.take(MAX_RESULTS),
            bestOverallTeam = bestTeam,
            bestPair = bestPair ?: Pair(
                CapturedHero("", "", "", HeroRarity.COMMON, HeroRole.TANK, Faction.WANDERERS, 0, 0, 0, HeroStats(0,0,0,0,0f,0f), emptyList()),
                CapturedHero("", "", "", HeroRarity.COMMON, HeroRole.TANK, Faction.WANDERERS, 0, 0, 0, HeroStats(0,0,0,0,0f,0f), emptyList())
            ),
            pairAnalysis = pairAnalysis,
            roleCoverage = countRoles(bestTeam.allHeroes()),
            factionBonus = countFactions(bestTeam.allHeroes())
        )
    }

    private fun generateTeams(candidates: List<CapturedHero>): List<ArenaTeam> {
        val teams = mutableListOf<ArenaTeam>()
        val n = min(candidates.size, 15)
        val subset = candidates.take(n)

        generateSoloTeams(subset, teams)
        generateOnePairTeams(subset, teams)
        generateTwoPairTeams(subset, teams)
        generateMixedTeams(subset, teams)

        return teams.distinctBy { it.allHeroes().map { h -> h.id }.sorted() }
    }

    private fun generateSoloTeams(heroes: List<CapturedHero>, teams: MutableList<ArenaTeam>) {
        val combos = combinations(heroes, 5)
        combos.forEach { combo ->
            if (isValidTeam(combo)) {
                teams.add(ArenaTeam(
                    tile1 = TileAssignment(1, listOf(combo[0])),
                    tile2 = TileAssignment(2, listOf(combo[1])),
                    tile3 = TileAssignment(3, listOf(combo[2])),
                    tile4 = TileAssignment(4, listOf(combo[3])),
                    tile5 = TileAssignment(5, listOf(combo[4])),
                    teamScore = 0.0, synergyScore = 0.0, coverageScore = 0.0, pairScore = 0.0
                ))
            }
        }
    }

    private fun generateOnePairTeams(heroes: List<CapturedHero>, teams: MutableList<ArenaTeam>) {
        val pairs = generatePairs(heroes)
        pairs.forEach { pair ->
            val remaining = heroes.filter { it.id != pair.first.id && it.id != pair.second.id }
            val soloCombos = combinations(remaining, 3)
            soloCombos.forEach { solos ->
                val allHeroes = listOf(pair.first, pair.second) + solos
                if (isValidTeam(allHeroes)) {
                    teams.add(ArenaTeam(
                        tile1 = TileAssignment(1, listOf(pair.first, pair.second), true),
                        tile2 = TileAssignment(2, listOf(solos[0])),
                        tile3 = TileAssignment(3, listOf(solos[1])),
                        tile4 = TileAssignment(4, listOf(solos[2])),
                        tile5 = TileAssignment(5, emptyList()),
                        teamScore = 0.0, synergyScore = 0.0, coverageScore = 0.0, pairScore = 0.0
                    ))
                }
            }
        }
    }

    private fun generateTwoPairTeams(heroes: List<CapturedHero>, teams: MutableList<ArenaTeam>) {
        val pairs = generatePairs(heroes)
        pairs.forEachIndexed { i, pair1 ->
            pairs.drop(i + 1).forEach { pair2 ->
                val pair1Ids = pair1.toList().map { it.id }
                val pair2Ids = pair2.toList().map { it.id }
                if (pair1Ids.intersect(pair2Ids).isNotEmpty()) return@forEach
                val remaining = heroes.filter { it.id !in pair1Ids && it.id !in pair2Ids }
                if (remaining.isNotEmpty()) {
                    val solo = remaining.first()
                    val allHeroes = pair1.toList() + pair2.toList() + listOf(solo)
                    if (isValidTeam(allHeroes)) {
                        teams.add(ArenaTeam(
                            tile1 = TileAssignment(1, listOf(pair1.first, pair1.second), true),
                            tile2 = TileAssignment(2, listOf(pair2.first, pair2.second), true),
                            tile3 = TileAssignment(3, listOf(solo)),
                            tile4 = TileAssignment(4, emptyList()),
                            tile5 = TileAssignment(5, emptyList()),
                            teamScore = 0.0, synergyScore = 0.0, coverageScore = 0.0, pairScore = 0.0
                        ))
                    }
                }
            }
        }
    }

    private fun generateMixedTeams(heroes: List<CapturedHero>, teams: MutableList<ArenaTeam>) {
        val pairs = generatePairs(heroes).take(20)
        pairs.forEach { pair ->
            val remaining = heroes.filter { it.id != pair.first.id && it.id != pair.second.id }
            val solos = remaining.take(3)
            if (solos.size >= 3) {
                val allHeroes = listOf(pair.first, pair.second) + solos
                if (isValidTeam(allHeroes)) {
                    teams.add(ArenaTeam(
                        tile1 = TileAssignment(1, listOf(solos[0])),
                        tile2 = TileAssignment(2, listOf(solos[1])),
                        tile3 = TileAssignment(3, listOf(pair.first, pair.second), true),
                        tile4 = TileAssignment(4, listOf(solos[2])),
                        tile5 = TileAssignment(5, emptyList()),
                        teamScore = 0.0, synergyScore = 0.0, coverageScore = 0.0, pairScore = 0.0
                    ))
                }
            }
        }
    }

    private fun generatePairs(heroes: List<CapturedHero>): List<Pair<CapturedHero, CapturedHero>> {
        val pairs = mutableListOf<Pair<CapturedHero, CapturedHero>>()
        for (i in heroes.indices) {
            for (j in i + 1 until heroes.size) {
                pairs.add(Pair(heroes[i], heroes[j]))
            }
        }
        return pairs.sortedByDescending { PairingEngine.calculatePairScore(it.first, it.second) }
    }

    private fun isValidTeam(heroes: List<CapturedHero>): Boolean {
        val roles = heroes.map { it.role }
        val hasTankOrSupport = roles.any { it == HeroRole.TANK || it == HeroRole.SUPPORT }
        if (!hasTankOrSupport) return false
        val hasDamage = roles.any { it == HeroRole.DAMAGE || it == HeroRole.ASSASSIN }
        if (!hasDamage) return false
        val roleCounts = roles.groupingBy { it }.eachCount()
        if (roleCounts.values.any { it > 3 }) return false
        return true
    }

    private fun scoreTeam(team: ArenaTeam): ArenaTeam {
        val heroes = team.allHeroes()
        val synergyScore = calculateTeamSynergy(heroes)
        val coverageScore = calculateCoverageScore(heroes)
        val pairScore = team.bestPair()?.let {
            PairingEngine.calculatePairScore(it.first, it.second)
        } ?: 0.0
        val powerScore = heroes.sumOf { it.combatScore() } * 0.01
        val factionScore = calculateFactionBonus(heroes)
        val totalScore = synergyScore * 0.3 + coverageScore * 0.25 +
                pairScore * 0.25 + powerScore * 0.1 + factionScore * 0.1

        return team.copy(
            teamScore = totalScore,
            synergyScore = synergyScore,
            coverageScore = coverageScore,
            pairScore = pairScore
        )
    }

    private fun calculateTeamSynergy(heroes: List<CapturedHero>): Double {
        var total = 0.0
        var count = 0
        for (i in heroes.indices) {
            for (j in i + 1 until heroes.size) {
                total += PairingEngine.calculatePairScore(heroes[i], heroes[j])
                count++
            }
        }
        return if (count > 0) total / count else 0.0
    }

    private fun calculateCoverageScore(heroes: List<CapturedHero>): Double {
        val roles = heroes.map { it.role }.distinct()
        val score = when (roles.size) {
            5 -> 100.0
            4 -> 85.0
            3 -> 70.0
            2 -> 50.0
            else -> 30.0
        }
        val hasCore = heroes.any { it.role == HeroRole.TANK } &&
                heroes.any { it.role == HeroRole.SUPPORT || it.role == HeroRole.CONTROL } &&
                heroes.any { it.role == HeroRole.DAMAGE || it.role == HeroRole.ASSASSIN }
        return if (hasCore) score + 10 else score
    }

    private fun calculateFactionBonus(heroes: List<CapturedHero>): Double {
        val factionCounts = heroes.groupingBy { it.faction }.eachCount()
        val maxSameFaction = factionCounts.values.maxOrNull() ?: 0
        return when {
            maxSameFaction >= 4 -> 100.0
            maxSameFaction == 3 -> 75.0
            maxSameFaction == 2 -> 50.0
            else -> 25.0
        }
    }

    private fun countRoles(heroes: List<CapturedHero>): Map<HeroRole, Int> {
        return heroes.groupingBy { it.role }.eachCount()
    }

    private fun countFactions(heroes: List<CapturedHero>): Map<Faction, Int> {
        return heroes.groupingBy { it.faction }.eachCount()
    }

    private fun <T> combinations(list: List<T>, k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (list.size < k) return emptyList()
        if (k == 1) return list.map { listOf(it) }
        val result = mutableListOf<List<T>>()
        for (i in 0..list.size - k) {
            val head = list[i]
            val tailCombos = combinations(list.subList(i + 1, list.size), k - 1)
            tailCombos.forEach { result.add(listOf(head) + it) }
        }
        return result
    }

    private fun createEmptyResult(): ArenaOptimizationResult {
        val dummy = CapturedHero("", "", "", HeroRarity.COMMON, HeroRole.TANK, Faction.WANDERERS, 0, 0, 0, HeroStats(0,0,0,0,0f,0f), emptyList())
        return ArenaOptimizationResult(
            savedRoster = emptyList(),
            topTeams = emptyList(),
            bestOverallTeam = createEmptyTeam(),
            bestPair = Pair(dummy, dummy),
            pairAnalysis = "No heroes captured yet. Go to a hero screen and tap the floating button to save heroes to your Arena of Doom roster.",
            roleCoverage = emptyMap(),
            factionBonus = emptyMap()
        )
    }

    private fun createEmptyTeam(): ArenaTeam {
        val empty = CapturedHero("", "", "", HeroRarity.COMMON, HeroRole.TANK, Faction.WANDERERS, 0, 0, 0, HeroStats(0,0,0,0,0f,0f), emptyList())
        return ArenaTeam(
            tile1 = TileAssignment(1, listOf(empty)),
            tile2 = TileAssignment(2, listOf(empty)),
            tile3 = TileAssignment(3, listOf(empty)),
            tile4 = TileAssignment(4, listOf(empty)),
            tile5 = TileAssignment(5, listOf(empty)),
            teamScore = 0.0, synergyScore = 0.0, coverageScore = 0.0, pairScore = 0.0
        )
    }
}
