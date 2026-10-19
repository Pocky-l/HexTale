package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModSounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Crucible: a cauldron woken with hextech dust, the first vessel of chemtech. It holds water over a heat source;
 * the water warms by the heat's strength and cools without it. Only boiling water takes things in: bottles and jars of
 * essence pour into it, items dropped or put in sink and dissolve. What it makes of them comes later - for now it keeps
 * every essence and item that went in. Boiling water slowly evaporates.
 */
public class CrucibleBlockEntity extends BlockEntity {
    public static final int WATER_CAPACITY = 1000;
    public static final int BUCKET = 1000;
    public static final int WATER_BOTTLE = 250;
    /** Essence it holds: as much as a jar. */
    public static final int ESSENCE_CAPACITY = 256;
    /** Different things it can hold dissolved. */
    public static final int MAX_KINDS = 9;
    public static final float BOILING = 100;
    /** Degrees a tick per point of heat, and degrees a tick it cools without heat. */
    public static final float RISE = 0.1F;
    public static final float FALL = 0.15F;
    /** Boiling water loses a millibucket every this many ticks. */
    public static final int EVAPORATE_TICKS = 8;
    /** How often the crucible looks for items lying in it, and how often the clients hear of its temperature. */
    private static final int SCAN_TICKS = 5;
    private static final int SYNC_TICKS = 10;
    /** The water's surface: from the bottom of the bowl up to just under its rim, and the bowl's inside. */
    public static final float BOTTOM = 4F / 16;
    public static final float BRIM = 14F / 16;
    public static final float INSIDE_MIN = 3F / 16;
    public static final float INSIDE_MAX = 13F / 16;
    private static final int STEAM = 0xE8EEF2;

    /** How the water stands, by its temperature. */
    public enum Phase {
        COLD(0),
        WARMING(40),
        SIMMERING(75),
        BOILING(100);

        private final float from;

        Phase(float from) {
            this.from = from;
        }

        public static Phase of(float temperature) {
            Phase phase = COLD;
            for (Phase p : values()) {
                if (temperature >= p.from) {
                    phase = p;
                }
            }
            return phase;
        }

        public Component describe() {
            return Component.translatable("block.hextale.crucible.phase." + name().toLowerCase(Locale.ROOT));
        }
    }

    private int water;
    private float temperature;
    private EssenceMix essence = EssenceMix.EMPTY;
    private final List<ItemStack> dissolved = new ArrayList<>();
    private boolean dirty;
    /** Client animations: when essence was poured in last and its colour, when an item sank and which. */
    private long pouredAt = -1000;
    private int pouredColor;
    private long sankAt = -1000;
    private ItemStack sank = ItemStack.EMPTY;

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    /** How strongly the block under the crucible heats it: fire and campfires 2, their soul kin 3, magma 2, lava 4. */
    public static int heat(BlockState below) {
        if (below.is(Blocks.CAMPFIRE) || below.is(Blocks.SOUL_CAMPFIRE)) {
            return below.getValue(CampfireBlock.LIT) ? below.is(Blocks.SOUL_CAMPFIRE) ? 3 : 2 : 0;
        }
        if (below.is(Blocks.SOUL_FIRE)) {
            return 3;
        }
        if (below.is(BlockTags.FIRE) || below.is(Blocks.MAGMA_BLOCK)) {
            return 2;
        }
        return below.is(Blocks.LAVA) ? 4 : 0;
    }

