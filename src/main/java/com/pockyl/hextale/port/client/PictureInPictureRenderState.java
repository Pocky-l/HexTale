package com.pockyl.hextale.port.client;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

/**
 * A 3D scene in a GUI box, as 26.x describes it.
 *
 * <p>port-of: net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
 */
public interface PictureInPictureRenderState {
    int x0();

    int y0();

    int x1();

    int y1();

    float scale();

    @Nullable ScreenRectangle scissorArea();

    @Nullable ScreenRectangle bounds();

    static @Nullable ScreenRectangle getBounds(int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
        ScreenRectangle bounds = new ScreenRectangle(x0, y0, x1 - x0, y1 - y0);
        return scissorArea != null ? scissorArea.intersection(bounds) : bounds;
    }
}
