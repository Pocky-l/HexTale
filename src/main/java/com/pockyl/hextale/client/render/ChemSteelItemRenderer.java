package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ChemGear;
import com.pockyl.hextale.chem.ChemTank;

/**
 * A piece of chem-steel armour as an item: the very model it is worn as, with the fuel behind its glass in the colour of
 * its own tank (the tank chestplate) or of the tank the player wears ({@code {"type": "hextale:chem_steel", "piece":
 * "chestplate"}} in an item model definition; the base model gives the display transforms).
 */
public final class ChemSteelItemRenderer implements SpecialModelRenderer<Integer> {
    public static final Identifier ID = Hextale.id("chem_steel");

    private final ChemSteelClient.Piece piece;
    private final Model<Unit> model;

    private ChemSteelItemRenderer(ChemSteelClient.Piece piece, ModelPart root) {
        this.piece = piece;
        this.model = new Model<>(root, RenderTypes::armorCutoutNoCull) {
        };
    }

    @Override
    public Integer extractArgument(ItemStack stack) {
        ItemStack tank = ChemTank.is(stack) ? stack : ChemGear.clientTank.get();
        return ChemSteelClient.fuelTint(ChemTank.contents(tank));
    }

    /** Centres the piece on the item's middle and turns the model (y down, facing -z) the way items stand. */
    private void frame(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(piece.scale, -piece.scale, -piece.scale);
        poseStack.translate(0, -piece.centreY / 16F, 0);
    }

    @Override
    public void submit(@Nullable Integer tint, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay,
                       boolean hasFoil, int outlineColor) {
        poseStack.pushPose();
        frame(poseStack);
        collector.submitModel(model, Unit.INSTANCE, poseStack, ChemSteelClient.TEXTURE, light, overlay, outlineColor);
        if (tint != null && tint != 0) {
            collector.order(ChemSteelFuelLayer.ORDER).submitModel(model, Unit.INSTANCE, poseStack, model.renderType(ChemSteelClient.FUEL_TEXTURE), light, OverlayTexture.NO_OVERLAY, tint,
                    null, outlineColor);
        }
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        frame(poseStack);
        model.root().getExtentsForGui(poseStack, output);
    }

    /** The JSON side. */
    public record Unbaked(ChemSteelClient.Piece piece) implements SpecialModelRenderer.Unbaked<Integer> {
        private static final Codec<ChemSteelClient.Piece> PIECE = StringRepresentable.fromEnum(PieceName::values)
                .xmap(PieceName::piece, piece -> PieceName.valueOf(piece.name()));
        public static final MapCodec<Unbaked> MAP_CODEC = PIECE.fieldOf("piece").xmap(Unbaked::new, Unbaked::piece);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ChemSteelItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new ChemSteelItemRenderer(piece, context.entityModelSet().bakeLayer(piece.layer));
        }
    }

    /** Lower-case names of the pieces in JSON. */
    private enum PieceName implements StringRepresentable {
        HELMET, CHESTPLATE, LEGGINGS, BOOTS;

        ChemSteelClient.Piece piece() {
            return ChemSteelClient.Piece.valueOf(name());
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
