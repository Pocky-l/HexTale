package com.pockyl.hextale.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.block.HexVaultBlock;
import com.pockyl.hextale.block.HexVaultBlockEntity;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Hex Vault's gate: a stone platform with two white gold pillars and the vault standing between them. Woken, the vault
 * rises into the air above the platform and unfolds into a floating hexagon, a door of six petals in a frame of six bars
 * (drawn by {@code HexVaultRenderer}). The platform and pillars stay the blocks the player laid ({@link
 * MultiblockPattern.Key#walls()}): nothing tells the controller when one goes, so a formed vault looks after them itself
 * ({@link #stands}). The air the hexagon hangs in turns into invisible parts, thin slabs that take clicks and hold players
 * back. In the pattern the front faces south; layers go from the bottom up, rows from north to south. Storage Cubes are not
 * part of it: they count on the platform or against it ({@link #shell}).
 */
public final class VaultGate extends Multiblock {
    public static final Direction FRONT = Direction.SOUTH;
    /** The row of the pattern the hexagon stands in, the one between the pillars. */
    private static final int DOOR_ROW = 2;
    /** Half the thickness of the door's slabs, in pixels. */
    private static final int HALF_SLAB = 2;
    private static final VoxelShape ACROSS_X = Block.box(0, 0, 8 - HALF_SLAB, 16, 16, 8 + HALF_SLAB);
    private static final VoxelShape ACROSS_Z = Block.box(8 - HALF_SLAB, 0, 0, 8 + HALF_SLAB, 16, 16);

    VaultGate() {
        super("hex_vault", MultiblockPattern.builder()
                .layer(" SSSSS ", "SSSSSSS", "PIIIIIP", "SSSSSSS", " SSSSS ")
                .layer("       ", "       ", "P .V. P", "       ", "       ")
                .layer("       ", "       ", "P.....P", "       ", "       ")
                .layer("       ", "       ", " ..... ", "       ", "       ")
                .layer("       ", "       ", " ..... ", "       ", "       ")
                .layer("       ", "       ", "  ...  ", "       ", "       ")
                .where('S', new MultiblockPattern.Key(
                        state -> state.is(ModBlocks.MAGIC_STONE.get()) || state.is(ModBlocks.POLISHED_MAGIC_STONE.get())
                                || state.is(ModBlocks.WHORLED_MAGIC_STONE.get()),
                        () -> List.of(new ItemStack(ModItems.POLISHED_MAGIC_STONE.get()), new ItemStack(ModItems.MAGIC_STONE.get()),
                                new ItemStack(ModItems.WHORLED_MAGIC_STONE.get()))).walls())
                .where('P', MultiblockPattern.Key.block(ModBlocks.WHITE_GOLD_PILLAR).walls())
                .where('I', MultiblockPattern.Key.block(ModBlocks.WHITE_GOLD_INLAY).walls())
                .where('V', new MultiblockPattern.Key(state -> state.is(ModBlocks.HEX_VAULT.get()) && !state.getValue(HexVaultBlock.FORMED),
                        () -> List.of(new ItemStack(ModItems.HEX_VAULT.get()))))
                .controller('V')
                .build());
    }

    @Override
    public ResearchTopic topic() {
        return ResearchTopics.HEX_VAULT;
    }

    @Override
    public BlockState controller() {
        return ModBlocks.HEX_VAULT.get().defaultBlockState().setValue(HexVaultBlock.FORMED, true);
    }

    @Override
    public BlockState controller(MultiblockPattern.Placement placement) {
        return controller().setValue(HexVaultBlock.FACING, placement.rotation().rotate(FRONT));
    }

    /** A thin slab across the gate, whichever way it stands. */
    @Override
    public VoxelShape partShape(BlockPos offset, Rotation rotation) {
        return slabFacing(rotation.rotate(FRONT));
    }

    /** The door's slab for a gate with this front. */
    public static VoxelShape slabFacing(Direction front) {
        return front.getAxis() == Direction.Axis.Z ? ACROSS_X : ACROSS_Z;
    }

    /** Whether every block the player laid is still there. Nothing is read in a chunk that is not loaded. */
    public boolean stands(ServerLevel level, MultiblockPattern.Placement placement) {
        for (MultiblockPattern.Cell cell : pattern().cells()) {
            if (cell.key().kept()) {
                BlockPos pos = placement.at(cell.x(), cell.y(), cell.z());
                if (level.isLoaded(pos) && !cell.key().test().test(level.getBlockState(pos))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The cells on top of the platform and against its edge, the door's row left out: where Storage Cubes count. */
    public List<BlockPos> shell(MultiblockPattern.Placement placement) {
        Set<BlockPos> taken = new HashSet<>();
        for (MultiblockPattern.Cell cell : pattern().cells()) {
            taken.add(placement.at(cell.x(), cell.y(), cell.z()));
        }
        Set<BlockPos> cells = new LinkedHashSet<>();
        for (MultiblockPattern.Cell cell : pattern().cells()) {
            if (cell.y() != 0 || cell.z() == DOOR_ROW) {
                continue;
            }
            BlockPos floor = placement.at(cell.x(), cell.y(), cell.z());
            cells.add(floor.above());
            for (Direction side : Direction.Plane.HORIZONTAL) {
                cells.add(floor.relative(side));
            }
        }
        cells.removeAll(taken);
        return new ArrayList<>(cells);
    }

    @Override
    public void onFormed(ServerPlayer player, BlockPos controller) {
        if (player.level().getBlockEntity(controller) instanceof HexVaultBlockEntity vault) {
            vault.formedBy(player);
        }
    }

    @Override
    public SoundEvent breakSound() {
        return SoundEvents.DEEPSLATE_BRICKS_BREAK;
    }
}
