package com.pockyl.hextale.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

/**
 * Optional Curios support: the Researcher's Goggles also work from a Curios head slot (the slot and the item tag
 * {@code curios:head} are data in {@code data/hextale/curios/entities} and {@code data/curios/tags/item}). Only the nested
 * class touches the Curios API, so nothing loads it when Curios is absent.
 */
public final class CuriosCompat {
    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private CuriosCompat() {
    }

    /** Whether the entity wears this item in any Curios slot (false without Curios). */
    public static boolean wears(LivingEntity entity, Item item) {
        return LOADED && Api.wears(entity, item);
    }

    private static final class Api {
        static boolean wears(LivingEntity entity, Item item) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(entity)
                    .map(curios -> curios.isEquipped(item)).orElse(false);
        }
    }
}
