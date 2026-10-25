package com.pockyl.hextale.client;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.List;
import java.util.Set;

/**
 * Where the edge of the world wears thin, the green goes out of the land: grass, leaves and vines lose their colour and
 * lean to an ashen lilac, water darkens. Wraps the tints the game already gives these blocks.
 */
public final class ThinningTint {
    /** The ash the land fades to. */
    private static final int ASH = 0x7C7088;
    /** The murk water darkens to. */
    private static final int MURK = 0x3B2F4A;
    private static final Block[] PLANTS = {
            Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.BUSH,
            Blocks.POTTED_FERN, Blocks.PINK_PETALS, Blocks.WILDFLOWERS, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
            Blocks.VINE, Blocks.SUGAR_CANE, Blocks.LEAF_LITTER};
    private static final Block[] WATERS = {Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.WATER_CAULDRON};

    private ThinningTint() {
    }

    public static void register(RegisterColorHandlersEvent.BlockTintSources event) {
        for (Block block : PLANTS) {
            wrap(event, block, false);
        }
        for (Block block : WATERS) {
            wrap(event, block, true);
        }
    }

    private static void wrap(RegisterColorHandlersEvent.BlockTintSources event, Block block, boolean water) {
        List<BlockTintSource> sources = event.getBlockColors().getTintSources(block.defaultBlockState());
        if (sources.isEmpty()) {
            return;
        }
        event.register(sources.stream().map(source -> (BlockTintSource) new Faded(source, water)).toList(), block);
    }

    /** Fades {@code color} by {@code amount} (0-1): loses its saturation, then leans to ash (or murk for water). */
    public static int fade(int color, float amount, boolean water) {
        if (amount <= 0) {
            return color;
        }
        int r = ARGB.red(color);
        int g = ARGB.green(color);
        int b = ARGB.blue(color);
        float grey = r * 0.3F + g * 0.59F + b * 0.11F;
        int target = water ? MURK : ASH;
        float lose = amount * (water ? 0.4F : 0.75F);
        float lean = amount * (water ? 0.55F : 0.5F);
        float nr = Mth.lerp(lean, Mth.lerp(lose, r, grey), ARGB.red(target));
        float ng = Mth.lerp(lean, Mth.lerp(lose, g, grey), ARGB.green(target));
        float nb = Mth.lerp(lean, Mth.lerp(lose, b, grey), ARGB.blue(target));
        return ARGB.color(ARGB.alpha(color), Math.round(nr), Math.round(ng), Math.round(nb));
    }

    private record Faded(BlockTintSource inner, boolean water) implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return inner.color(state);
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            int color = inner.colorInWorld(state, level, pos);
            return fade(color, ClientThinning.tint(ClientThinning.at(pos.getX() + 0.5, pos.getZ() + 0.5)), water);
        }

        @Override
        public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            int color = inner.colorAsTerrainParticle(state, level, pos);
            return fade(color, ClientThinning.tint(ClientThinning.at(pos.getX() + 0.5, pos.getZ() + 0.5)), water);
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return inner.relevantProperties();
        }
    }
}
