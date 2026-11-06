package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.impact.ImpactOrigin;
import com.drehverschluss.runelessrpg.impact.SpellHitContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.delivery.SpellStashHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Origin {@link ImpactOrigin#STASH} for the hits of stashed spells released by a trigger. */
@Mixin(value = SpellStashHelper.class, remap = false)
public abstract class SpellStashHelperMixin {
    @WrapOperation(method = "useStashes", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/internals/impact/SpellImpacts;performImpacts(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Ljava/util/List;Lnet/spell_engine/internals/SpellExecution$ImpactContext;)Z"))
    private static boolean runelessrpg$stashOrigin(Level world, LivingEntity caster, Entity target, Entity aoeSource, Holder<Spell> spellEntry,
                                                   List<Spell.Impact> impacts, SpellExecution.ImpactContext context, Operation<Boolean> original) {
        SpellHitContext.pushOrigin(ImpactOrigin.STASH, null);
        try {
            return original.call(world, caster, target, aoeSource, spellEntry, impacts, context);
        } finally {
            SpellHitContext.popOrigin();
        }
    }
}
