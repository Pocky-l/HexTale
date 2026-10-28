package com.pockyl.hextale.thevoid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A breach folded into a Breach Heart: the numbers its outline grows from and what it had eaten towards its next birth.
 * Unfolded, the same breach opens again - wild, until a ring holds it anew.
 */
public record BreachHeart(VoidShape shape, float size, int seed, float fed) {
    public static final Codec<BreachHeart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(VoidShape::byId, VoidShape::id).fieldOf("shape").forGetter(BreachHeart::shape),
            Codec.floatRange(0.5F, 16).fieldOf("size").forGetter(BreachHeart::size),
            Codec.INT.fieldOf("seed").forGetter(BreachHeart::seed),
            Codec.FLOAT.optionalFieldOf("fed", 0F).forGetter(BreachHeart::fed)
    ).apply(instance, BreachHeart::new));
    public static final StreamCodec<ByteBuf, BreachHeart> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(VoidShape::byOrdinal, VoidShape::ordinal), BreachHeart::shape,
            ByteBufCodecs.FLOAT, BreachHeart::size,
            ByteBufCodecs.INT, BreachHeart::seed,
            ByteBufCodecs.FLOAT, BreachHeart::fed,
            BreachHeart::new);

    public static BreachHeart of(TheVoid breach, float fed) {
        return new BreachHeart(breach.shape(), breach.size(), breach.seed(), fed);
    }

    public VoidLayout layout() {
        return VoidLayout.of(shape, size, seed);
    }
}
