package com.pockyl.hextale.port.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A 26.x item tint; on 1.21.1 {@link ItemTints} turns the tints named in the item definitions into item colours.
 *
 * <p>port-of: net.minecraft.client.color.item.ItemTintSource
 */
public interface ItemTintSource {
    int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner);

    MapCodec<? extends ItemTintSource> type();
}
