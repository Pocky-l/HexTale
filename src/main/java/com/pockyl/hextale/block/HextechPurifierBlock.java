package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Hextech Purifier: a white calcite font of clear glowing water under a brass canopy; from the canopy's hub a
 * hextech crystal hangs point down over the water and draws the Thinning out of the land around, clouding over as it fills
 * ({@link HextechPurifierBlockEntity}). A hextech crystal in hand goes in; an empty hand takes the
 * crystal out, or tells what it does when it holds none.
 */
public class HextechPurifierBlock extends BaseEntityBlock {
    /** The calcite font, the brass canopy over it on its four posts and the cap of its hub. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 5, 14), Block.box(3, 5, 3, 13, 13, 13),
            Block.box(6, 13, 6, 10, 15, 10));

    public HextechPurifierBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!stack.is(ModItems.HEXTECH_CRYSTAL.get()) || !(level.getBlockEntity(pos) instanceof HextechPurifierBlockEntity purifier)
                || !purifier.crystal().isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            purifier.insert(stack);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HextechPurifierBlockEntity purifier) {
            if (purifier.crystal().isEmpty()) {
                player.sendOverlayMessage(status(purifier));
            } else {
                player.getInventory().placeItemBackInInventory(purifier.take());
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 0.7F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** What it does now, in one line. */
    public static Component status(HextechPurifierBlockEntity purifier) {
        int size = purifier.radius() * 2 + 1;
        return switch (purifier.mode()) {
            case EMPTY -> Component.translatable("block.hextale.hextech_purifier.status.empty");
            case CLOUDED -> Component.translatable("block.hextale.hextech_purifier.status.clouded");
            case UNLIT -> Component.translatable("block.hextale.hextech_purifier.status.unlit");
            case CLEAN -> Component.translatable("block.hextale.hextech_purifier.status.clean", size, size);
            case WORKING -> Component.translatable("block.hextale.hextech_purifier.status", size, size,
                    String.format(Locale.ROOT, "%.0f", purifier.clouding() * 100));
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** A comparator reads how clouded the crystal is (15 = clouded through, 0 = none in it). */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof HextechPurifierBlockEntity purifier) || purifier.crystal().isEmpty()) {
            return 0;
        }
        return 1 + Math.round(purifier.clouding() * 14);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HextechPurifierBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.HEXTECH_PURIFIER.get(), HextechPurifierBlockEntity::serverTick);
    }

    @Override
    protected com.mojang.serialization.MapCodec<HextechPurifierBlock> codec() {
        return simpleCodec(HextechPurifierBlock::new);
    }
}
