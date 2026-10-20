package com.pockyl.hextale.chem;

/**
 * A machine that stands on a Chem Burner and takes its heat: the burner burns fuel only while the machine above has
 * work for it (a crucible with liquid, an alembic with something to boil, a separator drawing an essence out, a furnace
 * with something to melt).
 */
public interface HeatUser {
    boolean wantsHeat();
}
