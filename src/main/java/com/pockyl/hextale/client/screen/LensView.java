package com.pockyl.hextale.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.LensStudy;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * The Hextech Lens of the Research Table: the item floating in the middle of a dark field, its hidden essences circling
 * it as wisps on their orbits, the lens on the rim shining a wedge of cold light down onto the item (see
 * {@link LensStudy}). A click (or Space) flashes the lens: a wisp inside the wedge bursts into sparks that spiral into
 * the item and stays on its orbit as its essence's sign. The screen does not wait for the server: it catches at once
 * and sends when it flashed; the server's answer replaces the guess within a moment.
 */
final class LensView {
    /** The field's middle, relative to the screen's top left. */
    static final int CX = 126;
    static final int CY = 141;
    private static final int AREA_X = 4;
    private static final int AREA_Y = 24;
    private static final int AREA_W = 244;
    private static final int AREA_H = 234;
    /** The rim (the lens sits on it) and where the wedge starts above the item. */
    private static final int RIM = 104;
    private static final int WEDGE_FROM = 30;
    private static final int ITEM_SIZE = 48;
    /** The essence cards on the side panel. */
    static final int CARDS_X = 258;
    static final int CARDS_Y = 64;
    private static final int CARD_W = 96;
    private static final int CARD_H = 20;
    private static final int CARD_STEP_X = 102;
    private static final int CARD_STEP_Y = 22;

    private static final long FLASH_MILLIS = 260;
    private static final long FRONT_MILLIS = 140;
    private static final long PREDICTION_MILLIS = 1500;
    private static final long BURST_MILLIS = 420;
    private static final long FINALE_MILLIS = 1100;

    private static final int NIGHT = 0x0A0D18;
    private static final int DEEP = 0x1C366E;
    private static final int HEX_BLUE = 0x3281C6;
    private static final int SPELL = 0x87B1F8;
    private static final int GLINT = 0xDDF2FF;
    private static final int BRASS = 0xD8A64A;
    private static final int BRASS_DARK = 0x6A5226;
    private static final int CLOUD = 0x6C7086;

    /** A spark of light; homing ones curve into the item. */
    private static final class Spark {
        float x;
        float y;
        float vx;
        float vy;
        final int color;
        final long born;
        final long life;
        final boolean homing;

