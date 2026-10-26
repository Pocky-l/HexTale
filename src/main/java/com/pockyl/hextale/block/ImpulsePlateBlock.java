package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.ImpulseRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * The Impulse Plate: whatever steps on it is thrown up and along its arrow. The throw is applied where the entity's
 * movement is simulated (a player's own client), like vanilla's slime and bubble columns; the server plays the whoosh
 * and, with Breath, remembers who was thrown so their landing does not hurt. Takes runes (Shift, Motor, Breath, Lens).
 */
public class ImpulsePlateBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.0);
    /** Ticks before the same entity can be thrown again (it is still on the plate for a tick or two). */
    private static final int REST = 10;
    /** Ticks a Breath landing stays soft after the throw. */
    private static final int SOFT_TICKS = 200;
    private static final Map<Entity, Long> THROWN = new WeakHashMap<>();
    /** Entities thrown with Breath, by id, until the game time their soft landing ends. */
    public static final Map<UUID, Long> SOFT = new ConcurrentHashMap<>();

    public ImpulsePlateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Plate(pos, state);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        long now = level.getGameTime();
        synchronized (THROWN) {
            Long last = THROWN.get(entity);
            if (last != null && now - last < REST) {
                return;
            }
            THROWN.put(entity, now);
        }
        PickaxeRunes runes = level.getBlockEntity(pos) instanceof Plate plate ? plate.runes : PickaxeRunes.EMPTY;
        if (entity.canSimulateMovement()) {
            Direction facing = state.getValue(FACING);
            double forward = ImpulseRune.forward(runes.strength(ImpulseRune.MOTOR.rune()));
            entity.setDeltaMovement(new Vec3(facing.getStepX() * forward, ImpulseRune.up(runes.strength(ImpulseRune.SHIFT.rune())),
                    facing.getStepZ() * forward));
            entity.resetFallDistance();
            entity.needsSync = true;
        }
        if (level instanceof ServerLevel server) {
            if (runes.strength(ImpulseRune.BREATH.rune()) > 0) {
                SOFT.put(entity.getUUID(), now + SOFT_TICKS);
            }
            server.playSound(null, pos, ModSounds.SKYPACK_IGNITE.get(), SoundSource.BLOCKS, 0.6F, 1.4F);
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 0, 0.9, 1, 0, 1);
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.1);
        }
        super.stepOn(level, pos, state, entity);
    }

    /** The plate's runes. */
    public static class Plate extends BlockEntity {
        private PickaxeRunes runes = PickaxeRunes.EMPTY;

        public Plate(BlockPos pos, BlockState state) {
            super(ModBlockEntities.IMPULSE_PLATE.get(), pos, state);
        }

        @Override
        protected void applyImplicitComponents(DataComponentGetter components) {
            super.applyImplicitComponents(components);
            runes = components.getOrDefault(ModDataComponents.IMPULSE_RUNES.get(), PickaxeRunes.EMPTY);
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
            super.collectImplicitComponents(components);
            if (!runes.isEmpty()) {
                components.set(ModDataComponents.IMPULSE_RUNES.get(), runes);
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
