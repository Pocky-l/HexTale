package com.pockyl.hextale.port;

import net.minecraft.world.Container;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

/**
 * A plain container as a 1.21.1 item handler.
 *
 * <p>port-of: net.neoforged.neoforge.transfer.item.VanillaContainerWrapper
 */
public final class VanillaContainerWrapper {
    private VanillaContainerWrapper() {
    }

    public static IItemHandler of(Container container) {
        return new InvWrapper(container);
    }
}
