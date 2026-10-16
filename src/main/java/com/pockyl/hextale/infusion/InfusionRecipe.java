package com.pockyl.hextale.infusion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * One thing the Hextech Altar can do, read from {@code data/<namespace>/hextale_infusion/<name>.json}.
 * <ul>
 *     <li>{@code infuse}: the centre item, the pedestal items (any order) and the essences become {@code result};
 *     {@code runes} must be chosen on rune steles around the altar.</li>
 *     <li>{@code engrave}: what engraving {@code rune} into a hextech tool costs (pedestal items + essences).</li>
 *     <li>{@code erase}: what grinding every rune off a hextech tool costs.</li>
 * </ul>
 *
 * @param keepComponents the result keeps the centre item's components (enchantments, damage, name)
 */
public record InfusionRecipe(Kind kind, Optional<Ingredient> center, List<Ingredient> pedestals, EssenceMix essences,
                             List<ComplexRune> runes, Optional<ItemStackTemplate> result, boolean keepComponents) {
    private static final Codec<InfusionRecipe> RAW_CODEC = RecordCodecBuilder.<InfusionRecipe>create(instance -> instance.group(
            Kind.CODEC.optionalFieldOf("kind", Kind.INFUSE).forGetter(InfusionRecipe::kind),
            Ingredient.CODEC.optionalFieldOf("center").forGetter(InfusionRecipe::center),
            Ingredient.CODEC.listOf().optionalFieldOf("pedestals", List.of()).forGetter(InfusionRecipe::pedestals),
            EssenceMix.CODEC.optionalFieldOf("essences", EssenceMix.EMPTY).forGetter(InfusionRecipe::essences),
            ComplexRunes.CODEC.listOf().optionalFieldOf("runes", List.of()).forGetter(InfusionRecipe::runes),
            ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(InfusionRecipe::result),
            Codec.BOOL.optionalFieldOf("keep_components", false).forGetter(InfusionRecipe::keepComponents)
    ).apply(instance, InfusionRecipe::new));
    public static final Codec<InfusionRecipe> CODEC = RAW_CODEC.validate(InfusionRecipe::validate);

    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionRecipe> STREAM_CODEC = StreamCodec.composite(
            Kind.STREAM_CODEC, InfusionRecipe::kind,
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, InfusionRecipe::center,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), InfusionRecipe::pedestals,
            EssenceMix.STREAM_CODEC.cast(), InfusionRecipe::essences,
            ComplexRunes.STREAM_CODEC.apply(ByteBufCodecs.list()).cast(), InfusionRecipe::runes,
            ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs::optional), InfusionRecipe::result,
            ByteBufCodecs.BOOL, InfusionRecipe::keepComponents,
            InfusionRecipe::new);

    private static DataResult<InfusionRecipe> validate(InfusionRecipe recipe) {
        return switch (recipe.kind) {
            case INFUSE -> recipe.center.isPresent() && recipe.result.isPresent()
                    ? DataResult.success(recipe)
                    : DataResult.error(() -> "an infusion needs a center and a result");
            case ENGRAVE -> recipe.runes.size() == 1
                    ? DataResult.success(recipe)
                    : DataResult.error(() -> "an engraving names exactly one rune");
            case ERASE -> DataResult.success(recipe);
        };
    }

    /** The rune an {@code engrave} entry is for. */
    public ComplexRune rune() {
        return runes.getFirst();
    }

    public ItemStack resultFor(ItemStack center) {
        ItemStackTemplate template = result.orElseThrow();
        if (keepComponents) {
            ItemStack out = center.transmuteCopy(template.item().value(), template.count());
            out.applyComponents(template.components());
            return out;
        }
        return template.create();
    }

    public enum Kind implements StringRepresentable {
        INFUSE, ENGRAVE, ERASE;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        public static final StreamCodec<ByteBuf, Kind> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Kind::ordinal);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
