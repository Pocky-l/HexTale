package com.pockyl.hextale.port.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The camera as 26.x hands it to renderers, read from the 1.21.1 main camera.
 *
 * <p>port-of: net.minecraft.client.renderer.state.level.CameraRenderState
 */
public class CameraRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public Vec3 pos = Vec3.ZERO;
    public float xRot;
    public float yRot;
    public boolean initialized;
    public boolean isFirstPerson;
    public Quaternionf orientation = new Quaternionf();

    /** The state of the main camera this frame. */
    public static CameraRenderState current() {
        return of(Minecraft.getInstance().gameRenderer.getMainCamera());
    }

    public static CameraRenderState of(Camera camera) {
        CameraRenderState state = new CameraRenderState();
        state.blockPos = camera.getBlockPosition();
        state.pos = camera.getPosition();
        state.xRot = camera.getXRot();
        state.yRot = camera.getYRot();
        state.initialized = camera.isInitialized();
        state.isFirstPerson = !camera.isDetached();
        state.orientation = new Quaternionf(camera.rotation());
        return state;
    }
}
