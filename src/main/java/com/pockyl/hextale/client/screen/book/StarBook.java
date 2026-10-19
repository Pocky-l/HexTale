package com.pockyl.hextale.client.screen.book;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopic;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Researcher's Diary: a magic book with two tabs on a dark sky. The quests tab holds the researcher's guide (see
 * {@link GuidePage}): item quests and devices still being worked out, each theme growing its own branch. The sky can be
 * dragged around and zoomed with the mouse wheel; a click opens a step's plate - a window in the middle of the sky with
 * a back arrow - and on a Research Table a step's plate lays its research on the parchment. The runes tab lists the runes
 * found so far ({@link RunesPage}). Shown by {@link StarBookScreen} in hand and inside the table's screen.
 */
public final class StarBook {
    private static final Identifier COSMOS = Hextale.id("textures/gui/cosmos.png");
    private static final Identifier FRAME = Hextale.id("textures/gui/star_book_frame.png");
    private static final int COSMOS_SIZE = 256;
    private static final int FRAME_SIZE = 48;
    private static final int FRAME_CORNER = 16;
    /** The frame's leather band; the sky starts inside it. */
    private static final int BORDER = 7;
    private static final float[] ZOOMS = {0.5F, 0.75F, 1, 1.5F, 2, 3};
    static final int BUTTON_HEIGHT = 16;
    static final int GOLD = 0xE0B85A;
    private static final int DRAG_THRESHOLD = 3;
    /** The tab chips in the sky's top left corner; the runes page starts below them. */
    static final int TAB_ROW = 22;
    private static final int TAB_WIDTH = 24;
    private static final int TAB_HEIGHT = 18;

    private enum Tab {
        QUESTS,
        RUNES
    }

    /** The tab stays as the reader left it between openings. */
    private static Tab tab = Tab.QUESTS;

    /** The sky stays where the reader left it, between openings, as long as some step can be seen there. */
    private static float cameraX;
    private static float cameraY;
    private static int zoomIndex = 2;
    private static boolean placed;

    /** Where the book is open: in hand (nothing to research) or on a table. */
    public interface Host {
        /** The research topic lying on the table, or null. */
        @Nullable
        ResearchTopic researching();

        void research(ResearchTopic topic);
    }

    @Nullable
    private final Host host;
    private final Font font;
    private int left;
    private int top;
    private int width;
    private int height;
    private final GuidePage guide;
    private final RunesPage runes;
    private final GuideEditor editor;
    private boolean dragging;
    private boolean pressed;
    private double pressX;
    private double pressY;
    private float pressCameraX;
    private float pressCameraY;

    public StarBook(@Nullable Host host) {
        this.host = host;
        this.font = Minecraft.getInstance().font;
        this.guide = new GuidePage(this);
        this.runes = new RunesPage(this);
        this.editor = new GuideEditor(this, guide);
        ResearchTopic onTable = host == null ? null : host.researching();
        if (onTable != null) {
            // On a table with a research on it: open on its step.
            tab = Tab.QUESTS;
            Quest quest = Guide.of(onTable);
            cameraX = quest.x();
            cameraY = quest.y();
            placed = true;
        } else {
            // The view is placed on the first frame, once the book knows its size.
            aimed = false;
        }
    }

    /** Whether the camera was checked against what the reader can see since the book opened. */
    private boolean aimed = true;

    /**
     * Opens on the steps the researcher can take now: a new reading, or a view that shows no visible step (empty sky),
     * moves the camera to them; otherwise the sky stays where the reader left it.
     */
    private void aim(RuneKnowledge knowledge) {
        aimed = true;
        float viewWidth = skyWidth() / zoom();
        float viewHeight = skyHeight() / zoom();
        if (!placed || !GuidePage.anyVisibleNear(knowledge, cameraX, cameraY, viewWidth, viewHeight)) {
            float[] focus = GuidePage.focus(knowledge, viewWidth, viewHeight);
            cameraX = focus[0];
            cameraY = focus[1];
            clampCamera();
            placed = true;
        }
    }

    @Nullable
    Host host() {
        return host;
    }

