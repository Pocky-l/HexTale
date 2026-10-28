package com.pockyl.hextale.client;

import net.neoforged.fml.ModList;

import com.pockyl.hextale.Hextale;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * Iris (optional, reached by reflection - no compile dependency). With a shader pack on, Iris draws every render
 * pipeline with a program of the pack and needs to be told which one fits our own pipelines; an unknown pipeline is
 * drawn with our shader straight into the pack's buffers, the shadow map included, which wrecks the picture. The
 * Void's own shaders read data, not colours, from their vertices, so with a pack on the breach is drawn after the pack
 * has finished the picture ({@link com.pockyl.hextale.client.render.VoidOverlay}).
 *
 * <p>1.21.1: Iris for 1.21.1 has no render pipelines to assign; the mod's glow is drawn with vanilla shaders Iris
 * already replaces, so only whether a pack is on (and drawing its shadows) is asked.
 */
public final class IrisCompat {
    private static @Nullable Object api;
    private static @Nullable Method packInUse;
    private static @Nullable Method shadowPass;

    private IrisCompat() {
    }

    /** Finds the Iris API (client setup). */
    public static void init() {
        if (!ModList.get().isLoaded("iris")) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = apiClass.getMethod("getInstance").invoke(null);
            packInUse = apiClass.getMethod("isShaderPackInUse");
            shadowPass = apiClass.getMethod("isRenderingShadowPass");
            api = instance;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Hextale.LOGGER.warn("Could not reach the Iris API; shader packs may draw hextale effects oddly", e);
        }
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
