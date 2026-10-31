package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.OptionalInt;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.pockyl.hextale.Hextale;

/**
 * The sky of the Void (the dimension's {@code neoforge:custom_skybox} is {@code hextale:void}): a box round the camera
 * painted by {@code shaders/core/void_sky.fsh} - the turning vortex overhead, the haze and far silhouettes at the
 * horizon, the abyss glowing below. With a shader pack on, the pack draws the world with its own programs and would
 * give the Void an overworld sky; so the pack's sky is left to it and ours is drawn over the finished picture
 * afterwards ({@link #OVER_PACK}), only where the world left the sky open.
 */
public final class VoidSky implements CustomSkyboxRenderer {
    public static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Hextale.id("pipeline/void_sky"))
            .withVertexShader(Hextale.id("core/void_sky"))
            .withFragmentShader(Hextale.id("core/void_sky"))
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withCull(false)
            .build();
    /** The same sky on the far plane, drawn after a shader pack has finished the picture, only where nothing stands. */
    public static final RenderPipeline OVER_PACK = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
            .withLocation(Hextale.id("pipeline/void_sky_over_pack"))
            .withVertexShader(Hextale.id("core/void_sky_far"))
            .withFragmentShader(Hextale.id("core/void_sky"))
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withCull(false)
            .build();
    /**
     * Half the box's side: far enough that the view bobbing's small sideways shift of the camera does not slide the
     * painted sky against the world. The shaders pin the box onto the far plane, so it is never clipped there.
     */
    private static final float SIZE = 512;
    /** Set when drawing failed once: the plain sky is left for the rest of the session. */
    private static boolean broken;

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterRenderPipelinesEvent event) -> {
            event.registerPipeline(PIPELINE);
            event.registerPipeline(OVER_PACK);
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(VoidSky::overPack);
        modBus.addListener((RegisterCustomEnvironmentEffectRendererEvent event) -> event.registerSkyboxRenderer(Hextale.id("void"), new VoidSky()));
    }

    @Override
    public boolean renderSky(LevelRenderState levelState, SkyRenderState skyState, Matrix4fc modelView, Runnable setupFog) {
        if (broken || com.pockyl.hextale.client.IrisCompat.shadersOn()) {
            return false;
        }
        try {
            setupFog.run();
            draw(PIPELINE, modelView);
            return true;
        } catch (RuntimeException e) {
            broken = true;
            Hextale.LOGGER.warn("Could not draw the Void's sky; the plain one stays from now on", e);
            return false;
        }
    }

    /** With a shader pack on, in the Void: our sky over the pack's, where the world left it open. */
    private static void overPack(net.neoforged.neoforge.client.event.RenderLevelStageEvent.AfterLevel event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (broken || minecraft.level == null || !minecraft.level.dimension().equals(com.pockyl.hextale.thevoid.realm.VoidPassage.LEVEL)
                || !com.pockyl.hextale.client.IrisCompat.shadersOn()) {
            return;
        }
        try {
            draw(OVER_PACK, event.getModelViewMatrix());
        } catch (RuntimeException e) {
            broken = true;
            Hextale.LOGGER.warn("Could not draw the Void's sky over the shader pack; the pack's sky stays from now on", e);
        }
    }

    private static void draw(RenderPipeline pipeline, Matrix4fc modelView) {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelView), new Vector4f(1, 1, 1, 1),
                new Vector3f(), new Matrix4f());
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(DefaultVertexFormat.POSITION.getVertexSize() * 24)) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            box(builder);
            try (MeshData mesh = builder.build()) {
                if (mesh == null) {
                    return;
                }
                int indexCount = mesh.drawState().indexCount();
                GpuBuffer vertices = RenderSystem.getDevice().createBuffer(() -> "hextale void sky", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                RenderSystem.AutoStorageIndexBuffer sequence = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                GpuBuffer indices = sequence.getBuffer(indexCount);
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "hextale void sky",
                        target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(pipeline);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transforms);
                    pass.setVertexBuffer(0, vertices);
                    pass.setIndexBuffer(indices, sequence.type());
                    pass.drawIndexed(0, 0, indexCount, 1);
                }
                vertices.close();
            }
        }
    }

    /** The six inner faces of a box round the camera. */
    private static void box(BufferBuilder builder) {
        float s = SIZE;
        float[][] faces = {
                {-s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s},
                {-s, s, -s, -s, s, s, s, s, s, s, s, -s},
                {-s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s},
                {-s, -s, s, s, -s, s, s, s, s, -s, s, s},
                {-s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s},
                {s, -s, -s, s, s, -s, s, s, s, s, -s, s}};
        for (float[] face : faces) {
            for (int i = 0; i < 12; i += 3) {
                builder.addVertex(face[i], face[i + 1], face[i + 2]);
            }
        }
    }
}
