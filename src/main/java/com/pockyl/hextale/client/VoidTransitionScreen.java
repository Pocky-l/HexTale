package com.pockyl.hextale.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;

import com.pockyl.hextale.thevoid.realm.VoidPassage;

/**
 * Passing through a breach: instead of the loading screen, a fall down the breach's throat - square cells, like the
 * breach's own, rushing past toward a heart that beats. Into the Void the cells burn magenta round a dark heart; out of
 * it they warm to gold round a daylit middle.
 */
public class VoidTransitionScreen extends LevelLoadingScreen {
    private static final int RINGS = 14;
    private final boolean home;
    private final long start = Util.getMillis();

    public VoidTransitionScreen(LevelLoadTracker tracker, Reason reason, boolean home) {
        super(tracker, reason);
        this.home = home;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterDimensionTransitionScreenEvent event) -> {
            event.registerIncomingEffect(VoidPassage.LEVEL, (tracker, reason) -> new VoidTransitionScreen(tracker, reason, false));
            event.registerOutgoingEffect(VoidPassage.LEVEL, (tracker, reason) -> new VoidTransitionScreen(tracker, reason, true));
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractBackground(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        float t = (Util.getMillis() - start) / 1000F;
        int w = width;
        int h = height;
        int cx = w / 2;
        int cy = h / 2;
        int deep = home ? 0xFF2A1E2E : 0xFF0A0310;
        graphics.fill(0, 0, w, h, deep);
        // The heart at the end of the throat: two beats, then a rest.
        float phase = (t / 1.9F) % 1;
        float beat = (float) (Math.exp(-Math.pow((phase - 0.08) / 0.045, 2)) + 0.65 * Math.exp(-Math.pow((phase - 0.27) / 0.06, 2)));
        int heart = home ? 0xF2D7A0 : 0xE040C8;
        float glow = Math.min(w, h) * (0.06F + 0.02F * beat);
        for (int i = 6; i >= 1; i--) {
            float r = glow * i * 0.6F;
            int alpha = (int) (28 + 10 * beat) + (6 - i) * 10;
            graphics.fill((int) (cx - r), (int) (cy - r), (int) (cx + r), (int) (cy + r), Mth.clamp(alpha, 0, 255) << 24 | heart);
        }
        // Rings of cells rushing outward past the eye: each one grows, brightens and is gone off the edge.
        float speed = 0.9F;
        for (int i = 0; i < RINGS; i++) {
            float k = ((t * speed + i / (float) RINGS) % 1);
            float depth = k * k * k;
            float half = Math.max(2, depth * Math.max(w, h) * 0.9F + 4);
            float thick = Math.max(1, depth * 9);
            int alpha = (int) (Math.min(1, k * 3) * (1 - Math.max(0, (k - 0.8F) / 0.2F)) * 220);
            int colour = ring(k);
            int c = Mth.clamp(alpha, 0, 255) << 24 | colour;
            // Each ring is broken into square cells, a few missing, like the breach's own outline.
            int cells = 4 + (int) (depth * 10);
            float cell = 2 * half / cells;
            for (int side = 0; side < 4; side++) {
                for (int n = 0; n < cells; n++) {
                    if (((i * 31 + n * 7 + side * 13) % 9) == 0) {
                        continue;
                    }
                    float x0;
                    float y0;
                    float x1;
                    float y1;
                    float along = -half + n * cell;
                    switch (side) {
                        case 0 -> { x0 = cx + along; y0 = cy - half; x1 = x0 + cell - 1; y1 = y0 + thick; }
                        case 1 -> { x0 = cx + along; y0 = cy + half - thick; x1 = x0 + cell - 1; y1 = cy + half; }
                        case 2 -> { x0 = cx - half; y0 = cy + along; x1 = x0 + thick; y1 = y0 + cell - 1; }
                        default -> { x0 = cx + half - thick; y0 = cy + along; x1 = cx + half; y1 = y0 + cell - 1; }
                    }
                    graphics.fill((int) x0, (int) y0, (int) x1, (int) y1, c);
                }
            }
        }
        // A veil over the edges: the throat is darkest where it is nearest.
        graphics.fillGradient(0, 0, w, h / 5, 0xC0000000 | (deep & 0xFFFFFF), 0x00000000);
        graphics.fillGradient(0, h - h / 5, w, h, 0x00000000, 0xC0000000 | (deep & 0xFFFFFF));
    }

    /** A ring's colour by how far it has come: deep and cold far in, hot near the eye. */
    private int ring(float k) {
        if (home) {
            return k < 0.5F ? 0xC8A8E0 : 0xF2D7A0;
        }
        return k < 0.35F ? 0x6A2A8A : k < 0.7F ? 0xB040C0 : 0xFF7AE6;
    }
}
