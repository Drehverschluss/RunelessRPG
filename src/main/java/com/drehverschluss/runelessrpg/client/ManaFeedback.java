package com.drehverschluss.runelessrpg.client;

import com.drehverschluss.runelessrpg.client.hud.ManaFlash;
import com.drehverschluss.runelessrpg.item.RunelessItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.gui.HudMessages;

/** Client reaction when the server reports a cast that failed for lack of mana. */
public final class ManaFeedback {
    private ManaFeedback() {}

    /** Spell Engine's "Missing Mana" message plus the flash of the bar. */
    public static void onServerInsufficientMana() {
        if (SpellEngineClient.config == null || SpellEngineClient.config.showSpellCastErrors) {
            String itemName = I18n.get(RunelessItems.MANA.get().getDescriptionId());
            HudMessages.INSTANCE.error(Component.translatable("hud.cast_attempt_error.missing_item", itemName).withStyle(ChatFormatting.RED));
        }
        ManaFlash.trigger();
    }
}
