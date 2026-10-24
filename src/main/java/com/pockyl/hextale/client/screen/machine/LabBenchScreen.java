package com.pockyl.hextale.client.screen.machine;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.pockyl.hextale.Hextale;
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
 * The Lab Bench's screen. On the left the journal: the chemtech topics as a grid of icons, the open topic's compound with
 * the reactions that build it (each with the zone it forms in once the researcher has seen it form) and the sample it is
 * sealed with. In the middle the bench: a rack of droppers, one per essence the jars around hold (click or hold to let
 * drops fall), the flask over its burner with the liquid in its mixed colour, bubbles and the flash of each reaction, what
 * the flask holds, and under it the gas valve: a slider with five notches in the colours of the zones they heat to. On
 * the right the pressure gauge, the vent and the sink, and the thermometer with the compounds known in each zone. The
 * flask and the sample sit left of the inventory, the notes and the seal right of it. Buttons, the valve handle, the gauge
 * and the empty-slot silhouettes are sprites drawn at twice the GUI resolution ({@code gui/sprites/lab_bench}).
 */
public class LabBenchScreen extends AbstractContainerScreen<LabBenchMenu> {
    private static final String KEY = "gui.hextale.lab_bench.";
    private static final int PANEL_BOTTOM = 172;
    // The journal.
    private static final int JX = 8;
    private static final int JY = 18;
    private static final int JW = 78;
    private static final int CELLS_X = JX + 3;
    private static final int CELLS_Y = JY + 3;
    private static final int CELL = 18;
    private static final int COLUMNS = 4;
    private static final int PLAN_Y = JY + 64;
    private static final int CHAIN_Y = PLAN_Y + 34;
    private static final int CHAIN_STEP = 11;
    private static final int SAMPLE_LINE_Y = PANEL_BOTTOM - 21;
    // The bench.
    private static final int FX = 90;
    private static final int FY = 18;
    private static final int FW = 132;
    private static final int RACK_X = FX + 6;
    private static final int RACK_Y = FY + 3;
    private static final int PIPETTE = 20;
    private static final int PIPETTES = 5;
    private static final int TIP_Y = RACK_Y + 33;
    private static final int CX = 172;
    private static final int LIP_Y = FY + 66;
    private static final int NECK_BOTTOM = FY + 78;
    private static final int BELLY_Y = FY + 97;
    private static final int RADIUS = 19;
    private static final int NECK = 4;
    private static final int FLASK_BOTTOM = BELLY_Y + RADIUS;
    private static final int BURNER_Y = FLASK_BOTTOM + 6;
    private static final int CONTENTS_X = FX + 4;
    private static final int CONTENTS_Y = FY + 62;
    private static final int STATUS_Y = FY + 47;
    // The gas valve under the flask: five tiles, one per flame setting.
    private static final int VALVE_Y = FY + 135;
    private static final int TILE_W = 22;
    private static final int TILE_H = 16;
    private static final int TILE_GAP = 2;
    private static final int TILES_X = FX + (FW - (TILE_W * 5 + TILE_GAP * 4)) / 2;
    // The gauges.
    private static final int RX = 226;
    private static final int RW = 66;
    private static final int GAUGE_CX = RX + RW / 2;
    private static final int DIAL_Y = FY + 21;
    private static final int DIAL = 32;
    private static final int BUTTONS_Y = FY + 42;
    private static final int BUTTON_W = 29;
    private static final int BUTTON_H = 18;
    private static final int VENT_X = RX + 3;
    private static final int POUR_X = RX + RW - 3 - BUTTON_W;
    private static final int THERMO_X = RX + 7;
    private static final int THERMO_TOP = FY + 66;
    private static final int THERMO_H = 70;
    private static final int KNOWN_X = THERMO_X + 15;
    // Under the panels, beside the inventory.
    private static final int SEAL_X = LabBenchMenu.OUTPUT_X - 20;
    private static final int SEAL_Y = LabBenchMenu.INVENTORY_Y + 36;
    private static final int SEAL_W = 56;
    private static final int SEAL_H = 18;
    /** Ticks between drops while a dropper is held. */
    private static final int DRIP_TICKS = 5;
    /** Milliseconds a drop takes from the tip to the liquid. */
    private static final long FALL_MS = 420;
    private static final long PRESS_MS = 160;
    private static final int[] ZONE_COLORS = {0x5AA8FF, 0x5CE0D8, 0xB8E05A, 0xFF9A3C, 0xFF4A3A};
    private static final ResourceLocation BUTTON = sprite("button");
    private static final ResourceLocation BUTTON_HOVER = sprite("button_hover");
    private static final ResourceLocation BUTTON_PRESSED = sprite("button_pressed");
    private static final ResourceLocation BUTTON_DISABLED = sprite("button_disabled");
    private static final ResourceLocation BUTTON_READY = sprite("button_ready");
    private static final ResourceLocation ICON_VENT = sprite("icon_vent");
    private static final ResourceLocation ICON_POUR = sprite("icon_pour");
    private static final ResourceLocation ICON_SEAL = sprite("icon_seal");
    private static final ResourceLocation FLAME = sprite("flame");
    private static final ResourceLocation DIAL_FACE = sprite("dial");
    private static final ResourceLocation NEEDLE = sprite("needle");

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
    private boolean draggingValve;
    private long lastFrame = -1;
    private long seenOutcome = Long.MIN_VALUE;
    private @Nullable Component toast;
    private int toastColor = HexPlate.TEXT;
    private long toastAt;
    /** Which button was pressed last and when (milliseconds), so it shows pressed for a moment. */
    private int pressedButton = -1;
    private long pressedAt;

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

