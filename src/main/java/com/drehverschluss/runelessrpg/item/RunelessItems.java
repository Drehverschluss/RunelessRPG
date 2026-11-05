package com.drehverschluss.runelessrpg.item;

import com.drehverschluss.runelessrpg.RunelessRPG;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RunelessItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RunelessRPG.MODID);

    /** Placeholder shown by Spell Engine's "Missing …" message when a spell fails for lack of mana. Not in any creative tab. */
    public static final DeferredItem<Item> MANA = ITEMS.registerSimpleItem("mana");

    private RunelessItems() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
