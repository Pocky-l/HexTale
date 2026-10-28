package com.pockyl.hextale.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;

/**
 * A 26.x entity model: a part tree posed from a render state. Every {@link #setupAnim} starts from the rest pose.
 *
 * <p>port-of: net.minecraft.client.model.EntityModel
 */
public abstract class EntityModel<S> {
    protected final ModelPart root;

    protected EntityModel(ModelPart root) {
        this.root = root;
    }

    public ModelPart root() {
        return root;
    }

    public void setupAnim(S state) {
        root.getAllParts().forEach(ModelPart::resetPose);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        root.render(poseStack, buffer, light, overlay, color);
    }
}
