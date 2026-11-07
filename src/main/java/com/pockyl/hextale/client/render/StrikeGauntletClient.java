package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import com.pockyl.hextale.port.client.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

import java.util.List;

import com.pockyl.hextale.network.StrikeDashPayload;
import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.StrikeDash;
import com.pockyl.hextale.runework.StrikeGauntletItem;

/**
 * The Strike Gauntlet is worn, not held. In first person the game draws no arm under a held item, so the hand is drawn
 * here instead: the bare arm where an empty hand would be (with its punch swing and equip motion) and the gauntlet put
 * on its fist the same way it sits on a player in third person ({@code thirdperson_*hand} of the item model: the model
 * is a right glove, {@code strike_gauntlet_left} the left one). While it charges the fist draws back and trembles; on
 * other players the arm aims the fist ({@link StrikeGauntletPose}). In a flight ({@link StrikeDash}) the fists thrust out
 * ahead and the view widens; seen from outside the body lies along the way, the fist first, with a streak of light
 * behind it.
 *
 * <p>1.21.1: the arm is drawn by the player renderer's renderRightHand / renderLeftHand, which post RenderArmEvent
 * themselves; the player is read directly (26.x: the avatar render state).
 */
public final class StrikeGauntletClient {
    /** One per hand: a submitted item state is drawn later in the frame, so the two hands never share one. */
    private static final ItemStackRenderState RIGHT = new ItemStackRenderState();
    private static final ItemStackRenderState LEFT = new ItemStackRenderState();

