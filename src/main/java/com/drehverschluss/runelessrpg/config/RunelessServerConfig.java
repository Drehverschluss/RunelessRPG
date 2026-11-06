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
    public static final ModConfigSpec.DoubleValue SPELL_MANA_LEECH_EXTRA_TARGET_PERCENT;
    public static final ModConfigSpec.BooleanValue LEECH_CAP_AT_TARGET_HEALTH;

    public static final ModConfigSpec.EnumValue<SpellInfinityMode> SPELL_INFINITY_MODE;
    public static final ModConfigSpec.DoubleValue SPELL_INFINITY_COST_REDUCTION;
    public static final ModConfigSpec.DoubleValue SPELL_INFINITY_FLOOR_REDUCTION;

    /** Effect of the enchantment spell_engine:spell_infinity on mana costs. */
    public enum SpellInfinityMode {
        /** Lowers the cost factor and its floor. */
        REDUCE_COST,
        /** No effect on mana costs. */
        NONE
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
        SPELLS_MULTIPLIER = builder.defineInRange("spellsMultiplier", 1.0, 0.0, 1000.0);
        CLASS_SKILLS_ENABLED = builder
                .comment("Class skills in #runelessrpg:cost/class_skills")
                .define("classSkillsEnabled", true);
        CLASS_SKILLS_MULTIPLIER = builder.defineInRange("classSkillsMultiplier", 0.75, 0.0, 1000.0);
        WEAPON_SKILLS_ENABLED = builder
                .comment("Weapon skills in #runelessrpg:cost/weapon_skills")
                .define("weaponSkillsEnabled", true);
        WEAPON_SKILLS_MULTIPLIER = builder.defineInRange("weaponSkillsMultiplier", 0.5, 0.0, 1000.0);
        ITEM_ABILITIES_ENABLED = builder
                .comment("Item abilities in #runelessrpg:cost/item_abilities")
                .define("itemAbilitiesEnabled", false);
        ITEM_ABILITIES_MULTIPLIER = builder.defineInRange("itemAbilitiesMultiplier", 1.0, 0.0, 1000.0);
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
        SPELL_MANA_LEECH_EXTRA_TARGET_PERCENT = builder
                .comment("Spell mana leech: every further target hit in the same game tick gives only this percentage (0 = first target only)")
                .defineInRange("spellManaLeechExtraTargetPercent", 25.0, 0.0, 100.0);
        LEECH_CAP_AT_TARGET_HEALTH = builder
                .comment("Count at most the health the target had before the hit (no mana from overkill damage)")
                .define("leechCapAtTargetHealth", true);
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

        SPEC = builder.build();
    }

    private RunelessServerConfig() {}
}
