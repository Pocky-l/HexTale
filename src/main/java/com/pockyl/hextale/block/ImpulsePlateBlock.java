package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.ImpulseRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * The Impulse Plate: whatever steps on it is thrown. Unaimed it throws up and along its arrow; aimed (right click it
 * with an empty hand, then right click the block it should throw to - see {@link ImpulseAim}) it is a faith plate: it
 * solves the throw for the very thing on it ({@link ImpulseArc}) so it lands on the target, where the landing is soft.
 * The throw is applied where the entity's movement is simulated (a player's own client), like vanilla's slime and
 * bubble columns; the server plays the launch, flips the panel for everyone and remembers who is in the air. Takes
 * runes (Shift, Motor, Breath).
 */
public class ImpulsePlateBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = com.pockyl.hextale.port.PortShapes.column(16.0, 0.0, 2.0);
    private static final VoxelShape TRIGGER = com.pockyl.hextale.port.PortShapes.column(16.0, 0.0, 4.0);
    /** Ticks before the same entity can be thrown again (it is still on the plate for a tick or two). */
    private static final int REST = 10;
    /** Ticks a Breath landing stays soft after the throw. */
    private static final int SOFT_TICKS = 200;
    /** The block event that flips the panel. */
    private static final int LAUNCH_EVENT = 1;
    private static final Map<Entity, Long> THROWN = new WeakHashMap<>();
    /** Entities thrown with Breath or at a target, by id, until the game time their soft landing ends. */
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

    /** Only feet right on the plate set it off. */
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return TRIGGER;
    }

    /** An empty hand starts choosing the target; on a plate already being aimed it clears the target instead. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ImpulseAim.toggle(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    /**
     * The plate is thin, so vanilla's {@code stepOn} goes to the block under it; whatever stands on it is inside its
     * cell instead.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!com.pockyl.hextale.port.PortShapes.inside(getEntityInsideCollisionShape(state, level, pos, entity), pos, entity)) {
            return;
        }
        launch(level, pos, state, entity);
    }

    /** The highest a target may lie above or below the plate. */
    public static final int REACH_Y = 24;

    /** Throws {@code entity} at the target, or up and along the arrow (once per {@link #REST} ticks). */
    public void launch(Level level, BlockPos pos, BlockState state, Entity entity) {
        long now = level.getGameTime();
        synchronized (THROWN) {
            Long last = THROWN.get(entity);
            if (last != null && now - last < REST) {
                return;
            }
            THROWN.put(entity, now);
        }
        Plate plate = level.getBlockEntity(pos) instanceof Plate found ? found : null;
        PickaxeRunes runes = plate != null ? plate.runes : PickaxeRunes.EMPTY;
        int banishing = runes.strength(ImpulseRune.BANISHING.rune());
        if (banishing > 0 && entity instanceof net.minecraft.world.entity.monster.Enemy) {
            banish(level, pos, state, entity, banishing);
            return;
        }
        BlockPos target = plate != null ? plate.target : null;
        Vec3 landing = target != null ? ImpulseArc.landing(level, target) : null;
        int ticks = 0;
        if (landing != null) {
            ticks = ImpulseArc.ticks(landing.distanceTo(entity.position()), runes.strength(ImpulseRune.MOTOR.rune()));
            ImpulseFlights.start(level, entity, landing, now + ticks);
        }
        if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(entity)) {
            if (landing != null) {
                entity.setDeltaMovement(ImpulseArc.velocity(entity, entity.position(), landing, ticks));
            } else {
                Direction facing = state.getValue(FACING);
                double forward = ImpulseRune.forward(runes.strength(ImpulseRune.MOTOR.rune()));
                entity.setDeltaMovement(new Vec3(facing.getStepX() * forward, ImpulseRune.up(runes.strength(ImpulseRune.SHIFT.rune())),
                        facing.getStepZ() * forward));
            }
            entity.resetFallDistance();
            entity.hurtMarked = true;
        }
        if (level instanceof ServerLevel server) {
            if (landing != null || runes.strength(ImpulseRune.BREATH.rune()) > 0) {
                SOFT.put(entity.getUUID(), now + (landing != null ? ticks + 60 : SOFT_TICKS));
            }
            int haste = runes.strength(ImpulseRune.HASTE.rune());
            if (haste > 0 && entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                        com.pockyl.hextale.runework.HolderRune.at(ImpulseRune.HASTE_TICKS, haste), haste >= 3 ? 1 : 0, true, false, true));
                server.sendParticles(ModParticles.glint(ImpulseRune.HASTE.rune().color()), entity.getX(), entity.getY() + 0.5, entity.getZ(), 6,
                        0.25, 0.4, 0.25, 0.05);
            }
            server.blockEvent(pos, this, LAUNCH_EVENT, 0);
            server.playSound(null, pos, ModSounds.IMPULSE_LAUNCH.get(), SoundSource.BLOCKS, 0.8F, 0.9F + server.getRandom().nextFloat() * 0.2F);
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5, 0, 0.9, 1, 0, 1);
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.1);
            if (landing != null) {
                // The arc lights up for a moment along the way the throw goes.
                List<Vec3> path = ImpulseArc.path(entity.position(), landing, ticks);
                for (int i = 2; i < path.size(); i += 2) {
                    Vec3 at = path.get(i);
                    server.sendParticles(ModParticles.glint(ModParticles.HEXTECH), at.x, at.y + 0.3, at.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
        }
    }

    /**
     * Banishing: a hostile creature is flung back against the arrow and high into the air - no target, no soft landing -
     * with a flash of the rune's light.
     */
    private void banish(Level level, BlockPos pos, BlockState state, Entity entity, int strength) {
        Direction back = state.getValue(FACING).getOpposite();
        if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(entity)) {
            entity.setDeltaMovement(new Vec3(back.getStepX() * ImpulseRune.BANISH_BACK, ImpulseRune.BANISH_UP[Math.min(strength, 4) - 1],
                    back.getStepZ() * ImpulseRune.BANISH_BACK));
            entity.hurtMarked = true;
        }
        if (level instanceof ServerLevel server) {
            server.blockEvent(pos, this, LAUNCH_EVENT, 0);
            server.playSound(null, pos, ModSounds.IMPULSE_LAUNCH.get(), SoundSource.BLOCKS, 0.9F, 0.7F + server.getRandom().nextFloat() * 0.1F);
            com.pockyl.hextale.runework.ToolEffects.puff(server, Vec3.atCenterOf(pos).add(0, -0.3, 0), ImpulseRune.BANISHING.rune(), 10, 0.4);
            // Hostiles flung back.
            com.pockyl.hextale.network.RuneEffectPayload.show(server, com.pockyl.hextale.network.RuneEffectPayload.Family.GUST, Vec3.atCenterOf(pos).add(0, -0.3, 0), 0xE8F4FF, 1.0F);
        }
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == LAUNCH_EVENT && level.getBlockEntity(pos) instanceof Plate plate) {
            plate.launchedAt = level.getGameTime();
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    /** The plate's runes, its target and (on the clients) when it last fired. */
    public static class Plate extends BlockEntity {
        private PickaxeRunes runes = PickaxeRunes.EMPTY;
        @Nullable
        private BlockPos target;
        /** Client: the game time the panel last flipped, for its animation. */
        public long launchedAt = -100;
        /** Client: the game time the target last changed (the arc shows for a while after). */
        public long aimedAt = -1000;

        public Plate(BlockPos pos, BlockState state) {
            super(ModBlockEntities.IMPULSE_PLATE.get(), pos, state);
        }

        public PickaxeRunes runes() {
            return runes;
        }

        @Nullable
        public BlockPos target() {
            return target;
        }

        /** How far (sideways) a target may lie: Shift makes it farther. */
        public int range() {
            return ImpulseRune.range(runes.strength(ImpulseRune.SHIFT.rune()));
        }

        /** Whether {@code at} is a target this plate can throw to. */
        public boolean inRange(BlockPos at) {
            double dx = at.getX() - worldPosition.getX();
            double dz = at.getZ() - worldPosition.getZ();
            return !at.equals(worldPosition) && Math.sqrt(dx * dx + dz * dz) <= range() && Math.abs(at.getY() - worldPosition.getY()) <= REACH_Y;
        }

        public void setTarget(@Nullable BlockPos value) {
            target = value;
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected void applyImplicitComponents(DataComponentInput components) {
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
        public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
            super.removeComponentsFromTag(output.tag());
            output.discard("runes");
        }

        @Override
        protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
            super.loadAdditional(input.tag(), input.registries());
            runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
            BlockPos before = target;
            target = input.read("target", BlockPos.CODEC).orElse(null);
            if (level != null && level.isClientSide() && !java.util.Objects.equals(before, target)) {
                aimedAt = level.getGameTime();
            }
        }

        @Override
        protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
            super.saveAdditional(output.tag(), output.registries());
            if (!runes.isEmpty()) {
                output.store("runes", PickaxeRunes.CODEC, runes);
            }
            if (target != null) {
                output.store("target", BlockPos.CODEC, target);
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

    /** Text the aiming shows (plain UI). */
    public static Component message(String key, Object... args) {
        return Component.translatable("block.hextale.impulse_plate." + key, args);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<ImpulsePlateBlock> codec() {
        return simpleCodec(ImpulsePlateBlock::new);
    }
}
