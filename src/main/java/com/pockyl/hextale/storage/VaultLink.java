package com.pockyl.hextale.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** A tie to a Hex Vault, on a pressed crystal or a Pocket Terminal: the vault's id, and where it stood for the tooltip. */
public record VaultLink(UUID vault, BlockPos pos) {
    public static final Codec<VaultLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("vault").forGetter(VaultLink::vault),
            BlockPos.CODEC.fieldOf("pos").forGetter(VaultLink::pos)).apply(instance, VaultLink::new));
    public static final StreamCodec<ByteBuf, VaultLink> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, VaultLink::vault,
            BlockPos.STREAM_CODEC, VaultLink::pos, VaultLink::new);
}
