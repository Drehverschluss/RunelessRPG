package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.RunelessRPG;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;

public final class CostTags {
    public static final TagKey<Spell> SPELLS = spell("cost/spells");
    public static final TagKey<Spell> CLASS_SKILLS = spell("cost/class_skills");
    public static final TagKey<Spell> WEAPON_SKILLS = spell("cost/weapon_skills");
    public static final TagKey<Spell> ITEM_ABILITIES = spell("cost/item_abilities");
    public static final TagKey<Spell> FREE = spell("cost/free");

    /** All rune items (includes the stones of mrpg_lib). */
    public static final TagKey<Item> RUNES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("runes", "runes"));

    private CostTags() {}

    private static TagKey<Spell> spell(String path) {
        return TagKey.create(SpellRegistry.KEY, ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, path));
    }
}
