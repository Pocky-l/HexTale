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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.PurifierRune;
import com.pockyl.hextale.thevoid.Healing;
import com.pockyl.hextale.thevoid.Thinning;

import org.jspecify.annotations.Nullable;

/**
 * A Hextech Purifier's state and work. Its hextech crystal draws the Thinning out of the land - every interval
 * {@link #PER_DRAW} out of the sickest chunk within its reach - and clouds over as it fills; at {@link #capacity()} it has
 * become a Clouded Crystal and the purifier rests until someone takes it out. The work costs light essence, drawn out of
 * a jar within {@link JarReach#RADIUS}: one mB is {@value #PER_MB} of the Thinning. A crystal taken out half full lets
 * what it held seep back into the land.
 *
 * <p>Hoppers and pylons put fresh crystals in through the top and sides and take clouded ones out from below.
 */
public class HextechPurifierBlockEntity extends BlockEntity implements WorldlyContainer {
    /** Thinning one crystal holds before it clouds. */
    public static final float HOLDS = 400;
    /** Thinning drawn at a time. */
    public static final float PER_DRAW = 2;
    /** Thinning drawn per mB of light essence. */
    public static final int PER_MB = 8;
    public static final int DRAW_MB = 4;
    /** Ticks a jar's stream shows after a draw. */
    public static final int STREAM_TICKS = 50;
    /** Ticks a Fertility blessing lasts on a chunk after a draw. */
    private static final int BLESS_TICKS = 600;
    private static final int[] SLOT = {0};
    /** The dark rising out of the land. */
    public static final int DARK = 0x2C0A26;
    public static final int PLUM = 0x7A2A6E;

    public enum Mode {
        /** No crystal in it. */
        EMPTY,
        /** Its crystal is clouded through: take it out. */
        CLOUDED,
        /** No light essence in the jars around. */
        UNLIT,
        /** The land within its reach is well. */
        CLEAN,
        WORKING
    }

    private ItemStack crystal = ItemStack.EMPTY;
    private float held;
    /** Thinning the light essence drawn so far still pays for. */
    private float light;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private Mode mode = Mode.EMPTY;
    private int wait;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;

