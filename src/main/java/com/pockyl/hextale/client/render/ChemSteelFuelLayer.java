package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import java.util.EnumSet;
import java.util.Set;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;

/**
 * The fuel behind the glass of worn chem-steel armour (helmet filters, chest ampoule, thigh vial): the pieces drawn
 * again with their fuel sheet in the colour of the fuel in the tank the wearer has on; shimmer glows. Nothing when the
 * tank is empty or missing - the armour's own sheet shows the glass dark then.
 */
public class ChemSteelFuelLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public record Fuel(Set<ChemSteelClient.Piece> pieces, int tint, boolean glows) {
    }

    public static final ContextKey<Fuel> FUEL = new ContextKey<>(Hextale.id("chem_steel_fuel"));
    /** Draw order: after the armour's own layers. */
    static final int ORDER = 16;
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public ChemSteelFuelLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new ChemSteelFuelLayer(renderer));
            }
        }
    }

    public static void registerModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & net.minecraft.client.entity.ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                extract(avatar, state);
            }
        });
    }

    private static void extract(LivingEntity entity, AvatarRenderState state) {
        Set<ChemSteelClient.Piece> pieces = EnumSet.noneOf(ChemSteelClient.Piece.class);
        for (EquipmentSlot slot : SLOTS) {
            ChemSteelClient.Piece piece = ChemSteelClient.Piece.of(entity.getItemBySlot(slot));
            if (piece != null) {
                pieces.add(piece);
            }
        }
        ItemStack tank = pieces.isEmpty() ? ItemStack.EMPTY : ChemTank.worn(entity);
        TankFuel fuel = ChemTank.contents(tank);
        int tint = ChemSteelClient.fuelTint(fuel);
        state.setRenderData(FUEL, tint == 0 ? null : new Fuel(pieces, tint, ChemSteelClient.glows(fuel)));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Fuel fuel = state.getRenderData(FUEL);
        if (fuel == null || state.isInvisible) {
            return;
        }
        int light = fuel.glows() ? LightCoordsUtil.FULL_BRIGHT : lightCoords;
        for (ChemSteelClient.Piece piece : fuel.pieces()) {
            var model = ChemSteelClient.FUEL.get(piece);
            if (model == null) {
                continue;
            }
            model.setupAnim(state);
            // After the armour layers (their orders start low), so the fuel lies over the glass at the same depth.
            collector.order(ORDER).submitModel(model, state, poseStack, model.renderType(ChemSteelClient.FUEL_TEXTURE), light, OverlayTexture.NO_OVERLAY, fuel.tint(),
                    null, state.outlineColor);
        }
    }
}
