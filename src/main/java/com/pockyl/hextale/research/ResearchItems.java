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

import static com.pockyl.hextale.rune.Essence.*;


/**
 * Items and the essences they carry. In game the essences of every item come from {@link ItemEssenceTable} (the
 * {@code hextale:essences} data map plus what is derived from recipes and names); the {@link #canonical()} set below is
 * the default data map, hand-tuned base materials.
 */
public final class ResearchItems {
    private static final Map<Item, ItemEssences> CANONICAL = new LinkedHashMap<>();

    static {
        // Hand-tuned base materials: what the words of their names alone would get only roughly right.
        add(Items.COAL, 1, FIRE, 4, DEPTHS, 2, ASH, 2, EARTH, 1);
        add(Items.CHARCOAL, 1, FIRE, 4, WOOD, 2, ASH, 2);
        add(Items.REDSTONE, 1, CIRCUIT, 4, ENERGY, 3, DUST, 1, SPARK, 1);
        add(Items.GLOWSTONE_DUST, 1, SUN, 3, LIGHT, 3, DUST, 1, HELL, 1);
        add(Items.STICK, 1, WOOD, 2);
        add(Items.CLAY_BALL, 1, CLAY, 4, WATER, 1);
        add(Items.INK_SAC, 1, SHADOW, 3, OCEAN, 1, DYE, 2, KNOWLEDGE, 1);
        add(Items.GLOW_INK_SAC, 2, LIGHT, 3, OCEAN, 1, DYE, 2, MAGIC, 1);
        add(Items.BONE, 1, BONE, 5, DECAY, 1);
        add(Items.STRING, 1, FIBER, 4, BEAST, 1);
        add(Items.GUNPOWDER, 1, BLAST, 5, DUST, 2, CHAOS, 1);
        add(Items.SLIME_BALL, 1, SLIME, 5, LIFE, 1, SAP, 1);
        add(Items.FEATHER, 1, AIR, 3, WIND, 2, BEAST, 1, SKY, 1);
        add(Items.LEATHER, 1, BEAST, 4, PROTECTION, 2);
        add(Items.FLINT, 1, EARTH, 2, FIRE, 2, TOOL, 2);
        add(Items.IRON_INGOT, 2, METAL, 5, FORGE, 2, ORE, 1);
        add(Items.GOLD_INGOT, 2, GOLD, 5, GLEAM, 3, FORGE, 1);
        add(Items.COPPER_INGOT, 1, METAL, 3, CIRCUIT, 2, RUST, 1, FORGE, 1);
        add(Items.AMETHYST_SHARD, 2, CRYSTAL, 4, SPARK, 2, GLEAM, 2);
        add(Items.QUARTZ, 2, CRYSTAL, 3, HELL, 2, PURITY, 2);
        add(Items.LAPIS_LAZULI, 2, GEM, 3, MAGIC, 3, WATER, 1);
        add(Items.EMERALD, 2, GEM, 5, LIFE, 2, CRAFT, 2);
        add(Items.DIAMOND, 3, GEM, 7, CRYSTAL, 4, GLEAM, 3, PROTECTION, 1);
        add(Items.PRISMARINE_CRYSTALS, 2, OCEAN, 4, CRYSTAL, 3, LIGHT, 2);
        add(Items.PRISMARINE_SHARD, 2, OCEAN, 4, CRYSTAL, 2, EARTH, 1);
        add(Items.BLAZE_ROD, 3, FIRE, 6, HELL, 3, FURY, 3, ENERGY, 2);
        add(Items.BLAZE_POWDER, 2, FIRE, 5, HELL, 2, FURY, 2, VIGOR, 1);
        add(Items.ENDER_PEARL, 2, RIFT, 5, ENDER, 3, MOTION, 2);
        add(Items.GHAST_TEAR, 3, SPIRIT, 5, PURITY, 3, SOUL, 3, HELL, 1);
        add(Items.PHANTOM_MEMBRANE, 2, DREAM, 4, SKY, 3, FLESH, 1, AIR, 2);
        add(Items.ECHO_SHARD, 3, ECHO, 7, DEPTHS, 3, SILENCE, 3, SOUL, 2);
        add(Items.HONEY_BOTTLE, 2, SWEET, 6, SAP, 3, LIFE, 1);
        add(Items.HEART_OF_THE_SEA, 3, OCEAN, 8, MAGIC, 4, PURITY, 3, LIFE, 3);
        add(Items.NAUTILUS_SHELL, 2, OCEAN, 4, BONE, 3, PROTECTION, 1);
        add(Items.WIND_CHARGE, 3, WIND, 6, AIR, 3, BLAST, 2, MOTION, 2);
        add(Items.BREEZE_ROD, 3, WIND, 7, STORM, 3, ENERGY, 2, MOTION, 2);
        add(Items.TOTEM_OF_UNDYING, 3, LIFE, 7, SPIRIT, 5, MAGIC, 4, TIME, 3, GOLD, 2);
        add(Items.NETHERITE_INGOT, 3, METAL, 8, HELL, 5, PROTECTION, 4, FORGE, 3);
        add(Items.NETHER_STAR, 3, STAR, 10, MAGIC, 6, CHAOS, 4, SOUL, 3, ENERGY, 3);
        add(Items.DRAGON_BREATH, 3, BREATH, 6, ENDER, 4, FURY, 3, MAGIC, 3);
        add(Items.SHULKER_SHELL, 3, ENDER, 5, PROTECTION, 4, ORDER, 2);
        add(Items.EXPERIENCE_BOTTLE, 2, MAGIC, 4, ENERGY, 3, SPIRIT, 2, GLASS, 1);
        // One clear source (strongest essence = a separation reagent) for essences the words spread thin.
        add(Items.CAULDRON, 2, STEAM, 5, METAL, 4, WATER, 1);
        add(Items.CAMPFIRE, 1, SMOKE, 5, FIRE, 4, WOOD, 2);
        add(Items.TUFF, 1, ASH, 4, EARTH, 3, DUST, 1);
        add(Items.TRIDENT, 3, STORM, 6, OCEAN, 4, WEAPON, 4, THUNDER, 2);
        add(Items.DEEPSLATE, 1, DEPTHS, 5, EARTH, 4);
        add(Items.HEAVY_CORE, 3, QUAKE, 6, METAL, 5, ORDER, 3);
        add(Items.COBWEB, 1, SILENCE, 4, FIBER, 3, SHADOW, 1);
        add(Items.GOAT_HORN, 2, THUNDER, 5, ECHO, 4, BEAST, 2);
        add(Items.TINTED_GLASS, 2, ILLUSION, 4, GLASS, 3, SHADOW, 2);
        add(Items.END_ROD, 2, ENERGY, 5, LIGHT, 3, ENDER, 2);
        add(Items.GLISTERING_MELON_SLICE, 2, VIGOR, 5, GOLD, 2, SWEET, 1, MAGIC, 1);
        add(Items.CLOCK, 2, TIME, 6, GOLD, 3, MECHANISM, 2);
        add(Items.NETHER_WART, 1, BLOOD, 4, HELL, 3, PLANT, 2, MAGIC, 1);
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
