package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.api.ManaCategory;
import com.drehverschluss.runelessrpg.api.ManaCostEvent;
import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig.ManaMode;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig.SpellInfinityMode;
import com.drehverschluss.runelessrpg.cost.CastContext.Stage;
import com.drehverschluss.runelessrpg.cost.SpellCostInfo.Source;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import com.drehverschluss.runelessrpg.potion.BorrowedPower;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellParameters;

import java.util.Optional;

/** Central cost logic, identical on client and server. */
public final class ManaCosts {
    private static final double EPSILON = 1.0E-9;
    private static final ResourceLocation SPELL_INFINITY = ResourceLocation.fromNamespaceAndPath("spell_engine", "spell_infinity");

    private ManaCosts() {}

    /**
     * Resolves category and base cost. Order: non-active → NONE, free tag, fixed datapack value, category tags
     * (item abilities, weapon skills, class skills, spells), rune cost item, otherwise NONE. A factor from a data file
     * scales the tier cost of an enabled category.
     */
    public static SpellCostInfo resolve(Level level, Holder<Spell> holder) {
        Spell spell = holder.value();
        boolean rune = isRuneCost(spell);
        boolean channeled = SpellParameters.isChanneled(spell);
        int tier = spell.tier;
        if (spell.type != Spell.Type.ACTIVE || spell.active == null) {
            return SpellCostInfo.noCost(ManaCategory.NONE, Source.NONE, rune, channeled, tier);
        }
        if (holder.is(CostTags.FREE)) {
            return SpellCostInfo.noCost(ManaCategory.FREE, Source.FREE, rune, channeled, tier);
        }

        boolean clientSide = level.isClientSide();
        CostSettings settings = CostSettings.forSide(clientSide);
        ManaCategory tagged = taggedCategory(holder);
        ManaCategory category = tagged != ManaCategory.NONE ? tagged : (rune ? ManaCategory.SPELL : ManaCategory.NONE);

        Optional<ResourceLocation> id = holder.unwrapKey().map(ResourceKey::location);
        Optional<SpellCostOverrides.Value> override = id.flatMap(spellId -> SpellCostOverrides.get(clientSide, spellId));
        Source dataSource = override.map(value -> value.builtin() ? Source.BUILTIN : Source.DATAPACK).orElse(null);
        if (override.isPresent() && !override.get().factor()) {
            // Fixed value: always applies, also for a turned-off category
            return new SpellCostInfo(category == ManaCategory.NONE ? ManaCategory.SPELL : category, dataSource, override.get().amount(),
                    rune, channeled, tier, true, 0.0, 1.0, 0.0);
        }
        if (category == ManaCategory.NONE || !isEnabled(settings, category)) {
            return SpellCostInfo.noCost(ManaCategory.NONE, Source.NONE, rune, channeled, tier);
        }
        // Factor from a data file: replaces nothing but scales the tier cost, so it follows tierCosts and the category
        double factor = override.map(SpellCostOverrides.Value::amount).orElse(1.0);
        double tierCost = settings.tierCost(tier);
        double multiplier = multiplier(settings, category);
        Source source = dataSource != null ? dataSource : (tagged != ManaCategory.NONE ? Source.TAG : Source.RUNE);
        return new SpellCostInfo(category, source, tierCost * factor * multiplier, rune, channeled, tier, false, tierCost, factor, multiplier);
    }

    private static ManaCategory taggedCategory(Holder<Spell> holder) {
        if (holder.is(CostTags.ITEM_ABILITIES)) {
            return ManaCategory.ITEM_ABILITY;
        }
        if (holder.is(CostTags.WEAPON_SKILLS)) {
            return ManaCategory.WEAPON_SKILL;
        }
        if (holder.is(CostTags.CLASS_SKILLS)) {
            return ManaCategory.CLASS_SKILL;
        }
        if (holder.is(CostTags.SPELLS)) {
            return ManaCategory.SPELL;
        }
        return ManaCategory.NONE;
    }

    private static boolean isEnabled(CostSettings settings, ManaCategory category) {
        return switch (category) {
            case SPELL -> settings.spellsEnabled();
            case CLASS_SKILL -> settings.classSkillsEnabled();
            case WEAPON_SKILL -> settings.weaponSkillsEnabled();
            case ITEM_ABILITY -> settings.itemAbilitiesEnabled();
            default -> false;
        };
    }

    private static double multiplier(CostSettings settings, ManaCategory category) {
        return switch (category) {
            case SPELL -> settings.spellsMultiplier();
            case CLASS_SKILL -> settings.classSkillsMultiplier();
            case WEAPON_SKILL -> settings.weaponSkillsMultiplier();
            case ITEM_ABILITY -> settings.itemAbilitiesMultiplier();
            default -> 0.0;
        };
    }

