package com.pockyl.hextale.essence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.pockyl.hextale.block.BrassPipeBlock;
import com.pockyl.hextale.block.EssenceTankBlockEntity;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Moves liquids through brass pipes: essence and water share the pipes. A block pushes out of one of its faces; the
 * liquid runs through every pipe connected there (up to {@value #MAX_PIPES} of them) to the first vessel that takes it
 * from the side it touches.
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
        int receive(Direction side, EssenceMix offer, boolean simulate);

        /** Whether the receiver lets liquid out through this face of its own. */
        default boolean isOutlet(Direction face) {
            return false;
        }
    }

    /** Something that takes water in through some of its faces (amounts in millibuckets). */
    public interface WaterReceiver {
        /**
         * @param side the receiver's own face the water comes in through
         * @return millibuckets taken
         */
        int receiveWater(Direction side, int amount, boolean simulate);
    }

    /** What a vessel takes of the liquid through one of its faces. */
    private interface Taker {
        int take(BlockEntity vessel, Direction side);
    }

    /**
     * Pushes {@code offer} out of the machine at {@code from} through its face {@code face}.
     *
     * @return the drops delivered (taken out of the offer in its proportions)
     */
    public static int push(Level level, BlockPos from, Direction face, EssenceMix offer) {
        if (offer.isEmpty()) {
            return 0;
        }
        return deliver(level, from, face, (vessel, side) -> vessel instanceof Receiver receiver ? receiver.receive(side, offer, false) : 0);
    }

    /**
     * Pushes {@code amount} millibuckets of water out of the block at {@code from} through its face {@code face}.
     *
     * @return the millibuckets delivered
     */
    public static int pushWater(Level level, BlockPos from, Direction face, int amount) {
        if (amount <= 0) {
            return 0;
        }
        return deliver(level, from, face,
                (vessel, side) -> vessel instanceof WaterReceiver receiver ? receiver.receiveWater(side, amount, false) : 0);
    }

    private static int deliver(Level level, BlockPos from, Direction face, Taker taker) {
        BlockPos start = from.relative(face);
        BlockEntity next = level.getBlockEntity(start);
        if (next != null) {
            return taker.take(next, face.getOpposite());
        }
        if (!(level.getBlockState(start).getBlock() instanceof BrassPipeBlock)) {
            return 0;
        }
        Set<BlockPos> pipes = network(level, start);
        for (BlockPos pipe : pipes) {
            for (Direction direction : Direction.values()) {
                BlockPos at = pipe.relative(direction);
                if (at.equals(from) || pipes.contains(at)) {
                    continue;
                }
                BlockEntity vessel = level.getBlockEntity(at);
                if (vessel == null || vessel instanceof Receiver receiver && feedsBack(at, receiver, pipes)) {
                    continue;
                }
                int taken = taker.take(vessel, direction.getOpposite());
                if (taken > 0) {
                    return taken;
                }
            }
        }
        return 0;
    }

    /**
     * Draws up to {@code amount} drops for the vessel at {@code to} through its face {@code face}: out of an Essence Tank
     * right there, or out of the first non-empty tank on the pipes joined there - so a machine's inlet takes from the
     * tanks it is piped to, whichever face of the tank the pipe meets. Never out of a network the vessel's own outlet
     * runs into.
     *
     * @return the drops taken out of the tank
     */
    public static EssenceMix draw(Level level, BlockPos to, Direction face, int amount, Receiver vessel) {
        if (amount <= 0) {
            return EssenceMix.EMPTY;
        }
        BlockPos start = to.relative(face);
        if (level.getBlockEntity(start) instanceof EssenceTankBlockEntity tank) {
            return take(tank, amount);
        }
        if (!(level.getBlockState(start).getBlock() instanceof BrassPipeBlock)) {
            return EssenceMix.EMPTY;
        }
        Set<BlockPos> pipes = network(level, start);
        if (feedsBack(to, vessel, pipes)) {
            return EssenceMix.EMPTY;
        }
        for (BlockPos pipe : pipes) {
            for (Direction direction : Direction.values()) {
                BlockPos at = pipe.relative(direction);
                if (!at.equals(to) && !pipes.contains(at) && level.getBlockEntity(at) instanceof EssenceTankBlockEntity tank
                        && !tank.tank().isEmpty()) {
                    return take(tank, amount);
                }
            }
        }
        return EssenceMix.EMPTY;
    }

    private static EssenceMix take(EssenceTankBlockEntity tank, int amount) {
        EssenceMix drawn = tank.tank().drain(amount, false);
        if (!drawn.isEmpty()) {
            tank.setChanged();
        }
        return drawn;
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
