package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.api.ManaCategory;
import com.drehverschluss.runelessrpg.api.ManaCostEvent;
import com.drehverschluss.runelessrpg.attribute.RunelessAttributes;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig.ManaMode;
import com.drehverschluss.runelessrpg.cost.CastContext.Stage;
import com.drehverschluss.runelessrpg.cost.SpellCostInfo.Source;
import com.drehverschluss.runelessrpg.mana.ManaManager;
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
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.internals.SpellParameters;

import java.util.Optional;

/** Central cost logic, identical on client and server. */
public final class ManaCosts {
    private static final double EPSILON = 1.0E-9;

    private ManaCosts() {}

    /**
     * Resolves category and base cost. Order: non-active → NONE, free tag, datapack value, category tags
     * (item abilities, weapon skills, class skills, spells), rune cost item, otherwise NONE.
     */
    public static SpellCostInfo resolve(Level level, Holder<Spell> holder) {
        Spell spell = holder.value();
        boolean rune = isRuneCost(spell);
        boolean channeled = SpellParameters.isChanneled(spell);
        int tier = spell.tier;
        if (spell.type != Spell.Type.ACTIVE || spell.active == null) {
            return new SpellCostInfo(ManaCategory.NONE, Source.NONE, 0.0, rune, channeled, tier);
        }
        if (holder.is(CostTags.FREE)) {
            return new SpellCostInfo(ManaCategory.FREE, Source.FREE, 0.0, rune, channeled, tier);
        }

        boolean clientSide = level.isClientSide();
        CostSettings settings = CostSettings.forSide(clientSide);
        ManaCategory tagged = taggedCategory(holder);
        ManaCategory category = tagged != ManaCategory.NONE ? tagged : (rune ? ManaCategory.SPELL : ManaCategory.NONE);

        Optional<ResourceLocation> id = holder.unwrapKey().map(ResourceKey::location);
        Optional<Double> override = id.flatMap(spellId -> SpellCostOverrides.get(clientSide, spellId));
        if (override.isPresent()) {
            return new SpellCostInfo(category == ManaCategory.NONE ? ManaCategory.SPELL : category, Source.DATAPACK, override.get(), rune, channeled, tier);
        }
        if (category == ManaCategory.NONE || !isEnabled(settings, category)) {
            return new SpellCostInfo(ManaCategory.NONE, Source.NONE, 0.0, rune, channeled, tier);
        }
        double base = settings.tierCost(tier) * multiplier(settings, category);
        return new SpellCostInfo(category, tagged != ManaCategory.NONE ? Source.TAG : Source.RUNE, base, rune, channeled, tier);
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

    /** Effective {@code mana_cost} factor including its lower limit. */
    public static double manaCostFactor(Player player, Holder<Spell> holder) {
        CostSettings settings = CostSettings.forSide(player.level().isClientSide());
        double floor = settings.minManaCostPercent() / 100.0;
        double manaCost = player.getAttributeValue(RunelessAttributes.MANA_COST);
        return applySpellInfinity(player, holder, Math.max(floor, manaCost));
    }

    // Hook for Infinite Spells (Phase 5): will lower the cost factor and its floor. No effect yet.
    private static double applySpellInfinity(Player player, Holder<Spell> holder, double factor) {
        return factor;
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
            return affordable(settings, mana, total, total);
        }
        double perTick = costPerChannelTick(player, holder, total);
        return switch (settings.channelCostMode()) {
            case DRAIN -> switch (stage) {
                case START -> affordable(settings, mana, total * settings.channelStartPercent() / 100.0 + perTick, total);
                case CHANNEL_TICK -> affordable(settings, mana, perTick, total);
                default -> true;
            };
            case START -> stage != Stage.START || affordable(settings, mana, total, total);
            case END -> affordable(settings, mana, total, total);
        };
    }

    /**
     * Mana mode check. {@code reference} is the full cost of the spell (DEBT_LIMIT allows debt up to a percentage of it).
     */
    public static boolean affordable(CostSettings settings, double mana, double amount, double reference) {
        if (amount <= 0.0) {
            return true;
        }
        return switch (settings.manaMode()) {
            case FULL_COST -> mana + EPSILON >= amount;
            case DEBT -> mana > EPSILON;
            case DEBT_LIMIT -> mana - amount + EPSILON >= -(settings.maxDebtPercent() / 100.0 * reference);
        };
    }

    public static boolean allowsDebt(CostSettings settings) {
        return settings.manaMode() != ManaMode.FULL_COST;
    }
}
