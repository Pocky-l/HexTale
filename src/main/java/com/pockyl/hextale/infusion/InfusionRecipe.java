package com.pockyl.hextale.infusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.List;

/**
 * One thing the Hextech Altar makes, read from {@code data/<namespace>/hextale_infusion/<name>.json}: the centre item,
 * the pedestal items (any order) and the essences become {@code result}, while every rune in {@code runes} stands chosen
 * on a rune stele around the altar. Runes take no items, only essence: each adds {@link #runeEssence(ComplexRune)} to
 * what the altar draws ({@link #totalEssences()}).
 *
 * @param keepComponents the result keeps the centre item's components (enchantments, damage, name)
 */
public record InfusionRecipe(Ingredient center, List<Ingredient> pedestals, EssenceMix essences, List<ComplexRune> runes,
                             ItemStackTemplate result, boolean keepComponents) {
    /** mB of a rune's essence by its chapter (I, II, III). */
    private static final int[] RUNE_MB = {16, 24, 32};

    public static final Codec<InfusionRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("center").forGetter(InfusionRecipe::center),
            Ingredient.CODEC.listOf().optionalFieldOf("pedestals", List.of()).forGetter(InfusionRecipe::pedestals),
            EssenceMix.CODEC.optionalFieldOf("essences", EssenceMix.EMPTY).forGetter(InfusionRecipe::essences),
            ComplexRunes.CODEC.listOf().optionalFieldOf("runes", List.of()).forGetter(InfusionRecipe::runes),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(InfusionRecipe::result),
            Codec.BOOL.optionalFieldOf("keep_components", false).forGetter(InfusionRecipe::keepComponents)
    ).apply(instance, InfusionRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, InfusionRecipe::center,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), InfusionRecipe::pedestals,
            EssenceMix.STREAM_CODEC.cast(), InfusionRecipe::essences,
            ComplexRunes.STREAM_CODEC.apply(ByteBufCodecs.list()).cast(), InfusionRecipe::runes,
            ItemStackTemplate.STREAM_CODEC, InfusionRecipe::result,
            ByteBufCodecs.BOOL, InfusionRecipe::keepComponents,
            InfusionRecipe::new);

    /** What a rune costs at the altar: the compound of its two primals (or both primals), by its chapter. */
    public static EssenceMix runeEssence(ComplexRune rune) {
        return EssenceMix.ofRune(rune, RUNE_MB[ResearchBook.chapter(rune) - 1]);
    }

    /** The recipe's essences plus its runes'. */
    public EssenceMix totalEssences() {
        EssenceMix total = essences;
        for (ComplexRune rune : runes) {
            total = total.plus(runeEssence(rune));
        }
        return total;
    }

    public ItemStack resultFor(ItemStack center) {
        if (keepComponents) {
            ItemStack out = center.transmuteCopy(result.item().value(), result.count());
            out.applyComponents(result.components());
            return out;
        }
        return result.create();
    }
}
