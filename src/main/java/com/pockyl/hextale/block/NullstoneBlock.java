package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.ArrayList;
import java.util.List;

/**
 * Nullstone: a stone that drinks stray magic. Near a Hexcore at work it soaks up the matrix's anger (each stone that is not
 * full takes {@link #CALM_PER_STONE} of it, up to {@link #MAX_STONES} stones), and with it fills up: {@link #SATURATION}
 * 0-3, its violet veins glowing brighter. A full stone stops drinking and lets the magic out again slowly (random ticks).
 *
 * <p>On sick land it drinks the Thinning too: now and then ({@link #randomTick}) a stone that is not full draws
 * {@link #DRINK} out of its chunk - the dark of the ground around rises into it - and fills on the way. A few stones where
 * the work is done keep a workshop's land well; they cannot heal a whole countryside.
 */
public class NullstoneBlock extends Block {
    public static final IntegerProperty SATURATION = IntegerProperty.create("saturation", 0, 3);
    public static final int FULL = 3;
    public static final int RADIUS = 6;
    public static final int MAX_STONES = 4;
    public static final float CALM_PER_STONE = 0.12F;
    /** Ritual ticks of drinking before a stone fills one step. */
    public static final int FILL_TICKS = 300;
    private static final int VIOLET = 0x9A6CF0;
    /** Thinning one drink takes out of the chunk. */
    public static final float DRINK = 3;
    /** Chance that a drink fills the stone a step. */
    public static final float FILL_PER_DRINK = 1 / 3F;
    /** Players this close note it in their diary. */
    private static final double NOTE_RADIUS = 16;

    public NullstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SATURATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SATURATION);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** A stone that is not full drinks the sick land; what it drank seeps back out, a step at a time. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int saturation = state.getValue(SATURATION);
        if (saturation < FULL && drink(level, pos, random)) {
            if (random.nextFloat() < FILL_PER_DRINK) {
                fill(level, pos);
            }
            return;
        }
        if (saturation > 0 && random.nextInt(4) == 0) {
            level.setBlock(pos, state.setValue(SATURATION, state.getValue(SATURATION) - 1), Block.UPDATE_ALL);
            Vec3 at = Vec3.atCenterOf(pos);
            level.sendParticles(ModParticles.glint(VIOLET), at.x, at.y + 0.5, at.z, 6, 0.3, 0.2, 0.3, 0.01);
        }
    }

    /** Draws {@link #DRINK} out of the chunk if it is sick: the dark of the ground around rises into the stone. */
    private static boolean drink(ServerLevel level, BlockPos pos, RandomSource random) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (com.pockyl.hextale.thevoid.Thinning.drain(level, chunk, DRINK) <= 0) {
            return false;
        }
        Vec3 at = Vec3.atCenterOf(pos);
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double reach = 1.5 + random.nextDouble() * 2;
            double x = at.x + Math.cos(angle) * reach;
            double z = at.z + Math.sin(angle) * reach;
            double y = at.y - 0.4 + random.nextDouble() * 0.6;
            // A void mote is sent where it ends, with the way back to where it starts.
            level.sendParticles(ModParticles.voidMote(i % 4 == 0 ? VIOLET : 0x24162E), at.x, at.y, at.z, 0, x - at.x, y - at.y, z - at.z, 1);
        }
        level.playSound(null, pos, ModSounds.NULLSTONE_DRINK.get(), SoundSource.BLOCKS, 0.5F, 0.9F + random.nextFloat() * 0.2F);
        Quests.fireNear(level, at, NOTE_RADIUS, Quests.NULL_WARD_DRANK);
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int saturation = state.getValue(SATURATION);
        if (saturation > 0 && random.nextInt(8 - saturation * 2) == 0) {
            level.addParticle(ModParticles.glint(VIOLET), pos.getX() + random.nextDouble(), pos.getY() + 1.02,
                    pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
    }

    /** Stones around {@code centre} that can still drink, the nearest first, at most {@link #MAX_STONES}. */
    public static List<BlockPos> thirsty(Level level, BlockPos centre) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-RADIUS, -2, -RADIUS), centre.offset(RADIUS, 2, RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.NULLSTONE.get()) && state.getValue(SATURATION) < FULL) {
                found.add(pos.immutable());
            }
        }
        found.sort((a, b) -> Double.compare(a.distSqr(centre), b.distSqr(centre)));
        return found.size() > MAX_STONES ? found.subList(0, MAX_STONES) : found;
    }

    /** How much of the matrix's anger is left with this many stones drinking (1 = none taken). */
    public static float calm(int stones) {
        return 1 - Math.min(stones, MAX_STONES) * CALM_PER_STONE;
    }

    /** One step fuller; a stone that fills up rings out once. */
    public static void fill(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.NULLSTONE.get()) || state.getValue(SATURATION) >= FULL) {
            return;
        }
        int next = state.getValue(SATURATION) + 1;
        level.setBlock(pos, state.setValue(SATURATION, next), Block.UPDATE_ALL);
        if (next == FULL) {
            Vec3 at = Vec3.atCenterOf(pos);
            level.sendParticles(ModParticles.ring(VIOLET), at.x, at.y, at.z, 0, 1.6, 0, 0, 1);
            level.playSound(null, pos, ModSounds.CRYSTAL_RESONATE.get(), SoundSource.BLOCKS, 0.8F, 0.7F);
        }
    }
}
