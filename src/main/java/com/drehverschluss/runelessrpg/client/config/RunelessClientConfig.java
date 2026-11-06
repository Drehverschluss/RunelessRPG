package com.drehverschluss.runelessrpg.client.config;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.client.hud.HudAnchor;
import com.drehverschluss.runelessrpg.client.hud.ManaBar;
import com.drehverschluss.runelessrpg.client.hud.ManaBarStyle;
import com.drehverschluss.runelessrpg.client.hud.ManaText;
import com.drehverschluss.runelessrpg.client.hud.TextFont;
import com.drehverschluss.runelessrpg.client.hud.TextMode;
import com.drehverschluss.runelessrpg.client.hud.TextVisibility;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Client config ({@code config/runelessrpg-client.toml}): layout and look of the mana bar and the mana number. */
public final class RunelessClientConfig {
    private static final int MAX_OFFSET = 10000;

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue VISIBLE;
    public static final ModConfigSpec.EnumValue<HudAnchor> ANCHOR;
    public static final ModConfigSpec.IntValue OFFSET_X;
    public static final ModConfigSpec.IntValue OFFSET_Y;
    public static final ModConfigSpec.DoubleValue SCALE;
    public static final ModConfigSpec.EnumValue<ManaBarStyle> STYLE;
    public static final ModConfigSpec.EnumValue<TextMode> TEXT_MODE;
    public static final ModConfigSpec.EnumValue<TextFont> TEXT_FONT;
    public static final ModConfigSpec.BooleanValue ANIMATED_FILL;
    public static final ModConfigSpec.DoubleValue ANIMATION_SPEED;
    public static final ModConfigSpec.BooleanValue TEXT_OUTLINE;
    public static final ModConfigSpec.EnumValue<TextVisibility> TEXT_VISIBILITY;
    public static final ModConfigSpec.IntValue BAND_WIDTH;
    public static final ModConfigSpec.IntValue BAND_GAP;
    public static final ModConfigSpec.ConfigValue<String> FILL_COLOR;
    public static final ModConfigSpec.ConfigValue<String> DEBT_COLOR;
    public static final ModConfigSpec.BooleanValue DRAW_BEHIND_MIKI_HUD;
    public static final ModConfigSpec.BooleanValue HIDE_IN_CREATIVE;
    public static final ModConfigSpec.BooleanValue HIDE_IN_SPECTATOR;

    public static final ModConfigSpec.BooleanValue FLASH_ON_INSUFFICIENT_MANA;
    public static final ModConfigSpec.ConfigValue<String> FLASH_COLOR;

