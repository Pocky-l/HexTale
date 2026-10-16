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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.infusion.InfusionRecipes;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.network.SteleRunePayload;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.ToolRunes;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The glyph picker of a rune stele: the codex as a 9 x 8 grid, one row per action essence (its sign on the left), one
 * column per target. Runes the researcher knows shine in their colour and can be chosen; unknown ones are dim
 * hexagons. A rune that can be engraved into tools carries a small brass mark. Click a rune to set it on the stele.
 */
public class SteleScreen extends Screen {
    private static final int TILE = 20;
    private static final int GRID_X = 30;
    private static final int GRID_Y = 34;
    private static final int PANEL_W = GRID_X + 8 * TILE + 22;
    private static final int PANEL_H = GRID_Y + 9 * TILE + 40;
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

    private boolean creative() {
        return minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        long now = Util.getMillis();
        HexGui.cosmos(graphics, left + 6, top + 6, PANEL_W - 12, PANEL_H - 12, (int) (now / 120), (int) (now / 200));
        HexGui.frame(graphics, left, top, PANEL_W, PANEL_H);
        graphics.centeredText(font, title, left + PANEL_W / 2, top + 12, 0xFFE8FAFF);
        graphics.centeredText(font, Component.translatable("gui.hextale.stele.subtitle"), left + PANEL_W / 2, top + 22, 0xFF8F88B8);

        RuneKnowledge knowledge = ClientKnowledge.get();
        boolean creative = creative();
        ComplexRune current = current();
        List<Component> tooltip = null;
        for (Essence action : Essence.values()) {
            int row = action.ordinal();
            int y = top + GRID_Y + row * TILE;
            PixelArt.essence(graphics, action, left + GRID_X - 15, y + 5, PixelArt.ICON, PixelArt.argb(action.color(), 1));
            int col = 0;
            for (Essence target : Essence.values()) {
                if (target == action) {
                    continue;
                }
                ComplexRune rune = ComplexRunes.of(action, target);
                int x = left + GRID_X + col * TILE;
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
        int bx = left + PANEL_W / 2 - 40;
        int by = top + PANEL_H - 30;
        boolean overClear = mouseX >= bx && mouseX < bx + 80 && mouseY >= by && mouseY < by + 14;
        button(graphics, bx, by, 80, Component.translatable("gui.hextale.stele.clear"), current != null, overClear);
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
        if (ToolRunes.worksOnTools(rune) && InfusionRecipes.engraving(true, rune) != null) {
            PixelArt.fill(graphics, x + TILE - 6, y + TILE - 5, x + TILE - 3, y + TILE - 4, PixelArt.argb(GOLD, 1));
            PixelArt.fill(graphics, x + TILE - 5, y + TILE - 6, x + TILE - 4, y + TILE - 3, PixelArt.argb(GOLD, 1));
        }
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
        lines.add(Component.translatable(rune.translationKey() + ".effect").withStyle(ChatFormatting.GRAY));
        if (ToolRunes.worksOnTools(rune)) {
            lines.add(Component.translatable("gui.hextale.stele.on_tools", Component.translatable(ToolRunes.translationKey(rune),
                    ToolRunes.cost(rune), Component.translatable(rune.action().translationKey()))).withColor(GOLD));
        }
        lines.add(Component.translatable("gui.hextale.stele.choose").withStyle(ChatFormatting.DARK_AQUA));
        return lines;
    }

    private void button(GuiGraphicsExtractor graphics, int x, int y, int w, Component label, boolean active, boolean hover) {
        int rim = !active ? 0x3A3460 : hover ? 0x9FE6FF : 0x5CC8F2;
        PixelArt.fill(graphics, x, y, x + w, y + 14, PixelArt.argb(0x140F24, 0.92F));
        PixelArt.fill(graphics, x, y, x + w, y + 1, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x, y + 13, x + w, y + 14, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x, y, x + 1, y + 14, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x + w - 1, y, x + w, y + 14, PixelArt.argb(rim, 1));
        PixelArt.flush(graphics);
        graphics.centeredText(font, label, x + w / 2, y + 3, active ? 0xFFE8FAFF : 0xFF7A7498);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int bx = left + PANEL_W / 2 - 40;
        int by = top + PANEL_H - 30;
        if (mx >= bx && mx < bx + 80 && my >= by && my < by + 14 && current() != null) {
            send(-1);
            return true;
        }
        int col = Mth.floor((mx - left - GRID_X) / TILE);
        int row = Mth.floor((my - top - GRID_Y) / TILE);
        if (col >= 0 && col < 8 && row >= 0 && row < 9) {
            Essence action = Essence.values()[row];
            Essence target = Essence.values()[col >= row ? col + 1 : col];
            ComplexRune rune = ComplexRunes.of(action, target);
            if (creative() || ClientKnowledge.get().knows(rune)) {
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
