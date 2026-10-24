package com.pockyl.hextale.client.screen;

import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import com.pockyl.hextale.port.client.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

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
    private static final int PAD = HexPlate.BORDER + 4;
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
        return titleHeight() + (rows(0) + rows(1) + rows(2)) * CELL + GAP * 2 + PAD * 2 + BLANK_H + 4;
    }

    /** The title wrapped to the panel's width, two lines at most. */
    private List<FormattedCharSequence> titleLines() {
        return HexPlate.wrap(font, title, width() - PAD * 2, 2);
    }

    private int titleHeight() {
        return titleLines().size() * 10 + 4;
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
        return new int[]{left() + PAD + index % COLUMNS * CELL, top() + PAD + titleHeight() + row * CELL + group * GAP};
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
    public void render(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        int w = width();
        int h = height();
        HexPlate.plate(graphics, x, y, w, h);
        Essence chosen = current();
        Essence over = at(mouseX, mouseY);
        for (Essence essence : Essence.values()) {
            int[] c = cell(essence);
            HexPlate.cell(graphics, c[0], c[1], CELL, essence == chosen, essence == over);
            PixelArt.essence(graphics, essence, c[0] + 2, c[1] + 2, 9, PixelArt.argb(essence.color(), 1));
        }
        PixelArt.flush(graphics);
        int ty = y + PAD;
        for (FormattedCharSequence line : titleLines()) {
            graphics.centeredText(font, line, x + w / 2, ty, HexPlate.TITLE);
            ty += 10;
        }
        int[] b = blank();
        boolean overBlank = mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
        HexPlate.button(graphics, font, b[0], b[1], b[2], b[3], Component.translatable("gui.hextale.sticker.blank"), overBlank, true);
        if (over != null) {
            graphics.setComponentTooltipForNextFrame(font, List.of(ResearchText.essence(over)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        boolean doubleClick = false;
        Essence essence = at(event.x(), event.y());
        int[] b = blank();
        boolean blank = event.x() >= b[0] && event.x() < b[0] + b[2] && event.y() >= b[1] && event.y() < b[1] + b[3];
        if (essence != null || blank) {
            PacketDistributor.sendToServer(new ChooseStickerPayload(hand == InteractionHand.MAIN_HAND, essence == null ? -1 : essence.ordinal()));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
            onClose();
            return true;
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }
}
