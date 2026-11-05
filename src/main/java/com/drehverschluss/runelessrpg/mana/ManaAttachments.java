package com.drehverschluss.runelessrpg.mana;

import com.drehverschluss.runelessrpg.RunelessRPG;
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

    private ManaAttachments() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static final class ManaSyncState {
        /** Value last sent to the client; NaN if nothing was sent yet. */
        public double lastSynced = Double.NaN;
    }
}
