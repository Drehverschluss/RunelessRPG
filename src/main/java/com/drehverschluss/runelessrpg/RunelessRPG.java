package com.drehverschluss.runelessrpg;

import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.command.RunelessCommands;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.cost.CostEvents;
import com.drehverschluss.runelessrpg.impact.ManaLeech;
import com.drehverschluss.runelessrpg.impact.Overcharge;
import com.drehverschluss.runelessrpg.item.RunelessItems;
import com.drehverschluss.runelessrpg.mana.ManaAttachments;
import com.drehverschluss.runelessrpg.mana.ManaEvents;
import com.drehverschluss.runelessrpg.network.RunelessNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import org.slf4j.Logger;

@Mod(RunelessRPG.MODID)
public class RunelessRPG {
    public static final String MODID = "runelessrpg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RunelessRPG(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, RunelessServerConfig.SPEC);
        RunelessAttributes.register(modEventBus);
        RunelessItems.register(modEventBus);
        ManaAttachments.register(modEventBus);
        RunelessNetwork.register(modEventBus);
        ManaEvents.register(modEventBus);
        CostEvents.register(modEventBus);
        ManaLeech.register();
        Overcharge.register();
        modEventBus.addListener(RunelessRPG::onAddPackFinders);
        NeoForge.EVENT_BUS.addListener(RunelessCommands::onRegisterCommands);
    }

    /**
     * Built-in resource pack at the top of the list, always active: overrides the description of Spell Engine's
     * Spell Infinity ("Reduces the mana cost of spells"). A plain entry in our own lang file is not enough, because
     * all mods share one resource pack and the order of the mods inside it is not guaranteed.
     */
    private static void onAddPackFinders(AddPackFindersEvent event) {
        event.addPackFinders(ResourceLocation.fromNamespaceAndPath(MODID, "resourcepacks/spell_infinity_text"), PackType.CLIENT_RESOURCES,
                Component.literal("RunelessRPG Spell Infinity text"), PackSource.BUILT_IN, true, Pack.Position.TOP);
    }
}
