package com.pockyl.hextale.essence;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A vessel inside a machine: holds up to {@code capacity} drops of essence; whatever is poured in mixes with what is
 * already there.
 */
public final class EssenceTank {
    private final int capacity;
    private Essence contents = Essence.EMPTY;

    public EssenceTank(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public Essence contents() {
        return contents;
    }

    public int amount() {
        return contents.total();
    }

    public int space() {
        return capacity - contents.total();
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public void set(Essence essence) {
        contents = essence;
    }

    /**
     * Pours in as much of {@code offer} as fits (keeping its proportions).
     *
     * @return the drops taken
     */
    public int fill(Essence offer, boolean simulate) {
        Essence taken = offer.part(Math.min(space(), offer.total()));
        if (!simulate) {
            contents = contents.plus(taken);
        }
        return taken.total();
    }

    /** Takes out up to {@code amount} drops, in the tank's proportions. */
    public Essence drain(int amount, boolean simulate) {
        Essence part = contents.part(amount);
        if (!simulate) {
            contents = contents.minus(part);
        }
        return part;
    }

    /** Takes exactly these drops out (they must be there). */
    public void remove(Essence essence) {
        contents = contents.minus(essence);
    }

    public void save(ValueOutput output, String key) {
        output.putIntArray(key, contents.drops());
        output.putBoolean(key + "_unknown", contents.isUnknown());
    }

    public void load(ValueInput input, String key) {
        int[] saved = input.getIntArray(key).orElse(new int[0]);
        int[] drops = new int[Essence.EMPTY.drops().length];
        System.arraycopy(saved, 0, drops, 0, Math.min(saved.length, drops.length));
        contents = Essence.of(drops, input.getBooleanOr(key + "_unknown", false));
    }
}
