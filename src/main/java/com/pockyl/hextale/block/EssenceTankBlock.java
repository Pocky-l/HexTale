package com.pockyl.hextale.block;

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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.Flasks;
import com.pockyl.hextale.item.EssenceStickerItem;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.network.chat.Component;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * An Essence Jar (the block id is still essence_tank): a big glass jar with a wooden lid that stores a large amount of
 * one liquid (whatever flows in mixes). Pipes fill it through every face, the bottom too; it lets its liquid out at the
 * bottom and through pipes set to pump it out. A bottle of essence by hand pours in, a glass bottle draws out; an empty hand reads it.
 */
public class EssenceTankBlock extends BaseEntityBlock implements EssenceMachine {

    public EssenceTankBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (Direction face : Direction.values()) {
            state = state.setValue(PORTS.get(face), false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction face : Direction.values()) {
            builder.add(PORTS.get(face));
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    /** Every face: in everywhere, out at the bottom (and where a pipe pumps); the model shows a fitting where a pipe joins. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return EssenceMachine.connectAll(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return EssenceMachine.connect(state, direction, neighbour);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return EssenceMachine.rotatePorts(state, rotation);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        push(level, pos, player);
        return com.pockyl.hextale.port.Interactions.itemOn(use(stack, level, pos, player, hand, hitResult));
    }

    /** Every right click nudges the jar: it rocks on its foot for a moment (drawn by the renderer on every client). */
    private void push(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            level.blockEvent(pos, this, EssenceTankBlockEntity.PUSHED, player.getDirection().get2DDataValue());
        }
    }

    private InteractionResult use(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof EssenceTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (com.pockyl.hextale.item.HextechSkypackItem.is(stack)) {
            // A skypack drinks the air out of the jar (any jar, the creative one too).
            if (!com.pockyl.hextale.item.HextechSkypackItem.fillFrom((ServerLevel) level, tank, stack, player.position().add(0, 1.1, 0),
                    com.pockyl.hextale.item.HextechSkypackItem.CAPACITY)) {
                player.displayClientMessage(Component.translatable("item.hextale.hextech_skypack.no_air"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (com.pockyl.hextale.item.RecallCharmItem.is(stack)) {
            // A recall charm drinks rift essence to wind itself again.
            int wound = com.pockyl.hextale.item.RecallCharmItem.wind((ServerLevel) level, tank, stack, player.position().add(0, 1.1, 0));
            player.displayClientMessage(wound > 0 ? Component.translatable("item.hextale.recall_charm.wound",
                    com.pockyl.hextale.item.RecallCharmItem.windings(stack), com.pockyl.hextale.item.RecallCharmItem.WINDINGS)
                    : Component.translatable(com.pockyl.hextale.item.RecallCharmItem.windings(stack) < com.pockyl.hextale.item.RecallCharmItem.WINDINGS
                    ? "item.hextale.recall_charm.no_rift" : "item.hextale.recall_charm.full"), true);
            return InteractionResult.SUCCESS;
        }
        if (tank.isCreative()) {
            return InteractionResult.PASS;
        }
        if (stack.is(ModItems.ESSENCE_STICKER.get())) {
            // A sticker goes on the jar (the one already there comes back); a blank one asks for its essence first.
            Essence essence = EssenceStickerItem.essence(stack);
            if (essence == null) {
                player.displayClientMessage(Component.translatable("item.hextale.essence_sticker.blank"), true);
                return InteractionResult.SUCCESS;
            }
            Essence old = tank.sticker();
            tank.setSticker(essence);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (old != null) {
                com.pockyl.hextale.item.PylonFilterItem.give(player, EssenceStickerItem.of(ModItems.ESSENCE_STICKER.get(), old));
            }
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.3F);
            if (level instanceof ServerLevel serverLevel) {
                // The label takes: a hexagon of its essence's colour ripples over the face it went on, a wisp of it rises.
                Direction face = hitResult.getDirection();
                Vec3 at = hitResult.getLocation().add(face.getStepX() * 0.03, face.getStepY() * 0.03, face.getStepZ() * 0.03);
                int color = essence.color();
                com.pockyl.hextale.network.RuneFxPayload.send(serverLevel, com.pockyl.hextale.network.RuneFxPayload.ripple(at, face, 0.4F, color));
                serverLevel.sendParticles(ModParticles.wisp(color), at.x, at.y, at.z, 4, 0.08, 0.1, 0.08, 0.01);
                serverLevel.sendParticles(ModParticles.glint(color), at.x, at.y, at.z, 3, 0.12, 0.12, 0.12, 0.01);
            }
            player.displayClientMessage(Component.translatable("block.hextale.essence_tank.sticker", Component.translatable(essence.translationKey())), true);
            return InteractionResult.SUCCESS;
        }
        if (tank.sticker() != null && EssenceFlaskItem.isFlask(stack) && !EssenceFlaskItem.contents(stack).isEmpty()
                && !tank.accepts(EssenceFlaskItem.contents(stack))) {
            player.displayClientMessage(Component.translatable("block.hextale.essence_tank.refuses",
                    Component.translatable(tank.sticker().translationKey())), true);
            return InteractionResult.SUCCESS;
        }
        boolean pouring = EssenceFlaskItem.isFlask(stack) && !EssenceFlaskItem.contents(stack).isEmpty();
        int liquid = pouring ? EssenceFlaskItem.contents(stack).color() : tank.tank().contents().color();
        if (Flasks.use(player, hand, stack, tank.tank())) {
            tank.setChanged();
            if (level instanceof ServerLevel server) {
                Vec3 mouth = com.pockyl.hextale.essence.JarReach.mouth(pos);
                Vec3 bottle = com.pockyl.hextale.network.EssenceFxPayload.hand(player);
                com.pockyl.hextale.network.EssenceFxPayload.pour(server, pouring ? bottle : mouth, pouring ? mouth : bottle, liquid, 10);
            }
            level.playSound(null, pos, ModSounds.TANK_GLUG.get(), SoundSource.BLOCKS, 0.6F, 0.8F + level.getRandom().nextFloat() * 0.3F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.getMainHandItem().isEmpty()) {
            // With an item in hand the push already came from useItemOn.
            push(level, pos, player);
        }
        if (!(level.getBlockEntity(pos) instanceof EssenceTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive() && tank.sticker() != null) {
            // Sneak-use with an empty hand peels the sticker off.
            if (!level.isClientSide()) {
                com.pockyl.hextale.item.PylonFilterItem.give(player, EssenceStickerItem.of(ModItems.ESSENCE_STICKER.get(), tank.sticker()));
                tank.setSticker(null);
                level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            player.displayClientMessage(Flasks.describe(tank.tank()), true);
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
        return createTickerHelper(type, ModBlockEntities.ESSENCE_TANK.get(),
                level.isClientSide() ? EssenceTankBlockEntity::clientTick : EssenceTankBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<EssenceTankBlock> codec() {
        return simpleCodec(EssenceTankBlock::new);
    }
}
