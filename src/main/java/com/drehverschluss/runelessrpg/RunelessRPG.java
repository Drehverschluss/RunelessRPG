package com.drehverschluss.runelessrpg;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(RunelessRPG.MODID)
public class RunelessRPG {
    public static final String MODID = "runelessrpg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RunelessRPG(IEventBus modEventBus, ModContainer modContainer) {
    }
}
