package com.pockyl.hextale.runework;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The runes engraved into a hextech tool, by slot. Slot order matters: a Lens focuses the rune in the next slot, and
 * later rune chains run in order. Immutable; every change returns a new value.
 */
public record RuneSlots(List<Engraving> engravings) {
    public static final RuneSlots EMPTY = new RuneSlots(List.of());

    public static final Codec<RuneSlots> CODEC = Engraving.CODEC.listOf().xmap(RuneSlots::new, RuneSlots::engravings);
    public static final StreamCodec<ByteBuf, RuneSlots> STREAM_CODEC =
            Engraving.STREAM_CODEC.apply(ByteBufCodecs.list()).map(RuneSlots::new, RuneSlots::engravings);

    public RuneSlots {
        engravings = engravings.stream().sorted(Comparator.comparingInt(Engraving::slot)).toList();
    }

    /** The rune in that slot, or null when the slot is empty. */
    @Nullable
    public Engraving get(int slot) {
        for (Engraving engraving : engravings) {
            if (engraving.slot() == slot) {
                return engraving;
            }
        }
        return null;
    }

    @Nullable
    public Engraving find(ComplexRune rune) {
        for (Engraving engraving : engravings) {
            if (engraving.rune() == rune) {
                return engraving;
            }
        }
        return null;
    }

    public boolean has(ComplexRune rune) {
        return find(rune) != null;
    }

    public int count() {
        return engravings.size();
    }

    public boolean isEmpty() {
        return engravings.isEmpty();
    }

    public RuneSlots with(Engraving engraving) {
        List<Engraving> list = new ArrayList<>(without(engraving.slot()).engravings);
        list.add(engraving);
        return new RuneSlots(list);
    }

    public RuneSlots without(int slot) {
        return new RuneSlots(engravings.stream().filter(engraving -> engraving.slot() != slot).toList());
    }

    /**
     * One engraved rune.
     *
     * @param slot  0-based slot on the tool
     * @param stars the stars of the scroll it was engraved from (1-3)
     */
    public record Engraving(int slot, ComplexRune rune, int stars) {
        public static final Codec<Engraving> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 15).fieldOf("slot").forGetter(Engraving::slot),
                ComplexRunes.CODEC.fieldOf("rune").forGetter(Engraving::rune),
                Codec.intRange(1, 3).fieldOf("stars").forGetter(Engraving::stars)
        ).apply(instance, Engraving::new));
        public static final StreamCodec<ByteBuf, Engraving> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Engraving::slot,
                ComplexRunes.STREAM_CODEC, Engraving::rune,
                ByteBufCodecs.VAR_INT, Engraving::stars,
                Engraving::new);
    }
}
