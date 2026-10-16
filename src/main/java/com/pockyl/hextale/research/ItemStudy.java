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
 * Studying an item: "echoes". The item lies in the centre of the parchment; nine strings run from it to the corners,
 * one per element. Each element the item carries hides on its string, as many sections out as it is strong.
 * <p>
 * The researcher strikes the parchment anywhere: a ripple runs out from the struck section and stops at the nearest
 * element still hidden. Everything inside the ripple is empty; somewhere on its edge an element sings. Striking an
 * element's own section reveals it, and a revealed element goes quiet, so later ripples run on to the others. Strikes
 * are limited: three plus three per element.
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy).
 */
public final class ItemStudy {
    /** Strings start two sections out: near the centre nine directions would share six neighbours. */
    private static final int FIRST_STEP = 2;
    private static final int LAST_STEP = 12;
    private static final int[][] STRINGS = new int[HexGrid.RING.length][];
    private static final int[] STRING_OF = new int[HexGrid.COUNT];
    private static final int[] INDEX_OF = new int[HexGrid.COUNT];

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
                int[] offset = ItemEssences.of(1, HexGrid.RING[i], n).offset(false);
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
        Arrays.fill(STRING_OF, -1);
        Arrays.fill(INDEX_OF, -1);
        for (int i = 0; i < HexGrid.RING.length; i++) {
            STRINGS[i] = strings.get(i).stream().mapToInt(Integer::intValue).toArray();
            for (int k = 0; k < STRINGS[i].length; k++) {
                STRING_OF[STRINGS[i][k]] = i;
                INDEX_OF[STRINGS[i][k]] = k;
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

    /** The sections of an element's string, from the centre out. */
    public static int[] string(int ringIndex) {
        return STRINGS[ringIndex];
    }

    /** Which string a section belongs to, or -1. */
    public static int stringOf(int cell) {
        return STRING_OF[cell];
    }

    /** The section's place on its string (0 = nearest the item), or -1. */
    public static int indexOf(int cell) {
        return INDEX_OF[cell];
    }

    public static int ringIndex(Essence element) {
        for (int i = 0; i < HexGrid.RING.length; i++) {
            if (HexGrid.RING[i] == element) {
                return i;
            }
        }
        return -1;
    }

    /** The section where an element of this strength hides (the last of its string for strengths beyond it). */
    public static int hiding(Essence element, int strength) {
        int[] string = STRINGS[ringIndex(element)];
        return string[Math.min(strength, string.length) - 1];
    }

    public static int maxStrikes(ItemEssences elements) {
        return 3 + 3 * elements.essences().size();
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

    public boolean isRevealed(Essence element) {
        return isRevealed(ringIndex(element));
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
        for (Map.Entry<Essence, Integer> entry : elements.essences().entrySet()) {
            if (isRevealed(entry.getKey()) && HexGrid.steps(echo.cell(), hiding(entry.getKey(), entry.getValue())) == echo.radius()) {
                return false;
            }
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
        for (Essence element : elements.essences().keySet()) {
            if (!isRevealed(element)) {
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
        for (Map.Entry<Essence, Integer> entry : elements.essences().entrySet()) {
            int string = ringIndex(entry.getKey());
            if (isRevealed(string)) {
                continue;
            }
            int d = HexGrid.steps(cell, hiding(entry.getKey(), entry.getValue()));
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
