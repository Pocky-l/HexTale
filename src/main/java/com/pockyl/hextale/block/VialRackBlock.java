package com.pockyl.hextale.block;

import com.pockyl.hextale.chem.ChemVialItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * The Vial Rack: eight vials of any brew on show, standing on a floor in two stepped rows or hung on a wall in two
 * shelves. Each place is clicked by itself, like a chiseled bookshelf's: a vial in hand goes into the empty place aimed
 * at, and a filled place gives its vial back.
 */
public class VialRackBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    /** Where a place's column ends, in pixels of the north-facing model, from +X (the player's left) inwards. */
    private static final float[] COLUMNS = {11.5F, 8, 4.5F};
    /** Above this height (pixels) the aim is on the back row of the standing rack, or the upper shelf on a wall. */
    private static final float STANDING_ROW = 7.5F;
    private static final float WALL_ROW = 8;
    /** A click on the standing rack's step from above: its back row starts here (pixels from the north side). */
    private static final float STEP_Z = 7.5F;

    public VialRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WALL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL);
    }

    /** On a wall it hangs facing out of the wall; anywhere else it stands facing the player. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isHorizontal()) {
            BlockState hung = defaultBlockState().setValue(FACING, face).setValue(WALL, true);
            if (hung.canSurvive(context.getLevel(), context.getClickedPos())) {
                return hung;
            }
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(WALL)) {
            return true;
        }
        Direction front = state.getValue(FACING);
        BlockPos wall = pos.relative(front.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, front);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.getValue(WALL) && direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    /** The place aimed at: 0-3 the front row or lower shelf from the player's left, 4-7 the back row or upper shelf. */
    public static int slotAt(BlockState state, BlockPos pos, Vec3 hit, Direction side) {
        double x = hit.x - pos.getX() - 0.5;
        double z = hit.z - pos.getZ() - 0.5;
        // Back into the north-facing model.
        double modelX;
        double modelZ;
        switch (state.getValue(FACING)) {
            case EAST -> {
                modelX = z;
                modelZ = -x;
            }
            case SOUTH -> {
                modelX = -x;
                modelZ = -z;
            }
            case WEST -> {
                modelX = -z;
                modelZ = x;
            }
            default -> {
                modelX = x;
                modelZ = z;
            }
        }
        float px = (float) (modelX + 0.5) * 16;
        float py = (float) (hit.y - pos.getY()) * 16;
        float pz = (float) (modelZ + 0.5) * 16;
        int column = COLUMNS.length;
        for (int i = 0; i < COLUMNS.length; i++) {
            if (px >= COLUMNS[i]) {
                column = i;
                break;
            }
        }
        boolean high = state.getValue(WALL) ? py >= WALL_ROW : py >= STANDING_ROW || side == Direction.UP && pz >= STEP_Z;
        return (high ? 4 : 0) + column;
    }

    /** A vial goes into the empty place aimed at; on a filled place the hand counts as empty and takes that vial. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (ChemVialItem.brew(stack) == null || !(level.getBlockEntity(pos) instanceof VialRackBlockEntity rack)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = slotAt(state, pos, hitResult.getLocation(), hitResult.getDirection());
        if (!rack.getItem(slot).isEmpty()) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            rack.setItem(slot, stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.3F, 1.5F);
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof VialRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        int slot = slotAt(state, pos, hitResult.getLocation(), hitResult.getDirection());
        if (rack.getItem(slot).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.getInventory().placeItemBackInInventory(rack.removeItem(slot, 1));
            level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.3F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Grows with the vials on it: 15 when all eight are there. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof VialRackBlockEntity rack ? (rack.filled() * 15 + 7) / VialRackBlockEntity.SLOTS : 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VialRackBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<VialRackBlock> codec() {
        return simpleCodec(VialRackBlock::new);
    }
}
