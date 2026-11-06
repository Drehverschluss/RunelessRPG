package com.drehverschluss.runelessrpg;

import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import com.drehverschluss.runelessrpg.client.hud.ManaHudLayer;
import com.drehverschluss.runelessrpg.client.screen.ManaHudEditScreen;
import com.drehverschluss.runelessrpg.client.screen.RunelessConfigMenuScreen;
import com.drehverschluss.runelessrpg.cost.CastContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/** Client entry point (not loaded on dedicated servers): mana bar, layout editor, client config. */
@Mod(value = RunelessRPG.MODID, dist = Dist.CLIENT)
public class RunelessRPGClient {
    public static final KeyMapping EDIT_HUD_KEY = new KeyMapping("key.runelessrpg.edit_hud",
            InputConstants.UNKNOWN.getValue(), "key.categories.runelessrpg");

    public RunelessRPGClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, RunelessClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, RunelessConfigMenuScreen::new);

        modEventBus.addListener(RunelessRPGClient::onRegisterGuiLayers);
        modEventBus.addListener(RunelessRPGClient::onRegisterKeyMappings);
        modEventBus.addListener(RunelessRPGClient::onConfigLoading);
        modEventBus.addListener(RunelessRPGClient::onConfigReloading);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, event -> CastContext.clear());
        NeoForge.EVENT_BUS.addListener(RunelessRPGClient::onClientTick);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Behind: below the hotbar, so the hotbar and Miki's HUD (drawn above the hotbar) cover the bar.
        event.registerBelow(VanillaGuiLayers.HOTBAR, id("mana_bar_behind"), ManaHudLayer.BEHIND);
        // Front: above Miki's HUD if present (registerAbove(HOTBAR) would end up below Miki's layer).
        ResourceLocation front = ModList.get().isLoaded("mikis_hud")
                ? ResourceLocation.fromNamespaceAndPath("mikis_hud", "stats") : VanillaGuiLayers.HOTBAR;
        try {
            event.registerAbove(front, id("mana_bar_front"), ManaHudLayer.FRONT);
        } catch (IllegalArgumentException missingLayer) {
            RunelessRPG.LOGGER.warn("Miki's HUD layer {} not found, drawing the mana bar above the hotbar instead", front);
            event.registerAbove(VanillaGuiLayers.HOTBAR, id("mana_bar_front"), ManaHudLayer.FRONT);
        }
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EDIT_HUD_KEY);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (EDIT_HUD_KEY.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new ManaHudEditScreen(null));
            }
        }
    }

    private static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == RunelessClientConfig.SPEC) {
            RunelessClientConfig.apply();
        }
    }

    private static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == RunelessClientConfig.SPEC) {
            RunelessClientConfig.apply();
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, path);
    }
}
