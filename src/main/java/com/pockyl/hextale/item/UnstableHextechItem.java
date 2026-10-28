package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.entity.ThrownHextech;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * A lumpy orb of lightning-awakened amethyst; it listens to runes drawn next to it. Raw hextech is volatile: a dropped
 * orb that lands after a long fall, or one caught in a blast, bursts in a flash of arcane sparks that knocks back and
 * hurts what stands close. Orbs fresh from the storm are still settling and do not burst on their first fall. Thrown
 * (by hand or from a dispenser) an orb shatters where it lands into hextech dust; a burst leaves dust too.
 */
public class UnstableHextechItem extends Item implements ProjectileItem {
    /** Blocks an orb must fall to burst on landing. */
    public static final double BURST_FALL = 8.0;
    /** Entity tag of orbs born by lightning; they may fall once without bursting while young. */
    public static final String BORN_TAG = "hextale.storm_born";
    private static final int SETTLING_TICKS = 200;
    private static final double BURST_RADIUS = 2.5;
    /** Dust each orb of a bursting stack leaves behind. */
    public static final int BURST_DUST = 2;
    private static final int THROW_COOLDOWN = 10;

    /** The longest fall of each airborne orb, kept between its ticks (the fall distance is reset by the landing). */
    private static final Map<ItemEntity, Double> FALLS = new WeakHashMap<>();

    public UnstableHextechItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HEXTECH_THROW.get(), SoundSource.NEUTRAL, 0.7F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileFromRotation(ThrownHextech::new, serverLevel, stack, player, 0.0F, 1.2F, 1.0F);
        }
        player.getCooldowns().addCooldown(stack, THROW_COOLDOWN);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        ThrownHextech orb = new ThrownHextech(com.pockyl.hextale.registry.ModEntities.THROWN_HEXTECH.get(), level);
        orb.setPos(position.x(), position.y(), position.z());
        orb.setItem(stack);
        return orb;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        if (entity.isInWater() || entity.isInLava()) {
            FALLS.remove(entity);
        } else if (!entity.onGround()) {
            if (entity.fallDistance > 0) {
                FALLS.merge(entity, entity.fallDistance, Math::max);
            }
        } else {
            Double fall = FALLS.remove(entity);
            boolean settling = entity.entityTags().contains(BORN_TAG) && entity.getAge() < SETTLING_TICKS;
            if (fall != null && fall >= BURST_FALL && !settling) {
                burst(level, entity);
                return true;
            }
        }
        return false;
    }

    /** The orb (the whole stack) bursts: sparks, a flash of shards, a crack, and a push with magic damage around. */
    public static void burst(ServerLevel level, ItemEntity entity) {
        Vec3 centre = entity.position().add(0, 0.2, 0);
        int count = entity.getItem().getCount();
        float damage = Math.min(6.0F, 2.0F + count / 4.0F);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(BURST_RADIUS))) {
            Vec3 away = victim.position().subtract(centre);
            double distance = away.length();
            if (distance > BURST_RADIUS) {
                continue;
            }
            victim.hurtServer(level, level.damageSources().magic(), damage);
            Vec3 push = distance < 1.0E-3 ? new Vec3(0, 1, 0) : away.normalize();
            victim.push(push.x * 0.6, 0.35, push.z * 0.6);
        }
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), centre.x, centre.y, centre.z, 24 + count * 2, 0.25, 0.25, 0.25, 0.6);
        level.sendParticles(ModParticles.glint(0x98E3FB), centre.x, centre.y, centre.z, 14, 0.4, 0.3, 0.4, 0.05);
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), centre.x, centre.y, centre.z, 0, 1.2 + count * 0.05, 0, 0, 1);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
                centre.x, centre.y, centre.z, 20, 0.2, 0.2, 0.2, 0.2);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), centre.x, centre.y, centre.z, 6, 0.1, 0.1, 0.1, 0.12);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.ORB_CRACK.get(), SoundSource.BLOCKS, 1.4F, 0.9F);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.2F, 0.7F);
        for (int dust = count * BURST_DUST; dust > 0; dust -= 64) {
            ItemEntity pinch = new ItemEntity(level, centre.x, centre.y, centre.z, new ItemStack(ModItems.HEXTECH_DUST.get(), Math.min(64, dust)));
            pinch.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(pinch);
        }
        com.pockyl.hextale.thevoid.Thinning.add(level, entity.blockPosition(), count * com.pockyl.hextale.thevoid.Thinning.ORB_BURST, com.pockyl.hextale.thevoid.Thinning.Source.ORB_BURST);
        FALLS.remove(entity);
        entity.discard();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.unstable_hextech.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.unstable_hextech.volatile").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("item.hextale.unstable_hextech.throw").withStyle(ChatFormatting.DARK_AQUA));
    }
}
