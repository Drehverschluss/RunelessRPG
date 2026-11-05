package com.drehverschluss.runelessrpg.cost;

import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig.ChannelCostMode;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig.ManaMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of all cost-related server settings. The server uses the live config, the client the copy it received
 * from the server ({@link #client()}), so both sides calculate the same costs.
 */
public record CostSettings(
        ManaMode manaMode,
        double maxDebtPercent,
        boolean creativeFreeCasting,
        double minManaCostPercent,
        List<Double> tierCosts,
        boolean spellsEnabled, double spellsMultiplier,
        boolean classSkillsEnabled, double classSkillsMultiplier,
        boolean weaponSkillsEnabled, double weaponSkillsMultiplier,
        boolean itemAbilitiesEnabled, double itemAbilitiesMultiplier,
        ChannelCostMode channelCostMode,
        double channelStartPercent) {

    public static final CostSettings DEFAULTS = new CostSettings(ManaMode.FULL_COST, 50.0, true, 50.0,
            List.of(4.0, 5.0, 7.0, 10.0, 14.0, 18.0), true, 1.0, true, 0.75, true, 0.5, false, 1.0, ChannelCostMode.DRAIN, 0.0);

    public static final StreamCodec<FriendlyByteBuf, CostSettings> STREAM_CODEC = StreamCodec.of(CostSettings::write, CostSettings::read);

    private static volatile CostSettings clientSettings = DEFAULTS;

    public static CostSettings forSide(boolean clientSide) {
        return clientSide ? clientSettings : server();
    }

    public static CostSettings client() {
        return clientSettings;
    }

    public static void setClient(CostSettings settings) {
        clientSettings = settings;
    }

    public static CostSettings server() {
        List<Double> tiers = new ArrayList<>();
        for (Number number : RunelessServerConfig.TIER_COSTS.get()) {
            tiers.add(number.doubleValue());
        }
        return new CostSettings(
                RunelessServerConfig.MANA_MODE.get(),
                RunelessServerConfig.MAX_DEBT_PERCENT.get(),
                RunelessServerConfig.CREATIVE_FREE_CASTING.get(),
                RunelessServerConfig.MIN_MANA_COST_PERCENT.get(),
                List.copyOf(tiers),
                RunelessServerConfig.SPELLS_ENABLED.get(), RunelessServerConfig.SPELLS_MULTIPLIER.get(),
                RunelessServerConfig.CLASS_SKILLS_ENABLED.get(), RunelessServerConfig.CLASS_SKILLS_MULTIPLIER.get(),
                RunelessServerConfig.WEAPON_SKILLS_ENABLED.get(), RunelessServerConfig.WEAPON_SKILLS_MULTIPLIER.get(),
                RunelessServerConfig.ITEM_ABILITIES_ENABLED.get(), RunelessServerConfig.ITEM_ABILITIES_MULTIPLIER.get(),
                RunelessServerConfig.CHANNEL_COST_MODE.get(),
                RunelessServerConfig.CHANNEL_START_PERCENT.get());
    }

    /** Tier table lookup: higher tiers use the last value, negative tiers the first. */
    public double tierCost(int tier) {
        if (tierCosts.isEmpty()) {
            return 0.0;
        }
        int index = Math.max(0, Math.min(tier, tierCosts.size() - 1));
        return tierCosts.get(index);
    }

    private static void write(FriendlyByteBuf buf, CostSettings s) {
        buf.writeEnum(s.manaMode);
        buf.writeDouble(s.maxDebtPercent);
        buf.writeBoolean(s.creativeFreeCasting);
        buf.writeDouble(s.minManaCostPercent);
        buf.writeVarInt(s.tierCosts.size());
        for (double cost : s.tierCosts) {
            buf.writeDouble(cost);
        }
        buf.writeBoolean(s.spellsEnabled);
        buf.writeDouble(s.spellsMultiplier);
        buf.writeBoolean(s.classSkillsEnabled);
        buf.writeDouble(s.classSkillsMultiplier);
        buf.writeBoolean(s.weaponSkillsEnabled);
        buf.writeDouble(s.weaponSkillsMultiplier);
        buf.writeBoolean(s.itemAbilitiesEnabled);
        buf.writeDouble(s.itemAbilitiesMultiplier);
        buf.writeEnum(s.channelCostMode);
        buf.writeDouble(s.channelStartPercent);
    }

    private static CostSettings read(FriendlyByteBuf buf) {
        ManaMode manaMode = buf.readEnum(ManaMode.class);
        double maxDebt = buf.readDouble();
        boolean creative = buf.readBoolean();
        double minPercent = buf.readDouble();
        int size = buf.readVarInt();
        List<Double> tiers = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            tiers.add(buf.readDouble());
        }
        return new CostSettings(manaMode, maxDebt, creative, minPercent, List.copyOf(tiers),
                buf.readBoolean(), buf.readDouble(), buf.readBoolean(), buf.readDouble(),
                buf.readBoolean(), buf.readDouble(), buf.readBoolean(), buf.readDouble(),
                buf.readEnum(ChannelCostMode.class), buf.readDouble());
    }
}
