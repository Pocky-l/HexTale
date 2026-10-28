package com.pockyl.hextale.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.ClientConfig;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.network.BreachCinematicDonePayload;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.thevoid.BreachCamera;
import com.pockyl.hextale.thevoid.TheVoid;

/**
 * The camera flies to a breach of the Void that tears open nearby: from the player's eyes out to a point before it,
 * holds while the omen shakes the ground and the breach slowly opens, watches the first voidling push out of it and drop,
 * then flies back. Black bars, no HUD, no hand; the player stands still and (on the server) comes to no harm. Skipped
 * with a key, turned off in the client config. The timeline follows the breach's own clock, so it stays in step with what
 * the world shows. Every player near any breach in its omen also feels the ground shake.
 */
public final class BreachCinematic {
    /** Ticks of the flight out to the shot. */
    private static final int FLY_IN = 40;
    /** Ticks of the flight back to the eyes. */
    private static final int FLY_OUT = 30;
    /** Ticks the camera watches the voidling drop before it flies back. */
    private static final int WATCH_FALL = 22;
    /** Ticks to wait for the breach to reach the client after the order. */
    private static final int WAIT = 60;
    /** How far the ground shakes around a breach in its omen (blocks). */
    private static final float SHAKE_RANGE = 48;
    private static final Identifier LETTERBOX = Hextale.id("breach_letterbox");

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Hextale.id("hextale"));
    public static final KeyMapping SKIP = new KeyMapping("key.hextale.skip_cinematic", InputConstants.Type.KEYSYM,
            InputConstants.KEY_BACKSPACE, CATEGORY);

    private static int pendingId = -1;
    private static int pendingTicks;
    private static @Nullable TheVoid breach;
    private static @Nullable BreachCamera camera;
    private static Vec3 shot = Vec3.ZERO;
    private static float startYaw;
    private static float startPitch;
    private static long end;
    private static @Nullable CameraType savedType;

    private BreachCinematic() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(CATEGORY);
            event.register(SKIP);
        });
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(LETTERBOX, BreachCinematic::letterbox));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener((RenderFrameEvent.Pre event) -> frame(event.getPartialTick()));
        NeoForge.EVENT_BUS.addListener(BreachCinematic::hideLayer);
        NeoForge.EVENT_BUS.addListener((RenderHandEvent event) -> {
            if (active()) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((CalculatePlayerTurnEvent event) -> {
            if (active()) {
                // 0 sensitivity in the game's own formula: the mouse does not turn the waiting player.
                event.setMouseSensitivity(-1 / 3.0);
                event.setCinematicCameraEnabled(false);
            }
        });
        NeoForge.EVENT_BUS.addListener((InputEvent.InteractionKeyMappingTriggered event) -> {
            if (active()) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(BreachCinematic::shake);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            pendingId = -1;
            stop(false);
        });
    }

    /** The server says a breach is tearing open near the player. */
    public static void start(int breachId) {
        if (!ClientConfig.BREACH_CINEMATIC.get() || active()) {
            ClientPacketDistributor.sendToServer(BreachCinematicDonePayload.INSTANCE);
            return;
        }
        pendingId = breachId;
        pendingTicks = WAIT;
    }

    public static boolean active() {
        return camera != null;
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            pendingId = -1;
            stop(false);
            return;
        }
        if (pendingId >= 0) {
            Entity entity = level.getEntity(pendingId);
            if (entity instanceof TheVoid found) {
                pendingId = -1;
                begin(minecraft, player, found);
            } else if (--pendingTicks <= 0) {
                pendingId = -1;
                ClientPacketDistributor.sendToServer(BreachCinematicDonePayload.INSTANCE);
            }
        }
        if (!active()) {
            return;
        }
        boolean skipped = false;
        while (SKIP.consumeClick()) {
            skipped = true;
        }
        if (skipped || breach == null || breach.isRemoved() || breach.level() != level || player.isDeadOrDying()
                || level.getGameTime() >= end) {
            stop(true);
        } else if (minecraft.options.getCameraType() != CameraType.FIRST_PERSON) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private static void begin(Minecraft minecraft, LocalPlayer player, TheVoid found) {
        breach = found;
        startYaw = player.getYRot();
        startPitch = player.getXRot();
        shot = shotFor(found, (ClientLevel) player.level());
        end = found.born() + found.openTicks() + TheVoid.FIRST_PUSH_DELAY + TheVoid.PUSH_TICKS + WATCH_FALL + FLY_OUT;
        BreachCamera eye = new BreachCamera(ModEntities.BREACH_CAMERA.get(), player.level());
        place(eye, player.getEyePosition(), startYaw, startPitch);
        camera = eye;
        savedType = minecraft.options.getCameraType();
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        minecraft.setCameraEntity(eye);
    }

    /** Ends it (and tells the server, unless the player is leaving). */
    private static void stop(boolean tellServer) {
        if (camera == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        camera = null;
        breach = null;
        if (minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
        }
        if (savedType != null) {
            minecraft.options.setCameraType(savedType);
            savedType = null;
        }
        if (tellServer && minecraft.getConnection() != null) {
            ClientPacketDistributor.sendToServer(BreachCinematicDonePayload.INSTANCE);
        }
    }

    /**
     * Where the camera watches from: before the breach's front, a little to one side and above, far enough to see all
     * of it and the ground below - pulled closer when something stands in the way.
     */
    private static Vec3 shotFor(TheVoid found, ClientLevel level) {
        Vec3 centre = found.position();
        Vector3f normal = found.normal();
        float distance = found.layout().reach() * 1.9F + 3.5F;
        Vec3 best = centre.add(normal.x * 3, normal.y * 3 + 0.5, normal.z * 3);
        double bestDistance = -1;
        for (float turn : new float[] {18, -18, 32, -32, 0}) {
            Vec3 way = new Vec3(normal.x, 0, normal.z).yRot(turn * Mth.DEG_TO_RAD).normalize();
            Vec3 target = centre.add(way.scale(distance)).add(0, 0.8, 0);
            BlockHitResult hit = level.clip(new ClipContext(centre, target, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                    net.minecraft.world.phys.shapes.CollisionContext.empty()));
            Vec3 at = hit.getType() == HitResult.Type.MISS ? target : hit.getLocation().subtract(way.scale(0.7));
            double reached = at.distanceTo(centre);
            if (reached >= distance * 0.9) {
                return at;
            }
            if (reached > bestDistance) {
                best = at;
                bestDistance = reached;
            }
        }
        return best;
    }

    private static void frame(DeltaTracker delta) {
        BreachCamera eye = camera;
        TheVoid watched = breach;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (eye == null || watched == null || player == null || minecraft.level == null) {
            return;
        }
        float partialTick = delta.getGameTimeDeltaPartialTick(true);
        double now = minecraft.level.getGameTime() + partialTick;
        double since = now - watched.omenStart();
        float in = smooth((float) (since / FLY_IN));
        float out = smooth((float) ((now - (end - FLY_OUT)) / FLY_OUT));

        // The aim: the breach's heart, sinking towards where the voidling bulges out and drops.
        Vec3 aim = watched.position();
        Vector3fc push = watched.pushPoint();
        if (push != null) {
            Vec3 bulge = watched.toWorld(new Vector3f(push));
            aim = aim.lerp(bulge.add(0, -0.6, 0), Math.min(1, watched.push(partialTick) * 1.5F) * 0.6);
        }
        double afterOpen = now - watched.born() - watched.openTicks() - TheVoid.FIRST_PUSH_DELAY - TheVoid.PUSH_TICKS;
        if (afterOpen > 0) {
            aim = aim.add(0, -Math.min(1, afterOpen / 15) * 0.9, 0);
        }
        // A slow push in while it holds the shot.
        Vec3 toAim = aim.subtract(shot);
        double dolly = Math.min(Math.max(0, since - FLY_IN) * 0.012, Math.max(0, toAim.length() - 3) * 0.35);
        Vec3 held = shot.add(toAim.normalize().scale(dolly));

        Vec3 eyes = player.getEyePosition(partialTick);
        Vec3 pos = eyes.lerp(held, in).lerp(eyes, out);
        double arc = Math.sin(Math.PI * in) * (1 - out) + Math.sin(Math.PI * out) * in;
        pos = pos.add(0, arc * 1.2, 0);

        Vec3 look = aim.subtract(pos);
        float aimYaw = (float) (Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG);
        float aimPitch = (float) (-Mth.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * Mth.RAD_TO_DEG);
        float yaw = Mth.rotLerp(out, Mth.rotLerp(in, startYaw, aimYaw), startYaw);
        float pitch = Mth.lerp(out, Mth.lerp(in, startPitch, aimPitch), startPitch);
        place(eye, pos, yaw, pitch);
    }

    /** Puts the camera's eye at {@code pos}, turned so; no interpolation of its own (it is set every frame). */
    private static void place(BreachCamera eye, Vec3 pos, float yaw, float pitch) {
        double y = pos.y - eye.getEyeHeight();
        eye.setPos(pos.x, y, pos.z);
        eye.xo = pos.x;
        eye.yo = y;
        eye.zo = pos.z;
        eye.xOld = pos.x;
        eye.yOld = y;
        eye.zOld = pos.z;
        eye.setYRot(yaw);
        eye.setXRot(pitch);
        eye.yRotO = yaw;
        eye.xRotO = pitch;
    }

    private static float smooth(float t) {
        float k = Mth.clamp(t, 0, 1);
        return k * k * k * (k * (k * 6 - 15) + 10);
    }

    /** While it plays, nothing of the HUD shows but the bars and the Void's own darkening of the edges. */
    private static void hideLayer(RenderGuiLayerEvent.Pre event) {
        if (!active()) {
            return;
        }
        Identifier name = event.getName();
        if (!name.equals(LETTERBOX) && !(name.getNamespace().equals(Hextale.MOD_ID) && name.getPath().endsWith("vignette"))) {
            event.setCanceled(true);
        }
    }

    private static void letterbox(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        TheVoid watched = breach;
        if (!active() || watched == null || minecraft.level == null) {
            return;
        }
        double now = minecraft.level.getGameTime() + delta.getGameTimeDeltaPartialTick(true);
        float shown = Math.min(smooth((float) ((now - watched.omenStart()) / 15)), smooth((float) ((end - now) / 15)));
        int bar = Math.round(graphics.guiHeight() * 0.11F * shown);
        if (bar <= 0) {
            return;
        }
        graphics.fill(0, 0, graphics.guiWidth(), bar, 0xFF000000);
        graphics.fill(0, graphics.guiHeight() - bar, graphics.guiWidth(), graphics.guiHeight(), 0xFF000000);
        Component hint = Component.translatable("gui.hextale.cinematic.skip", SKIP.getTranslatedKeyMessage());
        int alpha = Math.round(shown * 0x90);
        if (alpha > 8 && bar > 12) {
            int width = minecraft.font.width(hint);
            graphics.text(minecraft.font, hint, graphics.guiWidth() - width - 8, graphics.guiHeight() - bar / 2 - 4,
                    alpha << 24 | 0xB8A8C8, false);
        }
    }

    /** The ground shakes around a breach in its omen (and a little when something drops out of one). */
    private static void shake(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        Vec3 at = event.getCamera().position();
        float strength = 0;
        for (TheVoid near : minecraft.level.getEntitiesOfClass(TheVoid.class, new AABB(at, at).inflate(SHAKE_RANGE))) {
            float distance = (float) near.position().distanceTo(at);
            float falloff = 1 - Mth.clamp(distance / SHAKE_RANGE, 0, 1);
            float omen = near.foretold() ? near.omen(partialTick) : 0;
            float push = near.push(partialTick);
            strength = Math.max(strength, falloff * Math.max((float) Math.pow(omen, 1.6), push * 0.35F));
        }
        if (strength <= 0.001F) {
            return;
        }
        float time = (minecraft.level.getGameTime() % 100000) + partialTick;
        event.setRoll(event.getRoll() + strength * 1.6F * Mth.sin(time * 2.3F) * Mth.sin(time * 1.37F + 1));
        event.setPitch(event.getPitch() + strength * 0.8F * Mth.sin(time * 3.1F + 2));
        event.setYaw(event.getYaw() + strength * 0.5F * Mth.sin(time * 2.7F + 4));
    }
}
