package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemGear;
import com.pockyl.hextale.chem.PneumaticBoots;
import com.pockyl.hextale.chem.Respirator;
import com.pockyl.hextale.network.PneumaticJumpPayload;
import com.pockyl.hextale.registry.ModSounds;

/**
 * The client side of the gear built into chem-steel armour: the boots' jump of the player at the keys (the jets come
 * after it, {@link ChemJetsClient}), and the breath of every respirator under water near the camera (a quiet exhale of
 * bubbles from both filters).
 */
public final class ChemGearClient {
    /** Blocks around the camera whose respirators are seen and heard. */
    private static final double SEEN = 24;
    /** Ticks of one breath, and the part of it that bubbles. */
    private static final int BREATH_TICKS = 70;
    private static final int EXHALE_TICKS = 12;
    private static boolean jumpHeld;
    private static boolean jumpSpent;

    private ChemGearClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ChemGearClient::beforeTick);
        NeoForge.EVENT_BUS.addListener(ChemGearClient::afterTick);
    }

    /** Whether the next press of jump in the air belongs to the boots; anything else on that key waits for this. */
    public static boolean airJumpLeft(LocalPlayer player) {
        return !jumpSpent && PneumaticBoots.canJump(player);
    }

    /** Runs before the player's own tick, so that a press taken by the boots can be hidden from the vanilla glider check. */
    private static void beforeTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused()) {
            return;
        }
        boolean down = minecraft.screen == null && minecraft.options.keyJump.isDown();
        boolean pressed = down && !jumpHeld;
        jumpHeld = down;
        if (PneumaticBoots.rests(player)) {
            jumpSpent = false;
        }
        ChemFuel fuel = ChemGear.power(player);
        boolean booted = pressed && fuel != null && airJumpLeft(player);
        if (booted) {
            airJump(player, fuel);
            hidePress(player);
        }
        ChemJetsClient.control(minecraft, player, down, pressed, booted);
    }

    /** The boots' jump: lifts the player at once and asks the server to pay for it. */
    static void airJump(LocalPlayer player, ChemFuel fuel) {
        jumpSpent = true;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, PneumaticBoots.jumpSpeed(fuel), motion.z);
        player.resetFallDistance();
        PacketDistributor.sendToServer(PneumaticJumpPayload.INSTANCE);
    }

    /** Makes this tick's press of jump look already held, so the vanilla glider check does not see it. */
    static void hidePress(LocalPlayer player) {
        player.input.jumping = true;
    }

    private static void afterTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        for (Player player : minecraft.level.players()) {
            if (player.distanceToSqr(minecraft.player) <= SEEN * SEEN && player.isEyeInFluid(FluidTags.WATER) && Respirator.works(player)) {
                breathe(minecraft, player);
            }
        }
    }

    private static void breathe(Minecraft minecraft, Player player) {
        int phase = (player.tickCount + player.getId() * 17) % BREATH_TICKS;
        if (phase >= EXHALE_TICKS || phase % 2 != 0) {
            return;
        }
        Vec3 face = Vec3.directionFromRotation(0, player.getYHeadRot());
        Vec3 side = new Vec3(-face.z, 0, face.x).scale(0.2);
        Vec3 mouth = player.getEyePosition().add(face.scale(0.32)).add(0, -0.2, 0);
        if (phase == 0) {
            minecraft.level.playLocalSound(mouth.x, mouth.y, mouth.z, ModSounds.CHEM_HELMET_BREATH.get(), SoundSource.PLAYERS, 1,
                    0.9F + player.getRandom().nextFloat() * 0.2F, false);
        }
        for (int filter = -1; filter <= 1; filter += 2) {
            Vec3 at = mouth.add(side.scale(filter));
            minecraft.level.addParticle(ParticleTypes.BUBBLE, at.x, at.y, at.z, face.x * 0.06 + side.x * filter * 0.1,
                    0.08 + player.getRandom().nextDouble() * 0.04, face.z * 0.06 + side.z * filter * 0.1);
        }
    }
}
