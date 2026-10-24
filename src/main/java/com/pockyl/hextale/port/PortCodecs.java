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
}