    /** The whole book including its frame. */
    public void layout(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    float zoom() {
        return ZOOMS[zoomIndex];
    }

    int skyLeft() {
        return left + BORDER;
    }

    int skyTop() {
        return top + BORDER;
    }

    int skyWidth() {
        return width - BORDER * 2;
    }

    int skyHeight() {
        return height - BORDER * 2;
    }

    private float centreX() {
        return skyLeft() + skyWidth() / 2.0F;
    }

    private float centreY() {
        return skyTop() + skyHeight() / 2.0F;
    }

    /** Map coordinates under a screen point. */
    float mapX(double screenX) {
        return (float) ((screenX - centreX()) / zoom() + cameraX);
    }

    float mapY(double screenY) {
        return (float) ((screenY - centreY()) / zoom() + cameraY);
    }

    private boolean overSky(double x, double y) {
        return x >= skyLeft() && x < skyLeft() + skyWidth() && y >= skyTop() && y < skyTop() + skyHeight();
    }

    // ---- Rendering ------------------------------------------------------------------------------------------------

    /** Draws the book; returns the tooltip to show on top of everything, or null. */
    @Nullable
    public List<FormattedCharSequence> render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        long now = Util.getMillis();
        RuneKnowledge knowledge = ClientKnowledge.get();
        if (!aimed && width > 0) {
            aim(knowledge);
        }
        graphics.enableScissor(skyLeft(), skyTop(), skyLeft() + skyWidth(), skyTop() + skyHeight());
        renderSpace(graphics, now);
        if (tab == Tab.RUNES) {
            List<FormattedCharSequence> tooltip = runes.render(graphics, knowledge, mouseX, mouseY, now);
            PixelArt.flush(graphics);
            List<FormattedCharSequence> tabTip = renderTabs(graphics, mouseX, mouseY);
            PixelArt.flush(graphics);
            graphics.disableScissor();
            renderFrame(graphics);
            PixelArt.flush(graphics);
            return tabTip != null ? tabTip : tooltip;
        }

        graphics.pose().pushMatrix();
        graphics.pose().translate(centreX(), centreY());
        graphics.pose().scale(zoom(), zoom());
        graphics.pose().translate(-cameraX, -cameraY);
        // While a plate is open the map waits behind it.
        boolean free = !dragging && overSky(mouseX, mouseY) && guide.selected() == null;
        Quest hovered = free ? guide.questAt(mouseX, mouseY, knowledge) : null;
        guide.renderMap(graphics, knowledge, now, hovered);
        PixelArt.flush(graphics);
        graphics.pose().popMatrix();
        guide.renderLegend(graphics, skyLeft() + 4, skyTop() + skyHeight() - 4, now);
        editor.renderHint(graphics, skyLeft() + skyWidth() - 4, skyTop() + skyHeight() - 4);

        List<FormattedCharSequence> tabTip = guide.selected() == null ? renderTabs(graphics, mouseX, mouseY) : null;
        List<FormattedCharSequence> tooltip = guide.renderPlate(graphics, knowledge, mouseX, mouseY, now);
        // The pixel art is batched: draw it while the scissor still clips it to the sky.
        PixelArt.flush(graphics);
        graphics.disableScissor();
        renderFrame(graphics);
        PixelArt.flush(graphics);
        if (tabTip != null) {
            return tabTip;
        }
        if (hovered != null && tooltip == null && !overTabs(mouseX, mouseY)) {
            tooltip = guide.tooltip(hovered, knowledge);
        }
        return tooltip;
    }

    private int tabX(int index) {
        return skyLeft() + 4 + index * (TAB_WIDTH + 3);
    }

    private int tabY() {
        return skyTop() + 3;
    }

    private boolean overTab(double x, double y, int index) {
        return x >= tabX(index) && x < tabX(index) + TAB_WIDTH && y >= tabY() && y < tabY() + TAB_HEIGHT;
    }

    /** The tab bar: a small plate in the sky's corner holding both chips; the map never shows through it. */
    private boolean overTabs(double x, double y) {
        return x >= skyLeft() && x < tabX(1) + TAB_WIDTH + 5 && y >= skyTop() && y < tabY() + TAB_HEIGHT + 4;
    }

