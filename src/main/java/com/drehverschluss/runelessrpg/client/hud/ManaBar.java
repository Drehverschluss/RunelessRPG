package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.client.ClientManaState;
import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import com.drehverschluss.runelessrpg.util.CostFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * The mana bar: layout (anchor + offset + scale, as in Perilscope's {@code HudElement}, same author, MIT) and drawing
 * of the three styles. Positions are GUI pixels.
 */
public final class ManaBar {
    public static final ManaBar INSTANCE = new ManaBar();

    public static final float MIN_SCALE = 0.25F;
    public static final float MAX_SCALE = 4.0F;
    public static final HudAnchor DEFAULT_ANCHOR = HudAnchor.MIKI_RIGHT;

    /** A Miki anchor is kept while dragging as long as the bar stays this close to the docking position. */
    public static final int MIKI_KEEP_DISTANCE = 48;
    /** Dragging a bar this close to a docking position docks it at Miki again. */
    private static final int MIKI_SNAP_DISTANCE = 4;
    private static final long FILL_GLIDE_NANOS = 100_000_000L;
    private static final int TEXT_ABOVE_GAP = 1;
    private static final int EDGE_MARGIN = 2;

    private static final int THIN_WIDTH = 182;
    private static final int THIN_HEIGHT = 5;
    private static final int MIKI_WIDTH = 86;
    private static final int MIKI_HEIGHT = 10;
    private static final int VERTICAL_WIDTH = 7;
    private static final int VERTICAL_HEIGHT = 62;

    private static final int BORDER = 0xFF000000;
    private static final int EMPTY = 0xFF1B1D26;
    private static final int MIKI_OUTER = 0xFF140F0E;
    private static final int MIKI_FRAME_LIGHT = 0xFFC88A6A;
    private static final int MIKI_FRAME_MID = 0xFF9A5B48;
    private static final int MIKI_FRAME_DARK = 0xFF5E3127;
    private static final int MIKI_INNER = 0xFF0E1619;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DEBT_TEXT_COLOR = 0xFFFF9A9A;

    /** Bar position and size on screen (scale applied). */
    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    private HudAnchor anchor = DEFAULT_ANCHOR;
    private int offsetX;
    private int offsetY;
    private float scale = 1.0F;
    private boolean visible = true;

    private double animFrom = Double.NaN;
    private double animTarget = Double.NaN;
    private long animStart;
    private double lastShown;

    private ManaBar() {}

    // ------------------------------------------------------------------ layout

    public void applyLayout(HudAnchor anchor, int offsetX, int offsetY, float scale, boolean visible) {
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        setScale(scale);
        this.visible = visible;
    }

    public void resetLayout() {
        applyLayout(DEFAULT_ANCHOR, 0, 0, 1.0F, true);
    }

    public ManaBarStyle effectiveStyle() {
        ManaBarStyle style = RunelessClientConfig.STYLE.get();
        if (style == ManaBarStyle.AUTO) {
            return MikiCompat.isLoaded() ? ManaBarStyle.MIKI : ManaBarStyle.THIN;
        }
        return style;
    }

    private static int naturalWidth(ManaBarStyle style) {
        return switch (style) {
            case MIKI -> MIKI_WIDTH;
            case VERTICAL -> VERTICAL_WIDTH;
            default -> THIN_WIDTH;
        };
    }

    /** THIN is shortened to the MIKI length at the docked anchors, where 182 px would not fit beside the HUD. */
    private int widthFor(ManaBarStyle style) {
        if (style == ManaBarStyle.THIN && anchor.isMiki()) {
            return MIKI_WIDTH;
        }
        return naturalWidth(style);
    }

    private static int naturalHeight(ManaBarStyle style) {
        return switch (style) {
            case MIKI -> MIKI_HEIGHT;
            case VERTICAL -> VERTICAL_HEIGHT;
            default -> THIN_HEIGHT;
        };
    }

    /** Total scale: own scale × Miki's shrink factor for the Miki anchors. */
    public double effectiveScale(int screenWidth, int screenHeight) {
        if (anchor.isMiki()) {
            return scale * MikiCompat.dock(anchor == HudAnchor.MIKI_RIGHT, screenWidth, screenHeight).scale();
        }
        return scale;
    }

    /** Position of the bar for the given anchor without offset (used for Miki anchors). */
    private double[] dockedBase(HudAnchor miki, double width, double height, int screenWidth, int screenHeight) {
        boolean right = miki == HudAnchor.MIKI_RIGHT;
        MikiCompat.Dock dock = MikiCompat.dock(right, screenWidth, screenHeight);
        double x;
        double y;
        if (dock.miki()) {
            // Starts under the outer tip of the diamond, vertically centered on it
            x = right ? dock.x() : dock.x() - width;
            y = dock.y() - height / 2.0;
            if (x + width > screenWidth - EDGE_MARGIN || x < EDGE_MARGIN) {
                // No room beside Miki's HUD (narrow screen): centered above the diamond instead
                x = Mth.clamp(dock.diamondX() - width / 2.0, EDGE_MARGIN, Math.max(EDGE_MARGIN, screenWidth - EDGE_MARGIN - width));
                y = dock.artTop() - height - 1;
            }
        } else {
            // Beside the hotbar at the experience bar, kept above the bottom edge
            x = right ? dock.x() : dock.x() - width;
            y = Math.min(dock.y() - height / 2.0, screenHeight - EDGE_MARGIN - height);
        }
        return new double[]{x, y};
    }

    public Bounds getBounds(int screenWidth, int screenHeight) {
        ManaBarStyle style = effectiveStyle();
        double s = effectiveScale(screenWidth, screenHeight);
        int width = Math.max(1, Mth.ceil(widthFor(style) * s));
        int height = Math.max(1, Mth.ceil(naturalHeight(style) * s));
        if (anchor.isMiki()) {
            double[] base = dockedBase(anchor, width, height, screenWidth, screenHeight);
            return new Bounds((int) Math.round(base[0]) + offsetX, (int) Math.round(base[1]) + offsetY, width, height);
        }
        int x = Mth.clamp(anchor.resolveX(screenWidth, width, offsetX), 0, Math.max(0, screenWidth - width));
        int y = Mth.clamp(anchor.resolveY(screenHeight, height, offsetY), 0, Math.max(0, screenHeight - height));
        return new Bounds(x, y, width, height);
    }

    /** Area used by the editor to pick the bar: the bar plus the text above it. */
    public Bounds getEditorBounds(int screenWidth, int screenHeight) {
        Bounds bar = getBounds(screenWidth, screenHeight);
        if (textAbove(effectiveStyle()) && RunelessClientConfig.TEXT_MODE.get() != TextMode.NONE) {
            int extra = Mth.ceil((Minecraft.getInstance().font.lineHeight + TEXT_ABOVE_GAP) * effectiveScale(screenWidth, screenHeight));
            return new Bounds(bar.x(), bar.y() - extra, bar.width(), bar.height() + extra);
        }
        return bar;
    }

    /**
     * Moves the bar so that its top left corner is at the given position. A Miki anchor is kept while the bar stays
     * near its docking point; otherwise the nearest screen anchor is chosen. Dropping the bar on a docking point
     * docks it again.
     */
    public void moveTo(int x, int y, int screenWidth, int screenHeight) {
        Bounds bounds = getBounds(screenWidth, screenHeight);
        int width = bounds.width();
        int height = bounds.height();
        for (HudAnchor miki : new HudAnchor[]{HudAnchor.MIKI_RIGHT, HudAnchor.MIKI_LEFT}) {
            double[] base = dockedBase(miki, width, height, screenWidth, screenHeight);
            int dx = x - (int) Math.round(base[0]);
            int dy = y - (int) Math.round(base[1]);
            boolean keep = miki == anchor && Math.abs(dx) <= MIKI_KEEP_DISTANCE && Math.abs(dy) <= MIKI_KEEP_DISTANCE;
            boolean snap = Math.abs(dx) <= MIKI_SNAP_DISTANCE && Math.abs(dy) <= MIKI_SNAP_DISTANCE;
            if (keep || snap) {
                boolean redock = miki != anchor;
                anchor = miki;
                offsetX = redock ? 0 : dx;
                offsetY = redock ? 0 : dy;
                return;
            }
        }
        int clampedX = Mth.clamp(x, 0, Math.max(0, screenWidth - width));
        int clampedY = Mth.clamp(y, 0, Math.max(0, screenHeight - height));
        anchor = HudAnchor.nearest(clampedX + width / 2, clampedY + height / 2, screenWidth, screenHeight);
        offsetX = anchor.offsetX(clampedX, screenWidth, width);
        offsetY = anchor.offsetY(clampedY, screenHeight, height);
    }

    // ------------------------------------------------------------------ drawing

    /** @return true if the bar should be drawn for the local player right now */
    public boolean shouldRender() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!visible || player == null || mc.options.hideGui || mc.gameMode == null) {
            return false;
        }
        if (player.isSpectator() && RunelessClientConfig.HIDE_IN_SPECTATOR.get()) {
            return false;
        }
        if (player.getAbilities().instabuild && RunelessClientConfig.HIDE_IN_CREATIVE.get()) {
            return false;
        }
        return maxMana(player) > 0.0;
    }

    public void render(GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        draw(graphics, ClientManaState.getMana(), maxMana(player), false);
    }

    /** Editor preview: the real values if available, otherwise a sample. */
    public void renderPreview(GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        double max = player != null ? maxMana(player) : 0.0;
        if (max <= 0.0) {
            draw(graphics, 14.0, 20.0, !visible);
        } else {
            draw(graphics, ClientManaState.getMana(), max, !visible);
        }
    }

    private static double maxMana(LocalPlayer player) {
        return player.getAttributes().hasAttribute(RunelessAttributes.MAX_MANA) ? player.getAttributeValue(RunelessAttributes.MAX_MANA) : 0.0;
    }

    /** Displayed (smoothed) mana: glides to a new value within ~100 ms. */
    public double shownMana(double mana) {
        long now = System.nanoTime();
        if (Double.isNaN(animTarget)) {
            animFrom = mana;
            animTarget = mana;
            animStart = now;
        } else if (mana != animTarget) {
            animFrom = lastShown;
            animTarget = mana;
            animStart = now;
        }
        double t = Math.min(1.0, (now - animStart) / (double) FILL_GLIDE_NANOS);
        lastShown = animFrom + (animTarget - animFrom) * t;
        return lastShown;
    }

    private void draw(GuiGraphics graphics, double mana, double max, boolean dimmed) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        ManaBarStyle style = effectiveStyle();
        Bounds bounds = getBounds(screenWidth, screenHeight);
        float s = (float) effectiveScale(screenWidth, screenHeight);
        int width = widthFor(style);
        int height = naturalHeight(style);

        double shown = shownMana(mana);
        double fill = max > 0 ? Mth.clamp(shown / max, 0.0, 1.0) : 0.0;
        double debt = max > 0 && shown < 0 ? Mth.clamp(-shown / max, 0.0, 1.0) : 0.0;
        int fillColor = RunelessClientConfig.color(RunelessClientConfig.FILL_COLOR, 0xFF3A7BFF);
        int debtColor = RunelessClientConfig.color(RunelessClientConfig.DEBT_COLOR, 0xFFB02020);

        graphics.pose().pushPose();
        graphics.pose().translate(bounds.x(), bounds.y(), 0.0F);
        graphics.pose().scale(s, s, 1.0F);
        switch (style) {
            case MIKI -> drawMiki(graphics, width, height, fill, debt, fillColor, debtColor);
            case VERTICAL -> drawVertical(graphics, width, height, fill, debt, fillColor, debtColor);
            default -> drawThin(graphics, width, height, fill, debt, fillColor, debtColor);
        }
        float flash = ManaFlash.intensity();
        if (flash > 0.0F) {
            int flashColor = RunelessClientConfig.color(RunelessClientConfig.FLASH_COLOR, 0xFFFF3030);
            graphics.fill(0, 0, width, height, withAlpha(flashColor, 0.7F * flash));
            graphics.renderOutline(-1, -1, width + 2, height + 2, withAlpha(flashColor, flash));
        }
        drawText(graphics, style, width, height, mana, max);
        if (dimmed) {
            graphics.fill(-1, -1, width + 1, height + 1, 0x90000000);
        }
        graphics.pose().popPose();
    }

    private static void drawThin(GuiGraphics g, int w, int h, double fill, double debt, int fillColor, int debtColor) {
        g.fill(0, 0, w, h, BORDER);
        g.fill(1, 1, w - 1, h - 1, EMPTY);
        int inner = w - 2;
        int filled = (int) Math.round(inner * fill);
        if (filled > 0) {
            g.fill(1, 1, 1 + filled, h - 1, fillColor);
            g.fill(1, 1, 1 + filled, 2, lighter(fillColor, 0.35F));
        }
        int debtLength = (int) Math.round(inner * debt);
        if (debtLength > 0) {
            g.fill(1, 1, 1 + debtLength, h - 1, debtColor);
        }
    }

    private static void drawMiki(GuiGraphics g, int w, int h, double fill, double debt, int fillColor, int debtColor) {
        g.fill(0, 0, w, h, MIKI_OUTER);
        g.fill(1, 1, w - 1, h - 1, MIKI_FRAME_MID);
        g.fill(1, 1, w - 1, 2, MIKI_FRAME_LIGHT);
        g.fill(1, h - 2, w - 1, h - 1, MIKI_FRAME_DARK);
        g.fill(2, 2, w - 2, h - 2, MIKI_INNER);
        int inner = w - 4;
        int filled = (int) Math.round(inner * fill);
        if (filled > 0) {
            g.fillGradient(2, 2, 2 + filled, h - 2, lighter(fillColor, 0.25F), darker(fillColor, 0.35F));
            g.fill(2, 2, 2 + filled, 3, lighter(fillColor, 0.55F));
        }
        int debtLength = (int) Math.round(inner * debt);
        if (debtLength > 0) {
            g.fillGradient(2, 2, 2 + debtLength, h - 2, lighter(debtColor, 0.2F), darker(debtColor, 0.35F));
        }
        // Small rivets at both ends, like the studs on Miki's frames
        g.fill(0, h / 2 - 1, 1, h / 2 + 1, MIKI_FRAME_LIGHT);
        g.fill(w - 1, h / 2 - 1, w, h / 2 + 1, MIKI_FRAME_LIGHT);
    }

    private static void drawVertical(GuiGraphics g, int w, int h, double fill, double debt, int fillColor, int debtColor) {
        g.fill(0, 0, w, h, BORDER);
        g.fill(1, 1, w - 1, h - 1, EMPTY);
        int inner = h - 2;
        int filled = (int) Math.round(inner * fill);
        if (filled > 0) {
            g.fill(1, h - 1 - filled, w - 1, h - 1, fillColor);
            g.fill(1, h - 1 - filled, 2, h - 1, lighter(fillColor, 0.35F));
        }
        int debtLength = (int) Math.round(inner * debt);
        if (debtLength > 0) {
            g.fill(1, h - 1 - debtLength, w - 1, h - 1, debtColor);
        }
    }

    private static boolean textAbove(ManaBarStyle style) {
        return style != ManaBarStyle.MIKI;
    }

    private static void drawText(GuiGraphics g, ManaBarStyle style, int w, int h, double mana, double max) {
        TextMode mode = RunelessClientConfig.TEXT_MODE.get();
        if (mode == TextMode.NONE || max <= 0) {
            return;
        }
        String current = String.valueOf(Mth.floor(mana));
        String text = switch (mode) {
            case CURRENT -> current;
            case CURRENT_MAX -> current + " / " + CostFormat.format(max);
            case PERCENT -> Math.round(mana / max * 100.0) + "%";
            default -> "";
        };
        Font font = Minecraft.getInstance().font;
        int color = mana < 0 ? DEBT_TEXT_COLOR : TEXT_COLOR;
        int textWidth = font.width(text);
        int x = (w - textWidth) / 2;
        int y = textAbove(style) ? -font.lineHeight - TEXT_ABOVE_GAP + 1 : (h - font.lineHeight) / 2 + 1;
        g.drawString(font, text, x, y, color, true);
    }

    private static int withAlpha(int color, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (color & 0xFFFFFF);
    }

    private static int lighter(int color, float amount) {
        return FastColor.ARGB32.lerp(amount, color, 0xFFFFFFFF);
    }

    private static int darker(int color, float amount) {
        return FastColor.ARGB32.lerp(amount, color, 0xFF000000);
    }

    // ------------------------------------------------------------------ accessors

    public HudAnchor getAnchor() {
        return anchor;
    }

    public void setAnchor(HudAnchor anchor) {
        this.anchor = anchor;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
