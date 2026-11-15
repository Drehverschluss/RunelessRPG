package com.drehverschluss.runelessrpg.enchantment;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.cost.CostSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.Optional;

/**
 * The loot-only leech enchantments {@code runelessrpg:mana_leech} and {@code runelessrpg:spell_mana_leech}. Their effect
 * is a plain attribute effect in the enchantment JSON; this class only switches them off when the server config says so.
 * <p>
 * A disabled enchantment gets level 0 in {@link GetEnchantmentLevelEvent}, the gameplay view of an item's enchantments.
 * Its attribute modifiers are then neither applied to the player nor shown in the item tooltip, also on items that
 * already have it. The stored enchantment stays on the item, so it works again when turned back on. Loot is cleaned by
 * {@link DisabledEnchantmentsLootModifier}.
 */
public final class LeechEnchantments {
    public static final ResourceKey<Enchantment> MANA_LEECH = key("mana_leech");
    public static final ResourceKey<Enchantment> SPELL_MANA_LEECH = key("spell_mana_leech");
    private static final List<ResourceKey<Enchantment>> ALL = List.of(MANA_LEECH, SPELL_MANA_LEECH);

    private LeechEnchantments() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LeechEnchantments::onGetEnchantmentLevel);
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, path));
    }

    /** Live server config. Before the config is loaded, everything counts as enabled. */
    public static boolean isEnabledOnServer(ResourceKey<Enchantment> key) {
        if (!RunelessServerConfig.SPEC.isLoaded()) {
            return true;
        }
        if (key == MANA_LEECH) {
            return RunelessServerConfig.MANA_LEECH_ENCHANTMENT_ENABLED.get();
        }
        return key != SPELL_MANA_LEECH || RunelessServerConfig.SPELL_MANA_LEECH_ENCHANTMENT_ENABLED.get();
    }

    /** The copy the client received from the server. */
    private static boolean isEnabledOnClient(ResourceKey<Enchantment> key) {
        CostSettings settings = CostSettings.client();
        if (key == MANA_LEECH) {
            return settings.manaLeechEnchantmentEnabled();
        }
        return key != SPELL_MANA_LEECH || settings.spellManaLeechEnchantmentEnabled();
    }

    public static boolean allEnabledOnServer() {
        return isEnabledOnServer(MANA_LEECH) && isEnabledOnServer(SPELL_MANA_LEECH);
    }

    /** True if the holder is one of ours and switched off in the server config. */
    public static boolean isDisabledOnServer(Holder<Enchantment> holder) {
        for (ResourceKey<Enchantment> key : ALL) {
            if (holder.is(key)) {
                return !isEnabledOnServer(key);
            }
        }
        return false;
    }

    /**
     * The event has no side: the dedicated server and the integrated server thread use the live config, everything else
     * (client thread, tooltips) the copy the client received. In singleplayer both are the same values.
     */
    private static boolean useServerConfig() {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            return true;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread();
    }

    // Fires very often (every enchantment iteration of every entity): leave as early as possible.
    private static void onGetEnchantmentLevel(GetEnchantmentLevelEvent event) {
        if (allEnabledOnServer() && isEnabledOnClient(MANA_LEECH) && isEnabledOnClient(SPELL_MANA_LEECH)) {
            return;
        }
        ItemEnchantments.Mutable enchantments = event.getEnchantments();
        Boolean serverSide = null;
        for (ResourceKey<Enchantment> key : ALL) {
            if (!event.isTargetting(key)) {
                continue;
            }
            Optional<Holder.Reference<Enchantment>> holder = event.getHolder(key);
            if (holder.isEmpty() || enchantments.getLevel(holder.get()) <= 0) {
                continue;
            }
            if (serverSide == null) {
                serverSide = useServerConfig();
            }
            if (!(serverSide ? isEnabledOnServer(key) : isEnabledOnClient(key))) {
                enchantments.set(holder.get(), 0);
            }
        }
    }

    /**
     * After a config change: brings the attribute modifiers of everything players have equipped in line with the new
     * setting. Vanilla only adds or removes enchantment modifiers when equipment changes, so without this an item held
     * during the change would keep (or keep missing) its modifier until it is unequipped.
     */
    public static void refreshEquipment(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HolderLookup.RegistryLookup<Enchantment> lookup = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            for (ResourceKey<Enchantment> key : ALL) {
                lookup.get(key).ifPresent(holder -> refresh(player, holder, isEnabledOnServer(key)));
            }
        }
    }

    private static void refresh(ServerPlayer player, Holder<Enchantment> holder, boolean enabled) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            // The stored level, not the gameplay level: the event would hide a disabled enchantment here
            int level = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(holder);
            if (level <= 0 || !holder.value().matchingSlot(slot)) {
                continue;
            }
            for (EnchantmentAttributeEffect effect : holder.value().getEffects(EnchantmentEffectComponents.ATTRIBUTES)) {
                AttributeInstance instance = player.getAttribute(effect.attribute());
                if (instance == null) {
                    continue;
                }
                AttributeModifier modifier = effect.getModifier(level, slot);
                instance.removeModifier(modifier.id());
                if (enabled) {
                    instance.addTransientModifier(modifier);
                }
            }
        }
    }
}
