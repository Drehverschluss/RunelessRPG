package com.drehverschluss.runelessrpg.attribute;

import com.drehverschluss.runelessrpg.RunelessRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RunelessAttributes {
    // Attribute icons (assets/runelessrpg/textures/attribute_icon/<attribute>.png, read by Apothic Attributes and Gearscope):
    // Clockwork Raven Studios, "RPG Icon Pack - 400+ Skills and Spells"; mana_cost recolored, the two leech icons with a "+".
    // Credit them in the README credits list.
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, RunelessRPG.MODID);

    public static final DeferredHolder<Attribute, Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
            () -> new RangedAttribute(key("max_mana"), 20.0, 0.0, 100000.0)
                    .setSyncable(true).setSentiment(Attribute.Sentiment.POSITIVE));
    public static final DeferredHolder<Attribute, Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regen",
            () -> new PerSecondAttribute(key("mana_regen"), 0.5, -1000.0, 1000.0)
                    .setSyncable(true).setSentiment(Attribute.Sentiment.POSITIVE));
    public static final DeferredHolder<Attribute, Attribute> MANA_COST = ATTRIBUTES.register("mana_cost",
            () -> new PercentageAttribute(key("mana_cost"), 1.0, 0.0, 10.0)
                    .setSyncable(true).setSentiment(Attribute.Sentiment.NEGATIVE));
    public static final DeferredHolder<Attribute, Attribute> OVERCHARGE = ATTRIBUTES.register("overcharge",
            () -> new PercentageAttribute(key("overcharge"), 0.0, 0.0, 10.0)
                    .setSyncable(true).setSentiment(Attribute.Sentiment.NEUTRAL));
    public static final DeferredHolder<Attribute, Attribute> MANA_LEECH = ATTRIBUTES.register("mana_leech",
            () -> new PercentageAttribute(key("mana_leech"), 0.0, 0.0, 1.0)
                    .setSyncable(true).setSentiment(Attribute.Sentiment.POSITIVE));
    public static final DeferredHolder<Attribute, Attribute> SPELL_MANA_LEECH = ATTRIBUTES.register("spell_mana_leech",
            () -> new PercentageAttribute(key("spell_mana_leech"), 0.0, 0.0, 1.0)
                    .setSyncable(true).setSentiment(Attribute.Sentiment.POSITIVE));

    private RunelessAttributes() {}

    private static String key(String name) {
        return "attribute.name." + RunelessRPG.MODID + "." + name;
    }

    public static void register(IEventBus modBus) {
        ATTRIBUTES.register(modBus);
        modBus.addListener(RunelessAttributes::onAttributeModification);
    }

    // Players only: mobs never get mana attributes.
    private static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_MANA);
        event.add(EntityType.PLAYER, MANA_REGEN);
        event.add(EntityType.PLAYER, MANA_COST);
        event.add(EntityType.PLAYER, OVERCHARGE);
        event.add(EntityType.PLAYER, MANA_LEECH);
        event.add(EntityType.PLAYER, SPELL_MANA_LEECH);
    }
}
