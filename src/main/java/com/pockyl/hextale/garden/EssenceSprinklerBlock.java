package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.block.EssenceMachine;
import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The Essence Sprinkler: a copper riser with a turning head. Water and sap come in by pipe (sides and bottom), brews
 * from vials poured into it; every few seconds it sprays the beds around it. A plain use tells what is in it.
 */
public class EssenceSprinklerBlock extends BaseEntityBlock implements EssenceMachine {
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

    public EssenceSprinklerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face != Direction.UP;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof EssenceSprinklerBlockEntity sprinkler)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return GardenPouring.pourInto(sprinkler, stack, level, player, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof EssenceSprinklerBlockEntity sprinkler)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            player.sendOverlayMessage(sprinkler.status());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.pockyl.hextale.block.ModelShapes.covers(this) ? com.pockyl.hextale.block.ModelShapes.of(state) : SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceSprinklerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ESSENCE_SPRINKLER.get(), EssenceSprinklerBlockEntity::serverTick);
    }
}
