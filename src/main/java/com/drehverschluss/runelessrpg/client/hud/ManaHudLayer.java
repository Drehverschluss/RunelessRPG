package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * GUI layers of the mana bar. Two layers are registered: one below the vanilla hotbar (behind the hotbar and Miki's HUD,
 * which Miki draws directly above the hotbar) and one above it; the config decides which one draws.
 */
public final class ManaHudLayer implements LayeredDraw.Layer {
    public static final ManaHudLayer BEHIND = new ManaHudLayer(true);
    public static final ManaHudLayer FRONT = new ManaHudLayer(false);

    private final boolean behind;

    private ManaHudLayer(boolean behind) {
        this.behind = behind;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (RunelessClientConfig.DRAW_BEHIND_MIKI_HUD.get() != behind || !ManaBar.INSTANCE.shouldRender()) {
            return;
        }
        ManaBar.INSTANCE.render(graphics);
    }
}
