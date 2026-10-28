package com.pockyl.hextale.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * A block woken by a sprinkle of something (recipe type {@code hextale:awakening}): any {@code block} of the list or tag,
 * right-clicked with the {@code catalyst} (hextech dust when left out), turns into {@code result} after a short swirl.
 * What the woken block takes over from the old one (a cauldron's water, the furnace's owner) is the result's own business.
 */
public record AwakeningRecipe(HolderSet<Block> block, Optional<Ingredient> catalyst, Block result) {
    public static final MapCodec<AwakeningRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.holderSet(Registries.BLOCK).fieldOf("block").forGetter(AwakeningRecipe::block),
            Ingredient.CODEC.optionalFieldOf("catalyst").forGetter(AwakeningRecipe::catalyst),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("result").forGetter(AwakeningRecipe::result)
    ).apply(instance, AwakeningRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AwakeningRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.holderSet(Registries.BLOCK), AwakeningRecipe::block,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), AwakeningRecipe::catalyst,
            ByteBufCodecs.registry(Registries.BLOCK), AwakeningRecipe::result,
            AwakeningRecipe::new);

    /** Every awakening, server and client. */
    public static final DataRecipes<AwakeningRecipe> ALL = new DataRecipes<>("awakening", MAP_CODEC, STREAM_CODEC,
            recipe -> recipe.result().asItem());

    /** Whether this item wakes blocks of this recipe. */
    public boolean wakesWith(ItemStack stack) {
        return catalyst.map(ingredient -> ingredient.test(stack)).orElseGet(() -> stack.is(ModItems.HEXTECH_DUST.get()));
    }

    /** The awakening of this block by this item, in id order (null when none). */
    public static @Nullable AwakeningRecipe of(BlockState state, ItemStack stack, boolean clientSide) {
        for (AwakeningRecipe recipe : ALL.get(clientSide).values()) {
            if (state.is(recipe.block()) && recipe.wakesWith(stack)) {
                return recipe;
            }
        }
        return null;
    }
}
