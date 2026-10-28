package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.block.ChalkMarkBlock;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModSounds;

import java.util.function.Consumer;

/**
 * Rune Chalk: magic stone ground with dust. Used on the top of a solid block it draws a rune there (a {@link ChalkMarkBlock},
 * one of three glyphs, turned towards the one drawing it) and wears down a little; a stick goes for
 * {@code max_damage} marks. It also goes into the Hextech Altar: the circle is drawn before it stands.
 */
public class RuneChalkItem extends Item {
    public RuneChalkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();
        BlockState mark = ModBlocks.CHALK_MARK.get().defaultBlockState()
                .setValue(ChalkMarkBlock.FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ChalkMarkBlock.GLYPH, level.getRandom().nextInt(3));
        if (!level.getBlockState(pos).canBeReplaced() || !mark.canSurvive(level, pos)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, mark, Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.BRUSH_STROKE.get(), SoundSource.BLOCKS, 0.7F, 0.95F + level.getRandom().nextFloat() * 0.15F);
            Player player = context.getPlayer();
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("item.hextale.rune_chalk.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.rune_chalk.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
