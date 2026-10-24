package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import com.pockyl.hextale.port.client.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.item.EssenceStickerItem;
import com.pockyl.hextale.rune.Essence;

/** Item tint {@code hextale:sticker}: the colour of the essence on a sticker; a blank sticker's seal stays grey. */
public record StickerTint() implements ItemTintSource {
    public static final MapCodec<StickerTint> MAP_CODEC = MapCodec.unit(new StickerTint());
    private static final int BLANK = 0xB8B0A0;

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        Essence essence = EssenceStickerItem.essence(stack);
        return FastColor.ARGB32.opaque(essence == null ? BLANK : essence.color());
    }

    @Override
    public MapCodec<StickerTint> type() {
        return MAP_CODEC;
    }
}
