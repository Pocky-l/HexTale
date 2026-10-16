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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.pockyl.hextale.rune.Essence.AIR;
import static com.pockyl.hextale.rune.Essence.EARTH;
import static com.pockyl.hextale.rune.Essence.FIRE;
import static com.pockyl.hextale.rune.Essence.LIFE;
import static com.pockyl.hextale.rune.Essence.LIGHT;
import static com.pockyl.hextale.rune.Essence.METAL;
import static com.pockyl.hextale.rune.Essence.SHADOW;
import static com.pockyl.hextale.rune.Essence.SPARK;
import static com.pockyl.hextale.rune.Essence.WATER;

/**
 * Gives every item its elements, in three passes:
 * <ol>
 *     <li>items with a value in the {@code hextale:elements} data map keep it (the base materials);</li>
 *     <li>raw items no recipe makes get elements from the words of their name (logs carry life and earth, ice water
 *     and air...);</li>
 *     <li>everything else is made of its ingredients: a recipe passes on the sum of its ingredients' elements, shared
 *     among the items it makes and softened (a block of nine ingots is stronger than one, not nine times); cooking adds
 *     a little fire.</li>
 * </ol>
 * Whatever is still left (circular recipes) falls back to its name, and at last to its kind. An item keeps at most
 * three elements, each 1-9.
 */
public final class ItemEssenceResolver {
    private static final int MAX_ELEMENTS = 3;
    private static final int MAX_POINTS = 9;
    private static final int PASSES = 12;
    /** Totals up to this stay as they are; above it they grow more slowly. */
    private static final float SOFT_FROM = 4;
    private static final double SOFTENING = 0.55;
    private static final Map<String, Object[]> WORDS = new LinkedHashMap<>();

