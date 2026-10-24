package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import com.pockyl.hextale.port.client.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.runework.PickaxeRunes;

/**
 * Item tint {@code hextale:pickaxe_runes}: a hextech pickaxe's crystal glows in the blended colour of its runes (each
 * weighted by its stars), hextech cyan while it carries none.
 */
public record PickaxeRuneTint() implements ItemTintSource {
    public static final MapCodec<PickaxeRuneTint> MAP_CODEC = MapCodec.unit(new PickaxeRuneTint());
    private static final int PLAIN = 0x5CC8F2;

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        return FastColor.ARGB32.opaque(blend(PickaxeRunes.of(stack)));
    }

    /** The runes' colours mixed by their stars; hextech cyan for none (also the Hex Smelter's gem). */
    public static int blend(PickaxeRunes runes) {
        if (runes.isEmpty()) {
            return PLAIN;
        }
        float r = 0;
        float g = 0;
        float b = 0;
        float weight = 0;
        for (PickaxeRunes.Entry entry : runes.entries()) {
            int color = entry.rune().color();
            r += ((color >> 16) & 0xFF) * entry.stars();
            g += ((color >> 8) & 0xFF) * entry.stars();
            b += (color & 0xFF) * entry.stars();
            weight += entry.stars();
        }
        return Math.round(r / weight) << 16 | Math.round(g / weight) << 8 | Math.round(b / weight);
    }

    @Override
    public MapCodec<PickaxeRuneTint> type() {
        return MAP_CODEC;
    }
}
