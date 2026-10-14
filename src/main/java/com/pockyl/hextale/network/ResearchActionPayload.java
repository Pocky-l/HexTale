package com.pockyl.hextale.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.rune.ComplexRunes;

/**
 * Client to server: something the researcher does at the open table. The server checks everything again.
 *
 * @param value the rune index (SELECT), point index (LEAVE) or parchment section (JUMP)
 * @param item  the item that writes (WRITE)
 * @param ground whether the item is ground in the mortar first (WRITE)
 */
public record ResearchActionPayload(BlockPos pos, Action action, int value, ResourceLocation item, boolean ground)
        implements CustomPacketPayload {
    public static final Type<ResearchActionPayload> TYPE = new Type<>(Hextale.id("research_action"));
    private static final ResourceLocation NONE = ResourceLocation.withDefaultNamespace("air");

    public static final StreamCodec<ByteBuf, ResearchActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ResearchActionPayload::pos,
            ByteBufCodecs.VAR_INT.map(i -> Action.values()[Math.floorMod(i, Action.values().length)], Action::ordinal),
            ResearchActionPayload::action,
            ByteBufCodecs.VAR_INT, ResearchActionPayload::value,
            ResourceLocation.STREAM_CODEC, ResearchActionPayload::item,
            ByteBufCodecs.BOOL, ResearchActionPayload::ground,
            ResearchActionPayload::new);

    public enum Action {
        SELECT,
        NEW_SHEET,
        WRITE,
        LEAVE,
        SCRAPE,
        JUMP,
        STUDY_ITEM
    }

    public static ResearchActionPayload of(BlockPos pos, Action action, int value) {
        return new ResearchActionPayload(pos, action, value, NONE, false);
    }

    public static ResearchActionPayload write(BlockPos pos, Item item, boolean ground) {
        return new ResearchActionPayload(pos, Action.WRITE, 0, BuiltInRegistries.ITEM.getKey(item), ground);
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
                if (payload.value() >= 0 && payload.value() < ComplexRunes.COUNT) {
                    table.select(player, ComplexRunes.all().get(payload.value()));
                }
            }
            case NEW_SHEET -> table.newSheet(player);
            case WRITE -> BuiltInRegistries.ITEM.getOptional(payload.item())
                    .ifPresent(item -> table.write(player, item, payload.ground()));
            case LEAVE -> table.leave(player, payload.value());
            case SCRAPE -> table.scrape(player);
            case JUMP -> table.jump(player, payload.value());
            case STUDY_ITEM -> table.selectItem(player);
        }
    }
}
