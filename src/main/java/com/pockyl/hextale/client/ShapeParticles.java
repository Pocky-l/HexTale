package com.pockyl.hextale.client;

import com.pockyl.hextale.block.ModelShapes;
import com.pockyl.hextale.block.ShapePartBlock;
import com.pockyl.hextale.block.ShapeParts;
import com.pockyl.hextale.registry.ModBlocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Break particles of blocks whose shapes come from their models. The game puts up to 64 particles into every box of a
 * shape, and a generated shape can have dozens of boxes spreading over three blocks (the Hexcore had thousands of
 * particles, more again from each stand-in going with it). Here a block breaks into one grid over its shape's bounds,
 * as many particles as a full block at most; a device's stand-ins never break into particles of their own.
 */
final class ShapeParticles {
    private static final int MAX_PER_AXIS = 4;

    private ShapeParticles() {
    }

    static void register(RegisterClientExtensionsEvent event) {
        event.registerBlock(new IClientBlockExtensions() {
            @Override
            public boolean addHitEffects(BlockState state, Level level, net.minecraft.world.phys.@org.jspecify.annotations.Nullable HitResult target, ParticleEngine manager) {
                return true;
            }

            /** Only the player who mined the part gets this (the device's own break shows to everyone else). */
            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                BlockPos at = ShapePartBlock.device(state, pos);
                BlockState device = level.getBlockState(at);
                VoxelShape shape = device.getShape(level, at);
                if (level instanceof ClientLevel client && device.is(ShapeParts.OWNERS) && !shape.isEmpty()) {
                    burst(client, at, device, shape.bounds(), manager);
                }
                return true;
            }
        }, ModBlocks.SHAPE_PART.get());
        List<Block> shaped = new ArrayList<>();
        for (var entry : ModBlocks.BLOCKS.getEntries()) {
            if (ModelShapes.covers(entry.get())) {
                shaped.add(entry.get());
            }
        }
        event.registerBlock(new IClientBlockExtensions() {
            @Override
            public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
                VoxelShape shape = state.getShape(level, pos);
                if (level instanceof ClientLevel client && !shape.isEmpty()) {
                    burst(client, pos, state, shape.bounds(), manager);
                }
                return true;
            }
        }, shaped.toArray(Block[]::new));
    }

    private static void burst(ClientLevel level, BlockPos pos, BlockState state, AABB box, ParticleEngine manager) {
        int countX = count(box.getXsize());
        int countY = count(box.getYsize());
        int countZ = count(box.getZsize());
        for (int xx = 0; xx < countX; xx++) {
            for (int yy = 0; yy < countY; yy++) {
                for (int zz = 0; zz < countZ; zz++) {
                    double relX = (xx + 0.5) / countX;
                    double relY = (yy + 0.5) / countY;
                    double relZ = (zz + 0.5) / countZ;
                    manager.add(new TerrainParticle(level, pos.getX() + box.minX + relX * box.getXsize(),
                            pos.getY() + box.minY + relY * box.getYsize(), pos.getZ() + box.minZ + relZ * box.getZsize(),
                            relX - 0.5, relY - 0.5, relZ - 0.5, state, pos).updateSprite(state, pos));
                }
            }
        }
    }

    /** Particles along one side: one per quarter block like the game, at least 2 and at most 4. */
    private static int count(double size) {
        return Mth.clamp(Mth.ceil(Math.min(1.0, size) / 0.25), 2, MAX_PER_AXIS);
    }
}
