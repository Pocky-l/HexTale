package com.pockyl.hextale.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.research.OpticsStudy;
import com.pockyl.hextale.research.OpticsStudy.Type;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The optics bench of the Research Table: the item in the middle of a round bench, the researcher's lamps (white or of a
 * primal's colour) and mirrors on it, the light traced through them every frame and the hidden essences answering it
 * (see {@link OpticsStudy}). Tools are dragged out of the tray beside the bench and carried off the bench to put them
 * away; a click only picks one. They glide after the mouse, coast a little when let go and turn with the wheel (Q / E) or
 * by their knob, settling softly on every 15 degrees. The bench goes to the server whenever it settles.
 */
final class OpticsView {
    /** The bench's middle and its radius on screen, relative to the screen's top left. */
    static final int CX = 126;
    static final int CY = 141;
    private static final float SCALE = 1.1F;
    private static final int RADIUS = Math.round(OpticsStudy.BENCH * SCALE);
    private static final int AREA_X = 4;
    private static final int AREA_Y = 24;
    private static final int AREA_W = 244;
    private static final int AREA_H = 234;
    /** The tray of tools and the glass colours, on the side panel. */
    static final int TRAY_X = 258;
    static final int TRAY_Y = 63;
    static final int TILE_W = 96;
    static final int TILE_H = 30;
    static final int TILE_STEP = 102;
    static final int SWATCH_Y = 97;
    static final int SWATCH_W = 18;
    static final int SWATCH_H = 8;
    static final int SWATCH_STEP = 20;
    private static final float KNOB = 14;
    private static final float SNAP = Mth.PI / 12;
    private static final long SEND_DELAY = 120;
    private static final long APPEAR_MILLIS = 180;
    private static final long WAKE_MILLIS = 900;
    private static final int BRASS = 0xD8A64A;
    private static final int BRASS_DARK = 0x6A5226;
    private static final int WOOD = 0x4A3A1A;
    private static final int WHITE_LIGHT = 0xFFF2D8;
    private static final int WARM = 0xFFD696;

    /** A tool on the screen's copy of the bench: where it is drawn now, where it is headed, how it coasts. */
    private static final class Piece {
        final Type type;
        @Nullable
        Essence color;
        float x;
        float y;
        float tx;
        float ty;
        float vx;
        float vy;
        float a;
        float ta;
        long born;
        long dying;

        Piece(Type type, float x, float y, float a, @Nullable Essence color, long born) {
            this.type = type;
            this.x = this.tx = x;
            this.y = this.ty = y;
            this.a = this.ta = a;
            this.color = color;
            this.born = born;
        }

        OpticsStudy.Tool tool(boolean target) {
            return new OpticsStudy.Tool(type, target ? tx : x, target ? ty : y, target ? ta : a, color);
        }
    }

    private enum Grip {
        MOVE,
        TURN
    }

    private record Drag(Piece piece, Grip grip, float dx, float dy) {
    }

    private int left;
    private int top;
    private final List<Piece> pieces = new ArrayList<>();
    @Nullable
    private Piece selected;
    @Nullable
    private Drag drag;
    @Nullable
    private Item item;
    private int seenRevision = Integer.MIN_VALUE;
    private long sendAt;
    private long lastFrame;
    /** The colour new lamps shine, and the chosen lamp when one is picked; null = white light. */
    @Nullable
    private Essence glass;
    private long[] wokeAt = new long[0];
    private boolean[] wasAwake = new boolean[0];
    private final float[] light = new float[AREA_W * AREA_H * 3];

    void layout(int left, int top) {
        this.left = left;
        this.top = top;
    }

    // ---- Keeping in step with the server -------------------------------------------------------------------------

    /** Takes the server's bench when it is a new item, or when it changed and the researcher is not moving anything. */
    void sync(OpticsStudy study, long now) {
        boolean newItem = study.item() != item;
        if (newItem) {
            item = study.item();
            selected = null;
            drag = null;
            sendAt = 0;
            wokeAt = new long[study.spots().size()];
            wasAwake = new boolean[study.spots().size()];
            for (int i = 0; i < wasAwake.length; i++) {
                wasAwake[i] = study.isAwake(i);
            }
        }
        if (newItem || study.revision() != seenRevision && drag == null && sendAt == 0
                && !Arrays.equals(OpticsStudy.encode(study.tools()), OpticsStudy.encode(targets()))) {
            pieces.clear();
            for (OpticsStudy.Tool tool : study.tools()) {
                pieces.add(new Piece(tool.type(), tool.x(), tool.y(), tool.angle(), tool.color(), 0));
            }
        }
        seenRevision = study.revision();
        for (int i = 0; i < wasAwake.length && i < study.spots().size(); i++) {
            if (study.isAwake(i) && !wasAwake[i]) {
                wokeAt[i] = now;
            }
            wasAwake[i] = study.isAwake(i);
        }
    }

