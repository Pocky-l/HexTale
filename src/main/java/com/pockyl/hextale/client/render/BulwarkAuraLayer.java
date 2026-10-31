package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

import com.pockyl.hextale.runework.BulwarkArmorItem;
import com.pockyl.hextale.runework.RuneTier;

/**
 * The tools' auras on worn Bulwark armour: radiant pieces carry the quiet lightning, riftforged ones the abyss, each as
 * fields along the body parts the piece covers (helmet - head, cuirass - body and arms, greaves - legs, boots - feet),
 * thinner than on a tool so a whole suit stays quiet. Drawn on players and armour stands.
 *
 * <p>1.21.1: the layer reads the worn pieces off the entity (26.x: the humanoid render state).
 */
public class BulwarkAuraLayer<S extends LivingEntity, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    /** A field along one body part, in that part's pixels (y down). */
    private record Field(Function<HumanoidModel<?>, ModelPart> part, ToolAuraRenderer lightning, ToolAuraRenderer abyss) {
        static Field of(Function<HumanoidModel<?>, ModelPart> part, float x, float fromY, float toY, List<Float> profile, float depth, long seed) {
            return new Field(part,
                    ToolAuraRenderer.field(ToolAuraRenderer.Kind.LIGHTNING, new Vector3f(x, fromY, 0), new Vector3f(x, toY, 0), profile, depth, seed, DENSITY),
                    ToolAuraRenderer.field(ToolAuraRenderer.Kind.VOID, new Vector3f(x, fromY, 0), new Vector3f(x, toY, 0), profile, depth, seed, DENSITY));
        }
    }

    /** Each field's share of a tool's aura. */
    private static final float DENSITY = 0.35F;

    private static final List<Field> HEAD = List.of(Field.of(model -> model.head, 0, 0, -8, List.of(4.5F, 5.2F, 4.6F), 1, 101));
    private static final List<Field> CHEST = List.of(
            Field.of(model -> model.body, 0, 12, 0, List.of(4.6F, 5.0F, 5.6F), 0.7F, 211),
            Field.of(model -> model.rightArm, -1, 10, -2, List.of(2.6F, 2.8F, 3.2F), 1, 223),
            Field.of(model -> model.leftArm, 1, 10, -2, List.of(2.6F, 2.8F, 3.2F), 1, 227));
    private static final List<Field> LEGS = List.of(
            Field.of(model -> model.rightLeg, 0, 12, 0, List.of(2.6F, 2.8F, 3.0F), 1, 307),
            Field.of(model -> model.leftLeg, 0, 12, 0, List.of(2.6F, 2.8F, 3.0F), 1, 311));
    private static final List<Field> FEET = List.of(
            Field.of(model -> model.rightLeg, 0, 12.5F, 8, List.of(3.2F, 3.0F), 1, 401),
            Field.of(model -> model.leftLeg, 0, 12.5F, 8, List.of(3.2F, 3.0F), 1, 409));

    public BulwarkAuraLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new BulwarkAuraLayer<>(renderer));
            }
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof LivingEntityRenderer stand) {
            stand.addLayer(new BulwarkAuraLayer(stand));
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int lightCoords, S entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
        Minecraft minecraft = Minecraft.getInstance();
        float time = HexFx.time(minecraft.level, partialTick);
        piece(poseStack, collector, entity.getItemBySlot(EquipmentSlot.HEAD), HEAD, time);
        piece(poseStack, collector, entity.getItemBySlot(EquipmentSlot.CHEST), CHEST, time);
        piece(poseStack, collector, entity.getItemBySlot(EquipmentSlot.LEGS), LEGS, time);
        piece(poseStack, collector, entity.getItemBySlot(EquipmentSlot.FEET), FEET, time);
    }

    private void piece(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, List<Field> fields, float time) {
        if (!(stack.getItem() instanceof BulwarkArmorItem)) {
            return;
        }
        int level = RuneTier.tier(stack);
        if (level < 3) {
            return;
        }
        for (Field field : fields) {
            poseStack.pushPose();
            field.part().apply(getParentModel()).translateAndRotate(poseStack);
            (level >= 4 ? field.abyss() : field.lightning()).draw(poseStack, collector, time);
            poseStack.popPose();
        }
    }
}
