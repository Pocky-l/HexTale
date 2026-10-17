package com.pockyl.hextale.transport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Where a pylon sends: a block and the face it was linked by. A container takes items through that face (a furnace's
 * top feeds what it smelts, a side its fuel); a relay pylon passes them on.
 */
public record PylonLink(BlockPos pos, Direction face) {
    public static final Codec<PylonLink> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PylonLink::pos),
            Direction.CODEC.fieldOf("face").forGetter(PylonLink::face)
    ).apply(instance, PylonLink::new));

    /** The middle of the linked face, where items go in. */
    public Vec3 entry() {
        return Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
    }

    /** A little in front of the linked face: where a flight turns to go in. */
    public Vec3 approach() {
        return Vec3.atCenterOf(pos).add(face.getStepX() * 0.95, face.getStepY() * 0.95, face.getStepZ() * 0.95);
    }
}
