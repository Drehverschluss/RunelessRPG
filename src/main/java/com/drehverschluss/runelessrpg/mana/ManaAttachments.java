package com.drehverschluss.runelessrpg.mana;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.impact.ImpactOrigin;
import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ManaAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RunelessRPG.MODID);

    /** Current mana of a player. Saved with the player, not copied on death (respawn refills it). */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Double>> MANA = ATTACHMENTS.register("mana",
            () -> AttachmentType.builder(() -> 0.0).serialize(Codec.DOUBLE).build());

    /** Runtime-only sync bookkeeping, never saved. */
    public static final Supplier<AttachmentType<ManaSyncState>> SYNC_STATE = ATTACHMENTS.register("mana_sync_state",
            () -> AttachmentType.builder(ManaSyncState::new).build());

    /**
     * Overcharge bonus (multiplier, 1 = none) a summon or spell cloud was created with: the overcharge of the player
     * at that moment, if the creating spell cost him mana. Saved with the entity, never sent to clients.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Double>> OVERCHARGE_SNAPSHOT = ATTACHMENTS.register(
            "overcharge_snapshot", () -> AttachmentType.builder(() -> 1.0).serialize(Codec.DOUBLE).build());

    /**
     * Origin of everything an entity causes ({@link ImpactOrigin#SPAWNED} for spell-spawned entities, the inherited
     * origin for projectiles of summons, spawned entities and clouds). Saved with the entity, never sent to clients.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ImpactOrigin>> IMPACT_ORIGIN = ATTACHMENTS.register(
            "impact_origin", () -> AttachmentType.builder(() -> ImpactOrigin.DIRECT)
                    .serialize(Codec.STRING.xmap(ImpactOrigin::valueOf, ImpactOrigin::name)).build());

    private ManaAttachments() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static final class ManaSyncState {
        /** Value last sent to the client; NaN if nothing was sent yet. */
        public double lastSynced = Double.NaN;
    }
}
