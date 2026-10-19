package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.Loupe;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * The loupe arm on the Research Table's desk: a brass telescoping boom on a swivel at the edge of the side panel, its
 * head a lens with two smaller lenses hinged beside it (x2, x4, x8). Held with the left button it follows the mouse, the
 * boom sliding out of itself; resting on an item, the lenses fold over the main one one after another while a magnified
 * view of the item opens above the head and its essences surface around it (see {@link Loupe}). Released, the arm
 * swings back onto its rest. Drawn over the slots, all in pixel art.
 */
final class LoupeView {
    /** The swivel, and where the head rests (relative to the screen's top left). */
    private static final int PIVOT_X = 414;
    private static final int PIVOT_Y = 131;
    private static final int REST_X = 350;
    private static final int REST_Y = 131;
    /** The main lens's rim radius; the boom ends at its yoke. */
    private static final int RIM = 8;
    /** Telescope tubes: length, overlap, widths outermost first. */
    private static final int TUBE = 56;
    private static final int OVERLAP = 6;
    private static final int[] TUBE_WIDTH = {4, 3, 2};
    private static final float MIN_REACH = 30;
    private static final float MAX_REACH = 3 * TUBE - 2 * OVERLAP + RIM + 2;
    /** The hinged lenses (x4, x8): diameters. */
    private static final int[] SIDE_LENS = {11, 8};
    /** The magnified view: diameter per lens swung in. */
    private static final int[] VIEW = {28, 38, 48};
    private static final float[] VIEW_SCALE = {1.25F, 1.75F, 2.25F};
    private static final int MAX_BEADS = 6;
    private static final long POP_MILLIS = 600;
    private static final long FINALE_MILLIS = 900;

    private static final int OUTLINE = 0x24180A;
    private static final int BRASS = 0xD8A64A;
    private static final int BRASS_LIGHT = 0xF6DC8E;
    private static final int BRASS_MID = 0xAE8136;
    private static final int BRASS_DARK = 0x6E4F20;
    private static final int IRON = 0x3A2C1C;
    /** Where the light comes from (up and to the left), as a unit vector in screen space. */
    private static final float LX = -0.6F;
    private static final float LY = -0.8F;
    private static final int GLASS = 0x9FD4F0;
    private static final int GLINT = 0xDDF2FF;
    private static final int HEX_BLUE = 0x3281C6;
    private static final int NIGHT = 0x0E1526;
    private static final int GOLD = 0xFFD36E;

    /** A spark flying off a bead that just showed its essence. */
    private static final class Spark {
        float x;
        float y;
        float vx;
        float vy;
        final int color;
        final long born;

