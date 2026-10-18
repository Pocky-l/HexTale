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

/** Client to server: choose a rune on a rune stele ({@code rune} = index in the codex, -1 clears it). */
public record SteleRunePayload(BlockPos pos, int rune) implements CustomPacketPayload {
    public static final Type<SteleRunePayload> TYPE = new Type<>(Hextale.id("stele_rune"));

    public static final StreamCodec<ByteBuf, SteleRunePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SteleRunePayload::pos,
            ByteBufCodecs.VAR_INT, SteleRunePayload::rune,
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
        boolean creative = player.getAbilities().instabuild;
        if (!creative && !knowledge.knows(rune)) {
            return;
        }
        // With an item on the altar only the runes that do something for it may be chosen (the screen shows no others).
        com.pockyl.hextale.block.HextechAltarBlockEntity altar = com.pockyl.hextale.infusion.Ritual.altarOf(player.level(), payload.pos);
        java.util.Set<ComplexRune> allowed = altar == null ? null : com.pockyl.hextale.infusion.Ritual.runesFor(altar.center(), false);
        if (allowed != null && !allowed.contains(rune)) {
            return;
        }
        // Creative researchers choose runes they never studied with full stars.
        stele.choose(rune, knowledge.knows(rune) ? knowledge.stars(rune) : 3);
        player.level().playSound(null, payload.pos, ModSounds.STELE_SET.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
    }
}
