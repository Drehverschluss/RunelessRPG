package com.drehverschluss.runelessrpg.enchantment;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Optional;

/**
 * Global loot modifier {@code runelessrpg:remove_disabled_enchantments}: removes leech enchantments that are switched
 * off in the server config from all generated loot (chests, fishing, mob drops, other mods' tables). An enchanted book
 * left without enchantments gets another random enchantment from {@code #minecraft:on_random_loot}, like the vanilla
 * function {@code enchant_randomly}. Does nothing while both enchantments are enabled.
 */
public final class DisabledEnchantmentsLootModifier extends LootModifier {
    public static final MapCodec<DisabledEnchantmentsLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            instance -> codecStart(instance).apply(instance, DisabledEnchantmentsLootModifier::new));

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RunelessRPG.MODID);

    static {
        SERIALIZERS.register("remove_disabled_enchantments", () -> CODEC);
    }

    public DisabledEnchantmentsLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (LeechEnchantments.allEnabledOnServer()) {
            return generatedLoot;
        }
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            removeDisabled(stack, DataComponents.ENCHANTMENTS);
            if (removeDisabled(stack, DataComponents.STORED_ENCHANTMENTS) && stack.is(Items.ENCHANTED_BOOK)
                    && stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()) {
                generatedLoot.set(i, reroll(stack, context));
            }
        }
        return generatedLoot;
    }

    /** Returns true if something was removed. */
    private static boolean removeDisabled(ItemStack stack, DataComponentType<ItemEnchantments> type) {
        ItemEnchantments enchantments = stack.get(type);
        if (enchantments == null || enchantments.isEmpty()
                || enchantments.keySet().stream().noneMatch(LeechEnchantments::isDisabledOnServer)) {
            return false;
        }
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
        mutable.removeIf(LeechEnchantments::isDisabledOnServer);
        stack.set(type, mutable.toImmutable());
        return true;
    }

    /** A random enabled enchantment from #minecraft:on_random_loot at a random level, or a plain book if there is none. */
    private static ItemStack reroll(ItemStack book, LootContext context) {
        Optional<HolderSet.Named<Enchantment>> pool = context.getLevel().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.ON_RANDOM_LOOT);
        List<Holder<Enchantment>> candidates = pool.map(set -> set.stream()
                .filter(holder -> !LeechEnchantments.isDisabledOnServer(holder)).toList()).orElse(List.of());
        if (candidates.isEmpty()) {
            return new ItemStack(Items.BOOK, book.getCount());
        }
        RandomSource random = context.getRandom();
        Holder<Enchantment> enchantment = candidates.get(random.nextInt(candidates.size()));
        int level = Mth.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel());
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantment, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return book;
    }
}
