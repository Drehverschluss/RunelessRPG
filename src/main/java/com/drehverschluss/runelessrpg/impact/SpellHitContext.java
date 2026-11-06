package com.drehverschluss.runelessrpg.impact;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.spell_engine.api.spell.Spell;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Server-thread context of Spell Engine hits. The origin (cloud, stash, otherwise direct) is set around the calls
 * into {@code SpellImpacts}; the hit itself is set around the {@code Entity.hurt} call in {@code performImpact}, so
 * {@code LivingDamageEvent}s fired synchronously inside it know which spell, which caster and which origin caused
 * the damage. Always set and reset in try/finally.
 */
public final class SpellHitContext {
    /** Origin frame: where the current impacts come from, and the cloud entity for {@link ImpactOrigin#CLOUD}. */
    private record Frame(ImpactOrigin origin, @Nullable Entity source) {}

    /**
     * A spell hit in progress.
     *
     * @param givenCaster the caster Spell Engine passed (a player, or a summon for its own spells)
     * @param spell       the spell that hits
     * @param origin      direct hit, cloud or stash
     * @param source      the cloud entity for {@link ImpactOrigin#CLOUD}, otherwise null
     */
    public record Hit(LivingEntity givenCaster, Holder<Spell> spell, ImpactOrigin origin, @Nullable Entity source) {}

    private static final ThreadLocal<Deque<Frame>> ORIGINS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<Hit>> HITS = ThreadLocal.withInitial(ArrayDeque::new);

    private SpellHitContext() {}

    public static void pushOrigin(ImpactOrigin origin, @Nullable Entity source) {
        ORIGINS.get().push(new Frame(origin, source));
    }

    public static void popOrigin() {
        ORIGINS.get().poll();
    }

    public static ImpactOrigin origin() {
        Frame frame = ORIGINS.get().peek();
        return frame != null ? frame.origin() : ImpactOrigin.DIRECT;
    }

    @Nullable
    public static Entity originSource() {
        Frame frame = ORIGINS.get().peek();
        return frame != null ? frame.source() : null;
    }

    public static Hit pushHit(LivingEntity givenCaster, Holder<Spell> spell) {
        Hit hit = new Hit(givenCaster, spell, origin(), originSource());
        HITS.get().push(hit);
        return hit;
    }

    public static void popHit() {
        HITS.get().poll();
    }

    /** @return the spell hit whose damage is being dealt right now, or null outside of Spell Engine hits */
    @Nullable
    public static Hit currentHit() {
        return HITS.get().peek();
    }
}
