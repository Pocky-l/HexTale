package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.ModelPart;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderType;
import com.pockyl.hextale.port.client.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ChemGear;
import com.pockyl.hextale.chem.ChemTank;

/**
 * A piece of chem-steel armour as an item: the very model it is worn as, with the fuel behind its glass in the colour of
 * its own tank (the tank chestplate) or of the tank the player wears ({@code {"type": "hextale:chem_steel", "piece":
 * "chestplate"}} in an item model definition; the base model gives the display transforms).
 */
public final class ChemSteelItemRenderer implements SpecialModelRenderer<Integer> {
    public static final ResourceLocation ID = Hextale.id("chem_steel");

    private final ChemSteelClient.Piece piece;
    private final ModelPart root;

    private ChemSteelItemRenderer(ChemSteelClient.Piece piece, ModelPart root) {
        this.piece = piece;
        this.root = root;
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
        root.render(poseStack, collector.buffer(RenderType.armorCutoutNoCull(ChemSteelClient.TEXTURE)), light, overlay);
        if (tint != null && tint != 0) {
            // 1.21.1: drawn right after the armour sheet, at the same depth.
            root.render(poseStack, collector.buffer(RenderType.armorCutoutNoCull(ChemSteelClient.FUEL_TEXTURE)), light, OverlayTexture.NO_OVERLAY, tint);
        }
        poseStack.popPose();
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
