package com.pockyl.hextale.client.screen.diary;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.network.NotesReadPayload;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

/**
 * The Researcher's Diary: a two-page book with bookmarks for its chapters - getting started, the simple runes, the
 * codex of complex runes, hints and notes, and milestones.
 */
public class DiaryScreen extends Screen {
    static final ResourceLocation TEXTURE = Hextale.id("textures/gui/diary.png");
    static final int SHEET_WIDTH = 512;
    static final int SHEET_HEIGHT = 256;
    static final int BOOK_WIDTH = 330;
    static final int BOOK_HEIGHT = 206;
    static final int SLOT_U = 336;
    static final int SLOT_V = 48;
    private static final int TAB_U = 336;
    private static final int TAB_V = 0;
    private static final int TAB_WIDTH = 30;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_SPACING = 25;
    private static final int TAB_TOP = 14;
    /** Page numbers and arrows sit between the text and the bottom rule of the pages. */
    private static final int FOOTER_Y = 182;

    private final String owner;
    private final DiarySection[] sections;
    private final int[] spreads;
    private final CodexSection codex = new CodexSection();
    private final HintsSection hints;
    private int section;
    private int bookLeft;
    private int bookTop;
    @Nullable
    private List<FormattedCharSequence> pendingTooltip;

    public DiaryScreen(String owner) {
        super(Component.translatable("item.hextale.researchers_diary.owned", owner));
        this.owner = owner;
        this.hints = new HintsSection(ClientKnowledge.get());
        this.sections = new DiarySection[]{new StartSection(), new SimpleRunesSection(), codex, hints, new MilestonesSection()};
        this.spreads = new int[sections.length];
        if (ClientKnowledge.get().unreadNotes() > 0) {
            section = 3;
        }
    }

