package com.pockyl.hextale.essence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.block.BrassPipeBlock;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Moves essence through brass pipes. A machine pushes out of one of its faces; the liquid runs through every pipe
 * connected there (up to {@value #MAX_PIPES} of them) to the first vessel that takes it from the side it touches.
 * <p>
 * A vessel never takes from a pipe network its own outlet runs into: otherwise what it lets out (the separator's rest,
 * a tank's contents) would flow straight back in and go round in circles.
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

        /** Whether the receiver lets essence out through this face of its own. */
        default boolean isOutlet(Direction face) {
            return false;
        }
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
        Set<BlockPos> pipes = network(level, start);
        for (BlockPos pipe : pipes) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pipe.relative(direction);
                if (next.equals(from) || pipes.contains(next) || !(level.getBlockEntity(next) instanceof Receiver receiver)
                        || feedsBack(next, receiver, pipes)) {
                    continue;
                }
                int taken = receiver.receive(direction.getOpposite(), offer, false);
                if (taken > 0) {
                    return taken;
                }
            }
        }
        return 0;
    }

    /** The pipes connected to {@code start}, nearest first. */
    private static Set<BlockPos> network(Level level, BlockPos start) {
        Set<BlockPos> pipes = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        pipes.add(start);
        queue.add(start);
        while (!queue.isEmpty() && pipes.size() < MAX_PIPES) {
            BlockPos pipe = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pipe.relative(direction);
                if (!pipes.contains(next) && level.getBlockState(next).getBlock() instanceof BrassPipeBlock) {
                    pipes.add(next);
                    queue.add(next);
                }
            }
        }
        return pipes;
    }

    /** Whether one of the receiver's outlets runs into these pipes. */
    private static boolean feedsBack(BlockPos at, Receiver receiver, Set<BlockPos> pipes) {
        for (Direction face : Direction.values()) {
            if (receiver.isOutlet(face) && pipes.contains(at.relative(face))) {
                return true;
            }
        }
        return false;
    }
}
