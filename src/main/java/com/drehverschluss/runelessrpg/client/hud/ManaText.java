package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.client.ClientManaState;
import com.drehverschluss.runelessrpg.client.config.RunelessClientConfig;
import com.drehverschluss.runelessrpg.util.CostFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The mana number as its own HUD element ("Mana Text"): attached to the mana display (moves with it) or on its own
 * screen anchor, with its own offset, scale and visibility.
 * <p>
 * While attached without offset and with scale 1 ("automatic"), the number sits at the default place of the style
 * and is fitted there: smaller digits, CURRENT instead of CURRENT_MAX / PERCENT, hidden if it does not fit. Once moved
 * or scaled, it shows exactly the chosen text mode at the chosen size. The size always snaps to whole screen pixels
 * per font pixel, so both fonts stay sharp.
 */
public final class ManaText {
    public static final ManaText INSTANCE = new ManaText();

    public static final float MIN_SCALE = 0.25F;
    public static final float MAX_SCALE = 4.0F;
    public static final HudAnchor DEFAULT_ANCHOR = HudAnchor.BOTTOM_CENTER;

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DEBT_TEXT_COLOR = 0xFFFF9A9A;
    private static final int OUTLINE_COLOR = 0x000000;
    private static final int SHADOW_COLOR = 0x3F3F3F;
    /** Glyph height of the Minecraft font for digits, "/", "%", "k" (no descenders). */
    private static final int VANILLA_GLYPH_HEIGHT = 7;
    /** Corners of the glyphs stay inside the fill of the diamond (pixel centers at most 17 px from the center) … */
    private static final double DIAMOND_INSIDE_RADIUS = 18.0;
    /** … the outline may reach onto the dark frame ring. */
    private static final double DIAMOND_OUTLINE_RADIUS = 19.0;
    /** Gap between the number and the bar (THIN / VERTICAL: above it, band: beside the tip), in bar pixels. */
    private static final double TEXT_GAP = 1.0;

    private boolean visible = true;
    private boolean attached = true;
    private HudAnchor anchor = DEFAULT_ANCHOR;
    private int offsetX;
    private int offsetY;
    private float scale = 1.0F;

    /**
     * Where and how the number is drawn: box (glyphs plus outline / shadow) in GUI coordinates; {@code fontScale} =
     * GUI pixels per font pixel.
     */
    public record Layout(String text, boolean pixelFont, PixelFont.Size size, double x, double y, double fontScale,
                         double width, double height) {
        public ManaBar.Bounds bounds() {
            int x0 = Mth.floor(x);
            int y0 = Mth.floor(y);
            return new ManaBar.Bounds(x0, y0, Mth.ceil(x + width) - x0, Mth.ceil(y + height) - y0);
        }

        public boolean contains(double px, double py) {
            return px >= x && px < x + width && py >= y && py < y + height;
        }
    }

    /** Default place of the attached number: reference point, alignment of the box (0 / 0.5 / 1) and the bar's scale. */
    private record Reference(double x, double y, double alignX, double alignY, double barScale) {}

    /** Text size in font units: glyphs, box with outline / shadow, and the glyph offset inside the box. */
    private record Metrics(double glyphWidth, double glyphHeight, double boxWidth, double boxHeight, double pad) {}

    /** A font size to try: pixel digits of one size, or the Minecraft font; {@code scale} = GUI pixels per font pixel. */
    private record Candidate(PixelFont.Size size, double scale) {}

    private ManaText() {}

    // ------------------------------------------------------------------ layout state

