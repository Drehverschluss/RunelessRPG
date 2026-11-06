package com.drehverschluss.runelessrpg.client;

import com.drehverschluss.runelessrpg.config.RunelessServerConfig.ChannelCostMode;
import com.drehverschluss.runelessrpg.cost.CastContext;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import com.drehverschluss.runelessrpg.cost.ManaCosts;
import com.drehverschluss.runelessrpg.cost.SpellCostInfo;
import com.drehverschluss.runelessrpg.util.CostFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.cost.Ammo;
import net.spell_engine.internals.impact.SpellEstimation;

import java.util.List;
import java.util.function.Function;

/** Tooltip lines of spells that cost mana. */
public final class ManaTooltip {
    private ManaTooltip() {}

    /** Item cost line: Spell Engine's own result (arrows stay), but no rune line for spells handled by RunelessRPG. */
    public static Ammo.Result itemCostLine(Player player, Spell spell, ItemStack stack, Function<Object[], Ammo.Result> original) {
        Ammo.Result result;
        CastContext.push(CastContext.Stage.BYPASS);
        try {
            result = original.apply(new Object[]{player, spell, stack});
        } finally {
            CastContext.pop();
        }
        if (player == null || result.item() == null) {
            return result;
        }
        Holder<Spell> holder = SpellRegistry.from(player.level()).wrapAsHolder(spell);
        SpellCostInfo info = ManaCosts.resolve(player.level(), holder);
        if (info.handled() && info.runeCost()) {
            return new Ammo.Result(result.satisfied(), null, 0, List.of());
        }
        return result;
    }

    public static void appendManaLine(Holder<Spell> holder, Player player, int indentLevel, List<Component> lines) {
        if (player == null) {
            return;
        }
        SpellCostInfo info = ManaCosts.resolve(player.level(), holder);
        if (!info.costsMana()) {
            return;
        }
        double total = ManaCosts.cost(player, holder, info);
        boolean affordable = ManaCosts.canPay(player, holder, info, CastContext.Stage.START);
        MutableComponent text;
        Spell spell = holder.value();
        if (info.channeled() && CostSettings.client().channelCostMode() == ChannelCostMode.DRAIN && spell.active.cast.duration > 0.0F) {
            double perSecond = total / spell.active.cast.duration;
            text = Component.translatable("spell.tooltip.runelessrpg.mana.channel", CostFormat.format(total), CostFormat.format(perSecond));
        } else {
            text = Component.translatable("spell.tooltip.runelessrpg.mana", CostFormat.format(total));
        }
        String indent = indentLevel > 0 ? " ".repeat(indentLevel) : "";
        lines.add(Component.literal(indent).append(text).withStyle(affordable ? ChatFormatting.GREEN : ChatFormatting.RED));
        appendOverchargeLine(holder, player, indent, lines);
    }

    /** "Overcharge: +10% damage" (or healing / damage and healing) below the cost line, only with a bonus. */
    private static void appendOverchargeLine(Holder<Spell> holder, Player player, String indent, List<Component> lines) {
        double bonus = ManaCosts.overchargeBonus(player, holder) - 1.0;
        if (bonus <= 1.0E-9) {
            return;
        }
        boolean damage = hasImpact(holder.value(), Spell.Impact.Action.Type.DAMAGE);
        boolean healing = hasImpact(holder.value(), Spell.Impact.Action.Type.HEAL) && CostSettings.client().overchargeAffectsHealing();
        String key;
        if (damage && healing) {
            key = "spell.tooltip.runelessrpg.overcharge.both";
        } else if (healing) {
            key = "spell.tooltip.runelessrpg.overcharge.healing";
        } else if (damage) {
            key = "spell.tooltip.runelessrpg.overcharge.damage";
        } else {
            return;
        }
        lines.add(Component.literal(indent).append(Component.translatable(key, CostFormat.format(bonus * 100.0)))
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    private static boolean hasImpact(Spell spell, Spell.Impact.Action.Type type) {
        if (spell.impacts != null && spell.impacts.stream().anyMatch(impact -> impact.action.type == type)) {
            return true;
        }
        return spell.modifiers != null && spell.modifiers.stream()
                .anyMatch(modifier -> modifier.impacts.stream().anyMatch(impact -> impact.action.type == type));
    }

    /** Spell Engine's damage / healing estimate of the description, multiplied by the overcharge bonus. */
    public static SpellEstimation.EstimatedOutput withOvercharge(SpellEstimation.EstimatedOutput output, Holder<Spell> holder, Player player) {
        if (player == null) {
            return output;
        }
        double bonus = ManaCosts.overchargeBonus(player, holder);
        if (bonus == 1.0) {
            return output;
        }
        double healBonus = CostSettings.client().overchargeAffectsHealing() ? bonus : 1.0;
        return new SpellEstimation.EstimatedOutput(
                output.damage().stream().map(value -> value.multiply(bonus)).toList(),
                output.heal().stream().map(value -> value.multiply(healBonus)).toList());
    }
}
