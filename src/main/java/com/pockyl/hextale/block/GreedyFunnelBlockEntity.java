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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.ReachIndex;
import com.pockyl.hextale.network.FunnelPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.FunnelRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.transport.PylonInsert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A Greedy Funnel's state and work. It never looks for anything: a creature dying within its reach asks the
 * {@link #INDEX} for the nearest funnel, and the funnel swallows the death on the spot ({@link #swallow},
 * {@link #swallowExperience}) - the drops go straight into the container under it and the experience into its crystal,
 * so not one item or orb is ever spawned. What the container cannot take waits in the funnel ({@value #HELD_STACKS}
 * stacks); only when that is full too do drops fall as they always did.
 *
 * <p>Every death costs decay essence: one mB drawn out of a jar within {@link JarReach#RADIUS} pays for
 * {@value #DEATHS_PER_MB} deaths. Without decay the funnel goes hungry and the deaths around it are left alone. The
 * block ticks only while it holds something to pass down.
 */
public class GreedyFunnelBlockEntity extends BlockEntity {
    /** The loaded funnels, asked by every death. */
    public static final ReachIndex<GreedyFunnelBlockEntity> INDEX = new ReachIndex<>(GreedyFunnelBlockEntity.class,
            FunnelRune.MAX_RADIUS, GreedyFunnelBlockEntity::covers);
    public static final int DEATHS_PER_MB = 4;
    /** mB of decay drawn at once. */
    public static final int DRAW_MB = 4;
    /** Stacks it holds when the container below is full. */
    public static final int HELD_STACKS = 9;
    /** Experience points the crystal holds (thirty levels' worth). */
    public static final int EXPERIENCE_CAP = 1395;
    /** Ticks a jar's stream stays visible after a draw. */
    public static final int STREAM_TICKS = 50;
    /** Ticks between two tries to pass held stacks down. */
    private static final int PASS_EVERY = 20;
    /** Drop icons sent to the clients per death. */
    private static final int ICONS = 4;

    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** Deaths paid for. */
    private int charges;
    private boolean hungry;
    private int experience;
    private final List<ItemStack> held = new ArrayList<>();
    /** Client: how many stacks it holds (the stacks themselves stay on the server). */
    private int heldCount;
    /** The last death paid for and the experience swallowed from it (it comes just before its drops). */
    private int paidFor = -1;
    private int paidExperience;
    private int wait;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;
    /** Client: deaths being drawn in now. */
    public final List<Shown> shown = new ArrayList<>();
    /** Client: the vortex's angle (degrees) and turning speed. */
    public float vortex;
    public float vortexBefore;
    private float vortexSpeed;

    public GreedyFunnelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GREEDY_FUNNEL.get(), pos, state);
    }

    /** Client: a death being drawn in: where it died, a few of its drops, its experience, when. */
    public record Shown(Vec3 from, List<ItemStack> icons, int experience, long start, int seed) {
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    private int strength(FunnelRune rune) {
        return runes.strength(rune.rune());
    }

    public int radius() {
        return FunnelRune.radius(strength(FunnelRune.SHIFT));
    }

    // ---- State ----------------------------------------------------------------------------------------------------------

    public int charges() {
        return charges;
    }

    /** Whether a death came by and it had no decay to pay for it. */
    public boolean hungry() {
        return hungry;
    }

    public int experience() {
        return experience;
    }

    /** Stacks waiting for room in the container below. */
    public List<ItemStack> held() {
        return held;
    }

    /** Whether it holds all it can and lets drops fall again. */
    public boolean full() {
        return (level != null && level.isClientSide() ? heldCount : held.size()) >= HELD_STACKS;
    }

    /** The jar it is drawing from now (for its stream), or null. */
    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** Where the drawn-in deaths sink: just over the floor of the bowl, five pixels under the rim. */
    public static Vec3 mouth(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 9.0 / 16, 0.5);
    }

    /** Where the crystal hangs, over the mouth. */
    public static Vec3 crystal(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 1 + 3.0 / 16, 0.5);
    }

    /** Whether a death at this point is within its reach: a cube of {@link #radius()} round the funnel. */
    public boolean covers(Vec3 at) {
        int radius = radius();
        Vec3 center = Vec3.atCenterOf(worldPosition);
        return Math.abs(at.x - center.x) <= radius + 0.5 && Math.abs(at.z - center.z) <= radius + 0.5
                && Math.abs(at.y - center.y) <= radius + 0.5;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Swallowing -----------------------------------------------------------------------------------------------------

    /** Pays for a death once (its experience and its drops ask both); false when there is no decay to pay with. */
    private boolean pay(ServerLevel level, LivingEntity dead) {
        if (paidFor == dead.getId()) {
            return true;
        }
        if (charges <= 0 && !drawDecay(level)) {
            setHungry(true);
            return false;
        }
        int dam = strength(FunnelRune.DAM);
        if (dam == 0 || level.getRandom().nextFloat() >= HolderRune.at(FunnelRune.THRIFT, dam)) {
            charges--;
        }
        paidFor = dead.getId();
        paidExperience = 0;
        setHungry(false);
        setChanged();
        return true;
    }

    /**
     * The death's experience goes into the crystal; returns what is left to drop (all of it when the funnel is hungry,
     * the overflow when the crystal is full).
     */
    public int swallowExperience(ServerLevel level, LivingEntity dead, int points) {
        if (points <= 0 || !pay(level, dead)) {
            return points;
        }
        int radiance = strength(FunnelRune.RADIANCE);
        int gained = radiance > 0 ? Math.round(points * HolderRune.at(FunnelRune.EXPERIENCE, radiance)) : points;
        // An Experience Condenser in reach gets it first; the crystal keeps what the condenser has no room for.
        XpCondenserBlockEntity condenser = XpCondenserBlockEntity.INDEX.covering(level, crystal(worldPosition));
        int poured = condenser == null ? 0 : condenser.fill(level, gained, crystal(worldPosition));
        int taken = Math.min(gained - poured, EXPERIENCE_CAP - experience);
        experience += taken;
        paidExperience += taken + poured;
        changed();
        int rest = gained - poured - taken;
        return rest <= 0 ? 0 : Math.min(points, rest);
    }

    /**
     * The death's drops: what fits goes into the container below or is held, and leaves {@code drops} (those items never
     * enter the world). Returns false when the funnel is hungry and swallowed nothing.
     */
    public boolean swallow(ServerLevel level, LivingEntity dead, Collection<ItemEntity> drops) {
        if (drops.isEmpty() && paidFor != dead.getId() || !pay(level, dead)) {
            return false;
        }
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemEntity drop : drops) {
            stacks.add(drop.getItem().copy());
        }
        enrich(level, dead, stacks);
        int sponge = strength(FunnelRune.SPONGE);
        if (sponge > 0 && level.getRandom().nextFloat() < HolderRune.at(FunnelRune.SOAK, sponge)) {
            soak(level, stacks);
        }
        List<ItemStack> icons = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (icons.size() < ICONS && !stack.isEmpty()) {
                icons.add(stack.copyWithCount(1));
            }
        }
        // The drops now live only as stacks: what nothing takes falls where the creature died.
        drops.clear();
        for (ItemStack stack : stacks) {
            ItemStack rest = store(level, stack);
            if (!rest.isEmpty()) {
                drops.add(new ItemEntity(level, dead.getX(), dead.getY(), dead.getZ(), rest));
            }
        }
        Vec3 from = dead.position().add(0, dead.getBbHeight() * 0.5, 0);
        Vec3 at = mouth(worldPosition);
        level.playSound(null, at.x, at.y, at.z, ModSounds.FUNNEL_SWALLOW.get(), SoundSource.BLOCKS, 0.45F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        FunnelPayload payload = new FunnelPayload(worldPosition, from, icons, paidExperience);
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(worldPosition), false)) {
            ModNetwork.sendToPlayer(player, payload);
        }
        paidFor = -1;
        paidExperience = 0;
        changed();
        return true;
    }

    /**
     * Sponge: what the drops are made of (their essences, as an alembic would boil them) soaks into the first jar beside
     * the funnel with room; the drops themselves stay. Returns the millibuckets soaked.
     */
    public int soak(ServerLevel level, List<ItemStack> stacks) {
        EssenceMix mix = EssenceMix.EMPTY;
        for (ItemStack stack : stacks) {
            com.pockyl.hextale.research.ItemEssences essences = com.pockyl.hextale.research.ResearchItems.of(stack);
            if (essences != null) {
                mix = mix.plus(EssenceMix.boiled(essences, 1).times(stack.getCount()));
            }
        }
        if (mix.isEmpty()) {
            return 0;
        }
        for (com.pockyl.hextale.block.EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            int soaked = jar.tank().fill(mix, false);
            if (soaked > 0) {
                jar.setChanged();
                Vec3 at = mouth(worldPosition);
                ToolEffects.puff(level, at, FunnelRune.SPONGE.rune(), 8, 0.3);
                ToolEffects.sound(level, at, ModSounds.ESSENCE_DRAW.get(), 0.4F, 0.8F);
                return soaked;
            }
        }
        return 0;
    }

    /** Spoils: one more of a stackable drop now and then; Engraving: now and then the creature's head. */
    void enrich(ServerLevel level, LivingEntity dead, List<ItemStack> stacks) {
        RandomSource random = level.getRandom();
        int spoils = strength(FunnelRune.SPOILS);
        if (spoils > 0) {
            for (ItemStack stack : stacks) {
                // Like Looting: never the creature's gear.
                if (!stack.isDamageableItem() && !stack.isEnchanted() && stack.getMaxStackSize() > 1
                        && stack.getCount() < stack.getMaxStackSize() && random.nextFloat() < HolderRune.at(FunnelRune.MORE, spoils)) {
                    stack.grow(1);
                }
            }
        }
        int engraving = strength(FunnelRune.ENGRAVING);
        Item head = engraving > 0 ? ToolEffects.headOf(dead.getType()) : null;
        if (head != null && random.nextFloat() < HolderRune.at(FunnelRune.HEAD, engraving)) {
            stacks.add(new ItemStack(head));
        }
    }

    /** Into the container below, else held; returns what neither took. */
    private ItemStack store(ServerLevel level, ItemStack stack) {
        ItemStack rest = passDown(level, stack);
        for (ItemStack waiting : held) {
            if (rest.isEmpty()) {
                break;
            }
            if (ItemStack.isSameItemSameComponents(waiting, rest) && waiting.getCount() < waiting.getMaxStackSize()) {
                int moved = Math.min(rest.getCount(), waiting.getMaxStackSize() - waiting.getCount());
                waiting.grow(moved);
                rest.shrink(moved);
            }
        }
        if (!rest.isEmpty() && held.size() < HELD_STACKS) {
            held.add(rest);
            wait = PASS_EVERY;
            return ItemStack.EMPTY;
        }
        return rest;
    }

    /** Puts as much as the block below takes into it (a furnace sorts it like a pylon would); returns the rest. */
    private ItemStack passDown(ServerLevel level, ItemStack stack) {
        BlockPos below = worldPosition.below();
        if (stack.isEmpty() || level.getCapability(Capabilities.Item.BLOCK, below, Direction.UP) == null) {
            return stack;
        }
        int moved = PylonInsert.insert(level, below, Direction.UP, ItemResource.of(stack), stack.getCount(), null, null);
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** Draws decay out of the nearest jar that holds some; false when none does. */
    private boolean drawDecay(ServerLevel level) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            int taken = Math.min(DRAW_MB, jar.tank().contents().get(Essence.DECAY));
            if (taken <= 0) {
                continue;
            }
            jar.tank().remove(EssenceMix.pure(Essence.DECAY, taken));
            jar.setChanged();
            charges += taken * DEATHS_PER_MB;
            drewFrom = jar.getBlockPos();
            drewAt = level.getGameTime();
            Vec3 mouth = JarReach.mouth(drewFrom);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 0.8F);
            changed();
            return true;
        }
        return false;
    }

    private void setHungry(boolean value) {
        if (hungry != value) {
            hungry = value;
            changed();
        }
    }

    // ---- Work -----------------------------------------------------------------------------------------------------------

    /** Only while it holds something: now and then it tries to pass it down again. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, GreedyFunnelBlockEntity funnel) {
        if (funnel.held.isEmpty() || --funnel.wait > 0) {
            return;
        }
        funnel.wait = PASS_EVERY;
        ServerLevel server = (ServerLevel) level;
        boolean moved = false;
        for (Iterator<ItemStack> it = funnel.held.iterator(); it.hasNext(); ) {
            ItemStack stack = it.next();
            ItemStack rest = funnel.passDown(server, stack);
            if (rest.getCount() != stack.getCount()) {
                moved = true;
                if (rest.isEmpty()) {
                    it.remove();
                } else {
                    stack.setCount(rest.getCount());
                }
            }
        }
        if (moved) {
            funnel.changed();
        }
    }

    /** The crystal's experience goes to the player. */
    public int release(ServerPlayer player) {
        int points = experience;
        if (points > 0) {
            experience = 0;
            player.giveExperiencePoints(points);
            Vec3 at = crystal(worldPosition);
            player.level().playSound(null, at.x, at.y, at.z, ModSounds.FUNNEL_RELEASE.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            changed();
        }
        return points;
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

    /** The funnel is broken: what it held falls out and the crystal lets its experience go. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) {
            for (ItemStack stack : held) {
                Containers.dropItemStack(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
            held.clear();
            if (experience > 0) {
                ExperienceOrb.award(server, Vec3.atCenterOf(pos), experience);
                experience = 0;
            }
        }
    }

    // ---- Client ---------------------------------------------------------------------------------------------------------

    /** Client: a death is being drawn in. */
    public void show(FunnelPayload payload, long now) {
        shown.add(new Shown(payload.from(), payload.icons(), payload.experience(), now, (int) (now * 31 + shown.size())));
    }

    /** Client tick: the vortex in the bowl spins up while deaths are drawn in; old ones are forgotten. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, GreedyFunnelBlockEntity funnel) {
        long now = level.getGameTime();
        funnel.shown.removeIf(death -> now - death.start() > com.pockyl.hextale.farm.FunnelTimeline.TOTAL);
        float target = funnel.hungry ? 0.6F : funnel.shown.isEmpty() ? 3F : 18F;
        funnel.vortexSpeed += (target - funnel.vortexSpeed) * 0.08F;
        funnel.vortexBefore = funnel.vortex;
        funnel.vortex += funnel.vortexSpeed;
        if (funnel.vortex > 360) {
            funnel.vortex -= 360;
            funnel.vortexBefore -= 360;
        }
        com.pockyl.hextale.farm.FunnelTimeline.particles(level, funnel, now);
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.FUNNEL_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.FUNNEL_RUNES.get(), runes);
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
        hungry = input.getBooleanOr("hungry", false);
        experience = input.getIntOr("experience", 0);
        held.clear();
        input.read("held", ItemStack.CODEC.listOf()).ifPresent(held::addAll);
        heldCount = input.getIntOr("held_count", held.size());
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
        output.putBoolean("hungry", hungry);
        output.putInt("experience", experience);
        if (!held.isEmpty()) {
            output.store("held", ItemStack.CODEC.listOf(), List.copyOf(held));
        }
        if (drewFrom != null) {
            output.store("drew_from", BlockPos.CODEC, drewFrom);
            output.putLong("drew_at", drewAt);
        }
    }

    /** The clients need the runes, the decay, the crystal's experience and whether it is full - not the held stacks' items. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove("held");
        tag.putInt("held_count", held.size());
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
