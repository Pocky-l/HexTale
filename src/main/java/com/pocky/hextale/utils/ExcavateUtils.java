package com.pocky.hextale.utils;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeHooks;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class ExcavateUtils {

    public static void excavateArea(Level world, BlockPos centerPos, ServerPlayer player, ItemStack stack, Item usedTool,
                                    int width, int height, int depth) {
        final Set<BlockPos> positions = findPositions(centerPos, player.getDirection(), width, height, depth);

        assert positions != null;
        for (BlockPos foundPos : positions) {

            if (centerPos.equals(foundPos)) {
                continue;
            }

            BlockState targetState = world.getBlockState(foundPos);
            if (targetState.isAir()) {
                continue;
            }

            float hardness = targetState.getDestroySpeed(world, foundPos);
            if (hardness == -1) {
                continue;
            }

            int exp = ForgeHooks.onBlockBreakEvent(world, player.gameMode.getGameModeForPlayer(), player, foundPos);
            if (exp == -1) {
                //If we can't actually break the block continue (this allows mods to stop us from vein mining into protected land)
                continue;
            }
            //Otherwise, break the block
            Block block = targetState.getBlock();
            //Get the tile now so that we have it for when we try to harvest the block
            BlockEntity tileEntity = world.getBlockEntity(foundPos);
            //Remove the block
            if (targetState.onDestroyedByPlayer(world, foundPos, player, true, targetState.getFluidState())) {
                block.destroy(world, foundPos, targetState);
                //Harvest the block allowing it to handle block drops, incrementing block mined count, and adding exhaustion
                block.playerDestroy(world, player, foundPos, targetState, tileEntity, stack);
                player.awardStat(Stats.ITEM_USED.get(usedTool));
                if (exp > 0) {
                    //If we have xp drop it
                    block.popExperience((ServerLevel) world, foundPos, exp);
                }
            }
        }
    }

    public static void veinMineArea(Level world, BlockPos pos, ServerPlayer player, ItemStack stack, Item usedTool, int maxVein) {

        Set<BlockPos> found = findVeinPositions(world, pos, world.getBlockState(pos).getBlock(), maxVein);

        for (BlockPos foundEntry : found) {
            if (pos.equals(foundEntry)) {
                continue;
            }
            BlockState targetState = world.getBlockState(foundEntry);
            if (targetState.isAir()) {
                continue;
            }
            float hardness = targetState.getDestroySpeed(world, foundEntry);
            if (hardness == -1) {
                continue;
            }

            int exp = ForgeHooks.onBlockBreakEvent(world, player.gameMode.getGameModeForPlayer(), player, foundEntry);
            if (exp == -1) {
                //If we can't actually break the block continue (this allows mods to stop us from vein mining into protected land)
                continue;
            }
            //Otherwise, break the block
            Block block = targetState.getBlock();
            //Get the tile now so that we have it for when we try to harvest the block
            BlockEntity tileEntity = world.getBlockEntity(foundEntry);
            //Remove the block
            if (targetState.onDestroyedByPlayer(world, foundEntry, player, true, targetState.getFluidState())) {
                block.destroy(world, foundEntry, targetState);
                //Harvest the block allowing it to handle block drops, incrementing block mined count, and adding exhaustion
                block.playerDestroy(world, player, foundEntry, targetState, tileEntity, stack);
                player.awardStat(Stats.ITEM_USED.get(usedTool));
                if (exp > 0) {
                    //If we have xp drop it
                    block.popExperience((ServerLevel) world, foundEntry, exp);
                }
            }
        }
    }

    public static Set<BlockPos> findPositions(BlockPos center, Direction direction, int width, int height, int depth) {
        if (width % 2 == 0 || height % 2 == 0 || depth % 2 == 0) return null;

        int w = (width - 1) / 2;
        int h = (height - 1) / 2;

        AABB aabb = switch (direction) {
            case NORTH -> new AABB(center.getX() - w, center.getY() - h, center.getZ(), center.getX() + w, center.getY() + h, center.getZ() + (depth - 1));
            case EAST -> new AABB(center.getX(), center.getY() - h, center.getZ() - w, center.getX() + (depth - 1), center.getY() + h, center.getZ() + w);
            case WEST -> new AABB(center.getX(), center.getY() - h, center.getZ() + w, center.getX() - (depth - 1), center.getY() + h, center.getZ() - w);
            case SOUTH -> new AABB(center.getX() + w, center.getY() - h, center.getZ(), center.getX() - w, center.getY() + h, center.getZ() + (depth - 1));
            default -> null;
        };
        assert aabb != null;
        return BlockPos.betweenClosedStream(aabb).map(BlockPos::new).collect(Collectors.toUnmodifiableSet());
    }

    public static Set<BlockPos> findVeinPositions(Level world, BlockPos center, Block block, int maxVein) {
        Set<BlockPos> positions = new HashSet<>(maxVein);

        Queue<BlockPos> forChecking = new PriorityQueue<>(Set.of(center));

        while (positions.size() <= maxVein && !forChecking.isEmpty()) {
            BlockPos checkPos = forChecking.poll();
            if (positions.contains(checkPos)) continue;
            Set<BlockPos> pos = findVein(checkPos, block, world);
            positions.add(checkPos);
            forChecking.addAll(pos);
        }
        positions.remove(center);
        return positions;
    }

    private static Set<BlockPos> findVein(BlockPos pos, Block blockFilter, Level world) {
        AABB cubeAABB = new AABB(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1);
        return BlockPos.betweenClosedStream(cubeAABB).map(BlockPos::new).filter(e -> world.getBlockState(e).is(blockFilter)).collect(Collectors.toSet());
    }
}
