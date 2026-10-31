package com.pockyl.hextale.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.util.Util;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;

import com.pockyl.hextale.thevoid.realm.VoidPassage;

/**
 * Passing through a breach: instead of the loading screen, the fall down the breach's throat ({@link VoidPassageFx#tunnel})
 * - its wall turning, streaks and rings of cells rushing past toward a heart that beats. Into the Void it burns magenta
 * round a dark heart; out of it it warms to gold. It lasts at least {@link #LEAST} seconds however fast the other side
 * loads, so the passage is felt; when it closes the arrival plays on the other side.
 */
public class VoidTransitionScreen extends LevelLoadingScreen {
    /** The shortest fall down the throat, in seconds. */
    private static final float LEAST = 2.2F;
    private final boolean home;
    private final long start = Util.getMillis();
    private boolean ready;

    public VoidTransitionScreen(LevelLoadTracker tracker, Reason reason, boolean home) {
        super(tracker, reason);
        this.home = home;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterDimensionTransitionScreenEvent event) -> {
            event.registerIncomingEffect(VoidPassage.LEVEL, (tracker, reason) -> new VoidTransitionScreen(tracker, reason, false));
            event.registerOutgoingEffect(VoidPassage.LEVEL, (tracker, reason) -> new VoidTransitionScreen(tracker, reason, true));
        });
        VoidPassageFx.registerLayer(modBus);
    }

    private float seconds() {
        return (Util.getMillis() - start) / 1000F;
    }

    @Override
    public void tick() {
        super.tick();
        if (ready && seconds() >= LEAST) {
            ready = false;
            VoidPassageFx.arrive(home);
            super.onClose();
        }
    }

    /** The other side is ready: it waits for the fall to be long enough. */
    @Override
    public void onClose() {
        ready = true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractBackground(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.fill(0, 0, width, height, home ? 0xFF2A1E2E : 0xFF0A0310);
        VoidPassageFx.tunnel(graphics, width, height, Util.getMillis() / 1000F * 1.4F, 1, home, false);
    }
}
