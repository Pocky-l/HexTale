package com.pockyl.hextale.port;

import net.minecraft.server.level.ServerPlayer;

/**
 * The keys a player holds, as the server last heard them. 1.21.1 hears them only from a passenger
 * ({@code ServerboundPlayerInputPacket}), which is all the mod asks about.
 *
 * <p>port-of: net.minecraft.world.entity.player.Input
 */
public record Input(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift, boolean sprint) {
    public static final Input EMPTY = new Input(false, false, false, false, false, false, false);
    private static final float PRESSED = 0.01F;

    /** The 26.x {@code ServerPlayer#getLastClientInput}. */
    public static Input lastClient(ServerPlayer player) {
        return new Input(player.zza > PRESSED, player.zza < -PRESSED, player.xxa > PRESSED, player.xxa < -PRESSED, player.jumping,
                player.isShiftKeyDown(), player.isSprinting());
    }

    /** The 26.x {@code ServerPlayer#setLastClientInput} for game tests, also before the player gets on anything. */
    public static void setLast(ServerPlayer player, Input input) {
        player.xxa = (input.left ? 1 : 0) - (input.right ? 1 : 0);
        player.zza = (input.forward ? 1 : 0) - (input.backward ? 1 : 0);
        player.jumping = input.jump;
        player.setShiftKeyDown(input.shift);
    }
}
