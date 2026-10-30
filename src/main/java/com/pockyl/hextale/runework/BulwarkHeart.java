package com.pockyl.hextale.runework;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * The crystal heart of a Bulwark chestplate. Charged with raw hextech (an unstable orb or a synthetic crystal clicked onto
 * the chestplate), it soaks a share of every blow armour can stop - the higher the level, the larger the share - paying
 * one charge per point soaked. When it runs dry it cracks open with a flash that throws the creatures round its wearer
 * back (a last gift, no penalty); a cracked heart soaks nothing until it is charged full again.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class BulwarkHeart {
    public static final int MAX = 100;
    /** Charge one raw orb or synthetic crystal gives. */
    public static final int PER_CRYSTAL = 25;
    /** Share of a blow the heart soaks, by level (1-4). */
    public static final float[] SOAK = {0.3F, 0.4F, 0.5F, 0.6F};
    /** Reach and push of the flash when the heart cracks. */
    public static final double FLASH_REACH = 4;
    private static final double FLASH_PUSH = 1.2;
    private static final int HEART_BLUE = 0x7EF0FF;

    private BulwarkHeart() {
    }

    /** The heart's charge; a cracked heart soaks nothing until it is full again. */
    public record Charge(int charge, boolean cracked) {
        public static final Charge EMPTY = new Charge(0, false);
        public static final Codec<Charge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, MAX).fieldOf("charge").forGetter(Charge::charge),
                Codec.BOOL.optionalFieldOf("cracked", false).forGetter(Charge::cracked)
        ).apply(instance, Charge::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Charge> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Charge::charge,
                ByteBufCodecs.BOOL, Charge::cracked,
                Charge::new);

        public boolean shields() {
            return !cracked && charge > 0;
        }

        /** Charged by {@code amount}; a full heart has mended its crack. */
        public Charge plus(int amount) {
            int now = Math.min(MAX, charge + amount);
            return new Charge(now, cracked && now < MAX);
        }
    }

    public static boolean feeds(ItemStack stack) {
        return stack.is(ModItems.UNSTABLE_HEXTECH.get()) || stack.is(ModItems.SYNTHETIC_CRYSTAL.get());
    }

    public static Charge of(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.HEART_CHARGE.get(), Charge.EMPTY);
    }

    public static void set(ItemStack stack, Charge charge) {
        stack.set(ModDataComponents.HEART_CHARGE.get(), charge);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity wearer = event.getEntity();
        ItemStack chest = wearer.getItemBySlot(EquipmentSlot.CHEST);
        if (!(wearer.level() instanceof ServerLevel level) || !BulwarkArmorItem.isHeart(chest)
                || event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) || event.getAmount() <= 0) {
            return;
        }
        Charge charge = of(chest);
        if (!charge.shields()) {
            return;
        }
        float soak = Math.min(event.getAmount() * SOAK[RuneTier.tier(chest) - 1], charge.charge());
        int cost = Math.max(1, Mth.ceil(soak));
        event.setAmount(event.getAmount() - soak);
        int left = Math.max(0, charge.charge() - cost);
        set(chest, new Charge(left, left == 0));
        Vec3 at = wearer.position().add(0, wearer.getBbHeight() * 0.6, 0);
        level.sendParticles(ModParticles.glint(HEART_BLUE), at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.02);
        if (left == 0) {
            crack(level, wearer, at);
        }
    }

    /** The heart runs dry: a flash that throws the creatures round its wearer back. */
    private static void crack(ServerLevel level, LivingEntity wearer, Vec3 at) {
        level.sendParticles(ModParticles.ring(HEART_BLUE), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.glint(0xFFFFFF), at.x, at.y, at.z, 20, 0.4, 0.4, 0.4, 0.15);
        level.playSound(null, at.x, at.y, at.z, ModSounds.RITUAL_ABSORB.get(), SoundSource.PLAYERS, 0.6F, 0.6F);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, wearer.getBoundingBox().inflate(FLASH_REACH),
                near -> near != wearer && ToolEffects.mayStrike(near, wearer instanceof net.minecraft.world.entity.player.Player player ? player : null))) {
            Vec3 away = near.position().subtract(wearer.position()).multiply(1, 0, 1);
            if (away.lengthSqr() < 1.0E-4) {
                continue;
            }
            away = away.normalize().scale(FLASH_PUSH);
            near.push(away.x, 0.4, away.z);
            near.needsSync = true;
        }
    }
}