    public int heat() {
        return level == null ? 0 : heat(level.getBlockState(worldPosition.below()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        crucible.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        int heat = heat();
        Phase before = phase();
        float next;
        if (water <= 0) {
            next = Math.max(0, temperature - FALL * 4);
        } else if (heat > 0) {
            next = Math.min(BOILING, temperature + RISE * heat);
        } else {
            next = Math.max(0, temperature - FALL);
        }
        if (next != temperature) {
            temperature = next;
            dirty = true;
        }
        long now = level.getGameTime();
        if (boiling() && now % EVAPORATE_TICKS == 0) {
            water--;
            dirty = true;
        }
        if (now % SCAN_TICKS == 0 && water > 0) {
            takeItemsLyingIn(level);
        }
        if (phase() != before || dirty && now % SYNC_TICKS == 0) {
            dirty = false;
            setChanged();
            sync();
        }
    }

    /** Items lying in the water: boiling, it dissolves them; otherwise they bob up and over the rim. */
    private void takeItemsLyingIn(ServerLevel level) {
        AABB inside = new AABB(worldPosition.getX() + INSIDE_MIN, worldPosition.getY() + BOTTOM, worldPosition.getZ() + INSIDE_MIN,
                worldPosition.getX() + INSIDE_MAX, worldPosition.getY() + surface() + 0.1, worldPosition.getZ() + INSIDE_MAX);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, inside, ItemEntity::isAlive)) {
            ItemStack stack = entity.getItem();
            int taken = boiling() ? dissolve(stack, stack.getCount()) : 0;
            if (taken > 0) {
                stack.shrink(taken);
                if (stack.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(stack);
                }
            } else if (entity.getDeltaMovement().y <= 0.05) {
                // Cold water, or no room: the crucible will not take it - it bobs back up and out.
                entity.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.25, 0.35, (level.getRandom().nextDouble() - 0.5) * 0.25);
                level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3F, 1.4F);
            }
        }
    }

    /** Height of the water's surface in the block (the bottom of the bowl when empty). */
    public float surface() {
        return BOTTOM + (BRIM - BOTTOM) * Math.min(1, water / (float) WATER_CAPACITY);
    }

    public int water() {
        return water;
    }

    public float temperature() {
        return temperature;
    }

    public Phase phase() {
        return water <= 0 ? Phase.COLD : Phase.of(temperature);
    }

    /** The water rolls: only now does it take anything in. */
    public boolean boiling() {
        return water > 0 && temperature >= BOILING && heat() > 0;
    }

    public EssenceMix essence() {
        return essence;
    }

    /** What went in and dissolved (a copy). */
    public List<ItemStack> dissolved() {
        return dissolved.stream().map(ItemStack::copy).toList();
    }

    public int dissolvedCount() {
        return dissolved.stream().mapToInt(ItemStack::getCount).sum();
    }

    public long pouredAt() {
        return pouredAt;
    }

    public int pouredColor() {
        return pouredColor;
    }

    public long sankAt() {
        return sankAt;
    }

    public ItemStack sank() {
        return sank;
    }

    // ---- Filling and emptying ------------------------------------------------------------------------------------

    /** @return the water taken (all of {@code amount} or nothing) */
    public int fillWater(int amount, boolean simulate) {
        if (water + amount > WATER_CAPACITY) {
            return 0;
        }
        if (!simulate) {
            water += amount;
            changed();
            playAt(SoundEvents.BUCKET_EMPTY, 0.8F, 1.0F);
        }
        return amount;
    }

    /** Carried over from a water cauldron that woke into this crucible. */
    public void setWater(int water) {
        this.water = Mth.clamp(water, 0, WATER_CAPACITY);
        changed();
    }

    /**
     * Pours essence in, as much as there is room for; boiling water only.
     *
     * @return what was taken
     */
    public EssenceMix pour(EssenceMix offer) {
        if (!boiling() || offer.isEmpty()) {
            return EssenceMix.EMPTY;
        }
        EssenceMix taken = offer.part(Math.min(offer.total(), ESSENCE_CAPACITY - essence.total()));
        if (taken.isEmpty()) {
            return EssenceMix.EMPTY;
        }
        essence = essence.plus(taken);
        pouredColor = taken.color();
        pouredAt = level == null ? 0 : level.getGameTime();
        playAt(ModSounds.TANK_GLUG.get(), 0.8F, 0.8F);
        changed();
        return taken;
    }

    /**
     * Dissolves up to {@code count} of the item in boiling water.
     *
     * @return how many were taken
     */
    public int dissolve(ItemStack stack, int count) {
        if (!boiling() || stack.isEmpty() || count <= 0) {
            return 0;
        }
        ItemStack same = null;
        for (ItemStack held : dissolved) {
            if (ItemStack.isSameItemSameComponents(held, stack)) {
                same = held;
            }
        }
        if (same == null) {
            if (dissolved.size() >= MAX_KINDS) {
                return 0;
            }
            dissolved.add(stack.copyWithCount(count));
        } else {
            same.grow(count);
        }
        sank = stack.copyWithCount(1);
        sankAt = level == null ? 0 : level.getGameTime();
        playAt(ModSounds.CRUCIBLE_DISSOLVE.get(), 0.8F, 0.9F + (level == null ? 0 : level.getRandom().nextFloat() * 0.2F));
        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
            server.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.05, at.z, 8, 0.15, 0.02, 0.15, 0.05);
            server.sendParticles(ParticleTypes.WHITE_SMOKE, at.x, at.y + 0.1, at.z, 4, 0.12, 0.05, 0.12, 0.01);
        }
        changed();
        return count;
    }

    /** Takes a bucket of water out (contents and all are lost with it); true if there was a bucket of water. */
    public boolean takeBucket() {
        if (water < BUCKET) {
            return false;
        }
        empty();
        playAt(SoundEvents.BUCKET_FILL, 0.8F, 1.0F);
        return true;
    }

    /** Pours everything out on the ground: water, essence and whatever dissolved in it. */
    public void pourOut() {
        if (water <= 0 && essence.isEmpty() && dissolved.isEmpty()) {
            return;
        }
        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
            server.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 30, 0.35, 0.1, 0.35, 0.1);
            if (temperature > Phase.SIMMERING.from) {
                server.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.2, at.z, 6, 0.25, 0.1, 0.25, 0.02);
            }
        }
        playAt(SoundEvents.GENERIC_SPLASH, 0.6F, 0.9F);
        empty();
    }

    private void empty() {
        water = 0;
        temperature = 0;
        essence = EssenceMix.EMPTY;
        dissolved.clear();
        changed();
    }

    private void playAt(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
    }

    private void changed() {
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Client: steam and bubbles ----------------------------------------------------------------------------------

    /** Steam over warm water, bubbles breaking on a boiling surface. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity crucible) {
        Phase phase = crucible.phase();
        if (phase == Phase.COLD) {
            return;
        }
        var random = level.getRandom();
        double y = pos.getY() + crucible.surface();
        double x = pos.getX() + INSIDE_MIN + random.nextDouble() * (INSIDE_MAX - INSIDE_MIN);
        double z = pos.getZ() + INSIDE_MIN + random.nextDouble() * (INSIDE_MAX - INSIDE_MIN);
        int steamEvery = switch (phase) {
            case WARMING -> 30;
            case SIMMERING -> 10;
            default -> 3;
        };
        if (random.nextInt(steamEvery) == 0) {
            level.addParticle(ParticleTypes.WHITE_SMOKE, x, y + 0.05, z, 0, 0.02 + random.nextDouble() * 0.02, 0);
        }
        if (phase == Phase.BOILING) {
            if (random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.BUBBLE_POP, x, y + 0.02, z, 0, 0.02, 0);
            }
            if (random.nextInt(14) == 0) {
                level.addParticle(ParticleTypes.SPLASH, x, y + 0.05, z, 0, 0.1, 0);
            }
            if (random.nextInt(40) == 0) {
                level.playLocalSound(pos, ModSounds.CRUCIBLE_BOIL.get(), SoundSource.BLOCKS, 0.22F + random.nextFloat() * 0.08F,
                        0.9F + random.nextFloat() * 0.2F, false);
            }
        } else if (phase == Phase.SIMMERING && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, x, y + 0.02, z, 0, 0.01, 0);
        }
    }

    public static int steamColor() {
        return STEAM;
    }

    // ---- Saving -----------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        water = Mth.clamp(input.getIntOr("water", 0), 0, WATER_CAPACITY);
        temperature = Mth.clamp(input.getFloatOr("temperature", 0), 0, BOILING);
        essence = input.read("essence", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        dissolved.clear();
        dissolved.addAll(input.read("dissolved", ItemStack.CODEC.listOf()).orElse(List.of()));
        pouredAt = input.getLongOr("poured_at", -1000);
        pouredColor = input.getIntOr("poured_color", 0);
        sankAt = input.getLongOr("sank_at", -1000);
        sank = input.read("sank", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("water", water);
        output.putFloat("temperature", temperature);
        if (!essence.isEmpty()) {
            output.store("essence", EssenceMix.CODEC, essence);
        }
        if (!dissolved.isEmpty()) {
            output.store("dissolved", ItemStack.CODEC.listOf(), List.copyOf(dissolved));
        }
        // The last pour and the last item sunk, for the clients' animations (kept: they are small).
        output.putLong("poured_at", pouredAt);
        output.putInt("poured_color", pouredColor);
        output.putLong("sank_at", sankAt);
        if (!sank.isEmpty()) {
            output.store("sank", ItemStack.CODEC, sank);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
