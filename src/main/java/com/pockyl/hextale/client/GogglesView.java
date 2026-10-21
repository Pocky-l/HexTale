package com.pockyl.hextale.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.block.BrassPipeBlock;
import com.pockyl.hextale.block.DrainValveBlockEntity;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.block.InkSupply;
import com.pockyl.hextale.transport.PylonLink;
import com.pockyl.hextale.block.ResearchTableBlock;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The Researcher's Goggles at work: while worn, looking at a block of the essence works shows a small plate beside the
 * crosshair with the essences inside it (a pipe shows what the vessels on its network hold), and looking at a Research
 * Table shows its ink, essence by essence, while streams of coloured motes run from each tank of concentrate to the
 * orb. A hex pylon shows its reach, pace, links and runes (every link of the pylons around is drawn while the goggles
 * are on), a block pylons send to shows which ones. Nothing shows when the goggles are off or the player looks
 * elsewhere. Reads only what the blocks sync anyway.
 */
public final class GogglesView {
    private static final int GOLD = 0xE0B85A;
    private static final int TEXT = 0xFFE8E0F0;
    private static final int DIM = 0xFF9A90B8;
    private static final int LINE = 11;
    /** Text colours of the pipe side modes (by ordinal): the collars' green and blue, red for shut. */
    private static final int[] PIPE_COLOURS = {0x9A90B8, 0xE8E0F0, 0x7FD38E, 0x98E3FB, 0xE07A6A};
    private static final String KEY = "goggles.hextale.";
    /** The plate slides out from the crosshair when the goggles settle on a new block. */
    private static final long SLIDE_MILLIS = 160;
    @Nullable
    private static BlockPos shown;
    private static long shownSince;

