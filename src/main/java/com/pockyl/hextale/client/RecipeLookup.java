package com.pockyl.hextale.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

/**
 * Opens a recipe viewer (JEI) on an item the mod draws on its own screens, such as the diary's pictures: the viewer's own
 * "show recipe" / "show uses" bindings decide which key does what. Without a viewer nothing happens. The viewer's
 * compat code plugs itself in, so nothing here loads its classes.
 */
public final class RecipeLookup {
    /** A recipe viewer: opens on the item when this key is one of its bindings; true when it did. */
    public interface Viewer {
        boolean show(InputConstants.Key key, ItemStack stack);
    }

    @Nullable
    private static Viewer viewer;

    private RecipeLookup() {
    }

    public static void set(@Nullable Viewer value) {
        viewer = value;
    }

    /** Opens the viewer on the item when the key is its "show recipe" or "show uses" binding. */
    public static boolean show(InputConstants.Key key, ItemStack stack) {
        Viewer current = viewer;
        return current != null && !stack.isEmpty() && current.show(key, stack);
    }
}
