package com.pockyl.hextale.chem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** What a Chem Carboy holds: one brew and how many doses of it, a dose being one vial. */
public record CarboyContents(Brew brew, int doses) {
    /** Doses a carboy holds: sixteen stacks of vials. */
    public static final int CAPACITY = 256;
    public static final CarboyContents EMPTY = new CarboyContents(Brew.FUEL, 0);

    public static final Codec<CarboyContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Brew.CODEC.fieldOf("brew").forGetter(CarboyContents::brew),
            Codec.intRange(0, CAPACITY).fieldOf("doses").forGetter(CarboyContents::doses)).apply(instance, CarboyContents::new));
    public static final StreamCodec<ByteBuf, CarboyContents> STREAM_CODEC = StreamCodec.composite(
            Brew.STREAM_CODEC, CarboyContents::brew, ByteBufCodecs.VAR_INT, CarboyContents::doses, CarboyContents::new);

    public boolean isEmpty() {
        return doses <= 0;
    }

    /** How full it is, 0-1. */
    public float level() {
        return Math.min(1, doses / (float) CAPACITY);
    }

    /** Doses of this brew that still fit: none on top of another brew. */
    public int room(Brew other) {
        return isEmpty() || brew == other ? CAPACITY - Math.max(0, doses) : 0;
    }

    /** The contents with {@code count} more doses of the brew (call {@link #room} first). */
    public CarboyContents plus(Brew other, int count) {
        return new CarboyContents(other, Math.min(CAPACITY, Math.max(0, doses) + count));
    }

    /** The contents with one dose less; the last dose leaves it empty. */
    public CarboyContents minusDose() {
        return doses <= 1 ? EMPTY : new CarboyContents(brew, doses - 1);
    }
}
