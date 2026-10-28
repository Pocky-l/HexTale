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

    /**
     * Motes of light drift up the column and fade where it ends - so its height shows - and a faint ring hangs at its top
     * now and then. While it carries someone the motes stream up thickly.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof LevitationPlateBlockEntity plate)) {
            return;
        }
        int reach = plate.reach(level);
        int motes = plate.riders() > 0 ? 6 : 2;
        for (int i = 0; i < motes; i++) {
            double y = pos.getY() + 0.4 + random.nextDouble() * reach;
            level.addParticle(ModParticles.glint(ModParticles.HEX_LIGHT), pos.getX() + 0.2 + random.nextDouble() * 0.6, y,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, Math.min(0.06, (pos.getY() + 1 + reach - y) * 0.03), 0);
        }
        if (random.nextInt(plate.riders() > 0 ? 3 : 8) == 0) {
            level.addParticle(ModParticles.ring(ModParticles.HEXTECH), pos.getX() + 0.5, pos.getY() + 1 + reach - 0.05, pos.getZ() + 0.5, 0.4, 1, 0);
        }
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<LevitationPlateBlock> codec() {
        return simpleCodec(LevitationPlateBlock::new);
    }
}
