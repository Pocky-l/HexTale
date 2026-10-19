package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.Arrays;
import java.util.List;

/**
 * Client to server: something the researcher does at the open table. The server checks everything again.
 *
 * @param value the topic index (SELECT), the field section to check (STRIKE) or the port to shine from (FIRE)
 * @param cells the whole ink thread from the orb (THREAD)
 */
public record ResearchActionPayload(BlockPos pos, Action action, int value, List<Integer> cells) implements CustomPacketPayload {
    public static final Type<ResearchActionPayload> TYPE = new Type<>(Hextale.id("research_action"));
    /** Longer than any thread the parchment's sections allow without crossings. */
    private static final int MAX_CELLS = 1024;

    public static final StreamCodec<ByteBuf, ResearchActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ResearchActionPayload::pos,
            ByteBufCodecs.VAR_INT.map(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal),
            ResearchActionPayload::action,
            ByteBufCodecs.VAR_INT, ResearchActionPayload::value,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_CELLS)), ResearchActionPayload::cells,
            ResearchActionPayload::new);

    public enum Action {
        SELECT,
        THREAD,
        STRIKE,
        CLOSE_STUDY,
        FIRE
    }

    public static ResearchActionPayload of(BlockPos pos, Action action, int value) {
        return new ResearchActionPayload(pos, action, value, List.of());
    }

    public static ResearchActionPayload thread(BlockPos pos, int[] cells) {
        return new ResearchActionPayload(pos, Action.THREAD, 0, Arrays.stream(cells).boxed().toList());
    }

    @Override
    public Type<ResearchActionPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        ResearchTableBlockEntity table = ResearchTableMenu.openTable(player, payload.pos());
        if (table == null) {
            return;
        }
        switch (payload.action()) {
            case SELECT -> {
                ResearchTopic topic = ResearchTopics.byIndex(payload.value());
                if (topic != null) {
                    table.select(player, topic);
                }
            }
            case THREAD -> table.thread(player, payload.cells().stream().mapToInt(Integer::intValue).toArray());
            case STRIKE -> table.strike(player, payload.value());
            case CLOSE_STUDY -> table.closeStudy(player);
            case FIRE -> table.fire(player, payload.value());
        }
    }
}
