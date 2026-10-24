package com.pockyl.hextale.port.client;

import com.pockyl.hextale.Hextale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Method;

/**
 * Screen behaviour 26.x has in the base classes: a container screen's hovered-slot tooltip is drawn after the screen
 * (1.21.1 leaves that call to each screen). The protected {@code renderTooltip} is called reflectively, so the screens'
 * overrides keep their 26.x shape.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID, value = Dist.CLIENT)
public final class PortScreens {
    private static final Method RENDER_TOOLTIP;

    static {
        try {
            RENDER_TOOLTIP = AbstractContainerScreen.class.getDeclaredMethod("renderTooltip", GuiGraphics.class, int.class, int.class);
            RENDER_TOOLTIP.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private PortScreens() {
    }

    @SubscribeEvent
    static void afterRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen
                && screen.getClass().getName().startsWith("com.pockyl.hextale.")) {
            try {
                RENDER_TOOLTIP.invoke(screen, event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
