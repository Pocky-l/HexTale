package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.menu.VaultMenu;

import java.util.Optional;

/** A click on the grid of a Hex Vault screen: what was clicked, if anything, and what the player wants done with it. */
public record VaultActionPayload(int containerId, Optional<ItemResource> resource, int action) implements CustomPacketPayload {
    public static final Type<VaultActionPayload> TYPE = new Type<>(Hextale.id("vault_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VaultActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VaultActionPayload::containerId,
            ByteBufCodecs.optional(ItemResource.STREAM_CODEC), VaultActionPayload::resource,
            ByteBufCodecs.VAR_INT, VaultActionPayload::action,
            VaultActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VaultActionPayload payload, IPayloadContext context) {
        VaultMenu.Action[] actions = VaultMenu.Action.values();
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof VaultMenu menu && menu.containerId == payload.containerId()
                && payload.action() >= 0 && payload.action() < actions.length) {
            menu.act(player, payload.resource().orElse(ItemResource.EMPTY), actions[payload.action()]);
        }
    }
}
