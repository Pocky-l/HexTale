package com.pockyl.hextale.port.client;

/**
 * What a 26.x entity renderer extracts before drawing (the parts the mod reads).
 *
 * <p>port-of: net.minecraft.client.renderer.entity.state.EntityRenderState
 */
public class EntityRenderState {
    public double x;
    public double y;
    public double z;
    public float ageInTicks;
    public float partialTick;
    public int lightCoords;
}
