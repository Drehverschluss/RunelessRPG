package com.drehverschluss.runelessrpg.compat.apotheosis;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixDefinition;
import dev.shadowsoffire.apotheosis.affix.AttributeAffix;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.placebo.codec.PlaceboCodecs;
import dev.shadowsoffire.placebo.util.StepFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Affix type {@code runelessrpg:attribute}: Apotheosis' {@code apotheosis:attribute} (same fields) plus two optional
 * item filters on top of the loot categories.
 * <ul>
 *   <li>{@code items}: item tag the item must be in</li>
 *   <li>{@code exclude_items}: item tag the item must not be in</li>
 * </ul>
 * Apotheosis has no loot category for spell weapons (staffs and wands fall into {@code apotheosis:melee_weapon} by
 * their attack damage), and a separate category would take all melee affixes away from them. The filters let mana
 * affixes target spell weapons inside the melee category without touching the other affixes.
 */
public class FilteredAttributeAffix extends AttributeAffix {
    /** Item tag, written as {@code "#namespace:path"} (the {@code #} is optional). */
    private static final Codec<TagKey<Item>> ITEM_TAG = Codec.STRING.comapFlatMap(value -> {
        ResourceLocation id = ResourceLocation.tryParse(value.startsWith("#") ? value.substring(1) : value);
        return id == null ? DataResult.error(() -> "Invalid item tag: " + value) : DataResult.success(TagKey.create(Registries.ITEM, id));
    }, tag -> "#" + tag.location());

    public static final Codec<FilteredAttributeAffix> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                    Affix.<FilteredAttributeAffix>affixDef(),
                    BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(a -> a.attribute),
                    PlaceboCodecs.enumCodec(Operation.class).fieldOf("operation").forGetter(a -> a.operation),
                    LootRarity.mapCodec(StepFunction.CODEC).fieldOf("values").forGetter(a -> a.values),
                    LootCategory.SET_CODEC.fieldOf("categories").forGetter(a -> a.categories),
                    ITEM_TAG.optionalFieldOf("items").forGetter(a -> a.items),
                    ITEM_TAG.optionalFieldOf("exclude_items").forGetter(a -> a.excludeItems))
            .apply(inst, FilteredAttributeAffix::new));

    private final Optional<TagKey<Item>> items;
    private final Optional<TagKey<Item>> excludeItems;

    public FilteredAttributeAffix(AffixDefinition def, Holder<Attribute> attribute, Operation operation, Map<LootRarity, StepFunction> values,
                                  Set<LootCategory> categories, Optional<TagKey<Item>> items, Optional<TagKey<Item>> excludeItems) {
        super(def, attribute, operation, values, categories);
        this.items = items;
        this.excludeItems = excludeItems;
    }

    @Override
    public boolean canApplyTo(ItemStack stack, LootCategory cat, LootRarity rarity) {
        if (!super.canApplyTo(stack, cat, rarity)) {
            return false;
        }
        if (items.isPresent() && !stack.is(items.get())) {
            return false;
        }
        return excludeItems.isEmpty() || !stack.is(excludeItems.get());
    }

    @Override
    public Codec<? extends Affix> getCodec() {
        return CODEC;
    }
}
