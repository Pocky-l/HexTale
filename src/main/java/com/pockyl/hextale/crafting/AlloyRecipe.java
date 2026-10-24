package com.pockyl.hextale.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ItemStackTemplate;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A Hex Furnace alloy, read from {@code data/<namespace>/hextale_alloying/<name>.json}: one to three {@code inputs}
 * (ingredient + count, each in its own input slot, any order) melt together in {@code time} ticks of burning fuel into
 * {@code result}, paying {@code experience} when the result is taken out.
 */
public record AlloyRecipe(List<SizedIngredient> inputs, ItemStackTemplate result, int time, float experience) {
    public static final int SLOTS = 3;

    public static final Codec<AlloyRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf(1, SLOTS).fieldOf("inputs").forGetter(AlloyRecipe::inputs),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(AlloyRecipe::result),
            Codec.intRange(1, 6000).optionalFieldOf("time", 200).forGetter(AlloyRecipe::time),
            Codec.FLOAT.optionalFieldOf("experience", 0.0F).forGetter(AlloyRecipe::experience)
    ).apply(instance, AlloyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloyRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), AlloyRecipe::inputs,
            ItemStackTemplate.STREAM_CODEC, AlloyRecipe::result,
            ByteBufCodecs.VAR_INT, AlloyRecipe::time,
            ByteBufCodecs.FLOAT, AlloyRecipe::experience,
            AlloyRecipe::new);

    /** Every alloy, server and client. */
    public static final DataRecipes<AlloyRecipe> ALL = new DataRecipes<>("hextale_alloying", CODEC);

    /**
     * Which input slot feeds each of the recipe's inputs, or null when the slots do not hold the recipe: every input
     * needs its own slot with enough of it, and no slot may hold anything the recipe does not take.
     */
    public int @Nullable [] assign(List<ItemStack> slots) {
        int[] chosen = new int[inputs.size()];
        return assign(slots, 0, new boolean[slots.size()], chosen) && leftoversEmpty(slots, chosen) ? chosen : null;
    }

    private boolean assign(List<ItemStack> slots, int input, boolean[] used, int[] chosen) {
        if (input == inputs.size()) {
            return true;
        }
        for (int slot = 0; slot < slots.size(); slot++) {
            if (!used[slot] && inputs.get(input).test(slots.get(slot))) {
                used[slot] = true;
                chosen[input] = slot;
                if (assign(slots, input + 1, used, chosen)) {
                    return true;
                }
                used[slot] = false;
            }
        }
        return false;
    }

    private static boolean leftoversEmpty(List<ItemStack> slots, int[] chosen) {
        for (int slot = 0; slot < slots.size(); slot++) {
            boolean taken = false;
            for (int c : chosen) {
                taken |= c == slot;
            }
            if (!taken && !slots.get(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The first alloy the slots hold, in id order. */
    public static Map.@Nullable Entry<ResourceLocation, AlloyRecipe> find(List<ItemStack> slots, boolean clientSide) {
        for (Map.Entry<ResourceLocation, AlloyRecipe> entry : ALL.get(clientSide).entrySet()) {
            if (entry.getValue().assign(slots) != null) {
                return entry;
            }
        }
        return null;
    }
}
