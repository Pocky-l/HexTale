package com.pockyl.hextale.client;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Where the edge of the world wears thin, the colour goes out of the land: grass, leaves and vines lose their green and
 * lean to an ashen lilac, water darkens to murk, stone, sand and earth turn a sick lilac grey. Plants and water wrap the
 * tints the game already gives them; the ground blocks get a tint of their own (their models are made tintable by the
 * mod's copies of them in {@code assets/minecraft/models/block}).
 *
 * <p>1.21.1: block colours instead of 26.x tint sources; water (block and fluid alike) takes its colour from the water
 * colour resolver, which is wrapped once at start-up. 1.21.1 has no bush, wildflowers or leaf litter.
 */
public final class ThinningTint {
    private static final Block[] PLANTS = {
            Blocks.GRASS_BLOCK, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN,
            Blocks.POTTED_FERN, Blocks.PINK_PETALS, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES,
            Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
            Blocks.VINE, Blocks.SUGAR_CANE};
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

    public static void register(RegisterColorHandlersEvent.Block event) {
        for (Block block : PLANTS) {
            BlockColor inner = event.getBlockColors().blockColors.get(block);
            if (inner != null) {
                event.register((state, level, pos, tint) -> pos == null ? inner.getColor(state, level, pos, tint)
                        : faded(inner.getColor(state, level, pos, tint), pos, Kind.PLANT), block);
            }
        }
        for (Block block : GROUND) {
            event.register((state, level, pos, tint) -> pos == null ? -1 : faded(-1, pos, Kind.GROUND), block);
        }
    }

    /** Fades {@code color} by {@code amount} (0-1): loses its saturation, then leans to the kind's shade. */
    public static int fade(int color, float amount, Kind kind) {
        if (amount <= 0) {
            return color;
        }
        int r = FastColor.ARGB32.red(color);
        int g = FastColor.ARGB32.green(color);
        int b = FastColor.ARGB32.blue(color);
        float grey = r * 0.3F + g * 0.59F + b * 0.11F;
        float lose = amount * kind.lose;
        float lean = amount * kind.lean;
        float nr = Mth.lerp(lean, Mth.lerp(lose, r, grey), FastColor.ARGB32.red(kind.target));
        float ng = Mth.lerp(lean, Mth.lerp(lose, g, grey), FastColor.ARGB32.green(kind.target));
        float nb = Mth.lerp(lean, Mth.lerp(lose, b, grey), FastColor.ARGB32.blue(kind.target));
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(color), Math.round(nr), Math.round(ng), Math.round(nb));
    }

    /**
     * Water (its block, its fluid, cauldrons and bubble columns) takes its colour from the water colour resolver: the
     * resolver is wrapped before the first client level makes its tint cache, so water darkens to murk where the world
     * wears thin.
     */
    public static void fadeWater() {
        ColorResolver water = BiomeColors.WATER_COLOR_RESOLVER;
        BiomeColors.WATER_COLOR_RESOLVER = (biome, x, z) -> fade(water.getColor(biome, x, z),
                ClientThinning.tint(ClientThinning.at(x + 0.5, z + 0.5)), Kind.WATER);
    }

    private static int faded(int color, BlockPos pos, Kind kind) {
        return fade(color, ClientThinning.tint(ClientThinning.at(pos.getX() + 0.5, pos.getZ() + 0.5)), kind);
    }
}
