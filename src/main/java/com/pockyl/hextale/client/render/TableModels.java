package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import com.pockyl.hextale.Hextale;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Props on the Research Table and its gimbal rings, baked as standalone models (props in place on the north-facing
 * desk, rings centred on their own block) and submitted by its renderer.
 */
public final class TableModels {
    private static final Map<StandaloneModelKey<BlockStateModelPart>, Identifier> MODELS = new LinkedHashMap<>();
    private static final int[] NO_TINTS = new int[0];

    public static final StandaloneModelKey<BlockStateModelPart> PARCHMENT = prop("parchment");
    public static final StandaloneModelKey<BlockStateModelPart> PARCHMENT_STACK = prop("parchment_stack");
    public static final StandaloneModelKey<BlockStateModelPart> INK_BOTTLE = prop("ink_bottle");
    public static final StandaloneModelKey<BlockStateModelPart> BRUSH_IN_BOTTLE = prop("brush_in_bottle");
    public static final StandaloneModelKey<BlockStateModelPart> SCROLL = prop("scroll");
    public static final StandaloneModelKey<BlockStateModelPart> RING_OUTER = prop("ring_outer");
    public static final StandaloneModelKey<BlockStateModelPart> RING_INNER = prop("ring_inner");
    /** The loupe arm: its post, the swivelling boom with the main lens, and a hinged lens (drawn twice). */
    public static final StandaloneModelKey<BlockStateModelPart> LOUPE_POST = prop("loupe_post");
    public static final StandaloneModelKey<BlockStateModelPart> LOUPE_ARM = prop("loupe_arm");
    public static final StandaloneModelKey<BlockStateModelPart> LOUPE_LENS = prop("loupe_lens");
    /** The Hextech Altar's gimbal, larger rings with hextech studs, centred on their block like the table's. */
    public static final StandaloneModelKey<BlockStateModelPart> ALTAR_RING_OUTER = model("block/hextech_altar/ring_outer");
    public static final StandaloneModelKey<BlockStateModelPart> ALTAR_RING_INNER = model("block/hextech_altar/ring_inner");
    /** The Essence Jar's body (drawn by the jar's renderer so it can rock; four times larger for the Big Essence Jar). */
    public static final StandaloneModelKey<BlockStateModelPart> ESSENCE_JAR = model("block/essence_tank");
    public static final StandaloneModelKey<BlockStateModelPart> CREATIVE_JAR = model("block/creative_essence_tank");
    /** The Hex Lapidary's lap disc, turned by the lapidary's renderer about its spindle. */
    public static final StandaloneModelKey<BlockStateModelPart> LAPIDARY_DISC = model("block/hex_lapidary_disc");
    public static final StandaloneModelKey<BlockStateModelPart> WHIRL_REAPER_VANES = model("block/whirl_reaper_vanes");
    public static final StandaloneModelKey<BlockStateModelPart> HEX_SAW_DISC = model("block/hex_sawmill_disc");
    /** The formed Hex Foundry round its heart, facing north, and its glowing parts (drawn at full light while hot). */
    public static final StandaloneModelKey<BlockStateModelPart> HEX_FOUNDRY = model("block/hex_foundry/formed");
    public static final StandaloneModelKey<BlockStateModelPart> HEX_FOUNDRY_GLOW = model("block/hex_foundry/glow");
    /** The Hexcore's matrix piece by piece (tiles in {@link com.pockyl.hextale.block.HexcoreMatrix#TILES} order), centred on their own block. */
    public static final List<StandaloneModelKey<BlockStateModelPart>> HEXCORE_TILES = java.util.Arrays.stream(com.pockyl.hextale.block.HexcoreMatrix.TILES)
            .map(tile -> model("block/hexcore/" + tile)).toList();
    public static final StandaloneModelKey<BlockStateModelPart> HEXCORE_CRYSTAL = model("block/hexcore/crystal");
    /** The Grapple Gauntlet's claw, pointing along -Z with the rope's eye at (8, 8, 12) px. */
    public static final StandaloneModelKey<BlockStateModelPart> GRAPPLE_HOOK = model("item/grapple_hook");

    private TableModels() {
    }

    private static StandaloneModelKey<BlockStateModelPart> prop(String name) {
        return model("block/research_table/" + name);
    }

    private static StandaloneModelKey<BlockStateModelPart> model(String path) {
        Identifier id = Hextale.id(path);
        StandaloneModelKey<BlockStateModelPart> key = new StandaloneModelKey<>(id::toString);
        MODELS.put(key, id);
        return key;
    }

    public static void register(ModelEvent.RegisterStandalone event) {
        MODELS.forEach((key, id) -> event.register(key, SimpleUnbakedStandaloneModel.simpleModelWrapper(id)));
    }

    /** Turns the pose like a blockstate "y" rotation of a model built facing north. */
    public static void rotateToFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0, 0.5);
        poseStack.rotate(Axis.YP.rotationDegrees(-((facing.toYRot() + 180) % 360)));
        poseStack.translate(-0.5, 0, -0.5);
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, StandaloneModelKey<BlockStateModelPart> key, int light) {
        BlockStateModelPart part = Minecraft.getInstance().getModelManager().getStandaloneModel(key);
        collector.submitMultiLayerBlockModel(poseStack, List.of(part), false, NO_TINTS, light, OverlayTexture.NO_OVERLAY, 0);
    }
}
