package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.TankFuel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.FastColor;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The Chem Tank worn on a player's back (chest slot or Curios back slot): its worn model strapped to the body, and the
 * fuel inside it behind the glass, as high as the tank is full, in the fuel's colour; shimmer glows. Numbers from the
 * Blockbench model of the worn tank.
 *
 * <p>1.21.1: a player render layer that reads the player directly (26.x extracts into the avatar render state); the
 * cape cannot be hidden without changing vanilla's cape layer.
 */
public class ChemTankLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public record Worn(float level, int color, boolean glows, float time, boolean overArmour, boolean jets, float heat) {
    }

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

    public ChemTankLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer) {
        super(renderer);
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new ChemTankLayer(renderer));
            }
        }
    }

    private static @org.jspecify.annotations.Nullable Worn extract(LivingEntity entity, float partialTick) {
        ItemStack tank = ChemTank.worn(entity);
        if (tank.isEmpty()) {
            return null;
        }
        TankFuel fuel = ChemTank.contents(tank);
        return new Worn(fuel.isEmpty() ? 0 : fuel.level(), fuel.color(),
                !fuel.isEmpty() && fuel.fuel().brew() != Brew.EMBER_OIL, HexFx.time(entity.level(), partialTick),
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() instanceof com.pockyl.hextale.item.ChemArmourItem,
                ChemTank.hasJets(tank), com.pockyl.hextale.client.ChemJetsClient.heat(entity));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Worn worn = extract(player, partialTick);
        if (worn == null || player.isInvisible()) {
            return;
        }
        SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
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
            int core = FastColor.ARGB32.lerp(0.7F, worn.color(), 0xFFFFFF);
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
