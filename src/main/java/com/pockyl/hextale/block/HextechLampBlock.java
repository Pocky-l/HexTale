package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.Thinning;

/**
 * A hextech lamp: a refined gemstone floating in a brass cage, standing on a stone foot or hanging from the block above
 * (like a lantern). It shines at full light and now and then sheds a cold mote.
 *
 * <p>On land the Thinning has made sick (from {@link Thinning.Stage#MIASMA}) the Void eats at its light: now and then the
 * gem chokes and the lamp {@link #FLICKER}s dim for a moment, a few times in a row on blighted land.
 */
public class HextechLampBlock extends LanternBlock {

    public static final BooleanProperty FLICKER = BooleanProperty.create("flicker");
    /** Light while it chokes. */
    public static final int DIM = 4;
    private static final int VOID_DARK = 0x2C0A26;

    public HextechLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FLICKER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FLICKER);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** On sick land the gem may choke. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Thinning.Stage stage = Thinning.stage(level, pos);
        if (stage.ordinal() < Thinning.Stage.MIASMA.ordinal() || state.getValue(FLICKER)) {
            return;
        }
        float chance = stage == Thinning.Stage.MIASMA ? 0.5F : 0.85F;
        if (random.nextFloat() < chance) {
            choke(state, level, pos, random);
        }
    }

    private static void choke(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, state.setValue(FLICKER, true), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), 2 + random.nextInt(5));
        level.playSound(null, pos, ModSounds.LAMP_FLICKER.get(), SoundSource.BLOCKS, 0.5F, 0.85F + random.nextFloat() * 0.3F);
    }

    /** The light comes back - and on blighted land it may choke again a moment later. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(FLICKER)) {
            level.setBlock(pos, state.setValue(FLICKER, false), Block.UPDATE_ALL);
            Thinning.Stage stage = Thinning.stage(level, pos);
            float again = stage == Thinning.Stage.MIASMA ? 0.25F : stage.ordinal() > Thinning.Stage.MIASMA.ordinal() ? 0.6F : 0;
            if (random.nextFloat() < again) {
                level.scheduleTick(pos, state.getBlock(), 3 + random.nextInt(8));
            }
        } else if (Thinning.stage(level, pos).ordinal() >= Thinning.Stage.MIASMA.ordinal()) {
            choke(state, level, pos, random);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FLICKER)) {
            // The dark is drawn into the choking gem.
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            for (int i = 0; i < 2; i++) {
                level.addParticle(ModParticles.voidMote(VOID_DARK), cx, cy, cz, (random.nextDouble() - 0.5) * 1.6,
                        (random.nextDouble() - 0.5) * 1.2, (random.nextDouble() - 0.5) * 1.6);
            }
            return;
        }
        if (random.nextInt(5) != 0) {
            return;
        }
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3;
        level.addParticle(ModParticles.glint(ModParticles.HEXTECH), x, pos.getY() + 0.5, z, 0, 0.01, 0);
    }
}
