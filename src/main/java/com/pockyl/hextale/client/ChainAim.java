package com.pockyl.hextale.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import com.pockyl.hextale.port.client.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ChainAnchorBlockEntity;
import com.pockyl.hextale.chem.Zipline;
import com.pockyl.hextale.client.render.ChainRender;
import com.pockyl.hextale.item.ChainLauncherItem;
import com.pockyl.hextale.network.ChainRidePayload;

import org.jspecify.annotations.Nullable;

/**
 * The Chain Launcher on the client: while it is held, a hologram shows the line a shot would make - the chain from
 * where the player stands to where it would hold, a ghost post at each end a post would rise at, a ring on a post the
 * line would join or on the wall a claw would bite (green when the shot is good, red with the reason when not), and
 * the length under the crosshair. Without a launcher in hand, a click on a chain hangs the player on it.
 */
public final class ChainAim {
    private static Zipline.@Nullable Aim aim;

    private ChainAim() {
    }

    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(ChainAim::onInteract);
        NeoForge.EVENT_BUS.addListener(ChainAim::submit);
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.CROSSHAIR, Hextale.id("chain_aim"), (portGraphics, portDelta) -> ChainAim.hud(com.pockyl.hextale.port.client.GuiGraphicsExtractor.of(portGraphics), portDelta)));
    }

    private static boolean holding(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof ChainLauncherItem || player.getOffhandItem().getItem() instanceof ChainLauncherItem;
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.screen != null || player.isPassenger() || player.isSpectator() || !holding(player)) {
            aim = null;
            return;
        }
        aim = Zipline.aim(level, player);
    }

    private static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND
                || player.isPassenger() || holding(player)) {
            return;
        }
        Grab grab = pickChain(minecraft, player);
        if (grab != null) {
            event.setCanceled(true);
            event.setSwingHand(true);
            PacketDistributor.sendToServer(new ChainRidePayload(grab.owner(), grab.other(), (float) grab.t()));
        }
    }

    record Grab(net.minecraft.core.BlockPos owner, net.minecraft.core.BlockPos other, double t) {
    }

    /** The chain the player points at within reach, nearer than the block they look at, or null. */
    private static @Nullable Grab pickChain(Minecraft minecraft, LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        double reach = ChainRidePayload.REACH;
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            reach = Math.min(reach, hit.getLocation().distanceTo(eye) + 0.2);
        } else if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            return null;
        }
        Grab best = null;
        double bestDistance = 0.4;
        for (ChainAnchorBlockEntity anchor : ChainAnchorBlockEntity.loaded()) {
            if (anchor.getLevel() != minecraft.level) {
                continue;
            }
            for (ChainAnchorBlockEntity.Link link : anchor.links()) {
                if (!link.owner() || link.flying()) {
                    continue;
                }
                Zipline.Line line = link.line();
                int steps = Math.clamp((long) Math.ceil(line.length() * 5), 4, 400);
                for (int i = 0; i <= steps; i++) {
                    double t = i / (double) steps;
                    Vec3 rel = line.point(t).subtract(eye);
                    double along = rel.dot(look);
                    if (along < 0 || along > reach) {
                        continue;
                    }
                    double off = rel.subtract(look.scale(along)).length();
                    if (off < bestDistance) {
                        bestDistance = off;
                        best = new Grab(anchor.getBlockPos(), link.other(), t);
                    }
                }
            }
        }
        return best;
    }

    private static void submit(SubmitCustomGeometryEvent event) {
        Zipline.Aim current = aim;
        if (current == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float time = event.getLevelRenderState().gameTime + event.getLevelRenderState().worldPartialTicks;
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack poseStack = event.getPoseStack();
        int colour = current.ok() ? ChainRender.GOOD : ChainRender.BAD;
        if (current.line() != null) {
            ChainRender.submitGhost(poseStack, collector, ChainRender.points(current.line(), 1, camera), camera, colour, time);
        }
        if (current.start() != null && current.line() != null) {
            end(poseStack, collector, current.start(), camera, colour, time);
        }
        if (current.target() != null) {
            end(poseStack, collector, current.target(), camera, colour, time);
        }
        if (current.blockedAt() != null) {
            Vec3 at = current.blockedAt().subtract(camera);
            Vec3 toCamera = camera.subtract(current.blockedAt()).normalize();
            ChainRender.submitMark(poseStack, collector, at, toCamera, ChainRender.BAD, time);
        }
    }

    /** One end of the hologram: a ghost pole where a post would rise, a ring on a post it joins or where a claw bites. */
    private static void end(PoseStack poseStack, SubmitNodeCollector collector, Zipline.Anchor anchor, Vec3 camera, int colour, float time) {
        Vec3 eye = anchor.eye();
        Vec3 normal = net.minecraft.world.phys.Vec3.atLowerCornerOf(anchor.face().getNormal());
        if (!anchor.post()) {
            ChainRender.submitMark(poseStack, collector, eye.subtract(normal.scale(Zipline.CLAW_EYE)).subtract(camera), normal, colour, time);
        } else if (anchor.existing()) {
            ChainRender.submitMark(poseStack, collector, eye.subtract(camera), new Vec3(0, 1, 0), colour, time);
        } else {
            Vec3 foot = eye.subtract(normal.scale(Zipline.POST_EYE));
            Vec3[] pole = {foot.subtract(camera), foot.add(eye).scale(0.5).subtract(camera), eye.subtract(camera)};
            ChainRender.submitGhost(poseStack, collector, pole, camera, colour, time);
            ChainRender.submitMark(poseStack, collector, foot.add(normal.scale(0.02)).subtract(camera), normal, colour, time);
        }
    }

    private static void hud(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        Zipline.Aim current = aim;
        if (current == null) {
            return;
        }
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 12;
        int length = current.line() == null ? 0 : (int) Math.round(current.line().length());
        Component text = Component.translatable(current.status().key(), length);
        int colour = current.status() == Zipline.Status.NO_TARGET ? 0xFFB8C4B0 : 0xFF000000 | (current.ok() ? ChainRender.GOOD : ChainRender.BAD);
        graphics.centeredText(minecraft.font, text, x, y, colour);
    }
}
