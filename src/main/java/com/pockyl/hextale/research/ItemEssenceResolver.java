package com.pockyl.hextale.research;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextMap;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModDataMaps;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.rune.PrimalRing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Gives every item its essences, in three passes:
 * <ol>
 *     <li>items with a value in the {@code hextale:essences} data map keep it (hand-tuned materials);</li>
 *     <li>raw items no recipe makes get essences from the words of their name ({@link ItemEssenceWords}: logs carry wood
 *     and sap, ice ice and cold...);</li>
 *     <li>everything else is made of its ingredients: a recipe passes on the sum of its ingredients' essences, shared
 *     among the items it makes and softened (a block of nine ingots is stronger than one, not nine times); cooking adds
 *     a little fire; the words of the result's own name add what it IS (a sword is also a weapon).</li>
 * </ol>
 * Whatever is still left (circular recipes) falls back to its name, and at last to its kind. An item keeps its three
 * strongest essences, each 1-32: few enough to tell apart at a glance.
 */
public final class ItemEssenceResolver {
    /** Up to three essences. */
    private static final int MAX_ELEMENTS = 3;
    private static final int MAX_POINTS = 32;
    private static final int PASSES = 12;
    /** Totals up to this stay as they are; above it they grow more slowly. */
    private static final float SOFT_FROM = 10;
    private static final double SOFTENING = 0.55;
    private ItemEssenceResolver() {
    }

    /** Resolves the elements of every registered item. */
    public static Map<Item, ItemEssences> resolve(RecipeManager recipes, RegistryAccess registries) {
        long start = System.nanoTime();
        Map<Item, ItemEssences> out = new HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            @SuppressWarnings("deprecation")
            Holder<Item> holder = item.builtInRegistryHolder();
            ItemEssences base = holder.getData(ModDataMaps.ESSENCES);
            if (base != null) {
                out.put(item, base);
            }
        }

