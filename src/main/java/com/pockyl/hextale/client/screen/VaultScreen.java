package com.pockyl.hextale.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.pockyl.hextale.port.client.KeyEvent;
import com.pockyl.hextale.port.client.MouseButtonEvent;
import com.pockyl.hextale.port.client.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import com.pockyl.hextale.port.ItemResource;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.RiftGateBlock;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.EssenceLookup;
import com.pockyl.hextale.client.RecipeLookup;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.menu.VaultMenu;
import com.pockyl.hextale.network.VaultActionPayload;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.storage.VaultContents;
import com.pockyl.hextale.storage.VaultToll;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The one screen of a Hex Vault, at the vault, a terminal or a Pocket Terminal. The grid takes as many columns and rows as
 * the window gives it: the player's slots keep their place at {@code leftPos, topPos} and the panel grows up and out round
 * them. On top the title and the search, then the essence tabs, the capacity bar and the sorting; under them the pinned row,
 * the grid, and one line that says what state the vault and the toll are in.
 */
public class VaultScreen extends AbstractContainerScreen<VaultMenu> {
    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics portGraphics, float portPartialTick, int portMouseX, int portMouseY) {
    }

    /** The part round the player's slots, which is all the base class knows of. */
    private static final int INVENTORY_W = 176;
    private static final int INVENTORY_H = 98;
    private static final int CELL = VaultMenu.CELL;
    private static final int PAD = 8;
    private static final int SCROLL_GAP = 2;
    private static final int SCROLL_W = 7;
    private static final int LEAST_COLUMNS = 11;
    private static final int MOST_COLUMNS = VaultContents.MOST_PINS;
    private static final int LEAST_ROWS = 3;
    private static final int MOST_ROWS = 14;
    /** Left free at either side of the panel, so JEI keeps a usable list. */
    private static final int SIDE_ROOM = 100;
    private static final int WINDOW_MARGIN = 2;
    private static final int TOP = 6;
    private static final int BAR = 14;
    private static final int BAR_GAP = 2;
    private static final int PIN_GAP = 3;
    private static final int STATUS_H = 9;
    /** All the height that is not grid rows. */
    private static final int CHROME = TOP + BAR + BAR_GAP + BAR + PIN_GAP + CELL + PIN_GAP + PIN_GAP + STATUS_H + PIN_GAP + INVENTORY_H;
    private static final int TAB = 14;
    private static final int SIGN = 12;
    private static final int ICON = 12;
    private static final int NOTE_TICKS = 70;
    private static final float AMOUNT_ROOM = 16;
    private static final int OVER_CELL = 0x2EFFFFFF;
    private static final int ALARM = 0xFFFF9A7A;
    private static final int WARNING = 0xFFE9C66D;
    private static final ResourceLocation PIN = icon("pin");
    private static final ResourceLocation STORE = icon("store");
    private static final ResourceLocation HELP = icon("help");
    private static final ResourceLocation ORDER_DOWN = icon("order_down");
    private static final ResourceLocation ORDER_UP = icon("order_up");
    private static final String[] UNITS = {"K", "M", "B", "T"};
    private static final List<String> HELP_LINES = List.of("left", "right", "shift", "wheel", "double", "drag", "pin");

    /** How the grid is ordered, in its natural direction. */
    private enum Sort {
        COUNT, NAME, WEIGHT;

        final ResourceLocation sprite = icon("sort_" + name().toLowerCase(Locale.ROOT));

        Comparator<Shown> order() {
            Comparator<Shown> byName = Comparator.comparing(Shown::lowerName);
            return switch (this) {
                case COUNT -> Comparator.comparingLong((Shown shown) -> shown.line.count()).reversed().thenComparing(byName);
                case NAME -> byName;
                case WEIGHT -> Comparator.comparingLong((Shown shown) -> shown.line.count() * shown.line.weight()).reversed().thenComparing(byName);
            };
        }

        Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private record Shown(VaultMenu.Line line, ItemStack stack, String lowerName) {
    }

    private static Sort sort = Sort.COUNT;
    private static boolean reversed;
    private static String remembered = "";

    private EditBox search;
    private final List<Shown> shown = new ArrayList<>();
    private final List<Shown> pinned = new ArrayList<>();
    private final Set<Essence> tabs = new HashSet<>();
    private int columns = LEAST_COLUMNS;
    private int rows = LEAST_ROWS;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int seenRevision = -1;
    private String seenSearch = "";
    private int firstRow;
    private boolean draggingBar;
    /** The left button went down on an inventory slot and picked its stack up: letting go over the grid stores it. */
    private boolean draggingStack;
    /** What the last click took, so the second click of a double click gathers the rest. */
    private ItemResource lastTaken = ItemResource.EMPTY;
    private int waitLeft;
    private int seenWait;
    private int noteLeft;
    private int note = -1;

    public VaultScreen(VaultMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = INVENTORY_W;
        this.imageHeight = INVENTORY_H;
        inventoryLabelX = VaultMenu.INVENTORY_X;
        inventoryLabelY = 5;
    }

    private static ResourceLocation icon(String name) {
        return Hextale.id("vault/" + name);
    }

    @Override
    protected void init() {
        columns = Mth.clamp((width - SIDE_ROOM * 2 - PAD * 2 - SCROLL_GAP - SCROLL_W) / CELL, LEAST_COLUMNS, MOST_COLUMNS);
        rows = Mth.clamp((height - WINDOW_MARGIN * 2 - CHROME) / CELL, LEAST_ROWS, MOST_ROWS);
        super.init();
        panelW = PAD * 2 + columns * CELL + SCROLL_GAP + SCROLL_W;
        panelH = CHROME + rows * CELL;
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        leftPos = panelX + (panelW - INVENTORY_W) / 2;
        topPos = panelY + panelH - INVENTORY_H;

        int searchX = gridX() + Math.min(font.width(title), panelW / 2) + 8;
        int searchRight = helpX() - 3;
        search = addRenderableWidget(new EditBox(font, searchX + 3, barY() + 3, searchRight - searchX - 6, 9, Component.translatable("gui.hextale.vault.search")));
        search.setBordered(false);
        search.setMaxLength(64);
        search.setTextColor(HexPlate.TEXT);
        search.setHint(Component.translatable("gui.hextale.vault.search"));
        search.setValue(remembered);
        search.setResponder(value -> remembered = value);
        setInitialFocus(search);
        seenRevision = -1;
    }

    // JEI sizes its lists from these: it has to see the panel, not only the part round the slots.

    @Override
    public int getGuiLeft() {
        return panelX;
    }

    @Override
    public int getGuiTop() {
        return panelY;
    }

    @Override
    public int getXSize() {
        return panelW;
    }

    @Override
    public int getYSize() {
        return panelH;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int portButton) {
        return mouseX < panelX || mouseY < panelY || mouseX >= panelX + panelW || mouseY >= panelY + panelH;
    }

    // ---- Where things are ----------------------------------------------------------------------------------------------

    private int gridX() {
        return panelX + PAD;
    }

    private int barY() {
        return panelY + TOP;
    }

    private int tabsY() {
        return barY() + BAR + BAR_GAP;
    }

    private int pinY() {
        return tabsY() + BAR + PIN_GAP;
    }

    private int gridY() {
        return pinY() + CELL + PIN_GAP;
    }

    private int gridH() {
        return rows * CELL;
    }

    private int statusY() {
        return gridY() + gridH() + PIN_GAP;
    }

    private int scrollX() {
        return gridX() + columns * CELL + SCROLL_GAP;
    }

    private int rightEdge() {
        return scrollX() + SCROLL_W;
    }

    private int helpX() {
        return rightEdge() - BAR;
    }

    private int storeX() {
        return rightEdge() - BAR;
    }

    private int orderX() {
        return storeX() - BAR - 3;
    }

    private int sortX() {
        return orderX() - BAR - 1;
    }

    private int gaugeX() {
        return gridX() + Essence.PRIMALS.size() * TAB + 4;
    }

    private int gaugeW() {
        return sortX() - 4 - gaugeX();
    }

    private static boolean over(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private boolean overBar(double mouseX, double mouseY, int x, int y) {
        return over(mouseX, mouseY, x, y, BAR, BAR);
    }

    private boolean overScroll(double mouseX, double mouseY) {
        return over(mouseX, mouseY, scrollX(), gridY(), SCROLL_W, gridH());
    }

    private boolean overCells(double mouseX, double mouseY) {
        return over(mouseX, mouseY, gridX(), gridY(), columns * CELL, gridH()) || over(mouseX, mouseY, gridX(), pinY(), columns * CELL, CELL);
    }

    private int tabAt(double mouseX, double mouseY) {
        if (!over(mouseX, mouseY, gridX(), tabsY(), Essence.PRIMALS.size() * TAB, BAR)) {
            return -1;
        }
        return (int) ((mouseX - gridX()) / TAB);
    }

    /** The entry under the mouse, or null. */
    @Nullable
    private Shown at(double mouseX, double mouseY) {
        if (!overCells(mouseX, mouseY)) {
            return null;
        }
        int column = (int) ((mouseX - gridX()) / CELL);
        if (mouseY < gridY()) {
            return column < pinned.size() ? pinned.get(column) : null;
        }
        int index = (firstRow + (int) ((mouseY - gridY()) / CELL)) * columns + column;
        return index < shown.size() ? shown.get(index) : null;
    }

    // ---- What to show --------------------------------------------------------------------------------------------------

    private void refresh() {
        String wanted = search.getValue().trim().toLowerCase(Locale.ROOT);
        if (seenRevision == menu.revision() && seenSearch.equals(wanted)) {
            return;
        }
        seenRevision = menu.revision();
        seenSearch = wanted;
        rebuild();
    }

    private void rebuild() {
        String wanted = seenSearch;
        Set<Item> byEssence = wanted.isEmpty() ? Set.of() : new HashSet<>(EssenceLookup.studiedWith(wanted));
        shown.clear();
        for (VaultMenu.Line line : menu.lines()) {
            ItemStack stack = line.resource().toStack(1);
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            if ((wanted.isEmpty() || name.contains(wanted) || byEssence.contains(stack.getItem())) && passesTabs(stack)) {
                shown.add(new Shown(line, stack, name));
            }
        }
        shown.sort(reversed ? sort.order().reversed() : sort.order());
        pinned.clear();
        for (ItemResource pin : menu.pins()) {
            VaultMenu.Line line = menu.line(pin);
            pinned.add(new Shown(line == null ? new VaultMenu.Line(pin, 0, 0) : line, pin.toStack(1), ""));
        }
        firstRow = Mth.clamp(firstRow, 0, lastRow());
    }

    /** Tabs lit show studied items carrying all of them; what is not studied has no essences to show. */
    private boolean passesTabs(ItemStack stack) {
        if (tabs.isEmpty()) {
            return true;
        }
        if (!ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return false;
        }
        ItemEssences carried = ResearchItems.of(stack);
        if (carried == null) {
            return false;
        }
        for (Essence sign : tabs) {
            if (carried.essences().keySet().stream().noneMatch(essence -> essence.share(sign) > 0)) {
                return false;
            }
        }
        return true;
    }

    private int lines() {
        return (shown.size() + columns - 1) / columns;
    }

    private int lastRow() {
        return Math.max(0, lines() - rows);
    }

    /** 950, 1.2K, 34K, 3.4M: rounded down, so a count never promises more than is there. */
    static String amount(long count) {
        if (count < 1000) {
            return Long.toString(count);
        }
        long size = 1000;
        int unit = 0;
        while (count / size >= 1000 && unit < UNITS.length - 1) {
            size *= 1000;
            unit++;
        }
        long whole = count / size;
        long tenth = count % size / (size / 10);
        return whole < 10 && tenth > 0 ? whole + "." + tenth + UNITS[unit] : whole + UNITS[unit];
    }

    // ---- Drawing -------------------------------------------------------------------------------------------------------

    @Override
    public void containerTick() {
        super.containerTick();
        if (menu.waiting() != seenWait) {
            seenWait = menu.waiting();
            waitLeft = seenWait;
        } else if (waitLeft > 0) {
            waitLeft--;
        }
        int newNote = menu.takeNote();
        if (newNote >= 0) {
            note = newNote;
            noteLeft = NOTE_TICKS;
        } else if (noteLeft > 0) {
            noteLeft--;
        }
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.renderBackground(graphics.vanilla(), mouseX, mouseY, partialTick);
        refresh();
        HexPlate.plate(graphics, panelX, panelY, panelW, panelH);
        HexPlate.divider(graphics, panelX, topPos - 1, panelW);
        HexPlate.title(graphics, font, title, gridX() + 2, barY() + 3, panelW / 2);
        HexPlate.well(graphics, search.getX() - 3, barY(), search.getWidth() + 6, BAR);
        if (search.getValue().isEmpty() && search.isFocused()) {
            // The box hides its own hint while it has the focus, and here it always has.
            graphics.text(font, Component.translatable("gui.hextale.vault.search"), search.getX() + 7, search.getY(), HexPlate.MUTED, false);
        }
        button(graphics, HELP, helpX(), barY(), mouseX, mouseY);

        tabs(graphics, mouseX, mouseY);
        gauge(graphics);
        button(graphics, sort.sprite, sortX(), tabsY(), mouseX, mouseY);
        button(graphics, reversed ? ORDER_UP : ORDER_DOWN, orderX(), tabsY(), mouseX, mouseY);
        button(graphics, STORE, storeX(), tabsY(), mouseX, mouseY);

        wells(graphics);
        for (Slot slot : menu.slots) {
            HexPlate.slot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        scrollBar(graphics);
        Shown hovered = menu.getCarried().isEmpty() || overCells(mouseX, mouseY) ? at(mouseX, mouseY) : null;
        if (overCells(mouseX, mouseY)) {
            int x = gridX() + (mouseX - gridX()) / CELL * CELL;
            int y = mouseY < gridY() ? pinY() : gridY() + (mouseY - gridY()) / CELL * CELL;
            if (hovered != null || !menu.getCarried().isEmpty()) {
                PixelArt.fill(graphics, x + 1, y + 1, x + CELL - 1, y + CELL - 1, OVER_CELL);
            }
        }
        PixelArt.flush(graphics);
        items(graphics);
        status(graphics);
    }

    private void button(GuiGraphicsExtractor graphics, ResourceLocation sprite, int x, int y, int mouseX, int mouseY) {
        PixelArt.flush(graphics);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HexPlate.buttonSprite(overBar(mouseX, mouseY, x, y), true), x, y, BAR, BAR);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 1, y + 1, ICON, ICON);
    }

    private void tabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int hovered = tabAt(mouseX, mouseY);
        for (int i = 0; i < Essence.PRIMALS.size(); i++) {
            Essence essence = Essence.PRIMALS.get(i);
            int x = gridX() + i * TAB;
            HexPlate.well(graphics, x, tabsY(), TAB, BAR);
            HexPlate.cell(graphics, x, tabsY(), TAB, tabs.contains(essence), hovered == i);
            int colour = PixelArt.argb(essence.color(), tabs.isEmpty() || tabs.contains(essence) || hovered == i ? 1 : 0.45F);
            PixelArt.essence(graphics, essence, x + 1, tabsY() + 1, SIGN, colour);
        }
    }

    private void gauge(GuiGraphicsExtractor graphics) {
        int x = gaugeX();
        int y = tabsY();
        int w = gaugeW();
        HexPlate.well(graphics, x, y, w, BAR);
        float full = menu.capacity() <= 0 ? 0 : Math.min(1, (float) menu.used() / menu.capacity());
        int width = Math.round((w - 4) * full);
        if (width > 0) {
            int colour = full >= 0.98F ? 0xFF7A5A : HexPlate.BRASS;
            PixelArt.fill(graphics, x + 2, y + 2, x + 2 + width, y + BAR - 2, PixelArt.argb(colour, 1));
            PixelArt.fill(graphics, x + 2, y + 2, x + 2 + width, y + 3, PixelArt.argb(full >= 0.98F ? 0xFFB09A : HexPlate.BRASS_LIGHT, 1));
        }
        String text = amount(menu.used()) + " / " + amount(menu.capacity());
        if (font.width(text) <= w - 4) {
            PixelArt.flush(graphics);
            graphics.centeredText(font, Component.literal(text), x + w / 2, y + 3, 0xFFFFFFFF);
        }
    }

    /** The pinned row and the grid. A row without pins is one long well that says how to fill it. */
    private void wells(GuiGraphicsExtractor graphics) {
        if (pinned.isEmpty()) {
            HexPlate.well(graphics, gridX(), pinY(), columns * CELL, CELL);
            HexPlate.centered(graphics, font, Component.translatable("gui.hextale.vault.pin.hint"), gridX() + columns * CELL / 2, pinY() + 5, columns * CELL - 8, HexPlate.MUTED);
        } else {
            for (int column = 0; column < columns; column++) {
                HexPlate.slot(graphics, gridX() + column * CELL + 1, pinY() + 1);
            }
        }
        HexPlate.well(graphics, scrollX(), pinY(), SCROLL_W, CELL);
        PixelArt.flush(graphics);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PIN, scrollX() - 2, pinY() + 3, ICON, ICON);
        HexPlate.well(graphics, scrollX(), gridY(), SCROLL_W, gridH());
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                HexPlate.slot(graphics, gridX() + column * CELL + 1, gridY() + row * CELL + 1);
            }
        }
    }

    private void scrollBar(GuiGraphicsExtractor graphics) {
        int lines = lines();
        if (lines <= rows) {
            return;
        }
        int bar = Math.max(12, gridH() * rows / lines);
        int top = gridY() + (gridH() - bar) * firstRow / Math.max(1, lastRow());
        PixelArt.flush(graphics);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HexPlate.BUTTON, scrollX(), top, SCROLL_W, bar);
    }

    private void items(GuiGraphicsExtractor graphics) {
        int first = firstRow * columns;
        int last = Math.min(shown.size(), first + rows * columns);
        int pins = Math.min(pinned.size(), columns);
        for (int i = 0; i < pins; i++) {
            graphics.item(pinned.get(i).stack(), gridX() + i * CELL + 1, pinY() + 1);
        }
        for (int i = first; i < last; i++) {
            graphics.item(shown.get(i).stack(), cellX(i - first) + 1, cellY(i - first) + 1);
        }
        PixelArt.flush(graphics);
        graphics.nextStratum();
        for (int i = 0; i < pins; i++) {
            Shown pin = pinned.get(i);
            int x = gridX() + i * CELL;
            if (pin.line().count() <= 0) {
                // Nothing left of it: the pin stays, greyed out, until it is taken off.
                PixelArt.fill(graphics, x + 1, pinY() + 1, x + CELL - 1, pinY() + CELL - 1, PixelArt.argb(HexPlate.WELL_FLOOR, 0.7F));
            }
        }
        PixelArt.flush(graphics);
        float scale = amountScale();
        for (int i = 0; i < pins; i++) {
            amount(graphics, pinned.get(i).line().count(), gridX() + i * CELL, pinY(), scale);
        }
        for (int i = first; i < last; i++) {
            amount(graphics, shown.get(i).line().count(), cellX(i - first), cellY(i - first), scale);
        }
    }

    /** A whole number of screen pixels per font pixel, as near three quarters of the normal size as the GUI scale allows. */
    private float amountScale() {
        int gui = (int) minecraft.getWindow().getGuiScale();
        return gui == 3 ? 2F / 3 : 0.75F;
    }

    private void amount(GuiGraphicsExtractor graphics, long count, int x, int y, float scale) {
        if (count <= 1) {
            return;
        }
        String text = amount(count);
        // The widest amounts (345K) shrink a little, so neighbours never run together.
        float fitted = Math.min(scale, AMOUNT_ROOM / font.width(text));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 17, y + 17 - 8 * fitted);
        graphics.pose().scale(fitted, fitted);
        graphics.text(font, text, -font.width(text), 0, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }

    private int cellX(int index) {
        return gridX() + index % columns * CELL;
    }

    private int cellY(int index) {
        return gridY() + index / columns * CELL;
    }

    /** The one line under the grid: a refusal first, then what keeps the vault from working, then the price of a take. */
    private void status(GuiGraphicsExtractor graphics) {
        int x = gridX();
        int y = statusY();
        int width = rightEdge() - x;
        Component text = statusText();
        HexPlate.line(graphics, font, text, x, y, width, statusColour(), false);
        if (noteLeft <= 0 && menu.working() && menu.dry() && waitLeft > 0) {
            int bar = width * waitLeft / VaultToll.SLOW_TICKS;
            PixelArt.fill(graphics, x, y + STATUS_H, x + bar, y + STATUS_H + 1, PixelArt.argb(RiftGateBlock.RIFT, 1));
            PixelArt.flush(graphics);
        }
    }

    private Component statusText() {
        if (noteLeft > 0 && note >= 0 && note < VaultContents.Refusal.values().length) {
            return Component.translatable(VaultMenu.refusalKey(VaultContents.Refusal.values()[note]));
        }
        if (!menu.working()) {
            return Component.translatable("gui.hextale.vault.asleep");
        }
        if (menu.kind() == VaultMenu.Kind.VAULT) {
            return Component.translatable("gui.hextale.vault.free");
        }
        return Component.translatable(menu.dry() ? "gui.hextale.vault.dry" : "gui.hextale.vault.toll");
    }

    private int statusColour() {
        if (noteLeft > 0 && note >= 0) {
            return ALARM;
        }
        return !menu.working() || menu.kind() != VaultMenu.Kind.VAULT && menu.dry() ? WARNING : HexPlate.MUTED;
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HexPlate.TEXT, false);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
        List<Component> tooltip = tooltip(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    private List<Component> tooltip(int mouseX, int mouseY) {
        Shown hovered = at(mouseX, mouseY);
        if (hovered != null && menu.getCarried().isEmpty()) {
            List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(hovered.stack()));
            lines.add(muted(Component.translatable("gui.hextale.vault.held", hovered.line().count())));
            if (hovered.line().count() > 0) {
                lines.add(muted(Component.translatable("gui.hextale.vault.weight", hovered.line().weight())));
            }
            return lines;
        }
        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0) {
            return List.of(Component.translatable(Essence.PRIMALS.get(tab).translationKey()), muted(Component.translatable("gui.hextale.vault.tab.hint")));
        }
        if (over(mouseX, mouseY, gaugeX(), tabsY(), gaugeW(), BAR)) {
            return List.of(Component.translatable("gui.hextale.vault.gauge", menu.used(), menu.capacity()));
        }
        if (overBar(mouseX, mouseY, sortX(), tabsY())) {
            return List.of(Component.translatable("gui.hextale.vault.sort." + sort.name().toLowerCase(Locale.ROOT)));
        }
        if (overBar(mouseX, mouseY, orderX(), tabsY())) {
            return List.of(Component.translatable("gui.hextale.vault.order." + (sort == Sort.NAME ? "name" : "size") + (reversed ? ".up" : ".down")));
        }
        if (overBar(mouseX, mouseY, storeX(), tabsY())) {
            return List.of(Component.translatable("gui.hextale.vault.store_all"), muted(Component.translatable("gui.hextale.vault.store_all.hint")));
        }
        if (overBar(mouseX, mouseY, helpX(), barY())) {
            List<Component> lines = new ArrayList<>();
            HELP_LINES.forEach(line -> lines.add(Component.translatable("gui.hextale.vault.help." + line)));
            return lines;
        }
        Component line = statusText();
        if (over(mouseX, mouseY, gridX(), statusY(), rightEdge() - gridX(), STATUS_H) && HexPlate.cut(font, line, rightEdge() - gridX())) {
            return List.of(line);
        }
        return List.of();
    }

    private static Component muted(Component text) {
        return text.copy().withStyle(ChatFormatting.GRAY);
    }

    // ---- Input ---------------------------------------------------------------------------------------------------------

    private void send(ItemResource resource, VaultMenu.Action action) {
        PacketDistributor.sendToServer(new VaultActionPayload(menu.containerId, resource.isEmpty() ? Optional.empty() : Optional.of(resource), action.ordinal()));
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
    }

    @Override
    public boolean mouseClicked(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        boolean doubleClick = false;
        double mouseX = event.x();
        double mouseY = event.y();
        boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
        draggingStack = false;
        if (left && controls(mouseX, mouseY)) {
            click();
            return true;
        }
        if (left && overScroll(mouseX, mouseY)) {
            draggingBar = true;
            dragBar(mouseY);
            return true;
        }
        if (overCells(mouseX, mouseY)) {
            cellClicked(event, doubleClick);
            return true;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT && search.isMouseOver(mouseX, mouseY)) {
            search.setValue("");
            return true;
        }
        draggingStack = left && !event.hasShiftDown() && menu.getCarried().isEmpty() && overFullSlot(mouseX, mouseY);
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    private boolean overFullSlot(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (slot.hasItem() && over(mouseX, mouseY, leftPos + slot.x - 1, topPos + slot.y - 1, CELL, CELL)) {
                return true;
            }
        }
        return false;
    }

    /** The tabs and buttons; true when the click hit one. */
    private boolean controls(double mouseX, double mouseY) {
        int tab = tabAt(mouseX, mouseY);
        if (tab >= 0) {
            Essence essence = Essence.PRIMALS.get(tab);
            if (!tabs.remove(essence)) {
                tabs.add(essence);
            }
            rebuild();
            return true;
        }
        if (overBar(mouseX, mouseY, sortX(), tabsY())) {
            sort = sort.next();
            rebuild();
            return true;
        }
        if (overBar(mouseX, mouseY, orderX(), tabsY())) {
            reversed = !reversed;
            rebuild();
            return true;
        }
        if (overBar(mouseX, mouseY, storeX(), tabsY())) {
            send(ItemResource.EMPTY, VaultMenu.Action.STORE_ALL);
            return true;
        }
        return false;
    }

    private void cellClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean right = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
        Shown hovered = at(event.x(), event.y());
        if (hovered != null && (event.button() == InputConstants.MOUSE_BUTTON_MIDDLE || left && event.hasAltDown())) {
            send(hovered.line().resource(), VaultMenu.Action.PIN);
            click();
            return;
        }
        if (left && doubleClick && hovered != null && hovered.line().resource().equals(lastTaken)) {
            send(lastTaken, VaultMenu.Action.TAKE_ALL);
            lastTaken = ItemResource.EMPTY;
            return;
        }
        lastTaken = ItemResource.EMPTY;
        if (!menu.getCarried().isEmpty()) {
            send(ItemResource.EMPTY, right ? VaultMenu.Action.PUT_ONE : VaultMenu.Action.PUT_CARRIED);
        } else if (hovered != null && (left || right)) {
            lastTaken = hovered.line().resource();
            send(lastTaken, event.hasShiftDown() ? VaultMenu.Action.TAKE_TO_INVENTORY : right ? VaultMenu.Action.TAKE_HALF : VaultMenu.Action.TAKE_STACK);
        }
    }

    @Override
    public boolean mouseDragged(double portX, double portY, int portButton, double dx, double dy) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        if (draggingBar) {
            dragBar(event.y());
            return true;
        }
        return super.mouseDragged(event.x(), event.y(), event.button(), dx, dy);
    }

    @Override
    public boolean mouseReleased(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        draggingBar = false;
        if (draggingStack && overCells(event.x(), event.y()) && !menu.getCarried().isEmpty()) {
            send(ItemResource.EMPTY, VaultMenu.Action.PUT_CARRIED);
        }
        draggingStack = false;
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    private void dragBar(double mouseY) {
        float share = (float) ((mouseY - gridY()) / gridH());
        firstRow = Mth.clamp(Math.round(share * lastRow()), 0, lastRow());
    }

    /** The wheel scrolls; with Shift it moves one of the item under the mouse, up out of the vault and down back in. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overCells(mouseX, mouseY) || overScroll(mouseX, mouseY)) {
            Shown hovered = at(mouseX, mouseY);
            if (shiftDown()) {
                if (hovered != null && scrollY != 0) {
                    send(hovered.line().resource(), scrollY > 0 ? VaultMenu.Action.TAKE_ONE : VaultMenu.Action.RETURN_ONE);
                }
                return true;
            }
            firstRow = Mth.clamp(firstRow - (int) Math.signum(scrollY), 0, lastRow());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static boolean shiftDown() {
        return InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), InputConstants.KEY_RSHIFT);
    }

    /** The search keeps every key but Escape while it has the focus; over the grid JEI's own keys work on the item under the mouse. */
    @Override
    public boolean keyPressed(int portKey, int portScan, int portMods) {
        KeyEvent event = new KeyEvent(portKey, portScan, portMods);
        if (event.isEscape()) {
            return super.keyPressed(event.key(), event.scancode(), event.modifiers());
        }
        if (search.isFocused()) {
            if (search.keyPressed(event.key(), event.scancode(), event.modifiers())) {
                return true;
            }
            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                setFocused(null);
                search.setFocused(false);
                return true;
            }
            return true;
        }
        double mouseX = minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight();
        Shown hovered = at(mouseX, mouseY);
        if (hovered != null && RecipeLookup.show(InputConstants.getKey(event.key(), event.scancode()), hovered.stack())) {
            return true;
        }
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }
}
