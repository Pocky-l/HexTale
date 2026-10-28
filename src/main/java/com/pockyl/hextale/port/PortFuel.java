package com.pockyl.hextale.port;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Furnace fuel on 26.1.2: burn times come from the level's FuelValues and IItemExtension#getBurnTime.
 * port-of: 26.3 cooking_fuel item component (DataComponents.COOKING_FUEL)
 */
public final class PortFuel {
    private PortFuel() {
    }

    /** Ticks the stack burns in a furnace, 0 when it is no fuel. */
    public static int burnTime(Level level, ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
    }

    /** Whether the stack is fuel; without a server at hand (remote client) every stack passes, the server decides. */
    public static boolean isFuel(ItemStack stack) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null || !stack.isEmpty() && stack.getBurnTime(null, server.fuelValues()) > 0;
    }
}
