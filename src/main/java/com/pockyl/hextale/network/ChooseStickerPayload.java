package com.pockyl.hextale.network;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import io.netty.buffer.ByteBuf;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

/** Client to server: write this essence (or none: -1) on the stickers in this hand. */
public record ChooseStickerPayload(boolean mainHand, int essence) implements CustomPacketPayload {
    public static final Type<ChooseStickerPayload> TYPE = new Type<>(Hextale.id("choose_sticker"));
    public static final StreamCodec<ByteBuf, ChooseStickerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ChooseStickerPayload::mainHand, ByteBufCodecs.VAR_INT, ChooseStickerPayload::essence, ChooseStickerPayload::new);

    @Override
    public Type<ChooseStickerPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseStickerPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!stack.is(ModItems.ESSENCE_STICKER.get())) {
            return;
        }
        if (payload.essence() >= 0 && payload.essence() < Essence.values().length) {
            stack.set(ModDataComponents.STICKER.get(), Essence.values()[payload.essence()]);
        } else {
            stack.remove(ModDataComponents.STICKER.get());
        }
    }
}
