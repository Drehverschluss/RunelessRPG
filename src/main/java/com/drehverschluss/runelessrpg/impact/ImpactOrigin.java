package com.drehverschluss.runelessrpg.impact;

/** Where a hit inside Spell Engine's {@code SpellImpacts.performImpact} comes from. */
public enum ImpactOrigin {
    /** Projectile hit / impact, instant or area hit while casting, channel tick, melee spell, meteor landing. */
    DIRECT,
    /** A lasting spell cloud / zone ({@code SpellCloud.tick}). */
    CLOUD,
    /**
     * A projectile a summon launched on behalf of its owner (non-attackable summons like the Fire Hydra launch their
     * projectiles with the player as owner, so the hit itself looks like a player hit).
     */
    SUMMON,
    /**
     * An entity a spell placed with Spell Engine's SPAWN impact action (lasting effect: no mana leech), and projectiles
     * such an entity launches.
     */
    SPAWNED,
    /** A stashed spell released by a trigger ({@code SpellStashHelper.useStashes}). */
    STASH
}
