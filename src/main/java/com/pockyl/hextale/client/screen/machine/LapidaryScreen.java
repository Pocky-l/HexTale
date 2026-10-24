package com.pockyl.hextale.client.screen.machine;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.pockyl.hextale.port.client.KeyEvent;
import com.pockyl.hextale.port.client.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.hextale.block.LapidaryBlockEntity;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.client.screen.HexPlate;
import com.pockyl.hextale.crafting.Cleavage;
import com.pockyl.hextale.menu.LapidaryMenu;
import com.pockyl.hextale.network.LapidaryStrokePayload;
import com.pockyl.hextale.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The lapidary's cutting field: the crystal drawn large over the turning lap disc. Turn it with the mouse wheel, A/D or
 * the arrow keys (it keeps turning a little and settles), hold the left mouse button or space to grind it on the disc.
 * The contact line glows green as a cleavage plane comes into line and the facet grinds flat before the eyes; grinding
 * across the planes scrapes red and cracks the crystal, and so does too much heat. Each stroke goes to the server when
 * the button is let go.
 */
public class LapidaryScreen extends AbstractContainerScreen<LapidaryMenu> {
    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics portGraphics, float portPartialTick, int portMouseX, int portMouseY) {
    }

    private static final int FIELD_X = 8;
    private static final int FIELD_Y = 18;
    private static final int FIELD_W = 186;
    private static final int FIELD_H = 132;
    /** Crystal pixels are drawn this many GUI pixels wide. */
    private static final int PX = 2;
    private static final int RADIUS = 21;
    private static final int CX = FIELD_X + FIELD_W / 2 + 6;
    private static final int CY = FIELD_Y + 58;
    private static final int DISC_Y = FIELD_Y + 106;
    private static final int GAUGE_X = FIELD_X + 6;
    private static final int GAUGE_Y = FIELD_Y + 10;
    private static final int GAUGE_H = 92;
    private static final int OUTLINE_POINTS = 28;

    /** Turning: the crystal's angle (degrees) and spin; the keys push the spin, friction settles it. */
    private float angle;
    private float spin;
    private long lastFrame = -1;
    private boolean keyLeft;
    private boolean keyRight;
    /** Grinding: since when (client ticks) the button is held, at which angle; the local copy of the cut. */
    private boolean grinding;
    private int heldTicks;
    private float strokeAngle;
    private Cleavage.@Nullable Cut preview;
    private long previewSeed = Long.MIN_VALUE;
    private int previewRevision = -1;
    /** How close the nearest uncut plane is (0-1) and whether it lies along the disc, from the last frame. */
    private float lastWarmth;
    private boolean lastAligned;

    public LapidaryScreen(LapidaryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = LapidaryMenu.WIDTH;
        this.imageHeight = LapidaryMenu.INVENTORY_Y + 82;
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelX = LapidaryMenu.INVENTORY_X;
        inventoryLabelY = LapidaryMenu.INVENTORY_Y - 11;
    }

    private @Nullable LapidaryBlockEntity lapidary() {
        return minecraft == null || minecraft.level == null ? null
                : minecraft.level.getBlockEntity(menu.pos()) instanceof LapidaryBlockEntity entity ? entity : null;
    }

    /** The cut as the server last sent it, plus the stroke being ground now. */
    private Cleavage.@Nullable Cut cut(LapidaryBlockEntity lapidary) {
        Cleavage.Cut server = lapidary.cut();
        if (server == null) {
            preview = null;
            return null;
        }
        int revision = java.util.Arrays.hashCode(server.ground) * 31 + server.cracks;
        if (preview == null || previewSeed != lapidary.seed() || !grinding && previewRevision != revision) {
            preview = server;
            previewSeed = lapidary.seed();
            previewRevision = revision;
        }
        return preview;
    }

    // ---- Ticking: grinding is counted in game ticks, like the server counts it --------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        LapidaryBlockEntity lapidary = lapidary();
        if (lapidary == null || minecraft == null) {
            return;
        }
        Cleavage.Cut cut = cut(lapidary);
        if (grinding) {
            if (cut == null || lapidary.blocked() != null || cut.over()) {
                release();
                return;
            }
            heldTicks++;
            int facets = cut.facetsCut();
            int cracks = cut.cracks;
            cut.stroke(0, strokeAngle, 1, lapidary.speed());
            boolean aligned = cut.aligned(strokeAngle, cut.grade.tolerance) >= 0;
            if (heldTicks % 4 == 0) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GRINDSTONE_USE, aligned ? 1.6F : 0.9F, 0.18F));
            }
            if (cut.facetsCut() > facets) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.5F, 0.5F));
            }
            if (cut.cracks > cracks) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(com.pockyl.hextale.registry.ModSounds.ORB_CRACK.get(), 1.3F, 0.5F));
            }
            if (cut.over()) {
                release();
            }
        } else if (cut != null) {
            cut.heat = Math.max(0, cut.heat - Cleavage.COOLING);
        }
    }

    private void startGrinding() {
        LapidaryBlockEntity lapidary = lapidary();
        if (grinding || lapidary == null || lapidary.blocked() != null || cut(lapidary) == null) {
            return;
        }
        grinding = true;
        heldTicks = 0;
        strokeAngle = angle;
        spin = 0;
    }

    private void release() {
        if (!grinding) {
            return;
        }
        grinding = false;
        if (heldTicks > 0) {
            PacketDistributor.sendToServer(new LapidaryStrokePayload(menu.pos(), strokeAngle, heldTicks));
        }
        heldTicks = 0;
        previewRevision = -1;
    }

    // ---- Input ----------------------------------------------------------------------------------------------------------

    private boolean overField(double mouseX, double mouseY) {
        return mouseX >= leftPos + FIELD_X && mouseX < leftPos + FIELD_X + FIELD_W && mouseY >= topPos + FIELD_Y
                && mouseY < topPos + FIELD_Y + FIELD_H;
    }

    @Override
    public boolean mouseClicked(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        boolean doubleClick = false;
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && overField(event.x(), event.y())) {
            startGrinding();
            return true;
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(double portX, double portY, int portButton) {
        MouseButtonEvent event = new MouseButtonEvent(portX, portY, portButton);
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && grinding) {
            release();
            return true;
        }
        return super.mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overField(mouseX, mouseY) && !grinding) {
            spin += (float) scrollY * 40;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int portKey, int portScan, int portMods) {
        KeyEvent event = new KeyEvent(portKey, portScan, portMods);
        int key = event.key();
        if (key == InputConstants.KEY_A || key == InputConstants.KEY_LEFT) {
            keyLeft = true;
            return true;
        }
        if (key == InputConstants.KEY_D || key == InputConstants.KEY_RIGHT) {
            keyRight = true;
            return true;
        }
        if (key == InputConstants.KEY_SPACE) {
            startGrinding();
            return true;
        }
        return super.keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean keyReleased(int portKey, int portScan, int portMods) {
        KeyEvent event = new KeyEvent(portKey, portScan, portMods);
        int key = event.key();
        if (key == InputConstants.KEY_A || key == InputConstants.KEY_LEFT) {
            keyLeft = false;
            return true;
        }
        if (key == InputConstants.KEY_D || key == InputConstants.KEY_RIGHT) {
            keyRight = false;
            return true;
        }
        if (key == InputConstants.KEY_SPACE) {
            release();
            return true;
        }
        return super.keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public void removed() {
        release();
        super.removed();
    }

    /** Turns the crystal by its spin; held keys speed it up gently, friction settles it (frame-rate independent). */
    private void turn() {
        long now = Util.getMillis();
        float dt = lastFrame < 0 ? 0 : Math.min(0.1F, (now - lastFrame) / 1000.0F);
        lastFrame = now;
        if (grinding) {
            return;
        }
        float push = (keyRight ? 1 : 0) - (keyLeft ? 1 : 0);
        spin += push * 160 * dt;
        spin = Mth.clamp(spin, -90, 90);
        angle = Mth.positiveModulo(angle + spin * dt, 180);
        spin *= (float) Math.pow(push != 0 ? 0.3 : 0.02, dt);
        if (Math.abs(spin) < 0.4F && push == 0) {
            spin = 0;
        }
    }

    // ---- Drawing --------------------------------------------------------------------------------------------------------

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.renderBackground(graphics.vanilla(), mouseX, mouseY, partialTick);
        turn();
        int x = leftPos;
        int y = topPos;
        HexPlate.plate(graphics, x, y, imageWidth, imageHeight);
        HexPlate.divider(graphics, x, y + LapidaryMenu.INVENTORY_Y - 15, imageWidth);
        HexPlate.well(graphics, x + FIELD_X, y + FIELD_Y, FIELD_W, FIELD_H);
        for (Slot slot : menu.slots) {
            if (slot.index == LapidaryBlockEntity.OUTPUT) {
                HexPlate.outputSlot(graphics, x + slot.x, y + slot.y);
            } else {
                HexPlate.slot(graphics, x + slot.x, y + slot.y);
            }
        }
        PixelArt.flush(graphics);
        LapidaryBlockEntity lapidary = lapidary();
        Cleavage.Cut cut = lapidary == null ? null : cut(lapidary);
        disc(graphics, x + CX, y + DISC_Y, lapidary != null && !lapidary.getItem(LapidaryBlockEntity.DISC).isEmpty(), cut);
        if (lapidary != null && cut != null) {
            crystal(graphics, x + CX, y + CY, cut, lapidary.seed());
            contact(graphics, x + CX, y + CY, cut, lapidary);
            heat(graphics, x + GAUGE_X, y + GAUGE_Y, cut);
        }
        PixelArt.flush(graphics);
    }

    /** The lap disc in perspective, its grooves turning; it spins faster under a grinding crystal. */
    private void disc(GuiGraphicsExtractor graphics, int cx, int cy, boolean present, Cleavage.@Nullable Cut cut) {
        int rx = 66;
        int ry = 11;
        long now = Util.getMillis();
        float turn = now / (grinding ? 90.0F : 400.0F);
        for (int dy = -ry; dy <= ry; dy++) {
            int half = Math.round(rx * Mth.sqrt(1 - (dy / (float) ry) * (dy / (float) ry)));
            for (int dx = -half; dx <= half; dx++) {
                float a = (float) Math.atan2(dy * rx / (float) ry, dx);
                float r = Mth.sqrt(dx * dx + (dy * rx / (float) ry) * (dy * rx / (float) ry)) / rx;
                int base = present ? (cut != null && cut.grade == Cleavage.Grade.FINISH ? 0x9AA6B8 : 0x7A7F88) : 0x2A2630;
                boolean groove = present && ((int) Math.floor((a + turn) * 6 / Mth.PI) & 1) == 0 && r > 0.25F;
                int colour = groove ? PixelArt.mix(base, 0x000000, 0.25F) : base;
                if (r > 0.92F) {
                    colour = PixelArt.mix(colour, 0x000000, 0.4F);
                } else if (dy < -ry / 2 && present) {
                    colour = PixelArt.mix(colour, 0xFFFFFF, 0.15F);
                }
                PixelArt.fill(graphics, cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, PixelArt.argb(colour, 1));
            }
        }
        PixelArt.fill(graphics, cx - 2, cy - 1, cx + 3, cy + 2, PixelArt.argb(0x3A2A1A, 1));
    }

    /**
     * The crystal: a rough outline clipped by every plane as far as it is ground (cut facets are flat, sharp edges), turned
     * by the angle, drawn in chunky pixels with light from the top left; cracks run as dark jagged lines.
     */
    private void crystal(GuiGraphicsExtractor graphics, int cx, int cy, Cleavage.Cut cut, long seed) {
        float shown = grinding ? strokeAngle : angle;
        List<float[]> outline = outline(cut, seed);
        float rad = shown * Mth.DEG_TO_RAD;
        float cos = Mth.cos(rad);
        float sin = Mth.sin(rad);
        List<float[]> turned = new ArrayList<>();
        for (float[] p : outline) {
            turned.add(new float[]{p[0] * cos - p[1] * sin, p[0] * sin + p[1] * cos});
        }
        boolean finish = cut.grade == Cleavage.Grade.FINISH;
        int dark = finish ? 0x2A7FB8 : 0x1A557F;
        int mid = finish ? 0x7AD6F6 : 0x3FA9DF;
        int light = finish ? 0xE8FBFF : 0x98E3FB;
        long now = Util.getMillis();
        for (int py = -RADIUS - 2; py <= RADIUS + 2; py++) {
            for (int px = -RADIUS - 2; px <= RADIUS + 2; px++) {
                float fx = px + 0.5F;
                float fy = py + 0.5F;
                if (!inside(turned, fx, fy)) {
                    continue;
                }
                float edge = edgeDistance(turned, fx, fy);
                float lightness = 0.5F - (fx + fy) / (RADIUS * 4.0F);
                float shimmer = 0.03F * Mth.sin(now / 500.0F + (px + py) * 0.15F);
                int colour = lightness + shimmer > 0.62F ? light : lightness + shimmer > 0.38F ? mid : dark;
                if (edge < 1.0F) {
                    colour = PixelArt.mix(colour, (fx + fy) < 0 ? 0xFFFFFF : 0x100820, 0.55F);
                }
                PixelArt.fill(graphics, cx + px * PX, cy + py * PX, cx + px * PX + PX, cy + py * PX + PX, PixelArt.argb(colour, 1));
            }
        }
        // Cracks: jagged dark lines from the edge inward, one per crack, placed by the seed.
        RandomSource random = RandomSource.create(seed ^ 0x5DEECE66DL);
        for (int i = 0; i < cut.cracks; i++) {
            float a = random.nextFloat() * Mth.TWO_PI + rad;
            float r = RADIUS * 0.9F;
            float x0 = Mth.cos(a) * r;
            float y0 = Mth.sin(a) * r;
            for (int step = 0; step < 6; step++) {
                float x1 = x0 * 0.78F + (random.nextFloat() - 0.5F) * 4;
                float y1 = y0 * 0.78F + (random.nextFloat() - 0.5F) * 4;
                PixelArt.line(graphics, cx + Math.round(x0 * PX), cy + Math.round(y0 * PX), cx + Math.round(x1 * PX), cy + Math.round(y1 * PX),
                        PixelArt.argb(0x200410, 0.9F));
                x0 = x1;
                y0 = y1;
            }
        }
    }

    /** The crystal's outline in its own frame: a rough round stone, clipped on both sides of every plane as far as ground. */
    private static List<float[]> outline(Cleavage.Cut cut, long seed) {
        RandomSource random = RandomSource.create(seed);
        List<float[]> poly = new ArrayList<>();
        for (int i = 0; i < OUTLINE_POINTS; i++) {
            float a = Mth.TWO_PI * i / OUTLINE_POINTS;
            float r = RADIUS * (0.86F + 0.14F * random.nextFloat());
            poly.add(new float[]{Mth.cos(a) * r, Mth.sin(a) * r * 1.08F});
        }
        float finalDepth = cut.grade == Cleavage.Grade.FINISH ? 0.9F : 0.84F;
        for (int i = 0; i < cut.planes.length; i++) {
            float progress = cut.ground[i] / (float) cut.grade.grindTicks;
            if (progress <= 0) {
                continue;
            }
            // The plane lies along angle p; its facets face p + 90 and p - 90.
            float normal = (cut.planes[i] + 90) * Mth.DEG_TO_RAD;
            float nx = Mth.cos(normal);
            float ny = Mth.sin(normal);
            float depth = RADIUS * Mth.lerp(progress, 1.1F, finalDepth);
            poly = clip(poly, nx, ny, depth);
            poly = clip(poly, -nx, -ny, depth);
        }
        return poly;
    }

    /** Keeps the part of the polygon where p . n <= d (Sutherland-Hodgman against one half-plane). */
    private static List<float[]> clip(List<float[]> poly, float nx, float ny, float d) {
        List<float[]> out = new ArrayList<>();
        for (int i = 0; i < poly.size(); i++) {
            float[] a = poly.get(i);
            float[] b = poly.get((i + 1) % poly.size());
            float da = a[0] * nx + a[1] * ny - d;
            float db = b[0] * nx + b[1] * ny - d;
            if (da <= 0) {
                out.add(a);
            }
            if (da * db < 0) {
                float t = da / (da - db);
                out.add(new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t});
            }
        }
        return out;
    }

    private static boolean inside(List<float[]> poly, float x, float y) {
        boolean in = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            float[] a = poly.get(i);
            float[] b = poly.get(j);
            if ((a[1] > y) != (b[1] > y) && x < (b[0] - a[0]) * (y - a[1]) / (b[1] - a[1]) + a[0]) {
                in = !in;
            }
        }
        return in;
    }

    private static float edgeDistance(List<float[]> poly, float x, float y) {
        float best = Float.MAX_VALUE;
        for (int i = 0; i < poly.size(); i++) {
            float[] a = poly.get(i);
            float[] b = poly.get((i + 1) % poly.size());
            float ex = b[0] - a[0];
            float ey = b[1] - a[1];
            float len = ex * ex + ey * ey;
            float t = len <= 0 ? 0 : Mth.clamp(((x - a[0]) * ex + (y - a[1]) * ey) / len, 0, 1);
            float dx = a[0] + ex * t - x;
            float dy = a[1] + ey * t - y;
            best = Math.min(best, dx * dx + dy * dy);
        }
        return Mth.sqrt(best);
    }

    /**
     * The contact line where the crystal meets the disc: it warms to green as an uncut plane comes into line (a master
     * disc feels it from further off), and scrapes red with sparks while grinding across the planes.
     */
    private void contact(GuiGraphicsExtractor graphics, int cx, int cy, Cleavage.Cut cut, LapidaryBlockEntity lapidary) {
        float shown = grinding ? strokeAngle : angle;
        boolean master = lapidary.getItem(LapidaryBlockEntity.DISC).is(ModItems.MASTER_CUTTING_DISC.get());
        float window = cut.grade.tolerance * (master ? 3.5F : 2.2F);
        float closest = 180;
        for (int i = 0; i < cut.planes.length; i++) {
            if (!cut.cut(i)) {
                closest = Math.min(closest, Cleavage.difference(shown, cut.planes[i]));
            }
        }
        float warmth = Mth.clamp(1 - closest / window, 0, 1);
        boolean aligned = closest <= cut.grade.tolerance;
        int lineY = DISC_Y + topPos - 10;
        float pulse = 0.5F + 0.5F * Mth.sin(Util.getMillis() / 120.0F);
        int colour = grinding && !aligned ? 0xE0503C : aligned ? 0x6CFF8A : PixelArt.mix(0x6A6A78, 0xE8D25A, warmth);
        // The contact band: three pixels thick, grey when far, warming to yellow as a plane nears, bright pulsing green
        // when one lies along the disc; arrows at its ends point at it then.
        PixelArt.fill(graphics, cx - 50, lineY - 1, cx + 50, lineY + 2, PixelArt.argb(0x0A0810, 0.8F));
        PixelArt.fill(graphics, cx - 49, lineY, cx + 49, lineY + 1, PixelArt.argb(colour, aligned ? 0.75F + 0.25F * pulse : 0.6F + 0.4F * warmth));
        if (aligned || warmth > 0.3F) {
            PixelArt.fill(graphics, cx - 49, lineY - 1, cx + 49, lineY, PixelArt.argb(colour, 0.35F * warmth + (aligned ? 0.3F * pulse : 0)));
            PixelArt.fill(graphics, cx - 49, lineY + 1, cx + 49, lineY + 2, PixelArt.argb(colour, 0.35F * warmth + (aligned ? 0.3F * pulse : 0)));
        }
        if (aligned && !grinding) {
            int bob = Math.round(2 * pulse);
            for (int k = 0; k < 4; k++) {
                PixelArt.fill(graphics, cx - 60 + k - bob, lineY - 3 + k, cx - 59 + k - bob, lineY + 4 - k, PixelArt.argb(0x6CFF8A, 1));
                PixelArt.fill(graphics, cx + 59 - k + bob, lineY - 3 + k, cx + 60 - k + bob, lineY + 4 - k, PixelArt.argb(0x6CFF8A, 1));
            }
        }
        lastWarmth = warmth;
        lastAligned = aligned;
        if (grinding) {
            RandomSource random = RandomSource.create(Util.getMillis() / 50);
            int count = aligned ? 3 : 6;
            for (int i = 0; i < count; i++) {
                int sx = cx + random.nextInt(60) - 30;
                int len = 2 + random.nextInt(6);
                int dir = random.nextBoolean() ? 1 : -1;
                PixelArt.line(graphics, sx, lineY, sx + dir * len, lineY - len / 2,
                        PixelArt.argb(aligned ? 0xA8FFC8 : 0xFFB060, 0.9F));
            }
        }
    }

    /** The heat gauge: cool blue to glowing red; at the top the crystal cracks. */
    private static void heat(GuiGraphicsExtractor graphics, int x, int y, Cleavage.Cut cut) {
        HexPlate.well(graphics, x - 1, y - 1, 8, GAUGE_H + 2);
        float share = Mth.clamp(cut.heat / Cleavage.MAX_HEAT, 0, 1);
        int filled = Math.round(GAUGE_H * share);
        for (int row = 0; row < filled; row++) {
            float t = row / (float) GAUGE_H;
            int colour = PixelArt.mix(0x3A7FD8, 0xFF4A1C, t);
            PixelArt.fill(graphics, x, y + GAUGE_H - 1 - row, x + 6, y + GAUGE_H - row, PixelArt.argb(colour, 1));
        }
        PixelArt.fill(graphics, x, y + 2, x + 6, y + 3, PixelArt.argb(0xFF4A1C, 0.8F));
    }

    @Override
    protected void renderLabels(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        HexPlate.line(graphics, font, title, titleLabelX, titleLabelY, imageWidth - 16, HexPlate.TITLE, true);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, HexPlate.TEXT, false);
        LapidaryBlockEntity lapidary = lapidary();
        if (lapidary == null) {
            return;
        }
        Cleavage.Cut cut = cut(lapidary);
        int textX = FIELD_X + 16;
        if (cut != null) {
            graphics.text(font, Component.translatable("gui.hextale.hex_lapidary.facets", cut.facetsCut(), cut.planes.length),
                    textX, FIELD_Y + 5, HexPlate.TEXT, false);
            graphics.text(font, Component.translatable("gui.hextale.hex_lapidary.cracks", cut.cracks, cut.grade.maxCracks),
                    textX, FIELD_Y + 15, cut.cracks > 0 ? 0xFFE0503C : HexPlate.MUTED, false);
        }
        String blocked = lapidary.blocked();
        if (blocked != null) {
            HexPlate.centered(graphics, font, Component.translatable("gui.hextale.hex_lapidary." + blocked), FIELD_X + FIELD_W / 2,
                    FIELD_Y + FIELD_H / 2 - 4, FIELD_W - 20, HexPlate.MUTED);
        } else if (cut != null && !grinding) {
            Component state = Component.translatable(lastAligned ? "gui.hextale.hex_lapidary.aligned"
                    : lastWarmth > 0 ? "gui.hextale.hex_lapidary.near" : "gui.hextale.hex_lapidary.turn");
            HexPlate.centered(graphics, font, state, FIELD_X + FIELD_W / 2, FIELD_Y + FIELD_H - 12, FIELD_W - 12,
                    lastAligned ? 0xFF6CFF8A : lastWarmth > 0 ? 0xFFE8D25A : HexPlate.MUTED);
        }
        if (minecraft != null && minecraft.level != null && minecraft.level.getGameTime() - lapidary.outcomeAt() < 50 && lapidary.outcome() != 0) {
            Component outcome = Component.translatable(lapidary.outcome() > 0 ? "gui.hextale.hex_lapidary.done" : "gui.hextale.hex_lapidary.shattered");
            HexPlate.centered(graphics, font, outcome, FIELD_X + FIELD_W / 2, FIELD_Y + 30, FIELD_W - 20,
                    lapidary.outcome() > 0 ? 0xFF6CFF8A : 0xFFE0503C);
        }
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics portGraphics, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsExtractor graphics = GuiGraphicsExtractor.of(portGraphics);
        super.render(graphics.vanilla(), mouseX, mouseY, partialTick);
        if (mouseX >= leftPos + GAUGE_X - 1 && mouseX < leftPos + GAUGE_X + 7 && mouseY >= topPos + GAUGE_Y && mouseY < topPos + GAUGE_Y + GAUGE_H) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.hextale.hex_lapidary.heat"),
                    Component.translatable("gui.hextale.hex_lapidary.heat.hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && hoveredSlot.index < LapidaryBlockEntity.SIZE) {
            String hint = switch (hoveredSlot.index) {
                case LapidaryBlockEntity.INPUT -> "hex_lapidary.input";
                case LapidaryBlockEntity.DISC -> "hex_lapidary.disc";
                default -> "hex_lapidary.output";
            };
            graphics.setTooltipForNextFrame(font, font.split(Component.translatable("gui.hextale.slot." + hint), 180), mouseX, mouseY);
        }
    }
}
