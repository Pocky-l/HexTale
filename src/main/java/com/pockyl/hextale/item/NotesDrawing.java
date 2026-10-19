package com.pockyl.hextale.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.pockyl.hextale.research.HexGrid;

import java.util.Arrays;
import java.util.List;

/**
 * The finished drawing written into research notes: which board of the topic it was drawn on and its steps. Laid back
 * on a Research Table, the notes show it on the parchment again.
 */
public record NotesDrawing(int attempt, List<Integer> steps) {
    public static final Codec<NotesDrawing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("attempt").forGetter(NotesDrawing::attempt),
            Codec.INT.listOf().fieldOf("steps").forGetter(NotesDrawing::steps)
    ).apply(instance, NotesDrawing::new));
    public static final StreamCodec<ByteBuf, NotesDrawing> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NotesDrawing::attempt,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(HexGrid.COUNT * 2)), NotesDrawing::steps,
            NotesDrawing::new);

    public NotesDrawing {
        steps = List.copyOf(steps);
    }

    public static NotesDrawing of(int attempt, int[] tree) {
        return new NotesDrawing(attempt, Arrays.stream(tree).boxed().toList());
    }

    /** The steps as a sheet keeps them; empty if the data is broken (edited by hand). */
    public int[] tree() {
        int[] tree = steps.stream().mapToInt(Integer::intValue).toArray();
        return tree.length % 2 != 0 || Arrays.stream(tree).anyMatch(c -> c < 0 || c >= HexGrid.COUNT) ? new int[0] : tree;
    }
}
