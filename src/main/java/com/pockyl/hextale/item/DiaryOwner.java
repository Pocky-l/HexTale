package com.pockyl.hextale.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** The researcher a diary is signed by: the first player who opened it or researched with it on a table. */
public record DiaryOwner(UUID id, String name) {
    public static final Codec<DiaryOwner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(DiaryOwner::id),
            Codec.STRING.fieldOf("name").forGetter(DiaryOwner::name)
    ).apply(instance, DiaryOwner::new));
    public static final StreamCodec<ByteBuf, DiaryOwner> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, DiaryOwner::id,
            ByteBufCodecs.STRING_UTF8, DiaryOwner::name,
            DiaryOwner::new);

    public static DiaryOwner of(Player player) {
        return new DiaryOwner(player.getUUID(), player.getGameProfile().name());
    }

    public boolean is(Player player) {
        return id.equals(player.getUUID());
    }
}
