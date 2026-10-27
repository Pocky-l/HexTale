package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.block.ChalkSigilBlock;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModSounds;

import java.util.function.Consumer;

/**
 * Binding Chalk: Stabilizing Chalk steeped in shadow at the Hextech Altar. Used on the top of a solid block it draws
 * one cell of a line ({@link ChalkSigilBlock}); cells join their neighbours, and a line closed round a piece of floor
 * becomes a binding figure ({@link com.pockyl.hextale.block.ChalkFigure}). Each cell wears the stick a little.
 */
public class BindingChalkItem extends Item {
    public BindingChalkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();
        ChalkSigilBlock sigil = ModBlocks.CHALK_SIGIL.get();
        BlockState line = sigil.drawn(level, pos, level.getRandom());
        if (!level.getBlockState(pos).canBeReplaced() || level.getBlockState(pos).is(sigil) || !line.canSurvive(level, pos)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, line, Block.UPDATE_ALL);
            server.playSound(null, pos, ModSounds.CHALK_SCRATCH.get(), SoundSource.BLOCKS, 0.7F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            server.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, 6, 0.25, 0.01, 0.25, 0.01);
            Player player = context.getPlayer();
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.binding_chalk.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.binding_chalk.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
