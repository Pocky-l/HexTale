package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.Felling;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.SawPayload;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.SawmillRune;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * A Hex Sawmill's state and work, the Whirl Reaper's way for trees. Every interval it looks around for the nearest grown
 * tree ({@link Felling}) and sends its saw: a disc of hextech light that flies to the trunk ({@code travel} ticks), saws
 * through it ({@value #SAW_TICKS}), and the tree falls ({@value #FALL_TICKS}). The server keeps the same timeline: the
 * tree leaves the world when the saw is through (the clients draw it falling), and its drops land on the foot with
 * saplings from its own leaves planted back when it has fallen. Every tree costs {@value #MB_PER_TREE} mB of air
 * essence from a jar within {@link JarReach#RADIUS}; no air = becalmed. Idle, it looks more and more rarely.
 */
public class HexSawmillBlockEntity extends BlockEntity implements com.pockyl.hextale.port.Removal.SideEffects {
    public static final int SAW_TICKS = 30;
    public static final int FALL_TICKS = 26;
    /** Trees felled at once. */
    public static final int MAX_SAWS = 2;
    public static final int MB_PER_TREE = 2;
    public static final int DRAW_MB = 4;
    public static final int DOWN = 3;
    public static final int UP = 3;
    public static final int MAX_SLEEP = 200;
    public static final int STREAM_TICKS = 50;
    /** Feet found to carry no tree are not walked again for this long. */
    private static final int FORGET_TICKS = 1200;

    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** Air essence paid in, mB. */
    private int wind;
    private boolean becalmed;
    private final List<Pending> pending = new ArrayList<>();
    private final Set<BlockPos> notTrees = new HashSet<>();
    private long forgetAt;
    private int wait;
    private int idle;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;
    /** Client: saws in the air and trees falling. */
    public final List<Shown> shown = new ArrayList<>();
    /** Client: the disc's angle (degrees) and turning speed. */
    public float disc;
    public float discBefore;
    private float discSpeed;

    public HexSawmillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_SAWMILL.get(), pos, state);
    }

    /** A felling on its way, kept until the tree has fallen. */
    public record Pending(BlockPos foot, List<BlockPos> feet, List<BlockPos> logs, List<BlockPos> leaves, long cut, long done,
                          boolean felled, List<ItemStack> drops) {
        public static final Codec<Pending> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("foot").forGetter(Pending::foot),
                BlockPos.CODEC.listOf().fieldOf("feet").forGetter(Pending::feet),
                BlockPos.CODEC.listOf().fieldOf("logs").forGetter(Pending::logs),
                BlockPos.CODEC.listOf().fieldOf("leaves").forGetter(Pending::leaves),
                Codec.LONG.fieldOf("cut").forGetter(Pending::cut),
                Codec.LONG.fieldOf("done").forGetter(Pending::done),
                Codec.BOOL.optionalFieldOf("felled", false).forGetter(Pending::felled),
                ItemStack.CODEC.listOf().optionalFieldOf("drops", List.of()).forGetter(Pending::drops)
        ).apply(instance, Pending::new));

        Felling.Tree tree() {
            return new Felling.Tree(foot, feet, logs, leaves);
        }
    }

    /**
     * Client: a saw being shown. {@code blocks}/{@code states} = the tree as it stood when the saw left (it falls drawn
     * from them), {@code away} = the side it falls to.
     */
    public record Shown(BlockPos foot, long start, int travel, List<BlockPos> blocks, List<BlockState> states, Direction away, int top) {
        public int cut() {
            return travel + SAW_TICKS;
        }

        public int done() {
            return cut() + FALL_TICKS;
        }
    }

    // ---- Runes and state ------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    public int radius() {
        return SawmillRune.radius(runes.strength(SawmillRune.SHIFT.rune()));
    }

    public int interval() {
        return SawmillRune.interval(runes.strength(SawmillRune.MOTOR.rune()));
    }

    public int wind() {
        return wind;
    }

    public boolean becalmed() {
        return becalmed;
    }

    public List<Pending> pending() {
        return pending;
    }

    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** The disc's hub: where saws leave from. */
    public static Vec3 hub(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 8 / 16.0, 8.5 / 16);
    }

    /** Where the wind drawn from a jar goes in: the top of the disc's guard. */
    public static Vec3 intake(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 16.0 / 16, 8.5 / 16);
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, HexSawmillBlockEntity mill) {
        ServerLevel server = (ServerLevel) level;
        mill.advance(server);
        if (--mill.wait > 0) {
            return;
        }
        int interval = mill.interval();
        if (mill.pending.size() >= MAX_SAWS) {
            mill.wait = interval;
            return;
        }
        if (server.getGameTime() >= mill.forgetAt) {
            mill.notTrees.clear();
            mill.forgetAt = server.getGameTime() + FORGET_TICKS;
        }
        Felling.Tree tree = mill.findTree(server);
        if (tree == null) {
            mill.idle = Math.min(mill.idle + 1, 3);
            mill.wait = Math.min(MAX_SLEEP, interval << mill.idle);
            mill.setBecalmed(false);
            return;
        }
        mill.idle = 0;
        if (mill.wind < MB_PER_TREE && !mill.drawWind(server)) {
            mill.setBecalmed(true);
            mill.wait = 40;
            return;
        }
        mill.setBecalmed(false);
        mill.launch(server, tree);
        mill.wait = interval;
    }

    /** Trees whose saw is through fall; fallen trees drop and are planted again. */
    private void advance(ServerLevel level) {
        if (pending.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        boolean dirty = false;
        for (int i = 0; i < pending.size(); i++) {
            Pending saw = pending.get(i);
            if (!saw.felled() && now >= saw.cut()) {
                List<ItemStack> drops = fellWithRunes(level, saw);
                saw = new Pending(saw.foot(), saw.feet(), saw.logs(), saw.leaves(), saw.cut(), saw.done(), true, drops);
                pending.set(i, saw);
                dirty = true;
                Vec3 at = Vec3.atCenterOf(saw.foot());
                level.playSound(null, at.x, at.y, at.z, ModSounds.SAW_FELL.get(), SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            }
            if (now >= saw.done()) {
                land(level, saw);
                pending.remove(i--);
                dirty = true;
            }
        }
        if (dirty) {
            setChanged();
        }
    }

    private int strength(SawmillRune rune) {
        return runes.strength(rune.rune());
    }

    /**
     * The tree comes down with the sawmill's runes: Pollen cuts the leaves with Fortune, Blades may give more logs, Hush
     * takes bee nests whole, Photosynthesis may leave a golden apple.
     */
    private List<ItemStack> fellWithRunes(ServerLevel level, Pending saw) {
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        int pollen = strength(SawmillRune.POLLEN);
        if (pollen > 0) {
            ItemStack lucky = ToolEffects.withFortune(level, axe, HolderRune.at(SawmillRune.LEAF_FORTUNE, pollen));
            axe = lucky == null ? axe : lucky;
        }
        int blades = strength(SawmillRune.BLADES);
        List<ItemStack> drops = new ArrayList<>(Felling.fell(level, saw.tree(), axe, blades == 0 ? 0 : HolderRune.at(SawmillRune.MORE_LOGS, blades)));
        Vec3 crown = Vec3.atCenterOf(saw.foot()).add(0, 2, 0);
        if (strength(SawmillRune.HUSH) > 0) {
            List<ItemStack> nests = Felling.takeNests(level, saw.tree());
            if (!nests.isEmpty()) {
                drops.addAll(nests);
                ToolEffects.puff(level, crown, SawmillRune.HUSH.rune(), 10, 0.6);
            }
        }
        int light = strength(SawmillRune.PHOTOSYNTHESIS);
        if (light > 0 && level.getRandom().nextFloat() < HolderRune.at(SawmillRune.GOLDEN, light)) {
            drops.add(new ItemStack(Items.GOLDEN_APPLE));
            ToolEffects.puff(level, crown, SawmillRune.PHOTOSYNTHESIS.rune(), 16, 0.8);
            level.sendParticles(ModParticles.glint(ModParticles.GOLD), crown.x, crown.y, crown.z, 12, 0.6, 0.6, 0.6, 0.05);
        }
        return drops;
    }

    private void land(ServerLevel level, Pending saw) {
        List<ItemStack> drops = new ArrayList<>(saw.drops().stream().map(ItemStack::copy).toList());
        List<BlockPos> planted = new ArrayList<>();
        Felling.replant(level, saw.foot(), saw.feet(), drops, planted);
        int growth = strength(SawmillRune.GROWTH);
        if (growth > 0 && level.getRandom().nextFloat() < HolderRune.at(SawmillRune.GROW, growth)) {
            for (BlockPos sapling : planted) {
                Felling.grow(level, sapling);
            }
            if (!planted.isEmpty()) {
                ToolEffects.puff(level, Vec3.atCenterOf(saw.foot()), SawmillRune.GROWTH.rune(), 10, 0.4);
            }
        }
        Felling.drop(level, saw.foot(), drops);
        Vec3 at = Vec3.atCenterOf(saw.foot());
        level.playSound(null, at.x, at.y, at.z, ModSounds.SAW_LAND.get(), SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
    }

    /** The nearest grown tree no saw is on its way to, or null. */
    Felling.@Nullable Tree findTree(ServerLevel level) {
        int radius = radius();
        Set<BlockPos> claimed = new HashSet<>();
        for (Pending saw : pending) {
            claimed.addAll(saw.logs());
        }
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        List<BlockPos> feet = new ArrayList<>();
        for (int dy = -DOWN; dy <= UP; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    at.setWithOffset(worldPosition, dx, dy, dz);
                    if (!level.isLoaded(at) || claimed.contains(at) || notTrees.contains(at)) {
                        continue;
                    }
                    if (Felling.isFoot(level, at, level.getBlockState(at))) {
                        feet.add(at.immutable());
                    }
                }
            }
        }
        feet.sort(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(worldPosition)).thenComparingLong(BlockPos::asLong));
        for (BlockPos foot : feet) {
            if (claimed.contains(foot)) {
                continue;
            }
            Felling.Tree tree = Felling.find(level, foot);
            if (tree == null) {
                notTrees.add(foot);
                continue;
            }
            // A big tree is found from each of its feet: once is enough.
            if (tree.logs().stream().anyMatch(claimed::contains)) {
                continue;
            }
            return tree;
        }
        return null;
    }

    private boolean drawWind(ServerLevel level) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            int taken = Math.min(DRAW_MB, jar.tank().contents().get(Essence.AIR));
            if (taken <= 0) {
                continue;
            }
            jar.tank().remove(EssenceMix.pure(Essence.AIR, taken));
            jar.setChanged();
            wind += taken;
            drewFrom = jar.getBlockPos();
            drewAt = level.getGameTime();
            Vec3 mouth = JarReach.mouth(drewFrom);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 1.2F);
            changed();
            if (wind >= MB_PER_TREE) {
                return true;
            }
        }
        return wind >= MB_PER_TREE;
    }

    private void setBecalmed(boolean value) {
        if (becalmed != value) {
            becalmed = value;
            changed();
        }
    }

    private void launch(ServerLevel level, Felling.Tree tree) {
        int dam = strength(SawmillRune.DAM);
        if (dam == 0 || level.getRandom().nextFloat() >= HolderRune.at(SawmillRune.THRIFT, dam)) {
            wind -= MB_PER_TREE;
        }
        int travel = travelTicks(worldPosition, tree.foot());
        long now = level.getGameTime();
        pending.add(new Pending(tree.foot(), tree.feet(), tree.logs(), tree.leaves(), now + travel + SAW_TICKS,
                now + travel + SAW_TICKS + FALL_TICKS, false, List.of()));
        changed();
        Vec3 from = hub(worldPosition);
        level.playSound(null, from.x, from.y, from.z, ModSounds.SAW_LAUNCH.get(), SoundSource.BLOCKS, 0.5F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        SawPayload payload = new SawPayload(worldPosition, tree.foot(), travel);
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
            ModNetwork.sendToPlayer(player, payload);
        }
    }

    public static int travelTicks(BlockPos mill, BlockPos foot) {
        return 10 + Mth.ceil(Math.sqrt(mill.distSqr(foot)) * 1.8);
    }

    /** The side a tree falls to: away from the mill. */
    public static Direction away(BlockPos mill, BlockPos foot) {
        int dx = foot.getX() - mill.getX();
        int dz = foot.getZ() - mill.getZ();
        if (dx == 0 && dz == 0) {
            return Direction.NORTH;
        }
        return Math.abs(dx) >= Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        com.pockyl.hextale.port.Removal.vanilla(this, pos, state);
        if (level instanceof ServerLevel server) {
            for (Pending saw : pending) {
                if (saw.felled()) {
                    Felling.drop(server, saw.foot(), saw.drops());
                }
            }
            pending.clear();
        }
    }

    // ---- Client ---------------------------------------------------------------------------------------------------------

    /** Client: a saw left; the tree it will fell is read now, while it still stands. */
    public void show(SawPayload payload, long now) {
        List<BlockPos> blocks = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        int top = payload.foot().getY();
        Felling.Tree tree = level == null ? null : Felling.find(level, payload.foot());
        if (tree != null) {
            for (BlockPos pos : tree.logs()) {
                blocks.add(pos);
                states.add(level.getBlockState(pos));
            }
            for (BlockPos pos : tree.leaves()) {
                blocks.add(pos);
                states.add(level.getBlockState(pos));
            }
            top = tree.top();
        }
        shown.add(new Shown(payload.foot(), now, payload.travel(), blocks, states, away(worldPosition, payload.foot()), top));
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, HexSawmillBlockEntity mill) {
        long now = level.getGameTime();
        mill.shown.removeIf(saw -> now - saw.start() > saw.done() + com.pockyl.hextale.farm.SawTimeline.FADE);
        boolean sawing = mill.shown.stream().anyMatch(saw -> now - saw.start() < saw.cut());
        float target = mill.becalmed ? 0.5F : sawing ? 40F : 4F;
        mill.discSpeed += (target - mill.discSpeed) * 0.05F;
        mill.discBefore = mill.disc;
        mill.disc += mill.discSpeed;
        if (mill.disc > 360) {
            mill.disc -= 360;
            mill.discBefore -= 360;
        }
        com.pockyl.hextale.farm.SawTimeline.particles(level, mill, now);
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.SAWMILL_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.SAWMILL_RUNES.get(), runes);
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
        wind = input.getIntOr("wind", 0);
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
        output.putInt("wind", wind);
        output.putBoolean("becalmed", becalmed);
        if (!pending.isEmpty()) {
            output.store("pending", Pending.CODEC.listOf(), List.copyOf(pending));
        }
        if (drewFrom != null) {
            output.store("drew_from", BlockPos.CODEC, drewFrom);
            output.putLong("drew_at", drewAt);
        }
    }

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
