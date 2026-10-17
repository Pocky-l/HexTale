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

    /**
     * Where items are shown going in: the middle of the block's top, whatever face the link uses (a side face would send
     * the flight down to the ground beside the block). The face only decides the slots.
     */
    public Vec3 entry() {
        return Vec3.atCenterOf(pos).add(0, 0.5, 0);
    }

    /** Above the block's top: where a flight turns to sink in. */
    public Vec3 approach() {
        return Vec3.atCenterOf(pos).add(0, 1.1, 0);
    }
}
