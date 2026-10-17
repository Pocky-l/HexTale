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
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.transport.PylonFlight;
import com.pockyl.hextale.transport.PylonKind;
import com.pockyl.hextale.transport.PylonLink;
import com.pockyl.hextale.transport.PylonLinking;

import java.util.ArrayList;
import java.util.List;

/**
 * A hex pylon's gem and its magic. The gem floats over the crown in a halo of its runes' colour and spins up for a
 * moment after every move. Each kind has its rings: a gathering pylon a wide sigil ring lying in its dish that snaps
 * inward when it pulls, a sending pylon a ring turning upright around the gem, a relay two crossed rings. Filters float
 * around the gem as small items. Moved items fly along their way as glowing items with a trail of motes; a gathered item
 * leaves a ring of light where it lay, a landed one a flash. While the player sneaks or holds a pylon the links show as
 * gold threads with motes running along them; while linking, a cyan thread runs from the gem to the crosshair.
 */
public class HexPylonRenderer implements BlockEntityRenderer<HexPylonBlockEntity, HexPylonRenderer.State> {
    private static final int GOLD = 0xF0C24A;
    private static final int LINK_CYAN = 0x7EF0FF;
    private static final int TRAIL = 5;
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

        List<ItemStack> filters = pylon.filters();
        state.filterCount = filters.size();
        for (int i = 0; i < filters.size(); i++) {
            itemModelResolver.updateForTopItem(state.item(state.filters, i), filters.get(i), ItemDisplayContext.GROUND, level, null, seed + i);
        }

        // Links: only while the player sneaks, holds a pylon or is linking, so a busy base is not a web of threads.
        LocalPlayer player = Minecraft.getInstance().player;
        boolean linkingHere = pos.equals(PylonLinking.clientFrom);
        boolean showLinks = linkingHere || player != null && (player.isShiftKeyDown() || ModItems.isPylon(player.getMainHandItem()));
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
        for (PylonFlight.Shown shown : pylon.shown) {
            float age = now - shown.start;
            if (age < 0) {
                continue;
            }
            int i = state.flightCount++;
            FlightState fs = i < state.flights.size() ? state.flights.get(i) : add(state.flights, new FlightState());
            float t = Mth.clamp(age / shown.duration, 0, 1);
            float eased = t * t * (3 - 2 * t);
            fs.age = age;
            fs.progress = t;
            fs.landed = age >= shown.duration ? (age - shown.duration) / PylonLinking.FADE_TICKS : -1;
            fs.gathered = shown.flight.gathered();
            fs.start = shown.flight.path().getFirst().subtract(origin);
            fs.end = shown.flight.path().getLast().subtract(origin);
            for (int k = 0; k <= TRAIL; k++) {
                float tk = Mth.clamp(eased - k * 0.035F, 0, 1);
                fs.trail[k] = shown.flight.at(tk).subtract(origin);
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
                if (state.flare > 0) {
                    // The pull: a wide ring snapping in from the gathering radius.
                    HexFx.sigilRing(poseStack, collector, 0.36F + 1.4F * state.flare * state.flare, 12, RuneDrawing.lighten(state.color, 0.3F),
                            0.6F * state.flare);
                }
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
        for (int i = 0; i < state.filterCount; i++) {
            float angle = time * 0.05F + i * Mth.TWO_PI / state.filterCount;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * 0.3F, Mth.sin(time * 0.07F + i) * 0.04F, Mth.sin(angle) * 0.3F);
            poseStack.rotate(Axis.YP.rotationDegrees(-time * 3));
            poseStack.scale(0.22F, 0.22F, 0.22F);
            poseStack.translate(0, -0.15, 0);
            state.filters.get(i).submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        poseStack.rotate(Axis.YP.rotationDegrees(time * (1.5F + 12 * state.flare)));
        poseStack.scale(0.42F, 0.42F, 0.42F);
        poseStack.translate(0, -0.2, 0);
        state.gem.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

        // Links and the thread of a link being made.
        for (Vec3 to : state.links) {
            thread(poseStack, collector, camera, eye, gem, to, GOLD, 0.22F, time);
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
            flight(poseStack, collector, camera, state.flights.get(i), state.color);
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

    private static void flight(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, FlightState fs, int color) {
        if (fs.gathered && fs.age < 10) {
            // Where it lay: a ring of light on the ground, opening and fading.
            float r = fs.age / 10F;
            poseStack.pushPose();
            poseStack.translate(fs.start.x, fs.start.y - 0.2, fs.start.z);
            poseStack.rotate(Axis.XP.rotationDegrees(90));
            HexFx.sigilRing(poseStack, collector, 0.18F + 0.25F * r, 6, color, 0.8F * (1 - r));
            poseStack.popPose();
        }
        if (fs.landed >= 0) {
            // Landed: a flash at the end that swells and fades.
            float f = Mth.clamp(fs.landed, 0, 1);
            poseStack.pushPose();
            poseStack.translate(fs.end.x, fs.end.y, fs.end.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.3F + 0.5F * f, RuneDrawing.lighten(color, 0.3F), 0.7F * (1 - f));
            poseStack.popPose();
            return;
        }
        for (int k = TRAIL; k >= 1; k--) {
            Vec3 p = fs.trail[k];
            float fade = 1 - k / (float) (TRAIL + 1);
            poseStack.pushPose();
            poseStack.translate(p.x, p.y, p.z);
            HexFx.sprite(poseStack, collector, camera.orientation, HexFx.MOTE, 0.05F + 0.07F * fade, RuneDrawing.lighten(color, 0.35F), 0.75F * fade);
            poseStack.popPose();
        }
        Vec3 at = fs.trail[0];
        // Grows out of where it was and shrinks into where it goes.
        float size = 0.38F * Mth.clamp(Math.min(fs.progress * 6, (1 - fs.progress) * 5), 0.25F, 1);
        poseStack.pushPose();
        poseStack.translate(at.x, at.y, at.z);
        HexFx.sprite(poseStack, collector, camera.orientation, HexFx.HALO, 0.42F, color, 0.55F);
        poseStack.rotate(Axis.YP.rotationDegrees(fs.age * 18));
        poseStack.scale(size, size, size);
        poseStack.translate(0, -0.15, 0);
        fs.item.submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
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
        }
        return box;
    }

    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState gem = new ItemStackRenderState();
        final List<ItemStackRenderState> filters = new ArrayList<>();
        final List<FlightState> flights = new ArrayList<>();
        final List<Vec3> links = new ArrayList<>();
        @Nullable
        Vec3 pending;
        PylonKind kind = PylonKind.RELAY;
        Vec3 gemAt = Vec3.ZERO;
        float time;
        float flare;
        int color;
        int filterCount;
        int flightCount;

        ItemStackRenderState item(List<ItemStackRenderState> pool, int i) {
            while (pool.size() <= i) {
                pool.add(new ItemStackRenderState());
            }
            return pool.get(i);
        }
    }

    static final class FlightState {
        final ItemStackRenderState item = new ItemStackRenderState();
        final Vec3[] trail = new Vec3[TRAIL + 1];
        Vec3 start = Vec3.ZERO;
        Vec3 end = Vec3.ZERO;
        float age;
        float progress;
        float landed;
        boolean gathered;
    }
}
