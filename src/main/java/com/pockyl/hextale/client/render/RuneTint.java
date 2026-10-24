package com.pockyl.hextale.client.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.rune.ComplexRune;

/** Item tint {@code hextale:rune}: the colour of a rune scroll's wax seal - its rune's colour, red for a blank one. */
public record RuneTint() implements ItemTintSource {
    public static final MapCodec<RuneTint> MAP_CODEC = MapCodec.unit(new RuneTint());
    private static final int BLANK = 0xA83228;

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        ComplexRune rune = RuneScrollItem.rune(stack);
        return FastColor.ARGB32.opaque(rune == null ? BLANK : rune.color());
    }

    @Override
    public MapCodec<RuneTint> type() {
        return MAP_CODEC;
    }
}
