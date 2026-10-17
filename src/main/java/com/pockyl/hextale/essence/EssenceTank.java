package com.pockyl.hextale.essence;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Amounts are millibuckets. A vessel inside a machine: holds up to {@code capacity} drops of essence; whatever is poured in mixes with what is
 * already there.
 */
public final class EssenceTank {
    private final int capacity;
    private EssenceMix contents = EssenceMix.EMPTY;

    public EssenceTank(int capacity) {
        this.capacity = capacity;
    }

    public int capacity() {
        return capacity;
    }

    public EssenceMix contents() {
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

    public void set(EssenceMix essence) {
        contents = essence;
    }

    /**
     * Pours in as much of {@code offer} as fits (keeping its proportions).
     *
     * @return the drops taken
     */
    public int fill(EssenceMix offer, boolean simulate) {
        EssenceMix taken = offer.part(Math.min(space(), offer.total()));
        if (!simulate) {
            contents = contents.plus(taken);
        }
        return taken.total();
    }

    /** Takes out up to {@code amount} drops, in the tank's proportions. */
    public EssenceMix drain(int amount, boolean simulate) {
        EssenceMix part = contents.part(amount);
        if (!simulate) {
            contents = contents.minus(part);
        }
        return part;
    }

    /** Takes exactly these drops out (they must be there). */
    public void remove(EssenceMix essence) {
        contents = contents.minus(essence);
    }

    public void save(ValueOutput output, String key) {
        output.store(key, EssenceMix.CODEC, contents);
    }

    public void load(ValueInput input, String key) {
        contents = input.read(key, EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
    }
}
