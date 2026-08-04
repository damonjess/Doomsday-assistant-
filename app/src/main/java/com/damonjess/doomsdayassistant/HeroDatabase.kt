package com.damonjess.doomsdayassistant

object HeroDatabase {

    private val heroes = listOf(
        Hero(
            id = "miyamoto_doichi",
            name = "Miyamoto Doichi",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.TANK,
            faction = Faction.WANDERERS,
            skills = listOf(
                HeroSkill("Blade Storm", "AOE damage to surrounding enemies", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Defensive Posture", "Infantry DMG +9%, counterattacks reduce enemy HP by 19%", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Last Stand", "Survives fatal blow once per battle", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Warrior's Path", "Permanently gains ATK and DEF after each victory", HeroSkill.SkillType.AWAKENED, 3)
            ),
            baseStats = HeroStats(5229, 36815, 488115, 95, 0.05f, 1.5f),
            synergies = listOf("norah", "ammara", "park_dong_wook"),
            counters = listOf("ghost", "shadow"),
            weakAgainst = listOf("witch", "reaper")
        ),
        Hero(
            id = "norah",
            name = "Norah",
            rarity = HeroRarity.EPIC,
            role = HeroRole.SUPPORT,
            faction = Faction.SURVIVORS,
            skills = listOf(
                HeroSkill("Healing Wave", "Restores HP to all allies", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Inspire", "Boosts ally ATK by 20%", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Revive", "Brings back fallen ally at 40% HP", HeroSkill.SkillType.ULTIMATE, 1)
            ),
            baseStats = HeroStats(3200, 2500, 350000, 105, 0.08f, 1.4f),
            synergies = listOf("miyamoto_doichi", "lynne", "chef"),
            counters = listOf("reaper", "witch"),
            weakAgainst = listOf("ghost", "shadow")
        ),
        Hero(
            id = "ammara",
            name = "Ammara",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.DAMAGE,
            faction = Faction.OUTLAWS,
            skills = listOf(
                HeroSkill("Sniper Shot", "Highest ATK single-target damage", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Eagle Eye", "Ignores 30% of target DEF", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Headshot", "Critical hit chance +80%", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Spotter", "Reveals hidden enemies", HeroSkill.SkillType.AWAKENED, 3)
            ),
            baseStats = HeroStats(6800, 2200, 280000, 115, 0.35f, 2.1f),
            synergies = listOf("jeb", "miyamoto_doichi", "park_dong_wook"),
            counters = listOf("ghost", "shadow"),
            weakAgainst = listOf("miyamoto_doichi", "sarge")
        ),
        Hero(
            id = "park_dong_wook",
            name = "Park Dong-wook",
            rarity = HeroRarity.EPIC,
            role = HeroRole.CONTROL,
            faction = Faction.BROTHERHOOD,
            skills = listOf(
                HeroSkill("Flashbang", "Stuns enemies in AOE", HeroSkill.SkillType.ACTIVE, 2),
                HeroSkill("Tactical", "Reduces enemy speed by 25%", HeroSkill.SkillType.PASSIVE, 3),
                HeroSkill("Air Strike", "Delayed massive AOE damage", HeroSkill.SkillType.ULTIMATE, 1)
            ),
            baseStats = HeroStats(4500, 3500, 320000, 100, 0.12f, 1.5f),
            synergies = listOf("miyamoto_doichi", "ammara", "ash"),
            counters = listOf("witch", "ghost"),
            weakAgainst = listOf("reaper", "shadow")
        ),
        Hero(
            id = "lynne",
            name = "Lynne",
            rarity = HeroRarity.RARE,
            role = HeroRole.SUPPORT,
            faction = Faction.SCIENTISTS,
            skills = listOf(
                HeroSkill("Nano Repair", "Single target heal + shield", HeroSkill.SkillType.ACTIVE, 2),
                HeroSkill("Vaccine", "Prevents next debuff on ally", HeroSkill.SkillType.PASSIVE, 3),
                HeroSkill("Emergency Protocol", "All allies immune for 1.5s", HeroSkill.SkillType.ULTIMATE, 1)
            ),
            baseStats = HeroStats(2800, 3000, 290000, 98, 0.06f, 1.3f),
            synergies = listOf("norah", "doc", "miyamoto_doichi"),
            counters = listOf("reaper", "witch"),
            weakAgainst = listOf("ghost", "shadow")
        ),
        Hero(
            id = "maddie",
            name = "Maddie",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.TANK,
            faction = Faction.WANDERERS,
            skills = listOf(
                HeroSkill("Shield Bash", "Stuns frontline enemies", HeroSkill.SkillType.ACTIVE, 2),
                HeroSkill("Iron Will", "Reduces damage taken by 30%", HeroSkill.SkillType.PASSIVE, 3),
                HeroSkill("Fortress", "Creates damage-absorbing shield for team", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Unbreakable", "Revives once per battle at 50% HP", HeroSkill.SkillType.AWAKENED, 4)
            ),
            baseStats = HeroStats(850, 1200, 15000, 95, 0.05f, 1.5f),
            synergies = listOf("nikola", "jeb", "ash"),
            counters = listOf("ghost", "reaper"),
            weakAgainst = listOf("sarge", "chef")
        ),
        Hero(
            id = "sarge",
            name = "Sarge",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.TANK,
            faction = Faction.BROTHERHOOD,
            skills = listOf(
                HeroSkill("War Cry", "Taunts all enemies", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Veteran", "Gains defense as HP drops", HeroSkill.SkillType.PASSIVE, 3),
                HeroSkill("Last Stand", "Immune to damage for 3s", HeroSkill.SkillType.ULTIMATE, 2),
                HeroSkill("Brotherhood", "Buffs nearby allies' defense", HeroSkill.SkillType.AWAKENED, 4)
            ),
            baseStats = HeroStats(780, 1350, 16500, 88, 0.03f, 1.3f),
            synergies = listOf("chef", "doc", "maddie"),
            counters = listOf("ghost", "shadow"),
            weakAgainst = listOf("reaper", "witch")
        ),
        Hero(
            id = "ghost",
            name = "Ghost",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.ASSASSIN,
            faction = Faction.OUTLAWS,
            skills = listOf(
                HeroSkill("Shadow Strike", "Teleports to weakest enemy", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Vanish", "Becomes untargetable after kill", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Death Mark", "Executes enemies below 30% HP", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Phantom", "Gains crit damage per kill", HeroSkill.SkillType.AWAKENED, 3)
            ),
            baseStats = HeroStats(1450, 450, 6200, 120, 0.35f, 2.2f),
            synergies = listOf("shadow", "reaper", "witch"),
            counters = listOf("chef", "doc", "jeb"),
            weakAgainst = listOf("maddie", "sarge")
        ),
        Hero(
            id = "reaper",
            name = "Reaper",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.DAMAGE,
            faction = Faction.OUTLAWS,
            skills = listOf(
                HeroSkill("Scythe Swing", "AOE damage to frontline", HeroSkill.SkillType.ACTIVE, 2),
                HeroSkill("Life Steal", "Heals for 20% of damage dealt", HeroSkill.SkillType.PASSIVE, 3),
                HeroSkill("Harvest", "Massive AOE execute damage", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Soul Collector", "Permanently gains ATK on kill", HeroSkill.SkillType.AWAKENED, 4)
            ),
            baseStats = HeroStats(1380, 520, 7800, 105, 0.25f, 1.8f),
            synergies = listOf("ghost", "witch", "shadow"),
            counters = listOf("maddie", "chef"),
            weakAgainst = listOf("sarge", "doc")
        ),
        Hero(
            id = "jeb",
            name = "Jeb",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.DAMAGE,
            faction = Faction.WANDERERS,
            skills = listOf(
                HeroSkill("Sniper Shot", "Highest ATK target damage", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Eagle Eye", "Ignores 40% of target defense", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Headshot", "Critical hit chance +100%", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Spotter", "Reveals invisible enemies", HeroSkill.SkillType.AWAKENED, 3)
            ),
            baseStats = HeroStats(1520, 380, 6500, 110, 0.40f, 2.0f),
            synergies = listOf("maddie", "ash", "doc"),
            counters = listOf("ghost", "shadow"),
            weakAgainst = listOf("sarge", "maddie")
        ),
        Hero(
            id = "chef",
            name = "Chef",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.SUPPORT,
            faction = Faction.SURVIVORS,
            skills = listOf(
                HeroSkill("Healing Stew", "AOE heal over time", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Food Buff", "Increases team ATK by 25%", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Feast", "Full team heal + cleanse debuffs", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Second Serving", "Revives fallen ally at 30% HP", HeroSkill.SkillType.AWAKENED, 3)
            ),
            baseStats = HeroStats(620, 680, 9500, 92, 0.08f, 1.4f),
            synergies = listOf("sarge", "doc", "maddie"),
            counters = listOf("witch", "reaper"),
            weakAgainst = listOf("ghost", "shadow")
        ),
        Hero(
            id = "doc",
            name = "Doc",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.SUPPORT,
            faction = Faction.SCIENTISTS,
            skills = listOf(
                HeroSkill("Nano Repair", "Single target massive heal", HeroSkill.SkillType.ACTIVE, 2),
                HeroSkill("Vaccine", "Prevents next debuff", HeroSkill.SkillType.PASSIVE, 3),
                HeroSkill("Emergency Protocol", "All allies immune for 2s", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Overclock", "Boosts target's speed by 50%", HeroSkill.SkillType.AWAKENED, 4)
            ),
            baseStats = HeroStats(580, 720, 8800, 98, 0.06f, 1.3f),
            synergies = listOf("jeb", "sarge", "nikola"),
            counters = listOf("reaper", "witch"),
            weakAgainst = listOf("ghost", "shadow")
        ),
        Hero(
            id = "witch",
            name = "Witch",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.CONTROL,
            faction = Faction.SCIENTISTS,
            skills = listOf(
                HeroSkill("Mind Control", "Charms enemy for 3s", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Hex", "Reduces enemy healing by 50%", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Mass Hysteria", "All enemies attack each other", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Curse", "Target takes 30% more damage", HeroSkill.SkillType.AWAKENED, 3)
            ),
            baseStats = HeroStats(920, 580, 7200, 102, 0.15f, 1.6f),
            synergies = listOf("reaper", "ghost", "shadow"),
            counters = listOf("chef", "doc"),
            weakAgainst = listOf("jeb", "ash")
        ),
        Hero(
            id = "ash",
            name = "Ash",
            rarity = HeroRarity.LEGENDARY,
            role = HeroRole.CONTROL,
            faction = Faction.BROTHERHOOD,
            skills = listOf(
                HeroSkill("Flashbang", "Stuns enemies in AOE", HeroSkill.SkillType.ACTIVE, 2),
                HeroSkill("Tactical", "Reduces enemy speed by 30%", HeroSkill.SkillType.PASSIVE, 3),
                HeroSkill("Air Strike", "Massive delayed AOE damage", HeroSkill.SkillType.ULTIMATE, 1),
                HeroSkill("Smoke Screen", "Allies gain evasion", HeroSkill.SkillType.AWAKENED, 4)
            ),
            baseStats = HeroStats(880, 620, 8000, 100, 0.12f, 1.5f),
            synergies = listOf("jeb", "maddie", "sarge"),
            counters = listOf("witch", "ghost"),
            weakAgainst = listOf("reaper", "shadow")
        ),
        Hero(
            id = "nikola",
            name = "Nikola",
            rarity = HeroRarity.EPIC,
            role = HeroRole.DAMAGE,
            faction = Faction.SCIENTISTS,
            skills = listOf(
                HeroSkill("Tesla Coil", "Chain lightning damage", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Conductive", "Damage spreads to nearby enemies", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Overload", "Massive AOE lightning strike", HeroSkill.SkillType.ULTIMATE, 1)
            ),
            baseStats = HeroStats(1150, 420, 6800, 108, 0.20f, 1.7f),
            synergies = listOf("doc", "maddie", "chef"),
            counters = listOf("ghost", "shadow"),
            weakAgainst = listOf("sarge", "maddie")
        ),
        Hero(
            id = "shadow",
            name = "Shadow",
            rarity = HeroRarity.EPIC,
            role = HeroRole.ASSASSIN,
            faction = Faction.OUTLAWS,
            skills = listOf(
                HeroSkill("Backstab", "Bonus damage from behind", HeroSkill.SkillType.ACTIVE, 1),
                HeroSkill("Smoke Bomb", "Becomes invisible for 2s", HeroSkill.SkillType.PASSIVE, 2),
                HeroSkill("Assassinate", "Instant kill if target < 25% HP", HeroSkill.SkillType.ULTIMATE, 1)
            ),
            baseStats = HeroStats(1280, 380, 5800, 115, 0.30f, 2.0f),
            synergies = listOf("ghost", "witch", "reaper"),
            counters = listOf("chef", "doc"),
            weakAgainst = listOf("jeb", "ash")
        )
    )

    fun findHeroByName(name: String): Hero? {
        return heroes.find { 
            it.name.equals(name, ignoreCase = true) || 
            it.id.equals(name, ignoreCase = true) ||
            name.contains(it.name, ignoreCase = true)
        }
    }

    fun findHeroesByRole(role: HeroRole): List<Hero> = heroes.filter { it.role == role }
    fun getAllHeroes(): List<Hero> = heroes

    data class ArenaComposition(
        val name: String,
        val heroes: List<String>,
        val strategy: String,
        val strength: String,
        val weakness: String
    )

    fun getArenaCompositions(): List<ArenaComposition> = listOf(
        ArenaComposition(
            name = "The Unkillable Wall",
            heroes = listOf("miyamoto_doichi", "maddie", "chef", "doc", "norah"),
            strategy = "Double tank + double healer. Outlasts any team with Miyamoto and Maddie frontline.",
            strength = "Extremely durable, hard to burst down",
            weakness = "Low damage, struggles against time-limits"
        ),
        ArenaComposition(
            name = "Assassin Blitz",
            heroes = listOf("ghost", "shadow", "witch", "reaper", "chef"),
            strategy = "Eliminate enemy backline before they act. Ghost + Shadow dive, Witch CCs.",
            strength = "Devastating against squishy teams",
            weakness = "Folds to tanky teams with strong heals"
        ),
        ArenaComposition(
            name = "Balanced Core",
            heroes = listOf("miyamoto_doichi", "ammara", "chef", "witch", "reaper"),
            strategy = "One of each role for maximum flexibility. Miyamoto tanks, Ammara snipes.",
            strength = "Adapts to any enemy composition",
            weakness = "No extreme strengths to exploit"
        ),
        ArenaComposition(
            name = "Control & Burst",
            heroes = listOf("miyamoto_doichi", "ash", "witch", "ammara", "doc"),
            strategy = "Lock down enemies while Ammara snipes. Ash + Witch CC chain.",
            strength = "Picks off key targets quickly",
            weakness = "If Ammara dies, damage drops severely"
        ),
        ArenaComposition(
            name = "Infantry Core",
            heroes = listOf("miyamoto_doichi", "sarge", "park_dong_wook", "ash", "chef"),
            strategy = "Full infantry synergy. Massive frontline with Miyamoto and Sarge.",
            strength = "Devastating infantry bonuses stack",
            weakness = "Weak to rider-focused teams"
        ),
        ArenaComposition(
            name = "AOE Destruction",
            heroes = listOf("miyamoto_doichi", "reaper", "nikola", "ash", "chef"),
            strategy = "Wipe entire enemy team simultaneously with stacked AOE.",
            strength = "Devastating against clustered enemies",
            weakness = "Spreads damage too thin vs tanks"
        )
    )
}
