package com.pockyl.hextale.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;

import java.util.List;

/** Codecs for saved data that must survive entries the game can no longer read (items or blocks of removed mods). */
public final class SafeCodecs {
    private SafeCodecs() {
    }

    /**
     * A list that writes like {@code element.listOf()} but reads every entry on its own: an entry that fails to decode
     * is skipped instead of failing the whole list.
     */
    public static <T> Codec<List<T>> lenientList(Codec<T> element) {
        Decoder<List<T>> decoder = new Decoder<>() {
            @Override
            public <O> DataResult<Pair<List<T>, O>> decode(DynamicOps<O> ops, O input) {
                return ops.getStream(input).map(stream -> Pair.of(
                        stream.flatMap(entry -> element.parse(ops, entry).result().stream()).toList(), ops.empty()));
            }
        };
        return Codec.of(element.listOf(), decoder);
    }
}
