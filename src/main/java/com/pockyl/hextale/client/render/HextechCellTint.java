package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.runework.HextechTools;
import com.pockyl.hextale.runework.RuneSlots;

/**
 * Item tint {@code hextale:hextech_cell}: a hextech tool's crystal glows in the colour of its runes (blended); without
 * runes it is a dull slate.
 */
public record HextechCellTint() implements ItemTintSource {
    public static final MapCodec<HextechCellTint> MAP_CODEC = MapCodec.unit(new HextechCellTint());
    private static final int EMPTY = 0x5A6E7A;

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        RuneSlots runes = HextechTools.runes(stack);
        if (runes.isEmpty()) {
            return ARGB.opaque(EMPTY);
        }
        int color = -1;
        int count = 0;
        for (RuneSlots.Engraving engraving : runes.engravings()) {
            int rune = engraving.rune().color();
            color = count == 0 ? rune : PixelArt.mix(color, rune, 1F / (count + 1));
            count++;
        }
        return ARGB.opaque(PixelArt.mix(color, 0xFFFFFF, 0.15F));
    }

    @Override
    public MapCodec<HextechCellTint> type() {
        return MAP_CODEC;
    }
}
