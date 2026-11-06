package com.drehverschluss.runelessrpg.potion;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.config.RunelessCommonConfig;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/** Mana effects, mana potions and their brewing recipes. */
public final class RunelessPotions {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, RunelessRPG.MODID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, RunelessRPG.MODID);

    /** Instant: restores a percentage of max mana. */
    public static final DeferredHolder<MobEffect, MobEffect> MANA_RESTORE = EFFECTS.register("mana_restore",
            () -> new ManaRestoreEffect(0x2E6BFF));
    /** Lowers {@code runelessrpg:mana_cost} (value per level from the server config). */
    public static final DeferredHolder<MobEffect, MobEffect> MAGICAL_STABILITY = EFFECTS.register("magical_stability",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x46D7E6) {}.addAttributeModifier(RunelessAttributes.MANA_COST,
                    id("effect.magical_stability"), AttributeModifier.Operation.ADD_VALUE,
                    amplifier -> -(amplifier + 1) * CostSettings.current().magicalStabilityManaCost()));
    /** Allows casting into debt down to a percentage of max mana; backlash damage when it ends in debt. */
    public static final DeferredHolder<MobEffect, MobEffect> BORROWED_POWER = EFFECTS.register("borrowed_power",
            () -> new MobEffect(MobEffectCategory.NEUTRAL, 0x6B4BE0) {});

    public static final DeferredHolder<Potion, Potion> MANA = POTIONS.register("mana",
            () -> new Potion(name("mana"), new MobEffectInstance(MANA_RESTORE, 1, 0)));
    public static final DeferredHolder<Potion, Potion> STRONG_MANA = POTIONS.register("strong_mana",
            () -> new Potion(name("mana"), new MobEffectInstance(MANA_RESTORE, 1, 1)));
    public static final DeferredHolder<Potion, Potion> MAGICAL_STABILITY_POTION = POTIONS.register("magical_stability",
            () -> new ConfiguredPotion(name("magical_stability"), MAGICAL_STABILITY, 0, CostSettings::magicalStabilityDurationSeconds));
    public static final DeferredHolder<Potion, Potion> LONG_MAGICAL_STABILITY_POTION = POTIONS.register("long_magical_stability",
            () -> new ConfiguredPotion(name("magical_stability"), MAGICAL_STABILITY, 0, CostSettings::longMagicalStabilityDurationSeconds));
    public static final DeferredHolder<Potion, Potion> BORROWED_POWER_POTION = POTIONS.register("borrowed_power",
            () -> new ConfiguredPotion(name("borrowed_power"), BORROWED_POWER, 0, CostSettings::borrowedPowerDurationSeconds));

    /** Damage when Borrowed Power ends in debt; in {@code #minecraft:bypasses_armor} and {@code #minecraft:bypasses_enchantments}. */
    public static final ResourceKey<DamageType> MANA_BACKLASH = ResourceKey.create(Registries.DAMAGE_TYPE, id("mana_backlash"));

    private RunelessPotions() {}

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        POTIONS.register(modBus);
        NeoForge.EVENT_BUS.addListener(RunelessPotions::onRegisterBrewing);
        BorrowedPower.register();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, path);
    }

    // Potion name for the item names: item.minecraft.potion.effect.runelessrpg.<name> (strong/long variants share it)
    private static String name(String base) {
        return RunelessRPG.MODID + "." + base;
    }

    /** Fired on client and server whenever the brewing recipes are built (server start, client login); the common config is loaded by then. */
    private static void onRegisterBrewing(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();
        mix(builder, Potions.AWKWARD, RunelessCommonConfig.MANA_INGREDIENT, MANA);
        mix(builder, MANA, RunelessCommonConfig.STRONG_MANA_INGREDIENT, STRONG_MANA);
        mix(builder, MANA, RunelessCommonConfig.MAGICAL_STABILITY_INGREDIENT, MAGICAL_STABILITY_POTION);
        mix(builder, MAGICAL_STABILITY_POTION, RunelessCommonConfig.LONG_MAGICAL_STABILITY_INGREDIENT, LONG_MAGICAL_STABILITY_POTION);
        mix(builder, MANA, RunelessCommonConfig.BORROWED_POWER_INGREDIENT, BORROWED_POWER_POTION);
    }

    private static void mix(PotionBrewing.Builder builder, Holder<Potion> input, ModConfigSpec.ConfigValue<String> ingredient, Holder<Potion> output) {
        ingredientItem(ingredient).ifPresent(item -> builder.addMix(input, item, output));
    }

    /** @return the configured item; empty (recipe disabled) for an empty value, or with a warning for an unknown id */
    private static Optional<Item> ingredientItem(ModConfigSpec.ConfigValue<String> config) {
        String value = config.get().trim();
        String key = String.join(".", config.getPath());
        if (value.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation itemId = ResourceLocation.tryParse(value);
        Optional<Item> item = itemId == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(itemId).filter(i -> i != Items.AIR);
        if (item.isEmpty()) {
            RunelessRPG.LOGGER.warn("runelessrpg-common.toml: {} = \"{}\" is not a valid item id, the brewing recipe is disabled", key, value);
        }
        return item;
    }
}
