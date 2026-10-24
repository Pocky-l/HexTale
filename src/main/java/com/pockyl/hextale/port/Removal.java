package com.pockyl.hextale.port;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Minecraft 26.x removes a block in two hooks: the block entity's {@code preRemoveSideEffects} (by default a container
 * drops its contents) and the block's {@code affectNeighborsAfterRemoval}. On 1.21.1 the mod's entity blocks call
 * {@link #before} and {@link #after} around {@code onRemove}, so the 26.x hooks keep their meaning.
 */
public final class Removal {
    private Removal() {
    }

    /** A block entity with its own removal side effects. */
    public interface SideEffects {
        void preRemoveSideEffects(BlockPos pos, BlockState state);
    }

    /** A block that updates its neighbours after it is gone. */
    public interface Neighbors {
        void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston);
    }

    /** Before the block entity is removed: its side effects, or the vanilla container drop. */
    public static void before(BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (state.is(newState.getBlock())) {
            return;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SideEffects effects) {
            effects.preRemoveSideEffects(pos, state);
        } else if (entity != null) {
            vanilla(entity, pos, state);
        }
    }

    /** After the block is gone. */
    public static void after(Object block, BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && block instanceof Neighbors neighbors && level instanceof ServerLevel server) {
            neighbors.affectNeighborsAfterRemoval(state, server, pos, movedByPiston);
        }
    }

    /** What the 26.x base block entity does on removal (the {@code super.preRemoveSideEffects} call). */
    public static void vanilla(BlockEntity entity, BlockPos pos, BlockState state) {
        if (entity instanceof Container container && entity.getLevel() != null) {
            Containers.dropContents(entity.getLevel(), pos, container);
        }
    }
}