    @Override
    protected void init() {
        bookLeft = (width - BOOK_WIDTH - TAB_WIDTH + 6) / 2;
        bookTop = (height - BOOK_HEIGHT) / 2;
        sections[section].opened(context(null, 0, 0));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private long seed() {
        UUID id = minecraft != null && minecraft.player != null ? minecraft.player.getUUID() : new UUID(0, owner.hashCode());
        return id.getMostSignificantBits() ^ id.getLeastSignificantBits();
    }

    private DiaryContext context(@Nullable GuiGraphics graphics, int mouseX, int mouseY) {
        return new DiaryContext(this, graphics, font, bookLeft, bookTop, ClientKnowledge.get(), owner, seed(), mouseX, mouseY);
    }

    void setPendingTooltip(List<FormattedCharSequence> lines) {
        pendingTooltip = lines;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        pendingTooltip = null;
        renderTabs(graphics, mouseX, mouseY);
        graphics.blit(TEXTURE, bookLeft, bookTop, 0, 0, BOOK_WIDTH, BOOK_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        DiaryContext context = context(graphics, mouseX, mouseY);
        DiarySection current = sections[section];
        int count = Math.max(1, current.spreads(context));
        spreads[section] = Mth.clamp(spreads[section], 0, count - 1);
        current.render(context, spreads[section], mouseX, mouseY);
        renderFooter(graphics, count, mouseX, mouseY);
        if (pendingTooltip != null) {
            graphics.renderTooltip(font, pendingTooltip, mouseX, mouseY);
        }
    }

    private void renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        InkRenderer ink;
        for (int i = 0; i < sections.length; i++) {
            boolean active = i == section;
            int x = tabX(i);
            int y = tabY(i);
            graphics.blit(TEXTURE, x, y, TAB_U, TAB_V + (active ? TAB_HEIGHT : 0), TAB_WIDTH, TAB_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
            ink = InkRenderer.begin(graphics);
            sections[i].icon(ink, x + TAB_WIDTH - 12, y + TAB_HEIGHT / 2.0F, active ? 0xF3E3B8 : 0xC8B48C);
            ink.end();
            if (mouseX >= x && mouseX < x + TAB_WIDTH && mouseY >= y && mouseY < y + TAB_HEIGHT) {
                pendingTooltip = font.split(sections[i].title(), 200);
            }
        }
    }

    private int tabX(int i) {
        return bookLeft + BOOK_WIDTH - 6 + (i == section ? 3 : 0);
    }

    private int tabY(int i) {
        return bookTop + TAB_TOP + i * TAB_SPACING;
    }

    private void renderFooter(GuiGraphics graphics, int count, int mouseX, int mouseY) {
        int spread = spreads[section];
        int y = bookTop + FOOTER_Y;
        if (count > 1) {
            Component pages = Component.literal((spread * 2 + 1) + "");
            graphics.drawString(font, pages, bookLeft + 86 - font.width(pages) / 2, y, 0xFF000000 | DiaryContext.MUTED, false);
            Component pages2 = Component.literal((spread * 2 + 2) + "");
            graphics.drawString(font, pages2, bookLeft + 244 - font.width(pages2) / 2, y, 0xFF000000 | DiaryContext.MUTED, false);
        }
        InkRenderer ink = InkRenderer.begin(graphics);
        if (spread > 0) {
            arrow(ink, bookLeft + 26, y + 4, -1, isOver(mouseX, mouseY, bookLeft + 18, y - 2, 18, 12));
        }
        if (spread < count - 1) {
            arrow(ink, bookLeft + BOOK_WIDTH - 26, y + 4, 1, isOver(mouseX, mouseY, bookLeft + BOOK_WIDTH - 36, y - 2, 18, 12));
        }
        ink.end();
    }

    private static void arrow(InkRenderer ink, float x, float y, int direction, boolean hovered) {
        int color = RuneDrawing.argb(hovered ? DiaryContext.GOLD : DiaryContext.HEADING, hovered ? 1 : 0.8F);
        float d = direction * 4;
        ink.stroke(new float[]{x - d * 1.6F, y, x + d, y}, 1.4F, color);
        ink.stroke(new float[]{x + d * 0.1F, y - 3.5F, x + d, y, x + d * 0.1F, y + 3.5F}, 1.4F, color);
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (int i = 0; i < sections.length; i++) {
            if (isOver(mouseX, mouseY, tabX(i), tabY(i), TAB_WIDTH, TAB_HEIGHT)) {
                openSection(i);
                return true;
            }
        }
        int y = bookTop + FOOTER_Y;
        DiaryContext context = context(null, (int) mouseX, (int) mouseY);
        int count = Math.max(1, sections[section].spreads(context));
        if (spreads[section] > 0 && isOver(mouseX, mouseY, bookLeft + 18, y - 2, 18, 12)) {
            turn(-1, count);
            return true;
        }
        if (spreads[section] < count - 1 && isOver(mouseX, mouseY, bookLeft + BOOK_WIDTH - 36, y - 2, 18, 12)) {
            turn(1, count);
            return true;
        }
        if (sections[section].mouseClicked(context, spreads[section], mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int count = Math.max(1, sections[section].spreads(context(null, 0, 0)));
        if (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_PAGEUP) {
            turn(-1, count);
            return true;
        }
        if (keyCode == InputConstants.KEY_RIGHT || keyCode == InputConstants.KEY_PAGEDOWN) {
            turn(1, count);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void turn(int direction, int count) {
        int next = Mth.clamp(spreads[section] + direction, 0, count - 1);
        if (next != spreads[section]) {
            spreads[section] = next;
            pageSound();
        }
    }

    private void openSection(int index) {
        if (index != section) {
            section = index;
            pageSound();
            sections[section].opened(context(null, 0, 0));
        }
    }

    /** Jumps to the codex with this rune selected (from a suggestion on the hints page). */
    void openCodex(ComplexRune rune) {
        codex.select(rune);
        openSection(2);
    }

    /** The reader has seen the notes; the "new" markers stay for this visit and are gone next time. */
    void notesSeen() {
        if (ClientKnowledge.get().unreadNotes() > 0) {
            PacketDistributor.sendToServer(NotesReadPayload.INSTANCE);
        }
    }

    private void pageSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }
}