    static {
        // Fire
        word("blaze", FIRE, 3);
        word("magma", FIRE, 2, EARTH, 1);
        word("fire", FIRE, 2);
        word("lava", FIRE, 3, EARTH, 1);
        word("campfire", FIRE, 2, LIFE, 1);
        word("torch", FIRE, 1, LIGHT, 1);
        word("netherrack", FIRE, 1, EARTH, 1);
        word("nether", FIRE, 1);
        word("crimson", FIRE, 1, LIFE, 1);
        word("warped", SHADOW, 1, LIFE, 1);
        word("basalt", FIRE, 1, EARTH, 2);
        word("blackstone", EARTH, 2, SHADOW, 1);
        word("quartz", LIGHT, 1, EARTH, 1);
        word("ghast", SHADOW, 1, AIR, 2);
        word("cooked", FIRE, 1);
        word("charcoal", FIRE, 2, LIFE, 1);
        // Water
        word("water", WATER, 2);
        word("ice", WATER, 2, AIR, 1);
        word("snow", WATER, 1, AIR, 1);
        word("snowball", WATER, 1, AIR, 1);
        word("prismarine", WATER, 2, LIGHT, 1);
        word("sea", WATER, 2);
        word("coral", WATER, 2, LIFE, 1);
        word("kelp", WATER, 2, LIFE, 1);
        word("cod", WATER, 2, LIFE, 1);
        word("salmon", WATER, 2, LIFE, 1);
        word("tropical", WATER, 2, LIFE, 1);
        word("pufferfish", WATER, 2, SHADOW, 1);
        word("fish", WATER, 2, LIFE, 1);
        word("sponge", WATER, 2, LIFE, 1);
        word("clay", EARTH, 1, WATER, 1);
        word("mud", EARTH, 1, WATER, 1);
        word("squid", WATER, 1, SHADOW, 1);
        word("turtle", WATER, 2, EARTH, 1);
        word("scute", WATER, 1, EARTH, 1);
        word("nautilus", WATER, 2, EARTH, 1);
        word("trident", WATER, 3, METAL, 1);
        word("axolotl", WATER, 1, LIFE, 2);
        word("frog", WATER, 1, LIFE, 1);
        word("tadpole", WATER, 1, LIFE, 1);
        word("dripleaf", WATER, 1, LIFE, 1);
        word("lily", WATER, 1, LIFE, 1);
        word("bucket", METAL, 2);
        // Earth
        word("stone", EARTH, 2);
        word("cobblestone", EARTH, 2);
        word("deepslate", EARTH, 2, SHADOW, 1);
        word("tuff", EARTH, 2);
        word("calcite", EARTH, 2, LIGHT, 1);
        word("granite", EARTH, 2, FIRE, 1);
        word("diorite", EARTH, 2, LIGHT, 1);
        word("andesite", EARTH, 2);
        word("dirt", EARTH, 1, LIFE, 1);
        word("grass", LIFE, 1, EARTH, 1);
        word("gravel", EARTH, 2);
        word("sand", EARTH, 1, AIR, 1);
        word("sandstone", EARTH, 2, AIR, 1);
        word("brick", EARTH, 2, FIRE, 1);
        word("terracotta", EARTH, 2, FIRE, 1);
        word("concrete", EARTH, 2, WATER, 1);
        word("powder", EARTH, 1, AIR, 1);
        word("obsidian", EARTH, 2, FIRE, 1, SHADOW, 1);
        word("ore", EARTH, 2);
        word("raw", METAL, 1, EARTH, 1);
        word("diamond", EARTH, 2, LIGHT, 2);
        word("emerald", LIGHT, 2, LIFE, 1);
        word("amethyst", LIGHT, 1, EARTH, 1, SPARK, 1);
        word("lapis", WATER, 1, LIGHT, 1);
        word("flint", EARTH, 1, FIRE, 1);
        word("bedrock", EARTH, 4);
        word("dripstone", EARTH, 2, WATER, 1);
        word("pointed", EARTH, 1);
        word("ancient", EARTH, 2, FIRE, 2);
        word("debris", EARTH, 2, FIRE, 2);
        word("sherd", EARTH, 1, SHADOW, 1);
        // Air
        word("feather", AIR, 2);
        word("phantom", AIR, 3, SHADOW, 1);
        word("string", AIR, 1, LIFE, 1);
        word("cobweb", AIR, 1, SHADOW, 1);
        word("wool", LIFE, 1, AIR, 1);
        word("carpet", LIFE, 1, AIR, 1);
        word("bed", LIFE, 1, AIR, 1);
        word("wind", AIR, 3);
        word("breeze", AIR, 3);
        word("bamboo", LIFE, 1, AIR, 1);
        word("glass", LIGHT, 1, EARTH, 1);
        word("bell", METAL, 2, AIR, 1);
        word("horn", AIR, 2, LIFE, 1);
        word("disc", AIR, 2, SPARK, 1);
        word("firework", FIRE, 1, AIR, 2);
        word("elytra", AIR, 4, SHADOW, 1);
        word("cloud", AIR, 2);
        // Light
        word("glow", LIGHT, 2);
        word("glowstone", LIGHT, 3);
        word("shroomlight", LIGHT, 2, LIFE, 1);
        word("froglight", LIGHT, 2, WATER, 1);
        word("lantern", LIGHT, 2, METAL, 1);
        word("beacon", LIGHT, 4, METAL, 1);
        word("gold", METAL, 2, LIGHT, 1);
        word("golden", METAL, 2, LIGHT, 1);
        word("candle", FIRE, 1, LIGHT, 1);
        word("rod", LIGHT, 1);
        word("light", LIGHT, 1);
        word("white", LIGHT, 1);
        word("sunflower", LIGHT, 2, LIFE, 1);
        // Shadow
        word("ink", SHADOW, 1, WATER, 1);
        word("sculk", SHADOW, 2);
        word("echo", SHADOW, 4);
        word("spider", SHADOW, 1, LIFE, 1);
        word("rotten", SHADOW, 1, LIFE, 1);
        word("bone", LIFE, 1, SHADOW, 1);
        word("skull", SHADOW, 2, LIFE, 1);
        word("head", SHADOW, 1, LIFE, 1);
        word("soul", SHADOW, 2);
        word("black", SHADOW, 1);
        word("dark", SHADOW, 1);
        word("wither", SHADOW, 3);
        word("ender", SHADOW, 2, SPARK, 1);
        word("end", SHADOW, 1, AIR, 1);
        word("chorus", SHADOW, 1, LIFE, 1, AIR, 1);
        word("purpur", SHADOW, 1, EARTH, 1);
        word("shulker", SHADOW, 2, METAL, 1);
        word("dragon", FIRE, 2, SHADOW, 2, AIR, 1);
        word("crying", SHADOW, 1, LIGHT, 1);
        word("gunpowder", FIRE, 2, SPARK, 1);
        // Life
        word("log", LIFE, 2, EARTH, 1);
        word("wood", LIFE, 2, EARTH, 1);
        word("stem", LIFE, 2, SHADOW, 1);
        word("hyphae", LIFE, 2, SHADOW, 1);
        word("planks", LIFE, 1, EARTH, 1);
        word("sapling", LIFE, 2);
        word("propagule", LIFE, 2, WATER, 1);
        word("leaves", LIFE, 2, AIR, 1);
        word("flower", LIFE, 2, LIGHT, 1);
        word("tulip", LIFE, 2, LIGHT, 1);
        word("orchid", LIFE, 2, LIGHT, 1);
        word("allium", LIFE, 2, LIGHT, 1);
        word("poppy", LIFE, 2, FIRE, 1);
        word("dandelion", LIFE, 2, LIGHT, 1);
        word("cornflower", LIFE, 2, WATER, 1);
        word("daisy", LIFE, 2, LIGHT, 1);
        word("bluet", LIFE, 2, WATER, 1);
        word("lilac", LIFE, 2, AIR, 1);
        word("peony", LIFE, 2, LIGHT, 1);
        word("rose", LIFE, 2, FIRE, 1);
        word("torchflower", LIFE, 2, FIRE, 1);
        word("pitcher", LIFE, 2, WATER, 1);
        word("seeds", LIFE, 1, EARTH, 1);
        word("wheat", LIFE, 2);
        word("carrot", LIFE, 2);
        word("potato", LIFE, 2, EARTH, 1);
        word("beetroot", LIFE, 2);
        word("melon", LIFE, 2, WATER, 1);
        word("pumpkin", LIFE, 2);
        word("apple", LIFE, 2);
        word("berries", LIFE, 2);
        word("beef", LIFE, 2);
        word("porkchop", LIFE, 2);
        word("chicken", LIFE, 2, AIR, 1);
        word("mutton", LIFE, 2);
        word("rabbit", LIFE, 2, AIR, 1);
        word("egg", LIFE, 2);
        word("leather", LIFE, 1, EARTH, 1);
        word("hide", LIFE, 1, EARTH, 1);
        word("honey", LIFE, 3);
        word("honeycomb", LIFE, 2, LIGHT, 1);
        word("slime", WATER, 1, LIFE, 2);
        word("mushroom", LIFE, 1, SHADOW, 1);
        word("fungus", LIFE, 1, SHADOW, 1);
        word("vine", LIFE, 1);
        word("vines", LIFE, 1);
        word("moss", LIFE, 2, EARTH, 1);
        word("azalea", LIFE, 2);
        word("cactus", LIFE, 1, EARTH, 1);
        word("cane", LIFE, 1, WATER, 1);
        word("sugar", LIFE, 1, AIR, 1);
        word("spawn", LIFE, 3);
        word("totem", LIFE, 4, LIGHT, 1);
        word("cocoa", LIFE, 1, FIRE, 1);
        word("milk", LIFE, 2, WATER, 1);
        word("potion", WATER, 1, LIFE, 1, LIGHT, 1);
        word("dye", LIGHT, 1, LIFE, 1);
        word("paper", LIFE, 1, AIR, 1);
        word("book", LIFE, 1, LIGHT, 1);
        word("wart", LIFE, 1, FIRE, 1, SHADOW, 1);
        word("sniffer", LIFE, 2, EARTH, 1);
        word("armadillo", LIFE, 1, EARTH, 1);
        word("hay", LIFE, 2);
        // Metal
        word("iron", METAL, 2);
        word("copper", METAL, 1, SPARK, 1);
        word("netherite", METAL, 4, FIRE, 1);
        word("chain", METAL, 1);
        word("anvil", METAL, 3, EARTH, 1);
        word("rail", METAL, 1, SPARK, 1);
        word("minecart", METAL, 2);
        word("saddle", LIFE, 1, METAL, 1);
        word("key", METAL, 1, SHADOW, 1);
        word("trial", METAL, 2, SPARK, 1);
        word("vault", METAL, 2, SHADOW, 1);
        word("spawner", SHADOW, 2, METAL, 2);
        word("heavy", METAL, 4, EARTH, 1);
        word("mace", METAL, 3, EARTH, 2);
        word("template", METAL, 2, LIGHT, 1);
        word("armor", METAL, 2);
        word("lodestone", METAL, 2, EARTH, 2, SPARK, 1);
        word("compass", METAL, 1, SPARK, 1);
        word("clock", METAL, 1, LIGHT, 1);
        // Lightning
        word("redstone", SPARK, 2);
        word("repeater", SPARK, 2, EARTH, 1);
        word("comparator", SPARK, 2, EARTH, 1);
        word("piston", SPARK, 1, METAL, 1, EARTH, 1);
        word("observer", SPARK, 2, EARTH, 1);
        word("lightning", SPARK, 3, METAL, 1);
        word("tnt", FIRE, 2, SPARK, 2);
        word("creeper", SPARK, 2, FIRE, 1);
        word("daylight", LIGHT, 1, SPARK, 1);
        word("target", SPARK, 1, LIFE, 1);
        word("button", SPARK, 1);
        word("lever", SPARK, 1);
        word("plate", SPARK, 1, EARTH, 1);
        word("conduit", WATER, 2, SPARK, 2);
        word("star", SPARK, 3, LIGHT, 2);
        word("experience", SPARK, 2, LIGHT, 1);
        word("enchanted", SPARK, 1, LIGHT, 2);
        word("ominous", SHADOW, 2, SPARK, 1);
    }

