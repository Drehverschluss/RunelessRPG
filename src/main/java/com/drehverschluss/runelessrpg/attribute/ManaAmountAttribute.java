package com.drehverschluss.runelessrpg.attribute;

import com.drehverschluss.runelessrpg.util.DisplayUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;
import org.jetbrains.annotations.Nullable;

/**
 * Ranged attribute for an amount of mana ({@code runelessrpg:max_mana}): flat values are shown in the display unit
 * ({@link DisplayUnit}), percent values unchanged. With factor 1 the text is the same as NeoForge's default.
 */
public class ManaAmountAttribute extends RangedAttribute {
    public ManaAmountAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue, min, max);
    }

    @Override
    public MutableComponent toValueComponent(@Nullable AttributeModifier.Operation op, double value, TooltipFlag flag) {
        if (IAttributeExtension.isNullOrAddition(op)) {
            return Component.translatable("neoforge.value.flat", FORMAT.format(value * DisplayUnit.factor()));
        }
        return super.toValueComponent(op, value, flag);
    }
}
