package com.pockyl.hextale.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModFluids;

/**
 * What one sees with the head inside the Void's ichor: as in lava, nothing past an arm's length - a thick glowing
 * murk of dark magenta. Night vision lets one see a few blocks, as fire resistance does in lava.
 */
public final class IchorView {
    private static final Vector4f MURK = new Vector4f(0.36F, 0.04F, 0.3F, 1);

    private IchorView() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                    Vector4f fluidFogColor) {
                fluidFogColor.set(MURK);
            }

            @Override
            public void modifyFogRender(Camera camera, @Nullable FogEnvironment environment, float renderDistance, float partialTick, FogData fog) {
                if (camera.entity().isSpectator()) {
                    fog.environmentalStart = -8;
                    fog.environmentalEnd = renderDistance * 0.5F;
                } else if (camera.entity() instanceof LivingEntity living && living.hasEffect(MobEffects.NIGHT_VISION)) {
                    fog.environmentalStart = 0;
                    fog.environmentalEnd = 5;
                } else {
                    fog.environmentalStart = 0.25F;
                    fog.environmentalEnd = 1.5F;
                }
                fog.skyEnd = fog.environmentalEnd;
                fog.cloudEnd = fog.environmentalEnd;
            }
        }, ModFluids.VOID_ICHOR_TYPE.get());
    }
}
