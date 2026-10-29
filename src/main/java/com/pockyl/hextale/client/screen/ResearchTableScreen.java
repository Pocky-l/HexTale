package com.pockyl.hextale.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.pockyl.hextale.port.client.KeyEvent;
import com.pockyl.hextale.port.client.MouseButtonEvent;
import com.pockyl.hextale.port.client.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.InkSupply;
import com.pockyl.hextale.block.Requirement;
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
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.ResearchBoard;
import com.pockyl.hextale.research.ResearchBoards;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Research Table, in two tabs: the researcher's book (choose a device or tool to research) and the desk - the
 * parchment on the left, where the research topic's ink is drawn with the mouse, and on the right the table's ink vials,
 * slots, the loupe arm (drag it onto an item to study it) and the player's inventory.
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> implements StarBook.Host {
    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics portGraphics, float portPartialTick, int portMouseX, int portMouseY) {
    }

    private static final ResourceLocation BOOK_TEXTURE = Hextale.id("textures/gui/research_book.png");
    private static final ResourceLocation DESK_TEXTURE = Hextale.id("textures/gui/research_desk.png");
    private static final ResourceLocation WIDGETS = Hextale.id("textures/gui/research_table_widgets.png");
    private static final int WIDGETS_SIZE_X = 256;
    private static final int WIDGETS_SIZE_Y = 128;
    private static final String KEY = "gui.hextale.research_table.";
    private static final int TAB_WIDTH = 24;
    private static final int TAB_HEIGHT = 20;
    private static final int TEXT = 0xF0E3C2;
    private static final int TEXT_MUTED = 0xC9B48A;
    private static final int PANEL_X = 252;
    private static final int BOARD_X = BoardView.SHEET_X;
    private static final int BOARD_Y = BoardView.SHEET_Y;
    private static final int BOARD_W = BoardView.SHEET_W;
    private static final int BOARD_H = BoardView.SHEET_H;
    /** The button that burns the parchment on purpose, in the paper's top right corner; a second click within the arm time burns. */
    private static final int BURN_X = 225;
    private static final int BURN_Y = 31;
    private static final int BURN_SIZE = 16;
    private static final long BURN_ARM_MILLIS = 3000;
    /** Text in the header runs from beside the topic's icon to the panel's right frame. */
    private static final int HEADER_TEXT_W = 168;
    /** Ink vials sit in the six frames drawn on the desk texture (outer x 290 + n * 23, y 61-96), filled from the left. */
    private static final int VIAL_X = 292;
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

    private enum Tab {
        BOOK,
        DESK
    }

    private final BoardView board = new BoardView();
    private final LoupeView loupe = new LoupeView();
    private StarBook book;
    private Tab tab;
    @Nullable
    private List<Component> pendingTooltip;

    // The drawing: the client's copy (steps as from, to pairs), sent to the server when the mouse is released.
    private int[] tree = new int[0];
    /** Where the line being drawn ends now. */
    private int head = HexGrid.CENTRE;
    /** Steps of the drawing before the current stroke: dragging back only winds up this stroke. */
    private int strokeBase;
    /** Flags the researcher set on dark sections, per topic; kept while the game runs. */
    private static final Map<ResearchTopic, BitSet> FLAGS = new HashMap<>();
    @Nullable
    private ResearchTopic threadTopic;
    private int threadAttempt;
    private int serverRevision = Integer.MIN_VALUE;
    private boolean dragging;
    private ResearchSheet.@Nullable Trace trace;
    private String fogKey = "";
    /** Parchments burned on this desk as last seen, -1 before the first look: a new burn plays {@link #burning}. */
    private int seenBurns = -1;
    /** The board drawn last frame, and the burning parchment while it burns. */
    @Nullable
    private ResearchBoard lastBoard;
    private BoardView.@Nullable Burn burning;
    /** When the burn button was clicked once (it waits for a second click), 0 = not armed. */
    private long burnArmed;

    // Feedback.
    private Component toast = Component.empty();
    private ResearchMessagePayload.Tone toastTone = ResearchMessagePayload.Tone.INFO;
    private long toastStart;
    private final long[] vialFlash = new long[Essence.values().length];
    private final float[] vialLevel = new float[Essence.values().length];
    @Nullable
    private InkSupply supply;
    private long supplyFrame;
    private long requirementFlash;
    private int lastStepSound;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = ResearchTableMenu.IMAGE_WIDTH;
        this.imageHeight = ResearchTableMenu.IMAGE_HEIGHT;
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
            boolean subject = table != null && table.sheet().topic() != null;
            setTab(subject || missingDiary() != null ? Tab.DESK : Tab.BOOK);
            Arrays.fill(vialLevel, -1);
        }
        book.layout(leftPos + 4, topPos + 24, imageWidth - 8, imageHeight - 28);
        board.layout(leftPos, topPos);
        loupe.layout(leftPos, topPos);
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

    private void send(ResearchActionPayload payload) {
        PacketDistributor.sendToServer(payload);
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
    public ResearchTopic researching() {
        ResearchTableBlockEntity table = table();
        return table == null ? null : table.sheet().topic();
    }

    @Override
    public void research(ResearchTopic topic) {
        send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.SELECT, topic.index()));
        sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        setTab(Tab.DESK);
    }

    // ---- The thread -----------------------------------------------------------------------------------------------

    @Nullable
    private ResearchBoard currentBoard() {
        ResearchTableBlockEntity table = table();
        ResearchTopic topic = table == null ? null : table.sheet().topic();
        return topic == null ? null : ResearchBoards.request(topic, table.sheet().attempt()).getNow(null);
    }

    /** Takes the server's drawing whenever the server changed it and the researcher is not drawing. */
    private void syncThread(ResearchTableBlockEntity table, ResearchBoard current) {
        ResearchSheet sheet = table.sheet();
        if (threadTopic != current.topic() || threadAttempt != sheet.attempt()) {
            threadTopic = current.topic();
            threadAttempt = sheet.attempt();
            tree = sheet.tree().clone();
            serverRevision = sheet.revision();
            board.reset();
            trace = null;
            head = lastCell();
        } else if (!dragging && serverRevision != sheet.revision()) {
            serverRevision = sheet.revision();
            tree = sheet.tree().clone();
            trace = null;
            head = lastCell();
        }
        if (trace == null) {
            trace = ResearchSheet.trace(current, tree);
            if (trace == null) {
                tree = new int[0];
                trace = ResearchSheet.trace(current, tree);
                head = HexGrid.CENTRE;
            }
        }
    }

    /** The section the drawing ended on last (the orb for a bare sheet). */
    private int lastCell() {
        return tree.length == 0 ? HexGrid.CENTRE : tree[tree.length - 1];
    }

    private boolean inked(int cell) {
        if (cell == HexGrid.CENTRE) {
            return true;
        }
        for (int i = 1; i < tree.length; i += 2) {
            if (tree[i] == cell) {
                return true;
            }
        }
        return false;
    }

    /** The most recently inked neighbour of a section, where a click on it would draw from; -1 if none. */
    private int inkedNeighbour(int cell) {
        for (int i = tree.length - 1; i >= 1; i -= 2) {
            if (HexGrid.adjacent(tree[i], cell)) {
                return tree[i];
            }
        }
        return HexGrid.adjacent(HexGrid.CENTRE, cell) ? HexGrid.CENTRE : -1;
    }

    private BitSet flags(ResearchTopic topic) {
        return FLAGS.computeIfAbsent(topic, t -> new BitSet());
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

    /** Sections the ink around pays for, per essence. */
    private int[] ink() {
        InkSupply ink = inkSupply();
        return ink == null ? new int[Essence.values().length] : ink.sections();
    }

    /** Millibuckets of ink around, per essence. */
    private int[] inkMb() {
        InkSupply ink = inkSupply();
        return ink == null ? new int[Essence.values().length] : ink.available();
    }

    private int[] inkCapacity() {
        InkSupply ink = inkSupply();
        return ink == null ? new int[Essence.values().length] : ink.capacity();
    }

    private boolean done(ResearchTableBlockEntity table) {
        return table.sheet().isDone() && Arrays.equals(tree, table.sheet().tree());
    }

    /** Tries to ink a step from an inked section into its neighbour; on failure the section shakes and says why. */
    private boolean extend(ResearchBoard current, int from, int cell) {
        if (missing() != null) {
            nudgeRequirement(cell);
            return false;
        }
        if (flags(current.topic()).get(cell)) {
            // A flagged section is the researcher's own warning: take the flag off first.
            refuse(cell);
            return false;
        }
        int[] candidate = Arrays.copyOf(tree, tree.length + 2);
        candidate[tree.length] = from;
        candidate[tree.length + 1] = cell;
        ResearchSheet.Trace next = ResearchSheet.trace(current, candidate);
        if (next == null) {
            refuse(cell);
            return false;
        }
        if (!next.affordable(ink())) {
            vialFlash[ResearchSheet.paying(current, from, cell).ordinal()] = Util.getMillis();
            refuse(cell);
            return false;
        }
        int lit = trace == null ? 0 : trace.lit();
        tree = candidate;
        trace = next;
        head = cell;
        Essence paying = ResearchSheet.paying(current, from, cell);
        int point = current.pointAt(cell);
        if (next.burst() >= 0) {
            dragging = false;
            send(ResearchActionPayload.thread(menu.pos(), tree));
            return true;
        }
        if ((next.lit() & ~lit) != 0 && point >= 0) {
            sound(ModSounds.RUNE_RECOGNISED.get(), 0.95F + (float) Math.random() * 0.1F, 0.6F);
        } else {
            sound(ModSounds.INK_STEP.get(), 0.95F + (float) Math.random() * 0.1F, 0.5F);
            if (++lastStepSound % 3 == 0) {
                sound(ModSounds.BRUSH_STROKE.get(), 0.9F + (float) Math.random() * 0.3F, 0.2F);
            }
        }
        if (next.complete(current)) {
            dragging = false;
            send(ResearchActionPayload.thread(menu.pos(), tree));
        }
        return true;
    }

    /** Takes back the last step of the line being drawn. */
    private void retract() {
        if (tree.length / 2 > strokeBase) {
            head = tree[tree.length - 2];
            tree = Arrays.copyOf(tree, tree.length - 2);
            ResearchBoard current = currentBoard();
            trace = current == null ? null : ResearchSheet.trace(current, tree);
        }
    }

    private void refuse(int cell) {
        board.shake(cell);
        sound(ModSounds.DEVICE_REFUSE.get(), 1.0F, 0.6F);
    }

    private void nudgeRequirement(int cell) {
        requirementFlash = Util.getMillis();
        board.shake(cell);
        sound(ModSounds.DEVICE_REFUSE.get(), 0.9F, 0.6F);
    }

    /** Follows the mouse section by section while drawing: forward into new sections, back along this stroke. */
    private void follow(ResearchBoard current, double mouseX, double mouseY) {
        for (int guard = 0; guard < 24; guard++) {
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
            boolean back = tree.length / 2 > strokeBase && tree[tree.length - 1] == head && tree[tree.length - 2] == best;
            if (back) {
                retract();
            } else if (!extend(current, head, best)) {
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
        if (table != null && !Arrays.equals(tree, table.sheet().tree())) {
            send(ResearchActionPayload.thread(menu.pos(), tree));
        }
    }

    // ---- Rendering ------------------------------------------------------------------------------------------------

    /** Everything above the background: slots, the book, then tooltips (shown after the frame). */
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
        if (tab == Tab.BOOK) {
            List<FormattedCharSequence> tooltip = book.render(graphics, mouseX, mouseY);
            if (tooltip != null) {
                graphics.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
        } else {
            renderSlotOverlays(graphics, mouseX, mouseY);
            renderToast(graphics);
            PixelArt.flush(graphics);
            graphics.nextStratum();
            List<Component> loupeTip = loupe.render(graphics, font, menu, mouseX, mouseY,
                    slot -> send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.LOUPE, slot)),
                    slot -> send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.STUDY, slot)));
            if (loupeTip != null) {
                pendingTooltip = loupeTip;
            }
        }
        PixelArt.flush(graphics);
        if (pendingTooltip != null && !pendingTooltip.isEmpty() && !loupe.held()) {
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
    public void renderBackground(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        pendingTooltip = null;
        super.renderBackground(graphics.vanilla(), mouseX, mouseY, partialTick);
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, tab == Tab.BOOK ? BOOK_TEXTURE : DESK_TEXTURE, leftPos, topPos, 0, 0, imageWidth,
                imageHeight, imageWidth, imageHeight);
        renderTabs(graphics, mouseX, mouseY);
        if (tab == Tab.DESK) {
            renderDesk(graphics, mouseX, mouseY);
        }
        PixelArt.flush(graphics);
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        PixelArt.flush(graphics);
        graphics.text(font, title, titleLabelX, titleLabelY, 0xFF000000 | TEXT, true);
        Component codex = Component.translatable(KEY + "codex", ClientKnowledge.get().runeCount(), ComplexRunes.inUse().size());
        PixelArt.flush(graphics);
        graphics.text(font, codex, imageWidth - Tab.values().length * TAB_WIDTH - 34 - font.width(codex), titleLabelY, 0xFF000000 | TEXT_MUTED, true);
    }

    private int tabX(int index) {
        return leftPos + imageWidth - 20 - (Tab.values().length - index) * (TAB_WIDTH + 2);
    }

    private void renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack[] icons = {new ItemStack(ModItems.RESEARCHERS_DIARY.get()), new ItemStack(ModItems.RESEARCH_TABLE.get())};
        for (int i = 0; i < icons.length; i++) {
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
        for (int i = 0; i < keys.length; i++) {
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
        int[] reserved = new int[Essence.COUNT];
        List<Essence> shown = List.of();
        {
            ResearchBoard current = currentBoard();
            if (table != null) {
                noticeBurn(table);
            }
            if (burning != null && board.renderBurn(graphics, font, burning, current)) {
                renderHeader(graphics, burning.board().topic());
                renderVials(graphics, reserved, burning.board().inks(), mouseX, mouseY);
                renderGhosts(graphics);
                return;
            }
            boolean burnt = burning != null;
            burning = null;
            if (table == null || current == null) {
                board.renderEmpty(graphics, overBoard(mouseX, mouseY));
                renderEmptyHeader(graphics);
            } else {
                syncThread(table, current);
                if (burnt) {
                    board.freshSheet();
                    fogKey = "";
                }
                renderTopic(graphics, table, current, hovered, mouseX, mouseY);
                lastBoard = current;
                shown = current.inks();
                if (trace != null && !done(table)) {
                    reserved = trace.ink().clone();
                    for (int i = 0; i < reserved.length; i++) {
                        reserved[i] *= InkSupply.MB_PER_SECTION;
                    }
                }
            }
        }
        renderVials(graphics, reserved, shown, mouseX, mouseY);
        renderGhosts(graphics);
    }

    // ---- The research topic -------------------------------------------------------------------------------------------------

    /**
     * The server burned the parchment (the orb burst): the old board, with the drawing that set it alight, chars away from
     * the burst and a fresh sheet slides in with the new board.
     */
    private void noticeBurn(ResearchTableBlockEntity table) {
        int burns = table.sheet().burns();
        if (seenBurns >= 0 && burns != seenBurns && lastBoard != null && burning == null) {
            dragging = false;
            burning = new BoardView.Burn(lastBoard, tree.clone(), trace, table.sheet().burnCell(), Util.getMillis());
            FLAGS.remove(lastBoard.topic());
            sound(SoundEvents.FIRECHARGE_USE, 0.8F, 0.5F);
            sound(SoundEvents.FIRE_AMBIENT, 1.0F, 0.8F);
        }
        seenBurns = burns;
    }

    private void renderTopic(GuiGraphicsExtractor graphics, ResearchTableBlockEntity table, ResearchBoard current, int hovered,
                            int mouseX, int mouseY) {
        ResearchTopic topic = current.topic();
        String key = topic.id() + "|" + current.hashCode() + "|" + tree.length + "|" + lastCell() + "|" + ClientKnowledge.version();
        if (!key.equals(fogKey)) {
            fogKey = key;
            board.updateFog(ClientKnowledge.get().seen(topic), ResearchSheet.cells(tree));
        }
        boolean done = done(table);
        boolean ready = !table.getItem(ResearchTableBlockEntity.HEXTECH).isEmpty() || done;
        int preview = -1;
        int previewFrom = -1;
        if (!dragging && !done && hovered >= 0 && !inked(hovered) && missing() == null && !flags(topic).get(hovered)) {
            int from = inkedNeighbour(hovered);
            if (from >= 0) {
                int[] candidate = Arrays.copyOf(tree, tree.length + 2);
                candidate[tree.length] = from;
                candidate[tree.length + 1] = hovered;
                ResearchSheet.Trace next = ResearchSheet.trace(current, candidate);
                if (next != null && next.affordable(ink())) {
                    preview = hovered;
                    previewFrom = from;
                }
            }
        }
        BitSet known = ClientKnowledge.get().seen(topic);
        board.renderTopic(graphics, font, current, tree, trace, done, dragging, hovered, preview, previewFrom, ready, head, known,
                flags(topic));
        if (hovered >= 0 && !dragging) {
            int point = current.pointAt(hovered);
            if (point >= 0) {
                boolean reached = trace != null && (trace.lit() & 1 << point) != 0;
                pendingTooltip = List.of(
                        Component.translatable(KEY + "goal", ResearchText.essence(current.points().get(point).element())),
                        Component.translatable(reached ? KEY + "goal.reached" : KEY + "goal.hint", ResearchSheet.POINT_COST)
                                .withStyle(reached ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }
        int corner = !dragging && overBoard(mouseX, mouseY) ? board.cornerAt(mouseX, mouseY) : -1;
        if (corner >= 0) {
            pendingTooltip = List.of(Component.translatable(KEY + "corner", ResearchText.essence(current.essence(corner))),
                    Component.translatable(KEY + "corner.hint").withStyle(ChatFormatting.GRAY));
        }

        renderHeader(graphics, topic);
        if (!done) {
            board.renderGoals(graphics, font, current, trace == null ? 0 : trace.lit(),
                    Component.translatable(KEY + "goals", Integer.bitCount(trace == null ? 0 : trace.lit()), current.points().size()));
            renderBurnButton(graphics, mouseX, mouseY);
        }
    }

    /** Where a flag may go: a dark section not inked yet. */
    private boolean flaggable(ResearchBoard current, int cell) {
        return !inked(cell) && current.dark(cell);
    }

    private boolean overBurnButton(double mouseX, double mouseY) {
        return isOver(mouseX, mouseY, leftPos + BURN_X, topPos + BURN_Y, BURN_SIZE, BURN_SIZE);
    }

    private boolean burnArmed() {
        return burnArmed != 0 && Util.getMillis() - burnArmed < BURN_ARM_MILLIS;
    }

    /**
     * A small sepia plaque with a flame: burns the parchment to start the topic over on a new board. The first click arms
     * it (the flame flares red and asks again), the second burns.
     */
    private void renderBurnButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean over = overBurnButton(mouseX, mouseY);
        boolean armed = burnArmed();
        float flicker = 0.5F + 0.5F * Mth.sin(Util.getMillis() / 90.0F);
        int x = leftPos + BURN_X;
        int y = topPos + BURN_Y;
        int rim = armed ? 0xB0302A : over ? 0x8A5A12 : 0x5E4526;
        PixelArt.fill(graphics, x, y, x + BURN_SIZE, y + BURN_SIZE, PixelArt.argb(rim, 0.9F));
        PixelArt.fill(graphics, x + 1, y + 1, x + BURN_SIZE - 1, y + BURN_SIZE - 1,
                PixelArt.argb(armed ? 0x3A120C : over ? 0xF3E2B8 : 0xE6D2A4, 1));
        int outer = armed ? PixelArt.mix(0xE04A3A, 0xFFB040, flicker) : over ? 0xD8742A : 0x8A5A3A;
        int inner = armed ? 0xFFE08A : over ? 0xF2C040 : 0xB08A5A;
        // The flame: a tongue with a lick to the left and a bright core.
        int cx = x + 8;
        int base = y + 13;
        PixelArt.fill(graphics, cx - 4, base - 4, cx + 4, base, PixelArt.argb(outer, 1));
        PixelArt.fill(graphics, cx - 3, base - 7, cx + 3, base - 4, PixelArt.argb(outer, 1));
        PixelArt.fill(graphics, cx - 1, base - 10, cx + 2, base - 7, PixelArt.argb(outer, 1));
        PixelArt.fill(graphics, cx, base - 11, cx + 1, base - 10, PixelArt.argb(outer, 1));
        PixelArt.fill(graphics, cx - 4, base - 7, cx - 3, base - 5, PixelArt.argb(outer, 1));
        PixelArt.fill(graphics, cx - 2, base - 4, cx + 2, base - 1, PixelArt.argb(inner, 1));
        PixelArt.fill(graphics, cx - 1, base - 6, cx + 1, base - 4, PixelArt.argb(inner, 1));
        if (over) {
            pendingTooltip = armed
                    ? List.of(Component.translatable(KEY + "burn.confirm").withStyle(ChatFormatting.RED))
                    : List.of(Component.translatable(KEY + "burn"), Component.translatable(KEY + "burn.hint").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Header: what is researched, and whether the researcher has worked it out already. */
    private void renderHeader(GuiGraphicsExtractor graphics, ResearchTopic topic) {
        int x0 = leftPos + PANEL_X;
        int y0 = topPos;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x0 + 7, y0 + 29);
        graphics.pose().scale(1.5F, 1.5F);
        PixelArt.flush(graphics);
        graphics.item(topic.icon(), 0, 0);
        graphics.pose().popMatrix();
        PixelArt.flush(graphics);
        Component title = Component.translatable(topic.translationKey()).withStyle(ChatFormatting.BOLD);
        graphics.text(font, fit(title, HEADER_TEXT_W), x0 + 38, y0 + 29, 0xFF000000 | PixelArt.mix(topic.color(), 0xFFFFFF, 0.35F), true);
        PixelArt.flush(graphics);
        boolean researched = ClientKnowledge.get().researched(topic);
        // The status wraps onto a second line under the title; anything longer is cut.
        List<FormattedCharSequence> status = font.split(Component.translatable(KEY + (researched ? "researched" : "unresearched")), HEADER_TEXT_W);
        for (int i = 0; i < Math.min(2, status.size()); i++) {
            graphics.text(font, status.get(i), x0 + 38, y0 + 39 + i * 9, researched ? 0xFF8FE07A : 0xFF000000 | TEXT_MUTED, true);
        }
    }

    /** The text cut to a width, with an ellipsis when it did not fit. */
    private FormattedCharSequence fit(Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        FormattedText cut = font.substrByWidth(text, width - font.width("..."));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("...")));
    }

    private void renderEmptyHeader(GuiGraphicsExtractor graphics) {
        PixelArt.flush(graphics);
        graphics.text(font, Component.translatable(KEY + "choose"), leftPos + PANEL_X + 8, topPos + 36, 0xFF000000 | TEXT_MUTED, true);
    }

    // ---- Vials, slots, toast --------------------------------------------------------------------------------------

    /**
     * The ink vials of the six essences of the parchment on the desk: liquid rises as the tanks fill; the part the current
     * thread would spend shimmers.
     */
    private void renderVials(GuiGraphicsExtractor graphics, int[] reserved, List<Essence> shown, int mouseX, int mouseY) {
        long now = Util.getMillis();
        int[] ink = inkMb();
        int[] capacity = inkCapacity();
        for (int i = 0; i < shown.size(); i++) {
            Essence element = shown.get(i);
            int e = element.ordinal();
            int x = leftPos + VIAL_X + i * VIAL_STEP;
            int y = topPos + VIAL_Y;
            int full = Math.max(1, capacity[e]);
            int amount = ink[e];
            if (vialLevel[e] < 0) {
                vialLevel[e] = amount;
            }
            boolean rising = vialLevel[e] < amount - 0.5F;
            vialLevel[e] += (amount - vialLevel[e]) * 0.12F;
            float level = Math.min(1, vialLevel[e] / full);
            int liquid = Math.round(level * VIAL_H);
            int spend = Math.min(liquid, Math.round(Math.min(reserved[e], amount) / (float) full * VIAL_H));
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
            if (rising) {
                for (int b = 0; b < 3; b++) {
                    float t = ((now + b * 377L + i * 91L) % 900) / 900.0F;
                    int bx = x + 2 + (b * 4 + i) % (VIAL_W - 3);
                    int by = y + VIAL_H - 1 - Math.round(t * Math.max(1, liquid - 1));
                    graphics.fill(bx, by, bx + 1, by + 1, PixelArt.argb(0xFFFFFF, 0.7F * (1 - t)));
                }
            }
            // Glass highlight.
            graphics.fill(x + 1, y + 1, x + 2, y + VIAL_H - 2, 0x40FFFFFF);
            PixelArt.essence(graphics, element, x + VIAL_W / 2 - 4, y + VIAL_H + 4, 9, PixelArt.argb(color, 1));
            if (isOver(mouseX, mouseY, x - 2, y - 2, VIAL_W + 4, VIAL_H + 16)) {
                Component count = Component.literal(ink[e] + "/" + capacity[e] + " mB");
                boolean noStorage = capacity[e] == 0;
                pendingTooltip = List.of(Component.translatable(KEY + "ink", ResearchText.essence(element)),
                        count.copy().withStyle(noStorage ? ChatFormatting.RED : ChatFormatting.GRAY),
                        Component.translatable(KEY + (noStorage ? "ink.no_storage" : "ink.hint"), ResearchText.essence(element))
                                .withStyle(noStorage ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
            }
        }
    }

    /** Ghost icons in empty slots and a pulsing frame around a slot that needs something. */
    private void renderGhosts(GuiGraphicsExtractor graphics) {
        ghost(graphics, ResearchTableBlockEntity.DIARY, ResearchTableMenu.DIARY_X, ResearchTableMenu.ROW_Y, GHOST_DIARY);
        ghost(graphics, ResearchTableBlockEntity.HEXTECH, ResearchTableMenu.HEXTECH_X, ResearchTableMenu.ROW_Y, GHOST_ORB);
        ghost(graphics, ResearchTableBlockEntity.PAPER, ResearchTableMenu.PAPER_X, ResearchTableMenu.ROW_Y, GHOST_PAPER);

        ResearchTableBlockEntity table = table();
        boolean wantsTopic = table != null && table.sheet().topic() != null;
        Requirement missing = wantsTopic ? missing() : missingDiary();
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

    /** A check mark on studied items, and slot hints. */
    private void renderSlotOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Slot slot = hoveredSlot;
        if (slot == null || slot.container != menu.table() || !menu.getCarried().isEmpty() || loupe.held()) {
            return;
        }
        int index = slot.getContainerSlot();
        if (slot.hasItem()) {
            return;
        }
        String hint = switch (index) {
            case ResearchTableBlockEntity.DIARY -> "diary";
            case ResearchTableBlockEntity.HEXTECH -> "hextech";
            case ResearchTableBlockEntity.PAPER -> "paper";
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

    // ---- Input ----------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        boolean doubleClick = false;
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        for (int i = 0; i < Tab.values().length; i++) {
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
        if (tab == Tab.DESK && button == InputConstants.MOUSE_BUTTON_LEFT && menu.getCarried().isEmpty() && loupe.grab(mouseX, mouseY)) {
            return true;
        }
        if (tab == Tab.DESK && overBoard(mouseX, mouseY)) {
            boardPressed(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    private void boardPressed(double mouseX, double mouseY, int button) {
        ResearchTableBlockEntity table = table();
        if (table == null || burning != null) {
            return;
        }
        int cell = board.cellAt(mouseX, mouseY);
        ResearchBoard current = currentBoard();
        if (current == null) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                setTab(Tab.BOOK);
                sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            }
            return;
        }
        syncThread(table, current);
        if (!done(table) && overBurnButton(mouseX, mouseY)) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                if (burnArmed()) {
                    burnArmed = 0;
                    release();
                    send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.BURN, 0));
                } else {
                    burnArmed = Util.getMillis();
                    sound(SoundEvents.FLINTANDSTEEL_USE, 1.2F, 0.5F);
                }
            }
            return;
        }
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            if (cell >= 0 && flaggable(current, cell)) {
                // A flag on a section the researcher suspects (only a mark: it guards against a careless step).
                BitSet flags = flags(current.topic());
                flags.flip(cell);
                sound(SoundEvents.BOOK_PAGE_TURN, flags.get(cell) ? 1.5F : 1.2F, 0.5F);
            } else if (tree.length > 0 && !done(table)) {
                // Take back the last step drawn.
                tree = Arrays.copyOf(tree, tree.length - 2);
                trace = ResearchSheet.trace(current, tree);
                head = lastCell();
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
                tree = new int[0];
                trace = ResearchSheet.trace(current, tree);
                head = HexGrid.CENTRE;
                strokeBase = 0;
                dragging = true;
            }
            return;
        }
        if (inked(cell)) {
            // A new line from any inked section (the orb too).
            head = cell;
            strokeBase = tree.length / 2;
            dragging = true;
            sound(ModSounds.BRUSH_STROKE.get(), 1.2F, 0.2F);
            return;
        }
        int from = inkedNeighbour(cell);
        if (from >= 0) {
            strokeBase = tree.length / 2;
            dragging = extend(current, from, cell) && trace != null && !trace.complete(current) && trace.burst() < 0;
        } else {
            board.shake(cell);
        }
    }

    @Override
    public boolean mouseDragged(double portX, double portY, int portButton, double dragX, double dragY) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        if (tab == Tab.BOOK && book.mouseDragged(event.x(), event.y(), event.button())) {
            return true;
        }
        if (loupe.held()) {
            loupe.drag(event.x(), event.y());
            return true;
        }
        if (tab == Tab.DESK && dragging) {
            ResearchBoard current = currentBoard();
            if (current != null) {
                follow(current, event.x(), event.y());
            }
            return true;
        }
        return super.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        if (tab == Tab.BOOK && book.mouseReleased(event.x(), event.y(), event.button())) {
            return true;
        }
        if (loupe.held()) {
            putLoupeBack();
            return true;
        }
        if (dragging) {
            release();
            return true;
        }
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean keyPressed(int portKey, int portScan, int portMods) {
        KeyEvent event = new KeyEvent(portKey, portScan, portMods);
        if (tab == Tab.BOOK && book.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.BOOK && book.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** The loupe hides the item tooltips it would cover while it is carried. */
    @Override
    protected void renderTooltip(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        if (!loupe.held()) {
            super.renderTooltip(graphics.vanilla(), mouseX, mouseY);
        }
    }

    private void putLoupeBack() {
        loupe.release(slot -> send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.LOUPE, slot)));
    }

    @Override
    public void removed() {
        if (dragging) {
            release();
        }
        putLoupeBack();
        super.removed();
    }
}
