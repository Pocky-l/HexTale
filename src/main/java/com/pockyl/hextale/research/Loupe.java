package com.pockyl.hextale.research;

import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.registry.ModItems;

/**
 * The Research Table's loupe arm, rules shared by both sides: the researcher drags the loupe onto an item, its three
 * lenses swing in one after another (each magnifies more), and when the last one has looked long enough the item is
 * studied. Free and unlimited; the server only checks that the loupe really rested on that item for the time.
 */
public final class Loupe {
    /** The lenses: x2, x4, x8. */
    public static final int LENSES = 3;
    /** How long each lens looks before the next swings in. */
    public static final int LENS_TICKS = 9;
    public static final int STUDY_TICKS = LENSES * LENS_TICKS;
    /** Network and frame slack the server allows on the time the loupe rested. */
    public static final int SLACK_TICKS = 4;

    private Loupe() {
    }

    /** Whether the loupe finds essences in this item (bottles of essence carry their liquid's, not their own). */
    public static boolean studiable(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(ModItems.ESSENCE_FLASK.get()) && ResearchItems.of(stack) != null;
    }

    /** Lenses swung in after {@code ticks} of looking, 1 to {@link #LENSES}. */
    public static int lenses(float ticks) {
        return Math.min(LENSES, 1 + (int) (Math.max(0, ticks) / LENS_TICKS));
    }
}
