package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.PinkBrazierBlockEntity;

import java.util.List;

/**
 * Server to the clients near a Pink Brazier: its breath of warmth has just made the plants at {@code grown} grow. The
 * clients draw the warmth running out over the soil to each of them from this one message.
 */
public record WarmthPayload(BlockPos brazier, List<BlockPos> grown) implements CustomPacketPayload {
    public static final Type<WarmthPayload> TYPE = new Type<>(Hextale.id("warmth"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarmthPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WarmthPayload::brazier,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(PinkBrazierBlockEntity.PULSE_TRIES)), WarmthPayload::grown,
            WarmthPayload::new);

    @Override
    public Type<WarmthPayload> type() {
        return TYPE;
    }
}