    private List<OpticsStudy.Tool> targets() {
        List<OpticsStudy.Tool> out = new ArrayList<>();
        for (Piece piece : pieces) {
            if (piece.dying == 0) {
                out.add(piece.tool(true));
            }
        }
        return out;
    }

    /** Sends the bench once it has settled for a moment. */
    void maybeSend(long now, Consumer<int[]> send) {
        if (sendAt != 0 && now >= sendAt && drag == null) {
            sendAt = 0;
            send.accept(OpticsStudy.encode(targets()));
        }
    }

    private void changed() {
        sendAt = Util.getMillis() + SEND_DELAY;
    }

    // ---- Motion ----------------------------------------------------------------------------------------------------

    private void step(long now) {
        float dt = lastFrame == 0 ? 16 : Math.min(50, now - lastFrame);
        lastFrame = now;
        float follow = 1 - (float) Math.pow(0.6, dt / 16.0);
        float turn = 1 - (float) Math.pow(0.72, dt / 16.0);
        float limit = OpticsStudy.BENCH - OpticsStudy.RIM_GAP;
        for (Piece piece : pieces) {
            if (drag != null && drag.piece() == piece && drag.grip() == Grip.MOVE) {
                float nx = piece.x + (piece.tx - piece.x) * follow;
                float ny = piece.y + (piece.ty - piece.y) * follow;
                piece.vx = (nx - piece.x) / dt * 16;
                piece.vy = (ny - piece.y) / dt * 16;
                piece.x = nx;
                piece.y = ny;
            } else if (Math.abs(piece.vx) + Math.abs(piece.vy) > 0.02F) {
                // Let go, a tool coasts on and slows on the felt, bouncing softly off the rim.
                piece.x += piece.vx * dt / 16;
                piece.y += piece.vy * dt / 16;
                float slow = (float) Math.pow(0.84, dt / 16.0);
                piece.vx *= slow;
                piece.vy *= slow;
                float d = Mth.sqrt(piece.x * piece.x + piece.y * piece.y);
                if (d > limit && piece.dying == 0) {
                    float ux = piece.x / d;
                    float uy = piece.y / d;
                    piece.x = ux * limit;
                    piece.y = uy * limit;
                    float dot = piece.vx * ux + piece.vy * uy;
                    piece.vx -= 1.6F * dot * ux;
                    piece.vy -= 1.6F * dot * uy;
                }
                piece.tx = piece.x;
                piece.ty = piece.y;
                if (Math.abs(piece.vx) + Math.abs(piece.vy) <= 0.02F) {
                    changed();
                }
            } else {
                piece.x += (piece.tx - piece.x) * follow;
                piece.y += (piece.ty - piece.y) * follow;
            }
            piece.a += (piece.ta - piece.a) * turn;
        }
        pieces.removeIf(piece -> piece.dying != 0 && now - piece.dying > APPEAR_MILLIS);
    }

    private List<OpticsStudy.Tool> live() {
        List<OpticsStudy.Tool> out = new ArrayList<>();
        for (Piece piece : pieces) {
            if (piece.dying == 0) {
                out.add(piece.tool(false));
            }
        }
        return out;
    }

    // ---- Drawing -----------------------------------------------------------------------------------------------------

    void render(GuiGraphicsExtractor graphics, Font font, OpticsStudy study, int mouseX, int mouseY) {
        long now = Util.getMillis();
        sync(study, now);
        step(now);
        bench(graphics);
        int cx = left + CX;
        int cy = top + CY;
        if (item == null) {
            return;
        }
        // The item, large, in the middle.
        PixelArt.flush(graphics);
        float size = OpticsStudy.ITEM_SIZE * SCALE;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx - size / 2, cy - size / 2);
        graphics.pose().scale(size / 16, size / 16);
        graphics.item(new ItemStack(item), 0, 0);
        graphics.pose().popMatrix();

