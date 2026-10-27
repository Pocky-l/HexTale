package com.pockyl.hextale.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Walks a box of blocks section by section and skips every 16-block section whose palette cannot hold what is looked
 * for (or that is not loaded), so a device looking for logs or crops around it reads only the sections that have some.
 */
public final class SectionScan {
    private SectionScan() {
    }

    /** Calls {@code found} for every loaded block in {@code min}..{@code max} (inclusive) whose state passes {@code wanted}. */
    public static void forEach(Level level, BlockPos min, BlockPos max, Predicate<BlockState> wanted, BiConsumer<BlockPos, BlockState> found) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int minSy = Math.max(level.getMinSectionY(), SectionPos.blockToSectionCoord(min.getY()));
        int maxSy = Math.min(level.getMaxSectionY(), SectionPos.blockToSectionCoord(max.getY()));
        for (int cx = SectionPos.blockToSectionCoord(min.getX()); cx <= SectionPos.blockToSectionCoord(max.getX()); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(min.getZ()); cz <= SectionPos.blockToSectionCoord(max.getZ()); cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                int x0 = Math.max(min.getX(), SectionPos.sectionToBlockCoord(cx));
                int x1 = Math.min(max.getX(), SectionPos.sectionToBlockCoord(cx) + 15);
                int z0 = Math.max(min.getZ(), SectionPos.sectionToBlockCoord(cz));
                int z1 = Math.min(max.getZ(), SectionPos.sectionToBlockCoord(cz) + 15);
                for (int sy = minSy; sy <= maxSy; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir() || !section.maybeHas(wanted)) {
                        continue;
                    }
                    int y0 = Math.max(min.getY(), SectionPos.sectionToBlockCoord(sy));
                    int y1 = Math.min(max.getY(), SectionPos.sectionToBlockCoord(sy) + 15);
                    for (int y = y0; y <= y1; y++) {
                        for (int x = x0; x <= x1; x++) {
                            for (int z = z0; z <= z1; z++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (wanted.test(state)) {
                                    found.accept(at.set(x, y, z), state);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
