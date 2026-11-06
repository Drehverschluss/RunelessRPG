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
 *   <li>Summons, spell clouds, spell-spawned entities and the projectiles they launch: the snapshot taken when they
 *       were created ({@link ManaAttachments#OVERCHARGE_SNAPSHOT}), independent of the owner's current overcharge.</li>
 *   <li>Everything else: no bonus.</li>
 * </ul>
 */
public final class Overcharge {
    private Overcharge() {}

    /** Multiplier for a hit of {@code spell} cast by {@code givenCaster} (1 = no bonus). */
    public static double multiplier(LivingEntity givenCaster, Holder<Spell> spell, ImpactOrigin origin, @Nullable Entity source) {
        if (origin != ImpactOrigin.DIRECT && origin != ImpactOrigin.STASH && source != null) {
            return snapshotOf(source);
        }
        if (givenCaster instanceof Player player) {
            return ManaCosts.overchargeBonus(player, spell);
        }
        return snapshotOf(givenCaster);
    }

    private static double snapshotOf(Entity entity) {
        if (!RunelessServerConfig.OVERCHARGE_AFFECTS_SUMMONS.get()) {
            return 1.0;
        }
        return rawSnapshot(entity);
    }

    private static double rawSnapshot(Entity entity) {
        return entity.hasData(ManaAttachments.OVERCHARGE_SNAPSHOT) ? entity.getData(ManaAttachments.OVERCHARGE_SNAPSHOT) : 1.0;
    }

    /**
     * Stores the overcharge bonus a summon, cloud or spawned entity is created with: inside the hits of a summon's
     * projectile, a cloud or a spawned entity the snapshot of that source; otherwise the creator's current bonus if the
     * creator is a player (no bonus for spells that do not cost him mana), or the creator's own snapshot.
     */
    public static void stamp(Entity created, @Nullable Entity creator, @Nullable Holder<Spell> spell) {
        if (creator == null || spell == null || created.level().isClientSide()) {
            return;
        }
        double bonus;
        Entity relay = SpellHitContext.originSource();
        if (SpellHitContext.origin() != ImpactOrigin.DIRECT && relay != null) {
            bonus = rawSnapshot(relay);
        } else if (creator instanceof Player player) {
            bonus = ManaCosts.overchargeBonus(player, spell);
        } else {
            bonus = rawSnapshot(creator);
        }
        created.setData(ManaAttachments.OVERCHARGE_SNAPSHOT, bonus);
    }

    /** Marks an entity placed by a spell's SPAWN action: its hits run with origin {@link ImpactOrigin#SPAWNED}. */
    public static void markSpawned(Entity entity) {
        if (!entity.level().isClientSide()) {
            entity.setData(ManaAttachments.IMPACT_ORIGIN, ImpactOrigin.SPAWNED);
        }
    }

    /**
     * Projectiles: launched while a cloud, spawned entity or relayed projectile is acting, they inherit its origin and
     * snapshot; launched by a summon with its owner as projectile owner (Spell Engine does that for non-attackable
     * summons like the Fire Hydra) they get origin {@link ImpactOrigin#SUMMON} and the summon's snapshot.
     */
    public static void register() {
        SpellEvents.PROJECTILE_SHOOT.register(event -> onLaunch(event.projectile(), event.caster()));
        SpellEvents.PROJECTILE_FALL.register(event -> onLaunch(event.projectile(), event.caster()));
    }

    private static void onLaunch(SpellProjectile projectile, LivingEntity launcher) {
        if (projectile.level().isClientSide()) {
            return;
        }
        ImpactOrigin origin = SpellHitContext.origin();
        Entity source = SpellHitContext.originSource();
        if (origin != ImpactOrigin.DIRECT && origin != ImpactOrigin.STASH && source != null) {
            inherit(projectile, origin, source);
        } else if (!(launcher instanceof Player) && projectile.getOwner() != launcher) {
            projectile.setData(ManaAttachments.IMPACT_ORIGIN, ImpactOrigin.SUMMON);
            projectile.setData(ManaAttachments.OVERCHARGE_SNAPSHOT, rawSnapshot(launcher));
        }
    }

    /** A projectile created from another one (chain reaction): same origin and snapshot as the parent. */
    public static void inheritFromParent(Entity child, Entity parent) {
        if (child.level().isClientSide()) {
            return;
        }
        if (parent.hasData(ManaAttachments.IMPACT_ORIGIN)) {
            child.setData(ManaAttachments.IMPACT_ORIGIN, parent.getData(ManaAttachments.IMPACT_ORIGIN));
        }
        if (parent.hasData(ManaAttachments.OVERCHARGE_SNAPSHOT)) {
            child.setData(ManaAttachments.OVERCHARGE_SNAPSHOT, parent.getData(ManaAttachments.OVERCHARGE_SNAPSHOT));
        }
    }

    private static void inherit(Entity projectile, ImpactOrigin origin, Entity source) {
        projectile.setData(ManaAttachments.IMPACT_ORIGIN, origin);
        projectile.setData(ManaAttachments.OVERCHARGE_SNAPSHOT, rawSnapshot(source));
    }

    /** @return the origin the hits of {@code entity} run with, or null for ordinary entities (no own origin) */
    @Nullable
    public static ImpactOrigin originOf(Entity entity) {
        if (!entity.hasData(ManaAttachments.IMPACT_ORIGIN)) {
            return null;
        }
        ImpactOrigin origin = entity.getData(ManaAttachments.IMPACT_ORIGIN);
        return origin == ImpactOrigin.DIRECT ? null : origin;
    }
}
