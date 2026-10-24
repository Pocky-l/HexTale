package com.pockyl.hextale.port.client;

import net.minecraft.client.gui.screens.Screen;

/**
 * A mouse click as 26.x hands it to screens; on 1.21.1 built from the (x, y, button) arguments. Buttons are GLFW
 * numbers, which the mod only compares with {@code InputConstants.MOUSE_BUTTON_*}.
 *
 * <p>port-of: net.minecraft.client.input.MouseButtonEvent
 */
public record MouseButtonEvent(double x, double y, int button) {
    public int input() {
        return button;
    }

    public boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    public boolean hasControlDown() {
        return Screen.hasControlDown();
    }

    public boolean hasAltDown() {
        return Screen.hasAltDown();
    }
}
