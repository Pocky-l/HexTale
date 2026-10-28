package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * The Levitation Plate: a low stone plate with a brass ring round a hextech crystal. The air above it lifts whatever
 * stands there (see {@link LevitationPlateBlockEntity}); motes of light rise through the column so it can be seen.
 */
public class LevitationPlateBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = com.pockyl.hextale.port.PortShapes.column(16.0, 0.0, 3.0);

    public LevitationPlateBlock(Properties properties) {
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
        return new LevitationPlateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LEVITATION_PLATE.get(), LevitationPlateBlockEntity::tick);
    }

    /** Motes of light drift up the column, and now and then a faint ring rises off the crystal. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof LevitationPlateBlockEntity plate)) {
            return;
        }
        int reach = plate.reach(level);
        for (int i = 0; i < 2; i++) {
            level.addParticle(ModParticles.glint(ModParticles.HEX_LIGHT), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.25 + random.nextDouble() * reach, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.04, 0);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ModParticles.ring(ModParticles.HEXTECH), pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0.45, 1, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<LevitationPlateBlock> codec() {
        return simpleCodec(LevitationPlateBlock::new);
    }
}
