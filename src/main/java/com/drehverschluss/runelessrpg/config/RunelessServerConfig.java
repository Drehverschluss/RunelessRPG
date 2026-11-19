package com.drehverschluss.runelessrpg.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Server-side configuration ({@code config/runelessrpg-server.toml}).
 * The cost-related values are mirrored to clients by {@link com.drehverschluss.runelessrpg.cost.CostSettings}.
 */
public final class RunelessServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue BASE_MAX_MANA;
    public static final ModConfigSpec.DoubleValue BASE_MANA_REGEN;

    public static final ModConfigSpec.EnumValue<ManaMode> MANA_MODE;
    public static final ModConfigSpec.DoubleValue MAX_DEBT_PERCENT;
    public static final ModConfigSpec.BooleanValue CREATIVE_FREE_CASTING;
    public static final ModConfigSpec.DoubleValue MIN_MANA_COST_PERCENT;

    public static final ModConfigSpec.ConfigValue<List<? extends Number>> TIER_COSTS;
    public static final ModConfigSpec.BooleanValue SPELLS_ENABLED;
    public static final ModConfigSpec.DoubleValue SPELLS_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue CLASS_SKILLS_ENABLED;
    public static final ModConfigSpec.DoubleValue CLASS_SKILLS_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue WEAPON_SKILLS_ENABLED;
    public static final ModConfigSpec.DoubleValue WEAPON_SKILLS_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ITEM_ABILITIES_ENABLED;
    public static final ModConfigSpec.DoubleValue ITEM_ABILITIES_MULTIPLIER;

    public static final ModConfigSpec.EnumValue<ChannelCostMode> CHANNEL_COST_MODE;
    public static final ModConfigSpec.DoubleValue CHANNEL_START_PERCENT;
    public static final ModConfigSpec.BooleanValue COOLDOWN_ON_CHANNEL_ABORT;

    public static final ModConfigSpec.DoubleValue OVERCHARGE_DAMAGE_FACTOR;
    public static final ModConfigSpec.BooleanValue OVERCHARGE_AFFECTS_HEALING;
    public static final ModConfigSpec.BooleanValue OVERCHARGE_AFFECTS_SUMMONS;

    public static final ModConfigSpec.DoubleValue MANA_LEECH_EXTRA_TARGET_PERCENT;
    public static final ModConfigSpec.DoubleValue MANA_LEECH_MAX_PER_SECOND;
    public static final ModConfigSpec.DoubleValue SPELL_MANA_LEECH_EXTRA_TARGET_PERCENT;
    public static final ModConfigSpec.BooleanValue LEECH_CAP_AT_TARGET_HEALTH;
    public static final ModConfigSpec.BooleanValue MANA_LEECH_ENCHANTMENT_ENABLED;
    public static final ModConfigSpec.BooleanValue SPELL_MANA_LEECH_ENCHANTMENT_ENABLED;

    public static final ModConfigSpec.EnumValue<SpellInfinityMode> SPELL_INFINITY_MODE;
    public static final ModConfigSpec.DoubleValue SPELL_INFINITY_COST_REDUCTION;
    public static final ModConfigSpec.DoubleValue SPELL_INFINITY_FLOOR_REDUCTION;

    public static final ModConfigSpec.DoubleValue MANA_POTION_PERCENT_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue LINGERING_MANA_PERCENT_PER_LEVEL;
    public static final ModConfigSpec.IntValue MANA_SICKNESS_SECONDS;
    public static final ModConfigSpec.DoubleValue MANA_SICKNESS_RESTORE_PERCENT;
    public static final ModConfigSpec.DoubleValue MAGICAL_STABILITY_MANA_COST;
    public static final ModConfigSpec.IntValue MAGICAL_STABILITY_DURATION;
    public static final ModConfigSpec.IntValue LONG_MAGICAL_STABILITY_DURATION;
    public static final ModConfigSpec.IntValue BORROWED_POWER_DURATION;
    public static final ModConfigSpec.DoubleValue BORROWED_POWER_DEBT_PERCENT;
    public static final ModConfigSpec.EnumValue<BacklashMode> BACKLASH_MODE;
    public static final ModConfigSpec.DoubleValue BACKLASH_HEALTH_PERCENT_PER_10_PERCENT;
    public static final ModConfigSpec.DoubleValue BACKLASH_MAX_HEALTH_PERCENT;
    public static final ModConfigSpec.DoubleValue BACKLASH_DAMAGE_PER_10_PERCENT;
    public static final ModConfigSpec.DoubleValue BACKLASH_MAX_DAMAGE;
    public static final ModConfigSpec.IntValue MANA_HARVEST_DURATION;
    public static final ModConfigSpec.IntValue LONG_MANA_HARVEST_DURATION;
    public static final ModConfigSpec.IntValue STRONG_MANA_HARVEST_DURATION;

    public static final ModConfigSpec.BooleanValue MANA_ORBS_ENABLED;
    public static final ModConfigSpec.DoubleValue ORB_MANA_PERCENT;
    public static final ModConfigSpec.IntValue ORB_LIFETIME_SECONDS;
    public static final ModConfigSpec.IntValue MAX_ORBS_PER_STACK;
    public static final ModConfigSpec.DoubleValue KILL_BASE_CHANCE_PERCENT;
    public static final ModConfigSpec.DoubleValue KILL_CHANCE_PERCENT_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue EXTRA_KILL_PERCENT;
    public static final ModConfigSpec.DoubleValue KILL_COOLDOWN_SECONDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> KILL_MOB_CATEGORIES;
    public static final ModConfigSpec.IntValue BOSS_KILL_ORBS;
    public static final ModConfigSpec.DoubleValue BOSS_HIT_BASE_CHANCE_PERCENT;
    public static final ModConfigSpec.DoubleValue BOSS_HIT_CHANCE_PERCENT_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue BOSS_HIT_COOLDOWN_SECONDS;
    public static final ModConfigSpec.DoubleValue BOSS_ORB_MIN_DISTANCE;
    public static final ModConfigSpec.DoubleValue BOSS_ORB_MAX_DISTANCE;

    /** Effect of the enchantment spell_engine:spell_infinity on mana costs. */
    public enum SpellInfinityMode {
        /** Lowers the cost factor and its floor. */
        REDUCE_COST,
        /** No effect on mana costs. */
        NONE
    }

    /** How the Borrowed Power backlash damage is calculated. */
    public enum BacklashMode {
        /** Percent of the player's max health per 10% debt, with a maximum in percent of max health. */
        PERCENT_MAX_HEALTH,
        /** Fixed damage per 10% debt, with a fixed maximum. */
        FLAT
    }

    /** What happens when a player has less mana than a spell costs. */
    public enum ManaMode {
        /** Cast only if the mana covers the whole cost. */
        FULL_COST,
        /** Cast while mana is above 0; the rest goes into debt. */
        DEBT,
        /** Debt allowed up to {@code maxDebtPercent} of the spell's cost. */
        DEBT_LIMIT
    }

    /** How channeled spells are paid. */
    public enum ChannelCostMode {
        /** The cost is spread over the channel ticks. */
        DRAIN,
        /** The full cost is paid when the channel starts. */
        START,
        /** Spell Engine behavior: paid once when the channel ends. */
        END
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Base mana values of every player").push("mana");
        BASE_MAX_MANA = builder
                .comment("Base value of runelessrpg:max_mana for every player")
                .defineInRange("baseMaxMana", 20.0, 0.0, 100000.0);
        BASE_MANA_REGEN = builder
                .comment("Base value of runelessrpg:mana_regen (mana per second)")
                .defineInRange("baseManaRegen", 0.5, -1000.0, 1000.0);
        builder.pop();

        builder.comment("Casting rules").push("casting");
        MANA_MODE = builder
                .comment("FULL_COST  = cast only if the mana covers the whole cost",
                        "DEBT       = cast while mana is above 0, the rest goes below 0",
                        "DEBT_LIMIT = debt allowed, at most maxDebtPercent of the spell's cost")
                .defineEnum("manaMode", ManaMode.FULL_COST);
        MAX_DEBT_PERCENT = builder
                .comment("DEBT_LIMIT: maximum negative mana after a cast, in percent of that spell's cost")
                .defineInRange("maxDebtPercent", 50.0, 0.0, 10000.0);
        CREATIVE_FREE_CASTING = builder
                .comment("true  = players in creative mode cast for free and are never blocked",
                        "false = creative players pay like survival players")
                .define("creativeFreeCasting", true);
        MIN_MANA_COST_PERCENT = builder
                .comment("Lower limit for the runelessrpg:mana_cost attribute when calculating costs (in percent)")
                .defineInRange("minManaCostPercent", 50.0, 0.0, 1000.0);
        builder.pop();

        builder.comment("Base costs and categories").push("costs");
        TIER_COSTS = builder
                .comment("Base mana cost per spell tier (index = tier). Higher tiers use the last value, negative tiers the first.")
                .defineList("tierCosts", List.of(4, 5, 7, 10, 14, 18), () -> 4,
                        value -> value instanceof Number number && number.doubleValue() >= 0.0);
        SPELLS_ENABLED = builder
                .comment("Spells that cost runes, plus everything in #runelessrpg:cost/spells")
                .define("spellsEnabled", true);
        SPELLS_MULTIPLIER = builder.comment("Multiplier for the tier cost of spells").defineInRange("spellsMultiplier", 1.0, 0.0, 1000.0);
        CLASS_SKILLS_ENABLED = builder
                .comment("Class skills in #runelessrpg:cost/class_skills")
                .define("classSkillsEnabled", true);
        CLASS_SKILLS_MULTIPLIER = builder.comment("Multiplier for the tier cost of class skills").defineInRange("classSkillsMultiplier", 0.75, 0.0, 1000.0);
        WEAPON_SKILLS_ENABLED = builder
                .comment("Weapon skills in #runelessrpg:cost/weapon_skills")
                .define("weaponSkillsEnabled", true);
        WEAPON_SKILLS_MULTIPLIER = builder.comment("Multiplier for the tier cost of weapon skills").defineInRange("weaponSkillsMultiplier", 0.5, 0.0, 1000.0);
        ITEM_ABILITIES_ENABLED = builder
                .comment("Item abilities in #runelessrpg:cost/item_abilities")
                .define("itemAbilitiesEnabled", false);
        ITEM_ABILITIES_MULTIPLIER = builder.comment("Multiplier for the tier cost of item abilities").defineInRange("itemAbilitiesMultiplier", 1.0, 0.0, 1000.0);
        builder.pop();

        builder.comment("Channeled spells").push("channel");
        CHANNEL_COST_MODE = builder
                .comment("DRAIN = the cost is spread over the channel ticks (stopping early is cheaper)",
                        "START = the full cost is paid when the channel starts",
                        "END   = Spell Engine behavior: paid once when the channel ends")
                .defineEnum("channelCostMode", ChannelCostMode.DRAIN);
        CHANNEL_START_PERCENT = builder
                .comment("DRAIN: this percentage of the total cost is paid immediately when the channel starts")
                .defineInRange("channelStartPercent", 0.0, 0.0, 100.0);
        COOLDOWN_ON_CHANNEL_ABORT = builder
                .comment("Prevents skipping a channeled spell's cooldown by interrupting it.",
                        "An interrupted channel (item switch, new spell, death, logout) gets the cooldown of releasing it at that point.")
                .define("cooldownOnChannelAbort", true);
        builder.pop();

        builder.comment("Overcharge: spells cost more and deal more damage / healing").push("overcharge");
        OVERCHARGE_DAMAGE_FACTOR = builder
                .comment("Damage / healing bonus = overcharge x this factor (0.5: +20% cost = +10% damage)")
                .defineInRange("overchargeDamageFactor", 0.5, 0.0, 10.0);
        OVERCHARGE_AFFECTS_HEALING = builder
                .comment("Overcharge also increases spell healing")
                .define("overchargeAffectsHealing", true);
        OVERCHARGE_AFFECTS_SUMMONS = builder
                .comment("Summons and spell clouds keep the overcharge bonus their spell was paid with (snapshot when created)")
                .define("overchargeAffectsSummons", true);
        builder.pop();

        builder.comment("Mana leech (weapon damage) and spell mana leech (direct spell hits)").push("mana_leech");
        MANA_LEECH_EXTRA_TARGET_PERCENT = builder
                .comment("Weapon mana leech: every further target hit in the same game tick gives only this percentage (0 = first target only)")
                .defineInRange("manaLeechExtraTargetPercent", 25.0, 0.0, 100.0);
        MANA_LEECH_MAX_PER_SECOND = builder
                .comment("Weapon mana leech: at most this much mana per second from all sources of runelessrpg:mana_leech together",
                        "(affixes, enchantment, gear). Up to one second of it can be stored for a single big hit. 0 = no limit.",
                        "Counted in game ticks (20 per second). Spell mana leech has no such limit")
                .defineInRange("manaLeechMaxPerSecond", 2.5, 0.0, 100000.0);
        SPELL_MANA_LEECH_EXTRA_TARGET_PERCENT = builder
                .comment("Spell mana leech: every further target hit in the same game tick gives only this percentage (0 = first target only)")
                .defineInRange("spellManaLeechExtraTargetPercent", 25.0, 0.0, 100.0);
        LEECH_CAP_AT_TARGET_HEALTH = builder
                .comment("Count at most the health + absorption the target had before the hit (no mana from overkill damage)")
                .define("leechCapAtTargetHealth", true);
        MANA_LEECH_ENCHANTMENT_ENABLED = builder
                .comment("Enchantment runelessrpg:mana_leech (melee and ranged weapons, loot only).",
                        "false = no effect, also on items that already have it, and it is removed from newly generated loot.",
                        "The enchantment stays on existing items and works again when turned back on. Applies immediately")
                .define("manaLeechEnchantmentEnabled", true);
        SPELL_MANA_LEECH_ENCHANTMENT_ENABLED = builder
                .comment("Enchantment runelessrpg:spell_mana_leech (spell weapons, loot only). Same rules as manaLeechEnchantmentEnabled")
                .define("spellManaLeechEnchantmentEnabled", true);
        builder.pop();

        builder.comment("Enchantment spell_engine:spell_infinity").push("spell_infinity");
        SPELL_INFINITY_MODE = builder
                .comment("REDUCE_COST = reduces the mana cost and lowers the floor of mana_cost",
                        "NONE        = no effect on mana costs (a pack may disable it by emptying #spell_engine:enchantable/spell_infinity)",
                        "Spells that cost runes (category disabled) keep Spell Engine's behavior: no runes are consumed")
                .defineEnum("mode", SpellInfinityMode.REDUCE_COST);
        SPELL_INFINITY_COST_REDUCTION = builder
                .comment("Counts as this many percent less runelessrpg:mana_cost")
                .defineInRange("costReductionPercent", 20.0, 0.0, 100.0);
        SPELL_INFINITY_FLOOR_REDUCTION = builder
                .comment("Lowers minManaCostPercent by this many points (never below 0)")
                .defineInRange("floorReductionPercent", 20.0, 0.0, 100.0);
        builder.pop();

        builder.comment("Potions. Durations also apply to potions that already exist (they are read when a potion takes effect).",
                "Brewing ingredients are in config/runelessrpg-common.toml").push("potions");
        MANA_POTION_PERCENT_PER_LEVEL = builder
                .comment("Mana restoration: (level) x this percentage of max mana (Potion of Mana I = 25%, II = 50%)",
                        "Pays debt back first, never above max mana. Splash potions restore less at the edge of the splash, like Instant Health")
                .defineInRange("manaPotionPercentPerLevel", 25.0, 0.0, 1000.0);
        LINGERING_MANA_PERCENT_PER_LEVEL = builder
                .comment("Lingering Potion of Mana: each application of the cloud (once per second per player) restores (level) x this",
                        "percentage of max mana (I = 5%, II = 10%). Pays debt back first. Lingering clouds neither cause Mana Sickness",
                        "nor are they weakened by it. The cloud shrinks with every application, so one player gets about five")
                .defineInRange("lingeringManaPercentPerLevel", 5.0, 0.0, 1000.0);
        MANA_SICKNESS_SECONDS = builder
                .comment("Every mana restoration (drunk, splash, tipped arrow; not lingering clouds) gives the effect runelessrpg:mana_sickness",
                        "for this many seconds, or renews it. Milk and totems do not remove it, death does. 0 = no Mana Sickness")
                .defineInRange("manaSicknessSeconds", 20, 0, 86400);
        MANA_SICKNESS_RESTORE_PERCENT = builder
                .comment("During Mana Sickness a mana restoration gives only this percentage of its normal amount (0 = nothing)")
                .defineInRange("manaSicknessRestorePercent", 25.0, 0.0, 100.0);
        MAGICAL_STABILITY_MANA_COST = builder
                .comment("Magical Stability: runelessrpg:mana_cost -this value per level (0.15 = -15%). The floor minManaCostPercent still applies")
                .defineInRange("magicalStabilityManaCost", 0.15, 0.0, 10.0);
        MAGICAL_STABILITY_DURATION = builder
                .comment("Potion of Magical Stability: duration in seconds")
                .defineInRange("magicalStabilityDurationSeconds", 180, 1, 86400);
        LONG_MAGICAL_STABILITY_DURATION = builder
                .comment("Long Potion of Magical Stability (redstone): duration in seconds")
                .defineInRange("longMagicalStabilityDurationSeconds", 480, 1, 86400);
        BORROWED_POWER_DURATION = builder
                .comment("Potion of Borrowed Power: duration in seconds")
                .defineInRange("borrowedPowerDurationSeconds", 30, 1, 86400);
        BORROWED_POWER_DEBT_PERCENT = builder
                .comment("Borrowed Power: spells may take mana down to -(this percentage of max mana), also in manaMode FULL_COST.",
                        "If the mana mode itself allows more (DEBT, or a larger DEBT_LIMIT), the more generous rule applies")
                .defineInRange("borrowedPowerDebtPercent", 50.0, 0.0, 1000.0);
        BACKLASH_MODE = builder
                .comment("Backlash when Borrowed Power ends (runs out, milk, /effect clear) with mana below 0.",
                        "PERCENT_MAX_HEALTH = damage in percent of the player's max health (backlashHealthPercentPer10Percent, backlashMaxHealthPercent)",
                        "FLAT               = fixed damage (backlashDamagePer10Percent, backlashMaxDamage)",
                        "Damage type runelessrpg:mana_backlash ignores armor and enchantments. The debt stays, regeneration pays it back")
                .defineEnum("backlashMode", BacklashMode.PERCENT_MAX_HEALTH);
        BACKLASH_HEALTH_PERCENT_PER_10_PERCENT = builder
                .comment("PERCENT_MAX_HEALTH: damage in percent of max health per 10% of max mana in debt")
                .defineInRange("backlashHealthPercentPer10Percent", 10.0, 0.0, 1000.0);
        BACKLASH_MAX_HEALTH_PERCENT = builder
                .comment("PERCENT_MAX_HEALTH: maximum backlash damage in percent of max health")
                .defineInRange("backlashMaxHealthPercent", 50.0, 0.0, 10000.0);
        BACKLASH_DAMAGE_PER_10_PERCENT = builder
                .comment("FLAT: damage per 10% of max mana in debt")
                .defineInRange("backlashDamagePer10Percent", 2.0, 0.0, 1000.0);
        BACKLASH_MAX_DAMAGE = builder
                .comment("FLAT: maximum backlash damage")
                .defineInRange("backlashMaxDamage", 10.0, 0.0, 100000.0);
        MANA_HARVEST_DURATION = builder
                .comment("Potion of Mana Harvest: duration in seconds")
                .defineInRange("manaHarvestDurationSeconds", 180, 1, 86400);
        LONG_MANA_HARVEST_DURATION = builder
                .comment("Long Potion of Mana Harvest (redstone): duration in seconds")
                .defineInRange("longManaHarvestDurationSeconds", 480, 1, 86400);
        STRONG_MANA_HARVEST_DURATION = builder
                .comment("Potion of Mana Harvest II (glowstone): duration in seconds")
                .defineInRange("strongManaHarvestDurationSeconds", 90, 1, 86400);
        builder.pop();

        builder.comment("Mana orbs (entity runelessrpg:mana_orb): dropped by monsters a player kills, mostly with the potion of Mana Harvest.",
                "Orbs stay on the ground (they do not fly to players); any player picks them up on touch, also with full mana.",
                "Per-mob chances and counts: data/<ns>/runelessrpg/mana_orbs/*.json. Tags: #runelessrpg:no_mana_orbs (never),",
                "#runelessrpg:mana_orb_bosses (boss rules, contains #c:bosses), damage type tag #runelessrpg:mana_orbs/ignored_damage").push("mana_orbs");
        MANA_ORBS_ENABLED = builder
                .comment("Mana orbs on/off. Off = no new orbs, orbs on the ground disappear")
                .define("enabled", true);
        ORB_MANA_PERCENT = builder
                .comment("Mana per orb in percent of the max mana of the player who picks it up. Pays debt back first.",
                        "Orbs neither cause Mana Sickness nor are they weakened by it")
                .defineInRange("orbManaPercent", 3.0, 0.0, 100.0);
        ORB_LIFETIME_SECONDS = builder
                .comment("How long an orb lies on the ground, in seconds")
                .defineInRange("orbLifetimeSeconds", 30, 1, 3600);
        MAX_ORBS_PER_STACK = builder
                .comment("At most this many orbs merge into one big orb (1 = no merging). A touch takes the whole orb at once:",
                        "count x orbManaPercent (5 x 3% = 15%). Full orbs sparkle on the client. Lowering the value keeps bigger orbs",
                        "on the ground as they are; they take no more orbs")
                .defineInRange("maxOrbsPerStack", 5, 1, 100);
        KILL_BASE_CHANCE_PERCENT = builder
                .comment("Chance per kill in percent without Mana Harvest (0 = orbs only with the potion).",
                        "Only kills that would drop experience count (a real player hit the mob, doMobLoot on); fake players never count")
                .defineInRange("killBaseChancePercent", 0.0, 0.0, 100.0);
        KILL_CHANCE_PERCENT_PER_LEVEL = builder
                .comment("Added to the kill chance per level of Mana Harvest of the killing player (I = 50%, II = 100%)")
                .defineInRange("killChancePercentPerLevel", 50.0, 0.0, 100.0);
        EXTRA_KILL_PERCENT = builder
                .comment("Further kills by the same player in the same game tick (area spells) get this percentage of the chance")
                .defineInRange("extraKillPercent", 25.0, 0.0, 100.0);
        KILL_COOLDOWN_SECONDS = builder
                .comment("After a kill dropped orbs, further kills of this player drop none for this many seconds (0 = off)")
                .defineInRange("killCooldownSeconds", 0.0, 0.0, 3600.0);
        KILL_MOB_CATEGORIES = builder
                .comment("Mob categories that can drop orbs (MONSTER, CREATURE, AMBIENT, AXOLOTLS, UNDERGROUND_WATER_CREATURE,",
                        "WATER_CREATURE, WATER_AMBIENT, MISC). Mobs with an entry in a mana_orbs data file can always drop orbs")
                .defineListAllowEmpty("killMobCategories", List.of("MONSTER"), () -> "MONSTER", value -> value instanceof String);
        BOSS_KILL_ORBS = builder
                .comment("Orbs dropped when a boss (#runelessrpg:mana_orb_bosses) is killed by a player. The kill chance does not apply to bosses")
                .defineInRange("bossKillOrbs", 0, 0, 64);
        BOSS_HIT_BASE_CHANCE_PERCENT = builder
                .comment("Chance per hit on a boss in percent without Mana Harvest (0 = only with the potion).",
                        "Every hit the player causes counts (melee, projectiles, spells, channel ticks, clouds, summons), except",
                        "damage types in #runelessrpg:mana_orbs/ignored_damage (thorns)")
                .defineInRange("bossHitBaseChancePercent", 0.0, 0.0, 100.0);
        BOSS_HIT_CHANCE_PERCENT_PER_LEVEL = builder
                .comment("Added to the boss hit chance per level of Mana Harvest (I = 10%, II = 20%)")
                .defineInRange("bossHitChancePercentPerLevel", 10.0, 0.0, 100.0);
        BOSS_HIT_COOLDOWN_SECONDS = builder
                .comment("At most one orb from boss hits per player in this many seconds (0 = no limit)")
                .defineInRange("bossHitCooldownSeconds", 3.0, 0.0, 3600.0);
        BOSS_ORB_MIN_DISTANCE = builder
                .comment("An orb from a boss hit appears on free ground this many blocks around the player (minimum)")
                .defineInRange("bossOrbMinDistance", 2.0, 0.0, 16.0);
        BOSS_ORB_MAX_DISTANCE = builder
                .comment("Maximum distance of an orb from a boss hit")
                .defineInRange("bossOrbMaxDistance", 4.0, 0.0, 16.0);
        builder.pop();

        SPEC = builder.build();
    }

    private RunelessServerConfig() {}
}
