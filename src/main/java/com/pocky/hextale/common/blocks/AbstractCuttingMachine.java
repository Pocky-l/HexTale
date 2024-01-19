package com.pocky.hextale.common.blocks;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCuttingMachine extends Block implements EntityBlock, IForgeBakedModel {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D);

    public AbstractCuttingMachine(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.translucent());
    }

    public BlockState getStateForPlacement(BlockPlaceContext p_57070_) {
        return this.defaultBlockState().setValue(FACING, p_57070_.getHorizontalDirection().getOpposite());
    }

    public InteractionResult use(BlockState p_57083_, Level p_57084_, BlockPos p_57085_, Player p_57086_, InteractionHand p_57087_, BlockHitResult p_57088_) {
        if (p_57084_.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            p_57086_.openMenu(p_57083_.getMenuProvider(p_57084_, p_57085_));
            p_57086_.awardStat(Stats.INTERACT_WITH_STONECUTTER);
            return InteractionResult.CONSUME;
        }
    }

    public VoxelShape getShape(BlockState p_57100_, BlockGetter p_57101_, BlockPos p_57102_, CollisionContext p_57103_) {
        return SHAPE;
    }

    public boolean useShapeForLightOcclusion(BlockState p_57109_) {
        return true;
    }

    public BlockState rotate(BlockState p_57093_, Rotation p_57094_) {
        return p_57093_.setValue(FACING, p_57094_.rotate(p_57093_.getValue(FACING)));
    }

    public BlockState mirror(BlockState p_57090_, Mirror p_57091_) {
        return p_57090_.rotate(p_57091_.getRotation(p_57090_.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_57096_) {
        p_57096_.add(FACING);
    }

    public boolean isPathfindable(BlockState p_57078_, BlockGetter p_57079_, BlockPos p_57080_, PathComputationType p_57081_) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
