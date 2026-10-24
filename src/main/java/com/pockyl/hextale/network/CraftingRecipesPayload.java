package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.AlloyRecipe;
import com.pockyl.hextale.crafting.BrewRecipe;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.research.ExperimentPlan;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server to client: every Hex Workbench, Hex Furnace, Crucible and brew recipe and every Lab Bench experiment, sent on login
 * and after data packs reload.
 */
public record CraftingRecipesPayload(Map<ResourceLocation, HexCraftingRecipe> crafting, Map<ResourceLocation, AlloyRecipe> alloying,
                                     Map<ResourceLocation, CrucibleRecipe> crucible, Map<ResourceLocation, BrewRecipe> brewing,
                                     Map<ResourceLocation, ExperimentPlan> experiments)
        implements CustomPacketPayload {
    public static final Type<CraftingRecipesPayload> TYPE = new Type<>(Hextale.id("crafting_recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftingRecipesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(LinkedHashMap<ResourceLocation, HexCraftingRecipe>::new, ResourceLocation.STREAM_CODEC, HexCraftingRecipe.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.crafting()),
            ByteBufCodecs.map(LinkedHashMap<ResourceLocation, AlloyRecipe>::new, ResourceLocation.STREAM_CODEC, AlloyRecipe.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.alloying()),
            ByteBufCodecs.map(LinkedHashMap<ResourceLocation, CrucibleRecipe>::new, ResourceLocation.STREAM_CODEC, CrucibleRecipe.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.crucible()),
            ByteBufCodecs.map(LinkedHashMap<ResourceLocation, BrewRecipe>::new, ResourceLocation.STREAM_CODEC, BrewRecipe.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.brewing()),
            ByteBufCodecs.map(LinkedHashMap<ResourceLocation, ExperimentPlan>::new, ResourceLocation.STREAM_CODEC, ExperimentPlan.STREAM_CODEC),
            payload -> new LinkedHashMap<>(payload.experiments()),
            CraftingRecipesPayload::new);

    @Override
    public Type<CraftingRecipesPayload> type() {
        return TYPE;
    }
}
