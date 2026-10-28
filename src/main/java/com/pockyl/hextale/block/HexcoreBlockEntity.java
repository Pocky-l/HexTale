package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
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
import com.pockyl.hextale.registry.ModTags;
import com.pockyl.hextale.world.WorldEdits;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Hexcore: a Hextech Altar whose heart woke up. It runs every ritual the altar runs, from the same circle (later it
 * will make things only it can make); but its matrix is alive. The more pedestal items, essence and runes a ritual
 * takes, the higher its {@link #instability()}, and the longer a ritual runs (waiting included), the angrier the matrix
 * gets ({@link #anger()}). It throws bolts: a calm one only cracks into the ground, an angry one strikes - it hurts and
 * sets alight whoever stands close, scorches the ground, sets things on fire, shatters fragile blocks, cracks a jar so
 * its essence boils off, even burns an item off a pedestal before the ritual could draw it. Nothing says what went
 * missing. Lightning rods in the circle catch some of the bolts.
 */
public class HexcoreBlockEntity extends HextechAltarBlockEntity {
    /** Height of the matrix centre over the block's floor: resting, and risen during a ritual (blocks). */
    public static final double REST_Y = 2.0;
    public static final double RISEN_Y = 2.85;
    /** Ticks the matrix takes to assemble when the altar becomes a Hexcore. */
    public static final int BIRTH_TICKS = 90;
    /** Ticks at the start of a ritual before the first bolt. */
    private static final int CALM_TICKS = 30;
    /** Ticks of a ritual that add full anger on their own. */
    private static final int ANGER_TICKS = 900;
    /** Anger from which bolts do harm, and from which they burn pedestal items. */
    private static final float HARM_FROM = 0.2F;
    private static final float ITEM_FROM = 0.5F;
    private static final int STRIKE_RADIUS = 6;
    private static final int ROD_RADIUS = 8;
    private static final double SEND_RADIUS = 48;

    /** 0..1: how hard the running ritual is on the matrix (synced). */
    private float instability;
    /** 0..1: the ritual's weight plus the time it has run (synced). */
    private float anger;
    private int angerTicks;
    /** Nullstone around that still drinks (server, refreshed while a ritual runs). */
    private List<BlockPos> nullstones = List.of();
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
        // The core acts in the name of whoever placed the altar it grew from.
        java.util.UUID placer = level.getBlockEntity(pos) == null ? null : WorldEdits.placer(level.getBlockEntity(pos));
        level.setBlock(pos, core, Block.UPDATE_ALL);
        if (!(level.getBlockEntity(pos) instanceof HexcoreBlockEntity hexcore)) {
            return;
        }
        if (placer != null) {
            WorldEdits.setPlacer(hexcore, placer);
        }
        // The cradle spreads further than the altar did: stand-ins for the new cells, in the placer's name.
        ShapeParts.refresh(level, pos, null);
        hexcore.bornAt = level.getGameTime();
        hexcore.setChanged();
        Vec3 at = Vec3.atBottomCenterOf(pos).add(0, REST_Y, 0);
        level.sendParticles(ModParticles.ring(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 0, 2.6, 0, 0, 1);
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, pos.getY() + 0.05, at.z, 0, 6, 1, 0, 1);
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 60, 1.2, 1.0, 1.2, 0.4);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 50, 0.4, 0.4, 0.4, 0.25);
        level.playSound(null, pos, ModSounds.RITUAL_COMPLETE.get(), SoundSource.BLOCKS, 1.2F, 0.8F);
        level.playSound(null, pos, ModSounds.BLOCK_AWAKEN.get(), SoundSource.BLOCKS, 1.2F, 0.7F);
        level.playSound(null, pos, ModSounds.HEXCORE_STRIKE.get(), SoundSource.BLOCKS, 0.9F, 0.75F);
        // The new matrix reaches out to every part of its circle once.
        Ritual.Circle circle = Ritual.Circle.scan(level, pos);
        for (var stele : circle.steles()) {
            hexcore.send(level, Vec3.atLowerCornerOf(stele.getBlockPos()).add(0.5, RuneSteleBlockEntity.GLYPH_Y, 0.5), HexcoreStrikePayload.CALM);
        }
        for (var pedestal : circle.pedestals()) {
            hexcore.send(level, Vec3.atCenterOf(pedestal.getBlockPos()).add(0, PedestalBlockEntity.ITEM_Y - 0.5, 0), HexcoreStrikePayload.CALM);
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(Ritual.TANK_RADIUS * 2))) {
            Quests.fire(player, Quests.HEXCORE_AWAKENED);
        }
    }

    public float instability() {
        return instability;
    }

    /** 0..1: how angry the matrix is now - its ritual's weight, growing the longer the ritual runs. */
    public float anger() {
        return anger;
    }

    public long bornAt() {
        return bornAt;
    }

    @Override
    protected double itemY() {
        return active() ? RISEN_Y : REST_Y;
    }

    // ---- Anger and bolts (server) -----------------------------------------------------------------------------------

    @Override
    protected void ritualStarted(ServerLevel level) {
        Ritual.Circle circle = circle();
        long runes = circle == null ? 0 : circle.steles().stream().filter(stele -> stele.rune() != null).count();
        instability = instabilityOf(itemsTotal(), essenceTotal().total(), (int) runes);
        angerTicks = 0;
        nullstones = NullstoneBlock.thirsty(level, getBlockPos());
        anger = angerOf(instability, 0) * NullstoneBlock.calm(nullstones.size());
    }

    /** How hard a ritual is on the matrix: pedestal items, mB of essence and runes on steles, 0..1. */
    public static float instabilityOf(int items, int essenceMb, int runes) {
        return Mth.clamp(items * 0.07F + essenceMb / 300F + runes * 0.08F - 0.15F, 0, 1);
    }

    /** The matrix's anger {@code ticks} into a ritual of that instability: it only grows, waiting or not. */
    public static float angerOf(float instability, int ticks) {
        return Mth.clamp(0.6F * instability + ticks / (float) ANGER_TICKS, 0, 1);
    }

    @Override
    protected void ritualTick(ServerLevel level) {
        angerTicks++;
        if (angerTicks % 40 == 0) {
            nullstones = NullstoneBlock.thirsty(level, getBlockPos());
        }
        // Nullstone around the circle drinks part of the anger - and fills up with it.
        anger = angerOf(instability, angerTicks) * NullstoneBlock.calm(nullstones.size());
        if (!nullstones.isEmpty()) {
            Vec3 core = itemPosition();
            for (BlockPos stone : nullstones) {
                if (angerTicks % 10 == 0) {
                    Vec3 to = Vec3.atCenterOf(stone).add(0, 0.5, 0);
                    level.sendParticles(ModParticles.flow(0x9A6CF0), to.x, to.y, to.z, 0, core.x - to.x, core.y - to.y, core.z - to.z, 1.0);
                }
                if (angerTicks % NullstoneBlock.FILL_TICKS == 0) {
                    NullstoneBlock.fill(level, stone);
                }
            }
        }
        if (angerTicks < CALM_TICKS) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextFloat() < 0.004F + 0.06F * anger * anger) {
            strike(level, random);
        }
    }

    /** One bolt out of the matrix. The angrier it is, the likelier the bolt goes for something that matters. */
    private void strike(ServerLevel level, RandomSource random) {
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
        if (anger < HARM_FROM || random.nextFloat() > anger + 0.2F) {
            // Only into the ground around it - still leaving a mark once it is angry enough.
            scorch(level, random, anger >= HARM_FROM);
            return;
        }
        boolean done = switch (random.nextInt(anger >= ITEM_FROM ? 6 : 5)) {
            case 0 -> strikePlayer(level, random);
            case 1 -> ignite(level, random);
            case 2 -> shatter(level, random);
            case 3 -> spill(level, random);
            case 4 -> scorch(level, random, true);
            default -> burnPedestalItem(level, random);
        };
        if (!done) {
            scorch(level, random, true);
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
        player.hurt(level.damageSources().lightningBolt(), 2 + 6 * anger);
        player.igniteForSeconds(2 + 2 * anger);
        Vec3 away = player.position().subtract(Vec3.atCenterOf(getBlockPos())).multiply(1, 0, 1).normalize().scale(0.5 + 0.5 * anger);
        player.push(away.x, 0.3, away.z);
        return true;
    }

    private boolean ignite(ServerLevel level, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos at = around(random).above();
            if (level.isEmptyBlock(at) && BaseFireBlock.canBePlacedAt(level, at, Direction.UP)
                    && WorldEdits.mayPlace(level, at, BaseFireBlock.getState(level, at), WorldEdits.owner(level, this))) {
                level.setBlockAndUpdate(at, BaseFireBlock.getState(level, at));
                land(level, Vec3.atBottomCenterOf(at), HexcoreStrikePayload.HARM);
                return true;
            }
        }
        return false;
    }

    /** Smashes a block that cannot take it: glass, leaves, flowers, torches, lanterns... never one of the mod's own. */
    private boolean shatter(ServerLevel level, RandomSource random) {
        BlockPos pos = getBlockPos();
        for (int attempt = 0; attempt < 40; attempt++) {
            BlockPos at = pos.offset(random.nextInt(STRIKE_RADIUS * 2 + 1) - STRIKE_RADIUS, random.nextInt(6) - 1,
                    random.nextInt(STRIKE_RADIUS * 2 + 1) - STRIKE_RADIUS);
            BlockState state = level.getBlockState(at);
            float hardness = state.getDestroySpeed(level, at);
            if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty() || hardness < 0 || hardness > 1.0F
                    || at.equals(pos) || "hextale".equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())
                    || state.is(ModTags.VOID_IMMUNE) || !WorldEdits.mayBreak(level, at, WorldEdits.owner(level, this))) {
                continue;
            }
            land(level, Vec3.atCenterOf(at), HexcoreStrikePayload.HARM);
            level.destroyBlock(at, true);
            return true;
        }
        return false;
    }

    /** Cracks a jar of the circle: part of its essence boils away, and the ritual may run short of it. */
    private boolean spill(ServerLevel level, RandomSource random) {
        Ritual.Circle circle = circle();
        if (circle == null) {
            return false;
        }
        List<EssenceTankBlockEntity> jars = circle.tanks().stream()
                .filter(jar -> !jar.isRemoved() && !jar.isCreative() && !jar.tank().contents().isEmpty()).toList();
        if (jars.isEmpty()) {
            return false;
        }
        EssenceTankBlockEntity jar = jars.get(random.nextInt(jars.size()));
        EssenceMix contents = jar.tank().contents();
        int lost = Math.max(1, Math.round(contents.total() * (0.15F + 0.25F * anger)));
        jar.tank().remove(contents.part(lost));
        jar.setChanged();
        Vec3 at = Vec3.atCenterOf(jar.getBlockPos()).add(0, 0.5, 0);
        land(level, at, HexcoreStrikePayload.HARM);
        level.sendParticles(ModParticles.wisp(contents.color()), at.x, at.y, at.z, 24, 0.3, 0.3, 0.3, 0.04);
        level.sendParticles(ModParticles.vapour(contents.color()), at.x, at.y, at.z, 8, 0.25, 0.2, 0.25, 0.02);
        level.playSound(null, jar.getBlockPos(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
        return true;
    }

    /** Burns an item the ritual has not drawn yet off its pedestal; the ritual then waits, without saying for what. */
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
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, burnt), at.x, at.y, at.z, 14, 0.12, 0.12, 0.12, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 6, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, input.pos(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
        return true;
    }

    /** A bolt into the ground; a {@code marking} one scorches grass and moss to coarse dirt and may leave a flame. */
    private boolean scorch(ServerLevel level, RandomSource random, boolean marking) {
        BlockPos ground = around(random);
        land(level, Vec3.atBottomCenterOf(ground.above()), marking ? HexcoreStrikePayload.HARM : HexcoreStrikePayload.CALM);
        if (!marking) {
            return true;
        }
        BlockState state = level.getBlockState(ground);
        net.minecraft.world.entity.player.Player actor = WorldEdits.owner(level, this);
        if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.DIRT_PATH)) && !state.is(ModTags.VOID_IMMUNE)) {
            WorldEdits.change(level, ground, Blocks.COARSE_DIRT.defaultBlockState(), Block.UPDATE_ALL, actor);
        }
        BlockPos above = ground.above();
        BlockState top = level.getBlockState(above);
        if (!top.isAir() && top.canBeReplaced() && top.getFluidState().isEmpty() && !top.is(ModTags.VOID_IMMUNE)) {
            // Grass and flowers on the spot burn away.
            WorldEdits.destroy(level, above, false, actor);
        }
        if (random.nextFloat() < 0.3F && level.isEmptyBlock(above) && BaseFireBlock.canBePlacedAt(level, above, Direction.UP)
                && WorldEdits.mayPlace(level, above, BaseFireBlock.getState(level, above), actor)) {
            level.setBlockAndUpdate(above, BaseFireBlock.getState(level, above));
        }
        return true;
    }

    /** The top solid block of a random column within {@link #STRIKE_RADIUS} of the core (never the core's own). */
    private BlockPos around(RandomSource random) {
        BlockPos pos = getBlockPos();
        int x = 0;
        int z = 0;
        while (x == 0 && z == 0) {
            x = random.nextInt(STRIKE_RADIUS * 2 + 1) - STRIKE_RADIUS;
            z = random.nextInt(STRIKE_RADIUS * 2 + 1) - STRIKE_RADIUS;
        }
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(pos.getX() + x, pos.getY() + 4, pos.getZ() + z);
        for (int dy = 0; dy < 9 && level != null && level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                && level.getBlockState(at).getFluidState().isEmpty(); dy++) {
            at.move(0, -1, 0);
        }
        return at.immutable();
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

    /** The bolt lands: the clients draw it, everyone hears the crack and sees the burst where it struck. */
    private void land(ServerLevel level, Vec3 at, int kind) {
        send(level, at, kind);
        boolean calm = kind == HexcoreStrikePayload.CALM;
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, calm ? 10 : 30, 0.25, 0.25, 0.25, 0.35);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, calm ? 4 : 10, 0.15, 0.15, 0.15, 0.08);
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y + 0.05, at.z, 0, calm ? 0.8 : 1.6, 1, 0, 1);
        if (!calm) {
            level.sendParticles(ModParticles.vapour(ModParticles.SOOT), at.x, at.y, at.z, 8, 0.2, 0.15, 0.2, 0.02);
        }
        level.playSound(null, BlockPos.containing(at), ModSounds.HEXCORE_STRIKE.get(), SoundSource.BLOCKS,
                calm ? 0.45F : 1.0F, 0.95F + level.getRandom().nextFloat() * 0.15F);
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

    /** Client: a bolt the server sent. */
    public void strike(HexcoreStrikePayload payload, long now) {
        arcs.add(new Arc(payload.target(), now, payload.kind() == HexcoreStrikePayload.CALM ? 10 : 16, (int) (now * 31 + arcs.size()),
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
        // Small arcs crackle over the circle all through a ritual; the angrier, the more of them.
        RandomSource random = level.getRandom();
        if (random.nextFloat() < 0.12F + 0.5F * anger) {
            Vec3 to = crackleTarget(random);
            if (to != null) {
                arcs.add(new Arc(to, now, 3 + random.nextInt(4), random.nextInt(), false));
            }
        }
    }

    private @Nullable Vec3 crackleTarget(RandomSource random) {
        Ritual.Circle circle = circle();
        BlockPos pos = getBlockPos();
        int pick = random.nextInt(4);
        if (circle != null && pick == 0 && !circle.pedestals().isEmpty()) {
            return Vec3.atCenterOf(circle.pedestals().get(random.nextInt(circle.pedestals().size())).getBlockPos()).add(0, PedestalBlockEntity.ITEM_Y - 0.5, 0);
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
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        instability = input.getFloatOr("instability", 0);
        anger = input.getFloatOr("anger", 0);
        angerTicks = input.getIntOr("anger_ticks", 0);
        bornAt = input.getLongOr("born_at", Long.MIN_VALUE);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        if (active()) {
            output.putFloat("instability", instability);
            output.putFloat("anger", anger);
            output.putInt("anger_ticks", angerTicks);
        }
        if (bornAt != Long.MIN_VALUE) {
            output.putLong("born_at", bornAt);
        }
    }
}
