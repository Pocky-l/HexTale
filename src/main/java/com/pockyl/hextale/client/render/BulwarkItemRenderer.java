package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;

import com.pockyl.hextale.Hextale;

/**
 * A piece of Bulwark armour as an item: the very model it is worn as, painted with the sheet of its level
 * ({@code {"type": "hextale:bulwark", "piece": "chestplate"}} in an item model definition; the base model gives the
 * display transforms).
 */
public final class BulwarkItemRenderer implements SpecialModelRenderer<Identifier> {
    public static final Identifier ID = Hextale.id("bulwark");

    private final BulwarkClient.Piece piece;
    private final Model<Unit> model;

    private BulwarkItemRenderer(BulwarkClient.Piece piece, ModelPart root) {
        this.piece = piece;
        this.model = new Model<>(root, RenderTypes::armorCutoutNoCull) {
        };
    }

    @Override
    public @Nullable Identifier extractArgument(ItemStack stack) {
        return BulwarkClient.texture(piece, stack);
    }

    /** Centres the piece on the item's middle and turns the model (y down, facing -z) the way items stand. */
    private void frame(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(piece.scale, -piece.scale, -piece.scale);
        poseStack.translate(0, -piece.centreY / 16F, 0);
    }

    @Override
    public void submit(@Nullable Identifier texture, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
                       boolean hasFoil, int outlineColor) {
        if (texture == null) {
            return;
        }
        poseStack.pushPose();
        frame(poseStack);
        collector.submitModel(model, Unit.INSTANCE, poseStack, texture, light, overlay, outlineColor, null);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        frame(poseStack);
        model.root().getExtentsForGui(poseStack, output);
    }

    /** The JSON side. */
    public record Unbaked(BulwarkClient.Piece piece) implements SpecialModelRenderer.Unbaked<Identifier> {
        private static final Codec<BulwarkClient.Piece> PIECE = StringRepresentable.fromEnum(PieceName::values)
                .xmap(PieceName::piece, piece -> PieceName.valueOf(piece.name()));
        public static final MapCodec<Unbaked> MAP_CODEC = PIECE.fieldOf("piece").xmap(Unbaked::new, Unbaked::piece);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public BulwarkItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new BulwarkItemRenderer(piece, context.entityModelSet().bakeLayer(piece.layer));
        }
    }

    /** Lower-case names of the pieces in JSON. */
    private enum PieceName implements StringRepresentable {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS;

        BulwarkClient.Piece piece() {
            return BulwarkClient.Piece.valueOf(name());
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
