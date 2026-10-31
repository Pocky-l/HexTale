package com.pockyl.hextale.client;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.network.VoidPullPayload;
import com.pockyl.hextale.registry.ModSounds;

/**
 * Passing through a breach or a gate, the part the player sees around the loading: first the PULL - the breach draws
 * them in along a tightening spiral, the view narrows and twists, the throat's rings and streaks close round the
 * edges until there is nothing else; then the tunnel ({@link VoidTransitionScreen}, drawn with {@link #tunnel}); then
 * the ARRIVAL - spat out on the other side, the view snaps wide and settles, the throat thins away.
 */
public final class VoidPassageFx {
    /** Seconds the arrival lasts. */
    private static final float ARRIVAL = 1.6F;
    private static final Identifier GLOW = Hextale.id("textures/gui/void_passage/glow.png");
    private static final Identifier RING = Hextale.id("textures/gui/void_passage/ring.png");
    private static final Identifier STREAKS = Hextale.id("textures/gui/void_passage/streaks.png");
    private static final Identifier VEIL = Hextale.id("textures/gui/void_passage/veil.png");
    private static final Identifier VIGNETTE = Identifier.withDefaultNamespace("textures/misc/vignette.png");
    private static final Map<Identifier, Boolean> PRESENT = new HashMap<>();
    private static Vec3 centre;
    private static boolean into;
    private static long pullStart = -1;
    private static float pullSeconds = 1.7F;
    private static long cancelAt = -1;
    private static long arriveStart = -1;
    private static boolean arriveHome;

