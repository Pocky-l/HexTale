package com.pockyl.hextale.chem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModEffects;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;

/**
 * Shimmer in the veins: power now, dependence later - without ever taking anything from the player. Every vial of refined
 * shimmer drunk leaves one more violet vein ({@link #MAX} at most) and feeds the body for a while ({@link #fedTicks}, the
 * shorter the more veins there are). While fed, the veins give: a heart, half a point of damage and a little size each;
 * from {@value #NIGHT_EYES} veins eyes that see in the dark; at {@value #MAX} hands that never tire (Haste). When the
 * feeding runs out the gifts are only withheld - the veins dim, nothing is lost below what the body was before - until the
 * next vial. A bottle of essence with at least {@value #WASH_LIGHT} mB of light essence washes one vein out: hextech's
 * clean answer.
 *
 * <p>Kept on the player as the attachment {@code hextale:shimmer_veins} (synced, so everyone sees the veins); the gifts
 * are transient attribute modifiers and effects renewed every {@value #CHECK_TICKS} ticks.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ShimmerVeins {
    public static final int MAX = 5;
    /** Ticks a vial feeds one vein; every further vein takes {@value #FED_LESS} off. */
    public static final int FED_TICKS = 24000;
    public static final int FED_LESS = 2400;
    /** Millibuckets of light essence a bottle needs to wash a vein out. */
    public static final int WASH_LIGHT = 8;
    public static final float HEALTH = 2;
    public static final float DAMAGE = 0.5F;
    public static final float SIZE = 0.02F;
    /** Veins from which the eyes see in the dark. */
    public static final int NIGHT_EYES = 3;
    /** Light level below which a place counts as dark. */
    public static final int DARK = 6;
    public static final int CHECK_TICKS = 20;
    /** Night Vision kept this long, so it never starts to blink. */
    private static final int NIGHT_TICKS = 400;
    private static final int VIOLET = 0xC24CF0;
    private static final ResourceLocation HEALTH_ID = Hextale.id("shimmer_veins_health");
    private static final ResourceLocation DAMAGE_ID = Hextale.id("shimmer_veins_damage");
    private static final ResourceLocation SIZE_ID = Hextale.id("shimmer_veins_size");
    public static final String VEINS_GROWN = "shimmer_veins";
    public static final String VEINS_WASHED = "veins_washed";
    public static final String CRAVING = "shimmer_craving";

    /** The veins of a player and the game time until which the body is fed. */
    public record Veins(int count, long fedUntil) {
        public static final Veins NONE = new Veins(0, 0);
        public static final Codec<Veins> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("count").forGetter(Veins::count),
                Codec.LONG.fieldOf("fed_until").forGetter(Veins::fedUntil)).apply(instance, Veins::new));
        public static final StreamCodec<ByteBuf, Veins> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Veins::count, ByteBufCodecs.VAR_LONG, Veins::fedUntil, Veins::new);

        public boolean fed(long now) {
            return count > 0 && now < fedUntil;
        }
    }

    private ShimmerVeins() {
    }

    public static Veins of(Player player) {
        return player.getData(ModAttachments.SHIMMER_VEINS);
    }

    /** Ticks a vial feeds a body with this many veins. */
    public static int fedTicks(int veins) {
        return FED_TICKS - FED_LESS * Math.max(0, Math.min(veins, MAX) - 1);
    }

    /** A vial of refined shimmer drunk: one more vein, and the body is fed afresh. */
    public static void drink(ServerPlayer player) {
        Veins before = of(player);
        int count = Math.min(MAX, before.count() + 1);
        long now = player.level().getGameTime();
        player.setData(ModAttachments.SHIMMER_VEINS, new Veins(count, now + fedTicks(count)));
        com.pockyl.hextale.port.PortSync.syncData(player, ModAttachments.SHIMMER_VEINS);
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(0, player.getBbHeight() * 0.55, 0);
        level.sendParticles(ModParticles.SHIMMER_MOTE.get(), at.x, at.y, at.z, 14, 0.3, 0.45, 0.3, 0.02);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHIMMER_RUSH.get(), SoundSource.PLAYERS, 0.8F,
                1.05F - count * 0.04F);
        apply(player);
        Quests.fire(player, VEINS_GROWN);
    }

    /** A bottle of essence drunk: enough light essence in it washes one vein out. Returns whether it did. */
    public static boolean wash(ServerPlayer player, EssenceMix drunk) {
        Veins veins = of(player);
        if (veins.count() <= 0 || drunk.get(Essence.LIGHT) < WASH_LIGHT) {
            return false;
        }
        int count = veins.count() - 1;
        player.setData(ModAttachments.SHIMMER_VEINS, count == 0 ? Veins.NONE : new Veins(count, veins.fedUntil()));
        com.pockyl.hextale.port.PortSync.syncData(player, ModAttachments.SHIMMER_VEINS);
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(0, player.getBbHeight() * 0.55, 0);
        level.sendParticles(ModParticles.glint(ModParticles.GOLD), at.x, at.y, at.z, 12, 0.3, 0.5, 0.3, 0.02);
        level.sendParticles(ModParticles.voidMote(VIOLET), at.x, at.y + 0.6, at.z, 0, (level.getRandom().nextDouble() - 0.5) * 1.5, -1.2,
                (level.getRandom().nextDouble() - 0.5) * 1.5, 1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.VEINS_WASH.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        apply(player);
        Quests.fire(player, VEINS_WASHED);
        return true;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % CHECK_TICKS == 0) {
            apply(player);
        }
    }

    /** Gives the gifts of fed veins, or withholds them. */
    public static void apply(ServerPlayer player) {
        Veins veins = of(player);
        long now = player.level().getGameTime();
        boolean fed = veins.fed(now);
        int count = fed ? veins.count() : 0;
        show(player, veins, now);
        boolean had = modify(player, Attributes.MAX_HEALTH, HEALTH_ID, count * HEALTH);
        modify(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, count * DAMAGE);
        modify(player, Attributes.SCALE, SIZE_ID, count * SIZE);
        if (!fed) {
            if (had) {
                // The feeding ran out: the body lets go of what the shimmer lent it.
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHIMMER_FADE.get(), SoundSource.PLAYERS,
                        0.7F, 1.0F);
                Quests.fire(player, CRAVING);
            }
            return;
        }
        boolean dark = player.level().getMaxLocalRawBrightness(player.blockPosition()) < DARK;
        if (count >= NIGHT_EYES && dark) {
            keep(player, MobEffects.NIGHT_VISION, NIGHT_TICKS, 0);
        } else {
            MobEffectInstance night = player.getEffect(MobEffects.NIGHT_VISION);
            if (night != null && night.isAmbient() && night.getDuration() <= NIGHT_TICKS) {
                // Only our own night eyes close in the light; a potion's stay.
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        }
        if (count >= MAX) {
            keep(player, MobEffects.DIG_SPEED, CHECK_TICKS * 3, 0);
        }
    }

    /**
     * Shows the veins as a status effect, so the player sees them by the HUD's effect icons and in the inventory:
     * "Shimmer in the Veins" (level = veins, time = how long the feeding lasts) while fed, "Fading Veins" (no end) when
     * the feeding ran out, nothing without veins. The effects do nothing themselves; the attachment is the truth.
     */
    private static void show(Player player, Veins veins, long now) {
        boolean fed = veins.fed(now);
        Holder<MobEffect> wanted = veins.count() <= 0 ? null : fed ? ModEffects.SHIMMER_VEINS : ModEffects.SHIMMER_CRAVING;
        Holder<MobEffect> other = wanted == ModEffects.SHIMMER_VEINS ? ModEffects.SHIMMER_CRAVING : ModEffects.SHIMMER_VEINS;
        if (wanted == null) {
            player.removeEffect(ModEffects.SHIMMER_VEINS);
            player.removeEffect(ModEffects.SHIMMER_CRAVING);
            return;
        }
        player.removeEffect(other);
        int amplifier = veins.count() - 1;
        int ticks = fed ? (int) Math.min(Integer.MAX_VALUE, veins.fedUntil() - now) : MobEffectInstance.INFINITE_DURATION;
        MobEffectInstance current = player.getEffect(wanted);
        if (current != null && current.getAmplifier() == amplifier
                && (!fed || Math.abs(current.getDuration() - ticks) <= CHECK_TICKS * 2)) {
            return;
        }
        player.removeEffect(wanted);
        player.addEffect(new MobEffectInstance(wanted, ticks, amplifier, true, false, true));
    }

    /** Sets the veins' modifier of an attribute ({@code amount} 0 = none); returns whether one was there before. */
    private static boolean modify(Player player, Holder<Attribute> attribute, ResourceLocation id, float amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return false;
        }
        AttributeModifier old = instance.getModifier(id);
        if (amount <= 0) {
            if (old != null) {
                instance.removeModifier(id);
                if (attribute == Attributes.MAX_HEALTH && player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
            return old != null;
        }
        if (old == null || old.amount() != amount) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
        return old != null;
    }

    /** Keeps a quiet effect of the veins on (ambient, no particles). */
    private static void keep(Player player, Holder<MobEffect> effect, int ticks, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.isAmbient() && current.getDuration() < ticks - CHECK_TICKS) {
            player.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false, true));
        }
    }
}
