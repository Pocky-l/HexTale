package com.pockyl.hextale.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModTags;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * What a Whirl Reaper takes and how - pure world rules, no timing. A crop is ripe at its last age and is reaped where it
 * stands: its drops fall on its own block, one seed goes back into the ground and the plant starts again from its first
 * age (a berry bush keeps its leaves, as when it is picked by hand). A melon or pumpkin is taken only while a stem holds
 * it. A stalk (sugar cane, cactus, bamboo) is taken once it is {@value #STALK_HEIGHT} blocks tall: everything above its
 * lowest block, dropped beside it (a cactus would burn its own pieces).
 */
public final class Reaping {
    /** A stalk this tall (or taller) is ripe. */
    public static final int STALK_HEIGHT = 3;
    /** The tallest stalk taken at once. */
    private static final int MAX_STALK = 24;

    private Reaping() {
    }

    /** What kind of harvest the plant at {@code pos} is ripe for, or null when it is not ripe (or not a plant at all). */
    public static @Nullable WhirlKind ripe(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return null;
        }
        if (isCrop(state)) {
            IntegerProperty age = age(state);
            return age != null && state.getValue(age) >= max(age) ? WhirlKind.CROP : null;
        }
        if (state.is(ModTags.WHIRL_GOURDS)) {
            return onStem(level, pos, state) ? WhirlKind.GOURD : null;
        }
        if (state.is(ModTags.WHIRL_STALKS) && !level.getBlockState(pos.below()).is(state.getBlock())) {
            return height(level, pos, state) >= STALK_HEIGHT ? WhirlKind.STALK : null;
        }
        return null;
    }

    /** Blocks of the same plant from {@code base} up (1 = the base alone). */
    public static int height(BlockGetter level, BlockPos base, BlockState state) {
        int height = 1;
        while (height < MAX_STALK && level.getBlockState(base.above(height)).is(state.getBlock())) {
            height++;
        }
        return height;
    }

    private static boolean isCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock || state.is(ModTags.WHIRL_CROPS);
    }

    private static boolean onStem(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockState stem = level.getBlockState(pos.relative(side));
            if (stem.getBlock() instanceof AttachedStemBlock && stem.getValue(AttachedStemBlock.FACING) == side.getOpposite()) {
                return true;
            }
        }
        return false;
    }

    /** The plant's age property (every crop of the game and of most mods calls it "age"), or null. */
    private static @Nullable IntegerProperty age(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && "age".equals(property.getName())) {
                return integer;
            }
        }
        return null;
    }

    private static int max(IntegerProperty property) {
        return property.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    private static int min(IntegerProperty property) {
        return property.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0);
    }

    // ---- Harvests (server) ------------------------------------------------------------------------------------------------

    /**
     * Reaps a ripe crop in place: drops on its block, the plant back to its first age (a bush to its leaves). False when it
     * is no longer ripe.
     */
    public static boolean reapCrop(ServerLevel level, BlockPos pos) {
        return reapCrop(level, pos, drops -> { });
    }

    /** {@link #reapCrop(ServerLevel, BlockPos)}, letting {@code more} add to the drops before they fall (the reaper's runes). */
    public static boolean reapCrop(ServerLevel level, BlockPos pos, java.util.function.Consumer<List<ItemStack>> more) {
        BlockState state = level.getBlockState(pos);
        IntegerProperty age = age(state);
        if (ripe(level, pos, state) != WhirlKind.CROP || age == null) {
            return false;
        }
        boolean bush = state.getBlock() instanceof SweetBerryBushBlock;
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos)));
        if (!bush) {
            // The seed that stays in the ground.
            takeOne(drops, state.getBlock().asItem());
        }
        BlockState young = state.setValue(age, bush ? Math.min(1, max(age)) : min(age));
        level.setBlock(pos, young, Block.UPDATE_ALL);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(young));
        more.accept(drops);
        for (ItemStack drop : drops) {
            Block.popResource(level, pos, drop);
        }
        return true;
    }

    private static void takeOne(List<ItemStack> drops, Item seed) {
        for (ItemStack drop : drops) {
            if (drop.is(seed)) {
                drop.shrink(1);
                return;
            }
        }
    }

    /**
     * Takes the ripe fruit or stalk at {@code pos} up out of the world, top first so nothing above it breaks on its own:
     * returns the blocks taken (a fruit alone, or a stalk's pieces with a cactus flower on top), empty when it is no longer
     * ripe.
     */
    public static List<BlockState> lift(ServerLevel level, BlockPos pos, WhirlKind kind) {
        BlockState state = level.getBlockState(pos);
        if (ripe(level, pos, state) != kind) {
            return List.of();
        }
        List<BlockState> taken = new ArrayList<>();
        if (kind == WhirlKind.GOURD) {
            taken.add(state);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(state));
            return taken;
        }
        int height = height(level, pos, state);
        for (int dy = height - 1; dy >= 1; dy--) {
            BlockPos at = pos.above(dy);
            taken.add(level.getBlockState(at));
            level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos.above(), GameEvent.Context.of(state));
        return taken;
    }

    /**
     * Lets the lifted blocks fall as their drops: a fruit's where it lay, a stalk's beside its base on the side the whirl
     * came from ({@code from} = the reaper).
     */
    public static void drop(ServerLevel level, BlockPos pos, WhirlKind kind, List<BlockState> taken, BlockPos from) {
        drop(level, pos, kind, taken, from, drops -> { });
    }

    /** {@link #drop(ServerLevel, BlockPos, WhirlKind, List, BlockPos)}, letting {@code more} add to the drops first. */
    public static void drop(ServerLevel level, BlockPos pos, WhirlKind kind, List<BlockState> taken, BlockPos from,
                            java.util.function.Consumer<List<ItemStack>> more) {
        Vec3 at = kind == WhirlKind.STALK ? beside(pos, from) : Vec3.atCenterOf(pos);
        List<ItemStack> drops = new ArrayList<>();
        for (BlockState state : taken) {
            drops.addAll(Block.getDrops(state, level, pos, null));
        }
        more.accept(drops);
        for (ItemStack drop : drops) {
            ItemEntity item = new ItemEntity(level, at.x + (level.getRandom().nextDouble() - 0.5) * 0.3, at.y,
                    at.z + (level.getRandom().nextDouble() - 0.5) * 0.3, drop);
            item.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.06, 0.12, (level.getRandom().nextDouble() - 0.5) * 0.06);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
    }

    /** Where a stalk's pieces fall: the middle of the block next to its base, towards {@code from}. */
    public static Vec3 beside(BlockPos base, BlockPos from) {
        int dx = from.getX() - base.getX();
        int dz = from.getZ() - base.getZ();
        Direction side = Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST)
                : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
        if (dx == 0 && dz == 0) {
            side = Direction.NORTH;
        }
        return Vec3.atBottomCenterOf(base.relative(side)).add(0, 0.3, 0);
    }
}
