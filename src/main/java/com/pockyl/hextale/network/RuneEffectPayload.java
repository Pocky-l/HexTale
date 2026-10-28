package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModSounds;

import java.util.List;
import java.util.function.Supplier;

/**
 * Server to the clients near a rune at work, drawn by the client's {@code RuneEffects}: what the rune DID, where it did
 * it (owner 2026-10-28: "every rune its own effect - 3x3 digging -> lightning striking the blocks, smelting -> the blocks
 * catch fire - with quiet sounds"). One family of effect per message, played with that family's quiet sound:
 * <ul>
 * <li>{@link Family#ARCS}: lightning from {@code at} striking every block of {@code blocks};</li>
 * <li>{@link Family#KINDLE}: every block of {@code blocks} catches fire for a moment;</li>
 * <li>{@link Family#CRUMBLE}: dust and a thread of light running block by block along {@code blocks};</li>
 * <li>{@link Family#RISE}: motes spiralling up from {@code at} (life, light, heat - by colour);</li>
 * <li>{@link Family#GUST}: a breath of air whirling round {@code at}.</li>
 * </ul>
 */
public record RuneEffectPayload(int family, Vec3 at, List<BlockPos> blocks, int color, float size) implements CustomPacketPayload {
    public static final int MAX_BLOCKS = 64;
    private static final double RADIUS = 40;

    public enum Family {
        ARCS(ModSounds.RUNE_CRACKLE, 0.35F),
        KINDLE(ModSounds.RUNE_KINDLE, 0.35F),
        CRUMBLE(ModSounds.RUNE_CRUMBLE, 0.35F),
        RISE(ModSounds.RUNE_BLOOM, 0.25F),
        GUST(ModSounds.RUNE_GUST, 0.3F);

        final Supplier<SoundEvent> sound;
        final float volume;

        Family(Supplier<SoundEvent> sound, float volume) {
            this.sound = sound;
            this.volume = volume;
        }
    }

    public static final Type<RuneEffectPayload> TYPE = new Type<>(Hextale.id("rune_effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RuneEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RuneEffectPayload::family,
            Vec3.STREAM_CODEC, RuneEffectPayload::at,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_BLOCKS)), RuneEffectPayload::blocks,
            ByteBufCodecs.INT, RuneEffectPayload::color,
            ByteBufCodecs.FLOAT, RuneEffectPayload::size,
            RuneEffectPayload::new);

    public Family kind() {
        return Family.values()[Math.clamp(family, 0, Family.values().length - 1)];
    }

    /** Shows {@code family} at {@code at} over {@code blocks} in {@code color} to the players near, with its quiet sound. */
    public static void show(ServerLevel level, Family family, Vec3 at, List<BlockPos> blocks, int color, float size) {
        List<BlockPos> sent = blocks.size() > MAX_BLOCKS ? List.copyOf(blocks.subList(0, MAX_BLOCKS)) : List.copyOf(blocks);
        RuneEffectPayload payload = new RuneEffectPayload(family.ordinal(), at, sent, color, size);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) <= RADIUS * RADIUS) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
        // One sound for one moment: a 3x3 smelted at once is one kindling, not nine.
        long now = level.getGameTime();
        long key = BlockPos.asLong((int) at.x >> 3, (int) at.y >> 3, (int) at.z >> 3) * 8 + family.ordinal();
        java.util.Map<Long, Long> heard = HEARD.computeIfAbsent(level, l -> new java.util.HashMap<>());
        if (heard.getOrDefault(key, Long.MIN_VALUE) == now) {
            return;
        }
        if (heard.size() > 256) {
            heard.clear();
        }
        heard.put(key, now);
        level.playSound(null, at.x, at.y, at.z, family.sound.get(), SoundSource.PLAYERS, family.volume,
                0.9F + level.getRandom().nextFloat() * 0.2F);
    }

    /** Per level: the tick each 8-block cell last heard each family. */
    private static final java.util.Map<ServerLevel, java.util.Map<Long, Long>> HEARD = new java.util.WeakHashMap<>();

    /** {@link #show} round one point (RISE, GUST). */
    public static void show(ServerLevel level, Family family, Vec3 at, int color, float size) {
        show(level, family, at, List.of(), color, size);
    }

    @Override
    public Type<RuneEffectPayload> type() {
        return TYPE;
    }
}
