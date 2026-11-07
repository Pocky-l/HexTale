package com.pockyl.hextale.thevoid.boss;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * What it has learned so far: how full each kind's study is, which kinds are written down, and a copy of the last thing
 * of each kind that hurt it. It knows nothing of entities; the Prosector turns "full" into a Specimen.
 */
public final class Study {
    private static final float NEARLY = 0.99F;
    private final Map<StudyKind, Float> fill = new EnumMap<>(StudyKind.class);
    private final Set<StudyKind> written = EnumSet.noneOf(StudyKind.class);
    private final Map<StudyKind, ItemStack> copies = new EnumMap<>(StudyKind.class);

    /** 0 empty .. 1 full. */
    public float fill(StudyKind kind) {
        return written.contains(kind) ? 1 : fill.getOrDefault(kind, 0F);
    }

    public boolean written(StudyKind kind) {
        return written.contains(kind);
    }

    public int writtenCount() {
        return written.size();
    }

    public Set<StudyKind> writtenKinds() {
        Set<StudyKind> kinds = EnumSet.noneOf(StudyKind.class);
        kinds.addAll(written);
        return kinds;
    }

    public ItemStack copy(StudyKind kind) {
        return copies.getOrDefault(kind, ItemStack.EMPTY);
    }

    public void remember(StudyKind kind, ItemStack copy) {
        if (!copy.isEmpty()) {
            copies.put(kind, copy);
        }
    }

    /**
     * Adds to a kind's study. Returns true when this fills it and {@code room} says another kind may be written down; with
     * no room left the study waits just under full, so the kind is written as soon as an older one is forgotten.
     */
    public boolean add(StudyKind kind, float share, boolean room) {
        if (written.contains(kind)) {
            return false;
        }
        float now = fill.getOrDefault(kind, 0F) + share;
        if (now >= 1 && room) {
            fill.put(kind, 1F);
            return true;
        }
        fill.put(kind, Math.min(now, NEARLY));
        return false;
    }

    public void write(StudyKind kind) {
        written.add(kind);
    }

    public void forget(StudyKind kind) {
        written.remove(kind);
        fill.put(kind, 0F);
        copies.remove(kind);
    }

    public void forgetAll() {
        written.clear();
        fill.clear();
        copies.clear();
    }

    public void save(ValueOutput output) {
        ValueOutput.TypedOutputList<Integer> list = output.list("study_written", Codec.INT);
        written.forEach(kind -> list.add(kind.ordinal()));
        for (StudyKind kind : StudyKind.values()) {
            output.putFloat("study_" + kind.ordinal(), fill.getOrDefault(kind, 0F));
            ItemStack copy = copies.get(kind);
            if (copy != null) {
                output.store("study_copy_" + kind.ordinal(), ItemStack.CODEC, copy);
            }
        }
    }

    public void load(ValueInput input) {
        forgetAll();
        input.listOrEmpty("study_written", Codec.INT).forEach(id -> written.add(StudyKind.byId(id)));
        for (StudyKind kind : StudyKind.values()) {
            fill.put(kind, input.getFloatOr("study_" + kind.ordinal(), 0F));
            input.read("study_copy_" + kind.ordinal(), ItemStack.CODEC).ifPresent(copy -> copies.put(kind, copy));
        }
    }
}
