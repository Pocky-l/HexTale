package com.pockyl.hextale.port;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

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
}
