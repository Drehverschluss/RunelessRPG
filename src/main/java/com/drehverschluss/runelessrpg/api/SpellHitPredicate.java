package com.drehverschluss.runelessrpg.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

/**
 * Tells RunelessRPG that a hit is a direct spell hit of another mod's spell, so that Spell Mana Leech
 * ({@code runelessrpg:spell_mana_leech}) applies to it ({@link RunelessApi#registerSpellHitPredicate}).
 * <p>
 * Asked on the server for every damage a player deals that is not a Spell Engine hit, after RunelessRPG's own checks
 * (the player caused it, the target is not the player and not in {@code #runelessrpg:no_mana_leech}, damage above 0).
 * All rules of Spell Mana Leech apply unchanged: the attribute, fewer mana for further targets in the same tick (one
 * counter shared with Spell Engine hits, {@code spellManaLeechExtraTargetPercent}), the cap at the target's health and
 * absorption, reason {@link ManaChangeEvent.Reason#SPELL_LEECH}. Such hits never count for weapon Mana Leech.
 * <p>
 * Count only hits of spells the player cast himself, directly: projectiles, instant hits, channel ticks. Not damage over
 * time, lasting zones or clouds, or summons. Keep the check cheap; an exception is caught, logged once and counts as
 * {@code false}.
 *
 * @apiNote Experimental since 0.4.0: this interface may still change in later versions.
 */
@ApiStatus.Experimental
@FunctionalInterface
public interface SpellHitPredicate {
    /**
     * @param source the damage source of the hit
     * @param target the entity that was hit
     * @param player the player that caused the damage ({@code source.getEntity()})
     * @return {@code true} if this is a direct hit of a spell the player cast
     */
    boolean isDirectSpellHit(DamageSource source, LivingEntity target, ServerPlayer player);
}