    private VoidPassageFx() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> tick());
        NeoForge.EVENT_BUS.addListener(VoidPassageFx::fov);
        NeoForge.EVENT_BUS.addListener(VoidPassageFx::angles);
    }

    public static void registerLayer(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Hextale.id("void_passage"), VoidPassageFx::overlay));
    }

    public static void pull(VoidPullPayload payload) {
        if (payload.cancel()) {
            cancelAt = Util.getMillis();
            return;
        }
        centre = new Vec3(payload.x(), payload.y(), payload.z());
        into = payload.into();
        pullStart = Util.getMillis();
        pullSeconds = payload.ticks() / 20F;
        cancelAt = -1;
        arriveStart = -1;
    }

    /** The tunnel screen has closed on the other side: the arrival plays. */
    public static void arrive(boolean home) {
        pullStart = -1;
        cancelAt = -1;
        arriveStart = Util.getMillis();
        arriveHome = home;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(ModSounds.VOID_PASSAGE_ARRIVE.get(), 0.8F, home ? 1.15F : 0.85F);
        }
    }

    /** 0..1 through the pull (it holds at 1 until the crossing), fading back to 0 once a pull was let go. */
    private static float pulled() {
        if (pullStart < 0) {
            return 0;
        }
        float t = Mth.clamp((Util.getMillis() - pullStart) / 1000F / pullSeconds, 0, 1);
        if (cancelAt >= 0) {
            float back = (Util.getMillis() - cancelAt) / 500F;
            if (back >= 1) {
                pullStart = -1;
                return 0;
            }
            return t * (1 - back);
        }
        // Never stuck behind the veil: if the crossing never came, it lets go after a while.
        if ((Util.getMillis() - pullStart) / 1000F > pullSeconds + 6) {
            cancelAt = Util.getMillis();
        }
        return t;
    }

    /** 1 just arrived, falling to 0. */
    private static float arriving() {
        if (arriveStart < 0) {
            return 0;
        }
        float a = 1 - (Util.getMillis() - arriveStart) / 1000F / ARRIVAL;
        if (a <= 0) {
            arriveStart = -1;
            return 0;
        }
        return a;
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        float t = pulled();
        if (player == null || t <= 0 || centre == null || cancelAt >= 0) {
            return;
        }
        // Drawn in along a tightening spiral, faster and faster.
        Vec3 middle = player.position().add(0, player.getBbHeight() * 0.5, 0);
        Vec3 to = centre.subtract(middle);
        double distance = to.length();
        if (distance < 1.0E-3) {
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 in = to.scale(1 / distance);
        Vec3 around = in.cross(new Vec3(0, 1, 0));
        around = around.lengthSqr() < 1.0E-4 ? Vec3.ZERO : around.normalize();
        double speed = Math.min(distance * 0.35, 0.05 + 0.55 * t * t);
        Vec3 want = in.scale(speed).add(around.scale(0.12 * (1 - t) * Math.min(1, distance)));
        player.setDeltaMovement(player.getDeltaMovement().scale(0.4).add(want.scale(0.6)));
        player.resetFallDistance();
    }

    private static float ease(float t) {
        return t * t * (3 - 2 * t);
    }

    private static void fov(ViewportEvent.ComputeFov event) {
        float t = pulled();
        float a = arriving();
        if (t > 0) {
            event.setFOV(event.getFOV() * (1 - 0.5F * ease(t)));
        } else if (a > 0) {
            event.setFOV(event.getFOV() * (1 + 0.45F * a * a));
        }
    }

    private static void angles(ViewportEvent.ComputeCameraAngles event) {
        float t = pulled();
        float a = arriving();
        if (t > 0) {
            event.setRoll(event.getRoll() + 40 * t * t * t);
        } else if (a > 0) {
            event.setRoll(event.getRoll() - 22 * a * a * (arriveHome ? -1 : 1));
        }
    }

    private static void overlay(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        float t = pulled();
        float a = arriving();
        if (t <= 0 && a <= 0) {
            return;
        }
        float seconds = Util.getMillis() / 1000F;
        if (t > 0) {
            // Going in: the throat closes round the edges and fills the view.
            tunnel(graphics, graphics.guiWidth(), graphics.guiHeight(), seconds, (float) Math.pow(t, 1.6), !into, true);
        } else {
            // Spat out: the throat thins away from the middle outward.
            tunnel(graphics, graphics.guiWidth(), graphics.guiHeight(), seconds * 1.6F, (float) Math.pow(a, 1.4), arriveHome, false);
        }
    }

    private static boolean present(Identifier texture) {
        return PRESENT.computeIfAbsent(texture, id -> Minecraft.getInstance().getResourceManager().getResource(id).isPresent());
    }

    /**
     * The throat of a breach at strength {@code k} (0 nothing, 1 all there is): the deep colour, the veil of its wall
     * turning, streaks rushing past, rings of cells flying outward, the heart at the end beating. {@code closing}: the
     * pull, where it grows in from the edges; otherwise it fades evenly.
     */
    public static void tunnel(GuiGraphicsExtractor graphics, int w, int h, float seconds, float k, boolean home, boolean closing) {
        int cx = w / 2;
        int cy = h / 2;
        float size = Math.max(w, h);
        int deep = home ? 0x2A1E2E : 0x0A0310;
        int wall = home ? 0xC8A8E0 : 0x8A5CB8;
        int heart = home ? 0xF2D7A0 : 0xE040C8;
        // The deep: last to come in while closing, so the world is seen through the middle till the end.
        float deepAlpha = closing ? Mth.clamp((k - 0.55F) / 0.4F, 0, 1) : k;
        graphics.fill(0, 0, w, h, alpha(deepAlpha) | deep);
        // The edges darken first.
        if (present(VIGNETTE)) {
            for (int i = 0; i < 2; i++) {
                graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0, 0, w, h, w, h, alpha(Math.min(1, k * 1.4F)) | 0xFFFFFF);
            }
        }
        // The throat's wall: the veil, turning slowly, two layers against each other.
        if (present(VEIL)) {
            for (int layer = 0; layer < 2; layer++) {
                float turn = seconds * (layer == 0 ? 0.12F : -0.08F);
                float scale = size / 256F * (layer == 0 ? 1.6F : 2.3F) * (1 + 0.05F * Mth.sin(seconds * 1.3F + layer));
                textured(graphics, VEIL, cx, cy, 256, scale, turn, alpha(k * (layer == 0 ? 0.45F : 0.3F)) | wall);
            }
        }
        // Streaks rushing past: three copies growing out of the middle, each fading as it passes.
        if (present(STREAKS)) {
            for (int i = 0; i < 3; i++) {
                float p = (seconds * 0.7F + i / 3F) % 1;
                float scale = size / 256F * (0.4F + p * p * 2.6F);
                float fade = Math.min(1, p * 4) * (1 - p);
                textured(graphics, STREAKS, cx, cy, 256, scale, i * 2.1F + seconds * 0.2F, alpha(k * fade * 0.85F) | ring(home, p));
            }
        }
        // Rings of cells flying outward past the eye (drawn cell by cell until their texture is there).
        if (!present(RING)) {
            cells(graphics, w, h, seconds, k, home);
        } else {
            int rings = 9;
            for (int i = 0; i < rings; i++) {
                float p = (seconds * 0.55F + i / (float) rings) % 1;
                float depth = p * p * p;
                float scale = size / 220F * (0.05F + depth * 1.6F);
                float fade = Math.min(1, p * 3) * (1 - Math.max(0, (p - 0.75F) / 0.25F));
                // While closing, the far rings come in only late; the near ones are there from the start.
                float shown = closing ? Mth.clamp((k - (1 - p) * 0.6F) / 0.4F, 0, 1) : k;
                textured(graphics, RING, cx, cy, 256, scale, i * 0.7F + seconds * (i % 2 == 0 ? 0.3F : -0.25F),
                        alpha(shown * fade) | ring(home, p));
            }
        }
        // The heart at the end of the throat: two beats, then a rest.
        if (present(GLOW)) {
            float phase = (seconds / 1.9F) % 1;
            float beat = (float) (Math.exp(-Math.pow((phase - 0.08) / 0.045, 2)) + 0.65 * Math.exp(-Math.pow((phase - 0.27) / 0.06, 2)));
            float scale = size / 128F * (0.22F + 0.06F * beat);
            textured(graphics, GLOW, cx, cy, 128, scale, 0, alpha(k * (0.7F + 0.3F * beat)) | heart);
            textured(graphics, GLOW, cx, cy, 128, scale * 0.35F, 0, alpha(k * 0.9F) | 0xFFFFFF);
        }
    }

    /** Rings of square cells, the breach's own outline, drawn as plain fills. */
    private static void cells(GuiGraphicsExtractor graphics, int w, int h, float seconds, float k, boolean home) {
        int cx = w / 2;
        int cy = h / 2;
        int rings = 14;
        for (int i = 0; i < rings; i++) {
            float p = (seconds * 0.9F + i / (float) rings) % 1;
            float depth = p * p * p;
            float half = Math.max(2, depth * Math.max(w, h) * 0.9F + 4);
            float thick = Math.max(1, depth * 9);
            float fade = Math.min(1, p * 3) * (1 - Math.max(0, (p - 0.8F) / 0.2F));
            int c = alpha(k * fade * 0.86F) | ring(home, p);
            int count = 4 + (int) (depth * 10);
            float cell = 2 * half / count;
            for (int side = 0; side < 4; side++) {
                for (int n = 0; n < count; n++) {
                    if (((i * 31 + n * 7 + side * 13) % 9) == 0) {
                        continue;
                    }
                    float along = -half + n * cell;
                    switch (side) {
                        case 0 -> graphics.fill((int) (cx + along), (int) (cy - half), (int) (cx + along + cell - 1), (int) (cy - half + thick), c);
                        case 1 -> graphics.fill((int) (cx + along), (int) (cy + half - thick), (int) (cx + along + cell - 1), (int) (cy + half), c);
                        case 2 -> graphics.fill((int) (cx - half), (int) (cy + along), (int) (cx - half + thick), (int) (cy + along + cell - 1), c);
                        default -> graphics.fill((int) (cx + half - thick), (int) (cy + along), (int) (cx + half), (int) (cy + along + cell - 1), c);
                    }
                }
            }
        }
    }

    private static void textured(GuiGraphicsExtractor graphics, Identifier texture, int cx, int cy, int px, float scale, float turn, int colour) {
        if ((colour >>> 24) == 0) {
            return;
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().rotate(turn);
        graphics.pose().scale(scale, scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, -px / 2, -px / 2, 0, 0, px, px, px, px, colour);
        graphics.pose().popMatrix();
    }

    private static int alpha(float a) {
        return Mth.clamp((int) (a * 255), 0, 255) << 24;
    }

    /** A ring's colour by how far it has come: deep and cold far in, hot near the eye. */
    private static int ring(boolean home, float p) {
        if (home) {
            return p < 0.5F ? 0xC8A8E0 : 0xF2D7A0;
        }
        return p < 0.35F ? 0x6A2A8A : p < 0.7F ? 0xB040C0 : 0xFF7AE6;
    }
}
