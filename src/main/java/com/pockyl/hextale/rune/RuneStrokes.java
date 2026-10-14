package com.pockyl.hextale.rune;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * The ink of one drawing circle as sent from the client: polylines {@code [x0, y0, x1, y1, ...]} in units of the
 * circle's radius, centred on the circle. Decoding refuses oversized data, so a client cannot make the server run the
 * recognizer on megabytes of points.
 */
public record RuneStrokes(List<float[]> strokes) {
    public static final int MAX_STROKES = 12;
    public static final int MAX_POINTS = 600;
    /** Strokes belong to the half of the parchment where they started, so they may wander a bit outside the circle. */
    public static final float MAX_COORDINATE = 4;
    public static final RuneStrokes EMPTY = new RuneStrokes(List.of());

    public static final StreamCodec<ByteBuf, RuneStrokes> STREAM_CODEC = StreamCodec.of(RuneStrokes::write, RuneStrokes::read);

    public RuneStrokes {
        strokes = List.copyOf(strokes);
    }

    /** Whether every stroke has at least two finite points inside the allowed area. */
    public boolean isValid() {
        if (strokes.isEmpty() || strokes.size() > MAX_STROKES) {
            return false;
        }
        for (float[] stroke : strokes) {
            if (stroke.length < 4 || stroke.length % 2 != 0 || stroke.length > MAX_POINTS * 2) {
                return false;
            }
            for (float value : stroke) {
                if (!Float.isFinite(value) || Math.abs(value) > MAX_COORDINATE) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void write(ByteBuf buf, RuneStrokes value) {
        VarInt.write(buf, value.strokes.size());
        for (float[] stroke : value.strokes) {
            VarInt.write(buf, stroke.length / 2);
            for (float v : stroke) {
                buf.writeFloat(v);
            }
        }
    }

    private static RuneStrokes read(ByteBuf buf) {
        int count = VarInt.read(buf);
        if (count < 0 || count > MAX_STROKES) {
            throw new DecoderException("Too many rune strokes: " + count);
        }
        List<float[]> strokes = new ArrayList<>(count);
        for (int s = 0; s < count; s++) {
            int points = VarInt.read(buf);
            if (points < 0 || points > MAX_POINTS) {
                throw new DecoderException("Too many points in a rune stroke: " + points);
            }
            float[] stroke = new float[points * 2];
            for (int i = 0; i < stroke.length; i++) {
                stroke[i] = buf.readFloat();
            }
            strokes.add(stroke);
        }
        return new RuneStrokes(strokes);
    }
}
