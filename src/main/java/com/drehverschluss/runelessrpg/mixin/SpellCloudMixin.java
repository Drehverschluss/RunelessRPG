package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.impact.ImpactOrigin;
import com.drehverschluss.runelessrpg.impact.Overcharge;
import com.drehverschluss.runelessrpg.impact.SpellHitContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.entity.SpellCloud;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.impact.SpellImpacts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Spell clouds: the overcharge snapshot when the cloud is created (every cloud goes through
 * {@code onCreatedFromSpell}, also clouds placed when a projectile lands), and the origin {@link ImpactOrigin#CLOUD}
 * for the hits of its ticks.
 */
@Mixin(value = SpellCloud.class, remap = false)
public abstract class SpellCloudMixin {
    @Inject(method = "onCreatedFromSpell", at = @At("TAIL"))
    private void runelessrpg$stamp(ResourceLocation spellId, Spell.Delivery.Cloud cloudData, SpellExecution.ImpactContext context,
                                   float timeToLive, Spell.Modifier.Cloud cloudModifier, CallbackInfo ci) {
        SpellCloud self = (SpellCloud) (Object) this;
        Overcharge.stamp(self, self.getOwner(), self.getSpellEntry());
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/internals/impact/SpellImpacts;lookupAndPerformAreaImpact(Lnet/spell_engine/api/spell/Spell$AreaImpact;Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Ljava/util/List;Lnet/spell_engine/internals/SpellExecution$ImpactContext;ZLjava/lang/Float;)Z"))
    private boolean runelessrpg$cloudOrigin(Spell.AreaImpact areaImpact, Holder<Spell> spellEntry, LivingEntity caster, Entity exclude,
                                            Entity aoeSource, List<Spell.Impact> impacts, SpellExecution.ImpactContext context,
                                            boolean additionalTargetLookup, Float radius, Operation<Boolean> original) {
        SpellHitContext.pushOrigin(ImpactOrigin.CLOUD, (Entity) (Object) this);
        try {
            return original.call(areaImpact, spellEntry, caster, exclude, aoeSource, impacts, context, additionalTargetLookup, radius);
        } finally {
            SpellHitContext.popOrigin();
        }
    }
}
