package com.pockyl.hextale.research;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.registry.ModDataMaps;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

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
 * Items and the elements they carry (their ink at the Research Table). In game the elements of every item come from
 * {@link ItemEssenceTable} (the {@code hextale:elements} data map plus what is derived from recipes and names); the
 * {@link #canonical()} set below is the default data map.
 */
public final class ResearchItems {
    private static final Map<Item, ItemEssences> CANONICAL = new LinkedHashMap<>();

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

    }

    private ResearchItems() {
    }

    private static void add(Item item, int tier, Object... pairs) {
        CANONICAL.put(item, ItemEssences.of(tier, pairs));
    }

    /** The base materials of the default data map. */
    public static Map<Item, ItemEssences> canonical() {
        return Collections.unmodifiableMap(CANONICAL);
    }

    /**
     * What an item carries in this world: every item carries something once the server has resolved the table (see
     * {@link ItemEssenceResolver}); before that only the data map's base materials do. Null for air.
     */
    @Nullable
    public static ItemEssences of(Item item) {
        ItemEssences resolved = ItemEssenceTable.get(item);
        if (resolved != null) {
            return resolved;
        }
        @SuppressWarnings("deprecation")
        Holder<Item> holder = item.builtInRegistryHolder();
        return holder.getData(ModDataMaps.ESSENCES);
    }

    @Nullable
    public static ItemEssences of(ItemStack stack) {
        return stack.isEmpty() ? null : of(stack.getItem());
    }
}
