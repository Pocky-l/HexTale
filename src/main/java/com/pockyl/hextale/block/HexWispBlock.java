package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.PickaxeRune;
import com.pockyl.hextale.runework.PickaxeRunes;

/**
 * A hex wisp: a mote of cold hextech light a pickaxe with the Eternal Flame rune leaves in the dark where it dug. Its
 * look - a flickering core in a soft halo with sparks circling it, clinging under the ceiling - is drawn by
 * {@code HexWispRenderer}; the block entity carries nothing. It lights the tunnel for good. A pickaxe with the Eternal
 * Flame rune passes through it, so digging on never puts it out; anything else puts it out at a touch. Nothing walks into it; fluids take its place.
 *
 * <p>Wisps are also made in the crucible and come in the hextech blue and the sixteen dye colours ({@link #dye()}). Only
 * a wisp a player set down from its item ({@link #PLACED}) drops it again and holds its cell against other blocks; the
 * rune's wisps, and any set some other way, go out for good and give way to whatever is put there.
 */
public class HexWispBlock extends Block implements EntityBlock {
    /** Set down by a player from the item: drops it when put out, other blocks cannot take its place. */
    public static final BooleanProperty PLACED = BooleanProperty.create("placed");
    private static final VoxelShape SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);

    /** The three colours of the flame: outer tongue, middle tongue, core (and the brightest sparks). */
    public record Flame(int deep, int middle, int core) {
        public static final Flame HEXTECH = new Flame(0x3A8CFF, 0x5CC8F2, 0xE8FAFF);

        public static Flame of(DyeColor dye) {
            return switch (dye) {
                case WHITE -> new Flame(0x9AA4B8, 0xD8E0EC, 0xFFFFFF);
                case ORANGE -> new Flame(0xD2561A, 0xF9932B, 0xFFE6B8);
                case MAGENTA -> new Flame(0xA02FA8, 0xDE5BD8, 0xFFD8F8);
                case LIGHT_BLUE -> new Flame(0x2F86C8, 0x6CC6F0, 0xE2F6FF);
                case YELLOW -> new Flame(0xC89A10, 0xF8D838, 0xFFFBD8);
                case LIME -> new Flame(0x58A818, 0x9CE040, 0xF0FFD0);
                case PINK -> new Flame(0xD05A8A, 0xF4A0C0, 0xFFE8F0);
                case GRAY -> new Flame(0x4A5058, 0x7A828C, 0xD0D4DA);
                case LIGHT_GRAY -> new Flame(0x7C8088, 0xB4B8BE, 0xF0F2F4);
                case CYAN -> new Flame(0x168C8C, 0x3CC8C0, 0xD8FFF8);
                case PURPLE -> new Flame(0x6A2AB0, 0x9C5CE0, 0xEAD8FF);
                case BLUE -> new Flame(0x2A3AA8, 0x4C6CF0, 0xD8E0FF);
                case BROWN -> new Flame(0x7A4A22, 0xB07840, 0xF4DCC0);
                case GREEN -> new Flame(0x3A6A14, 0x66A030, 0xE0F4C8);
                case RED -> new Flame(0xA01E1E, 0xE84040, 0xFFD8D0);
                case BLACK -> new Flame(0x1E1A26, 0x3C3448, 0x9C94AC);
            };
        }
    }

    private final @Nullable DyeColor dye;
    private final Flame flame;

    /** The hextech blue wisp, the one the Eternal Flame rune leaves. */
    public HexWispBlock(Properties properties) {
        this(null, properties);
    }

    public HexWispBlock(@Nullable DyeColor dye, Properties properties) {
        super(properties);
        this.dye = dye;
        this.flame = dye == null ? Flame.HEXTECH : Flame.of(dye);
        registerDefaultState(stateDefinition.any().setValue(PLACED, false));
    }

    /** The dye this wisp was coloured with, null for the hextech blue one. */
    public @Nullable DyeColor dye() {
        return dye;
    }

    public Flame flame() {
        return flame;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PLACED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(PLACED, true);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        // A wisp someone set down keeps its cell; a rune's wisp gives way like grass.
        return !state.getValue(PLACED) && super.canBeReplaced(state, context);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // A pickaxe with the Eternal Flame rune looks past its own wisps while it digs on; anything else can aim at it.
        return context instanceof EntityCollisionContext entity && entity.getEntity() instanceof LivingEntity holder
                && PickaxeRunes.of(holder.getMainHandItem()).strength(PickaxeRune.ETERNAL_FLAME) > 0 ? Shapes.empty() : SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Glow(pos, state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Wisps left before they had a block entity get one, and clients see it once the block is set again.
        if (level.getChunkAt(pos).getBlockEntity(pos, net.minecraft.world.level.chunk.LevelChunk.EntityCreationType.CHECK) == null) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Now and then a spark drifts up off the flame; the flame itself is the renderer's.
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.glint(random.nextBoolean() ? flame.middle() : flame.core()), pos.getX() + 0.35 + random.nextDouble() * 0.3,
                    pos.getY() + 0.45 + random.nextDouble() * 0.2, pos.getZ() + 0.35 + random.nextDouble() * 0.3,
                    (random.nextDouble() - 0.5) * 0.006, 0.012, (random.nextDouble() - 0.5) * 0.006);
        }
    }

    /** The wisp's block entity: only there so the renderer draws it. */
    public static class Glow extends BlockEntity {
        /** Client: when the renderer first saw it, for the flame growing in. */
        public float clientBorn = -1;

        public Glow(BlockPos pos, BlockState state) {
            super(ModBlockEntities.HEX_WISP.get(), pos, state);
        }
    }
}
