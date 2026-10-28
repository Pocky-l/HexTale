package com.pockyl.hextale.transport;

import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ResourceHandler;
import com.pockyl.hextale.port.TransferPreconditions;
import com.pockyl.hextale.port.ItemResource;
import com.pockyl.hextale.port.SnapshotJournal;
import com.pockyl.hextale.port.TransactionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * An item handler for blocks without slots that use items up as they arrive (a battery soaking crystals, a crucible
 * swallowing what falls in): hoppers, pylons and other mods' pipes can put items in, nothing comes out. What arrives in
 * a transaction is used only when the outermost transaction commits; the deliverer ({@link Deliverer}) is remembered.
 */
public final class ItemIntake extends SnapshotJournal<List<ItemIntake.Arrival>> implements ResourceHandler<ItemResource> {
    /** Items promised in the open transaction: what, how many, for whom. */
    public record Arrival(ItemResource resource, int count, @Nullable UUID deliverer) {
    }

    /** The block behind the intake. */
    public interface Mouth {
        /**
         * How many of {@code resource} it takes now, on top of what is already on its way in the open transaction.
         *
         * @param deliverer the owner of the pylon that brings it (null: a hopper or another mod's pipe)
         */
        int room(ItemResource resource, List<Arrival> pending, @Nullable UUID deliverer);

        /** Items arriving, never more than {@link #room} allowed. */
        void arrive(ItemStack stack, @Nullable UUID deliverer);
    }

    private final Mouth mouth;
    private List<Arrival> pending = new ArrayList<>();

    public ItemIntake(Mouth mouth) {
        this.mouth = mouth;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public ItemResource getResource(int index) {
        Objects.checkIndex(index, size());
        return ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        return 0;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        Objects.checkIndex(index, size());
        return resource.isEmpty() ? 64 : resource.getMaxStackSize();
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        Objects.checkIndex(index, size());
        return mouth.room(resource, List.of(), Deliverer.current()) > 0;
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        UUID deliverer = Deliverer.current();
        int taken = Math.min(amount, mouth.room(resource, List.copyOf(pending), deliverer));
        if (taken <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending.add(new Arrival(resource, taken, deliverer));
        return taken;
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return 0;
    }

    @Override
    protected List<Arrival> createSnapshot() {
        return new ArrayList<>(pending);
    }

    @Override
    protected void revertToSnapshot(List<Arrival> snapshot) {
        pending = snapshot;
    }

    @Override
    protected void onRootCommit(List<Arrival> originalState) {
        List<Arrival> arrived = pending;
        pending = new ArrayList<>();
        for (Arrival arrival : arrived) {
            mouth.arrive(arrival.resource().toStack(arrival.count()), arrival.deliverer());
        }
    }
}
