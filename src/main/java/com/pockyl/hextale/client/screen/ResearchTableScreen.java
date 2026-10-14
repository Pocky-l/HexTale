package com.pockyl.hextale.client.screen;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.FusionResult;
import com.pockyl.hextale.block.Requirement;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.network.DrawingPayload;
import com.pockyl.hextale.network.FuseRunesPayload;
import com.pockyl.hextale.network.RecognitionFailedPayload;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneStrokes;
import com.pockyl.hextale.rune.SimpleRune;

import org.jetbrains.annotations.Nullable;

/**
 * The Research Table: the diary's rune reference page on the far left, then the board with the materials, the parchment
 * with the two circles and the orb, and the scroll on the right.
 */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> implements RuneCanvas.Host {
    public static final ResourceLocation TEXTURE = Hextale.id("textures/gui/research_table.png");
    public static final ResourceLocation WIDGETS = Hextale.id("textures/gui/research_table_widgets.png");
    public static final int SHEET_WIDTH = 512;
    public static final int SHEET_HEIGHT = 256;
    private static final int WIDGETS_WIDTH = 128;
    private static final int WIDGETS_HEIGHT = 64;
    private static final int BOARD_X = ResearchTableMenu.BOARD_X;
    private static final int BOARD_WIDTH = 276;
    /** The reference page sits a little lower than the board and slides under its left edge. */
    private static final int PAGE_Y = 10;
    private static final int OPEN_PAGE_U = 280;
    private static final int CLOSED_PAGE_U = 392;
    private static final int CANVAS_X = BOARD_X + 34;
    private static final int CANVAS_Y = 17;
    private static final int CANVAS_WIDTH = 208;
    private static final int CANVAS_HEIGHT = 112;
    /** Ghost icons for the empty column slots, left to right in column order. */
    private static final int GHOST_U = 0;
    private static final int GHOST_V = 0;
    private static final int BUTTON_U = 0;
    private static final int BUTTON_V = 20;
    private static final int TEXT_LIGHT = 0xE8D9B5;
    private static final String[] SLOT_HINTS = {"diary", "paper", "hextech", "ink", "brush"};
    private static final Requirement[] SLOT_REQUIREMENTS = {Requirement.DIARY, Requirement.PAPER, Requirement.HEXTECH,
            Requirement.INK, Requirement.BRUSH};

    private final RuneCanvas canvas;
    private final RuneReferencePage reference;

    public ResearchTableScreen(ResearchTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = ResearchTableMenu.IMAGE_WIDTH;
        imageHeight = ResearchTableMenu.IMAGE_HEIGHT;
        titleLabelX = BOARD_X + 8;
        inventoryLabelX = ResearchTableMenu.INVENTORY_X;
        inventoryLabelY = ResearchTableMenu.INVENTORY_Y - 10;
        canvas = new RuneCanvas(this, Minecraft.getInstance().font);
        reference = new RuneReferencePage(Minecraft.getInstance().font);
    }

    @Override
    protected void init() {
        super.init();
        canvas.layout(leftPos + CANVAS_X, topPos + CANVAS_Y, CANVAS_WIDTH, CANVAS_HEIGHT);
        reference.layout(leftPos, topPos + PAGE_Y);
        addRenderableWidget(new IconButton(leftPos + BOARD_X + 248, topPos + 90, 20, 18,
                Component.translatable("gui.hextale.research_table.swap"), WIDGETS, BUTTON_U, BUTTON_V, WIDGETS_WIDTH, WIDGETS_HEIGHT,
                ResearchTableScreen::swapIcon, button -> canvas.swap()));
        addRenderableWidget(new IconButton(leftPos + BOARD_X + 248, topPos + 111, 20, 18,
                Component.translatable("gui.hextale.research_table.clear"), WIDGETS, BUTTON_U, BUTTON_V, WIDGETS_WIDTH, WIDGETS_HEIGHT,
                ResearchTableScreen::clearIcon, button -> canvas.clear()));
    }

    private static void swapIcon(InkRenderer ink, IconButton button) {
        float x = button.centerX();
        float y = button.centerY();
        int color = RuneDrawing.argb(0x3A2614, 1);
        ink.stroke(new float[]{x - 5, y - 2.5F, x + 5, y - 2.5F}, 1.3F, color);
        ink.stroke(new float[]{x + 2.5F, y - 5, x + 5, y - 2.5F, x + 2.5F, y}, 1.3F, color);
        ink.stroke(new float[]{x + 5, y + 2.5F, x - 5, y + 2.5F}, 1.3F, color);
        ink.stroke(new float[]{x - 2.5F, y, x - 5, y + 2.5F, x - 2.5F, y + 5}, 1.3F, color);
    }

    private static void clearIcon(InkRenderer ink, IconButton button) {
        float x = button.centerX();
        float y = button.centerY();
        int color = RuneDrawing.argb(0x3A2614, 1);
        ink.stroke(new float[]{x - 4, y - 4, x + 4, y + 4}, 1.4F, color);
        ink.stroke(new float[]{x + 4, y - 4, x - 4, y + 4}, 1.4F, color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && hoveredSlot != null && !hoveredSlot.hasItem()) {
            Component hint = slotHint(hoveredSlot);
            if (hint != null) {
                graphics.renderTooltip(font, font.split(hint, 180), mouseX, mouseY);
            }
        }
    }

    @Nullable
    private Component slotHint(Slot slot) {
        if (slot.container != menu.table()) {
            return null;
        }
        int index = slot.getContainerSlot();
        if (index == ResearchTableBlockEntity.OUTPUT) {
            return Component.translatable("gui.hextale.research_table.slot.output");
        }
        return index < SLOT_HINTS.length ? Component.translatable("gui.hextale.research_table.slot." + SLOT_HINTS[index]) : null;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RuneReferencePage.State page = pageState();
        graphics.blit(TEXTURE, leftPos, topPos + PAGE_Y, page == RuneReferencePage.State.OPEN ? OPEN_PAGE_U : CLOSED_PAGE_U, 0,
                RuneReferencePage.WIDTH, RuneReferencePage.HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        reference.render(graphics, page, canvas.guide(), mouseX, mouseY);
        graphics.blit(TEXTURE, leftPos + BOARD_X, topPos, 0, 0, BOARD_WIDTH, imageHeight, SHEET_WIDTH, SHEET_HEIGHT);
        Requirement missing = missing();
        int[] containerSlots = {ResearchTableBlockEntity.DIARY, ResearchTableBlockEntity.PAPER, ResearchTableBlockEntity.HEXTECH,
                ResearchTableBlockEntity.INK, ResearchTableBlockEntity.BRUSH};
        for (int i = 0; i < containerSlots.length; i++) {
            int x = leftPos + ResearchTableMenu.COLUMN_X;
            int y = topPos + ResearchTableMenu.COLUMN_Y[i];
            if (menu.table().getItem(containerSlots[i]).isEmpty()) {
                graphics.blit(WIDGETS, x, y, GHOST_U + i * 16, GHOST_V, 16, 16, WIDGETS_WIDTH, WIDGETS_HEIGHT);
            }
            boolean wanted = missing == SLOT_REQUIREMENTS[i] || missing == Requirement.OWN_DIARY && i == 0;
            if (wanted) {
                highlight(graphics, x, y);
            }
        }
        canvas.render(graphics, missing, mouseX, mouseY);
        canvas.renderMessage(graphics, leftPos + CANVAS_X, topPos + CANVAS_Y + CANVAS_HEIGHT + 4, CANVAS_WIDTH);
    }

    /** A pulsing brass frame around the slot the canvas asks for. */
    private static void highlight(GuiGraphics graphics, int x, int y) {
        float pulse = 0.5F + 0.5F * Mth.sin(Util.getMillis() / 160.0F);
        InkRenderer ink = InkRenderer.begin(graphics);
        int color = RuneDrawing.argb(0xFFD36E, 0.45F + 0.5F * pulse);
        ink.stroke(new float[]{x - 1.5F, y - 1.5F, x + 17.5F, y - 1.5F, x + 17.5F, y + 17.5F, x - 1.5F, y + 17.5F, x - 1.5F, y - 1.5F},
                1.2F, color);
        ink.end();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF000000 | TEXT_LIGHT, true);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF000000 | TEXT_LIGHT, true);
        Component codex = Component.translatable("gui.hextale.research_table.codex", ClientKnowledge.get().runeCount(), ComplexRunes.COUNT);
        graphics.drawString(font, codex, imageWidth - 8 - font.width(codex), titleLabelY, 0xFFB8A27A, true);
    }

    /** Open with the player's own diary on the table, closed otherwise. */
    private RuneReferencePage.State pageState() {
        ItemStack diary = menu.table().getItem(ResearchTableBlockEntity.DIARY);
        if (!diary.is(ModItems.RESEARCHERS_DIARY.get())) {
            return RuneReferencePage.State.NO_DIARY;
        }
        return minecraft != null && minecraft.player != null && !ResearchersDiaryItem.canResearchWith(diary, minecraft.player)
                ? RuneReferencePage.State.FOREIGN_DIARY : RuneReferencePage.State.OPEN;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && canvas.mouseDown(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && pageState() == RuneReferencePage.State.OPEN) {
            SimpleRune rune = reference.runeAt(mouseX, mouseY);
            if (rune != null) {
                canvas.showGuide(rune);
                playSound(SoundEvents.BOOK_PAGE_TURN, 1.2F, 0.6F);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && canvas.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && canvas.mouseUp()) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void onFusionResult(FusionResult result) {
        canvas.onFusionResult(result);
    }

    // ---- RuneCanvas.Host -----------------------------------------------------------------------------------

    @Nullable
    @Override
    public Requirement missing() {
        return minecraft == null || minecraft.player == null ? null : Requirement.firstMissing(menu.table(), minecraft.player);
    }

    @Override
    public void fuse(RuneStrokes action, RuneStrokes target) {
        PacketDistributor.sendToServer(new FuseRunesPayload(menu.pos(), action, target));
    }

    @Override
    public void drawing() {
        PacketDistributor.sendToServer(new DrawingPayload(menu.pos()));
    }

    @Override
    public void recognitionFailed() {
        PacketDistributor.sendToServer(new RecognitionFailedPayload(menu.pos()));
    }

    @Override
    public void playSound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
