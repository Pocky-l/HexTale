package com.pockyl.hextale.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.essence.EssenceMix;

import java.util.Comparator;

import org.jspecify.annotations.Nullable;

/**
 * A Crucible transmutation, read from {@code data/<namespace>/hextale_crucible/<name>.json}: a {@code catalyst} item
 * thrown into a crucible that holds at least the recipe's {@code essences} takes them out of the water and rises as
 * {@code result}. A catalyst the water cannot pay for is just another item: boiling water dissolves it into its own
 * essences.
 */
public record CrucibleRecipe(Ingredient catalyst, EssenceMix essences, ItemStackTemplate result) {
    public static final Codec<CrucibleRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("catalyst").forGetter(CrucibleRecipe::catalyst),
            EssenceMix.CODEC.fieldOf("essences").forGetter(CrucibleRecipe::essences),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrucibleRecipe::result)
    ).apply(instance, CrucibleRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CrucibleRecipe::catalyst,
            EssenceMix.STREAM_CODEC, CrucibleRecipe::essences,
            ItemStackTemplate.STREAM_CODEC, CrucibleRecipe::result,
            CrucibleRecipe::new);

    /** Every transmutation, server and client. */
    public static final DataRecipes<CrucibleRecipe> ALL = new DataRecipes<>("hextale_crucible", CODEC);

    /**
     * The transmutation this item starts in water holding {@code essence}: of the recipes for it that the water can pay,
     * the one asking the most (so a richer brew is never wasted on a lesser result).
     */
    public static @Nullable CrucibleRecipe find(ItemStack stack, EssenceMix essence) {
        return ALL.get(false).values().stream()
                .filter(recipe -> recipe.catalyst().test(stack) && essence.holds(recipe.essences()))
                .max(Comparator.comparingInt(recipe -> recipe.essences().total()))
                .orElse(null);
    }
}
