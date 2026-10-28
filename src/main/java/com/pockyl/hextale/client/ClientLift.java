package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.block.HexLiftBlock;
import com.pockyl.hextale.network.HexLiftPayload;

/** Notices a jump or a sneak of the player standing on a Hex Lift and asks the server for the ride. */
public final class ClientLift {
    private static boolean jumpWasDown;
    private static boolean sneakWasDown;
    /** The lift the player last stood on, and the tick they did: a jump is seen only once they have left it. */
    private static BlockPos lastLift;
    private static int lastLiftTick;
    /** Ticks after leaving a lift that a jump still counts as made on it. */
    private static final int GRACE = 6;

    private ClientLift() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            jumpWasDown = false;
            sneakWasDown = false;
            return;
        }
        boolean jump = minecraft.options.keyJump.isDown();
        boolean sneak = minecraft.options.keyShift.isDown();
        BlockPos under = HexLiftBlock.under(player);
        if (under != null) {
            lastLift = under;
            lastLiftTick = player.tickCount;
        }
        BlockPos from = lastLift != null && player.tickCount - lastLiftTick <= GRACE ? lastLift : null;
        if (from != null) {
            if (jump && !jumpWasDown) {
                PacketDistributor.sendToServer(new HexLiftPayload(from, true));
                lastLift = null;
            } else if (sneak && !sneakWasDown && under != null) {
                PacketDistributor.sendToServer(new HexLiftPayload(under, false));
                lastLift = null;
            }
        }
        jumpWasDown = jump;
        sneakWasDown = sneak;
    }
}
