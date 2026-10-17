package com.pockyl.hextale.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Studying an item: "echoes". The item lies in the centre of the hexagonal parchment; six strings run from it to the
 * corners, one per direction. Each essence the item carries (up to six, strongest first) hides on its own string, the
 * farther out the more of it there is.
 * <p>
 * The researcher strikes the parchment anywhere: a ripple runs out from the struck section and stops at the nearest
 * essence still hidden. Everything inside the ripple is empty; somewhere on its edge an essence sings. Striking an
 * essence's own section reveals it, and a revealed essence goes quiet, so later ripples run on to the others. Strikes
 * are limited: three plus three per essence.
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy).
 */
public final class ItemStudy {
    /** Strings start two sections out: right by the item the six directions crowd together. */
    private static final int FIRST_STEP = 2;
    public static final int STRINGS_COUNT = HexGrid.DIRECTIONS.length;
    private static final int[][] STRINGS = new int[STRINGS_COUNT][];
    private static final int[] STRING_OF = new int[HexGrid.COUNT];
    private static final int[] INDEX_OF = new int[HexGrid.COUNT];

    static {
        Arrays.fill(STRING_OF, -1);
        Arrays.fill(INDEX_OF, -1);
        for (int d = 0; d < STRINGS_COUNT; d++) {
            List<Integer> string = new ArrayList<>();
            int cell = HexGrid.CENTRE;
            for (int n = 1; n <= HexGrid.RADIUS; n++) {
                cell = HexGrid.step(cell, d);
                if (n >= FIRST_STEP && cell >= 0) {
                    string.add(cell);
                }
            }
            STRINGS[d] = string.stream().mapToInt(Integer::intValue).toArray();
            for (int k = 0; k < STRINGS[d].length; k++) {
                STRING_OF[STRINGS[d][k]] = d;
                INDEX_OF[STRINGS[d][k]] = k;
            }
        }
    }

    /**
     * A strike and its ripple.
     *
     * @param radius sections from the struck one to the nearest hidden element; 0 = found it
     */
    public record Echo(int cell, int radius) {
    }

    @Nullable
    private Item item;
    /** Revealed elements, bit per ring index. */
    private int revealed;
    private final List<Echo> echoes = new ArrayList<>();
    private int maxStrikes;

    public ItemStudy() {
        clear();
    }

    /** The sections of a string, from the centre out. */
    public static int[] string(int string) {
        return STRINGS[string];
    }

    /** Which string a section belongs to, or -1. */
    public static int stringOf(int cell) {
        return STRING_OF[cell];
    }

    /** The section's place on its string (0 = nearest the item), or -1. */
    public static int indexOf(int cell) {
        return INDEX_OF[cell];
    }

    /** The string an essence of the item hides on (its place among the item's essences, strongest first), or -1. */
    public static int stringFor(ItemEssences essences, Essence essence) {
        int i = 0;
        for (Essence e : essences.essences().keySet()) {
            if (e == essence) {
                return i < STRINGS_COUNT ? i : -1;
            }
            i++;
        }
        return -1;
    }

    /** The essence hiding on a string, or null when the item has fewer essences. */
    @Nullable
    public static Essence essenceOn(ItemEssences essences, int string) {
        int i = 0;
        for (Essence e : essences.essences().keySet()) {
            if (i++ == string) {
                return e;
            }
        }
        return null;
    }

    /** The section where an essence of this strength hides on its string: two units of essence per section, the rim at most. */
    public static int hiding(int string, int strength) {
        int[] cells = STRINGS[string];
        return cells[Math.max(0, Math.min(cells.length, (strength + 1) / 2) - 1)];
    }

    public static int maxStrikes(ItemEssences elements) {
        return 3 + 3 * Math.min(STRINGS_COUNT, elements.essences().size());
    }

    @Nullable
    public Item item() {
        return item;
    }

    /** A fresh study of this item. */
    public void begin(Item item, ItemEssences elements) {
        clear();
        this.item = item;
        maxStrikes = maxStrikes(elements);
    }

    public void clear() {
        item = null;
        revealed = 0;
        echoes.clear();
        maxStrikes = 0;
    }

    /** The first strike uses up one of the item: the study has begun. */
    public boolean started() {
        return !echoes.isEmpty();
    }

    public List<Echo> echoes() {
        return echoes;
    }

    public int strikes() {
        return echoes.size();
    }

    public int maxStrikes() {
        return maxStrikes;
    }

    public int strikesLeft() {
        return maxStrikes - echoes.size();
    }