    private StrikeGauntletClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(StrikeGauntletClient::renderHand);
        NeoForge.EVENT_BUS.addListener(StrikeGauntletClient::widenView);
        StrikeDash.trail = (flight, from, to) -> WeaponFx.accept(new WeaponFxPayload(WeaponFxPayload.Kind.TRAIL.ordinal(), from, to, List.of(), List.of(),
                ModParticles.HEXTECH, 0.35F + 0.15F * flight.stage()));
    }

    /** The server's word on a flight: other players' flights start and end here, the wearer's own only at a wall. */
    public static void accept(StrikeDashPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.level.getEntity(payload.entity()) instanceof Player player)) {
            return;
        }
        if (player == minecraft.player) {
            if (payload.wall()) {
                StrikeDash.stop(player, true);
            }
        } else if (payload.start()) {
            StrikeDash.shown(player, payload.dir(), payload.stage());
        } else {
            StrikeDash.stop(player, payload.wall());
        }
    }

    /**
     * The body lies along the flight, not the look: an elytra's tilt, complete within two ticks. 1.21.1: the pose is turned
     * here, at the end of the renderer's rotations, from the body's yaw to the flight's (26.x: an elytra's render state).
     */
    public static void flightPose(LivingEntity entity, PoseStack pose, float bodyRot, float partialTick) {
        StrikeDash.Flight flight = entity instanceof Player player ? StrikeDash.of(player) : null;
        if (flight == null) {
            return;
        }
        Vec3 dir = flight.dir();
        float pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(dir.y, -1, 1)));
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float ticks = 3 + flight.age() * 4 + partialTick;
        float tilt = Mth.clamp(ticks * ticks / 100, 0, 1);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(bodyRot - yaw));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(tilt * (-90 - pitch)));
    }

    /** In a flight the head looks ahead along the body, as a glider's does. */
    public static float look(LivingEntity entity, float headPitch) {
        return entity instanceof Player player && StrikeDash.flying(player) ? -45 : headPitch;
    }

    private static void widenView(ComputeFovModifierEvent event) {
        if (StrikeDash.flying(event.getPlayer())) {
            event.setNewFovModifier(event.getNewFovModifier() * 1.2F);
        }
    }

    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        com.pockyl.hextale.port.client.SpecialItemModels.claim(ModItems.STRIKE_GAUNTLET.get());
        event.registerItem(new IClientItemExtensions() {
            // 1.21.1: the item's special model is drawn by the port's renderer, which needs this same registration.
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.pockyl.hextale.port.client.SpecialItemModels.customRenderer(ModItems.STRIKE_GAUNTLET.get());
            }

            @Override
            public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (entity instanceof Player player && StrikeDash.flying(player)) {
                    return StrikeGauntletPose.DASH.getValue();
                }
                return entity.isUsingItem() && entity.getUsedItemHand() == hand ? StrikeGauntletPose.CHARGE.getValue() : null;
            }
        }, ModItems.STRIKE_GAUNTLET.get());
    }

    private static void renderHand(RenderHandEvent event) {
        ItemStack stack = event.getItemStack();
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!(stack.getItem() instanceof StrikeGauntletItem) || player == null) {
            return;
        }
        event.setCanceled(true);
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = arm == HumanoidArm.RIGHT;
        float invert = right ? 1 : -1;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        if (player.isUsingItem() && player.getUsedItemHand() == event.getHand()) {
            // Charging: the fist draws back towards the shoulder, trembling harder with every stage.
            float held = player.getTicksUsingItem() + event.getPartialTick();
            float drawn = Mth.clamp(held / StrikeGauntletItem.STAGE_TICKS, 0, 1);
            float shake = Mth.sin(held * 1.3F) * 0.006F * StrikeGauntletItem.stage((int) held);
            pose.translate(invert * 0.06F * drawn + shake, -0.05F * drawn + shake, 0.16F * drawn);
        }
        StrikeDash.Flight flight = StrikeDash.of(player);
        if (flight != null) {
            // In flight: the fist thrusts out to the middle of the view within two ticks.
            float thrust = Mth.clamp((flight.age() + event.getPartialTick()) / 2, 0, 1);
            pose.translate(-invert * 0.24F * thrust, 0.14F * thrust, -0.4F * thrust);
        }
        armTransform(pose, event.getSwingProgress(), event.getEquipProgress(), invert);

        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().<AbstractClientPlayer>getRenderer(player);
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        ModelPart part = right ? model.rightArm : model.leftArm;
        int light = event.getPackedLight();
        if (player.isInvisible()) {
            // The bare arm stays unseen; the gauntlet still needs the arm's resting pose.
            part.resetPose();
            part.zRot = invert * 0.1F;
        } else if (right) {
            renderer.renderRightHand(pose, event.getMultiBufferSource(), light, player);
        } else {
            renderer.renderLeftHand(pose, event.getMultiBufferSource(), light, player);
        }

        // On the fist exactly as on a player's arm (ItemInHandLayer.renderArmWithItem).
        part.translateAndRotate(pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(invert / 16, 2 / 16F, -10 / 16F);
        ItemStackRenderState item = right ? RIGHT : LEFT;
        com.pockyl.hextale.port.client.ItemModelResolver.INSTANCE.updateForLiving(item, stack,
                right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, player);
        item.submit(pose, new SubmitNodeCollector(event.getMultiBufferSource()), light, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }

    /** Where the empty hand sits and how it punches (FirstPersonHandsAndItemsRenderer.renderPlayerArm). */
    private static void armTransform(PoseStack pose, float attack, float inverseArmHeight, float invert) {
        float sqrtAttack = Mth.sqrt(attack);
        float x = -0.3F * Mth.sin(sqrtAttack * Mth.PI);
        float y = 0.4F * Mth.sin(sqrtAttack * Mth.TWO_PI);
        float z = -0.4F * Mth.sin(attack * Mth.PI);
        pose.translate(invert * (x + 0.64000005F), y - 0.6F - inverseArmHeight * 0.6F, z - 0.71999997F);
        pose.mulPose(Axis.YP.rotationDegrees(invert * 45));
        pose.mulPose(Axis.YP.rotationDegrees(invert * Mth.sin(sqrtAttack * Mth.PI) * 70));
        pose.mulPose(Axis.ZP.rotationDegrees(invert * Mth.sin(attack * attack * Mth.PI) * -20));
        pose.translate(invert * -1, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(invert * 120));
        pose.mulPose(Axis.XP.rotationDegrees(200));
        pose.mulPose(Axis.YP.rotationDegrees(invert * -135));
        pose.translate(invert * 5.6F, 0, 0);
    }
}
