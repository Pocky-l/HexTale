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
import net.minecraft.world.item.ItemUtils;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * The Axolotl Tank. An axolotl in a bucket goes in, an empty bucket takes it out again; fish (or a bucket of tropical
 * fish, four meals) feed it; an empty hand takes out the slime it shed, or reads how it is doing.
 */
public class AxolotlTankBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public AxolotlTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof AxolotlTankBlockEntity tank)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (stack.is(Items.AXOLOTL_BUCKET)) {
            if (!level.isClientSide()) {
                if (!tank.letIn(stack)) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("block.hextale.axolotl_tank.say.taken"));
                    return InteractionResult.FAIL;
                }
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_AXOLOTL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.BUCKET) && tank.hasAxolotl()) {
            if (!level.isClientSide()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, tank.takeOut()));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL_AXOLOTL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.TROPICAL_FISH_BUCKET)) {
            if (!level.isClientSide()) {
                if (!tank.feedBucket()) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("block.hextale.axolotl_tank.say.fed"));
                    return InteractionResult.FAIL;
                }
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_FISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (AxolotlTankBlockEntity.isFood(stack)) {
            if (!level.isClientSide()) {
                int taken = tank.feed(stack);
                if (taken <= 0) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("block.hextale.axolotl_tank.say.fed"));
                    return InteractionResult.FAIL;
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(taken);
                }
                level.playSound(null, pos, SoundEvents.AXOLOTL_SPLASH, SoundSource.BLOCKS, 0.5F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AxolotlTankBlockEntity tank) {
            ItemStack slime = tank.takeOutput();
            if (slime.isEmpty()) {
                player.sendOverlayMessage(tank.status());
            } else {
                if (!player.getInventory().add(slime)) {
                    player.drop(slime, false);
                }
                level.playSound(null, pos, SoundEvents.SLIME_BLOCK_HIT, SoundSource.BLOCKS, 0.6F, 1.3F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Bubbles rise in the water while an axolotl lives there. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && level.getBlockEntity(pos) instanceof AxolotlTankBlockEntity tank && tank.hasAxolotl()) {
            level.addParticle(ModParticles.bubble(ModParticles.WATER), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.15,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.03, 0);
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
        return new AxolotlTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.AXOLOTL_TANK.get(), AxolotlTankBlockEntity::serverTick);
    }

    @Override
    protected com.mojang.serialization.MapCodec<AxolotlTankBlock> codec() {
        return simpleCodec(AxolotlTankBlock::new);
    }
}
