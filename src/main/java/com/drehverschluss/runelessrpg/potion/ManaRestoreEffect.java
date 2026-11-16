package com.drehverschluss.runelessrpg.potion;

import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Instant mana restoration ({@code runelessrpg:mana_restore}), players only.
 * <ul>
 *   <li>Drunk, splash, tipped arrow, {@code /effect give}: (level) × {@code manaPotionPercentPerLevel} % of max mana.
 *       Splash potions pass Vanilla's strength factor (less at the edge of the splash), exactly like Instant Health.
 *       Every such restoration gives Mana Sickness; during it only {@code manaSicknessRestorePercent} %
 *       ({@link ManaSickness}).</li>
 *   <li>Lingering cloud ({@link AreaEffectCloud} as the source, once per second per player): (level) ×
 *       {@code lingeringManaPercentPerLevel} % of max mana per application, Vanilla's half strength is ignored. No Mana
 *       Sickness, and an existing one does not weaken it.</li>
 * </ul>
 */
public class ManaRestoreEffect extends InstantenousMobEffect {
    public ManaRestoreEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }

    // Tipped arrows and /effect give: one application at full strength
    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        restore(living, amplifier, 1.0);
        return true;
    }

    @Override
    public void applyInstantenousEffect(@Nullable Entity source, @Nullable Entity indirectSource, LivingEntity living, int amplifier, double strength) {
        if (source instanceof AreaEffectCloud) {
            restoreFromCloud(living, amplifier);
        } else {
            restore(living, amplifier, strength);
        }
    }

    private static void restore(LivingEntity living, int amplifier, double strength) {
        if (!(living instanceof ServerPlayer player) || strength <= 0.0) {
            return;
        }
        double percent = (amplifier + 1) * RunelessServerConfig.MANA_POTION_PERCENT_PER_LEVEL.get();
        double amount = ManaManager.getMaxMana(player) * percent / 100.0 * strength * ManaSickness.restoreFactor(player);
        ManaSickness.trigger(player);
        if (amount > 0.0) {
            ManaManager.restoreMana(player, amount, ManaChangeEvent.Reason.POTION);
        }
    }

    private static void restoreFromCloud(LivingEntity living, int amplifier) {
        if (!(living instanceof ServerPlayer player)) {
            return;
        }
        double percent = (amplifier + 1) * RunelessServerConfig.LINGERING_MANA_PERCENT_PER_LEVEL.get();
        double amount = ManaManager.getMaxMana(player) * percent / 100.0;
        if (amount > 0.0) {
            ManaManager.restoreMana(player, amount, ManaChangeEvent.Reason.POTION);
        }
    }
}