    public static final ModConfigSpec.BooleanValue TEXT_VISIBLE;
    public static final ModConfigSpec.BooleanValue TEXT_ATTACH;
    public static final ModConfigSpec.EnumValue<HudAnchor> TEXT_ANCHOR;
    public static final ModConfigSpec.IntValue TEXT_OFFSET_X;
    public static final ModConfigSpec.IntValue TEXT_OFFSET_Y;
    public static final ModConfigSpec.DoubleValue TEXT_SCALE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Mana bar. Position and scale can also be changed with the layout editor (key \"Edit HUD Layout\").").push("hud");
        VISIBLE = builder.comment("Show the mana bar").define("visible", true);
        ANCHOR = builder
                .comment("Screen anchor the bar is positioned relative to: the nine screen anchors, or",
                        "MIKI_LEFT / MIKI_RIGHT = docked at Miki's HUD (health / hunger side),",
                        "MIKI_LEVEL_LEFT / MIKI_LEVEL_RIGHT = beside the level box of Miki's HUD;",
                        "without Miki's HUD the Miki anchors place the bar beside the hotbar")
                .defineEnum("anchor", HudAnchor.MIKI_RIGHT);
        OFFSET_X = builder
                .comment("Horizontal offset from the anchor in GUI pixels (positive = right)")
                .defineInRange("offsetX", 0, -MAX_OFFSET, MAX_OFFSET);
        OFFSET_Y = builder
                .comment("Vertical offset from the anchor in GUI pixels (positive = down)")
                .defineInRange("offsetY", 0, -MAX_OFFSET, MAX_OFFSET);
        SCALE = builder
                .comment("Scale of the bar. At MIKI_LEVEL_LEFT / MIKI_LEVEL_RIGHT the MIKI and THIN bars keep their length",
                        "(level box to diamond) and only get thicker, up to 2.5")
                .defineInRange("scale", 1.0, ManaBar.MIN_SCALE, ManaBar.MAX_SCALE);
        STYLE = builder
                .comment("AUTO = MIKI_DIAMOND with Miki's HUD, otherwise THIN",
                        "THIN = narrow like the experience bar, MIKI = framed to match Miki's HUD, VERTICAL = upright, fills from the bottom",
                        "MIKI_DIAMOND = diamond behind Miki's hunger / health diamond (a whole diamond at other anchors or without Miki's HUD)",
                        "MIKI_BAND = band along the outer edges of Miki's hunger / health diamond (MIKI at other anchors or without Miki's HUD)")
                .defineEnum("style", ManaBarStyle.AUTO);
        BAND_WIDTH = builder.comment("Width of the MIKI_BAND style in GUI pixels").defineInRange("bandWidth", 7, 3, 16);
        BAND_GAP = builder.comment("Gap between the MIKI_BAND style and Miki's diamond in GUI pixels").defineInRange("bandGap", 2, 0, 8);
        ANIMATED_FILL = builder
                .comment("Slowly moving and shimmering fill texture (MIKI_DIAMOND, MIKI_BAND, MIKI); false = still texture")
                .define("animatedFill", true);
        ANIMATION_SPEED = builder
                .comment("Speed factor of the moving fill texture (two layers drifting slowly in different directions)")
                .defineInRange("animationSpeed", 1.0, 0.1, 3.0);
        FILL_COLOR = builder.comment("Fill color (#RRGGBB)").define("fillColor", "#3A7BFF", RunelessClientConfig::isColor);
        DEBT_COLOR = builder.comment("Color of negative mana (debt), #RRGGBB").define("debtColor", "#B02020", RunelessClientConfig::isColor);
        DRAW_BEHIND_MIKI_HUD = builder
                .comment("Draw the bar behind the hotbar and Miki's HUD (true) or in front of them (false).",
                        "MIKI_DIAMOND at Miki's diamond is always drawn behind it; the number is always drawn in front")
                .define("drawBehindMikiHud", true);
        HIDE_IN_CREATIVE = builder.comment("Hide the bar in creative mode").define("hideInCreative", true);
        HIDE_IN_SPECTATOR = builder.comment("Hide the bar in spectator mode").define("hideInSpectator", true);
        builder.pop();

        builder.comment("Mana number (\"Mana Text\"), its own HUD element. Can also be moved and scaled with the layout editor.").push("text");
        TEXT_VISIBLE = builder.comment("Show the mana number").define("visible", true);
        TEXT_ATTACH = builder
                .comment("true = positioned relative to the mana bar (moves with it); false = own screen anchor")
                .define("attachToBar", true);
        TEXT_ANCHOR = builder
                .comment("Screen anchor of the number when attachToBar = false (the nine screen anchors or the Miki anchors)")
                .defineEnum("anchor", ManaText.DEFAULT_ANCHOR);
        TEXT_OFFSET_X = builder
                .comment("Horizontal offset in GUI pixels: from the default place of the style (attached) or from the anchor")
                .defineInRange("offsetX", 0, -MAX_OFFSET, MAX_OFFSET);
        TEXT_OFFSET_Y = builder
                .comment("Vertical offset in GUI pixels: from the default place of the style (attached) or from the anchor")
                .defineInRange("offsetY", 0, -MAX_OFFSET, MAX_OFFSET);
        TEXT_SCALE = builder
                .comment("Size of the number; snaps to sizes where a font pixel is a whole number of screen pixels.",
                        "Attached with offset 0 and scale 1 the number is fitted to its place automatically")
                .defineInRange("scale", 1.0, ManaText.MIN_SCALE, ManaText.MAX_SCALE);
        TEXT_MODE = builder
                .comment("NONE, CURRENT (\"14\"), CURRENT_MAX (\"14 / 20\"), PERCENT (\"70%\"); from 1000 shortened (\"1.5k\")")
                .defineEnum("textMode", TextMode.CURRENT);
        TEXT_FONT = builder
                .comment("VANILLA = the Minecraft font, PIXEL = own small pixel digits")
                .defineEnum("textFont", TextFont.VANILLA);
        TEXT_OUTLINE = builder.comment("Dark 1 px outline around the number (otherwise a drop shadow)").define("textOutline", true);
        TEXT_VISIBILITY = builder
                .comment("ALWAYS = number always shown,",
                        "ON_CHANGE = number shown for 2 s after a notable change (spell cost, potion, leech, command, respawn, max mana)",
                                "            and after \"Missing Mana\"; regeneration and channel ticks do not show it")
                .defineEnum("textVisibility", TextVisibility.ALWAYS);
        builder.pop();

