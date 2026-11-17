package com.drehverschluss.runelessrpg.client;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig.BacklashMode;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import com.drehverschluss.runelessrpg.potion.RunelessPotions;
import com.drehverschluss.runelessrpg.util.CostFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TippedArrowItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Short gray lines below the effect lines of potions, splash and lingering potions and tipped arrows with a RunelessRPG
 * effect. The numbers come from the server config ({@link CostSettings#client()}).
 * <p>
 * Uses {@link ItemTooltipEvent} (fired by every {@code ItemStack.getTooltipLines}, so also in the creative inventory and
 * in JEI). The lines go right below the matching effect line; if that line is not in the tooltip (hidden tooltip,
 * another mod rebuilt it), nothing is added.
 */
public final class PotionTooltip {
    private static final String KEY = "tooltip.runelessrpg.potion.";

    private static boolean errorLogged;

    private enum Form {
        DRINK(""), SPLASH(".splash"), LINGERING(".lingering"), ARROW(".arrow");

        private final String suffix;

        Form(String suffix) {
            this.suffix = suffix;
        }
    }

    private PotionTooltip() {}

    public static void onItemTooltip(ItemTooltipEvent event) {
        try {
            addLines(event.getItemStack(), event.getToolTip());
        } catch (RuntimeException e) {
            if (!errorLogged) {
                errorLogged = true;
                RunelessRPG.LOGGER.error("Could not add the potion tooltip lines", e);
            }
        }
    }

    private static void addLines(ItemStack stack, List<Component> tooltip) {
        Form form = form(stack.getItem());
        if (form == null) {
            return;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        CostSettings settings = CostSettings.client();
        int searchFrom = 0;
        for (MobEffectInstance instance : contents.getAllEffects()) {
            List<Component> lines = linesFor(instance, form, settings);
            if (lines.isEmpty()) {
                continue;
            }
            int index = findEffectLine(tooltip, instance.getDescriptionId(), searchFrom);
            if (index < 0) {
                continue;
            }
            tooltip.addAll(index + 1, lines);
            searchFrom = index + 1 + lines.size();
        }
    }

    private static Form form(Item item) {
        if (item instanceof LingeringPotionItem) {
            return Form.LINGERING;
        }
        if (item instanceof ThrowablePotionItem) {
            return Form.SPLASH;
        }
        if (item instanceof PotionItem) {
            return Form.DRINK;
        }
        if (item instanceof TippedArrowItem) {
            return Form.ARROW;
        }
        return null;
    }

    private static List<Component> linesFor(MobEffectInstance instance, Form form, CostSettings settings) {
        MobEffect effect = instance.getEffect().value();
        List<Component> lines = new ArrayList<>(2);
        if (effect == RunelessPotions.MANA_RESTORE.get()) {
            manaRestore(instance.getAmplifier(), form, settings, lines);
        } else if (effect == RunelessPotions.BORROWED_POWER.get()) {
            borrowedPower(settings, lines);
        } else if (effect == RunelessPotions.MAGICAL_STABILITY.get()) {
            if (settings.magicalStabilityManaCost() > 0.0 && settings.minManaCostPercent() > 0.0) {
                lines.add(line("magical_stability", percent(settings.minManaCostPercent())));
            }
        } else if (effect == RunelessPotions.MANA_HARVEST.get()) {
            manaHarvest(instance.getAmplifier() + 1, settings, lines);
        }
        return lines;
    }

    // Same lines for every form: the effect counts for whoever has it when killing or hitting
    private static void manaHarvest(int level, CostSettings settings, List<Component> lines) {
        if (!settings.manaOrbsEnabled()) {
            return;
        }
        double kill = Math.min(100.0, settings.killBaseChancePercent() + level * settings.killChancePercentPerLevel());
        if (kill > 0.0) {
            lines.add(line("mana_harvest", percent(kill), percent(settings.orbManaPercent())));
        }
        double boss = Math.min(100.0, settings.bossHitBaseChancePercent() + level * settings.bossHitChancePercentPerLevel());
        if (boss > 0.0) {
            if (settings.bossHitCooldownSeconds() > 0.0) {
                lines.add(line("mana_harvest.boss", percent(boss), CostFormat.format(settings.bossHitCooldownSeconds())));
            } else {
                lines.add(line("mana_harvest.boss.no_cooldown", percent(boss)));
            }
        }
    }

    private static void manaRestore(int amplifier, Form form, CostSettings settings, List<Component> lines) {
        if (form == Form.LINGERING) {
            // Lingering clouds: a share each second, no Mana Sickness (ManaRestoreEffect)
            double perSecond = (amplifier + 1) * settings.lingeringManaPercentPerLevel();
            if (perSecond > 0.0) {
                lines.add(line("mana_restore.lingering", percent(perSecond)));
                if (settings.manaSicknessSeconds() > 0 && settings.manaSicknessRestorePercent() < 100.0) {
                    lines.add(line("mana_restore.lingering.no_sickness"));
                }
            }
            return;
        }
        double restore = (amplifier + 1) * settings.manaPotionPercentPerLevel();
        if (restore > 0.0) {
            lines.add(line("mana_restore" + form.suffix, percent(restore)));
        }
        double sicknessShare = settings.manaSicknessRestorePercent();
        if (settings.manaSicknessSeconds() > 0 && sicknessShare < 100.0) {
            String seconds = String.valueOf(settings.manaSicknessSeconds());
            if (sicknessShare <= 0.0) {
                lines.add(line("mana_sickness.none", seconds));
            } else {
                lines.add(line("mana_sickness", seconds, percent(sicknessShare)));
            }
        }
    }

    private static void borrowedPower(CostSettings settings, List<Component> lines) {
        if (settings.borrowedPowerDebtPercent() > 0.0) {
            lines.add(line("borrowed_power", percent(settings.borrowedPowerDebtPercent())));
        }
        if (settings.backlashMode() == BacklashMode.FLAT) {
            if (settings.backlashDamagePer10Percent() > 0.0 && settings.backlashMaxDamage() > 0.0) {
                lines.add(line("backlash.flat", CostFormat.format(settings.backlashDamagePer10Percent()),
                        CostFormat.format(settings.backlashMaxDamage())));
            }
        } else if (settings.backlashHealthPercentPer10Percent() > 0.0 && settings.backlashMaxHealthPercent() > 0.0) {
            lines.add(line("backlash.percent", percent(settings.backlashHealthPercentPer10Percent()),
                    percent(settings.backlashMaxHealthPercent())));
        }
    }

    private static Component line(String key, Object... args) {
        return Component.translatable(KEY + key, args).withStyle(ChatFormatting.GRAY);
    }

    private static String percent(double value) {
        return CostFormat.format(value);
    }

    /** Index of the first line at or after {@code from} that contains the translation key of the effect, or -1. */
    private static int findEffectLine(List<Component> tooltip, String effectKey, int from) {
        for (int i = from; i < tooltip.size(); i++) {
            if (containsKey(tooltip.get(i), effectKey)) {
                return i;
            }
        }
        return -1;
    }

    // The effect line is "potion.withDuration"/"potion.withAmplifier" with the effect name as an argument
    private static boolean containsKey(Component component, String key) {
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (key.equals(translatable.getKey())) {
                return true;
            }
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component argComponent && containsKey(argComponent, key)) {
                    return true;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            if (containsKey(sibling, key)) {
                return true;
            }
        }
        return false;
    }
}
