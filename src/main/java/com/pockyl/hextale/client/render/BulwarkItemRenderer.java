package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.ModelPart;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import com.pockyl.hextale.port.client.SpecialModelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import com.pockyl.hextale.Hextale;

/**
 * A piece of Bulwark armour as an item: the very model it is worn as, painted with the sheet of its level
 * ({@code {"type": "hextale:bulwark", "piece": "chestplate"}} in an item model definition; the base model gives the
 * display transforms).
 */
public final class BulwarkItemRenderer implements SpecialModelRenderer<ResourceLocation> {
    public static final ResourceLocation ID = Hextale.id("bulwark");

    private final BulwarkClient.Piece piece;
    private final ModelPart root;

    private BulwarkItemRenderer(BulwarkClient.Piece piece, ModelPart root) {
        this.piece = piece;
        this.root = root;
    }

    @Override
    public @Nullable ResourceLocation extractArgument(ItemStack stack) {
        return BulwarkClient.texture(piece, stack);
    }

    /** Centres the piece on the item's middle and turns the model (y down, facing -z) the way items stand. */
    private void frame(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(piece.scale, -piece.scale, -piece.scale);
        poseStack.translate(0, -piece.centreY / 16F, 0);
    }

    @Override
    public void submit(@Nullable ResourceLocation texture, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
                       boolean hasFoil, int outlineColor) {
        if (texture == null) {
            return;
        }
        poseStack.pushPose();
        frame(poseStack);
        root.render(poseStack, collector.buffer(RenderType.armorCutoutNoCull(texture)), light, overlay);
        poseStack.popPose();
    }

    /** The JSON side. */
    public record Unbaked(BulwarkClient.Piece piece) implements SpecialModelRenderer.Unbaked<ResourceLocation> {
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
