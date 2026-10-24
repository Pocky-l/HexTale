package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * A key press as 26.x hands it to screens; on 1.21.1 built from (keyCode, scanCode, modifiers). Key codes are GLFW
 * numbers, which the mod only compares with {@code InputConstants.KEY_*}.
 *
 * <p>port-of: net.minecraft.client.input.KeyEvent
 */
public record KeyEvent(int key, int scancode, int modifiers) {
    public int input() {
        return key;
    }

    public boolean isEscape() {
        return key == InputConstants.KEY_ESCAPE;
    }

    public boolean isConfirmation() {
        return key == InputConstants.KEY_RETURN || key == InputConstants.KEY_SPACE || key == InputConstants.KEY_NUMPADENTER;
    }

    public boolean hasShiftDown() {
        return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
    }

    public boolean hasControlDown() {
        return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    public boolean hasAltDown() {
        return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }
}
