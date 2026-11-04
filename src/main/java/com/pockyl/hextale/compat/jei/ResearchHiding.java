package com.pockyl.hextale.compat.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.multiblock.Multiblock;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * JEI shows how a thing is made (its recipe, its multiblock, its info page) only once the player has researched it
 * (owner, 2026-11-03: "the craft or the structure must not show until we research it"). The items themselves stay in
 * the list; their recipes come back as soon as the research is learned. The Lab Bench experiments stay visible: they
 * are how a topic is researched.
 */
final class ResearchHiding {
    private static @Nullable IJeiRuntime runtime;
    private static boolean listening;
    private static int seenVersion = -1;
    /** What is hidden now, per recipe type, so it can be shown again. */
    private static final Map<IRecipeType<?>, java.util.Set<Object>> HIDDEN = new HashMap<>();

    private ResearchHiding() {
    }

    static void start(IJeiRuntime jei) {
        runtime = jei;
        seenVersion = -1;
        HIDDEN.clear();
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener(ResearchHiding::onClientTick);
        }
    }

    static void stop() {
        runtime = null;
        HIDDEN.clear();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (runtime != null && seenVersion != ClientKnowledge.version()) {
            seenVersion = ClientKnowledge.version();
            refresh(runtime, ClientKnowledge.get());
        }
    }

    private static void refresh(IJeiRuntime jei, RuneKnowledge knowledge) {
        Function<@Nullable ResearchTopic, Boolean> locked = topic -> topic != null && !knowledge.researched(topic);
        apply(jei, HexCraftingCategory.TYPE, recipe -> locked.apply(of(recipe.recipe().result().create())));
        apply(jei, AlloyingCategory.TYPE, recipe -> locked.apply(of(recipe.recipe().result().create())));
        apply(jei, TemperingCategory.TYPE, recipe -> locked.apply(of(recipe.recipe().result().create())));
        apply(jei, InfusionCategory.TYPE, recipe -> locked.apply(of(recipe.recipe().result().create())));
        apply(jei, CrucibleCategory.TYPE, recipe -> locked.apply(recipe.recipe().topic()));
        apply(jei, BrewingCategory.TYPE, recipe -> locked.apply(ResearchTopics.forBrew(recipe.recipe().brew())));
        apply(jei, MultiblockCategory.TYPE, (Multiblock multiblock) -> locked.apply(multiblock.topic()));
        for (IRecipeType<StepCategory.Step> type : List.of(HextechWorkingCategory.TYPE, DeviceSteps.STILL, DeviceSteps.TANK,
                DeviceSteps.SOAKING, DeviceSteps.WRITING, DeviceSteps.SPLASH)) {
            apply(jei, type, step -> step.result().stacks().stream().allMatch(stack -> locked.apply(of(stack))));
        }
        apply(jei, NotesCategory.TYPE, page -> !page.items().isEmpty() && page.items().stream().allMatch(stack -> locked.apply(of(stack))));
        // The mod's own recipes in the crafting table and the stonecutter (decor of researched materials).
        apply(jei, RecipeTypes.CRAFTING, holder -> own(holder) && outputsLocked(jei, RecipeTypes.CRAFTING, holder, locked));
        apply(jei, RecipeTypes.STONECUTTING, holder -> own(holder) && outputsLocked(jei, RecipeTypes.STONECUTTING, holder, locked));
    }

    /** The topic guarding a stack: a vial's brew, else the item's own. */
    private static @Nullable ResearchTopic of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Brew brew = stack.get(ModDataComponents.BREW.get());
        if (brew != null) {
            return ResearchTopics.forBrew(brew);
        }
        return ResearchTopics.of(stack.getItem());
    }

    private static boolean own(RecipeHolder<?> holder) {
        return Hextale.MOD_ID.equals(holder.id().identifier().getNamespace());
    }

    private static <T> boolean outputsLocked(IJeiRuntime jei, IRecipeType<T> type, T recipe,
                                             Function<@Nullable ResearchTopic, Boolean> locked) {
        IRecipeCategory<T> category = jei.getRecipeManager().getRecipeCategory(type);
        var layout = jei.getRecipeManager().createRecipeLayoutDrawable(category, recipe,
                jei.getJeiHelpers().getFocusFactory().getEmptyFocusGroup());
        if (layout.isEmpty()) {
            return false;
        }
        List<ItemStack> outputs = layout.get().getRecipeSlotsView().getSlotViews(RecipeIngredientRole.OUTPUT).stream()
                .flatMap(IRecipeSlotView::getItemStacks).toList();
        return !outputs.isEmpty() && outputs.stream().allMatch(stack -> locked.apply(of(stack)));
    }

    @SuppressWarnings("unchecked")
    private static <T> void apply(IJeiRuntime jei, IRecipeType<T> type, java.util.function.Predicate<T> hide) {
        Stream<T> all;
        try {
            all = jei.getRecipeManager().createRecipeLookup(type).includeHidden().get();
        } catch (RuntimeException unknownType) {
            return;
        }
        java.util.Set<T> nowHidden = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (T recipe : (Iterable<T>) all::iterator) {
            if (hide.test(recipe)) {
                nowHidden.add(recipe);
            }
        }
        java.util.Set<T> before = (java.util.Set<T>) (java.util.Set<?>) HIDDEN.getOrDefault(type, java.util.Set.of());
        List<T> show = before.stream().filter(recipe -> !nowHidden.contains(recipe)).toList();
        List<T> hideNew = nowHidden.stream().filter(recipe -> !before.contains(recipe)).toList();
        if (!show.isEmpty()) {
            jei.getRecipeManager().unhideRecipes(type, show);
        }
        if (!hideNew.isEmpty()) {
            jei.getRecipeManager().hideRecipes(type, hideNew);
        }
        HIDDEN.put(type, (java.util.Set<Object>) (java.util.Set<?>) nowHidden);
    }
}
