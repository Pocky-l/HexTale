package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexVaultBlock;
import com.pockyl.hextale.block.HexVaultBlockEntity;
import com.pockyl.hextale.client.IrisCompat;

import java.util.List;

/**
 * A Hex Vault in its gate: a hexagon hanging in the air, held by nothing, over the platform. Six bars frame it and six
 * petals close it, with a hub in the middle where they meet. A player coming near opens it: the hub turns and sinks back,
 * the petals swing out towards the viewer one after the other, and behind them lies a space of its own, with the kinds that
 * take up the most room drifting in it. The same six petals cover the back. Lamps on the bars fill clockwise with what is
 * held, so the level reads from across a room; a refusal flashes them red. When the gate is woken the hub rises from the
 * block's cell, the bars gather round it and the petals close. Out of its gate the block's own model does the work.
 *
 * <p>Everything is drawn in a frame with its origin in the middle of the door, x and y in the door's plane and the front
 * towards -z, like the models. A model's own origin is its (8, 8, 8).
 */
public class HexVaultRenderer implements BlockEntityRenderer<HexVaultBlockEntity, HexVaultRenderer.State> {
    private static final int SIDES = 6;
    private static final float SIDE_ANGLE = 360F / SIDES;
    /** From the middle to the middle of a bar, and to the outer edge of a closed petal. */
    private static final float BAR_RADIUS = 30 / 16F;
    private static final float PETAL_RADIUS = 24 / 16F;
    /** Where the outer edge lies in the petal's model. */
    private static final float PETAL_EDGE = 20 / 16F;
    /** From the middle to the edge of the space, under the bars. */
    private static final float SPACE_RADIUS = 27 / 16F;
    /** Depth of the space and of the back cover, behind the door. */
    private static final float SPACE_Z = 0.1F;
    private static final float COVER_Z = 0.3F;
    /** The cover's rims reach into the door's in depth, so it is a hair smaller: their sides never share a plane. */
    private static final float COVER_SIZE = 0.99F;
    /**
     * Shut, six petals lie edge to edge and overlap at the seams and in the middle, so each has a depth of its own, in pixels.
     * Any two are 0.1 apart, because one rim of a seam is 0.05 wider than the other; neighbours at most 0.2, so a rim stays
     * proud of the plate beside it, which it stands 0.25 over.
     */
    private static final float[] PETAL_DEPTHS = {0, 0.1F, 0.3F, 0.5F, 0.4F, 0.2F};
    /** How far the petals swing out, and how far apart in the opening they start. */
    private static final float PETAL_SWING = 104;
    private static final float PETAL_STAGGER = 0.07F;
    private static final float HUB_TURN = 300;
    private static final float HUB_SINK = 0.1F;
    /** Clear of the petals' spines, which it covers the ends of. */
    private static final float HUB_PROUD = -1.25F / 16;
    private static final float HUB_OPEN_SIZE = 0.62F;
    private static final float BOB = 0.05F;
    /** From the door's middle down to the block's own cell, where the hub starts when the gate is woken. */
    private static final float RISE_FROM = -2;
    private static final float GATHER_FROM = 1.5F;
    private static final int LAMPS_PER_SIDE = 4;
    private static final float LAMP_STEP = 6 / 16F;
    private static final float LAMP_Z = -6F / 16;
    private static final float DRIFT_RADIUS = 1;
    private static final float DRIFT_SIZE = 0.42F;
    /** The pillars' tops, seen from the door's middle, and the corner of the frame each one holds up. */
    private static final float PILLAR_X = 3;
    private static final float PILLAR_Y = -0.5F;
    private static final float CORNER_X = 2.2F;
    /** Width of those arcs: the soft sprite's, and the light drawn in its place over a shader pack's picture. */
    private static final float ARC_WIDTH = 0.3F;
    private static final int GOLD = 0xFFD98A;
    private static final int UNLIT = 0x3A2A18;
    private static final int REFUSAL_COLOUR = 0xFF6A4A;
    private static final int FLASH_TICKS = 24;

    private final ItemModelResolver itemModelResolver;

