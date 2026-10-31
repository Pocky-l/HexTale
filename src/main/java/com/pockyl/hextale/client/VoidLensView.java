package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.thevoid.realm.VoidLensItem;

/**
 * Looking through a Void Lens: the haze draws back (the fog's far edge moves out to {@link #CLEAR} blocks, eased in
 * and out), and what the Void hides glints in the lens's field: riftmetal violet, hollow crystals white, worn skin a
 * warning pink.
 */
public final class VoidLensView {
    private static final float CLEAR = 150;
    private static final int REACH = 32;
    private static float strength;
    private static float previous;
    private static final RandomSource RANDOM = RandomSource.create();

    private VoidLensView() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
        NeoForge.EVENT_BUS.addListener(VoidLensView::fog);
    }

    private static void tick() {
        previous = strength;
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        boolean looking = player != null && minecraft.level != null && VoidLensItem.looking(player);
        strength += ((looking ? 1 : 0) - strength) * 0.12F;
        if (!looking) {
            return;
        }
        // Sample the field of view: points along random rays inside the lens's narrow cone.
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 260; i++) {
            Vec3 ray = look.add((RANDOM.nextDouble() - 0.5) * 0.5, (RANDOM.nextDouble() - 0.5) * 0.5, (RANDOM.nextDouble() - 0.5) * 0.5).normalize();
            double distance = 3 + RANDOM.nextDouble() * REACH;
            at.set(eye.x + ray.x * distance, eye.y + ray.y * distance, eye.z + ray.z * distance);
            BlockState state = minecraft.level.getBlockState(at);
            int colour;
            if (state.is(ModBlocks.RIFT_ORE.get())) {
                colour = 0xB07CFF;
            } else if (state.is(ModBlocks.HOLLOW_CRYSTAL_CLUSTER.get())) {
                colour = 0xF2F0FA;
            } else if (state.is(ModBlocks.WORN_VOID_MEMBRANE.get())) {
                colour = 0xFF6FA8;
            } else {
                continue;
            }
            minecraft.level.addParticle(ModParticles.glint(colour), at.getX() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.9,
                    at.getY() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.9, at.getZ() + 0.5 + (RANDOM.nextDouble() - 0.5) * 0.9, 0, 0.01, 0);
        }
    }

    private static void fog(ViewportEvent.RenderFog event) {
        float s = Mth.lerp((float) event.getPartialTick(), previous, strength);
        if (s <= 0.001F) {
            return;
        }
        float far = event.getFarPlaneDistance();
        if (far < CLEAR) {
            event.setFarPlaneDistance(Mth.lerp(s, far, CLEAR));
            event.setNearPlaneDistance(Mth.lerp(s, event.getNearPlaneDistance(), CLEAR * 0.6F));
        }
    }
}
