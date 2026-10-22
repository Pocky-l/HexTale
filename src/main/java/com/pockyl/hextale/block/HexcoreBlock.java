package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * The Hexcore: a rune matrix around a hextech crystal, floating over a brass cradle. The block model is the cradle; the
 * matrix is drawn by {@link com.pockyl.hextale.client.render.HexcoreRenderer} so it can turn.
 */
public class HexcoreBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(5, 3, 5, 11, 8, 11),
            Block.box(3, 8, 3, 13, 11, 13));

    public HexcoreBlock(Properties properties) {
        super(properties);
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexcoreBlockEntity(pos, state);
    }
}
