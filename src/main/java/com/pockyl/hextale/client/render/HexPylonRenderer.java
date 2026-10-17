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
        state.color = PickaxeRuneTint.blend(pylon.runes());
        itemModelResolver.updateForTopItem(state.gem, new ItemStack(ModItems.HEXTECH_GEMSTONE.get()), ItemDisplayContext.GROUND, level, null, seed);

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
        state.reach = pylon.kind() == PylonKind.GATHERING && (linkingHere || goggles && lookedAt) ? pylon.radius() : 0;
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
                poseStack.translate(0.5, 13.15 / 16, 0.5);
                poseStack.rotate(Axis.XP.rotationDegrees(90));
                poseStack.rotate(Axis.ZP.rotationDegrees(time * 0.6F));
                HexFx.sigilRing(poseStack, collector, 0.36F, 6, state.color, 0.45F + 0.4F * state.flare);
            }
            case SENDING -> {
                poseStack.translate(gem.x, gem.y + bob, gem.z);
                poseStack.rotate(Axis.YP.rotationDegrees(time * (2.2F + 10 * state.flare)));
                HexFx.sigilRing(poseStack, collector, 0.21F, 4, state.color, 0.5F + 0.4F * state.flare);
            }
            case RELAY -> {
                poseStack.translate(gem.x, gem.y + bob, gem.z);
                poseStack.pushPose();
                poseStack.rotate(Axis.YP.rotationDegrees(time * (1.8F + 8 * state.flare)));
                HexFx.sigilRing(poseStack, collector, 0.17F, 3, state.color, 0.45F + 0.4F * state.flare);
                poseStack.popPose();
                poseStack.rotate(Axis.YP.rotationDegrees(-time * (1.3F + 8 * state.flare) + 90));
                poseStack.rotate(Axis.XP.rotationDegrees(70));
                HexFx.sigilRing(poseStack, collector, 0.15F, 3, state.color, 0.35F + 0.4F * state.flare);
            }
        }
        poseStack.popPose();

        // The gem and its halo.
        poseStack.pushPose();
        poseStack.translate(gem.x, gem.y + bob, gem.z);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.45F + 0.3F * state.flare, state.color, 0.35F + 0.4F * state.flare);
        poseStack.rotate(Axis.YP.rotationDegrees(time * (1.5F + 12 * state.flare)));
        poseStack.scale(0.42F, 0.42F, 0.42F);
        poseStack.translate(0, -0.2, 0);
        state.gem.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

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
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            poseStack.rotate(Axis.ZP.rotationDegrees(time * 0.15F));
            HexFx.sigilRing(poseStack, collector, state.reach + 0.5F, 36, state.color, 0.35F);
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

    /** A thin glowing line from the gem to {@code to} with three motes running along it. */
    private static void thread(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, Vec3 eye, Vec3 from, Vec3 to,
                               int color, float alpha, float time) {
        Vec3 d = to.subtract(from);
        poseStack.pushPose();
        poseStack.translate(from.x, from.y, from.z);
        Vector3f toCamera = eye.subtract(from).toVector3f();
        HexFx.beam(poseStack, collector, (float) d.x, (float) d.y, (float) d.z, 0.04F, color, alpha, toCamera);
        float length = (float) d.length();
        for (int k = 0; k < 3; k++) {
            float s = ((time * 0.04F / Math.max(1, length)) + k / 3F) % 1F;
            poseStack.pushPose();
            poseStack.translate(d.x * s, d.y * s, d.z * s);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.09F, RuneDrawing.lighten(color, 0.4F), Math.min(1, alpha * 3));
            poseStack.popPose();
        }
        poseStack.popPose();
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
        poseStack.rotate(Axis.YP.rotationDegrees(spin));
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
        poseStack.rotate(camera.orientation);
        poseStack.rotate(Axis.ZP.rotation(time * 0.12F));
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
        poseStack.rotate(Axis.XP.rotationDegrees(90));
        poseStack.rotate(Axis.ZP.rotation(-time * 0.08F));
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
        } else if (pylon.kind() == PylonKind.GATHERING && ClientPylons.goggles()) {
            box = box.inflate(pylon.radius() + 1, 1, pylon.radius() + 1);
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState gem = new ItemStackRenderState();
        final List<FlightState> flights = new ArrayList<>();
        final List<Vec3> links = new ArrayList<>();
        @Nullable
        Vec3 pending;
        PylonKind kind = PylonKind.RELAY;
        Vec3 gemAt = Vec3.ZERO;
        float time;
        float flare;
        int color;
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
