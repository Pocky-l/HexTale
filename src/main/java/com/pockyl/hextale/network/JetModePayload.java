package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ChemJets;

/** Client to server: what the keys of a jet tank's wearer ask for now. The server checks everything again each tick. */
public record JetModePayload(ChemJets.Mode mode) implements CustomPacketPayload {
    public static final Type<JetModePayload> TYPE = new Type<>(Hextale.id("jet_mode"));
    public static final StreamCodec<ByteBuf, JetModePayload> STREAM_CODEC = ByteBufCodecs.idMapper(i -> ChemJets.Mode.values()[i], ChemJets.Mode::ordinal)
            .map(JetModePayload::new, JetModePayload::mode);

    @Override
    public Type<JetModePayload> type() {
        return TYPE;
    }

    public static void handle(JetModePayload payload, IPayloadContext context) {
        ChemJets.ask(context.player(), payload.mode);
    }
}
