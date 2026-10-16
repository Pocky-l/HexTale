package com.pockyl.hextale.essence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.block.BrassPipeBlock;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Moves essence through brass pipes. A machine pushes out of one of its faces; the liquid runs through every pipe
 * connected there (up to {@value #MAX_PIPES} of them) to the first vessel that takes it from the side it touches.
 */
public final class EssenceNetwork {
    public static final int MAX_PIPES = 256;

    private EssenceNetwork() {
    }

    /** Something that takes essence in through some of its faces. */
    public interface Receiver {
        /**
         * @param side the receiver's own face the liquid comes in through
         * @return drops taken
         */
        int receive(Direction side, Essence offer, boolean simulate);
    }

    /**
     * Pushes {@code offer} out of the machine at {@code from} through its face {@code face}.
     *
     * @return the drops delivered (taken out of the offer in its proportions)
     */
    public static int push(Level level, BlockPos from, Direction face, Essence offer) {
        if (offer.isEmpty()) {
            return 0;
        }
        BlockPos start = from.relative(face);
        if (level.getBlockEntity(start) instanceof Receiver receiver) {
            return receiver.receive(face.getOpposite(), offer, false);
        }
        if (!(level.getBlockState(start).getBlock() instanceof BrassPipeBlock)) {
            return 0;
        }
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos pipe = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pipe.relative(direction);
                if (next.equals(from) || !seen.add(next)) {
                    continue;
                }
                if (level.getBlockState(next).getBlock() instanceof BrassPipeBlock) {
                    if (seen.size() < MAX_PIPES) {
                        queue.add(next);
                    }
                } else if (level.getBlockEntity(next) instanceof Receiver receiver) {
                    int taken = receiver.receive(direction.getOpposite(), offer, false);
                    if (taken > 0) {
                        return taken;
                    }
                }
            }
        }
        return 0;
    }
}