        Spark(float x, float y, float vx, float vy, int color, long born) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.born = born;
        }
    }

    private int left;
    private int top;
    /** The head's centre and velocity, relative to the screen. */
    private float headX = REST_X;
    private float headY = REST_Y;
    private float velX;
    private float velY;
    private boolean held;
    private float mouseX;
    private float mouseY;
    /** Where on the head it was picked up; shrinks to nothing so the lens centres under the mouse. */
    private float grabDX;
    private float grabDY;
    /** The hinged lenses' fold: 0 parked beside the head, 1 lying over the main lens. */
    private final float[] fold = new float[2];
    private final float[] foldVel = new float[2];
    /** Sideways sway of the parked lenses, from the head's motion. */
    private float sway;

    // What the loupe looks at.
    @Nullable
    private Slot looking;
    private ItemStack lookingAt = ItemStack.EMPTY;
    private long lookStart;
    private int lensesIn;
    private boolean sent;
    private long studiedAt;
    /** The last slot told to the server, -1 = none. */
    private int toldSlot = -1;
    /** Items this screen studied, shown as known before the server's knowledge arrives. */
    private final Set<Item> studiedHere = new HashSet<>();
    /** The magnified view's diameter now, and what it shows (kept while it closes). */
    private float view;
    private float viewVel;
    private ItemStack shown = ItemStack.EMPTY;
    private final float[] beadShown = new float[MAX_BEADS];
    private final long[] beadPop = new long[MAX_BEADS];
    private final boolean[] beadBurst = new boolean[MAX_BEADS];
    private final List<Spark> sparks = new ArrayList<>();
    private final Random random = new Random();
    private long lastFrame;

    void layout(int left, int top) {
        this.left = left;
        this.top = top;
    }

    boolean held() {
        return held;
    }

    // ---- Input -------------------------------------------------------------------------------------------------------

    boolean overHead(double mx, double my) {
        return Mth.square((float) mx - (left + headX)) + Mth.square((float) my - (top + headY)) <= Mth.square(RIM + 3);
    }

    /** Picks the arm up if the press is on its head. */
    boolean grab(double mx, double my) {
        if (!overHead(mx, my)) {
            return false;
        }
        held = true;
        mouseX = (float) mx - left;
        mouseY = (float) my - top;
        grabDX = headX - mouseX;
        grabDY = headY - mouseY;
        sound(ModSounds.LOUPE_GRAB.get(), 1.1F, 0.6F);
        return true;
    }

    void drag(double mx, double my) {
        mouseX = (float) mx - left;
        mouseY = (float) my - top;
    }

    /** Lets go: the arm swings back onto its rest. */
    void release(IntConsumer tell) {
        if (!held) {
            return;
        }
        held = false;
        sound(ModSounds.LOUPE_GRAB.get(), 0.8F, 0.45F);
        look(null, tell);
    }

    // ---- Motion and looking ------------------------------------------------------------------------------------------

    /** Advances the arm by one frame: the head follows the mouse (or swings home), the boom stays within its reach. */
    private void move(float dt) {
        float tx;
        float ty;
        float k;
        float damping;
        if (held) {
            grabDX *= (float) Math.pow(0.8, dt);
            grabDY *= (float) Math.pow(0.8, dt);
            tx = mouseX + grabDX;
            ty = mouseY + grabDY;
            k = 0.35F;
            damping = 0.5F;
        } else {
            tx = REST_X;
            ty = REST_Y;
            k = 0.12F;
            damping = 0.72F;
        }
        float steps = Math.max(1, Math.round(dt));
        for (int i = 0; i < steps; i++) {
            velX = (velX + (tx - headX) * k) * damping;
            velY = (velY + (ty - headY) * k) * damping;
            headX += velX;
            headY += velY;
            float dx = headX - PIVOT_X;
            float dy = headY - PIVOT_Y;
            float d = Mth.sqrt(dx * dx + dy * dy);
            float clamped = Mth.clamp(d, MIN_REACH, MAX_REACH);
            if (d > 0.01F && clamped != d) {
                headX = PIVOT_X + dx / d * clamped;
                headY = PIVOT_Y + dy / d * clamped;
            }
            // The parked lenses lag behind sideways motion.
            float bx = dx / Math.max(1, d);
            float by = dy / Math.max(1, d);
            float across = -velX * by + velY * bx;
            sway += (Mth.clamp(across * 0.06F, -0.5F, 0.5F) - sway) * 0.25F;
            for (int l = 0; l < fold.length; l++) {
                float target = lensesIn > l + 1 ? 1 : 0;
                foldVel[l] = (foldVel[l] + (target - fold[l]) * 0.3F) * 0.62F;
                fold[l] = Mth.clamp(fold[l] + foldVel[l], -0.15F, 1.08F);
            }
            float targetView = looking == null || lookingAt.isEmpty() ? 0 : VIEW[Math.max(0, lensesIn - 1)];
            viewVel = (viewVel + (targetView - view) * 0.22F) * 0.6F;
            view = Math.max(0, view + viewVel);
        }
    }

    /** The slot under the head while it is held and has caught up with the mouse. */
    @Nullable
    private Slot slotUnder(AbstractContainerMenu menu) {
        if (!held) {
            return null;
        }
        if (Mth.square(headX - mouseX - grabDX) + Mth.square(headY - mouseY - grabDY) > 36) {
            return looking;
        }
        for (Slot slot : menu.slots) {
            if (slot.isActive() && headX >= slot.x - 1 && headX < slot.x + 17 && headY >= slot.y - 1 && headY < slot.y + 17) {
                return slot.hasItem() ? slot : null;
            }
        }
        return null;
    }

    private void look(@Nullable Slot slot, IntConsumer tell) {
        looking = slot;
        lookingAt = slot == null ? ItemStack.EMPTY : slot.getItem().copy();
        lookStart = Util.getMillis();
        lensesIn = slot == null ? 0 : 1;
        sent = false;
        int index = slot == null ? -1 : slot.index;
        if (index != toldSlot) {
            toldSlot = index;
            tell.accept(index);
        }
        if (slot != null) {
            shown = lookingAt;
            Arrays.fill(beadShown, 0);
            Arrays.fill(beadPop, 0);
            Arrays.fill(beadBurst, false);
            sound(ModSounds.LOUPE_LENS.get(), 0.85F, 0.45F);
        }
    }

    private boolean known(Item item) {
        Minecraft minecraft = Minecraft.getInstance();
        return studiedHere.contains(item) || ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * Follows what lies under the head: a new item starts the lenses over; each lens swings in after its time; the last
     * one studies an unstudied item ({@code study} gets the menu slot). Items already studied open at triple speed.
     */
    private void watch(AbstractContainerMenu menu, IntConsumer tell, IntConsumer study, long now) {
        Slot slot = slotUnder(menu);
        if (slot != looking || slot != null && !ItemStack.isSameItem(slot.getItem(), lookingAt)) {
            look(slot, tell);
        }
        if (looking == null) {
            return;
        }
        boolean studiable = Loupe.studiable(lookingAt);
        boolean known = known(lookingAt.getItem());
        float ticks = (now - lookStart) / 50.0F * (known ? 3 : 1);
        int lenses = studiable ? Loupe.lenses(ticks) : 1;
        if (lenses > lensesIn) {
            lensesIn = lenses;
            sound(ModSounds.LOUPE_LENS.get(), 0.85F + 0.12F * (lenses - 1), 0.45F);
        }
        if (studiable && !known && !sent && ticks >= Loupe.STUDY_TICKS) {
            sent = true;
            studiedAt = now;
            studiedHere.add(lookingAt.getItem());
            study.accept(looking.index);
            sound(ModSounds.LOUPE_STUDIED.get(), 1.0F, 0.6F);
        }
    }

    // ---- Drawing -----------------------------------------------------------------------------------------------------

    /**
     * Draws the arm (and advances it), and while it looks at something the magnified view with the item's essences.
     * Returns the tooltip of the resting head when the mouse is over it.
     */
    @Nullable
    List<Component> render(GuiGraphicsExtractor graphics, Font font, AbstractContainerMenu menu, int mx, int my,
                           IntConsumer tell, IntConsumer study) {
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 1 : Mth.clamp((now - lastFrame) / 16.0F, 0.25F, 4);
        lastFrame = now;
        if (held) {
            drag(mx, my);
        }
        watch(menu, tell, study, now);
        move(dt);

        float hx = left + headX;
        float hy = top + headY;
        float px = left + PIVOT_X;
        float py = top + PIVOT_Y;
        float dx = hx - px;
        float dy = hy - py;
        float d = Math.max(1, Mth.sqrt(dx * dx + dy * dy));
        float bx = dx / d;
        float by = dy / d;
        boolean hover = !held && overHead(mx, my);

        shadow(graphics, px, py, hx, hy, bx, by);
        pivot(graphics, px, py, bx, by, now);
        boom(graphics, px, py, bx, by, d - RIM - 1);
        head(graphics, hx, hy, bx, by, hover, now);
        if (view > 2 && !shown.isEmpty()) {
            magnified(graphics, font, hx, hy, now);
        }
        sparks(graphics, now);
        return hover ? List.of(Component.translatable("gui.hextale.research_table.loupe"),
                Component.translatable("gui.hextale.research_table.loupe.hint").withStyle(ChatFormatting.GRAY)) : null;
    }

    /** A soft shadow under the boom and the head; it falls further and fainter while the arm is lifted. */
    private void shadow(GuiGraphicsExtractor graphics, float px, float py, float hx, float hy, float bx, float by) {
        float lift = held ? 3.5F : 1.5F;
        int color = PixelArt.argb(0x000000, held ? 0.2F : 0.3F);
        bar(graphics, px + lift, py + lift, hx - bx * RIM + lift, hy - by * RIM + lift, 2, color);
        ellipse(graphics, hx + lift, hy + lift, 1, 0, RIM + 0.5F, RIM + 0.5F, color);
    }

    /** The swivel: a turned brass boss on an iron foot, a hextech crystal set in its top, a counterweight behind. */
    private void pivot(GuiGraphicsExtractor graphics, float px, float py, float bx, float by, long now) {
        tube(graphics, px, py, px - bx * 8, py - by * 8, bx, by, 2);
        tube(graphics, px - bx * 5, py - by * 5, px - bx * 9, py - by * 9, bx, by, 3);
        dome(graphics, px + 0.5F, py + 0.5F, 7, IRON, 1);
        dome(graphics, px, py, 5.5F, BRASS, 1);
        float pulse = 0.5F + 0.5F * Mth.sin(now / 700.0F);
        ellipse(graphics, px, py, 1, 0, 3.2F, 3.2F, PixelArt.argb(OUTLINE, 1));
        ellipse(graphics, px, py, 1, 0, 2.4F, 2.4F, PixelArt.argb(PixelArt.mix(HEX_BLUE, GLINT, 0.2F + 0.3F * pulse), 1));
        int x = Mth.floor(px);
        int y = Mth.floor(py);
        PixelArt.fill(graphics, x - 1, y - 1, x, y, PixelArt.argb(GLINT, 0.95F));
        ellipse(graphics, px, py, 1, 0, 9, 9, PixelArt.argb(HEX_BLUE, 0.04F + 0.06F * pulse));
    }

    /** A turned dome: a disc lit from the top left (bright on the lit side, dark on the far side), outlined. */
    private static void dome(GuiGraphicsExtractor graphics, float cx, float cy, float r, int base, float alpha) {
        paint(graphics, cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1, (x, y) -> {
            float dx = x - cx;
            float dy = y - cy;
            float d = Mth.sqrt(dx * dx + dy * dy);
            if (d > r + 0.5F) {
                return 0;
            }
            if (d > r - 0.5F) {
                return PixelArt.argb(OUTLINE, alpha);
            }
            float lam = d < 0.01F ? 0 : -(dx * LX + dy * LY) / r;
            return PixelArt.argb(ramp(base, lam), alpha);
        });
    }

    /** Four steps of a material lit at {@code lam} (-1 facing away from the light, 1 facing it). */
    private static int ramp(int base, float lam) {
        if (base == BRASS) {
            return lam > 0.45F ? BRASS_LIGHT : lam > -0.05F ? BRASS : lam > -0.55F ? BRASS_MID : BRASS_DARK;
        }
        return lam > 0.45F ? PixelArt.mix(base, 0xFFFFFF, 0.25F) : lam > -0.05F ? base
                : lam > -0.55F ? PixelArt.mix(base, 0x000000, 0.25F) : PixelArt.mix(base, 0x000000, 0.45F);
    }

    /** The telescope: up to three tubes slide out of each other, the thinnest last, with collars where they meet. */
    private void boom(GuiGraphicsExtractor graphics, float px, float py, float bx, float by, float length) {
        float[] from = new float[3];
        float[] to = new float[3];
        for (int i = 0; i < 3; i++) {
            from[i] = i * (TUBE - OVERLAP);
            to[i] = i == 2 ? length : Math.min(length, from[i] + TUBE);
        }
        for (int i = 2; i >= 0; i--) {
            if (to[i] - from[i] < 2) {
                continue;
            }
            float w = TUBE_WIDTH[i] / 2.0F;
            float ax = px + bx * Math.max(from[i], 4);
            float ay = py + by * Math.max(from[i], 4);
            float ex = px + bx * to[i];
            float ey = py + by * to[i];
            tube(graphics, ax, ay, ex, ey, bx, by, w);
            if (i < 2 && to[i + 1] - from[i + 1] >= 2) {
                // A collar where the next tube comes out.
                float cx = px + bx * (to[i] - 2);
                float cy = py + by * (to[i] - 2);
                tube(graphics, cx, cy, cx + bx * 2, cy + by * 2, bx, by, w + 0.75F);
            }
        }
    }

    /**
     * One tube, shaded as a cylinder lit from the top left: an outline, then dark, mid and base brass and a bright line
     * along the side that faces the light.
     */
    private static void tube(GuiGraphicsExtractor graphics, float ax, float ay, float ex, float ey, float bx, float by, float w) {
        float nx = -by;
        float ny = bx;
        if (nx * LX + ny * LY < 0) {
            nx = -nx;
            ny = -ny;
        }
        float fnx = nx;
        float fny = ny;
        float dx = ex - ax;
        float dy = ey - ay;
        float len2 = Math.max(0.0001F, dx * dx + dy * dy);
        float reach = w + 1;
        paint(graphics, Math.min(ax, ex) - reach, Math.min(ay, ey) - reach, Math.max(ax, ex) + reach, Math.max(ay, ey) + reach, (x, y) -> {
            float t = ((x - ax) * dx + (y - ay) * dy) / len2;
            if (t < 0 || t > 1) {
                return 0;
            }
            float across = (x - ax) * fnx + (y - ay) * fny;
            float k = across / w;
            if (Math.abs(k) > 1 + 1 / w) {
                return 0;
            }
            if (Math.abs(k) > 1) {
                return PixelArt.argb(OUTLINE, 1);
            }
            return PixelArt.argb(ramp(BRASS, k), 1);
        });
    }

    /**
     * The head: a yoke on the boom's end holding the main lens; the two hinged lenses beside it fold over it (seen from
     * above: they flatten to their brass edge over the hinge and open again on top). Each lens throws a small shadow
     * that falls further while it stands up, so a folding lens rises off the one below.
     */
    private void head(GuiGraphicsExtractor graphics, float hx, float hy, float bx, float by, boolean hover, long now) {
        float yx = hx - bx * RIM;
        float yy = hy - by * RIM;
        tube(graphics, yx - bx * 3, yy - by * 3, yx + bx, yy + by, bx, by, 2);
        int stacked = (fold[0] > 0.9F ? 1 : 0) + (fold[1] > 0.9F ? 1 : 0);
        lens(graphics, hx, hy, 1, 0, RIM, 1, hover, stacked, now);
        // Hinges on the rim, square to the boom.
        float nx = -by;
        float ny = bx;
        for (int l = 0; l < 2; l++) {
            float side = l == 0 ? 1 : -1;
            float ux = nx * side;
            float uy = ny * side;
            float hingeX = hx + ux * RIM;
            float hingeY = hy + uy * RIM;
            float angle = Mth.PI * Mth.clamp(fold[l], 0, 1) + sway * side * (1 - fold[l]);
            // Parked, the lens lies beside the head; folded over, its centre comes down on the head's.
            float along = Mth.cos(angle);
            float cx = hingeX + ux * RIM * along;
            float cy = hingeY + uy * RIM * along;
            float rise = Mth.sin(Mth.clamp(angle, 0, Mth.PI)) * 2.5F + (fold[l] > 0.5F ? 1 : 0);
            float r = SIDE_LENS[l] / 2.0F;
            ellipse(graphics, cx + 1 + rise, cy + 1 + rise, ux, uy, Math.max(0.8F, r * Math.abs(along)) + 0.5F, r + 0.5F,
                    PixelArt.argb(0x000000, 0.28F));
            dome(graphics, hingeX, hingeY, 1.5F, BRASS, 1);
            lens(graphics, cx, cy, ux, uy, r, Math.max(0.14F, Math.abs(along)), hover, 0, now);
        }
    }

    /**
     * A lens in a turned brass ring of radius {@code r}, foreshortened along (ux, uy) by {@code squash}: an outline, the
     * ring shaded as a rounded rim lit at the top left with a dark inner lip, then glass that is deep under the rim and
     * clears towards the far side where light passes through (deeper still with other lenses on it), a glint that drifts
     * slowly and a small second reflection. Edge-on it is only its brass edge.
     */
    private void lens(GuiGraphicsExtractor graphics, float cx, float cy, float ux, float uy, float r, float squash,
                      boolean hover, int stacked, long now) {
        float ra = Math.max(0.8F, r * squash);
        float rim = Math.max(1.5F, r * 0.25F);
        float drift = ((now % 6000) / 6000.0F - 0.5F) * 0.6F;
        paint(graphics, cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1, (x, y) -> {
            float dx = x - cx;
            float dy = y - cy;
            float a = (dx * ux + dy * uy) / (ra + 0.5F);
            float b = (-dx * uy + dy * ux) / (r + 0.5F);
            float q = Mth.sqrt(a * a + b * b);
            if (q > 1) {
                return 0;
            }
            float d = Mth.sqrt(dx * dx + dy * dy);
            float lam = d < 0.01F ? 0 : -(dx * LX + dy * LY) / d;
            float edge = 1 / (Math.min(ra, r) + 0.5F);
            if (q > 1 - edge) {
                return PixelArt.argb(OUTLINE, 1);
            }
            if (ra < 2.5F) {
                return PixelArt.argb(ramp(BRASS, lam), 1);
            }
            float inner = 1 - (rim + 1) * edge;
            if (q > inner + edge) {
                int brass = ramp(BRASS, lam);
                return PixelArt.argb(hover ? PixelArt.mix(brass, 0xFFFFFF, 0.2F) : brass, 1);
            }
            if (q > inner) {
                // The inner lip is concave: dark on the lit side, catching light on the far side.
                return PixelArt.argb(lam > 0.2F ? OUTLINE : lam < -0.4F ? BRASS_MID : BRASS_DARK, 1);
            }
            float sx = dx + (0.38F + drift * 0.3F) * r;
            float sy = dy + 0.4F * r;
            if (sx * sx + sy * sy <= Math.max(0.55F, r * r * 0.045F)) {
                return PixelArt.argb(GLINT, 0.95F);
            }
            float tx = dx - 0.36F * r;
            float ty = dy - 0.36F * r;
            if (r > 5 && tx * tx + ty * ty <= 0.6F) {
                return PixelArt.argb(GLINT, 0.5F);
            }
            float through = -lam * q / Math.max(0.01F, inner);
            int glass = through > 0.4F ? PixelArt.mix(NIGHT, GLASS, 0.72F)
                    : through > -0.15F ? PixelArt.mix(NIGHT, GLASS, 0.5F) : PixelArt.mix(NIGHT, GLASS, 0.3F);
            return PixelArt.argb(glass, 0.6F + 0.15F * stacked);
        });
    }

    /**
     * The magnified view over the head: a dark glass disc with a reticle, the item enlarged by the lenses swung in, its
     * essences surfacing as beads around the disc and a caption under it; a cone of light joins it to the lens.
     */
    private void magnified(GuiGraphicsExtractor graphics, Font font, float hx, float hy, long now) {
        int diameter = Math.round(view) / 2 * 2;
        float r = diameter / 2.0F;
        float vx = Mth.clamp(hx, left + 6 + r, left + ResearchTableMenu.IMAGE_WIDTH - 6 - r);
        float vy = hy - RIM - 12 - r;
        boolean above = vy - r - 44 >= top + 2;
        if (!above) {
            vy = hy + RIM + 12 + r;
        }
        // The cone of light from the lens.
        float cx = vx - hx;
        float cy = vy - hy;
        float cd = Math.max(1, Mth.sqrt(cx * cx + cy * cy));
        float nx = -cy / cd;
        float ny = cx / cd;
        float beam = 0.16F + 0.06F * Mth.sin(now / 300.0F);
        quad(graphics, hx + nx * (RIM - 2), hy + ny * (RIM - 2), vx + nx * (r - 2), vy + ny * (r - 2),
                vx - nx * (r - 2), vy - ny * (r - 2), hx - nx * (RIM - 2), hy - ny * (RIM - 2), PixelArt.argb(GLASS, beam));
        quad(graphics, hx + nx * (RIM - 6), hy + ny * (RIM - 6), vx + nx * (r - 10), vy + ny * (r - 10),
                vx - nx * (r - 10), vy - ny * (r - 10), hx - nx * (RIM - 6), hy - ny * (RIM - 6), PixelArt.argb(GLINT, beam * 0.6F));
        for (int side = -1; side <= 1; side += 2) {
            PixelArt.line(graphics, Math.round(hx + nx * side * (RIM - 1)), Math.round(hy + ny * side * (RIM - 1)),
                    Math.round(vx + nx * side * (r - 1)), Math.round(vy + ny * side * (r - 1)), PixelArt.argb(GLASS, 0.35F));
        }
        int x = Math.round(vx);
        int y = Math.round(vy);
        float finale = studiedAt == 0 ? 0 : Math.max(0, 1 - (now - studiedAt) / (float) FINALE_MILLIS);
        ellipse(graphics, vx + 2, vy + 2, 1, 0, r + 1.5F, r + 1.5F, PixelArt.argb(0x000000, 0.3F));
        float breathe = 0.85F + 0.15F * Mth.sin(now / 500.0F);
        int glow = PixelArt.mix(HEX_BLUE, GOLD, finale);
        float glowAlpha = (0.22F + 0.4F * finale) * breathe;
        // The view's lens: a rounded brass bezel, a dark inner lip, deep glass clearing towards the middle, a crescent of
        // reflected light at its top left and a fine reticle.
        float viewY = vy;
        paint(graphics, vx - r - 2, vy - r - 2, vx + r + 2, vy + r + 2, (px, py) -> {
            float dx = px - vx;
            float dy = py - viewY;
            float d = Mth.sqrt(dx * dx + dy * dy);
            if (d > r + 1) {
                return 0;
            }
            float lam = d < 0.01F ? 0 : -(dx * LX + dy * LY) / d;
            if (d > r) {
                return PixelArt.argb(OUTLINE, 1);
            }
            if (d > r - 3) {
                return PixelArt.argb(ramp(BRASS, lam), 1);
            }
            if (d > r - 4) {
                return PixelArt.argb(lam > 0.2F ? OUTLINE : BRASS_DARK, 1);
            }
            float k = d / (r - 4);
            if (k > 0.8F && k < 0.92F && lam > 0.6F) {
                return PixelArt.argb(PixelArt.mix(0x1B2A48, GLINT, 0.4F), 1);
            }
            boolean tick = (Math.abs(dx) < 0.5F || Math.abs(dy) < 0.5F) && k > 0.64F && k < 0.84F;
            int base = k > 0.82F ? 0x0B1120 : k > 0.55F ? 0x111B33 : 0x16243F;
            int color = PixelArt.mix(base, glow, k < 0.55F ? glowAlpha : glowAlpha * 0.4F);
            return PixelArt.argb(tick ? PixelArt.mix(color, GLASS, 0.35F) : color, 1);
        });
        if (finale > 0) {
            float k = 1 - finale;
            int ring = Math.round(diameter + 4 + k * 30) / 2 * 2;
            PixelArt.discRing(graphics, x, y, ring, PixelArt.argb(GOLD, 0.8F * finale));
            PixelArt.discRing(graphics, x, y, ring - 4, PixelArt.argb(GOLD, 0.3F * finale));
        }

        // The item, as large as the lenses make it (and the view has opened to).
        int step = Math.max(0, Math.min(VIEW.length - 1, lensesIn - 1));
        float scale = Math.min(VIEW_SCALE[step], (diameter - 10) / 16.0F);
        if (scale > 0.5F) {
            PixelArt.flush(graphics);
            float bob = Mth.sin(now / 650.0F) * 0.8F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(vx - 8 * scale, vy - 8 * scale + bob);
            graphics.pose().scale(scale, scale);
            graphics.item(shown, 0, 0);
            graphics.pose().popMatrix();
        }
        beads(graphics, vx, vy, r, above, now);
        caption(graphics, font, vx, vy, r, above);
    }

    /** An enamel bead: a dark glassy face in a rim of the essence's colour, lit from the top left. */
    private static void bead(GuiGraphicsExtractor graphics, float cx, float cy, float r, int color) {
        paint(graphics, cx - r - 1, cy - r - 1, cx + r + 1, cy + r + 1, (x, y) -> {
            float dx = x - cx;
            float dy = y - cy;
            float d = Mth.sqrt(dx * dx + dy * dy);
            if (d > r + 0.5F) {
                return 0;
            }
            float lam = d < 0.01F ? 0 : -(dx * LX + dy * LY) / d;
            if (d > r - 0.5F) {
                return PixelArt.argb(PixelArt.mix(color, 0x000000, 0.55F), 1);
            }
            if (d > r - 1.5F) {
                return PixelArt.argb(ramp(color, lam), 1);
            }
            return PixelArt.argb(lam < -0.3F && d > r * 0.5F ? 0x16203A : NIGHT, 1);
        });
    }

    /** The item's essences, strongest first. */
    private List<Map.Entry<Essence, Integer>> essences() {
        ItemEssences essences = ResearchItems.of(shown);
        if (essences == null) {
            return List.of();
        }
        List<Map.Entry<Essence, Integer>> list = new ArrayList<>(essences.essences().entrySet());
        list.sort(Map.Entry.<Essence, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(e -> e.getKey().ordinal()));
        return list.subList(0, Math.min(MAX_BEADS, list.size()));
    }

    /**
     * Beads on an arc over the view: nothing through the first lens, faint glimmers through the second, glimmers taking
     * their essence's tint through the third; once the item is studied each opens into its essence's sign with a pop.
     */
    private void beads(GuiGraphicsExtractor graphics, float vx, float vy, float r, boolean above, long now) {
        if (!Loupe.studiable(shown)) {
            return;
        }
        List<Map.Entry<Essence, Integer>> list = essences();
        boolean known = known(shown.getItem());
        float radius = r + 8;
        for (int i = 0; i < list.size(); i++) {
            Essence essence = list.get(i).getKey();
            float a = (above ? -Mth.HALF_PI : Mth.HALF_PI) + (i - (list.size() - 1) / 2.0F) * 0.5F;
            float x = vx + Mth.cos(a) * radius;
            float y = vy + Mth.sin(a) * radius;
            float target = known ? 1 : lensesIn >= 2 ? 0.5F : 0;
            if (beadShown[i] < 1 && target >= 1 && beadPop[i] == 0) {
                beadPop[i] = now + i * 70L;
            }
            beadShown[i] += (target - beadShown[i]) * 0.2F;
            int color = essence.color();
            if (target >= 1 && now >= beadPop[i]) {
                float pop = Math.max(0, 1 - (now - beadPop[i]) / (float) POP_MILLIS);
                if (!beadBurst[i]) {
                    beadBurst[i] = true;
                    burst(x, y, color, now);
                }
                float size = 5.5F + pop * 2;
                ellipse(graphics, x, y, 1, 0, size + 3, size + 3, PixelArt.argb(color, 0.12F + 0.3F * pop));
                ellipse(graphics, x + 1, y + 1.5F, 1, 0, size + 0.5F, size + 0.5F, PixelArt.argb(0x000000, 0.3F));
                bead(graphics, x, y, size, color);
                PixelArt.essence(graphics, essence, Math.round(x) - 4, Math.round(y) - 4, PixelArt.ICON, PixelArt.argb(color, 1));
            } else if (beadShown[i] > 0.05F) {
                float twinkle = 0.6F + 0.4F * Mth.sin(now / 180.0F + i * 1.9F);
                int tint = PixelArt.mix(GLINT, color, lensesIn >= 3 ? 0.55F : 0.15F);
                float alpha = Math.min(1, beadShown[i] * 2) * twinkle;
                PixelArt.disc(graphics, Math.round(x), Math.round(y), 7, PixelArt.argb(tint, 0.3F * alpha), 0);
                PixelArt.fill(graphics, Math.round(x) - 1, Math.round(y) - 1, Math.round(x) + 1, Math.round(y) + 1, PixelArt.argb(GLINT, alpha));
                if (twinkle > 0.9F) {
                    PixelArt.fill(graphics, Math.round(x) - 3, Math.round(y), Math.round(x) + 3, Math.round(y) + 1, PixelArt.argb(tint, alpha * 0.6F));
                    PixelArt.fill(graphics, Math.round(x), Math.round(y) - 3, Math.round(x) + 1, Math.round(y) + 3, PixelArt.argb(tint, alpha * 0.6F));
                }
            }
        }
    }

    /**
     * Beyond the view and its beads (away from the head): the item's name, then its essences and strengths once known,
     * else what the loupe is doing.
     */
    private void caption(GuiGraphicsExtractor graphics, Font font, float vx, float vy, float r, boolean above) {
        Component name = ResearchText.item(shown.getItem());
        boolean studiable = Loupe.studiable(shown);
        boolean known = studiable && known(shown.getItem());
        List<Map.Entry<Essence, Integer>> list = known ? essences() : List.of();
        Component status = !studiable ? Component.translatable("gui.hextale.research_table.loupe.nothing")
                : known ? null : Component.translatable("gui.hextale.research_table.loupe.looking");
        int row = 0;
        for (Map.Entry<Essence, Integer> e : list) {
            row += 10 + font.width("×" + e.getValue()) + 4;
        }
        int width = Math.min(200, Math.max(font.width(name), status == null ? row - 4 : font.width(status)) + 10);
        int x0 = Mth.clamp(Math.round(vx) - width / 2, left + 4, left + ResearchTableMenu.IMAGE_WIDTH - 4 - width);
        int y0 = above ? Math.round(vy - r) - 18 - 24 : Math.round(vy + r) + 18;
        y0 = Mth.clamp(y0, top + 2, top + ResearchTableMenu.IMAGE_HEIGHT - 27);
        PixelArt.fill(graphics, x0 - 1, y0 - 1, x0 + width + 1, y0 + 25, PixelArt.argb(OUTLINE, 0.95F));
        PixelArt.fill(graphics, x0, y0, x0 + width, y0 + 24, PixelArt.argb(0x1A140C, 0.95F));
        PixelArt.fill(graphics, x0, y0, x0 + width, y0 + 1, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.flush(graphics);
        graphics.text(font, font.plainSubstrByWidth(name.getString(), width - 8), x0 + 5, y0 + 3, 0xFFF0E3C2, true);
        if (status != null) {
            PixelArt.flush(graphics);
            graphics.text(font, font.plainSubstrByWidth(status.getString(), width - 8), x0 + 5, y0 + 13,
                    studiable ? 0xFFC9B48A : 0xFF8A7A62, true);
            return;
        }
        int x = x0 + 5;
        for (Map.Entry<Essence, Integer> e : list) {
            String amount = "×" + e.getValue();
            if (x + 10 + font.width(amount) > x0 + width) {
                break;
            }
            PixelArt.essence(graphics, e.getKey(), x, y0 + 13, PixelArt.ICON, PixelArt.argb(e.getKey().color(), 1));
            PixelArt.flush(graphics);
            graphics.text(font, amount, x + 10, y0 + 14, 0xFF000000 | PixelArt.mix(e.getKey().color(), 0xFFFFFF, 0.45F), true);
            x += 10 + font.width(amount) + 4;
        }
    }

    private void burst(float x, float y, int color, long now) {
        for (int i = 0; i < 8; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float speed = 0.6F + random.nextFloat() * 1.2F;
            sparks.add(new Spark(x, y, Mth.cos(angle) * speed, Mth.sin(angle) * speed, i % 3 == 0 ? GLINT : color, now));
        }
    }

    private void sparks(GuiGraphicsExtractor graphics, long now) {
        sparks.removeIf(spark -> now - spark.born > 500);
        for (Spark spark : sparks) {
            spark.x += spark.vx;
            spark.y += spark.vy;
            spark.vx *= 0.9F;
            spark.vy = spark.vy * 0.9F + 0.03F;
            float life = 1 - (now - spark.born) / 500.0F;
            int x = Math.round(spark.x);
            int y = Math.round(spark.y);
            PixelArt.fill(graphics, x, y, x + 1, y + 1, PixelArt.argb(spark.color, Math.min(1, life * 1.4F)));
        }
    }

    // ---- Raster helpers ----------------------------------------------------------------------------------------------

    private interface Inside {
        boolean test(float x, float y);
    }

    /** The colour of a pixel (by its centre), or 0 to leave it. */
    private interface Shade {
        int at(float x, float y);
    }

    /** Fills the box pixel by pixel with the shade's colours, joining equal neighbours into horizontal runs. */
    private static void paint(GuiGraphicsExtractor graphics, float minX, float minY, float maxX, float maxY, Shade shade) {
        int x0 = Mth.floor(minX);
        int x1 = Mth.ceil(maxX);
        int y0 = Mth.floor(minY);
        int y1 = Mth.ceil(maxY);
        for (int y = y0; y <= y1; y++) {
            int run = x0;
            int color = 0;
            for (int x = x0; x <= x1 + 1; x++) {
                int c = x <= x1 ? shade.at(x + 0.5F, y + 0.5F) : 0;
                if (c != color) {
                    if (color != 0) {
                        PixelArt.fill(graphics, run, y, x, y + 1, color);
                    }
                    run = x;
                    color = c;
                }
            }
        }
    }

    /** Fills every pixel of the box whose centre is inside, in horizontal runs. */
    private static void raster(GuiGraphicsExtractor graphics, float minX, float minY, float maxX, float maxY, Inside inside, int argb) {
        int x0 = Mth.floor(minX);
        int x1 = Mth.ceil(maxX);
        int y0 = Mth.floor(minY);
        int y1 = Mth.ceil(maxY);
        for (int y = y0; y <= y1; y++) {
            int run = Integer.MIN_VALUE;
            for (int x = x0; x <= x1 + 1; x++) {
                boolean in = x <= x1 && inside.test(x + 0.5F, y + 0.5F);
                if (in && run == Integer.MIN_VALUE) {
                    run = x;
                } else if (!in && run != Integer.MIN_VALUE) {
                    PixelArt.fill(graphics, run, y, x, y + 1, argb);
                    run = Integer.MIN_VALUE;
                }
            }
        }
    }

    /** A straight bar with square ends from a to e, {@code half} pixels to each side. */
    private static void bar(GuiGraphicsExtractor graphics, float ax, float ay, float ex, float ey, float half, int argb) {
        float dx = ex - ax;
        float dy = ey - ay;
        float len2 = Math.max(0.0001F, dx * dx + dy * dy);
        float len = Mth.sqrt(len2);
        raster(graphics, Math.min(ax, ex) - half, Math.min(ay, ey) - half, Math.max(ax, ex) + half, Math.max(ay, ey) + half, (x, y) -> {
            float t = ((x - ax) * dx + (y - ay) * dy) / len2;
            if (t < 0 || t > 1) {
                return false;
            }
            float across = Math.abs((x - ax) * dy - (y - ay) * dx) / len;
            return across <= half;
        }, argb);
    }

    /** An ellipse with radius {@code ra} along (ux, uy) and {@code rb} across. */
    private static void ellipse(GuiGraphicsExtractor graphics, float cx, float cy, float ux, float uy, float ra, float rb, int argb) {
        float reach = Math.max(ra, rb);
        raster(graphics, cx - reach, cy - reach, cx + reach, cy + reach, (x, y) -> {
            float along = ((x - cx) * ux + (y - cy) * uy) / ra;
            float across = (-(x - cx) * uy + (y - cy) * ux) / rb;
            return along * along + across * across <= 1;
        }, argb);
    }

    /** A convex quad given clockwise or counter-clockwise. */
    private static void quad(GuiGraphicsExtractor graphics, float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int argb) {
        float[] xs = {ax, bx, cx, dx};
        float[] ys = {ay, by, cy, dy};
        float minX = Math.min(Math.min(ax, bx), Math.min(cx, dx));
        float maxX = Math.max(Math.max(ax, bx), Math.max(cx, dx));
        float minY = Math.min(Math.min(ay, by), Math.min(cy, dy));
        float maxY = Math.max(Math.max(ay, by), Math.max(cy, dy));
        raster(graphics, minX, minY, maxX, maxY, (x, y) -> {
            int sign = 0;
            for (int i = 0; i < 4; i++) {
                int j = (i + 1) % 4;
                float cross = (xs[j] - xs[i]) * (y - ys[i]) - (ys[j] - ys[i]) * (x - xs[i]);
                int s = cross > 0 ? 1 : cross < 0 ? -1 : 0;
                if (s != 0) {
                    if (sign != 0 && s != sign) {
                        return false;
                    }
                    sign = s;
                }
            }
            return true;
        }, argb);
    }

    private static void sound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