        builder.comment("Flash of the bar when a spell fails for lack of mana").push("flash");
        FLASH_ON_INSUFFICIENT_MANA = builder
                .comment("Flash the bar when a spell fails for lack of mana (Spell Engine's message is shown either way)")
                .define("flashOnInsufficientMana", true);
        FLASH_COLOR = builder.comment("Flash color (#RRGGBB)").define("flashColor", "#FF3030", RunelessClientConfig::isColor);
        builder.pop();

        SPEC = builder.build();
    }

    private RunelessClientConfig() {}

    /** Keys that moved from [hud] to [text] in 0.4.3. */
    private static final List<String> MOVED_TEXT_KEYS = List.of("textMode", "textFont", "textOutline", "textVisibility");

    /**
     * Moves the number settings of older config files from [hud] to [text] before NeoForge loads the file (it would
     * drop the unknown [hud] entries otherwise). Values already under [text] win.
     */
    public static void migrateLegacyFile() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(RunelessRPG.MODID + "-client.toml");
        if (!Files.exists(file)) {
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(file).preserveInsertionOrder().sync().build()) {
            config.load();
            List<String> moved = new ArrayList<>();
            for (String key : MOVED_TEXT_KEYS) {
                Object value = config.get("hud." + key);
                if (value == null) {
                    continue;
                }
                if (config.get("text." + key) == null) {
                    config.set("text." + key, value);
                }
                config.remove("hud." + key);
                moved.add(key + "=" + value);
            }
            if (!moved.isEmpty()) {
                config.save();
                RunelessRPG.LOGGER.info("Moved mana number settings from [hud] to [text] in {}: {}", file.getFileName(), moved);
            }
        } catch (RuntimeException e) {
            RunelessRPG.LOGGER.warn("Could not migrate {}, the number settings start with their defaults", file, e);
        }
    }

    /** Copies the layout values into the bar and the number. */
    public static void apply() {
        ManaBar.INSTANCE.applyLayout(ANCHOR.get(), OFFSET_X.get(), OFFSET_Y.get(), SCALE.get().floatValue(), VISIBLE.get());
        ManaText.INSTANCE.applyLayout(TEXT_VISIBLE.get(), TEXT_ATTACH.get(), TEXT_ANCHOR.get(), TEXT_OFFSET_X.get(),
                TEXT_OFFSET_Y.get(), TEXT_SCALE.get().floatValue());
    }

    /** Writes the layout of the bar and the number (and the editor options) back to the file. */
    public static void save() {
        ManaBar bar = ManaBar.INSTANCE;
        ANCHOR.set(bar.getAnchor());
        OFFSET_X.set(bar.getOffsetX());
        OFFSET_Y.set(bar.getOffsetY());
        SCALE.set(Math.round(bar.getScale() * 100.0) / 100.0);
        VISIBLE.set(bar.isVisible());
        ManaText text = ManaText.INSTANCE;
        TEXT_VISIBLE.set(text.isVisible());
        TEXT_ATTACH.set(text.isAttached());
        TEXT_ANCHOR.set(text.getAnchor());
        TEXT_OFFSET_X.set(text.getOffsetX());
        TEXT_OFFSET_Y.set(text.getOffsetY());
        TEXT_SCALE.set(Math.round(text.getScale() * 10000.0) / 10000.0);
        SPEC.save();
    }

    /** @return the parsed color with full alpha, or {@code fallback} for invalid values */
    public static int color(ModConfigSpec.ConfigValue<String> value, int fallback) {
        String text = value.get();
        if (!isColor(text)) {
            return fallback;
        }
        return 0xFF000000 | Integer.parseInt(((String) text).substring(1), 16);
    }

    private static boolean isColor(Object value) {
        return value instanceof String text && text.matches("#[0-9A-Fa-f]{6}");
    }
}