    private static ResourceLocation sprite(String name) {
        return Hextale.id("lab_bench/" + name);
    }

    private @Nullable LabBenchBlockEntity bench() {
        return minecraft == null || minecraft.level == null ? null
                : minecraft.level.getBlockEntity(menu.pos()) instanceof LabBenchBlockEntity entity ? entity : null;
    }

    private void send(LabActionPayload.Action action, int value) {
        ClientPacketDistributor.sendToServer(new LabActionPayload(menu.pos(), action, value));
    }

    private void sound(SoundEvent sound, float pitch, float volume) {
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
                shatter();
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

    /** The flask burst: glass shards fly off. */
    private void shatter() {
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
            if (over(mouseX, mouseY, RACK_X + i * PIPETTE, RACK_Y - 2, PIPETTE, TIP_Y - RACK_Y + 14)) {
                return rack.get(i + rackScroll);
            }
        }
        return null;
    }

    private boolean overRack(double mouseX, double mouseY) {
        return over(mouseX, mouseY, RACK_X, RACK_Y - 2, PIPETTES * PIPETTE, TIP_Y - RACK_Y + 14);
    }

    private boolean overValve(double mouseX, double mouseY) {
        return over(mouseX, mouseY, TILES_X - 1, VALVE_Y - 1, TILE_W * 5 + TILE_GAP * 4 + 2, TILE_H + 2);
    }

    private boolean overThermometer(double mouseX, double mouseY) {
        return over(mouseX, mouseY, THERMO_X - 4, THERMO_TOP - 2, RW - 8, THERMO_H + 16);
    }

    /** The left x of a flame setting's tile. */
    private static int tileX(int flame) {
        return TILES_X + flame * (TILE_W + TILE_GAP);
    }

    /** The flame setting whose tile is under (or nearest to) a mouse x. */
    private int flameAt(double mouseX) {
        int t = (int) Math.floor((mouseX - leftPos - TILES_X + TILE_GAP / 2.0) / (TILE_W + TILE_GAP));
        return Math.clamp(t, 0, Flask.FLAMES - 1);
    }

