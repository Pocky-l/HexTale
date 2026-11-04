package com.pockyl.hextale.port.client;

import net.minecraft.util.StringUtil;

/**
 * A typed character as 26.x hands it to screens; on 1.21.1 built from (codePoint, modifiers) of {@code charTyped}.
 *
 * <p>port-of: net.minecraft.client.input.CharacterEvent
 */
public record CharacterEvent(int codepoint, int modifiers) {
    public boolean isAllowedChatCharacter() {
        return StringUtil.isAllowedChatCharacter((char) codepoint);
    }

    public String codepointAsString() {
        return Character.toString(codepoint);
    }
}
