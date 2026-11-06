package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.impact.ImpactOrigin;
import com.drehverschluss.runelessrpg.impact.SpellHitContext;
import com.drehverschluss.runelessrpg.mana.ManaAttachments;
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
 * <p>
 * Runs for every entity tick, so the common case is one attachment lookup ({@code hasData}: a null check for entities
 * without attachments, otherwise one map lookup) and the original call; no allocation. The calls are wrapped with
 * MixinExtras {@code @WrapOperation} (no {@code @Redirect} / {@code @Overwrite}), so other mods can wrap or cancel the
 * same methods.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @WrapOperation(method = "tickNonPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void runelessrpg$tickWithOrigin(Entity entity, Operation<Void> original) {
        if (!entity.hasData(ManaAttachments.IMPACT_ORIGIN)) {
            original.call(entity);
            return;
        }
        runelessrpg$runWithOrigin(entity, original);
    }

    @WrapOperation(method = "tickPassenger", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void runelessrpg$rideTickWithOrigin(Entity passenger, Operation<Void> original) {
        if (!passenger.hasData(ManaAttachments.IMPACT_ORIGIN)) {
            original.call(passenger);
            return;
        }
        runelessrpg$runWithOrigin(passenger, original);
    }

    private static void runelessrpg$runWithOrigin(Entity entity, Operation<Void> original) {
        ImpactOrigin origin = entity.getData(ManaAttachments.IMPACT_ORIGIN);
        if (origin == ImpactOrigin.DIRECT) {
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
}
