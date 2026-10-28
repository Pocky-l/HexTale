package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * The Foundry Hatch: an iron furnace door set in fire bricks. Alone it is only a door; built into the front of a
 * Hex Foundry and lit, it becomes the kiln's controller ({@link HexFoundryBlockEntity}) and its renderer draws the whole
 * kiln. While the kiln burns, flames lick through the hatch's grille and smoke and sparks rise from the top grate.
 */
public class HexFoundryBlock extends BaseEntityBlock implements WorldlyContainerHolder {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Part of a lit kiln (the hatch alone is a plain block). */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    /** Burning right now. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HexFoundryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED, LIT);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof HexFoundryBlockEntity foundry) {
            if (foundry.isFormed()) {
                player.openMenu(foundry, buf -> buf.writeBlockPos(pos));
            } else {
                player.sendOverlayMessage(Component.translatable("block.hextale.hex_foundry.alone"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    /** Formed, the renderer draws the whole kiln; alone it is the hatch's own model. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    /** Formed, it lets light through like the kiln's other blocks, so the kiln's model is lit from where it stands. */
    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return state.getValue(FORMED);
    }

    /** While it burns: flames behind the grille, smoke and sparks over the top grate, the roar of a big fire. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(FORMED) || !state.getValue(LIT)) {
            return;
        }
        Direction front = state.getValue(FACING);
        double across = random.nextDouble() * 0.5 - 0.25;
        double ax = front.getAxis() == Direction.Axis.X ? 0 : across;
        double az = front.getAxis() == Direction.Axis.Z ? 0 : across;
        double x = pos.getX() + 0.5 + front.getStepX() * 0.55 + ax;
        double z = pos.getZ() + 0.5 + front.getStepZ() * 0.55 + az;
        level.addParticle(ModParticles.flame(ModParticles.FIRE), x, pos.getY() + 0.25 + random.nextDouble() * 0.2, z, 0, 0.004, 0);
        Vec3 grate = Vec3.atCenterOf(HexFoundryBlockEntity.heart(pos, front).above());
        if (random.nextInt(2) == 0) {
            level.addParticle(ModParticles.vapour(ModParticles.SMOKE), grate.x + random.nextGaussian() * 0.15, grate.y + 0.6,
                    grate.z + random.nextGaussian() * 0.15, 0, 0.05, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ModParticles.spark(ModParticles.FIRE), grate.x + random.nextGaussian() * 0.15, grate.y + 0.55,
                    grate.z + random.nextGaussian() * 0.15, 0, 0.08, 0);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexFoundryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.HEX_FOUNDRY.get(), HexFoundryBlockEntity::serverTick);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return state.getValue(FORMED) && level.getBlockEntity(pos) instanceof HexFoundryBlockEntity foundry ? foundry : null;
    }

    /** One status line for Jade and the goggles. */
    public static Component status(HexFoundryBlockEntity foundry) {
        if (!foundry.blocked().isEmpty()) {
            return Component.translatable("gui.hextale.hex_foundry." + foundry.blocked());
        }
        int percent = foundry.heat() * 100 / HexFoundryBlockEntity.MAX_HEAT;
        return Component.translatable(foundry.heat() < HexFoundryBlockEntity.MELT_HEAT ? "gui.hextale.hex_foundry.waking" : "gui.hextale.hex_foundry.heat",
                percent);
    }

    @Override
    protected com.mojang.serialization.MapCodec<HexFoundryBlock> codec() {
        return simpleCodec(HexFoundryBlock::new);
    }
}
