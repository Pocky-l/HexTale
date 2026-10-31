package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.network.ShockwavePayload;
import com.pockyl.hextale.runework.WeaponRunes;

/**
 * A charged swing of a Shockwave weapon at the ground (a block below the player's middle, or the top of one) sends the
 * wave instead of starting to dig: the client asks, the server decides ({@link ShockwavePayload}). An uncharged swing,
 * or one during the weapon's rest, digs as usual.
 */
public final class WeaponInput {
    private WeaponInput() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WeaponInput::onInteraction);
    }

    private static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!event.isAttack() || player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !WeaponRunes.shockwaveReady(player)) {
            return;
        }
        boolean ground = hit.getDirection() == Direction.UP || hit.getLocation().y <= player.getY() + 0.6;
        if (!ground) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(true);
        player.resetAttackStrengthTicker();
        PacketDistributor.sendToServer(ShockwavePayload.INSTANCE);
    }
}
