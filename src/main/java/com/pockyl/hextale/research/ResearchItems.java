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
        add(Items.COAL, 1, FIRE, 4);
        add(Items.CHARCOAL, 1, FIRE, 3, ASH, 1);
        add(Items.REDSTONE, 1, SPARK, 4);
        add(Items.GLOWSTONE_DUST, 1, LIGHT, 4);
        add(Items.STICK, 1, PLANT, 1);
        add(Items.CLAY_BALL, 1, EARTH, 2, WATER, 2);
        add(Items.INK_SAC, 1, SHADOW, 4);
        add(Items.GLOW_INK_SAC, 2, LIGHT, 3, MAGIC, 1);
        add(Items.BONE, 1, DECAY, 4);
        add(Items.STRING, 1, BEAST, 2, PLANT, 1);
        add(Items.GUNPOWDER, 1, FIRE, 3, ASH, 2);
        add(Items.SLIME_BALL, 1, SAP, 4);
        add(Items.FEATHER, 1, AIR, 3);
        add(Items.LEATHER, 1, BEAST, 4);
        add(Items.FLINT, 1, EARTH, 2, FIRE, 1);
        add(Items.IRON_INGOT, 2, METAL, 5);
        add(Items.GOLD_INGOT, 2, METAL, 4, LIGHT, 2);
        add(Items.COPPER_INGOT, 1, METAL, 3, SPARK, 2);
        add(Items.AMETHYST_SHARD, 2, CRYSTAL, 4, SPARK, 2);
        add(Items.QUARTZ, 2, CRYSTAL, 4, MAGMA, 1);
        add(Items.LAPIS_LAZULI, 2, MAGIC, 3, CRYSTAL, 2);
        add(Items.EMERALD, 2, CRYSTAL, 5, LIFE, 2);
        add(Items.DIAMOND, 3, CRYSTAL, 8, LIGHT, 2);
        add(Items.PRISMARINE_CRYSTALS, 2, WATER, 3, CRYSTAL, 3, LIGHT, 1);
        add(Items.PRISMARINE_SHARD, 2, WATER, 4, CRYSTAL, 2);
        add(Items.BLAZE_ROD, 3, FIRE, 6, MAGMA, 2, MAGIC, 1);
        add(Items.BLAZE_POWDER, 2, FIRE, 5, MAGMA, 1);
        add(Items.ENDER_PEARL, 2, RIFT, 5, AIR, 1);
        add(Items.GHAST_TEAR, 3, MAGIC, 4, WATER, 2);
        add(Items.PHANTOM_MEMBRANE, 2, AIR, 3, RIFT, 3);
        add(Items.ECHO_SHARD, 3, SHADOW, 6, MAGIC, 2);
        add(Items.HONEY_BOTTLE, 2, SAP, 6);
        add(Items.HEART_OF_THE_SEA, 3, WATER, 8, MAGIC, 4);
        add(Items.NAUTILUS_SHELL, 2, WATER, 4, DECAY, 2);
        add(Items.WIND_CHARGE, 3, AIR, 6, FIRE, 1);
        add(Items.BREEZE_ROD, 3, AIR, 6, SPARK, 3);
        add(Items.TOTEM_OF_UNDYING, 3, MAGIC, 8, LIFE, 6);
        add(Items.NETHERITE_INGOT, 3, METAL, 10, MAGMA, 4);
        add(Items.NETHER_STAR, 3, MAGIC, 16, RIFT, 4);
        add(Items.DRAGON_BREATH, 3, RIFT, 4, MAGIC, 3, FIRE, 2);
        add(Items.SHULKER_SHELL, 3, RIFT, 4, METAL, 3);
        add(Items.EXPERIENCE_BOTTLE, 2, MAGIC, 6);
        // Plain sources of the primals found everywhere: their ink needs no separator.
        add(Items.SNOWBALL, 1, WATER, 2);
        add(Items.SEAGRASS, 1, WATER, 3);
        add(Items.WHEAT_SEEDS, 1, LIFE, 3);
        // One clear source for essences the words spread thin.
        add(Items.CAULDRON, 2, STEAM, 4, METAL, 4);
        add(Items.CAMPFIRE, 1, ASH, 4, FIRE, 3);
        add(Items.TUFF, 1, ASH, 3, EARTH, 3);
        add(Items.TRIDENT, 3, WATER, 5, SPARK, 4, METAL, 2);
        add(Items.DEEPSLATE, 1, EARTH, 4, SHADOW, 2);
        add(Items.HEAVY_CORE, 3, METAL, 5, EARTH, 3);
        add(Items.COBWEB, 1, BEAST, 2, SHADOW, 2);
        add(Items.GOAT_HORN, 2, AIR, 4, BEAST, 3);
        add(Items.TINTED_GLASS, 2, CRYSTAL, 3, SHADOW, 2);
        add(Items.END_ROD, 2, LIGHT, 4, MAGIC, 2);
        add(Items.GLISTERING_MELON_SLICE, 2, LIFE, 4, MAGIC, 1);
        add(Items.CLOCK, 2, METAL, 3, MAGIC, 3);
        add(Items.NETHER_WART, 1, MAGMA, 2, PLANT, 2);
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
