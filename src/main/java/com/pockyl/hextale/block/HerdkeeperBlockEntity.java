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
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.Herding;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HerdkeeperRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.transport.PylonInsert;
import com.pockyl.hextale.world.SafeCodecs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A Herdkeeper's state and work. Every interval it tends the nearest rested animal of its pasture: the animal's own loot is
 * rolled ({@link Herding}) for {@value #MB_PER_TEND} mB of life essence from the jars within {@link JarReach#RADIUS}, the bell
 * sounds, a stream of light runs from the animal to the trough ({@value #TEND_TICKS} ticks), and the produce arrives: into a
 * container beside it, else it pops out of the trough. The animal then rests; an animal that gives nothing costs nothing.
 * More animals keep it busier, up to one per interval. With {@value #TROUGH_LIMIT} items lying round it stops by itself.
 */
public class HerdkeeperBlockEntity extends BlockEntity {
    public static final int MB_PER_TEND = 2;
    public static final int TEND_TICKS = 24;
    public static final int DOWN = 3;
    public static final int UP = 3;
    public static final int MAX_SLEEP = 200;
    public static final int TROUGH_LIMIT = 64;
    /** Ticks without a tended animal before the bell comes to rest. */
    private static final int ACTIVE_TICKS = 40;
    /** Containers take produce on these sides: the cell above is the crook's. */
    private static final List<Direction> OUTLETS = List.of(Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private boolean hungry;
    private boolean full;
    private int wait;
    private int idle;
    private long activeUntil;
    /** Produce on its way along the stream, due at {@link #arrives}. */
    private final List<ItemStack> produce = new ArrayList<>();
    private long arrives;
    /** Where the last tended animal stood, for the stream the clients draw. */
    private @Nullable Vec3 tended;
    private long tendedAt = -1000;

    public HerdkeeperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HERDKEEPER.get(), pos, state);
    }

    // ---- Runes and state ------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }

    private int strength(HerdkeeperRune rune) {
        return runes.strength(rune.rune());
    }

    public int radius() {
        return HerdkeeperRune.radius(strength(HerdkeeperRune.SHIFT));
    }

    public int interval() {
        return HerdkeeperRune.interval(strength(HerdkeeperRune.MOTOR));
    }

    public int restTicks() {
        return HerdkeeperRune.rest(strength(HerdkeeperRune.SPRING));
    }

    public boolean hungry() {
        return hungry;
    }

    public boolean full() {
        return full;
    }

    /** Client: the animal the stream comes from, while it runs. */
    public @Nullable Vec3 streamFrom() {
        return level != null && tended != null && level.getGameTime() - tendedAt <= TEND_TICKS ? tended : null;
    }

    public long tendedAt() {
        return tendedAt;
    }

    /** Where the stream ends and produce leaves: over the hay in the trough. */
    public static Vec3 trough(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 12.0 / 16, 0.5);
    }

    /** Where life essence goes in: the bell's lip. */
    public static Vec3 bell(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(7.0 / 16, 1.0, 0.5);
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Work -----------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, HerdkeeperBlockEntity keeper) {
        ServerLevel server = (ServerLevel) level;
        keeper.deliver(server);
        keeper.settleBell(server, state);
        if (--keeper.wait > 0) {
            return;
        }
        int interval = keeper.interval();
        if (!keeper.produce.isEmpty()) {
            keeper.wait = 2;
            return;
        }
        if (keeper.troughHeaped(server)) {
            keeper.setStopped(false, true);
            keeper.wait = 40;
            return;
        }
        if (keeper.tendNext(server)) {
            keeper.idle = 0;
            keeper.wait = interval;
        } else if (keeper.hungry) {
            keeper.wait = 40;
        } else {
            keeper.idle = Math.min(keeper.idle + 1, 3);
            keeper.wait = Math.min(MAX_SLEEP, interval << keeper.idle);
        }
    }

    /** Tends the nearest rested animal that has something to give; false when none does or the essence is out. */
    private boolean tendNext(ServerLevel level) {
        long now = level.getGameTime();
        AABB pasture = new AABB(worldPosition).inflate(radius(), 0, radius()).expandTowards(0, UP, 0).expandTowards(0, -DOWN, 0);
        List<Mob> herd = new ArrayList<>(level.getEntitiesOfClass(Mob.class, pasture, mob -> Herding.tendable(mob) && !Herding.resting(mob, now)));
        herd.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(Vec3.atCenterOf(worldPosition))));
        for (Mob animal : herd) {
            List<ItemStack> gathered = Herding.gather(level, animal);
            if (gathered.isEmpty()) {
                // Nothing to give (a wolf, a goat): looked at again after its rest, at no cost.
                Herding.rest(animal, now + restTicks());
                continue;
            }
            if (!pay(level)) {
                setStopped(true, false);
                return false;
            }
            int pollen = strength(HerdkeeperRune.POLLEN);
            Vec3 at = animal.position().add(0, animal.getBbHeight() * 0.6, 0);
            if (pollen > 0 && level.getRandom().nextFloat() < HolderRune.at(HerdkeeperRune.MORE, pollen)) {
                gathered.addAll(Herding.gather(level, animal));
                ToolEffects.puff(level, at, HerdkeeperRune.POLLEN.rune(), 10, 0.4);
            }
            Herding.rest(animal, now + restTicks());
            start(level, at, gathered);
            return true;
        }
        setStopped(false, false);
        return false;
    }

    private boolean pay(ServerLevel level) {
        int dam = strength(HerdkeeperRune.DAM);
        if (dam > 0 && level.getRandom().nextFloat() < HolderRune.at(HerdkeeperRune.THRIFT, dam)) {
            ToolEffects.puff(level, bell(worldPosition), HerdkeeperRune.DAM.rune(), 8, 0.3);
            return true;
        }
        return JarReach.pay(level, worldPosition, bell(worldPosition), Essence.LIFE, MB_PER_TEND);
    }

    private void start(ServerLevel level, Vec3 at, List<ItemStack> gathered) {
        long now = level.getGameTime();
        produce.addAll(gathered);
        arrives = now + TEND_TICKS;
        tended = at;
        tendedAt = now;
        activeUntil = now + interval() + TEND_TICKS + ACTIVE_TICKS;
        hungry = false;
        full = false;
        BlockState state = getBlockState();
        if (!state.getValue(HerdkeeperBlock.ACTIVE)) {
            level.setBlock(worldPosition, state.setValue(HerdkeeperBlock.ACTIVE, true), Block.UPDATE_CLIENTS);
        }
        level.sendParticles(ModParticles.glint(Essence.LIFE.color()), at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.02);
        Vec3 bell = bell(worldPosition);
        level.playSound(null, bell.x, bell.y, bell.z, ModSounds.HERDKEEPER_BELL.get(), SoundSource.BLOCKS, 0.5F, 0.92F + level.getRandom().nextFloat() * 0.16F);
        changed();
    }

    /** The stream has reached the trough: the produce goes on. */
    private void deliver(ServerLevel level) {
        if (produce.isEmpty() || level.getGameTime() < arrives) {
            return;
        }
        Vec3 at = trough(worldPosition);
        for (ItemStack stack : produce) {
            ItemStack rest = intoContainers(level, stack);
            if (!rest.isEmpty()) {
                popOut(level, rest);
            }
        }
        produce.clear();
        setChanged();
        level.sendParticles(ModParticles.glint(Essence.LIFE.color()), at.x, at.y, at.z, 5, 0.2, 0.1, 0.2, 0.01);
        level.playSound(null, at.x, at.y, at.z, ModSounds.HERDKEEPER_GATHER.get(), SoundSource.BLOCKS, 0.4F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        int radiance = strength(HerdkeeperRune.RADIANCE);
        if (radiance > 0 && level.getRandom().nextFloat() < HolderRune.at(HerdkeeperRune.EXPERIENCE, radiance)) {
            ExperienceOrb.award(level, at, 1);
            ToolEffects.puff(level, at, HerdkeeperRune.RADIANCE.rune(), 8, 0.3);
        }
    }

    private ItemStack intoContainers(ServerLevel level, ItemStack stack) {
        ItemStack rest = stack;
        for (Direction side : OUTLETS) {
            if (rest.isEmpty()) {
                break;
            }
            int moved = PylonInsert.insert(level, worldPosition.relative(side), side.getOpposite(), ItemResource.of(rest), rest.getCount(), null, null);
            rest = rest.copyWithCount(rest.getCount() - moved);
        }
        return rest;
    }

    /** Out of the trough and over its front board. */
    private void popOut(ServerLevel level, ItemStack stack) {
        Vec3 at = trough(worldPosition);
        Direction front = getBlockState().getValue(HerdkeeperBlock.FACING);
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack, front.getStepX() * 0.12, 0.22, front.getStepZ() * 0.12);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    /** Produce nobody takes away piles up to {@value #TROUGH_LIMIT} items, and then the Herdkeeper waits. */
    private boolean troughHeaped(ServerLevel level) {
        int lying = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition).inflate(2, 1, 2))) {
            lying += item.getItem().getCount();
        }
        return lying >= TROUGH_LIMIT;
    }

    private void settleBell(ServerLevel level, BlockState state) {
        if (state.getValue(HerdkeeperBlock.ACTIVE) && level.getGameTime() >= activeUntil) {
            level.setBlock(worldPosition, state.setValue(HerdkeeperBlock.ACTIVE, false), Block.UPDATE_CLIENTS);
        }
    }

    private void setStopped(boolean hungry, boolean full) {
        if (this.hungry != hungry || this.full != full) {
            this.hungry = hungry;
            this.full = full;
            changed();
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            for (ItemStack stack : produce) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
            produce.clear();
        }
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.HERDKEEPER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.HERDKEEPER_RUNES.get(), runes);
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
        hungry = input.getBooleanOr("hungry", false);
        full = input.getBooleanOr("full", false);
        activeUntil = input.getLongOr("active_until", 0);
        produce.clear();
        input.read("produce", SafeCodecs.lenientList(ItemStack.CODEC)).ifPresent(produce::addAll);
        arrives = input.getLongOr("arrives", 0);
        tended = input.read("tended", Vec3.CODEC).orElse(null);
        tendedAt = input.getLongOr("tended_at", -1000);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        output.putBoolean("hungry", hungry);
        output.putBoolean("full", full);
        output.putLong("active_until", activeUntil);
        if (!produce.isEmpty()) {
            output.store("produce", ItemStack.CODEC.listOf(), List.copyOf(produce));
            output.putLong("arrives", arrives);
        }
        if (tended != null) {
            output.store("tended", Vec3.CODEC, tended);
            output.putLong("tended_at", tendedAt);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove("produce");
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
