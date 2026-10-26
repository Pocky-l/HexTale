package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * The Null Lantern: a lantern of nullstone whose cold light drinks the magic that lets hostile creatures form - none
 * spawn by themselves within its quiet (see {@link NullLanternBlockEntity}). Stands or hangs like a lantern.
 */
public class NullLanternBlock extends LanternBlock implements EntityBlock {
    private static final int LIGHT = 0xC6B4F2;

    public NullLanternBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NullLanternBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != ModBlockEntities.NULL_LANTERN.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<NullLanternBlockEntity>) NullLanternBlockEntity::serverTick;
    }

    /** Motes of null light sink slowly out of it. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.glint(LIGHT), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.3 + random.nextDouble() * 0.3,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, -0.01, 0);
        }
    }
}
