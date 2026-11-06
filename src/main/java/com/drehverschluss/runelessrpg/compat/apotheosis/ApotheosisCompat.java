package com.drehverschluss.runelessrpg.compat.apotheosis;

import com.drehverschluss.runelessrpg.RunelessRPG;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Apotheosis integration. Only loaded when Apotheosis is present ({@code ModList.isLoaded("apotheosis")}); nothing else
 * in RunelessRPG references Apotheosis classes.
 * <p>
 * Registers the affix type {@code runelessrpg:attribute} ({@link FilteredAttributeAffix}) in Apotheosis' affix registry,
 * on both sides (the registry is synced to clients). The affixes themselves are data:
 * {@code data/runelessrpg/affixes/**}, each with the condition {@code neoforge:mod_loaded apotheosis}.
 */
public final class ApotheosisCompat {
    public static final ResourceLocation AFFIX_TYPE = ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "attribute");

    private ApotheosisCompat() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() -> {
            AffixRegistry.INSTANCE.registerCodec(AFFIX_TYPE, FilteredAttributeAffix.CODEC);
            RunelessRPG.LOGGER.info("Registered Apotheosis affix type {}", AFFIX_TYPE);
        }));
    }
}
