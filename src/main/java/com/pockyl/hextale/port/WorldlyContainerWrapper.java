package com.pockyl.hextale.port;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import org.jspecify.annotations.Nullable;

/**
 * A sided container as a 1.21.1 item handler (the capability the mod's machines register).
 *
 * <p>port-of: net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper
 */
public class WorldlyContainerWrapper extends SidedInvWrapper {
    public WorldlyContainerWrapper(WorldlyContainer container, @Nullable Direction side) {
        super(container, side);
    }
}
