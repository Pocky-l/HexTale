package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.realm.VoidFog;
import com.pockyl.hextale.thevoid.realm.VoidPassage;

/**
 * A fog bank of the Void rolling over the player ({@link VoidFog}): the fog closes in to a few blocks and turns pale
 * lavender, with a long low swell of sound as it comes. Also the Void's fog by pitch: looking down into the abyss, the
 * haze takes on its magenta glow; looking up, the cooler violet of the funnel overhead.
 */
public final class VoidFogBanks {
    /** How close the fog comes at a bank's thickest. */
    private static final float THICK = 9;
    private static float strength;
    private static float previous;
    private static boolean rolling;

    private VoidFogBanks() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(VoidFogBanks::fog);
        NeoForge.EVENT_BUS.addListener(VoidFogBanks::fogColour);
    }

    private static boolean inVoid(Minecraft minecraft) {
        return minecraft.player != null && minecraft.level != null && minecraft.level.dimension().equals(VoidPassage.LEVEL);
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        previous = strength;
        if (!inVoid(minecraft)) {
            strength = 0;
            rolling = false;
            return;
        }
        float bank = VoidFog.bank(minecraft.player.getX(), minecraft.player.getZ(), minecraft.level.getGameTime());
        strength += (bank - strength) * 0.1F;
        if (bank > 0.05F && !rolling) {
            rolling = true;
            minecraft.player.playSound(ModSounds.VOID_FOG_BANK.get(), 0.9F, 1.0F);
        } else if (bank <= 0) {
            rolling = false;
        }
    }

    private static void fog(ViewportEvent.RenderFog event) {
        float s = Mth.lerp((float) event.getPartialTick(), previous, strength);
        if (s <= 0.001F) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(s, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), THICK)));
        event.setNearPlaneDistance(Mth.lerp(s, event.getNearPlaneDistance(), 0));
        event.setCanceled(true);
    }

    private static void fogColour(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!inVoid(minecraft)) {
            return;
        }
        // By pitch: down toward the abyss's magenta, up toward the funnel's violet.
        float pitch = Mth.clamp(event.getCamera().getXRot() / 90F, -1, 1);
        float r = event.getRed(), g = event.getGreen(), b = event.getBlue();
        if (pitch > 0) {
            float k = pitch * pitch * 0.45F;
            r = Mth.lerp(k, r, 0.55F);
            g = Mth.lerp(k, g, 0.16F);
            b = Mth.lerp(k, b, 0.48F);
        } else {
            float k = pitch * pitch * 0.3F;
            r = Mth.lerp(k, r, 0.26F);
            g = Mth.lerp(k, g, 0.2F);
            b = Mth.lerp(k, b, 0.42F);
        }
        float s = Mth.lerp((float) event.getPartialTick(), previous, strength);
        if (s > 0.001F) {
            r = Mth.lerp(s, r, 0.72F);
            g = Mth.lerp(s, g, 0.66F);
            b = Mth.lerp(s, b, 0.8F);
        }
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }
}
