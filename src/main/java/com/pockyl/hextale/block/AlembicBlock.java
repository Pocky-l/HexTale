package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.transport.WaterInlet;
import com.pockyl.hextale.research.ResearchItems;

import org.jspecify.annotations.Nullable;

/**
 * The Alembic: a glass flask on a brass stand over a fire (fire, a lit campfire, lava or magma below it). Studied items
 * put into it boil down, in water, into essence - a mixture of all their elements - which it pours into glass bottles or
 * lets out through its spout at the front. Pipes join it at the front (essence out) and at the back and the sides (water
 * in); a water bucket fills it by hand.
 */
public class AlembicBlock extends BaseEntityBlock implements WorldlyContainerHolder, EssenceMachine {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Heat below: the alembic works. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 12, 13), Block.box(6, 12, 6, 10, 14, 10));

    public AlembicBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            state = state.setValue(PORTS.get(face), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            builder.add(PORTS.get(face));
        }
    }

    /** The spout at the front lets essence out; the other sides take water in. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face.getAxis().isHorizontal();
    }

    /** A water bucket fills the flask; so do the buckets and tanks of other mods, a bucket's worth at most. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!stack.is(Items.WATER_BUCKET)) {
            return pourContainer(alembic, level, pos, player, hand);
        }
        if (alembic.fillWater(1000, true) < 1000) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            alembic.fillWater(1000, false);
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult pourContainer(AlembicBlockEntity alembic, Level level, BlockPos pos, Player player, InteractionHand hand) {
        int room = Math.min(1000, alembic.fillWater(1000, true));
        int poured = room <= 0 ? 0 : WaterInlet.pourFromHeld(player, hand, room, true);
        if (poured <= 0) {
            return WaterInlet.pourFromHeld(player, hand, 1000, true) > 0 ? InteractionResult.FAIL : InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            poured = WaterInlet.pourFromHeld(player, hand, poured, false);
            alembic.fillWater(poured, false);
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Whether the block below heats an alembic. */
    public static boolean isHeat(BlockState below) {
        if (below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) {
            return below.getValue(CampfireBlock.LIT);
        }
        return below.is(BlockTags.FIRE) || below.is(Blocks.LAVA) || below.is(Blocks.MAGMA_BLOCK) || ChemBurnerBlock.isLit(below);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, isHeat(context.getLevel().getBlockState(context.getClickedPos().below())));
        return EssenceMachine.connectAll(context.getLevel(), context.getClickedPos(), state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic) {
            alembic.setOwner(player);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        if (direction == Direction.DOWN) {
            return state.setValue(LIT, isHeat(neighbour));
        }
        return EssenceMachine.connect(state, direction, neighbour);
    }

    /** Opens the alembic's screen. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic) {
            player.openMenu(alembic, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return EssenceMachine.rotatePorts(state.setValue(FACING, rotation.rotate(state.getValue(FACING))), rotation);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** While it boils, the alembic simmers quietly now and then (heard by the client only). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT) && random.nextInt(50) == 0 && level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic
                && ResearchItems.of(alembic.input()) != null && alembic.water() >= AlembicBlockEntity.WATER_PER_ITEM) {
            level.playLocalSound(pos, ModSounds.ALEMBIC_BUBBLE.get(), SoundSource.BLOCKS, 0.35F + random.nextFloat() * 0.15F,
                    0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlembicBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.ALEMBIC.get(), AlembicBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.ALEMBIC.get(), AlembicBlockEntity::serverTick);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AlembicBlockEntity alembic ? alembic : null;
    }

    @Override
    protected com.mojang.serialization.MapCodec<AlembicBlock> codec() {
        return simpleCodec(AlembicBlock::new);
    }
}
