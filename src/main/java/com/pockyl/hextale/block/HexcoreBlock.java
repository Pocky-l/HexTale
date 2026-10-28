package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
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
    public HexcoreBlock(Properties properties) {
        super(properties);
    }

    /**
     * What is stood on: the whole cradle with its tiers, which spread into the neighbouring cells, so they can be climbed
     * like stairs. The outline (the altar's {@code getShape}) stays inside the block's own cell.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state, "collision");
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
