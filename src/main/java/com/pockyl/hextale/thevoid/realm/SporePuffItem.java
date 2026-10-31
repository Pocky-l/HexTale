package com.pockyl.hextale.thevoid.realm;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.pockyl.hextale.registry.ModParticles;

/**
 * A spore puff picked: planted on the Void's ground it grows; crushed in the hand (use in the air) it bursts round its
 * bearer, and the spores hold them up - they fall slowly for a while. Something to keep at hand over the abyss.
 */
public class SporePuffItem extends BlockItem {
    /** How long the spores hold one up. */
    public static final int HOLDS = 160;

    public SporePuffItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, HOLDS, 0, false, true, true));
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.PLAYERS, 0.6F, 0.7F);
            server.sendParticles(ModParticles.glint(0xE6B8F0), player.getX(), player.getY() + 1, player.getZ(), 30, 0.5, 0.6, 0.5, 0.04);
            server.sendParticles(ModParticles.vapour(0xB48CD8), player.getX(), player.getY() + 0.6, player.getZ(), 14, 0.4, 0.3, 0.4, 0.02);
            stack.consume(1, player);
            player.getCooldowns().addCooldown(stack, 20);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
