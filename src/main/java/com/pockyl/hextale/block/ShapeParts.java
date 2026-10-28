package com.pockyl.hextale.block;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.world.WorldEdits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Devices whose model spreads past their own cell (the Hexcore's tiers, the altar's dais and prongs, the furnace's
 * chimney): the game only looks at the block in a cell when it aims, collides or clicks, so the spreading parts could be
 * neither outlined nor clicked. Every device in the block tag {@code hextale:shape_part_owners} puts a
 * {@link ShapePartBlock} into each empty cell its shape reaches (one to every side and the layer above, also diagonally).
 * Parts are placed when a player places the device (asking the claims as that player), when the Hexcore awakens and,
 * for devices placed before, when their chunk loads; they go by themselves when the device goes.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ShapeParts {
    public static final TagKey<Block> OWNERS = TagKey.create(Registries.BLOCK, Hextale.id("shape_part_owners"));
    /** Devices of freshly loaded chunks, checked on the next tick (a chunk must not be changed while it loads). */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> PENDING = new HashMap<>();

    private ShapeParts() {
    }

    /** Puts the missing parts round the device at {@code pos} into the empty cells its shape reaches. */
    public static void refresh(ServerLevel level, BlockPos pos, @Nullable Player actor) {
        BlockState device = level.getBlockState(pos);
        if (!device.is(OWNERS)) {
            return;
        }
        Player who = actor != null ? actor : WorldEdits.owner(level, level.getBlockEntity(pos));
        VoxelShape shape = device.getShape(level, pos);
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if ((dx != 0 || dy != 0 || dz != 0) && reaches(shape, dx, dy, dz)) {
                        place(level, pos.offset(dx, dy, dz), ShapePartBlock.at(dx, dy, dz), who);
                    }
                }
            }
        }
    }

    private static void place(ServerLevel level, BlockPos cell, BlockState part, Player who) {
        if (!level.isLoaded(cell)) {
            return;
        }
        BlockState there = level.getBlockState(cell);
        // Only empty cells: never another device's part, never a block or a fluid someone put there.
        if (there.getBlock() instanceof ShapePartBlock || !there.canBeReplaced() || !there.getFluidState().isEmpty()) {
            return;
        }
        if (WorldEdits.mayPlace(level, cell, part, who)) {
            level.setBlock(cell, part, Block.UPDATE_ALL);
        }
    }

    /** Whether the shape (in the device's block space) reaches into the cell {@code dx, dy, dz} away. */
    static boolean reaches(VoxelShape shape, int dx, int dy, int dz) {
        return Shapes.joinIsNotEmpty(shape.move(-dx, -dy, -dz), Shapes.block(), BooleanOp.AND);
    }

    /** After every other listener: the placer is known (and remembered) and the claims had their say. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level && event.getPlacedBlock().is(OWNERS)) {
            refresh(level, event.getPos(), event.getEntity() instanceof Player player ? player : null);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            for (BlockEntity entity : chunk.getBlockEntities().values()) {
                if (entity.getBlockState().is(OWNERS)) {
                    PENDING.computeIfAbsent(level.dimension(), key -> new LinkedHashSet<>()).add(entity.getBlockPos());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            Set<BlockPos> pending = PENDING.remove(level.dimension());
            if (pending != null) {
                for (BlockPos pos : pending) {
                    if (level.isLoaded(pos)) {
                        refresh(level, pos, null);
                    }
                }
            }
        }
    }
}
