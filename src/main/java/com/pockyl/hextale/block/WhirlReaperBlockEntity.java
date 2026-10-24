package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LevelEvent;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.Reaping;
import com.pockyl.hextale.farm.WhirlKind;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.WhirlPayload;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ReaperRune;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A Whirl Reaper's state and work. Every interval it looks over its field for the nearest ripe plant nobody is reaping
 * yet and sends a whirl of air to it. The whirl is a timeline the clients play from one {@link WhirlPayload}: it flies
 * for {@code travel} ticks, then works over the plant for the kind's {@link WhirlKind#work()} ticks. The server keeps the
 * same timeline as a {@link Pending} and acts when the clients see it happen: a fruit or stalk is taken up when the whirl
 * arrives, the harvest falls when it is done.
 *
 * <p>Every whirl costs air essence: one mB of it drawn out of a jar within {@link JarReach#RADIUS} gives
 * {@value #WHIRLS_PER_MB} whirls. Without wind the reaper stands still ({@link #becalmed()}). Nothing ripe = it looks
 * more and more rarely (up to {@value #MAX_SLEEP} ticks), so a sleeping field costs almost nothing; the field is only
 * read block by block, never searched for entities.
 */
public class WhirlReaperBlockEntity extends BlockEntity {
    /** Whirls in the air at once. */
    public static final int MAX_WHIRLS = 4;
    public static final int WHIRLS_PER_MB = 4;
    /** mB of air drawn at once. */
    public static final int DRAW_MB = 4;
    /** Field height below and above the reaper. */
    public static final int DOWN = 3;
    public static final int UP = 3;
    /** Longest sleep of an idle reaper, in ticks. */
    public static final int MAX_SLEEP = 80;
    /** Ticks a jar's stream stays visible after a draw. */
    public static final int STREAM_TICKS = 50;

    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** Whirls paid for. */
    private int charges;
    private boolean becalmed;
    private final List<Pending> pending = new ArrayList<>();
    private int wait;
    private int idle;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;
    /** Client: whirls in the air now. */
    public final List<Shown> shown = new ArrayList<>();
    /** Client: the vanes' angle (degrees) and turning speed. */
    public float vanes;
    public float vanesBefore;
    private float vaneSpeed;

    public WhirlReaperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WHIRL_REAPER.get(), pos, state);
    }

    /** A whirl on its way, kept by the server until its harvest has fallen. */
    public record Pending(BlockPos target, WhirlKind kind, long arrive, long done, boolean lifted, List<BlockState> taken) {
        public static final Codec<Pending> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("target").forGetter(Pending::target),
                Codec.INT.xmap(WhirlKind::byIndex, WhirlKind::ordinal).fieldOf("kind").forGetter(Pending::kind),
                Codec.LONG.fieldOf("arrive").forGetter(Pending::arrive),
                Codec.LONG.fieldOf("done").forGetter(Pending::done),
                Codec.BOOL.optionalFieldOf("lifted", false).forGetter(Pending::lifted),
                BlockState.CODEC.listOf().optionalFieldOf("taken", List.of()).forGetter(Pending::taken)
        ).apply(instance, Pending::new));
    }

    /** Client: a whirl being shown; {@code column} = the blocks it takes up (a fruit, a stalk's pieces), read on launch. */
    public record Shown(BlockPos target, WhirlKind kind, long start, int travel, List<BlockState> column) {
        public int arrive() {
            return travel;
        }

        public int done() {
            return travel + kind.work();
        }
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    public int radius() {
        return ReaperRune.radius(runes.strength(ReaperRune.SHIFT.rune()));
    }

    public int interval() {
        return ReaperRune.interval(runes.strength(ReaperRune.MOTOR.rune()));
    }

    // ---- State ----------------------------------------------------------------------------------------------------------

    /** Whirls already paid for. */
    public int charges() {
        return charges;
    }

    /** Whether it found something ripe but had no air essence to send a whirl. */
    public boolean becalmed() {
        return becalmed;
    }

    public List<Pending> pending() {
        return pending;
    }

    /** The jar it is drawing from now (for its stream), or null. */
    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** Where whirls leave from and the crystal floats: over the vanes. */
    public static Vec3 crystal(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 14.5 / 16, 0.5);
    }

    /** Look again at once (a rune changed, a plant was placed by hand...). */
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, WhirlReaperBlockEntity reaper) {
        ServerLevel server = (ServerLevel) level;
        reaper.advance(server);
        if (--reaper.wait > 0) {
            return;
        }
        int interval = reaper.interval();
        if (reaper.pending.size() >= MAX_WHIRLS) {
            reaper.wait = interval;
            return;
        }
        BlockPos target = reaper.findRipe(server);
        if (target == null) {
            reaper.idle = Math.min(reaper.idle + 1, 3);
            reaper.wait = Math.min(MAX_SLEEP, interval << reaper.idle);
            reaper.setBecalmed(false);
            return;
        }
        reaper.idle = 0;
        if (reaper.charges <= 0 && !reaper.drawWind(server)) {
            reaper.setBecalmed(true);
            reaper.wait = 40;
            return;
        }
        reaper.setBecalmed(false);
        reaper.launch(server, target);
        reaper.wait = interval;
    }

    /** Arrivals and harvests that are due now. */
    private void advance(ServerLevel level) {
        if (pending.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        boolean dirty = false;
        for (int i = 0; i < pending.size(); i++) {
            Pending whirl = pending.get(i);
            if (whirl.kind().lifts() && !whirl.lifted() && now >= whirl.arrive()) {
                List<BlockState> taken = Reaping.lift(level, whirl.target(), whirl.kind());
                whirl = new Pending(whirl.target(), whirl.kind(), whirl.arrive(), whirl.done(), true, taken);
                pending.set(i, whirl);
                dirty = true;
            }
            if (now >= whirl.done()) {
                harvest(level, whirl);
                pending.remove(i--);
                dirty = true;
            }
        }
        if (dirty) {
            setChanged();
        }
    }

    private void harvest(ServerLevel level, Pending whirl) {
        boolean reaped;
        BlockPos target = whirl.target();
        if (whirl.kind() == WhirlKind.CROP) {
            reaped = Reaping.reapCrop(level, target, drops -> enrich(level, target, drops));
        } else {
            Reaping.drop(level, target, whirl.kind(), whirl.taken(), worldPosition, drops -> enrich(level, target, drops));
            reaped = !whirl.taken().isEmpty();
        }
        if (reaped) {
            Vec3 at = Vec3.atCenterOf(target);
            level.playSound(null, at.x, at.y, at.z, ModSounds.WHIRL_REAP.get(), SoundSource.BLOCKS, 0.5F,
                    0.9F + level.getRandom().nextFloat() * 0.2F);
            afterHarvest(level, target);
        }
    }

    private int strength(ReaperRune rune) {
        return runes.strength(rune.rune());
    }

    /** Pollen: one or two more of the produce; Photosynthesis: now and then a golden fruit. */
    void enrich(ServerLevel level, BlockPos pos, List<ItemStack> drops) {
        RandomSource random = level.getRandom();
        Vec3 at = Vec3.atCenterOf(pos);
        ItemStack produce = drops.stream().filter(stack -> !stack.isEmpty()).findFirst().orElse(ItemStack.EMPTY);
        if (produce.isEmpty()) {
            return;
        }
        int pollen = strength(ReaperRune.POLLEN);
        if (pollen > 0 && random.nextFloat() < HolderRune.at(ReaperRune.MORE, pollen)) {
            drops.add(produce.copyWithCount(1 + random.nextInt(2)));
            ToolEffects.puff(level, at, ReaperRune.POLLEN.rune(), 6, 0.3);
        }
        int light = strength(ReaperRune.PHOTOSYNTHESIS);
        if (light > 0 && random.nextFloat() < HolderRune.at(ReaperRune.GOLDEN, light)) {
            drops.add(new ItemStack(golden(drops)));
            ToolEffects.puff(level, at, ReaperRune.PHOTOSYNTHESIS.rune(), 14, 0.4);
            level.sendParticles(ModParticles.glint(ModParticles.GOLD), at.x, at.y, at.z, 10, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.BLOCKS, 0.6F, 0.7F);
        }
    }

    /** The golden fruit of a harvest: a golden carrot from carrots, a glistering slice from melons, else a golden apple. */
    static Item golden(List<ItemStack> drops) {
        for (ItemStack stack : drops) {
            if (stack.is(Items.CARROT)) {
                return Items.GOLDEN_CARROT;
            }
            if (stack.is(Items.MELON_SLICE)) {
                return Items.GLISTERING_MELON_SLICE;
            }
        }
        return Items.GOLDEN_APPLE;
    }

    /** Fertility: the plant may be bonemealed; Spring: the farmland around is wetted and the plants around may grow; Radiance: experience. */
    void afterHarvest(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        int fertility = strength(ReaperRune.FERTILITY);
        BlockState plant = level.getBlockState(pos);
        if (fertility > 0 && plant.getBlock() instanceof BonemealableBlock meal && meal.isValidBonemealTarget(level, pos, plant, BonemealSource.MOB)
                && random.nextFloat() < HolderRune.at(ReaperRune.BONEMEAL, fertility)) {
            meal.performBonemeal(level, random, pos, plant, BonemealSource.MOB);
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
            ToolEffects.puff(level, Vec3.atCenterOf(pos), ReaperRune.FERTILITY.rune(), 6, 0.3);
        }
        int spring = strength(ReaperRune.SPRING);
        if (spring > 0) {
            for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 0, 1))) {
                BlockState state = level.getBlockState(near);
                if (state.getBlock() instanceof FarmlandBlock && state.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE) {
                    level.setBlock(near, state.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
                } else if (near.getY() == pos.getY() && !near.equals(pos) && state.isRandomlyTicking()
                        && random.nextFloat() < HolderRune.at(ReaperRune.QUICKEN, spring)) {
                    state.randomTick(level, near.immutable(), random);
                }
            }
            level.sendParticles(ModParticles.drop(ModParticles.WATER), pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 10, 0.6, 0.1, 0.6, 0.1);
        }
        int radiance = strength(ReaperRune.RADIANCE);
        if (radiance > 0 && random.nextFloat() < HolderRune.at(ReaperRune.EXPERIENCE, radiance)) {
            ExperienceOrb.award(level, crystal(worldPosition), 1);
        }
    }

    /** The nearest ripe plant of the field that no whirl is on its way to, or null. */
    @Nullable
    BlockPos findRipe(ServerLevel level) {
        int radius = radius();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        List<BlockPos> ripe = new ArrayList<>();
        for (int dy = -DOWN; dy <= UP; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    at.setWithOffset(worldPosition, dx, dy, dz);
                    if (dx == 0 && dz == 0 && dy == 0 || !level.isLoaded(at)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(at);
                    if (Reaping.ripe(level, at, state) != null && !claimed(at)) {
                        ripe.add(at.immutable());
                    }
                }
            }
        }
        return ripe.stream().min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(worldPosition)).thenComparingLong(BlockPos::asLong))
                .orElse(null);
    }

    private boolean claimed(BlockPos target) {
        for (Pending whirl : pending) {
            if (whirl.target().equals(target)) {
                return true;
            }
        }
        return false;
    }

    /** Draws air essence out of the nearest jar that holds some; false when none does. */
    private boolean drawWind(ServerLevel level) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            int taken = Math.min(DRAW_MB, jar.tank().contents().get(Essence.AIR));
            if (taken <= 0) {
                continue;
            }
            jar.tank().remove(EssenceMix.pure(Essence.AIR, taken));
            jar.setChanged();
            charges += taken * WHIRLS_PER_MB;
            drewFrom = jar.getBlockPos();
            drewAt = level.getGameTime();
            Vec3 mouth = JarReach.mouth(drewFrom);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 1.2F);
            changed();
            return true;
        }
        return false;
    }

    private void setBecalmed(boolean value) {
        if (becalmed != value) {
            becalmed = value;
            changed();
        }
    }

    /** Sends a whirl from the crystal to the plant and tells the clients around. */
    private void launch(ServerLevel level, BlockPos target) {
        BlockState state = level.getBlockState(target);
        WhirlKind kind = Reaping.ripe(level, target, state);
        if (kind == null) {
            return;
        }
        int dam = strength(ReaperRune.DAM);
        if (dam == 0 || level.getRandom().nextFloat() >= HolderRune.at(ReaperRune.THRIFT, dam)) {
            charges--;
        }
        int travel = travelTicks(worldPosition, target);
        long now = level.getGameTime();
        pending.add(new Pending(target.immutable(), kind, now + travel, now + travel + kind.work(), false, List.of()));
        // The clients' count of whirls left (Jade, the status line).
        changed();
        Vec3 from = crystal(worldPosition);
        level.playSound(null, from.x, from.y, from.z, ModSounds.WHIRL_LAUNCH.get(), SoundSource.BLOCKS, 0.4F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        WhirlPayload payload = new WhirlPayload(worldPosition, target.immutable(), kind, travel);
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(worldPosition), false)) {
            ModNetwork.sendToPlayer(player, payload);
        }
    }

    /** Ticks a whirl flies from the reaper to a plant: a little longer the farther it is. */
    public static int travelTicks(BlockPos reaper, BlockPos target) {
        return 8 + Mth.ceil(Math.sqrt(reaper.distSqr(target)) * 2.2);
    }

    /** The reaper is broken: whirls on their way let their harvest fall at once, nothing taken is lost. */
    public void dropPending(ServerLevel level) {
        for (Pending whirl : pending) {
            if (whirl.lifted()) {
                Reaping.drop(level, whirl.target(), whirl.kind(), whirl.taken(), worldPosition);
            }
        }
        pending.clear();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            dropPending(server);
        }
    }

    // ---- Client ---------------------------------------------------------------------------------------------------------

    /** Client: a whirl left; the blocks it will take up are read now, while they still stand. */
    public void show(WhirlPayload payload, long now) {
        List<BlockState> column = new ArrayList<>();
        if (level != null && payload.kind() != WhirlKind.CROP) {
            BlockState base = level.getBlockState(payload.target());
            if (payload.kind() == WhirlKind.GOURD) {
                column.add(base);
            } else {
                int height = Reaping.height(level, payload.target(), base);
                for (int dy = 1; dy < height; dy++) {
                    column.add(level.getBlockState(payload.target().above(dy)));
                }
            }
        }
        shown.add(new Shown(payload.target(), payload.kind(), now, payload.travel(), column));
    }

    /** Client tick: the vanes speed up while whirls fly and stand still without wind; old whirls are forgotten. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, WhirlReaperBlockEntity reaper) {
        long now = level.getGameTime();
        reaper.shown.removeIf(whirl -> now - whirl.start() > whirl.done() + com.pockyl.hextale.farm.WhirlTimeline.FADE);
        float target = reaper.becalmed ? 0.4F : reaper.shown.isEmpty() ? 2.5F : 16F;
        reaper.vaneSpeed += (target - reaper.vaneSpeed) * 0.06F;
        reaper.vanesBefore = reaper.vanes;
        reaper.vanes += reaper.vaneSpeed;
        if (reaper.vanes > 360) {
            reaper.vanes -= 360;
            reaper.vanesBefore -= 360;
        }
        com.pockyl.hextale.farm.WhirlTimeline.particles(level, reaper, now);
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.REAPER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.REAPER_RUNES.get(), runes);
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
        becalmed = input.getBooleanOr("becalmed", false);
        pending.clear();
        input.read("pending", Pending.CODEC.listOf()).ifPresent(pending::addAll);
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
        output.putBoolean("becalmed", becalmed);
        if (!pending.isEmpty()) {
            output.store("pending", Pending.CODEC.listOf(), List.copyOf(pending));
        }
        if (drewFrom != null) {
            output.store("drew_from", BlockPos.CODEC, drewFrom);
            output.putLong("drew_at", drewAt);
        }
    }

    /** The clients need the runes, the wind and the jar it draws from - not the whirls (they come as payloads). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove("pending");
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