    public void applyLayout(boolean visible, boolean attached, HudAnchor anchor, int offsetX, int offsetY, float scale) {
        this.visible = visible;
        this.attached = attached;
        this.anchor = anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public void resetLayout() {
        applyLayout(true, true, DEFAULT_ANCHOR, 0, 0, 1.0F);
    }

    /** Attached, not moved, not scaled: the number is fitted to the style's default place. */
    public boolean isAuto() {
        return attached && offsetX == 0 && offsetY == 0 && scale == 1.0F;
    }

    // ------------------------------------------------------------------ text and sizes

    /** Up to 999 in full, then shortened ("1.2k", "15k"), always rounded towards zero. */
    static String shortNumber(double value) {
        long n = (long) Math.floor(value);
        if (Math.abs(n) < 1000) {
            return Long.toString(n);
        }
        long tenths = n / 100;
        if (Math.abs(tenths) < 100) {
            String text = (tenths / 10) + (tenths % 10 != 0 ? "." + Math.abs(tenths % 10) : "");
            return (n < 0 && tenths / 10 == 0 ? "-" : "") + text + "k";
        }
        return (n / 1000) + "k";
    }

    static String text(TextMode mode, double mana, double max) {
        return switch (mode) {
            case CURRENT -> shortNumber(mana);
            case CURRENT_MAX -> shortNumber(mana) + " / " + (max < 1000 ? CostFormat.format(max) : shortNumber(max));
            case PERCENT -> Math.round(mana / max * 100.0) + "%";
            default -> "";
        };
    }

    private static double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    /** Nearest size at which one font pixel is a whole number of screen pixels (at least one). */
    public static double snapScale(double value) {
        double guiScale = guiScale();
        return Math.max(1, Math.round(value * guiScale)) / guiScale;
    }

    /** Position on the screen pixel grid. */
    private static double snap(double value) {
        double guiScale = guiScale();
        return Math.round(value * guiScale) / guiScale;
    }

    private static Metrics metrics(String text, boolean pixelFont, PixelFont.Size size, boolean outline) {
        double glyphWidth = pixelFont ? PixelFont.width(text, size) : Math.max(0, Minecraft.getInstance().font.width(text) - 1);
        double glyphHeight = pixelFont ? size.height : VANILLA_GLYPH_HEIGHT;
        double extra = outline ? 2 : 1;
        return new Metrics(glyphWidth, glyphHeight, glyphWidth + extra, glyphHeight + extra, outline ? 1 : 0);
    }

    private static Layout layoutAt(String text, boolean pixelFont, Candidate candidate, double x, double y, boolean outline) {
        Metrics m = metrics(text, pixelFont, candidate.size(), outline);
        return new Layout(text, pixelFont, candidate.size(), snap(x), snap(y), candidate.scale(),
                m.boxWidth() * candidate.scale(), m.boxHeight() * candidate.scale());
    }

    /**
     * Sizes tried at the default place: pixel digits in whole GUI pixels (large, then small where it helps), or the
     * Minecraft font from the bar's size down to half of it in whole screen pixels per font pixel.
     */
    private static List<Candidate> candidates(boolean pixelFont, ManaBarStyle style, double barScale) {
        List<Candidate> list = new ArrayList<>();
        if (pixelFont) {
            double pixel = Math.max(1, Math.round(barScale));
            if (style != ManaBarStyle.MIKI) {
                list.add(new Candidate(PixelFont.Size.LARGE, pixel));
            }
            if (style == ManaBarStyle.MIKI || style == ManaBarStyle.MIKI_DIAMOND) {
                list.add(new Candidate(PixelFont.Size.SMALL, pixel));
            }
        } else {
            double guiScale = guiScale();
            int largest = Math.max(1, (int) Math.floor(guiScale * barScale + 1.0E-6));
            int smallest = Math.max(1, (int) Math.ceil(guiScale * barScale / 2.0));
            for (int k = largest; k >= smallest; k--) {
                list.add(new Candidate(PixelFont.Size.LARGE, k / guiScale));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ placement

    /** Layout of the number right now, or null (no text mode, nothing to show or no room at the default place). */
    @Nullable
    public Layout layout(int screenWidth, int screenHeight, double mana, double max) {
        TextMode mode = RunelessClientConfig.TEXT_MODE.get();
        if (mode == TextMode.NONE || max <= 0) {
            return null;
        }
        boolean pixelFont = RunelessClientConfig.TEXT_FONT.get() == TextFont.PIXEL;
        boolean outline = RunelessClientConfig.TEXT_OUTLINE.get();
        if (isAuto()) {
            return autoLayout(mode, pixelFont, outline, screenWidth, screenHeight, mana, max);
        }
        String text = text(mode, mana, max);
        Candidate candidate = new Candidate(PixelFont.Size.LARGE, snapScale(scale));
        Metrics m = metrics(text, pixelFont, candidate.size(), outline);
        double width = m.boxWidth() * candidate.scale();
        double height = m.boxHeight() * candidate.scale();
        double x;
        double y;
        if (attached) {
            Reference ref = reference(screenWidth, screenHeight);
            x = ref.x() + offsetX * ref.barScale() - ref.alignX() * width;
            y = ref.y() + offsetY * ref.barScale() - ref.alignY() * height;
        } else if (anchor.isMiki()) {
            boolean right = anchor.isMikiRight();
            MikiCompat.Dock dock = anchor.isMikiLevel()
                    ? MikiCompat.levelDock(right, screenWidth, screenHeight)
                    : MikiCompat.dock(right, screenWidth, screenHeight);
            x = dock.x() + offsetX - (right ? 0 : width);
            y = dock.y() + offsetY - height / 2.0;
        } else {
            x = anchor.resolveX(screenWidth, Mth.ceil(width), offsetX);
            y = anchor.resolveY(screenHeight, Mth.ceil(height), offsetY);
        }
        x = Mth.clamp(x, 0, Math.max(0, screenWidth - width));
        y = Mth.clamp(y, 0, Math.max(0, screenHeight - height));
        return layoutAt(text, pixelFont, candidate, x, y, outline);
    }

    /** Default place of the number for the current style, relative to the mana display. */
    private Reference reference(int screenWidth, int screenHeight) {
        ManaBar bar = ManaBar.INSTANCE;
        ManaBar.Placement p = bar.placement(screenWidth, screenHeight);
        double s = p.scale();
        ManaBarStyle style = bar.effectiveStyle();
        switch (style) {
            case MIKI_DIAMOND -> {
                double centerX = p.x() + p.width() * s / 2.0;
                double centerY = p.y() + p.height() * s / 2.0;
                return new Reference(preferredDiamondX(bar, p, screenWidth, screenHeight), centerY, 0.5, 0.5, s);
            }
            case MIKI -> {
                return new Reference(p.x() + p.width() * s / 2.0, p.y() + p.height() * s / 2.0, 0.5, 0.5, s);
            }
            case MIKI_BAND -> {
                ManaShapes.Shape shape = bar.shapeFor(style);
                boolean right = bar.getAnchor().isMikiRight();
                double tipX = p.x() + shape.textX * s;
                return new Reference(tipX + (right ? TEXT_GAP : -TEXT_GAP) * s, p.y() + shape.textY * s, right ? 0.0 : 1.0, 0.5, s);
            }
            default -> {
                return new Reference(p.x() + p.width() * s / 2.0, p.y() - TEXT_GAP * s, 0.5, 1.0, s);
            }
        }
    }

    /**
     * Middle of the visible fill of the diamond on its center row (beside Miki's diamond for the half diamond). It is
     * computed for the docked position and then moved by the display's offset, so the number moves exactly with it.
     */
    private static double preferredDiamondX(ManaBar bar, ManaBar.Placement p, int screenWidth, int screenHeight) {
        double s = p.scale();
        double centerX = p.x() + p.width() * s / 2.0;
        if (!bar.atMikiDiamond()) {
            return centerX;
        }
        double dockedX = centerX - bar.getOffsetX();
        double dockedY = p.y() + p.height() * s / 2.0 - bar.getOffsetY();
        double inner = DIAMOND_INSIDE_RADIUS * s;
        double mikiScale = MikiCompat.scale(screenWidth);
        boolean right = bar.getAnchor().isMikiRight();
        double frameX = screenWidth / 2 + (right ? 1 : -1) * MikiCompat.FRAME_CENTER_X * mikiScale;
        double frameY = screenHeight + MikiCompat.FRAME_CENTER_Y * mikiScale;
        double frameEdge = MikiCompat.FRAME_RADIUS * mikiScale - Math.abs(dockedY - frameY);
        double docked = right ? (Math.max(dockedX - inner, frameX + frameEdge) + dockedX + inner) / 2.0
                : (dockedX - inner + Math.min(dockedX + inner, frameX - frameEdge)) / 2.0;
        return docked + bar.getOffsetX();
    }

    /** The number fitted to the default place of the style (see class comment). */
    @Nullable
    private Layout autoLayout(TextMode mode, boolean pixelFont, boolean outline, int screenWidth, int screenHeight, double mana, double max) {
        ManaBar bar = ManaBar.INSTANCE;
        ManaBar.Placement p = bar.placement(screenWidth, screenHeight);
        double s = p.scale();
        ManaBarStyle style = bar.effectiveStyle();
        TextMode[] modes = mode == TextMode.CURRENT ? new TextMode[]{mode} : new TextMode[]{mode, TextMode.CURRENT};
        List<Candidate> candidates = candidates(pixelFont, style, p.textScale());
        switch (style) {
            case MIKI_DIAMOND -> {
                for (TextMode m : modes) {
                    for (Candidate candidate : candidates) {
                        Layout layout = fitInDiamond(text(m, mana, max), pixelFont, candidate, outline, bar, p, screenWidth, screenHeight);
                        if (layout != null) {
                            return layout;
                        }
                    }
                }
                return null;
            }
            case MIKI -> {
                double barWidth = p.width() * s;
                double barHeight = p.height() * s;
                for (TextMode m : modes) {
                    String text = text(m, mana, max);
                    for (Candidate candidate : candidates) {
                        Metrics metrics = metrics(text, pixelFont, candidate.size(), outline);
                        double width = metrics.boxWidth() * candidate.scale();
                        double height = metrics.boxHeight() * candidate.scale();
                        // Inside the bar: not over its top / bottom edge, not over the end frames
                        if (height <= barHeight + 1.0E-6 && width <= barWidth - 2 * s) {
                            return layoutAt(text, pixelFont, candidate, p.x() + (barWidth - width) / 2.0,
                                    p.y() + (barHeight - height) / 2.0, outline);
                        }
                    }
                }
                return null;
            }
            default -> {
                String text = text(mode, mana, max);
                Candidate candidate = candidates.get(0);
                Metrics metrics = metrics(text, pixelFont, candidate.size(), outline);
                Reference ref = reference(screenWidth, screenHeight);
                return layoutAt(text, pixelFont, candidate, ref.x() - ref.alignX() * metrics.boxWidth() * candidate.scale(),
                        ref.y() - ref.alignY() * metrics.boxHeight() * candidate.scale(), outline);
            }
        }
    }

    /**
     * Finds a place for the number in the diamond: glyphs inside the fill, outline at most on the frame ring, and for
     * the half diamond never on Miki's diamond. The place is chosen for the docked diamond (as close to the middle of
     * the visible fill as fits) and then moved by the display's offset, so the number moves exactly with the diamond;
     * if it no longer fits there, there is no place (smaller digits or hiding follow).
     */
    @Nullable
    private static Layout fitInDiamond(String text, boolean pixelFont, Candidate candidate, boolean outline, ManaBar bar,
                                       ManaBar.Placement p, int screenWidth, int screenHeight) {
        Metrics m = metrics(text, pixelFont, candidate.size(), outline);
        double f = candidate.scale();
        double width = m.boxWidth() * f;
        double height = m.boxHeight() * f;
        double pad = m.pad() * f;
        double glyphWidth = m.glyphWidth() * f;
        double glyphHeight = m.glyphHeight() * f;
        double s = p.scale();
        double offsetX = bar.atMikiDiamond() ? bar.getOffsetX() : 0;
        double offsetY = bar.atMikiDiamond() ? bar.getOffsetY() : 0;
        double centerX = p.x() + p.width() * s / 2.0;
        double centerY = p.y() + p.height() * s / 2.0;
        double dockedX = centerX - offsetX;
        double dockedY = centerY - offsetY;
        double inner = DIAMOND_INSIDE_RADIUS * s;
        double outer = DIAMOND_OUTLINE_RADIUS * s;
        double[] frame = bar.atMikiDiamond() ? mikiFrame(bar, screenWidth, screenHeight) : null;
        double preferred = preferredDiamondX(bar, p, screenWidth, screenHeight) - offsetX;
        double step = 1.0 / guiScale();
        double y = snap(dockedY - glyphHeight / 2.0) - pad;
        double best = Double.NaN;
        double bestDistance = Double.MAX_VALUE;
        for (double x = snap(dockedX - outer - width); x <= dockedX + outer; x += step) {
            if (!fits(x, y, width, height, pad, glyphWidth, glyphHeight, dockedX, dockedY, inner, outer, frame)) {
                continue;
            }
            double distance = Math.abs(x + pad + glyphWidth / 2.0 - preferred);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = x;
            }
        }
        if (Double.isNaN(best)) {
            return null;
        }
        double x = best + offsetX;
        double movedY = y + offsetY;
        if (!fits(x, movedY, width, height, pad, glyphWidth, glyphHeight, centerX, centerY, inner, outer, frame)) {
            return null;
        }
        return layoutAt(text, pixelFont, candidate, x, movedY, outline);
    }

    /** Center x, center y and radius of Miki's diamond on the anchor's side. */
    private static double[] mikiFrame(ManaBar bar, int screenWidth, int screenHeight) {
        double mikiScale = MikiCompat.scale(screenWidth);
        return new double[]{
                screenWidth / 2 + (bar.getAnchor().isMikiRight() ? 1 : -1) * MikiCompat.FRAME_CENTER_X * mikiScale,
                screenHeight + MikiCompat.FRAME_CENTER_Y * mikiScale,
                MikiCompat.FRAME_RADIUS * mikiScale};
    }

    private static boolean fits(double x, double y, double width, double height, double pad, double glyphWidth, double glyphHeight,
                                double centerX, double centerY, double inner, double outer, @Nullable double[] frame) {
        double glyphX = x + pad;
        double glyphY = y + pad;
        return maxDistance(glyphX, glyphX + glyphWidth, glyphY, glyphY + glyphHeight, centerX, centerY) <= inner + 1.0E-6
                && maxDistance(x, x + width, y, y + height, centerX, centerY) <= outer + 1.0E-6
                && (frame == null || minDistance(x, x + width, y, y + height, frame[0], frame[1]) >= frame[2] - 1.0E-6);
    }

    /** Largest Manhattan distance of a box corner from a point (box inside a diamond if at most its radius). */
    private static double maxDistance(double x0, double x1, double y0, double y1, double px, double py) {
        return Math.max(Math.abs(x0 - px), Math.abs(x1 - px)) + Math.max(Math.abs(y0 - py), Math.abs(y1 - py));
    }

    /** Smallest Manhattan distance of a box from a point (box outside a diamond if at least its radius). */
    private static double minDistance(double x0, double x1, double y0, double y1, double px, double py) {
        return Math.max(0, Math.max(x0 - px, px - x1)) + Math.max(0, Math.max(y0 - py, py - y1));
    }

    // ------------------------------------------------------------------ drawing

    /** Draws the number; {@code preview} = editor (always drawn, dimmed when hidden, no fading). */
    public void render(GuiGraphics g, double mana, double max, boolean preview) {
        if (!visible && !preview) {
            return;
        }
        float alpha = preview ? 1.0F : ManaTextFade.alpha();
        if (alpha < 0.02F) {
            return;
        }
        Layout layout = layout(g.guiWidth(), g.guiHeight(), mana, max);
        if (layout == null) {
            return;
        }
        boolean outline = RunelessClientConfig.TEXT_OUTLINE.get();
        int color = withAlpha(mana < 0 ? DEBT_TEXT_COLOR : TEXT_COLOR, alpha);
        int dark = withAlpha(outline ? OUTLINE_COLOR : SHADOW_COLOR, alpha);
        float f = (float) layout.fontScale();
        g.pose().pushPose();
        g.pose().translate((float) layout.x(), (float) layout.y(), 0.0F);
        g.pose().scale(f, f, 1.0F);
        if (layout.pixelFont()) {
            PixelFont.draw(g, layout.text(), layout.size(), 0, 0, 1, color, dark, outline);
        } else {
            Font font = Minecraft.getInstance().font;
            if (outline) {
                g.drawString(font, layout.text(), 0, 1, dark, false);
                g.drawString(font, layout.text(), 2, 1, dark, false);
                g.drawString(font, layout.text(), 1, 0, dark, false);
                g.drawString(font, layout.text(), 1, 2, dark, false);
                g.drawString(font, layout.text(), 1, 1, color, false);
            } else {
                g.drawString(font, layout.text(), 0, 0, color, true);
            }
        }
        if (preview && !visible) {
            g.fill(-1, -1, Mth.ceil(layout.width() / f) + 1, Mth.ceil(layout.height() / f) + 1, 0x90000000);
        }
        g.pose().popPose();
    }

    private static int withAlpha(int color, float alpha) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (color & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ editor and tooltip

    /** Current values for the editor: the real ones if available, otherwise a sample (14 / 20). */
    private static double[] values() {
        LocalPlayer player = Minecraft.getInstance().player;
        double max = player != null ? ManaBar.maxMana(player) : 0.0;
        return max > 0.0 ? new double[]{ClientManaState.getMana(), max} : new double[]{14.0, 20.0};
    }

    @Nullable
    public Layout currentLayout(int screenWidth, int screenHeight) {
        double[] values = values();
        return layout(screenWidth, screenHeight, values[0], values[1]);
    }

    /** True if the visible number is at the point (for the hover tooltip). */
    public boolean hitTest(double x, double y, int screenWidth, int screenHeight) {
        if (!visible) {
            return false;
        }
        Layout layout = currentLayout(screenWidth, screenHeight);
        return layout != null && layout.contains(x, y);
    }

    /** Before the player moves or scales the number: keep the size it has right now (the automatic size). */
    public void beginEdit(int screenWidth, int screenHeight) {
        if (isAuto()) {
            Layout layout = currentLayout(screenWidth, screenHeight);
            if (layout != null) {
                scale = (float) Mth.clamp(layout.fontScale(), MIN_SCALE, MAX_SCALE);
            }
        }
    }

    /** Moves the number so that its box starts at (x, y): new offset to the default place, or to the nearest screen anchor. */
    public void moveTo(double x, double y, int screenWidth, int screenHeight) {
        Layout layout = currentLayout(screenWidth, screenHeight);
        if (layout == null) {
            return;
        }
        double width = layout.width();
        double height = layout.height();
        if (attached) {
            Reference ref = reference(screenWidth, screenHeight);
            offsetX = (int) Math.round((x + ref.alignX() * width - ref.x()) / ref.barScale());
            offsetY = (int) Math.round((y + ref.alignY() * height - ref.y()) / ref.barScale());
            return;
        }
        int boxX = (int) Math.round(Mth.clamp(x, 0, Math.max(0, screenWidth - width)));
        int boxY = (int) Math.round(Mth.clamp(y, 0, Math.max(0, screenHeight - height)));
        anchor = HudAnchor.nearest(boxX + Mth.ceil(width) / 2, boxY + Mth.ceil(height) / 2, screenWidth, screenHeight);
        offsetX = anchor.offsetX(boxX, screenWidth, Mth.ceil(width));
        offsetY = anchor.offsetY(boxY, screenHeight, Mth.ceil(height));
    }

    /** Attaches the number to the display (offset from its default place) or detaches it (nearest screen anchor); stays in place. */
    public void setAttached(boolean attach, int screenWidth, int screenHeight) {
        if (attach == attached) {
            return;
        }
        beginEdit(screenWidth, screenHeight);
        Layout layout = currentLayout(screenWidth, screenHeight);
        attached = attach;
        if (layout != null) {
            moveTo(layout.x(), layout.y(), screenWidth, screenHeight);
        }
    }

    /** One size step up or down: one screen pixel more or less per font pixel. */
    public void scaleStep(int direction, int screenWidth, int screenHeight) {
        beginEdit(screenWidth, screenHeight);
        double guiScale = guiScale();
        long pixels = Math.max(1, Math.round(snapScale(scale) * guiScale) + direction);
        scale = (float) Mth.clamp(pixels / guiScale, MIN_SCALE, MAX_SCALE);
    }

    // ------------------------------------------------------------------ accessors

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isAttached() {
        return attached;
    }

    public HudAnchor getAnchor() {
        return anchor;
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
}
