package com.drehverschluss.runelessrpg.api;

/** Cost category of a spell, as resolved by RunelessRPG. */
public enum ManaCategory {
    /** Not handled by RunelessRPG; Spell Engine costs apply unchanged. */
    NONE,
    /** In {@code #runelessrpg:cost/free}: costs nothing (runes are not consumed either). */
    FREE,
    /** Spells that cost runes, or are in {@code #runelessrpg:cost/spells}. */
    SPELL,
    /** {@code #runelessrpg:cost/class_skills} */
    CLASS_SKILL,
    /** {@code #runelessrpg:cost/weapon_skills} */
    WEAPON_SKILL,
    /** {@code #runelessrpg:cost/item_abilities} */
    ITEM_ABILITY
}
