package com.pockyl.hextale.compat.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    /** The first stack worn in any Curios slot that passes the test, or empty (always empty without Curios). */
    public static ItemStack find(LivingEntity entity, java.util.function.Predicate<ItemStack> test) {
        return LOADED ? Api.find(entity, test) : ItemStack.EMPTY;
    }

    private static final class Api {
        static ItemStack find(LivingEntity entity, java.util.function.Predicate<ItemStack> test) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(entity)
                    .flatMap(curios -> curios.findFirstCurio(test)).map(top.theillusivec4.curios.api.SlotResult::stack)
                    .orElse(ItemStack.EMPTY);
        }

        static boolean wears(LivingEntity entity, Item item) {
            return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(entity)
                    .map(curios -> curios.isEquipped(item)).orElse(false);
        }
    }
}
