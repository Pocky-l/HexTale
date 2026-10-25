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
 * Where the edge of the world wears thin, the colour goes out of the land: grass, leaves and vines lose their green and
 * lean to an ashen lilac, water darkens to murk, stone, sand and earth turn a sick lilac grey. Plants and water wrap the
 * tints the game already gives them; the ground blocks get a tint of their own (their models are made tintable by the
 * mod's copies of them in {@code assets/minecraft/models/block}).
 */
public final class ThinningTint {
    private static final Block[] PLANTS = {
            Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.BUSH,
            Blocks.POTTED_FERN, Blocks.PINK_PETALS, Blocks.WILDFLOWERS, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
            Blocks.VINE, Blocks.SUGAR_CANE, Blocks.LEAF_LITTER};
    private static final Block[] WATERS = {Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.WATER_CAULDRON};
    /** Keep in step with the tintable model copies in {@code assets/minecraft/models/block}. */
    private static final Block[] GROUND = {
            Blocks.STONE, Blocks.COBBLESTONE, Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRANITE, Blocks.SAND, Blocks.RED_SAND,
            Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.GRAVEL, Blocks.DIRT, Blocks.COARSE_DIRT};

    /** How a kind of block fades: how much colour it loses, how far it leans to which shade. */
    public enum Kind {
        PLANT(0.9F, 0.62F, 0x6E5F80),
        WATER(0.65F, 0.8F, 0x33263F),
        GROUND(0.5F, 0.5F, 0x8C7C9E);

        final float lose;
        final float lean;
        final int target;

        Kind(float lose, float lean, int target) {
            this.lose = lose;
            this.lean = lean;
            this.target = target;
        }
    }

    private ThinningTint() {
    }

    public static void register(RegisterColorHandlersEvent.BlockTintSources event) {
        for (Block block : PLANTS) {
            wrap(event, block, Kind.PLANT);
        }
        for (Block block : WATERS) {
            wrap(event, block, Kind.WATER);
        }
        for (Block block : GROUND) {
            event.register(List.of(new Faded(state -> -1, Kind.GROUND)), block);
        }
    }

    private static void wrap(RegisterColorHandlersEvent.BlockTintSources event, Block block, Kind kind) {
        List<BlockTintSource> sources = event.getBlockColors().getTintSources(block.defaultBlockState());
        if (sources.isEmpty()) {
            return;
        }
        event.register(sources.stream().map(source -> (BlockTintSource) new Faded(source, kind)).toList(), block);
    }

    /** Fades {@code color} by {@code amount} (0-1): loses its saturation, then leans to the kind's shade. */
    public static int fade(int color, float amount, Kind kind) {
        if (amount <= 0) {
            return color;
        }
        int r = ARGB.red(color);
        int g = ARGB.green(color);
        int b = ARGB.blue(color);
        float grey = r * 0.3F + g * 0.59F + b * 0.11F;
        float lose = amount * kind.lose;
        float lean = amount * kind.lean;
        float nr = Mth.lerp(lean, Mth.lerp(lose, r, grey), ARGB.red(kind.target));
        float ng = Mth.lerp(lean, Mth.lerp(lose, g, grey), ARGB.green(kind.target));
        float nb = Mth.lerp(lean, Mth.lerp(lose, b, grey), ARGB.blue(kind.target));
        return ARGB.color(ARGB.alpha(color), Math.round(nr), Math.round(ng), Math.round(nb));
    }

    private static int faded(int color, BlockPos pos, Kind kind) {
        return fade(color, ClientThinning.tint(ClientThinning.at(pos.getX() + 0.5, pos.getZ() + 0.5)), kind);
    }

    private record Faded(BlockTintSource inner, Kind kind) implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return inner.color(state);
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return faded(inner.colorInWorld(state, level, pos), pos, kind);
        }

        @Override
        public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
            return faded(inner.colorAsTerrainParticle(state, level, pos), pos, kind);
        }

        @Override
        public Set<Property<?>> relevantProperties() {
            return inner.relevantProperties();
        }
    }
}
