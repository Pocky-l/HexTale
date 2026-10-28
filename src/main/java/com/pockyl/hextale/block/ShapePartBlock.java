package com.pockyl.hextale.block;

import com.pockyl.hextale.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * An invisible stand-in in a cell a device's model spreads into ({@link ShapeParts}). It knows its device by its offset
 * from it (dx and dz 0..2 for -1..1, dy 0..1 for the cell beside or above), outlines the whole device, passes clicks
 * and mining on to it and is replaced like grass when a block is put into its cell.
 */
public class ShapePartBlock extends Block {
    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 2);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 1);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 2);

    public ShapePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DX, 1).setValue(DY, 1).setValue(DZ, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DY, DZ);
    }

    /** The part that stands {@code dx, dy, dz} away from its device. */
    public static BlockState at(int dx, int dy, int dz) {
        return ModBlocks.SHAPE_PART.get().defaultBlockState().setValue(DX, dx + 1).setValue(DY, dy).setValue(DZ, dz + 1);
    }

    /** Where the device of the part at {@code pos} stands. */
    public static BlockPos device(BlockState state, BlockPos pos) {
        return pos.offset(1 - state.getValue(DX), -state.getValue(DY), 1 - state.getValue(DZ));
    }

    /** The device the part stands in for, or null when it is gone. */
    private static @Nullable BlockState deviceState(BlockGetter level, BlockState state, BlockPos pos) {
        BlockState device = level.getBlockState(device(state, pos));
        return device.is(ShapeParts.OWNERS) ? device : null;
    }

    private static VoxelShape fromDevice(VoxelShape shape, BlockState state) {
        return shape.move(1 - state.getValue(DX), -state.getValue(DY), 1 - state.getValue(DZ));
    }

    /** The device's whole outline, so aiming at any of its parts outlines all of it. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState device = deviceState(level, state, pos);
        return device == null ? Shapes.empty() : fromDevice(device.getShape(level, device(state, pos), context), state);
    }

    /** What is stood on here: the device's collision inside this cell. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState device = deviceState(level, state, pos);
        if (device == null) {
            return Shapes.empty();
        }
        VoxelShape shape = fromDevice(device.getCollisionShape(level, device(state, pos), context), state);
        return Shapes.join(shape, Shapes.block(), BooleanOp.AND);
    }

    /**
     * A part whose device went away goes too, a tick later and quietly (turning into air here would break it with
     * particles and a sound, once for every part); parts tell each other, so it runs through all of them.
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (deviceState(level, state, pos) == null) {
            level.scheduleTick(pos, this, 1);
        }
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (deviceState(level, state, pos) == null) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter portLevel, BlockPos portPos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        BlockState device = deviceState(level, state, pos);
        if (device == null) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return com.pockyl.hextale.port.Interactions.itemOn(device.useItemOn(stack, level, player, hand, hitResult.withPosition(device(state, pos))));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockState device = deviceState(level, state, pos);
        if (device == null) {
            return InteractionResult.PASS;
        }
        return device.useWithoutItem(level, player, hitResult.withPosition(device(state, pos)));
    }

    /** Mined as fast as the device itself. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState device = deviceState(level, state, pos);
        return device == null ? 1.0F : device.getDestroyProgress(player, level, device(state, pos));
    }

    /**
     * Mining a part mines the device (its drops, the tool's wear, the claim checks), which then takes its parts away.
     * The server shows the device's break to everyone but the miner, whose own game shows it for the part it mined:
     * the device's sound and particles ({@code ShapeParticles}), never a second time for anybody else.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide()) {
            spawnDestroyParticles(level, player, pos, state);
        } else if (player instanceof ServerPlayer server && deviceState(level, state, pos) != null) {
            server.gameMode.destroyBlock(device(state, pos));
        }
        return state;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockState device = deviceState(level, state, pos);
        return device == null ? ItemStack.EMPTY : new ItemStack(device.getBlock());
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader level, BlockPos pos, @Nullable Entity entity) {
        BlockState device = deviceState(level, state, pos);
        return device == null ? super.getSoundType(state, level, pos, entity) : device.getSoundType(level, device(state, pos), entity);
    }
}
