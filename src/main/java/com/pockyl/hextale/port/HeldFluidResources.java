package com.pockyl.hextale.port;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import org.jspecify.annotations.Nullable;

/**
 * The fluid container in a player's hand, one item of the stack at a time (26.x
 * {@code ItemAccess.forPlayerInteraction(player, hand).oneByOne().getCapability(Capabilities.Fluid.ITEM)}). When a
 * change goes through, the changed container replaces one item in the hand (the rest of a stack stays; a container that
 * no longer fits the stack goes to the inventory).
 */
public final class HeldFluidResources extends DelegatingResourceHandler<FluidResource> {
    private final Player player;
    private final InteractionHand hand;
    private final IFluidHandlerItem container;

    private HeldFluidResources(Player player, InteractionHand hand, IFluidHandlerItem container) {
        super(ResourceHandler.of(container));
        this.player = player;
        this.hand = hand;
        this.container = container;
    }

    public static @Nullable ResourceHandler<FluidResource> of(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return null;
        }
        IFluidHandlerItem container = held.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM);
        return container == null ? null : new HeldFluidResources(player, hand, container);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, @Nullable TransactionContext transaction) {
        return changed(super.insert(index, resource, amount, transaction), transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, @Nullable TransactionContext transaction) {
        return changed(super.insert(resource, amount, transaction), transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, @Nullable TransactionContext transaction) {
        return changed(super.extract(index, resource, amount, transaction), transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, @Nullable TransactionContext transaction) {
        return changed(super.extract(resource, amount, transaction), transaction);
    }

    private int changed(int moved, @Nullable TransactionContext transaction) {
        if (moved > 0) {
            if (transaction == null) {
                swap();
            } else {
                transaction.onCommit(this::swap);
            }
        }
        return moved;
    }

    private boolean swapped;

    private void swap() {
        if (swapped || player.getAbilities().instabuild) {
            return;
        }
        swapped = true;
        ItemStack held = player.getItemInHand(hand);
        ItemStack result = container.getContainer();
        if (held.getCount() <= 1) {
            player.setItemInHand(hand, result);
        } else {
            held.shrink(1);
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
    }
}
