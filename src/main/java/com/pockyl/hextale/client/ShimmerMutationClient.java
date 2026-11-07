package com.pockyl.hextale.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import com.pockyl.hextale.port.client.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ShimmerMutations;
import com.pockyl.hextale.chem.ShimmerRide;
import com.pockyl.hextale.client.render.ShimmerOverlayLayer;
import com.pockyl.hextale.client.render.ShimmerPose;
import com.pockyl.hextale.registry.ModParticles;

/** What a client adds to a mutated animal: its layers, its motes, which thin out on the last day, and a Raptor's wind bar. */
public final class ShimmerMutationClient {
    private static final int MOTE_TICKS = 8;
    private static final int LAST_DAY_MOTE_TICKS = 30;
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    /** Width of the bar inside its rim; the frame's tick for low wind sits at {@link #BAR_LOW_WIND} of it. */
    private static final int BAR_INSIDE = 180;
    private static final float BAR_LOW_WIND = 0.25F;
    private static final ResourceLocation BAR_FRAME = Hextale.id("hud/raptor_wind_frame");
    private static final ResourceLocation BAR_FILL = Hextale.id("hud/raptor_wind_fill");
    private static final ResourceLocation BAR_LOW = Hextale.id("hud/raptor_wind_low");

    private ShimmerMutationClient() {
    }

    public static void register(IEventBus modBus) {
        ShimmerRide.leanFrom(ShimmerPose::pitch);
        NeoForge.EVENT_BUS.addListener(ShimmerMutationClient::motes);
        modBus.addListener((EntityRenderersEvent.AddLayers event) -> ShimmerOverlayLayer.addLayers(event));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR, Hextale.id("raptor_stamina"), (portGraphics, portDelta) -> ShimmerMutationClient.bar(com.pockyl.hextale.port.client.GuiGraphicsExtractor.of(portGraphics), portDelta)));
    }

    private static void motes(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (!level.isClientSide() || !(entity instanceof Mob) || entity.isInvisible()) {
            return;
        }
        ShimmerMutations.Mutation mutation = ShimmerMutations.of(entity);
        if (mutation == null) {
            return;
        }
        ShimmerPose.tick((Mob) entity, mutation.kind());
        boolean lastDay = mutation.until() - level.getGameTime() < ShimmerMutations.DAY;
        if (entity.tickCount % (lastDay ? LAST_DAY_MOTE_TICKS : MOTE_TICKS) == 0) {
            RandomSource random = entity.getRandom();
            level.addParticle(ModParticles.SHIMMER_MOTE.get(), entity.getRandomX(0.6), entity.getRandomY(), entity.getRandomZ(0.6),
                    (random.nextDouble() - 0.5) * 0.02, 0.02, (random.nextDouble() - 0.5) * 0.02);
        }
    }

    /** Over the experience bar, where a horse's jump bar lies, while on a Raptor. */
    private static void bar(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        LocalPlayer player = Minecraft.getInstance().player;
        Entity vehicle = player == null ? null : player.getVehicle();
        ShimmerMutations.Mutation mutation = vehicle == null ? null : ShimmerMutations.of(vehicle);
        if (mutation == null || mutation.kind() != ShimmerMutations.Kind.RAPTOR) {
            return;
        }
        int x = (graphics.guiWidth() - BAR_WIDTH) / 2;
        int y = graphics.guiHeight() - 29;
        float wind = mutation.stamina() / (float) ShimmerRide.STAMINA;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_FRAME, x, y, BAR_WIDTH, BAR_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, wind < BAR_LOW_WIND ? BAR_LOW : BAR_FILL, BAR_WIDTH, BAR_HEIGHT, 0, 0, x, y,
                1 + Math.round(BAR_INSIDE * wind), BAR_HEIGHT);
    }
}
