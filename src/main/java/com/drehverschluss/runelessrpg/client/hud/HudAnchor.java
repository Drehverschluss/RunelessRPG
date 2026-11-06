package com.drehverschluss.runelessrpg.client.hud;

import org.jetbrains.annotations.Nullable;

/**
 * Screen anchor of the mana bar. The final position is anchor point + offset, so the bar keeps its relative position
 * for every window size and GUI scale. {@link #MIKI_LEFT}/{@link #MIKI_RIGHT} dock at Miki's HUD (see {@link MikiCompat}).
 * <p>
 * Based on {@code HudAnchor} from Perilscope (same author, MIT), extended by the two Miki anchors.
 */
public enum HudAnchor {
    TOP_LEFT(Align.START, Align.START),
    TOP_CENTER(Align.CENTER, Align.START),
    TOP_RIGHT(Align.END, Align.START),
    CENTER_LEFT(Align.START, Align.CENTER),
    CENTER(Align.CENTER, Align.CENTER),
    CENTER_RIGHT(Align.END, Align.CENTER),
    BOTTOM_LEFT(Align.START, Align.END),
    BOTTOM_CENTER(Align.CENTER, Align.END),
    BOTTOM_RIGHT(Align.END, Align.END),
    /** Docked at the left (health) side of Miki's HUD, or above the health row without it. */
    MIKI_LEFT(null, null),
    /** Docked at the right (hunger) side of Miki's HUD, or above the hunger row without it. */
    MIKI_RIGHT(null, null);

    @Nullable
    private final Align horizontal;
    @Nullable
    private final Align vertical;

    HudAnchor(@Nullable Align horizontal, @Nullable Align vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public boolean isMiki() {
        return horizontal == null;
    }

    /** @return x of the left edge of the element (screen anchors only) */
    public int resolveX(int screenWidth, int elementWidth, int offsetX) {
        return horizontal.base(screenWidth, elementWidth) + offsetX;
    }

    /** @return y of the top edge of the element (screen anchors only) */
    public int resolveY(int screenHeight, int elementHeight, int offsetY) {
        return vertical.base(screenHeight, elementHeight) + offsetY;
    }

    /** @return the offset that places the left edge of the element at {@code x} (screen anchors only) */
    public int offsetX(int x, int screenWidth, int elementWidth) {
        return x - horizontal.base(screenWidth, elementWidth);
    }

    /** @return the offset that places the top edge of the element at {@code y} (screen anchors only) */
    public int offsetY(int y, int screenHeight, int elementHeight) {
        return y - vertical.base(screenHeight, elementHeight);
    }

    /** Picks the screen anchor of the 3×3 cell that contains the given point. */
    public static HudAnchor nearest(int pointX, int pointY, int screenWidth, int screenHeight) {
        Align horizontal = Align.fromPosition(pointX, screenWidth);
        Align vertical = Align.fromPosition(pointY, screenHeight);
        for (HudAnchor anchor : values()) {
            if (anchor.horizontal == horizontal && anchor.vertical == vertical) {
                return anchor;
            }
        }
        throw new IllegalStateException("No anchor for " + horizontal + "/" + vertical);
    }

    private enum Align {
        START, CENTER, END;

        static Align fromPosition(int position, int screenSize) {
            if (position * 3 < screenSize) {
                return START;
            }
            return position * 3 >= screenSize * 2 ? END : CENTER;
        }

        int base(int screenSize, int elementSize) {
            return switch (this) {
                case START -> 0;
                case CENTER -> (screenSize - elementSize) / 2;
                case END -> screenSize - elementSize;
            };
        }
    }
}
