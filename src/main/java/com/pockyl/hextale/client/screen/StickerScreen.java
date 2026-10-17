package com.pockyl.hextale.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.item.EssenceStickerItem;
import com.pockyl.hextale.network.ChooseStickerPayload;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The essence picker of a sticker: every essence in its colour, in three bands (the nine primals, their pairs, the deeper
 * compounds), the one on the sticker framed in gold; a click writes it on the stickers in hand, the button at the bottom
 * leaves them blank.
 */
public class StickerScreen extends Screen {
    private static final int COLUMNS = 12;
    private static final int CELL = 13;
    private static final int GAP = 5;
    private static final int PAD = 8;
    private static final int TITLE = 14;
    private static final int BLANK_H = 14;
    private final InteractionHand hand;

    public StickerScreen(InteractionHand hand) {
        super(Component.translatable("gui.hextale.sticker.title"));
        this.hand = hand;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int group(Essence essence) {
        return essence.isPrimal() ? 0 : essence.tier() == 1 ? 1 : 2;
    }

    private static int rows(int group) {
        long count = java.util.Arrays.stream(Essence.values()).filter(e -> group(e) == group).count();
        return (int) ((count + COLUMNS - 1) / COLUMNS);
    }

    private int width() {
        return COLUMNS * CELL + PAD * 2;
    }

    private int height() {
        return TITLE + (rows(0) + rows(1) + rows(2)) * CELL + GAP * 2 + PAD * 2 + BLANK_H + 4;
    }

    private int left() {
        return (super.width - width()) / 2;
    }

    private int top() {
        return (super.height - height()) / 2;
    }

    private int[] cell(Essence essence) {
        int group = group(essence);
        int index = 0;
        for (Essence other : Essence.values()) {
            if (other == essence) {
                break;
            }
            if (group(other) == group) {
                index++;
            }
        }
        int row = (group == 0 ? 0 : group == 1 ? rows(0) : rows(0) + rows(1)) + index / COLUMNS;
        return new int[]{left() + PAD + index % COLUMNS * CELL, top() + PAD + TITLE + row * CELL + group * GAP};
    }

    private @Nullable Essence at(double mouseX, double mouseY) {
        for (Essence essence : Essence.values()) {
            int[] c = cell(essence);
            if (mouseX >= c[0] && mouseX < c[0] + CELL && mouseY >= c[1] && mouseY < c[1] + CELL) {
                return essence;
            }
        }
        return null;
    }

    private int[] blank() {
        return new int[]{left() + PAD, top() + height() - PAD - BLANK_H, width() - PAD * 2, BLANK_H};
    }

    private @Nullable Essence current() {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStack held = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(hand);
        return EssenceStickerItem.essence(held);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        int w = width();
        int h = height();
        PixelArt.fill(graphics, x, y, x + w, y + h, 0xFF4A3312);
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + h - 1, 0xF0120D16);
        PixelArt.fill(graphics, x + 1, y + 1, x + w - 1, y + 2, 0xFFC29445);
        Essence chosen = current();
        Essence over = at(mouseX, mouseY);
        for (Essence essence : Essence.values()) {
            int[] c = cell(essence);
            if (essence == chosen || essence == over) {
                int rim = essence == chosen ? 0xFFE9C66D : 0xFF6A5A40;
                PixelArt.fill(graphics, c[0], c[1], c[0] + CELL, c[1] + CELL, rim);
                PixelArt.fill(graphics, c[0] + 1, c[1] + 1, c[0] + CELL - 1, c[1] + CELL - 1, 0xFF120D16);
            }
            PixelArt.essence(graphics, essence, c[0] + 2, c[1] + 2, 9, PixelArt.argb(essence.color(), 1));
        }
        int[] b = blank();
        boolean overBlank = mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
        PixelArt.fill(graphics, b[0], b[1], b[0] + b[2], b[1] + b[3], overBlank ? 0xFFF8E3A0 : 0xFFC29445);
        PixelArt.fill(graphics, b[0] + 1, b[1] + 1, b[0] + b[2] - 1, b[1] + b[3] - 1, overBlank ? 0xFF2E2440 : 0xFF221B30);
        PixelArt.flush(graphics);
        graphics.centeredText(font, title, x + w / 2, y + PAD, 0xFFE9C66D);
        graphics.centeredText(font, Component.translatable("gui.hextale.sticker.blank"), b[0] + b[2] / 2, b[1] + 3, 0xFFE8E0F0);
        if (over != null) {
            graphics.setComponentTooltipForNextFrame(font, List.of(ResearchText.essence(over)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Essence essence = at(event.x(), event.y());
        int[] b = blank();
        boolean blank = event.x() >= b[0] && event.x() < b[0] + b[2] && event.y() >= b[1] && event.y() < b[1] + b[3];
        if (essence != null || blank) {
            ClientPacketDistributor.sendToServer(new ChooseStickerPayload(hand == InteractionHand.MAIN_HAND, essence == null ? -1 : essence.ordinal()));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
