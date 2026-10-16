package com.pockyl.hextale.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.Requirement;
import com.pockyl.hextale.block.InkSupply;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.screen.book.StarBook;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.network.ResearchActionPayload;
import com.pockyl.hextale.network.ResearchMessagePayload;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ItemStudy;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.ResearchTool;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.research.RuneBoards;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Research Table, in two tabs: the star book (choose a rune) and the desk - the parchment on the left, the table's
 * ink vials, slots and the player's inventory on the right. The parchment holds one of two games: drawing a rune's ink
 * thread with the mouse, or striking an item's parchment to find its elements by their echoes.
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> implements StarBook.Host {
    private static final Identifier BOOK_TEXTURE = Hextale.id("textures/gui/research_book.png");
    private static final Identifier DESK_TEXTURE = Hextale.id("textures/gui/research_desk.png");
    private static final Identifier WIDGETS = Hextale.id("textures/gui/research_table_widgets.png");
    private static final int WIDGETS_SIZE_X = 256;
    private static final int WIDGETS_SIZE_Y = 128;
    private static final String KEY = "gui.hextale.research_table.";
    private static final int TAB_WIDTH = 24;
    private static final int TAB_HEIGHT = 20;
    private static final int TEXT = 0xF0E3C2;
    private static final int TEXT_MUTED = 0xC9B48A;
    private static final int PANEL_X = 252;
    private static final int BOARD_X = 4;
    private static final int BOARD_Y = 24;
    private static final int BOARD_W = 244;
    private static final int BOARD_H = 234;
    /** Ink vials: nine glass tubes in ring order. */
    private static final int VIAL_X = 258;
    private static final int VIAL_Y = 64;
    private static final int VIAL_STEP = 23;
    private static final int VIAL_W = 12;
    private static final int VIAL_H = 30;
    private static final long TOAST_MILLIS = 4500;
    private static final long FLASH_MILLIS = 700;
    /** Ghost icons of empty slots in the widget sheet (index * 16, row 24). */
    private static final int GHOST_DIARY = 0;
    private static final int GHOST_PAPER = 1;
    private static final int GHOST_ORB = 2;
    private static final int GHOST_TOOLS = 5;
    private static final int GHOST_STUDY = 11;

    private enum Tab {
        BOOK,
        DESK
    }

    private final BoardView board = new BoardView();
    private StarBook book;
    private Tab tab;
    @Nullable
    private List<Component> pendingTooltip;

    // The thread being drawn: the client's copy, sent to the server when the mouse is released.
    private int[] thread = {HexGrid.CENTRE};
    @Nullable
    private ComplexRune threadRune;
    private int serverRevision = Integer.MIN_VALUE;
    private boolean dragging;
    private ResearchSheet.@Nullable Trace trace;
    private String fogKey = "";

    // Feedback.
    private Component toast = Component.empty();
    private ResearchMessagePayload.Tone toastTone = ResearchMessagePayload.Tone.INFO;
    private long toastStart;
    private final long[] vialFlash = new long[SimpleRune.values().length];
    private final float[] vialLevel = new float[SimpleRune.values().length];
    @Nullable
    private InkSupply supply;
    private long supplyFrame;
    private long requirementFlash;
    private int lastStepSound;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, ResearchTableMenu.IMAGE_WIDTH, ResearchTableMenu.IMAGE_HEIGHT);
        inventoryLabelX = -1000;
        titleLabelX = 8;
        titleLabelY = 7;
    }

    @Override
    protected void init() {
        super.init();
        if (book == null) {
            book = new StarBook(this);
            ResearchTableBlockEntity table = table();
            boolean subject = table != null && (table.isStudyingItem() || table.sheet().rune() != null);
            setTab(subject || missingDiary() != null ? Tab.DESK : Tab.BOOK);
            Arrays.fill(vialLevel, -1);
        }
        book.layout(leftPos + 4, topPos + 24, imageWidth - 8, imageHeight - 28);
        board.layout(leftPos, topPos);
        setTab(tab);
    }

    private void setTab(Tab tab) {
        this.tab = tab;
        menu.setSlotsVisible(tab == Tab.DESK);
    }

    @Nullable
    private ResearchTableBlockEntity table() {
        return minecraft != null && minecraft.level != null
                && minecraft.level.getBlockEntity(menu.pos()) instanceof ResearchTableBlockEntity table ? table : null;
    }

    @Nullable
    private Requirement missingDiary() {
        return minecraft == null || minecraft.player == null ? null : Requirement.diary(menu.table(), minecraft.player);
    }

    /** What is missing to draw on: the paper only while the current attempt has not taken one yet. */
    @Nullable
    private Requirement missing() {
        ResearchTableBlockEntity table = table();
        boolean paid = table != null && table.sheet().paid() && !done(table);
        return minecraft == null || minecraft.player == null ? null : Requirement.firstMissing(menu.table(), minecraft.player, paid);
    }

    private int toolLevel(ResearchTool tool) {
        return ResearchTableBlockEntity.toolLevel(menu.table(), tool, ClientKnowledge.get());
    }

    private ResearchSheet.Allowance allowance() {
        return ResearchTool.allowance(toolLevel(ResearchTool.FORK), toolLevel(ResearchTool.SCRAPER), toolLevel(ResearchTool.STRETCHER),
                toolLevel(ResearchTool.MORTAR));
    }

    /** Studied, or anything in creative mode. */
    private boolean studied(Item item) {
        return creative() || ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(item));
    }

    private boolean creative() {
        return minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    private void send(ResearchActionPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    private static void sound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public void onMessage(Component text, ResearchMessagePayload.Tone tone) {
        if (text.getString().isEmpty()) {
            return;
        }
        toast = text;
        toastTone = tone;
        toastStart = Util.getMillis();
    }

    // ---- StarBook.Host --------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public ComplexRune studying() {
        ResearchTableBlockEntity table = table();
        return table == null || table.isStudyingItem() ? null : table.sheet().rune();
    }

    @Override
    public void study(ComplexRune rune) {
        send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.SELECT, rune.index()));
        sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        setTab(Tab.DESK);
    }

    // ---- The thread -----------------------------------------------------------------------------------------------

    @Nullable
    private RuneBoard currentBoard() {
        ResearchTableBlockEntity table = table();
        ComplexRune rune = table == null || table.isStudyingItem() ? null : table.sheet().rune();
        return rune == null ? null : RuneBoards.request(rune).getNow(null);
    }

    /** Takes the server's thread whenever the server changed it and the researcher is not drawing. */
    private void syncThread(ResearchTableBlockEntity table, RuneBoard current) {
        ResearchSheet sheet = table.sheet();
        if (threadRune != current.rune()) {
            threadRune = current.rune();
            thread = sheet.thread().clone();
            serverRevision = sheet.revision();
            board.reset();
            trace = null;
        } else if (!dragging && serverRevision != sheet.revision()) {
            serverRevision = sheet.revision();
            thread = sheet.thread().clone();
            trace = null;
        }
        if (trace == null) {
            trace = ResearchSheet.trace(current, thread, allowance());
            if (trace == null) {
                thread = new int[]{HexGrid.CENTRE};
                trace = ResearchSheet.trace(current, thread, allowance());
            }
        }
    }

    /** The ink in the Essence Tanks around the desk, read once per frame. */
    private InkSupply inkSupply() {
        ResearchTableBlockEntity table = table();
        if (table == null || table.getLevel() == null) {
            return null;
        }
        if (supplyFrame != Util.getMillis() || supply == null) {
            supply = table.inkSupply();
            supplyFrame = Util.getMillis();
        }
        return supply;
    }

    private int[] ink() {
        InkSupply ink = inkSupply();
        return ink == null ? new int[SimpleRune.values().length] : ink.available();
    }

    private int[] inkCapacity() {
        InkSupply ink = inkSupply();
        return ink == null ? new int[SimpleRune.values().length] : ink.capacity();
    }

    private boolean done(ResearchTableBlockEntity table) {
        return table.sheet().isDone() && Arrays.equals(thread, table.sheet().thread());
    }

    /** Tries to step the thread into a neighbouring section; on failure the section shakes and says why. */
    private boolean extend(RuneBoard current, int cell) {
        if (missing() != null) {
            nudgeRequirement(cell);
            return false;
        }
        int[] candidate = Arrays.copyOf(thread, thread.length + 1);
        candidate[thread.length] = cell;
        ResearchSheet.Trace next = ResearchSheet.trace(current, candidate, allowance());
        if (next == null) {
            refuse(cell, 0.6F);
            return false;
        }
        if (!creative() && !next.affordable(ink())) {
            SimpleRune lacking = ResearchSheet.toward(thread[thread.length - 1], cell);
            vialFlash[lacking.ordinal()] = Util.getMillis();
            refuse(cell, 0.8F);
            return false;
        }
        int lit = trace == null ? 0 : trace.lit();
        thread = candidate;
        trace = next;
        int point = current.pointAt(cell);
        if ((next.lit() & ~lit) != 0 && point >= 0) {
            sound(ModSounds.RUNE_RECOGNISED.get(), 0.8F + current.points().get(point).element().ordinal() * 0.06F, 0.6F);
        } else if (++lastStepSound % 2 == 0) {
            sound(ModSounds.BRUSH_STROKE.get(), 0.9F + (float) Math.random() * 0.3F, 0.25F);
        }
        if (next.complete(current)) {
            dragging = false;
            send(ResearchActionPayload.thread(menu.pos(), thread));
        }
        return true;
    }

    private void retract() {
        if (thread.length > 1) {
            thread = Arrays.copyOf(thread, thread.length - 1);
            trace = null;
            RuneBoard current = currentBoard();
            if (current != null) {
                trace = ResearchSheet.trace(current, thread, allowance());
            }
        }
    }

    private void refuse(int cell, float pitch) {
        board.shake(cell);
        sound(SoundEvents.NOTE_BLOCK_BASS.value(), pitch, 0.5F);
    }

    private void nudgeRequirement(int cell) {
        requirementFlash = Util.getMillis();
        board.shake(cell);
        sound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5F, 0.5F);
    }

    /** Follows the mouse section by section while drawing: forward into new sections, back along the thread. */
    private void follow(RuneBoard current, double mouseX, double mouseY) {
        for (int guard = 0; guard < 24; guard++) {
            int head = thread[thread.length - 1];
            int target = board.cellAt(mouseX, mouseY);
            if (target < 0 || target == head) {
                return;
            }
            int[] h = board.screen(head);
            double here = Math.hypot(mouseX - h[0], mouseY - h[1]);
            int best = -1;
            double bestDistance = here;
            for (int next : HexGrid.neighbours(head)) {
                int[] p = board.screen(next);
                double d = Math.hypot(mouseX - p[0], mouseY - p[1]);
                if (d < bestDistance) {
                    best = next;
                    bestDistance = d;
                }
            }
            if (best < 0) {
                return;
            }
            if (thread.length >= 2 && best == thread[thread.length - 2]) {
                retract();
            } else if (!extend(current, best)) {
                return;
            }
            if (!dragging) {
                return;
            }
        }
    }

    private void release() {
        ResearchTableBlockEntity table = table();
        dragging = false;
        if (table != null && !Arrays.equals(thread, table.sheet().thread())) {
            send(ResearchActionPayload.thread(menu.pos(), thread));
        }
    }

    // ---- Rendering ------------------------------------------------------------------------------------------------

    /** Everything above the background: slots, the book, then tooltips (shown after the frame). */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (tab == Tab.BOOK) {
            List<FormattedCharSequence> tooltip = book.render(graphics, mouseX, mouseY);
            if (tooltip != null) {
                graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
        } else {
            renderSlotOverlays(graphics, mouseX, mouseY);
            renderToast(graphics);
        }
        PixelArt.flush(graphics);
        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : pendingTooltip) {
                lines.addAll(font.split(line, 220));
            }
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
        renderTabTooltips(graphics, mouseX, mouseY);
    }

    /** The tab's background, the parchment game and the side panel; runs before {@link #extractRenderState}. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = null;
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, tab == Tab.BOOK ? BOOK_TEXTURE : DESK_TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight, imageWidth, imageHeight);
        renderTabs(graphics, mouseX, mouseY);
        if (tab == Tab.DESK) {
            renderDesk(graphics, mouseX, mouseY);
        }
        PixelArt.flush(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        PixelArt.flush(graphics);
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF000000 | TEXT, true);
        Component codex = Component.translatable(KEY + "codex", ClientKnowledge.get().runeCount(), 72);
        PixelArt.flush(graphics);
        graphics.text(font, codex, imageWidth - 2 * TAB_WIDTH - 32 - font.width(codex), titleLabelY, 0xFF000000 | TEXT_MUTED, true);
    }

    private int tabX(int index) {
        return leftPos + imageWidth - 20 - (2 - index) * (TAB_WIDTH + 2);
    }

    private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack[] icons = {new ItemStack(ModItems.RESEARCHERS_DIARY.get()), new ItemStack(ModItems.RESEARCH_TABLE.get())};
        for (int i = 0; i < 2; i++) {
            int x = tabX(i);
            int y = topPos + 1;
            boolean selected = tab.ordinal() == i;
            boolean hovered = isOver(mouseX, mouseY, x, y, TAB_WIDTH, TAB_HEIGHT);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, x, y, (selected ? 2 : hovered ? 1 : 0) * TAB_WIDTH, 0, TAB_WIDTH, TAB_HEIGHT,
                    WIDGETS_SIZE_X, WIDGETS_SIZE_Y);
            PixelArt.flush(graphics);
            graphics.item(icons[i], x + 4, y + 2);
        }
    }

    private void renderTabTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String[] keys = {"tab.book", "tab.desk"};
        for (int i = 0; i < 2; i++) {
            if (isOver(mouseX, mouseY, tabX(i), topPos + 1, TAB_WIDTH, TAB_HEIGHT)) {
                graphics.setTooltipForNextFrame(font, Component.translatable(KEY + keys[i]), mouseX, mouseY);
            }
        }
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean overBoard(double mouseX, double mouseY) {
        return isOver(mouseX, mouseY, leftPos + BOARD_X, topPos + BOARD_Y, BOARD_W, BOARD_H);
    }

    private void renderDesk(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ResearchTableBlockEntity table = table();
        int hovered = overBoard(mouseX, mouseY) ? board.cellAt(mouseX, mouseY) : -1;
        int[] reserved = new int[SimpleRune.values().length];
        if (table != null && table.isStudyingItem() && table.study().item() != null) {
            renderStudy(graphics, table, hovered, mouseX, mouseY);
        } else {
            RuneBoard current = currentBoard();
            if (table == null || current == null) {
                board.renderEmpty(graphics, overBoard(mouseX, mouseY));
                renderEmptyHeader(graphics);
            } else {
                syncThread(table, current);
                renderRune(graphics, table, current, hovered, mouseX, mouseY);
                if (trace != null && !done(table)) {
                    reserved = trace.ink();
                }
            }
        }
        renderVials(graphics, reserved, mouseX, mouseY);
        renderGhosts(graphics);
    }

    // ---- The rune -------------------------------------------------------------------------------------------------

    private void renderRune(GuiGraphicsExtractor graphics, ResearchTableBlockEntity table, RuneBoard current, int hovered,
                            int mouseX, int mouseY) {
        ComplexRune rune = current.rune();
        int lens = toolLevel(ResearchTool.LENS);
        String key = rune.key() + "|" + thread.length + "|" + thread[thread.length - 1] + "|" + ClientKnowledge.version() + "|" + lens;
        if (!key.equals(fogKey)) {
            fogKey = key;
            board.updateFog(ClientKnowledge.get().seen(rune), thread, lens);
        }
        boolean done = done(table);
        boolean ready = !table.getItem(ResearchTableBlockEntity.HEXTECH).isEmpty() || done;
        int preview = -1;
        if (!dragging && !done && hovered >= 0 && thread.length >= 1 && HexGrid.adjacent(thread[thread.length - 1], hovered)
                && missing() == null) {
            int[] candidate = Arrays.copyOf(thread, thread.length + 1);
            candidate[thread.length] = hovered;
            ResearchSheet.Trace next = ResearchSheet.trace(current, candidate, allowance());
            if (next != null && (creative() || next.affordable(ink()))) {
                preview = hovered;
            }
        }
        board.renderRune(graphics, font, current, thread, trace, done, dragging, hovered, preview, ready);
        if (hovered >= 0 && !dragging) {
            int point = current.pointAt(hovered);
            if (point >= 0) {
                pendingTooltip = List.of(ResearchText.element(current.points().get(point).element()));
            }
        }

        // Header: the rune, its stars, and the thread's length against the star marks.
        int x0 = leftPos + PANEL_X;
        int y0 = topPos;
        PixelArt.glyph(graphics, rune, x0 + 7, y0 + 29, 24, PixelArt.argb(rune.color(), 1), 0xFF000000);
        PixelArt.flush(graphics);
        graphics.text(font, Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD), x0 + 38, y0 + 29,
                0xFF000000 | PixelArt.mix(rune.color(), 0xFFFFFF, 0.35F), true);
        PixelArt.flush(graphics);
        graphics.text(font, starsText(ClientKnowledge.get().stars(rune)), x0 + 38, y0 + 41, 0xFFFFFFFF, true);
        renderLengthMeter(graphics, current, x0 + 82, y0 + 44);
    }

    /**
     * A thin ruler of the thread's length: gold up to three stars, silver up to two; the thread's end slides along it.
     */
    private void renderLengthMeter(GuiGraphicsExtractor graphics, RuneBoard current, int x, int y) {
        int width = 120;
        int max = current.par().two() + Math.max(6, current.par().two() / 3);
        int three = x + width * current.par().three() / max;
        int two = x + width * current.par().two() / max;
        graphics.fill(x, y, three, y + 3, 0xFFD6AB52);
        graphics.fill(three, y, two, y + 3, 0xFFB8C4CC);
        graphics.fill(two, y, x + width, y + 3, 0xFF6E4D2A);
        int length = thread.length - 1;
        int at = x + Math.min(width, width * length / max);
        int colour = length <= current.par().three() ? 0xFFFFF3C0 : length <= current.par().two() ? 0xFFFFFFFF : 0xFFF3A59B;
        graphics.fill(at - 1, y - 2, at + 1, y + 5, colour);
    }

    private void renderEmptyHeader(GuiGraphicsExtractor graphics) {
        PixelArt.flush(graphics);
        graphics.text(font, Component.translatable(KEY + "choose"), leftPos + PANEL_X + 8, topPos + 36, 0xFF000000 | TEXT_MUTED, true);
    }

    // ---- The item study -------------------------------------------------------------------------------------------

    private void renderStudy(GuiGraphicsExtractor graphics, ResearchTableBlockEntity table, int hovered, int mouseX, int mouseY) {
        ItemStudy study = table.study();
        Item item = study.item();
        ItemElements elements = ResearchItems.of(item);
        if (elements == null) {
            return;
        }
        board.renderStudy(graphics, font, item, elements, study, hovered);

        int x0 = leftPos + PANEL_X;
        int y0 = topPos;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x0 + 7, y0 + 29);
        graphics.pose().scale(1.5F, 1.5F);
        PixelArt.flush(graphics);
        graphics.item(new ItemStack(item), 0, 0);
        graphics.pose().popMatrix();
        PixelArt.flush(graphics);
        graphics.text(font, ResearchText.item(item).copy().withStyle(ChatFormatting.BOLD), x0 + 38, y0 + 29, 0xFF000000 | TEXT, true);
        // Strikes left: brass tokens, spent ones dark.
        for (int i = 0; i < study.maxStrikes(); i++) {
            boolean left = i >= study.strikes();
            PixelArt.hex(graphics, x0 + 42 + i * 10, y0 + 46, 7, PixelArt.argb(left ? 0xD6AB52 : 0x2A2018, 1),
                    PixelArt.argb(left ? 0x6E4A16 : 0x4A3A28, 1));
        }
        if (hovered >= 0) {
            int string = ItemStudy.stringOf(hovered);
            if (string >= 0) {
                pendingTooltip = List.of(ResearchText.element(HexGrid.RING[string]));
            }
        }
    }

    // ---- Vials, slots, toast --------------------------------------------------------------------------------------

    /** The nine ink vials: liquid rises as items dissolve; the part the current thread would spend shimmers. */
    private void renderVials(GuiGraphicsExtractor graphics, int[] reserved, int mouseX, int mouseY) {
        long now = Util.getMillis();
        int[] ink = ink();
        int[] capacity = inkCapacity();
        boolean infinite = creative();
        for (int i = 0; i < HexGrid.RING.length; i++) {
            SimpleRune element = HexGrid.RING[i];
            int e = element.ordinal();
            int x = leftPos + VIAL_X + i * VIAL_STEP;
            int y = topPos + VIAL_Y;
            int full = infinite ? 1 : Math.max(1, capacity[e]);
            int amount = infinite ? 1 : ink[e];
            if (vialLevel[e] < 0) {
                vialLevel[e] = amount;
            }
            boolean rising = vialLevel[e] < amount - 0.5F;
            vialLevel[e] += (amount - vialLevel[e]) * 0.12F;
            float level = Math.min(1, vialLevel[e] / full);
            int liquid = Math.round(level * VIAL_H);
            int spend = infinite ? 0 : Math.min(liquid, Math.round(Math.min(reserved[e], amount) / (float) full * VIAL_H));
            int color = element.color();
            float flash = Math.max(0, 1 - (now - vialFlash[e]) / (float) FLASH_MILLIS);
            int shake = flash > 0 ? Math.round(Mth.sin(now / 25.0F) * 2 * flash) : 0;
            x += shake;
            // Glass.
            graphics.fill(x - 1, y - 1, x + VIAL_W + 1, y + VIAL_H + 1, flash > 0 ? PixelArt.argb(0xE04A3A, flash) : 0xFF120B05);
            graphics.fill(x, y, x + VIAL_W, y + VIAL_H, 0xFF1C1A22);
            // Liquid with a wobbling surface.
            for (int col = 0; col < VIAL_W; col++) {
                int wobble = liquid > 1 && liquid < VIAL_H ? Math.round(Mth.sin(now / 260.0F + col * 0.7F + i)) : 0;
                int topY = y + VIAL_H - liquid + wobble;
                int shade = col < 3 ? PixelArt.mix(color, 0xFFFFFF, 0.25F) : col > VIAL_W - 3 ? PixelArt.mix(color, 0x000000, 0.3F) : color;
                if (liquid > 0) {
                    graphics.fill(x + col, Math.max(y, topY), x + col + 1, y + VIAL_H, 0xFF000000 | shade);
                    graphics.fill(x + col, Math.max(y, topY), x + col + 1, Math.max(y, topY) + 1, 0xFF000000 | PixelArt.mix(color, 0xFFFFFF, 0.55F));
                }
                if (spend > 0) {
                    int from = y + VIAL_H - liquid + Math.max(0, wobble);
                    for (int row = from; row < from + spend && row < y + VIAL_H; row++) {
                        if ((row + col + now / 120) % 3 == 0) {
                            graphics.fill(x + col, row, x + col + 1, row + 1, 0xC0FFFFFF);
                        }
                    }
                }
            }
            if (rising || infinite) {
                for (int b = 0; b < 3; b++) {
                    float t = ((now + b * 377L + i * 91L) % 900) / 900.0F;
                    int bx = x + 2 + (b * 4 + i) % (VIAL_W - 3);
                    int by = y + VIAL_H - 1 - Math.round(t * Math.max(1, liquid - 1));
                    graphics.fill(bx, by, bx + 1, by + 1, PixelArt.argb(0xFFFFFF, 0.7F * (1 - t)));
                }
            }
            // Glass highlight.
            graphics.fill(x + 1, y + 1, x + 2, y + VIAL_H - 2, 0x40FFFFFF);
            PixelArt.radical(graphics, element, x + VIAL_W / 2 - 4, y + VIAL_H + 4, 9, PixelArt.argb(color, 1));
            if (isOver(mouseX, mouseY, x - 2, y - 2, VIAL_W + 4, VIAL_H + 16)) {
                Component count = infinite ? Component.literal("∞") : Component.literal(ink[e] + "/" + capacity[e]);
                boolean noStorage = !infinite && capacity[e] == 0;
                pendingTooltip = List.of(Component.translatable(KEY + "ink", ResearchText.element(element)),
                        count.copy().withStyle(noStorage ? ChatFormatting.RED : ChatFormatting.GRAY),
                        Component.translatable(KEY + (noStorage ? "ink.no_storage" : "ink.hint"), ResearchText.element(element))
                                .withStyle(noStorage ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
            }
        }
    }

    /** Ghost icons in empty slots and a pulsing frame around a slot that needs something. */
    private void renderGhosts(GuiGraphicsExtractor graphics) {
        ghost(graphics, ResearchTableBlockEntity.DIARY, ResearchTableMenu.DIARY_X, ResearchTableMenu.ROW_Y, GHOST_DIARY);
        ghost(graphics, ResearchTableBlockEntity.HEXTECH, ResearchTableMenu.HEXTECH_X, ResearchTableMenu.ROW_Y, GHOST_ORB);
        ghost(graphics, ResearchTableBlockEntity.PAPER, ResearchTableMenu.PAPER_X, ResearchTableMenu.ROW_Y, GHOST_PAPER);
        ghost(graphics, ResearchTableBlockEntity.STUDY, ResearchTableMenu.STUDY_X, ResearchTableMenu.ROW_Y, GHOST_STUDY);
        for (ResearchTool tool : ResearchTool.values()) {
            int x = ResearchTableMenu.TOOLS_X + tool.ordinal() * ResearchTableMenu.TOOL_SPACING;
            ghost(graphics, ResearchTableBlockEntity.TOOLS + tool.ordinal(), x, ResearchTableMenu.TOOLS_Y, GHOST_TOOLS + tool.ordinal());
        }

        ResearchTableBlockEntity table = table();
        boolean wantsRune = table != null && !table.isStudyingItem() && table.sheet().rune() != null;
        Requirement missing = wantsRune ? missing() : missingDiary();
        float strong = Math.max(0, 1 - (Util.getMillis() - requirementFlash) / 1500.0F);
        if (missing != null) {
            int x = switch (missing) {
                case DIARY, OWN_DIARY -> ResearchTableMenu.DIARY_X;
                case HEXTECH -> ResearchTableMenu.HEXTECH_X;
                case PAPER -> ResearchTableMenu.PAPER_X;
            };
            highlight(graphics, leftPos + x, topPos + ResearchTableMenu.ROW_Y, 0xFFD36E, strong);
        }
    }

    private void ghost(GuiGraphicsExtractor graphics, int slot, int x, int y, int icon) {
        if (menu.table().getItem(slot).isEmpty()) {
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, WIDGETS, leftPos + x, topPos + y, icon * 16, 24, 16, 16, WIDGETS_SIZE_X, WIDGETS_SIZE_Y);
        }
    }

    /** A pulsing frame around a slot; {@code strong} (0-1) makes it flare after a refused move. */
    private static void highlight(GuiGraphicsExtractor graphics, int x, int y, int rgb, float strong) {
        float pulse = 0.5F + 0.5F * Mth.sin(Util.getMillis() / (strong > 0 ? 60.0F : 160.0F));
        int color = PixelArt.argb(rgb, 0.45F + 0.5F * pulse);
        int g = 2 + Math.round(strong * 2);
        graphics.fill(x - g, y - g, x + 16 + g, y - g + 1, color);
        graphics.fill(x - g, y + 15 + g, x + 16 + g, y + 16 + g, color);
        graphics.fill(x - g, y - g + 1, x - g + 1, y + 15 + g, color);
        graphics.fill(x + 15 + g, y - g + 1, x + 16 + g, y + 15 + g, color);
    }

    /** Star pips under tools, a check mark on studied items, and slot hints. */
    private void renderSlotOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        for (ResearchTool tool : ResearchTool.values()) {
            if (menu.table().getItem(ResearchTableBlockEntity.TOOLS + tool.ordinal()).isEmpty()) {
                continue;
            }
            int x = leftPos + ResearchTableMenu.TOOLS_X + tool.ordinal() * ResearchTableMenu.TOOL_SPACING;
            int y = topPos + ResearchTableMenu.TOOLS_Y;
            int level = toolLevel(tool);
            for (int s = 0; s < 3; s++) {
                graphics.fill(x + 2 + s * 5, y + 17, x + 5 + s * 5, y + 19, s < level ? 0xFFE0A21C : 0xFF3A2712);
            }
        }
        ItemStack study = menu.table().getItem(ResearchTableBlockEntity.STUDY);
        if (!study.isEmpty() && studied(study.getItem())) {
            PixelArt.flush(graphics);
            graphics.text(font, "✔", leftPos + ResearchTableMenu.STUDY_X + 10, topPos + ResearchTableMenu.ROW_Y - 3, 0xFF8FE07A, true);
        }

        Slot slot = hoveredSlot;
        if (slot == null || slot.container != menu.table() || !menu.getCarried().isEmpty()) {
            return;
        }
        int index = slot.getContainerSlot();
        if (index >= ResearchTableBlockEntity.TOOLS && index < ResearchTableBlockEntity.STUDY) {
            ResearchTool tool = ResearchTool.values()[index - ResearchTableBlockEntity.TOOLS];
            int level = toolLevel(tool);
            List<Component> lines = new ArrayList<>();
            lines.add(ResearchText.item(tool.item()).copy().withStyle(ChatFormatting.GOLD));
            if (slot.hasItem() && level > 0) {
                lines.add(Component.literal("★".repeat(level) + " ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(tool.translationKey() + ".level." + level).withStyle(ChatFormatting.WHITE)));
            } else {
                lines.add(Component.translatable(KEY + "slot.tool", Component.translatable(tool.rune().translationKey())
                        .withColor(tool.rune().color())).withStyle(ChatFormatting.GRAY));
            }
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        if (slot.hasItem()) {
            return;
        }
        String hint = switch (index) {
            case ResearchTableBlockEntity.DIARY -> "diary";
            case ResearchTableBlockEntity.HEXTECH -> "hextech";
            case ResearchTableBlockEntity.PAPER -> "paper";
            case ResearchTableBlockEntity.STUDY -> "study";
            default -> "output";
        };
        graphics.setTooltipForNextFrame(font, font.split(Component.translatable(KEY + "slot." + hint), 180), mouseX, mouseY);
    }

    /** The last message: a dark plaque over the bottom of the parchment that fades away. */
    private void renderToast(GuiGraphicsExtractor graphics) {
        long age = Util.getMillis() - toastStart;
        if (toast.getString().isEmpty() || age > TOAST_MILLIS) {
            return;
        }
        float alpha = Math.min(1, Math.min(age / 150.0F, (TOAST_MILLIS - age) / 600.0F));
        int rise = Math.round(Math.max(0, 1 - age / 200.0F) * 6);
        List<FormattedCharSequence> lines = font.split(toast, BOARD_W - 30);
        int height = lines.size() * 10 + 8;
        int width = 0;
        for (FormattedCharSequence line : lines) {
            width = Math.max(width, font.width(line));
        }
        int cx = leftPos + BOARD_X + BOARD_W / 2;
        int y = topPos + BOARD_Y + BOARD_H - height - 8 + rise;
        int color = switch (toastTone) {
            case GOOD -> 0x9FE3FF;
            case BAD -> 0xF3A59B;
            case WIN -> 0xFFD36E;
            default -> TEXT;
        };
        PixelArt.flush(graphics);
        graphics.fill(cx - width / 2 - 7, y, cx + width / 2 + 7, y + height, PixelArt.argb(0x140C06, 0.88F * alpha));
        graphics.fill(cx - width / 2 - 7, y, cx + width / 2 + 7, y + 1, PixelArt.argb(color, 0.8F * alpha));
        for (int i = 0; i < lines.size(); i++) {
            graphics.centeredText(font, lines.get(i), cx, y + 5 + i * 10, PixelArt.argb(color, Math.max(0.05F, alpha)));
        }
    }

    private static Component starsText(int count) {
        return Component.literal("★".repeat(count)).withStyle(ChatFormatting.GOLD)
                .append(Component.literal("★".repeat(3 - count)).withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---- Input ----------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (int i = 0; i < 2; i++) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT && isOver(mouseX, mouseY, tabX(i), topPos + 1, TAB_WIDTH, TAB_HEIGHT)) {
                if (tab.ordinal() != i) {
                    release();
                    setTab(Tab.values()[i]);
                    sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.6F);
                }
                return true;
            }
        }
        if (tab == Tab.BOOK && book.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (tab == Tab.DESK && overBoard(mouseX, mouseY)) {
            boardPressed(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void boardPressed(double mouseX, double mouseY, int button) {
        ResearchTableBlockEntity table = table();
        if (table == null) {
            return;
        }
        int cell = board.cellAt(mouseX, mouseY);
        if (table.isStudyingItem() && table.study().item() != null) {
            ItemStudy study = table.study();
            ItemElements elements = ResearchItems.of(study.item());
            if (button != InputConstants.MOUSE_BUTTON_LEFT || elements == null) {
                return;
            }
            if (!study.isActive(elements)) {
                send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.CLOSE_STUDY, 0));
            } else if (!study.started() && Requirement.paper(menu.table(), minecraft.player) != null) {
                nudgeRequirement(cell < 0 ? HexGrid.CENTRE : cell);
            } else if (study.canStrike(cell, elements)) {
                send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.STRIKE, cell));
                sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.2F, 0.6F);
            }
            return;
        }
        RuneBoard current = currentBoard();
        if (current == null) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                setTab(Tab.BOOK);
                sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            }
            return;
        }
        syncThread(table, current);
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            // Wind the thread back to the point before its end (or to the orb).
            if (thread.length > 1 && !done(table)) {
                int keep = 1;
                for (int i = thread.length - 2; i >= 1; i--) {
                    if (current.pointAt(thread[i]) >= 0) {
                        keep = i + 1;
                        break;
                    }
                }
                thread = Arrays.copyOf(thread, keep);
                trace = ResearchSheet.trace(current, thread, allowance());
                sound(ModSounds.BRUSH_STROKE.get(), 0.6F, 0.4F);
                release();
            }
            return;
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT || cell < 0) {
            return;
        }
        if (done(table)) {
            if (cell == HexGrid.CENTRE) {
                thread = new int[]{HexGrid.CENTRE};
                trace = ResearchSheet.trace(current, thread, allowance());
                dragging = true;
            }
            return;
        }
        int index = -1;
        for (int i = thread.length - 1; i >= 0; i--) {
            if (thread[i] == cell) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            if (index < thread.length - 1) {
                thread = Arrays.copyOf(thread, index + 1);
                trace = ResearchSheet.trace(current, thread, allowance());
                sound(ModSounds.BRUSH_STROKE.get(), 0.7F, 0.3F);
            }
            dragging = true;
        } else if (HexGrid.adjacent(thread[thread.length - 1], cell)) {
            dragging = extend(current, cell) && !trace.complete(current);
        } else {
            board.shake(thread[thread.length - 1]);
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (tab == Tab.BOOK && book.mouseDragged(event.x(), event.y(), event.button())) {
            return true;
        }
        if (tab == Tab.DESK && dragging) {
            RuneBoard current = currentBoard();
            if (current != null) {
                follow(current, event.x(), event.y());
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (tab == Tab.BOOK && book.mouseReleased(event.x(), event.y(), event.button())) {
            return true;
        }
        if (dragging) {
            release();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.BOOK && book.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        if (dragging) {
            release();
        }
        super.removed();
    }
}
