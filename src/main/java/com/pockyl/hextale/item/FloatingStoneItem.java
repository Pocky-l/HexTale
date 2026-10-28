package com.pockyl.hextale.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import com.pockyl.hextale.registry.ModParticles;

/**
 * Floating Stone: placed like any block against another, and also into thin air - right click with nothing aimed at
 * and it settles {@link #REACH} blocks ahead of the eyes and stays there.
 */
public class FloatingStoneItem extends BlockItem {
    public static final double REACH = 3.0;

    public FloatingStoneItem(Block block, Properties properties) {
        super(block, properties);
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        Vec3 eye = player.getEyePosition();
        BlockPos at = BlockPos.containing(eye.add(player.getLookAngle().scale(REACH)));
        BlockState state = getBlock().defaultBlockState();
        if (!level.getBlockState(at).canBeReplaced() || !level.isUnobstructed(state, at, CollisionContext.empty())
                || !level.mayInteract(player, at)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            server.setBlockAndUpdate(at, state);
            server.playSound(null, at, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.3F);
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.getX() + 0.5, at.getY() - 0.05, at.getZ() + 0.5, 0, 0.8, 1, 0, 1);
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 6, 0.3, 0.05, 0.3, 0.02);
            ItemStack stack = player.getItemInHand(hand);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }
}
