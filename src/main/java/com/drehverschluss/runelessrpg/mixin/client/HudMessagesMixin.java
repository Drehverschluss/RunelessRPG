package com.drehverschluss.runelessrpg.mixin.client;

import com.drehverschluss.runelessrpg.client.hud.ManaFlash;
import com.drehverschluss.runelessrpg.client.hud.ManaTextFade;
import com.drehverschluss.runelessrpg.item.RunelessItems;
import net.spell_engine.client.gui.HudMessages;
import net.spell_engine.internals.casting.SpellCast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flashes the mana bar when Spell Engine shows its "Missing Mana" message for a cast attempt. */
@Mixin(value = HudMessages.class, remap = false)
public abstract class HudMessagesMixin {
    @Inject(method = "castAttemptError", at = @At("HEAD"))
    private void runelessrpg$flashOnMissingMana(SpellCast.Attempt attempt, CallbackInfo ci) {
        if (attempt.result() == SpellCast.Attempt.Result.MISSING_ITEM && attempt.missingItem() != null
                && attempt.missingItem().item().item() == RunelessItems.MANA.get()) {
            ManaFlash.trigger();
            ManaTextFade.ping();
        }
    }
}
