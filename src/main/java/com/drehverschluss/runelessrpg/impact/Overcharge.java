package com.drehverschluss.runelessrpg.impact;

import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.cost.ManaCosts;
import com.drehverschluss.runelessrpg.mana.ManaAttachments;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.entity.SpellProjectile;
import org.jetbrains.annotations.Nullable;

/**
 * Overcharge on spell damage and healing (server side, inside Spell Engine's {@code performImpact}).
 * <ul>
 *   <li>Player casts: the player's current overcharge, only for spells that cost him mana.</li>
 *   <li>Summons (their own spells, also projectiles they launch on behalf of their owner) and spell clouds: the
 *       snapshot taken when they were created ({@link ManaAttachments#OVERCHARGE_SNAPSHOT}), independent of the owner's
 *       current overcharge.</li>
 *   <li>Everything else: no bonus.</li>
 * </ul>
 */
public final class Overcharge {
    private Overcharge() {}

    /** Multiplier for a hit of {@code spell} cast by {@code givenCaster} (1 = no bonus). */
    public static double multiplier(LivingEntity givenCaster, Holder<Spell> spell, ImpactOrigin origin, @Nullable Entity source) {
        if ((origin == ImpactOrigin.CLOUD || origin == ImpactOrigin.SUMMON) && source != null) {
            return snapshotOf(source);
        }
        if (givenCaster instanceof Player player) {
            return ManaCosts.overchargeBonus(player, spell);
        }
        return snapshotOf(givenCaster);
    }

    private static double snapshotOf(Entity entity) {
        if (!RunelessServerConfig.OVERCHARGE_AFFECTS_SUMMONS.get() || !entity.hasData(ManaAttachments.OVERCHARGE_SNAPSHOT)) {
            return 1.0;
        }
        return entity.getData(ManaAttachments.OVERCHARGE_SNAPSHOT);
    }

    /**
     * Stores the overcharge bonus a summon or cloud is created with: the creator's current bonus if the creator is a
     * player (no bonus for spells that do not cost him mana), or the creator's own snapshot if it is a summon itself.
     */
    public static void stamp(Entity created, @Nullable Entity creator, @Nullable Holder<Spell> spell) {
        if (creator == null || spell == null || created.level().isClientSide()) {
            return;
        }
        double bonus;
        Entity relay = SpellHitContext.originSource();
        if (SpellHitContext.origin() != ImpactOrigin.DIRECT && relay != null) {
            // created by the impact of a summon's projectile or of a cloud: inherits that snapshot
            bonus = rawSnapshot(relay);
        } else if (creator instanceof Player player) {
            bonus = ManaCosts.overchargeBonus(player, spell);
        } else {
            bonus = rawSnapshot(creator);
        }
        created.setData(ManaAttachments.OVERCHARGE_SNAPSHOT, bonus);
    }

    private static double rawSnapshot(Entity entity) {
        return entity.hasData(ManaAttachments.OVERCHARGE_SNAPSHOT) ? entity.getData(ManaAttachments.OVERCHARGE_SNAPSHOT) : 1.0;
    }

    /**
     * Marks projectiles a summon launches with its owner as projectile owner (Spell Engine does that for non-attackable
     * summons): the projectile carries the summon's snapshot, its impacts run with origin {@link ImpactOrigin#SUMMON}.
     */
    public static void register() {
        SpellEvents.PROJECTILE_SHOOT.register(event -> {
            SpellProjectile projectile = event.projectile();
            LivingEntity launcher = event.caster();
            if (!projectile.level().isClientSide() && !(launcher instanceof Player) && projectile.getOwner() != launcher) {
                projectile.setData(ManaAttachments.OVERCHARGE_SNAPSHOT, rawSnapshot(launcher));
            }
        });
    }

    /** @return whether {@code projectile} was launched by a summon on behalf of its owner (see {@link #register()}) */
    public static boolean isRelayedProjectile(Entity projectile) {
        return projectile.hasData(ManaAttachments.OVERCHARGE_SNAPSHOT);
    }
}
