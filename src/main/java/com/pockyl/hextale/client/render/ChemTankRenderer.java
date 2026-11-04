package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.block.ChemTankBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.TankFuel;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import com.pockyl.hextale.port.client.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.pockyl.hextale.port.client.BlockEntityRenderState;
import com.pockyl.hextale.port.client.ModelFeatureRenderer;
import com.pockyl.hextale.port.client.CameraRenderState;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/** A placed Chem Tank's fuel behind its glass: as high as the tank is full, in the fuel's colour; shimmer glows. */
public class ChemTankRenderer implements BlockEntityRenderer<ChemTankBlockEntity, ChemTankRenderer.State> {
    public ChemTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ChemTankBlockEntity tank, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTick, cameraPosition, breakProgress);
        TankFuel fuel = tank.contents();
        state.level = fuel.isEmpty() ? 0 : fuel.level();
        state.color = fuel.color();
        state.glows = !fuel.isEmpty() && fuel.fuel().brew() != Brew.EMBER_OIL;
        state.time = HexFx.time(tank.getLevel(), partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.level > 0) {
            ChemTankLayer.submitLiquid(poseStack, collector, ChemTankLayer.liquidTop(state.level), state.color, state.glows, state.time,
                    state.lightCoords);
        }
    }

    public static final class State extends BlockEntityRenderState {
        float level;
        int color;
        boolean glows;
        float time;
    }
}
