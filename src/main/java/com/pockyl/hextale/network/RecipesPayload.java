package com.pockyl.hextale.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.crafting.DataRecipes;

import java.util.Map;

/**
 * Server to client: every recipe of one of the mod's recipe kinds (Hex Workbench, altar, furnace, crucible, brews, Lab
 * Bench experiments, awakenings), sent on login and after data packs reload. The guide and JEI read them on the client.
 */
public record RecipesPayload(Kind<?> recipes) implements CustomPacketPayload {
    public static final Type<RecipesPayload> TYPE = new Type<>(Hextale.id("recipes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecipesPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RecipesPayload decode(RegistryFriendlyByteBuf buffer) {
            String name = ByteBufCodecs.STRING_UTF8.decode(buffer);
            DataRecipes<?> kind = DataRecipes.byName(name);
            if (kind == null) {
                throw new IllegalArgumentException("Unknown hextale recipe kind " + name);
            }
            return new RecipesPayload(read(kind, buffer));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RecipesPayload payload) {
            ByteBufCodecs.STRING_UTF8.encode(buffer, payload.recipes().kind().name());
            payload.recipes().write(buffer);
        }
    };

    public static <T> RecipesPayload of(DataRecipes<T> kind) {
        return new RecipesPayload(new Kind<>(kind, kind.get(false)));
    }

    private static <T> Kind<T> read(DataRecipes<T> kind, RegistryFriendlyByteBuf buffer) {
        return new Kind<>(kind, kind.syncCodec().decode(buffer));
    }

    /** One kind's recipes, typed. */
    public record Kind<T>(DataRecipes<T> kind, Map<ResourceLocation, T> recipes) {
        void write(RegistryFriendlyByteBuf buffer) {
            kind.syncCodec().encode(buffer, recipes);
        }

        /** Hands the recipes to the client's copy of the kind. */
        public void apply() {
            kind.setClient(recipes);
        }
    }

    @Override
    public Type<RecipesPayload> type() {
        return TYPE;
    }
}
