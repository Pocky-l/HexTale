package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.ResearchTool;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.research.RuneBoards;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * The Research Table, in three tabs: the research book (choose a rune), the parchment (the mini-game: items from the
 * inventory write symbols, resources fill the points) and the table itself (materials, research tools, the scroll).
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

    private enum Tab {
        BOOK,
        PARCHMENT,
        TABLE
    }

    private record ItemEntry(Item item, ItemElements elements, int count) {
    }

    /** A small brass button of the side panel. */
    private record PanelButton(int x, int y, int width, Component label, boolean active, Runnable action, @Nullable Component tooltip) {
    }

    private final BoardView board = new BoardView();
    private StarBook book;
    private Tab tab;
    private Component message = Component.empty();
    private ResearchMessagePayload.Tone tone = ResearchMessagePayload.Tone.INFO;
    private boolean grind;
    private int itemScroll;
    private final List<ItemEntry> items = new ArrayList<>();
    private final List<PanelButton> buttons = new ArrayList<>();
    private int fogVersion = -1;
    @Nullable
    private ComplexRune fogRune;
    private int fogLens = -1;

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
                setTab(table != null && table.sheet().rune() != null ? Tab.PARCHMENT : Tab.BOOK);
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

    private int toolLevel(ResearchTool tool) {
        return ResearchTableBlockEntity.toolLevel(menu.table(), tool, ClientKnowledge.get());
    }

    @Nullable
    private RuneBoard currentBoard() {
        ResearchTableBlockEntity table = table();
        ComplexRune rune = table == null ? null : table.sheet().rune();
        return rune == null ? null : RuneBoards.request(rune).getNow(null);
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

    // ---- StarBook.Host -------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public ComplexRune studying() {
        ResearchTableBlockEntity table = table();
        return table == null ? null : table.sheet().rune();
    }

    @Override
    public void study(ComplexRune rune) {
        if (missingDiary() != null) {
            onMessage(Component.translatable(missingDiary().translationKey()), ResearchMessagePayload.Tone.BAD);
            setTab(Tab.TABLE);
            return;
        }
        send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.SELECT, rune.index()));
        click();
        setTab(Tab.PARCHMENT);
    }

    // ---- Rendering ------------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tab == Tab.BOOK) {
            List<FormattedCharSequence> tooltip = book.render(graphics, mouseX, mouseY);
            if (tooltip != null) {
                graphics.renderTooltip(font, tooltip, mouseX, mouseY);
            }
        } else if (tab == Tab.PARCHMENT) {
            renderParchmentTooltips(graphics, mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
            renderSlotHints(graphics, mouseX, mouseY);
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
        if (tab == Tab.PARCHMENT) {
            renderParchment(graphics, mouseX, mouseY);
        } else if (tab == Tab.TABLE) {
            renderTableTab(graphics);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF000000 | TEXT, true);
        Component codex = Component.translatable(KEY + "codex", ClientKnowledge.get().runeCount(), 72);
        graphics.drawString(font, codex, imageWidth - 92 - font.width(codex), titleLabelY, 0xFF000000 | TEXT_MUTED, true);
        if (tab == Tab.TABLE) {
            graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF000000 | TEXT, true);
            graphics.drawString(font, Component.translatable(KEY + "materials"), 144, 39, 0xFF000000 | TEXT, true);
            graphics.drawString(font, Component.translatable(KEY + "tools"), 172, 93, 0xFF000000 | TEXT, true);
        }
    }

    private int tabX(int index) {
        return leftPos + imageWidth - 8 - (3 - index) * (TAB_WIDTH + 2);
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

    // ---- The table tab --------------------------------------------------------------------------------------------

    private static final int[] GHOSTS = {0, 1, 2, 3, 4};

    private void renderTableTab(GuiGraphics graphics) {
        int[] materials = {ResearchTableBlockEntity.DIARY, ResearchTableBlockEntity.PAPER, ResearchTableBlockEntity.HEXTECH,
                ResearchTableBlockEntity.INK, ResearchTableBlockEntity.BRUSH};
        Requirement missing = minecraft.player == null ? null : Requirement.firstMissing(menu.table(), minecraft.player);
        for (int i = 0; i < materials.length; i++) {
            int x = leftPos + ResearchTableMenu.MATERIALS_X + i * ResearchTableMenu.SLOT_SPACING;
            int y = topPos + ResearchTableMenu.MATERIALS_Y;
            if (menu.table().getItem(materials[i]).isEmpty()) {
                graphics.blit(WIDGETS, x, y, GHOSTS[i] * 16, 24, 16, 16, WIDGETS_SIZE_X, WIDGETS_SIZE_Y);
            }
            if (missing != null && slotOf(missing) == materials[i]) {
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
    }

    private static int slotOf(Requirement requirement) {
        return switch (requirement) {
            case DIARY, OWN_DIARY -> ResearchTableBlockEntity.DIARY;
            case BRUSH -> ResearchTableBlockEntity.BRUSH;
            case INK -> ResearchTableBlockEntity.INK;
            case PAPER -> ResearchTableBlockEntity.PAPER;
            case HEXTECH -> ResearchTableBlockEntity.HEXTECH;
        };
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
        String[] hints = {"diary", "paper", "hextech", "ink", "brush", "output"};
        if (index < hints.length) {
            graphics.renderTooltip(font, font.split(Component.translatable(KEY + "slot." + hints[index]), 180), mouseX, mouseY);
        }
    }

    // ---- The parchment tab ----------------------------------------------------------------------------------------

    private void renderParchment(GuiGraphics graphics, int mouseX, int mouseY) {
        ResearchTableBlockEntity table = table();
        ComplexRune rune = table == null ? null : table.sheet().rune();
        RuneBoard current = currentBoard();
        collectItems();
        buttons.clear();
        if (rune == null || current == null || table == null) {
            Component text = Component.translatable(rune == null ? KEY + "no_rune" : KEY + "preparing");
            List<FormattedCharSequence> lines = font.split(text, 190);
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawCenteredString(font, lines.get(i), leftPos + BoardView.CX, topPos + BoardView.CY - 10 + i * 10, 0xFF3B2A1A);
            }
            renderPanelEmpty(graphics);
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
        board.observe(current, sheet);
        ItemEntry hovered = hoveredItem(mouseX, mouseY);
        BoardView.Preview preview = hovered == null ? null : new BoardView.Preview(hovered.elements(),
                knowledge.knowsItem(BuiltInRegistries.ITEM.getKey(hovered.item())), canGrind(hovered.elements()));
        int cell = isOver(mouseX, mouseY, leftPos + 4, topPos + 24, 244, 234) ? board.cellAt(mouseX, mouseY) : -1;
        board.render(graphics, font, current, sheet, preview, cell, toolLevel(ResearchTool.FORK));
        renderPanel(graphics, current, sheet, mouseX, mouseY);
    }

    private boolean canGrind(ItemElements elements) {
        return grind && toolLevel(ResearchTool.MORTAR) >= elements.tier();
    }

    /** Items in the inventory that can write on the parchment, one entry per item, simple ones first. */
    private void collectItems() {
        items.clear();
        if (minecraft.player == null) {
            return;
        }
        Map<Item, Integer> counts = new LinkedHashMap<>();
        Inventory inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (ResearchItems.of(stack) != null) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        counts.forEach((item, count) -> items.add(new ItemEntry(item, ResearchItems.of(item), count)));
        items.sort((a, b) -> a.elements().tier() != b.elements().tier() ? a.elements().tier() - b.elements().tier()
                : Integer.compare(order(a.item()), order(b.item())));
        int pages = Math.max(0, (items.size() - 1) / ITEM_COLUMNS - ITEM_ROWS + 1);
        itemScroll = Mth.clamp(itemScroll, 0, pages);
    }

    private static int order(Item item) {
        int i = 0;
        for (Item canonical : ResearchItems.canonical().keySet()) {
            if (canonical == item) {
                return i;
            }
            i++;
        }
        return Integer.MAX_VALUE;
    }

    @Nullable
    private ItemEntry hoveredItem(double mouseX, double mouseY) {
        for (int i = 0; i < ITEM_COLUMNS * ITEM_ROWS; i++) {
            int index = itemScroll * ITEM_COLUMNS + i;
            if (index >= items.size()) {
                break;
            }
            int x = leftPos + ITEMS_X + i % ITEM_COLUMNS * 18;
            int y = topPos + ITEMS_Y + i / ITEM_COLUMNS * 18;
            if (isOver(mouseX, mouseY, x - 1, y - 1, 18, 18)) {
                return items.get(index);
            }
        }
        return null;
    }

    private void renderPanelEmpty(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable(KEY + "goal.none"), leftPos + PANEL_X + 6, topPos + 28, 0xFF000000 | TEXT_MUTED, false);
        renderItems(graphics, null, null, -1, -1);
        renderMessage(graphics, message.getString().isEmpty() ? Component.translatable(KEY + "hint.book") : message);
    }

    private void renderPanel(GuiGraphics graphics, RuneBoard current, ResearchSheet sheet, int mouseX, int mouseY) {
        ComplexRune rune = current.rune();
        RuneKnowledge knowledge = ClientKnowledge.get();
        int x0 = leftPos + PANEL_X;
        int y0 = topPos;
        // Goal: the rune, its kind and stars.
        graphics.fill(x0 + 4, y0 + 27, x0 + 34, y0 + 57, PixelArt.argb(PixelArt.mix(rune.color(), 0x1C130B, 0.75F), 1));
        PixelArt.glyph(graphics, rune, x0 + 6, y0 + 29, 26, PixelArt.argb(rune.color(), 1), 0xFF000000);
        graphics.drawString(font, Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD), x0 + 40, y0 + 28,
                0xFF000000 | rune.color(), true);
        graphics.drawString(font, Component.translatable("rune.hextale.formula", Component.translatable(rune.action().translationKey()),
                Component.translatable(rune.target().translationKey())), x0 + 40, y0 + 38, 0xFF000000 | TEXT_MUTED, false);
        graphics.drawString(font, starsText(knowledge.stars(rune)), x0 + 40, y0 + 48, 0xFFFFFFFF, false);

        // Meter: one pip per symbol the sheet holds, tinted by the star they still allow.
        RuneBoard.Par par = current.par();
        int cap = sheet.isOpen() ? sheet.cap() : par.sheet() + toolLevel(ResearchTool.STRETCHER);
        int used = sheet.isOpen() ? sheet.used() : 0;
        int step = cap > 40 ? 4 : 5;
        for (int i = 0; i < cap; i++) {
            int color = i < par.three() ? 0xD6AB52 : i < par.two() ? 0xB8C4CC : 0x6E4D2A;
            int px = x0 + 6 + i * step;
            int py = y0 + 65;
            boolean done = i < used;
            graphics.fill(px, py, px + step - 1, py + 7, PixelArt.argb(done ? 0xF0E3C2 : PixelArt.mix(color, 0x1C130B, 0.45F), 1));
            graphics.fill(px, py + 7, px + step - 1, py + 8, PixelArt.argb(color, 1));
        }
        MutableComponent meter = Component.translatable(KEY + "meter", used, cap).withColor(TEXT).append("  ")
                .append(Component.literal("★★★≤" + par.three()).withStyle(ChatFormatting.GOLD)).append(" ")
                .append(Component.literal("★★≤" + par.two()).withColor(0xC8D2DA));
        graphics.drawString(font, meter, x0 + 6, y0 + 75, 0xFFFFFFFF, false);

        // Points: radical, resource and its button.
        for (int i = 0; i < current.points().size(); i++) {
            RuneBoard.Point point = current.points().get(i);
            int px = x0 + 6 + i % 2 * 104;
            int py = y0 + POINTS_Y + i / 2 * 19;
            boolean filled = sheet.isOpen() && sheet.isFilled(i);
            boolean ready = sheet.isActive() && sheet.ready(current, i);
            PixelArt.radical(graphics, point.element(), px, py + 4, 9, PixelArt.argb(point.element().color(), filled ? 0.5F : 1));
            ItemStack resource = new ItemStack(point.resource());
            graphics.renderItem(resource, px + 12, py);
            int have = count(point.resource());
            graphics.drawString(font, String.valueOf(have), px + 30, py + 5, have > 0 ? 0xFFE8D9B5 : 0xFFE06A5A, false);
            if (filled) {
                graphics.drawString(font, "✔", px + 50, py + 5, 0xFF8FE07A, false);
            } else {
                Component label = Component.translatable(KEY + "leave");
                buttons.add(new PanelButton(px + 46, py + 1, 50, label, ready && have > 0,
                        leaveAction(i), Component.translatable(ready ? KEY + "leave.tooltip" : KEY + "leave.cover",
                        point.resource().getDescription(), ResearchText.element(point.element()))));
            }
            if (isOver(mouseX, mouseY, px, py, 44, 17)) {
                pendingTooltip = List.of(Component.translatable(KEY + "point", ResearchText.element(point.element())),
                        point.resource().getDescription().copy().withStyle(ChatFormatting.GRAY));
            }
        }

        // Buttons: a new sheet, the scraper, the mortar.
        Requirement missing = minecraft.player == null ? null : Requirement.firstMissing(menu.table(), minecraft.player);
        Component sheetLabel = Component.translatable(sheet.isActive() ? KEY + "sheet.burn" : KEY + "sheet.take");
        buttons.add(new PanelButton(x0 + 6, y0 + BUTTONS_Y, 80, sheetLabel, missing == null,
                () -> send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.NEW_SHEET, 0)),
                missing != null ? Component.translatable(missing.translationKey()) : Component.translatable(KEY + "sheet.tooltip")));
        int scraper = toolLevel(ResearchTool.SCRAPER);
        if (scraper > 0) {
            int left = Math.max(0, scraper - sheet.scrapes());
            buttons.add(new PanelButton(x0 + 90, y0 + BUTTONS_Y, 58, Component.translatable(KEY + "scrape", left),
                    sheet.isActive() && left > 0 && !sheet.marks().isEmpty(),
                    () -> send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.SCRAPE, 0)),
                    Component.translatable(KEY + "scrape.tooltip")));
        }
        if (toolLevel(ResearchTool.MORTAR) > 0) {
            buttons.add(new PanelButton(x0 + 152, y0 + BUTTONS_Y, 54, Component.translatable(grind ? KEY + "grind.on" : KEY + "grind.off"),
                    true, () -> grind = !grind, Component.translatable(KEY + "grind.tooltip", toolLevel(ResearchTool.MORTAR))));
        }
        for (PanelButton button : buttons) {
            renderButton(graphics, button, mouseX, mouseY);
        }

        renderItems(graphics, current, sheet, mouseX, mouseY);
        renderMessage(graphics, messageFor(current, sheet, missing));
    }

    private Runnable leaveAction(int point) {
        return () -> send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.LEAVE, point));
    }

    @Nullable
    private List<Component> pendingTooltip;

    private Component messageFor(RuneBoard current, ResearchSheet sheet, @Nullable Requirement missing) {
        if (!message.getString().isEmpty()) {
            return message;
        }
        if (!sheet.isOpen()) {
            return missing != null ? Component.translatable(missing.translationKey()) : Component.translatable(KEY + "hint.take");
        }
        return Component.translatable(KEY + "hint.write");
    }

    private void renderItems(GuiGraphics graphics, @Nullable RuneBoard current, @Nullable ResearchSheet sheet, int mouseX, int mouseY) {
        RuneKnowledge knowledge = ClientKnowledge.get();
        boolean canWrite = sheet != null && sheet.canWrite();
        for (int i = 0; i < ITEM_COLUMNS * ITEM_ROWS; i++) {
            int index = itemScroll * ITEM_COLUMNS + i;
            if (index >= items.size()) {
                break;
            }
            ItemEntry entry = items.get(index);
            int x = leftPos + ITEMS_X + i % ITEM_COLUMNS * 18;
            int y = topPos + ITEMS_Y + i / ITEM_COLUMNS * 18;
            ItemStack stack = new ItemStack(entry.item(), entry.count());
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
            if (!canWrite) {
                graphics.fill(x, y, x + 16, y + 16, 0x80140C06);
            } else if (canGrind(entry.elements())) {
                graphics.drawString(font, "½", x, y, 0xFF7EF0FF, true);
            }
            if (!knowledge.knowsItem(BuiltInRegistries.ITEM.getKey(entry.item()))) {
                graphics.drawString(font, "?", x + 11, y - 1, 0xFFE0A21C, true);
            }
            if (isOver(mouseX, mouseY, x - 1, y - 1, 18, 18)) {
                graphics.fill(x, y, x + 16, y + 16, 0x60FFFFFF);
                List<Component> lines = new ArrayList<>();
                lines.add(entry.item().getDescription());
                if (knowledge.knowsItem(BuiltInRegistries.ITEM.getKey(entry.item()))) {
                    lines.add(ResearchText.elements(entry.elements()));
                } else {
                    lines.add(Component.translatable(KEY + "item.unknown").withStyle(ChatFormatting.GRAY));
                }
                lines.add(Component.translatable(KEY + "item.tier." + entry.elements().tier()).withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.translatable(canWrite ? KEY + "item.write" : KEY + "item.no_sheet").withStyle(ChatFormatting.DARK_GRAY));
                pendingTooltip = lines;
            }
        }
        if (items.isEmpty()) {
            List<FormattedCharSequence> lines = font.split(Component.translatable(KEY + "items.none"), 196);
            for (int i = 0; i < lines.size(); i++) {
                graphics.drawString(font, lines.get(i), leftPos + PANEL_X + 8, topPos + ITEMS_Y + 4 + i * 10, 0xFF000000 | TEXT_MUTED, false);
            }
        }
        int pages = Math.max(0, (items.size() - 1) / ITEM_COLUMNS - ITEM_ROWS + 1);
        if (pages > 0) {
            Component scroll = Component.literal((itemScroll + 1) + "/" + (pages + 1));
            graphics.drawString(font, scroll, leftPos + PANEL_X + PANEL_W - 4 - font.width(scroll), topPos + ITEMS_Y - 10,
                    0xFF000000 | TEXT_MUTED, true);
        }
    }

    private void renderMessage(GuiGraphics graphics, Component text) {
        int color = switch (tone) {
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
        if (lines.size() > max && isOver(mouseXLast, mouseYLast, leftPos + PANEL_X, topPos + MESSAGE_Y - 3, PANEL_W, 32)) {
            pendingTooltip = List.of(text);
        }
    }

    private int mouseXLast;
    private int mouseYLast;

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

    private void renderParchmentTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        mouseXLast = mouseX;
        mouseYLast = mouseY;
        if (pendingTooltip != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : pendingTooltip) {
                lines.addAll(font.split(line, 220));
            }
            graphics.renderTooltip(font, lines, mouseX, mouseY);
            pendingTooltip = null;
            return;
        }
        RuneBoard current = currentBoard();
        ResearchTableBlockEntity table = table();
        if (current == null || table == null || !isOver(mouseX, mouseY, leftPos + 4, topPos + 24, 244, 234)) {
            return;
        }
        int cell = board.cellAt(mouseX, mouseY);
        if (cell >= 0) {
            List<Component> lines = board.describe(current, table.sheet(), cell, toolLevel(ResearchTool.FORK));
            if (!lines.isEmpty()) {
                graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }

    private int count(Item item) {
        if (minecraft.player == null) {
            return 0;
        }
        if (minecraft.player.getAbilities().instabuild) {
            return 99;
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
        if (tab == Tab.BOOK) {
            if (book.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        } else if (tab == Tab.PARCHMENT && button == 0 && parchmentClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean parchmentClicked(double mouseX, double mouseY) {
        for (PanelButton panelButton : buttons) {
            if (isOver(mouseX, mouseY, panelButton.x(), panelButton.y(), panelButton.width(), 16)) {
                if (panelButton.active()) {
                    panelButton.action().run();
                    click();
                }
                return true;
            }
        }
        ResearchTableBlockEntity table = table();
        RuneBoard current = currentBoard();
        if (table == null || current == null) {
            return false;
        }
        ItemEntry entry = hoveredItem(mouseX, mouseY);
        if (entry != null) {
            if (table.sheet().canWrite()) {
                send(ResearchActionPayload.write(menu.pos(), entry.item(), canGrind(entry.elements())));
            } else {
                onMessage(Component.translatable(table.sheet().isOpen() ? KEY + "sheet.over" : KEY + "hint.take"),
                        ResearchMessagePayload.Tone.BAD);
            }
            return true;
        }
        if (isOver(mouseX, mouseY, leftPos + 4, topPos + 24, 244, 234)) {
            int cell = board.cellAt(mouseX, mouseY);
            int fork = toolLevel(ResearchTool.FORK);
            if (cell >= 0 && table.sheet().canJump(current, cell, fork)) {
                send(ResearchActionPayload.of(menu.pos(), ResearchActionPayload.Action.JUMP, cell));
                click();
            }
            return true;
        }
        return false;
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
        if (tab == Tab.PARCHMENT && isOver(mouseX, mouseY, leftPos + PANEL_X, topPos + ITEMS_Y - 3, PANEL_W, 58)) {
            itemScroll = Math.max(0, itemScroll - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
