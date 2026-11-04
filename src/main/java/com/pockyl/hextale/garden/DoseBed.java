package com.pockyl.hextale.garden;

/** A tank of doses of one kind that vials, bottles and sprinklers pour into: a Chem Planter, an Essence Sprinkler. */
public interface DoseBed {
    /** The kind of dose in it ({@link Dose#NONE} when empty). */
    Dose dose();

    int charges();

    /** How many doses of this kind still fit (0 when another kind is in it). */
    int room(Dose kind);

    /** Pours doses in; returns how many it took. */
    int pour(Dose kind, int count);
}
