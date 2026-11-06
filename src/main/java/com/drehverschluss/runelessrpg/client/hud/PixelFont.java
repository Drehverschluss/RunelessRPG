package com.drehverschluss.runelessrpg.client.hud;

import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.Map;

/**
 * Small pixel digits for the mana number (own glyphs, drawn as GUI-pixel rectangles, so they stay sharp at every GUI
 * scale). Characters: 0–9, "/", "%", "-", ".", "k" and space. Two sizes: LARGE (4×6) and SMALL (3×5).
 */
public final class PixelFont {
    public enum Size {
        LARGE(6), SMALL(5);

        final int height;

        Size(int height) {
            this.height = height;
        }
    }

    private static final Map<Character, String[]> LARGE = new HashMap<>();
    private static final Map<Character, String[]> SMALL = new HashMap<>();

    static {
        LARGE.put('0', new String[]{".##.", "#..#", "#..#", "#..#", "#..#", ".##."});
        LARGE.put('1', new String[]{".#.", "##.", ".#.", ".#.", ".#.", "###"});
        LARGE.put('2', new String[]{".##.", "#..#", "...#", "..#.", ".#..", "####"});
        LARGE.put('3', new String[]{"###.", "...#", ".##.", "...#", "...#", "###."});
        LARGE.put('4', new String[]{"#..#", "#..#", "####", "...#", "...#", "...#"});
        LARGE.put('5', new String[]{"####", "#...", "###.", "...#", "...#", "###."});
        LARGE.put('6', new String[]{".##.", "#...", "###.", "#..#", "#..#", ".##."});
        LARGE.put('7', new String[]{"####", "...#", "..#.", ".#..", ".#..", ".#.."});
        LARGE.put('8', new String[]{".##.", "#..#", ".##.", "#..#", "#..#", ".##."});
        LARGE.put('9', new String[]{".##.", "#..#", "#..#", ".###", "...#", ".##."});
        LARGE.put('/', new String[]{"...#", "..#.", "..#.", ".#..", ".#..", "#..."});
        LARGE.put('%', new String[]{"#..#", "...#", "..#.", ".#..", "#...", "#..#"});
        LARGE.put('-', new String[]{"...", "...", "###", "...", "...", "..."});
        LARGE.put('.', new String[]{".", ".", ".", ".", ".", "#"});
        LARGE.put('k', new String[]{"#...", "#..#", "#.#.", "##..", "#.#.", "#..#"});
        LARGE.put(' ', new String[]{"..", "..", "..", "..", "..", ".."});

        SMALL.put('0', new String[]{"###", "#.#", "#.#", "#.#", "###"});
        SMALL.put('1', new String[]{".#", "##", ".#", ".#", ".#"});
        SMALL.put('2', new String[]{"###", "..#", "###", "#..", "###"});
        SMALL.put('3', new String[]{"###", "..#", ".##", "..#", "###"});
        SMALL.put('4', new String[]{"#.#", "#.#", "###", "..#", "..#"});
        SMALL.put('5', new String[]{"###", "#..", "###", "..#", "###"});
        SMALL.put('6', new String[]{"###", "#..", "###", "#.#", "###"});
        SMALL.put('7', new String[]{"###", "..#", "..#", ".#.", ".#."});
        SMALL.put('8', new String[]{"###", "#.#", "###", "#.#", "###"});
        SMALL.put('9', new String[]{"###", "#.#", "###", "..#", "###"});
        SMALL.put('/', new String[]{"..#", "..#", ".#.", "#..", "#.."});
        SMALL.put('%', new String[]{"#.#", "..#", ".#.", "#..", "#.#"});
        SMALL.put('-', new String[]{"...", "...", "###", "...", "..."});
        SMALL.put('.', new String[]{".", ".", ".", ".", "#"});
        SMALL.put('k', new String[]{"#..", "#.#", "##.", "#.#", "#.#"});
        SMALL.put(' ', new String[]{".", ".", ".", ".", "."});
    }

    private PixelFont() {}

    private static String[] glyph(char c, Size size) {
        Map<Character, String[]> map = size == Size.LARGE ? LARGE : SMALL;
        String[] glyph = map.get(c);
        return glyph != null ? glyph : map.get(' ');
    }

    /** Width of the glyphs in font pixels (1 pixel spacing between characters, no outline). */
    public static int width(String text, Size size) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += glyph(text.charAt(i), size)[0].length() + (i > 0 ? 1 : 0);
        }
        return width;
    }

    /** Size of the drawn box in GUI pixels: glyphs plus the 1 px outline (or the shadow) on each side. */
    public static int boxWidth(String text, Size size, int pixel, boolean outline) {
        return (width(text, size) + (outline ? 2 : 1)) * pixel;
    }

    public static int boxHeight(Size size, int pixel, boolean outline) {
        return (size.height + (outline ? 2 : 1)) * pixel;
    }

    /**
     * Draws the text with its box's top left corner at (x, y); {@code pixel} = GUI pixels per font pixel. The dark
     * outline (or shadow) is drawn from a mask, so overlapping outlines never add up when the text fades.
     */
    public static void draw(GuiGraphics g, String text, Size size, int x, int y, int pixel, int color, int darkColor, boolean outline) {
        int width = width(text, size) + 2;
        int height = size.height + 2;
        boolean[] lit = new boolean[width * height];
        int cursor = 1;
        for (int i = 0; i < text.length(); i++) {
            String[] glyph = glyph(text.charAt(i), size);
            for (int row = 0; row < glyph.length; row++) {
                for (int col = 0; col < glyph[row].length(); col++) {
                    if (glyph[row].charAt(col) == '#') {
                        lit[(row + 1) * width + cursor + col] = true;
                    }
                }
            }
            cursor += glyph[0].length() + 1;
        }
        boolean[] dark = new boolean[width * height];
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                if (lit[py * width + px]) {
                    continue;
                }
                dark[py * width + px] = outline
                        ? isLit(lit, width, height, px - 1, py) || isLit(lit, width, height, px + 1, py)
                        || isLit(lit, width, height, px, py - 1) || isLit(lit, width, height, px, py + 1)
                        : isLit(lit, width, height, px - 1, py - 1);
            }
        }
        // Without outline the box starts at the glyphs (no margin on the left / top)
        int originX = outline ? x : x - pixel;
        int originY = outline ? y : y - pixel;
        fillMask(g, dark, width, height, originX, originY, pixel, darkColor);
        fillMask(g, lit, width, height, originX, originY, pixel, color);
    }

    private static boolean isLit(boolean[] lit, int width, int height, int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height && lit[y * width + x];
    }

    private static void fillMask(GuiGraphics g, boolean[] mask, int width, int height, int x, int y, int pixel, int color) {
        for (int row = 0; row < height; row++) {
            int start = -1;
            for (int col = 0; col <= width; col++) {
                boolean on = col < width && mask[row * width + col];
                if (on && start < 0) {
                    start = col;
                } else if (!on && start >= 0) {
                    g.fill(x + start * pixel, y + row * pixel, x + col * pixel, y + (row + 1) * pixel, color);
                    start = -1;
                }
            }
        }
    }
}
