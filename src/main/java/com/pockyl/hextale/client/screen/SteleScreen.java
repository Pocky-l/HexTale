package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.RuneSteleBlockEntity;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.network.SteleRunePayload;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The glyph picker of a rune stele. With an empty altar: the codex as a 9 x 8 grid, one row per action essence (its
 * sign on the left), one column per target; runes the researcher knows shine in their colour and can be chosen,
 * unknown ones are dim hexagons. With an item on the altar of the stele's circle: a tidy list of only the runes that do
 * something for that item ({@link Ritual#usesFor}), in the order the book shows them, each with its name, stars, what
 * it does on that item and its number at the researcher's stars; the wheel scrolls it. Click a rune to set it.
 */
public class SteleScreen extends Screen {
    private static final int TILE = 20;
    private static final int GRID_X = 30;
    private static final int GRID_Y = 34;
    private static final int PANEL_W = GRID_X + 8 * TILE + 15;
    private static final int LIST_W = 290;
    private static final int PANEL_H = GRID_Y + 9 * TILE + 30;
    private static final int ROW_H = 24;
    private static final int BUTTON_W = 80;
    private static final int BUTTON_H = 14;
    private static final int GOLD = 0xF0C24A;
    private static final int REFUSED = 0xFFE07A6A;

    private final BlockPos pos;
    private int top;
    /** First list row shown. */
    private int scroll;

    public SteleScreen(BlockPos pos) {
        super(Component.translatable("block.hextale.rune_stele"));
        this.pos = pos;
    }

    @Override
    protected void init() {
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

    /** The runes chosen on the other steles of this stele's circle (none without an altar). */
    private List<ComplexRune> onOtherSteles() {
        if (minecraft == null || minecraft.level == null) {
            return List.of();
        }
        HextechAltarBlockEntity altar = Ritual.altarOf(minecraft.level, pos);
        return altar == null ? List.of() : Ritual.onOtherSteles(minecraft.level, altar, pos);
    }

    /** The item on the altar of this stele's circle, or empty. */
    private ItemStack altarItem() {
        if (minecraft == null || minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        HextechAltarBlockEntity altar = Ritual.altarOf(minecraft.level, pos);
        return altar == null ? ItemStack.EMPTY : altar.center();
    }

    private boolean creative() {
        return minecraft != null && minecraft.player != null && minecraft.player.getAbilities().instabuild;
    }

    private boolean knows(ComplexRune rune) {
        return creative() || ClientKnowledge.get().knows(rune);
    }

    /** The stars the researcher would set the rune with (creative: full). */
    private int stars(ComplexRune rune) {
        RuneKnowledge knowledge = ClientKnowledge.get();
        return knowledge.knows(rune) ? knowledge.stars(rune) : 3;
    }

    private static int panelWidth(@Nullable List<Ritual.RuneUse> uses) {
        return uses == null ? PANEL_W : LIST_W;
    }

    private int left(@Nullable List<Ritual.RuneUse> uses) {
        return (width - panelWidth(uses)) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        long now = Util.getMillis();
        ItemStack altarItem = altarItem();
        List<Ritual.RuneUse> uses = Ritual.usesFor(altarItem, true);
        int w = panelWidth(uses);
        int left = left(uses);
        HexPlate.plate(graphics, left, top, w, PANEL_H);
        PixelArt.flush(graphics);
        int textWidth = w - HexPlate.BORDER * 2 - 8;
        HexPlate.centered(graphics, font, title, left + w / 2, top + 9, textWidth, HexPlate.TITLE);
        List<ComplexRune> others = uses == null ? List.of() : onOtherSteles();
        Component subtitle = uses == null ? Component.translatable("gui.hextale.stele.subtitle")
                : Component.translatable("gui.hextale.stele.for", altarItem.getHoverName());
        RuneHolder holder = RuneHolder.of(altarItem);
        if (uses != null && holder != null) {
            // How many of the item's rune slots it would fill with what all the steles show now.
            List<ComplexRune> all = new ArrayList<>(others);
            if (current() != null) {
                all.add(current());
            }
            subtitle = Component.translatable("gui.hextale.stele.for_slots", altarItem.getHoverName(),
                    Ritual.carried(altarItem, all).size(), holder.slots());
        }
        HexPlate.centered(graphics, font, subtitle, left + w / 2, top + 20, textWidth, HexPlate.MUTED);

        List<Component> tooltip = uses == null ? grid(graphics, left, mouseX, mouseY, now)
                : list(graphics, left, uses, altarItem, others, mouseX, mouseY, now);
        PixelArt.flush(graphics);
        // Clear button.
        int bx = left + (w - BUTTON_W) / 2;
        int by = buttonY();
        boolean overClear = mouseX >= bx && mouseX < bx + BUTTON_W && mouseY >= by && mouseY < by + BUTTON_H;
        HexPlate.button(graphics, font, bx, by, BUTTON_W, BUTTON_H, Component.translatable("gui.hextale.stele.clear"), overClear,
                current() != null);
        if (tooltip != null) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : tooltip) {
                lines.addAll(font.split(line, 220));
            }
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    // ---- The whole codex (empty altar) ------------------------------------------------------------------------------

    private @Nullable List<Component> grid(GuiGraphicsExtractor graphics, int left, int mouseX, int mouseY, long now) {
        ComplexRune current = current();
        List<Component> tooltip = null;
        for (Essence action : Essence.PRIMALS) {
            int row = action.ordinal();
            int y = top + GRID_Y + row * TILE;
            PixelArt.essence(graphics, action, left + GRID_X - 15, y + 5, PixelArt.ICON, PixelArt.argb(action.color(), 1));
            int col = 0;
            for (Essence target : Essence.PRIMALS) {
                if (target == action) {
                    continue;
                }
                ComplexRune rune = ComplexRunes.of(action, target);
                int x = left + GRID_X + col * TILE;
                boolean known = knows(rune);
                boolean hover = mouseX >= x && mouseX < x + TILE && mouseY >= y && mouseY < y + TILE;
                tile(graphics, rune, x, y, known, hover, rune == current, now);
                if (hover) {
                    tooltip = describe(rune, known);
                }
                col++;
            }
        }
        return tooltip;
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

    private List<Component> describe(ComplexRune rune, boolean known) {
        List<Component> lines = new ArrayList<>();
        if (!known) {
            lines.add(Component.translatable("gui.hextale.stele.unknown").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        lines.add(nameWithStars(rune, stars(rune)));
        // Only what the rune is in general - its essences and kind; what it does on a tool is for the player to find.
        lines.add(Component.translatable("rune.hextale.formula", Component.translatable(rune.action().translationKey()),
                Component.translatable(rune.target().translationKey())).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(rune.category().translationKey()).withColor(rune.color()));
        lines.add(Component.translatable("gui.hextale.stele.choose").withStyle(ChatFormatting.DARK_AQUA));
        return lines;
    }

    private static MutableComponent nameWithStars(ComplexRune rune, int stars) {
        return Component.translatable(rune.translationKey()).withColor(rune.color())
                .append(Component.literal("  " + "★".repeat(stars)).withColor(GOLD))
                .append(Component.literal("★".repeat(Math.max(0, 3 - stars))).withColor(0x3A3460));
    }

    // ---- The runes for the altar's item -----------------------------------------------------------------------------

    private int listTop() {
        return top + GRID_Y;
    }

    private int visibleRows() {
        return (buttonY() - 4 - listTop()) / ROW_H;
    }

    private int maxScroll(List<Ritual.RuneUse> uses) {
        return Math.max(0, uses.size() - visibleRows());
    }

    private @Nullable List<Component> list(GuiGraphicsExtractor graphics, int left, List<Ritual.RuneUse> uses, ItemStack center,
                                           List<ComplexRune> others, int mouseX, int mouseY, long now) {
        if (uses.isEmpty()) {
            HexPlate.centeredParagraph(graphics, font, Component.translatable("gui.hextale.stele.none"), left + LIST_W / 2,
                    listTop() + 3 * ROW_H, LIST_W - 40, 3, HexPlate.MUTED);
            return null;
        }
        scroll = Mth.clamp(scroll, 0, maxScroll(uses));
        ComplexRune current = current();
        boolean scrolls = maxScroll(uses) > 0;
        int x0 = left + HexPlate.BORDER + 4;
        int rowW = LIST_W - (HexPlate.BORDER + 4) * 2 - (scrolls ? 6 : 0);
        List<Component> tooltip = null;
        for (int i = 0; i < visibleRows() && scroll + i < uses.size(); i++) {
            Ritual.RuneUse use = uses.get(scroll + i);
            ComplexRune rune = use.rune();
            int y = listTop() + i * ROW_H;
            boolean known = knows(rune);
            boolean hover = mouseX >= x0 && mouseX < x0 + rowW && mouseY >= y && mouseY < y + ROW_H;
            boolean chosen = rune == current;
            Component refusal = chosen ? null : Ritual.steleRefusal(center, others, rune);
            if (hover || chosen) {
                PixelArt.fill(graphics, x0, y, x0 + rowW, y + ROW_H - 1, PixelArt.argb(chosen ? GOLD : rune.color(), chosen ? 0.16F : 0.1F));
            }
            if (i > 0) {
                PixelArt.fill(graphics, x0 + 4, y - 1, x0 + rowW - 4, y, PixelArt.argb(0x3A3460, 0.6F));
            }
            tile(graphics, rune, x0 + 1, y + 2, known && refusal == null, hover, chosen, now);
            PixelArt.flush(graphics);
            int tx = x0 + TILE + 6;
            int tw = rowW - TILE - 8;
            int costW = cost(graphics, use.cost(stars(rune)), x0 + rowW - 3, y + 3);
            HexPlate.line(graphics, font, firstLine(use, known), tx, y + 3, tw - costW - 4, HexPlate.TEXT, true);
            HexPlate.line(graphics, font, refusal != null ? refusal : secondLine(use), tx, y + 13, tw,
                    refusal != null ? REFUSED : HexPlate.MUTED, false);
            if (refusal != null) {
                // Cannot be chosen now: the row dims.
                PixelArt.fill(graphics, x0, y, x0 + rowW, y + ROW_H - 1, PixelArt.argb(0x0A0814, 0.45F));
                PixelArt.flush(graphics);
            }
            if (hover) {
                tooltip = explain(use, known);
                if (refusal != null) {
                    tooltip.set(tooltip.size() - 1, refusal.copy().withColor(REFUSED & 0xFFFFFF));
                }
            }
        }
        if (scrolls) {
            // A thin brass bar: where the shown rows are in the whole list.
            int barX = left + LIST_W - HexPlate.BORDER - 6;
            int y0 = listTop();
            int h = visibleRows() * ROW_H;
            PixelArt.fill(graphics, barX, y0, barX + 2, y0 + h, PixelArt.argb(0x3A3460, 0.8F));
            int thumb = Math.max(8, h * visibleRows() / uses.size());
            int ty = y0 + (h - thumb) * scroll / maxScroll(uses);
            PixelArt.fill(graphics, barX, ty, barX + 2, ty + thumb, PixelArt.argb(HexPlate.BRASS, 1));
        }
        return tooltip;
    }

    /**
     * Draws the essence the rune costs right-aligned at {@code right}: each essence's sign and its millibuckets.
     *
     * @return the width used
     */
    private int cost(GuiGraphicsExtractor graphics, EssenceMix cost, int right, int y) {
        int x = right;
        for (var entry : cost.asMap().entrySet()) {
            Component amount = Component.translatable("gui.hextale.stele.mb", entry.getValue());
            x -= font.width(amount);
            graphics.text(font, amount, x, y, 0xFF000000 | PixelArt.mix(entry.getKey().color(), 0xFFFFFF, 0.45F), true);
            x -= PixelArt.ICON + 2;
            PixelArt.essence(graphics, entry.getKey(), x, y - 1, PixelArt.ICON, PixelArt.argb(entry.getKey().color(), 1));
            PixelArt.flush(graphics);
            x -= 4;
        }
        return right - x;
    }

    /** "24 mB X, 12 mB Y" for a cost, each essence in its colour. */
    private static MutableComponent amounts(EssenceMix cost) {
        MutableComponent text = Component.empty();
        boolean first = true;
        for (var entry : cost.asMap().entrySet()) {
            if (!first) {
                text.append(", ");
            }
            first = false;
            text.append(Component.translatable("gui.hextale.stele.mb", entry.getValue()).withStyle(ChatFormatting.WHITE)).append(" ")
                    .append(Component.translatable(entry.getKey().translationKey()).withColor(entry.getKey().color()));
        }
        return text;
    }

    /** Name and stars, and on a holder the rune's number at those stars. */
    private Component firstLine(Ritual.RuneUse use, boolean known) {
        ComplexRune rune = use.rune();
        int stars = stars(rune);
        MutableComponent line = nameWithStars(rune, stars);
        if (!known) {
            line = Component.translatable(rune.translationKey()).withColor(0x6A6090)
                    .append(Component.literal("  ").append(Component.translatable("gui.hextale.stele.not_learned")).withColor(0x6A6090));
        } else if (use.holder() != null) {
            line.append(Component.literal("  " + amount(use.holder(), rune, stars)).withColor(0xFFE8E0F0));
        }
        return line;
    }

    /** What the rune does on the item, or what the rituals make with it. */
    private Component secondLine(Ritual.RuneUse use) {
        if (use.holder() != null) {
            return Component.translatable(use.holder().descriptionKey(use.rune()));
        }
        return makes(use);
    }

    private static Component makes(Ritual.RuneUse use) {
        MutableComponent names = Component.empty();
        for (int i = 0; i < use.makes().size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(use.makes().get(i).getHoverName());
        }
        return Component.translatable("gui.hextale.stele.makes", names);
    }

    private static String amount(RuneHolder holder, ComplexRune rune, int stars) {
        return Component.translatable(holder.valueKey(rune), holder.amount(rune, stars)).getString();
    }

    private List<Component> explain(Ritual.RuneUse use, boolean known) {
        ComplexRune rune = use.rune();
        List<Component> lines = new ArrayList<>();
        lines.add(known ? nameWithStars(rune, stars(rune)) : Component.translatable(rune.translationKey()).withColor(rune.color()));
        RuneHolder holder = use.holder();
        if (holder != null) {
            lines.add(Component.translatable(holder.descriptionKey(rune)).withStyle(ChatFormatting.GRAY));
            for (int stars = 1; stars <= 3; stars++) {
                lines.add(Component.literal("★".repeat(stars) + "  ").withColor(GOLD)
                        .append(Component.literal(amount(holder, rune, stars)).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(amounts(use.cost(stars))));
            }
            List<Component> clashes = new ArrayList<>();
            for (ComplexRune other : holder.takes()) {
                if (other != rune && holder.clashes(rune, other)) {
                    clashes.add(Component.translatable(other.translationKey()));
                }
            }
            if (!clashes.isEmpty()) {
                MutableComponent names = Component.empty();
                for (int i = 0; i < clashes.size(); i++) {
                    names.append(i > 0 ? Component.literal(", ") : Component.empty()).append(clashes.get(i));
                }
                lines.add(Component.translatable("book.hextale.quest.pickaxe_rune.clash", names).withStyle(ChatFormatting.RED));
            }
        }
        if (!use.makes().isEmpty()) {
            lines.add(makes(use).copy().withStyle(ChatFormatting.GRAY));
            if (holder == null) {
                lines.add(Component.translatable("gui.hextale.stele.rune_cost", amounts(use.cost(1))).withStyle(ChatFormatting.GRAY));
            }
            // The whole ritual: its own essences and its runes', drawn from the jars around the altar.
            for (int i = 0; i < use.recipes().size(); i++) {
                lines.add(Component.translatable("gui.hextale.stele.ritual_cost", use.makes().get(i).getHoverName(),
                        amounts(use.recipes().get(i).totalEssences())).withStyle(ChatFormatting.GRAY));
            }
        }
        lines.add(known ? Component.translatable("gui.hextale.stele.choose").withStyle(ChatFormatting.DARK_AQUA)
                : Component.translatable("gui.hextale.stele.unknown").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<Ritual.RuneUse> uses = Ritual.usesFor(altarItem(), true);
        if (uses != null && scrollY != 0) {
            scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll(uses));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        List<Ritual.RuneUse> uses = Ritual.usesFor(altarItem(), true);
        int w = panelWidth(uses);
        int left = left(uses);
        int bx = left + (w - BUTTON_W) / 2;
        int by = buttonY();
        if (mx >= bx && mx < bx + BUTTON_W && my >= by && my < by + BUTTON_H && current() != null) {
            send(-1);
            return true;
        }
        ComplexRune rune = uses == null ? gridRuneAt(left, mx, my) : listRuneAt(left, uses, mx, my);
        if (rune != null) {
            ItemStack center = altarItem();
            boolean refused = uses != null && rune != current() && Ritual.steleRefusal(center, onOtherSteles(), rune) != null;
            if (knows(rune) && !refused) {
                send(rune.index());
                onClose();
            } else if (refused && minecraft != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F, 0.4F));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private @Nullable ComplexRune gridRuneAt(int left, double mx, double my) {
        int col = Mth.floor((mx - left - GRID_X) / TILE);
        int row = Mth.floor((my - top - GRID_Y) / TILE);
        if (col < 0 || col >= 8 || row < 0 || row >= 9) {
            return null;
        }
        Essence action = Essence.PRIMALS.get(row);
        Essence target = Essence.PRIMALS.get(col >= row ? col + 1 : col);
        return ComplexRunes.of(action, target);
    }

    private @Nullable ComplexRune listRuneAt(int left, List<Ritual.RuneUse> uses, double mx, double my) {
        int x0 = left + HexPlate.BORDER + 4;
        if (mx < x0 || mx >= left + LIST_W - HexPlate.BORDER - 4 || my < listTop()) {
            return null;
        }
        int row = Mth.floor((my - listTop()) / ROW_H);
        int index = scroll + row;
        return row < visibleRows() && index < uses.size() ? uses.get(index).rune() : null;
    }

    private void send(int rune) {
        ClientPacketDistributor.sendToServer(new SteleRunePayload(pos, rune));
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F));
        }
    }
}
