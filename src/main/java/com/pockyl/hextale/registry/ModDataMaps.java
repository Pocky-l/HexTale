package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.research.ItemElements;

public final class ModDataMaps {
    /** The elements items carry on the research parchment: {@code data/<ns>/data_maps/item/elements.json}. */
    public static final DataMapType<Item, ItemElements> ELEMENTS = DataMapType.builder(Hextale.id("elements"), Registries.ITEM,
            ItemElements.CODEC).synced(ItemElements.CODEC, true).build();

    private ModDataMaps() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(ELEMENTS);
    }
}
