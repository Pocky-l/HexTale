package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.block.HexLiftBlock;
import com.pockyl.hextale.network.HexLiftPayload;

/** Notices a jump or a sneak of the player standing on a Hex Lift and asks the server for the ride. */
public final class ClientLift {
    private static boolean jumpWasDown;
    private static boolean sneakWasDown;

    private ClientLift() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gui.screen() != null) {
            jumpWasDown = false;
            sneakWasDown = false;
            return;
        }
        boolean jump = minecraft.options.keyJump.isDown();
        boolean sneak = minecraft.options.keyShift.isDown();
        if (HexLiftBlock.under(player) != null) {
            if (jump && !jumpWasDown) {
                ClientPacketDistributor.sendToServer(new HexLiftPayload(true));
            } else if (sneak && !sneakWasDown) {
                ClientPacketDistributor.sendToServer(new HexLiftPayload(false));
            }
        }
        jumpWasDown = jump;
        sneakWasDown = sneak;
    }
}
