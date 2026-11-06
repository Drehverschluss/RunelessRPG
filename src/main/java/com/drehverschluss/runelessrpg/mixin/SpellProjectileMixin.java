package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.impact.Overcharge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.spell_engine.entity.SpellProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Projectiles split off by a chain reaction inherit origin and overcharge snapshot of the projectile they come from. */
@Mixin(value = SpellProjectile.class, remap = false)
public abstract class SpellProjectileMixin {
    @WrapOperation(method = "chainReactionFrom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean runelessrpg$inherit(Level level, Entity child, Operation<Boolean> original) {
        Overcharge.inheritFromParent(child, (Entity) (Object) this);
        return original.call(level, child);
    }
}
