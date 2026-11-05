package com.drehverschluss.runelessrpg.mixin.client;

import com.drehverschluss.runelessrpg.client.ManaTooltip;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.client.gui.SpellTooltip;
import net.spell_engine.internals.cost.Ammo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/** Spell tooltip: hides the rune line of mana spells and adds a "Mana: N" line. */
@Mixin(value = SpellTooltip.class, remap = false)
public abstract class SpellTooltipMixin {
    @WrapOperation(method = "addSpellDetails", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/internals/cost/Ammo;ammoForSpell(Lnet/minecraft/world/entity/player/Player;Lnet/spell_engine/api/spell/Spell;Lnet/minecraft/world/item/ItemStack;)Lnet/spell_engine/internals/cost/Ammo$Result;"))
    private static Ammo.Result runelessrpg$itemLine(Player player, Spell spell, ItemStack itemStack, Operation<Ammo.Result> original) {
        return ManaTooltip.itemCostLine(player, spell, itemStack, original::call);
    }

    @Inject(method = "addSpellDetails", at = @At("TAIL"))
    private static void runelessrpg$manaLine(Holder<Spell> spellEntry, Player player, ItemStack itemStack, int indentLevel,
                                             ArrayList<Component> lines, CallbackInfo ci) {
        ManaTooltip.appendManaLine(spellEntry, player, indentLevel, lines);
    }
}
