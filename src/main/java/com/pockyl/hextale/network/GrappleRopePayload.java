package com.pockyl.hextale.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.entity.GrappleHook;
import com.pockyl.hextale.registry.ModSounds;

/**
 * Client to server: the rope of the player's Grapple Gauntlet is now {@code length} blocks long (measured when the claw
 * bit, then reeled in or let out). The server shares it with the other players, so they see the rope as it is, and lets
 * them hear the reel.
 */
public record GrappleRopePayload(int hook, float length) implements CustomPacketPayload {
    public static final Type<GrappleRopePayload> TYPE = new Type<>(Hextale.id("grapple_rope"));
    public static final StreamCodec<ByteBuf, GrappleRopePayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT,
            GrappleRopePayload::hook, ByteBufCodecs.FLOAT, GrappleRopePayload::length, GrappleRopePayload::new);

    @Override
    public Type<GrappleRopePayload> type() {
        return TYPE;
    }

    public static void handle(GrappleRopePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.level().getEntity(payload.hook()) instanceof GrappleHook hook)
                || hook.owner() != player || !Float.isFinite(payload.length())) {
            return;
        }
        double before = hook.length();
        hook.reportLength(payload.length());
        long now = player.level().getGameTime();
        if (before > 0 && Math.abs(before - hook.length()) > 0.05 && now - hook.reelSoundAt >= 4) {
            hook.reelSoundAt = now;
            // Everyone but the wearer, who already heard it.
            player.level().playSound(player, player.getX(), player.getY() + 1, player.getZ(), ModSounds.GRAPPLE_REEL.get(), SoundSource.PLAYERS,
                    0.4F, hook.length() < before ? 1.1F : 0.8F);
        }
    }
}
