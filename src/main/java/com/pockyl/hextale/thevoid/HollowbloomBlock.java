package com.pockyl.hextale.thevoid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.pockyl.hextale.port.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModTags;

/**
 * Hollowbloom: a dead bush woken with hextech dust. What the dust wakes in it is a hunger for the Void's breath - the
 * thinner the world around it ({@link Thinning}), the more alive it is: dry twigs where the land is clean, then a
 * darkening stem, unfurling leaves, a flower whose heart is a hollow drawing the world in, at last a full glowing bloom.
 * A researcher plants it at home and watches it. It reads the chunk it stands in every {@value #CHECK_TICKS} ticks and
 * grows or withers one {@link #VIGOR} step at a time towards what it finds; it starts to stir well before the land
 * itself shows anything. A comparator beside it reads the Thinning finer, 0-15.
 */
public class HollowbloomBlock extends VegetationBlock {
    /** 0 withered, 1 stirring, 2 leafing, 3 blooming, 4 radiant. */
    public static final IntegerProperty VIGOR = IntegerProperty.create("vigor", 0, 4);
    /** The Thinning each vigor wakes at: it stirs long before the land is felt, blooms with the haze, glows with the blight. */
    public static final float[] WAKES_AT = {0, 150, 350, Thinning.Stage.HAZE.from, Thinning.Stage.BLIGHT.from};
    public static final int[] LIGHT = {0, 0, 2, 5, 8};
    public static final int CHECK_TICKS = 100;
    /**
     * Where the flower's heart sits at each vigor (blocks), for the motes it draws in: the drooping dead bud off to one
     * side, then the closed bud, then the black hollow of the bloom over the middle.
     */
    private static final double[] HEART = {0.38, 0.63, 0.70, 0.66, 0.72};
    private static final double[] HEART_X = {11 / 16.0, 0.5, 0.5, 0.5, 0.5};
    private static final VoxelShape[] SHAPES = {
            com.pockyl.hextale.port.PortShapes.column(10.0, 0.0, 10.0), com.pockyl.hextale.port.PortShapes.column(10.0, 0.0, 12.0), com.pockyl.hextale.port.PortShapes.column(12.0, 0.0, 13.0),
            com.pockyl.hextale.port.PortShapes.column(12.0, 0.0, 14.0), com.pockyl.hextale.port.PortShapes.column(14.0, 0.0, 15.0)};
    private static final int DRY = 0x7A6A58;
    private static final int MOTE = 0x2A1C34;
    private static final int GLINT = 0xD0469E;

    public HollowbloomBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(VIGOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VIGOR);
    }

    /** The vigor the Thinning {@code value} calls for. */
    public static int vigorFor(float value) {
        for (int vigor = WAKES_AT.length - 1; vigor > 0; vigor--) {
            if (value >= WAKES_AT[vigor]) {
                return vigor;
            }
        }
        return 0;
    }

    /** It takes root wherever a dead bush would, and on the land the Void has touched. */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON) || state.is(ModTags.VOID_GROUND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(VIGOR)];
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, 20 + level.getRandom().nextInt(CHECK_TICKS));
        }
    }

    /** One look at the land: a step towards what it finds, then the next look. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        sense(state, level, pos);
        level.scheduleTick(pos, this, CHECK_TICKS);
    }

    /** Grows or withers one step towards the vigor the Thinning here calls for; returns the new state. */
    public BlockState sense(BlockState state, ServerLevel level, BlockPos pos) {
        int vigor = state.getValue(VIGOR);
        int wanted = vigorFor(Thinning.get(level, pos));
        level.updateNeighbourForOutputSignal(pos, this);
        if (wanted == vigor) {
            return state;
        }
        int next = vigor + Integer.signum(wanted - vigor);
        BlockState changed = state.setValue(VIGOR, next);
        level.setBlock(pos, changed, Block.UPDATE_ALL);
        double x = pos.getX() + HEART_X[next];
        double z = pos.getZ() + 0.5;
        double heart = pos.getY() + HEART[next];
        if (next > vigor) {
            // Waking: the stalk creaks as it fills, dark breath gathers into the heart.
            level.playSound(null, pos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 0.45F, 0.6F + next * 0.08F);
            level.sendParticles(ModParticles.voidMote(MOTE), x, heart, z, 6 + next * 3, 0.5, 0.35, 0.5, 0);
            if (next >= 3) {
                level.sendParticles(ModParticles.glint(GLINT), x, heart, z, 4 + next, 0.2, 0.15, 0.2, 0.02);
            }
        } else {
            // Withering: it sags and sheds dry flakes.
            level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.4F, 0.7F);
            level.sendParticles(ModParticles.drop(DRY), x, pos.getY() + 0.4, z, 5, 0.2, 0.15, 0.2, 0);
        }
        return changed;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** The Thinning here, 0 for clean land up to 15 for the most it can be. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return signal(Thinning.get(level, pos));
    }

    public static int signal(float value) {
        return value <= 0 ? 0 : Mth.clamp(Mth.ceil(value / Thinning.MAX * 15), 1, 15);
    }

    /**
     * Nothing comes out of a withered bloom but now and then a dry flake; a stirring one breathes dark wisps; a living
     * one draws motes of the world into its hollow heart, and in full bloom glints flicker in it.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int vigor = state.getValue(VIGOR);
        double x = pos.getX() + HEART_X[vigor];
        double z = pos.getZ() + 0.5;
        double heart = pos.getY() + HEART[vigor];
        switch (vigor) {
            case 0 -> {
                if (random.nextInt(40) == 0) {
                    level.addParticle(ModParticles.drop(DRY), x - 0.2 + random.nextDouble() * 0.4, pos.getY() + 0.4, z - 0.2 + random.nextDouble() * 0.4, 0, 0, 0);
                }
            }
            case 1, 2 -> {
                if (random.nextInt(vigor == 1 ? 14 : 7) == 0) {
                    level.addParticle(ModParticles.wisp(MOTE), x - 0.15 + random.nextDouble() * 0.3, heart - 0.1, z - 0.15 + random.nextDouble() * 0.3, 0, 0.01, 0);
                }
                if (vigor == 2 && random.nextInt(16) == 0) {
                    drawIn(level, random, x, heart, z, 0.9);
                }
            }
            default -> {
                int motes = vigor == 4 ? 2 : 1;
                for (int i = 0; i < motes; i++) {
                    if (random.nextInt(vigor == 4 ? 2 : 4) == 0) {
                        drawIn(level, random, x, heart, z, vigor == 4 ? 2.2 : 1.4);
                    }
                }
                if (random.nextInt(vigor == 4 ? 4 : 9) == 0) {
                    level.addParticle(ModParticles.glint(GLINT), x - 0.12 + random.nextDouble() * 0.24, heart + random.nextDouble() * 0.1,
                            z - 0.12 + random.nextDouble() * 0.24, 0, 0.004, 0);
                }
            }
        }
    }

    /** A mote of the world, spawned AT the heart with the offset to where it starts from. */
    private static void drawIn(Level level, RandomSource random, double x, double heart, double z, double reach) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = reach * (0.5 + random.nextDouble() * 0.5);
        level.addParticle(ModParticles.voidMote(MOTE), x, heart, z, Math.cos(angle) * distance, -0.2 + random.nextDouble() * 0.6,
                Math.sin(angle) * distance);
    }
}
