package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * The Chem Tank worn on a player's back (chest slot or Curios back slot): its worn model strapped to the body, and the
 * fuel inside it behind the glass, as high as the tank is full, in the fuel's colour; shimmer glows. Numbers from the
 * Blockbench model of the worn tank.
 */
public class ChemTankLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public record Worn(float level, int color, boolean glows, float time, boolean overArmour, boolean jets, float heat) {
    }

    public static final ContextKey<Worn> WORN = new ContextKey<>(Hextale.id("chem_tank"));
    private static final float SCALE = 0.75F;
    private static final float Y = 6F / 16;
    private static final float Z = 5.95F / 16;
    /** Over a chem-steel chestplate the tank sits a little further back, clear of its leather. */
    private static final float Z_OVER_ARMOUR = 6.2F / 16;
    /** The liquid's box in model pixels: full from {@link #BOTTOM} to {@link #BOTTOM} + {@link #HEIGHT}. */
    private static final float MIN = 4.15F / 16;
    private static final float MAX = 11.85F / 16;
    private static final float BOTTOM = 2.15F / 16;
    private static final float HEIGHT = 12.75F / 16;
    /** The mouths of the Jet Tank's two nozzles in model pixels. */
    private static final float[][] NOZZLES = {{2, 1, 8}, {14, 1, 8}};
    private static final float FLAME_REACH = 0.7F;

    public ChemTankLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            if (renderer != null) {
                renderer.addLayer(new ChemTankLayer(renderer));
            }
        }
    }

    public static void registerModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                extract(avatar, state);
            }
        });
    }

    private static void extract(LivingEntity entity, AvatarRenderState state) {
        ItemStack tank = ChemTank.worn(entity);
        if (tank.isEmpty()) {
            state.setRenderData(WORN, null);
            return;
        }
        TankFuel fuel = ChemTank.contents(tank);
        state.showCape = false;
        state.setRenderData(WORN, new Worn(fuel.isEmpty() ? 0 : fuel.level(), fuel.color(),
                !fuel.isEmpty() && fuel.fuel().brew() != Brew.EMBER_OIL, HexFx.time(entity.level(), state.partialTick),
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() instanceof com.pockyl.hextale.item.ChemArmourItem,
                ChemTank.hasJets(tank), com.pockyl.hextale.client.ChemJetsClient.heat(entity)));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        Worn worn = state.getRenderData(WORN);
        if (worn == null || state.isInvisible) {
            return;
        }
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0, Y, worn.overArmour() ? Z_OVER_ARMOUR : Z);
        poseStack.scale(SCALE, -SCALE, -SCALE);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        TableModels.submit(poseStack, collector, worn.jets() ? TableModels.CHEM_JET_TANK_WORN : TableModels.CHEM_TANK_WORN, lightCoords);
        if (worn.level() > 0) {
            float top = BOTTOM + HEIGHT * Math.max(0.03F, worn.level());
            submitLiquid(poseStack, collector, top, worn.color(), worn.glows(), worn.time(), lightCoords);
        }
        if (worn.heat() > 0) {
            int core = ARGB.srgbLerp(0.7F, worn.color(), 0xFFFFFF);
            for (int i = 0; i < NOZZLES.length; i++) {
                float[] n = NOZZLES[i];
                poseStack.pushPose();
                poseStack.translate(n[0] / 16, n[1] / 16, n[2] / 16);
                SkypackLayer.flame(poseStack, collector, worn.time() + i * 7.3F, worn.heat(), FLAME_REACH, i, worn.color(), core);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    /** The fuel behind the glass, shared with the placed tank's renderer. */
    static void submitLiquid(PoseStack poseStack, SubmitNodeCollector collector, float top, int color, boolean glows, float time, int light) {
        int alpha = glows ? 0xE6 : 0xD0;
        LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, top, alpha << 24 | color, time, 17L, glows ? 2 : 0);
        if (glows) {
            // Shimmer lights itself: a second, brighter skin of the same liquid.
            LiquidBox.submit(poseStack, collector, MIN, MIN, MAX, MAX, BOTTOM, top, 0x30 << 24 | color);
        }
    }

    static float liquidTop(float level) {
        return BOTTOM + HEIGHT * Math.max(0.03F, level);
    }
}
