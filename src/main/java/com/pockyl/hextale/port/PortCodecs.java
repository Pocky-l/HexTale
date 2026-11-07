package com.pockyl.hextale.port;

import com.mojang.datafixers.util.Function7;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;

/** Stream codecs Minecraft 26.x has and 1.21.1 lacks. */
public final class PortCodecs {
    private PortCodecs() {
    }

    /** {@code Vec3.STREAM_CODEC} of 26.x: three doubles. */
    public static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y,
            ByteBufCodecs.DOUBLE, Vec3::z,
            Vec3::new);

    /** {@code StreamCodec.composite} with seven fields (1.21.1 stops at six). */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6,
            StreamCodec<? super B, T7> c7, Function<C, T7> g7, Function7<T1, T2, T3, T4, T5, T6, T7, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                return factory.apply(c1.decode(buffer), c2.decode(buffer), c3.decode(buffer), c4.decode(buffer), c5.decode(buffer),
                        c6.decode(buffer), c7.decode(buffer));
            }

            @Override
            public void encode(B buffer, C value) {
                c1.encode(buffer, g1.apply(value));
                c2.encode(buffer, g2.apply(value));
                c3.encode(buffer, g3.apply(value));
                c4.encode(buffer, g4.apply(value));
                c5.encode(buffer, g5.apply(value));
                c6.encode(buffer, g6.apply(value));
                c7.encode(buffer, g7.apply(value));
            }
        };
    }

    /** {@code StreamCodec.composite} with eight fields (1.21.1 stops at six). */
    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> codec1, java.util.function.Function<C, T1> getter1,
            StreamCodec<? super B, T2> codec2, java.util.function.Function<C, T2> getter2,
            StreamCodec<? super B, T3> codec3, java.util.function.Function<C, T3> getter3,
            StreamCodec<? super B, T4> codec4, java.util.function.Function<C, T4> getter4,
            StreamCodec<? super B, T5> codec5, java.util.function.Function<C, T5> getter5,
            StreamCodec<? super B, T6> codec6, java.util.function.Function<C, T6> getter6,
            StreamCodec<? super B, T7> codec7, java.util.function.Function<C, T7> getter7,
            StreamCodec<? super B, T8> codec8, java.util.function.Function<C, T8> getter8,
            StreamCodec<? super B, T9> codec9, java.util.function.Function<C, T9> getter9,
            StreamCodec<? super B, T10> codec10, java.util.function.Function<C, T10> getter10,
            com.mojang.datafixers.util.Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 v1 = codec1.decode(buffer);
                T2 v2 = codec2.decode(buffer);
                T3 v3 = codec3.decode(buffer);
                T4 v4 = codec4.decode(buffer);
                T5 v5 = codec5.decode(buffer);
                T6 v6 = codec6.decode(buffer);
                T7 v7 = codec7.decode(buffer);
                T8 v8 = codec8.decode(buffer);
                T9 v9 = codec9.decode(buffer);
                T10 v10 = codec10.decode(buffer);
                return factory.apply(v1, v2, v3, v4, v5, v6, v7, v8, v9, v10);
            }

            @Override
            public void encode(B buffer, C value) {
                codec1.encode(buffer, getter1.apply(value));
                codec2.encode(buffer, getter2.apply(value));
                codec3.encode(buffer, getter3.apply(value));
                codec4.encode(buffer, getter4.apply(value));
                codec5.encode(buffer, getter5.apply(value));
                codec6.encode(buffer, getter6.apply(value));
                codec7.encode(buffer, getter7.apply(value));
                codec8.encode(buffer, getter8.apply(value));
                codec9.encode(buffer, getter9.apply(value));
                codec10.encode(buffer, getter10.apply(value));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite(
            StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
            StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4,
            StreamCodec<? super B, T5> c5, Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6,
            StreamCodec<? super B, T7> c7, Function<C, T7> g7, StreamCodec<? super B, T8> c8, Function<C, T8> g8,
            com.mojang.datafixers.util.Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> factory) {
        return new StreamCodec<>() {
            @Override
            public C decode(B buffer) {
                return factory.apply(c1.decode(buffer), c2.decode(buffer), c3.decode(buffer), c4.decode(buffer), c5.decode(buffer),
                        c6.decode(buffer), c7.decode(buffer), c8.decode(buffer));
            }

            @Override
            public void encode(B buffer, C value) {
                c1.encode(buffer, g1.apply(value));
                c2.encode(buffer, g2.apply(value));
                c3.encode(buffer, g3.apply(value));
                c4.encode(buffer, g4.apply(value));
                c5.encode(buffer, g5.apply(value));
                c6.encode(buffer, g6.apply(value));
                c7.encode(buffer, g7.apply(value));
                c8.encode(buffer, g8.apply(value));
            }
        };
    }
}