    private GogglesView() {
    }

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, Hextale.id("researchers_goggles"), GogglesView::render);
    }

    /** A titled list of what the looked-at block holds. */
    private record Reading(Component title, List<Row> rows) {
    }

    /** One line: an essence's sign (or none) and its text. */
    private record Row(@Nullable Essence essence, Component text, int color) {
        static Row heading(Component text) {
            return new Row(null, text, DIM);
        }
    }

    public static boolean wearing(@Nullable Player player) {
        return player != null && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.RESEARCHERS_GOGGLES.get());
    }

    @Nullable
    private static BlockPos lookedAt(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block.getBlockPos() : null;
    }

    // ---- The plate ------------------------------------------------------------------------------------------------

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!wearing(minecraft.player) || minecraft.level == null) {
            return;
        }
        BlockPos pos = lookedAt(minecraft);
        Reading reading = pos == null ? null : read(minecraft.level, pos);
        if (reading == null) {
            shown = null;
            return;
        }
        long now = Util.getMillis();
        if (!pos.equals(shown)) {
            shown = pos.immutable();
            shownSince = now;
        }
        float t = Math.min(1, (now - shownSince) / (float) SLIDE_MILLIS);
        float eased = 1 - (1 - t) * (1 - t);
        Font font = minecraft.font;
        int width = font.width(reading.title());
        for (Row row : reading.rows()) {
            width = Math.max(width, (row.essence() != null ? 12 : 0) + font.width(row.text()));
        }
        int w = width + 12;
        int h = 18 + reading.rows().size() * LINE;
        int x = graphics.guiWidth() / 2 + 14 - Math.round((1 - eased) * 10);
        int y = graphics.guiHeight() / 2 - h / 2;
        PixelArt.fill(graphics, x, y, x + w, y + h, 0xD0080614);
        int rim = PixelArt.argb(GOLD, 0.8F);
        PixelArt.fill(graphics, x, y, x + w, y + 1, rim);
        PixelArt.fill(graphics, x, y + h - 1, x + w, y + h, rim);
        PixelArt.fill(graphics, x, y, x + 1, y + h, rim);
        PixelArt.fill(graphics, x + w - 1, y, x + w, y + h, rim);
        PixelArt.flush(graphics);
        graphics.text(font, reading.title(), x + 6, y + 5, 0xFF000000 | GOLD, true);
        int ty = y + 17;
        for (Row row : reading.rows()) {
            int tx = x + 6;
            if (row.essence() != null) {
                PixelArt.essence(graphics, row.essence(), tx, ty - 1, PixelArt.ICON, PixelArt.argb(row.essence().color(), 1));
                tx += 12;
            }
            graphics.text(font, row.text(), tx, ty, row.color(), true);
            ty += LINE;
        }
        PixelArt.flush(graphics);
    }

    /** What the goggles see in the block, or null when it is not part of the essence works. */
    @Nullable
    private static Reading read(Level level, BlockPos pos) {
        // A block of a formed structure reads as the structure's heart.
        if (level.getBlockEntity(pos) instanceof com.pockyl.hextale.multiblock.MultiblockPartBlockEntity part) {
            pos = part.controller();
        }
        BlockState state = level.getBlockState(pos);
        Component name = state.getBlock().getName();
        BlockEntity entity = level.getBlockEntity(pos);
        List<Row> rows = new ArrayList<>();
        if (state.getBlock() instanceof ResearchTableBlock) {
            BlockPos main = ResearchTableBlock.mainPos(state, pos);
            InkSupply supply = InkSupply.around(level, main, state.getValue(ResearchTableBlock.FACING));
            rows.add(Row.heading(Component.translatable(KEY + "ink")));
            boolean any = false;
            for (Essence essence : Essence.values()) {
                int amount = supply.available()[essence.ordinal()];
                int tanks = supply.tanks(essence).size();
                if (tanks > 0) {
                    any = true;
                    rows.add(new Row(essence, Component.translatable(KEY + "ink.line", Component.translatable(essence.translationKey()),
                            amount, supply.capacity()[essence.ordinal()], tanks), 0xFF000000 | essence.color()));
                }
            }
            if (!any) {
                rows.add(Row.heading(Component.translatable(KEY + "ink.none")));
            }
            return new Reading(name, rows);
        }
        if (entity instanceof EssenceTankBlockEntity tank && tank.isCreative()) {
            rows.add(new Row(null, Component.translatable("block.hextale.creative_essence_tank.all"), 0xFFE8D0FF));
        } else if (entity instanceof EssenceTankBlockEntity tank) {
            if (tank.sticker() != null) {
                rows.add(new Row(tank.sticker(), Component.translatable(KEY + "jar.sticker", Component.translatable(tank.sticker().translationKey())),
                        0xFF000000 | tank.sticker().color()));
            }
            contents(rows, tank.tank().contents());
            rows.add(Row.heading(Component.translatable(KEY + "filled", tank.tank().amount(), tank.tank().capacity())));
            return new Reading(name, rows);
        }
        if (entity instanceof com.pockyl.hextale.block.CrucibleBlockEntity crucible) {
            rows.add(Row.heading(Component.translatable(KEY + "crucible", crucible.phase().describe(), Math.round(crucible.temperature()))));
            rows.add(Row.heading(Component.translatable(KEY + "filled", crucible.volume(), com.pockyl.hextale.block.CrucibleBlockEntity.CAPACITY)));
            rows.add(Row.heading(Component.translatable(KEY + "water", crucible.water(), com.pockyl.hextale.block.CrucibleBlockEntity.CAPACITY)));
            if (crucible.brew() != null) {
                rows.add(new Row(null, Component.translatable(KEY + "brew", Component.translatable(crucible.brew().translationKey()),
                        crucible.doses()), 0xFF000000 | crucible.brew().color()));
            }
            contents(rows, crucible.essence());
            return new Reading(name, rows);
        }
        if (entity instanceof com.pockyl.hextale.block.AxolotlTankBlockEntity tank) {
            rows.add(Row.heading(tank.status()));
            rows.add(Row.heading(Component.translatable(KEY + "tank.meals", tank.meals() + tank.getItem(com.pockyl.hextale.block.AxolotlTankBlockEntity.FOOD).getCount())));
            if (!tank.output().isEmpty()) {
                rows.add(Row.heading(Component.translatable(KEY + "tank.slime", tank.output().getCount())));
            }
            return new Reading(name, rows);
        }
        if (entity instanceof com.pockyl.hextale.block.ChemStillBlockEntity still) {
            rows.add(Row.heading(still.status()));
            rows.add(Row.heading(Component.translatable(KEY + "still.raw", still.input().getCount())));
            rows.add(Row.heading(Component.translatable(KEY + "still.refined", still.output().getCount())));
            return new Reading(name, rows);
        }
        if (entity instanceof com.pockyl.hextale.block.PinkBrazierBlockEntity brazier) {
            rows.add(Row.heading(brazier.status()));
            if (!brazier.fuel().isEmpty()) {
                rows.add(Row.heading(Component.translatable(KEY + "brazier.vials", brazier.fuel().getCount())));
            }
            return new Reading(name, rows);
        }
        if (entity instanceof com.pockyl.hextale.block.ChemBurnerBlockEntity burner) {
            rows.add(Row.heading(burner.status()));
            if (!burner.fuel().isEmpty()) {
                rows.add(Row.heading(Component.translatable(KEY + "burner.vials", burner.fuel().getCount())));
            }
            contents(rows, burner.tank());
            return new Reading(name, rows);
        }
        if (entity instanceof AlembicBlockEntity alembic) {
            contents(rows, alembic.tank().contents());
            rows.add(Row.heading(Component.translatable(KEY + "water", alembic.water(), AlembicBlockEntity.WATER_CAPACITY)));
            return new Reading(name, rows);
        }
        if (entity instanceof SeparatorBlockEntity separator) {
            section(rows, KEY + "mix", separator.mix().contents());
            section(rows, KEY + "concentrate", separator.extract().contents());
            section(rows, KEY + "rest", separator.rest().contents());
            return new Reading(name, rows);
        }
        if (state.getBlock() instanceof BrassPipeBlock || entity instanceof DrainValveBlockEntity) {
            rows.add(Row.heading(Component.translatable(KEY + "network")));
            contents(rows, network(level, pos));
            // The side under the crosshair: what it does, and how to change it.
            if (state.getBlock() instanceof BrassPipeBlock && Minecraft.getInstance().hitResult instanceof BlockHitResult hit) {
                Direction side = BrassPipeBlock.aimedSide(state, pos, hit);
                com.pockyl.hextale.block.PipeSide mode = BrassPipeBlock.side(state, side);
                if (mode != com.pockyl.hextale.block.PipeSide.NONE) {
                    rows.add(new Row(null, Component.translatable(KEY + "pipe_side",
                            Component.translatable(KEY + "pipe_side." + mode.getSerializedName())), 0xFF000000 | PIPE_COLOURS[mode.ordinal()]));
                }
                rows.add(Row.heading(Component.translatable("block.hextale.brass_pipe.hint")));
            }
            return new Reading(name, rows);
        }
        if (entity instanceof HexPylonBlockEntity pylon) {
            pylon(level, pylon, rows);
            return new Reading(name, rows);
        }
        // A block pylons send to: which ones, and how to untie it.
        List<HexPylonBlockEntity> senders = ClientPylons.linkingTo(level, pos);
        if (!senders.isEmpty()) {
            rows.add(Row.heading(Component.translatable(KEY + "pylon.senders")));
            for (HexPylonBlockEntity sender : senders) {
                rows.add(new Row(null, Component.translatable(KEY + "pylon.from", sender.getBlockState().getBlock().getName(),
                        distance(sender.gem(), Vec3.atCenterOf(pos))), 0xFF000000 | GOLD));
            }
            rows.add(Row.heading(Component.translatable(KEY + "pylon.untie")));
            return new Reading(name, rows);
        }
        return null;
    }

    /** A pylon: what it does with its runes, where its links go (a gone block in red), its runes, how to set it up. */
    private static void pylon(Level level, HexPylonBlockEntity pylon, List<Row> rows) {
        String seconds = String.format(java.util.Locale.ROOT, "%.1f", pylon.interval() / 20.0F);
        switch (pylon.kind()) {
            case GATHERING -> {
                rows.add(new Row(null, Component.translatable(KEY + "pylon.radius", pylon.radius()), TEXT));
                rows.add(new Row(null, Component.translatable(KEY + "pylon.pace", seconds, pylon.amount()), TEXT));
            }
            case SENDING -> rows.add(new Row(null, Component.translatable(KEY + "pylon.pace", seconds, pylon.amount()), TEXT));
            case SIPHON -> rows.add(new Row(null, Component.translatable(KEY + "pylon.pace_mb", seconds, pylon.essenceAmount()), TEXT));
            case SORTING -> {
                rows.add(new Row(null, Component.translatable(KEY + "pylon.pace_kinds", seconds, pylon.amount(), HexPylonBlockEntity.SORT_KINDS), TEXT));
                rows.add(new Row(null, Component.translatable(KEY + "pylon.storages", pylon.radius(),
                        HexPylonBlockEntity.sortingStorages(level, pylon.getBlockPos(), pylon.radius()).size()), TEXT));
            }
            case RELAY -> {
            }
        }
        rows.add(new Row(null, Component.translatable(KEY + "pylon.range", pylon.range()), TEXT));
        if (!pylon.ownerName().isEmpty()) {
            rows.add(new Row(null, Component.translatable(KEY + "pylon.owner", pylon.ownerName()), TEXT));
        }
        rows.add(Row.heading(Component.translatable(KEY + "pylon.links", pylon.links().size(), pylon.maxLinks())));
        for (PylonLink link : pylon.links()) {
            BlockState target = level.getBlockState(link.pos());
            Vec3 to = level.getBlockEntity(link.pos()) instanceof HexPylonBlockEntity relay ? relay.gem() : Vec3.atCenterOf(link.pos());
            boolean gone = target.isAir();
            rows.add(new Row(null, Component.translatable(KEY + "pylon.to", target.getBlock().getName(), distance(pylon.gem(), to)),
                    gone ? 0xFFE05A4A : 0xFF000000 | GOLD));
        }
        com.pockyl.hextale.transport.ItemFilter filter = pylon.filter();
        if (filter != null) {
            Component what = Component.translatable(filter.entries().isEmpty() ? "gui.hextale.pylon_filter.summary.empty"
                    : !filter.whitelist() ? "gui.hextale.pylon_filter.summary.blacklist"
                    : "gui.hextale.pylon_filter.summary." + filter.rule().getSerializedName());
            rows.add(new Row(null, Component.translatable(KEY + "pylon.filter", what), 0xFF000000 | GOLD));
        }
        if (pylon.sticker() != null) {
            rows.add(new Row(pylon.sticker(), Component.translatable(KEY + "pylon.sticker", Component.translatable(pylon.sticker().translationKey())),
                    0xFF000000 | pylon.sticker().color()));
        }
        for (var entry : pylon.runes().entries()) {
            rows.add(new Row(null, Component.translatable(entry.rune().translationKey())
                    .append(" " + "★".repeat(entry.stars())), 0xFF000000 | entry.rune().color()));
        }
        rows.add(Row.heading(Component.translatable("block.hextale.hex_pylon.hint.link")));
        rows.add(Row.heading(Component.translatable("block.hextale.hex_pylon.hint.clear")));
    }

    private static int distance(Vec3 a, Vec3 b) {
        return (int) Math.round(a.distanceTo(b));
    }

    private static void section(List<Row> rows, String key, EssenceMix mix) {
        rows.add(Row.heading(Component.translatable(key)));
        contents(rows, mix);
    }

    /** One row per essence in the mixture, then the unknown essence; "empty" when there is nothing. */
    private static void contents(List<Row> rows, EssenceMix mix) {
        if (mix.isEmpty()) {
            rows.add(new Row(null, Component.translatable("block.hextale.essence.empty"), DIM));
            return;
        }
        mix.asMap().forEach((essence, amount) -> rows.add(new Row(essence,
                Component.translatable(KEY + "amount", Component.translatable(essence.translationKey()), amount), 0xFF000000 | essence.color())));
        if (mix.unknown() > 0) {
            rows.add(new Row(null, Component.translatable(KEY + "unknown", mix.unknown()), 0xFF000000 | 0x8A80A0));
        }
    }

    /** Everything held by the vessels the pipes around {@code pos} reach. */
    private static EssenceMix network(Level level, BlockPos pos) {
        Set<BlockPos> pipes = EssenceNetwork.network(level, pos);
        EssenceMix total = EssenceMix.EMPTY;
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos pipe : pipes) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pipe.relative(direction);
                if (pipes.contains(next) || !seen.add(next)) {
                    continue;
                }
                BlockEntity entity = level.getBlockEntity(next);
                if (entity instanceof EssenceTankBlockEntity tank) {
                    total = total.plus(tank.tank().contents());
                } else if (entity instanceof AlembicBlockEntity alembic) {
                    total = total.plus(alembic.tank().contents());
                } else if (entity instanceof SeparatorBlockEntity separator) {
                    total = total.plus(separator.mix().contents()).plus(separator.extract().contents()).plus(separator.rest().contents());
                }
            }
        }
        return total;
    }

    // ---- Ink streams ----------------------------------------------------------------------------------------------

    /** While the goggles look at a Research Table, motes of each essence run from its tanks to the orb. */
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.isPaused() || !wearing(minecraft.player)) {
            return;
        }
        BlockPos pos = lookedAt(minecraft);
        if (pos == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ResearchTableBlock)) {
            return;
        }
        BlockPos main = ResearchTableBlock.mainPos(state, pos);
        if (!(level.getBlockEntity(main) instanceof ResearchTableBlockEntity table)) {
            return;
        }
        Vec3 orb = table.orbPosition();
        InkSupply supply = InkSupply.around(level, main, state.getValue(ResearchTableBlock.FACING));
        long time = level.getGameTime();
        for (Essence essence : Essence.values()) {
            for (EssenceTankBlockEntity tank : supply.tanks(essence)) {
                if ((time + tank.getBlockPos().hashCode()) % 4 != 0) {
                    continue;
                }
                // A mote of the ink flies from the jar's lid into the orb (flow motes start at their offset).
                Vec3 offset = com.pockyl.hextale.essence.JarReach.mouth(tank.getBlockPos()).subtract(orb);
                level.addParticle(com.pockyl.hextale.registry.ModParticles.flow(essence.color()), orb.x, orb.y, orb.z, offset.x, offset.y,
                        offset.z);
            }
        }
    }
}
