package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.network.ArcPayload;
import com.pockyl.hextale.network.ModNetwork;
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
    /** Reach below and above the spire. */
    public static final int DOWN = 4;
    public static final int UP = 2;
    public static final int MAX_SLEEP = 100;
    public static final int STREAM_TICKS = 50;
    /** Ticks an arc is drawn for. */
    public static final int ARC_TICKS = 8;

    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private int charges;
    private boolean uncharged;
    private boolean waiting;
    private int wait;
    private int idle;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;
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

    /** Where the arcs leave from: the crystal held over the spire's tip. */
    public static Vec3 crystal(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 1 + 1.0 / 16, 0.5);
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

    /** The hostile creatures in reach, nearest first, as many as one arc strikes. */
    List<Mob> findTargets(ServerLevel level) {
        int radius = radius();
        AABB box = new AABB(worldPosition).inflate(radius, 0, radius).expandTowards(0, -DOWN, 0).expandTowards(0, UP, 0);
        Vec3 from = crystal(worldPosition);
        List<Mob> found = level.getEntitiesOfClass(Mob.class, box, ArcSpireBlockEntity::prey);
        found.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(from)));
        return found.size() > targets() ? found.subList(0, targets()) : found;
    }

    /** Hostile, alive, not named and no boss. */
    public static boolean prey(Mob mob) {
        return mob instanceof Enemy && mob.isAlive() && !mob.hasCustomName() && !mob.typeHolder().is(Tags.EntityTypes.BOSSES);
    }

    /** One arc: every target takes the blow of a player standing at the spire. */
    private void strike(ServerLevel level, List<Mob> targets) {
        int dam = strength(SpireRune.DAM);
        if (dam == 0 || level.getRandom().nextFloat() >= HolderRune.at(SpireRune.THRIFT, dam)) {
            charges--;
        }
        Vec3 from = crystal(worldPosition);
        FakePlayer striker = FakePlayerFactory.getMinecraft(level);
        striker.setPos(from.x, from.y - 1, from.z);
        int flash = strength(SpireRune.FLASH);
        List<Vec3> points = new ArrayList<>();
        for (Mob mob : targets) {
            float damage = DAMAGE;
            if (flash > 0 && mob.getHealth() >= mob.getMaxHealth()) {
                damage *= 1 + HolderRune.at(SpireRune.BRIGHT, flash);
            }
            Vec3 at = mob.position().add(0, mob.getBbHeight() * 0.6, 0);
            points.add(at);
            mob.hurtServer(level, level.damageSources().playerAttack(striker), damage);
            level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 10, 0.2, 0.3, 0.2, 0.1);
        }
        level.playSound(null, from.x, from.y, from.z, ModSounds.ARC_STRIKE.get(), SoundSource.BLOCKS, 0.6F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        ArcPayload payload = new ArcPayload(worldPosition, points);
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(worldPosition), false)) {
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
            Vec3 at = crystal(pos);
            level.addParticle(ModParticles.spark(ModParticles.HEXTECH), at.x + (level.getRandom().nextDouble() - 0.5) * 0.3,
                    at.y - 0.5 - level.getRandom().nextDouble() * 0.4, at.z + (level.getRandom().nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
        }
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
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
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("runes");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        charges = input.getIntOr("charges", 0);
        uncharged = input.getBooleanOr("uncharged", false);
        waiting = input.getBooleanOr("waiting", false);
        drewFrom = input.read("drew_from", BlockPos.CODEC).orElse(null);
        drewAt = input.getLongOr("drew_at", -1000);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
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
