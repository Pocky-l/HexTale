package com.pockyl.hextale.client;

import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

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
            if (local.input.jumping) {
                return 1;
            }
            return local.input.shiftKeyDown ? -1 : 0;
        };
        GrappleGauntletItem.reportLength = (hook, length) ->
                PacketDistributor.sendToServer(new GrappleRopePayload(hook.getId(), length.floatValue()));
        // 26.x: the gauntlet's use effects let its wearer walk at full speed while using it; 1.21.1 slows every use to a
        // fifth after this event, so the input is raised by as much first.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) -> {
            if (event.getEntity().isUsingItem() && !event.getEntity().isPassenger()
                    && event.getEntity().getUseItem().getItem() instanceof GrappleGauntletItem) {
                event.getInput().leftImpulse *= 5.0F;
                event.getInput().forwardImpulse *= 5.0F;
            }
        });
    }
}
