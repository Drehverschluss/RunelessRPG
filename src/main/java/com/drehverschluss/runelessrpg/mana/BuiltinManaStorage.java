package com.drehverschluss.runelessrpg.mana;

import com.drehverschluss.runelessrpg.RunelessRPG;
import com.drehverschluss.runelessrpg.api.ManaStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * RunelessRPG's own storage: the saved {@link ManaAttachments#MANA} value on the player. It may go below 0 (debt),
 * regenerates through {@code runelessrpg:mana_regen} and leaves login, respawn and first fill to RunelessRPG.
 */
public final class BuiltinManaStorage implements ManaStorage {
    public static final BuiltinManaStorage INSTANCE = new BuiltinManaStorage();
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RunelessRPG.MODID, "builtin");

    private BuiltinManaStorage() {}

    @Override
    public ResourceLocation id() {
        return ID;
    }

    /** A player that never had mana (new player) starts full on the first access. */
    @Override
    public double getMana(ServerPlayer player) {
        double mana = player.getData(ManaAttachments.MANA);
        return Double.isNaN(mana) ? ManaManager.initialize(player) : mana;
    }

    @Override
    public double getMaxMana(ServerPlayer player) {
        return ManaManager.attributeMax(player);
    }

    @Override
    public void setMana(ServerPlayer player, double mana) {
        player.setData(ManaAttachments.MANA, mana);
    }

    @Override
    public boolean canGoNegative() {
        return true;
    }

    @Override
    public boolean regeneratesItself() {
        return false;
    }

    @Override
    public boolean handlesLifecycle() {
        return false;
    }
}
