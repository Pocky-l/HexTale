package com.pockyl.hextale.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.neoforged.fml.ModList;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.HexGlow;
import com.pockyl.hextale.client.render.VoidPipelines;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * Iris (optional, reached by reflection - no compile dependency). With a shader pack on, Iris draws every render
 * pipeline with a program of the pack and needs to be told which one fits our own pipelines; an unknown pipeline is
 * drawn with our shader straight into the pack's buffers, the shadow map included, which wrecks the picture. The
 * Void's own shaders read data, not colours, from their vertices, so with a pack on the breach is drawn after the pack
 * has finished the picture ({@link com.pockyl.hextale.client.render.VoidOverlay}).
 */
public final class IrisCompat {
    private static @Nullable Object api;
    private static @Nullable Method packInUse;
    private static @Nullable Method shadowPass;

    private IrisCompat() {
    }

    /** Tells Iris which of its programs draws each of our pipelines (client setup). */
    public static void init() {
        if (!ModList.get().isLoaded("iris")) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = apiClass.getMethod("getInstance").invoke(null);
            Class<?> program = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Class<?> shadowProgram = Class.forName("net.irisshaders.iris.api.v0.IrisShadowProgram");
            Method assign = apiClass.getMethod("assignPipeline", RenderPipeline.class, program);
            Method assignShadow = apiClass.getMethod("assignPipelineShadow", RenderPipeline.class, shadowProgram);
            assign(assign, assignShadow, instance, program, shadowProgram, HexGlow.PIPELINE, "BASIC", "SHADOW_LIGHTNING");
            assign(assign, assignShadow, instance, program, shadowProgram, HexGlow.TEXTURED_PIPELINE, "EMISSIVE_ENTITIES", "SHADOW_ENTITIES");
            // Not drawn while a pack is on; mapped all the same so a frame caught between the two never draws raw.
            assign(assign, assignShadow, instance, program, shadowProgram, VoidPipelines.BODY, "ENTITIES", "SHADOW_ENTITIES");
            assign(assign, assignShadow, instance, program, shadowProgram, VoidPipelines.HALO, "ENTITIES_TRANSLUCENT", "SHADOW_ENTITIES");
            packInUse = apiClass.getMethod("isShaderPackInUse");
            shadowPass = apiClass.getMethod("isRenderingShadowPass");
            api = instance;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Hextale.LOGGER.warn("Could not reach the Iris API; shader packs may draw hextale effects oddly", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void assign(Method assign, Method assignShadow, Object api, Class<?> program, Class<?> shadowProgram,
                               RenderPipeline pipeline, String main, String shadow) throws ReflectiveOperationException {
        assign.invoke(api, pipeline, Enum.valueOf((Class<Enum>) program, main));
        assignShadow.invoke(api, pipeline, Enum.valueOf((Class<Enum>) shadowProgram, shadow));
    }

    /** Whether the pack is drawing its shadow map right now (entities are submitted again for it). */
    public static boolean shadowPass() {
        if (api == null || shadowPass == null) {
            return false;
        }
        try {
            return (boolean) shadowPass.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** Whether a shader pack draws the world right now. */
    public static boolean shadersOn() {
        if (api == null || packInUse == null) {
            return false;
        }
        try {
            return (boolean) packInUse.invoke(api);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
