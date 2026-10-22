package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.network.HexcoreStrikePayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Hexcore: a Hextech Altar whose heart woke up. It runs every ritual the altar runs, from the same circle, and draws a
 * quarter less essence for it; but its matrix is alive and a heavy ritual makes it unstable. The more pedestal items,
 * essence and runes a ritual takes, the higher its {@link #instability()}, and while it runs the matrix throws arcs: a
 * light one only crackles, a hard one strikes - it hurts and sets alight whoever stands close, sets things on fire,
 * shatters fragile blocks, even burns an item off a pedestal before the ritual could draw it. Lightning rods in the
 * circle catch some of the strikes.
 */
public class HexcoreBlockEntity extends HextechAltarBlockEntity {
    /** Height of the matrix centre over the block's floor: resting, and risen during a ritual (blocks). */
    public static final double REST_Y = 1.5;
    public static final double RISEN_Y = 2.35;
    /** Ticks the matrix takes to assemble when the altar becomes a Hexcore. */
    public static final int BIRTH_TICKS = 90;
    /** Share of a recipe's essences a Hexcore really draws. */
    private static final float THRIFT = 0.75F;
    /** Ticks at the start of a ritual before the first strike. */
    private static final int CALM_TICKS = 30;
    private static final int STRIKE_RADIUS = 6;
    private static final int ROD_RADIUS = 8;
    private static final double SEND_RADIUS = 48;

    /** 0..1: how hard the running ritual is on the matrix (synced). */
    private float instability;
    /** Game time this Hexcore was born from an altar (synced, for the assembly animation). */
    private long bornAt = Long.MIN_VALUE;

    // Client side.
    private final HexcoreMatrix matrix = new HexcoreMatrix();
    private final List<Arc> arcs = new ArrayList<>();
    private float rise = (float) REST_Y;
    private float riseTime = Float.NaN;

    public HexcoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEXCORE.get(), pos, state);
    }

    /** Replaces the altar at {@code pos} with a Hexcore that assembles itself out of the ritual's light. */
    public static void awaken(ServerLevel level, BlockPos pos, BlockState altar) {
        BlockState core = ModBlocks.HEXCORE.get().defaultBlockState()
                .setValue(HextechAltarBlock.POWERED, altar.getValue(HextechAltarBlock.POWERED));
        level.setBlock(pos, core, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof HexcoreBlockEntity hexcore)) {
            return;
        }
        hexcore.bornAt = level.getGameTime();
        hexcore.setChanged();
        Vec3 at = Vec3.atBottomCenterOf(pos).add(0, REST_Y, 0);
        level.sendParticles(ModParticles.ring(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 0, 2.6, 0, 0, 1);
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, pos.getY() + 0.05, at.z, 0, 6, 1, 0, 1);
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 60, 1.2, 1.0, 1.2, 0.4);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 50, 0.4, 0.4, 0.4, 0.25);
        level.playSound(null, pos, ModSounds.RITUAL_COMPLETE.get(), SoundSource.BLOCKS, 1.2F, 0.8F);
        level.playSound(null, pos, ModSounds.BLOCK_AWAKEN.get(), SoundSource.BLOCKS, 1.2F, 0.7F);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.5F, 1.5F);
        // The new matrix reaches out to every part of its circle once.
        Ritual.Circle circle = Ritual.Circle.scan(level, pos);
        for (var stele : circle.steles()) {
            hexcore.send(level, Vec3.atCenterOf(stele.getBlockPos()).add(0, 0.6, 0), HexcoreStrikePayload.CALM);
        }
        for (var pedestal : circle.pedestals()) {
            hexcore.send(level, Vec3.atCenterOf(pedestal.getBlockPos()).add(0, 0.6, 0), HexcoreStrikePayload.CALM);
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(Ritual.TANK_RADIUS * 2))) {
            Quests.fire(player, Quests.HEXCORE_AWAKENED);
        }
    }

    public float instability() {
        return instability;
    }

    public long bornAt() {
        return bornAt;
    }

    @Override
    protected double itemY() {
        return active() ? RISEN_Y : REST_Y;
    }

    @Override
    protected EssenceMix cost(EssenceMix planned) {
        return planned.isEmpty() ? planned : planned.part(Math.max(1, Math.round(planned.total() * THRIFT)));
    }

    // ---- Instability and strikes (server) ----------------------------------------------------------------------------

    @Override
    protected void ritualStarted(ServerLevel level) {
        Ritual.Circle circle = circle();
        long runes = circle == null ? 0 : circle.steles().stream().filter(stele -> stele.rune() != null).count();
        instability = instabilityOf(itemsTotal(), essenceTotal().total(), (int) runes);
    }

    /** How hard a ritual is on the matrix: pedestal items, mB of essence and runes on steles, 0..1. */
    public static float instabilityOf(int items, int essenceMb, int runes) {
        return Mth.clamp(items * 0.07F + essenceMb / 300F + runes * 0.08F - 0.15F, 0, 1);
    }

    @Override
    protected void ritualTick(ServerLevel level) {
        if (elapsed() < CALM_TICKS || stalled()) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextFloat() < 0.003F + 0.03F * instability * instability) {
            strike(level, random);
        }
    }

    /** One arc out of the matrix; with enough instability it does harm where it lands. */
    private void strike(ServerLevel level, RandomSource random) {
        BlockPos pos = getBlockPos();
        List<BlockPos> rods = rods(level);
        if (!rods.isEmpty() && random.nextFloat() < Math.min(0.85F, rods.size() * 0.35F)) {
            // A lightning rod in the circle catches it.
            BlockPos rod = rods.get(random.nextInt(rods.size()));
            BlockState state = level.getBlockState(rod);
            if (state.getBlock() instanceof LightningRodBlock rodBlock) {
                rodBlock.onLightningStrike(state, level, rod);
            }
            land(level, Vec3.atCenterOf(rod).add(0, 0.4, 0), HexcoreStrikePayload.GROUNDED);
            return;
        }
        float harm = instability;
        float roll = random.nextFloat() * (2 - harm + harm * 4);
        if (roll < 2 - harm || harm < 0.25F) {
            land(level, Vec3.atBottomCenterOf(pos).add((random.nextDouble() - 0.5) * 6, 0.1, (random.nextDouble() - 0.5) * 6), HexcoreStrikePayload.CALM);
            return;
        }
        roll -= 2 - harm;
        boolean done = false;
        if (roll < harm) {
            done = strikePlayer(level, random);
        } else if (roll < harm * 2) {
            done = ignite(level, random);
        } else if (roll < harm * 3) {
            done = shatter(level, random);
        } else if (harm >= 0.55F) {
            done = burnPedestalItem(level, random);
        }
        if (!done) {
            land(level, Vec3.atBottomCenterOf(pos).add((random.nextDouble() - 0.5) * 6, 0.1, (random.nextDouble() - 0.5) * 6), HexcoreStrikePayload.CALM);
        }
    }

    private boolean strikePlayer(ServerLevel level, RandomSource random) {
        List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(getBlockPos()).inflate(STRIKE_RADIUS),
                player -> !player.isSpectator() && !player.isCreative());
        if (players.isEmpty()) {
            return false;
        }
        Player player = players.get(random.nextInt(players.size()));
        land(level, player.position().add(0, player.getBbHeight() * 0.6, 0), HexcoreStrikePayload.HARM);
        player.hurtServer(level, level.damageSources().lightningBolt(), 2 + 5 * instability);
        player.igniteForSeconds(2);
        Vec3 away = player.position().subtract(Vec3.atCenterOf(getBlockPos())).multiply(1, 0, 1).normalize().scale(0.6);
        player.push(away.x, 0.25, away.z);
        return true;
    }

    private boolean ignite(ServerLevel level, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos at = around(random, 1);
            if (level.isEmptyBlock(at) && BaseFireBlock.canBePlacedAt(level, at, net.minecraft.core.Direction.UP)) {
                level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
                land(level, Vec3.atBottomCenterOf(at), HexcoreStrikePayload.HARM);
                return true;
            }
        }
        return false;
    }

    private boolean shatter(ServerLevel level, RandomSource random) {
        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos at = around(random, 0);
            BlockState state = level.getBlockState(at);
            float hardness = state.getDestroySpeed(level, at);
            if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() || hardness < 0 || hardness > 1.0F
                    || "hextale".equals(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())) {
                continue;
            }
            land(level, Vec3.atCenterOf(at), HexcoreStrikePayload.HARM);
            level.destroyBlock(at, true);
            return true;
        }
        return false;
    }

    /** Burns an item the ritual has not drawn yet off its pedestal: the ritual waits until it is put back. */
    private boolean burnPedestalItem(ServerLevel level, RandomSource random) {
        List<Ritual.Input> pending = pendingInputs();
        if (pending.size() < 2) {
            return false;
        }
        // Never the one being drawn right now.
        Ritual.Input input = pending.get(1 + random.nextInt(pending.size() - 1));
        if (!(level.getBlockEntity(input.pos()) instanceof PedestalBlockEntity pedestal) || pedestal.item().isEmpty()) {
            return false;
        }
        ItemStack burnt = pedestal.take();
        Vec3 at = Vec3.atCenterOf(input.pos()).add(0, 0.65, 0);
        land(level, at, HexcoreStrikePayload.HARM);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, burnt.getItem()), at.x, at.y, at.z, 14, 0.12, 0.12, 0.12, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, input.pos(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
        return true;
    }

    /** A random block within {@link #STRIKE_RADIUS} of the core, {@code up} blocks over the first solid one from above. */
    private BlockPos around(RandomSource random, int up) {
        BlockPos pos = getBlockPos();
        int x = pos.getX() + random.nextInt(STRIKE_RADIUS * 2 + 1) - STRIKE_RADIUS;
        int z = pos.getZ() + random.nextInt(STRIKE_RADIUS * 2 + 1) - STRIKE_RADIUS;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, pos.getY() + 3, z);
        for (int dy = 0; dy < 7 && level != null && level.getBlockState(at).isAir(); dy++) {
            at.move(0, -1, 0);
        }
        return at.immutable().above(up);
    }

    private List<BlockPos> rods(ServerLevel level) {
        List<BlockPos> rods = new ArrayList<>();
        for (BlockPos at : BlockPos.betweenClosed(getBlockPos().offset(-ROD_RADIUS, -2, -ROD_RADIUS), getBlockPos().offset(ROD_RADIUS, 4, ROD_RADIUS))) {
            if (level.getBlockState(at).getBlock() instanceof LightningRodBlock) {
                rods.add(at.immutable());
            }
        }
        return rods;
    }

    /** The arc lands: the clients draw the bolt, everyone hears the crack and sees the sparks where it struck. */
    private void land(ServerLevel level, Vec3 at, int kind) {
        send(level, at, kind);
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, kind == HexcoreStrikePayload.CALM ? 6 : 16,
                0.2, 0.2, 0.2, 0.2);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS,
                kind == HexcoreStrikePayload.CALM ? 0.35F : 0.9F, 1.4F + level.getRandom().nextFloat() * 0.3F);
    }

    private void send(ServerLevel level, Vec3 at, int kind) {
        HexcoreStrikePayload payload = new HexcoreStrikePayload(getBlockPos(), at, kind);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(getBlockPos()).inflate(SEND_RADIUS))) {
            ModNetwork.sendToPlayer(player, payload);
        }
    }

    // ---- Client ---------------------------------------------------------------------------------------------------

    /** A bolt from the matrix to {@code to} (absolute), drawn for {@code life} ticks. */
    public record Arc(Vec3 to, long born, int life, int seed, boolean strong) {
    }

    public HexcoreMatrix matrix() {
        return matrix;
    }

    public List<Arc> arcs() {
        return arcs;
    }

    /** Client: a strike the server sent. */
    public void strike(HexcoreStrikePayload payload, long now) {
        arcs.add(new Arc(payload.target(), now, payload.kind() == HexcoreStrikePayload.CALM ? 8 : 12, (int) (now * 31 + arcs.size()),
                payload.kind() != HexcoreStrikePayload.CALM));
    }

    /** Client: eases the matrix up during a ritual and down after it; returns its height at {@code time}. */
    public float animateRise(float time) {
        float delta = Float.isNaN(riseTime) || time < riseTime ? 0 : Math.min(time - riseTime, 5);
        riseTime = time;
        rise = Mth.lerp(Math.min(1, 0.04F * delta), rise, (float) itemY());
        return rise;
    }

    @Override
    protected void clientUpdate(Level level) {
        long now = level.getGameTime();
        arcs.removeIf(arc -> now - arc.born() > arc.life() || arc.born() > now);
        if (!active()) {
            return;
        }
        // Small arcs crackle over the circle all through a ritual; the more unstable, the more of them.
        RandomSource random = level.getRandom();
        if (random.nextFloat() < 0.12F + 0.5F * instability) {
            Vec3 to = crackleTarget(level, random);
            if (to != null) {
                arcs.add(new Arc(to, now, 3 + random.nextInt(4), random.nextInt(), false));
            }
        }
    }

    private @Nullable Vec3 crackleTarget(Level level, RandomSource random) {
        Ritual.Circle circle = circle();
        BlockPos pos = getBlockPos();
        int pick = random.nextInt(4);
        if (circle != null && pick == 0 && !circle.pedestals().isEmpty()) {
            return Vec3.atCenterOf(circle.pedestals().get(random.nextInt(circle.pedestals().size())).getBlockPos()).add(0, 0.6, 0);
        }
        if (circle != null && pick == 1 && !circle.steles().isEmpty()) {
            return Vec3.atCenterOf(circle.steles().get(random.nextInt(circle.steles().size())).getBlockPos()).add(0, 0.9, 0);
        }
        if (pick == 2) {
            // Back into the cradle's claws.
            double angle = (random.nextInt(4) + 0.5) * Math.PI / 2;
            return Vec3.atBottomCenterOf(pos).add(Math.cos(angle) * 0.42, 1.1, Math.sin(angle) * 0.42);
        }
        return Vec3.atBottomCenterOf(pos).add((random.nextDouble() - 0.5) * 5, 0.05, (random.nextDouble() - 0.5) * 5);
    }

    // ---- Saving -----------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        instability = input.getFloatOr("instability", 0);
        bornAt = input.getLongOr("born_at", Long.MIN_VALUE);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (active()) {
            output.putFloat("instability", instability);
        }
        if (bornAt != Long.MIN_VALUE) {
            output.putLong("born_at", bornAt);
        }
    }
}
