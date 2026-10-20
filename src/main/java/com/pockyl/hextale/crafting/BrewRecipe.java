package com.pockyl.hextale.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.Comparator;

import org.jspecify.annotations.Nullable;

/**
 * A Crucible brew, read from {@code data/<namespace>/hextale_brew/<name>.json}: a {@code catalyst} that sinks into water
 * boiling over at least {@code heat} (fire 2, soul fire 3, lava 4, a Chem Burner 5) and holding the recipe's
 * {@code essences} takes them and turns the water into {@code doses} doses of the {@code brew}. Brewing must be
 * researched (the Chem Vials topic) by whoever threw or put the catalyst in; for anyone else it is just another item.
 */
public record BrewRecipe(Ingredient catalyst, EssenceMix essences, Brew brew, int heat, int doses) {
    public static final Codec<BrewRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.fieldOf("catalyst").forGetter(BrewRecipe::catalyst),
            EssenceMix.CODEC.fieldOf("essences").forGetter(BrewRecipe::essences),
            Brew.CODEC.fieldOf("brew").forGetter(BrewRecipe::brew),
            Codec.intRange(1, 5).optionalFieldOf("heat", 2).forGetter(BrewRecipe::heat),
            Codec.intRange(1, 16).optionalFieldOf("doses", 3).forGetter(BrewRecipe::doses)
    ).apply(instance, BrewRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BrewRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, BrewRecipe::catalyst,
            EssenceMix.STREAM_CODEC, BrewRecipe::essences,
            Brew.STREAM_CODEC.cast(), BrewRecipe::brew,
            ByteBufCodecs.VAR_INT, BrewRecipe::heat,
            ByteBufCodecs.VAR_INT, BrewRecipe::doses,
            BrewRecipe::new);

    /** Every brew, server and client. */
    public static final DataRecipes<BrewRecipe> ALL = new DataRecipes<>("hextale_brew", CODEC);

    /** Heat of a Chem Burner, the hottest there is. */
    public static final int BURNER_HEAT = 5;

    /** Whether this researcher may brew: they have researched the Chem Vials. */
    public static boolean mayBrew(@Nullable Player researcher) {
        return researcher != null && Research.of(researcher).researched(ResearchTopics.CHEM_VIAL);
    }

    /**
     * The brew this item starts in water holding {@code essence} over {@code heat}, when the crucible holds no brew or
     * this same {@code current} one: of the recipes the water can pay, the one asking the most. Null when none.
     */
    public static @Nullable BrewRecipe find(ItemStack stack, EssenceMix essence, int heat, @Nullable Brew current) {
        return ALL.get(false).values().stream()
                .filter(recipe -> recipe.catalyst().test(stack) && essence.holds(recipe.essences()) && heat >= recipe.heat()
                        && (current == null || current == recipe.brew()))
                .max(Comparator.comparingInt(recipe -> recipe.essences().total()))
                .orElse(null);
    }

    /** Whether some brew starts with this catalyst at all (for the crucible's hints). */
    public static @Nullable BrewRecipe anyFor(ItemStack stack, boolean clientSide) {
        return ALL.get(clientSide).values().stream().filter(recipe -> recipe.catalyst().test(stack)).findFirst().orElse(null);
    }
}
