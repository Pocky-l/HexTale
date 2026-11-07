package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.pockyl.hextale.Hextale;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiConsumer;

/**
 * The Void and the Hex Vault's gate while a shader pack is on: a pack runs its own programs for everything drawn inside
 * the world, and ours cannot be one of them (they read data, not colours, from the vertices). So what is seen in the
 * frame is kept and drawn with our own shaders once the pack has finished the picture, tested against the world's depth -
 * the same look as without a pack. It takes no part in the pack's shadows, fog or light. What is solid goes first, so the
 * light added after it shows over it.
 */
public final class VoidOverlay {
    private static final List<Part> BODIES = new ArrayList<>();
    private static final List<Part> SPACES = new ArrayList<>();
    private static final List<Part> HALOS = new ArrayList<>();
    private static final List<Part> LIGHTS = new ArrayList<>();
    /** Set when drawing failed once: the renderer falls back to the plain look for the rest of the session. */
    private static boolean broken;

    private VoidOverlay() {
    }

    /** One thing to draw in this frame, in the pose it was submitted in. */
    record Part(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> geometry) {
    }

    public static boolean works() {
        return !broken;
    }

    /** One breach: its cells and its glow. */
    static void queue(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> body, BiConsumer<VertexConsumer, PoseStack.Pose> halo) {
        PoseStack.Pose kept = pose.copy();
        BODIES.add(new Part(kept, body));
        HALOS.add(new Part(kept, halo));
    }

    /** The space behind one Hex Vault's door. */
    static void queueSpace(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> body) {
        SPACES.add(new Part(pose.copy(), body));
    }

    /** Additive light in {@link HexGlow}'s format, position and colour. */
    static void queueLight(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> light) {
        LIGHTS.add(new Part(pose.copy(), light));
    }

    public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        if (BODIES.isEmpty() && SPACES.isEmpty() && LIGHTS.isEmpty()) {
            return;
        }
        try {
            RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(event.getModelViewMatrix()),
                    new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
            draw(target, transforms, VoidPipelines.BODY, DefaultVertexFormat.ENTITY, BODIES);
            draw(target, transforms, VaultSpacePipeline.PIPELINE, DefaultVertexFormat.ENTITY, SPACES);
            draw(target, transforms, VoidPipelines.HALO, DefaultVertexFormat.ENTITY, HALOS);
            draw(target, transforms, HexGlow.PIPELINE, DefaultVertexFormat.POSITION_COLOR, LIGHTS);
        } catch (RuntimeException e) {
            broken = true;
            Hextale.LOGGER.warn("Could not draw the Void over the shader pack's picture; it takes a plain look from now on", e);
        } finally {
            BODIES.clear();
            SPACES.clear();
            HALOS.clear();
            LIGHTS.clear();
        }
    }

    private static void draw(RenderTarget target, GpuBufferSlice transforms, RenderPipeline pipeline, VertexFormat format, List<Part> parts) {
        if (parts.isEmpty()) {
            return;
        }
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(format.getVertexSize() * 1024)) {
            BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, format);
            for (Part part : parts) {
                part.geometry().accept(builder, part.pose());
            }
            try (MeshData mesh = builder.build()) {
                if (mesh == null) {
                    return;
                }
                int indexCount = mesh.drawState().indexCount();
                GpuBuffer vertices = RenderSystem.getDevice().createBuffer(() -> "hextale void overlay", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                RenderSystem.AutoStorageIndexBuffer sequence = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
                GpuBuffer indices = sequence.getBuffer(indexCount);
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "hextale void overlay",
                        target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
                    pass.setPipeline(RenderSystem.getCompiledPipeline(pipeline));
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transforms);
                    pass.setVertexBuffer(0, vertices.slice());
                    pass.setIndexBuffer(indices, sequence.type());
                    pass.drawIndexed(indexCount, 1, 0, 0, 0);
                }
                vertices.close();
            }
        }
    }
}
