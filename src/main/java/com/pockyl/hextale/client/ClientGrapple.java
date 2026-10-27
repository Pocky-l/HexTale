package com.pockyl.hextale.client;

import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import com.pockyl.hextale.item.GrappleGauntletItem;
import com.pockyl.hextale.network.GrappleRopePayload;

/** The Grapple Gauntlet's rope on the client: jump reels in, sneak lets out, and the length is reported to the server. */
public final class ClientGrapple {
    private ClientGrapple() {
    }

    public static void register() {
        GrappleGauntletItem.reelInput = player -> {
            if (!(player instanceof LocalPlayer local)) {
                return 0;
            }
            if (local.input.keyPresses.jump()) {
                return 1;
            }
            return local.input.keyPresses.shift() ? -1 : 0;
        };
        GrappleGauntletItem.reportLength = (hook, length) ->
                ClientPacketDistributor.sendToServer(new GrappleRopePayload(hook.getId(), length.floatValue()));
    }
}
