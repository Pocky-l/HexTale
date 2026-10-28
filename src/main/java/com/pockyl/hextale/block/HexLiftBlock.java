package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.LiftRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Lift: standing on one, a jump carries a player to the next lift straight above (within its reach), a sneak to
 * the next one below - the client notices the key and asks ({@link com.pockyl.hextale.network.HexLiftPayload}), the
 * server checks and moves. A target lift needs two blocks of room above it. Takes runes (Shift, Blink, Haste, Lens).
 */
public class HexLiftBlock extends BaseEntityBlock {
    /** Ticks a player rests between two rides (a held key does not ride the whole shaft at once). */
    private static final int REST = 8;
    private static final Map<UUID, Long> LAST_RIDE = new ConcurrentHashMap<>();
    /** Where one stands on the lift: the top of its brass pad. */
    public static final double PAD = 6 / 16.0;
    /**
     * The low plinth and the pad in its middle. The model's four guide horns at the corners are left out on purpose
     * (owner): they caught the player and the crosshair, the plate alone is easier to step on and aim at.
     */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0, 0, 0, 16, 5, 16), Block.box(3, 5, 3, 13, 6, 13));

    public HexLiftBlock(Properties properties) {
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
        return new Lift(pos, state);
    }

    /** The lift right under a player, or null. */
    @Nullable
    public static BlockPos under(Player player) {
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        return player.level().getBlockState(below).is(ModBlocks.HEX_LIFT.get()) ? below : null;
    }

    /** The next lift from {@code from} going up or down within {@code reach} blocks with room above it, or null. */
    @Nullable
    public static BlockPos next(BlockGetter level, BlockPos from, boolean up, int reach) {
        for (int d = 1; d <= reach; d++) {
            BlockPos at = from.above(up ? d : -d);
            if (level.getBlockState(at).is(ModBlocks.HEX_LIFT.get()) && room(level, at.above()) && room(level, at.above(2))) {
                return at;
            }
        }
        return null;
    }

    private static boolean room(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** A ride asked for by a player: checked here, then the player (and with Blink their company) is moved. */
    public static boolean ride(ServerPlayer player, boolean up) {
        return ride(player, under(player), up);
    }

    /**
     * A ride from the lift at {@code from}, named by the client: the player must stand on it or hang just above it (a
     * jump lifts them off before the key is seen).
     */
    public static boolean ride(ServerPlayer player, @Nullable BlockPos from, boolean up) {
        ServerLevel level = player.serverLevel();
        if (from != null && (!level.getBlockState(from).is(ModBlocks.HEX_LIFT.get())
                || Math.abs(player.getX() - (from.getX() + 0.5)) > 1.2 || Math.abs(player.getZ() - (from.getZ() + 0.5)) > 1.2
                || player.getY() < from.getY() + 0.2 || player.getY() > from.getY() + 3)) {
            from = null;
        }
        long now = level.getGameTime();
        Long last = LAST_RIDE.get(player.getUUID());
        if (from == null || last != null && now - last < REST) {
            return false;
        }
        PickaxeRunes runes = level.getBlockEntity(from) instanceof Lift lift ? lift.runes : PickaxeRunes.EMPTY;
        BlockPos to = next(level, from, up, LiftRune.reach(runes.strength(LiftRune.SHIFT.rune())));
        if (to == null) {
            return false;
        }
        LAST_RIDE.put(player.getUUID(), now);
        double dy = to.getY() - from.getY();
        if (runes.strength(LiftRune.BLINK.rune()) > 0) {
            List<LivingEntity> company = level.getEntitiesOfClass(LivingEntity.class, new AABB(from.getX(), from.getY() + 0.2, from.getZ(), from.getX() + 1, from.getY() + 2.4, from.getZ() + 1).inflate(0.1, 0, 0.1),
                    entity -> entity != player && !(entity instanceof Player));
            for (LivingEntity creature : company) {
                creature.teleportTo(creature.getX(), creature.getY() + dy, creature.getZ());
            }
        }
        player.teleportTo(player.getX(), to.getY() + PAD, player.getZ());
        player.resetFallDistance();
        int haste = runes.strength(LiftRune.HASTE.rune());
        if (haste > 0) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                    LiftRune.SPEED_TICKS[Math.min(haste, 4) - 1], 0, true, false, true));
        }
        level.playSound(null, to, ModSounds.PYLON_ARRIVE.get(), SoundSource.BLOCKS, 0.7F, up ? 1.3F : 0.9F);
        for (BlockPos at : new BlockPos[] {from, to}) {
            level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.getX() + 0.5, at.getY() + PAD + 0.05, at.getZ() + 0.5, 0, 0.8, 1, 0, 1);
            level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.getX() + 0.5, at.getY() + PAD + 0.4, at.getZ() + 0.5, 10, 0.25, 0.5, 0.25, 0.02);
        }
        return true;
    }

    /** The lift's runes. */
    public static class Lift extends BlockEntity {
        private PickaxeRunes runes = PickaxeRunes.EMPTY;

        public Lift(BlockPos pos, BlockState state) {
            super(ModBlockEntities.HEX_LIFT.get(), pos, state);
        }

        @Override
        protected void applyImplicitComponents(DataComponentInput components) {
            super.applyImplicitComponents(components);
            runes = components.getOrDefault(ModDataComponents.LIFT_RUNES.get(), PickaxeRunes.EMPTY);
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
            super.collectImplicitComponents(components);
            if (!runes.isEmpty()) {
                components.set(ModDataComponents.LIFT_RUNES.get(), runes);
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
        }

        @Override
        protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
            super.saveAdditional(output.tag(), output.registries());
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

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<HexLiftBlock> codec() {
        return simpleCodec(HexLiftBlock::new);
    }
}
