package com.pockyl.hextale.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.transport.PylonFlight;

import java.util.List;

/**
 * Server to the clients near a Greedy Funnel: it has just swallowed a death at {@code from}. The clients draw it in from
 * this one message - a few of the drops as {@code icons} and the experience as motes - while the server has already put
 * everything away.
 */
public record FunnelPayload(BlockPos funnel, Vec3 from, List<ItemStack> icons, int experience) implements CustomPacketPayload {
    public static final Type<FunnelPayload> TYPE = new Type<>(Hextale.id("funnel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FunnelPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FunnelPayload::funnel,
            PylonFlight.VEC, FunnelPayload::from,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(4)), FunnelPayload::icons,
            ByteBufCodecs.VAR_INT, FunnelPayload::experience,
            FunnelPayload::new);

    @Override
    public Type<FunnelPayload> type() {
        return TYPE;
    }
}
