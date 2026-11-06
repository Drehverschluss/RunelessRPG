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
 * of the styles. Positions are GUI pixels.
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

    /** Width available for the number in the visible part of the half diamond (x 168 … 185 at the tip rows). */
    private static final double HALF_DIAMOND_TEXT_WIDTH = 17.0;
    /** Width available for the number in a whole diamond. */
    private static final double DIAMOND_TEXT_WIDTH = 26.0;
    /** Smallest text scale in a diamond before CURRENT_MAX / PERCENT fall back to CURRENT. */
    private static final double MIN_DIAMOND_TEXT_SCALE = 0.75;
    private static final double BAND_TEXT_SCALE = 0.75;
    private static final double BAND_TEXT_GAP = 2.0;

    private static final int BORDER = 0xFF000000;
    private static final int EMPTY = 0xFF1B1D26;
    private static final int MIKI_OUTER = 0xFF140F0E;
    private static final int MIKI_FRAME_LIGHT = 0xFFC88A6A;
    private static final int MIKI_FRAME_MID = 0xFF9A5B48;
    private static final int MIKI_FRAME_DARK = 0xFF5E3127;
    private static final int MIKI_INNER = 0xFF0E1619;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DEBT_TEXT_COLOR = 0xFFFF9A9A;
    private static final int TEXT_OUTLINE_COLOR = 0x000000;

    /** Bar position and size on screen (scale applied). */
    public record Bounds(int x, int y, int width, int height) {
        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }

        Bounds union(Bounds other) {
            int minX = Math.min(x, other.x);
            int minY = Math.min(y, other.y);
            return new Bounds(minX, minY, Math.max(x + width, other.x + other.width) - minX,
                    Math.max(y + height, other.y + other.height) - minY);
        }
    }

    /** Exact position of the display: top left corner, scale and unscaled size. */
    private record Placement(double x, double y, double scale, int width, int height) {}

    /** What a GUI layer draws: everything, only the shape (behind Miki) or only the number (in front of Miki). */
    public enum Pass { ALL, SHAPE, TEXT }

    /** Text to draw: string, center or left / right reference point (local pixels) and scale. */
    private record TextLayout(String text, double x, double y, double scale, int align) {}

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

    /** The style that is drawn: AUTO resolved, MIKI_BAND falls back to MIKI away from Miki's diamonds. */
    public ManaBarStyle effectiveStyle() {
        ManaBarStyle style = RunelessClientConfig.STYLE.get();
        if (style == ManaBarStyle.AUTO) {
            return MikiCompat.isLoaded() ? ManaBarStyle.MIKI : ManaBarStyle.THIN;
        }
        if (style == ManaBarStyle.MIKI_BAND && !atMikiDiamond()) {
            return ManaBarStyle.MIKI;
        }
        return style;
    }

    /** True when the display is docked beside Miki's health / hunger diamond right now. */
    private boolean atMikiDiamond() {
        return anchor.isMikiSide() && MikiCompat.isActive();
    }

    private boolean isShape(ManaBarStyle style) {
        return style == ManaBarStyle.MIKI_DIAMOND || style == ManaBarStyle.MIKI_BAND;
    }

    /** The half diamond docked at Miki's diamond: always drawn behind it (Miki's diamond hides its inner half). */
    public boolean behindMikiDiamond() {
        return effectiveStyle() == ManaBarStyle.MIKI_DIAMOND && atMikiDiamond();
    }

    /** @return true if the display is drawn in the layer behind the hotbar and Miki's HUD */
    public boolean drawsBehind() {
        return behindMikiDiamond() || RunelessClientConfig.DRAW_BEHIND_MIKI_HUD.get();
    }

    /** Drawn behind Miki's HUD at a Miki anchor: the number goes to the front layer so Miki's vines cannot cover it. */
    public boolean textInFront() {
        return drawsBehind() && anchor.isMiki() && MikiCompat.isActive();
    }

    private ManaShapes.Shape shapeFor(ManaBarStyle style) {
        boolean right = !anchor.isMiki() || anchor.isMikiRight();
        if (style == ManaBarStyle.MIKI_BAND) {
            return ManaShapes.band(RunelessClientConfig.BAND_WIDTH.get(), RunelessClientConfig.BAND_GAP.get(), right);
        }
        return ManaShapes.diamond(right);
    }

    private static int naturalWidth(ManaBarStyle style) {
        return switch (style) {
            case MIKI -> MIKI_WIDTH;
            case VERTICAL -> VERTICAL_WIDTH;
            case MIKI_DIAMOND -> ManaShapes.DIAMOND_SIZE;
            default -> THIN_WIDTH;
        };
    }

    /**
     * THIN is shortened to the MIKI length at the docked anchors, where 182 px would not fit beside the HUD; beside
     * Miki's level box both bars fill the free space up to the diamonds.
     */
    private int widthFor(ManaBarStyle style) {
        if ((style == ManaBarStyle.THIN || style == ManaBarStyle.MIKI) && anchor.isMikiLevel() && MikiCompat.isActive()) {
            return anchor.isMikiRight() ? MikiCompat.LEVEL_BAR_LENGTH_RIGHT : MikiCompat.LEVEL_BAR_LENGTH_LEFT;
        }
        if (style == ManaBarStyle.THIN && anchor.isMiki()) {
            return MIKI_WIDTH;
        }
        return naturalWidth(style);
    }

    private static int naturalHeight(ManaBarStyle style) {
        return switch (style) {
            case MIKI -> MIKI_HEIGHT;
            case VERTICAL -> VERTICAL_HEIGHT;
            case MIKI_DIAMOND -> ManaShapes.DIAMOND_SIZE;
            default -> THIN_HEIGHT;
        };
    }

    /** Total scale: own scale × Miki's shrink factor for the Miki anchors. */
    public double effectiveScale(int screenWidth, int screenHeight) {
        if (anchor.isMiki()) {
            return scale * MikiCompat.dock(anchor.isMikiRight(), screenWidth, screenHeight).scale();
        }
        return scale;
    }

    /** Position of the bar for the given Miki anchor without offset. */
    private double[] dockedBase(HudAnchor miki, double width, double height, int screenWidth, int screenHeight) {
        boolean right = miki.isMikiRight();
        MikiCompat.Dock dock = miki.isMikiLevel()
                ? MikiCompat.levelDock(right, screenWidth, screenHeight)
                : MikiCompat.dock(right, screenWidth, screenHeight);
        double x = right ? dock.x() : dock.x() - width;
        double y;
        if (dock.miki() && miki.isMikiSide()) {
            // Starts under the outer tip of the diamond, vertically centered on it
            y = dock.y() - height / 2.0;
            if (x + width > screenWidth - EDGE_MARGIN || x < EDGE_MARGIN) {
                // No room beside Miki's HUD (narrow screen): centered above the diamond instead
                x = Mth.clamp(dock.diamondX() - width / 2.0, EDGE_MARGIN, Math.max(EDGE_MARGIN, screenWidth - EDGE_MARGIN - width));
                y = dock.artTop() - height - 1;
            }
        } else if (dock.miki()) {
            // Beside the level box, running outwards, vertically centered on the box
            y = dock.y() - height / 2.0;
        } else {
            // Beside the hotbar at the experience bar, kept above the bottom edge
            y = Math.min(dock.y() - height / 2.0, screenHeight - EDGE_MARGIN - height);
        }
        return new double[]{x, y};
    }

    private Placement placement(int screenWidth, int screenHeight) {
        ManaBarStyle style = effectiveStyle();
        if (isShape(style) && atMikiDiamond()) {
            return mikiShapePlacement(style, screenWidth, screenHeight);
        }
        double s = effectiveScale(screenWidth, screenHeight);
        int naturalWidth = widthFor(style);
        int naturalHeight = naturalHeight(style);
        int width = Math.max(1, Mth.ceil(naturalWidth * s));
        int height = Math.max(1, Mth.ceil(naturalHeight * s));
        if (anchor.isMiki()) {
            double[] base = dockedBase(anchor, width, height, screenWidth, screenHeight);
            return new Placement(Math.round(base[0]) + offsetX, Math.round(base[1]) + offsetY, s, naturalWidth, naturalHeight);
        }
        int x = Mth.clamp(anchor.resolveX(screenWidth, width, offsetX), 0, Math.max(0, screenWidth - width));
        int y = Mth.clamp(anchor.resolveY(screenHeight, height, offsetY), 0, Math.max(0, screenHeight - height));
        return new Placement(x, y, s, naturalWidth, naturalHeight);
    }

    /**
     * Diamond / band at Miki's diamond, in Miki coordinates scaled like Miki's HUD. The own scale zooms around the
     * diamond center (diamond) or the center of Miki's diamond (band). Kept inside the screen.
     */
    private Placement mikiShapePlacement(ManaBarStyle style, int screenWidth, int screenHeight) {
        boolean right = anchor.isMikiRight();
        ManaShapes.Shape shape = shapeFor(style);
        double side = right ? 1.0 : -1.0;
        double refX;
        double refY = MikiCompat.FRAME_CENTER_Y;
        double originX;
        double originY;
        if (style == ManaBarStyle.MIKI_DIAMOND) {
            refX = side * MikiCompat.SIDE_DIAMOND_X;
            originX = refX - shape.width() / 2.0;
            originY = refY - shape.height() / 2.0;
        } else {
            refX = side * MikiCompat.FRAME_CENTER_X;
            originX = shape.originX;
            originY = shape.originY;
        }
        double mikiScale = MikiCompat.scale(screenWidth);
        double s = scale * mikiScale;
        double x = screenWidth / 2 + (refX + (originX - refX) * scale) * mikiScale + offsetX;
        double y = screenHeight + (refY + (originY - refY) * scale) * mikiScale + offsetY;
        double width = shape.width() * s;
        double height = shape.height() * s;
        if (width <= screenWidth - 2 * EDGE_MARGIN) {
            x = Mth.clamp(x, EDGE_MARGIN, screenWidth - EDGE_MARGIN - width);
        }
        if (height <= screenHeight - EDGE_MARGIN) {
            y = Math.min(y, screenHeight - EDGE_MARGIN - height);
        }
        return new Placement(x, y, s, shape.width(), shape.height());
    }

    private static Bounds toBounds(Placement p) {
        return new Bounds((int) Math.round(p.x()), (int) Math.round(p.y()),
                Math.max(1, Mth.ceil(p.width() * p.scale())), Math.max(1, Mth.ceil(p.height() * p.scale())));
    }

    public Bounds getBounds(int screenWidth, int screenHeight) {
        return toBounds(placement(screenWidth, screenHeight));
    }

    /**
     * Area used by the editor to pick and frame the display: the bar plus the text above it, the visible part of the
     * half diamond, the band plus its number.
     */
    public Bounds getEditorBounds(int screenWidth, int screenHeight) {
        ManaBarStyle style = effectiveStyle();
        Placement p = placement(screenWidth, screenHeight);
        Bounds bounds = toBounds(p);
        if (style == ManaBarStyle.MIKI_DIAMOND && atMikiDiamond()) {
            return visibleHalfDiamond(p, screenWidth, screenHeight);
        }
        if (style == ManaBarStyle.MIKI_BAND) {
            TextLayout text = textLayout(style, 20.0, 20.0);
            if (text != null) {
                Font font = Minecraft.getInstance().font;
                double textWidth = (font.width(text.text()) + 2) * text.scale() * p.scale();
                double textX = p.x() + text.x() * p.scale() - (text.align() < 0 ? textWidth : 0);
                double textHeight = (font.lineHeight + 1) * text.scale() * p.scale();
                double textY = p.y() + text.y() * p.scale() - textHeight / 2.0;
                bounds = bounds.union(new Bounds(Mth.floor(textX), Mth.floor(textY), Mth.ceil(textWidth), Mth.ceil(textHeight)));
            }
            return bounds;
        }
        if (textAbove(style) && RunelessClientConfig.TEXT_MODE.get() != TextMode.NONE) {
            int extra = Mth.ceil((Minecraft.getInstance().font.lineHeight + TEXT_ABOVE_GAP) * p.scale());
            return new Bounds(bounds.x(), bounds.y() - extra, bounds.width(), bounds.height() + extra);
        }
        return bounds;
    }

    /** True if the point lies on the visible display (bar rectangle, shape pixels, uncovered part of the half diamond). */
    public boolean hitTest(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        ManaBarStyle style = effectiveStyle();
        Placement p = placement(screenWidth, screenHeight);
        if (!isShape(style)) {
            return toBounds(p).contains(mouseX, mouseY);
        }
        int localX = Mth.floor((mouseX - p.x()) / p.scale());
        int localY = Mth.floor((mouseY - p.y()) / p.scale());
        if (!shapeFor(style).contains(localX, localY)) {
            return false;
        }
        return style != ManaBarStyle.MIKI_DIAMOND || !atMikiDiamond() || !underMikiDiamond(mouseX, mouseY, screenWidth, screenHeight);
    }

    /** True if Miki's health / hunger diamond (on the anchor's side) covers the point. */
    private boolean underMikiDiamond(double x, double y, int screenWidth, int screenHeight) {
        double mikiScale = MikiCompat.scale(screenWidth);
        double side = anchor.isMikiRight() ? 1.0 : -1.0;
        double centerX = screenWidth / 2 + side * MikiCompat.FRAME_CENTER_X * mikiScale;
        double centerY = screenHeight + MikiCompat.FRAME_CENTER_Y * mikiScale;
        return Math.abs(x - centerX) + Math.abs(y - centerY) <= MikiCompat.FRAME_RADIUS * mikiScale;
    }

    private Bounds visibleHalfDiamond(Placement p, int screenWidth, int screenHeight) {
        ManaShapes.Shape shape = shapeFor(ManaBarStyle.MIKI_DIAMOND);
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int y = 0; y < shape.height(); y++) {
            for (int x = 0; x < shape.width(); x++) {
                double sx = p.x() + (x + 0.5) * p.scale();
                double sy = p.y() + (y + 0.5) * p.scale();
                if (shape.contains(x, y) && !underMikiDiamond(sx, sy, screenWidth, screenHeight)) {
                    minX = Math.min(minX, p.x() + x * p.scale());
                    minY = Math.min(minY, p.y() + y * p.scale());
                    maxX = Math.max(maxX, p.x() + (x + 1) * p.scale());
                    maxY = Math.max(maxY, p.y() + (y + 1) * p.scale());
                }
            }
        }
        if (minX > maxX) {
            return toBounds(p);
        }
        return new Bounds(Mth.floor(minX), Mth.floor(minY), Mth.ceil(maxX) - Mth.floor(minX), Mth.ceil(maxY) - Mth.floor(minY));
    }

    /**
     * Moves the bar so that its top left corner is at the given position. A Miki anchor is kept while the bar stays
     * near its docking point; otherwise the nearest screen anchor is chosen. Dropping the bar on a docking point
     * docks it again.
     */
    public void moveTo(int x, int y, int screenWidth, int screenHeight) {
        HudAnchor previous = anchor;
        int previousX = offsetX;
        int previousY = offsetY;
        Bounds bounds = getBounds(screenWidth, screenHeight);
        int width = bounds.width();
        int height = bounds.height();
        // The current anchor first, so it is kept while the bar stays near it
        HudAnchor[] candidates = {previous, HudAnchor.MIKI_RIGHT, HudAnchor.MIKI_LEFT, HudAnchor.MIKI_LEVEL_RIGHT, HudAnchor.MIKI_LEVEL_LEFT};
        for (HudAnchor miki : candidates) {
            if (!miki.isMiki()) {
                continue;
            }
            // Docked position of this anchor (size and scale can differ per anchor)
            anchor = miki;
            offsetX = 0;
            offsetY = 0;
            Bounds docked = getBounds(screenWidth, screenHeight);
            int dx = x - docked.x();
            int dy = y - docked.y();
            boolean keep = miki == previous && Math.abs(dx) <= MIKI_KEEP_DISTANCE && Math.abs(dy) <= MIKI_KEEP_DISTANCE;
            boolean snap = Math.abs(dx) <= MIKI_SNAP_DISTANCE && Math.abs(dy) <= MIKI_SNAP_DISTANCE;
            if (keep || snap) {
                boolean redock = miki != previous;
                offsetX = redock ? 0 : dx;
                offsetY = redock ? 0 : dy;
                return;
            }
        }
        anchor = previous;
        offsetX = previousX;
        offsetY = previousY;
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

    public void render(GuiGraphics graphics, Pass pass) {
        LocalPlayer player = Minecraft.getInstance().player;
        draw(graphics, ClientManaState.getMana(), maxMana(player), false, pass);
    }

    /** Editor preview: the real values if available, otherwise a sample. */
    public void renderPreview(GuiGraphics graphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        double max = player != null ? maxMana(player) : 0.0;
        // A visible shape behind Miki's diamond is already drawn by the HUD below the editor; only its number on top
        Pass pass = visible && textInFront() && shouldRender() ? Pass.TEXT : Pass.ALL;
        if (max <= 0.0) {
            draw(graphics, 14.0, 20.0, !visible, pass);
        } else {
            draw(graphics, ClientManaState.getMana(), max, !visible, pass);
        }
    }

    public static double maxMana(LocalPlayer player) {
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

    private void draw(GuiGraphics graphics, double mana, double max, boolean dimmed, Pass pass) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        ManaBarStyle style = effectiveStyle();
        Placement p = placement(screenWidth, screenHeight);
        float s = (float) p.scale();
        int width = p.width();
        int height = p.height();

        double shown = shownMana(mana);
        double fill = max > 0 ? Mth.clamp(shown / max, 0.0, 1.0) : 0.0;
        double debt = max > 0 && shown < 0 ? Mth.clamp(-shown / max, 0.0, 1.0) : 0.0;
        int fillColor = RunelessClientConfig.color(RunelessClientConfig.FILL_COLOR, 0xFF3A7BFF);
        int debtColor = RunelessClientConfig.color(RunelessClientConfig.DEBT_COLOR, 0xFFB02020);
        float flash = ManaFlash.intensity();
        int flashColor = RunelessClientConfig.color(RunelessClientConfig.FLASH_COLOR, 0xFFFF3030);
        ManaShapes.Shape shape = isShape(style) ? shapeFor(style) : null;

        graphics.pose().pushPose();
        graphics.pose().translate((float) p.x(), (float) p.y(), 0.0F);
        graphics.pose().scale(s, s, 1.0F);
        if (pass != Pass.TEXT) {
            if (shape != null) {
                ManaShapes.draw(graphics, shape, fill, debt, fillColor, debtColor);
                if (flash > 0.0F) {
                    ManaShapes.overlay(graphics, shape, withAlpha(flashColor, 0.75F * flash));
                }
            } else {
                switch (style) {
                    case MIKI -> drawMiki(graphics, width, height, fill, debt, fillColor, debtColor);
                    case VERTICAL -> drawVertical(graphics, width, height, fill, debt, fillColor, debtColor);
                    default -> drawThin(graphics, width, height, fill, debt, fillColor, debtColor);
                }
                if (flash > 0.0F) {
                    graphics.fill(0, 0, width, height, withAlpha(flashColor, 0.7F * flash));
                    graphics.renderOutline(-1, -1, width + 2, height + 2, withAlpha(flashColor, flash));
                }
            }
        }
        if (pass != Pass.SHAPE) {
            drawText(graphics, style, width, height, mana, max, s);
        }
        if (dimmed && pass != Pass.TEXT) {
            if (shape != null) {
                ManaShapes.overlay(graphics, shape, 0x90000000);
            } else {
                graphics.fill(-1, -1, width + 1, height + 1, 0x90000000);
            }
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
        return style == ManaBarStyle.THIN || style == ManaBarStyle.VERTICAL;
    }

    private static String textFor(TextMode mode, double mana, double max) {
        return switch (mode) {
            case CURRENT -> String.valueOf(Mth.floor(mana));
            case CURRENT_MAX -> Mth.floor(mana) + " / " + CostFormat.format(max);
            case PERCENT -> Math.round(mana / max * 100.0) + "%";
            default -> "";
        };
    }

    /**
     * Where the number goes for the diamond and band styles, or null for none. In a diamond the text is shrunk to
     * the free width; CURRENT_MAX / PERCENT fall back to CURRENT if that needs a scale below 0.75.
     */
    private TextLayout textLayout(ManaBarStyle style, double mana, double max) {
        TextMode mode = RunelessClientConfig.TEXT_MODE.get();
        if (mode == TextMode.NONE || max <= 0) {
            return null;
        }
        Font font = Minecraft.getInstance().font;
        ManaShapes.Shape shape = shapeFor(style);
        if (style == ManaBarStyle.MIKI_BAND) {
            boolean right = anchor.isMikiRight();
            double x = shape.textX + (right ? BAND_TEXT_GAP : -BAND_TEXT_GAP);
            return new TextLayout(textFor(mode, mana, max), x, shape.textY, BAND_TEXT_SCALE, right ? 1 : -1);
        }
        boolean half = atMikiDiamond();
        double available = half ? HALF_DIAMOND_TEXT_WIDTH : DIAMOND_TEXT_WIDTH;
        double textScale = fitScale(font, mode, mana, max, available);
        if (textScale < MIN_DIAMOND_TEXT_SCALE && mode != TextMode.CURRENT) {
            mode = TextMode.CURRENT;
            textScale = fitScale(font, mode, mana, max, available);
        }
        double x = half ? shape.textX : shape.width() / 2.0;
        return new TextLayout(textFor(mode, mana, max), x, shape.height() / 2.0, textScale, 0);
    }

    /** Scale that fits the widest text of the mode (current or full value, plus the outline) into the width. */
    private static double fitScale(Font font, TextMode mode, double mana, double max, double available) {
        int widest = Math.max(font.width(textFor(mode, mana, max)), font.width(textFor(mode, max, max))) + 2;
        return Math.min(1.0, available / widest);
    }

    private void drawText(GuiGraphics g, ManaBarStyle style, int w, int h, double mana, double max, float barScale) {
        float alpha = ManaTextFade.alpha();
        if (alpha < 0.02F) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int color = mana < 0 ? DEBT_TEXT_COLOR : TEXT_COLOR;
        if (isShape(style)) {
            TextLayout layout = textLayout(style, mana, max);
            if (layout == null) {
                return;
            }
            // Whole screen pixels per font pixel keep the shrunk number sharp, unless that would shrink it a lot more
            double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
            double pixels = guiScale * barScale * layout.scale();
            double snapped = Math.floor(pixels);
            double textScale = snapped >= 1.0 && snapped >= pixels * 0.75 ? snapped / (guiScale * barScale) : layout.scale();
            int textWidth = font.width(layout.text());
            g.pose().pushPose();
            g.pose().translate((float) layout.x(), (float) layout.y(), 0.0F);
            g.pose().scale((float) textScale, (float) textScale, 1.0F);
            int x = layout.align() == 0 ? -textWidth / 2 : layout.align() > 0 ? 1 : -textWidth - 1;
            drawNumber(g, font, layout.text(), x, -font.lineHeight / 2, color, alpha);
            g.pose().popPose();
            return;
        }
        TextMode mode = RunelessClientConfig.TEXT_MODE.get();
        if (mode == TextMode.NONE || max <= 0) {
            return;
        }
        String text = textFor(mode, mana, max);
        int textWidth = font.width(text);
        int x = (w - textWidth) / 2;
        int y = textAbove(style) ? -font.lineHeight - TEXT_ABOVE_GAP + 1 : (h - font.lineHeight) / 2 + 1;
        drawNumber(g, font, text, x, y, color, alpha);
    }

    /** The number with a dark 1 px outline (like Miki's numbers) or, if disabled, with the usual drop shadow. */
    private static void drawNumber(GuiGraphics g, Font font, String text, int x, int y, int color, float alpha) {
        int textColor = withAlpha(color, alpha);
        if (RunelessClientConfig.TEXT_OUTLINE.get()) {
            int outline = withAlpha(TEXT_OUTLINE_COLOR, alpha);
            g.drawString(font, text, x - 1, y, outline, false);
            g.drawString(font, text, x + 1, y, outline, false);
            g.drawString(font, text, x, y - 1, outline, false);
            g.drawString(font, text, x, y + 1, outline, false);
            g.drawString(font, text, x, y, textColor, false);
        } else {
            g.drawString(font, text, x, y, textColor, true);
        }
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
