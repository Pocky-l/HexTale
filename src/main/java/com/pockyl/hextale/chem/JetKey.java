package com.pockyl.hextale.chem;

/**
 * What the jump key means in the air on Chem Jets, for the player at the keys. The jets light on a hold; with a glider on, a
 * tap opens the glider and only a hold past {@link #LIGHT_TICKS} lights the jets. Without a glider a press lights them at
 * once. Lives outside the client code so that a game test can walk through it.
 */
public final class JetKey {
    /** Ticks in the air jump must stay held after a take-off or a boots' jump before the jets light: a plain jump burns nothing. */
    public static final int LIGHT_TICKS = 6;
    private int lightIn = LIGHT_TICKS;
    private boolean tapping;

    /**
     * One tick of the key; true on the tick a tap ends and the glider should open.
     *
     * @param spent  the press belongs to someone else, or the player stands: the count starts over
     * @param glider a press now may turn out to be a tap for a glider
     */
    public boolean update(boolean down, boolean pressed, boolean spent, boolean glider) {
        if (spent) {
            reset();
        } else if (pressed) {
            tapping = glider;
            lightIn = glider ? LIGHT_TICKS : 0;
        } else if (down) {
            lightIn = Math.max(0, lightIn - 1);
            tapping &= lightIn > 0;
        } else if (tapping) {
            tapping = false;
            return true;
        }
        return false;
    }

    /** Whether a held key fires the jets now. */
    public boolean lit() {
        return lightIn == 0;
    }

    /** The count starts over; a tap under way is forgotten. */
    public void reset() {
        lightIn = LIGHT_TICKS;
        tapping = false;
    }
}
