package com.drehverschluss.runelessrpg.cost;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How much mana the last failed cost check was missing: on the client for the local player (read when Spell Engine
 * shows its message), on the server per player (sent with the "not enough mana" packet).
 */
public final class ManaShortfall {
    private static volatile double lastClient;
    private static final Map<UUID, Double> SERVER = new HashMap<>();

    private ManaShortfall() {}

    static void record(Player player, double missing) {
        if (player.level().isClientSide()) {
            lastClient = missing;
        } else if (player instanceof ServerPlayer) {
            SERVER.put(player.getUUID(), missing);
        }
    }

    /** @return the amount the local player's last failed check was missing */
    public static double lastClient() {
        return lastClient;
    }

    /** @return the amount the player's last failed server check was missing (0 if unknown) */
    public static double server(ServerPlayer player) {
        return SERVER.getOrDefault(player.getUUID(), 0.0);
    }

    static void forget(ServerPlayer player) {
        SERVER.remove(player.getUUID());
    }
}