        List<OpticsStudy.Beam> beams = OpticsStudy.trace(live());
        beams(graphics, beams);
        float[][] exposure = OpticsStudy.exposure(study.spots(), beams);
        spots(graphics, study, exposure, now);
        for (Piece piece : pieces) {
            tool(graphics, piece, now);
        }
    }

    /** The bench: dark felt in a brass ring with degree ticks. */
    private void bench(GuiGraphicsExtractor graphics) {
        int cx = left + CX;
        int cy = top + CY;
        PixelArt.fill(graphics, left + AREA_X, top + AREA_Y, left + AREA_X + AREA_W, top + AREA_Y + AREA_H, PixelArt.argb(0x0D0F14, 1));
        PixelArt.disc(graphics, cx, cy, RADIUS * 2, PixelArt.argb(0x151821, 1), 0);
        PixelArt.disc(graphics, cx, cy - 12, RADIUS * 2 - 60, PixelArt.argb(0x1A1E2A, 1), 0);
        PixelArt.disc(graphics, cx, cy - 20, RADIUS * 2 - 120, PixelArt.argb(0x1E2331, 1), 0);
        for (int r = 30; r < RADIUS; r += 30) {
            PixelArt.discRing(graphics, cx, cy, r * 2, PixelArt.argb(0xFFFFFF, 0.04F));
        }
        PixelArt.discRing(graphics, cx, cy, RADIUS * 2 + 4, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.discRing(graphics, cx, cy, RADIUS * 2 + 2, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.discRing(graphics, cx, cy, RADIUS * 2, PixelArt.argb(BRASS, 1));
        for (int i = 0; i < 72; i++) {
            float a = i * Mth.TWO_PI / 72;
            boolean long_ = i % 6 == 0;
            float inner = RADIUS - (long_ ? 6 : 3);
            PixelArt.line(graphics, Math.round(cx + Mth.cos(a) * inner), Math.round(cy + Mth.sin(a) * inner),
                    Math.round(cx + Mth.cos(a) * (RADIUS - 1)), Math.round(cy + Mth.sin(a) * (RADIUS - 1)),
                    PixelArt.argb(BRASS, long_ ? 0.8F : 0.35F));
        }
    }

    /**
     * The light: every beam is laid into a buffer of the bench's pixels (brighter where it is narrow, white or its
     * primal's colour), then drawn as runs of equal colour.
     */
    private void beams(GuiGraphicsExtractor graphics, List<OpticsStudy.Beam> beams) {
        Arrays.fill(light, 0);
        boolean any = false;
        for (OpticsStudy.Beam beam : beams) {
            int rgb = beam.color() == null ? WHITE_LIGHT : beam.color().color();
            float r = (rgb >> 16 & 255) / 255F;
            float g = (rgb >> 8 & 255) / 255F;
            float b = (rgb & 255) / 255F;
            float length = beam.length();
            if (length <= 0) {
                continue;
            }
            float dx = (beam.x1() - beam.x0()) / length;
            float dy = (beam.y1() - beam.y0()) / length;
            int steps = Math.max(1, Math.round(length * SCALE));
            for (int i = 0; i <= steps; i++) {
                float s = length * i / steps;
                float half = beam.width(s) * SCALE / 2;
                float intensity = Math.min(4, beam.intensity(s));
                float px = CX + (beam.x0() + dx * s) * SCALE;
                float py = CY + (beam.y0() + dy * s) * SCALE;
                int reach = (int) Math.ceil(half);
                for (int o = -reach; o <= reach; o++) {
                    float edge = Math.abs(o) / Math.max(0.5F, half);
                    if (edge > 1) {
                        continue;
                    }
                    float k = intensity * (1 - 0.6F * edge * edge) * 0.35F;
                    int x = Math.round(px - dy * o) - AREA_X;
                    int y = Math.round(py + dx * o) - AREA_Y;
                    if (x < 0 || y < 0 || x >= AREA_W || y >= AREA_H) {
                        continue;
                    }
                    int at = (y * AREA_W + x) * 3;
                    light[at] = Math.max(light[at], r * k);
                    light[at + 1] = Math.max(light[at + 1], g * k);
                    light[at + 2] = Math.max(light[at + 2], b * k);
                    any = true;
                }
            }
        }
        if (!any) {
            return;
        }
        for (int y = 0; y < AREA_H; y++) {
            int runStart = -1;
            int runColor = 0;
            for (int x = 0; x <= AREA_W; x++) {
                int color = 0;
                if (x < AREA_W) {
                    int at = (y * AREA_W + x) * 3;
                    float top = Math.max(light[at], Math.max(light[at + 1], light[at + 2]));
                    if (top > 0.02F) {
                        // Quantised so neighbours share a colour and draw as one run.
                        float alpha = Math.min(0.85F, 0.12F + top * 0.7F);
                        int qa = Math.round(alpha * 15);
                        int cr = Math.round(Math.min(1, light[at] / top) * 15) * 17;
                        int cg = Math.round(Math.min(1, light[at + 1] / top) * 15) * 17;
                        int cb = Math.round(Math.min(1, light[at + 2] / top) * 15) * 17;
                        color = (qa * 17) << 24 | cr << 16 | cg << 8 | cb;
                    }
                }
                if (color != runColor) {
                    if (runColor != 0) {
                        PixelArt.fill(graphics, left + AREA_X + runStart, top + AREA_Y + y, left + AREA_X + x, top + AREA_Y + y + 1, runColor);
                    }
                    runStart = x;
                    runColor = color;
                }
            }
        }
    }

    /**
     * The hidden essences: white light warms a spot into a soft haze its size, tinted towards its essence; a woken spot
     * flares and shows its essence.
     */
    private void spots(GuiGraphicsExtractor graphics, OpticsStudy study, float[][] exposure, long now) {
        List<OpticsStudy.Spot> spots = study.spots();
        for (int i = 0; i < spots.size(); i++) {
            OpticsStudy.Spot spot = spots.get(i);
            int x = Math.round(left + CX + spot.x() * SCALE);
            int y = Math.round(top + CY + spot.y() * SCALE);
            int diameter = Math.max(4, Math.round(spot.radius() * SCALE) * 2);
            int color = spot.essence().color();
            if (study.isAwake(i)) {
                float t = Math.min(1, (now - wokeAt[i]) / (float) WAKE_MILLIS);
                if (wokeAt[i] != 0 && t < 1) {
                    PixelArt.disc(graphics, x, y, diameter + Math.round(t * 40) * 2, PixelArt.argb(color, 0.5F * (1 - t)), 0);
                }
                float pulse = 0.5F + 0.5F * Mth.sin(now / 500.0F + i);
                PixelArt.disc(graphics, x, y, diameter, PixelArt.argb(color, 0.6F + 0.2F * pulse), PixelArt.argb(0x1A1408, 0.9F));
                PixelArt.essence(graphics, spot.essence(), x - 5, y - 5, 11, PixelArt.argb(PixelArt.mix(color, 0x1A1408, 0.7F), 1));
                continue;
            }
            float white = exposure[i][0];
            if (white > 0.05F) {
                // Found: a warm glow its size, tinted towards its essence - the hint which light will wake it.
                int tint = PixelArt.mix(WARM, color, 0.6F);
                float a = Math.min(0.5F, 0.15F + white * 0.15F);
                float breathe = 0.85F + 0.15F * Mth.sin(now / 300.0F + i);
                for (int ring = 0; ring < 3; ring++) {
                    PixelArt.disc(graphics, x, y, diameter + 6 - ring * 4, PixelArt.argb(tint, a * breathe * (0.35F + ring * 0.25F)), 0);
                }
            }
        }
    }

    private void tool(GuiGraphicsExtractor graphics, Piece piece, long now) {
        float appear = piece.born == 0 ? 1 : Math.min(1, (now - piece.born) / (float) APPEAR_MILLIS);
        float gone = piece.dying == 0 ? 1 : Math.max(0, 1 - (now - piece.dying) / (float) APPEAR_MILLIS);
        float k = (1 - (1 - appear) * (1 - appear) * (1 - appear)) * gone;
        if (k <= 0.05F) {
            return;
        }
        float x = left + CX + piece.x * SCALE;
        float y = top + CY + piece.y * SCALE;
        if (piece == selected) {
            PixelArt.discRing(graphics, Math.round(x), Math.round(y), Math.round(KNOB * SCALE) * 2, PixelArt.argb(BRASS, 0.35F));
            int kx = Math.round(x + Mth.cos(piece.a) * KNOB * SCALE);
            int ky = Math.round(y + Mth.sin(piece.a) * KNOB * SCALE);
            PixelArt.disc(graphics, kx, ky, 6, PixelArt.argb(BRASS, 1), PixelArt.argb(BRASS_DARK, 1));
        }
        drawTool(graphics, piece.type, piece.color, x, y, piece.a, k * SCALE);
    }

    /**
     * A tool drawn pixel by pixel, turned by {@code angle} and scaled by {@code scale} (screen pixels per bench unit);
     * the lamp shines along +x, the glass of the others lies along y.
     */
    static void drawTool(GuiGraphicsExtractor graphics, Type type, @Nullable Essence color, float cx, float cy, float angle, float scale) {
        float half = type.length / 2;
        float reach = (half + 3) * scale;
        float cos = Mth.cos(-angle);
        float sin = Mth.sin(-angle);
        for (int py = (int) Math.floor(cy - reach); py <= Math.ceil(cy + reach); py++) {
            for (int px = (int) Math.floor(cx - reach); px <= Math.ceil(cx + reach); px++) {
                float ox = (px + 0.5F - cx) / scale;
                float oy = (py + 0.5F - cy) / scale;
                float lx = ox * cos - oy * sin;
                float ly = ox * sin + oy * cos;
                int argb = switch (type) {
                    case LAMP -> lamp(lx, ly, color);
                    case MIRROR -> lx >= -2.2F && lx < -0.6F && Math.abs(ly) <= half + 0.8F ? PixelArt.argb(WOOD, 1)
                            : lx >= -0.6F && lx < 1.2F && Math.abs(ly) <= half ? PixelArt.argb(ly < -half * 0.3F && lx > 0.3F ? 0xFFFFFF : 0xDFE8EE, 1) : 0;
                };
                if (argb != 0) {
                    PixelArt.fill(graphics, px, py, px + 1, py + 1, argb);
                }
            }
        }
    }

    /** A brass lamp: a box with a dark knob, its glass at the front in the colour it shines. */
    private static int lamp(float lx, float ly, @Nullable Essence color) {
        if (lx >= 3 && lx <= 5.5F && Math.abs(ly) <= 3 + (lx - 3) * 0.6F) {
            return PixelArt.argb(color == null ? 0xFFF3C4 : color.color(), 1);
        }
        if (lx >= -5 && lx < 3 && Math.abs(ly) <= 4) {
            boolean rim = lx < -4.2F || lx > 2.2F || Math.abs(ly) > 3.2F;
            if (!rim && lx > -3 && lx < -0.5F && Math.abs(ly) < 1.3F) {
                return PixelArt.argb(0x3A2C12, 1);
            }
            return PixelArt.argb(rim ? BRASS_DARK : BRASS, 1);
        }
        return 0;
    }

    /** The tray on the side panel: the five tools, then the glass colours. */
    void renderTray(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Type[] types = Type.values();
        for (int i = 0; i < types.length; i++) {
            int x = left + TRAY_X + i * TILE_STEP;
            int y = top + TRAY_Y;
            boolean over = isOver(mouseX, mouseY, x, y, TILE_W, TILE_H);
            PixelArt.fill(graphics, x, y, x + TILE_W, y + TILE_H, PixelArt.argb(over ? 0x3A2E1C : 0x1E170E, 1));
            PixelArt.fill(graphics, x, y, x + TILE_W, y + 1, PixelArt.argb(BRASS, over ? 0.9F : 0.5F));
            PixelArt.fill(graphics, x, y + TILE_H - 1, x + TILE_W, y + TILE_H, PixelArt.argb(BRASS_DARK, 0.9F));
            float angle = types[i] == Type.LAMP ? 0 : Mth.PI / 4;
            drawTool(graphics, types[i], types[i] == Type.LAMP ? glass : null, x + TILE_W / 2.0F, y + TILE_H / 2.0F, angle, 1.6F);
        }
        for (int i = 0; i <= Essence.PRIMALS.size(); i++) {
            Essence primal = swatch(i);
            int x = left + TRAY_X + i * SWATCH_STEP;
            int y = top + SWATCH_Y;
            boolean chosen = primal == glass;
            PixelArt.fill(graphics, x - 1, y - 1, x + SWATCH_W + 1, y + SWATCH_H + 1, PixelArt.argb(chosen ? 0xFFF6D6 : 0x0E0A06, 1));
            PixelArt.fill(graphics, x, y, x + SWATCH_W, y + SWATCH_H, PixelArt.argb(primal == null ? 0xFFF3C4 : primal.color(), 1));
        }
    }

    /** The palette: white light first, then the nine primals. */
    @Nullable
    private static Essence swatch(int index) {
        return index == 0 ? null : Essence.PRIMALS.get(index - 1);
    }

    // ---- Tooltips ----------------------------------------------------------------------------------------------------

    @Nullable
    List<Component> tooltip(OpticsStudy study, double mouseX, double mouseY, String key) {
        Type[] types = Type.values();
        for (int i = 0; i < types.length; i++) {
            if (isOver(mouseX, mouseY, left + TRAY_X + i * TILE_STEP, top + TRAY_Y, TILE_W, TILE_H)) {
                String name = types[i].name().toLowerCase(java.util.Locale.ROOT);
                return List.of(Component.translatable(key + "tool." + name),
                        Component.translatable(key + "tool." + name + ".hint").withStyle(ChatFormatting.GRAY));
            }
        }
        for (int i = 0; i <= Essence.PRIMALS.size(); i++) {
            if (isOver(mouseX, mouseY, left + TRAY_X + i * SWATCH_STEP, top + SWATCH_Y - 1, SWATCH_W, SWATCH_H + 2)) {
                Essence primal = swatch(i);
                return List.of(primal == null ? Component.translatable(key + "white") : Component.translatable(key + "glass", ResearchText.essence(primal)));
            }
        }
        if (drag == null) {
            float bx = toBenchX(mouseX);
            float by = toBenchY(mouseY);
            List<OpticsStudy.Spot> spots = study.spots();
            for (int i = 0; i < spots.size(); i++) {
                OpticsStudy.Spot spot = spots.get(i);
                if (study.isAwake(i) && Mth.square(bx - spot.x()) + Mth.square(by - spot.y()) <= Mth.square(spot.radius())) {
                    return List.of(Component.translatable(key + "spot", ResearchText.essence(spot.essence()), spot.amount()));
                }
            }
        }
        return null;
    }

    // ---- Input -------------------------------------------------------------------------------------------------------

    private float toBenchX(double mouseX) {
        return (float) ((mouseX - left - CX) / SCALE);
    }

    private float toBenchY(double mouseY) {
        return (float) ((mouseY - top - CY) / SCALE);
    }

    private static boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    boolean overBench(double mouseX, double mouseY) {
        return isOver(mouseX, mouseY, left + AREA_X, top + AREA_Y, AREA_W, AREA_H);
    }

    @Nullable
    private Drag pick(float x, float y) {
        if (selected != null && selected.dying == 0) {
            float kx = selected.x + Mth.cos(selected.a) * KNOB;
            float ky = selected.y + Mth.sin(selected.a) * KNOB;
            if (Mth.square(x - kx) + Mth.square(y - ky) < 16) {
                return new Drag(selected, Grip.TURN, 0, 0);
            }
        }
        for (int i = pieces.size() - 1; i >= 0; i--) {
            Piece piece = pieces.get(i);
            if (piece.dying != 0) {
                continue;
            }
            float dx = x - piece.x;
            float dy = y - piece.y;
            float half = piece.type.length / 2;
            if (piece.type == Type.LAMP) {
                if (dx * dx + dy * dy < 36) {
                    return new Drag(piece, Grip.MOVE, -dx, -dy);
                }
                continue;
            }
            // Along the glass and close across it.
            float ux = Mth.cos(piece.a + Mth.HALF_PI);
            float uy = Mth.sin(piece.a + Mth.HALF_PI);
            float along = dx * ux + dy * uy;
            float across = dx * uy - dy * ux;
            if (Math.abs(along) <= half + 1.5F && Math.abs(across) <= 4) {
                return new Drag(piece, Grip.MOVE, -dx, -dy);
            }
        }
        return null;
    }

    /** @return whether the click was the bench's */
    boolean mousePressed(double mouseX, double mouseY, int button) {
        Type[] types = Type.values();
        for (int i = 0; i < types.length; i++) {
            if (isOver(mouseX, mouseY, left + TRAY_X + i * TILE_STEP, top + TRAY_Y, TILE_W, TILE_H)) {
                if (item == null || pieces.size() >= OpticsStudy.MAX_TOOLS) {
                    return true;
                }
                // A new tool comes out of the tray and follows the mouse onto the bench.
                float x = toBenchX(mouseX);
                float y = toBenchY(mouseY);
                Piece piece = new Piece(types[i], x, y, 0, types[i] == Type.LAMP ? glass : null, Util.getMillis());
                pieces.add(piece);
                selected = piece;
                drag = new Drag(piece, Grip.MOVE, 0, 0);
                return true;
            }
        }
        for (int i = 0; i <= Essence.PRIMALS.size(); i++) {
            if (isOver(mouseX, mouseY, left + TRAY_X + i * SWATCH_STEP, top + SWATCH_Y - 1, SWATCH_W, SWATCH_H + 2)) {
                // The colour of new lamps, and of the lamp picked on the bench.
                glass = swatch(i);
                if (selected != null && selected.type == Type.LAMP) {
                    selected.color = glass;
                    changed();
                }
                return true;
            }
        }
        if (!overBench(mouseX, mouseY) || item == null) {
            return false;
        }
        if (button != 0) {
            return true;
        }
        // A click picks a tool (and takes up a lamp's colour); only carrying it off the bench puts it away.
        Drag hit = pick(toBenchX(mouseX), toBenchY(mouseY));
        selected = hit == null ? null : hit.piece();
        drag = hit;
        if (hit != null && hit.piece().type == Type.LAMP) {
            glass = hit.piece().color;
        }
        return true;
    }

    void mouseDragged(double mouseX, double mouseY) {
        if (drag == null) {
            return;
        }
        Piece piece = drag.piece();
        float x = toBenchX(mouseX);
        float y = toBenchY(mouseY);
        if (drag.grip() == Grip.MOVE) {
            piece.tx = x + drag.dx();
            piece.ty = y + drag.dy();
        } else {
            float a = (float) Math.atan2(y - piece.y, x - piece.x);
            piece.ta = unwrap(piece.ta, snap(a));
        }
    }

    boolean dragging() {
        return drag != null;
    }

    void mouseReleased() {
        if (drag == null) {
            return;
        }
        Piece piece = drag.piece();
        drag = null;
        float d = Mth.sqrt(piece.tx * piece.tx + piece.ty * piece.ty);
        float limit = OpticsStudy.BENCH - OpticsStudy.RIM_GAP;
        if (d > OpticsStudy.BENCH + 4) {
            // Carried off the bench: put away.
            remove(piece);
            return;
        }
        if (d > limit) {
            piece.tx = piece.tx / d * limit;
            piece.ty = piece.ty / d * limit;
            piece.vx = 0;
            piece.vy = 0;
        }
        changed();
    }

    void scrolled(double mouseX, double mouseY, double amount) {
        Drag hit = overBench(mouseX, mouseY) ? pick(toBenchX(mouseX), toBenchY(mouseY)) : null;
        Piece piece = hit != null ? hit.piece() : selected;
        if (piece == null) {
            return;
        }
        selected = piece;
        turn(piece, amount > 0 ? -1 : 1);
    }

    /** Q / E turn the chosen tool; true if the key was the bench's. */
    boolean keyPressed(int key) {
        if (selected == null) {
            return false;
        }
        switch (key) {
            case com.mojang.blaze3d.platform.InputConstants.KEY_Q -> turn(selected, -1);
            case com.mojang.blaze3d.platform.InputConstants.KEY_E -> turn(selected, 1);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void turn(Piece piece, int steps) {
        piece.ta = Math.round(piece.ta / SNAP + steps) * SNAP;
        changed();
    }

    private void remove(Piece piece) {
        piece.dying = Util.getMillis();
        if (selected == piece) {
            selected = null;
        }
        changed();
    }

    /** Soft snap: close to a multiple of 15 degrees, the angle settles on it. */
    private static float snap(float angle) {
        float near = Math.round(angle / SNAP) * SNAP;
        return Math.abs(near - angle) < Mth.PI / 60 ? near : angle;
    }

    private static float unwrap(float previous, float next) {
        while (next - previous > Mth.PI) {
            next -= Mth.TWO_PI;
        }
        while (next - previous < -Mth.PI) {
            next += Mth.TWO_PI;
        }
        return next;
    }
}
