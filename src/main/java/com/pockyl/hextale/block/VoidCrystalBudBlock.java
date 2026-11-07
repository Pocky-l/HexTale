package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModBlocks;

/** A Void Crystal bud or cluster. Cutting one off a Budding Void Crystal is the diary's step for that block. */
public class VoidCrystalBudBlock extends AmethystClusterBlock {
    public VoidCrystalBudBlock(float height, float width, Properties properties) {
        super(height, width, properties);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player instanceof ServerPlayer server
                && level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).is(ModBlocks.BUDDING_VOID_CRYSTAL.get())) {
            Quests.fire(server, "void_crystal_cut");
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
