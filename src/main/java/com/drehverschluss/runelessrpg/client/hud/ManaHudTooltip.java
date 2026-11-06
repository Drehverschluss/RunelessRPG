package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.client.ClientManaState;
import com.drehverschluss.runelessrpg.util.CostFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

/**
 * Tooltip on the mana display ("Mana: 14 / 20", "+0.5/s") while a screen with a free mouse leaves the HUD visible:
 * the chat, and container screens (inventory, chests …) as long as the mouse is outside the container window.
 */
public final class ManaHudTooltip {
    private ManaHudTooltip() {}

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        if (!showsHud(screen, mouseX, mouseY)) {
            return;
        }
        ManaBar bar = ManaBar.INSTANCE;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !bar.shouldRender()
                || !bar.hitTest(mouseX, mouseY, event.getGuiGraphics().guiWidth(), event.getGuiGraphics().guiHeight())) {
            return;
        }
        event.getGuiGraphics().renderComponentTooltip(mc.font, lines(player), event.getMouseX(), event.getMouseY());
    }

    /** Chat: always; container screens: only outside the container window (no slot tooltips there). */
    private static boolean showsHud(Screen screen, double mouseX, double mouseY) {
        if (screen instanceof ChatScreen) {
            return true;
        }
        if (screen instanceof AbstractContainerScreen<?> container) {
            boolean inWindow = mouseX >= container.getGuiLeft() && mouseX < container.getGuiLeft() + container.getXSize()
                    && mouseY >= container.getGuiTop() && mouseY < container.getGuiTop() + container.getYSize();
            return !inWindow && container.getSlotUnderMouse() == null;
        }
        return false;
    }

    public static List<Component> lines(LocalPlayer player) {
        double mana = ClientManaState.getMana();
        double max = ManaBar.maxMana(player);
        double regen = player.getAttributes().hasAttribute(RunelessAttributes.MANA_REGEN)
                ? player.getAttributeValue(RunelessAttributes.MANA_REGEN) : 0.0;
        String rate = (regen >= 0 ? "+" : "") + CostFormat.format(regen);
        return List.of(
                Component.translatable("runelessrpg.hud.tooltip.mana", String.valueOf(Mth.floor(mana)), CostFormat.format(max)),
                Component.translatable("runelessrpg.hud.tooltip.regen", rate).withStyle(ChatFormatting.GRAY));
    }
}
