package com.pockyl.hextale.port;

import net.minecraft.world.item.ArmorItem;

/**
 * The armour pieces of Minecraft 26.x, mapped onto the 1.21.1 {@link ArmorItem.Type}.
 *
 * <p>port-of: net.minecraft.world.item.equipment.ArmorType
 */
public enum ArmorType {
    HELMET(ArmorItem.Type.HELMET),
    CHESTPLATE(ArmorItem.Type.CHESTPLATE),
    LEGGINGS(ArmorItem.Type.LEGGINGS),
    BOOTS(ArmorItem.Type.BOOTS),
    BODY(ArmorItem.Type.BODY);

    private final ArmorItem.Type vanilla;

    ArmorType(ArmorItem.Type vanilla) {
        this.vanilla = vanilla;
    }

    public ArmorItem.Type vanilla() {
        return vanilla;
    }
}
