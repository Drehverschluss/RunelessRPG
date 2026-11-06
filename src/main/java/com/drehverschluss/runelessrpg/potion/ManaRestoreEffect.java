package com.drehverschluss.runelessrpg.potion;

import com.drehverschluss.runelessrpg.api.ManaChangeEvent;
import com.drehverschluss.runelessrpg.config.RunelessServerConfig;
import com.drehverschluss.runelessrpg.mana.ManaManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Instant mana restoration ({@code runelessrpg:mana_restore}): (level) × {@code manaPotionPercentPerLevel} % of max
 * mana, players only. Splash potions and lingering clouds pass Vanilla's strength factor (less at the edge of the
 * splash), exactly like Instant Health.
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
        restore(living, amplifier, strength);
    }

    private static void restore(LivingEntity living, int amplifier, double strength) {
        if (!(living instanceof ServerPlayer player) || strength <= 0.0) {
            return;
        }
        double percent = (amplifier + 1) * RunelessServerConfig.MANA_POTION_PERCENT_PER_LEVEL.get();
        double amount = ManaManager.getMaxMana(player) * percent / 100.0 * strength;
        ManaManager.restoreMana(player, amount, ManaChangeEvent.Reason.POTION);
    }
}
