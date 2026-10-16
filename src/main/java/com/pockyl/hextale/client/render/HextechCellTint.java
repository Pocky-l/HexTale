package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.runework.HextechTools;

/** Item tint {@code hextale:hextech_cell}: a hextech tool's crystal glows in the colour of its cell; empty = dull slate. */
public record HextechCellTint() implements ItemTintSource {
    public static final MapCodec<HextechCellTint> MAP_CODEC = MapCodec.unit(new HextechCellTint());
    private static final int EMPTY = 0x5A6E7A;

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        EssenceMix cell = HextechTools.cell(stack);
        return ARGB.opaque(cell.isEmpty() ? EMPTY : PixelArt.mix(cell.color(), 0xFFFFFF, 0.15F));
    }

    @Override
    public MapCodec<HextechCellTint> type() {
        return MAP_CODEC;
    }
}
