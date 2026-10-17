package com.pockyl.hextale.runework;

import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.registry.ModDataComponents;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * The hextech pickaxes. Each one is made at the Hextech Altar from a pickaxe, pedestal items and essences while the
 * runes its recipe names stand on rune steles around the altar (data: {@code hextale_infusion/<id>.json}); without
 * them it cannot be made. Its abilities are fixed; how strong they are - and how fast it digs - depends only on its
 * stars, which are the stars of the weakest rune it was made with. Tier = how many runes its recipe needs (1-4).
 */
public enum HextechPickaxe {
    /** Smelting: what it mines comes out smelted. */
    SMELTING(1, Ability.SMELT),
    /** Insight: right click shows the ores around through the stone. */
    PROSPECTOR(1, Ability.REVEAL),
    /** Tempering: barely wears, cuts hard stone fast. */
    TEMPERED(1, Ability.TEMPER),
    /** Drill + Tempering: digs an area. */
    DRILL(2, Ability.AREA, Ability.TEMPER),
    /** Erosion + Forge: takes a whole ore vein, ores give more. */
    VEIN(2, Ability.VEIN, Ability.RICH_ORE),
    /** Gemstone + Lens: stone gives up gems, the deep is seen clearly. */
    GEODE(2, Ability.GEMS, Ability.CLEAR_SIGHT),
    /** Passage + Drill + Grounding: digs an area, right click bores a tunnel. */
    TUNNEL(3, Ability.AREA, Ability.TUNNEL),
    /** Quake + Forge + Erosion: right click shatters the rock around and leaves the ores; takes whole veins. */
    QUAKE(3, Ability.QUAKE, Ability.VEIN, Ability.RICH_ORE),
    /** Smelting + Drill + Cache + Magnet: the area, smelted, straight into the pocket; drops fly to it; bores tunnels. */
    CORE(4, Ability.AREA, Ability.SMELT, Ability.CACHE, Ability.MAGNET, Ability.TUNNEL);

    /** Dig speed by stars (1-3): the stars matter more than anything else. */
    private static final float[] SPEED = {1.0F, 1.5F, 2.2F};

    private final int tier;
    private final Set<Ability> abilities;

    HextechPickaxe(int tier, Ability first, Ability... more) {
        this.tier = tier;
        this.abilities = EnumSet.of(first, more);
    }

    /** Item id, e.g. {@code smelting_pickaxe}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT) + "_pickaxe";
    }

    /** How many runes the recipe needs: 1 for the first pickaxes, up to 4 for the last. */
    public int tier() {
        return tier;
    }

    public Set<Ability> abilities() {
        return abilities;
    }

    public boolean has(Ability ability) {
        return abilities.contains(ability);
    }

    /** The right-click ability, or null when the pickaxe has none. */
    public Ability active() {
        for (Ability ability : abilities) {
            if (ability.active) {
                return ability;
            }
        }
        return null;
    }

    /** A pickaxe's stars: the weakest rune it was made with (1 when unknown, e.g. from the creative tab). */
    public static int stars(ItemStack stack) {
        return Math.max(1, Math.min(3, stack.getOrDefault(ModDataComponents.STARS.get(), 1)));
    }

    public static float speed(int stars) {
        return SPEED[stars - 1];
    }

    /** What a pickaxe can do; values by stars 1-3 live in {@link PickaxeEffects}. */
    public enum Ability {
        SMELT(false), REVEAL(true), TEMPER(false), AREA(false), VEIN(false), RICH_ORE(false), GEMS(false), CLEAR_SIGHT(false),
        TUNNEL(true), QUAKE(true), CACHE(false), MAGNET(false);

        private final boolean active;

        Ability(boolean active) {
            this.active = active;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
