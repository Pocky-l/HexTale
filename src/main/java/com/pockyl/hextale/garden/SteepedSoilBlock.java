package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.util.TriState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModTags;

/**
 * Steeped Soil: farmland a brew was poured on. It never dries out and is never trampled, crops grow on it as on wet
 * farmland, and it holds up to {@value #MAX_CHARGES} doses of one kind ({@link Dose}): a plant on it that grows a stage
 * may spend one and grow another. Poured over again with the same dose it fills up; another dose takes the old one's
 * place. Its colour tells the dose; dry of doses it stays dark, rich soil.
 */
public class SteepedSoilBlock extends Block {
    public static final EnumProperty<Dose> DOSE = EnumProperty.create("dose", Dose.class);
    public static final IntegerProperty CHARGES = IntegerProperty.create("charges", 0, 8);
    public static final int MAX_CHARGES = 8;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public SteepedSoilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DOSE, Dose.NONE).setValue(CHARGES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DOSE, CHARGES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** Crops, stems and the garden's own plants take root in it as in farmland. */
    @Override
    public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
        // 1.21.1: stems take no support tags, so the soil says itself that a Sap Gourd may grow on it.
        return facing == Direction.UP && (grows(plant) || plant.is(com.pockyl.hextale.registry.ModBlocks.SAP_GOURD.get())) ? TriState.TRUE : TriState.DEFAULT;
    }

    /** What a garden bed grows: anything that grows on farmland, and the plants of the garden. */
    public static boolean grows(BlockState plant) {
        return plant.getBlock() instanceof CropBlock || plant.getBlock() instanceof StemBlock || plant.getBlock() instanceof AttachedStemBlock
                || plant.is(ModTags.GARDEN_PLANTS);
    }

    /** Always wet: crops on it grow as fast as on watered farmland. */
    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** The dose shows: now and then a wet glint of its colour on the surface. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Dose dose = state.getValue(DOSE);
        if (dose != Dose.NONE && state.getValue(CHARGES) > 0 && random.nextInt(6) == 0) {
            level.addParticle(ModParticles.glint(dose.color()), pos.getX() + 0.15 + random.nextDouble() * 0.7, pos.getY() + 15.2 / 16,
                    pos.getZ() + 0.15 + random.nextDouble() * 0.7, 0, 0.01, 0);
        }
    }

    /** Whether a dose may be poured here: farmland or steeped soil. */
    public static boolean takesDose(BlockState state) {
        return state.is(Blocks.FARMLAND) || state.getBlock() instanceof SteepedSoilBlock;
    }

    /**
     * Pours {@code charges} doses on farmland or steeped soil: farmland turns into steeped soil, the same dose fills up
     * to {@value #MAX_CHARGES}, another dose takes the old one's place.
     *
     * @return whether it took them
     */
    public static boolean steep(ServerLevel level, BlockPos pos, Dose dose, int charges) {
        BlockState state = level.getBlockState(pos);
        if (!takesDose(state) || dose == Dose.NONE || charges <= 0) {
            return false;
        }
        int now = state.getBlock() instanceof SteepedSoilBlock && state.getValue(DOSE) == dose ? state.getValue(CHARGES) : 0;
        if (now >= MAX_CHARGES) {
            return false;
        }
        BlockState steeped = ModBlocks.STEEPED_SOIL.get().defaultBlockState().setValue(DOSE, dose).setValue(CHARGES, Math.min(MAX_CHARGES, now + charges));
        level.setBlock(pos, steeped, Block.UPDATE_ALL);
        level.sendParticles(ModParticles.drop(dose.color()), pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 14, 0.3, 0.02, 0.3, 0.04);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 0.8F);
        return true;
    }

    /** How many doses of this kind still fit here (farmland and another dose: all of them). */
    public static int room(BlockState state, Dose dose) {
        if (!takesDose(state)) {
            return 0;
        }
        return state.getBlock() instanceof SteepedSoilBlock && state.getValue(DOSE) == dose ? MAX_CHARGES - state.getValue(CHARGES) : MAX_CHARGES;
    }

    /** Spends one dose of the soil (the plant on it grew a stage more). */
    public static void spend(ServerLevel level, BlockPos pos, BlockState state) {
        int left = state.getValue(CHARGES) - 1;
        level.setBlock(pos, left <= 0 ? state.setValue(CHARGES, 0).setValue(DOSE, Dose.NONE) : state.setValue(CHARGES, left), Block.UPDATE_CLIENTS);
    }
}
