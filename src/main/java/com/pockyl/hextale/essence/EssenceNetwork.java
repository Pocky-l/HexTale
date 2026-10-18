package com.pockyl.hextale.essence;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.block.BrassPipeBlock;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.PipeSide;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Moves liquids through brass pipes: essence and water share the pipes. A block pushes out of one of its faces; the
 * liquid runs through every pipe connected there (up to {@value #MAX_PIPES} of them) to the first vessel that takes it
 * from the side it touches.
 * <p>
 * A vessel never takes from a pipe network another of its own outlets runs into: otherwise what it lets out (the
 * separator's rest, a tank's contents) would flow straight back in and go round in circles. Each pipe side may be set
 * by hand ({@link PipeSide}): shut, in only (into the block there) or out only (out of it).
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
        return deliver(level, from, face, null, (vessel, side) -> vessel instanceof Receiver receiver ? receiver.receive(side, offer, false) : 0);
    }

    /**
     * Pushes {@code offer}, drawn out of {@code jar} by a pipe that pumps it, from the jar's block at {@code from}
     * through its face {@code face}; the jar itself (any block of a big one) takes none of it back.
     *
     * @return the drops delivered
     */
    public static int pushOutOf(Level level, BlockPos from, Direction face, EssenceMix offer, EssenceTankBlockEntity jar) {
        if (offer.isEmpty()) {
            return 0;
        }
        return deliver(level, from, face, jar, (vessel, side) -> vessel instanceof Receiver receiver ? receiver.receive(side, offer, false) : 0);
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
        return deliver(level, from, face, null,
                (vessel, side) -> vessel instanceof WaterReceiver receiver ? receiver.receiveWater(side, amount, false) : 0);
    }

    private static int deliver(Level level, BlockPos from, Direction face, @Nullable EssenceTankBlockEntity source, Taker taker) {
        BlockPos start = from.relative(face);
        BlockState first = level.getBlockState(start);
        if (!(first.getBlock() instanceof BrassPipeBlock)) {
            BlockEntity next = level.getBlockEntity(start);
            return next != null ? taker.take(next, face.getOpposite()) : 0;
        }
        if (!BrassPipeBlock.side(first, face.getOpposite()).outOfBlock()) {
            // That pipe side lets nothing out of this block (in only, or shut).
            return 0;
        }
        Set<BlockPos> pipes = network(level, start);
        for (BlockPos pipe : pipes) {
            BlockState state = level.getBlockState(pipe);
            for (Direction direction : Direction.values()) {
                BlockPos at = pipe.relative(direction);
                if (!BrassPipeBlock.side(state, direction).intoBlock() || at.equals(from) || pipes.contains(at)) {
                    continue;
                }
                BlockEntity vessel = level.getBlockEntity(at);
                if (vessel == null || source != null && EssenceTankBlockEntity.at(level, at) == source
                        || vessel instanceof Receiver receiver && feedsBack(at, receiver, pipes, direction.getOpposite())) {
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
     * right there, or out of the first non-empty tank on the pipes joined there (a block of a Big Essence Jar counts as
     * the jar) - so a machine's inlet takes from the tanks it is piped to, whichever face of the tank the pipe meets.
     * A pipe side set to "in only" towards a tank keeps it out; never out of a network another outlet of the vessel
     * runs into.
     *
     * @return the drops taken out of the tank
     */
    public static EssenceMix draw(Level level, BlockPos to, Direction face, int amount, Receiver vessel) {
        if (amount <= 0) {
            return EssenceMix.EMPTY;
        }
        BlockPos start = to.relative(face);
        BlockState first = level.getBlockState(start);
        if (!(first.getBlock() instanceof BrassPipeBlock)) {
            EssenceTankBlockEntity here = EssenceTankBlockEntity.at(level, start);
            return here != null ? take(here, amount) : EssenceMix.EMPTY;
        }
        if (!BrassPipeBlock.side(first, face.getOpposite()).intoBlock()) {
            return EssenceMix.EMPTY;
        }
        Set<BlockPos> pipes = network(level, start);
        if (feedsBack(to, vessel, pipes, face)) {
            return EssenceMix.EMPTY;
        }
        for (BlockPos pipe : pipes) {
            BlockState state = level.getBlockState(pipe);
            for (Direction direction : Direction.values()) {
                BlockPos at = pipe.relative(direction);
                if (!BrassPipeBlock.side(state, direction).outOfBlock() || at.equals(to) || pipes.contains(at)) {
                    continue;
                }
                EssenceTankBlockEntity tank = EssenceTankBlockEntity.at(level, at);
                if (tank != null && !tank.tank().isEmpty()) {
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

    /**
     * The pipes joined to {@code start}, nearest first; two pipes are joined unless their joint was shut. A start that
     * is no pipe (a drain valve) reaches the pipes joined to it.
     */
    public static Set<BlockPos> network(Level level, BlockPos start) {
        Set<BlockPos> pipes = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        pipes.add(start);
        queue.add(start);
        while (!queue.isEmpty() && pipes.size() < MAX_PIPES) {
            BlockPos pipe = queue.poll();
            BlockState state = level.getBlockState(pipe);
            boolean isPipe = state.getBlock() instanceof BrassPipeBlock;
            for (Direction direction : Direction.values()) {
                BlockPos next = pipe.relative(direction);
                if (pipes.contains(next)) {
                    continue;
                }
                PipeSide back = BrassPipeBlock.side(level.getBlockState(next), direction.getOpposite());
                boolean joined = isPipe ? BrassPipeBlock.side(state, direction) == PipeSide.PIPE && back == PipeSide.PIPE : back.open();
                if (joined) {
                    pipes.add(next);
                    queue.add(next);
                }
            }
        }
        return pipes;
    }

    /**
     * Whether one of the receiver's outlets other than {@code side} runs into these pipes. Through the outlet itself it
     * may take (a jar fed from below through the pipe it pours into).
     */
    private static boolean feedsBack(BlockPos at, Receiver receiver, Set<BlockPos> pipes, Direction side) {
        for (Direction face : Direction.values()) {
            if (face != side && receiver.isOutlet(face) && pipes.contains(at.relative(face))) {
                return true;
            }
        }
        return false;
    }
}
