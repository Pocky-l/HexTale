package com.pockyl.hextale.storage;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.capabilities.Capabilities;
import com.pockyl.hextale.port.ItemResource;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.Essence;

/**
 * What a Hex Vault holds and what that costs it. Items do not take slots: an item weighs its essence points (at least 1),
 * all of them share one capacity, so cheap bulk is light and treasure heavy. Items with data of their own are kept as
 * entries of their own; whatever holds items inside it stays out.
 */
public final class VaultRules {
    /** Points a vault holds by itself. */
    public static final long BASE_CAPACITY = 65_536;
    /** Points each Storage Cube of a closed ring adds. */
    public static final long CUBE_CAPACITY = 32_768;
    /** Different entries a vault keeps at most. */
    public static final int MAX_ENTRIES = 4096;

    private VaultRules() {
    }

    /** What one of this item weighs. */
    public static int weight(ItemResource resource) {
        ItemEssences essences = ResearchItems.of(resource.toStack(1));
        int points = 0;
        if (essences != null) {
            for (int value : essences.essences().values()) {
                points += value;
            }
        }
        return Math.max(1, points);
    }

    /** How much of each primal essence one of this item carries (a compound counts for its two primals), in {@link Essence#PRIMALS} order. */
    public static float[] primals(ItemResource resource) {
        float[] out = new float[Essence.PRIMALS.size()];
        ItemEssences essences = ResearchItems.of(resource.toStack(1));
        if (essences != null) {
            essences.essences().forEach((essence, points) -> {
                for (int i = 0; i < out.length; i++) {
                    out[i] += (float) (points * essence.share(Essence.PRIMALS.get(i)));
                }
            });
        }
        return out;
    }

    /** Whether the vault takes this item at all: never what holds items inside it, never a vault that remembers its own store. */
    public static boolean accepts(ItemResource resource) {
        if (resource.getComponents().has(ModDataComponents.VAULT_MEMORY.get())) {
            return false;
        }
        ItemContainerContents container = resource.getComponents().get(DataComponents.CONTAINER);
        if (container != null && container.nonEmptyItems().iterator().hasNext()) {
            return false;
        }
        BundleContents bundle = resource.getComponents().get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null && !bundle.isEmpty()) {
            return false;
        }
        return !holdsItems(resource.toStack(1));
    }

    /** Backpacks of other mods answer through the item capability. */
    private static boolean holdsItems(ItemStack stack) {
        net.neoforged.neoforge.items.IItemHandler inside = stack.getCapability(Capabilities.ItemHandler.ITEM);
        if (inside == null) {
            return false;
        }
        for (int i = 0; i < inside.getSlots(); i++) {
            if (!inside.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
