package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.block.ShapePartBlock;
import com.pockyl.hextale.block.ShapeParts;

/**
 * Right clicks on devices that spread past their cell. Such a device and its stand-ins each carry the whole device's
 * shape, so a look ray that passes over one of their cells first reports a point on the device that may lie a cell or
 * two away from the cell it names - a flat Rune Circle seen from a few steps away nearly always. The server throws such a
 * click away ("too far away from hit block"), so the item never went in until the player stepped close. Before the click
 * goes out, the hit is moved to the cell that really holds the point, when that cell is part of the same device.
 */
public final class ShapeHits {
    /** How far the point is pushed into the face it hit, so a point on a cell's border lands in the cell behind it. */
    private static final double INTO = 1.0E-4;

    private ShapeHits() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ShapeHits::onInteraction);
    }

    private static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isUseItem() || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK || !spreads(minecraft.level.getBlockState(hit.getBlockPos()))) {
            return;
        }
        Vec3 location = hit.getLocation();
        Vec3 inside = location.subtract(Vec3.atLowerCornerOf(hit.getDirection().getUnitVec3i()).scale(INTO));
        BlockPos holder = BlockPos.containing(inside);
        if (!holder.equals(hit.getBlockPos()) && spreads(minecraft.level.getBlockState(holder))) {
            minecraft.hitResult = new BlockHitResult(location, hit.getDirection(), holder, hit.isInside());
        }
    }

    /** A device whose shape spreads past its cell, or one of its stand-ins. */
    private static boolean spreads(BlockState state) {
        return state.getBlock() instanceof ShapePartBlock || state.is(ShapeParts.OWNERS);
    }
}
