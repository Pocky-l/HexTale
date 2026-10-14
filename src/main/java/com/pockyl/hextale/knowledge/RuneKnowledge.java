package com.pockyl.hextale.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * What a researcher has found out. Lives on the player (a data attachment copied on death), not on the diary item: a
 * lost diary loses nothing. The server copy is authoritative; clients get a copy through {@code KnowledgeSyncPayload}.
 */
public final class RuneKnowledge {
    public static final Codec<RuneKnowledge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("runes", List.of()).forGetter(k -> keys(k.runes, ComplexRune::key)),
            Codec.STRING.listOf().optionalFieldOf("notes", List.of()).forGetter(k -> keys(k.notes, DiaryNote::id)),
            Codec.STRING.listOf().optionalFieldOf("read_notes", List.of()).forGetter(k -> keys(k.readNotes, DiaryNote::id)),
            Codec.STRING.listOf().optionalFieldOf("drawn", List.of()).forGetter(k -> keys(k.drawn, SimpleRune::id)),
            Codec.INT.optionalFieldOf("failures", 0).forGetter(k -> k.failures),
            Codec.INT.optionalFieldOf("cracks", 0).forGetter(k -> k.cracks)
    ).apply(instance, RuneKnowledge::new));

    private static final StreamCodec<ByteBuf, List<String>> STRINGS = ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256));
    public static final StreamCodec<ByteBuf, RuneKnowledge> STREAM_CODEC = StreamCodec.composite(
            STRINGS, k -> keys(k.runes, ComplexRune::key),
            STRINGS, k -> keys(k.notes, DiaryNote::id),
            STRINGS, k -> keys(k.readNotes, DiaryNote::id),
            STRINGS, k -> keys(k.drawn, SimpleRune::id),
            ByteBufCodecs.VAR_INT, k -> k.failures,
            ByteBufCodecs.VAR_INT, k -> k.cracks,
            RuneKnowledge::new);

    /** Learned complex runes in the order of discovery. */
    private final Set<ComplexRune> runes = new LinkedHashSet<>();
    /** Unlocked diary notes in the order they were written. */
    private final Set<DiaryNote> notes = new LinkedHashSet<>();
    private final Set<DiaryNote> readNotes = EnumSet.noneOf(DiaryNote.class);
    /** Elements the orb has recognised from this player's brush, even in failed fusions. */
    private final Set<SimpleRune> drawn = EnumSet.noneOf(SimpleRune.class);
    private int failures;
    private int cracks;

    public RuneKnowledge() {
    }

    private RuneKnowledge(List<String> runes, List<String> notes, List<String> readNotes, List<String> drawn, int failures,
                          int cracks) {
        runes.stream().map(ComplexRunes::byKey).filter(Objects::nonNull).forEach(this.runes::add);
        notes.stream().map(DiaryNote::byId).filter(Objects::nonNull).forEach(this.notes::add);
        readNotes.stream().map(DiaryNote::byId).filter(Objects::nonNull).forEach(this.readNotes::add);
        drawn.stream().map(SimpleRune::byId).filter(Objects::nonNull).forEach(this.drawn::add);
        this.failures = failures;
        this.cracks = cracks;
    }

    private static <T> List<String> keys(Collection<T> values, Function<T, String> key) {
        return values.stream().map(key).toList();
    }

    public boolean knows(ComplexRune rune) {
        return runes.contains(rune);
    }

    /** @return whether the rune was new */
    public boolean learn(ComplexRune rune) {
        return runes.add(rune);
    }

    public boolean forget(ComplexRune rune) {
        return runes.remove(rune);
    }

    public int runeCount() {
        return runes.size();
    }

    /** Learned runes in the order of discovery. */
    public List<ComplexRune> runes() {
        return List.copyOf(runes);
    }

    public boolean hasNote(DiaryNote note) {
        return notes.contains(note);
    }

    /** @return whether the note was new */
    public boolean addNote(DiaryNote note) {
        return notes.add(note);
    }

    /** Unlocked notes, newest first. */
    public List<DiaryNote> notesNewestFirst() {
        List<DiaryNote> list = new ArrayList<>(notes);
        Collections.reverse(list);
        return list;
    }

    public boolean isRead(DiaryNote note) {
        return readNotes.contains(note);
    }

    public int unreadNotes() {
        int count = 0;
        for (DiaryNote note : notes) {
            if (!readNotes.contains(note)) {
                count++;
            }
        }
        return count;
    }

    /** @return whether anything changed */
    public boolean markNotesRead() {
        return readNotes.addAll(notes);
    }

    public void recordDrawn(SimpleRune element) {
        drawn.add(element);
    }

    /** Elements the player has worked with: recognised by the orb or part of a learned rune. */
    public Set<SimpleRune> usedElements() {
        Set<SimpleRune> used = EnumSet.noneOf(SimpleRune.class);
        used.addAll(drawn);
        for (ComplexRune rune : runes) {
            used.add(rune.action());
            used.add(rune.target());
        }
        return used;
    }

    public int failures() {
        return failures;
    }

    public void recordFailure() {
        failures++;
    }

    public int cracks() {
        return cracks;
    }

    public void recordCrack() {
        cracks++;
    }

    public RuneKnowledge copy() {
        return new RuneKnowledge(keys(runes, ComplexRune::key), keys(notes, DiaryNote::id), keys(readNotes, DiaryNote::id),
                keys(drawn, SimpleRune::id), failures, cracks);
    }
}
