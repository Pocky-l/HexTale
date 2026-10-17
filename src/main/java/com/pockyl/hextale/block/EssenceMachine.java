package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

/**
 * A block brass pipes connect to (alembic, separator, tank). It has ports - the faces essence goes in or out through -
 * and a pipe joins it only at a port. A machine may keep a boolean per face ({@link #PORTS}, the same names as the
 * pipe's): true while a pipe is connected there, so its model shows the joint.
 */
public interface EssenceMachine {
    Map<Direction, BooleanProperty> PORTS = PipeBlock.PROPERTY_BY_DIRECTION;

    /** Whether essence goes in or out through this face of the machine in this state. */
    boolean isPort(BlockState state, Direction face);

    /** The state with every port property it has set for its neighbours. */
    static BlockState connectAll(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction face : Direction.values()) {
            state = connect(state, face, level.getBlockState(pos.relative(face)));
        }
        return state;
    }

    /** The state with the port property on {@code face} set for the neighbour there. */
    static BlockState connect(BlockState state, Direction face, BlockState neighbour) {
        BooleanProperty property = PORTS.get(face);
        if (!state.hasProperty(property) || !(state.getBlock() instanceof EssenceMachine machine)) {
            return state;
        }
        boolean joined = neighbour.getBlock() instanceof BrassPipeBlock
                // A drain valve right beside a jar takes from it directly; both show the joint.
                || state.getBlock() instanceof DrainValveBlock && neighbour.getBlock() instanceof EssenceTankBlock
                || state.getBlock() instanceof EssenceTankBlock && neighbour.getBlock() instanceof DrainValveBlock valve
                        && valve.isPort(neighbour, face.getOpposite());
        return state.setValue(property, joined && machine.isPort(state, face));
    }

    /** Turns the port properties with the block (a pipe that was connected stays connected). */
    static BlockState rotatePorts(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction face : Direction.Plane.HORIZONTAL) {
            BooleanProperty from = PORTS.get(face);
            BooleanProperty to = PORTS.get(rotation.rotate(face));
            if (state.hasProperty(from) && state.hasProperty(to)) {
                rotated = rotated.setValue(to, state.getValue(from));
            }
        }
        return rotated;
    }
}
