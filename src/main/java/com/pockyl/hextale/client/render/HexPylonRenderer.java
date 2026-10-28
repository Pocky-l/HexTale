package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.client.ClientPylons;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.transport.PylonFlight;
import com.pockyl.hextale.transport.PylonKind;
import com.pockyl.hextale.transport.PylonLink;
import com.pockyl.hextale.transport.PylonLinking;

import java.util.ArrayList;
import java.util.List;

/**
 * A hex pylon's gem and its magic. The gem floats over the crown in a halo of its runes' colour and spins up for a
 * moment after every move. Each kind has its rings: a gathering pylon a wide sigil ring lying in its dish, a sending pylon a ring turning upright around the gem, a relay two crossed rings. Moved items are fetched by the magic ({@link #flight}): a tendril of light ({@link TendrilFx})
 * reaches out of the gem, closes its grip around the item, drags it in and hands it on along the next tendril. While the player wears the goggles, sneaks or holds a pylon the links show as
 * gold threads with motes running along them; while linking, a cyan thread runs from the gem to the crosshair and the
 * ends already tied pulse. The goggles also show a gathering pylon's reach as a ring on the ground.
 */
public class HexPylonRenderer implements BlockEntityRenderer<HexPylonBlockEntity, HexPylonRenderer.State> {
    private static final int GOLD = 0xF0C24A;
    private static final int AMETHYST = 0xB98CF0;
    private static final int LINK_CYAN = 0x7EF0FF;
    private final ItemModelResolver itemModelResolver;

