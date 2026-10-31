package com.pockyl.hextale.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;

import com.pockyl.hextale.registry.ModFluids;

/**
 * What one sees with the head inside the Void's ichor: as in lava, nothing past an arm's length - a thick glowing
 * murk of dark magenta. Night vision lets one see a few blocks, as fire resistance does in lava.
 */
public final class IchorView {
    private static final Vector3f MURK = new Vector3f(0.36F, 0.04F, 0.3F);

    private IchorView() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public net.minecraft.resources.ResourceLocation getStillTexture() {
                return com.pockyl.hextale.Hextale.id("block/void_ichor_still");
            }

            @Override
            public net.minecraft.resources.ResourceLocation getFlowingTexture() {
                return com.pockyl.hextale.Hextale.id("block/void_ichor_flow");
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                    Vector3f fluidFogColor) {
                return new Vector3f(MURK);
            }

            @Override
            public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance,
                    float farDistance, FogShape shape) {
                if (camera.getEntity().isSpectator()) {
                    RenderSystem.setShaderFogStart(-8);
                    RenderSystem.setShaderFogEnd(renderDistance * 0.5F);
                } else if (camera.getEntity() instanceof LivingEntity living && living.hasEffect(MobEffects.NIGHT_VISION)) {
                    RenderSystem.setShaderFogStart(0);
                    RenderSystem.setShaderFogEnd(5);
                } else {
                    RenderSystem.setShaderFogStart(0.25F);
                    RenderSystem.setShaderFogEnd(1.5F);
                }
            }
        }, ModFluids.VOID_ICHOR_TYPE.get());
    }
}
