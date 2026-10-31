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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.network.ArcPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.SpireRune;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A Hex Arc Spire's state and work. Every interval it asks the level once for the hostile creatures in its reach (one box
 * query, never a scan per tick) and strikes the nearest with an arc of hextech charge - more of them with Chain
 * Lightning. The arc counts as a player's blow, so the creature drops what only a player's kill gives, experience too
 * (a Greedy Funnel around takes it all in). Named creatures and bosses are spared.
 *
 * <p>Every arc costs spark essence: one mB drawn out of a jar within {@link JarReach#RADIUS} gives {@value #ARCS_PER_MB}
 * arcs. Without spark it stands {@link #uncharged()}; when the funnel that would swallow its kills is full it waits
 * ({@link #waiting()}) - the creatures stay, the mob cap fills and the world stops spawning more. Nothing to strike =
 * it looks more and more rarely (up to {@value #MAX_SLEEP} ticks).
 */
public class ArcSpireBlockEntity extends BlockEntity {
    public static final int ARCS_PER_MB = 4;
    public static final int DRAW_MB = 4;
    /** Damage of one arc (two arcs fell a zombie or a skeleton). */
    public static final float DAMAGE = 12;
    /** Reach below and above the spire's base block. */
    public static final int DOWN = 4;
    public static final int UP = 3;
    /** Height of the crystal over the base block: over the needle (30 px) and the crown's prongs (31 px). */
    public static final double CRYSTAL_Y = 2 + 2.0 / 16;
    public static final int MAX_SLEEP = 100;
    public static final int STREAM_TICKS = 50;
    /** Ticks an arc is drawn for. */
    public static final int ARC_TICKS = 8;
    /** Ticks between two hisses of the Beam (every strike shows its beam; the sound is thinned out). */
    private static final int BEAM_SOUND_TICKS = 60;

    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private int charges;
    private boolean uncharged;
    private boolean waiting;
    private int wait;
    private int idle;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;
    private long beamHeardAt = -1000;
    /** Client: arcs being drawn now. */
    public final List<Shown> shown = new ArrayList<>();
    /** Client: when the last arc left (the coils glow after it). */
    public long struckAt = -1000;

    public ArcSpireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_SPIRE.get(), pos, state);
    }

    /** Client: an arc from the crystal to {@code to}, from tick {@code start}. */
    public record Shown(Vec3 to, long start, int seed) {
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    private int strength(SpireRune rune) {
        return runes.strength(rune.rune());
    }

    public int radius() {
        return SpireRune.radius(strength(SpireRune.SHIFT));
    }

    public int interval() {
        return SpireRune.interval(strength(SpireRune.MOTOR));
    }

    /** Creatures one arc strikes. */
    public int targets() {
        int chain = strength(SpireRune.CHAIN_LIGHTNING);
        return 1 + (chain > 0 ? HolderRune.at(SpireRune.FORKS, chain) : 0);
    }

    // ---- State ----------------------------------------------------------------------------------------------------------

    public int charges() {
        return charges;
    }

    /** Whether it had something to strike and no spark to strike with. */
    public boolean uncharged() {
        return uncharged;
    }

    /** Whether it holds back because the funnel that would swallow its kills is full. */
    public boolean waiting() {
        return waiting;
    }

    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** Where the arcs leave from: the crystal held over the spire's tip (one block lower while it stands without its top). */
    public Vec3 crystal() {
        return Vec3.atLowerCornerOf(worldPosition).add(0.5, tall() ? CRYSTAL_Y : CRYSTAL_Y - 1, 0.5);
    }

    /** Whether the upper half stands on it. */
    public boolean tall() {
        if (level == null) {
            return true;
        }
        BlockState above = level.getBlockState(worldPosition.above());
        return above.getBlock() instanceof ArcSpireBlock && ArcSpireBlock.upper(above);
    }

    public void wake() {
        wait = Math.min(wait, 2);
        idle = 0;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Work -----------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ArcSpireBlockEntity spire) {
        if (--spire.wait > 0) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        spire.growTop(server, state);
        int interval = spire.interval();
        List<Mob> targets = spire.findTargets(server);
        if (targets.isEmpty()) {
            spire.idle = Math.min(spire.idle + 1, 4);
            spire.wait = Math.min(MAX_SLEEP, interval << spire.idle);
            spire.setState(false, false);
            return;
        }
        spire.idle = 0;
        GreedyFunnelBlockEntity funnel = GreedyFunnelBlockEntity.INDEX.covering(server, targets.getFirst().position());
        if (funnel != null && funnel.full()) {
            spire.setState(false, true);
            spire.wait = 40;
            return;
        }
        if (spire.charges <= 0 && !spire.drawSpark(server)) {
            spire.setState(true, false);
            spire.wait = 40;
            return;
        }
        spire.setState(false, false);
        spire.strike(server, targets);
        spire.wait = interval;
    }

    /**
     * A spire without its upper half (one built before spires stood two blocks tall, or one whose top was blown off)
     * grows it again as soon as the space above is free.
     */
    private void growTop(ServerLevel level, BlockState state) {
        if (!tall() && ArcSpireBlock.roomAbove(level, worldPosition)) {
            level.setBlock(worldPosition.above(), state.setValue(ArcSpireBlock.HALF,
                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    /** The hostile creatures in reach, nearest first, as many as one arc strikes. */
    List<Mob> findTargets(ServerLevel level) {
        int radius = radius();
        AABB box = new AABB(worldPosition).inflate(radius, 0, radius).expandTowards(0, -DOWN, 0).expandTowards(0, UP, 0);
        Vec3 from = crystal();
        List<Mob> found = level.getEntitiesOfClass(Mob.class, box, ArcSpireBlockEntity::prey);
        found.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(from)));
        return found.size() > targets() ? found.subList(0, targets()) : found;
    }

    /** Hostile, alive, not named and no boss. */
    public static boolean prey(Mob mob) {
        return mob instanceof Enemy && mob.isAlive() && !mob.hasCustomName() && !mob.getType().is(Tags.EntityTypes.BOSSES);
    }

    /** One arc: every target takes the blow of a player standing at the spire. */
    private void strike(ServerLevel level, List<Mob> targets) {
        int dam = strength(SpireRune.DAM);
        if (dam == 0 || level.getRandom().nextFloat() >= HolderRune.at(SpireRune.THRIFT, dam)) {
            charges--;
        }
        Vec3 from = crystal();
        FakePlayer striker = FakePlayerFactory.getMinecraft(level);
        striker.setPos(from.x, from.y - 1, from.z);
        int flash = strength(SpireRune.FLASH);
        int beam = strength(SpireRune.BEAM);
        List<Vec3> points = new ArrayList<>();
        for (Mob mob : targets) {
            float damage = DAMAGE;
            if (flash > 0 && mob.getHealth() >= mob.getMaxHealth()) {
                damage *= 1 + HolderRune.at(SpireRune.BRIGHT, flash);
            }
            Vec3 at = mob.position().add(0, mob.getBbHeight() * 0.6, 0);
            points.add(at);
            if (beam > 0) {
                // Set alight before the blow, so a creature it fells dies burning (and drops its meat cooked).
                mob.igniteForTicks(Math.max(mob.getRemainingFireTicks(), HolderRune.at(SpireRune.FIRE_TICKS, beam)));
                WeaponFxPayload.send(level, WeaponFxPayload.Kind.BEAM, from, at, SpireRune.BEAM.rune().color(), 1);
            }
            mob.hurt(level.damageSources().playerAttack(striker), damage);
            level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 10, 0.2, 0.3, 0.2, 0.1);
        }
        if (beam > 0 && !targets.isEmpty() && level.getGameTime() - beamHeardAt >= BEAM_SOUND_TICKS) {
            beamHeardAt = level.getGameTime();
            Vec3 first = points.getFirst();
            level.playSound(null, first.x, first.y, first.z, ModSounds.RUNE_KINDLE.get(), SoundSource.BLOCKS, 0.25F,
                    1.2F + level.getRandom().nextFloat() * 0.2F);
        }
        level.playSound(null, from.x, from.y, from.z, ModSounds.ARC_STRIKE.get(), SoundSource.BLOCKS, 0.6F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        ArcPayload payload = new ArcPayload(worldPosition, points);
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
            ModNetwork.sendToPlayer(player, payload);
        }
        changed();
    }

    private boolean drawSpark(ServerLevel level) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            int taken = Math.min(DRAW_MB, jar.tank().contents().get(Essence.SPARK));
            if (taken <= 0) {
                continue;
            }
            jar.tank().remove(EssenceMix.pure(Essence.SPARK, taken));
            jar.setChanged();
            charges += taken * ARCS_PER_MB;
            drewFrom = jar.getBlockPos();
            drewAt = level.getGameTime();
            Vec3 mouth = JarReach.mouth(drewFrom);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 1.3F);
            changed();
            return true;
        }
        return false;
    }

    private void setState(boolean uncharged, boolean waiting) {
        if (this.uncharged != uncharged || this.waiting != waiting) {
            this.uncharged = uncharged;
            this.waiting = waiting;
            changed();
        }
    }

    // ---- Client ---------------------------------------------------------------------------------------------------------

    public void show(ArcPayload payload, long now) {
        for (Vec3 to : payload.targets()) {
            shown.add(new Shown(to, now, (int) (now * 131 + to.hashCode())));
        }
        struckAt = now;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ArcSpireBlockEntity spire) {
        long now = level.getGameTime();
        spire.shown.removeIf(arc -> now - arc.start() > ARC_TICKS);
        if (!spire.uncharged && level.getRandom().nextInt(spire.waiting ? 40 : 14) == 0) {
            // Now and then a spark creeps up the coils.
            Vec3 at = spire.crystal();
            level.addParticle(ModParticles.spark(ModParticles.HEXTECH), at.x + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    at.y - 0.5 - level.getRandom().nextDouble() * 0.4, at.z + (level.getRandom().nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
        }
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.SPIRE_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.SPIRE_RUNES.get(), runes);
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
        charges = input.getIntOr("charges", 0);
        uncharged = input.getBooleanOr("uncharged", false);
        waiting = input.getBooleanOr("waiting", false);
        drewFrom = input.read("drew_from", BlockPos.CODEC).orElse(null);
        drewAt = input.getLongOr("drew_at", -1000);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        output.putInt("charges", charges);
        output.putBoolean("uncharged", uncharged);
        output.putBoolean("waiting", waiting);
        if (drewFrom != null) {
            output.store("drew_from", BlockPos.CODEC, drewFrom);
            output.putLong("drew_at", drewAt);
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
