package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.pipeline.RenderPipeline;
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
import java.util.OptionalInt;
import java.util.function.BiConsumer;

/**
 * The Void while a shader pack is on: a pack runs its own programs for everything drawn inside the world, and ours
 * cannot be one of them (they read data, not colours, from the vertices). So the breaches seen in the frame are kept
 * and drawn with our own shaders once the pack has finished the picture, tested against the world's depth - the same
 * look as without a pack. They take no part in the pack's shadows, fog or light.
 */
public final class VoidOverlay {
    private static final List<Queued> QUEUE = new ArrayList<>();
    /** Set when drawing failed once: the renderer falls back to the plain look for the rest of the session. */
    private static boolean broken;

    private VoidOverlay() {
    }

    /** One breach of this frame: what to draw for its cells and its glow, in the pose it was submitted in. */
    record Queued(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> body, BiConsumer<VertexConsumer, PoseStack.Pose> halo) {
    }

    public static boolean works() {
        return !broken;
    }

    static void queue(PoseStack.Pose pose, BiConsumer<VertexConsumer, PoseStack.Pose> body, BiConsumer<VertexConsumer, PoseStack.Pose> halo) {
        QUEUE.add(new Queued(pose.copy(), body, halo));
    }

    public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        if (QUEUE.isEmpty()) {
            return;
        }
        List<Queued> queued = List.copyOf(QUEUE);
        QUEUE.clear();
        try {
            RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(event.getModelViewMatrix()),
                    new Vector4f(1, 1, 1, 1), new Vector3f(), new Matrix4f());
            draw(target, transforms, VoidPipelines.BODY, queued, true);
            draw(target, transforms, VoidPipelines.HALO, queued, false);
        } catch (RuntimeException e) {
            broken = true;
            Hextale.LOGGER.warn("Could not draw the Void over the shader pack's picture; it takes a plain look from now on", e);
        }
    }

    private static void draw(RenderTarget target, GpuBufferSlice transforms, RenderPipeline pipeline, List<Queued> queued, boolean body) {
        try (ByteBufferBuilder bytes = new ByteBufferBuilder(DefaultVertexFormat.ENTITY.getVertexSize() * 1024)) {
            BufferBuilder builder = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.ENTITY);
            for (Queued breach : queued) {
                (body ? breach.body() : breach.halo()).accept(builder, breach.pose());
            }
            try (MeshData mesh = builder.build()) {
                if (mesh == null) {
                    return;
                }
                int indexCount = mesh.drawState().indexCount();
                GpuBuffer vertices = RenderSystem.getDevice().createBuffer(() -> "hextale void overlay", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
                RenderSystem.AutoStorageIndexBuffer sequence = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
                GpuBuffer indices = sequence.getBuffer(indexCount);
                try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "hextale void overlay",
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
}
