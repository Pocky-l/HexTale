package com.pockyl.hextale.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.Comparator;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * A Crucible transmutation, recipe type {@code hextale:crucible}: a {@code catalyst} item
 * thrown into a crucible that holds at least the recipe's {@code essences} takes them out of the water and rises as
 * {@code result}. A catalyst the water cannot pay for is just another item: boiling water dissolves it into its own
 * essences. A result that must be researched first (the Magic Tree Sprout) comes only for the researcher who threw or put
 * the catalyst in and has researched it; for anyone else the catalyst is just another item. A recipe may name its
 * {@code research} itself: transmutations (sand into red sand and back) wait for a transmutation topic, while the plain
 * result stays free everywhere else.
 */
public record CrucibleRecipe(Ingredient catalyst, EssenceMix essences, ItemStackTemplate result, Optional<ResearchTopic> research) {
    public static final MapCodec<CrucibleRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("catalyst").forGetter(CrucibleRecipe::catalyst),
            EssenceMix.CODEC.fieldOf("essences").forGetter(CrucibleRecipe::essences),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrucibleRecipe::result),
            ResearchTopics.CODEC.optionalFieldOf("research").forGetter(CrucibleRecipe::research)
    ).apply(instance, CrucibleRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CrucibleRecipe::catalyst,
            EssenceMix.STREAM_CODEC, CrucibleRecipe::essences,
            ItemStackTemplate.STREAM_CODEC, CrucibleRecipe::result,
            ByteBufCodecs.optional(ResearchTopics.STREAM_CODEC), CrucibleRecipe::research,
            CrucibleRecipe::new);

    /**
     * The research a recipe for this item waits for: the water could pay for it, but the researcher has not researched
     * its result (null when no such recipe).
     */
    public static @Nullable ResearchTopic lockedFor(ItemStack stack, EssenceMix essence, @Nullable Player researcher) {
        return lockedFor(stack, essence, researcher, false);
    }

    /** {@link #lockedFor(ItemStack, EssenceMix, Player)} on either side (the client knows the recipes too). */
    public static @Nullable ResearchTopic lockedFor(ItemStack stack, EssenceMix essence, @Nullable Player researcher, boolean clientSide) {
        return ALL.get(clientSide).values().stream()
                .filter(recipe -> recipe.catalyst().test(stack) && essence.holds(recipe.essences()) && !recipe.mayMake(researcher))
                .map(CrucibleRecipe::topic)
                .findFirst().orElse(null);
    }

    /** Whether some transmutation starts with this catalyst at all (whatever the water holds). */
    public static boolean anyFor(ItemStack stack, boolean clientSide) {
        return ALL.get(clientSide).values().stream().anyMatch(recipe -> recipe.catalyst().test(stack));
    }

    /** The research this transmutation waits for: the one the recipe names, else the result's own (null: free). */
    public @Nullable ResearchTopic topic() {
        return research.orElseGet(() -> ResearchTopics.of(result.item().value()));
    }

    /** Whether this researcher may transmute into the result: nothing guards it, or they have researched it. */
    public boolean mayMake(@Nullable Player researcher) {
        ResearchTopic topic = topic();
        return topic == null || researcher != null && Research.of(researcher).researched(topic);
    }

    /** Every transmutation, server and client. */
    public static final DataRecipes<CrucibleRecipe> ALL = new DataRecipes<>("crucible", MAP_CODEC, STREAM_CODEC, recipe -> recipe.result().item().value());

    /**
     * The transmutation this item starts in water holding {@code essence}: of the recipes for it that the water can pay,
     * the one asking the most (so a richer brew is never wasted on a lesser result). Recipes whose result the
     * {@code researcher} (who threw or put the item in, null when nobody did) has not researched are left out.
     */
    public static @Nullable CrucibleRecipe find(ItemStack stack, EssenceMix essence, @Nullable Player researcher) {
        return find(stack, essence, researcher, false);
    }

    /** {@link #find(ItemStack, EssenceMix, Player)} on either side (the client knows the recipes too). */
    public static @Nullable CrucibleRecipe find(ItemStack stack, EssenceMix essence, @Nullable Player researcher, boolean clientSide) {
        return ALL.get(clientSide).values().stream()
                .filter(recipe -> recipe.catalyst().test(stack) && essence.holds(recipe.essences()) && recipe.mayMake(researcher))
                .max(Comparator.comparingInt(recipe -> recipe.essences().total()))
                .orElse(null);
    }
}
