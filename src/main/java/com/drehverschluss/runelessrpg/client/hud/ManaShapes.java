package com.drehverschluss.runelessrpg.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Pixel shapes of the styles {@link ManaBarStyle#MIKI_DIAMOND} and {@link ManaBarStyle#MIKI_BAND}. Every shape is a
 * grid of GUI pixels (45° edges as one-pixel stairs), drawn as horizontal runs so it stays sharp at every GUI scale.
 * Colors follow Miki's palette (copper rim, dark frame, dark inside); no Miki textures are used.
 */
public final class ManaShapes {
    /** Size of the diamond: even, with two-pixel tips like Miki's small diamonds (half diagonal 22). */
    public static final int DIAMOND_SIZE = 44;

    static final byte EMPTY = 0;
    static final byte OUTLINE = 1;
    static final byte COPPER = 2;
    static final byte COPPER_LIGHT = 3;
    static final byte COPPER_SHADOW = 4;
    static final byte FRAME = 5;
    static final byte INSIDE = 6;

    private static final int OUTLINE_COLOR = 0xFF5E315B;
    private static final int COPPER_COLOR = 0xFFBA6156;
    private static final int COPPER_LIGHT_COLOR = 0xFFF2A65E;
    private static final int COPPER_SHADOW_COLOR = 0xFF8C3F5D;
    private static final int FRAME_COLOR = 0xFF10141F;
    private static final int INSIDE_COLOR = 0xFF0E1619;

    /** Length of each band arm, measured along the diamond edge (rows from the tip). */
    private static final int BAND_REACH = 22;

    private static Shape diamondRight;
    private static Shape diamondLeft;
    private static final Map<Integer, Shape> BANDS = new HashMap<>();

    private ManaShapes() {}

    /**
     * A pixel shape. {@code originX/originY}: top left corner in Miki coordinates (bands only); {@code textX/textY}:
     * reference point for the number (local pixels).
     */
    public static final class Shape {
        final int width;
        final int height;
        final byte[] kinds;
        final int[] rowMin;
        final int[] rowMax;
        public final double originX;
        public final double originY;
        public final double textX;
        public final double textY;

        Shape(int width, int height, byte[] kinds, double originX, double originY, double textX, double textY) {
            this.width = width;
            this.height = height;
            this.kinds = kinds;
            this.originX = originX;
            this.originY = originY;
            this.textX = textX;
            this.textY = textY;
            rowMin = new int[height];
            rowMax = new int[height];
            for (int y = 0; y < height; y++) {
                rowMin[y] = width;
                rowMax[y] = -1;
                for (int x = 0; x < width; x++) {
                    if (kinds[y * width + x] != EMPTY) {
                        rowMin[y] = Math.min(rowMin[y], x);
                        rowMax[y] = Math.max(rowMax[y], x);
                    }
                }
            }
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public boolean contains(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height && kinds[y * width + x] != EMPTY;
        }

        Shape mirrored() {
            byte[] flipped = new byte[kinds.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    flipped[y * width + (width - 1 - x)] = kinds[y * width + x];
                }
            }
            return new Shape(width, height, flipped, -(originX + width), originY, width - textX, textY);
        }
    }

    /** The diamond; {@code right} = outer half points right (hunger side). */
    public static Shape diamond(boolean right) {
        if (diamondRight == null) {
            int size = DIAMOND_SIZE;
            double center = (size - 1) / 2.0;
            byte[] kinds = new byte[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int d = (int) Math.round(Math.abs(x - center) + Math.abs(y - center));
                    boolean upper = y < size / 2;
                    byte kind;
                    if (d > 22) {
                        kind = EMPTY;
                    } else if (d == 22) {
                        kind = OUTLINE;
                    } else if (d == 21) {
                        kind = COPPER;
                    } else if (d == 20) {
                        kind = upper ? COPPER_LIGHT : COPPER;
                    } else if (d == 19) {
                        kind = COPPER_SHADOW;
                    } else if (d == 18) {
                        kind = FRAME;
                    } else {
                        kind = INSIDE;
                    }
                    kinds[y * size + x] = kind;
                }
            }
            // Number of the half diamond: middle of the part that shows beside Miki's diamond (x 168 … 185 at the tip rows)
            diamondRight = new Shape(size, size, kinds, 0, 0, 31.5, size / 2.0);
            diamondLeft = diamondRight.mirrored();
        }
        return right ? diamondRight : diamondLeft;
    }

    /**
     * The band along the outer edges of the hunger diamond ({@code right}) or the health diamond, {@code width} pixels
     * wide, {@code gap} pixels away from the diamond frame. Its arms end with diagonal caps {@value #BAND_REACH} rows
     * above and below the tip.
     */
    public static Shape band(int width, int gap, boolean right) {
        int key = (width << 8) | (gap << 1) | (right ? 1 : 0);
        Shape cached = BANDS.get(key);
        if (cached != null) {
            return cached;
        }
        if (!right) {
            Shape mirrored = band(width, gap, true).mirrored();
            BANDS.put(key, mirrored);
            return mirrored;
        }
        double frameX = MikiCompat.FRAME_CENTER_X;
        double frameY = MikiCompat.FRAME_CENTER_Y;
        int inner = (int) MikiCompat.FRAME_RADIUS + 1 + gap;
        int outer = (int) MikiCompat.FRAME_RADIUS + gap + width;
        // Arm end: where the middle of the band is BAND_REACH rows away from the tip
        int minAlong = (int) Math.ceil(MikiCompat.FRAME_RADIUS + gap + width / 2.0 - 2 * BAND_REACH);
        int left = (int) frameX;
        int right2 = (int) frameX + outer + 1;
        int top = (int) frameY - outer - 1;
        int bottom = (int) frameY + outer + 1;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int w = right2 - left;
        int h = bottom - top;
        byte[] grid = new byte[w * h];
        for (int y = top; y < bottom; y++) {
            for (int x = left; x < right2; x++) {
                double u = x + 0.5 - frameX;
                double v = Math.abs(y + 0.5 - frameY);
                int d = (int) Math.round(u + v);
                int along = (int) Math.round(u - v);
                if (u <= 0 || d < inner || d > outer || along < minAlong) {
                    continue;
                }
                int ring = d - inner;
                byte kind;
                if (along - minAlong < 2 || ring == 0 || ring == width - 1) {
                    kind = COPPER;
                } else if (width >= 6 && (ring == 1 || ring == width - 2)) {
                    kind = FRAME;
                } else {
                    kind = INSIDE;
                }
                grid[(y - top) * w + (x - left)] = kind;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        int bw = maxX - minX + 1;
        int bh = maxY - minY + 1;
        byte[] kinds = new byte[bw * bh];
        for (int y = 0; y < bh; y++) {
            System.arraycopy(grid, (y + minY - top) * w + (minX - left), kinds, y * bw, bw);
        }
        // Number: beside the outer edge of the tip
        Shape shape = new Shape(bw, bh, kinds, minX, minY, frameX + outer - minX, frameY - minY);
        BANDS.put(key, shape);
        return shape;
    }

    /**
     * Draws the shape at the origin of the current pose. The inside fills from the bottom row by row
     * ({@code fill} 0 … 1), debt replaces the fill with {@code debtColor}.
     */
    static void draw(GuiGraphics g, Shape shape, double fill, double debt, int fillColor, int debtColor) {
        int top = -1;
        int bottom = -1;
        for (int y = 0; y < shape.height; y++) {
            for (int x = shape.rowMin[y]; x <= shape.rowMax[y]; x++) {
                if (shape.kinds[y * shape.width + x] == INSIDE) {
                    if (top < 0) {
                        top = y;
                    }
                    bottom = y;
                    break;
                }
            }
        }
        int rows = top < 0 ? 0 : bottom - top + 1;
        boolean inDebt = debt > 0.0;
        int filledRows = Mth.ceil(rows * (inDebt ? debt : fill));
        int firstFilled = bottom + 1 - filledRows;
        int color = inDebt ? debtColor : fillColor;
        int light = lerp(0.3F, color, 0xFFFFFFFF);
        int dark = lerp(0.35F, color, 0xFF000000);
        int surface = lerp(0.55F, color, 0xFFFFFFFF);

        for (int y = 0; y < shape.height; y++) {
            int rowColor;
            if (filledRows > 0 && y >= firstFilled && y <= bottom) {
                rowColor = y == firstFilled && filledRows < rows && !inDebt ? surface
                        : lerp(rows > 1 ? (y - top) / (float) (rows - 1) : 0.0F, light, dark);
            } else {
                rowColor = INSIDE_COLOR;
            }
            int runStart = -1;
            int runColor = 0;
            for (int x = shape.rowMin[y]; x <= shape.rowMax[y] + 1; x++) {
                int c = x <= shape.rowMax[y] ? colorOf(shape.kinds[y * shape.width + x], rowColor) : 0;
                if (runStart >= 0 && c != runColor) {
                    g.fill(runStart, y, x, y + 1, runColor);
                    runStart = -1;
                }
                if (runStart < 0 && c != 0) {
                    runStart = x;
                    runColor = c;
                }
            }
        }
    }

    /** Covers the whole shape (rim and inside) with a color, for the flash and the editor's "hidden" state. */
    static void overlay(GuiGraphics g, Shape shape, int color) {
        for (int y = 0; y < shape.height; y++) {
            int runStart = -1;
            for (int x = shape.rowMin[y]; x <= shape.rowMax[y] + 1; x++) {
                boolean inside = x <= shape.rowMax[y] && shape.kinds[y * shape.width + x] != EMPTY;
                if (inside && runStart < 0) {
                    runStart = x;
                } else if (!inside && runStart >= 0) {
                    g.fill(runStart, y, x, y + 1, color);
                    runStart = -1;
                }
            }
        }
    }

    private static int colorOf(byte kind, int insideColor) {
        return switch (kind) {
            case OUTLINE -> OUTLINE_COLOR;
            case COPPER -> COPPER_COLOR;
            case COPPER_LIGHT -> COPPER_LIGHT_COLOR;
            case COPPER_SHADOW -> COPPER_SHADOW_COLOR;
            case FRAME -> FRAME_COLOR;
            case INSIDE -> insideColor;
            default -> 0;
        };
    }

    private static int lerp(float amount, int from, int to) {
        return FastColor.ARGB32.lerp(amount, from, to);
    }
}
