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
 * The Hexcore: the Hextech Altar after its heart woke up - its body grown into a massive cradle (three stepped tiers,
 * four buttresses carrying light up into a crown, four brass claws holding the rune matrix that floats over it). It is
 * used like the altar (item in hand onto it, sneak-use or redstone to start); the matrix is drawn by
 * {@link com.pockyl.hextale.client.render.HexcoreRenderer}.
 */
public class HexcoreBlock extends HextechAltarBlock {
    /** What is aimed at and outlined: the cradle's middle, inside its own block (the base spreads into the neighbours). */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 9, 16), Block.box(1, 9, 1, 15, 15, 15),
            Block.box(0, 15, 0, 16, 18, 16));
    /** What is stood on: the three stepped octagons, the piers and the crown, so the tiers can be climbed like stairs. */
    private static final VoxelShape COLLISION = Shapes.or(
            Block.box(-2, 0, -10, 18, 3, 26), Block.box(-7, 0, -7, 23, 3, 23), Block.box(-10, 0, -2, 26, 3, 18),
            Block.box(0, 3, -6, 16, 6, 22), Block.box(-3, 3, -3, 19, 6, 19), Block.box(-6, 3, 0, 22, 6, 16),
            Block.box(4, 3, -9, 12, 8, 25), Block.box(-9, 3, 4, 25, 8, 12),
            Block.box(2, 6, -3, 14, 9, 19), Block.box(-1, 6, -1, 17, 9, 17), Block.box(-3, 6, 2, 19, 9, 14),
            Block.box(1, 9, 1, 15, 15, 15),
            Block.box(1, 15, -5, 15, 18, 21), Block.box(-2, 15, -2, 18, 18, 18), Block.box(-5, 15, 1, 21, 18, 15));

    public HexcoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
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
