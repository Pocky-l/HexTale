package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.network.StaffCastPayload;
import com.pockyl.hextale.runework.RiftStaff;

/**
 * The Rift Staff's left click: it never digs and never strikes with the shaft; a click casts when the attack indicator
 * is full, and the button held down keeps casting in the staff's rhythm. The server decides ({@link StaffCastPayload}).
 */
public final class StaffInput {
    /** The indicator's fill at which a cast goes (a hair short of full, so a held button keeps the rhythm). */
    private static final float READY = 0.95F;

    private StaffInput() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StaffInput::onInteraction);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!event.isAttack() || player == null || event.getHand() != InteractionHand.MAIN_HAND || !RiftStaff.is(player.getMainHandItem())) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        tryCast(player);
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.screen() != null || !minecraft.options.keyAttack.isDown() || !RiftStaff.is(player.getMainHandItem())) {
            return;
        }
        tryCast(player);
    }

    private static void tryCast(LocalPlayer player) {
        if (player.isUsingItem() || player.isSpectator() || player.getAttackStrengthScale(0) < READY) {
            return;
        }
        player.resetAttackStrengthTicker();
        player.swing(InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
        ClientPacketDistributor.sendToServer(StaffCastPayload.INSTANCE);
    }
}
