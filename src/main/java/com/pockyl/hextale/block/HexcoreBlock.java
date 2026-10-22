package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The Hexcore: the Hextech Altar after its heart woke up - a rune matrix floating over a brass cradle. It is used like
 * the altar (item in hand onto it, sneak-use or redstone to start); the matrix is drawn by
 * {@link com.pockyl.hextale.client.render.HexcoreRenderer}.
 */
public class HexcoreBlock extends HextechAltarBlock {
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexcoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.HEXCORE.get(), HextechAltarBlockEntity::clientTick)
                : createTickerHelper(type, ModBlockEntities.HEXCORE.get(), HextechAltarBlockEntity::serverTick);
    }
}
