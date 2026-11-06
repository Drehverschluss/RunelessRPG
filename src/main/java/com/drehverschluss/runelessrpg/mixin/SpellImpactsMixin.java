package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.impact.ImpactOrigin;
import com.drehverschluss.runelessrpg.impact.Overcharge;
import com.drehverschluss.runelessrpg.impact.SpellHitContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.entity.SpellEntity;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.summon.SpellSummoned;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.impact.SpellImpacts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Overcharge and the hit context for mana leech, in the one place where Spell Engine knows which spell hits and who
 * cast it ({@code givenCaster}: the player, or a summon for its own spells). Also stamps the overcharge snapshot on
 * summons and spell-spawned entities.
 */
@Mixin(value = SpellImpacts.class, remap = false)
public abstract class SpellImpactsMixin {
    @WrapMethod(method = "projectileImpact")
    private static boolean runelessrpg$projectileOrigin(LivingEntity caster, Entity projectile, Entity target, Holder<Spell> spellEntry,
                                                        SpellExecution.ImpactContext context, Operation<Boolean> original) {
        if (!Overcharge.isRelayedProjectile(projectile)) {
            return original.call(caster, projectile, target, spellEntry, context);
        }
        SpellHitContext.pushOrigin(ImpactOrigin.SUMMON, projectile);
        try {
            return original.call(caster, projectile, target, spellEntry, context);
        } finally {
            SpellHitContext.popOrigin();
        }
    }

    @WrapMethod(method = "fallImpact")
    private static void runelessrpg$fallOrigin(LivingEntity caster, Entity projectile, Holder<Spell> spellEntry,
                                               SpellExecution.ImpactContext context, Operation<Void> original) {
        if (!Overcharge.isRelayedProjectile(projectile)) {
            original.call(caster, projectile, spellEntry, context);
            return;
        }
        SpellHitContext.pushOrigin(ImpactOrigin.SUMMON, projectile);
        try {
            original.call(caster, projectile, spellEntry, context);
        } finally {
            SpellHitContext.popOrigin();
        }
    }

    @WrapOperation(method = "performImpact", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean runelessrpg$damage(Entity target, DamageSource source, float amount, Operation<Boolean> original,
                                              @Local(argsOnly = true) LivingEntity givenCaster,
                                              @Local(argsOnly = true) Holder<Spell> spellEntry) {
        SpellHitContext.Hit hit = SpellHitContext.pushHit(givenCaster, spellEntry);
        try {
            double bonus = Overcharge.multiplier(givenCaster, spellEntry, hit.origin(), hit.source());
            return original.call(target, source, (float) (amount * bonus));
        } finally {
            SpellHitContext.popHit();
        }
    }

    @WrapOperation(method = "performImpact", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    private static void runelessrpg$heal(LivingEntity target, float amount, Operation<Void> original,
                                         @Local(argsOnly = true) LivingEntity givenCaster,
                                         @Local(argsOnly = true) Holder<Spell> spellEntry) {
        double bonus = 1.0;
        if (RunelessServerConfig.OVERCHARGE_AFFECTS_HEALING.get()) {
            bonus = Overcharge.multiplier(givenCaster, spellEntry, SpellHitContext.origin(), SpellHitContext.originSource());
        }
        original.call(target, (float) (amount * bonus));
    }

    @WrapOperation(method = "performImpact", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/api/entity/SpellEntity$Spawned;onSpawnedBySpell(Lnet/spell_engine/api/entity/SpellEntity$Spawned$Args;)V"))
    private static void runelessrpg$stampSpawned(SpellEntity.Spawned spawned, SpellEntity.Spawned.Args args, Operation<Void> original) {
        original.call(spawned, args);
        if (spawned instanceof Entity entity) {
            Overcharge.stamp(entity, args.owner(), args.spell());
        }
    }

    @WrapOperation(method = "summon", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/api/spell/summon/SpellSummoned;onSummonedBySpell(Lnet/spell_engine/api/spell/summon/SpellSummoned$Args;)V"))
    private static void runelessrpg$stampSummon(SpellSummoned summoned, SpellSummoned.Args args, Operation<Void> original) {
        original.call(summoned, args);
        if (summoned instanceof Entity entity) {
            Overcharge.stamp(entity, args.owner, args.spell);
        }
    }
}
