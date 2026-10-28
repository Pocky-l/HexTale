package com.pockyl.hextale.transport;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.Tags;
import com.pockyl.hextale.port.ResourceHandler;
import com.pockyl.hextale.port.TransferPreconditions;
import com.pockyl.hextale.port.FluidResource;
import com.pockyl.hextale.port.SnapshotJournal;
import com.pockyl.hextale.port.Transaction;
import com.pockyl.hextale.port.TransactionContext;

import java.util.Objects;

/**
 * The water inlet of a vessel for the pipes and tanks of other mods ({@code Capabilities.Fluid.BLOCK}): any fluid tagged
 * as water goes in up to the vessel's room, nothing comes out. Water taken inside a transaction is held back and reaches
 * the vessel only when the outermost transaction commits.
 * <p>
 * Also the bridge for fluid containers held in hand (modded buckets and tanks): {@link #pourFromHeld} and
 * {@link #fillHeld}.
 */
public final class WaterInlet extends SnapshotJournal<Integer> implements ResourceHandler<FluidResource> {
    /** The vessel behind the inlet. */
    public interface Vessel {
        /** Water it holds now, in mB (what the pipes see). */
        int water();

        /** How much more water it takes now, in mB (0: none). */
        int waterRoom();

        /** Water arriving through the inlet, never more than {@link #waterRoom()}. */
        void pipeWater(int amount);
    }

    private final Vessel vessel;
    /** Water taken in the open transaction, not yet in the vessel. */
    private int pending;

    public WaterInlet(Vessel vessel) {
        this.vessel = vessel;
    }

    /** Water by the vanilla tag or the common {@code c:water} tag. */
    public static boolean isWater(FluidResource resource) {
        return !resource.isEmpty() && (resource.getType().is(FluidTags.WATER) || resource.getType().is(Tags.Fluids.WATER));
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, size());
        return vessel.water() + pending > 0 ? FluidResource.of(Fluids.WATER) : FluidResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        return vessel.water() + pending;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return resource.isEmpty() || isWater(resource) ? vessel.water() + Math.max(0, vessel.waterRoom()) : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return isWater(resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!isWater(resource)) {
            return 0;
        }
        int taken = Math.min(amount, vessel.waterRoom() - pending);
        if (taken <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending += taken;
        return taken;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    protected Integer createSnapshot() {
        return pending;
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        pending = snapshot;
    }

    @Override
    protected void onRootCommit(Integer originalState) {
        int arrived = pending;
        pending = 0;
        if (arrived > 0) {
            vessel.pipeWater(arrived);
        }
    }

    // ---- Fluid containers in hand -------------------------------------------------------------------------------------

    /** The fluid handler of the item in the player's hand (a modded bucket or tank), or null. */
    private static ResourceHandler<FluidResource> held(Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.HeldFluidResources.of(player, hand);
    }

    /**
     * Pours water out of the fluid container in the player's hand: up to {@code max} mB, all it gives or nothing. The
     * container (and the player's hand) change only when not {@code simulate}.
     *
     * @return the water poured, in mB (0: the item holds no water or is no fluid container)
     */
    public static int pourFromHeld(Player player, InteractionHand hand, int max, boolean simulate) {
        ResourceHandler<FluidResource> handler = held(player, hand);
        if (handler == null || max <= 0) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int poured = 0;
            for (int index = 0; index < handler.size() && poured < max; index++) {
                FluidResource resource = handler.getResource(index);
                if (isWater(resource)) {
                    poured += handler.extract(index, resource, max - poured, transaction);
                }
            }
            if (poured > 0 && !simulate) {
                transaction.commit();
            }
            return poured;
        }
    }

    /**
     * Fills the fluid container in the player's hand with exactly {@code amount} mB of water, or not at all.
     *
     * @return whether it took the water
     */
    public static boolean fillHeld(Player player, InteractionHand hand, int amount, boolean simulate) {
        ResourceHandler<FluidResource> handler = held(player, hand);
        if (handler == null || amount <= 0) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            boolean filled = handler.insert(FluidResource.of(Fluids.WATER), amount, transaction) == amount;
            if (filled && !simulate) {
                transaction.commit();
            }
            return filled;
        }
    }
}
