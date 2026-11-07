package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;

import java.io.IOException;

/**
 * The space behind a Hex Vault's door: one opaque hexagon whose depth is painted by {@code shaders/core/vault_space.fsh}. UV0 =
 * the point in the door's own plane (blocks, middle = 0); Color: r = how far the door is open, g = how full the vault is.
 *
 * <p>1.21.1: a core shader program registered in {@link RegisterShadersEvent} and a render type over it.
 */
public final class VaultSpacePipeline extends RenderType {
    private static @Nullable ShaderInstance shader;

    public static final RenderType TYPE = create("hextale_vault_space", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
            1536, false, false, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> shader))
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private VaultSpacePipeline(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean crumbling, boolean sort,
                               Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sort, setup, clear);
    }

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), Hextale.id("vault_space"), DefaultVertexFormat.NEW_ENTITY),
                    registered -> shader = registered);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
