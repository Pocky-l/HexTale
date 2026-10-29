package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.farm.ReachIndex;
import com.pockyl.hextale.network.CondensePayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.transport.ExperienceOutlet;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * An Experience Condenser's state. It never looks for orbs: an orb about to appear within {@value #RADIUS} blocks asks
 * the {@link #INDEX} for the nearest condenser ({@link com.pockyl.hextale.farm.CondenserEvents}), which drinks it before
 * it exists, and a Greedy Funnel in reach pours its deaths' experience straight in. The experience waits in the glass as
 * a glowing liquid ({@value #CAPACITY} points) until a player takes it, a level at a time or all at once, or bottles it.
 */
public class XpCondenserBlockEntity extends BlockEntity implements ExperienceOutlet.Store {
    /** Blocks around it in which no orb appears. */
    public static final int RADIUS = 8;
    /** The loaded condensers, asked by every orb and every funnel. */
    public static final ReachIndex<XpCondenserBlockEntity> INDEX = new ReachIndex<>(XpCondenserBlockEntity.class, RADIUS,
            XpCondenserBlockEntity::covers);
    /** Points it holds (about level 50). */
    public static final int CAPACITY = 5345;
    /** Points one Bottle o' Enchanting costs (a bottle gives back 3-11, never more). */
    public static final int BOTTLE_COST = 11;
    /** Client: ticks a drawn-in stream is shown for. */
    public static final int STREAM_TICKS = 22;

    private int points;
    private long sentAt = -1;
    /** Liquid experience for other mods' pipes and tanks, when a mod adds such a liquid. */
    private final ExperienceOutlet outlet = new ExperienceOutlet(this);
    /** Client: streams of experience being drawn in now. */
    public final List<Shown> shown = new ArrayList<>();
    /** Client: the liquid's height eased towards the fill (0-1). */
    public float liquid;
    public float liquidBefore;

    public XpCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.XP_CONDENSER.get(), pos, state);
    }

    /** Client: experience flowing in from {@code from}, started at tick {@code start}. */
    public record Shown(Vec3 from, int points, long start) {
    }

    public int points() {
        return points;
    }

    @Override
    public int capacity() {
        return CAPACITY;
    }

    /** Pipes of other mods draw the experience out as liquid (null: no mod adds liquid experience). */
    public @Nullable ExperienceOutlet outlet() {
        return ExperienceOutlet.fluid() == null ? null : outlet;
    }

    @Override
    public void drainPoints(int taken) {
        int drained = Math.min(taken, points);
        if (drained > 0) {
            points -= drained;
            changed();
        }
    }

    public float fill() {
        return points / (float) CAPACITY;
    }

    /** Comparator signal: 0 empty, 15 full. */
    public int signal() {
        return points <= 0 ? 0 : 1 + Mth.floor(fill() * 14);
    }

    /** Where the experience goes in: the crystal in the brass cup on the lid. */
    public static Vec3 intake(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 16.5 / 16, 0.5);
    }

    public boolean covers(Vec3 at) {
        Vec3 center = Vec3.atCenterOf(worldPosition);
        return Math.abs(at.x - center.x) <= RADIUS + 0.5 && Math.abs(at.y - center.y) <= RADIUS + 0.5
                && Math.abs(at.z - center.z) <= RADIUS + 0.5;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    /** Takes in as much as it has room for; returns how much it took. The clients see it flow in from {@code from}. */
    public int fill(ServerLevel level, int amount, Vec3 from) {
        int taken = Math.min(amount, CAPACITY - points);
        if (taken <= 0) {
            return 0;
        }
        points += taken;
        long now = level.getGameTime();
        // One stream a tick is plenty to see; a burst of orbs in one tick shows as one.
        if (sentAt != now) {
            sentAt = now;
            CondensePayload payload = new CondensePayload(worldPosition, from, taken);
            for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(worldPosition), false)) {
                ModNetwork.sendToPlayer(player, payload);
            }
        }
        changed();
        return taken;
    }

    /** Gives a player experience: enough for their next level, or everything with {@code all}; returns the points given. */
    public int give(ServerPlayer player, boolean all) {
        int wanted = all ? points : Math.max(1, player.getXpNeededForNextLevel() - Mth.floor(player.experienceProgress * player.getXpNeededForNextLevel()));
        int given = Math.min(wanted, points);
        if (given > 0) {
            points -= given;
            player.giveExperiencePoints(given);
            Vec3 at = intake(worldPosition);
            player.level().playSound(null, at.x, at.y, at.z, ModSounds.FUNNEL_RELEASE.get(), SoundSource.BLOCKS, 0.5F, all ? 0.85F : 1.1F);
            changed();
        }
        return given;
    }

    /** Spends one bottle's worth; false when it holds too little. */
    public boolean bottle() {
        if (points < BOTTLE_COST) {
            return false;
        }
        points -= BOTTLE_COST;
        changed();
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            INDEX.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            INDEX.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    /** Broken: the liquid spills out as orbs. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && points > 0) {
            // Removed from the index first, or the orbs would flow straight back in.
            INDEX.remove(server, pos);
            ExperienceOrb.award(server, Vec3.atCenterOf(pos), points);
            points = 0;
        }
    }

    // ---- Client ---------------------------------------------------------------------------------------------------------

    public void show(CondensePayload payload, long now) {
        shown.add(new Shown(payload.from(), payload.points(), now));
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, XpCondenserBlockEntity condenser) {
        long now = level.getGameTime();
        condenser.shown.removeIf(stream -> now - stream.start() > STREAM_TICKS);
        condenser.liquidBefore = condenser.liquid;
        condenser.liquid += (condenser.fill() - condenser.liquid) * 0.1F;
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        points = input.getIntOr("points", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("points", points);
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
