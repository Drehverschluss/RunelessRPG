package com.drehverschluss.runelessrpg.client.config;

import com.drehverschluss.runelessrpg.client.hud.HudAnchor;
import com.drehverschluss.runelessrpg.client.hud.ManaBar;
import com.drehverschluss.runelessrpg.client.hud.ManaBarStyle;
import com.drehverschluss.runelessrpg.client.hud.TextMode;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config ({@code config/runelessrpg-client.toml}): layout and look of the mana bar. */
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
    public static final ModConfigSpec.ConfigValue<String> FILL_COLOR;
    public static final ModConfigSpec.ConfigValue<String> DEBT_COLOR;
    public static final ModConfigSpec.BooleanValue DRAW_BEHIND_MIKI_HUD;
    public static final ModConfigSpec.BooleanValue HIDE_IN_CREATIVE;
    public static final ModConfigSpec.BooleanValue HIDE_IN_SPECTATOR;

    public static final ModConfigSpec.BooleanValue FLASH_ON_INSUFFICIENT_MANA;
    public static final ModConfigSpec.ConfigValue<String> FLASH_COLOR;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Mana bar. Position and scale can also be changed with the layout editor (key \"Edit HUD Layout\").").push("hud");
        VISIBLE = builder.comment("Show the mana bar").define("visible", true);
        ANCHOR = builder
                .comment("Screen anchor the bar is positioned relative to: the nine screen anchors, or",
                        "MIKI_LEFT / MIKI_RIGHT = docked at Miki's HUD (health / hunger side); without Miki's HUD above the health / hunger row")
                .defineEnum("anchor", HudAnchor.MIKI_RIGHT);
        OFFSET_X = builder
                .comment("Horizontal offset from the anchor in GUI pixels (positive = right)")
                .defineInRange("offsetX", 0, -MAX_OFFSET, MAX_OFFSET);
        OFFSET_Y = builder
                .comment("Vertical offset from the anchor in GUI pixels (positive = down)")
                .defineInRange("offsetY", 0, -MAX_OFFSET, MAX_OFFSET);
        SCALE = builder.comment("Scale of the bar").defineInRange("scale", 1.0, ManaBar.MIN_SCALE, ManaBar.MAX_SCALE);
        STYLE = builder
                .comment("AUTO = MIKI with Miki's HUD, otherwise THIN",
                        "THIN = narrow like the experience bar, MIKI = framed to match Miki's HUD, VERTICAL = upright, fills from the bottom")
                .defineEnum("style", ManaBarStyle.AUTO);
        TEXT_MODE = builder
                .comment("Number on the bar: NONE, CURRENT (\"14\"), CURRENT_MAX (\"14 / 20\"), PERCENT (\"70%\")")
                .defineEnum("textMode", TextMode.CURRENT_MAX);
        FILL_COLOR = builder.comment("Fill color (#RRGGBB)").define("fillColor", "#3A7BFF", RunelessClientConfig::isColor);
        DEBT_COLOR = builder.comment("Color of negative mana (debt), #RRGGBB").define("debtColor", "#B02020", RunelessClientConfig::isColor);
        DRAW_BEHIND_MIKI_HUD = builder
                .comment("Draw the bar behind the hotbar and Miki's HUD (true) or in front of them (false)")
                .define("drawBehindMikiHud", true);
        HIDE_IN_CREATIVE = builder.comment("Hide the bar in creative mode").define("hideInCreative", true);
        HIDE_IN_SPECTATOR = builder.comment("Hide the bar in spectator mode").define("hideInSpectator", true);
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

    /** Copies the layout values into the bar. */
    public static void apply() {
        ManaBar.INSTANCE.applyLayout(ANCHOR.get(), OFFSET_X.get(), OFFSET_Y.get(), SCALE.get().floatValue(), VISIBLE.get());
    }

    /** Writes the layout of the bar (and the editor options) back to the file. */
    public static void save() {
        ManaBar bar = ManaBar.INSTANCE;
        ANCHOR.set(bar.getAnchor());
        OFFSET_X.set(bar.getOffsetX());
        OFFSET_Y.set(bar.getOffsetY());
        SCALE.set(Math.round(bar.getScale() * 100.0) / 100.0);
        VISIBLE.set(bar.isVisible());
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
