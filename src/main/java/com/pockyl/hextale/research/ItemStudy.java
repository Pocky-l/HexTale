package com.pockyl.hextale.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Studying an item: "resonance". The item lies in the centre of the parchment; nine strings run from it to the
 * corners, one per element. Each element the item carries hides on its string, as many sections out as it is strong.
 * <p>
 * The researcher places guess marks, one click at a time, and every mark answers at once: exact (the element is
 * revealed), farther or nearer (the sections it rules out go dark), or silent (the whole string goes dark). A mark also
 * stirs the two neighbouring strings: they shimmer when they hold an element and go dark when they do not, so marks
 * spread around the ring find the elements fastest. A string with a single section left reveals itself for free.
 * Guesses are limited: three plus two per element.
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy).
 */
public final class ItemStudy {
    /** Strings start two sections out: near the centre nine directions would share six neighbours. */
    private static final int FIRST_STEP = 2;
    private static final int LAST_STEP = 12;
    private static final int[][] STRINGS = new int[HexGrid.RING.length][];

    static {
        boolean[] taken = new boolean[HexGrid.COUNT];
        taken[HexGrid.CENTRE] = true;
        List<List<Integer>> strings = new ArrayList<>();
        for (int i = 0; i < HexGrid.RING.length; i++) {
            strings.add(new ArrayList<>());
        }
        // Grow all strings together, step by step, so none of them steals the others' sections.
        boolean[] alive = new boolean[HexGrid.RING.length];
        Arrays.fill(alive, true);
        for (int n = FIRST_STEP; n <= LAST_STEP; n++) {
            for (int i = 0; i < HexGrid.RING.length; i++) {
                if (!alive[i]) {
                    continue;
                }
                int[] offset = ItemElements.of(1, HexGrid.RING[i], n).offset(false);
                int cell = HexGrid.index(HexGrid.q(HexGrid.CENTRE) + offset[0], HexGrid.r(HexGrid.CENTRE) + offset[1]);
                List<Integer> string = strings.get(i);
                if (cell >= 0 && !string.isEmpty() && string.get(string.size() - 1) == cell) {
                    continue;
                }
                if (cell < 0 || taken[cell]) {
                    alive[i] = false;
                    continue;
                }
                taken[cell] = true;
                string.add(cell);
            }
        }
        for (int i = 0; i < HexGrid.RING.length; i++) {
            STRINGS[i] = strings.get(i).stream().mapToInt(Integer::intValue).toArray();
        }
    }

    /** What a guess mark heard. */
    public enum Echo {
        EXACT,
        NEARER,
        FARTHER,
        SILENT
    }

    /** What is known about a whole string. */
    public enum Presence {
        UNKNOWN,
        /** It shimmered: the item has this element. */
        PRESENT,
        /** It went dark: the item has no such element. */
        ABSENT
    }

    /** One guess and everything it taught, for messages and effects. */
    public record Answer(int cell, int string, Echo echo, List<Integer> stirred, List<Integer> selfRevealed) {
    }

    @Nullable
    private Item item;
    private final Presence[] presence = new Presence[HexGrid.RING.length];
    /** Per string: the first and last section the element can still be on. */
    private final int[] low = new int[HexGrid.RING.length];
    private final int[] high = new int[HexGrid.RING.length];
    /** Revealed elements, bit per ring index. */
    private int revealed;
    /** Every guess mark and its answer, in order. */
    private final Map<Integer, Echo> marks = new LinkedHashMap<>();
    private int guesses;
    private int maxGuesses;
    private boolean started;

    public ItemStudy() {
        clear();
    }

    /** The sections of an element's string, from the centre out. */
    public static int[] string(int ringIndex) {
        return STRINGS[ringIndex];
    }

