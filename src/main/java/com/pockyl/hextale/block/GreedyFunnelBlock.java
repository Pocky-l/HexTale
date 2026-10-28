package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The Greedy Funnel: a nullstone cone narrowing from a brass-cornered rim to a brass spout, a vortex of shadow turning in
 * its mouth. Set on a chest, it swallows the deaths around it - drops into the chest, experience into the crystal -
 * before a single item or orb appears ({@link GreedyFunnelBlockEntity}). A right click with an empty hand takes the experience, or tells what it
 * does when there is none.
 */
public class GreedyFunnelBlock extends BaseEntityBlock {
    /** The spout and its flange, the cone, the rim with its brass corners. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 6, 11), Block.box(2, 6, 2, 14, 12, 14),
            Block.box(0, 12, 0, 16, 16, 16));

    public GreedyFunnelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof GreedyFunnelBlockEntity funnel) {
            int points = funnel.release(server);
            player.displayClientMessage(points > 0 ? Component.translatable("block.hextale.greedy_funnel.released", points) : status(funnel), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** What it does now, in one line: reach, decay, experience - or why it lets deaths be. */
    public static Component status(GreedyFunnelBlockEntity funnel) {
        if (funnel.hungry()) {
            return Component.translatable("block.hextale.greedy_funnel.status.hungry");
        }
        if (funnel.full()) {
            return Component.translatable("block.hextale.greedy_funnel.status.full");
        }
        int size = funnel.radius() * 2 + 1;
        return Component.translatable("block.hextale.greedy_funnel.status", size, size, funnel.charges(), funnel.experience());
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
        return new GreedyFunnelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GREEDY_FUNNEL.get(),
                level.isClientSide() ? GreedyFunnelBlockEntity::clientTick : GreedyFunnelBlockEntity::serverTick);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<GreedyFunnelBlock> codec() {
        return simpleCodec(GreedyFunnelBlock::new);
    }
}