        // What each item can be made from.
        // Recipes only keep their result in their displays (what the recipe book shows).
        ContextMap context = ContextMap.builder().set(SlotDisplayContext.REGISTRIES, registries).buildAndValidate(SlotDisplayContext.CONTEXT);
        Map<Item, List<Made>> makers = new HashMap<>();
        for (RecipeHolder<?> holder : recipes.getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.placementInfo().isImpossibleToPlace() || recipe.display().isEmpty()) {
                continue;
            }
            ItemStack result;
            try {
                RecipeDisplay display = recipe.display().getFirst();
                result = display.result().resolveForFirstStack(context);
            } catch (RuntimeException e) {
                continue;
            }
            if (result.isEmpty()) {
                continue;
            }
            List<Ingredient> ingredients = recipe.placementInfo().ingredients().stream().filter(i -> !i.isEmpty()).toList();
            if (!ingredients.isEmpty()) {
                makers.computeIfAbsent(result.getItem(), k -> new ArrayList<>())
                        .add(new Made(ingredients, result.getCount(), recipe instanceof AbstractCookingRecipe));
            }
        }

        // Raw materials: named.
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR && !out.containsKey(item) && !makers.containsKey(item)) {
                ItemEssences named = byName(item);
                if (named != null) {
                    out.put(item, named);
                }
            }
        }

        // Made things: from their ingredients, until nothing changes.
        for (int pass = 0; pass < PASSES; pass++) {
            Map<Item, ItemEssences> found = new HashMap<>();
            for (Map.Entry<Item, List<Made>> entry : makers.entrySet()) {
                if (out.containsKey(entry.getKey())) {
                    continue;
                }
                ItemEssences best = null;
                for (Made made : entry.getValue()) {
                    ItemEssences candidate = fromIngredients(made, out, entry.getKey());
                    if (candidate != null && (best == null || total(candidate) < total(best))) {
                        best = candidate;
                    }
                }
                if (best != null) {
                    found.put(entry.getKey(), best);
                }
            }
            if (found.isEmpty()) {
                break;
            }
            out.putAll(found);
        }

        // Whatever is left: its name, then its kind.
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR || out.containsKey(item)) {
                continue;
            }
            ItemEssences named = byName(item);
            out.put(item, named != null ? named : byKind(item));
        }
        Hextale.LOGGER.debug("Resolved the elements of {} items in {} ms", out.size(), (System.nanoTime() - start) / 1_000_000);
        return out;
    }

    private record Made(List<Ingredient> ingredients, int count, boolean cooked) {
    }

    private static ItemEssences fromIngredients(Made made, Map<Item, ItemEssences> known, Item result) {
        Map<Essence, Float> sum = new EnumMap<>(Essence.class);
        int tier = 1;
        for (Ingredient ingredient : made.ingredients()) {
            ItemEssences cheapest = null;
            for (Holder<Item> option : ingredient.items().toList()) {
                ItemEssences elements = option.value() == result ? null : known.get(option.value());
                if (elements != null && (cheapest == null || total(elements) < total(cheapest))) {
                    cheapest = elements;
                }
            }
            if (cheapest == null) {
                return null;
            }
            tier = Math.max(tier, cheapest.tier());
            cheapest.essences().forEach((element, points) -> sum.merge(element, (float) points, Float::sum));
        }
        if (made.cooked()) {
            sum.merge(Essence.FIRE, 2.0F, Float::sum);
        }
        // Soften big totals, keep the proportions: nine ingots make a stronger block than one ingot, not nine times.
        float total = 0;
        for (float points : sum.values()) {
            total += points / made.count();
        }
        float scale = total <= SOFT_FROM ? 1 : SOFT_FROM * (float) Math.pow(total / SOFT_FROM, SOFTENING) / total;
        Map<Essence, Integer> soft = new EnumMap<>(Essence.class);
        sum.forEach((element, points) -> soft.put(element,
                Math.max(1, Math.min(MAX_POINTS, Math.round(points / made.count() * scale)))));
        // What the thing is, by its name, on top of what it is made of.
        words(BuiltInRegistries.ITEM.getKey(result).getPath()).forEach((essence, points) ->
                soft.merge(essence, points, (a, b) -> Math.min(MAX_POINTS, a + b)));
        return strongest(soft, Math.max(tier, rarityTier(result)));
    }

    /** Essences from the words of the item's id, or null when no word is known. */
    static ItemEssences byName(Item item) {
        Map<Essence, Integer> sum = words(BuiltInRegistries.ITEM.getKey(item).getPath());
        if (sum.isEmpty()) {
            return null;
        }
        int total = sum.values().stream().mapToInt(Integer::intValue).sum();
        return strongest(sum, Math.max(rarityTier(item), total >= 16 ? 3 : total >= 9 ? 2 : 1));
    }

    /** The essences the words of an id bring, summed (each word once). */
    static Map<Essence, Integer> words(String path) {
        Map<Essence, Integer> sum = new EnumMap<>(Essence.class);
        Set<String> used = new HashSet<>();
        for (String token : path.split("_")) {
            List<String> matches = new ArrayList<>();
            for (String key : ItemEssenceWords.WORDS.keySet()) {
                if (token.equals(key) || token.equals(key + "s") || key.length() >= 4 && token.contains(key)) {
                    matches.add(key);
                }
            }
            // The longest word wins over the words inside it: "redstone" is no "stone", "glowstone" no "glow".
            matches.removeIf(key -> matches.stream().anyMatch(other -> other.length() > key.length() && other.contains(key)));
            for (String key : matches) {
                if (used.add(key)) {
                    Object[] pairs = ItemEssenceWords.WORDS.get(key);
                    for (int i = 0; i < pairs.length; i += 2) {
                        sum.merge((Essence) pairs[i], (Integer) pairs[i + 1], Integer::sum);
                    }
                }
            }
        }
        sum.replaceAll((essence, points) -> Math.min(MAX_POINTS, points));
        return sum;
    }

    private static ItemEssences byKind(Item item) {
        ItemStack stack = new ItemStack(item);
        if (stack.has(DataComponents.FOOD)) {
            return ItemEssences.of(rarityTier(item), Essence.LIFE, 2, Essence.PLANT, 1);
        }
        if (item instanceof BlockItem) {
            return ItemEssences.of(rarityTier(item), Essence.EARTH, 2);
        }
        return ItemEssences.of(rarityTier(item), Essence.METAL, 2);
    }

    @SuppressWarnings("deprecation")
    private static int rarityTier(Item item) {
        Rarity rarity = new ItemStack(item).getRarity();
        return rarity == Rarity.EPIC || rarity == Rarity.RARE ? 3 : rarity == Rarity.UNCOMMON ? 2 : 1;
    }

    /** The strongest essences, strongest first (kind breaks ties). */
    private static ItemEssences strongest(Map<Essence, Integer> all, int tier) {
        List<Essence> order = new ArrayList<>(all.keySet());
        order.sort((a, b) -> all.get(b) - all.get(a) != 0 ? all.get(b) - all.get(a) : ring(a) - ring(b));
        Map<Essence, Integer> kept = new LinkedHashMap<>();
        for (Essence element : order.subList(0, Math.min(MAX_ELEMENTS, order.size()))) {
            kept.put(element, all.get(element));
        }
        return new ItemEssences(kept, Math.max(1, Math.min(3, tier)));
    }

    /** Ties break by kind: primals first in their circle order, then compounds in enum order. */
    private static int ring(Essence essence) {
        int primal = PrimalRing.index(essence);
        return primal >= 0 ? primal : 100 + essence.ordinal();
    }

    private static int total(ItemEssences elements) {
        return elements.essences().values().stream().mapToInt(Integer::intValue).sum();
    }
}
