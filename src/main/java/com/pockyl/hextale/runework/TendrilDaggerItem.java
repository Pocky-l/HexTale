package com.pockyl.hextale.runework;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.entity.ThrownDagger;

/**
 * The Tendril Dagger (four levels, like the tools): a quick blade that takes the sword's runes. Held in the off hand
 * beside another weapon it strikes along with every blow; held and released after a short swing it is thrown, and it
 * always comes back along a tendril of light ({@link ThrownDagger}). With the Blink rune a right click blinks instead.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public class TendrilDaggerItem extends HextechToolItem {
    /** Ticks of the swing before a release throws. */
    public static final int THROW_TICKS = 8;
    /** Damage the off-hand dagger adds to a main-hand blow, by level (1-4). */
    public static final float[] OFF_HAND = {2, 2.5F, 3, 3.5F};

    public TendrilDaggerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && WeaponRunes.strength(stack, ToolRune.BLINK) > 0 || stack.nextDamageWillBreak()) {
            return super.use(level, player, hand);
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TRIDENT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player) || getUseDuration(stack, entity) - remainingTime < THROW_TICKS) {
            return false;
        }
        if (level instanceof ServerLevel server) {
            stack.hurtWithoutBreaking(1, player);
            ItemStack thrown = stack.consumeAndReturn(1, player);
            ThrownDagger dagger = Projectile.spawnProjectileFromRotation((lvl, owner, item) -> new ThrownDagger(lvl, owner, item),
                    server, thrown, player, 0.0F, 2.2F, 0.5F);
            if (player.hasInfiniteMaterials()) {
                dagger.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            server.playSound(null, dagger, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        return true;
    }

    /** A dagger in the off hand strikes along with the main hand's blow. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player) {
            return;
        }
        ItemStack off = player.getOffhandItem();
        if (!(off.getItem() instanceof TendrilDaggerItem) || player.getMainHandItem().getItem() instanceof TendrilDaggerItem
                || player.getMainHandItem().isEmpty()) {
            return;
        }
        event.setAmount(event.getAmount() + OFF_HAND[RuneTier.tier(off) - 1]);
        player.swing(InteractionHand.OFF_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
    }
}
