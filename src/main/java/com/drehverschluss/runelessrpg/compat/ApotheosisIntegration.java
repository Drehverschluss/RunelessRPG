package com.drehverschluss.runelessrpg.compat;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Load condition {@code runelessrpg:apotheosis_integration}: true only if our Apotheosis integration (the affix type
 * {@code runelessrpg:attribute}) was registered. Our affix files carry it next to {@code neoforge:mod_loaded}, so a
 * failed integration (e.g. after an incompatible Apotheosis update) does not make Apotheosis log unknown affix types.
 * This class never touches Apotheosis classes.
 */
public final class ApotheosisIntegration {
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, RunelessRPG.MODID);

    private static volatile boolean active;

    static {
        CONDITIONS.register("apotheosis_integration", () -> Condition.CODEC);
    }

    private ApotheosisIntegration() {}

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }

    public static boolean isActive() {
        return active;
    }

    public static void setActive(boolean value) {
        active = value;
    }

    /** Logs the single warning when the integration cannot be used. */
    public static void disable(Throwable cause) {
        active = false;
        RunelessRPG.LOGGER.warn("RunelessRPG: Apotheosis integration disabled: {}", cause.toString());
    }

    public record Condition() implements ICondition {
        public static final Condition INSTANCE = new Condition();
        public static final MapCodec<Condition> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public boolean test(IContext context) {
            return active;
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }
}
