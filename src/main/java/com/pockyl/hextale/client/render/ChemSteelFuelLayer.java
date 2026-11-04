package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.EnumSet;
import java.util.Set;

import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;

/**
 * The fuel behind the glass of worn chem-steel armour (helmet filters, chest ampoule, thigh vial): the pieces drawn
 * again with their fuel sheet in the colour of the fuel in the tank the wearer has on; shimmer glows. Nothing when the
 * tank is empty or missing - the armour's own sheet shows the glass dark then.
 *
 * <p>1.21.1: a player render layer that reads the player directly (26.x extracts into the avatar render state); added
 * after the armour layer, it draws after it.
 */
public class ChemSteelFuelLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public record Fuel(Set<ChemSteelClient.Piece> pieces, int tint, boolean glows) {
    }

    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public ChemSteelFuelLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new ChemSteelFuelLayer(renderer));
            }
        }
    }

    private static @org.jspecify.annotations.Nullable Fuel extract(LivingEntity entity) {
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
        return tint == 0 ? null : new Fuel(pieces, tint, ChemSteelClient.glows(fuel));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copyPose(HumanoidModel from, HumanoidModel to) {
        from.copyPropertiesTo(to);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Fuel fuel = extract(player);
        if (fuel == null || player.isInvisible()) {
            return;
        }
        int light = fuel.glows() ? LightTexture.FULL_BRIGHT : lightCoords;
        for (ChemSteelClient.Piece piece : fuel.pieces()) {
            var model = ChemSteelClient.FUEL.get(piece);
            if (model == null) {
                continue;
            }
            copyPose(getParentModel(), model);
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.armorCutoutNoCull(ChemSteelClient.FUEL_TEXTURE)), light,
                    OverlayTexture.NO_OVERLAY, fuel.tint());
        }
    }
}
