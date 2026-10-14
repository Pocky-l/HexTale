package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.Requirement;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.screen.book.StarBook;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.network.ResearchActionPayload;
import com.pockyl.hextale.network.ResearchMessagePayload;
import com.pockyl.hextale.registry.ModItems;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * The Research Table, in three tabs: the star book (choose a rune), the parchment and the table itself (diary, orb,
 * research tools, the item to study). The parchment holds one of two games, both played right on its sections: learning
 * a rune (click a glowing section to write the next symbol with a studied item, click a covered point to leave its
 * resource) or studying an item by resonance (click string sections to place tuning marks, then listen).
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> implements StarBook.Host {
    private static final ResourceLocation BOOK_TEXTURE = Hextale.id("textures/gui/research_book.png");
    private static final ResourceLocation PARCHMENT_TEXTURE = Hextale.id("textures/gui/research_parchment.png");
    private static final ResourceLocation TABLE_TEXTURE = Hextale.id("textures/gui/research_table.png");
    private static final ResourceLocation WIDGETS = Hextale.id("textures/gui/research_table_widgets.png");
    private static final int WIDGETS_SIZE_X = 256;
    private static final int WIDGETS_SIZE_Y = 128;
    private static final String KEY = "gui.hextale.research_table.";
    private static final int TAB_WIDTH = 24;
    private static final int TAB_HEIGHT = 20;
    private static final int TEXT = 0xE8D9B5;
    private static final int TEXT_MUTED = 0xB8A27A;
    // Side panel of the parchment tab (wells come from the background texture).
    private static final int PANEL_X = 252;
    private static final int PANEL_W = 212;
    private static final int POINTS_Y = 88;
    private static final int BUTTONS_Y = 147;
    private static final int ITEMS_X = 260;
    private static final int ITEMS_Y = 169;
    private static final int ITEM_COLUMNS = 11;
    private static final int ITEM_ROWS = 3;
    private static final int MESSAGE_Y = 229;
    private static final int BOARD_X = 4;
    private static final int BOARD_Y = 24;
    private static final int BOARD_W = 244;
    private static final int BOARD_H = 234;

    private enum Tab {
        BOOK,
        PARCHMENT,
        TABLE
    }

    private record Lever(Item item, ItemElements elements, int count) {
    }

    /** A small brass button of the side panel. */
    private record PanelButton(int x, int y, int width, Component label, boolean active, Runnable action, @Nullable Component tooltip) {
    }

    private final BoardView board = new BoardView();
    private StarBook book;
    private Tab tab;
    private Component message = Component.empty();
    private ResearchMessagePayload.Tone tone = ResearchMessagePayload.Tone.INFO;
    private int leverScroll;
    private final List<Lever> levers = new ArrayList<>();
    private final List<PanelButton> buttons = new ArrayList<>();
    private Map<Integer, List<BoardView.Move>> moves = Map.of();
    /** Which of several items reaching the hovered section is shown (right click switches). */
    private int choice;
    private int choiceCell = -1;
    private int fogVersion = -1;
    @Nullable
    private ComplexRune fogRune;
    private int fogLens = -1;
    @Nullable
    private List<Component> pendingTooltip;
    private int lastMouseX;
    private int lastMouseY;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ResearchTableMenu.IMAGE_WIDTH;
        imageHeight = ResearchTableMenu.IMAGE_HEIGHT;
        inventoryLabelX = ResearchTableMenu.INVENTORY_X;
        inventoryLabelY = ResearchTableMenu.INVENTORY_Y - 11;
        titleLabelX = 8;
        titleLabelY = 7;
    }

    @Override
    protected void init() {
        super.init();
        if (book == null) {
            book = new StarBook(this);
            ResearchTableBlockEntity table = table();
            if (missingDiary() != null) {
                setTab(Tab.TABLE);
            } else {
                boolean subject = table != null && (table.isStudyingItem() ? table.study().item() != null : table.sheet().rune() != null);
                setTab(subject ? Tab.PARCHMENT : Tab.BOOK);
            }
        }
        book.layout(leftPos + 4, topPos + 24, imageWidth - 8, imageHeight - 28);
        board.layout(leftPos, topPos);
        setTab(tab);
    }

    private void setTab(Tab tab) {
        this.tab = tab;
        menu.setSlotsVisible(tab == Tab.TABLE);
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

    @Nullable
    private Requirement missing() {
        return minecraft == null || minecraft.player == null ? null : Requirement.firstMissing(menu.table(), minecraft.player);
    }

    private int toolLevel(ResearchTool tool) {
        return ResearchTableBlockEntity.toolLevel(menu.table(), tool, ClientKnowledge.get());
    }

    private static boolean studied(Item item) {
        return ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(item));
    }

    private void send(ResearchActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public void onMessage(Component text, ResearchMessagePayload.Tone tone) {
        this.message = text;
        this.tone = tone;
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
        if (missingDiary() != null) {
            onMessage(Component.translatable(missingDiary().translationKey()), ResearchMessagePayload.Tone.BAD);
            setTab(Tab.TABLE);
            return;
        }
        send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.SELECT, rune.index()));
        message = Component.empty();
        click();
        setTab(Tab.PARCHMENT);
    }

    // ---- Rendering ------------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        pendingTooltip = null;
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tab == Tab.BOOK) {
            List<FormattedCharSequence> tooltip = book.render(graphics, mouseX, mouseY);
            if (tooltip != null) {
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        } else if (tab == Tab.TABLE) {
            renderTooltip(graphics, mouseX, mouseY);
            renderSlotHints(graphics, mouseX, mouseY);
        }
        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : pendingTooltip) {
                lines.addAll(font.split(line, 220));
            }
            graphics.renderTooltip(font, lines, mouseX, mouseY);
        }
        renderTabTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = switch (tab) {
            case BOOK -> BOOK_TEXTURE;
            case PARCHMENT -> PARCHMENT_TEXTURE;
            case TABLE -> TABLE_TEXTURE;
        };
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        renderTabs(graphics, mouseX, mouseY);
        buttons.clear();
        if (tab == Tab.PARCHMENT) {
            renderParchment(graphics, mouseX, mouseY);
        } else if (tab == Tab.TABLE) {
            renderTableTab(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF000000 | TEXT, true);
        Component codex = Component.translatable(KEY + "codex", ClientKnowledge.get().runeCount(), 72);
        graphics.drawString(font, codex, imageWidth - 92 - font.width(codex), titleLabelY, 0xFF000000 | TEXT_MUTED, true);
        if (tab == Tab.TABLE) {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF000000 | TEXT, true);
            graphics.drawString(font, Component.translatable(KEY + "materials"), ResearchTableMenu.MATERIALS_X - 10, 39, 0xFF000000 | TEXT, true);
            graphics.drawString(font, Component.translatable(KEY + "tools"), 172, 93, 0xFF000000 | TEXT, true);
            graphics.drawString(font, Component.translatable(KEY + "study"), ResearchTableMenu.STUDY_X - 10, 93, 0xFF000000 | TEXT, true);
        }
    }

    private int tabX(int index) {
        return leftPos + imageWidth - 20 - (3 - index) * (TAB_WIDTH + 2);
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack[] icons = {new ItemStack(ModItems.RESEARCHERS_DIARY.get()), new ItemStack(Items.PAPER),
                new ItemStack(ModItems.RESEARCH_TABLE.get())};
        for (int i = 0; i < 3; i++) {
            int x = tabX(i);
            int y = topPos + 1;
            boolean selected = tab.ordinal() == i;
            boolean hovered = isOver(mouseX, mouseY, x, y, TAB_WIDTH, TAB_HEIGHT);
            graphics.blit(WIDGETS, x, y, (selected ? 2 : hovered ? 1 : 0) * TAB_WIDTH, 0, TAB_WIDTH, TAB_HEIGHT, WIDGETS_SIZE_X, WIDGETS_SIZE_Y);
            graphics.renderItem(icons[i], x + 4, y + 2);
        }
    }

    private void renderTabTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        String[] keys = {"tab.book", "tab.parchment", "tab.table"};
        for (int i = 0; i < 3; i++) {
            if (isOver(mouseX, mouseY, tabX(i), topPos + 1, TAB_WIDTH, TAB_HEIGHT)) {
                graphics.renderTooltip(font, Component.translatable(KEY + keys[i]), mouseX, mouseY);
            }
        }
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean overBoard(double mouseX, double mouseY) {
        return isOver(mouseX, mouseY, leftPos + BOARD_X, topPos + BOARD_Y, BOARD_W, BOARD_H);
    }

    // ---- The table tab --------------------------------------------------------------------------------------------

    private void renderTableTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int[] materials = {ResearchTableBlockEntity.DIARY, ResearchTableBlockEntity.HEXTECH};
        int[] ghosts = {0, 2};
        Requirement missing = missing();
        for (int i = 0; i < materials.length; i++) {
            int x = leftPos + ResearchTableMenu.MATERIALS_X + i * ResearchTableMenu.SLOT_SPACING;
            int y = topPos + ResearchTableMenu.MATERIALS_Y;
            if (menu.table().getItem(materials[i]).isEmpty()) {
                graphics.blit(WIDGETS, x, y, ghosts[i] * 16, 24, 16, 16, WIDGETS_SIZE_X, WIDGETS_SIZE_Y);
            }
            boolean wanted = missing != null && (missing == Requirement.HEXTECH ? i == 1 : i == 0);
            if (wanted) {
                highlight(graphics, x, y);
            }
        }
        for (ResearchTool tool : ResearchTool.values()) {
            int x = leftPos + ResearchTableMenu.TOOLS_X + tool.ordinal() * ResearchTableMenu.SLOT_SPACING;
            int y = topPos + ResearchTableMenu.TOOLS_Y;
            if (menu.table().getItem(ResearchTableBlockEntity.TOOLS + tool.ordinal()).isEmpty()) {
                graphics.blit(WIDGETS, x, y, (5 + tool.ordinal()) * 16, 24, 16, 16, WIDGETS_SIZE_X, WIDGETS_SIZE_Y);
            } else {
                int level = toolLevel(tool);
                for (int s = 0; s < 3; s++) {
                    graphics.fill(x + 2 + s * 5, y + 19, x + 5 + s * 5, y + 21, s < level ? 0xFFE0A21C : 0xFF3A2712);
                }
            }
        }
        ItemStack study = menu.table().getItem(ResearchTableBlockEntity.STUDY);
        if (!study.isEmpty() && studied(study.getItem())) {
            graphics.drawString(font, "✔", leftPos + ResearchTableMenu.STUDY_X + 11, topPos + ResearchTableMenu.TOOLS_Y - 3, 0xFF8FE07A, true);
        }
        PanelButton button = studyButton();
        buttons.add(button);
        renderButton(graphics, button, mouseX, mouseY);
    }

    /** The button next to the study slot. */
    private PanelButton studyButton() {
        ItemStack stack = menu.table().getItem(ResearchTableBlockEntity.STUDY);
        boolean known = !stack.isEmpty() && studied(stack.getItem());
        Component tooltip = stack.isEmpty() ? Component.translatable(KEY + "study.button.empty")
                : known ? Component.translatable(KEY + "study.button.known") : Component.translatable(KEY + "study.button.tooltip");
        return new PanelButton(leftPos + ResearchTableMenu.STUDY_X + 22, topPos + ResearchTableMenu.TOOLS_Y, 64,
                Component.translatable(KEY + "study.button"), !stack.isEmpty() && !known, this::startStudy, tooltip);
    }

    private void startStudy() {
        send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.STUDY_ITEM, 0));
        message = Component.empty();
        setTab(Tab.PARCHMENT);
    }

    /** A pulsing brass frame around a slot that needs something. */
    private static void highlight(GuiGraphics graphics, int x, int y) {
        float pulse = 0.5F + 0.5F * Mth.sin(Util.getMillis() / 160.0F);
        int color = PixelArt.argb(0xFFD36E, 0.45F + 0.5F * pulse);
        graphics.fill(x - 2, y - 2, x + 18, y - 1, color);
        graphics.fill(x - 2, y + 17, x + 18, y + 18, color);
        graphics.fill(x - 2, y - 1, x - 1, y + 17, color);
        graphics.fill(x + 17, y - 1, x + 18, y + 17, color);
    }

    private void renderSlotHints(GuiGraphics graphics, int mouseX, int mouseY) {
        Slot slot = hoveredSlot;
        if (slot == null || slot.container != menu.table() || !menu.getCarried().isEmpty()) {
            return;
        }
        int index = slot.getContainerSlot();
        if (index == ResearchTableBlockEntity.STUDY) {
            if (!slot.hasItem()) {
                graphics.renderTooltip(font, font.split(Component.translatable(KEY + "slot.study"), 180), mouseX, mouseY);
            }
            return;
        }
        if (index >= ResearchTableBlockEntity.TOOLS) {
            ResearchTool tool = ResearchTool.values()[index - ResearchTableBlockEntity.TOOLS];
            int level = toolLevel(tool);
            List<Component> lines = new ArrayList<>();
            lines.add(tool.item().getDescription().copy().withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable(KEY + "slot.tool", Component.translatable(tool.rune().translationKey())
                    .withColor(tool.rune().color())).withStyle(ChatFormatting.GRAY));
            if (slot.hasItem()) {
                lines.add(level > 0 ? Component.literal("★".repeat(level) + " ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(tool.translationKey() + ".level." + level).withStyle(ChatFormatting.WHITE))
                        : Component.translatable(KEY + "slot.tool.no_rune").withStyle(ChatFormatting.RED));
            }
            if (!slot.hasItem() || level > 0) {
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
            return;
        }
        if (slot.hasItem()) {
            return;
        }
        String hint = switch (index) {
            case ResearchTableBlockEntity.DIARY -> "diary";
            case ResearchTableBlockEntity.HEXTECH -> "hextech";
            default -> "output";
        };
        graphics.renderTooltip(font, font.split(Component.translatable(KEY + "slot." + hint), 180), mouseX, mouseY);
    }

    // ---- The parchment tab ----------------------------------------------------------------------------------------

    private void renderParchment(GuiGraphics graphics, int mouseX, int mouseY) {
        ResearchTableBlockEntity table = table();
        if (table != null && table.isStudyingItem() && table.study().item() != null) {
            renderStudy(graphics, table, mouseX, mouseY);
            return;
        }
        ComplexRune rune = table == null ? null : table.sheet().rune();
        RuneBoard current = rune == null ? null : RuneBoards.request(rune).getNow(null);
        collectLevers();
        if (current == null) {
            Component text = Component.translatable(rune == null ? KEY + "no_rune" : KEY + "preparing");
            List<FormattedCharSequence> lines = font.split(text, 190);
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawCenteredString(font, lines.get(i), leftPos + BoardView.CX, topPos + BoardView.CY - 10 + i * 10, 0xFF3B2A1A);
            }
            board.renderEmpty(graphics);
            graphics.drawString(font, Component.translatable(KEY + "goal.none"), leftPos + PANEL_X + 6, topPos + 28, 0xFF000000 | TEXT_MUTED, false);
            renderLevers(graphics, mouseX, mouseY);
            renderMessage(graphics, message);
            return;
        }
        ResearchSheet sheet = table.sheet();
        RuneKnowledge knowledge = ClientKnowledge.get();
        int lens = toolLevel(ResearchTool.LENS);
        if (fogVersion != ClientKnowledge.version() || fogRune != rune || fogLens != lens) {
            board.updateFog(knowledge.seen(rune), lens);
            fogVersion = ClientKnowledge.version();
            fogRune = rune;
            fogLens = lens;
        }
        board.observe(current, sheet, null);
        moves = computeMoves(current, sheet);
        int cell = overBoard(mouseX, mouseY) ? board.cellAt(mouseX, mouseY) : -1;
        if (cell != choiceCell) {
            choiceCell = cell;
            choice = 0;
        }
        BoardView.Move chosen = chosenMove(cell);
        int fork = toolLevel(ResearchTool.FORK);
        board.renderRune(graphics, font, current, sheet, moves, cell, chosen, choice, fork);
        if (cell >= 0 && chosen == null) {
            pendingTooltip = board.describe(current, sheet, cell);
        }
        renderRunePanel(graphics, current, sheet, mouseX, mouseY);
    }

    @Nullable
    private BoardView.Move chosenMove(int cell) {
        List<BoardView.Move> options = moves.get(cell);
        return options == null || options.isEmpty() ? null : options.get(choice % options.size());
    }

    /** Studied items in the inventory, one entry per item, simple ones first. */
    private void collectLevers() {
        levers.clear();
        if (minecraft.player == null) {
            return;
        }
        Map<Item, Integer> counts = new LinkedHashMap<>();
        Inventory inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && studied(stack.getItem()) && ResearchItems.of(stack) != null) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        counts.forEach((item, count) -> levers.add(new Lever(item, ResearchItems.of(item), count)));
        levers.sort((a, b) -> a.elements().tier() != b.elements().tier() ? a.elements().tier() - b.elements().tier()
                : b.count() - a.count());
        int pages = Math.max(0, (levers.size() - 1) / ITEM_COLUMNS - ITEM_ROWS + 1);
        leverScroll = Mth.clamp(leverScroll, 0, pages);
    }

    /** Every section the next symbol can reach with a studied item from the inventory, plain or ground in the mortar. */
    private Map<Integer, List<BoardView.Move>> computeMoves(RuneBoard current, ResearchSheet sheet) {
        Map<Integer, List<BoardView.Move>> out = new LinkedHashMap<>();
        if (missingDiary() != null || sheet.isOver() || (!sheet.isOpen() && missing() != null)) {
            return out;
        }
        ResearchSheet from = sheet;
        if (!sheet.isOpen()) {
            from = new ResearchSheet();
            from.begin(current.par().sheet() + toolLevel(ResearchTool.STRETCHER));
        }
        if (!from.canWrite()) {
            return out;
        }
        int mortar = toolLevel(ResearchTool.MORTAR);
        for (Lever lever : levers) {
            for (boolean ground : new boolean[]{false, true}) {
                if (ground && mortar < lever.elements().tier()) {
                    continue;
                }
                HexGrid.Walk walk = from.preview(current, lever.elements(), ground);
                if (walk.end() == from.from()) {
                    continue;
                }
                out.computeIfAbsent(walk.end(), k -> new ArrayList<>()).add(new BoardView.Move(lever.item(), lever.elements(), ground, walk));
            }
        }
        return out;
    }

    private void renderRunePanel(GuiGraphics graphics, RuneBoard current, ResearchSheet sheet, int mouseX, int mouseY) {
        ComplexRune rune = current.rune();
        RuneKnowledge knowledge = ClientKnowledge.get();
        int x0 = leftPos + PANEL_X;
        int y0 = topPos;
        graphics.fill(x0 + 4, y0 + 27, x0 + 34, y0 + 57, PixelArt.argb(PixelArt.mix(rune.color(), 0x1C130B, 0.75F), 1));
        PixelArt.glyph(graphics, rune, x0 + 6, y0 + 29, 26, PixelArt.argb(rune.color(), 1), 0xFF000000);
        graphics.drawString(font, Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD), x0 + 40, y0 + 28,
                0xFF000000 | rune.color(), true);
        graphics.drawString(font, Component.translatable("rune.hextale.formula", Component.translatable(rune.action().translationKey()),
                Component.translatable(rune.target().translationKey())), x0 + 40, y0 + 38, 0xFF000000 | TEXT_MUTED, false);
        graphics.drawString(font, starsText(knowledge.stars(rune)), x0 + 40, y0 + 48, 0xFFFFFFFF, false);

        RuneBoard.Par par = current.par();
        int cap = sheet.isOpen() ? sheet.cap() : par.sheet() + toolLevel(ResearchTool.STRETCHER);
        int used = sheet.isOpen() ? sheet.used() : 0;
        int step = cap > 40 ? 4 : 5;
        for (int i = 0; i < cap; i++) {
            int color = i < par.three() ? 0xD6AB52 : i < par.two() ? 0xB8C4CC : 0x6E4D2A;
            int px = x0 + 6 + i * step;
            int py = y0 + 65;
            graphics.fill(px, py, px + step - 1, py + 7, PixelArt.argb(i < used ? 0xF0E3C2 : PixelArt.mix(color, 0x1C130B, 0.45F), 1));
            graphics.fill(px, py + 7, px + step - 1, py + 8, PixelArt.argb(color, 1));
        }
        MutableComponent meter = Component.translatable(KEY + "meter", used, cap).withColor(TEXT).append("  ")
                .append(Component.literal("★★★≤" + par.three()).withStyle(ChatFormatting.GOLD)).append(" ")
                .append(Component.literal("★★≤" + par.two()).withColor(0xC8D2DA));
        graphics.drawString(font, meter, x0 + 6, y0 + 75, 0xFFFFFFFF, false);

        for (int i = 0; i < current.points().size(); i++) {
            RuneBoard.Point point = current.points().get(i);
            int px = x0 + 6 + i % 2 * 104;
            int py = y0 + POINTS_Y + i / 2 * 19;
            boolean filled = sheet.isOpen() && sheet.isFilled(i);
            boolean ready = sheet.isActive() && sheet.ready(current, i);
            boolean known = studied(point.resource());
            PixelArt.radical(graphics, point.element(), px, py + 4, 9, PixelArt.argb(point.element().color(), filled ? 0.5F : 1));
            graphics.renderItem(new ItemStack(point.resource()), px + 12, py);
            int have = count(point.resource());
            graphics.drawString(font, String.valueOf(have), px + 30, py + 5, have > 0 ? 0xFFE8D9B5 : 0xFFE06A5A, false);
            Component status = filled ? Component.literal("✔").withColor(0x8FE07A)
                    : !known ? Component.translatable(KEY + "point.unstudied").withColor(0xE06A5A)
                    : ready ? Component.translatable(KEY + "point.ready").withColor(0xFFD36E)
                    : Component.translatable(KEY + "point.open").withColor(TEXT_MUTED);
            graphics.drawString(font, status, px + 46, py + 5, 0xFFFFFFFF, false);
            if (isOver(mouseX, mouseY, px, py, 100, 17)) {
                pendingTooltip = List.of(Component.translatable(KEY + "point", ResearchText.element(point.element())),
                        point.resource().getDescription().copy().withStyle(ChatFormatting.GRAY),
                        Component.translatable(!known ? KEY + "point.unstudied.tooltip" : ready ? KEY + "point.ready.tooltip"
                                : KEY + "point.open.tooltip").withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        buttons.add(new PanelButton(x0 + 6, y0 + BUTTONS_Y, 80, Component.translatable(KEY + "sheet.new"), sheet.isOpen(),
                () -> send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.NEW_SHEET, 0)),
                Component.translatable(KEY + "sheet.tooltip")));
        int scraper = toolLevel(ResearchTool.SCRAPER);
        if (scraper > 0) {
            int left = Math.max(0, scraper - sheet.scrapes());
            buttons.add(new PanelButton(x0 + 90, y0 + BUTTONS_Y, 58, Component.translatable(KEY + "scrape", left),
                    sheet.isActive() && left > 0 && !sheet.marks().isEmpty(),
                    () -> send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.SCRAPE, 0)),
                    Component.translatable(KEY + "scrape.tooltip")));
        }
        for (PanelButton button : buttons) {
            renderButton(graphics, button, mouseX, mouseY);
        }
        renderLevers(graphics, mouseX, mouseY);
        renderMessage(graphics, runeMessage(sheet));
    }

    private Component runeMessage(ResearchSheet sheet) {
        Requirement missing = missing();
        if (missing != null && !sheet.isActive()) {
            return Component.translatable(missing.translationKey());
        }
        if (!message.getString().isEmpty()) {
            return message;
        }
        return levers.isEmpty() ? Component.translatable(KEY + "hint.no_levers") : Component.empty();
    }

    /** The studied items in the inventory, for reference: hover one to see its elements. */
    private void renderLevers(GuiGraphics graphics, int mouseX, int mouseY) {
        for (int i = 0; i < ITEM_COLUMNS * ITEM_ROWS; i++) {
            int index = leverScroll * ITEM_COLUMNS + i;
            if (index >= levers.size()) {
                break;
            }
            Lever lever = levers.get(index);
            int x = leftPos + ITEMS_X + i % ITEM_COLUMNS * 18;
            int y = topPos + ITEMS_Y + i / ITEM_COLUMNS * 18;
            ItemStack stack = new ItemStack(lever.item(), lever.count());
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
            if (isOver(mouseX, mouseY, x - 1, y - 1, 18, 18)) {
                graphics.fill(x, y, x + 16, y + 16, 0x60FFFFFF);
                pendingTooltip = List.of(lever.item().getDescription(), ResearchText.elements(lever.elements()),
                        Component.translatable(KEY + "item.tier." + lever.elements().tier()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (levers.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(Component.translatable(KEY + "levers.none"), 196);
            for (int i = 0; i < lines.size() && i < 5; i++) {
                graphics.drawString(font, lines.get(i), leftPos + PANEL_X + 8, topPos + ITEMS_Y + 1 + i * 10, 0xFF000000 | TEXT_MUTED, false);
            }
        }
        int pages = Math.max(0, (levers.size() - 1) / ITEM_COLUMNS - ITEM_ROWS + 1);
        if (pages > 0) {
            Component label = Component.literal((leverScroll + 1) + "/" + (pages + 1));
            graphics.drawString(font, label, leftPos + PANEL_X + PANEL_W - 6 - font.width(label), topPos + BUTTONS_Y + 4,
                    0xFF000000 | TEXT_MUTED, true);
        }
    }

    // ---- The item study -------------------------------------------------------------------------------------------

    private void renderStudy(GuiGraphics graphics, ResearchTableBlockEntity table, int mouseX, int mouseY) {
        ItemStudy study = table.study();
        Item item = study.item();
        ItemElements elements = ResearchItems.of(item);
        if (elements == null) {
            return;
        }
        board.observe(null, null, study);
        int cell = overBoard(mouseX, mouseY) ? board.cellAt(mouseX, mouseY) : -1;
        board.renderStudy(graphics, item, elements, study, cell);
        if (cell >= 0) {
            List<Component> lines = board.describeStudy(study, cell);
            if (!lines.isEmpty()) {
                pendingTooltip = lines;
            }
        }

        int x0 = leftPos + PANEL_X;
        int y0 = topPos;
        ItemStack stack = new ItemStack(item);
        graphics.fill(x0 + 4, y0 + 27, x0 + 34, y0 + 57, 0xFF1C2A3A);
        graphics.pose().pushPose();
        graphics.pose().translate(x0 + 7, y0 + 30, 0);
        graphics.pose().scale(1.5F, 1.5F, 1);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
        graphics.drawString(font, stack.getHoverName().copy().withStyle(ChatFormatting.BOLD), x0 + 40, y0 + 28, 0xFFE8D9B5, true);
        graphics.drawString(font, Component.translatable(KEY + "study.goal"), x0 + 40, y0 + 38, 0xFF000000 | TEXT_MUTED, false);
        int found = 0;
        for (SimpleRune element : elements.elements().keySet()) {
            if (study.isRevealed(element)) {
                found++;
            }
        }
        graphics.drawString(font, Component.translatable(KEY + "study.found", found, elements.elements().size()), x0 + 40, y0 + 48,
                0xFF9FE3FF, false);

        // Guesses: brass tokens for the ones left.
        int total = study.maxGuesses();
        for (int i = 0; i < total; i++) {
            boolean left = i >= study.guesses();
            PixelArt.hex(graphics, x0 + 11 + i * 12, y0 + 69, 9, PixelArt.argb(left ? 0xD6AB52 : 0x2A2018, 1),
                    PixelArt.argb(left ? 0x6E4A16 : 0x4A3A28, 1));
        }
        graphics.drawString(font, Component.translatable(KEY + "study.guesses", study.guessesLeft(), total), x0 + 6, y0 + 76,
                0xFF000000 | TEXT, false);

        // The hidden elements: revealed ones by sign and strength.
        int i = 0;
        for (Map.Entry<SimpleRune, Integer> entry : elements.elements().entrySet()) {
            int px = x0 + 6 + i % 2 * 104;
            int py = y0 + POINTS_Y + i / 2 * 19;
            if (study.isRevealed(entry.getKey())) {
                PixelArt.radical(graphics, entry.getKey(), px, py + 4, 9, PixelArt.argb(entry.getKey().color(), 1));
                graphics.drawString(font, ResearchText.element(entry.getKey()).append(" " + entry.getValue()), px + 13, py + 5, 0xFFFFFFFF, false);
            } else {
                PixelArt.hex(graphics, px + 4, py + 8, 11, PixelArt.argb(0x2A2240, 1), PixelArt.argb(0x8A7AB8, 1));
                graphics.drawString(font, "?", px + 2, py + 4, 0xFFD8CCF8, false);
                graphics.drawString(font, Component.translatable(KEY + "study.hidden"), px + 13, py + 5, 0xFF8A7AB8, false);
            }
            i++;
        }

        if (!study.isActive(elements)) {
            ItemStack slot = menu.table().getItem(ResearchTableBlockEntity.STUDY);
            boolean again = !slot.isEmpty() && !studied(slot.getItem());
            buttons.add(new PanelButton(x0 + 6, y0 + BUTTONS_Y, 90, Component.translatable(KEY + "study.again"), again,
                    this::startStudy, Component.translatable(again ? KEY + "study.button.tooltip" : KEY + "study.button.empty")));
        }
        for (PanelButton button : buttons) {
            renderButton(graphics, button, mouseX, mouseY);
        }

        // A small legend: what the marks and strings say, over the (unused) slot grid of the well.
        graphics.fill(x0 + 5, topPos + ITEMS_Y - 2, x0 + PANEL_W - 5, topPos + ITEMS_Y + 54, 0xFF2A1B0C);
        int lx = x0 + 10;
        int ly = topPos + ITEMS_Y;
        PixelArt.hex(graphics, lx, ly + 4, 11, PixelArt.argb(0xD6AB52, 1), PixelArt.argb(0x6E4A16, 1));
        graphics.drawString(font, "›", lx - 1, ly, 0xFF3B2208, false);
        graphics.drawString(font, Component.translatable(KEY + "study.legend.hum"), lx + 10, ly, 0xFF000000 | TEXT, false);
        PixelArt.hex(graphics, lx, ly + 18, 11, PixelArt.argb(0x8A8078, 1), PixelArt.argb(0x4A4440, 1));
        graphics.drawString(font, "×", lx - 2, ly + 14, 0xFF2A2420, false);
        graphics.drawString(font, Component.translatable(KEY + "study.legend.silent"), lx + 10, ly + 14, 0xFF000000 | TEXT, false);
        PixelArt.hexRing(graphics, lx, ly + 32, 11, PixelArt.argb(0x9FE3FF, 0.6F + 0.4F * Mth.sin(Util.getMillis() / 180.0F)));
        graphics.drawString(font, Component.translatable(KEY + "study.legend.shimmer"), lx + 10, ly + 28, 0xFF000000 | TEXT, false);
        PixelArt.hex(graphics, lx, ly + 46, 11, PixelArt.argb(0xFFE08A, 1), PixelArt.argb(0xC88A12, 1));
        graphics.drawString(font, Component.translatable(KEY + "study.legend.exact"), lx + 10, ly + 42, 0xFF000000 | TEXT, false);

        renderMessage(graphics, message);
    }

    // ---- Shared panel parts ---------------------------------------------------------------------------------------

    private void renderMessage(GuiGraphics graphics, Component text) {
        int color = text != message ? TEXT : switch (tone) {
            case GOOD -> 0x9FE3FF;
            case BAD -> 0xF3A59B;
            case WIN -> 0xFFD36E;
            default -> TEXT;
        };
        List<FormattedCharSequence> lines = font.split(text, PANEL_W - 10);
        int max = 3;
        int start = Math.max(0, lines.size() - max);
        for (int i = start; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), leftPos + PANEL_X + 5, topPos + MESSAGE_Y + (i - start) * 9, 0xFF000000 | color, false);
        }
        if (lines.size() > max && isOver(lastMouseX, lastMouseY, leftPos + PANEL_X, topPos + MESSAGE_Y - 3, PANEL_W, 32)) {
            pendingTooltip = List.of(text);
        }
    }

    private void renderButton(GuiGraphics graphics, PanelButton button, int mouseX, int mouseY) {
        int x = button.x();
        int y = button.y();
        boolean hovered = isOver(mouseX, mouseY, x, y, button.width(), 16);
        int v = 44 + (button.active() ? hovered ? 16 : 0 : 32);
        graphics.blit(WIDGETS, x, y, 0, v, button.width() - 2, 16, WIDGETS_SIZE_X, WIDGETS_SIZE_Y);
        graphics.blit(WIDGETS, x + button.width() - 2, y, 62, v, 2, 16, WIDGETS_SIZE_X, WIDGETS_SIZE_Y);
        graphics.drawCenteredString(font, button.label(), x + button.width() / 2, y + 4, button.active() ? 0xFF2A1B0C : 0xFF5E4C34);
        if (hovered && button.tooltip() != null) {
            pendingTooltip = List.of(button.tooltip());
        }
    }

    private int count(Item item) {
        if (minecraft.player == null) {
            return 0;
        }
        int total = 0;
        Inventory inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                total += inventory.getItem(i).getCount();
            }
        }
        return total;
    }

    private static Component starsText(int count) {
        return Component.literal("★".repeat(count)).withStyle(ChatFormatting.GOLD)
                .append(Component.literal("★".repeat(3 - count)).withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---- Input ----------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < 3; i++) {
            if (button == 0 && isOver(mouseX, mouseY, tabX(i), topPos + 1, TAB_WIDTH, TAB_HEIGHT)) {
                if (tab.ordinal() != i) {
                    setTab(Tab.values()[i]);
                    click();
                }
                return true;
            }
        }
        if (tab == Tab.BOOK && book.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (tab != Tab.BOOK && button == 0) {
            for (PanelButton panelButton : buttons) {
                if (isOver(mouseX, mouseY, panelButton.x(), panelButton.y(), panelButton.width(), 16)) {
                    if (panelButton.active()) {
                        panelButton.action().run();
                        click();
                    }
                    return true;
                }
            }
        }
        if (tab == Tab.PARCHMENT && overBoard(mouseX, mouseY)) {
            boardClicked(mouseX, mouseY, button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** A click on the parchment: write, leave, fork, or place a tuning mark. */
    private void boardClicked(double mouseX, double mouseY, int button) {
        ResearchTableBlockEntity table = table();
        if (table == null) {
            return;
        }
        int cell = board.cellAt(mouseX, mouseY);
        if (table.isStudyingItem()) {
            ItemStudy study = table.study();
            ItemElements elements = study.item() == null ? null : ResearchItems.of(study.item());
            if (button == 0 && cell >= 0 && elements != null && study.canGuess(cell, elements)) {
                send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.GUESS, cell));
                message = Component.empty();
            }
            return;
        }
        ComplexRune rune = table.sheet().rune();
        RuneBoard current = rune == null ? null : RuneBoards.request(rune).getNow(null);
        if (current == null || cell < 0) {
            return;
        }
        ResearchSheet sheet = table.sheet();
        if (button == 1) {
            // Right click: the next of several items that reach this section.
            List<BoardView.Move> options = moves.get(cell);
            if (options != null && options.size() > 1) {
                choice = (choice + 1) % options.size();
                click();
            }
            return;
        }
        if (button != 0) {
            return;
        }
        if (sheet.isOver()) {
            send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.NEW_SHEET, 0));
            message = Component.empty();
            click();
            return;
        }
        int point = current.pointAt(cell);
        int fork = toolLevel(ResearchTool.FORK);
        boolean canJump = fork > 0 && sheet.canJump(current, cell, fork);
        if (point >= 0 && sheet.isActive() && sheet.ready(current, point)) {
            send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.LEAVE, point));
            return;
        }
        if (canJump && Screen.hasShiftDown()) {
            send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.JUMP, cell));
            click();
            return;
        }
        BoardView.Move move = chosenMove(cell);
        if (move != null) {
            send(ResearchActionPayload.write(menu.pos(), move.item(), move.ground(), cell));
            message = Component.empty();
            return;
        }
        if (canJump) {
            send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.JUMP, cell));
            click();
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (tab == Tab.BOOK && book.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (tab == Tab.BOOK && book.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.BOOK && book.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (tab == Tab.PARCHMENT && overBoard(mouseX, mouseY) && scrollY != 0) {
            List<BoardView.Move> options = moves.get(board.cellAt(mouseX, mouseY));
            if (options != null && options.size() > 1) {
                choice = Math.floorMod(choice - (int) Math.signum(scrollY), options.size());
                click();
            }
            return true;
        }
        if (tab == Tab.PARCHMENT && isOver(mouseX, mouseY, leftPos + PANEL_X, topPos + ITEMS_Y - 3, PANEL_W, 58)) {
            leverScroll = Math.max(0, leverScroll - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
