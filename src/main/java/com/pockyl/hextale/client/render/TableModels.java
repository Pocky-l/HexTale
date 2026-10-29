package com.pockyl.hextale.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import com.pockyl.hextale.port.client.SubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import com.pockyl.hextale.Hextale;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Props on the Research Table and its gimbal rings, baked as standalone models (props in place on the north-facing
 * desk, rings centred on their own block) and submitted by its renderer.
 *
 * <p>1.21.1: additional models drawn on the translucent block sheet (26.x picks the layer per quad).
 */
public final class TableModels {
    private static final Map<ModelResourceLocation, ResourceLocation> MODELS = new LinkedHashMap<>();

    public static final ModelResourceLocation PARCHMENT = prop("parchment");
    public static final ModelResourceLocation PARCHMENT_STACK = prop("parchment_stack");
    public static final ModelResourceLocation INK_BOTTLE = prop("ink_bottle");
    public static final ModelResourceLocation BRUSH_IN_BOTTLE = prop("brush_in_bottle");
    public static final ModelResourceLocation SCROLL = prop("scroll");
    public static final ModelResourceLocation RING_OUTER = prop("ring_outer");
    public static final ModelResourceLocation RING_INNER = prop("ring_inner");
    /** The loupe arm: its post, the swivelling boom with the main lens, and a hinged lens (drawn twice). */
    public static final ModelResourceLocation LOUPE_POST = prop("loupe_post");
    public static final ModelResourceLocation LOUPE_ARM = prop("loupe_arm");
    public static final ModelResourceLocation LOUPE_LENS = prop("loupe_lens");
    /** The Hextech Altar's gimbal, larger rings with hextech studs, centred on their block like the table's. */
    public static final ModelResourceLocation ALTAR_RING_OUTER = model("block/hextech_altar/ring_outer");
    public static final ModelResourceLocation ALTAR_RING_INNER = model("block/hextech_altar/ring_inner");
    /** The Essence Jar's body (drawn by the jar's renderer so it can rock; four times larger for the Big Essence Jar). */
    public static final ModelResourceLocation ESSENCE_JAR = model("block/essence_tank");
    public static final ModelResourceLocation CREATIVE_JAR = model("block/creative_essence_tank");
    /**
     * The Hex Lapidary's grinding wheel, the cutting disc on its side and the treadle's flywheel under the bench, turned by
     * the lapidary's renderer about their axles (along x).
     */
    public static final ModelResourceLocation LAPIDARY_WHEEL = model("block/hex_lapidary_wheel");
    public static final ModelResourceLocation LAPIDARY_DISC = model("block/hex_lapidary_disc");
    public static final ModelResourceLocation LAPIDARY_GEAR = model("block/hex_lapidary_gear");
    public static final ModelResourceLocation WHIRL_REAPER_VANES = model("block/whirl_reaper_vanes");
    public static final ModelResourceLocation HEX_SAW_DISC = model("block/hex_sawmill_disc");
    /** The formed Hex Foundry round its heart, facing north, and its glowing parts (drawn at full light while hot). */
    public static final ModelResourceLocation HEX_FOUNDRY = model("block/hex_foundry/formed");
    public static final ModelResourceLocation HEX_FOUNDRY_GLOW = model("block/hex_foundry/glow");
    /** The Hexcore's matrix piece by piece (tiles in {@link com.pockyl.hextale.block.HexcoreMatrix#TILES} order), centred on their own block. */
    public static final List<ModelResourceLocation> HEXCORE_TILES = java.util.Arrays.stream(com.pockyl.hextale.block.HexcoreMatrix.TILES)
            .map(tile -> model("block/hexcore/" + tile)).toList();
    public static final ModelResourceLocation HEXCORE_CRYSTAL = model("block/hexcore/crystal");
    /** The Grapple Gauntlet's claw, pointing along -Z with the rope's eye at (8, 8, 12) px. */
    public static final ModelResourceLocation GRAPPLE_HOOK = model("item/grapple_hook");
    /** The Impulse Plate's launch panel, built for a north-facing plate, hinged at its front edge (8, 2, 2) px. */
    public static final ModelResourceLocation IMPULSE_PANEL = model("block/impulse_plate_panel");
    /** The Levitation Plate's floating ring, centred on (8, 6, 8) px. */
    public static final ModelResourceLocation LEVITATION_RING = model("block/levitation_plate_ring");
    /** A Dust Charge stuck on a wall: base on its -Z face at (8, 8, 0) px. */
    public static final ModelResourceLocation DUST_CHARGE_STUCK = model("item/dust_charge_stuck");
    /** The Chem Boiler's gauge needle (built pointing up from {@code ChemBoilerRenderer.NEEDLE_PIVOT}) and safety valve. */
    public static final ModelResourceLocation CHEM_BOILER_NEEDLE = model("block/chem_boiler_needle");
    public static final ModelResourceLocation CHEM_BOILER_VALVE = model("block/chem_boiler_valve");

    private TableModels() {
    }

    private static ModelResourceLocation prop(String name) {
        return model("block/research_table/" + name);
    }

    private static ModelResourceLocation model(String path) {
        ResourceLocation id = Hextale.id(path);
        ModelResourceLocation key = ModelResourceLocation.standalone(id);
        MODELS.put(key, id);
        return key;
    }

    public static void register(ModelEvent.RegisterAdditional event) {
        MODELS.keySet().forEach(event::register);
    }

    /** Turns the pose like a blockstate "y" rotation of a model built facing north. */
    public static void rotateToFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180) % 360)));
        poseStack.translate(-0.5, 0, -0.5);
    }

    public static void submit(PoseStack poseStack, SubmitNodeCollector collector, ModelResourceLocation key, int light) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(key);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                collector.buffer(Sheets.translucentCullBlockSheet()), null, model, 1, 1, 1, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, Sheets.translucentCullBlockSheet());
    }
}
