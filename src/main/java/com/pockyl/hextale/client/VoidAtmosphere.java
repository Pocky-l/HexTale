package com.pockyl.hextale.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.thevoid.TheVoid;

/**
 * The world grows wrong near the Void: the fog closes in and turns a dark violet, and the edges of sight darken in the
 * rhythm of its heartbeat. One strength for the nearest open breach, eased so it creeps in and fades out.
 */
public final class VoidAtmosphere {
    /** How far (blocks beyond its own reach, plus a share of its size) a breach is felt. */
    private static final float FELT = 10;
    private static final Identifier VIGNETTE = Identifier.withDefaultNamespace("textures/misc/vignette.png");
    private static float strength;
    private static float previous;

    private VoidAtmosphere() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Hextale.id("the_void_vignette"), VoidAtmosphere::vignette));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(VoidAtmosphere::fogColour);
        NeoForge.EVENT_BUS.addListener(VoidAtmosphere::fog);
    }

    private static void tick() {
        previous = strength;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            strength = 0;
            return;
        }
        float target = 0;
        AABB around = player.getBoundingBox().inflate(48);
        // A gate is the way home: it casts no gloom (only the breaches of our world do).
        for (TheVoid breach : minecraft.level.getEntitiesOfClass(TheVoid.class, around, breach -> !breach.gate())) {
            float reach = breach.layout().reach();
            float felt = FELT + breach.size() * 3;
            float distance = (float) breach.position().distanceTo(player.getEyePosition()) - reach;
            float near = 1 - Mth.clamp(distance / felt, 0, 1);
            target = Math.max(target, near * near * (3 - 2 * near) * breach.reveal(0));
        }
        strength += (target - strength) * 0.04F;
    }

    private static float strength(float partialTick) {
        return Mth.lerp(partialTick, previous, strength);
    }

    /** The same heartbeat as the breach's shader (seconds): two beats, then a long rest. */
    private static float heartbeat(float seconds) {
        float phase = seconds / 1.9F % 1;
        float a = (phase - 0.08F) / 0.045F;
        float b = (phase - 0.27F) / 0.06F;
        return (float) (Math.exp(-a * a) + 0.65 * Math.exp(-b * b));
    }

    private static void fogColour(ViewportEvent.ComputeFogColor event) {
        float s = strength((float) event.getPartialTick()) * 0.75F;
        if (s <= 0.001F) {
            return;
        }
        event.setRed(Mth.lerp(s, event.getRed(), 0.09F));
        event.setGreen(Mth.lerp(s, event.getGreen(), 0.025F));
        event.setBlue(Mth.lerp(s, event.getBlue(), 0.13F));
    }

    private static void fog(ViewportEvent.RenderFog event) {
        float s = strength((float) event.getPartialTick()) * 0.8F;
        if (s <= 0.001F) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(s, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), 36)));
        event.setNearPlaneDistance(Mth.lerp(s, event.getNearPlaneDistance(), Math.min(event.getNearPlaneDistance(), 2)));
    }

    private static void vignette(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        float s = strength(delta.getGameTimeDeltaPartialTick(false));
        if (s <= 0.003F || minecraft.level == null) {
            return;
        }
        float seconds = (minecraft.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false)) / 20F;
        float k = Mth.clamp(s * (0.8F + 0.2F * heartbeat(seconds)), 0, 1);
        // The vignette pipeline darkens by its colour: green most, blue least, so the dark edges lean to violet.
        int color = ARGB.colorFromFloat(1, k * 0.8F, k, k * 0.6F);
        graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0, 0, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(),
                graphics.guiHeight(), color);
    }
}