    public HexVaultRenderer(BlockEntityRendererProvider.Context context) {
        itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** The door and its light are as big as a room: seen from further off than a block entity usually is. */
    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(HexVaultBlockEntity vault, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(vault, state, partialTick, cameraPosition, breakProgress);
        Level level = vault.getLevel();
        state.formed = vault.getBlockState().getValue(HexVaultBlock.FORMED);
        state.facing = vault.getBlockState().getValue(HexVaultBlock.FACING);
        state.open = vault.doorOpen(partialTick);
        state.forming = vault.forming(partialTick);
        state.filled = vault.capacity() > 0 ? Mth.clamp((float) vault.used() / vault.capacity(), 0, 1) : 0;
        state.time = HexFx.time(level, partialTick);
        float since = level == null ? FLASH_TICKS : level.getGameTime() - vault.refusedAt + partialTick;
        state.flash = since >= 0 && since < FLASH_TICKS ? 1 - since / FLASH_TICKS : 0;
        if (level != null) {
            // The block's own cell lies under the bottom bar: the door takes the light of the air in front of it.
            state.lightCoords = LightCoordsUtil.getLightCoords(level, vault.getBlockPos().above(2).relative(state.facing));
        }
        List<ItemStack> shown = vault.shown();
        state.drifting = state.open > 0 ? Math.min(shown.size(), state.items.length) : 0;
        for (int i = 0; i < state.drifting; i++) {
            itemModelResolver.updateForTopItem(state.items[i], shown.get(i), ItemDisplayContext.FIXED, level, null, (int) vault.getBlockPos().asLong() + i);
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.formed) {
            return;
        }
        // A shader pack's shadow map only takes the frame and the door: the light, the space and what drifts in it cast nothing.
        boolean shadow = IrisCompat.shadowPass();
        pose.pushPose();
        pose.translate(0.5, HexVaultBlockEntity.DOOR_HEIGHT, 0.5);
        pose.rotate(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
        if (!shadow) {
            arcs(state, pose, collector);
        }
        pose.translate(0, Mth.sin(state.time * 0.045F) * BOB, 0);

        cover(state, pose, collector);
        if (!shadow) {
            space(state, pose, collector);
        }
        bars(state, pose, collector, shadow);
        petals(state, pose, collector);
        hub(state, pose, collector);
        if (!shadow) {
            drifting(state, pose, collector);
        }
        pose.popPose();
    }

    /** Eases from 0 to 1 over the stretch of {@code t} from {@code from} to {@code to}. */
    private static float eased(float t, float from, float to) {
        float x = Mth.clamp((t - from) / (to - from), 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static void model(PoseStack pose, SubmitNodeCollector collector, net.neoforged.neoforge.client.model.standalone.StandaloneModelKey<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> key,
                              int light) {
        pose.pushPose();
        pose.translate(-0.5, -0.5, -0.5);
        TableModels.submit(pose, collector, key, light);
        pose.popPose();
    }

    /** A petal's origin is the middle of its outer edge, the one it swings on. */
    private static void petal(PoseStack pose, SubmitNodeCollector collector, int light) {
        pose.pushPose();
        pose.translate(-0.5, -PETAL_EDGE, -0.5);
        TableModels.submit(pose, collector, TableModels.VAULT_PETAL, light);
        pose.popPose();
    }

    /** How far behind its plane one petal of the door or the cover stands. */
    private static float petalDepth(int petal) {
        return PETAL_DEPTHS[petal] / 16;
    }

    /** The six bars, each hanging by itself; when the gate is woken they gather from further out, one after the other. */
    private static void bars(State state, PoseStack pose, SubmitNodeCollector collector, boolean shadow) {
        for (int i = 0; i < SIDES; i++) {
            float gathered = eased(state.forming, 0.25F + i * 0.05F, 0.6F + i * 0.05F);
            if (gathered <= 0) {
                continue;
            }
            float drift = Mth.sin(state.time * 0.06F + i * 1.7F) * 0.015F + state.openEased() * 0.06F;
            pose.pushPose();
            pose.rotate(Axis.ZP.rotationDegrees(i * SIDE_ANGLE + (1 - gathered) * 40));
            pose.translate(0, BAR_RADIUS + drift + (1 - gathered) * GATHER_FROM, 0);
            pose.scale(gathered, gathered, gathered);
            model(pose, collector, TableModels.VAULT_BAR, state.lightCoords);
            if (!shadow) {
                lamps(state, i, pose, collector);
            }
            pose.popPose();
        }
    }

    /** The lamps of one bar: lit clockwise from the top up to the share held. Unlit ones are only sockets in the bar's texture. */
    private static void lamps(State state, int side, PoseStack pose, SubmitNodeCollector collector) {
        for (int j = 0; j < LAMPS_PER_SIDE; j++) {
            int lamp = side * LAMPS_PER_SIDE + j;
            boolean lit = (lamp + 0.5F) / (SIDES * LAMPS_PER_SIDE) <= state.filled;
            if (!lit && state.flash <= 0) {
                continue;
            }
            pose.pushPose();
            pose.translate((1.5F - j) * LAMP_STEP - 0.5F, -0.5F, LAMP_Z - 0.5F);
            int colour = state.flash > 0 ? PixelArt.mix(lit ? GOLD : UNLIT, REFUSAL_COLOUR, state.flash) : GOLD;
            TableModels.submitTinted(pose, collector, TableModels.VAULT_LAMP, LightCoordsUtil.FULL_BRIGHT, 0xFF000000 | colour);
            pose.popPose();
        }
    }

    /** The door: every petal swings on its outer edge, the next one a moment after the last. */
    private static void petals(State state, PoseStack pose, SubmitNodeCollector collector) {
        // Waking, the petals come in open and close over the space; after that the door follows the players.
        float closing = 1 - eased(state.forming, 0.7F, 1);
        float shown = eased(state.forming, 0.5F, 0.72F);
        if (shown <= 0) {
            return;
        }
        for (int i = 0; i < SIDES; i++) {
            float open = Math.max(state.open * (1 + PETAL_STAGGER * (SIDES - 1)) - i * PETAL_STAGGER, closing);
            float eased = eased(open, 0, 1);
            float flutter = eased * Mth.sin(state.time * 0.05F + i * 2.1F) * 2.5F;
            pose.pushPose();
            pose.rotate(Axis.ZP.rotationDegrees(i * SIDE_ANGLE));
            pose.translate(0, PETAL_RADIUS + eased * 0.14F, petalDepth(i));
            pose.rotate(Axis.XP.rotationDegrees(eased * PETAL_SWING + flutter));
            pose.scale(shown, shown, shown);
            petal(pose, collector, state.lightCoords);
            pose.popPose();
        }
    }

    /** The hub: it rises from the block's cell when the gate is woken, and turns and sinks back as the door opens. */
    private static void hub(State state, PoseStack pose, SubmitNodeCollector collector) {
        float risen = eased(state.forming, 0, 0.45F);
        float open = state.openEased();
        // It stands proud of the petals and only sinks once they have swung clear of it.
        float sunk = eased(state.open, 0.45F, 1);
        float size = 1 - (1 - HUB_OPEN_SIZE) * sunk;
        pose.pushPose();
        pose.translate(0, RISE_FROM * (1 - risen), HUB_PROUD + sunk * (HUB_SINK - HUB_PROUD));
        pose.rotate(Axis.ZP.rotationDegrees(open * HUB_TURN + (1 - risen) * 200));
        pose.scale(size, size, size);
        model(pose, collector, TableModels.VAULT_HUB, state.lightCoords);
        pose.popPose();
    }

    /** The back of the gate: the petals' twin, shut for good. */
    private static void cover(State state, PoseStack pose, SubmitNodeCollector collector) {
        float shown = eased(state.forming, 0.5F, 0.72F);
        if (shown <= 0) {
            return;
        }
        pose.pushPose();
        pose.scale(COVER_SIZE, COVER_SIZE, 1);
        for (int i = 0; i < SIDES; i++) {
            pose.pushPose();
            pose.rotate(Axis.ZP.rotationDegrees(i * SIDE_ANGLE));
            pose.translate(0, PETAL_RADIUS, COVER_Z + petalDepth(i));
            pose.scale(shown, shown, shown);
            petal(pose, collector, state.lightCoords);
            pose.popPose();
        }
        pose.popPose();
    }

    /** The space behind the door. Its look is all in the shader; a shader pack cannot run that one, so it is drawn after the pack. */
    private static void space(State state, PoseStack pose, SubmitNodeCollector collector) {
        float shown = eased(state.forming, 0.45F, 0.7F);
        if (shown <= 0) {
            return;
        }
        pose.pushPose();
        pose.translate(0, 0, SPACE_Z);
        pose.scale(shown, shown, 1);
        int open = (int) (state.openEased() * 255);
        int filled = (int) (state.filled * 255);
        if (IrisCompat.shadersOn()) {
            if (VoidOverlay.works()) {
                VoidOverlay.queueSpace(pose.last(), (consumer, at) -> hexagon(consumer, at, open, filled));
            } else {
                collector.submitCustomGeometry(pose, RenderTypes.entitySolid(TheVoidRenderer.STARS), HexVaultRenderer::plainHexagon);
            }
        } else {
            collector.submitCustomGeometry(pose, VaultSpacePipeline.TYPE, (at, consumer) -> hexagon(consumer, at, open, filled));
        }
        pose.popPose();
    }

    /** Three quads share the middle. The vertex colour carries how far open the door is and how full the vault. */
    private static void hexagon(VertexConsumer consumer, PoseStack.Pose pose, int open, int filled) {
        for (int quad = 0; quad < 3; quad++) {
            spaceVertex(consumer, pose, 0, 0, open, filled);
            for (int k = 0; k < 3; k++) {
                float angle = (quad * 2 + (2 - k)) * Mth.TWO_PI / SIDES;
                float radius = SPACE_RADIUS / Mth.cos(Mth.PI / SIDES);
                spaceVertex(consumer, pose, Mth.cos(angle) * radius, Mth.sin(angle) * radius, open, filled);
            }
        }
    }

    private static void spaceVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, int open, int filled) {
        consumer.addVertex(pose, x, y, 0).setColor(open, filled, 0, 255).setUv(x, y).setUv1(0, 0).setUv2(0, 0).setNormal(0, 0, -1);
    }

    private static void plainHexagon(PoseStack.Pose pose, VertexConsumer consumer) {
        for (int quad = 0; quad < 3; quad++) {
            plainVertex(consumer, pose, 0, 0);
            for (int k = 0; k < 3; k++) {
                float angle = (quad * 2 + (2 - k)) * Mth.TWO_PI / SIDES;
                float radius = SPACE_RADIUS / Mth.cos(Mth.PI / SIDES);
                plainVertex(consumer, pose, Mth.cos(angle) * radius, Mth.sin(angle) * radius);
            }
        }
    }

    private static void plainVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y) {
        consumer.addVertex(pose, x, y, 0).setColor(0xFFFFFFFF).setUv(x * 0.25F + 0.5F, y * 0.25F + 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0, 0, -1);
    }

    /** What the vault holds most of, drifting round the hub. */
    private static void drifting(State state, PoseStack pose, SubmitNodeCollector collector) {
        float shown = eased(state.open, 0.45F, 1);
        if (shown <= 0) {
            return;
        }
        for (int i = 0; i < state.drifting; i++) {
            float angle = state.time * 0.008F + i * Mth.TWO_PI / Math.max(1, state.drifting);
            float radius = DRIFT_RADIUS + Mth.sin(state.time * 0.02F + i * 1.3F) * 0.1F;
            pose.pushPose();
            pose.translate(Mth.cos(angle) * radius, Mth.sin(angle) * radius + Mth.sin(state.time * 0.04F + i) * 0.04F, 0.02F);
            pose.rotate(Axis.YP.rotationDegrees(180 + Mth.sin(state.time * 0.03F + i * 2) * 25));
            float size = DRIFT_SIZE * shown;
            pose.scale(size, size, size);
            state.items[i].submit(pose, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    /**
     * Light from the pillars' tops to the corners of the frame: what the eye reads as holding it up. A soft sprite is what
     * shader packs draw worst (see {@link HexFx#sprite}), so with a pack on the same light is added once the pack has finished
     * the picture, like the breach's glow.
     */
    private static void arcs(State state, PoseStack pose, SubmitNodeCollector collector) {
        float shown = eased(state.forming, 0.3F, 0.6F);
        if (shown <= 0) {
            return;
        }
        for (int side = -1; side <= 1; side += 2) {
            float alpha = (0.55F + 0.2F * Mth.sin(state.time * 0.23F + side) + 0.1F * Mth.sin(state.time * 0.71F)) * shown;
            float dx = side * (CORNER_X - PILLAR_X);
            pose.pushPose();
            pose.translate(side * PILLAR_X, PILLAR_Y, 0);
            if (!IrisCompat.shadersOn()) {
                HexFx.beam(pose, collector, dx, -PILLAR_Y, 0, ARC_WIDTH, GOLD, alpha, new Vector3f(0, 0, -1));
            } else if (VoidOverlay.works()) {
                VoidOverlay.queueLight(pose.last(), (consumer, at) -> arc(consumer, at, dx, alpha));
            } else {
                collector.submitCustomGeometry(pose, HexGlow.TYPE, (at, consumer) -> arc(consumer, at, dx, alpha));
            }
            pose.popPose();
        }
    }

    private static void arc(VertexConsumer consumer, PoseStack.Pose pose, float dx, float alpha) {
        HexFx.glowSegment(consumer, pose, 0, 0, dx, -PILLAR_Y, ARC_WIDTH, GOLD, alpha);
    }

    public static class State extends BlockEntityRenderState {
        boolean formed;
        Direction facing = Direction.NORTH;
        float open;
        float forming = 1;
        float filled;
        float flash;
        float time;
        int drifting;
        final ItemStackRenderState[] items = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(),
                new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState()};

        float openEased() {
            return open * open * (3 - 2 * open);
        }
    }
}