    private ItemEssenceResolver() {
    }

    private static void word(String word, Object... pairs) {
        WORDS.put(word, pairs);
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
            sum.merge(FIRE, 1.0F, Float::sum);
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
        return strongest(soft, Math.max(tier, rarityTier(result)));
    }

    /** Elements from the words of the item's id, or null when no word is known. */
    static ItemEssences byName(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        Map<Essence, Integer> sum = new EnumMap<>(Essence.class);
        Set<String> used = new HashSet<>();
        String[] tokens = path.split("_");
        for (String token : tokens) {
            for (Map.Entry<String, Object[]> word : WORDS.entrySet()) {
                String key = word.getKey();
                boolean match = token.equals(key) || token.equals(key + "s") || key.length() >= 4 && token.contains(key);
                if (match && used.add(key)) {
                    Object[] pairs = word.getValue();
                    for (int i = 0; i < pairs.length; i += 2) {
                        sum.merge((Essence) pairs[i], (Integer) pairs[i + 1], Integer::sum);
                    }
                }
            }
        }
        if (sum.isEmpty()) {
            return null;
        }
        sum.replaceAll((element, points) -> Math.min(MAX_POINTS, points));
        int total = sum.values().stream().mapToInt(Integer::intValue).sum();
        return strongest(sum, Math.max(rarityTier(item), total >= 6 ? 3 : total >= 4 ? 2 : 1));
    }

