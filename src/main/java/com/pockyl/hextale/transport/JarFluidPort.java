package com.pockyl.hextale.transport;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceFluid;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.rune.Essence;

import java.util.Objects;

/**
 * An Essence Jar seen by the tanks and pipes of other mods ({@code Capabilities.Fluid.BLOCK}): pure essence as the fluid
 * {@code hextale:essence} ({@link EssenceFluid}).
 * <ul>
 * <li>A jar gives fluid only while its whole contents are one essence with no unknown share; a mixture shows as an empty
 * jar and is never split.</li>
 * <li>A jar takes the fluid of an essence it {@linkplain EssenceTankBlockEntity#accepts accepts} (its sticker) up to its
 * room, and the essence mixes with what is inside exactly as if a pipe had poured it in.</li>
 * <li>A Creative Essence Jar shows one endless slot per essence, gives any of them and takes nothing.</li>
 * </ul>
 * Moving essence this way is no magical work: it adds no Thinning. Inside a transaction the changes are only noted; they
 * reach the jar when the outermost transaction commits.
 */
public final class JarFluidPort extends SnapshotJournal<JarFluidPort.Pending> implements ResourceHandler<FluidResource> {
    /** Essence poured in and drawn out in the open transaction, not yet in the jar. */
    record Pending(EssenceMix added, EssenceMix removed) {
        static final Pending NONE = new Pending(EssenceMix.EMPTY, EssenceMix.EMPTY);
    }

    private final EssenceTankBlockEntity jar;
    private Pending pending = Pending.NONE;

    public JarFluidPort(EssenceTankBlockEntity jar) {
        this.jar = jar;
    }

    /** The jar's contents as the open transaction sees them. */
    private EssenceMix contents() {
        return jar.tank().contents().plus(pending.added()).minus(pending.removed());
    }

    /** The essence of a creative jar's slot. */
    private static Essence slot(int index) {
        return Essence.values()[index];
    }

    @Override
    public int size() {
        return jar.isCreative() ? Essence.COUNT : 1;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, size());
        if (jar.isCreative()) {
            return EssenceFluid.resource(slot(index));
        }
        Essence pure = contents().pureEssence();
        return pure == null ? FluidResource.EMPTY : EssenceFluid.resource(pure);
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        if (jar.isCreative()) {
            return jar.tank().capacity();
        }
        EssenceMix contents = contents();
        return contents.pureEssence() == null ? 0 : contents.total();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        return resource.isEmpty() || isValid(index, resource) ? jar.tank().capacity() : 0;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        Essence kind = EssenceFluid.kind(resource);
        if (kind == null) {
            return false;
        }
        return jar.isCreative() ? kind == slot(index) : jar.accepts(EssenceMix.pure(kind, 1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Essence kind = EssenceFluid.kind(resource);
        if (kind == null || jar.isCreative() || !jar.accepts(EssenceMix.pure(kind, Math.max(1, amount)))) {
            return 0;
        }
        int taken = Math.min(amount, jar.tank().capacity() - contents().total());
        if (taken <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending = new Pending(pending.added().plus(EssenceMix.pure(kind, taken)), pending.removed());
        return taken;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Essence kind = EssenceFluid.kind(resource);
        if (kind == null) {
            return 0;
        }
        if (jar.isCreative()) {
            // Endless: whatever is drawn is back the next tick, so nothing needs noting.
            return kind == slot(index) ? Math.min(amount, jar.tank().capacity()) : 0;
        }
        EssenceMix contents = contents();
        if (contents.pureEssence() != kind) {
            return 0;
        }
        int taken = Math.min(amount, contents.total());
        if (taken <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        pending = new Pending(pending.added(), pending.removed().plus(EssenceMix.pure(kind, taken)));
        return taken;
    }

    @Override
    protected Pending createSnapshot() {
        return pending;
    }

    @Override
    protected void revertToSnapshot(Pending snapshot) {
        pending = snapshot;
    }

    @Override
    protected void onRootCommit(Pending originalState) {
        Pending done = pending;
        pending = Pending.NONE;
        if (done.added().isEmpty() && done.removed().isEmpty() || jar.isRemoved()) {
            return;
        }
        jar.tank().set(jar.tank().contents().plus(done.added()).minus(done.removed()));
        jar.setChanged();
    }
}
