package com.pockyl.hextale.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * What a Hex Sawmill fells and how - pure world rules, no timing. A tree is a trunk of logs standing on dirt with leaves
 * grown on it (leaves a player placed do not count, so log walls and pillars are never cut): every log joined to the
 * trunk from its foot up and every grown leaf joined to those logs. Roots below the foot stay in the ground. Felled, the
 * whole tree falls as its drops on the foot, and saplings from its own leaves are planted back where it stood (four for
 * a tree that stood on four).
 */
public final class Felling {
    /** The most logs and leaves taken from one tree. */
    public static final int MAX_LOGS = 320;
    public static final int MAX_LEAVES = 1200;
    /** How far from the foot a tree may spread sideways. */
    private static final int SPREAD = 9;
    /** Leaves this far from a log (the game's own leaf distance) still belong to the tree. */
    private static final int LEAF_REACH = 6;
    /** Grown leaves needed to call a trunk a tree. */
    private static final int MIN_LEAVES = 4;

    private Felling() {
    }

    /** A tree found at a foot: the logs standing on dirt (one, or four for a big tree), its logs and its leaves. */
    public record Tree(BlockPos foot, List<BlockPos> feet, List<BlockPos> logs, List<BlockPos> leaves) {
        public int size() {
            return logs.size() + leaves.size();
        }

        public int top() {
            int top = foot.getY();
            for (BlockPos pos : logs) {
                top = Math.max(top, pos.getY());
            }
            for (BlockPos pos : leaves) {
                top = Math.max(top, pos.getY());
            }
            return top;
        }
    }

    /** Whether the block may be a tree's foot: a log on dirt, open to the air beside it (buried roots are not). */
    public static boolean isFoot(BlockGetter level, BlockPos pos, BlockState state) {
        if (!state.is(BlockTags.LOGS) || !level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
            return false;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(side)).isSolid()) {
                return true;
            }
        }
        return !level.getBlockState(pos.above()).isSolid();
    }

    /** The tree standing on this foot, or null when it is no tree (no grown leaves on it). */
    public static @Nullable Tree find(BlockGetter level, BlockPos foot) {
        if (!isFoot(level, foot, level.getBlockState(foot))) {
            return null;
        }
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(foot);
        seen.add(foot);
        while (!queue.isEmpty() && logs.size() < MAX_LOGS) {
            BlockPos pos = queue.poll();
            logs.add(pos);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (next.getY() < foot.getY() || Math.abs(next.getX() - foot.getX()) > SPREAD
                                || Math.abs(next.getZ() - foot.getZ()) > SPREAD || !seen.add(next)) {
                            continue;
                        }
                        if (level.getBlockState(next).is(BlockTags.LOGS)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        // Grown leaves, walked out from the logs no farther than leaves live from their wood.
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> leafSeen = new HashSet<>(logs);
        ArrayDeque<BlockPos> front = new ArrayDeque<>(logs);
        for (int step = 1; step <= LEAF_REACH && !front.isEmpty(); step++) {
            ArrayDeque<BlockPos> next = new ArrayDeque<>();
            for (BlockPos pos : front) {
                for (Direction side : Direction.values()) {
                    BlockPos leaf = pos.relative(side);
                    if (leaves.size() >= MAX_LEAVES || !leafSeen.add(leaf)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(leaf);
                    if (state.is(BlockTags.LEAVES) && !(state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT))) {
                        leaves.add(leaf);
                        next.add(leaf);
                    }
                }
            }
            front = next;
        }
        if (leaves.size() < MIN_LEAVES) {
            return null;
        }
        List<BlockPos> feet = new ArrayList<>();
        for (BlockPos log : logs) {
            if (log.getY() == foot.getY() && level.getBlockState(log.below()).is(BlockTags.DIRT)
                    && Math.abs(log.getX() - foot.getX()) <= 1 && Math.abs(log.getZ() - foot.getZ()) <= 1) {
                feet.add(log);
            }
        }
        return new Tree(foot, feet, logs, leaves);
    }

    // ---- Felling (server) ---------------------------------------------------------------------------------------------

    /**
     * Takes the tree out of the world, leaves first and then the logs from the top down, and returns what it drops, as
     * an axe would cut it. Blocks that changed since it was found (a player broke them) are skipped.
     */
    public static List<ItemStack> fell(ServerLevel level, Tree tree) {
        List<ItemStack> drops = new ArrayList<>();
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        List<BlockPos> order = new ArrayList<>(tree.leaves());
        List<BlockPos> logs = new ArrayList<>(tree.logs());
        logs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        order.addAll(logs);
        for (BlockPos pos : order) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.LOGS) && !state.is(BlockTags.LEAVES)) {
                continue;
            }
            for (ItemStack drop : Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, axe)) {
                add(drops, drop);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.gameEvent(GameEvent.BLOCK_DESTROY, tree.foot(), GameEvent.Context.of(Blocks.AIR.defaultBlockState()));
        return drops;
    }

    private static void add(List<ItemStack> drops, ItemStack drop) {
        for (ItemStack stack : drops) {
            if (ItemStack.isSameItemSameComponents(stack, drop) && stack.getCount() < stack.getMaxStackSize()) {
                int moved = Math.min(drop.getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(moved);
                drop.shrink(moved);
                if (drop.isEmpty()) {
                    return;
                }
            }
        }
        drops.add(drop.copy());
    }

    /**
     * Plants saplings from the drops where the tree stood: on every foot when there are enough of one kind for all of
     * them (a big tree needs four), else one on the main foot. Planted saplings are taken out of {@code drops}.
     *
     * @return how many were planted
     */
    public static int replant(ServerLevel level, BlockPos foot, List<BlockPos> feet, List<ItemStack> drops) {
        ItemStack best = ItemStack.EMPTY;
        for (ItemStack stack : drops) {
            if (stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(BlockTags.SAPLINGS)
                    && stack.getCount() > best.getCount()) {
                best = stack;
            }
        }
        if (best.isEmpty()) {
            return 0;
        }
        BlockState sapling = ((BlockItem) best.getItem()).getBlock().defaultBlockState();
        List<BlockPos> spots = best.getCount() >= feet.size() && feet.size() > 1 ? feet : List.of(foot);
        int planted = 0;
        for (BlockPos spot : spots) {
            if (best.isEmpty() || !level.getBlockState(spot).canBeReplaced() || !sapling.canSurvive(level, spot)) {
                continue;
            }
            level.setBlock(spot, sapling, Block.UPDATE_ALL);
            level.gameEvent(GameEvent.BLOCK_PLACE, spot, GameEvent.Context.of(sapling));
            best.shrink(1);
            planted++;
        }
        drops.removeIf(ItemStack::isEmpty);
        return planted;
    }

    /** Lets the felled tree's drops fall on its foot, a few stacks at a time thrown a little apart. */
    public static void drop(ServerLevel level, BlockPos foot, List<ItemStack> drops) {
        Vec3 at = Vec3.atBottomCenterOf(foot).add(0, 0.4, 0);
        for (ItemStack stack : drops) {
            ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack.copy());
            item.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.15, 0.2, (level.getRandom().nextDouble() - 0.5) * 0.15);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
    }
}
