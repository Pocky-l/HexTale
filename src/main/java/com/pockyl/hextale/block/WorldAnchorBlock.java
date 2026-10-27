package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.AnchorWorldRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import org.jspecify.annotations.Nullable;

/**
 * The World Anchor: it holds its chunk (with Shift the chunks around it too) loaded and ticking while it stands. The
 * chunks are held by a NeoForge ticket owned by the anchor's position, kept with the world; on world load tickets whose
 * anchor is gone are dropped. Takes runes (Shift, Haste, Fertility, Lens).
 */
public class WorldAnchorBlock extends BaseEntityBlock {
    public static final TicketController TICKETS = new TicketController(Hextale.id("world_anchor"), (level, helper) ->
            helper.getBlockTickets().keySet().stream().filter(pos -> !level.getBlockState(pos).is(ModBlocks.WORLD_ANCHOR.get())).toList()
                    .forEach(helper::removeAllTickets));
    /** Ticks between two Fertility pinches. */
    private static final int GROWTH_TICKS = 100;

    /** A deepslate socket with the anchor's crown, flukes and shank standing in it (nothing above 12 px). */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            net.minecraft.world.level.block.Block.box(0, 0, 0, 16, 4, 16), net.minecraft.world.level.block.Block.box(3, 4, 3, 13, 12, 13));

    public WorldAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                                                 net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Anchor(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || type != ModBlockEntities.WORLD_ANCHOR.get() ? null
                : (BlockEntityTicker<T>) (BlockEntityTicker<Anchor>) Anchor::serverTick;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        hold(level, pos, 2, false);
    }

    /** Holds (or lets go of) the chunks within {@code radius} chunks of the anchor at {@code pos}. */
    public static void hold(ServerLevel level, BlockPos pos, int radius, boolean add) {
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                TICKETS.forceChunk(level, pos, cx + dx, cz + dz, add, true);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ModParticles.glint(ModParticles.HEX_LIGHT), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.2, pos.getY() + 1.1,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
        }
    }

    /** The anchor's runes; it (re)takes its hold whenever it loads. */
    public static class Anchor extends BlockEntity {
        private PickaxeRunes runes = PickaxeRunes.EMPTY;

        public Anchor(BlockPos pos, BlockState state) {
            super(ModBlockEntities.WORLD_ANCHOR.get(), pos, state);
        }

        public int strength(AnchorWorldRune rune) {
            return runes.strength(rune.rune());
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level instanceof ServerLevel server) {
                hold(server, worldPosition, AnchorWorldRune.radius(strength(AnchorWorldRune.SHIFT)), true);
            }
        }

        static void serverTick(Level level, BlockPos pos, BlockState state, Anchor anchor) {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            int haste = anchor.strength(AnchorWorldRune.HASTE);
            int fertility = anchor.strength(AnchorWorldRune.FERTILITY);
            if (haste <= 0 && (fertility <= 0 || level.getGameTime() % GROWTH_TICKS != 0)) {
                return;
            }
            int radius = AnchorWorldRune.radius(anchor.strength(AnchorWorldRune.SHIFT));
            int cx = SectionPos.blockToSectionCoord(pos.getX());
            int cz = SectionPos.blockToSectionCoord(pos.getZ());
            RandomSource random = level.getRandom();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!server.hasChunk(cx + dx, cz + dz)) {
                        continue;
                    }
                    LevelChunk chunk = server.getChunk(cx + dx, cz + dz);
                    if (haste > 0) {
                        quicken(server, chunk, AnchorWorldRune.EXTRA_TICKS[Math.min(haste, 4) - 1], random);
                    }
                    if (fertility > 0 && level.getGameTime() % GROWTH_TICKS == 0) {
                        grow(server, chunk, AnchorWorldRune.GROWTHS[Math.min(fertility, 4) - 1], random);
                    }
                }
            }
        }

        /** Extra random ticks in every section of a held chunk that has something that ticks. */
        private static void quicken(ServerLevel level, LevelChunk chunk, int ticks, RandomSource random) {
            LevelChunkSection[] sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                if (!section.isRandomlyTickingBlocks()) {
                    continue;
                }
                int baseY = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(i));
                for (int t = 0; t < ticks; t++) {
                    int x = random.nextInt(16);
                    int y = random.nextInt(16);
                    int z = random.nextInt(16);
                    BlockState state = section.getBlockState(x, y, z);
                    if (state.isRandomlyTicking()) {
                        state.randomTick(level, chunk.getPos().getBlockAt(x, baseY + y, z), random);
                    }
                }
            }
        }

        /** A few tries at a growing plant somewhere in the chunk's surface; a hit gets a pinch of growth. */
        private static void grow(ServerLevel level, LevelChunk chunk, int plants, RandomSource random) {
            for (int tries = 0; tries < plants * 8 && plants > 0; tries++) {
                int x = chunk.getPos().getMinBlockX() + random.nextInt(16);
                int z = chunk.getPos().getMinBlockZ() + random.nextInt(16);
                BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, new BlockPos(x, 0, z)).below();
                for (BlockPos at : new BlockPos[] {top, top.above()}) {
                    BlockState state = level.getBlockState(at);
                    if (state.getBlock() instanceof BonemealableBlock plant && !(state.getBlock() instanceof net.minecraft.world.level.block.GrassBlock)
                            && plant.isValidBonemealTarget(level, at, state, BonemealSource.INTERACTION) && plant.isBonemealSuccess(level, random, at, state, BonemealSource.INTERACTION)) {
                        plant.performBonemeal(level, random, at, state, BonemealSource.INTERACTION);
                        level.sendParticles(ModParticles.glint(0xB8F08A), at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.01);
                        plants--;
                        break;
                    }
                }
            }
        }

        @Override
        protected void applyImplicitComponents(DataComponentGetter components) {
            super.applyImplicitComponents(components);
            runes = components.getOrDefault(ModDataComponents.WORLD_ANCHOR_RUNES.get(), PickaxeRunes.EMPTY);
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
            super.collectImplicitComponents(components);
            if (!runes.isEmpty()) {
                components.set(ModDataComponents.WORLD_ANCHOR_RUNES.get(), runes);
            }
        }

        @Override
        public void removeComponentsFromTag(ValueOutput output) {
            super.removeComponentsFromTag(output);
            output.discard("runes");
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (!runes.isEmpty()) {
                output.store("runes", PickaxeRunes.CODEC, runes);
            }
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }

        @Override
        public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
