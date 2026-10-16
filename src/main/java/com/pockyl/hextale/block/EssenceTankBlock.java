package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.Flasks;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechTools;

import org.jspecify.annotations.Nullable;

/**
 * An essence tank: stores a large amount of one liquid (whatever flows in mixes). Pipes fill it through the top and the
 * sides; it lets its liquid out at the bottom. A bottle of essence by hand pours in, a glass bottle draws out; an empty
 * hand reads it.
 */
public class EssenceTankBlock extends BaseEntityBlock implements EssenceMachine {
    public EssenceTankBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction face : Direction.Plane.HORIZONTAL) {
            state = state.setValue(PORTS.get(face), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            builder.add(PORTS.get(face));
        }
    }

    /** Every face: in at the top and the sides, out at the bottom. Only the side joints show on the model. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return EssenceMachine.connectAll(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbour, RandomSource random) {
        return EssenceMachine.connect(state, direction, neighbour);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return EssenceMachine.rotatePorts(state, rotation);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof EssenceTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (HextechTools.isTool(stack)) {
            int taken = HextechTools.fillFrom(stack, tank.tank());
            if (taken < 0) {
                player.sendOverlayMessage(Component.translatable("item.hextale.hextech_tool.unknown_essence"));
                return InteractionResult.FAIL;
            }
            if (taken > 0) {
                tank.setChanged();
                level.playSound(null, pos, ModSounds.TANK_GLUG.get(), SoundSource.BLOCKS, 0.6F, 1.2F);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            player.sendOverlayMessage(Component.translatable("item.hextale.hextech_tool.cell",
                    HextechTools.cell(stack).describe(), HextechTools.cell(stack).total(), HextechTools.CELL));
            return InteractionResult.SUCCESS;
        }
        Essence pure = EssenceFlaskItem.contents(stack).pureEssence();
        if (tank.isCreative() && pure != null) {
            tank.setCreativeEssence(pure);
            player.sendOverlayMessage(Flasks.describe(tank.tank()));
            return InteractionResult.SUCCESS;
        }
        if (Flasks.use(player, hand, stack, tank.tank())) {
            tank.setChanged();
            level.playSound(null, pos, ModSounds.TANK_GLUG.get(), SoundSource.BLOCKS, 0.6F, 0.8F + level.getRandom().nextFloat() * 0.3F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof EssenceTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive() && hitResult.getDirection() != Direction.DOWN) {
            // Sneak-use with an empty hand: the next essence's sticker on that face (a sign only).
            if (!level.isClientSide()) {
                tank.cycleLabel(hitResult.getDirection());
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.3F);
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 at = hitResult.getLocation();
                    serverLevel.sendParticles(ParticleTypes.WAX_ON, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Flasks.describe(tank.tank()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ESSENCE_TANK.get(), EssenceTankBlockEntity::serverTick);
    }
}
