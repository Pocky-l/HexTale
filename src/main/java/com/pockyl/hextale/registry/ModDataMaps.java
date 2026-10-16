package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.research.ItemEssences;

public final class ModDataMaps {
    /** The essences items carry on the research parchment: {@code data/<ns>/data_maps/item/essences.json}. */
    public static final DataMapType<Item, ItemEssences> ESSENCES = DataMapType.builder(Hextale.id("essences"), Registries.ITEM,
            ItemEssences.CODEC).synced(ItemEssences.CODEC, true).build();

    private ModDataMaps() {
    }

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(ESSENCES);
    }
}
