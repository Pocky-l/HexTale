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
    /** An empty hand shows the lantern's quiet zone on the ground. */
    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                                                  net.minecraft.world.entity.player.Player player,
                                                                  net.minecraft.world.phys.BlockHitResult hit) {
        showArea(level, pos);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.@org.jspecify.annotations.Nullable LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        showArea(level, pos);
    }

    private static void showArea(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel server && level.getBlockEntity(pos) instanceof NullLanternBlockEntity lantern) {
            com.pockyl.hextale.network.RuneFxPayload.send(server, com.pockyl.hextale.network.RuneFxPayload.area(
                    net.minecraft.world.phys.Vec3.atBottomCenterOf(pos), com.pockyl.hextale.rune.ComplexRunes.of(
                            com.pockyl.hextale.rune.Essence.LIGHT, com.pockyl.hextale.rune.Essence.SHADOW),
                    lantern.radius(), com.pockyl.hextale.network.RuneFxPayload.RUNE_COLOR));
        }
    }

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
