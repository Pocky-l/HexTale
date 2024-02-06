package com.pocky.hextale.common.blocks;

import com.pocky.hextale.common.register.ModBlockEntities;
import com.pocky.hextale.common.register.ModItems;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class HexcoreBlock extends BaseEntityBlock implements EntityBlock, IForgeBakedModel {

    protected static final VoxelShape SHAPE = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 24.0D, 16.0D);

    public HexcoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState p_60547_, BlockGetter p_60548_, BlockPos p_60549_) {
        return Block.box(0.0D, 0.0D, 0.0D, 16.0D, 32.0D, 16.0D);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.translucent());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            HexcoreEntity entity = (HexcoreEntity) level.getBlockEntity(pos);
            if (entity != null) {
                if (player.getItemInHand(hand).is(ModItems.PROCESSED_HEXTECH_CRYSTAL.get())) {
                    if (entity.items.get(0).is(Items.AIR)) {
                        entity.items.set(0, player.getItemInHand(hand).copy());
                        player.getItemInHand(hand).shrink(1);
                        entity.setChanged();
                        entity.playSoundActivate();
                        return InteractionResult.SUCCESS;
                    }
                }

                if (!entity.items.get(0).is(Items.AIR)) {
                    entity.nearbyItems.clear();
                    entity.nearbyItems.addAll(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(5.0D)));
                    if (entity.craftMatchingRecipe(level, false)) {
                        entity.itemDiscardIndex = 0;
                        entity.itemDiscardTick = 0;
                        entity.craftProgress = 0;
                        entity.startAttractingEntities();
                    }
                }
            }
            // Return SUCCESS to consume the interaction
            return InteractionResult.SUCCESS;
        } else {
            // Handle client-side behavior or other hand interactions
            return InteractionResult.SUCCESS;  // Or CONSUME if appropriate
        }
    }

    public VoxelShape getShape(BlockState p_57100_, BlockGetter p_57101_, BlockPos p_57102_, CollisionContext p_57103_) {
        return SHAPE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ModBlockEntities.HEXCORE.get(), HexcoreEntity::serverTick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.HEXCORE.get().create(pos, state);
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> blockEntityType,
                                                                                                            BlockEntityType<E> blockEntityType1,
                                                                                                            BlockEntityTicker<? super E> blockEntityTicker) {
        return blockEntityType1 == blockEntityType ? (BlockEntityTicker<A>)blockEntityTicker : null;
    }
}
