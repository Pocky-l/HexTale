package com.pockyl.hextale.transport;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import com.pockyl.hextale.port.ResourceHandler;
import com.pockyl.hextale.port.TransferPreconditions;
import com.pockyl.hextale.port.FluidResource;
import com.pockyl.hextale.port.SnapshotJournal;
import com.pockyl.hextale.port.TransactionContext;

import com.pockyl.hextale.Hextale;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Experience kept in a block, offered to other mods' pipes and tanks as liquid experience: the first source fluid in
 * the common tag {@code c:experience}, {@value #MB_PER_POINT} mB a point (the convention of that tag). Without such a
 * fluid (no mod adds one) the block has no fluid handler at all. Extract only; points leave when the outermost
 * transaction commits.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ExperienceOutlet extends SnapshotJournal<Integer> implements ResourceHandler<FluidResource> {
    public static final int MB_PER_POINT = 20;
    private static final Fluid UNRESOLVED = Fluids.FLOWING_WATER;
    private static volatile @Nullable Fluid fluid = UNRESOLVED;

    /** The store behind the outlet. */
    public interface Store {
        int points();

        int capacity();

        /** Points taken out by a pipe, never more than {@link #points()}. */
        void drainPoints(int points);
    }

    private final Store store;
    /** Points taken in the open transaction, not yet gone from the store. */
    private int pending;

    public ExperienceOutlet(Store store) {
        this.store = store;
    }

    /** The liquid experience of the mods present, or null when none is tagged {@code c:experience}. */
    public static @Nullable Fluid fluid() {
        Fluid known = fluid;
        if (known == UNRESOLVED) {
            known = null;
            for (Fluid candidate : BuiltInRegistries.FLUID) {
                if (candidate != Fluids.EMPTY && candidate.is(Tags.Fluids.EXPERIENCE) && candidate.isSource(candidate.defaultFluidState())) {
                    known = candidate;
                    break;
                }
            }
            fluid = known;
        }
        return known;
    }

    /** Tags were (re)loaded: look for the fluid again. */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        fluid = UNRESOLVED;
    }

    private int left() {
        return Math.max(0, store.points() - pending);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, size());
        Fluid liquid = fluid();
        return liquid == null || left() <= 0 ? FluidResource.EMPTY : FluidResource.of(liquid);
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        return (long) left() * MB_PER_POINT;
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return (long) store.capacity() * MB_PER_POINT;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        Fluid liquid = fluid();
        return liquid != null && resource.getFluid() == liquid;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Fluid liquid = fluid();
        if (liquid == null || resource.getFluid() != liquid || !resource.isComponentsPatchEmpty()) {
            return 0;
        }
        int points = Math.min(amount / MB_PER_POINT, left());
        if (points <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending += points;
        return points * MB_PER_POINT;
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
        int taken = pending;
        pending = 0;
        if (taken > 0) {
            store.drainPoints(Math.min(taken, store.points()));
        }
    }
}
