package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Pixel shapes of the textured styles ({@link ManaBarStyle#MIKI_DIAMOND}, {@link ManaBarStyle#MIKI_BAND},
 * {@link ManaBarStyle#MIKI}). Every shape is a grid of GUI pixels (45° edges as one-pixel stairs). Each frame the
 * shape is composed on the CPU into a small texture (rim, tinted and moving fill texture, inner shadow, light edge,
 * surface line) and drawn with a single blit, so it stays sharp at every GUI scale.
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
    static final byte BAR_OUTER = 7;
    static final byte BAR_LIGHT = 8;
    static final byte BAR_MID = 9;
    static final byte BAR_DARK = 10;

    private static final int[] PALETTE = {
            0, 0xFF5E315B, 0xFFBA6156, 0xFFF2A65E, 0xFF8C3F5D, 0xFF10141F, 0xFF0E1619,
            0xFF140F0E, 0xFFC88A6A, 0xFF9A5B48, 0xFF5E3127};

    private static final byte EDGE_NONE = 0;
    private static final byte EDGE_LIGHT = 1;
    private static final byte EDGE_DARK = 2;
    private static final byte EDGE_NEUTRAL = 3;
    private static final byte EDGE_SECOND = 4;

    /** Length of each band arm, measured along the diamond edge (rows from the tip). */
    private static final int BAND_REACH = 22;

    private static final ResourceLocation FILL_TEXTURE = ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "textures/gui/mana_fill.png");
    private static final int TEXTURE_SIZE = 32;
    /** Movement of the fill texture in pixels per second (to the left and upwards). */
    private static final double DRIFT_X = 1.0;
    private static final double DRIFT_Y = 2.5;

    private static Shape diamondRight;
    private static Shape diamondLeft;
    private static final Map<Integer, Shape> BANDS = new HashMap<>();
    private static final Map<Integer, Shape> BARS = new HashMap<>();
    private static final Map<Shape, Canvas> CANVASES = new IdentityHashMap<>();
    private static float[] fillTexture;
    private static int canvasCount;

    private ManaShapes() {}

    /** The texture a shape is composed into each frame. */
    private record Canvas(DynamicTexture texture, ResourceLocation location) {}

    /**
     * A pixel shape. {@code originX/originY}: top left corner in Miki coordinates (bands only); {@code textX/textY}:
     * reference point for the number (local pixels); {@code horizontalFill}: fills from left to right instead of
     * from the bottom.
     */
    public static final class Shape {
        final int width;
        final int height;
        final byte[] kinds;
        final byte[] edges;
        final int[] rowMin;
        final int[] rowMax;
        final boolean horizontalFill;
        final int insideMin;
        final int insideMax;
        public final double originX;
        public final double originY;
        public final double textX;
        public final double textY;

        Shape(int width, int height, byte[] kinds, double originX, double originY, double textX, double textY, boolean horizontalFill) {
            this.width = width;
            this.height = height;
            this.kinds = kinds;
            this.originX = originX;
            this.originY = originY;
            this.textX = textX;
            this.textY = textY;
            this.horizontalFill = horizontalFill;
            rowMin = new int[height];
            rowMax = new int[height];
            int min = Integer.MAX_VALUE;
            int max = -1;
            for (int y = 0; y < height; y++) {
                rowMin[y] = width;
                rowMax[y] = -1;
                for (int x = 0; x < width; x++) {
                    byte kind = kinds[y * width + x];
                    if (kind != EMPTY) {
                        rowMin[y] = Math.min(rowMin[y], x);
                        rowMax[y] = Math.max(rowMax[y], x);
                    }
                    if (kind == INSIDE) {
                        int along = horizontalFill ? x : y;
                        min = Math.min(min, along);
                        max = Math.max(max, along);
                    }
                }
            }
            insideMin = min;
            insideMax = max;
            edges = computeEdges();
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

        private boolean inside(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height && kinds[y * width + x] == INSIDE;
        }

        /**
         * Inner shadow / light edge of the inside: the outermost pixels get a side from the outward normal (sum of
         * the directions to non-inside pixels around them): up/left = light, down/right = dark, diagonal = neutral;
         * the second row inwards gets a soft shadow.
         */
        private byte[] computeEdges() {
            byte[] result = new byte[kinds.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!inside(x, y)) {
                        continue;
                    }
                    if (inside(x - 1, y) && inside(x + 1, y) && inside(x, y - 1) && inside(x, y + 1)) {
                        continue;
                    }
                    int nx = 0;
                    int ny = 0;
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            if (!inside(x + dx, y + dy)) {
                                nx += dx;
                                ny += dy;
                            }
                        }
                    }
                    int total = Math.abs(nx) + Math.abs(ny);
                    double side = total == 0 ? 0.0 : (nx + ny) / (double) total;
                    result[y * width + x] = side < -0.34 ? EDGE_LIGHT : side > 0.34 ? EDGE_DARK : EDGE_NEUTRAL;
                }
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = y * width + x;
                    if (inside(x, y) && result[i] == EDGE_NONE && (isEdge(result, x - 1, y) || isEdge(result, x + 1, y)
                            || isEdge(result, x, y - 1) || isEdge(result, x, y + 1))) {
                        result[i] = EDGE_SECOND;
                    }
                }
            }
            return result;
        }

        private boolean isEdge(byte[] edges, int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height && edges[y * width + x] != EDGE_NONE && edges[y * width + x] != EDGE_SECOND;
        }

        Shape mirrored() {
            byte[] flipped = new byte[kinds.length];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    flipped[y * width + (width - 1 - x)] = kinds[y * width + x];
                }
            }
            return new Shape(width, height, flipped, -(originX + width), originY, width - textX, textY, horizontalFill);
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
            diamondRight = new Shape(size, size, kinds, 0, 0, size / 2.0, size / 2.0, false);
            diamondLeft = diamondRight.mirrored();
        }
        return right ? diamondRight : diamondLeft;
    }

    /** The framed MIKI bar, filling from left to right. */
    public static Shape bar(int width, int height) {
        int key = (width << 8) | height;
        Shape cached = BARS.get(key);
        if (cached != null) {
            return cached;
        }
        byte[] kinds = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte kind;
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                    // Small rivets at both ends, like the studs on Miki's frames
                    boolean rivet = (x == 0 || x == width - 1) && (y == height / 2 - 1 || y == height / 2);
                    kind = rivet ? BAR_LIGHT : BAR_OUTER;
                } else if (y == 1) {
                    kind = BAR_LIGHT;
                } else if (y == height - 2) {
                    kind = BAR_DARK;
                } else if (x == 1 || x == width - 2) {
                    kind = BAR_MID;
                } else {
                    kind = INSIDE;
                }
                kinds[y * width + x] = kind;
            }
        }
        Shape shape = new Shape(width, height, kinds, 0, 0, width / 2.0, height / 2.0, true);
        BARS.put(key, shape);
        return shape;
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
        Shape shape = new Shape(bw, bh, kinds, minX, minY, frameX + outer - minX, frameY - minY, false);
        BANDS.put(key, shape);
        return shape;
    }

    /**
     * Draws the shape at the origin of the current pose. The inside fills from the bottom (or from the left) with the
     * tinted fill texture ({@code fill} 0 … 1); debt replaces the fill with {@code debtColor}.
     */
    static void draw(GuiGraphics g, Shape shape, double fill, double debt, int fillColor, int debtColor, boolean animated) {
        Canvas canvas = canvas(shape);
        NativeImage image = canvas.texture().getPixels();
        if (image == null) {
            return;
        }
        float[] texture = fillTexture();
        double time = animated ? Util.getMillis() / 1000.0 : 0.0;
        int shiftX = (int) Math.floor(time * DRIFT_X);
        int shiftY = (int) Math.floor(time * DRIFT_Y);

        boolean inDebt = debt > 0.0;
        int color = inDebt ? debtColor : fillColor;
        int steps = shape.insideMax - shape.insideMin + 1;
        int filled = shape.insideMax < 0 ? 0 : Mth.ceil(steps * (inDebt ? debt : fill));
        // Vertical: rows from firstFilled down to the bottom; horizontal: columns up to lastFilled
        int firstFilled = shape.insideMax + 1 - filled;
        int lastFilled = shape.insideMin + filled - 1;
        int emptyTint = darker(color, 0.82F);

        for (int y = 0; y < shape.height; y++) {
            for (int x = 0; x < shape.width; x++) {
                int i = y * shape.width + x;
                byte kind = shape.kinds[i];
                int argb;
                if (kind == EMPTY) {
                    argb = 0;
                } else if (kind != INSIDE) {
                    argb = PALETTE[kind];
                } else {
                    float t = texture[((y + shiftY) & (TEXTURE_SIZE - 1)) * TEXTURE_SIZE + ((x + shiftX) & (TEXTURE_SIZE - 1))];
                    boolean isFilled = shape.horizontalFill ? x <= lastFilled : y >= firstFilled;
                    if (isFilled) {
                        float factor = 0.55F + 0.7F * t;
                        // Slightly lighter at the top, darker at the bottom
                        factor *= 1.08F - 0.2F * (y / (float) Math.max(1, shape.height - 1));
                        if (animated) {
                            factor *= 1.0F + 0.06F * (float) Math.sin(time * 2.0 + (x - y) * 0.35);
                        }
                        argb = scale(color, factor);
                        boolean surface = shape.horizontalFill ? x == lastFilled && filled < steps : y == firstFilled && filled < steps;
                        if (surface) {
                            argb = lighter(argb, 0.55F);
                        }
                    } else {
                        argb = scale(emptyTint, 0.7F + 0.6F * t);
                    }
                    argb = switch (shape.edges[i]) {
                        case EDGE_LIGHT -> lighter(argb, 0.3F);
                        case EDGE_DARK -> darker(argb, 0.5F);
                        case EDGE_NEUTRAL -> darker(argb, 0.35F);
                        case EDGE_SECOND -> darker(argb, 0.18F);
                        default -> argb;
                    };
                }
                image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(argb));
            }
        }
        canvas.texture().upload();
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(canvas.location(), 0, 0, 0.0F, 0.0F, shape.width, shape.height, shape.width, shape.height);
        RenderSystem.disableBlend();
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

    private static Canvas canvas(Shape shape) {
        Canvas canvas = CANVASES.get(shape);
        if (canvas == null) {
            DynamicTexture texture = new DynamicTexture(shape.width, shape.height, true);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "dynamic/mana_shape_" + canvasCount++);
            Minecraft.getInstance().getTextureManager().register(location, texture);
            canvas = new Canvas(texture, location);
            CANVASES.put(shape, canvas);
        }
        return canvas;
    }

    /** Brightness values 0 … 1 of the tileable fill texture (own grayscale artwork, tinted at runtime). */
    private static float[] fillTexture() {
        if (fillTexture == null) {
            float[] values = new float[TEXTURE_SIZE * TEXTURE_SIZE];
            java.util.Arrays.fill(values, 0.6F);
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(FILL_TEXTURE);
            if (resource.isPresent()) {
                try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
                    for (int y = 0; y < TEXTURE_SIZE; y++) {
                        for (int x = 0; x < TEXTURE_SIZE; x++) {
                            int abgr = image.getPixelRGBA(x % image.getWidth(), y % image.getHeight());
                            values[y * TEXTURE_SIZE + x] = (abgr & 0xFF) / 255.0F;
                        }
                    }
                } catch (Exception e) {
                    RunelessRPG.LOGGER.warn("Could not load {}, using a flat fill", FILL_TEXTURE, e);
                }
            }
            fillTexture = values;
        }
        return fillTexture;
    }

    private static int scale(int color, float factor) {
        int r = Mth.clamp((int) (FastColor.ARGB32.red(color) * factor), 0, 255);
        int g = Mth.clamp((int) (FastColor.ARGB32.green(color) * factor), 0, 255);
        int b = Mth.clamp((int) (FastColor.ARGB32.blue(color) * factor), 0, 255);
        return FastColor.ARGB32.color(255, r, g, b);
    }

    private static int lighter(int color, float amount) {
        return FastColor.ARGB32.lerp(amount, color, 0xFFFFFFFF);
    }

    private static int darker(int color, float amount) {
        return FastColor.ARGB32.lerp(amount, color, 0xFF000000);
    }
}
