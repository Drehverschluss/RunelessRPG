package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.cost.CastContext.Stage;
import com.drehverschluss.runelessrpg.item.RunelessItems;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.cost.Ammo;

import java.util.List;

/** Replaces Spell Engine's item cost check for spells that cost mana (called from the {@code Ammo} mixin). */
public final class ManaCastCheck {
    private static boolean errorLogged = false;

    private ManaCastCheck() {}

    public static Ammo.Result modifyAmmoResult(Player player, Spell spell, ItemStack casterStack, Ammo.Result original) {
        Stage stage = CastContext.current();
        if (stage == Stage.BYPASS || player == null || spell == null) {
            return original;
        }
        try {
            Holder<Spell> holder = SpellRegistry.from(player.level()).wrapAsHolder(spell);
            SpellCostInfo info = ManaCosts.resolve(player.level(), holder);
            if (!info.handled()) {
                return original;
            }
            boolean noCostItem = spell.cost == null || spell.cost.item == null || spell.cost.item.id == null || spell.cost.item.id.isEmpty();
            // Runes are never searched or consumed; any other cost item (arrows) keeps Spell Engine's result.
            Ammo.Result pass = info.runeCost() || noCostItem ? new Ammo.Result(true, null, 0, List.of()) : original;
            if (!info.costsMana() || ManaCosts.isCreativeFree(player)) {
                return pass;
            }
            if (ManaCosts.canPay(player, holder, info, stage)) {
                return pass;
            }
            if (stage == Stage.CHANNEL_TICK && player instanceof ServerPlayer serverPlayer) {
                ManaCastHandler.requestChannelEnd(serverPlayer, holder);
            }
            return new Ammo.Result(false, new Ammo.Searched(null, RunelessItems.MANA.get()), 0, List.of());
        } catch (Throwable t) {
            if (!errorLogged) {
                errorLogged = true;
                RunelessRPG.LOGGER.error("Mana cost check failed, falling back to Spell Engine's item cost", t);
            }
            return original;
        }
    }
}
