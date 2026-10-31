package com.pockyl.hextale.runework;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.entity.ThrownDagger;

/**
 * The Tendril Dagger (four levels, like the tools): a quick blade that takes the sword's runes. Held in the off hand
 * beside another weapon it sometimes finds a gap and strikes along with the blow; held and released after a short swing it is thrown, and it
 * always comes back along a tendril of light ({@link ThrownDagger}). With the Blink rune a right click blinks instead.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public class TendrilDaggerItem extends HextechToolItem.Sword {
    /** Ticks of the swing before a release throws. */
    public static final int THROW_TICKS = 8;
    /** Damage the off-hand dagger adds when it finds a gap beside a main-hand blow, by level (1-4). */
    public static final float[] OFF_HAND = {3, 4, 5, 6};
    /** Chance that the off-hand dagger finds that gap, by level (1-4): a felt proc, never a flat bonus on every blow. */
    public static final float[] OFF_HAND_CHANCE = {0.30F, 0.35F, 0.40F, 0.45F};

    public TendrilDaggerItem(net.minecraft.world.item.Tier material, Properties properties) {
        super(material, properties);
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && WeaponRunes.strength(stack, ToolRune.BLINK) > 0 || com.pockyl.hextale.port.PortItems.nextDamageWillBreak(stack)) {
            return super.use(level, player, hand).getResult();
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player) || getUseDuration(stack, entity) - remainingTime < THROW_TICKS) {
            return;
        }
        if (level instanceof ServerLevel server) {
            com.pockyl.hextale.port.PortItems.hurtWithoutBreaking(stack, 1, player);
            ItemStack thrown = stack.consumeAndReturn(1, player);
            ThrownDagger dagger = com.pockyl.hextale.port.PortProjectiles.spawnProjectileFromRotation((lvl, owner, item) -> new ThrownDagger(lvl, owner, item),
                    server, thrown, player, 0.0F, 2.2F, 0.5F);
            if (player.hasInfiniteMaterials()) {
                dagger.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            server.playSound(null, dagger, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
        }
    }

    /** What an off-hand dagger adds to a blow for a roll in [0, 1): its level's bite when the roll finds the gap, else nothing. */
    public static float offHandExtra(ItemStack dagger, float roll) {
        int level = RuneTier.tier(dagger) - 1;
        return roll < OFF_HAND_CHANCE[level] ? OFF_HAND[level] : 0;
    }

    /** A dagger in the off hand sometimes strikes along with the main hand's blow. */
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
        float extra = offHandExtra(off, player.getRandom().nextFloat());
        if (extra <= 0) {
            return;
        }
        event.setAmount(event.getAmount() + extra);
        player.swing(InteractionHand.OFF_HAND, true);
        if (player.level() instanceof ServerLevel server) {
            LivingEntity target = event.getEntity();
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(),
                    6, 0.2, 0.25, 0.2, 0.15);
            server.playSound(null, target, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.25F, 1.6F);
        }
    }
}
