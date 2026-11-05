package com.drehverschluss.runelessrpg.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-side configuration (one file per world: {@code serverconfig/runelessrpg-server.toml}).
 * Later phases add further sections to this class.
 */
public final class RunelessServerConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue BASE_MAX_MANA;
    public static final ModConfigSpec.DoubleValue BASE_MANA_REGEN;

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

        SPEC = builder.build();
    }

    private RunelessServerConfig() {}
}
