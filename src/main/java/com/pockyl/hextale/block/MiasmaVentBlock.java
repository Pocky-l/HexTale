package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;

import org.jspecify.annotations.Nullable;

/**
 * The Miasma Vent: a rusted iron block with a grate on top, a sickly glow under it. Hostile creatures over it choke in
 * its fumes ({@link MiasmaVentBlockEntity}); a right click with an empty hand tells what it does now.
 */
public class MiasmaVentBlock extends BaseEntityBlock {
    public MiasmaVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MiasmaVentBlockEntity vent) {
            player.displayClientMessage(vent.dry() ? Component.translatable("block.hextale.miasma_vent.status.dry")
                    : Component.translatable("block.hextale.miasma_vent.status", vent.charges()), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** A wisp of fumes seeps out of the grate now and then while it has something to burn. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && level.getBlockEntity(pos) instanceof MiasmaVentBlockEntity vent && !vent.dry() && vent.charges() > 0) {
            level.addParticle(ModParticles.vapour(MiasmaVentBlockEntity.FUMES), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.02,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.015, 0);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MiasmaVentBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.MIASMA_VENT.get(), MiasmaVentBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<MiasmaVentBlock> codec() {
        return simpleCodec(MiasmaVentBlock::new);
    }
}
