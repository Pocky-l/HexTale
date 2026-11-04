package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlockEntities;

import java.util.EnumMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Sap Tap: a rusted copper spout driven into the side of a log. {@link #FACING} points at the log. Sap drips from it
 * while the log is part of a living tree; a glass bottle takes what gathered in the spout.
 */
public class SapTapBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        // The spout in the half of the block next to the log (north = log to the north).
        SHAPES.put(Direction.NORTH, Block.box(5, 4, 0, 11, 12, 8));
        SHAPES.put(Direction.SOUTH, Block.box(5, 4, 8, 11, 12, 16));
        SHAPES.put(Direction.WEST, Block.box(0, 4, 5, 8, 12, 11));
        SHAPES.put(Direction.EAST, Block.box(8, 4, 5, 16, 12, 11));
    }

    public SapTapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Set against the side of a log: it faces the log. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        if (clicked.getAxis().isVertical()) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(FACING, clicked.getOpposite());
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(BlockTags.LOGS);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return direction == state.getValue(FACING) && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    /** A glass bottle takes the sap gathered in the spout. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!stack.is(Items.GLASS_BOTTLE) || !(level.getBlockEntity(pos) instanceof SapTapBlockEntity tap)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (tap.held() < Dose.SAP_MB) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(tap.status());
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            EssenceMix sap = tap.bottle();
            ItemStack flask = EssenceFlaskItem.filled(sap);
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, flask);
            } else if (!player.getInventory().add(flask)) {
                player.drop(flask, false);
            }
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_FILL, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof SapTapBlockEntity tap)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.sendOverlayMessage(tap.status());
        }
        return InteractionResult.SUCCESS;
    }

    /** A slow amber drip from the lip while the tree is alive. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0 && level.getBlockEntity(pos) instanceof SapTapBlockEntity tap && tap.dripping()) {
            Vec3 tip = tap.tip(state);
            level.addParticle(ParticleTypes.DRIPPING_HONEY, tip.x, tip.y, tip.z, 0, 0, 0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.pockyl.hextale.block.ModelShapes.covers(this) ? com.pockyl.hextale.block.ModelShapes.of(state) : SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SapTapBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SAP_TAP.get(), SapTapBlockEntity::serverTick);
    }

    @Override
    protected com.mojang.serialization.MapCodec<SapTapBlock> codec() {
        return simpleCodec(SapTapBlock::new);
    }
}
