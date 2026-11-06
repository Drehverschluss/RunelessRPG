package com.drehverschluss.runelessrpg.potion;

import com.drehverschluss.runelessrpg.cost.CostSettings;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * A potion with one effect whose duration comes from the server config ({@link CostSettings}, synced to clients) at
 * the moment it is read, not when the potion is registered: registries are built before any config is loaded.
 * Vanilla reads the effects of a potion only through {@link #getEffects()} (drinking, splash, lingering cloud, tipped
 * arrows, tooltip, color), so changed durations apply to every potion, also ones brewed before.
 */
public class ConfiguredPotion extends Potion {
    private final Holder<MobEffect> effect;
    private final int amplifier;
    private final ToIntFunction<CostSettings> durationSeconds;

    public ConfiguredPotion(String name, Holder<MobEffect> effect, int amplifier, ToIntFunction<CostSettings> durationSeconds) {
        super(name, new MobEffectInstance(effect, durationSeconds.applyAsInt(CostSettings.DEFAULTS) * 20, amplifier));
        this.effect = effect;
        this.amplifier = amplifier;
        this.durationSeconds = durationSeconds;
    }

    @Override
    public List<MobEffectInstance> getEffects() {
        return List.of(new MobEffectInstance(effect, durationSeconds.applyAsInt(CostSettings.current()) * 20, amplifier));
    }
}
