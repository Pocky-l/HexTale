package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import org.jspecify.annotations.Nullable;

/**
 * The Chem Still: a copper pot boiler, a swan neck bending over into a cooling coil and a glass receiver at the coil's
 * foot (the receiver stands to the right of the front). Vials of raw shimmer pour in by hand; an empty hand takes the
 * refined vials out or reads how it is doing. {@link #WORKING} while it refines over a lit burner.
 */
public class ChemStillBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 15.0);
    private static final int DRIP = Brew.REFINED_SHIMMER.color();

    public ChemStillBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WORKING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!ChemStillBlockEntity.isRaw(stack) || !(level.getBlockEntity(pos) instanceof ChemStillBlockEntity still)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            int taken = still.feed(stack);
            if (taken <= 0) {
                player.sendOverlayMessage(Component.translatable("block.hextale.chem_still.say.full"));
                return InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(taken);
            }
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ChemStillBlockEntity still) {
            ItemStack refined = still.takeOutput();
            if (refined.isEmpty()) {
                player.sendOverlayMessage(still.status());
            } else {
                if (!player.getInventory().add(refined)) {
                    player.drop(refined, false, net.minecraft.util.Prediction.SERVER_ONLY);
                }
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Where the coil drips into the receiver, in block space. */
    public static Vec3 dripPoint(BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction right = facing.getCounterClockWise();
        return new Vec3(0.5 + right.getStepX() * 0.25 + facing.getStepX() * 0.2, 0.42, 0.5 + right.getStepZ() * 0.25 + facing.getStepZ() * 0.2);
    }

    /** While it works: drops of magenta fall into the receiver, steam leaves the neck, the boiler mutters. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(WORKING)) {
            return;
        }
        Vec3 drip = dripPoint(state).add(pos.getX(), pos.getY(), pos.getZ());
        if (random.nextInt(4) == 0) {
            level.addParticle(new DustParticleOptions(DRIP, 0.5F), drip.x, drip.y, drip.z, 0, -0.05, 0);
        }
        if (random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 0, 0.02, 0);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ModParticles.SHIMMER_MOTE.get(), pos.getX() + 0.3 + random.nextDouble() * 0.3, pos.getY() + 0.5,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.3, 0, 0.01, 0);
        }
        if (random.nextInt(60) == 0) {
            level.playLocalSound(pos, ModSounds.CHEM_STILL_BOIL.get(), SoundSource.BLOCKS, 0.4F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemStillBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CHEM_STILL.get(), ChemStillBlockEntity::serverTick);
    }
}
