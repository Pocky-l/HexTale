package com.pockyl.hextale.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
 * The researcher puts up to three tuning marks on strings and listens: a mark on the exact spot rings gold and reveals
 * the element; a mark on a string the item carries hums and points nearer or farther; a mark on a silent string stays
 * silent. Listening is limited, so it pays to cover several strings at once and to halve the distance each time.
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy).
 */
public final class ItemStudy {
    public static final int MAX_MARKS = 3;
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

    /** What a mark heard. */
    public enum Echo {
        EXACT,
        NEARER,
        FARTHER,
        SILENT
    }

    @Nullable
    private Item item;
    /** Per string (ring order): index of the mark on it, or -1. */
    private final int[] marks = new int[HexGrid.RING.length];
    /** What each listened section answered, in order. */
    private final Map<Integer, Echo> echoes = new LinkedHashMap<>();
    /** Revealed elements, bit per ring index. */
    private int revealed;
    private int listens;
    private int maxListens;
    private boolean started;

    public ItemStudy() {
        Arrays.fill(marks, -1);
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

    private static int ringIndex(SimpleRune element) {
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

    /** A fresh study of this item: as many listens as elements plus five. */
    public void begin(Item item, ItemElements elements) {
        this.item = item;
        Arrays.fill(marks, -1);
        echoes.clear();
        revealed = 0;
        listens = 0;
        maxListens = 5 + elements.elements().size();
        started = false;
    }

    public void clear() {
        item = null;
        Arrays.fill(marks, -1);
        echoes.clear();
        revealed = 0;
        listens = 0;
        maxListens = 0;
        started = false;
    }

    /** The first listen uses up one of the item: the study has begun. */
    public boolean started() {
        return started;
    }

    public int listens() {
        return listens;
    }

    public int maxListens() {
        return maxListens;
    }

    public int listensLeft() {
        return maxListens - listens;
    }

    public boolean isRevealed(SimpleRune element) {
        return (revealed & 1 << ringIndex(element)) != 0;
    }

    public int markCount() {
        int count = 0;
        for (int mark : marks) {
            if (mark >= 0) {
                count++;
            }
        }
        return count;
    }

    /** The mark on a string (ring index), or -1. */
    public int mark(int ringIndex) {
        return marks[ringIndex];
    }

    public Map<Integer, Echo> echoes() {
        return echoes;
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
        return listens >= maxListens && !won(elements);
    }

    public boolean isActive(ItemElements elements) {
        return item != null && !won(elements) && !lost(elements);
    }

    /**
     * Puts a mark on a section, moves the string's mark there, or takes it off when it is already there.
     *
     * @return whether anything changed
     */
    public boolean toggle(int cell, ItemElements elements) {
        if (!isActive(elements)) {
            return false;
        }
        int string = stringOf(cell);
        if (string < 0 || (revealed & 1 << string) != 0) {
            return false;
        }
        int index = indexOn(string, cell);
        if (marks[string] == index) {
            marks[string] = -1;
            return true;
        }
        if (marks[string] < 0 && markCount() >= MAX_MARKS) {
            return false;
        }
        marks[string] = index;
        return true;
    }

    private static int indexOn(int string, int cell) {
        int[] cells = STRINGS[string];
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == cell) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Listens to the marks: every mark answers, exact ones reveal their element.
     *
     * @return the echoes of this listen by section, or null when nothing could be heard
     */
    @Nullable
    public Map<Integer, Echo> listen(ItemElements elements) {
        if (!isActive(elements) || markCount() == 0) {
            return null;
        }
        started = true;
        listens++;
        Map<Integer, Echo> heard = new LinkedHashMap<>();
        for (int string = 0; string < marks.length; string++) {
            int index = marks[string];
            if (index < 0) {
                continue;
            }
            SimpleRune element = HexGrid.RING[string];
            Integer strength = elements.elements().get(element);
            Echo echo;
            if (strength == null) {
                echo = Echo.SILENT;
            } else {
                int target = answer(element, strength);
                echo = index == target ? Echo.EXACT : index < target ? Echo.FARTHER : Echo.NEARER;
            }
            if (echo == Echo.EXACT) {
                revealed |= 1 << string;
            }
            int cell = STRINGS[string][index];
            echoes.remove(cell);
            echoes.put(cell, echo);
            heard.put(cell, echo);
            marks[string] = -1;
        }
        return heard;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (item != null) {
            tag.putString("item", BuiltInRegistries.ITEM.getKey(item).toString());
        }
        tag.putIntArray("marks", marks);
        tag.putInt("revealed", revealed);
        tag.putInt("listens", listens);
        tag.putInt("max_listens", maxListens);
        tag.putBoolean("started", started);
        ListTag list = new ListTag();
        echoes.forEach((cell, echo) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("cell", cell);
            entry.putInt("echo", echo.ordinal());
            list.add(entry);
        });
        tag.put("echoes", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        ResourceLocation id = tag.contains("item") ? ResourceLocation.tryParse(tag.getString("item")) : null;
        item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        Arrays.fill(marks, -1);
        int[] saved = tag.getIntArray("marks");
        System.arraycopy(saved, 0, marks, 0, Math.min(saved.length, marks.length));
        revealed = tag.getInt("revealed");
        listens = tag.getInt("listens");
        maxListens = tag.getInt("max_listens");
        started = tag.getBoolean("started");
        echoes.clear();
        for (Tag element : tag.getList("echoes", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            int echo = entry.getInt("echo");
            if (echo >= 0 && echo < Echo.values().length) {
                echoes.put(entry.getInt("cell"), Echo.values()[echo]);
            }
        }
    }
}
