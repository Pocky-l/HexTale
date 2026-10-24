package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.essence.EssenceFlaskItem;

/** Item tint {@code hextale:essence}: the colour of the essence in a flask, its elements mixed by their drops. */
public record EssenceTint() implements ItemTintSource {
    public static final MapCodec<EssenceTint> MAP_CODEC = MapCodec.unit(new EssenceTint());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return FastColor.ARGB32.opaque(EssenceFlaskItem.contents(stack).color());
    }

    @Override
    public MapCodec<EssenceTint> type() {
        return MAP_CODEC;
    }
}