    private boolean ventActive(LabBenchBlockEntity bench) {
        return bench.flask().pressure() > 0.5F || !bench.flask().isEmpty() && bench.flask().temperature() > Flask.AMBIENT + 5;
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
        if (overValve(mx, my)) {
            draggingValve = true;
            setFlame(bench, flameAt(mx));
            return true;
        }
        if (over(mx, my, VENT_X, BUTTONS_Y, BUTTON_W, BUTTON_H)) {
            if (ventActive(bench)) {
                press(0);
                send(LabActionPayload.Action.VENT, 0);
            }
            return true;
        }
        if (over(mx, my, POUR_X, BUTTONS_Y, BUTTON_W, BUTTON_H)) {
            if (!bench.flask().isEmpty()) {
                press(1);
                send(LabActionPayload.Action.POUR, 0);
            }
            return true;
        }
        if (over(mx, my, SEAL_X, SEAL_Y, SEAL_W, SEAL_H)) {
            if (sealable(bench)) {
                press(2);
                send(LabActionPayload.Action.SEAL, 0);
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void press(int button) {
        pressedButton = button;
        pressedAt = Util.getMillis();
        sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.4F);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        LabBenchBlockEntity bench = bench();
        if (draggingValve && bench != null) {
            setFlame(bench, flameAt(event.x()));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && (held != null || draggingValve)) {
            held = null;
            draggingValve = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        LabBenchBlockEntity bench = bench();
        if (overRack(mouseX, mouseY) && rack.size() > PIPETTES) {
            rackScroll = Math.clamp(rackScroll - (int) Math.signum(scrollY), 0, rack.size() - PIPETTES);
            return true;
        }
        if (bench != null && (overValve(mouseX, mouseY) || overThermometer(mouseX, mouseY)
                || over(mouseX, mouseY, FX, FY + 44, FW, 90))) {
            setFlame(bench, bench.flask().flame() + (scrollY > 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void setFlame(LabBenchBlockEntity bench, int flame) {
        flame = Math.clamp(flame, 0, Flask.FLAMES - 1);
        if (flame != bench.flask().flame()) {
            bench.flask().setFlame(flame);
            send(LabActionPayload.Action.FLAME, flame);
            sound(SoundEvents.LEVER_CLICK, 1.2F + flame * 0.12F, 0.3F);
            if (flame > 0) {
                sound(SoundEvents.FIRECHARGE_USE, 1.6F + flame * 0.1F, 0.08F);
            }
        }
    }

    @Override
    public void removed() {
        held = null;
        draggingValve = false;
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
        HexPlate.well(graphics, x + JX, y + JY, JW, PANEL_BOTTOM - JY);
        HexPlate.well(graphics, x + FX, y + FY, FW, PANEL_BOTTOM - FY);
        HexPlate.well(graphics, x + RX, y + FY, RW, PANEL_BOTTOM - FY);
        for (Slot slot : menu.slots) {
            if (slot.index == LabBenchBlockEntity.OUTPUT && !(slot.container instanceof Inventory)) {
                HexPlate.outputSlot(graphics, x + slot.x, y + slot.y);
            } else {
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
        fallingDrops(graphics);
        flyingShards(graphics, dt);
        contents(graphics, bench);
        valve(graphics, bench, mouseX, mouseY);
        dial(graphics, bench.flask().pressure());
        thermometer(graphics, bench, knowledge);
        PixelArt.flush(graphics);
        button(graphics, 0, VENT_X, BUTTONS_Y, BUTTON_W, BUTTON_H, ventActive(bench), false, ICON_VENT, 14, mouseX, mouseY);
        button(graphics, 1, POUR_X, BUTTONS_Y, BUTTON_W, BUTTON_H, !bench.flask().isEmpty(), false, ICON_POUR, 14, mouseX, mouseY);
        boolean ready = sealable(bench);
        button(graphics, 2, SEAL_X, SEAL_Y, SEAL_W, SEAL_H, ready, ready, ICON_SEAL, 12, mouseX, mouseY);
    }

    /**
     * A sprite button: frame by state (pressed for a moment after a click, glowing gold when {@code ready}), the icon
     * centred (left of centre when there is a label), sunk a pixel while pressed.
     */
    private void button(GuiGraphicsExtractor graphics, int id, int bx, int by, int w, int h, boolean active, boolean ready, ResourceLocation icon,
                        int iconSize, int mouseX, int mouseY) {
        boolean hovered = active && over(mouseX, mouseY, bx, by, w, h);
        boolean pressed = pressedButton == id && Util.getMillis() - pressedAt < PRESS_MS;
        ResourceLocation frame = !active ? BUTTON_DISABLED : pressed ? BUTTON_PRESSED : ready && !hovered ? BUTTON_READY : hovered ? BUTTON_HOVER : BUTTON;
        int x = leftPos + bx;
        int y = topPos + by;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frame, x, y, w, h);
        if (ready && !pressed) {
            // A slow gold breath round a button that can be pressed now.
            float pulse = 0.5F + 0.5F * Mth.sin(Util.getMillis() / 260.0F);
            int glow = PixelArt.argb(0xFFE08A, 0.15F + 0.35F * pulse);
            PixelArt.fill(graphics, x - 1, y + 1, x, y + h - 1, glow);
            PixelArt.fill(graphics, x + w, y + 1, x + w + 1, y + h - 1, glow);
            PixelArt.fill(graphics, x + 1, y - 1, x + w - 1, y, glow);
            PixelArt.fill(graphics, x + 1, y + h, x + w - 1, y + h + 1, glow);
            PixelArt.flush(graphics);
        }
        int sink = pressed ? 1 : 0;
        int ix = id == 2 ? x + 4 : x + (w - iconSize) / 2;
        int iy = y + (h - iconSize) / 2 + sink;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, ix + sink, iy, iconSize, iconSize, active ? -1 : 0x80FFFFFF);
    }

    /** The topic grid, the open topic's compound, its reactions and the sample it is sealed with. */
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
        float amount = Math.min(1, flask.amount(target) / plan.amount());
        float purity = flask.purity(target);
        bar(graphics, px, topPos + PLAN_Y + 21, JW - 6, amount, amount >= 0.99F ? 0x6CFF8A : target.color());
        bar(graphics, px, topPos + PLAN_Y + 26, JW - 6, Math.min(1, purity / plan.purity()), purity >= plan.purity() - 0.005F ? 0x6CFF8A : 0xE8D25A);
        List<Essence> chain = plan.chain();
        for (int i = 0; i < chain.size() && i < 3; i++) {
            Essence compound = chain.get(chain.size() - 1 - i);
            int ly = topPos + CHAIN_Y + i * CHAIN_STEP;
            Essence[] parents = compound.parents();
            int arrow = PixelArt.argb(HexPlate.BRASS_LIGHT, 0.8F);
            PixelArt.essence(graphics, parents[0], px, ly, PixelArt.ICON, 0xFF000000 | parents[0].color());
            PixelArt.fill(graphics, px + 11, ly + 4, px + 14, ly + 5, arrow);
            PixelArt.fill(graphics, px + 12, ly + 3, px + 13, ly + 6, arrow);
            PixelArt.essence(graphics, parents[1], px + 16, ly, PixelArt.ICON, 0xFF000000 | parents[1].color());
            PixelArt.line(graphics, px + 28, ly + 4, px + 33, ly + 4, arrow);
            PixelArt.line(graphics, px + 32, ly + 2, px + 34, ly + 4, arrow);
            PixelArt.line(graphics, px + 32, ly + 6, px + 34, ly + 4, arrow);
            PixelArt.essence(graphics, compound, px + 37, ly, PixelArt.ICON, 0xFF000000 | compound.color());
            zoneChip(graphics, px + 50, ly + 1, knowledge.observed(compound) ? Reactions.zone(compound) : -1);
        }
        // The sample: its icon in a small well; ticked once it lies on the bench.
        HexPlate.divider(graphics, leftPos + JX - HexPlate.BORDER + 2, topPos + SAMPLE_LINE_Y - 4, JW + 4);
        ItemStack sample = sampleIcon(plan);
        if (!sample.isEmpty()) {
            int sx = leftPos + JX + JW - 21;
            int sy = topPos + SAMPLE_LINE_Y;
            HexPlate.slot(graphics, sx, sy);
            PixelArt.flush(graphics);
            graphics.item(sample, sx, sy);
            if (plan.sampleFits(bench.getItem(LabBenchBlockEntity.SAMPLE))) {
                PixelArt.fill(graphics, sx + 11, sy + 12, sx + 13, sy + 14, PixelArt.argb(0x6CFF8A, 1));
                PixelArt.fill(graphics, sx + 13, sy + 10, sx + 15, sy + 12, PixelArt.argb(0x6CFF8A, 1));
            }
        }
    }

    private static ItemStack sampleIcon(ExperimentPlan plan) {
        return plan.sample().flatMap(ingredient -> ingredient.items().findFirst()).map(item -> new ItemStack(item.value())).orElse(ItemStack.EMPTY);
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
            int px = x0 + i * PIPETTE + (PIPETTE - 9) / 2;
            if (i + rackScroll >= rack.size()) {
                PixelArt.fill(graphics, px + 2, y0 + 11, px + 7, y0 + 12, PixelArt.argb(0x1A1008, 1));
                continue;
            }
            Essence essence = rack.get(i + rackScroll);
            boolean usable = canDrop(bench, essence);
            boolean lift = essence == hovered && usable;
            if (lift) {
                // The hovered dropper glows from behind.
                PixelArt.fill(graphics, px - 2, y0 - 3, px + 11, TIP_Y + topPos + 13, PixelArt.argb(essence.color(), 0.12F));
            }
            int dy = essence == held ? -1 : lift ? -2 : 0;
            int bulb = essence == held ? 0x6A1818 : 0x9A3030;
            PixelArt.fill(graphics, px + 1, y0 + dy, px + 8, y0 + 7 + dy, PixelArt.argb(bulb, 1));
            PixelArt.fill(graphics, px, y0 + 1 + dy, px + 9, y0 + 6 + dy, PixelArt.argb(bulb, 1));
            PixelArt.fill(graphics, px + 2, y0 + 1 + dy, px + 4, y0 + 3 + dy, PixelArt.argb(0xD86060, 0.8F));
            PixelArt.fill(graphics, px + 1, y0 + 7 + dy, px + 8, y0 + 8 + dy, PixelArt.argb(HexPlate.BRASS, 1));
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
        int foot = topPos + BURNER_Y + 10;
        PixelArt.fill(graphics, rod, topPos + LIP_Y + 2, rod + 2, foot, iron);
        PixelArt.fill(graphics, rod, topPos + LIP_Y + 2, rod + 1, foot, ironLight);
        PixelArt.fill(graphics, rod - 14, foot, rod + 8, foot + 2, iron);
        int clampY = topPos + NECK_BOTTOM - 8;
        PixelArt.fill(graphics, leftPos + CX + NECK + 1, clampY, rod, clampY + 2, iron);
        PixelArt.fill(graphics, leftPos + CX + NECK, clampY - 1, leftPos + CX + NECK + 2, clampY + 3, PixelArt.argb(HexPlate.BRASS, 1));
        int ringY = topPos + FLASK_BOTTOM - 4;
        PixelArt.fill(graphics, leftPos + CX - 12, ringY, rod, ringY + 1, iron);
        if (!bench.hasFlask()) {
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
                    float wobble = temperature / 100.0F;
                    for (int px = left; px < right; px++) {
                        float wave = Mth.sin(px * 0.7F + nowMs / (160.0F - wobble * 90)) * wobble;
                        PixelArt.fill(graphics, px, sy + (wave > 0.4F ? 1 : 0), px + 1, sy + 1, PixelArt.argb(PixelArt.mix(liquid, 0xFFFFFF, 0.45F), 0.95F));
                    }
                } else {
                    PixelArt.fill(graphics, left, sy, right, sy + 1, PixelArt.argb(c, 0.92F));
                }
            }
            PixelArt.fill(graphics, left - 1, sy, left, sy + 1, glassLit);
            PixelArt.fill(graphics, right, sy, right + 1, sy + 1, glass);
        }
        PixelArt.fill(graphics, leftPos + CX - NECK - 2, topPos + LIP_Y - 2, leftPos + CX + NECK + 2, topPos + LIP_Y, glass);
        int bottom = topPos + FLASK_BOTTOM + 1;
        int bw = halfWidth(rows - 1);
        PixelArt.fill(graphics, leftPos + CX - bw, bottom - 1, leftPos + CX + bw, bottom, glass);
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
        int height = 1 + flame;
        for (int k = 0; k < height; k++) {
            float t = k / (float) height;
            int w = Math.max(1, Math.round((1 - t) * (2 + flame * 0.6F) + Mth.sin(now / 70.0F + k) * 0.6F));
            int color = PixelArt.mix(0x3AF0FF, 0x9CFF6A, t);
            PixelArt.fill(graphics, bx - w, by - 3 - k, bx + w, by - 2 - k, PixelArt.argb(color, 0.9F - t * 0.5F));
        }
    }

    private void fallingDrops(GuiGraphicsExtractor graphics) {
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
            if (sx < leftPos + FX || sx > leftPos + FX + FW || sy < topPos + FY || sy > topPos + PANEL_BOTTOM) {
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
            PixelArt.essence(graphics, essence, leftPos + CONTENTS_X, topPos + CONTENTS_Y + i * 10, PixelArt.ICON, 0xFF000000 | essence.color());
        }
    }

    /**
     * The gas valve: five tiles, one per flame setting, each lit in the colour of the zone that setting heats the flask to
     * and holding a flame that grows with it (the first is the burner off). Every tile up to the setting glows, the set one
     * is pressed in; click a tile, drag along them or turn the wheel.
     */
    private void valve(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench, int mouseX, int mouseY) {
        int flame = bench.flask().flame();
        int hoverFlame = overValve(mouseX, mouseY) || draggingValve ? flameAt(mouseX) : -1;
        long now = Util.getMillis();
        for (int k = 0; k < Flask.FLAMES; k++) {
            int x = leftPos + tileX(k);
            int y = topPos + VALVE_Y;
            int zone = Reactions.zoneAt(Flask.flameHeat(k));
            boolean set = k == flame;
            boolean lit = k <= flame;
            ResourceLocation frame = set ? BUTTON_PRESSED : k == hoverFlame ? BUTTON_HOVER : BUTTON;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frame, x, y, TILE_W, TILE_H);
            // The zone's colour along the tile's foot: bright up to the setting.
            PixelArt.fill(graphics, x + 3, y + TILE_H - 4, x + TILE_W - 3, y + TILE_H - 3, PixelArt.argb(ZONE_COLORS[zone], lit ? 1 : 0.3F));
            if (set) {
                float pulse = 0.5F + 0.5F * Mth.sin(now / 240.0F);
                int glow = PixelArt.argb(ZONE_COLORS[zone], 0.25F + 0.35F * pulse);
                PixelArt.fill(graphics, x + 1, y - 1, x + TILE_W - 1, y, glow);
                PixelArt.fill(graphics, x + 1, y + TILE_H, x + TILE_W - 1, y + TILE_H + 1, glow);
                PixelArt.fill(graphics, x - 1, y + 1, x, y + TILE_H - 1, glow);
                PixelArt.fill(graphics, x + TILE_W, y + 1, x + TILE_W + 1, y + TILE_H - 1, glow);
            }
            PixelArt.flush(graphics);
            int sink = set ? 1 : 0;
            if (k == 0) {
                // The burner off: an empty nozzle.
                int cx = x + TILE_W / 2;
                int nozzle = PixelArt.argb(lit ? 0x9A90B8 : 0x5A5070, 1);
                PixelArt.fill(graphics, cx - 3, y + 7 + sink, cx + 3, y + 9 + sink, nozzle);
                PixelArt.fill(graphics, cx - 1, y + 5 + sink, cx + 1, y + 7 + sink, nozzle);
                PixelArt.flush(graphics);
            } else {
                int size = 5 + k * 2;
                int flicker = set ? Math.round(Mth.sin(now / 90.0F + k) * 0.6F) : 0;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, x + (TILE_W - size) / 2 + sink, y + TILE_H - 4 - size + sink - flicker,
                        size, size, lit ? -1 : 0x70FFFFFF);
            }
        }
    }

    /** The pressure gauge: a needle sweeping to the red; it shivers near the top. */
    private void dial(GuiGraphicsExtractor graphics, float pressure) {
        int cx = leftPos + GAUGE_CX;
        int cy = topPos + DIAL_Y;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DIAL_FACE, cx - DIAL / 2, cy - DIAL / 2, DIAL, DIAL);
        float share = Mth.clamp(pressure / Flask.BURST, 0, 1);
        float shake = share > 0.75F ? Mth.sin(Util.getMillis() / 25.0F) * 40 * (share - 0.75F) : 0;
        float angle = (-135 + 270 * share + shake) * Mth.DEG_TO_RAD;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().rotate(angle);
        // The needle sprite is 3 x 15 GUI pixels with its pivot 13 down from its top.
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NEEDLE, -1, -13, 3, 15);
        graphics.pose().popMatrix();
    }

