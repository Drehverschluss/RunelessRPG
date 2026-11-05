package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.cost.CastContext;
import com.drehverschluss.runelessrpg.cost.ManaCastHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCastInteractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server cast start: check as START, and pay the start share of channeled spells once the process exists. */
@Mixin(value = SpellCastInteractor.class, remap = false)
public abstract class SpellCastInteractorMixin {
    @Shadow
    @Final
    private Player player;

    @Inject(method = "requestCast", at = @At("HEAD"))
    private void runelessrpg$pushStart(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot, CallbackInfo ci) {
        CastContext.push(CastContext.Stage.START);
    }

    @Inject(method = "requestCast", at = @At("RETURN"))
    private void runelessrpg$popStart(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot, CallbackInfo ci) {
        CastContext.pop();
    }

    @Inject(method = "requestCast", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/internals/casting/SpellCastInteractor;setProcess(Lnet/spell_engine/internals/casting/SpellCast$Process;)V",
            shift = At.Shift.AFTER))
    private void runelessrpg$onProcessStarted(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot, CallbackInfo ci) {
        SpellCast.Process process = ((SpellCastInteractor) (Object) this).process();
        if (process != null && player instanceof ServerPlayer serverPlayer) {
            ManaCastHandler.onCastStarted(serverPlayer, process.spell());
        }
    }
}