    public boolean isRevealed(int string) {
        return (revealed & 1 << string) != 0;
    }

    public boolean isRevealed(ItemEssences essences, Essence essence) {
        int string = stringFor(essences, essence);
        return string >= 0 && isRevealed(string);
    }

    /** Whether the section was struck already. */
    public boolean struck(int cell) {
        for (Echo echo : echoes) {
            if (echo.cell() == cell) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a hidden element could still be on this section, from what the ripples showed: it lies on a string whose
     * element is not revealed yet and inside no ripple.
     */
    public boolean possible(int cell) {
        int string = stringOf(cell);
        if (string < 0 || isRevealed(string)) {
            return false;
        }
        for (Echo echo : echoes) {
            if (HexGrid.steps(echo.cell(), cell) < echo.radius()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether an echo is still a clue: its ripple stopped at an element that has not been revealed yet (no revealed
     * element sits on its rim).
     */
    public boolean open(Echo echo, ItemEssences elements) {
        if (echo.radius() == 0) {
            return false;
        }
        int string = 0;
        for (Map.Entry<Essence, Integer> entry : elements.essences().entrySet()) {
            if (string < STRINGS_COUNT && isRevealed(string) && HexGrid.steps(echo.cell(), hiding(string, entry.getValue())) == echo.radius()) {
                return false;
            }
            string++;
        }
        return true;
    }

    /**
     * How many open echoes point at this section: it is possible and lies on their rims. Revealing elements only closes
     * echoes, so this never needs the hidden answer beyond what the table shows.
     */
    public int heat(int cell, ItemEssences elements) {
        if (!possible(cell)) {
            return 0;
        }
        int heat = 0;
        for (Echo echo : echoes) {
            if (HexGrid.steps(echo.cell(), cell) == echo.radius() && open(echo, elements)) {
                heat++;
            }
        }
        return heat;
    }

    public boolean canStrike(int cell, ItemEssences elements) {
        return isActive(elements) && cell >= 0 && cell < HexGrid.COUNT && cell != HexGrid.CENTRE && !struck(cell);
    }

    public boolean won(ItemEssences elements) {
        int strings = Math.min(STRINGS_COUNT, elements.essences().size());
        for (int string = 0; string < strings; string++) {
            if (!isRevealed(string)) {
                return false;
            }
        }
        return true;
    }

    public boolean lost(ItemEssences elements) {
        return echoes.size() >= maxStrikes && !won(elements);
    }

    public boolean isActive(ItemEssences elements) {
        return item != null && !won(elements) && !lost(elements);
    }

    /** Strikes a section: the ripple stops at the nearest hidden element (and reveals it when struck exactly). */
    @Nullable
    public Echo strike(int cell, ItemEssences elements) {
        if (!canStrike(cell, elements)) {
            return null;
        }
        int radius = Integer.MAX_VALUE;
        int found = -1;
        int string = -1;
        for (Map.Entry<Essence, Integer> entry : elements.essences().entrySet()) {
            string++;
            if (string >= STRINGS_COUNT || isRevealed(string)) {
                continue;
            }
            int d = HexGrid.steps(cell, hiding(string, entry.getValue()));
            if (d < radius) {
                radius = d;
                found = d == 0 ? string : -1;
            }
        }
        if (found >= 0) {
            revealed |= 1 << found;
        }
        Echo echo = new Echo(cell, radius);
        echoes.add(echo);
        return echo;
    }

    public void save(ValueOutput tag) {
        if (item != null) {
            tag.putString("item", BuiltInRegistries.ITEM.getKey(item).toString());
        }
        tag.putInt("revealed", revealed);
        tag.putInt("max_strikes", maxStrikes);
        int[] flat = new int[echoes.size() * 2];
        for (int i = 0; i < echoes.size(); i++) {
            flat[i * 2] = echoes.get(i).cell();
            flat[i * 2 + 1] = echoes.get(i).radius();
        }
        tag.putIntArray("echoes", flat);
    }

    public void load(ValueInput tag) {
        clear();
        Identifier id = tag.getString("item").map(Identifier::tryParse).orElse(null);
        item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        revealed = tag.getIntOr("revealed", 0);
        maxStrikes = tag.getIntOr("max_strikes", 0);
        int[] flat = tag.getIntArray("echoes").orElse(new int[0]);
        for (int i = 0; i + 1 < flat.length; i += 2) {
            if (flat[i] >= 0 && flat[i] < HexGrid.COUNT) {
                echoes.add(new Echo(flat[i], flat[i + 1]));
            }
        }
    }
}