    public HexPylonRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HexPylonBlockEntity pylon, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pylon, state, partialTick, cameraPosition, breakProgress);
        Level level = pylon.getLevel();
        BlockPos pos = pylon.getBlockPos();
        Vec3 origin = Vec3.atLowerCornerOf(pos);
        int seed = (int) pos.asLong();
        state.kind = pylon.kind();
        state.gemAt = pylon.gem().subtract(origin);
        state.time = HexFx.time(level, partialTick) + (seed & 127);
        float now = level == null ? 0 : level.getGameTime() + partialTick;
        state.flare = Mth.clamp(1 - (now - pylon.movedAt) / 14F, 0, 1);
        // Each kind's heart: the hextech gemstone over the bowl and the plate, an amethyst cluster over the relay (violet
        // light without runes), a swirl of the jar's essence in the siphon's bulb.
        state.color = pylon.kind() == PylonKind.RELAY && pylon.runes().isEmpty() ? AMETHYST : PickaxeRuneTint.blend(pylon.runes());
        state.essenceColor = -1;
        if (pylon.kind() == PylonKind.SIPHON && level != null
                && level.getBlockEntity(pos.below()) instanceof com.pockyl.hextale.block.EssenceTankBlockEntity jar && !jar.tank().isEmpty()) {
            state.essenceColor = jar.tank().contents().color();
        }
        ItemStack heart = switch (pylon.kind()) {
            case RELAY -> new ItemStack(net.minecraft.world.item.Items.AMETHYST_CLUSTER);
            case SIPHON -> ItemStack.EMPTY;
            default -> new ItemStack(ModItems.HEXTECH_GEMSTONE.get());
        };
        itemModelResolver.updateForTopItem(state.gem, heart, ItemDisplayContext.GROUND, level, null, seed);
        // A sticker colours the pylon's light; a filter shows its first items circling the heart.
        if (pylon.sticker() != null) {
            state.color = pylon.sticker().color();
        }
        com.pockyl.hextale.transport.ItemFilter filter = pylon.filter();
        state.filterCount = filter == null ? 0 : Math.min(3, filter.entries().size());
        while (state.filterIcons.size() < state.filterCount) {
            state.filterIcons.add(new ItemStackRenderState());
        }
        for (int i = 0; i < state.filterCount; i++) {
            itemModelResolver.updateForTopItem(state.filterIcons.get(i), filter.entries().get(i).item(), ItemDisplayContext.GROUND, level,
                    null, seed + 7 * i);
        }
        state.filterWhitelist = filter == null || filter.whitelist();

        // Links: only while the player wears the goggles, sneaks, holds a pylon or is linking, so a busy base is not a
        // web of threads.
        LocalPlayer player = Minecraft.getInstance().player;
        boolean linkingHere = pos.equals(PylonLinking.clientFrom);
        boolean goggles = ClientPylons.goggles();
        boolean showLinks = linkingHere || goggles || player != null && (player.isShiftKeyDown() || ModItems.isPylon(player.getMainHandItem()));
        state.linking = linkingHere;
        // The gathering reach on the ground, while the goggles look at the pylon or it is being linked.
        HitResult looked = Minecraft.getInstance().hitResult;
        boolean lookedAt = looked instanceof BlockHitResult block && looked.getType() == HitResult.Type.BLOCK && block.getBlockPos().equals(pos);
        boolean reaches = pylon.kind() == PylonKind.GATHERING || pylon.kind() == PylonKind.SORTING;
        state.reach = reaches && (linkingHere || goggles && lookedAt) ? pylon.radius() : 0;
        // The storages a sorting matrix fills, marked on their lids while it is looked at or linked.
        state.storages.clear();
        if (pylon.kind() == PylonKind.SORTING && state.reach > 0 && level != null) {
            for (BlockPos storage : HexPylonBlockEntity.sortingStorages(level, pos, pylon.radius())) {
                state.storages.add(Vec3.atCenterOf(storage).add(0, 0.52, 0).subtract(origin));
            }
        }
        state.links.clear();
        if (showLinks && level != null) {
            for (PylonLink link : pylon.links()) {
                Vec3 to = level.getBlockEntity(link.pos()) instanceof HexPylonBlockEntity other ? other.gem() : link.entry();
                state.links.add(to.subtract(origin));
            }
        }
        state.pending = null;
        if (linkingHere && player != null) {
            HitResult hit = Minecraft.getInstance().hitResult;
            Vec3 to = hit != null && hit.getType() == HitResult.Type.BLOCK ? ((BlockHitResult) hit).getLocation()
                    : player.getEyePosition(partialTick).add(player.getViewVector(partialTick).scale(4));
            state.pending = to.subtract(origin);
        }

        state.flightCount = 0;
        state.origin = origin;
        for (PylonFlight.Shown shown : pylon.shown) {
            float age = now - shown.start;
            if (age < 0) {
                continue;
            }
            int i = state.flightCount++;
            FlightState fs = i < state.flights.size() ? state.flights.get(i) : add(state.flights, new FlightState());
            fs.flight = shown.flight;
            fs.age = age;
            fs.seed = seed + i * 977;
            fs.landed = age >= shown.duration ? (age - shown.duration) / PylonLinking.FADE_TICKS : -1;
            fs.phase = shown.flight.phaseAt(shown.phases, age);
            fs.u = fs.phase == null ? 1 : fs.phase.progress(age);
            if (fs.phase != null && fs.phase.move() == PylonFlight.Move.ABSORB) {
                // The gem drinks the item in: it flares.
                state.flare = Math.max(state.flare, Mth.sin(fs.u * Mth.PI));
            }
            itemModelResolver.updateForTopItem(fs.item, shown.flight.icon(), ItemDisplayContext.GROUND, level, null, seed + i * 31);
        }
    }

    private static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float time = state.time;
        Vec3 eye = camera.pos.subtract(Vec3.atLowerCornerOf(state.blockPos));
        Vec3 gem = state.gemAt;
        float bob = Mth.sin(time * 0.08F) * 0.02F;

        // The kind's rings.
        poseStack.pushPose();
        switch (state.kind) {
            case GATHERING -> {
                // Lying in the bowl, just over its carved floor.
                poseStack.translate(0.5, 3.3 / 16, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.6F));
                HexFx.sigilRing(poseStack, collector, 0.34F, 6, state.color, 0.45F + 0.4F * state.flare);
            }
            case SENDING -> {
                poseStack.translate(gem.x, gem.y + bob, gem.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(time * (2.2F + 10 * state.flare)));
                HexFx.sigilRing(poseStack, collector, 0.17F, 4, state.color, 0.5F + 0.4F * state.flare);
            }
            case RELAY -> {
                poseStack.translate(gem.x, gem.y + bob, gem.z);
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(time * (1.8F + 8 * state.flare)));
                HexFx.sigilRing(poseStack, collector, 0.17F, 3, state.color, 0.45F + 0.4F * state.flare);
                poseStack.popPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(-time * (1.3F + 8 * state.flare) + 90));
                poseStack.mulPose(Axis.XP.rotationDegrees(70));
                HexFx.sigilRing(poseStack, collector, 0.15F, 3, state.color, 0.35F + 0.4F * state.flare);
            }
            case SORTING -> lattice(poseStack, collector, camera, eye, state, gem.add(0, bob, 0), time);
            case SIPHON -> {
                // Under the cap, turning: the siphon's sigil.
                poseStack.translate(0.5, 10.8 / 16, 0.5);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-time * (1.5F + 8 * state.flare)));
                HexFx.sigilRing(poseStack, collector, 0.2F, 6, state.essenceColor >= 0 ? state.essenceColor : state.color, 0.4F + 0.4F * state.flare);
            }
        }
        poseStack.popPose();

        if (state.kind == PylonKind.SIPHON) {
            // A swirl of the jar's essence in the bulb: a soft glow and motes circling inside the glass.
            if (state.essenceColor >= 0) {
                poseStack.pushPose();
                poseStack.translate(gem.x, gem.y, gem.z);
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.38F + 0.18F * state.flare, state.essenceColor,
                        0.45F + 0.35F * state.flare);
                for (int i = 0; i < 5; i++) {
                    float angle = time * (0.12F + 0.25F * state.flare) + i * Mth.TWO_PI / 5;
                    poseStack.pushPose();
                    poseStack.translate(Mth.cos(angle) * 0.14F, Mth.sin(time * 0.15F + i * 1.3F) * 0.12F, Mth.sin(angle) * 0.14F);
                    HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.07F, RuneDrawing.lighten(state.essenceColor, 0.35F), 0.85F);
                    poseStack.popPose();
                }
                poseStack.popPose();
            }
        } else {
            // The heart and its halo.
            float scale = state.kind == PylonKind.SENDING ? 0.32F : state.kind == PylonKind.RELAY ? 0.36F
                    : state.kind == PylonKind.SORTING ? 0.24F : 0.42F;
            poseStack.pushPose();
            poseStack.translate(gem.x, gem.y + bob, gem.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.45F + 0.3F * state.flare, state.color, 0.35F + 0.4F * state.flare);
            poseStack.mulPose(Axis.YP.rotationDegrees(time * (1.5F + 12 * state.flare)));
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0, -0.2, 0);
            state.gem.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        // The filter's items circling the heart (on a blacklist they glow red).
        for (int i = 0; i < state.filterCount; i++) {
            float angle = time * 0.04F + i * Mth.TWO_PI / state.filterCount;
            poseStack.pushPose();
            poseStack.translate(gem.x + Mth.cos(angle) * 0.3F, gem.y + 0.05F + Mth.sin(time * 0.07F + i) * 0.03F, gem.z + Mth.sin(angle) * 0.3F);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.22F, state.filterWhitelist ? 0xF0C24A : 0xE04A3A, 0.4F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-time * 2));
            poseStack.scale(0.18F, 0.18F, 0.18F);
            poseStack.translate(0, -0.15, 0);
            state.filterIcons.get(i).submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        // Links and the thread of a link being made. While linking, the ends already tied pulse: clicking one unties it.
        for (Vec3 to : state.links) {
            thread(poseStack, collector, camera, eye, gem, to, GOLD, state.linking ? 0.35F : 0.22F, time);
            if (state.linking) {
                float pulse = 0.5F + 0.5F * Mth.sin(time * 0.25F);
                poseStack.pushPose();
                poseStack.translate(to.x, to.y, to.z);
                HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.45F + 0.15F * pulse, GOLD, 0.45F + 0.3F * pulse);
                poseStack.popPose();
            }
        }
        if (state.reach > 0) {
            // The ground the gathering pylon reaches: a wide slow ring around the container it stands on.
            poseStack.pushPose();
            poseStack.translate(0.5, -0.94, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.15F));
            HexFx.sigilRing(poseStack, collector, state.reach + 0.5F, 36, state.color, 0.35F);
            poseStack.popPose();
        }
        for (int i = 0; i < state.storages.size(); i++) {
            // A sorting matrix's storages: a small turning ring on each lid, the nearest ones lighting first.
            Vec3 at = state.storages.get(i);
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.15F - i * 0.6F);
            poseStack.pushPose();
            poseStack.translate(at.x, at.y, at.z);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 0.8F + i * 40));
            HexFx.sigilRing(poseStack, collector, 0.3F, 6, GOLD, 0.3F + 0.3F * pulse);
            poseStack.popPose();
        }
        if (state.pending != null) {
            float pulse = 0.5F + 0.5F * Mth.sin(time * 0.3F);
            thread(poseStack, collector, camera, eye, gem, state.pending, LINK_CYAN, 0.3F + 0.25F * pulse, time * 2);
            poseStack.pushPose();
            poseStack.translate(state.pending.x, state.pending.y, state.pending.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.35F + 0.1F * pulse, LINK_CYAN, 0.6F);
            poseStack.popPose();
        }

        // Items in flight.
        for (int i = 0; i < state.flightCount; i++) {
            flight(poseStack, collector, camera, eye, state, state.flights.get(i));
        }
    }

    /** Corners of the sorting matrix's lattice: a cube of lenses around the gem. */
    private static final float[][] LATTICE = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1}, {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
    private static final int[][] LATTICE_EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
    private static final int EDGE_POINTS = 6;
    private static final int THREAD_POINTS = 20;

    /**
     * The sorting matrix's heart: a cube of eight lenses turning on its corner around the gem, joined by fine lines of
     * light, inside three crossed hexagons that turn on their own axes. A move spins it up, lights the lenses and sends
     * light running along the cube's edges, as if it read each item passing.
     */
    private static void lattice(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Vec3 eye, State state, Vec3 at,
                                float time) {
        float spin = 1 + 6 * state.flare;
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        // Three crossed hexagons, each on its own axis.
        for (int ring = 0; ring < 3; ring++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(ring * 60 + time * 0.7F * spin));
            poseStack.mulPose(Axis.XP.rotationDegrees(55 + ring * 35));
            poseStack.mulPose(Axis.ZP.rotationDegrees((ring % 2 == 0 ? 1 : -1) * time * (1.6F + ring * 0.5F) * spin));
            HexFx.sigilRing(poseStack, collector, 0.25F - ring * 0.02F, 4, state.color, 0.35F + 0.35F * state.flare);
            poseStack.popPose();
        }
        // The lens lattice: corners turned with the cube, edges as lines of light, lenses as glints.
        float half = 0.13F + 0.012F * Mth.sin(time * 0.09F) + 0.03F * state.flare;
        org.joml.Quaternionf turn = new org.joml.Quaternionf()
                .rotateY(time * 0.012F * spin)
                .rotateX(0.6155F)
                .rotateZ(Mth.PI / 4);
        Vec3[] corners = new Vec3[LATTICE.length];
        for (int i = 0; i < LATTICE.length; i++) {
            Vector3f c = turn.transform(new Vector3f(LATTICE[i][0] * half, LATTICE[i][1] * half, LATTICE[i][2] * half));
            corners[i] = new Vec3(c.x, c.y, c.z);
        }
        Vector3f toCamera = eye.subtract(at).toVector3f();
        int vivid = JarStreamFx.vivid(state.color);
        int core = RuneDrawing.mix(vivid, 0xFFFFFF, 0.65F);
        float base = 0.3F + 0.3F * state.flare;
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            Vec3[] points = new Vec3[EDGE_POINTS];
            float[] light = new float[EDGE_POINTS];
            for (int e = 0; e < LATTICE_EDGES.length; e++) {
                Vec3 a = corners[LATTICE_EDGES[e][0]];
                Vec3 b = corners[LATTICE_EDGES[e][1]];
                for (int k = 0; k < EDGE_POINTS; k++) {
                    float u = k / (float) (EDGE_POINTS - 1);
                    points[k] = a.add(b.subtract(a).scale(u));
                    // While it reads, light runs along every edge, each a little behind the last.
                    float crest = frac(time * 0.08F + e * 0.27F);
                    light[k] = 0.55F + 0.45F * state.flare * (float) Math.exp(-Mth.square(u - crest) / 0.04F);
                }
                JarStreamFx.strip(consumer, pose, points, light, toCamera, 0.05F, vivid, 0.4F * base);
                JarStreamFx.strip(consumer, pose, points, light, toCamera, 0.012F, core, base * 1.6F);
            }
        });
        List<float[]> lenses = new ArrayList<>(corners.length);
        for (int i = 0; i < corners.length; i++) {
            Vec3 c = corners[i];
            float glint = 0.5F + 0.5F * Mth.sin(time * 0.2F + i * 0.8F);
            lenses.add(HexFx.quad(c.x, c.y, c.z, 0.035F + 0.012F * glint + 0.015F * state.flare, i % HexFx.GLINT_FRAMES, time * 0.03F + i,
                    Math.min(1, 0.55F + 0.3F * glint + 0.3F * state.flare), RuneDrawing.lighten(state.color, 0.5F)));
        }
        HexFx.sheet(poseStack, collector, camera.orientation, HexFx.GLINT, HexFx.GLINT_FRAMES, lenses);
        poseStack.popPose();
    }

    /**
     * A link: a fine line of light from the gem to {@code to}, hanging in a slight curve, with pulses of light (a glint
     * riding each) running out of the gem along it - the way things will go.
     */
    private static void thread(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Vec3 eye, Vec3 from, Vec3 to,
                               int color, float alpha, float time) {
        Vec3 d = to.subtract(from);
        float length = (float) d.length();
        if (length < 0.05F) {
            return;
        }
        float sag = Math.min(0.3F, 0.035F * length);
        Vec3[] points = new Vec3[THREAD_POINTS + 1];
        float[] light = new float[THREAD_POINTS + 1];
        float[] crests = new float[2];
        for (int j = 0; j < crests.length; j++) {
            crests[j] = frac(time * 0.045F / Math.max(1, length * 0.5F) + j / (float) crests.length);
        }
        for (int i = 0; i <= THREAD_POINTS; i++) {
            float k = i / (float) THREAD_POINTS;
            points[i] = from.add(d.scale(k)).add(0, -Mth.sin(k * Mth.PI) * sag, 0);
            float pulse = 0;
            for (float crest : crests) {
                pulse += (float) Math.exp(-Mth.square(k - crest) * length * length / 0.08F);
            }
            // Soft at both ends, where it meets the gem and its target.
            float ends = Math.min(1, Math.min(k, 1 - k) * length * 4 + 0.3F);
            light[i] = ends * (0.4F + 0.6F * Math.min(1, pulse));
        }
        int vivid = JarStreamFx.vivid(color);
        int core = RuneDrawing.mix(vivid, 0xFFFFFF, 0.6F);
        Vector3f view = eye.toVector3f();
        collector.submitCustomGeometry(poseStack, HexGlow.TYPE, (pose, consumer) -> {
            JarStreamFx.strip(consumer, pose, points, light, view, 0.12F, vivid, Math.min(1, alpha * 1.3F));
            JarStreamFx.strip(consumer, pose, points, light, view, 0.025F, core, Math.min(1, alpha * 3));
        });
        List<float[]> glints = new ArrayList<>(crests.length);
        for (int j = 0; j < crests.length; j++) {
            float k = crests[j];
            Vec3 p = from.add(d.scale(k)).add(0, -Mth.sin(k * Mth.PI) * sag, 0);
            glints.add(HexFx.quad(p.x, p.y, p.z, 0.05F, j, time * 0.06F + j, Math.min(1, alpha * 3) * Mth.sin(k * Mth.PI), core));
        }
        HexFx.sheet(poseStack, collector, camera.orientation, HexFx.GLINT, HexFx.GLINT_FRAMES, glints);
    }

    private static float frac(float x) {
        return x - Mth.floor(x);
    }

    /**
     * One item fetched by the magic, by the phase of its timeline: a tendril reaches out of the gem, its tip closes
     * around the item, it pulls the item in along its own line, the item sinks into the gem; on every next leg a tendril
     * stretches to the next gem or container and draws the item along to it.
     */
    private static void flight(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Vec3 eye, State state,
                               FlightState fs) {
        PylonFlight flight = fs.flight;
        int color = state.color;
        float time = state.time;
        if (fs.landed >= 0 || fs.phase == null) {
            // Landed: a soft flash where it went in.
            float f = Mth.clamp(fs.landed, 0, 1);
            Vec3 end = flight.path().getLast().subtract(state.origin);
            poseStack.pushPose();
            poseStack.translate(end.x, end.y, end.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.25F + 0.45F * f, RuneDrawing.lighten(color, 0.3F), 0.6F * (1 - f));
            poseStack.popPose();
            return;
        }
        float u = fs.u;
        int leg = fs.phase.leg();
        Vec3 origin = state.origin;
        Vec3 lying = flight.path().getFirst().subtract(origin);
        switch (fs.phase.move()) {
            case REACH -> {
                float e = PylonFlight.out(u);
                TendrilFx.submit(poseStack, collector, camera.orientation, eye, flight, 0, 1 - e, 1, true, color, time, fs.seed, 1, 0,
                        origin, Math.min(1, u * 4));
                if (flight.gathered() && u > 0.5F) {
                    // The spot it lies on lights up as the tendril comes.
                    flatRing(poseStack, collector, lying.add(0, -0.18, 0), 0.32F - 0.08F * u, color, (u - 0.5F) * 1.4F, time);
                }
                item(poseStack, collector, fs, lying.add(0, Mth.sin(fs.age * 0.15F) * 0.03, 0), 0.5F, fs.age * 2);
            }
            case GRAB -> {
                float g = PylonFlight.smooth(u);
                TendrilFx.submit(poseStack, collector, camera.orientation, eye, flight, 0, 0, 1, true, color, time, fs.seed, -1, 0.6F * g,
                        origin, 1);
                Vec3 at = lying.add(0, 0.22 * g, 0);
                if (flight.gathered()) {
                    flatRing(poseStack, collector, lying.add(0, -0.18, 0), 0.24F, color, 0.7F * (1 - g), time);
                }
                claw(poseStack, collector, camera, at, Mth.lerp(g, 0.42F, 0.2F), color, 0.4F + 0.6F * g, time);
                item(poseStack, collector, fs, at, Mth.lerp(g, 0.5F, 0.42F), fs.age * (2 + 10 * g));
            }
            case DRAG -> {
                float p = PylonFlight.smooth(u);
                // A little behind the tendril's pull and swinging, like something heavy being dragged.
                Vec3 side = new Vec3(Mth.sin(fs.age * 0.35F), 0, Mth.cos(fs.age * 0.29F)).scale(0.05 * Mth.sin(p * Mth.PI));
                Vec3 at = flight.curve(0, p).subtract(origin).add(0, 0.22 * (1 - p), 0).add(side);
                TendrilFx.submit(poseStack, collector, camera.orientation, eye, flight, 0, p, 1, true, color, time, fs.seed, -1, 0.8F,
                        origin, 1);
                claw(poseStack, collector, camera, at, 0.2F, color, 1 - 0.5F * p, time);
                item(poseStack, collector, fs, at, 0.42F * (1 - 0.35F * p * p), fs.age * 12);
            }
            case ABSORB -> {
                Vec3 at = flight.path().get(1).subtract(origin);
                float shrink = 1 - PylonFlight.smooth(u);
                item(poseStack, collector, fs, at, 0.27F * shrink, fs.age * 30);
            }
            case EXTEND -> {
                float e = PylonFlight.out(u);
                TendrilFx.submit(poseStack, collector, camera.orientation, eye, flight, leg, 0, e, false, color, time, fs.seed + leg, 1, 0.2F,
                        origin, 1);
            }
            case SLIDE -> {
                float p = PylonFlight.smooth(u);
                Vec3 at = flight.curve(leg, p).subtract(origin);
                TendrilFx.submit(poseStack, collector, camera.orientation, eye, flight, leg, p, 1, true, color, time, fs.seed + leg, -1, 0.85F,
                        origin, 1);
                // Out of one gem, into the next: it grows at the start and, before a gem, shrinks at the end.
                boolean intoGem = leg + 1 < flight.path().size() - 2;
                float size = 0.42F * Math.min(1, u * 5) * (intoGem ? Math.min(1, (1 - u) * 5) : 1);
                claw(poseStack, collector, camera, at, 0.2F, color, 0.8F, time);
                item(poseStack, collector, fs, at, size, fs.age * 12);
            }
            case STREAM -> {
                // Essence flies as the altar's motes (particles, see PylonLinking): only a soft glow where it lands.
                if (fs.u > 0.6F) {
                    Vec3 end = flight.path().get(leg + 1).subtract(origin);
                    poseStack.pushPose();
                    poseStack.translate(end.x, end.y, end.z);
                    HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.35F, flight.color(), 0.5F * (fs.u - 0.6F) / 0.4F);
                    poseStack.popPose();
                }
            }
            case SINK -> {
                float p = PylonFlight.smooth(u);
                Vec3 from = flight.path().get(leg).subtract(origin);
                Vec3 to = flight.path().get(leg + 1).subtract(origin);
                flatRing(poseStack, collector, to.add(0, 0.02, 0), 0.3F - 0.12F * p, color, 0.8F * Mth.sin(u * Mth.PI), time);
                // Out of the gem (it grows back from nothing) and down into the container, shrinking.
                item(poseStack, collector, fs, from.lerp(to, p), 0.36F * Math.min(1, u * 4) * (1 - 0.75F * p), fs.age * 12);
            }
        }
    }

    /** The item itself, full bright in a soft halo of the magic holding it. */
    private static void item(PoseStack poseStack, SubmitNodeCollector collector, FlightState fs, Vec3 at, float size, float spin) {
        if (size <= 0.01F) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(size, size, size);
        poseStack.translate(0, -0.15, 0);
        fs.item.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /** The tendril's grip: a sigil ring facing the camera around what it holds, and a faint glow inside. */
    private static void claw(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Vec3 at, float radius, int color,
                             float alpha, float time) {
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, radius * 2.6F, color, 0.3F * alpha);
        poseStack.mulPose(camera.orientation);
        poseStack.mulPose(Axis.ZP.rotation(time * 0.12F));
        HexFx.sigilRing(poseStack, collector, radius, 4, RuneDrawing.lighten(color, 0.3F), alpha);
        poseStack.popPose();
    }

    /** A sigil ring lying flat (on the ground under an item, on a container's top). */
    private static void flatRing(PoseStack poseStack, SubmitNodeCollector collector, Vec3 at, float radius, int color, float alpha,
                                 float time) {
        if (alpha <= 0.01F) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.ZP.rotation(-time * 0.08F));
        HexFx.sigilRing(poseStack, collector, radius, 6, color, Math.min(1, alpha));
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HexPylonBlockEntity pylon) {
        BlockPos pos = pylon.getBlockPos();
        AABB box = new AABB(pos).expandTowards(0, 0.5, 0).inflate(pylon.kind() == PylonKind.GATHERING ? 1.8 : 0.3);
        for (PylonLink link : pylon.links()) {
            box = box.minmax(new AABB(link.pos()));
        }
        for (PylonFlight.Shown shown : pylon.shown) {
            for (Vec3 p : shown.flight.path()) {
                box = box.minmax(new AABB(p, p).inflate(0.5, 1.5, 0.5));
            }
        }
        if (pos.equals(PylonLinking.clientFrom)) {
            box = box.inflate(pylon.range() + 2);
        } else if ((pylon.kind() == PylonKind.GATHERING || pylon.kind() == PylonKind.SORTING) && ClientPylons.goggles()) {
            box = box.inflate(pylon.radius() + 1, 1, pylon.radius() + 1);
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState gem = new ItemStackRenderState();
        final List<FlightState> flights = new ArrayList<>();
        final List<Vec3> links = new ArrayList<>();
        final List<Vec3> storages = new ArrayList<>();
        @Nullable
        Vec3 pending;
        PylonKind kind = PylonKind.RELAY;
        Vec3 gemAt = Vec3.ZERO;
        float time;
        float flare;
        int color;
        int essenceColor = -1;
        final List<ItemStackRenderState> filterIcons = new ArrayList<>();
        int filterCount;
        boolean filterWhitelist = true;
        int flightCount;
        boolean linking;
        int reach;
        Vec3 origin = Vec3.ZERO;
    }

    static final class FlightState {
        final ItemStackRenderState item = new ItemStackRenderState();
        PylonFlight flight;
        PylonFlight.@Nullable Phase phase;
        float u;
        float age;
        float landed;
        int seed;
    }
}