    private static ItemEssences byKind(Item item) {
        ItemStack stack = new ItemStack(item);
        if (stack.has(DataComponents.FOOD)) {
            return ItemEssences.of(rarityTier(item), LIFE, 1);
        }
        if (item instanceof BlockItem) {
            return ItemEssences.of(rarityTier(item), EARTH, 1);
        }
        return ItemEssences.of(rarityTier(item), METAL, 1, AIR, 1);
    }

    @SuppressWarnings("deprecation")
    private static int rarityTier(Item item) {
        Rarity rarity = new ItemStack(item).getRarity();
        return rarity == Rarity.EPIC || rarity == Rarity.RARE ? 3 : rarity == Rarity.UNCOMMON ? 2 : 1;
    }

    /** The three strongest elements, strongest first (ring order breaks ties). */
    private static ItemEssences strongest(Map<Essence, Integer> all, int tier) {
        List<Essence> order = new ArrayList<>(all.keySet());
        order.sort((a, b) -> all.get(b) - all.get(a) != 0 ? all.get(b) - all.get(a) : ring(a) - ring(b));
        Map<Essence, Integer> kept = new LinkedHashMap<>();
        for (Essence element : order.subList(0, Math.min(MAX_ELEMENTS, order.size()))) {
            kept.put(element, all.get(element));
        }
        return new ItemEssences(kept, Math.max(1, Math.min(3, tier)));
    }

    private static int ring(Essence element) {
        for (int i = 0; i < HexGrid.RING.length; i++) {
            if (HexGrid.RING[i] == element) {
                return i;
            }
        }
        return 0;
    }

    private static int total(ItemEssences elements) {
        return elements.essences().values().stream().mapToInt(Integer::intValue).sum();
    }
}
