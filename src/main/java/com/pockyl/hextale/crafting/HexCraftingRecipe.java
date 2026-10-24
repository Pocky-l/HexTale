package com.pockyl.hextale.crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.neoforged.neoforge.common.util.RecipeMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * A Hex Workbench recipe, read from {@code data/<namespace>/hextale_crafting/<name>.json}: a vanilla-style shaped
 * ({@code key} + {@code pattern}) or shapeless ({@code ingredients}) grid, its {@code result}, and the {@code charge}
 * the workbench's crystal spends on it (default 1).
 */
public record HexCraftingRecipe(Either<ShapedRecipePattern, List<Ingredient>> shape, ItemStackTemplate result, int charge) {
    private static final MapCodec<Either<ShapedRecipePattern, List<Ingredient>>> SHAPE_CODEC =
            Codec.mapEither(ShapedRecipePattern.MAP_CODEC, Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients"));

    public static final Codec<HexCraftingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SHAPE_CODEC.forGetter(HexCraftingRecipe::shape),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(HexCraftingRecipe::result),
            Codec.intRange(0, 64).optionalFieldOf("charge", 1).forGetter(HexCraftingRecipe::charge)
    ).apply(instance, HexCraftingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HexCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.either(ShapedRecipePattern.STREAM_CODEC, Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list())),
            HexCraftingRecipe::shape,
            ItemStackTemplate.STREAM_CODEC, HexCraftingRecipe::result,
            ByteBufCodecs.VAR_INT, HexCraftingRecipe::charge,
            HexCraftingRecipe::new);

    /** Every hex crafting recipe, server and client. */
    public static final DataRecipes<HexCraftingRecipe> ALL = new DataRecipes<>("hextale_crafting", CODEC);

    /** Whether the grid (trimmed to its used rows and columns) holds this recipe. */
    public boolean matches(CraftingInput input) {
        return shape.map(pattern -> pattern.matches(input), ingredients -> {
            if (input.ingredientCount() != ingredients.size()) {
                return false;
            }
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : input.items()) {
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
            return RecipeMatcher.findMatches(items, ingredients) != null;
        });
    }

    /** The nine grid cells as the guide and JEI draw them, row by row (empty where nothing goes). */
    public List<Optional<Ingredient>> grid() {
        List<Optional<Ingredient>> cells = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            cells.add(Optional.empty());
        }
        shape.ifLeft(pattern -> {
            for (int y = 0; y < pattern.height(); y++) {
                for (int x = 0; x < pattern.width(); x++) {
                    cells.set(y * 3 + x, pattern.ingredients().get(y * pattern.width() + x));
                }
            }
        }).ifRight(ingredients -> {
            for (int i = 0; i < ingredients.size(); i++) {
                cells.set(i, Optional.of(ingredients.get(i)));
            }
        });
        return cells;
    }

    public boolean shapeless() {
        return shape.right().isPresent();
    }

    /** The first recipe the grid holds, in id order. */
    public static Map.@Nullable Entry<ResourceLocation, HexCraftingRecipe> find(CraftingInput input, boolean clientSide) {
        if (input.isEmpty()) {
            return null;
        }
        for (Map.Entry<ResourceLocation, HexCraftingRecipe> entry : ALL.get(clientSide).entrySet()) {
            if (entry.getValue().matches(input)) {
                return entry;
            }
        }
        return null;
    }
}