    /** The two tab chips: dark glass with a gold rim, the open one lit; returns the tooltip of the one under the mouse. */
    @Nullable
    private List<FormattedCharSequence> renderTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack[] icons = {new ItemStack(ModItems.RESEARCHERS_DIARY.get()), new ItemStack(ModItems.RUNE_SCROLL.get())};
        String[] keys = {"book.hextale.tab.quests", "book.hextale.tab.runes"};
        List<FormattedCharSequence> tooltip = null;
        // The bar: dark leather-brown glass with a brass edge on the sides that face the sky.
        int barRight = tabX(1) + TAB_WIDTH + 5;
        int barBottom = tabY() + TAB_HEIGHT + 4;
        PixelArt.fill(graphics, skyLeft(), skyTop(), barRight, barBottom, PixelArt.argb(0x120C08, 0.96F));
        PixelArt.fill(graphics, skyLeft(), barBottom - 1, barRight, barBottom, PixelArt.argb(GOLD, 0.7F));
        PixelArt.fill(graphics, barRight - 1, skyTop(), barRight, barBottom, PixelArt.argb(GOLD, 0.7F));
        PixelArt.fill(graphics, barRight - 2, barBottom - 2, barRight + 1, barBottom + 1, PixelArt.argb(0x5CC8F2, 1));
        for (Tab each : Tab.values()) {
            int i = each.ordinal();
            int x = tabX(i);
            int y = tabY();
            boolean open = tab == each;
            boolean hovered = overTab(mouseX, mouseY, i);
            PixelArt.fill(graphics, x, y, x + TAB_WIDTH, y + TAB_HEIGHT, PixelArt.argb(open ? 0x2A2210 : hovered ? 0x1A1630 : 0x0C0A18, 0.95F));
            int rim = PixelArt.argb(open ? GOLD : hovered ? 0xC9B48A : 0x5A5080, 1);
            PixelArt.fill(graphics, x, y, x + TAB_WIDTH, y + 1, rim);
            PixelArt.fill(graphics, x, y + TAB_HEIGHT - 1, x + TAB_WIDTH, y + TAB_HEIGHT, rim);
            PixelArt.fill(graphics, x, y, x + 1, y + TAB_HEIGHT, rim);
            PixelArt.fill(graphics, x + TAB_WIDTH - 1, y, x + TAB_WIDTH, y + TAB_HEIGHT, rim);
            if (open) {
                // The open tab glows from below like a lit page.
                PixelArt.fill(graphics, x + 2, y + TAB_HEIGHT - 3, x + TAB_WIDTH - 2, y + TAB_HEIGHT - 2, PixelArt.argb(0xFFE8A0, 0.9F));
                PixelArt.fill(graphics, x + 1, y + TAB_HEIGHT, x + TAB_WIDTH - 1, y + TAB_HEIGHT + 2, PixelArt.argb(GOLD, 0.45F));
            }
            PixelArt.flush(graphics);
            graphics.item(icons[i], x + (TAB_WIDTH - 16) / 2, y + 1);
            if (hovered) {
                tooltip = List.of(Component.translatable(keys[i]).getVisualOrderText());
            }
        }
        return tooltip;
    }

    /** The nebula drifts slower than the stars of the map; three layers of small stars drift at their own speeds. */
    private void renderSpace(GuiGraphicsExtractor graphics, long now) {
        int x0 = skyLeft();
        int y0 = skyTop();
        int w = skyWidth();
        int h = skyHeight();
        PixelArt.fill(graphics, x0, y0, x0 + w, y0 + h, 0xFF06050D);
        int offsetX = Math.floorMod((int) (cameraX * 0.25F), COSMOS_SIZE);
        int offsetY = Math.floorMod((int) (cameraY * 0.25F), COSMOS_SIZE);
        for (int ty = -offsetY; ty < h; ty += COSMOS_SIZE) {
            for (int tx = -offsetX; tx < w; tx += COSMOS_SIZE) {
                PixelArt.flush(graphics);
                graphics.blit(RenderPipelines.GUI_TEXTURED, COSMOS, x0 + tx, y0 + ty, 0, 0, COSMOS_SIZE, COSMOS_SIZE, COSMOS_SIZE, COSMOS_SIZE);
            }
        }
        float[] speeds = {0.12F, 0.3F, 0.55F};
        int[] counts = {90, 60, 34};
        for (int layer = 0; layer < speeds.length; layer++) {
            float shiftX = cameraX * speeds[layer] * zoom();
            float shiftY = cameraY * speeds[layer] * zoom();
            int cell = 512;
            int count = counts[layer] * Math.max(1, w * h / (300 * 180));
            for (int i = 0; i < count; i++) {
                long seed = i * 2654435761L + layer * 40503L;
                int sx = (int) Math.floorMod((long) (hash(seed) % cell) - (long) shiftX, cell);
                int sy = (int) Math.floorMod((long) (hash(seed + 7) % cell) - (long) shiftY, cell);
                if (sx >= w || sy >= h) {
                    continue;
                }
                float twinkle = 0.55F + 0.45F * Mth.sin(now / (700.0F + hash(seed + 3) % 900) + i);
                int tint = layer == 2 ? 0xFFF6E0 : hash(seed + 5) % 3 == 0 ? 0xB8C8FF : 0xE0D8F0;
                int argb = PixelArt.argb(tint, (0.25F + 0.25F * layer) * twinkle);
                PixelArt.fill(graphics, x0 + sx, y0 + sy, x0 + sx + 1, y0 + sy + 1, argb);
                if (layer == 2 && hash(seed + 11) % 4 == 0) {
                    int dim = PixelArt.argb(tint, 0.25F * twinkle);
                    PixelArt.fill(graphics, x0 + sx - 1, y0 + sy, x0 + sx, y0 + sy + 1, dim);
                    PixelArt.fill(graphics, x0 + sx + 1, y0 + sy, x0 + sx + 2, y0 + sy + 1, dim);
                    PixelArt.fill(graphics, x0 + sx, y0 + sy - 1, x0 + sx + 1, y0 + sy, dim);
                    PixelArt.fill(graphics, x0 + sx, y0 + sy + 1, x0 + sx + 1, y0 + sy + 2, dim);
                }
            }
        }
    }

    private static long hash(long value) {
        long x = value * 0x9E3779B97F4A7C15L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 27;
        return Math.abs(x);
    }

    /** Dark glass in a gold rim with cyan corner studs: the plates of both pages. */
    static void plateBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        PixelArt.fill(graphics, x, y, x + w, y + h, 0xE0080614);
        int rim = PixelArt.argb(GOLD, 0.8F);
        PixelArt.fill(graphics, x, y, x + w, y + 1, rim);
        PixelArt.fill(graphics, x, y + h - 1, x + w, y + h, rim);
        PixelArt.fill(graphics, x, y, x + 1, y + h, rim);
        PixelArt.fill(graphics, x + w - 1, y, x + w, y + h, rim);
        for (int[] corner : new int[][]{{x, y}, {x + w - 1, y}, {x, y + h - 1}, {x + w - 1, y + h - 1}}) {
            PixelArt.fill(graphics, corner[0] - 1, corner[1] - 1, corner[0] + 2, corner[1] + 2, PixelArt.argb(0x5CC8F2, 1));
        }
    }

    /** A rune-plate button: dark glass in a glowing rim, no vanilla stone. */
    void magicButton(GuiGraphicsExtractor graphics, int x, int y, int w, Component label, boolean active, boolean hovered, long now) {
        float pulse = 0.5F + 0.5F * Mth.sin(now / 300.0F);
        int rim = !active ? 0x4A4468 : hovered ? PixelArt.mix(0x7EF0FF, 0xFFFFFF, 0.3F * pulse) : PixelArt.mix(0x2B8FB8, 0x5CC8F2, pulse);
        PixelArt.fill(graphics, x, y, x + w, y + BUTTON_HEIGHT, PixelArt.argb(hovered ? 0x123048 : 0x0C1426, 0.95F));
        PixelArt.fill(graphics, x + 1, y, x + w - 1, y + 1, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x + 1, y + BUTTON_HEIGHT - 1, x + w - 1, y + BUTTON_HEIGHT, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x, y + 1, x + 1, y + BUTTON_HEIGHT - 1, PixelArt.argb(rim, 1));
        PixelArt.fill(graphics, x + w - 1, y + 1, x + w, y + BUTTON_HEIGHT - 1, PixelArt.argb(rim, 1));
        if (active) {
            PixelArt.fill(graphics, x + 2, y + 2, x + w - 2, y + 3, PixelArt.argb(rim, 0.25F));
            PixelArt.hex(graphics, x + 7, y + BUTTON_HEIGHT / 2, 5, PixelArt.argb(rim, 1), 0);
            PixelArt.hex(graphics, x + w - 8, y + BUTTON_HEIGHT / 2, 5, PixelArt.argb(rim, 1), 0);
        }
        PixelArt.flush(graphics);
        graphics.centeredText(font, label, x + w / 2, y + 4, active ? 0xFFE8FAFF : 0xFF7A7498);
    }

    private void renderFrame(GuiGraphicsExtractor graphics) {
        int c = FRAME_CORNER;
        int mid = FRAME_SIZE - 2 * c;
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, top, 0, 0, c, c, FRAME_SIZE, FRAME_SIZE);
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, top, FRAME_SIZE - c, 0, c, c, FRAME_SIZE, FRAME_SIZE);
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, top + height - c, 0, FRAME_SIZE - c, c, c, FRAME_SIZE, FRAME_SIZE);
        PixelArt.flush(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, top + height - c, FRAME_SIZE - c, FRAME_SIZE - c, c, c, FRAME_SIZE, FRAME_SIZE);
        for (int x = left + c; x < left + width - c; x += mid) {
            int piece = Math.min(mid, left + width - c - x);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, top, c, 0, piece, c, FRAME_SIZE, FRAME_SIZE);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, x, top + height - c, c, FRAME_SIZE - c, piece, c, FRAME_SIZE, FRAME_SIZE);
        }
        for (int y = top + c; y < top + height - c; y += mid) {
            int piece = Math.min(mid, top + height - c - y);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left, y, 0, c, c, piece, FRAME_SIZE, FRAME_SIZE);
            PixelArt.flush(graphics);
            graphics.blit(RenderPipelines.GUI_TEXTURED, FRAME, left + width - c, y, FRAME_SIZE - c, c, c, piece, FRAME_SIZE, FRAME_SIZE);
        }
    }

    static Component stars(int count) {
        return Component.literal("★".repeat(count)).withColor(GOLD).append(Component.literal("★".repeat(3 - count)).withColor(0x3A3460));
    }

    // ---- Input ----------------------------------------------------------------------------------------------------

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && tab == Tab.QUESTS && guide.selected() == null && overSky(mouseX, mouseY)) {
            return editor.press(mouseX, mouseY);
        }
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !overSky(mouseX, mouseY)) {
            return false;
        }
        if ((guide.selected() == null || tab == Tab.RUNES) && overTabs(mouseX, mouseY)) {
            for (Tab each : Tab.values()) {
                if (overTab(mouseX, mouseY, each.ordinal()) && tab != each) {
                    tab = each;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.6F));
                }
            }
            return true;
        }
        if (tab == Tab.RUNES) {
            return true;
        }
        RuneKnowledge knowledge = ClientKnowledge.get();
        if (guide.selected() != null) {
            // The plate is a window over the map: a click inside works it, a click beside it goes back to the map.
            if (guide.overPlate(mouseX, mouseY, knowledge)) {
                guide.plateClicked(mouseX, mouseY, knowledge);
            } else {
                guide.close();
            }
            return true;
        }
        pressed = true;
        dragging = false;
        pressX = mouseX;
        pressY = mouseY;
        pressCameraX = cameraX;
        pressCameraY = cameraY;
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            return editor.drag(mouseX, mouseY);
        }
        if (!pressed || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!dragging && Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) >= DRAG_THRESHOLD) {
            dragging = true;
        }
        if (dragging) {
            cameraX = pressCameraX - (float) ((mouseX - pressX) / zoom());
            cameraY = pressCameraY - (float) ((mouseY - pressY) / zoom());
            clampCamera();
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            return editor.release(mouseX, mouseY);
        }
        if (!pressed || button != InputConstants.MOUSE_BUTTON_LEFT) {
            return false;
        }
        pressed = false;
        if (!dragging) {
            RuneKnowledge knowledge = ClientKnowledge.get();
            Quest quest = guide.questAt(mouseX, mouseY, knowledge);
            if (quest != null && !guide.clickable(quest, knowledge)) {
                // A step not reachable yet only shows its name.
                quest = null;
            }
            if (guide.select(quest) && quest != null) {
                sound(Guide.done(quest, knowledge) ? 1.4F : 1.1F);
            }
        }
        dragging = false;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!overSky(mouseX, mouseY) || scrollY == 0) {
            return false;
        }
        if (tab == Tab.RUNES) {
            runes.scroll(scrollY);
            return true;
        }
        if (guide.selected() != null) {
            guide.scrollPlate(scrollY);
            return true;
        }
        int next = Mth.clamp(zoomIndex + (scrollY > 0 ? 1 : -1), 0, ZOOMS.length - 1);
        if (next != zoomIndex) {
            // Zoom around the cursor: the point under it stays put.
            float beforeX = mapX(mouseX);
            float beforeY = mapY(mouseY);
            zoomIndex = next;
            cameraX += beforeX - mapX(mouseX);
            cameraY += beforeY - mapY(mouseY);
            clampCamera();
        }
        return true;
    }

    /** Escape closes an open plate before it closes the book. */
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && tab == Tab.QUESTS && guide.selected() != null) {
            guide.close();
            return true;
        }
        return tab == Tab.QUESTS && editor.keyPressed(event);
    }

    private void clampCamera() {
        cameraX = Mth.clamp(cameraX, GuidePage.minX(), GuidePage.maxX());
        cameraY = Mth.clamp(cameraY, GuidePage.minY(), GuidePage.maxY());
    }

    private static void sound(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, pitch, 0.6F));
    }
}
