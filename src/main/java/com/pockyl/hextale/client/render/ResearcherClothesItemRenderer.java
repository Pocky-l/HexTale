package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import org.joml.Vector3fc;

import com.pockyl.hextale.Hextale;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A piece of the researcher's clothes as an item: the very model it is worn as, standing in the slot and in the hand
 * ({@code {"type": "hextale:researcher_clothes", "piece": "robe"}} in an item model definition; the base model gives the
 * display transforms).
 */
public final class ResearcherClothesItemRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier ID = Hextale.id("researcher_clothes");

    private final ResearcherClothesClient.Piece piece;
    private final Model<Unit> model;

    private ResearcherClothesItemRenderer(ResearcherClothesClient.Piece piece, ModelPart root) {
        this.piece = piece;
        this.model = new Model<>(root, RenderTypes::armorCutoutNoCull) {
        };
    }

    /** Centres the piece on the item's middle and turns the model (y down, facing -z) the way items stand. */
    private void frame(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(piece.scale, -piece.scale, -piece.scale);
        poseStack.translate(0, -piece.centreY / 16F, 0);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        poseStack.pushPose();
        frame(poseStack);
        collector.submitModel(model, Unit.INSTANCE, poseStack, ResearcherClothesClient.TEXTURE, light, overlay, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        frame(poseStack);
        model.root().getExtentsForGui(poseStack, output);
    }

    /** The JSON side. */
    public record Unbaked(ResearcherClothesClient.Piece piece) implements NoDataSpecialModelRenderer.Unbaked {
        private static final Codec<ResearcherClothesClient.Piece> PIECE = StringRepresentable.fromEnum(PieceName::values)
                .xmap(PieceName::piece, piece -> PieceName.valueOf(piece.name()));
        public static final MapCodec<Unbaked> MAP_CODEC = PIECE.fieldOf("piece").xmap(Unbaked::new, Unbaked::piece);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ResearcherClothesItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new ResearcherClothesItemRenderer(piece, context.entityModelSet().bakeLayer(piece.layer));
        }
    }

    /** Lower-case names of the pieces in JSON. */
    private enum PieceName implements StringRepresentable {
        HOOD, ROBE, TROUSERS, BOOTS;

        ResearcherClothesClient.Piece piece() {
            return ResearcherClothesClient.Piece.valueOf(name());
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
