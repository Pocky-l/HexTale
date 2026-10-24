package com.pockyl.hextale.port;

/**
 * Leaves have no ambient sounds on 1.21.1; this only keeps the 26.x constructor arguments.
 *
 * <p>port-of: net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer
 */
public final class AmbientLeavesBlockSoundPlayer {
    private static final AmbientLeavesBlockSoundPlayer NONE = new AmbientLeavesBlockSoundPlayer();

    private AmbientLeavesBlockSoundPlayer() {
    }

    public static AmbientLeavesBlockSoundPlayer noAmbientSound() {
        return NONE;
    }
}