    public HextechPurifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEXTECH_PURIFIER.get(), pos, state);
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    private int strength(PurifierRune rune) {
        return runes.strength(rune.rune());
    }

    /** Chunks every way it draws on. */
    public int radius() {
        return PurifierRune.radius(strength(PurifierRune.SHIFT));
    }

    public int interval() {
        return PurifierRune.interval(strength(PurifierRune.MOTOR));
    }

    /** Thinning the crystal holds before it clouds. */
    public float capacity() {
        int purification = strength(PurifierRune.PURIFICATION);
        return HOLDS * (1 + (purification > 0 ? HolderRune.at(PurifierRune.ROOM, purification) : 0));
    }

    // ---- State ----------------------------------------------------------------------------------------------------------

    public ItemStack crystal() {
        return crystal;
    }

    public Mode mode() {
        return mode;
    }

    public float held() {
        return held;
    }

    /** 0 for a fresh crystal, 1 for a clouded one. */
    public float clouding() {
        if (crystal.is(ModItems.CLOUDED_CRYSTAL.get())) {
            return 1;
        }
        return crystal.isEmpty() ? 0 : Mth.clamp(held / capacity(), 0, 1);
    }

    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** Where the crystal hangs, point down, from the canopy's hub over the font. */
    public static Vec3 crystalAt(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 8.5 / 16, 0.5);
    }

    /** Puts a fresh crystal in (one of the stack); false when it holds one already or it is no crystal. */
    public boolean insert(ItemStack stack) {
        if (!crystal.isEmpty() || !stack.is(ModItems.HEXTECH_CRYSTAL.get())) {
            return false;
        }
        crystal = stack.copyWithCount(1);
        held = 0;
        wait = 0;
        changed();
        return true;
    }

    /** Takes the crystal out; what a half-full one held seeps back into the land. */
    public ItemStack take() {
        ItemStack out = crystal;
        release();
        crystal = ItemStack.EMPTY;
        held = 0;
        mode = Mode.EMPTY;
        changed();
        return out;
    }

    private void release() {
        if (held > 0 && level instanceof ServerLevel server && crystal.is(ModItems.HEXTECH_CRYSTAL.get())) {
            LevelChunk chunk = server.getChunkAt(worldPosition);
            Thinning.set(server, chunk, Thinning.get(chunk) + held);
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void setMode(Mode next) {
        if (mode != next) {
            mode = next;
            changed();
        }
    }

    // ---- Work -----------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, HextechPurifierBlockEntity purifier) {
        if (--purifier.wait > 0) {
            return;
        }
        purifier.work((ServerLevel) level);
    }

    /** One round of work (the ticker calls it when its wait is over; tests call it straight away). */
    public void work(ServerLevel level) {
        if (crystal.isEmpty()) {
            setMode(Mode.EMPTY);
            wait = 40;
            return;
        }
        if (crystal.is(ModItems.CLOUDED_CRYSTAL.get())) {
            setMode(Mode.CLOUDED);
            wait = 40;
            return;
        }
        LevelChunk land = Thinning.sickestAround(level, worldPosition, radius());
        if (land == null) {
            setMode(Mode.CLEAN);
            wait = 100;
            return;
        }
        float want = Math.min(PER_DRAW, Math.min(Thinning.get(land), capacity() - held));
        if (light < want && !drawLight(level)) {
            setMode(Mode.UNLIT);
            wait = 40;
            return;
        }
        light = Math.max(0, light - want);
        float taken = Thinning.drain(level, land, want);
        held += taken;
        setMode(Mode.WORKING);
        wait = interval();
        int fertility = strength(PurifierRune.FERTILITY);
        if (fertility > 0) {
            Healing.bless(level, land, fertility, BLESS_TICKS);
        }
        shownDraw(level, land);
        if (held >= capacity() - 0.001F) {
            cloud(level);
        }
        changed();
    }

    /** The crystal is full of the Void: it becomes a Clouded Crystal. */
    private void cloud(ServerLevel level) {
        crystal = new ItemStack(ModItems.CLOUDED_CRYSTAL.get());
        held = 0;
        mode = Mode.CLOUDED;
        Vec3 at = crystalAt(worldPosition);
        level.sendParticles(ModParticles.ring(PLUM), at.x, at.y, at.z, 0, 1.4, 0, 0, 1);
        level.sendParticles(ModParticles.vapour(DARK), at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, worldPosition, ModSounds.PURIFIER_CLOUDED.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
    }

    /** The dark rises out of the drawn land and out of the ground around, into the crystal. */
    private void shownDraw(ServerLevel level, LevelChunk land) {
        RandomSource random = level.getRandom();
        Vec3 at = crystalAt(worldPosition);
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double reach = 2 + random.nextDouble() * 3.5;
            int x = Mth.floor(at.x + Math.cos(angle) * reach);
            int z = Mth.floor(at.z + Math.sin(angle) * reach);
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 0.1;
            // A void mote is sent where it ends, with the way back to where it starts.
            level.sendParticles(ModParticles.voidMote(i % 3 == 0 ? PLUM : DARK), at.x, at.y, at.z, 0, x + 0.5 - at.x, y - at.y,
                    z + 0.5 - at.z, 1);
        }
        ChunkPos chunk = land.getPos();
        int lx = chunk.getMinBlockX() + random.nextInt(16);
        int lz = chunk.getMinBlockZ() + random.nextInt(16);
        BlockPos ground = new BlockPos(lx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz), lz);
        if (!chunk.equals(ChunkPos.containing(worldPosition))) {
            level.sendParticles(ModParticles.vapour(DARK), ground.getX() + 0.5, ground.getY() + 0.2, ground.getZ() + 0.5, 4, 0.6, 0.1, 0.6, 0.02);
            level.sendParticles(ModParticles.glint(PLUM), ground.getX() + 0.5, ground.getY() + 0.4, ground.getZ() + 0.5, 2, 0.5, 0.3, 0.5, 0);
        }
        if (random.nextInt(3) == 0) {
            level.playSound(null, worldPosition, ModSounds.PURIFIER_DRAW.get(), SoundSource.BLOCKS, 0.45F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    private boolean drawLight(ServerLevel level) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            int taken = Math.min(DRAW_MB, jar.tank().contents().get(Essence.LIGHT));
            if (taken <= 0) {
                continue;
            }
            jar.tank().remove(EssenceMix.pure(Essence.LIGHT, taken));
            jar.setChanged();
            light += taken * PER_MB;
            drewFrom = jar.getBlockPos();
            drewAt = level.getGameTime();
            Vec3 mouth = JarReach.mouth(drewFrom);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 1.2F);
            return true;
        }
        return false;
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
        wait = 0;
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

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        release();
        held = 0;
        super.preRemoveSideEffects(pos, state);
    }

    // ---- Saving ---------------------------------------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.PURIFIER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.PURIFIER_RUNES.get(), runes);
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
        light = input.getFloatOr("light", 0);
        mode = Mode.values()[Mth.clamp(input.getIntOr("mode", 0), 0, Mode.values().length - 1)];
        drewFrom = input.read("drew_from", BlockPos.CODEC).orElse(null);
        drewAt = input.getLongOr("drew_at", -1000);
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
        output.putFloat("light", light);
        output.putInt("mode", mode.ordinal());
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