    /** The thermometer: five zone bands, the column of the liquid's heat, where the flame pulls, the compounds known per zone. */
    private void thermometer(GuiGraphicsExtractor graphics, LabBenchBlockEntity bench, RuneKnowledge knowledge) {
        int x = leftPos + THERMO_X;
        int top = topPos + THERMO_TOP;
        Flask flask = bench.flask();
        HexPlate.well(graphics, x - 1, top - 1, 12, THERMO_H + 2);
        int zone = Reactions.zoneAt(flask.temperature());
        float span = THERMO_H / (float) Reactions.ZONES;
        for (int z = 0; z < Reactions.ZONES; z++) {
            int zy1 = top + THERMO_H - Math.round(z * span);
            int zy0 = top + THERMO_H - Math.round((z + 1) * span);
            PixelArt.fill(graphics, x, zy0, x + 10, zy1, PixelArt.argb(ZONE_COLORS[z], z == zone ? 0.35F : 0.14F));
            if (z > 0) {
                PixelArt.fill(graphics, x, zy1, x + 10, zy1 + 1, PixelArt.argb(0x000000, 0.5F));
            }
            // A row guide to the known compounds of this zone.
            PixelArt.fill(graphics, x + 12, zy0 + Math.round(span / 2), x + 14, zy0 + Math.round(span / 2) + 1, PixelArt.argb(ZONE_COLORS[z], 0.6F));
        }
        int column = Math.round(THERMO_H * flask.temperature() / Reactions.MAX_TEMPERATURE);
        int mercury = ZONE_COLORS[zone];
        PixelArt.fill(graphics, x + 3, top + THERMO_H - column, x + 7, top + THERMO_H + 4, PixelArt.argb(mercury, 1));
        PixelArt.fill(graphics, x + 3, top + THERMO_H - column, x + 4, top + THERMO_H + 4, PixelArt.argb(0xFFFFFF, 0.35F));
        PixelArt.disc(graphics, x + 5, top + THERMO_H + 8, 12, PixelArt.argb(mercury, 1), PixelArt.argb(0x0A0810, 1));
        // Where the flame pulls the heat: a brass arrow on the left.
        int flameY = top + THERMO_H - Math.round(THERMO_H * Flask.flameHeat(flask.flame()) / Reactions.MAX_TEMPERATURE);
        for (int k = 0; k < 3; k++) {
            PixelArt.fill(graphics, x - 4 + k, flameY - 2 + k, x - 3 + k, flameY + 3 - k, PixelArt.argb(HexPlate.BRASS_LIGHT, 1));
        }
        ExperimentPlan plan = bench.plan();
        if (plan != null) {
            int[] perZone = new int[Reactions.ZONES];
            for (Essence compound : plan.chain()) {
                if (knowledge.observed(compound)) {
                    int z = Reactions.zone(compound);
                    if (perZone[z] < 4) {
                        int cy = top + THERMO_H - Math.round((z + 0.5F) * span) - 4;
                        PixelArt.essence(graphics, compound, leftPos + KNOWN_X + perZone[z] * 10, cy, PixelArt.ICON, 0xFF000000 | compound.color());
                    }
                    perZone[z]++;
                }
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        HexPlate.line(graphics, font, title, titleLabelX, titleLabelY, imageWidth - 16, HexPlate.TITLE, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HexPlate.TEXT, false);
        int slotCaption = LabBenchMenu.INVENTORY_Y + 1;
        HexPlate.centered(graphics, font, Component.translatable(KEY + "caption.flask"), LabBenchMenu.LEFT_SLOT_X + 8, slotCaption, 56, HexPlate.MUTED);
        HexPlate.centered(graphics, font, Component.translatable(KEY + "caption.sample"), LabBenchMenu.LEFT_SLOT_X + 8, slotCaption + 34, 56, HexPlate.MUTED);
        HexPlate.centered(graphics, font, Component.translatable(KEY + "caption.notes"), LabBenchMenu.OUTPUT_X + 8, slotCaption, 56, HexPlate.MUTED);
        LabBenchBlockEntity bench = bench();
        if (bench == null) {
            return;
        }
        boolean ready = sealable(bench);
        HexPlate.line(graphics, font, Component.translatable(KEY + "seal"), SEAL_X + 19, SEAL_Y + 5, SEAL_W - 22,
                ready ? 0xFFFFF4C0 : 0xFF7A7090, ready);
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
            Component sampleText = Component.translatable(plan.needsSample() ? KEY + "sample" : KEY + "sample.none");
            HexPlate.line(graphics, font, sampleText, tx, SAMPLE_LINE_Y + 4, plan.needsSample() ? JW - 28 : JW - 6, HexPlate.MUTED, false);
        }
        List<Essence> in = flask.contents();
        for (int i = 0; i < in.size() && i < 7; i++) {
            graphics.text(font, Component.literal(parts(flask.amount(in.get(i)))), CONTENTS_X + 11, CONTENTS_Y + 1 + i * 10, HexPlate.TEXT, false);
        }
        int zone = Reactions.zoneAt(flask.temperature());
        int textX = THERMO_X + 15;
        HexPlate.line(graphics, font, Component.translatable(KEY + "zone." + zone), textX, THERMO_TOP + THERMO_H, RX + RW - 3 - textX,
                0xFF000000 | ZONE_COLORS[zone], false);
        graphics.text(font, Component.literal(Math.round(flask.temperature()) + "°"), textX, THERMO_TOP + THERMO_H + 9, HexPlate.MUTED, false);
        Component status;
        int color = HexPlate.MUTED;
        if (toast != null && Util.getMillis() - toastAt < 3500) {
            status = toast;
            color = toastColor;
        } else {
            status = waiting(bench, plan);
        }
        if (minecraft != null && minecraft.level != null && minecraft.level.getGameTime() - bench.outcomeAt() < 50
                && bench.outcome() != LabBenchBlockEntity.OUTCOME_NONE) {
            int outcome = bench.outcome();
            status = Component.translatable(KEY + (outcome == LabBenchBlockEntity.OUTCOME_SEALED ? "sealed"
                    : outcome == LabBenchBlockEntity.OUTCOME_CAUGHT ? "caught" : "burst"));
            color = outcome == LabBenchBlockEntity.OUTCOME_SEALED ? 0xFF6CFF8A : outcome == LabBenchBlockEntity.OUTCOME_CAUGHT ? 0xFFC08AFF : 0xFFE0503C;
        }
        if (status != null) {
            // Up to two lines between the droppers' signs and the flask's lip.
            HexPlate.centeredParagraph(graphics, font, status, FX + FW / 2, STATUS_Y, FW - 8, 2, color);
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
        ItemStack icon = sampleIcon(plan);
        return icon.isEmpty() ? Component.empty() : icon.getHoverName();
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
            for (Component line : tip) {
                lines.addAll(font.split(line, 200));
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
            lines.add(Component.translatable(KEY + "topic.make", plan.amount(), name(plan.target())).withStyle(ChatFormatting.GRAY));
            if (knowledge.researched(topic)) {
                lines.add(Component.translatable(KEY + "topic.done").withStyle(ChatFormatting.GREEN));
            }
            return lines;
        }
        Essence pipette = pipetteAt(mouseX, mouseY);
        if (pipette != null) {
            int mb = available[pipette.ordinal()];
            return List.of(name(pipette), mb >= LabBenchBlockEntity.DROP_MB
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
                    return List.of(Component.translatable(KEY + "reaction", name(parents[0]), name(parents[1]), name(compound)),
                            knowledge.observed(compound)
                                    ? Component.translatable(KEY + "reaction.zone", Component.translatable(KEY + "zone." + Reactions.zone(compound))
                                    .withColor(ZONE_COLORS[Reactions.zone(compound)]))
                                    : Component.translatable(KEY + "reaction.unknown").withStyle(ChatFormatting.GRAY));
                }
            }
            if (over(mouseX, mouseY, JX + 3, PLAN_Y - 1, JW - 6, 32)) {
                return List.of(Component.translatable(KEY + "goal", plan.amount(), name(plan.target()), Math.round(plan.purity() * 100)));
            }
            if (over(mouseX, mouseY, JX + 3, SAMPLE_LINE_Y - 2, JW - 6, 20) && plan.needsSample()) {
                return List.of(Component.translatable(KEY + "goal.sample", sampleName(plan)),
                        Component.translatable(KEY + "slot.sample").withStyle(ChatFormatting.GRAY));
            }
        }
        if (over(mouseX, mouseY, SEAL_X, SEAL_Y, SEAL_W, SEAL_H)) {
            return List.of(Component.translatable(KEY + "seal.hint").withStyle(ChatFormatting.GRAY));
        }
        if (overValve(mouseX, mouseY)) {
            int flame = draggingValve ? bench.flask().flame() : flameAt(mouseX);
            int zone = Reactions.zoneAt(Flask.flameHeat(flame));
            return List.of(Component.translatable(KEY + "flame", flame, Component.translatable(KEY + "zone." + zone).withColor(ZONE_COLORS[zone])),
                    Component.translatable(KEY + "flame.hint").withStyle(ChatFormatting.GRAY));
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
        if (over(mouseX, mouseY, VENT_X, BUTTONS_Y, BUTTON_W, BUTTON_H)) {
            return List.of(Component.translatable(KEY + "vent"), Component.translatable(KEY + "vent.hint").withStyle(ChatFormatting.GRAY));
        }
        if (over(mouseX, mouseY, POUR_X, BUTTONS_Y, BUTTON_W, BUTTON_H)) {
            return List.of(Component.translatable(KEY + "pour"), Component.translatable(KEY + "pour.hint").withStyle(ChatFormatting.GRAY));
        }
        List<Essence> in = bench.flask().contents();
        for (int i = 0; i < in.size() && i < 7; i++) {
            if (over(mouseX, mouseY, CONTENTS_X, CONTENTS_Y + i * 10, 30, 10)) {
                return List.of(name(in.get(i)));
            }
        }
        if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < LabBenchBlockEntity.SIZE
                && !(hoveredSlot.container instanceof Inventory)) {
            if (hoveredSlot.index == LabBenchBlockEntity.SAMPLE && plan != null && plan.needsSample()) {
                return List.of(Component.translatable(KEY + "goal.sample", sampleName(plan)));
            }
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
