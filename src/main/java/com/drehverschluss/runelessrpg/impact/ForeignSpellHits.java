package com.drehverschluss.runelessrpg.impact;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.api.SpellHitPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Predicates of other mods that recognize direct hits of their spells for Spell Mana Leech. */
public final class ForeignSpellHits {
    private static final List<SpellHitPredicate> PREDICATES = new CopyOnWriteArrayList<>();
    private static final Set<SpellHitPredicate> FAILED = Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    private ForeignSpellHits() {}

    public static void register(SpellHitPredicate predicate) {
        if (predicate != null && !PREDICATES.contains(predicate)) {
            PREDICATES.add(predicate);
        }
    }

    public static void unregister(SpellHitPredicate predicate) {
        PREDICATES.remove(predicate);
    }

    /** True if a registered predicate calls this a direct spell hit; an exception counts as false and is logged once. */
    static boolean isDirect(DamageSource source, LivingEntity target, ServerPlayer player) {
        if (PREDICATES.isEmpty()) {
            return false;
        }
        for (SpellHitPredicate predicate : PREDICATES) {
            try {
                if (predicate.isDirectSpellHit(source, target, player)) {
                    return true;
                }
            } catch (Throwable t) {
                if (FAILED.add(predicate)) {
                    RunelessRPG.LOGGER.error("Spell hit predicate {} failed, the hit counts as not direct", predicate, t);
                }
            }
        }
        return false;
    }
}
