package com.drehverschluss.runelessrpg;

import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.command.RunelessCommands;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.mana.ManaAttachments;
import com.drehverschluss.runelessrpg.mana.ManaEvents;
import com.drehverschluss.runelessrpg.network.RunelessNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(RunelessRPG.MODID)
public class RunelessRPG {
    public static final String MODID = "runelessrpg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RunelessRPG(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, RunelessServerConfig.SPEC);
        RunelessAttributes.register(modEventBus);
        ManaAttachments.register(modEventBus);
        RunelessNetwork.register(modEventBus);
        ManaEvents.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RunelessCommands::onRegisterCommands);
    }
}
