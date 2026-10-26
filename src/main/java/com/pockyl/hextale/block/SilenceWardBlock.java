package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.SilenceRune;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * The Silence Ward: a wool-wrapped cocoon on a stone foot that swallows sound around it. The hush itself is on the
 * client ({@code client/SilenceHush} turns positioned sounds inside it down); with Insulator the server stops vibrations
 * inside from reaching sculk ({@code event/SilenceEvents}). Takes runes (Shift, Hush, Insulator, Lens).
 */
public class SilenceWardBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(3, 3, 3, 13, 13, 13), Block.box(6, 13, 6, 10, 16, 10));

    public SilenceWardBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Ward(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            level.addParticle(ModParticles.ring(0xB9B2C8), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.2, 0, 0);
        }
    }

    /** The ward's runes; loaded wards of both sides are indexed by dimension for the hush and the insulation. */
    public static class Ward extends BlockEntity {
        private static final Map<String, Set<Ward>> LOADED = new ConcurrentHashMap<>();
        private PickaxeRunes runes = PickaxeRunes.EMPTY;

        public Ward(BlockPos pos, BlockState state) {
            super(ModBlockEntities.SILENCE_WARD.get(), pos, state);
        }

        public int strength(SilenceRune rune) {
            return runes.strength(rune.rune());
        }

        public int radius() {
            return SilenceRune.radius(strength(SilenceRune.SHIFT));
        }

        public boolean covers(Vec3 at) {
            int radius = radius();
            return at.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= (double) radius * radius;
        }

        private static String key(Level level) {
            return (level.isClientSide() ? "c:" : "s:") + level.dimension().identifier();
        }

        /** The loaded wards of {@code level} covering {@code at} with {@code needing} (null = any): the quietest one. */
        public static @Nullable Ward covering(Level level, Vec3 at, @Nullable SilenceRune needing) {
            Ward best = null;
            for (Ward ward : LOADED.getOrDefault(key(level), Set.of())) {
                if (!ward.isRemoved() && ward.covers(at) && (needing == null || ward.strength(needing) > 0)
                        && (best == null || ward.left() < best.left())) {
                    best = ward;
                }
            }
            return best;
        }

        /** The share of a sound left inside. */
        public float left() {
            return SilenceRune.left(strength(SilenceRune.HUSH));
        }

        @Override
        public void onLoad() {
            super.onLoad();
            if (level != null) {
                LOADED.computeIfAbsent(key(level), key -> ConcurrentHashMap.newKeySet()).add(this);
            }
        }

        @Override
        public void setRemoved() {
            super.setRemoved();
            if (level != null) {
                Set<Ward> wards = LOADED.get(key(level));
                if (wards != null) {
                    wards.remove(this);
                }
            }
        }

        @Override
        public void onChunkUnloaded() {
            super.onChunkUnloaded();
            setRemoved();
        }

        @Override
        protected void applyImplicitComponents(DataComponentGetter components) {
            super.applyImplicitComponents(components);
            runes = components.getOrDefault(ModDataComponents.SILENCE_WARD_RUNES.get(), PickaxeRunes.EMPTY);
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
            super.collectImplicitComponents(components);
            if (!runes.isEmpty()) {
                components.set(ModDataComponents.SILENCE_WARD_RUNES.get(), runes);
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