    /** Which string a section belongs to, or -1. */
    public static int stringOf(int cell) {
        for (int i = 0; i < STRINGS.length; i++) {
            for (int c : STRINGS[i]) {
                if (c == cell) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** The section's place on its string (0 = nearest the item), or -1. */
    public static int indexOf(int cell) {
        int string = stringOf(cell);
        if (string < 0) {
            return -1;
        }
        int[] cells = STRINGS[string];
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == cell) {
                return i;
            }
        }
        return -1;
    }

    public static int ringIndex(SimpleRune element) {
        for (int i = 0; i < HexGrid.RING.length; i++) {
            if (HexGrid.RING[i] == element) {
                return i;
            }
        }
        return -1;
    }

    /** Where an element of this strength hides on its string (the last section for strengths beyond it). */
    public static int answer(SimpleRune element, int strength) {
        int[] string = STRINGS[ringIndex(element)];
        return Math.min(strength, string.length) - 1;
    }

    @Nullable
    public Item item() {
        return item;
    }

    /** A fresh study of this item. */
    public void begin(Item item, ItemElements elements) {
        clear();
        this.item = item;
        maxGuesses = 3 + 2 * elements.elements().size();
    }

    public void clear() {
        item = null;
        Arrays.fill(presence, Presence.UNKNOWN);
        Arrays.fill(low, 0);
        for (int i = 0; i < high.length; i++) {
            high[i] = STRINGS[i].length - 1;
        }
        revealed = 0;
        marks.clear();
        guesses = 0;
        maxGuesses = 0;
        started = false;
    }

    /** The first guess uses up one of the item: the study has begun. */
    public boolean started() {
        return started;
    }

    public int guesses() {
        return guesses;
    }

    public int maxGuesses() {
        return maxGuesses;
    }

    public int guessesLeft() {
        return maxGuesses - guesses;
    }

    public boolean isRevealed(int string) {
        return (revealed & 1 << string) != 0;
    }

    public boolean isRevealed(SimpleRune element) {
        return isRevealed(ringIndex(element));
    }

    public Presence presence(int string) {
        return isRevealed(string) ? Presence.PRESENT : presence[string];
    }

    public Map<Integer, Echo> marks() {
        return marks;
    }

    /** Whether the element of this string could still be on this section. */
    public boolean possible(int string, int index) {
        return presence[string] != Presence.ABSENT && !isRevealed(string) && index >= low[string] && index <= high[string];
    }

    /** Whether a guess on this section would teach anything (and cost a guess). */
    public boolean canGuess(int cell, ItemElements elements) {
        int string = stringOf(cell);
        return isActive(elements) && string >= 0 && !marks.containsKey(cell) && possible(string, indexOf(cell));
    }

    public boolean won(ItemElements elements) {
        for (SimpleRune element : elements.elements().keySet()) {
            if (!isRevealed(element)) {
                return false;
            }
        }
        return true;
    }

    public boolean lost(ItemElements elements) {
        return guesses >= maxGuesses && !won(elements);
    }

    public boolean isActive(ItemElements elements) {
        return item != null && !won(elements) && !lost(elements);
    }

    /** Places a guess mark: it answers at once and stirs the neighbouring strings. Null when the guess is not allowed. */
    @Nullable
    public Answer guess(int cell, ItemElements elements) {
        if (!canGuess(cell, elements)) {
            return null;
        }
        started = true;
        guesses++;
        int string = stringOf(cell);
        int index = indexOf(cell);
        SimpleRune element = HexGrid.RING[string];
        Integer strength = elements.elements().get(element);
        Echo echo;
        if (strength == null) {
            echo = Echo.SILENT;
            presence[string] = Presence.ABSENT;
        } else {
            presence[string] = Presence.PRESENT;
            int target = answer(element, strength);
            if (index == target) {
                echo = Echo.EXACT;
                revealed |= 1 << string;
            } else if (index < target) {
                echo = Echo.FARTHER;
                low[string] = Math.max(low[string], index + 1);
            } else {
                echo = Echo.NEARER;
                high[string] = Math.min(high[string], index - 1);
            }
        }
        marks.put(cell, echo);
        List<Integer> stirred = new ArrayList<>();
        for (int side : new int[]{-1, 1}) {
            int neighbour = Math.floorMod(string + side, STRINGS.length);
            if (presence[neighbour] == Presence.UNKNOWN && !isRevealed(neighbour)) {
                presence[neighbour] = elements.elements().containsKey(HexGrid.RING[neighbour]) ? Presence.PRESENT : Presence.ABSENT;
                stirred.add(neighbour);
            }
        }
        return new Answer(cell, string, echo, stirred, revealCertain());
    }

    /** Strings known to hold an element with one section left reveal themselves. */
    private List<Integer> revealCertain() {
        List<Integer> out = new ArrayList<>();
        for (int string = 0; string < STRINGS.length; string++) {
            if (presence[string] == Presence.PRESENT && !isRevealed(string) && low[string] == high[string]) {
                revealed |= 1 << string;
                marks.put(STRINGS[string][low[string]], Echo.EXACT);
                out.add(string);
            }
        }
        return out;
    }

    public void save(ValueOutput tag) {
        if (item != null) {
            tag.putString("item", BuiltInRegistries.ITEM.getKey(item).toString());
        }
        int[] known = new int[presence.length];
        for (int i = 0; i < presence.length; i++) {
            known[i] = presence[i].ordinal();
        }
        tag.putIntArray("presence", known);
        tag.putIntArray("low", low);
        tag.putIntArray("high", high);
        tag.putInt("revealed", revealed);
        tag.putInt("guesses", guesses);
        tag.putInt("max_guesses", maxGuesses);
        tag.putBoolean("started", started);
        ValueOutput.ValueOutputList list = tag.childrenList("marks");
        marks.forEach((cell, echo) -> {
            ValueOutput entry = list.addChild();
            entry.putInt("cell", cell);
            entry.putInt("echo", echo.ordinal());
        });
    }

    public void load(ValueInput tag) {
        clear();
        Identifier id = tag.getString("item").map(Identifier::tryParse).orElse(null);
        item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        int[] known = tag.getIntArray("presence").orElse(new int[0]);
        for (int i = 0; i < Math.min(known.length, presence.length); i++) {
            presence[i] = Presence.values()[Math.floorMod(known[i], Presence.values().length)];
        }
        int[] savedLow = tag.getIntArray("low").orElse(new int[0]);
        int[] savedHigh = tag.getIntArray("high").orElse(new int[0]);
        for (int i = 0; i < STRINGS.length; i++) {
            if (i < savedLow.length) {
                low[i] = savedLow[i];
            }
            if (i < savedHigh.length) {
                high[i] = savedHigh[i];
            }
        }
        revealed = tag.getIntOr("revealed", 0);
        guesses = tag.getIntOr("guesses", 0);
        maxGuesses = tag.getIntOr("max_guesses", 0);
        started = tag.getBooleanOr("started", false);
        for (ValueInput entry : tag.childrenListOrEmpty("marks")) {
            int echo = entry.getIntOr("echo", -1);
            if (echo >= 0 && echo < Echo.values().length) {
                marks.put(entry.getIntOr("cell", 0), Echo.values()[echo]);
            }
        }
    }
}