    /** True if Spell Engine's cost item of the spell is a rune (item in {@code #runes:runes}, or a tag containing runes). */
    public static boolean isRuneCost(Spell spell) {
        if (spell.cost == null || spell.cost.item == null || spell.cost.item.id == null || spell.cost.item.id.isEmpty()) {
            return false;
        }
        String id = spell.cost.item.id;
        if (id.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(id.substring(1));
            if (tagId == null) {
                return false;
            }
            if (tagId.equals(CostTags.RUNES.location())) {
                return true;
            }
            Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tagId));
            return tag.isPresent() && tag.get().stream().anyMatch(item -> item.is(CostTags.RUNES));
        }
        ResourceLocation itemId = ResourceLocation.tryParse(id);
        if (itemId == null) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item != Items.AIR && item.builtInRegistryHolder().is(CostTags.RUNES);
    }

    public static boolean isCreativeFree(Player player) {
        return player.getAbilities().instabuild && CostSettings.forSide(player.level().isClientSide()).creativeFreeCasting();
    }

    /** Effective {@code mana_cost} factor including its lower limit (and Spell Infinity on the held item). */
    public static double manaCostFactor(Player player, Holder<Spell> holder) {
        return manaCostFactor(player);
    }

    /** Effective {@code mana_cost} factor including its lower limit (and Spell Infinity on the held item); same for every spell. */
    public static double manaCostFactor(Player player) {
        CostSettings settings = CostSettings.forSide(player.level().isClientSide());
        double floorPercent = settings.minManaCostPercent();
        double manaCost = player.getAttributeValue(RunelessAttributes.MANA_COST);
        if (hasSpellInfinity(player, settings)) {
            floorPercent = Math.max(0.0, floorPercent - settings.spellInfinityFloorReduction());
            manaCost -= settings.spellInfinityCostReduction() / 100.0;
        }
        return Math.max(floorPercent / 100.0, manaCost);
    }

    /**
     * True if Spell Infinity lowers the cost: mode REDUCE_COST and the held item (the stack Spell Engine casts from,
     * {@code casterStack}) has {@code spell_engine:spell_infinity}.
     */
    public static boolean hasSpellInfinity(Player player, CostSettings settings) {
        if (settings.spellInfinityMode() != SpellInfinityMode.REDUCE_COST) {
            return false;
        }
        return player.level().registryAccess().registry(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, SPELL_INFINITY)))
                .map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, player.getMainHandItem()) > 0)
                .orElse(false);
    }

    /** Lower limit of the {@code mana_cost} factor in percent for this player right now (Spell Infinity lowers it). */
    public static double manaCostFloorPercent(Player player) {
        CostSettings settings = CostSettings.forSide(player.level().isClientSide());
        double floor = settings.minManaCostPercent();
        return hasSpellInfinity(player, settings) ? Math.max(0.0, floor - settings.spellInfinityFloorReduction()) : floor;
    }

    /**
     * Overcharge multiplier for damage / healing of the spell cast by this player right now: 1 + overcharge × factor,
     * only for spells that cost the player mana; 1 otherwise.
     */
    public static double overchargeBonus(Player player, Holder<Spell> holder) {
        SpellCostInfo info = resolve(player.level(), holder);
        if (!info.costsMana()) {
            return 1.0;
        }
        CostSettings settings = CostSettings.forSide(player.level().isClientSide());
        double overcharge = player.getAttributeValue(RunelessAttributes.OVERCHARGE);
        return 1.0 + Math.max(0.0, overcharge) * settings.overchargeDamageFactor();
    }

    /** Final cost for the player (total for the full channel duration of channeled spells). */
    public static double cost(Player player, Holder<Spell> holder, SpellCostInfo info) {
        if (!info.costsMana()) {
            return 0.0;
        }
        double overcharge = player.getAttributeValue(RunelessAttributes.OVERCHARGE);
        double cost = info.baseCost() * manaCostFactor(player, holder) * (1.0 + overcharge);
        ManaCostEvent event = NeoForge.EVENT_BUS.post(new ManaCostEvent(player, holder, info.category(), info.baseCost(), cost));
        return event.getCost();
    }

    public static int channelTicks(Player player, Holder<Spell> holder) {
        return Math.max(1, SpellParameters.channelTicks(player, holder));
    }

    public static double costPerChannelTick(Player player, Holder<Spell> holder, double totalCost) {
        return totalCost / channelTicks(player, holder);
    }

    /** Whether the player can pay for the given step of a cast. */
    public static boolean canPay(Player player, Holder<Spell> holder, SpellCostInfo info, Stage stage) {
        if (!info.costsMana() || isCreativeFree(player)) {
            return true;
        }
        CostSettings settings = CostSettings.forSide(player.level().isClientSide());
        double total = cost(player, holder, info);
        if (total <= 0.0) {
            return true;
        }
        double mana = ManaManager.getMana(player);
        if (!info.channeled()) {
            return affordable(player, settings, mana, total, total);
        }
        double perTick = costPerChannelTick(player, holder, total);
        return switch (settings.channelCostMode()) {
            case DRAIN -> switch (stage) {
                case START -> affordable(player, settings, mana, total * settings.channelStartPercent() / 100.0 + perTick, total);
                case CHANNEL_TICK -> affordable(player, settings, mana, perTick, total);
                default -> true;
            };
            case START -> stage != Stage.START || affordable(player, settings, mana, total, total);
            case END -> affordable(player, settings, mana, total, total);
        };
    }

    /**
     * How much mana the given step of a cast is missing (same steps and rules as {@link #canPay}); 0 if it is affordable.
     */
    public static double shortfall(Player player, Holder<Spell> holder, SpellCostInfo info, Stage stage) {
        if (!info.costsMana() || isCreativeFree(player)) {
            return 0.0;
        }
        CostSettings settings = CostSettings.forSide(player.level().isClientSide());
        double total = cost(player, holder, info);
        if (total <= 0.0) {
            return 0.0;
        }
        double mana = ManaManager.getMana(player);
        if (!info.channeled()) {
            return shortfall(player, settings, mana, total, total);
        }
        double perTick = costPerChannelTick(player, holder, total);
        return switch (settings.channelCostMode()) {
            case DRAIN -> switch (stage) {
                case START -> shortfall(player, settings, mana, total * settings.channelStartPercent() / 100.0 + perTick, total);
                case CHANNEL_TICK -> shortfall(player, settings, mana, perTick, total);
                default -> 0.0;
            };
            case START -> stage != Stage.START ? 0.0 : shortfall(player, settings, mana, total, total);
            case END -> shortfall(player, settings, mana, total, total);
        };
    }

    /**
     * Missing mana for {@link #affordable}: amount − (mana + allowed debt), at least 0. In the DEBT mode a cast needs
     * mana above 0, so it is at most the current debt.
     */
    public static double shortfall(Player player, CostSettings settings, double mana, double amount, double reference) {
        if (affordable(player, settings, mana, amount, reference)) {
            return 0.0;
        }
        double missing = amount - mana - debtLimit(player, settings, reference);
        if (settings.manaMode() == ManaMode.DEBT) {
            missing = Math.min(missing, -mana);
        }
        return Math.max(0.0, missing);
    }

    /**
     * Whether a player can pay {@code amount} for another mod's spell. With {@code allowDebt} the same rules as Spell
     * Engine spells apply ({@code manaMode}, {@code maxDebtPercent} of the amount, Borrowed Power); without it the full
     * amount is needed. Creative players with {@code creativeFreeCasting} can always pay.
     */
    public static boolean canAffordExternal(Player player, double amount, boolean allowDebt) {
        if (amount <= 0.0 || isCreativeFree(player)) {
            return true;
        }
        double mana = ManaManager.getMana(player);
        if (allowDebt) {
            return affordable(player, CostSettings.forSide(player.level().isClientSide()), mana, amount, amount);
        }
        return mana + EPSILON >= amount;
    }

    /** Missing mana for {@link #canAffordExternal}; 0 if affordable. */
    public static double shortfallExternal(Player player, double amount, boolean allowDebt) {
        if (amount <= 0.0 || isCreativeFree(player)) {
            return 0.0;
        }
        double mana = ManaManager.getMana(player);
        if (allowDebt) {
            return shortfall(player, CostSettings.forSide(player.level().isClientSide()), mana, amount, amount);
        }
        return mana + EPSILON >= amount ? 0.0 : amount - mana;
    }

    /**
     * Mana mode check. {@code reference} is the full cost of the spell (DEBT_LIMIT allows debt up to a percentage of it).
     * Borrowed Power allows debt down to a percentage of max mana in every mode; the more generous rule wins.
     */
    public static boolean affordable(Player player, CostSettings settings, double mana, double amount, double reference) {
        if (amount <= 0.0) {
            return true;
        }
        if (settings.manaMode() == ManaMode.DEBT && mana > EPSILON) {
            return true;
        }
        return mana - amount + EPSILON >= -debtLimit(player, settings, reference);
    }

    /**
     * How far below 0 a cast with full cost {@code reference} may take the mana right now (positive amount): the
     * DEBT_LIMIT of the mana mode or Borrowed Power, whichever is larger. DEBT mode has no limit (cast while mana is
     * above 0), it is not included here.
     */
    public static double debtLimit(Player player, CostSettings settings, double reference) {
        double modeLimit = settings.manaMode() == ManaMode.DEBT_LIMIT ? settings.maxDebtPercent() / 100.0 * reference : 0.0;
        return Math.max(modeLimit, BorrowedPower.debtLimit(player, settings));
    }

    /** Whether paying for spells may take the mana below 0 (a debt mode, or Borrowed Power). */
    public static boolean allowsDebt(Player player, CostSettings settings) {
        return settings.manaMode() != ManaMode.FULL_COST || BorrowedPower.isActive(player);
    }
}
