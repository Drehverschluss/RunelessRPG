package com.drehverschluss.runelessrpg.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common configuration ({@code config/runelessrpg-common.toml}), loaded on client and server at game start.
 * Holds the brewing ingredients: brewing recipes are built on both sides (the client needs them for the brewing stand
 * slots and recipe viewers), and a server config would not be loaded on the client.
 */
public final class RunelessCommonConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> MANA_INGREDIENT;
    public static final ModConfigSpec.ConfigValue<String> STRONG_MANA_INGREDIENT;
    public static final ModConfigSpec.ConfigValue<String> MAGICAL_STABILITY_INGREDIENT;
    public static final ModConfigSpec.ConfigValue<String> LONG_MAGICAL_STABILITY_INGREDIENT;
    public static final ModConfigSpec.ConfigValue<String> BORROWED_POWER_INGREDIENT;
    public static final ModConfigSpec.ConfigValue<String> MANA_HARVEST_INGREDIENT;
    public static final ModConfigSpec.ConfigValue<String> LONG_MANA_HARVEST_INGREDIENT;
    public static final ModConfigSpec.ConfigValue<String> STRONG_MANA_HARVEST_INGREDIENT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Brewing ingredients (item ids). Empty = recipe disabled. Changes need a restart (client and server).",
                "Splash and lingering variants (gunpowder, dragon's breath) and tipped arrows work like vanilla").push("brewing");
        MANA_INGREDIENT = ingredient(builder, "manaIngredient", "Awkward Potion -> Potion of Mana", "minecraft:lapis_lazuli");
        STRONG_MANA_INGREDIENT = ingredient(builder, "strongManaIngredient", "Potion of Mana -> Potion of Mana II", "minecraft:glowstone_dust");
        MAGICAL_STABILITY_INGREDIENT = ingredient(builder, "magicalStabilityIngredient",
                "Potion of Mana -> Potion of Magical Stability", "minecraft:amethyst_shard");
        LONG_MAGICAL_STABILITY_INGREDIENT = ingredient(builder, "longMagicalStabilityIngredient",
                "Potion of Magical Stability -> long Potion of Magical Stability", "minecraft:redstone");
        BORROWED_POWER_INGREDIENT = ingredient(builder, "borrowedPowerIngredient",
                "Potion of Mana -> Potion of Borrowed Power", "minecraft:fermented_spider_eye");
        MANA_HARVEST_INGREDIENT = ingredient(builder, "manaHarvestIngredient",
                "Potion of Mana -> Potion of Mana Harvest", "minecraft:rabbit_foot");
        LONG_MANA_HARVEST_INGREDIENT = ingredient(builder, "longManaHarvestIngredient",
                "Potion of Mana Harvest -> long Potion of Mana Harvest", "minecraft:redstone");
        STRONG_MANA_HARVEST_INGREDIENT = ingredient(builder, "strongManaHarvestIngredient",
                "Potion of Mana Harvest -> Potion of Mana Harvest II", "minecraft:glowstone_dust");
        builder.pop();
        SPEC = builder.build();
    }

    // Any string is accepted here: an id that cannot be parsed or names no item disables the recipe with a warning
    private static ModConfigSpec.ConfigValue<String> ingredient(ModConfigSpec.Builder builder, String key, String recipe, String defaultId) {
        return builder.comment(recipe).define(key, defaultId, value -> value instanceof String);
    }

    private RunelessCommonConfig() {}
}
