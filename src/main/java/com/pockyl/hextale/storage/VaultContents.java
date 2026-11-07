package com.pockyl.hextale.storage;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.pockyl.hextale.block.HexVaultBlockEntity;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * What one Hex Vault holds. It lives in {@link VaultStore}, not in the block: breaking the vault loses nothing and a
 * Hex Terminal reaches it in an unloaded chunk. This is also the vault's item handler, so every way in and out (hoppers,
 * pipes, pylons, the screen) goes through the same transactions and can neither make nor lose an item. Changes apply at
 * once and are undone from a log on a rollback; an entry emptied inside a transaction stays in the list until it ends, so
 * indices hold still while someone walks them.
 */
public final class VaultContents extends SnapshotJournal<Integer> implements ResourceHandler<ItemResource> {
    /** As many as the widest grid has columns. */
    public static final int MOST_PINS = 18;
    private static final Codec<ItemEntry> ENTRY = RecordCodecBuilder.create(instance -> instance.group(
            ItemResource.CODEC.fieldOf("item").forGetter(ItemEntry::resource),
            Codec.LONG.fieldOf("count").forGetter(ItemEntry::count)).apply(instance, ItemEntry::new));
    /** Entries that cannot be read now (their mod is gone) are carried along untouched, so they come back with it. */
    private static final Codec<Saved> SAVED_ENTRIES = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Saved, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getStream(input).map(stream -> {
                List<ItemEntry> read = new ArrayList<>();
                List<Dynamic<?>> unread = new ArrayList<>();
                stream.forEach(element -> ENTRY.parse(ops, element).result().ifPresentOrElse(read::add,
                        () -> unread.add(new Dynamic<>(ops, element))));
                return Pair.of(new Saved(read, unread), input);
            });
        }

        @Override
        public <T> DataResult<T> encode(Saved saved, DynamicOps<T> ops, T prefix) {
            ListBuilder<T> list = ops.listBuilder();
            saved.read().forEach(entry -> list.add(ENTRY.encodeStart(ops, entry)));
            saved.unread().forEach(entry -> list.add(entry.convert(ops).getValue()));
            return list.build(prefix);
        }
    };
    public static final Codec<VaultContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SAVED_ENTRIES.fieldOf("entries").forGetter(VaultContents::saved),
            Codec.LONG.optionalFieldOf("capacity", VaultRules.BASE_CAPACITY).forGetter(VaultContents::capacity),
            Codec.LONG.optionalFieldOf("peak", 0L).forGetter(contents -> contents.peak),
            Codec.INT.optionalFieldOf("thrift", 0).forGetter(VaultContents::thrift),
            Codec.INT.optionalFieldOf("radiance", 0).forGetter(VaultContents::radiance),
            GlobalPos.CODEC.optionalFieldOf("home").forGetter(contents -> Optional.ofNullable(contents.home)),
            Codec.BOOL.optionalFieldOf("open", false).forGetter(VaultContents::open),
            // Lenient: a pin of a mod that is gone costs the row, never the store.
            ItemResource.CODEC.listOf().lenientOptionalFieldOf("pins", List.of()).forGetter(VaultContents::pins)
    ).apply(instance, VaultContents::new));

    /** An entry as it is saved. */
    private record ItemEntry(ItemResource resource, long count) {
    }

    private record Saved(List<ItemEntry> read, List<Dynamic<?>> unread) {
    }

    /** Something that wants to hear of changes: an open screen. */
    public interface Watcher {
        void changed(Collection<Entry> entries);
    }

    /** Why an item stays out. */
    public enum Refusal {
        FULL, TOO_MANY_KINDS, CONTAINER, ASLEEP
    }

    /** One kind of item held. */
    public static final class Entry {
        private final ItemResource resource;
        private final int weight;
        private final float[] primals;
        private final boolean worn;
        private long count;

        private Entry(ItemResource resource) {
            this.resource = resource;
            this.weight = VaultRules.weight(resource);
            this.primals = VaultRules.primals(resource);
            this.worn = resource.getComponents().getOrDefault(DataComponents.DAMAGE, 0) > 0;
        }

        public ItemResource resource() {
            return resource;
        }

        public long count() {
            return count;
        }

        public int weight() {
            return weight;
        }

        /** The points this entry takes up. */
        public long points() {
            return count * weight;
        }
    }

    private record Change(Entry entry, long delta) {
    }

    private final Map<ItemResource, Entry> index = new HashMap<>();
    private final List<Entry> entries = new ArrayList<>();
    private final List<Dynamic<?>> unread;
    private final List<Change> log = new ArrayList<>();
    private final Set<Watcher> watchers = new LinkedHashSet<>();
    /** The favourites row of the screen, shared by everyone at this vault. A pin outlives its last item. */
    private final List<ItemResource> pins = new ArrayList<>();
    private final double[] primals = new double[Essence.PRIMALS.size()];
    /** Entries as loaded: weighed on first use, the essences of items are not resolved yet while the world loads. */
    @Nullable
    private List<ItemEntry> unweighed;
    private long used;
    private long capacity = VaultRules.BASE_CAPACITY;
    /** The most points ever held: Radiance pays only for new heights, never for taking out and putting back. */
    private long peak;
    private long unpaidPoints;
    private int thrift;
    private int radiance;
    @Nullable
    private GlobalPos home;
    /** The vault stands in its finished gate. Saved, so a terminal knows while its chunk is not loaded. */
    private boolean open;
    @Nullable
    private HexVaultBlockEntity standing;
    @Nullable
    private VaultStore store;

    VaultContents() {
        unread = new ArrayList<>();
    }

    private VaultContents(Saved saved, long capacity, long peak, int thrift, int radiance, Optional<GlobalPos> home, boolean open,
                          List<ItemResource> pins) {
        this.unread = new ArrayList<>(saved.unread());
        this.unweighed = saved.read();
        this.capacity = capacity;
        this.peak = peak;
        this.thrift = thrift;
        this.radiance = radiance;
        this.home = home.orElse(null);
        this.open = open;
        this.pins.addAll(pins);
    }

    public List<ItemResource> pins() {
        return pins;
    }

    /** Pins an item, or takes its pin away; a full row takes no more. */
    public void togglePin(ItemResource resource) {
        if (!pins.remove(resource)) {
            if (pins.size() >= MOST_PINS) {
                return;
            }
            pins.add(resource);
        }
        dirty();
    }

    public boolean open() {
        return open;
    }

    public void setOpen(boolean open) {
        if (this.open != open) {
            this.open = open;
            dirty();
        }
    }

    private void ready() {
        List<ItemEntry> load = unweighed;
        if (load != null) {
            unweighed = null;
            for (ItemEntry saved : load) {
                Entry entry = index.get(saved.resource());
                if (entry == null) {
                    entry = new Entry(saved.resource());
                    index.put(saved.resource(), entry);
                    entries.add(entry);
                }
                apply(entry, saved.count());
            }
        }
    }

    private Saved saved() {
        if (unweighed != null) {
            return new Saved(unweighed, unread);
        }
        List<ItemEntry> out = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            if (entry.count > 0) {
                out.add(new ItemEntry(entry.resource, entry.count));
            }
        }
        return new Saved(out, unread);
    }

    void attach(VaultStore owner) {
        store = owner;
    }

    private void dirty() {
        if (store != null) {
            store.setDirty();
        }
    }

    // ---- State ---------------------------------------------------------------------------------------------------------

    public long used() {
        ready();
        return used;
    }

    public long capacity() {
        return capacity;
    }

    public int kinds() {
        ready();
        return entries.size();
    }

    public int thrift() {
        return thrift;
    }

    public int radiance() {
        return radiance;
    }

    /** Where the vault stands now, or null while it is an item. */
    @Nullable
    public GlobalPos home() {
        return home;
    }

    /** The loaded block entity of the vault, if there is one. */
    @Nullable
    public HexVaultBlockEntity standing() {
        return standing;
    }

    /** What is held: a copy, safe to keep. */
    public List<Entry> entries() {
        ready();
        return List.copyOf(entries);
    }

    public long count(ItemResource resource) {
        ready();
        Entry entry = index.get(resource);
        return entry == null ? 0 : entry.count;
    }

    /** Each primal's share of what is held, in {@link Essence#PRIMALS} order; they add up to 1 unless nothing carries any. */
    public float[] shares() {
        ready();
        double total = Arrays.stream(primals).sum();
        float[] out = new float[primals.length];
        if (total > 0) {
            for (int i = 0; i < out.length; i++) {
                out[i] = (float) (primals[i] / total);
            }
        }
        return out;
    }

    /** The {@code limit} kinds that take up the most room, heaviest first. */
    public List<Entry> heaviest(int limit) {
        ready();
        return entries.stream().filter(entry -> entry.count > 0).sorted(Comparator.comparingLong(Entry::points).reversed()).limit(limit).toList();
    }

    /** 0 when empty, 1-15 by how full it is, as a comparator reads it. */
    public int signal() {
        ready();
        return used <= 0 ? 0 : (int) Math.min(15, 1 + used * 14 / Math.max(1, capacity));
    }

    /** Why one of this item would be refused now; null when it goes in. */
    @Nullable
    public Refusal refusal(ItemResource resource) {
        ready();
        if (!open) {
            return Refusal.ASLEEP;
        }
        if (!VaultRules.accepts(resource)) {
            return Refusal.CONTAINER;
        }
        Entry entry = index.get(resource);
        if (entry == null && entries.size() >= VaultRules.MAX_ENTRIES) {
            return Refusal.TOO_MANY_KINDS;
        }
        return used + (entry != null ? entry.weight : VaultRules.weight(resource)) > capacity ? Refusal.FULL : null;
    }

    // ---- What the block tells it ---------------------------------------------------------------------------------------

    /** The vault stands at {@code pos}. */
    public void placed(GlobalPos pos, HexVaultBlockEntity vault) {
        standing = vault;
        if (!pos.equals(home)) {
            home = pos;
            dirty();
        }
    }

    /** Its chunk went away: the items stay reachable, there is only no block to show them. */
    public void unloaded(HexVaultBlockEntity vault) {
        if (standing == vault) {
            standing = null;
        }
    }

    /** It was broken: the items wait for it to be placed again. */
    public void lifted(GlobalPos pos) {
        standing = null;
        if (pos.equals(home)) {
            home = null;
            dirty();
        }
    }

    /** What the block's cubes and runes make of it. A capacity below what is held only stops new items. */
    public void setPerks(long capacity, int thrift, int radiance) {
        if (this.capacity != capacity || this.thrift != thrift || this.radiance != radiance) {
            this.capacity = capacity;
            this.thrift = thrift;
            this.radiance = radiance;
            dirty();
        }
    }

    public void watch(Watcher watcher) {
        watchers.add(watcher);
    }

    public void unwatch(Watcher watcher) {
        watchers.remove(watcher);
    }

    /** Radiance: the points of new height not rewarded yet. */
    public long unpaidPoints() {
        return radiance > 0 ? unpaidPoints : 0;
    }

    public void paid(long points) {
        unpaidPoints = Math.max(0, unpaidPoints - points);
    }

    // ---- Moving items, outside any transaction -------------------------------------------------------------------------

    /** Puts as much of {@code stack} in as fits and shrinks it by that; returns how many went. */
    public int store(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int stored = insert(ItemResource.of(stack), stack.getCount(), transaction);
            transaction.commit();
            stack.shrink(stored);
            return stored;
        }
    }

    /** Takes up to {@code amount} out; returns what came, possibly less, possibly empty. */
    public ItemStack take(ItemResource resource, int amount) {
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = extract(resource, amount, transaction);
            transaction.commit();
            return taken > 0 ? resource.toStack(taken) : ItemStack.EMPTY;
        }
    }

    /** Mending: wears off {@code points} of damage on up to {@code limit} worn entries; true while any stay worn. */
    public boolean mend(int points, int limit) {
        ready();
        for (Entry entry : entries.stream().filter(entry -> entry.worn && entry.count > 0).limit(limit).toList()) {
            ItemStack mended = entry.resource.toStack(1);
            mended.setDamageValue(Math.max(0, mended.getDamageValue() - points));
            int moving = (int) Math.min(Integer.MAX_VALUE, entry.count);
            try (Transaction transaction = Transaction.openRoot()) {
                // What comes out must go back in whole, or nothing changes.
                int out = extract(entry.resource, moving, transaction);
                if (insert(ItemResource.of(mended), out, transaction) == out) {
                    transaction.commit();
                }
            }
        }
        return entries.stream().anyMatch(entry -> entry.worn && entry.count > 0);
    }

    // ---- The item handler ----------------------------------------------------------------------------------------------

    @Override
    public int size() {
        ready();
        return entries.size() < VaultRules.MAX_ENTRIES ? entries.size() + 1 : entries.size();
    }

    @Override
    public ItemResource getResource(int slot) {
        ready();
        return slot >= 0 && slot < entries.size() && entries.get(slot).count > 0 ? entries.get(slot).resource : ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int slot) {
        ready();
        return slot >= 0 && slot < entries.size() ? entries.get(slot).count : 0;
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        ready();
        if (resource.isEmpty()) {
            return capacity;
        }
        Entry entry = index.get(resource);
        return (entry == null ? 0 : entry.count) + Math.max(0, capacity - used) / (entry == null ? VaultRules.weight(resource) : entry.weight);
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        return VaultRules.accepts(resource);
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ready();
        return slot >= entries.size() || entries.get(slot).resource.equals(resource) ? insert(resource, amount, transaction) : 0;
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ready();
        if (amount == 0) {
            return 0;
        }
        if (refusal(resource) != null) {
            if (standing != null) {
                standing.refused();
            }
            return 0;
        }
        Entry entry = index.get(resource);
        int weight = entry != null ? entry.weight : VaultRules.weight(resource);
        int stored = (int) Math.min(amount, (capacity - used) / weight);
        updateSnapshots(transaction);
        if (entry == null) {
            entry = new Entry(resource);
            index.put(resource, entry);
            entries.add(entry);
        }
        change(entry, stored);
        return stored;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
        ready();
        return slot >= 0 && slot < entries.size() && entries.get(slot).resource.equals(resource) ? extract(resource, amount, transaction) : 0;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ready();
        Entry entry = index.get(resource);
        if (entry == null || entry.count <= 0 || amount == 0) {
            return 0;
        }
        int taken = (int) Math.min(amount, entry.count);
        updateSnapshots(transaction);
        change(entry, -taken);
        return taken;
    }

    private void change(Entry entry, long delta) {
        apply(entry, delta);
        log.add(new Change(entry, delta));
    }

    private void apply(Entry entry, long delta) {
        entry.count += delta;
        used += delta * entry.weight;
        for (int i = 0; i < primals.length; i++) {
            primals[i] += (double) delta * entry.primals[i];
        }
    }

    @Override
    protected Integer createSnapshot() {
        return log.size();
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        for (int i = log.size() - 1; i >= snapshot; i--) {
            Change change = log.remove(i);
            apply(change.entry(), -change.delta());
        }
        if (log.isEmpty()) {
            sweep();
        }
    }

    @Override
    protected void onRootCommit(Integer originalState) {
        Set<Entry> touched = new LinkedHashSet<>();
        log.forEach(change -> touched.add(change.entry()));
        log.clear();
        sweep();
        if (used > peak) {
            unpaidPoints += used - peak;
            peak = used;
        }
        dirty();
        for (Watcher watcher : List.copyOf(watchers)) {
            watcher.changed(touched);
        }
        if (standing != null) {
            standing.contentsChanged();
        }
    }

    /** Entries left empty leave the list, now that no one is walking it. */
    private void sweep() {
        entries.removeIf(entry -> {
            if (entry.count <= 0) {
                index.remove(entry.resource);
                return true;
            }
            return false;
        });
    }
}
