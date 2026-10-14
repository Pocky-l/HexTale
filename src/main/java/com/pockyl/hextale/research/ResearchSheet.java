package com.pockyl.hextale.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * One attempt at a rune = one sheet of parchment. Items write symbols in a chain: each one starts where the previous
 * one landed (the first at the orb in the centre). A symbol on a point lets its resource be left there at any time; a
 * filled point dissolves every dark rune of its element. The rune is learned when every point is filled; the sheet is
 * lost when it is full while nothing covered is left to fill.
 * <p>
 * Pure rules shared by the table (server) and its screen (client copy for previews). Tool strengths are passed in.
 */
public final class ResearchSheet {
    @Nullable
    private ComplexRune rune;
    private boolean open;
    private boolean over;
    private final List<Mark> marks = new ArrayList<>();
    private int from = HexGrid.CENTRE;
    private int cap;
    private int filled;
    private int scrapes;
    private int jumps;

    /**
     * A written symbol.
     *
     * @param cells the sections its ink line crossed, ending where it landed
     */
    public record Mark(int cell, SimpleRune element, int from, int[] cells, ResourceLocation item) {
    }

    public enum Result {
        OK,
        NOT_ALLOWED,
        /** The sheet is full and no covered point is left: the attempt failed. */
        FAILED,
        /** Every point is filled: the rune is learned. */
        COMPLETE
    }

    @Nullable
    public ComplexRune rune() {
        return rune;
    }

    /** Chooses the rune to study; drops any sheet in progress. */
    public void select(ComplexRune rune) {
        this.rune = rune;
        reset(false, 0);
    }

