package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.internals.SpellParameters;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCaster;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.cost.SpellCooldowns;

/**
 * Glitch guard: a channeled spell that ends without a release (item switch, new cast, death, logout) still gets the
 * cooldown Spell Engine would have set for releasing it at that point.
 */
public final class ChannelAbortCooldown {
    private ChannelAbortCooldown() {}

    /** Called when the server-side cast process of a player is cleared. */
    public static void onProcessCleared(ServerPlayer player, SpellCast.Process process, int channelTicksDone) {
        // A release clears the process from inside performSpell; Spell Engine sets the cooldown itself there.
        if (CastContext.current() == CastContext.Stage.RELEASE) {
            return;
        }
        impose(player, process, channelTicksDone);
    }

    public static void onLogout(ServerPlayer player) {
        SpellCaster.Player caster = (SpellCaster.Player) player;
        SpellCast.Process process = caster.getInteractor().process();
        if (process != null) {
            impose(player, process, caster.getInteractor().channelTickIndex());
        }
    }

    private static void impose(ServerPlayer player, SpellCast.Process process, int channelTicksDone) {
        Holder<Spell> holder = process.spell();
        if (holder == null || channelTicksDone <= 0 || !RunelessServerConfig.COOLDOWN_ON_CHANNEL_ABORT.get()) {
            return;
        }
        Spell spell = holder.value();
        if (spell.active == null || !SpellParameters.isChanneled(spell)) {
            return;
        }
        ResourceLocation id = holder.unwrapKey().map(ResourceKey::location).orElse(null);
        if (id == null) {
            return;
        }
        float progress = process.progress(player.level().getGameTime()).ratio();
        SpellContainerSource.SourcedContainer source = SpellContainerSource.getFirstSourceOfSpell(id, player);
        if (source == null) {
            // The hosting item is gone (e.g. switched away); the spell cooldown is still set, without the item lock.
            source = new SpellContainerSource.SourcedContainer("runelessrpg_channel_abort", null, SpellContainer.EMPTY);
        }
        SpellCooldowns.imposeCooldown(player, source, holder, progress);
    }
}