        Spark(float x, float y, float vx, float vy, int color, long born, long life, boolean homing) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.born = born;
            this.life = life;
            this.homing = homing;
        }
    }

    /** A ring of light widening from a point and fading. */
    private record Wave(float x, float y, int from, int to, int color, long born, long life) {
    }

    private int left;
    private int top;
    @Nullable
    private Item item;
    private long began;
    /** Caught on this screen but not yet confirmed by the server, and when. */
    private int predicted;
    private long predictedAt;
    /** What was shown caught last frame, so a new catch (here or the server's) bursts once. */
    private int shown;
    private float readyAt;
    private long flashedAt;
    private boolean flashMissed;
    private long finaleAt;
    private long glowAt;
    private int glowColor = HEX_BLUE;
    private long lastFrame;
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Wave> waves = new ArrayList<>();
    private final Random random = new Random();

    void layout(int left, int top) {
        this.left = left;
        this.top = top;
    }

    // ---- Time and state ------------------------------------------------------------------------------------------

    /** Ticks since the study began, as this screen sees them (quantised as they are sent). */
    private float ticks(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0;
        }
        return Math.round((minecraft.level.getGameTime() - began + partialTick) * 100) / 100F;
    }

    private void sync(LensStudy study, long now) {
        if (study.item() != item || study.item() != null && study.began() != began) {
            item = study.item();
            began = study.began();
            predicted = 0;
            readyAt = 0;
            finaleAt = 0;
            sparks.clear();
            waves.clear();
            shown = caughtMask(study);
            return;
        }
        if (predicted != 0 && now - predictedAt > PREDICTION_MILLIS) {
            predicted = 0;
        }
        predicted &= ~caughtMask(study);
    }

    private static int caughtMask(LensStudy study) {
        int mask = 0;
        for (int i = 0; i < study.wisps().size(); i++) {
            if (study.isCaught(i)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private boolean caught(LensStudy study, int wisp) {
        return study.isCaught(wisp) || (predicted & 1 << wisp) != 0;
    }

    private int caughtCount(LensStudy study) {
        return Integer.bitCount(caughtMask(study) | predicted);
    }

    private boolean done(LensStudy study) {
        return item != null && !study.wisps().isEmpty() && caughtCount(study) == study.wisps().size();
    }

    // ---- Drawing -----------------------------------------------------------------------------------------------------

    void render(GuiGraphicsExtractor graphics, LensStudy study, float partialTick, int mouseX, int mouseY) {
        long now = Util.getMillis();
        sync(study, now);
        float t = ticks(partialTick);
        field(graphics, now);
        int cx = left + CX;
        int cy = top + CY;
        List<LensStudy.Wisp> wisps = item == null ? List.of() : study.wisps();
        orbits(graphics, study, wisps, now);
        wedge(graphics, study, t, now);
        lens(graphics, t, now);

        // New catches - this screen's guess or the server's - burst where the wisp is.
        int mask = caughtMask(study) | predicted;
        for (int i = 0; i < wisps.size(); i++) {
            if ((mask & 1 << i) != 0 && (shown & 1 << i) == 0) {
                burst(wisps.get(i), t, now);
            }
        }
        if (mask != shown && done(study) && finaleAt == 0) {
            finaleAt = now;
        }
        shown = mask;

        if (item != null) {
            itemGlow(graphics, cx, cy, now);
            PixelArt.flush(graphics);
            float bob = Mth.sin(now / 700.0F) * 1.5F;
            graphics.pose().pushMatrix();
            graphics.pose().translate(cx - ITEM_SIZE / 2.0F, cy - ITEM_SIZE / 2.0F + bob);
            graphics.pose().scale(ITEM_SIZE / 16.0F, ITEM_SIZE / 16.0F);
            graphics.item(new ItemStack(item), 0, 0);
            graphics.pose().popMatrix();
        }
        for (int i = 0; i < wisps.size(); i++) {
            if (caught(study, i)) {
                sign(graphics, wisps.get(i), t, now, isOverWisp(wisps.get(i), t, mouseX, mouseY));
            } else {
                wisp(graphics, wisps.get(i), t, now, ready(t) && wisps.get(i).inLens(t));
            }
        }
        effects(graphics, now);
        finale(graphics, cx, cy, now);
    }

    /** The field: a deep blue night brightening towards the item, faint stars, a hexagon and the turning rim. */
    private void field(GuiGraphicsExtractor graphics, long now) {
        int cx = left + CX;
        int cy = top + CY;
        PixelArt.fill(graphics, left + AREA_X, top + AREA_Y, left + AREA_X + AREA_W, top + AREA_Y + AREA_H, PixelArt.argb(NIGHT, 1));
        PixelArt.disc(graphics, cx, cy, RIM * 2, PixelArt.argb(0x0E1426, 1), 0);
        PixelArt.disc(graphics, cx, cy, 160, PixelArt.argb(0x111B33, 1), 0);
        PixelArt.disc(graphics, cx, cy, 112, PixelArt.argb(0x14223F, 1), 0);
        PixelArt.disc(graphics, cx, cy, 70, PixelArt.argb(DEEP, 0.35F), 0);
        Random stars = new Random(7);
        for (int i = 0; i < 46; i++) {
            int x = left + AREA_X + 2 + stars.nextInt(AREA_W - 4);
            int y = top + AREA_Y + 2 + stars.nextInt(AREA_H - 4);
            float twinkle = 0.5F + 0.5F * Mth.sin(now / (500.0F + stars.nextInt(900)) + i);
            PixelArt.fill(graphics, x, y, x + 1, y + 1, PixelArt.argb(i % 5 == 0 ? SPELL : GLINT, 0.08F + 0.25F * twinkle));
        }
        PixelArt.hexRing(graphics, cx, cy, 196, PixelArt.argb(SPELL, 0.07F));
        PixelArt.discRing(graphics, cx, cy, RIM * 2 + 4, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.discRing(graphics, cx, cy, RIM * 2 + 2, PixelArt.argb(BRASS_DARK, 1));
        PixelArt.discRing(graphics, cx, cy, RIM * 2, PixelArt.argb(BRASS, 1));
        float turn = now / 24000.0F * Mth.TWO_PI;
        for (int i = 0; i < 48; i++) {
            float a = turn + i * Mth.TWO_PI / 48;
            boolean long_ = i % 8 == 0;
            float inner = RIM - (long_ ? 6 : 3);
            PixelArt.line(graphics, Math.round(cx + Mth.cos(a) * inner), Math.round(cy + Mth.sin(a) * inner),
                    Math.round(cx + Mth.cos(a) * (RIM - 1)), Math.round(cy + Mth.sin(a) * (RIM - 1)),
                    PixelArt.argb(BRASS, long_ ? 0.8F : 0.3F));
        }
    }

    /** The orbits: faint brass for hidden wisps, their essence's colour once caught. */
    private void orbits(GuiGraphicsExtractor graphics, LensStudy study, List<LensStudy.Wisp> wisps, long now) {
        int cx = left + CX;
        int cy = top + CY;
        int count = item == null ? LensStudy.MAX_WISPS : wisps.size();
        for (int i = 0; i < count; i++) {
            int diameter = Math.round(LensStudy.orbit(i, count)) * 2;
            if (i < wisps.size() && caught(study, i)) {
                int color = wisps.get(i).essence().color();
                float pulse = 0.5F + 0.5F * Mth.sin(now / 900.0F + i);
                PixelArt.discRing(graphics, cx, cy, diameter + 2, PixelArt.argb(color, 0.08F + 0.06F * pulse));
                PixelArt.discRing(graphics, cx, cy, diameter, PixelArt.argb(color, 0.4F));
                PixelArt.discRing(graphics, cx, cy, diameter - 2, PixelArt.argb(color, 0.08F + 0.06F * pulse));
            } else {
                PixelArt.discRing(graphics, cx, cy, diameter, PixelArt.argb(BRASS, item == null ? 0.07F : 0.14F));
            }
        }
    }

    private boolean ready(float t) {
        return t >= readyAt;
    }

    /**
     * The lens's wedge of light from the rim down to the item: cold blue and breathing while ready, flaring on a flash
     * (a bright front runs down it), grey and refilling from the lens while clouded.
     */
    private void wedge(GuiGraphicsExtractor graphics, LensStudy study, float t, long now) {
        int cx = left + CX;
        int cy = top + CY;
        float slope = (float) Math.tan(LensStudy.LENS_HALF);
        float flash = Math.max(0, 1 - (now - flashedAt) / (float) FLASH_MILLIS);
        float front = Math.min(1, (now - flashedAt) / (float) FRONT_MILLIS);
        boolean clouded = !ready(t) && flashMissed;
        float refill = clouded ? 1 - (readyAt - t) / LensStudy.CLOUDED : 1;
        boolean idle = item == null || done(study);
        float breathe = 0.85F + 0.15F * Mth.sin(now / 420.0F);
        for (int d = WEDGE_FROM; d <= RIM - 2; d++) {
            int y = cy - d;
            int half = Math.min(Math.round(d * slope), (int) Math.sqrt(Math.max(0, (RIM - 2) * (RIM - 2) - d * d)));
            // Brighter towards the lens; the refill runs down from the lens.
            float k = (d - WEDGE_FROM) / (float) (RIM - WEDGE_FROM);
            int color = SPELL;
            float alpha = (0.08F + 0.14F * k) * breathe;
            if (idle) {
                alpha *= 0.5F;
            } else if (clouded) {
                boolean filled = 1 - k <= refill;
                color = filled ? SPELL : CLOUD;
                alpha = filled ? alpha * 0.7F : 0.05F;
            }
            if (flash > 0) {
                float band = Math.max(0, 1 - Math.abs((1 - k) - front) * 6);
                color = PixelArt.mix(color, flashMissed ? CLOUD : GLINT, Math.min(1, flash + band));
                alpha += flash * (flashMissed ? 0.12F : 0.3F) + band * flash * 0.4F;
            }
            PixelArt.fill(graphics, cx - half, y, cx + half + 1, y + 1, PixelArt.argb(color, Math.min(0.85F, alpha)));
        }
        // The wedge's edges.
        int edge = PixelArt.argb(clouded ? CLOUD : SPELL, idle ? 0.12F : 0.3F + flash * 0.4F);
        float from = WEDGE_FROM / Mth.cos(LensStudy.LENS_HALF);
        for (int side = -1; side <= 1; side += 2) {
            float ux = Mth.sin(LensStudy.LENS_HALF) * side;
            float uy = -Mth.cos(LensStudy.LENS_HALF);
            PixelArt.line(graphics, Math.round(cx + ux * from), Math.round(cy + uy * from),
                    Math.round(cx + ux * (RIM - 4)), Math.round(cy + uy * (RIM - 4)), edge);
        }
    }

    /** The lens on the rim: a brass ring round a crystal that glows while ready and fogs over after a miss. */
    private void lens(GuiGraphicsExtractor graphics, float t, long now) {
        int x = left + CX;
        int y = top + CY - RIM;
        boolean clouded = !ready(t) && flashMissed;
        float flash = Math.max(0, 1 - (now - flashedAt) / (float) FLASH_MILLIS);
        int glass = clouded ? PixelArt.mix(CLOUD, HEX_BLUE, 1 - (readyAt - t) / LensStudy.CLOUDED) : HEX_BLUE;
        PixelArt.disc(graphics, x, y, 22, PixelArt.argb(clouded ? CLOUD : SPELL, 0.08F + 0.25F * flash), 0);
        PixelArt.disc(graphics, x, y, 16, PixelArt.argb(PixelArt.mix(glass, GLINT, flash), 1), PixelArt.argb(BRASS_DARK, 1));
        PixelArt.discRing(graphics, x, y, 14, PixelArt.argb(BRASS, 1));
        PixelArt.disc(graphics, x, y, 8, PixelArt.argb(PixelArt.mix(0x97D3F0, GLINT, flash), clouded ? 0.35F : 0.9F), 0);
        PixelArt.fill(graphics, x - 3, y - 4, x - 1, y - 3, PixelArt.argb(GLINT, clouded ? 0.3F : 0.9F));
        // Little brass claws holding the crystal.
        PixelArt.fill(graphics, x - 10, y - 1, x - 7, y + 1, PixelArt.argb(BRASS, 1));
        PixelArt.fill(graphics, x + 7, y - 1, x + 10, y + 1, PixelArt.argb(BRASS, 1));
    }

    /** A hidden wisp: a soft glow faintly tinted by its essence, a comet tail behind it; bright when the lens holds it. */
    private void wisp(GuiGraphicsExtractor graphics, LensStudy.Wisp wisp, float t, long now, boolean held) {
        int cx = left + CX;
        int cy = top + CY;
        float r = wisp.distance();
        int tint = PixelArt.mix(GLINT, wisp.essence().color(), 0.3F);
        for (int k = 8; k >= 1; k--) {
            float a = wisp.angle(t - k * 0.7F);
            int x = Math.round(cx + Mth.cos(a) * r);
            int y = Math.round(cy + Mth.sin(a) * r);
            int size = k < 4 ? 2 : 1;
            PixelArt.fill(graphics, x, y, x + size, y + size, PixelArt.argb(tint, 0.35F * (1 - k / 9.0F)));
        }
        float a = wisp.angle(t);
        int x = Math.round(cx + Mth.cos(a) * r);
        int y = Math.round(cy + Mth.sin(a) * r);
        int size = Math.round(wisp.radius() * 2);
        float breathe = 0.8F + 0.2F * Mth.sin(now / 160.0F + wisp.orbit() * 2);
        if (held) {
            PixelArt.discRing(graphics, x, y, size + 10, PixelArt.argb(GLINT, 0.55F));
        }
        PixelArt.disc(graphics, x, y, size + 8, PixelArt.argb(tint, (held ? 0.25F : 0.12F) * breathe), 0);
        PixelArt.disc(graphics, x, y, size + 2, PixelArt.argb(tint, (held ? 0.6F : 0.35F) * breathe), 0);
        PixelArt.disc(graphics, x, y, Math.max(3, size - 3), PixelArt.argb(GLINT, held ? 1 : 0.85F), 0);
        // A twinkle now and then.
        float twinkle = Mth.sin(now / 230.0F + wisp.orbit() * 1.7F);
        if (twinkle > 0.8F || held) {
            int arm = held ? 4 : 3;
            int color = PixelArt.argb(GLINT, held ? 0.8F : (twinkle - 0.8F) * 3);
            PixelArt.fill(graphics, x - arm, y, x + arm + 1, y + 1, color);
            PixelArt.fill(graphics, x, y - arm, x + 1, y + arm + 1, color);
        }
    }

    /** A caught wisp: its essence's sign in a dark bead, still circling. */
    private void sign(GuiGraphicsExtractor graphics, LensStudy.Wisp wisp, float t, long now, boolean hovered) {
        float r = wisp.distance();
        float a = wisp.angle(t);
        int x = Math.round(left + CX + Mth.cos(a) * r);
        int y = Math.round(top + CY + Mth.sin(a) * r);
        int color = wisp.essence().color();
        float pulse = 0.5F + 0.5F * Mth.sin(now / 500.0F + wisp.orbit());
        PixelArt.disc(graphics, x, y, 22, PixelArt.argb(color, 0.12F + 0.08F * pulse), 0);
        PixelArt.disc(graphics, x, y, 16, PixelArt.argb(0x0E1220, 0.95F), PixelArt.argb(hovered ? GLINT : color, 1));
        PixelArt.essence(graphics, wisp.essence(), x - 5, y - 5, PixelArt.ICON, PixelArt.argb(color, 1));
    }

    /** Behind the item: a soft hextech glow, flushing with an essence's colour as its sparks arrive. */
    private void itemGlow(GuiGraphicsExtractor graphics, int cx, int cy, long now) {
        float hit = Math.max(0, 1 - (now - glowAt) / 600.0F);
        int color = PixelArt.mix(HEX_BLUE, glowColor, hit);
        float breathe = 0.85F + 0.15F * Mth.sin(now / 600.0F);
        PixelArt.disc(graphics, cx, cy, 58 + Math.round(hit * 10), PixelArt.argb(color, (0.1F + 0.25F * hit) * breathe), 0);
        PixelArt.disc(graphics, cx, cy, 40, PixelArt.argb(color, (0.12F + 0.3F * hit) * breathe), 0);
    }

    // ---- Effects -------------------------------------------------------------------------------------------------------

    /** A caught wisp bursts: a ring of light and sparks that fly out and curve into the item. */
    private void burst(LensStudy.Wisp wisp, float t, long now) {
        float r = wisp.distance();
        float a = wisp.angle(t);
        float x = CX + Mth.cos(a) * r;
        float y = CY + Mth.sin(a) * r;
        int color = wisp.essence().color();
        waves.add(new Wave(x, y, 6, 36, color, now, BURST_MILLIS));
        waves.add(new Wave(x, y, 4, 20, GLINT, now, BURST_MILLIS / 2));
        for (int i = 0; i < 18; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float speed = 1.2F + random.nextFloat() * 1.8F;
            sparks.add(new Spark(x, y, Mth.cos(angle) * speed, Mth.sin(angle) * speed, i % 4 == 0 ? GLINT : color, now,
                    900 + random.nextInt(400), true));
        }
    }

    private void effects(GuiGraphicsExtractor graphics, long now) {
        float dt = lastFrame == 0 ? 1 : Math.min(3, (now - lastFrame) / 16.0F);
        lastFrame = now;
        for (Spark spark : sparks) {
            if (spark.homing) {
                // Pulled into the item, ever harder.
                float dx = CX - spark.x;
                float dy = CY - spark.y;
                float d = Math.max(1, Mth.sqrt(dx * dx + dy * dy));
                float pull = 0.08F + (now - spark.born) / 2500.0F;
                spark.vx = (spark.vx + dx / d * pull * 2) * 0.94F;
                spark.vy = (spark.vy + dy / d * pull * 2) * 0.94F;
            } else {
                spark.vy += 0.05F * dt;
            }
            spark.x += spark.vx * dt;
            spark.y += spark.vy * dt;
        }
        sparks.removeIf(spark -> {
            boolean arrived = spark.homing && Mth.square(spark.x - CX) + Mth.square(spark.y - CY) < 64;
            if (arrived) {
                glowAt = now;
                glowColor = spark.color == GLINT ? glowColor : spark.color;
            }
            return arrived || now - spark.born > spark.life;
        });
        for (Spark spark : sparks) {
            float life = 1 - (now - spark.born) / (float) spark.life;
            int x = Math.round(left + spark.x);
            int y = Math.round(top + spark.y);
            PixelArt.fill(graphics, x, y, x + 2, y + 2, PixelArt.argb(spark.color, Math.min(1, life * 1.5F)));
            PixelArt.fill(graphics, x - 1, y - 1, x + 3, y + 3, PixelArt.argb(spark.color, 0.15F * life));
        }
        waves.removeIf(wave -> now - wave.born() > wave.life());
        for (Wave wave : waves) {
            float k = (now - wave.born()) / (float) wave.life();
            float eased = 1 - (1 - k) * (1 - k);
            int diameter = Math.round(wave.from() + (wave.to() - wave.from()) * eased) * 2;
            PixelArt.discRing(graphics, Math.round(left + wave.x()), Math.round(top + wave.y()), diameter,
                    PixelArt.argb(wave.color(), 0.7F * (1 - k)));
        }
    }

    /** Every essence caught: a gold ring sweeps out over the field. */
    private void finale(GuiGraphicsExtractor graphics, int cx, int cy, long now) {
        if (finaleAt == 0 || now - finaleAt > FINALE_MILLIS) {
            return;
        }
        float k = (now - finaleAt) / (float) FINALE_MILLIS;
        float eased = 1 - (1 - k) * (1 - k) * (1 - k);
        int diameter = Math.round(40 + (RIM * 2 - 40) * eased) / 2 * 2;
        PixelArt.discRing(graphics, cx, cy, diameter, PixelArt.argb(BRASS, 0.9F * (1 - k)));
        PixelArt.discRing(graphics, cx, cy, diameter - 4, PixelArt.argb(BRASS, 0.35F * (1 - k)));
        PixelArt.disc(graphics, cx, cy, 70, PixelArt.argb(GLINT, 0.3F * (1 - k)), 0);
    }

    // ---- The essence cards -----------------------------------------------------------------------------------------

    /** The side panel's cards: one per wisp, a question mark until it is caught, then its essence and strength. */
    void renderCards(GuiGraphicsExtractor graphics, Font font, LensStudy study, long now) {
        List<LensStudy.Wisp> wisps = study.wisps();
        for (int i = 0; i < wisps.size(); i++) {
            int x = left + CARDS_X + i % 2 * CARD_STEP_X;
            int y = top + CARDS_Y + i / 2 * CARD_STEP_Y;
            LensStudy.Wisp wisp = wisps.get(i);
            boolean known = caught(study, i);
            int color = wisp.essence().color();
            PixelArt.fill(graphics, x, y, x + CARD_W, y + CARD_H, PixelArt.argb(0x1A140C, 1));
            PixelArt.fill(graphics, x, y, x + CARD_W, y + 1, PixelArt.argb(known ? color : BRASS_DARK, known ? 0.8F : 1));
            PixelArt.fill(graphics, x, y + CARD_H - 1, x + CARD_W, y + CARD_H, PixelArt.argb(0x0B0805, 1));
            if (known) {
                PixelArt.disc(graphics, x + 10, y + 10, 16, PixelArt.argb(0x0E1220, 1), PixelArt.argb(color, 1));
                PixelArt.essence(graphics, wisp.essence(), x + 5, y + 5, PixelArt.ICON, PixelArt.argb(color, 1));
                PixelArt.flush(graphics);
                String strength = "×" + wisp.amount();
                int sw = font.width(strength);
                String name = font.plainSubstrByWidth(ResearchText.essence(wisp.essence()).getString(), CARD_W - 26 - sw - 4);
                graphics.text(font, name, x + 21, y + 6, 0xFF000000 | PixelArt.mix(color, 0xFFFFFF, 0.45F), true);
                graphics.text(font, strength, x + CARD_W - 3 - sw, y + 6, 0xFFB8A27A, true);
            } else {
                float breathe = 0.5F + 0.5F * Mth.sin(now / 300.0F + i);
                PixelArt.disc(graphics, x + 10, y + 10, 16, PixelArt.argb(0x0E1220, 1), PixelArt.argb(BRASS_DARK, 1));
                PixelArt.disc(graphics, x + 10, y + 10, 6, PixelArt.argb(GLINT, 0.3F + 0.4F * breathe), 0);
                PixelArt.flush(graphics);
                String hidden = font.plainSubstrByWidth(Component.translatable("gui.hextale.research_table.lens.hidden").getString(), CARD_W - 26);
                graphics.text(font, hidden, x + 21, y + 6, 0xFF7E6D52, true);
            }
        }
    }

    // ---- Tooltips ----------------------------------------------------------------------------------------------------

    @Nullable
    List<Component> tooltip(LensStudy study, float partialTick, double mouseX, double mouseY) {
        if (item == null) {
            return null;
        }
        float t = ticks(partialTick);
        List<LensStudy.Wisp> wisps = study.wisps();
        for (int i = 0; i < wisps.size(); i++) {
            if (caught(study, i) && isOverWisp(wisps.get(i), t, mouseX, mouseY)) {
                return essenceTip(wisps.get(i));
            }
            int x = left + CARDS_X + i % 2 * CARD_STEP_X;
            int y = top + CARDS_Y + i / 2 * CARD_STEP_Y;
            if (mouseX >= x && mouseX < x + CARD_W && mouseY >= y && mouseY < y + CARD_H) {
                return caught(study, i) ? essenceTip(wisps.get(i))
                        : List.of(Component.translatable("gui.hextale.research_table.lens.hidden.hint").withStyle(ChatFormatting.GRAY));
            }
        }
        return null;
    }

    private static List<Component> essenceTip(LensStudy.Wisp wisp) {
        return List.of(Component.translatable("gui.hextale.research_table.lens.essence", ResearchText.essence(wisp.essence()), wisp.amount()));
    }

    private boolean isOverWisp(LensStudy.Wisp wisp, float t, double mouseX, double mouseY) {
        float r = wisp.distance();
        float a = wisp.angle(t);
        return Mth.square((float) mouseX - (left + CX + Mth.cos(a) * r)) + Mth.square((float) mouseY - (top + CY + Mth.sin(a) * r)) < 64;
    }

    // ---- Input -------------------------------------------------------------------------------------------------------

    boolean overField(double mouseX, double mouseY) {
        return mouseX >= left + AREA_X && mouseX < left + AREA_X + AREA_W && mouseY >= top + AREA_Y && mouseY < top + AREA_Y + AREA_H;
    }

    /**
     * Flashes the lens, if it is ready: what the wedge holds is caught at once (and confirmed by the server later).
     *
     * @param send gets the flash time in hundredths of a tick since the study began
     */
    void flash(LensStudy study, float partialTick, IntConsumer send) {
        if (item == null || done(study)) {
            return;
        }
        float t = ticks(partialTick);
        if (!ready(t)) {
            return;
        }
        int got = study.inLens(t) & ~predicted;
        long now = Util.getMillis();
        flashedAt = now;
        flashMissed = got == 0;
        readyAt = t + (got == 0 ? LensStudy.CLOUDED : LensStudy.RECHARGE);
        sound(ModSounds.LENS_PULSE.get(), 0.95F + random.nextFloat() * 0.1F, 0.5F);
        if (got == 0) {
            sound(ModSounds.LENS_MISS.get(), 1.0F, 0.6F);
            int lx = CX;
            int ly = CY - RIM + 6;
            for (int i = 0; i < 7; i++) {
                sparks.add(new Spark(lx, ly, (random.nextFloat() - 0.5F) * 1.6F, 0.2F + random.nextFloat() * 0.8F, CLOUD, now, 500, false));
            }
        } else {
            predicted |= got;
            predictedAt = now;
            boolean last = caughtCount(study) == study.wisps().size();
            sound(ModSounds.LENS_CATCH.get(), 0.9F + 0.1F * Integer.bitCount(got), 0.8F);
            if (last) {
                sound(ModSounds.LENS_STUDIED.get(), 1.0F, 0.9F);
            }
        }
        send.accept(Math.round(t * 100));
    }

    private static void sound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
