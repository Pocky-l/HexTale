package com.pockyl.hextale.port;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Registry object constructors whose Minecraft 26.x form does not exist on 1.21.1. */
public final class PortTypes {
    private PortTypes() {
    }

    /** {@code new BlockEntityType<>(factory, blocks...)} of 26.x. */
    public static <T extends BlockEntity> BlockEntityType<T> blockEntity(BlockEntityType.BlockEntitySupplier<? extends T> factory, Block... blocks) {
        return BlockEntityType.Builder.<T>of(factory, blocks).build(null);
    }
}