    /** Takes the rune off the table. */
    public void clear() {
        this.rune = null;
        reset(false, 0);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isOver() {
        return over;
    }

    /** Symbols can be written or resources left. */
    public boolean isActive() {
        return open && !over;
    }

    public List<Mark> marks() {
        return marks;
    }

    public int from() {
        return from;
    }

    public int used() {
        return marks.size();
    }

    public int cap() {
        return cap;
    }

    public int filledMask() {
        return filled;
    }

    public boolean isFilled(int point) {
        return (filled & 1 << point) != 0;
    }

    public int scrapes() {
        return scrapes;
    }

    public int jumps() {
        return jumps;
    }

    /** Starts a new sheet holding {@code cap} symbols. */
    public void begin(int cap) {
        reset(true, cap);
    }

    /** Burns the sheet: no attempt in progress. */
    public void burn() {
        reset(false, 0);
    }

    private void reset(boolean open, int cap) {
        this.open = open;
        this.over = false;
        this.marks.clear();
        this.from = HexGrid.CENTRE;
        this.cap = cap;
        this.filled = 0;
        this.scrapes = 0;
        this.jumps = 0;
    }

    @Nullable
    public Mark markAt(int cell) {
        for (Mark mark : marks) {
            if (mark.cell == cell) {
                return mark;
            }
        }
        return null;
    }

    /** A symbol covers the point and its resource is not there yet. */
    public boolean ready(RuneBoard board, int point) {
        return !isFilled(point) && markAt(board.points().get(point).cell()) != null;
    }

    public boolean complete(RuneBoard board) {
        return filled == (1 << board.points().size()) - 1;
    }

    public boolean canWrite() {
        return isActive() && used() < cap;
    }

    /** Where an item's symbol would land now. */
    public HexGrid.Walk preview(RuneBoard board, ItemElements elements, boolean ground) {
        return HexGrid.walk(board.activeWalls(filled), from, elements.offset(ground));
    }

    /** Why a symbol may not be written. */
    public enum Refusal {
        /** It would land where a symbol (or the orb) already is, or not move at all. */
        OCCUPIED,
        /** The previous symbol has the same element: two equal symbols never follow each other. */
        SAME_ELEMENT
    }

    /** Whether the item's symbol may go where {@code walk} takes it: null when it may. */
    @Nullable
    public Refusal refusal(ItemElements elements, HexGrid.Walk walk) {
        int end = walk.end();
        if (end == from || end == HexGrid.CENTRE || markAt(end) != null) {
            return Refusal.OCCUPIED;
        }
        if (!marks.isEmpty() && marks.get(marks.size() - 1).element() == elements.main()) {
            return Refusal.SAME_ELEMENT;
        }
        return null;
    }

    /** Writes the item's symbol; the returned walk says where it landed (null when not allowed). */
    @Nullable
    public HexGrid.Walk write(RuneBoard board, ItemElements elements, ResourceLocation item, boolean ground) {
        if (!canWrite()) {
            return null;
        }
        HexGrid.Walk walk = preview(board, elements, ground);
        if (refusal(elements, walk) != null) {
            return null;
        }
        marks.add(new Mark(walk.end(), elements.main(), from, walk.cells(), item));
        from = walk.end();
        checkFailed(board);
        return walk;
    }

    /** Leaves the resource on a covered point. */
    public Result leave(RuneBoard board, int point) {
        if (!isActive() || point < 0 || point >= board.points().size() || !ready(board, point)) {
            return Result.NOT_ALLOWED;
        }
        filled |= 1 << point;
        if (complete(board)) {
            over = true;
            return Result.COMPLETE;
        }
        return checkFailed(board) ? Result.FAILED : Result.OK;
    }

    /** Ends the sheet when it is full and nothing covered is left to fill. */
    private boolean checkFailed(RuneBoard board) {
        if (used() < cap || complete(board)) {
            return false;
        }
        for (int i = 0; i < board.points().size(); i++) {
            if (ready(board, i)) {
                return false;
            }
        }
        over = true;
        return true;
    }

    /** The sheet ran out without the rune: there is nothing more to do on it. */
    public boolean failed(RuneBoard board) {
        return over && !complete(board);
    }

    /** Scrapes off the last symbol and gives its place back ({@code allowed} = the scraper's strength). */
    public boolean scrape(RuneBoard board, int allowed) {
        if (!isActive() || marks.isEmpty() || scrapes >= allowed) {
            return false;
        }
        Mark last = marks.get(marks.size() - 1);
        int point = board.pointAt(last.cell);
        if (point >= 0 && isFilled(point)) {
            return false;
        }
        marks.remove(marks.size() - 1);
        from = last.from;
        scrapes++;
        return true;
    }

    /** Whether the fork of this strength could continue the chain from {@code cell}. */
    public boolean canJump(RuneBoard board, int cell, int fork) {
        if (!isActive() || fork <= 0 || cell == from || jumps >= fork) {
            return false;
        }
        int point = board.pointAt(cell);
        return cell == HexGrid.CENTRE || fork >= 2 && point >= 0 && isFilled(point);
    }

    public boolean jump(RuneBoard board, int cell, int fork) {
        if (!canJump(board, cell, fork)) {
            return false;
        }
        jumps++;
        from = cell;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (rune != null) {
            tag.putString("rune", rune.key());
        }
        tag.putBoolean("open", open);
        tag.putBoolean("over", over);
        tag.putInt("from", from);
        tag.putInt("cap", cap);
        tag.putInt("filled", filled);
        tag.putInt("scrapes", scrapes);
        tag.putInt("jumps", jumps);
        ListTag list = new ListTag();
        for (Mark mark : marks) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("cell", mark.cell);
            entry.putString("element", mark.element.id());
            entry.putInt("from", mark.from);
            entry.putIntArray("cells", mark.cells);
            entry.putString("item", mark.item.toString());
            list.add(entry);
        }
        tag.put("marks", list);
        return tag;
    }

    public void load(CompoundTag tag) {
        rune = tag.contains("rune") ? ComplexRunes.byKey(tag.getString("rune")) : null;
        open = tag.getBoolean("open");
        over = tag.getBoolean("over");
        from = valid(tag.contains("from") ? tag.getInt("from") : HexGrid.CENTRE);
        cap = tag.getInt("cap");
        filled = tag.getInt("filled");
        scrapes = tag.getInt("scrapes");
        jumps = tag.getInt("jumps");
        marks.clear();
        for (Tag element : tag.getList("marks", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) element;
            SimpleRune rune = SimpleRune.byId(entry.getString("element"));
            ResourceLocation item = ResourceLocation.tryParse(entry.getString("item"));
            if (rune != null && item != null) {
                marks.add(new Mark(valid(entry.getInt("cell")), rune, valid(entry.getInt("from")), entry.getIntArray("cells"), item));
            }
        }
        if (this.rune == null) {
            reset(false, 0);
        }
    }

    private static int valid(int cell) {
        return cell >= 0 && cell < HexGrid.COUNT ? cell : HexGrid.CENTRE;
    }
}
