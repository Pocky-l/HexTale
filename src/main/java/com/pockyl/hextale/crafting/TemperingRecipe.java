package com.pockyl.hextale.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * A Chem Vat tempering, recipe type {@code hextale:tempering}: the {@code inputs} (ingredient + count, anywhere among the
 * vat's input slots) soak for {@code time} ticks over the vat's fire in a bath that holds at least {@code acid} doses of
 * acid and {@code shimmer} doses of refined shimmer, which the tempering then spends, and come out as {@code result}.
 * {@code thinning} is what one tempering adds to the land's Thinning (chemtech pays there for making, never for using).
 */
public record TemperingRecipe(List<SizedIngredient> inputs, int acid, int shimmer, ItemStackTemplate result, int time, float thinning) {
    /** Input slots of the vat. */
    public static final int SLOTS = 3;

    public static final MapCodec<TemperingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SizedIngredient.NESTED_CODEC.listOf(1, SLOTS).fieldOf("inputs").forGetter(TemperingRecipe::inputs),
            Codec.intRange(0, 64).optionalFieldOf("acid", 0).forGetter(TemperingRecipe::acid),
            Codec.intRange(0, 64).optionalFieldOf("shimmer", 0).forGetter(TemperingRecipe::shimmer),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(TemperingRecipe::result),
            Codec.intRange(1, 12000).optionalFieldOf("time", 600).forGetter(TemperingRecipe::time),
            Codec.FLOAT.optionalFieldOf("thinning", 4.0F).forGetter(TemperingRecipe::thinning)
    ).apply(instance, TemperingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TemperingRecipe> STREAM_CODEC = StreamCodec.composite(
            SizedIngredient.STREAM_CODEC.apply(ByteBufCodecs.list()), TemperingRecipe::inputs,
            ByteBufCodecs.VAR_INT, TemperingRecipe::acid,
            ByteBufCodecs.VAR_INT, TemperingRecipe::shimmer,
            ItemStackTemplate.STREAM_CODEC, TemperingRecipe::result,
            ByteBufCodecs.VAR_INT, TemperingRecipe::time,
            ByteBufCodecs.FLOAT, TemperingRecipe::thinning,
            TemperingRecipe::new);

    /** Every tempering, server and client. */
    public static final DataRecipes<TemperingRecipe> ALL = new DataRecipes<>("tempering", MAP_CODEC, STREAM_CODEC,
            recipe -> recipe.result().item().value());

    /** The slot that feeds each input, or null when the slots do not hold every input (other slots may hold anything). */
    public int @Nullable [] assign(List<ItemStack> slots) {
        int[] chosen = new int[inputs.size()];
        return assign(slots, 0, new boolean[slots.size()], chosen) ? chosen : null;
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

    /** The first tempering the slots hold, in id order, with the slot of each input; null when none. */
    public static @Nullable Found find(List<ItemStack> slots, boolean clientSide) {
        for (Map.Entry<Identifier, TemperingRecipe> entry : ALL.get(clientSide).entrySet()) {
            int[] chosen = entry.getValue().assign(slots);
            if (chosen != null) {
                return new Found(entry.getKey(), entry.getValue(), chosen);
            }
        }
        return null;
    }

    /** Whether the item goes into some tempering (the vat takes only those). */
    public static boolean isInput(ItemStack stack, boolean clientSide) {
        for (TemperingRecipe recipe : ALL.get(clientSide).values()) {
            for (SizedIngredient input : recipe.inputs()) {
                if (input.ingredient().test(stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A tempering and the slot that feeds each of its inputs. */
    public record Found(Identifier id, TemperingRecipe recipe, int[] slots) {
    }
}
