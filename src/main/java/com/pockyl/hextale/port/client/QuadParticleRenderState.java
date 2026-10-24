package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Where a 26.x particle puts its quads; on 1.21.1 the particle sheet's buffer.
 *
 * <p>port-of: net.minecraft.client.renderer.state.level.QuadParticleRenderState
 */
public record QuadParticleRenderState(VertexConsumer buffer) {
}
