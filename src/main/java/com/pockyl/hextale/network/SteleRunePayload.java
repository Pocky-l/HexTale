package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

/**
 * Client to server: choose a rune on a rune stele ({@code rune} = index in the codex, -1 clears it) with {@code stars}
 * picked by hand, or 0 for the researcher's best.
 */
public record SteleRunePayload(BlockPos pos, int rune, int stars) implements CustomPacketPayload {
    public static final Type<SteleRunePayload> TYPE = new Type<>(Hextale.id("stele_rune"));

    public static final StreamCodec<ByteBuf, SteleRunePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SteleRunePayload::pos,
            ByteBufCodecs.VAR_INT, SteleRunePayload::rune,
            ByteBufCodecs.VAR_INT, SteleRunePayload::stars,
            SteleRunePayload::new);

    @Override
    public Type<SteleRunePayload> type() {
        return TYPE;
    }

    public static void handle(SteleRunePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.distanceToSqr(Vec3.atCenterOf(payload.pos)) > 64
                || !(player.level().getBlockEntity(payload.pos) instanceof RuneSteleBlockEntity stele)) {
            return;
        }
        if (payload.rune < 0 || payload.rune >= ComplexRunes.COUNT) {
            stele.choose(null, 1);
            return;
        }
        ComplexRune rune = ComplexRunes.all().get(payload.rune);
        RuneKnowledge knowledge = Research.of(player);
        if (!knowledge.knows(rune)) {
            return;
        }
        // With an item on the altar only the runes that do something for it may be chosen (the screen shows no others).
        com.pockyl.hextale.block.HextechAltarBlockEntity altar = com.pockyl.hextale.infusion.Ritual.altarOf(player.level(), payload.pos);
        java.util.Set<ComplexRune> allowed = altar == null ? null : com.pockyl.hextale.infusion.Ritual.runesFor(altar.center(), false);
        if (allowed != null && !allowed.contains(rune)) {
            return;
        }
        // One stele per rune, no more runes than the item has slots, no clashing runes.
        if (altar != null && allowed != null && rune != stele.rune()) {
            net.minecraft.network.chat.Component refusal = com.pockyl.hextale.infusion.Ritual.steleRefusal(altar.center(),
                    com.pockyl.hextale.infusion.Ritual.onOtherSteles(player.level(), altar, payload.pos), rune);
            if (refusal != null) {
                player.sendOverlayMessage(refusal);
                return;
            }
        }
        // Picked stars may be fewer than the researcher knows, never more.
        int known = knowledge.stars(rune);
        stele.choose(rune, payload.stars > 0 ? Math.min(payload.stars, known) : known, payload.stars > 0);
        player.level().playSound(null, payload.pos, ModSounds.STELE_SET.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
    }
}
