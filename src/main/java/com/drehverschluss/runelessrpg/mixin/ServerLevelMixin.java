package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.impact.ImpactOrigin;
import com.drehverschluss.runelessrpg.impact.Overcharge;
import com.drehverschluss.runelessrpg.impact.SpellHitContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Entities with an own impact origin (spell-spawned entities, projectiles of summons / spawned entities / clouds): while
 * they tick, everything they cause runs with that origin. Covers spawned entities that call Spell Engine's impacts or
 * plain {@code hurt} themselves, wherever the hit comes from inside their tick.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void runelessrpg$tickWithOrigin(Entity entity, Operation<Void> original) {
        ImpactOrigin origin = Overcharge.originOf(entity);
        if (origin == null) {
            original.call(entity);
            return;
        }
        SpellHitContext.pushOrigin(origin, entity);
        try {
            original.call(entity);
        } finally {
            SpellHitContext.popOrigin();
        }
    }

    @WrapOperation(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void runelessrpg$rideTickWithOrigin(Entity passenger, Operation<Void> original) {
        ImpactOrigin origin = Overcharge.originOf(passenger);
        if (origin == null) {
            original.call(passenger);
            return;
        }
        SpellHitContext.pushOrigin(origin, passenger);
        try {
            original.call(passenger);
        } finally {
            SpellHitContext.popOrigin();
        }
    }
}
