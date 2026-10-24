package com.pockyl.hextale.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * The elements of every item in the current world. The server resolves them when data packs load
 * ({@link ItemEssenceResolver}) and sends the table to every client, so tooltips and previews agree with the server.
 */
public final class ItemEssenceTable {
    private static volatile Map<Item, ItemEssences> table = Map.of();

    private ItemEssenceTable() {
    }

    @Nullable
    public static ItemEssences get(Item item) {
        return table.get(item);
    }

    public static boolean isEmpty() {
        return table.isEmpty();
    }

    public static void set(Map<Item, ItemEssences> resolved) {
        table = Collections.unmodifiableMap(new HashMap<>(resolved));
    }

    /** By item id, for the network. */
    public static Map<ResourceLocation, ItemEssences> byId() {
        Map<ResourceLocation, ItemEssences> out = new LinkedHashMap<>();
        table.forEach((item, elements) -> out.put(BuiltInRegistries.ITEM.getKey(item), elements));
        return out;
    }

    public static void setById(Map<ResourceLocation, ItemEssences> byId) {
        Map<Item, ItemEssences> resolved = new HashMap<>();
        byId.forEach((id, elements) -> BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> resolved.put(item, elements)));
        set(resolved);
    }
}
