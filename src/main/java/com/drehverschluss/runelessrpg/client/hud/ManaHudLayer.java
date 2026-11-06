package com.drehverschluss.runelessrpg.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

/**
 * GUI layers of the mana bar. Two layers are registered: one below the vanilla hotbar (behind the hotbar and Miki's HUD,
 * which Miki draws directly above the hotbar) and one above it; the config decides which one draws. The half diamond
 * docked at Miki's diamond always draws behind it. Behind Miki's HUD the number is drawn in the front layer.
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
        ManaBar bar = ManaBar.INSTANCE;
        if (!bar.shouldRender()) {
            return;
        }
        boolean drawsBehind = bar.drawsBehind();
        if (behind && drawsBehind) {
            bar.render(graphics, bar.textInFront() ? ManaBar.Pass.SHAPE : ManaBar.Pass.ALL);
        } else if (!behind && !drawsBehind) {
            bar.render(graphics, ManaBar.Pass.ALL);
        } else if (!behind && bar.textInFront()) {
            // Behind Miki's HUD the number would be covered by Miki's vines; it stays readable in front
            bar.render(graphics, ManaBar.Pass.TEXT);
        }
    }
}
