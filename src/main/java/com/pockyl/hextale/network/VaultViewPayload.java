package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.menu.VaultMenu;

import java.util.List;

/**
 * What an open Hex Vault screen shows: lines of the grid (all of them after {@code reset}, later only the ones that
 * changed; a count of 0 takes a line away) and the state of the vault and the toll.
 *
 * @param waiting ticks until a take through dry jars may go
 * @param note    a refusal to show, as a {@code VaultContents.Refusal} ordinal, or -1
 * @param working the vault stands in its building with the trial won, so it takes items in
 * @param pins    the favourites row, in order
 */
public record VaultViewPayload(int containerId, boolean reset, List<VaultMenu.Line> lines, long used, long capacity, int waiting, boolean dry, int note,
                               boolean working, List<ItemResource> pins)
        implements CustomPacketPayload {
    public static final Type<VaultViewPayload> TYPE = new Type<>(Hextale.id("vault_view"));
    private static final StreamCodec<RegistryFriendlyByteBuf, VaultMenu.Line> LINE = StreamCodec.composite(
            ItemResource.STREAM_CODEC, VaultMenu.Line::resource,
            ByteBufCodecs.VAR_LONG, VaultMenu.Line::count,
            ByteBufCodecs.VAR_INT, VaultMenu.Line::weight,
            VaultMenu.Line::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, VaultViewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VaultViewPayload::containerId,
            ByteBufCodecs.BOOL, VaultViewPayload::reset,
            LINE.apply(ByteBufCodecs.list()), VaultViewPayload::lines,
            ByteBufCodecs.VAR_LONG, VaultViewPayload::used,
            ByteBufCodecs.VAR_LONG, VaultViewPayload::capacity,
            ByteBufCodecs.VAR_INT, VaultViewPayload::waiting,
            ByteBufCodecs.BOOL, VaultViewPayload::dry,
            ByteBufCodecs.VAR_INT, VaultViewPayload::note,
            ByteBufCodecs.BOOL, VaultViewPayload::working,
            ItemResource.STREAM_CODEC.apply(ByteBufCodecs.list()), VaultViewPayload::pins,
            VaultViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
