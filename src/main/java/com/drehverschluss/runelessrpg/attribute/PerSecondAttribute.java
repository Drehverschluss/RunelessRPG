package com.drehverschluss.runelessrpg.attribute;

import com.drehverschluss.runelessrpg.RunelessRPG;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;
import org.jetbrains.annotations.Nullable;

/**
 * Ranged attribute whose flat values are shown as a rate in tooltips, e.g. "+0.5/s Mana Regeneration".
 */
public class PerSecondAttribute extends RangedAttribute {
    public PerSecondAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue, min, max);
    }

    @Override
    public MutableComponent toValueComponent(@Nullable AttributeModifier.Operation op, double value, TooltipFlag flag) {
        if (IAttributeExtension.isNullOrAddition(op)) {
            return Component.translatable("attribute." + RunelessRPG.MODID + ".value.per_second", FORMAT.format(value));
        }
        return super.toValueComponent(op, value, flag);
    }
}
