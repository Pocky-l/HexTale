package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.network.SteleRunePayload;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The glyph picker of a rune stele: the codex as a 9 x 8 grid, one row per action essence (its sign on the left), one
 * column per target. Runes the researcher knows shine in their colour and can be chosen; unknown ones are dim
 * hexagons. Click a rune to set it on the stele. With an item on the altar of the stele's circle, only the runes that
 * do something for that item are shown ({@link Ritual#runesFor}).
 */
public class SteleScreen extends Screen {
    private static final int TILE = 20;
    private static final int GRID_X = 30;
    private static final int GRID_Y = 34;
    private static final int PANEL_W = GRID_X + 8 * TILE + 15;
    private static final int PANEL_H = GRID_Y + 9 * TILE + 30;
    private static final int BUTTON_W = 80;
    private static final int BUTTON_H = 14;
    private static final int GOLD = 0xF0C24A;

    private final BlockPos pos;
    private int left;
    private int top;

    public SteleScreen(BlockPos pos) {
        super(Component.translatable("block.hextale.rune_stele"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Nullable
    private ComplexRune current() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof RuneSteleBlockEntity stele
                ? stele.rune() : null;
    }

    /** The item on the altar of this stele's circle, or empty. */
    private ItemStack altarItem() {
        if (minecraft == null || minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        HextechAltarBlockEntity altar = Ritual.altarOf(minecraft.level, pos);
        return altar == null ? ItemStack.EMPTY : altar.center();
    }

    /** The runes that may be chosen for the altar's item, or null when the altar is empty (any rune). */
    @Nullable
    private Set<ComplexRune> allowed() {
        return Ritual.runesFor(altarItem(), true);
    }

    private boolean creative() {
        return minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        long now = Util.getMillis();
        HexPlate.plate(graphics, left, top, PANEL_W, PANEL_H);
        PixelArt.flush(graphics);
        int textWidth = PANEL_W - HexPlate.BORDER * 2 - 8;
        HexPlate.centered(graphics, font, title, left + PANEL_W / 2, top + 9, textWidth, HexPlate.TITLE);
        ItemStack altarItem = altarItem();
        Set<ComplexRune> allowed = Ritual.runesFor(altarItem, true);
        Component subtitle = allowed == null ? Component.translatable("gui.hextale.stele.subtitle")
                : Component.translatable("gui.hextale.stele.for", altarItem.getHoverName());
        HexPlate.centered(graphics, font, subtitle, left + PANEL_W / 2, top + 20, textWidth, HexPlate.MUTED);
        if (allowed != null && allowed.isEmpty()) {
            HexPlate.paragraph(graphics, font, Component.translatable("gui.hextale.stele.none"), left + GRID_X - 15, top + GRID_Y + 4 * TILE,
                    textWidth - 10, 3, HexPlate.MUTED);
        }

        RuneKnowledge knowledge = ClientKnowledge.get();
        boolean creative = creative();
        ComplexRune current = current();
        List<Component> tooltip = null;
        for (Essence action : Essence.PRIMALS) {
            int row = action.ordinal();
            int y = top + GRID_Y + row * TILE;
            if (allowed == null || allowed.stream().anyMatch(rune -> rune.action() == action)) {
                PixelArt.essence(graphics, action, left + GRID_X - 15, y + 5, PixelArt.ICON, PixelArt.argb(action.color(), 1));
            }
            int col = 0;
            for (Essence target : Essence.PRIMALS) {
                if (target == action) {
                    continue;
                }
                ComplexRune rune = ComplexRunes.of(action, target);
                int x = left + GRID_X + col * TILE;
                if (allowed != null && !allowed.contains(rune)) {
                    // Does nothing for the item on the altar: not shown.
                    col++;
                    continue;
                }
                boolean known = creative || knowledge.knows(rune);
                boolean hover = mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE;
                tile(graphics, rune, x, y, known, hover, rune == current, now);
                if (hover) {
                    tooltip = describe(rune, known, knowledge);
                }
                col++;
            }
        }
        PixelArt.flush(graphics);
        // Clear button.
        int bx = left + (PANEL_W - BUTTON_W) / 2;
        int by = buttonY();
        boolean overClear = mouseX >= bx && mouseX < bx + BUTTON_W && mouseY >= by && mouseY < by + BUTTON_H;
        HexPlate.button(graphics, font, bx, by, BUTTON_W, BUTTON_H, Component.translatable("gui.hextale.stele.clear"), overClear,
                current != null);
        if (tooltip != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : tooltip) {
                lines.addAll(font.split(line, 200));
            }
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private void tile(GuiGraphicsExtractor graphics, ComplexRune rune, int x, int y, boolean known, boolean hover, boolean chosen, long now) {
        int cx = x + TILE / 2;
        int cy = y + TILE / 2;
        int color = rune.color();
        if (!known) {
            PixelArt.hex(graphics, cx, cy, 15, PixelArt.argb(0x1A1630, 0.85F), PixelArt.argb(0x3A3460, 1));
            return;
        }
        int rim = chosen ? GOLD : hover ? PixelArt.mix(color, 0xFFFFFF, 0.4F) : PixelArt.mix(color, 0x101018, 0.45F);
        PixelArt.hex(graphics, cx, cy, 17, PixelArt.argb(0x140F24, 0.92F), PixelArt.argb(rim, 1));
        if (chosen) {
            float pulse = 0.5F + 0.5F * Mth.sin(now / 200.0F);
            PixelArt.hex(graphics, cx, cy, 19, 0, PixelArt.argb(GOLD, 0.35F + 0.4F * pulse));
        }
        PixelArt.glyph(graphics, rune, x + 4, y + 4, 12, PixelArt.argb(hover || chosen ? PixelArt.mix(color, 0xFFFFFF, 0.25F) : color, 1), 0xFF000000);
    }

    private List<Component> describe(ComplexRune rune, boolean known, RuneKnowledge knowledge) {
        List<Component> lines = new ArrayList<>();
        if (!known) {
            lines.add(Component.translatable("gui.hextale.stele.unknown").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        int stars = knowledge.knows(rune) ? knowledge.stars(rune) : 3;
        lines.add(Component.translatable(rune.translationKey()).withColor(rune.color())
                .append(Component.literal("  " + "★".repeat(stars)).withColor(GOLD))
                .append(Component.literal("★".repeat(3 - stars)).withColor(0x3A3460)));
        // Only what the rune is in general - its essences and kind; what it does on a tool is for the player to find.
        lines.add(Component.translatable("rune.hextale.formula", Component.translatable(rune.action().translationKey()),
                Component.translatable(rune.target().translationKey())).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(rune.category().translationKey()).withColor(rune.color()));
        lines.add(Component.translatable("gui.hextale.stele.choose").withStyle(ChatFormatting.DARK_AQUA));
        return lines;
    }

    /** The clear button's top: centred between the grid and the plate's bottom frame. */
    private int buttonY() {
        int gridBottom = top + GRID_Y + 9 * TILE;
        return gridBottom + (top + PANEL_H - HexPlate.BORDER - gridBottom - BUTTON_H) / 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int bx = left + (PANEL_W - BUTTON_W) / 2;
        int by = buttonY();
        if (mx >= bx && mx < bx + BUTTON_W && my >= by && my < by + BUTTON_H && current() != null) {
            send(-1);
            return true;
        }
        int col = Mth.floor((mx - left - GRID_X) / TILE);
        int row = Mth.floor((my - top - GRID_Y) / TILE);
        if (col >= 0 && col < 8 && row >= 0 && row < 9) {
            Essence action = Essence.PRIMALS.get(row);
            Essence target = Essence.PRIMALS.get(col >= row ? col + 1 : col);
            ComplexRune rune = ComplexRunes.of(action, target);
            Set<ComplexRune> allowed = allowed();
            if ((allowed == null || allowed.contains(rune)) && (creative() || ClientKnowledge.get().knows(rune))) {
                send(rune.index());
                onClose();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void send(int rune) {
        ClientPacketDistributor.sendToServer(new SteleRunePayload(pos, rune));
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F));
        }
    }
}
