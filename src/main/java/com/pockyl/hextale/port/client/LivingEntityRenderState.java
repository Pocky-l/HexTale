package com.pockyl.hextale.port.client;

/**
 * What a 26.x living entity renderer extracts before drawing (the parts the mod's models read).
 *
 * <p>port-of: net.minecraft.client.renderer.entity.state.LivingEntityRenderState
 */
public class LivingEntityRenderState extends EntityRenderState {
    public float bodyRot;
    /** Head turn relative to the body (degrees). */
    public float yRot;
    public float xRot;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float scale = 1.0F;
    public boolean isInvisible;
    public int outlineColor;
}
