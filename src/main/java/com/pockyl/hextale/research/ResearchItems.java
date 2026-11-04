package com.pockyl.hextale.research;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.registry.ModDataMaps;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.pockyl.hextale.rune.Essence;


/**
 * Items and the essences they carry. In game the essences of every item come from {@link ItemEssenceTable}: the
 * {@code hextale:essences} data map (hand-tuned, and what data packs and modpacks set) plus what is derived from
 * recipes and names for everything it does not name.
 */
public final class ResearchItems {
    private ResearchItems() {
    }

    /**
     * The hand-tuned items: everything the {@code hextale:essences} data map names (this mod's
     * {@code data/hextale/data_maps/item/essences.json} plus whatever data packs add or replace).
     */
    public static Map<Item, ItemEssences> canonical() {
        Map<Item, ItemEssences> out = new LinkedHashMap<>();
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            @SuppressWarnings("deprecation")
            ItemEssences tuned = item.builtInRegistryHolder().getData(ModDataMaps.ESSENCES);
            if (tuned != null) {
                out.put(item, tuned);
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * What an item carries in this world: every item carries something once the server has resolved the table (see
     * {@link ItemEssenceResolver}); before that only the data map's base materials do. Null for air.
     */
    @Nullable
    public static ItemEssences of(Item item) {
        ItemEssences resolved = ItemEssenceTable.get(item);
        if (resolved != null) {
            return resolved;
        }
        @SuppressWarnings("deprecation")
        Holder<Item> holder = item.builtInRegistryHolder();
        return holder.getData(ModDataMaps.ESSENCES);
    }

    /**
     * What this very stack carries: an essence item is its one essence, a bottle what is poured into it (its three
     * strongest essences, 1 point a mB); everything else is its item's essences.
     */
    @Nullable
    public static ItemEssences of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Essence kind = com.pockyl.hextale.item.EssenceItem.essence(stack);
        if (kind != null) {
            return ItemEssences.of(kind.isPrimal() ? 1 : 2, kind, ESSENCE_ITEM_POINTS);
        }
        if (stack.is(com.pockyl.hextale.registry.ModItems.ESSENCE_FLASK.get())) {
            ItemEssences poured = poured(com.pockyl.hextale.essence.EssenceFlaskItem.contents(stack));
            if (poured != null) {
                return poured;
            }
        }
        return of(stack.getItem());
    }

    /** Points of its essence an essence item carries. */
    public static final int ESSENCE_ITEM_POINTS = 4;

    @Nullable
    private static ItemEssences poured(com.pockyl.hextale.essence.EssenceMix mix) {
        java.util.List<Map.Entry<Essence, Integer>> known = new java.util.ArrayList<>(mix.asMap().entrySet());
        if (known.isEmpty()) {
            return null;
        }
        known.sort((a, b) -> b.getValue() - a.getValue() != 0 ? b.getValue() - a.getValue() : a.getKey().ordinal() - b.getKey().ordinal());
        Map<Essence, Integer> kept = new LinkedHashMap<>();
        for (Map.Entry<Essence, Integer> entry : known.subList(0, Math.min(3, known.size()))) {
            kept.put(entry.getKey(), Math.min(64, entry.getValue()));
        }
        return new ItemEssences(kept, 1);
    }
}
