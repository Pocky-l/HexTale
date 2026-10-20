package com.pockyl.hextale.client.screen.machine;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.pockyl.hextale.block.InkSupply;
import com.pockyl.hextale.block.LabBenchBlockEntity;
import com.pockyl.hextale.chem.Flask;
import com.pockyl.hextale.chem.Reactions;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.screen.HexPlate;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.menu.LabBenchMenu;
import com.pockyl.hextale.network.LabActionPayload;
import com.pockyl.hextale.network.ResearchMessagePayload;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.research.ExperimentPlan;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Lab Bench's screen. On the left the journal: the chemtech topics as a grid of icons, and the open topic's compound
 * with the reactions that build it (each with the zone it forms in once the researcher has seen it form) and the seal.
 * In the middle the bench: a rack of droppers, one per essence the jars around hold (click or hold to let drops fall),
 * the flask over its burner with the liquid in its mixed colour, bubbles and the flash of each reaction, and what the
 * flask holds. On the right the thermometer with its five zones, the pressure gauge, the flame knob, the vent and the
 * sink. Everything is read from the bench (which runs the flask between the server's updates); actions go to the server.
 */
public class LabBenchScreen extends AbstractContainerScreen<LabBenchMenu> {
    private static final String KEY = "gui.hextale.lab_bench.";
    // The journal.
    private static final int JX = 8;
    private static final int JY = 18;
    private static final int JW = 78;
    private static final int JH = 148;
    private static final int CELLS_X = JX + 3;
    private static final int CELLS_Y = JY + 3;
    private static final int CELL = 18;
    private static final int COLUMNS = 4;
    private static final int PLAN_Y = JY + 64;
    private static final int CHAIN_Y = PLAN_Y + 34;
    private static final int CHAIN_STEP = 11;
    private static final int SEAL_X = JX + 3;
    private static final int SEAL_Y = JY + JH - 15;
    private static final int SEAL_W = JW - 6;
    // The bench.
    private static final int FX = 90;
    private static final int FY = 18;
    private static final int FW = 154;
    private static final int FH = 148;
    private static final int RACK_X = FX + 4;
    private static final int RACK_Y = FY + 3;
    private static final int PIPETTE = 17;
    private static final int PIPETTES = 5;
    private static final int TIP_Y = RACK_Y + 33;
    private static final int CX = 146;
    private static final int LIP_Y = FY + 46;
    private static final int NECK_BOTTOM = FY + 62;
    private static final int BELLY_Y = FY + 84;
    private static final int RADIUS = 21;
    private static final int NECK = 4;
    private static final int FLASK_BOTTOM = BELLY_Y + RADIUS;
    private static final int BURNER_Y = FLASK_BOTTOM + 7;
    private static final int CONTENTS_X = FX + 4;
    private static final int CONTENTS_Y = FY + 48;
    // The gauges.
    private static final int THERMO_X = 200;
    private static final int THERMO_TOP = FY + 4;
    private static final int THERMO_H = 100;
    private static final int GAUGE_CX = 229;
    private static final int DIAL_Y = FY + 16;
    private static final int DIAL = 24;
    private static final int KNOB_Y = FY + 48;
    private static final int KNOB = 18;
    private static final int BUTTONS_Y = FY + 64;
    private static final int BUTTON = 14;
    private static final int VENT_X = GAUGE_CX - BUTTON - 1;
    private static final int POUR_X = GAUGE_CX + 1;
    private static final int STATUS_Y = FY + FH - 12;
    /** Ticks between drops while a dropper is held. */
    private static final int DRIP_TICKS = 5;
    /** Milliseconds a drop takes from the tip to the liquid. */
    private static final long FALL_MS = 420;
    private static final int[] ZONE_COLORS = {0x5AA8FF, 0x5CE0D8, 0xB8E05A, 0xFF9A3C, 0xFF4A3A};

    private final List<Drop> drops = new ArrayList<>();
    private final List<Bubble> bubbles = new ArrayList<>();
    private final List<Shard> shards = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private List<Essence> rack = List.of();
    private int[] available = new int[Essence.COUNT];
    private int rackScroll;
    private long rackAt = -100;
    private @Nullable Essence held;
    private int heldTicks;
    private long lastFrame = -1;
    private long seenOutcome = Long.MIN_VALUE;
    private @Nullable Component toast;
    private int toastColor = HexPlate.TEXT;
    private long toastAt;

    private record Drop(Essence essence, int fromX, long start) {
    }

    private static final class Bubble {
        float x;
        float y;
        float speed;
        int color;
    }

    private static final class Shard {
        float x;
        float y;
        float vx;
        float vy;
        int color;
        int length;
        long born;
    }

    public LabBenchScreen(LabBenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LabBenchMenu.WIDTH, LabBenchMenu.INVENTORY_Y + 82);
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelX = LabBenchMenu.INVENTORY_X;
        inventoryLabelY = LabBenchMenu.INVENTORY_Y - 11;
    }

    private @Nullable LabBenchBlockEntity bench() {
        return minecraft == null || minecraft.level == null ? null
                : minecraft.level.getBlockEntity(menu.pos()) instanceof LabBenchBlockEntity entity ? entity : null;
    }

    private void send(LabActionPayload.Action action, int value) {
        ClientPacketDistributor.sendToServer(new LabActionPayload(menu.pos(), action, value));
    }

    private void sound(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    public void onMessage(Component text, ResearchMessagePayload.Tone tone) {
        toast = text;
        toastColor = switch (tone) {
            case BAD -> 0xFFE0503C;
            case WIN, GOOD -> 0xFF6CFF8A;
            default -> HexPlate.TEXT;
        };
        toastAt = Util.getMillis();
    }

    // ---- Ticking --------------------------------------------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        LabBenchBlockEntity bench = bench();
        if (bench == null || minecraft == null || minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (now - rackAt >= 10) {
            refreshRack(bench);
            rackAt = now;
        }
        if (held != null) {
            heldTicks++;
            if (heldTicks % DRIP_TICKS == 0) {
                drip(bench, held);
            }
        }
        if (bench.outcomeAt() != seenOutcome) {
            if (seenOutcome != Long.MIN_VALUE && now - bench.outcomeAt() < 10 && bench.outcome() == LabBenchBlockEntity.OUTCOME_BURST) {
                shatter(bench);
            }
            seenOutcome = bench.outcomeAt();
        }
    }

    /** The droppers: the plan's primals first (even when no jar holds them), then every other essence the jars hold. */
    private void refreshRack(LabBenchBlockEntity bench) {
        InkSupply supply = bench.supply();
        available = supply.available().clone();
        List<Essence> out = new ArrayList<>();
        ExperimentPlan plan = bench.plan();
        if (plan != null) {
            out.addAll(plan.reagents());
        }
        List<Essence> others = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            if (available[essence.ordinal()] >= LabBenchBlockEntity.DROP_MB && !out.contains(essence)) {
                others.add(essence);
            }
        }
        others.sort((a, b) -> Integer.compare(available[b.ordinal()], available[a.ordinal()]));
        out.addAll(others);
        rack = out;
        rackScroll = Math.clamp(rackScroll, 0, Math.max(0, rack.size() - PIPETTES));
    }

    private boolean canDrop(LabBenchBlockEntity bench, Essence essence) {
        return bench.hasFlask() && available[essence.ordinal()] >= LabBenchBlockEntity.DROP_MB
                && bench.flask().total() + 1 <= Flask.CAPACITY;
    }

    private void drip(LabBenchBlockEntity bench, Essence essence) {
        if (!canDrop(bench, essence)) {
            held = null;
            return;
        }
        int index = rack.indexOf(essence) - rackScroll;
        int fromX = RACK_X + Math.clamp(index, 0, PIPETTES - 1) * PIPETTE + PIPETTE / 2;
        drops.add(new Drop(essence, fromX, Util.getMillis()));
        available[essence.ordinal()] -= LabBenchBlockEntity.DROP_MB;
        send(LabActionPayload.Action.DROP, essence.ordinal());
    }

    /** The flask burst: glass shards fly off, drops of the liquid splash. */
    private void shatter(LabBenchBlockEntity bench) {
        long now = Util.getMillis();
        for (int i = 0; i < 26; i++) {
            Shard shard = new Shard();
            float angle = random.nextFloat() * Mth.TWO_PI;
            float speed = 40 + random.nextFloat() * 90;
            shard.x = CX;
            shard.y = BELLY_Y;
            shard.vx = Mth.cos(angle) * speed;
            shard.vy = Mth.sin(angle) * speed - 40;
            shard.color = i % 3 == 0 ? 0x9CFF6A : 0xCFEFF5;
            shard.length = 1 + random.nextInt(3);
            shard.born = now;
            shards.add(shard);
        }
        bubbles.clear();
        drops.clear();
        held = null;
    }

    // ---- Input ----------------------------------------------------------------------------------------------------------

    private boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private int topicAt(double mouseX, double mouseY) {
        List<ResearchTopic> topics = ResearchTopics.lab();
        for (int i = 0; i < topics.size(); i++) {
            if (over(mouseX, mouseY, CELLS_X + i % COLUMNS * CELL, CELLS_Y + i / COLUMNS * CELL, CELL, CELL)) {
                return i;
            }
        }
        return -1;
    }

    private @Nullable Essence pipetteAt(double mouseX, double mouseY) {
        for (int i = 0; i < PIPETTES && i + rackScroll < rack.size(); i++) {
            if (over(mouseX, mouseY, RACK_X + i * PIPETTE, RACK_Y, PIPETTE, TIP_Y - RACK_Y + 11)) {
                return rack.get(i + rackScroll);
            }
        }
        return null;
    }

    private boolean overRack(double mouseX, double mouseY) {
        return over(mouseX, mouseY, RACK_X, RACK_Y, PIPETTES * PIPETTE, TIP_Y - RACK_Y + 11);
    }

    private boolean overKnob(double mouseX, double mouseY) {
        return over(mouseX, mouseY, GAUGE_CX - KNOB / 2 - 2, KNOB_Y - KNOB / 2 - 2, KNOB + 4, KNOB + 4);
    }

    private boolean overThermometer(double mouseX, double mouseY) {
        return over(mouseX, mouseY, THERMO_X - 1, THERMO_TOP - 1, 11, THERMO_H + 16);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        LabBenchBlockEntity bench = bench();
        if (bench == null || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        double mx = event.x();
        double my = event.y();
        int topic = topicAt(mx, my);
        if (topic >= 0) {
            ResearchTopic chosen = ResearchTopics.lab().get(topic);
            if (Guide.client().canResearch(chosen, ClientKnowledge.get()) && chosen != bench.topic()) {
                send(LabActionPayload.Action.SELECT, chosen.index());
                sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.8F);
                rackAt = -100;
            }
            return true;
        }
        Essence essence = pipetteAt(mx, my);
        if (essence != null) {
            if (canDrop(bench, essence)) {
                held = essence;
                heldTicks = 0;
                drip(bench, essence);
            } else {
                sound(SoundEvents.DISPENSER_FAIL, 1.6F, 0.25F);
            }
            return true;
        }
        if (overKnob(mx, my)) {
            turnFlame(bench, mx < leftPos + GAUGE_CX ? -1 : 1);
            return true;
        }
        if (over(mx, my, VENT_X, BUTTONS_Y, BUTTON, BUTTON)) {
            send(LabActionPayload.Action.VENT, 0);
            return true;
        }
        if (over(mx, my, POUR_X, BUTTONS_Y, BUTTON, BUTTON)) {
            if (!bench.flask().isEmpty()) {
                send(LabActionPayload.Action.POUR, 0);
            }
            return true;
        }
        if (over(mx, my, SEAL_X, SEAL_Y, SEAL_W, 12)) {
            if (sealable(bench)) {
                send(LabActionPayload.Action.SEAL, 0);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && held != null) {
            held = null;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        LabBenchBlockEntity bench = bench();
        if (bench != null && (overKnob(mouseX, mouseY) || overThermometer(mouseX, mouseY)
                || over(mouseX, mouseY, CX - RADIUS - 6, BURNER_Y - 6, RADIUS * 2 + 12, 20))) {
            turnFlame(bench, scrollY > 0 ? 1 : -1);
            return true;
        }
        if (overRack(mouseX, mouseY) && rack.size() > PIPETTES) {
            rackScroll = Math.clamp(rackScroll - (int) Math.signum(scrollY), 0, rack.size() - PIPETTES);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void turnFlame(LabBenchBlockEntity bench, int step) {
        int flame = Math.clamp(bench.flask().flame() + step, 0, Flask.FLAMES - 1);
        if (flame != bench.flask().flame()) {
            bench.flask().setFlame(flame);
            send(LabActionPayload.Action.FLAME, flame);
            sound(SoundEvents.LEVER_CLICK, 1.4F + flame * 0.1F, 0.25F);
        }
    }

    @Override
    public void removed() {
        held = null;
        super.removed();
    }

    private static boolean sealable(LabBenchBlockEntity bench) {
        ExperimentPlan plan = bench.plan();
        return plan != null && bench.ready() && plan.sampleFits(bench.getItem(LabBenchBlockEntity.SAMPLE));
    }

    // ---- Drawing --------------------------------------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        long nowMs = Util.getMillis();
        float dt = lastFrame < 0 ? 0 : Math.min(0.1F, (nowMs - lastFrame) / 1000.0F);
        lastFrame = nowMs;
        int x = leftPos;
        int y = topPos;
        HexPlate.plate(graphics, x, y, imageWidth, imageHeight);
        HexPlate.divider(graphics, x, y + LabBenchMenu.INVENTORY_Y - 15, imageWidth);
        HexPlate.well(graphics, x + JX, y + JY, JW, JH);
        HexPlate.well(graphics, x + FX, y + FY, FW, FH);
        for (Slot slot : menu.slots) {
            if (slot.index == LabBenchBlockEntity.OUTPUT) {
                HexPlate.outputSlot(graphics, x + slot.x, y + slot.y);
            } else if (slot.index < LabBenchBlockEntity.SIZE) {
                HexPlate.slot(graphics, x + slot.x, y + slot.y);
            }
        }
        PixelArt.flush(graphics);
        LabBenchBlockEntity bench = bench();
        if (bench == null) {
            return;
        }
        RuneKnowledge knowledge = ClientKnowledge.get();
        journal(graphics, bench, knowledge, mouseX, mouseY);
        rack(graphics, bench, mouseX, mouseY);
        stand(graphics, bench);
        if (bench.hasFlask()) {
            flask(graphics, bench, dt);
        }
        burner(graphics, bench.flask().flame());
        fallingDrops(graphics, bench);
        flyingShards(graphics, dt);
        contents(graphics, bench);
        thermometer(graphics, bench, knowledge);
        dial(graphics, bench.flask().pressure());
        knob(graphics, bench.flask().flame(), overKnob(mouseX, mouseY));
        iconButton(graphics, VENT_X, BUTTONS_Y, over(mouseX, mouseY, VENT_X, BUTTONS_Y, BUTTON, BUTTON), true, false);
        iconButton(graphics, POUR_X, BUTTONS_Y, over(mouseX, mouseY, POUR_X, BUTTONS_Y, BUTTON, BUTTON), !bench.flask().isEmpty(), true);
        PixelArt.flush(graphics);
    }

    /** The topic grid and the open topic's compound, its reactions and the seal. */
    private void journal(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench, RuneKnowledge knowledge, int mouseX, int mouseY) {
        List<ResearchTopic> topics = ResearchTopics.lab();
        int hovered = topicAt(mouseX, mouseY);
        for (int i = 0; i < topics.size(); i++) {
            ResearchTopic topic = topics.get(i);
            int cx = leftPos + CELLS_X + i % COLUMNS * CELL;
            int cy = topPos + CELLS_Y + i / COLUMNS * CELL;
            boolean open = Guide.client().canResearch(topic, knowledge);
            HexPlate.cell(graphics, cx, cy, CELL, topic == bench.topic(), hovered == i && open);
            PixelArt.flush(graphics);
            if (open) {
                graphics.item(topic.icon(), cx + 1, cy + 1);
                if (knowledge.researched(topic)) {
                    // A small green tick in the corner: worked out already.
                    PixelArt.fill(graphics, cx + 12, cy + 13, cx + 14, cy + 15, PixelArt.argb(0x6CFF8A, 1));
                    PixelArt.fill(graphics, cx + 14, cy + 11, cx + 16, cy + 13, PixelArt.argb(0x6CFF8A, 1));
                }
            } else {
                PixelArt.disc(graphics, cx + 9, cy + 9, 10, PixelArt.argb(0x2A2238, 1), PixelArt.argb(0x4A4060, 1));
            }
        }
        HexPlate.divider(graphics, leftPos + JX - HexPlate.BORDER + 2, topPos + PLAN_Y - 4, JW + 4);
        ExperimentPlan plan = bench.plan();
        if (plan == null) {
            return;
        }
        Essence target = plan.target();
        Flask flask = bench.flask();
        int px = leftPos + JX + 3;
        PixelArt.essence(graphics, target, px, topPos + PLAN_Y, PixelArt.ICON, 0xFF000000 | target.color());
        // The amount and purity bars: grey to green as the flask gets there.
        float amount = Math.min(1, flask.amount(target) / plan.amount());
        float purity = flask.purity(target);
        bar(graphics, px, topPos + PLAN_Y + 21, JW - 6, amount, amount >= 0.99F ? 0x6CFF8A : target.color());
        bar(graphics, px, topPos + PLAN_Y + 31, JW - 6, Math.min(1, purity / plan.purity()), purity >= plan.purity() - 0.005F ? 0x6CFF8A : 0xE8D25A);
        List<Essence> chain = plan.chain();
        for (int i = 0; i < chain.size() && i < 3; i++) {
            Essence compound = chain.get(chain.size() - 1 - i);
            int ly = topPos + CHAIN_Y + i * CHAIN_STEP;
            Essence[] parents = compound.parents();
            PixelArt.essence(graphics, parents[0], px, ly, PixelArt.ICON, 0xFF000000 | parents[0].color());
            PixelArt.fill(graphics, px + 11, ly + 4, px + 14, ly + 5, PixelArt.argb(HexPlate.BRASS_LIGHT, 0.8F));
            PixelArt.fill(graphics, px + 12, ly + 3, px + 13, ly + 6, PixelArt.argb(HexPlate.BRASS_LIGHT, 0.8F));
            PixelArt.essence(graphics, parents[1], px + 16, ly, PixelArt.ICON, 0xFF000000 | parents[1].color());
            PixelArt.line(graphics, px + 28, ly + 4, px + 33, ly + 4, PixelArt.argb(HexPlate.BRASS_LIGHT, 0.8F));
            PixelArt.line(graphics, px + 32, ly + 2, px + 34, ly + 4, PixelArt.argb(HexPlate.BRASS_LIGHT, 0.8F));
            PixelArt.line(graphics, px + 32, ly + 6, px + 34, ly + 4, PixelArt.argb(HexPlate.BRASS_LIGHT, 0.8F));
            PixelArt.essence(graphics, compound, px + 37, ly, PixelArt.ICON, 0xFF000000 | compound.color());
            zoneChip(graphics, px + 50, ly + 1, knowledge.observed(compound) ? Reactions.zone(compound) : -1);
        }
        boolean ready = sealable(bench);
        HexPlate.button(graphics, font, leftPos + SEAL_X, topPos + SEAL_Y, SEAL_W, 12, Component.translatable(KEY + "seal"),
                ready && over(mouseX, mouseY, SEAL_X, SEAL_Y, SEAL_W, 12), ready);
    }

    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int w, float share, int color) {
        PixelArt.fill(graphics, x, y, x + w, y + 3, PixelArt.argb(0x0A0810, 1));
        PixelArt.fill(graphics, x, y, x + Math.round(w * Mth.clamp(share, 0, 1)), y + 3, PixelArt.argb(color, 1));
    }

    /** A small chip in the zone's colour; a grey one with a dot while the zone is unknown. */
    private static void zoneChip(GuiGraphicsExtractor graphics, int x, int y, int zone) {
        if (zone < 0) {
            PixelArt.fill(graphics, x, y, x + 20, y + 7, PixelArt.argb(0x2A2238, 1));
            PixelArt.fill(graphics, x + 9, y + 3, x + 11, y + 5, PixelArt.argb(0x7A7090, 1));
            return;
        }
        PixelArt.fill(graphics, x, y, x + 20, y + 7, PixelArt.argb(ZONE_COLORS[zone], 0.85F));
        for (int i = 0; i <= zone; i++) {
            PixelArt.fill(graphics, x + 2 + i * 4, y + 2, x + 4 + i * 4, y + 5, PixelArt.argb(0x16121F, 0.9F));
        }
    }

    /** The rack of droppers: a wooden shelf, a rubber bulb and a glass tube with the essence in it, its sign under the tip. */
    private void rack(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench, int mouseX, int mouseY) {
        int x0 = leftPos + RACK_X;
        int y0 = topPos + RACK_Y;
        PixelArt.fill(graphics, x0 - 2, y0 + 10, x0 + PIPETTES * PIPETTE + 2, y0 + 13, PixelArt.argb(0x4A2E18, 1));
        PixelArt.fill(graphics, x0 - 2, y0 + 10, x0 + PIPETTES * PIPETTE + 2, y0 + 11, PixelArt.argb(0x7A5030, 1));
        Essence hovered = pipetteAt(mouseX, mouseY);
        for (int i = 0; i < PIPETTES; i++) {
            int px = x0 + i * PIPETTE + 4;
            if (i + rackScroll >= rack.size()) {
                // An empty hole in the shelf.
                PixelArt.fill(graphics, px + 2, y0 + 11, px + 7, y0 + 12, PixelArt.argb(0x1A1008, 1));
                continue;
            }
            Essence essence = rack.get(i + rackScroll);
            boolean usable = canDrop(bench, essence);
            boolean lift = essence == hovered && usable;
            int dy = lift ? -2 : 0;
            if (essence == held) {
                dy = -1;
            }
            int bulb = essence == held ? 0x6A1818 : 0x9A3030;
            PixelArt.fill(graphics, px + 1, y0 + dy, px + 8, y0 + 7 + dy, PixelArt.argb(bulb, 1));
            PixelArt.fill(graphics, px, y0 + 1 + dy, px + 9, y0 + 6 + dy, PixelArt.argb(bulb, 1));
            PixelArt.fill(graphics, px + 2, y0 + 1 + dy, px + 4, y0 + 3 + dy, PixelArt.argb(0xD86060, 0.8F));
            PixelArt.fill(graphics, px + 1, y0 + 7 + dy, px + 8, y0 + 8 + dy, PixelArt.argb(HexPlate.BRASS, 1));
            // The tube: glass edges, the essence inside as full as the jars around are.
            int top = y0 + 8 + dy;
            int bottom = TIP_Y + topPos - 4 + dy;
            PixelArt.fill(graphics, px + 2, top, px + 7, bottom, PixelArt.argb(0xBFE8F0, 0.55F));
            PixelArt.fill(graphics, px + 3, top, px + 6, bottom, PixelArt.argb(0x0A0810, 0.7F));
            float full = Math.min(1, available[essence.ordinal()] / 64.0F);
            int level = bottom - Math.round((bottom - top - 1) * full);
            if (full > 0) {
                PixelArt.fill(graphics, px + 3, level, px + 6, bottom, PixelArt.argb(essence.color(), usable ? 1 : 0.45F));
                PixelArt.fill(graphics, px + 3, level, px + 4, bottom, PixelArt.argb(0xFFFFFF, 0.25F));
            }
            PixelArt.fill(graphics, px + 3, bottom, px + 6, bottom + 2, PixelArt.argb(0xBFE8F0, 0.55F));
            PixelArt.fill(graphics, px + 4, bottom + 2, px + 5, bottom + 4, PixelArt.argb(0xBFE8F0, 0.55F));
            int sign = usable ? 0xFF000000 | essence.color() : 0xFF4A4060;
            PixelArt.essence(graphics, essence, px, TIP_Y + topPos + 2, PixelArt.ICON, sign);
            if (lift) {
                PixelArt.fill(graphics, px - 1, TIP_Y + topPos + 12, px + 10, TIP_Y + topPos + 13, PixelArt.argb(HexPlate.BRASS_LIGHT, 0.8F));
            }
        }
        if (rack.size() > PIPETTES) {
            int more = PixelArt.argb(HexPlate.BRASS_LIGHT, 0.7F);
            if (rackScroll > 0) {
                PixelArt.line(graphics, x0 - 1, y0 + 16, x0 - 3, y0 + 18, more);
                PixelArt.line(graphics, x0 - 3, y0 + 18, x0 - 1, y0 + 20, more);
            }
            if (rackScroll + PIPETTES < rack.size()) {
                int ex = x0 + PIPETTES * PIPETTE;
                PixelArt.line(graphics, ex + 1, y0 + 16, ex + 3, y0 + 18, more);
                PixelArt.line(graphics, ex + 3, y0 + 18, ex + 1, y0 + 20, more);
            }
        }
    }

    /** The ring stand: a rod beside the flask, a clamp round its neck, the ring it sits in. */
    private void stand(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench) {
        int rod = leftPos + CX + RADIUS + 6;
        int iron = PixelArt.argb(0x3A3640, 1);
        int ironLight = PixelArt.argb(0x6A6670, 1);
        PixelArt.fill(graphics, rod, topPos + LIP_Y + 2, rod + 2, topPos + BURNER_Y + 14, iron);
        PixelArt.fill(graphics, rod, topPos + LIP_Y + 2, rod + 1, topPos + BURNER_Y + 14, ironLight);
        PixelArt.fill(graphics, rod - 14, topPos + BURNER_Y + 14, rod + 8, topPos + BURNER_Y + 16, iron);
        int clampY = topPos + NECK_BOTTOM - 8;
        PixelArt.fill(graphics, leftPos + CX + NECK + 1, clampY, rod, clampY + 2, iron);
        PixelArt.fill(graphics, leftPos + CX + NECK, clampY - 1, leftPos + CX + NECK + 2, clampY + 3, PixelArt.argb(HexPlate.BRASS, 1));
        int ringY = topPos + FLASK_BOTTOM - 4;
        PixelArt.fill(graphics, leftPos + CX - 12, ringY, rod, ringY + 1, iron);
        if (!bench.hasFlask()) {
            // Where the flask goes: a faint outline.
            int ghost = PixelArt.argb(0xBFE8F0, 0.12F);
            PixelArt.disc(graphics, leftPos + CX, topPos + BELLY_Y, RADIUS * 2, 0, ghost);
            PixelArt.fill(graphics, leftPos + CX - NECK, topPos + LIP_Y, leftPos + CX - NECK + 1, topPos + NECK_BOTTOM, ghost);
            PixelArt.fill(graphics, leftPos + CX + NECK, topPos + LIP_Y, leftPos + CX + NECK + 1, topPos + NECK_BOTTOM, ghost);
        }
    }

    /** Half the flask's inner width on a row (relative to the lip), 0 outside it. */
    private static int halfWidth(int row) {
        int y = LIP_Y + row;
        if (y < LIP_Y || y > FLASK_BOTTOM) {
            return 0;
        }
        float dy = y + 0.5F - BELLY_Y;
        int belly = dy * dy < RADIUS * RADIUS ? (int) Math.floor(Mth.sqrt(RADIUS * RADIUS - dy * dy)) : 0;
        return y < NECK_BOTTOM ? Math.max(NECK, y >= NECK_BOTTOM - 3 ? belly : 0) : Math.max(belly, y < NECK_BOTTOM + 3 ? NECK : 0);
    }

    /**
     * The flask: glass with a lit left edge, the liquid filled from the bottom by its volume in its mixed colour, its
     * surface trembling with the heat, bubbles rising (in the colour of what forms, while something forms), sparks of a
     * fresh reaction, vapour in the neck under pressure.
     */
    private void flask(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench, float dt) {
        Flask flask = bench.flask();
        int rows = FLASK_BOTTOM - LIP_Y + 1;
        // The liquid's surface: fill the belly rows from the bottom until their area matches the flask's share.
        float share = flask.total() / Flask.CAPACITY;
        int bellyArea = 0;
        for (int row = NECK_BOTTOM - LIP_Y; row < rows; row++) {
            bellyArea += halfWidth(row) * 2;
        }
        float wanted = bellyArea * share;
        int surface = rows;
        float filled = 0;
        while (surface > 0 && filled < wanted) {
            surface--;
            filled += halfWidth(surface) * 2;
        }
        long nowMs = Util.getMillis();
        float temperature = flask.temperature();
        int liquid = flask.color();
        boolean fresh = minecraft != null && minecraft.level != null && bench.formed() != null
                && minecraft.level.getGameTime() - bench.formedAt() < 20;
        int glass = PixelArt.argb(0xCFEFF5, 0.7F);
        int glassLit = PixelArt.argb(0xFFFFFF, 0.85F);
        for (int row = 0; row < rows; row++) {
            int hw = halfWidth(row);
            if (hw <= 0) {
                continue;
            }
            int sy = topPos + LIP_Y + row;
            int left = leftPos + CX - hw;
            int right = leftPos + CX + hw;
            PixelArt.fill(graphics, left, sy, right, sy + 1, PixelArt.argb(0x9CD0E0, 0.08F));
            if (row >= surface && !flask.isEmpty()) {
                float depth = (row - surface) / (float) Math.max(1, rows - surface);
                int c = PixelArt.mix(liquid, 0x000000, 0.12F + depth * 0.25F);
                if (row == surface) {
                    // The surface line trembles with the heat.
                    float wobble = temperature / 100.0F;
                    for (int px = left; px < right; px++) {
                        float wave = Mth.sin(px * 0.7F + nowMs / (160.0F - wobble * 90)) * wobble;
                        PixelArt.fill(graphics, px, sy + (wave > 0.4F ? 1 : 0), px + 1, sy + 1, PixelArt.argb(PixelArt.mix(liquid, 0xFFFFFF, 0.45F), 0.95F));
                    }
                } else {
                    PixelArt.fill(graphics, left, sy, right, sy + 1, PixelArt.argb(c, 0.92F));
                }
            }
            // Glass walls; the rim of the lip is thicker.
            PixelArt.fill(graphics, left - 1, sy, left, sy + 1, glassLit);
            PixelArt.fill(graphics, right, sy, right + 1, sy + 1, glass);
        }
        PixelArt.fill(graphics, leftPos + CX - NECK - 2, topPos + LIP_Y - 2, leftPos + CX + NECK + 2, topPos + LIP_Y, glass);
        int bottom = topPos + FLASK_BOTTOM + 1;
        int bw = halfWidth(rows - 1);
        PixelArt.fill(graphics, leftPos + CX - bw, bottom - 1, leftPos + CX + bw, bottom, glass);
        // A highlight down the left of the belly.
        for (int k = 0; k < 9; k++) {
            int hy = topPos + BELLY_Y - 12 + k;
            int hx = leftPos + CX - RADIUS + 5 + (k < 3 ? 2 - k : k > 6 ? k - 6 : 0);
            PixelArt.fill(graphics, hx, hy, hx + 1, hy + 1, PixelArt.argb(0xFFFFFF, 0.5F));
        }
        if (!flask.isEmpty()) {
            bubbles(graphics, flask, surface, fresh ? bench.formed().color() : PixelArt.mix(liquid, 0xFFFFFF, 0.5F), fresh, dt);
            if (fresh) {
                RandomSource sparks = RandomSource.create(nowMs / 60);
                for (int i = 0; i < 6; i++) {
                    int row = surface + 1 + sparks.nextInt(Math.max(1, rows - surface - 2));
                    int hw = halfWidth(row);
                    if (hw > 1) {
                        int sx = leftPos + CX - hw + 1 + sparks.nextInt(hw * 2 - 1);
                        PixelArt.fill(graphics, sx, topPos + LIP_Y + row, sx + 1, topPos + LIP_Y + row + 1,
                                PixelArt.argb(PixelArt.mix(bench.formed().color(), 0xFFFFFF, 0.6F), 0.9F));
                    }
                }
            }
        }
        // Vapour in the neck while pressure builds.
        float pressure = flask.pressure() / Flask.BURST;
        if (pressure > 0.15F) {
            RandomSource vapour = RandomSource.create(nowMs / 90);
            int puffs = Math.round(pressure * 10);
            for (int i = 0; i < puffs; i++) {
                int vy = topPos + LIP_Y + vapour.nextInt(Math.max(1, Math.min(surface, NECK_BOTTOM - LIP_Y + 6)));
                int vx = leftPos + CX - NECK + 1 + vapour.nextInt(NECK * 2 - 1);
                PixelArt.fill(graphics, vx, vy, vx + 1, vy + 1, PixelArt.argb(0xFFFFFF, 0.25F + pressure * 0.4F));
            }
        }
    }

    /** Bubbles rise from the bottom while the liquid is warm or reacting, and pop at the surface. */
    private void bubbles(GuiGraphicsExtractor graphics, Flask flask, int surface, int color, boolean reacting, float dt) {
        float heat = Math.max(0, (flask.temperature() - 30) / 70);
        float rate = heat * 14 + (reacting ? 26 : 0);
        int rows = FLASK_BOTTOM - LIP_Y;
        if (random.nextFloat() < rate * dt && bubbles.size() < 40 && surface < rows - 2) {
            int row = rows - 1 - random.nextInt(3);
            int hw = halfWidth(row);
            if (hw > 2) {
                Bubble bubble = new Bubble();
                bubble.x = CX - hw + 2 + random.nextInt(hw * 2 - 3);
                bubble.y = LIP_Y + row;
                bubble.speed = 14 + random.nextFloat() * 22 + heat * 20;
                bubble.color = color;
                bubbles.add(bubble);
            }
        }
        for (Iterator<Bubble> it = bubbles.iterator(); it.hasNext(); ) {
            Bubble bubble = it.next();
            bubble.y -= bubble.speed * dt;
            bubble.x += Mth.sin(bubble.y * 0.6F) * 0.15F;
            int row = Math.round(bubble.y) - LIP_Y;
            if (row <= surface || row < 0) {
                it.remove();
                continue;
            }
            int hw = halfWidth(row);
            bubble.x = Mth.clamp(bubble.x, CX - hw + 1, CX + hw - 2);
            int bx = leftPos + Math.round(bubble.x);
            int by = topPos + Math.round(bubble.y);
            PixelArt.fill(graphics, bx, by, bx + 1, by + 1, PixelArt.argb(bubble.color, 0.95F));
            if (bubble.speed > 30) {
                PixelArt.fill(graphics, bx + 1, by, bx + 2, by + 1, PixelArt.argb(bubble.color, 0.5F));
            }
        }
    }

    /** The little burner under the flask: a brass body and a flame that grows with the setting. */
    private void burner(GuiGraphicsExtractor graphics, int flame) {
        int bx = leftPos + CX;
        int by = topPos + BURNER_Y;
        PixelArt.fill(graphics, bx - 5, by, bx + 5, by + 9, PixelArt.argb(HexPlate.BRASS_DARK, 1));
        PixelArt.fill(graphics, bx - 4, by + 1, bx + 4, by + 8, PixelArt.argb(HexPlate.BRASS, 1));
        PixelArt.fill(graphics, bx - 4, by + 1, bx - 3, by + 8, PixelArt.argb(HexPlate.BRASS_LIGHT, 1));
        PixelArt.fill(graphics, bx - 7, by + 9, bx + 7, by + 11, PixelArt.argb(0x3A3640, 1));
        PixelArt.fill(graphics, bx - 2, by - 2, bx + 2, by, PixelArt.argb(0x3A3640, 1));
        if (flame <= 0) {
            return;
        }
        long now = Util.getMillis();
        int height = 1 + flame * 2;
        for (int k = 0; k < height; k++) {
            float t = k / (float) height;
            int w = Math.max(1, Math.round((1 - t) * (2 + flame * 0.6F) + Mth.sin(now / 70.0F + k) * 0.6F));
            int color = PixelArt.mix(0x3AF0FF, 0x9CFF6A, t);
            PixelArt.fill(graphics, bx - w, by - 3 - k, bx + w, by - 2 - k, PixelArt.argb(color, 0.9F - t * 0.5F));
        }
    }

    /** Drops falling from a tip to the flask's mouth and down into the liquid. */
    private void fallingDrops(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench) {
        long now = Util.getMillis();
        for (Iterator<Drop> it = drops.iterator(); it.hasNext(); ) {
            Drop drop = it.next();
            float t = (now - drop.start()) / (float) FALL_MS;
            if (t >= 1) {
                it.remove();
                continue;
            }
            float sx = Mth.lerp(Math.min(1, t * 1.6F), drop.fromX(), CX);
            float sy = TIP_Y + 4 + (BELLY_Y - TIP_Y) * t * t;
            int px = leftPos + Math.round(sx);
            int py = topPos + Math.round(sy);
            int color = drop.essence().color();
            PixelArt.fill(graphics, px, py, px + 2, py + 2, PixelArt.argb(color, 1));
            PixelArt.fill(graphics, px, py - 1, px + 1, py, PixelArt.argb(color, 0.6F));
        }
    }

    private void flyingShards(GuiGraphicsExtractor graphics, float dt) {
        long now = Util.getMillis();
        for (Iterator<Shard> it = shards.iterator(); it.hasNext(); ) {
            Shard shard = it.next();
            if (now - shard.born > 1400) {
                it.remove();
                continue;
            }
            shard.vy += 220 * dt;
            shard.x += shard.vx * dt;
            shard.y += shard.vy * dt;
            float fade = 1 - (now - shard.born) / 1400.0F;
            int sx = leftPos + Math.round(shard.x);
            int sy = topPos + Math.round(shard.y);
            if (sx < leftPos + FX || sx > leftPos + FX + FW || sy < topPos + FY || sy > topPos + FY + FH) {
                continue;
            }
            PixelArt.fill(graphics, sx, sy, sx + shard.length, sy + 1, PixelArt.argb(shard.color, fade));
        }
    }

    /** What the flask holds, most first: the essence's sign and its parts. */
    private void contents(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench) {
        List<Essence> in = bench.flask().contents();
        for (int i = 0; i < in.size() && i < 7; i++) {
            Essence essence = in.get(i);
            int ey = topPos + CONTENTS_Y + i * 10;
            PixelArt.essence(graphics, essence, leftPos + CONTENTS_X, ey, PixelArt.ICON, 0xFF000000 | essence.color());
        }
    }

    /** The thermometer: five zone bands, the column of the liquid's heat, where the flame pulls, the zones known. */
    private void thermometer(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench, RuneKnowledge knowledge) {
        int x = leftPos + THERMO_X;
        int top = topPos + THERMO_TOP;
        Flask flask = bench.flask();
        HexPlate.well(graphics, x - 1, top - 1, 10, THERMO_H + 2);
        int zone = Reactions.zoneAt(flask.temperature());
        float span = THERMO_H / (float) Reactions.ZONES;
        for (int z = 0; z < Reactions.ZONES; z++) {
            int zy1 = top + THERMO_H - Math.round(z * span);
            int zy0 = top + THERMO_H - Math.round((z + 1) * span);
            PixelArt.fill(graphics, x, zy0, x + 8, zy1, PixelArt.argb(ZONE_COLORS[z], z == zone ? 0.32F : 0.14F));
            if (z > 0) {
                PixelArt.fill(graphics, x, zy1, x + 8, zy1 + 1, PixelArt.argb(0x000000, 0.5F));
            }
        }
        int column = Math.round(THERMO_H * flask.temperature() / Reactions.MAX_TEMPERATURE);
        int mercury = ZONE_COLORS[zone];
        PixelArt.fill(graphics, x + 2, top + THERMO_H - column, x + 6, top + THERMO_H + 4, PixelArt.argb(mercury, 1));
        PixelArt.fill(graphics, x + 2, top + THERMO_H - column, x + 3, top + THERMO_H + 4, PixelArt.argb(0xFFFFFF, 0.35F));
        PixelArt.disc(graphics, x + 4, top + THERMO_H + 9, 10, PixelArt.argb(mercury, 1), PixelArt.argb(0x0A0810, 1));
        // Where the flame pulls the heat: a brass arrow on the right.
        int flameY = top + THERMO_H - Math.round(THERMO_H * Flask.flameHeat(flask.flame()) / Reactions.MAX_TEMPERATURE);
        for (int k = 0; k < 3; k++) {
            PixelArt.fill(graphics, x + 10 + k, flameY - 2 + k, x + 11 + k, flameY + 3 - k, PixelArt.argb(HexPlate.BRASS_LIGHT, 1));
        }
        // The plan's compounds whose zone the researcher has seen, beside their band.
        ExperimentPlan plan = bench.plan();
        if (plan != null) {
            int[] perZone = new int[Reactions.ZONES];
            for (Essence compound : plan.chain()) {
                if (knowledge.observed(compound)) {
                    int z = Reactions.zone(compound);
                    int cy = top + THERMO_H - Math.round((z + 0.5F) * span) - 4;
                    PixelArt.essence(graphics, compound, x - 11 - perZone[z] * 10, cy, PixelArt.ICON, 0xFF000000 | compound.color());
                    perZone[z]++;
                }
            }
        }
    }

    /** The pressure gauge: a needle sweeping to the red; it shivers near the top. */
    private void dial(GuiGraphicsExtractor graphics, float pressure) {
        int cx = leftPos + GAUGE_CX;
        int cy = topPos + DIAL_Y;
        PixelArt.disc(graphics, cx, cy, DIAL, PixelArt.argb(0xE8E0D0, 1), PixelArt.argb(HexPlate.BRASS, 1));
        PixelArt.discRing(graphics, cx, cy, DIAL - 2, PixelArt.argb(HexPlate.BRASS_DARK, 1));
        float share = Mth.clamp(pressure / Flask.BURST, 0, 1);
        for (int i = 0; i <= 10; i++) {
            float a = (-135 + 270 * i / 10.0F) * Mth.DEG_TO_RAD;
            int tx = cx + Math.round(Mth.sin(a) * 8);
            int ty = cy - Math.round(Mth.cos(a) * 8);
            PixelArt.fill(graphics, tx, ty, tx + 1, ty + 1, PixelArt.argb(i >= 8 ? 0xD02A1C : 0x3A3040, 1));
        }
        float shake = share > 0.75F ? Mth.sin(Util.getMillis() / 25.0F) * 4 * (share - 0.75F) : 0;
        float angle = (-135 + 270 * share + shake * 10) * Mth.DEG_TO_RAD;
        PixelArt.line(graphics, cx, cy, cx + Math.round(Mth.sin(angle) * 9), cy - Math.round(Mth.cos(angle) * 9),
                PixelArt.argb(share > 0.75F ? 0xD02A1C : 0x1A1420, 1));
        PixelArt.fill(graphics, cx - 1, cy - 1, cx + 1, cy + 1, PixelArt.argb(HexPlate.BRASS_DARK, 1));
    }

    /** The flame knob: a brass dial with five notches; the pointer shows the setting. */
    private void knob(GuiGraphicsExtractor graphics, int flame, boolean hovered) {
        int cx = leftPos + GAUGE_CX;
        int cy = topPos + KNOB_Y;
        for (int i = 0; i < Flask.FLAMES; i++) {
            float a = (-120 + 240 * i / (float) (Flask.FLAMES - 1)) * Mth.DEG_TO_RAD;
            int tx = cx + Math.round(Mth.sin(a) * 12);
            int ty = cy - Math.round(Mth.cos(a) * 12);
            PixelArt.fill(graphics, tx - 1, ty - 1, tx + 1, ty + 1, PixelArt.argb(i <= flame && flame > 0 ? 0x7CFF6A : 0x4A4060, 1));
        }
        PixelArt.disc(graphics, cx, cy, KNOB - 4, PixelArt.argb(hovered ? 0x4A3A2A : 0x2E2440, 1),
                PixelArt.argb(hovered ? HexPlate.BRASS_LIGHT : HexPlate.BRASS, 1));
        float a = (-120 + 240 * flame / (float) (Flask.FLAMES - 1)) * Mth.DEG_TO_RAD;
        PixelArt.line(graphics, cx, cy, cx + Math.round(Mth.sin(a) * 5), cy - Math.round(Mth.cos(a) * 5), PixelArt.argb(HexPlate.BRASS_LIGHT, 1));
    }

    /** A small brass button with a drawn sign: the vent's valve wheel or the sink's tipped flask. */
    private void iconButton(GuiGraphicsExtractor graphics, int bx, int by, boolean hovered, boolean active, boolean pour) {
        int x = leftPos + bx;
        int y = topPos + by;
        int rim = !active ? 0x5A4A3A : hovered ? HexPlate.BRASS_LIGHT : HexPlate.BRASS;
        PixelArt.fill(graphics, x + 1, y, x + BUTTON - 1, y + BUTTON, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x, y + 1, x + BUTTON, y + BUTTON - 1, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x + 1, y + 1, x + BUTTON - 1, y + BUTTON - 1, PixelArt.argb(hovered && active ? 0x2E2440 : 0x221B30, 1));
        int sign = PixelArt.argb(active ? 0xE8E0F0 : 0x7A7090, 1);
        int cx = x + BUTTON / 2;
        int cy = y + BUTTON / 2;
        if (pour) {
            // A flask tipped over a drop.
            PixelArt.line(graphics, cx - 4, cy - 3, cx + 1, cy - 3, sign);
            PixelArt.line(graphics, cx - 4, cy - 3, cx - 4, cy + 1, sign);
            PixelArt.line(graphics, cx - 4, cy + 1, cx + 1, cy + 1, sign);
            PixelArt.line(graphics, cx + 1, cy - 2, cx + 3, cy - 2, sign);
            PixelArt.fill(graphics, cx + 3, cy, cx + 4, cy + 2, PixelArt.argb(0x7CFF6A, active ? 1 : 0.4F));
            PixelArt.fill(graphics, cx + 3, cy + 3, cx + 4, cy + 4, PixelArt.argb(0x7CFF6A, active ? 0.7F : 0.3F));
        } else {
            // A valve wheel with puffs over it.
            PixelArt.discRing(graphics, cx, cy + 1, 8, sign);
            PixelArt.line(graphics, cx - 3, cy + 1, cx + 3, cy + 1, sign);
            PixelArt.line(graphics, cx, cy - 2, cx, cy + 4, sign);
            PixelArt.fill(graphics, cx - 2, cy - 5, cx - 1, cy - 4, PixelArt.argb(0xFFFFFF, 0.6F));
            PixelArt.fill(graphics, cx + 1, cy - 6, cx + 2, cy - 5, PixelArt.argb(0xFFFFFF, 0.6F));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        HexPlate.line(graphics, font, title, titleLabelX, titleLabelY, imageWidth - 16, HexPlate.TITLE, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HexPlate.TEXT, false);
        LabBenchBlockEntity bench = bench();
        if (bench == null) {
            return;
        }
        Flask flask = bench.flask();
        ExperimentPlan plan = bench.plan();
        int tx = JX + 3;
        if (plan == null) {
            HexPlate.centeredParagraph(graphics, font, Component.translatable(KEY + "pick"), JX + JW / 2, PLAN_Y + 4, JW - 8, 6, HexPlate.MUTED);
        } else {
            Essence target = plan.target();
            HexPlate.line(graphics, font, Component.translatable(target.translationKey()), tx + 12, PLAN_Y + 1, JW - 18, 0xFF000000 | target.color(), false);
            graphics.text(font, Component.literal(parts(flask.amount(target)) + "/" + plan.amount()), tx, PLAN_Y + 11, HexPlate.TEXT, false);
            String purity = Math.round(flask.purity(target) * 100) + "%/" + Math.round(plan.purity() * 100) + "%";
            graphics.text(font, Component.literal(purity), tx + JW - 6 - font.width(purity), PLAN_Y + 11, HexPlate.MUTED, false);
        }
        // What the flask holds: the parts beside each sign.
        List<Essence> in = flask.contents();
        for (int i = 0; i < in.size() && i < 7; i++) {
            graphics.text(font, Component.literal(parts(flask.amount(in.get(i)))), CONTENTS_X + 11, CONTENTS_Y + 1 + i * 10, HexPlate.TEXT, false);
        }
        int zone = Reactions.zoneAt(flask.temperature());
        HexPlate.centered(graphics, font, Component.translatable(KEY + "zone." + zone), GAUGE_CX, BUTTONS_Y + 20, 30, 0xFF000000 | ZONE_COLORS[zone]);
        HexPlate.centered(graphics, font, Component.literal(Math.round(flask.temperature()) + "°"), GAUGE_CX, BUTTONS_Y + 31, 30, HexPlate.MUTED);
        // The status line: a message, or what the bench waits for.
        Component status;
        int color = HexPlate.MUTED;
        if (toast != null && Util.getMillis() - toastAt < 3500) {
            status = toast;
            color = toastColor;
        } else {
            status = waiting(bench, plan);
        }
        if (status != null) {
            HexPlate.centered(graphics, font, status, FX + FW / 2, STATUS_Y, FW - 8, color);
        }
        if (minecraft != null && minecraft.level != null && minecraft.level.getGameTime() - bench.outcomeAt() < 50
                && bench.outcome() != LabBenchBlockEntity.OUTCOME_NONE) {
            int outcome = bench.outcome();
            Component text = Component.translatable(KEY + (outcome == LabBenchBlockEntity.OUTCOME_SEALED ? "sealed"
                    : outcome == LabBenchBlockEntity.OUTCOME_CAUGHT ? "caught" : "burst"));
            int c = outcome == LabBenchBlockEntity.OUTCOME_SEALED ? 0xFF6CFF8A : outcome == LabBenchBlockEntity.OUTCOME_CAUGHT ? 0xFFC08AFF : 0xFFE0503C;
            HexPlate.centered(graphics, font, text, CX, LIP_Y - 12, 100, c);
        }
    }

    /** What the bench waits for, or null when it waits for nothing. */
    private @Nullable Component waiting(LabBenchBlockEntity bench, @Nullable ExperimentPlan plan) {
        if (!bench.hasFlask()) {
            return Component.translatable(KEY + "need.flask");
        }
        if (rack.stream().noneMatch(e -> available[e.ordinal()] >= LabBenchBlockEntity.DROP_MB)) {
            return Component.translatable(KEY + "need.jars");
        }
        if (plan == null) {
            return Component.translatable(KEY + "need.topic");
        }
        if (bench.ready()) {
            return plan.sampleFits(bench.getItem(LabBenchBlockEntity.SAMPLE)) ? Component.translatable(KEY + "ready")
                    : Component.translatable(KEY + "need.sample", sampleName(plan));
        }
        if (bench.flask().pressure() > Flask.BURST * 0.75F) {
            return Component.translatable(KEY + "warn.pressure").withStyle(ChatFormatting.RED);
        }
        return null;
    }

    private static Component sampleName(ExperimentPlan plan) {
        return plan.sample().map(ingredient -> ingredient.items().findFirst()
                .map(item -> (Component) Component.translatable(item.value().getDescriptionId()))
                .orElse(Component.literal("?"))).orElse(Component.empty());
    }

    private static String parts(float amount) {
        return amount >= 9.95F ? Integer.toString(Math.round(amount)) : String.format(Locale.ROOT, "%.1f", amount);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        LabBenchBlockEntity bench = bench();
        if (bench == null) {
            return;
        }
        List<Component> tip = tooltip(bench, mouseX, mouseY);
        if (!tip.isEmpty()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (int i = 0; i < tip.size(); i++) {
                lines.addAll(font.split(tip.get(i), 200));
            }
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private List<Component> tooltip(LabBenchBlockEntity bench, int mouseX, int mouseY) {
        RuneKnowledge knowledge = ClientKnowledge.get();
        int topicIndex = topicAt(mouseX, mouseY);
        if (topicIndex >= 0) {
            ResearchTopic topic = ResearchTopics.lab().get(topicIndex);
            if (!Guide.client().canResearch(topic, knowledge)) {
                return List.of(Component.translatable(KEY + "topic.closed").withStyle(ChatFormatting.GRAY));
            }
            ExperimentPlan plan = ExperimentPlan.of(topic, true);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(topic.translationKey()).withColor(topic.color()));
            lines.add(Component.translatable(KEY + "topic.make", plan.amount(),
                    Component.translatable(plan.target().translationKey()).withColor(plan.target().color())).withStyle(ChatFormatting.GRAY));
            if (knowledge.researched(topic)) {
                lines.add(Component.translatable(KEY + "topic.done").withStyle(ChatFormatting.GREEN));
            }
            return lines;
        }
        Essence pipette = pipetteAt(mouseX, mouseY);
        if (pipette != null) {
            int mb = available[pipette.ordinal()];
            return List.of(Component.translatable(pipette.translationKey()).withColor(pipette.color()),
                    mb >= LabBenchBlockEntity.DROP_MB
                            ? Component.translatable(KEY + "dropper", mb).withStyle(ChatFormatting.GRAY)
                            : Component.translatable(KEY + "dropper.empty").withStyle(ChatFormatting.RED));
        }
        ExperimentPlan plan = bench.plan();
        if (plan != null) {
            List<Essence> chain = plan.chain();
            for (int i = 0; i < chain.size() && i < 3; i++) {
                if (over(mouseX, mouseY, JX + 3, CHAIN_Y + i * CHAIN_STEP - 1, JW - 6, CHAIN_STEP)) {
                    Essence compound = chain.get(chain.size() - 1 - i);
                    Essence[] parents = compound.parents();
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable(KEY + "reaction", name(parents[0]), name(parents[1]), name(compound)));
                    lines.add(knowledge.observed(compound)
                            ? Component.translatable(KEY + "reaction.zone", Component.translatable(KEY + "zone." + Reactions.zone(compound))
                            .withColor(ZONE_COLORS[Reactions.zone(compound)]))
                            : Component.translatable(KEY + "reaction.unknown").withStyle(ChatFormatting.GRAY));
                    return lines;
                }
            }
            if (over(mouseX, mouseY, JX + 3, PLAN_Y - 1, JW - 6, 36)) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable(KEY + "goal", plan.amount(), name(plan.target()), Math.round(plan.purity() * 100)));
                if (plan.needsSample()) {
                    lines.add(Component.translatable(KEY + "goal.sample", sampleName(plan)).withStyle(ChatFormatting.GRAY));
                }
                return lines;
            }
        }
        if (over(mouseX, mouseY, SEAL_X, SEAL_Y, SEAL_W, 12)) {
            return List.of(Component.translatable(KEY + "seal.hint").withStyle(ChatFormatting.GRAY));
        }
        if (overThermometer(mouseX, mouseY)) {
            int zone = Reactions.zoneAt(bench.flask().temperature());
            return List.of(Component.translatable(KEY + "thermometer", Math.round(bench.flask().temperature()),
                            Component.translatable(KEY + "zone." + zone).withColor(ZONE_COLORS[zone])),
                    Component.translatable(KEY + "thermometer.hint").withStyle(ChatFormatting.GRAY));
        }
        if (over(mouseX, mouseY, GAUGE_CX - DIAL / 2, DIAL_Y - DIAL / 2, DIAL, DIAL)) {
            return List.of(Component.translatable(KEY + "pressure", Math.round(bench.flask().pressure())),
                    Component.translatable(KEY + "pressure.hint").withStyle(ChatFormatting.GRAY));
        }
        if (overKnob(mouseX, mouseY)) {
            return List.of(Component.translatable(KEY + "flame", bench.flask().flame()),
                    Component.translatable(KEY + "flame.hint").withStyle(ChatFormatting.GRAY));
        }
        if (over(mouseX, mouseY, VENT_X, BUTTONS_Y, BUTTON, BUTTON)) {
            return List.of(Component.translatable(KEY + "vent"), Component.translatable(KEY + "vent.hint").withStyle(ChatFormatting.GRAY));
        }
        if (over(mouseX, mouseY, POUR_X, BUTTONS_Y, BUTTON, BUTTON)) {
            return List.of(Component.translatable(KEY + "pour"), Component.translatable(KEY + "pour.hint").withStyle(ChatFormatting.GRAY));
        }
        List<Essence> in = bench.flask().contents();
        for (int i = 0; i < in.size() && i < 7; i++) {
            if (over(mouseX, mouseY, CONTENTS_X, CONTENTS_Y + i * 10, 30, 10)) {
                return List.of(name(in.get(i)));
            }
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < LabBenchBlockEntity.SIZE) {
            String hint = switch (hoveredSlot.index) {
                case LabBenchBlockEntity.FLASK -> "flask";
                case LabBenchBlockEntity.SAMPLE -> "sample";
                default -> "output";
            };
            return List.of(Component.translatable(KEY + "slot." + hint).withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    private static Component name(Essence essence) {
        return Component.translatable(essence.translationKey()).withColor(essence.color());
    }
}
