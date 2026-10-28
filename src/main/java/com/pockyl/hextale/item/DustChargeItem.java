package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.pockyl.hextale.entity.DustCharge;
import com.pockyl.hextale.registry.ModEntities;

import java.util.function.Consumer;

/** Hextech dust packed into a paper twist: thrown (or shot from a dispenser), it sticks and cracks the rock around it. */
public class DustChargeItem extends Item implements ProjectileItem {
    private static final int THROW_COOLDOWN = 10;

    public DustChargeItem(Properties properties) {
        super(properties);
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel server) {
            com.pockyl.hextale.port.PortProjectiles.spawnProjectileFromRotation(DustCharge::new, server, stack, player, 0.0F, 1.0F, 1.0F);
        }
        player.getCooldowns().addCooldown(stack.getItem(), THROW_COOLDOWN);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        DustCharge charge = new DustCharge(ModEntities.DUST_CHARGE.get(), level);
        charge.setPos(position.x(), position.y(), position.z());
        charge.setItem(stack);
        return charge;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("item.hextale.dust_charge.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.dust_charge.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
