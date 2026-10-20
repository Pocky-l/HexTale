package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.LabBenchBlockEntity;
import com.pockyl.hextale.menu.LabBenchMenu;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;

/**
 * Client to server: something the researcher did at the Lab Bench's screen - opened a topic of the journal ({@code value}
 * = topic index, -1 none), let a drop fall ({@code value} = essence ordinal), turned the flame ({@code value} = 0-4),
 * opened the vent, poured the flask out or sealed it.
 */
public record LabActionPayload(BlockPos pos, Action action, int value) implements CustomPacketPayload {
    public static final Type<LabActionPayload> TYPE = new Type<>(Hextale.id("lab_action"));

    public enum Action {
        SELECT, DROP, FLAME, VENT, POUR, SEAL
    }

    private static final StreamCodec<ByteBuf, Action> ACTION =
            ByteBufCodecs.idMapper(i -> Action.values()[Math.clamp(i, 0, Action.values().length - 1)], Action::ordinal);

    public static final StreamCodec<ByteBuf, LabActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LabActionPayload::pos,
            ACTION, LabActionPayload::action,
            ByteBufCodecs.VAR_INT, LabActionPayload::value,
            LabActionPayload::new);

    @Override
    public Type<LabActionPayload> type() {
        return TYPE;
    }

    public static void handle(LabActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof LabBenchMenu menu)
                || !menu.pos().equals(payload.pos) || !menu.stillValid(player)
                || !(player.level().getBlockEntity(payload.pos) instanceof LabBenchBlockEntity bench)) {
            return;
        }
        int value = payload.value;
        switch (payload.action) {
            case SELECT -> bench.select(player, ResearchTopics.byIndex(value));
            case DROP -> {
                if (value >= 0 && value < Essence.COUNT) {
                    bench.drop(player, Essence.values()[value]);
                }
            }
            case FLAME -> bench.setFlame(value);
            case VENT -> bench.vent();
            case POUR -> bench.pour();
            case SEAL -> bench.seal(player);
        }
    }
}
