package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.HexcoreMatrix;

import java.util.function.Consumer;

/**
 * The Hexcore as an item: only its rune matrix round the glowing crystal, no cradle, at its true size, its layers
 * twisting now and then like a puzzle cube ({@link HexcoreMatrix}, one arrangement shared by every Hexcore item). The
 * base model gives the display transforms; the item model definition wraps this in {@link AnimatedItemModel} so the
 * slot is drawn again every frame.
 */
public final class HexcoreItemRenderer implements NoDataSpecialModelRenderer {
    public static final Identifier ID = Hextale.id("hexcore");
    /** Ticks between twists and ticks a twist takes: livelier than the placed Hexcore at rest. */
    private static final float INTERVAL = 22;
    private static final float DURATION = 12;
    /** Half the matrix across (block units), centred on the item's middle. */
    private static final float REACH = 12.1F / 16;

    private final HexcoreMatrix matrix = new HexcoreMatrix();
    private final RandomSource random = RandomSource.create();

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        Minecraft minecraft = Minecraft.getInstance();
        float time = HexFx.time(minecraft.level, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        matrix.update(time, INTERVAL, DURATION, random);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        for (int i = 0; i < HexcoreMatrix.TILES.length; i++) {
            poseStack.pushPose();
            poseStack.mulPose(matrix.pose(i, time));
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            TableModels.submit(poseStack, collector, TableModels.HEXCORE_TILES.get(i), LightCoordsUtil.FULL_BRIGHT);
            poseStack.popPose();
        }
        float pulse = 1 + 0.06F * Mth.sin(time * 0.2F);
        poseStack.scale(pulse, pulse, pulse);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        TableModels.submit(poseStack, collector, TableModels.HEXCORE_CRYSTAL, LightCoordsUtil.FULL_BRIGHT);
        poseStack.popPose();
    }

    /** The matrix is nearly a ball: the six poles and the eight diagonals on it bound it in any turn. */
    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int axis = 0; axis < 3; axis++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                output.accept(new Vector3f(0.5F).setComponent(axis, 0.5F + sign * REACH));
            }
        }
        float diagonal = REACH / Mth.sqrt(3);
        for (int corner = 0; corner < 8; corner++) {
            output.accept(new Vector3f((corner & 1) == 0 ? -diagonal : diagonal, (corner & 2) == 0 ? -diagonal : diagonal,
                    (corner & 4) == 0 ? -diagonal : diagonal).add(0.5F, 0.5F, 0.5F));
        }
    }

    /** The JSON side: {@code {"type": "hextale:hexcore"}} in an item model definition. */
    public record Unbaked() implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public HexcoreItemRenderer bake(SpecialModelRenderer.BakingContext context) {
            return new HexcoreItemRenderer();
        }
    }
}
