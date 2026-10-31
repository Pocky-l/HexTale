package com.pockyl.hextale.thevoid.realm;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;

import com.pockyl.hextale.registry.ModItems;

/**
 * The Void Lens: a spyglass whose lens is a Hollow Crystal. Looking through it, the Void's haze thins away and what the
 * Void hides glints through (riftmetal in the husks, hollow crystals, skin worn too thin to bear a step). All of it is
 * drawn on the looking player's own screen ({@code client/VoidLensView}).
 */
public class VoidLensItem extends SpyglassItem {
    public VoidLensItem(Properties properties) {
        super(properties);
    }

    /** Whether the entity is looking through a Void Lens right now. */
    public static boolean looking(LivingEntity entity) {
        ItemStack using = entity.getUseItem();
        return entity.isUsingItem() && using.is(ModItems.VOID_LENS.get());
    }
}
