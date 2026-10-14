package com.pockyl.hextale.client.screen;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import com.pockyl.hextale.block.FusionResult;
import com.pockyl.hextale.block.Requirement;
import com.pockyl.hextale.client.render.InkRenderer;
import com.pockyl.hextale.client.render.RuneDrawing;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.RuneRecognizer;
import com.pockyl.hextale.rune.RuneStrokes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * The parchment of the Research Table: two circles ("action" on the left, "target" on the right) and the orb between
 * them. Strokes go to the circle on whose half they start; a circle is read {@link RuneRecognizer#READ_DELAY_MS} after
 * its last stroke, as in the prototype. A recognised radical snaps into its clean glowing form; when both circles hold
 * one, the orb draws them in and asks the server to fuse them.
 */
public final class RuneCanvas {
    /** Glyph box of a recognised radical, in circle radii (the prototype's 1.25). */
    private static final float RADICAL_SIZE = 1.25F;
    /** New points are added once the brush has moved this far, in circle radii (3 px of the prototype's 187). */
    private static final float POINT_SPACING = 0.016F;
    private static final long FUSE_ANIMATION_MS = 450;
    private static final long CRACK_ANIMATION_MS = 1300;
    private static final long SERVER_TIMEOUT_MS = 6000;
    private static final int PARCHMENT_INK = 0x2B2118;
    private static final int ORB_COLOR = 0x5CC8F2;
    private static final long NEVER = Long.MIN_VALUE / 4;

    private enum Phase {
        IDLE,
        FUSING,
        RESULT,
        CRACK
    }

    /** What the canvas needs from its screen. */
    public interface Host {
        /** The first thing the table lacks, or null. */
        @Nullable
        Requirement missing();

        void fuse(RuneStrokes action, RuneStrokes target);

        void drawing();

        void recognitionFailed();

        void playSound(SoundEvent sound, float pitch, float volume);
    }

    private final Host host;
    private final Font font;
    private float left;
    private float top;
    private float width;
    private float height;
    private float radius;
    private final float[] centerX = new float[2];
    private float centerY;
    private float orbX;

    @SuppressWarnings("unchecked")
    private final List<float[]>[] ink = new List[]{new ArrayList<>(), new ArrayList<>()};
    @SuppressWarnings("unchecked")
    private final List<float[]>[] glyphInk = new List[]{new ArrayList<>(), new ArrayList<>()};
    @SuppressWarnings("unchecked")
    private final List<float[]>[] fading = new List[]{new ArrayList<>(), new ArrayList<>()};
    private final SimpleRune[] glyph = new SimpleRune[2];
    private final long[] readAt = {0, 0};
    private final long[] snapAt = {NEVER, NEVER};
    private final long[] failAt = {NEVER, NEVER};
    private final long[] fadeAt = {NEVER, NEVER};
    /** A radical chosen in the diary's reference page, shown as a faint outline to trace in {@link #guideCircle}. */
    @Nullable
    private SimpleRune guide;
    private int guideCircle;
    private long guideAt = NEVER;
    private FloatArrayList current;
    private int currentCircle = -1;
    private long lastDrawingPing = NEVER;
    private long lastStrokeAt = NEVER;

    private Phase phase = Phase.IDLE;
    private long phaseAt;
    @Nullable
    private FusionResult result;
    @Nullable
    private FusionResult pendingResult;
    private Component message;
    private int messageColor;
    private long messageAt = NEVER;

    public RuneCanvas(Host host, Font font) {
        this.host = host;
        this.font = font;
        say(Component.translatable("gui.hextale.research_table.start"), Tone.HINT);
    }

    public void layout(float left, float top, float width, float height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        radius = 36;
        centerX[0] = left + 54;
        centerX[1] = left + width - 54;
        centerY = top + 54;
        orbX = left + width / 2;
    }

    public boolean contains(double x, double y) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    // ---- Input -----------------------------------------------------------------------------------------------

    public boolean mouseDown(double x, double y) {
        if (!contains(x, y)) {
            return false;
        }
        long now = Util.getMillis();
        if (phase == Phase.FUSING || phase == Phase.CRACK) {
            return true;
        }
        if (phase == Phase.RESULT) {
            clearAll();
            phase = Phase.IDLE;
        }
        Requirement missing = host.missing();
        if (missing != null) {
            say(Component.translatable(missing.translationKey()), Tone.BAD);
            host.playSound(ModSounds.RUNE_FAILED.get(), 1.3F, 0.4F);
            return true;
        }
        int circle = x < left + width / 2 ? 0 : 1;
        if (glyph[circle] != null) {
            glyph[circle] = null;
            glyphInk[circle].clear();
        }
        readAt[circle] = 0;
        current = new FloatArrayList();
        currentCircle = circle;
        addPoint(x, y);
        if (now - lastDrawingPing > 400) {
            lastDrawingPing = now;
            host.drawing();
        }
        if (now - lastStrokeAt > 120) {
            host.playSound(ModSounds.BRUSH_STROKE.get(), 0.9F + (float) Math.random() * 0.25F, 0.35F);
        }
        lastStrokeAt = now;
        return true;
    }

    public boolean mouseDragged(double x, double y) {
        if (current == null) {
            return false;
        }
        float u = (float) ((x - centerX[currentCircle]) / radius);
        float v = (float) ((y - centerY) / radius);
        int n = current.size();
        float du = u - current.getFloat(n - 2);
        float dv = v - current.getFloat(n - 1);
        if (du * du + dv * dv > POINT_SPACING * POINT_SPACING) {
            current.add(Mth.clamp(u, -RuneStrokes.MAX_COORDINATE, RuneStrokes.MAX_COORDINATE));
            current.add(Mth.clamp(v, -RuneStrokes.MAX_COORDINATE, RuneStrokes.MAX_COORDINATE));
        }
        return true;
    }

    public boolean mouseUp() {
        if (current == null) {
            return false;
        }
        float[] stroke = current.toFloatArray();
        if (stroke.length == 2) {
            // A tap still leaves a dot of ink, like the prototype's two-pixel stroke.
            stroke = new float[]{stroke[0], stroke[1], stroke[0] + 0.0107F, stroke[1] + 0.0107F};
        }
        if (stroke.length > RuneStrokes.MAX_POINTS * 2) {
            stroke = simplify(stroke, RuneStrokes.MAX_POINTS);
        }
        List<float[]> circleInk = ink[currentCircle];
        if (circleInk.size() < RuneStrokes.MAX_STROKES) {
            circleInk.add(stroke);
        }
        readAt[currentCircle] = Util.getMillis() + RuneRecognizer.READ_DELAY_MS;
        current = null;
        currentCircle = -1;
        return true;
    }

    private void addPoint(double x, double y) {
        current.add((float) ((x - centerX[currentCircle]) / radius));
        current.add((float) ((y - centerY) / radius));
    }

    /**
     * Shows the radical as a faint outline in the first empty circle, so the player can trace it. Choosing the same
     * radical again hides it.
     */
    public void showGuide(SimpleRune rune) {
        if (phase == Phase.FUSING || phase == Phase.CRACK) {
            return;
        }
        if (guide == rune) {
            guide = null;
            return;
        }
        if (phase == Phase.RESULT) {
            clearAll();
            phase = Phase.IDLE;
        }
        guideCircle = glyph[0] == null || glyph[1] != null ? 0 : 1;
        if (glyph[0] != null && glyph[1] != null) {
            glyph[0] = null;
            glyphInk[0].clear();
        }
        guide = rune;
        guideAt = Util.getMillis();
        say(Component.translatable("gui.hextale.research_table.guide", elementName(rune),
                Component.translatable(guideCircle == 0 ? "gui.hextale.research_table.action" : "gui.hextale.research_table.target")),
                Tone.HINT);
    }

    @Nullable
    public SimpleRune guide() {
        return guide;
    }

    /** Clear button: wipes the parchment (nothing is spent, the ink was never fixed). */
    public void clear() {
        if (phase == Phase.FUSING || phase == Phase.CRACK) {
            return;
        }
        clearAll();
        guide = null;
        phase = Phase.IDLE;
        say(Component.translatable("gui.hextale.research_table.cleared"), Tone.HINT);
    }

    /**
     * Swap button: with one radical, moves it to the other circle; with two (after a fusion) swaps them and fuses the
     * reversed pair, which is always a different rune.
     */
    public void swap() {
        if (phase == Phase.FUSING || phase == Phase.CRACK) {
            return;
        }
        if (current != null) {
            return;
        }
        SimpleRune a = glyph[0];
        glyph[0] = glyph[1];
        glyph[1] = a;
        List<float[]> inkA = new ArrayList<>(glyphInk[0]);
        glyphInk[0].clear();
        glyphInk[0].addAll(glyphInk[1]);
        glyphInk[1].clear();
        glyphInk[1].addAll(inkA);
        ink[0].clear();
        ink[1].clear();
        readAt[0] = readAt[1] = 0;
        long now = Util.getMillis();
        snapAt[0] = snapAt[1] = now;
        phase = Phase.IDLE;
        result = null;
        if (glyph[0] != null && glyph[1] != null) {
            startFusion(now);
        } else if (glyph[0] != null || glyph[1] != null) {
            say(Component.translatable("gui.hextale.research_table.swapped"), Tone.HINT);
        }
    }

    private void clearAll() {
        for (int i = 0; i < 2; i++) {
            glyph[i] = null;
            glyphInk[i].clear();
            ink[i].clear();
            fading[i].clear();
            readAt[i] = 0;
        }
        current = null;
        currentCircle = -1;
        result = null;
        pendingResult = null;
    }

    // ---- Reading and fusing ----------------------------------------------------------------------------------

    private void update(long now) {
        for (int i = 0; i < 2; i++) {
            if (readAt[i] != 0 && now >= readAt[i] && currentCircle != i) {
                readAt[i] = 0;
                judge(i, now);
            }
        }
        if (phase == Phase.FUSING) {
            if (pendingResult != null && now - phaseAt >= FUSE_ANIMATION_MS) {
                apply(pendingResult, now);
                pendingResult = null;
            } else if (now - phaseAt > SERVER_TIMEOUT_MS) {
                phase = Phase.IDLE;
                say(Component.translatable("gui.hextale.research_table.no_answer"), Tone.BAD);
            }
        } else if (phase == Phase.CRACK && now - phaseAt > CRACK_ANIMATION_MS) {
            phase = Phase.IDLE;
            glyph[1] = null;
            glyphInk[1].clear();
        }
    }

    /** Reads one circle: the shape must fit a radical and the stroke count must be close to the radical's. */
    private void judge(int circle, long now) {
        List<float[]> strokes = ink[circle];
        if (strokes.isEmpty()) {
            return;
        }
        fading[circle].clear();
        fading[circle].addAll(strokes);
        fadeAt[circle] = now;
        SimpleRune rune = RuneRecognizer.recognise(strokes);
        if (rune == null) {
            strokes.clear();
            failAt[circle] = now;
            host.recognitionFailed();
            host.playSound(ModSounds.RUNE_FAILED.get(), 1.0F, 0.7F);
            say(Component.translatable("gui.hextale.research_table.unknown"), Tone.BAD);
            return;
        }
        glyph[circle] = rune;
        if (circle == guideCircle) {
            guide = null;
        }
        glyphInk[circle].clear();
        glyphInk[circle].addAll(strokes);
        strokes.clear();
        snapAt[circle] = now;
        host.playSound(ModSounds.RUNE_RECOGNISED.get(), pitch(rune), 0.8F);
        if (glyph[0] != null && glyph[1] != null) {
            startFusion(now);
        } else {
            say(Component.translatable("gui.hextale.research_table.recognised", elementName(rune),
                    Component.translatable(circle == 0 ? "gui.hextale.research_table.action" : "gui.hextale.research_table.target"),
                    Component.translatable(circle == 0 ? "gui.hextale.research_table.target" : "gui.hextale.research_table.action")),
                    Tone.GOOD);
        }
    }

    private void startFusion(long now) {
        Requirement missing = host.missing();
        if (missing != null) {
            say(Component.translatable(missing.translationKey()), Tone.BAD);
            return;
        }
        phase = Phase.FUSING;
        phaseAt = now;
        pendingResult = null;
        host.fuse(new RuneStrokes(glyphInk[0]), new RuneStrokes(glyphInk[1]));
        say(Component.translatable("gui.hextale.research_table.fusing"), Tone.HINT);
    }

    /** The server's answer; shown once the radicals have flown into the orb. */
    public void onFusionResult(FusionResult fusion) {
        if (phase != Phase.FUSING) {
            return;
        }
        pendingResult = fusion;
    }

    private void apply(FusionResult fusion, long now) {
        switch (fusion.status()) {
            case FUSED -> {
                ComplexRune rune = fusion.rune();
                phase = Phase.RESULT;
                phaseAt = now;
                result = fusion;
                host.playSound(ModSounds.RUNE_FUSED.get(), 1.0F, 1.0F);
                Component name = Component.translatable(rune.translationKey()).withColor(rune.color());
                say(Component.translatable(fusion.fresh() ? "gui.hextale.research_table.fused_new"
                        : "gui.hextale.research_table.fused_known", name), fusion.fresh() ? Tone.WIN : Tone.HINT);
            }
            case CRACKED -> {
                phase = Phase.CRACK;
                phaseAt = now;
                host.playSound(ModSounds.ORB_CRACK.get(), 1.0F, 1.0F);
                say(Component.translatable("gui.hextale.research_table.cracked"), Tone.BAD);
            }
            case UNRECOGNISED -> {
                phase = Phase.IDLE;
                if (fusion.action() == null) {
                    glyph[0] = null;
                    glyphInk[0].clear();
                }
                if (fusion.target() == null) {
                    glyph[1] = null;
                    glyphInk[1].clear();
                }
                say(Component.translatable("gui.hextale.research_table.lost_thread"), Tone.BAD);
            }
            case MISSING -> {
                phase = Phase.IDLE;
                Requirement missing = fusion.missing();
                say(missing == null ? Component.translatable("gui.hextale.research_table.no_answer")
                        : Component.translatable(missing.translationKey()), Tone.BAD);
            }
        }
    }

    /** Each element rings at its own pitch, low earth to high lightning. */
    private static float pitch(SimpleRune rune) {
        int semitones = switch (rune) {
            case EARTH -> -7;
            case SHADOW -> -5;
            case METAL -> -3;
            case WATER -> 0;
            case LIFE -> 2;
            case FIRE -> 4;
            case AIR -> 7;
            case LIGHT -> 9;
            case SPARK -> 12;
        };
        return (float) Math.pow(2, semitones / 12.0);
    }

    private static Component elementName(SimpleRune rune) {
        return Component.translatable(rune.translationKey()).withColor(rune.color());
    }

    // ---- Messages --------------------------------------------------------------------------------------------

    private enum Tone {
        HINT(0xE8D9B5),
        GOOD(0x8FE0F2),
        BAD(0xF08A7A),
        WIN(0xFFD36E);

        private final int color;

        Tone(int color) {
            this.color = color;
        }
    }

    private void say(Component text, Tone tone) {
        message = text;
        messageColor = tone.color;
        messageAt = Util.getMillis();
    }

    /** Status line under the parchment, two lines at most (smaller type when a translation runs long). */
    public void renderMessage(GuiGraphics graphics, float x, float y, float maxWidth) {
        if (message == null) {
            return;
        }
        float age = (Util.getMillis() - messageAt) / 250.0F;
        int alpha = (int) (Mth.clamp(age, 0.25F, 1) * 255);
        float scale = 1;
        List<FormattedCharSequence> lines = font.split(message, (int) maxWidth);
        if (lines.size() > 2) {
            scale = 0.8F;
            lines = font.split(message, (int) (maxWidth / scale));
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x + maxWidth / 2, y, 0);
        graphics.pose().scale(scale, scale, 1);
        int lineHeight = 10;
        for (int i = 0; i < Math.min(lines.size(), 3); i++) {
            FormattedCharSequence line = lines.get(i);
            graphics.drawString(font, line, -font.width(line) / 2, i * lineHeight, alpha << 24 | messageColor, true);
        }
        graphics.pose().popPose();
    }

    // ---- Rendering -------------------------------------------------------------------------------------------

    public void render(GuiGraphics graphics, @Nullable Requirement missing, int mouseX, int mouseY) {
        long now = Util.getMillis();
        update(now);
        InkRenderer ink = InkRenderer.begin(graphics);
        float drawEnergy = current != null ? 1 : 0;
        for (int i = 0; i < 2; i++) {
            float shake = shake(now - failAt[i]);
            drawCircle(ink, i, centerX[i] + shake, now, missing == null && contains(mouseX, mouseY)
                    && (mouseX < left + width / 2) == (i == 0));
        }
        drawChevrons(ink);
        if (missing != Requirement.HEXTECH || phase != Phase.IDLE) {
            drawOrb(ink, now, drawEnergy);
        } else {
            ink.ring(orbX, centerY, 5.5F, 6.5F, 0x557A6648, 0x557A6648, false);
        }
        for (int i = 0; i < 2; i++) {
            drawCircleInk(ink, i, now);
        }
        ink.end();

        renderLabels(graphics);
        if (phase == Phase.RESULT && result != null && result.rune() != null) {
            renderResult(graphics, result, now);
        } else if (phase == Phase.CRACK) {
            renderCrack(graphics, now);
        }
        if (missing != null && phase == Phase.IDLE) {
            renderRequirement(graphics, missing);
        }
    }

    private void drawCircle(InkRenderer ink, int i, float x, long now, boolean hovered) {
        boolean listening = readAt[i] != 0;
        float pulse = listening ? 0.5F + 0.5F * Mth.sin(now / 90.0F) : 0;
        int color = RuneDrawing.mix(0x7A6648, ORB_COLOR, pulse * 0.6F);
        float alpha = hovered ? 0.85F : 0.6F;
        int dashes = 36;
        for (int d = 0; d < dashes; d++) {
            float a0 = Mth.TWO_PI * d / dashes;
            float a1 = a0 + Mth.TWO_PI / dashes * 0.55F;
            float am = (a0 + a1) / 2;
            float[] dash = {
                    x + Mth.cos(a0) * radius, centerY + Mth.sin(a0) * radius,
                    x + Mth.cos(am) * radius, centerY + Mth.sin(am) * radius,
                    x + Mth.cos(a1) * radius, centerY + Mth.sin(a1) * radius};
            ink.stroke(dash, 1.1F, RuneDrawing.argb(color, alpha));
        }
    }

    /** Small chevrons on both sides of the orb: the action flows through the orb into the target. */
    private void drawChevrons(InkRenderer ink) {
        float gapLeft = centerX[0] + radius;
        float gapRight = centerX[1] - radius;
        float[] xs = {(gapLeft + orbX - 7) / 2, (orbX + 7 + gapRight) / 2};
        for (float x : xs) {
            ink.stroke(new float[]{x - 1.5F, centerY - 3, x + 1.5F, centerY, x - 1.5F, centerY + 3}, 1.2F,
                    RuneDrawing.argb(0x7A6648, 0.8F));
        }
    }

    private void drawOrb(InkRenderer ink, long now, float drawEnergy) {
        float energy = 0.25F + 0.45F * drawEnergy;
        for (int i = 0; i < 2; i++) {
            float sinceSnap = (now - snapAt[i]) / 400.0F;
            if (sinceSnap >= 0 && sinceSnap < 1) {
                energy += 0.8F * (1 - sinceSnap);
            }
            if (readAt[i] != 0) {
                energy += 0.25F;
            }
        }
        float shake = 0;
        float base = 6.0F;
        if (phase == Phase.FUSING) {
            float t = Mth.clamp((now - phaseAt) / (float) FUSE_ANIMATION_MS, 0, 1);
            energy += 1.2F * t + 0.3F * Mth.sin(now / 40.0F);
            base += 1.5F * t;
        } else if (phase == Phase.RESULT) {
            float t = (now - phaseAt) / 600.0F;
            energy += Math.max(0, 1.5F * (1 - t));
        } else if (phase == Phase.CRACK) {
            float t = (now - phaseAt) / (float) CRACK_ANIMATION_MS;
            shake = (1 - t) * 1.6F * Mth.sin(now / 18.0F);
            energy = Math.max(0, 0.8F * (1 - t * 1.4F));
            base *= Math.max(0, 1 - t * 0.9F);
        }
        float x = orbX + shake;
        float y = centerY + 0.6F * Mth.sin(now / 520.0F);
        float flicker = 0.92F + 0.08F * Mth.sin(now / 63.0F) * Mth.sin(now / 97.0F);
        float glowRadius = base * (2.4F + 0.8F * Math.min(energy, 2));
        ink.disc(x, y, glowRadius, RuneDrawing.argb(ORB_COLOR, Math.min(0.9F, 0.28F + 0.3F * energy) * flicker), 0x005CC8F2, true);
        if (base > 0.5F) {
            ink.disc(x, y, base, RuneDrawing.argb(0xE8FBFF, 1), RuneDrawing.argb(0x2F9BD0, 1), false);
            ink.disc(x - base * 0.3F, y - base * 0.35F, base * 0.35F, 0xC0FFFFFF, 0x00FFFFFF, true);
            float spin = now / 700.0F;
            for (int k = 0; k < 3; k++) {
                float a = spin + k * Mth.TWO_PI / 3;
                float r = base * (1.4F + 0.25F * Mth.sin(now / 300.0F + k));
                ink.disc(x + Mth.cos(a) * r, y + Mth.sin(a) * r * 0.55F, 0.9F, RuneDrawing.argb(0xBFF4FF, 0.7F * flicker),
                        0x005CC8F2, true);
            }
        }
    }

    private void drawCircleInk(InkRenderer ink, int i, long now) {
        float shake = shake(now - failAt[i]);
        float x = centerX[i] + shake;
        float inkWidth = radius * 0.05F;
        float fade = 1 - (now - fadeAt[i]) / 300.0F;
        if (fade > 0 && !fading[i].isEmpty()) {
            boolean failed = now - failAt[i] < 400;
            int color = failed ? 0x8E2A1E : PARCHMENT_INK;
            for (float[] stroke : fading[i]) {
                ink.stroke(toScreen(stroke, x), inkWidth, RuneDrawing.argb(color, 0.9F * fade));
            }
        }
        for (float[] stroke : this.ink[i]) {
            ink.stroke(toScreen(stroke, x), inkWidth, RuneDrawing.argb(PARCHMENT_INK, 0.92F));
        }
        if (current != null && currentCircle == i) {
            ink.stroke(toScreen(current.toFloatArray(), x), inkWidth, RuneDrawing.argb(0x1E1710, 0.95F));
        }
        SimpleRune rune = glyph[i];
        if (rune == null) {
            if (guide != null && guideCircle == i && phase == Phase.IDLE) {
                drawGuide(ink, guide, x, now);
            }
            return;
        }
        float size = radius * RADICAL_SIZE;
        float cx = x;
        float alpha = 1;
        float sinceSnap = (now - snapAt[i]) / 350.0F;
        if (sinceSnap >= 0 && sinceSnap < 1) {
            size *= 1 + 0.12F * (1 - ease(sinceSnap));
        }
        if (phase == Phase.FUSING) {
            float t = ease(Mth.clamp((now - phaseAt) / (float) FUSE_ANIMATION_MS, 0, 1));
            cx = Mth.lerp(t, x, orbX);
            size *= 1 - 0.7F * t;
            alpha = 1 - 0.6F * t;
        } else if (phase == Phase.RESULT) {
            alpha = 0.35F;
        } else if (phase == Phase.CRACK && i == 1) {
            alpha = Math.max(0, 1 - (now - phaseAt) / 600.0F);
        }
        RuneDrawing.radical(ink, rune, cx, centerY, size, RuneDrawing.Style.INK, alpha);
        if (sinceSnap >= 0 && sinceSnap < 1.5F) {
            RuneDrawing.strokes(ink, rune.radical(), cx, centerY, size, rune.color(), RuneDrawing.Style.GLOW,
                    Math.max(0, 1 - sinceSnap / 1.5F) * alpha);
        }
    }

    /** The radical to trace: a pale outline in the element's colour that breathes slowly, fading in when chosen. */
    private void drawGuide(InkRenderer ink, SimpleRune rune, float x, long now) {
        float appear = Mth.clamp((now - guideAt) / 250.0F, 0, 1);
        float breathe = 0.75F + 0.25F * Mth.sin(now / 420.0F);
        float size = radius * RADICAL_SIZE;
        float width = RuneDrawing.lineWidth(size);
        int color = RuneDrawing.darken(rune.color(), 0.75F);
        for (float[] stroke : rune.radical()) {
            float[] placed = RuneDrawing.place(stroke, x, centerY, size);
            ink.stroke(placed, width * 2.2F, RuneDrawing.argb(color, 0.16F * appear * breathe));
            ink.stroke(placed, width * 0.7F, RuneDrawing.argb(color, 0.42F * appear * breathe));
            // A dot where the stroke starts, so the order is easy to follow (any order is accepted).
            ink.disc(placed[0], placed[1], width * 0.9F, RuneDrawing.argb(color, 0.6F * appear), RuneDrawing.argb(color, 0.6F * appear), false);
        }
    }

    private float[] toScreen(float[] stroke, float x) {
        float[] out = new float[stroke.length];
        for (int k = 0; k + 1 < stroke.length; k += 2) {
            out[k] = x + stroke[k] * radius;
            out[k + 1] = centerY + stroke[k + 1] * radius;
        }
        return out;
    }

    private void renderLabels(GuiGraphics graphics) {
        String[] keys = {"gui.hextale.research_table.action", "gui.hextale.research_table.target"};
        for (int i = 0; i < 2; i++) {
            Component label = Component.translatable(keys[i]);
            graphics.drawString(font, label, (int) (centerX[i] - font.width(label) / 2.0F), (int) (centerY + radius + 4),
                    0xFF6B5A3E, false);
            SimpleRune rune = glyph[i];
            if (rune != null && phase != Phase.RESULT) {
                Component name = Component.translatable(rune.translationKey());
                graphics.drawString(font, name, (int) (centerX[i] - font.width(name) / 2.0F), (int) (centerY - radius - 11),
                        0xFF000000 | RuneDrawing.darken(rune.color(), 0.55F), false);
            }
        }
    }

    private void renderResult(GuiGraphics graphics, FusionResult fusion, long now) {
        ComplexRune rune = fusion.rune();
        float t = now - phaseAt;
        float grow = ease(Mth.clamp(t / 420.0F, 0, 1));
        float cx = left + width / 2;
        float cy = top + 46;
        InkRenderer ink = InkRenderer.begin(graphics);
        ink.rect(left, top, left + width, top + height, RuneDrawing.argb(0x231E15, 0.88F * Mth.clamp(t / 250.0F, 0, 1)));
        float flash = 1 - t / 380.0F;
        if (flash > 0) {
            ink.disc(cx, cy, 12 + t * 0.22F, RuneDrawing.argb(rune.color(), 0.85F * flash), rune.color() & 0xFFFFFF, true);
        }
        float pulse = 0.85F + 0.15F * Mth.sin(t / 420.0F);
        float size = 62 * grow;
        if (size > 1) {
            RuneDrawing.glyph(ink, rune, cx, cy, size, RuneDrawing.Style.GLOW, pulse);
        }
        ink.end();
        float textAlpha = Mth.clamp((t - 180) / 300.0F, 0, 1);
        if (textAlpha > 0.05F) {
            int a = (int) (textAlpha * 255) << 24;
            Component name = Component.translatable(rune.translationKey()).withStyle(ChatFormatting.BOLD);
            graphics.drawString(font, name, (int) (cx - font.width(name) / 2.0F), (int) (cy + 37), a | 0xF3E9D2, true);
            Component sub = Component.translatable("rune.hextale.formula", Component.translatable(rune.action().translationKey()),
                    Component.translatable(rune.target().translationKey())).append(" · ")
                    .append(Component.translatable(rune.category().translationKey()));
            graphics.drawString(font, sub, (int) (cx - font.width(sub) / 2.0F), (int) (cy + 49), a | rune.color(), false);
            if (fusion.fresh()) {
                Component badge = Component.translatable("gui.hextale.research_table.new_rune");
                graphics.drawString(font, badge, (int) (left + width - font.width(badge) - 5), (int) (top + 5), a | 0xFFD36E, true);
            }
        }
    }

    private void renderCrack(GuiGraphics graphics, long now) {
        float t = (now - phaseAt) / (float) CRACK_ANIMATION_MS;
        InkRenderer ink = InkRenderer.begin(graphics);
        float crack = Mth.clamp(t * 3, 0, 1);
        if (t < 0.7F) {
            float alpha = 1 - t / 0.7F;
            float[][] lines = {
                    {orbX - 1, centerY - 6, orbX + 1, centerY - 2, orbX - 1, centerY + 1, orbX + 2, centerY + 6},
                    {orbX + 1, centerY - 2, orbX + 5, centerY - 3},
                    {orbX - 1, centerY + 1, orbX - 5, centerY + 3}};
            for (float[] line : lines) {
                int n = Math.max(2, (int) Math.ceil(line.length / 2.0F * crack));
                ink.stroke(line, n, 0.9F, RuneDrawing.argb(0xFFFFFF, alpha));
            }
        }
        for (int k = 0; k < 5; k++) {
            float puff = t * 1.4F - k * 0.08F;
            if (puff > 0 && puff < 1) {
                float a = k * 1.7F;
                float px = orbX + Mth.cos(a) * puff * 10;
                float py = centerY - puff * 16 + Mth.sin(a) * 3;
                ink.disc(px, py, 3 + puff * 6, RuneDrawing.argb(0x5A5048, 0.5F * (1 - puff)), 0x005A5048, false);
            }
        }
        ink.end();
    }

    private void renderRequirement(GuiGraphics graphics, Requirement missing) {
        InkRenderer ink = InkRenderer.begin(graphics);
        ink.rect(left, top, left + width, top + height, RuneDrawing.argb(0x231E15, 0.62F));
        ink.end();
        Component text = Component.translatable(missing.translationKey());
        List<FormattedCharSequence> lines = font.split(text, (int) width - 28);
        int y = (int) (top + height / 2 - lines.size() * 5.5F);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, (int) (left + width / 2 - font.width(line) / 2.0F), y, 0xFFF3E9D2, true);
            y += 11;
        }
    }

    private static float shake(long sinceFail) {
        if (sinceFail < 0 || sinceFail > 400) {
            return 0;
        }
        float t = sinceFail / 400.0F;
        return Mth.sin(sinceFail / 22.0F) * 2.2F * (1 - t);
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** Keeps every n-th point so a very long stroke stays within the packet limit. */
    private static float[] simplify(float[] stroke, int maxPoints) {
        int points = stroke.length / 2;
        float step = (points - 1) / (float) (maxPoints - 1);
        float[] out = new float[maxPoints * 2];
        for (int i = 0; i < maxPoints; i++) {
            int k = Math.min(points - 1, Math.round(i * step));
            out[i * 2] = stroke[k * 2];
            out[i * 2 + 1] = stroke[k * 2 + 1];
        }
        return out;
    }

    /** The complex rune currently shown in the result, for the screen's tooltip. */
    @Nullable
    public ComplexRune shownRune() {
        return phase == Phase.RESULT && result != null ? result.rune() : null;
    }
}
