package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.infusion.InfusionRecipe;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server to client: every altar recipe, sent on login and after data packs reload. */
public record InfusionRecipesPayload(Map<ResourceLocation, InfusionRecipe> recipes) implements CustomPacketPayload {
    public static final Type<InfusionRecipesPayload> TYPE = new Type<>(Hextale.id("infusion_recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionRecipesPayload> STREAM_CODEC =
            ByteBufCodecs.map(LinkedHashMap<ResourceLocation, InfusionRecipe>::new, ResourceLocation.STREAM_CODEC, InfusionRecipe.STREAM_CODEC)
                    .map(InfusionRecipesPayload::new, payload -> new LinkedHashMap<>(payload.recipes()));

    @Override
    public Type<InfusionRecipesPayload> type() {
        return TYPE;
    }
}
