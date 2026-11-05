package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.cost.ManaCastCheck;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.cost.Ammo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Mana instead of runes: blocks casts without enough mana and stops rune lookup/consumption. Client and server. */
@Mixin(value = Ammo.class, remap = false)
public abstract class AmmoMixin {
    @ModifyReturnValue(method = "ammoForSpell", at = @At("RETURN"))
    private static Ammo.Result runelessrpg$manaInsteadOfRunes(Ammo.Result original, Player player, Spell spell, ItemStack casterStack) {
        return ManaCastCheck.modifyAmmoResult(player, spell, casterStack, original);
    }
}
