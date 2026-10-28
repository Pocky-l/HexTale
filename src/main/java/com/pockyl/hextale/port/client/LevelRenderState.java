package com.pockyl.hextale.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * The parts of the 26.x level render state the mod's custom geometry reads, taken from the 1.21.1 frame.
 *
 * <p>port-of: net.minecraft.client.renderer.state.level.LevelRenderState
 */
public class LevelRenderState {
    public CameraRenderState cameraRenderState = new CameraRenderState();
    public long gameTime;
    public float worldPartialTicks;

    /** The state of the frame being drawn. */
    public static LevelRenderState current(float partialTick) {
        LevelRenderState state = new LevelRenderState();
        state.cameraRenderState = CameraRenderState.current();
        ClientLevel level = Minecraft.getInstance().level;
        state.gameTime = level == null ? 0 : level.getGameTime();
        state.worldPartialTicks = partialTick;
        return state;
    }
}
