package com.drehverschluss.runelessrpg.client.hud;

import com.drehverschluss.runelessrpg.RunelessRPG;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.spell_engine.client.input.SpellHotbar;

import java.lang.reflect.Method;

/**
 * Position of Miki's HUD. Miki draws its layout around the bottom center and shrinks it to fit the screen:
 * {@code halfWidth = max(175, or 229 with Simply Skills; 185 + 20 × spell slots; RPG Survival Skills slots)},
 * {@code scale = min(1, (guiWidth − 4) / (2 × halfWidth))}, scaled around {@code (guiWidth / 2, guiHeight)}.
 * The hunger diamond is centered at {@code (guiWidth / 2 + 138, guiHeight − 37)}, the health diamond mirrored.
 * <p>
 * Only Miki's state flags are read by reflection (whether its Spell Engine / RPG Survival Skills adapters are active);
 * any failure is logged once and falls back to the vanilla layout.
 */
public final class MikiCompat {
    /** Horizontal distance of the docking point from the screen center (just inside the outer tip of a diamond). */
    private static final int DOCK_X = 168;
    /** Distance of the diamond center line from the bottom of the screen. */
    private static final int DOCK_Y = 37;
    /** Horizontal distance of the diamond centers from the screen center. */
    private static final int DIAMOND_X = 138;
    /** Height of Miki's artwork above the bottom of the screen. */
    private static final int ART_HEIGHT = 67;
    /** Without Miki: distance of the bar's inner end from the screen center (hotbar half width 91 + gap). */
    private static final int VANILLA_SIDE_X = 95;
    /** Without Miki: vertical center of the bar = center of the experience bar. */
    private static final int VANILLA_DOCK_Y = 27;

    private static Boolean loaded;
    private static boolean failed;
    private static Method spellEngineAvailable;
    private static Method survivalSkillsSlots;
    private static Method survivalSkillsStartX;

    /** Docking point in GUI coordinates; {@code scale} = Miki's shrink factor (1 without Miki). */
    public record Dock(double x, double y, double scale, boolean miki, double diamondX, double artTop) {}

    private MikiCompat() {}

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = ModList.get().isLoaded("mikis_hud");
            if (loaded) {
                resolveReflection();
            }
        }
        return loaded;
    }

    /** True when Miki's HUD replaces the vanilla status bars right now (same rule as Miki: survival / adventure only). */
    public static boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return isLoaded() && !failed && mc.player != null && mc.gameMode != null && !mc.options.hideGui
                && !mc.player.isSpectator() && mc.gameMode.canHurtPlayer();
    }

    /**
     * @param right true for {@link HudAnchor#MIKI_RIGHT}
     * @return where the bar docks: x of the diamond side the bar starts from, y of the bar's vertical center
     */
    public static Dock dock(boolean right, int guiWidth, int guiHeight) {
        int centerX = guiWidth / 2;
        if (isActive()) {
            double scale = scale(guiWidth);
            double mikiX = centerX + (right ? DOCK_X : -DOCK_X);
            double mikiY = guiHeight - DOCK_Y;
            double diamondX = centerX + (right ? DIAMOND_X : -DIAMOND_X) * scale;
            double artTop = guiHeight - ART_HEIGHT * scale;
            // Miki scales around (centerX, guiHeight)
            return new Dock(centerX + (mikiX - centerX) * scale, guiHeight + (mikiY - guiHeight) * scale, scale, true, diamondX, artTop);
        }
        // Without Miki: beside the hotbar at the height of the experience bar (free of vanilla elements).
        return new Dock(centerX + (right ? VANILLA_SIDE_X : -VANILLA_SIDE_X), guiHeight - VANILLA_DOCK_Y, 1.0, false,
                centerX, guiHeight - VANILLA_DOCK_Y);
    }

    /** Miki's shrink factor for the current screen width. */
    public static double scale(int guiWidth) {
        int halfWidth = ModList.get().isLoaded("simplyskills") ? 229 : 175;
        int spellSlots = spellSlots();
        if (spellSlots > 0) {
            halfWidth = Math.max(halfWidth, 185 + 20 * spellSlots);
        }
        int skillSlots = invokeInt(survivalSkillsSlots);
        if (skillSlots > 0) {
            halfWidth = Math.max(halfWidth, invokeInt(survivalSkillsStartX) + 2 + 20 * skillSlots);
        }
        return Math.min(1.0, (guiWidth - 4) / (double) (halfWidth * 2));
    }

    /** Spell slots Miki makes room for: 0 when Miki runs Spell Engine in its original layout. */
    public static int spellSlots() {
        if (spellEngineAvailable != null) {
            try {
                if (!(boolean) spellEngineAvailable.invoke(null)) {
                    return 0;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                fail(e);
                return 0;
            }
        }
        return SpellHotbar.INSTANCE.slots.size();
    }

    private static int invokeInt(Method method) {
        if (method == null) {
            return 0;
        }
        try {
            return (int) method.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return 0;
        }
    }

    private static void resolveReflection() {
        try {
            spellEngineAvailable = declared("local.mikishud.SpellEngineCompat", "isAvailable");
            if (ModList.get().isLoaded("rpg_survival_skills")) {
                survivalSkillsSlots = declared("local.mikishud.RpgSurvivalSkillsCompat", "slotCount");
                survivalSkillsStartX = declared("local.mikishud.RpgSurvivalSkillsCompat", "startX");
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            fail(e);
        }
    }

    private static Method declared(String className, String name) throws ReflectiveOperationException {
        Method method = Class.forName(className).getDeclaredMethod(name);
        method.setAccessible(true);
        return method;
    }

    private static void fail(Throwable error) {
        if (!failed) {
            failed = true;
            RunelessRPG.LOGGER.warn("Unsupported Miki's HUD version; the mana bar uses the vanilla layout instead", error);
        }
    }
}
