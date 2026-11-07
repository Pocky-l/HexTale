package com.pockyl.hextale.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** A Hex Vault taken off the ground: which vault it is, and for its tooltip what it held when it was lifted. */
public record VaultMemory(UUID vault, int kinds, long used) {
    public static final Codec<VaultMemory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("vault").forGetter(VaultMemory::vault),
            Codec.INT.fieldOf("kinds").forGetter(VaultMemory::kinds),
            Codec.LONG.fieldOf("used").forGetter(VaultMemory::used)).apply(instance, VaultMemory::new));
    public static final StreamCodec<ByteBuf, VaultMemory> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, VaultMemory::vault,
            ByteBufCodecs.VAR_INT, VaultMemory::kinds, ByteBufCodecs.VAR_LONG, VaultMemory::used, VaultMemory::new);
}
