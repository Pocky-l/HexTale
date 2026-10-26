package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.AnchorRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidMite;
import com.pockyl.hextale.thevoid.Voidling;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A Void Anchor's state. Its hextech crystal holds a breach: a breach counts the anchors around it ({@link TheVoid}) and
 * ties a thread of light to every one that holds a fresh crystal; six of them on every side close the ring and hold it.
 * Holding wears the crystal - it clouds a little with every moment ({@link #strain}), twice as fast while the breach
 * still fights, and with every mB the held breach eats - until it is a Clouded Crystal and the thread snaps. Swap it.
 *
 * <p>A held breach eats through its anchors: an anchor takes the unknown essence out of the jars around it (the waste no
 * one wants) and whatever siphons bring it, and passes it along its thread into the breach.
 */
public class VoidAnchorBlockEntity extends BlockEntity implements WorldlyContainer, EssenceNetwork.Receiver {
    /** How long a crystal holds a held breach before it clouds through (one point a tick). */
    public static final float HOLDS = 24000;
    /** Essence it keeps to feed the breach. */
    public static final int BUFFER = 500;
    /** Unknown essence taken out of a jar at a time. */
    public static final int DRAW_MB = 16;
    /** Essence passed into the breach per second. */
    public static final int FEED_MB = 40;
    /** Ticks a tie lasts without being renewed by the breach. */
    public static final int TIE_TICKS = 50;
    public static final int STREAM_TICKS = 50;
    private static final int[] SLOT = {0};
    public static final int COLD = 0x8A7CFF;
    public static final int HOT = 0xF23CE6;

    private ItemStack crystal = ItemStack.EMPTY;
    private float held;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private EssenceMix buffer = EssenceMix.EMPTY;
    private @Nullable UUID breach;
    private Vec3 breachAt = Vec3.ZERO;
    private float breachSize;
    private long tiedUntil = -1;
    private boolean tied;
    private boolean subdued;
    private long ateAt = -1000;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;
    private int wait;

    public VoidAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOID_ANCHOR.get(), pos, state);
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    private int strength(AnchorRune rune) {
        return runes.strength(rune.rune());
    }

    // ---- State ----------------------------------------------------------------------------------------------------------

    public ItemStack crystal() {
        return crystal;
    }

    /** Whether it can hold a breach: a fresh hextech crystal is in it. */
    public boolean holds() {
        return crystal.is(ModItems.HEXTECH_CRYSTAL.get());
    }

    /** 0 fresh, 1 clouded through. */
    public float clouding() {
        if (crystal.is(ModItems.CLOUDED_CRYSTAL.get())) {
            return 1;
        }
        return crystal.isEmpty() ? 0 : Mth.clamp(held / HOLDS, 0, 1);
    }

    public boolean tied() {
        return tied;
    }

    public boolean subdued() {
        return subdued;
    }

    public Vec3 breachAt() {
        return breachAt;
    }

    public float breachSize() {
        return breachSize;
    }

    public long ateAt() {
        return ateAt;
    }

    public EssenceMix buffer() {
        return buffer;
    }

    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** Where the crystal hangs: in the window through the stone. */
    public static Vec3 crystalAt(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 8.0 / 16, 0.5);
    }

    /** Where its thread meets the breach: on the breach's edge, on the anchor's side. */
    public Vec3 threadEnd() {
        Vec3 from = crystalAt(worldPosition);
        Vec3 way = from.subtract(breachAt);
        double length = way.length();
        return length < 1.0E-3 ? breachAt : breachAt.add(way.scale(Math.min(length * 0.8, breachSize * 0.42) / length));
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public boolean insert(ItemStack stack) {
        if (!crystal.isEmpty() || !stack.is(ModItems.HEXTECH_CRYSTAL.get())) {
            return false;
        }
        crystal = stack.copyWithCount(1);
        held = 0;
        changed();
        return true;
    }

    public ItemStack take() {
        ItemStack out = crystal;
        crystal = ItemStack.EMPTY;
        held = 0;
        untie();
        changed();
        return out;
    }

    // ---- Holding ----------------------------------------------------------------------------------------------------------

    /**
     * The breach counts this anchor: it is tied to it until {@link #TIE_TICKS} pass without the breach counting it again,
     * and holding wears its crystal by {@code strain}.
     */
    public void tie(ServerLevel level, TheVoid breach, boolean subdued, float strain) {
        if (!holds()) {
            return;
        }
        boolean fresh = !tied;
        this.breach = breach.getUUID();
        this.breachAt = breach.position();
        this.breachSize = breach.size();
        this.tiedUntil = level.getGameTime() + TIE_TICKS;
        boolean changedState = fresh || this.subdued != subdued;
        this.tied = true;
        this.subdued = subdued;
        if (fresh) {
            Vec3 at = crystalAt(worldPosition);
            level.playSound(null, at.x, at.y, at.z, ModSounds.ANCHOR_TIE.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        }
        strain(level, strain);
        if (changedState) {
            changed();
        }
    }

    /** Holding wears the crystal; clouded through, it lets go. */
    public void strain(ServerLevel level, float amount) {
        if (!holds() || amount <= 0) {
            return;
        }
        int dam = strength(AnchorRune.DAM);
        held += amount * (1 - (dam > 0 ? HolderRune.at(AnchorRune.THRIFT, dam) : 0));
        if (held >= HOLDS) {
            crystal = new ItemStack(ModItems.CLOUDED_CRYSTAL.get());
            held = 0;
            Vec3 at = crystalAt(worldPosition);
            level.sendParticles(ModParticles.ring(HOT), at.x, at.y, at.z, 0, 1.2, 0, 0, 1);
            level.sendParticles(ModParticles.vapour(0x2C0A26), at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, at.x, at.y, at.z, ModSounds.ANCHOR_SNAP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            untie();
            changed();
        } else if (level.getGameTime() % 100 == 0) {
            // The client shows how clouded it is: tell it now and then, not every tick.
            changed();
        }
    }

    private void untie() {
        if (tied) {
            tied = false;
            subdued = false;
            tiedUntil = -1;
        }
    }

    // ---- Work -------------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, VoidAnchorBlockEntity anchor) {
        ServerLevel server = (ServerLevel) level;
        if (anchor.tied && level.getGameTime() > anchor.tiedUntil) {
            anchor.untie();
            anchor.changed();
        }
        if (--anchor.wait > 0) {
            return;
        }
        anchor.wait = 20;
        if (anchor.tied && anchor.subdued) {
            anchor.feed(server);
        }
        anchor.guard(server);
    }

    /** Takes the waste out of the jars around and passes what it holds along the thread into the held breach. */
    private void feed(ServerLevel level) {
        if (buffer.total() < BUFFER) {
            for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
                int unknown = jar.tank().contents().unknown();
                if (unknown <= 0) {
                    continue;
                }
                int taken = Math.min(Math.min(DRAW_MB, unknown), BUFFER - buffer.total());
                EssenceMix part = EssenceMix.unknown(taken);
                jar.tank().remove(part);
                jar.setChanged();
                buffer = buffer.plus(part);
                drewFrom = jar.getBlockPos();
                drewAt = level.getGameTime();
                break;
            }
        }
        if (buffer.isEmpty() || breach == null || !(level.getEntity(breach) instanceof TheVoid target)) {
            return;
        }
        int amount = Math.min(FEED_MB, buffer.total());
        EssenceMix meal = buffer.part(amount);
        float unknownShare = meal.total() == 0 ? 0 : meal.unknown() / (float) meal.total();
        if (target.eat(level, meal.total(), unknownShare)) {
            buffer = buffer.minus(meal);
            ateAt = level.getGameTime();
            changed();
        }
    }

    /** Its runes keep the Void's creatures near it in check. */
    private void guard(ServerLevel level) {
        int chain = strength(AnchorRune.CHAIN_LIGHTNING);
        int banish = strength(AnchorRune.BANISHING);
        if ((chain == 0 && banish == 0) || !holds() || level.getGameTime() % 40 >= 20) {
            return;
        }
        Vec3 at = crystalAt(worldPosition);
        List<LivingEntity> spawn = level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(AnchorRune.REACH),
                living -> living.isAlive() && (living instanceof Voidling || living instanceof VoidMite));
        RandomSource random = level.getRandom();
        for (LivingEntity creature : spawn) {
            Vec3 to = creature.position().add(0, creature.getBbHeight() * 0.5, 0);
            if (chain > 0) {
                creature.hurtServer(level, level.damageSources().magic(), HolderRune.at(AnchorRune.ARC, chain));
                for (int i = 0; i < 6; i++) {
                    Vec3 p = at.lerp(to, i / 6.0).add((random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.3,
                            (random.nextDouble() - 0.5) * 0.3);
                    level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            if (banish > 0) {
                creature.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, banish - 1, false, true));
                creature.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
                level.sendParticles(ModParticles.glint(0xFFF2C8), to.x, to.y, to.z, 3, 0.3, 0.3, 0.3, 0);
            }
        }
    }

    /** The anchors within {@code radius} blocks of {@code center}: walks the block entities of the chunks it touches. */
    public static List<VoidAnchorBlockEntity> around(Level level, BlockPos center, int radius) {
        List<VoidAnchorBlockEntity> out = new ArrayList<>();
        for (int cx = (center.getX() - radius) >> 4; cx <= (center.getX() + radius) >> 4; cx++) {
            for (int cz = (center.getZ() - radius) >> 4; cz <= (center.getZ() + radius) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (entity instanceof VoidAnchorBlockEntity anchor && !anchor.isRemoved()
                            && Math.abs(entity.getBlockPos().getX() - center.getX()) <= radius
                            && Math.abs(entity.getBlockPos().getZ() - center.getZ()) <= radius
                            && Math.abs(entity.getBlockPos().getY() - center.getY()) <= radius) {
                        out.add(anchor);
                    }
                }
            }
        }
        out.sort((a, b) -> Long.compare(a.getBlockPos().asLong(), b.getBlockPos().asLong()));
        return out;
    }

    // ---- Client ---------------------------------------------------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, VoidAnchorBlockEntity anchor) {
        if (!anchor.tied) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 from = crystalAt(pos);
        Vec3 end = anchor.threadEnd();
        // What it feeds the breach runs along the thread as dark motes swallowed at the far end.
        boolean eating = level.getGameTime() - anchor.ateAt < 30;
        if (eating ? random.nextInt(2) == 0 : random.nextInt(anchor.subdued ? 30 : 12) == 0) {
            Vec3 start = from.add((random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2, (random.nextDouble() - 0.5) * 0.2);
            int colour = eating ? (random.nextInt(3) == 0 ? HOT : EssenceMix.UNKNOWN_COLOR) : anchor.subdued ? COLD : HOT;
            level.addParticle(ModParticles.voidMote(colour), end.x, end.y, end.z, start.x - end.x, start.y - end.y, start.z - end.z);
        }
    }

    // ---- Essence from siphons -------------------------------------------------------------------------------------------

    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (!holds()) {
            return 0;
        }
        int room = BUFFER - buffer.total();
        if (room <= 0 || offer.isEmpty()) {
            return 0;
        }
        EssenceMix taken = offer.total() <= room ? offer : offer.part(room);
        if (!simulate) {
            buffer = buffer.plus(taken);
            setChanged();
        }
        return taken.total();
    }

    // ---- Container: the crystal, for hoppers and pylons -----------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && crystal.isEmpty() && stack.is(ModItems.HEXTECH_CRYSTAL.get());
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && stack.is(ModItems.CLOUDED_CRYSTAL.get());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return crystal.isEmpty() && stack.is(ModItems.HEXTECH_CRYSTAL.get());
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return crystal.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return crystal;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return count > 0 && !crystal.isEmpty() ? take() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = crystal;
        crystal = ItemStack.EMPTY;
        held = 0;
        untie();
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            take();
            return;
        }
        crystal = stack.copyWithCount(1);
        held = 0;
        changed();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        crystal = ItemStack.EMPTY;
        held = 0;
        changed();
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.ANCHOR_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.ANCHOR_RUNES.get(), runes);
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
        crystal = input.read("crystal", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        held = input.getFloatOr("held", 0);
        buffer = input.read("buffer", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        breach = input.read("breach", UUIDUtil.CODEC).orElse(null);
        tied = input.getBooleanOr("tied", false);
        subdued = input.getBooleanOr("subdued", false);
        breachAt = new Vec3(input.getDoubleOr("bx", 0), input.getDoubleOr("by", 0), input.getDoubleOr("bz", 0));
        breachSize = input.getFloatOr("bsize", 3);
        ateAt = input.getLongOr("ate_at", -1000);
        drewFrom = input.read("drew_from", BlockPos.CODEC).orElse(null);
        drewAt = input.getLongOr("drew_at", -1000);
        // A tie never outlives a reload: the breach renews it within a second.
        tiedUntil = tied && level != null ? level.getGameTime() + TIE_TICKS : tiedUntil;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        if (!crystal.isEmpty()) {
            output.store("crystal", ItemStack.OPTIONAL_CODEC, crystal);
        }
        output.putFloat("held", held);
        if (!buffer.isEmpty()) {
            output.store("buffer", EssenceMix.CODEC, buffer);
        }
        if (breach != null) {
            output.store("breach", UUIDUtil.CODEC, breach);
        }
        output.putBoolean("tied", tied);
        output.putBoolean("subdued", subdued);
        output.putDouble("bx", breachAt.x);
        output.putDouble("by", breachAt.y);
        output.putDouble("bz", breachAt.z);
        output.putFloat("bsize", breachSize);
        output.putLong("ate_at", ateAt);
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
