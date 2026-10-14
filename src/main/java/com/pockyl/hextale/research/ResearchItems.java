package com.pockyl.hextale.research;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.registry.ModDataMaps;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import static com.pockyl.hextale.rune.SimpleRune.AIR;
import static com.pockyl.hextale.rune.SimpleRune.EARTH;
import static com.pockyl.hextale.rune.SimpleRune.FIRE;
import static com.pockyl.hextale.rune.SimpleRune.LIFE;
import static com.pockyl.hextale.rune.SimpleRune.LIGHT;
import static com.pockyl.hextale.rune.SimpleRune.METAL;
import static com.pockyl.hextale.rune.SimpleRune.SHADOW;
import static com.pockyl.hextale.rune.SimpleRune.SPARK;
import static com.pockyl.hextale.rune.SimpleRune.WATER;

/**
 * Items as levers on the parchment. In game the elements of an item come from the {@code hextale:elements} data map
 * (data packs may add items); boards and star thresholds are computed from the {@link #canonical()} set below, which
 * is also the default data map, so a board never changes with the items a pack adds.
 */
public final class ResearchItems {
    private static final Map<Item, ItemElements> CANONICAL = new LinkedHashMap<>();
    private static final Map<SimpleRune, Item[]> RESOURCES = new EnumMap<>(SimpleRune.class);

    static {
        add(Items.COAL, 1, FIRE, 2);
        add(Items.KELP, 1, WATER, 2, LIFE, 1);
        add(Items.COBBLESTONE, 1, EARTH, 2);
        add(Items.FEATHER, 1, AIR, 2);
        add(Items.GLOW_BERRIES, 1, LIGHT, 2, LIFE, 1);
        add(Items.INK_SAC, 1, SHADOW, 1, WATER, 1);
        add(Items.WHEAT, 1, LIFE, 2);
        add(Items.COPPER_INGOT, 1, METAL, 1, SPARK, 1);
        add(Items.REDSTONE, 1, SPARK, 2);
        add(Items.STICK, 1, LIFE, 1);
        add(Items.CLAY_BALL, 1, EARTH, 1, WATER, 1);
        add(Items.TORCH, 1, FIRE, 1, LIGHT, 1);
        add(Items.BLAZE_POWDER, 2, FIRE, 3);
        add(Items.PRISMARINE_CRYSTALS, 2, WATER, 2, LIGHT, 2);
        add(Items.OBSIDIAN, 2, EARTH, 2, FIRE, 1, SHADOW, 1);
        add(Items.PHANTOM_MEMBRANE, 2, AIR, 3, SHADOW, 1);
        add(Items.GLOWSTONE, 2, LIGHT, 3);
        add(Items.SCULK, 2, SHADOW, 2);
        add(Items.HONEY_BOTTLE, 2, LIFE, 3);
        add(Items.IRON_INGOT, 2, METAL, 2);
        add(Items.REDSTONE_BLOCK, 2, SPARK, 3);
        add(Items.GOLD_INGOT, 2, METAL, 2, LIGHT, 1);
        add(Items.AMETHYST_SHARD, 2, LIGHT, 1, EARTH, 1, SPARK, 1);
        add(Items.BLAZE_ROD, 3, FIRE, 4, AIR, 1);
        add(Items.HEART_OF_THE_SEA, 3, WATER, 4, LIFE, 1);
        add(Items.DIAMOND, 3, EARTH, 2, LIGHT, 2);
        add(Items.WIND_CHARGE, 3, AIR, 4);
        add(Items.BEACON, 3, LIGHT, 4, METAL, 1);
        add(Items.ECHO_SHARD, 3, SHADOW, 4);
        add(Items.TOTEM_OF_UNDYING, 3, LIFE, 4, LIGHT, 1);
        add(Items.NETHERITE_INGOT, 3, METAL, 4, FIRE, 1);
        add(Items.NETHER_STAR, 3, SPARK, 3, LIGHT, 2);

        resources(FIRE, Items.COAL, Items.BLAZE_POWDER, Items.BLAZE_ROD);
        resources(WATER, Items.KELP, Items.PRISMARINE_CRYSTALS, Items.HEART_OF_THE_SEA);
        resources(EARTH, Items.COBBLESTONE, Items.OBSIDIAN, Items.DIAMOND);
        resources(AIR, Items.FEATHER, Items.PHANTOM_MEMBRANE, Items.WIND_CHARGE);
        resources(LIGHT, Items.GLOW_BERRIES, Items.GLOWSTONE, Items.BEACON);
        resources(SHADOW, Items.INK_SAC, Items.SCULK, Items.ECHO_SHARD);
        resources(LIFE, Items.WHEAT, Items.HONEY_BOTTLE, Items.TOTEM_OF_UNDYING);
        resources(METAL, Items.COPPER_INGOT, Items.IRON_INGOT, Items.NETHERITE_INGOT);
        resources(SPARK, Items.REDSTONE, Items.REDSTONE_BLOCK, Items.NETHER_STAR);
    }

    private ResearchItems() {
    }

    private static void add(Item item, int tier, Object... pairs) {
        CANONICAL.put(item, ItemElements.of(tier, pairs));
    }

    private static void resources(SimpleRune element, Item... byTier) {
        RESOURCES.put(element, byTier);
    }

    /** The default levers in prototype order; boards and star thresholds are computed from these. */
    public static Map<Item, ItemElements> canonical() {
        return Collections.unmodifiableMap(CANONICAL);
    }

    /** The resource a point of this element asks for on a board of this tier (1-3). */
    public static Item resource(SimpleRune element, int tier) {
        return RESOURCES.get(element)[tier - 1];
    }

    /** What an item carries in this world (data map), or null when it cannot write on the parchment. */
    @Nullable
    public static ItemElements of(Item item) {
        @SuppressWarnings("deprecation")
        Holder<Item> holder = item.builtInRegistryHolder();
        return holder.getData(ModDataMaps.ELEMENTS);
    }

    @Nullable
    public static ItemElements of(ItemStack stack) {
        return stack.isEmpty() ? null : of(stack.getItem());
    }

    /** Distinct non-zero steps of the canonical items, plain and (with a mortar) ground. */
    static List<int[]> canonicalOffsets(boolean ground) {
        List<int[]> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ItemElements elements : CANONICAL.values()) {
            for (boolean half : ground ? new boolean[]{false, true} : new boolean[]{false}) {
                int[] offset = elements.offset(half);
                if ((offset[0] != 0 || offset[1] != 0) && seen.add(offset[0] + "," + offset[1])) {
                    out.add(offset);
                }
            }
        }
        return out;
    }
}
