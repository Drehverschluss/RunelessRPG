package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.cost.CastContext;
import com.drehverschluss.runelessrpg.cost.ChannelAbortCooldown;
import com.drehverschluss.runelessrpg.cost.ManaCastHandler;
import com.drehverschluss.runelessrpg.item.RunelessItems;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCastInteractor;
import net.spell_engine.internals.casting.SpellCasting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server cast start and end: check as START, pay the start share of channeled spells once the process exists,
 * report mana rejections to the client, and keep the cooldown when a channel is interrupted.
 */
@Mixin(value = SpellCastInteractor.class, remap = false)
public abstract class SpellCastInteractorMixin {
    @Shadow
    @Final
    private Player player;

    @Shadow
    @Nullable
    private SpellCast.Process process;

    @Shadow
    private int channelTickIndex;

    // try/finally around the whole method, so an exception never leaves a stale stage.
    @WrapMethod(method = "requestCast")
    private void runelessrpg$withStartStage(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot, Operation<Void> original) {
        CastContext.push(CastContext.Stage.START);
        try {
            original.call(spellId, snapshot);
        } finally {
            CastContext.pop();
        }
    }

    @WrapOperation(method = "requestCast", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/internals/casting/SpellCasting;attempt(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;)Lnet/spell_engine/internals/casting/SpellCast$Attempt;"))
    private SpellCast.Attempt runelessrpg$reportRejection(Player caster, ItemStack stack, ResourceLocation spellId, Operation<SpellCast.Attempt> original) {
        SpellCast.Attempt attempt = original.call(caster, stack, spellId);
        // The client only sends a cast request after its own check passed, so a rejection here is a desync.
        if (attempt.result() == SpellCast.Attempt.Result.MISSING_ITEM && attempt.missingItem() != null
                && attempt.missingItem().item().item() == RunelessItems.MANA.get() && caster instanceof ServerPlayer serverPlayer) {
            ManaCastHandler.notifyInsufficientMana(serverPlayer);
        }
        return attempt;
    }

    @Inject(method = "requestCast", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/internals/casting/SpellCastInteractor;setProcess(Lnet/spell_engine/internals/casting/SpellCast$Process;)V",
            shift = At.Shift.AFTER))
    private void runelessrpg$onProcessStarted(ResourceLocation spellId, SpellCast.TargetSnapshot snapshot, CallbackInfo ci) {
        SpellCast.Process started = ((SpellCastInteractor) (Object) this).process();
        if (started != null && player instanceof ServerPlayer serverPlayer) {
            ManaCastHandler.onCastStarted(serverPlayer, started.spell());
        }
    }

    @Inject(method = "setProcess", at = @At("HEAD"))
    private void runelessrpg$onProcessCleared(@Nullable SpellCast.Process next, CallbackInfo ci) {
        if (next == null && process != null && player instanceof ServerPlayer serverPlayer) {
            ChannelAbortCooldown.onProcessCleared(serverPlayer, process, channelTickIndex);
        }
    }
}
