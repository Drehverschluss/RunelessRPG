package com.drehverschluss.runelessrpg.mixin;

import com.drehverschluss.runelessrpg.cost.CastContext;
import com.drehverschluss.runelessrpg.cost.ManaCastHandler;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.target.SpellTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks which step of a cast is being checked (channel tick / release) and pays channel ticks (DRAIN) when the tick
 * is actually performed.
 */
@Mixin(value = SpellExecution.class, remap = false)
public abstract class SpellExecutionMixin {
    @Inject(method = "performSpell", at = @At("HEAD"))
    private static void runelessrpg$pushStage(Level world, Player player, Holder<Spell> spellEntry, SpellTarget.SearchResult targetResult,
                                             SpellCast.Action action, float progress, CallbackInfo ci) {
        CastContext.push(action == SpellCast.Action.CHANNEL ? CastContext.Stage.CHANNEL_TICK
                : action == SpellCast.Action.RELEASE ? CastContext.Stage.RELEASE : CastContext.Stage.TRIGGER);
    }

    @Inject(method = "performSpell", at = @At("RETURN"))
    private static void runelessrpg$popStage(Level world, Player player, Holder<Spell> spellEntry, SpellTarget.SearchResult targetResult,
                                            SpellCast.Action action, float progress, CallbackInfo ci) {
        CastContext.pop();
    }

    @Inject(method = "performSpell", at = @At(value = "INVOKE",
            target = "Lnet/spell_engine/internals/delivery/SpellDelivery;resolveAndDeliver(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/Holder;Lnet/spell_engine/internals/target/SpellTarget$SearchResult;Lnet/spell_engine/internals/SpellExecution$ImpactContext;Ljava/util/function/Consumer;)Z"))
    private static void runelessrpg$payChannelTick(Level world, Player player, Holder<Spell> spellEntry, SpellTarget.SearchResult targetResult,
                                                  SpellCast.Action action, float progress, CallbackInfo ci) {
        if (action == SpellCast.Action.CHANNEL && player instanceof ServerPlayer serverPlayer) {
            ManaCastHandler.onChannelTick(serverPlayer, spellEntry);
        }
    }
}
