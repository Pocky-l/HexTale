package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.AlloyRecipe;
import com.pockyl.hextale.crafting.BrewRecipe;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.crafting.HexCraftingRecipe;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server to client: every Hex Workbench, Hex Furnace, Crucible and brew recipe, sent on login and after data packs reload. */
public record CraftingRecipesPayload(Map<Identifier, HexCraftingRecipe> crafting, Map<Identifier, AlloyRecipe> alloying,
                                     Map<Identifier, CrucibleRecipe> crucible, Map<Identifier, BrewRecipe> brewing)
        implements CustomPacketPayload {
    public static final Type<CraftingRecipesPayload> TYPE = new Type<>(Hextale.id("crafting_recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingRecipesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(LinkedHashMap<Identifier, HexCraftingRecipe>::new, Identifier.STREAM_CODEC, HexCraftingRecipe.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.crafting()),
            ByteBufCodecs.map(LinkedHashMap<Identifier, AlloyRecipe>::new, Identifier.STREAM_CODEC, AlloyRecipe.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.alloying()),
            ByteBufCodecs.map(LinkedHashMap<Identifier, CrucibleRecipe>::new, Identifier.STREAM_CODEC, CrucibleRecipe.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.crucible()),
            ByteBufCodecs.map(LinkedHashMap<Identifier, BrewRecipe>::new, Identifier.STREAM_CODEC, BrewRecipe.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.brewing()),
            CraftingRecipesPayload::new);

    @Override
    public Type<CraftingRecipesPayload> type() {
        return TYPE;
    }
}
