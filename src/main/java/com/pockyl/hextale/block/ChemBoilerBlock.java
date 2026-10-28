package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.transport.WaterInlet;

import org.jspecify.annotations.Nullable;

/**
 * The Chem Boiler: a riveted pressure tank that sits on a Chem Burner and drives the machines around it with steam
 * ({@link ChemBoilerBlockEntity}, {@link com.pockyl.hextale.chem.Overdrive}). Water comes in by bucket or through pipes
 * on its sides and top; an empty hand reads the gauge. {@link #DRIVING} while its steam drives at least one machine.
 */
public class ChemBoilerBlock extends BaseEntityBlock implements EssenceMachine {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty DRIVING = BooleanProperty.create("driving");
    /** Steam with a breath of the green flame in it. */
    public static final int STEAM = 0xDDEDD8;
    /** What the driven machines puff out. */
    public static final int EXHAUST = 0xA8C27A;

    public ChemBoilerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DRIVING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DRIVING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** Water pipes join it on every side but its floor (the burner is there). */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face != Direction.DOWN;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    /** Where the safety valve lets the steam out, in world space. */
    public static Vec3 valve(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 15.5 / 16, 0.5);
    }

    /** A water bucket (or another mod's water container) fills the tank. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ChemBoilerBlockEntity boiler)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!stack.is(Items.WATER_BUCKET)) {
            int room = Math.min(1000, boiler.fillWater(1000, true));
            int poured = room <= 0 ? 0 : WaterInlet.pourFromHeld(player, hand, room, true);
            if (poured <= 0) {
                return WaterInlet.pourFromHeld(player, hand, 1000, true) > 0 ? InteractionResult.FAIL : InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide()) {
                poured = WaterInlet.pourFromHeld(player, hand, poured, false);
                boiler.fillWater(poured, false);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        if (boiler.fillWater(1000, true) < 1000) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            boiler.fillWater(1000, false);
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
            level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ChemBoilerBlockEntity boiler) {
            player.sendOverlayMessage(boiler.status());
        }
        return InteractionResult.SUCCESS;
    }

    /** While it drives: the tank seethes, a thin plume leaves the vent now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(DRIVING)) {
            return;
        }
        if (random.nextInt(3) == 0) {
            Vec3 valve = valve(pos);
            level.addParticle(ModParticles.vapour(STEAM), valve.x + (random.nextDouble() - 0.5) * 0.1, valve.y, valve.z + (random.nextDouble() - 0.5) * 0.1,
                    0, 0.03 + random.nextDouble() * 0.02, 0);
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
        return new ChemBoilerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CHEM_BOILER.get(),
                level.isClientSide() ? ChemBoilerBlockEntity::clientTick : ChemBoilerBlockEntity::serverTick);
    }
}
