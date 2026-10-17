package com.pockyl.hextale.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;

import org.jspecify.annotations.Nullable;

/**
 * The block entity of a formed structure's controller: it remembers which structure it is, where its pattern lies and
 * what block stood in its own cell, so the structure can be taken apart again. Save {@link Formed#CODEC} with the rest
 * of the entity's data.
 */
public interface MultiblockCore {
    @Nullable
    Formed formed();

    void setFormed(Formed formed);

    /** A formed structure: its id in {@link Multiblocks}, where it lies and the block its controller replaced. */
    record Formed(String multiblock, MultiblockPattern.Placement placement, BlockState original) {
        public static final Codec<Formed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("multiblock").forGetter(Formed::multiblock),
                MultiblockPattern.Placement.CODEC.fieldOf("placement").forGetter(Formed::placement),
                BlockState.CODEC.fieldOf("original").forGetter(Formed::original)
        ).apply(instance, Formed::new));
    }
}
