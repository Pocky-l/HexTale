package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import com.pockyl.hextale.Hextale;

import java.util.List;

/**
 * Props on the Research Table and its gimbal rings, baked as separate models (props in place on the north-facing desk,
 * rings centred on their own block) and drawn by its renderer.
 */
public final class TableModels {
    public static final ModelResourceLocation PARCHMENT = standalone("block/research_table/parchment");
    public static final ModelResourceLocation PARCHMENT_STACK = standalone("block/research_table/parchment_stack");
    public static final ModelResourceLocation INK_BOTTLE = standalone("block/research_table/ink_bottle");
    public static final ModelResourceLocation EMPTY_BOTTLE = standalone("block/research_table/empty_bottle");
    public static final ModelResourceLocation BRUSH_LYING = standalone("block/research_table/brush_lying");
    public static final ModelResourceLocation BRUSH_IN_BOTTLE = standalone("block/research_table/brush_in_bottle");
    public static final ModelResourceLocation SCROLL = standalone("block/research_table/scroll");
    public static final ModelResourceLocation RING_OUTER = standalone("block/research_table/ring_outer");
    public static final ModelResourceLocation RING_INNER = standalone("block/research_table/ring_inner");
    /** The plain parchment of a rune scroll item, under the hieroglyph its renderer adds. */
    public static final ModelResourceLocation SCROLL_ITEM = ModelResourceLocation.standalone(Hextale.id("item/rune_scroll_base"));
    private static final List<ModelResourceLocation> ALL = List.of(PARCHMENT, PARCHMENT_STACK, INK_BOTTLE, EMPTY_BOTTLE, BRUSH_LYING,
            BRUSH_IN_BOTTLE, SCROLL, RING_OUTER, RING_INNER, SCROLL_ITEM);

    private TableModels() {
    }

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(Hextale.id(path));
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        ALL.forEach(event::register);
    }

    public static BakedModel get(ModelResourceLocation location) {
        return Minecraft.getInstance().getModelManager().getModel(location);
    }

    /** Turns the pose like a blockstate "y" rotation of a model built facing north. */
    public static void rotateToFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180) % 360)));
        poseStack.translate(-0.5, 0, -0.5);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, ModelResourceLocation location, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                buffers.getBuffer(Sheets.cutoutBlockSheet()), null, get(location), 1, 1, 1, light, overlay, ModelData.EMPTY,
                Sheets.cutoutBlockSheet());
    }
}
