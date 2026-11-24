package com.drehverschluss.runelessrpg.client;

import com.drehverschluss.runelessrpg.api.ManaInsufficientEvent;
import com.drehverschluss.runelessrpg.client.hud.ManaFlash;
import com.drehverschluss.runelessrpg.client.hud.ManaTextFade;
import com.drehverschluss.runelessrpg.item.RunelessItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.spell_engine.client.SpellEngineClient;
import net.spell_engine.client.gui.HudMessages;

/** Client reaction to "not enough mana": flash of the bar, the number, and {@link ManaInsufficientEvent}. */
public final class ManaFeedback {
    private ManaFeedback() {}

    /** The server rejected a cast, ended a channel or another mod signaled it; {@code message} = Spell Engine's "Missing Mana". */
    public static void onServerInsufficientMana(double missing, boolean message) {
        if (message && (SpellEngineClient.config == null || SpellEngineClient.config.showSpellCastErrors)) {
            String itemName = I18n.get(RunelessItems.MANA.get().getDescriptionId());
            HudMessages.INSTANCE.error(Component.translatable("hud.cast_attempt_error.missing_item", itemName).withStyle(ChatFormatting.RED));
        }
        ManaFlash.trigger();
        ManaTextFade.ping();
        NeoForge.EVENT_BUS.post(new ManaInsufficientEvent(missing, ManaInsufficientEvent.Source.SERVER));
    }

    /** The client's own cast check failed (Spell Engine shows its message itself). */
    public static void onClientInsufficientMana(double missing) {
        ManaFlash.trigger();
        ManaTextFade.ping();
        NeoForge.EVENT_BUS.post(new ManaInsufficientEvent(missing, ManaInsufficientEvent.Source.CLIENT));
    }
}
