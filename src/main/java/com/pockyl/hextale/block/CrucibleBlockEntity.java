package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
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
 * every essence and item that went in. It holds {@value #CAPACITY} mB: water, essence and every dissolved item
 * ({@value #ITEM_VOLUME} mB each); more brims over the rim and spills liquid in proportion. The water never boils off by
 * itself. Stone thrown into a crucible holding magic essence soaks it up and comes back out as magic stone
 * ({@value #MAGIC_PER_STONE} mB of magic a block), boiling or not.
 */
public class CrucibleBlockEntity extends BlockEntity {
    /** What it holds in all: water, essence and the dissolved items. */
    public static final int CAPACITY = 1500;
    /** Room a dissolved item takes. */
    public static final int ITEM_VOLUME = 30;
    /** Items never crowd out the last of the liquid. */
    private static final int MIN_LIQUID = 200;
    public static final int BUCKET = 1000;
    public static final int WATER_BOTTLE = 250;
    /** Different things it can hold dissolved. */
    public static final int MAX_KINDS = 16;
    public static final float BOILING = 100;
    /** Degrees a tick per point of heat, and degrees a tick it cools without heat. */
    public static final float RISE = 0.1F;
    public static final float FALL = 0.15F;
    /** Magic essence a block of stone soaks up to become magic stone. */
    public static final int MAGIC_PER_STONE = 5;
    /** Ticks a soaking stone lies under the surface before it rises out as magic stone. */
    public static final int SOAK_TICKS = 24;
    /** How often the crucible looks for items over it, and how often the clients hear of its temperature. */
    private static final int SCAN_TICKS = 2;
    /** Items this far over the crucible (in blocks above it) are drawn into boiling water. */
    private static final double CAPTURE_HEIGHT = 2.5;
    /** Sneak-use boils the water dry in this many ticks. */
    public static final int BOIL_DRY_TICKS = 30;
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
    /** Where the last item was caught, in blocks above the crucible's floor: it falls from there into the water. */
    private float sankFrom = 1;
    /** When the water started boiling dry (sneak-use), and how much there was. */
    private long dryingAt = -1000;
    private int dryingFrom;
    /** Stones soaking in the magic now, and when they rise out as magic stone. */
    private int soaking;
    private long soakedAt;
    /** Magic stone just thrown out: no crucible catches it again for a moment. */
    private final java.util.Map<java.util.UUID, Long> thrownOut = new java.util.HashMap<>();

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
        if (liquid() <= 0) {
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
        if (drying()) {
            boilDry(level, now);
            return;
        }
        if (soaking > 0 && now >= soakedAt) {
            riseAsMagicStone(level);
        }
        if (now % SCAN_TICKS == 0 && liquid() > 0) {
            takeItems(level);
        }
        if (phase() != before || dirty && now % SYNC_TICKS == 0) {
            dirty = false;
            setChanged();
            sync();
        }
    }

    /**
     * Items over the crucible: boiling water draws them in at once (a thrown item is caught over the pot, it need not land
     * in the water); cold water lets them fall in and bob back up and out.
     */
    private void takeItems(ServerLevel level) {
        double x = worldPosition.getX();
        double y = worldPosition.getY();
        double z = worldPosition.getZ();
        AABB over = new AABB(x, y + BOTTOM, z, x + 1, y + 1 + CAPTURE_HEIGHT, z + 1);
        AABB inside = new AABB(x + INSIDE_MIN, y + BOTTOM, z + INSIDE_MIN, x + INSIDE_MAX, y + surface() + 0.1, z + INSIDE_MAX);
        long now = level.getGameTime();
        thrownOut.values().removeIf(at -> now - at > 40);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, boiling() ? over : inside, ItemEntity::isAlive)) {
            if (thrownOut.containsKey(entity.getUUID())) {
                continue;
            }
            ItemStack stack = entity.getItem();
            float caughtAt = (float) Math.max(surface(), entity.getY() - y);
            int taken = soaks(stack) ? soak(stack, stack.getCount(), entity.getBoundingBox().intersects(inside) || boiling()) : 0;
            if (taken == 0 && boiling()) {
                taken = dissolve(stack, stack.getCount());
            }
            if (taken > 0) {
                sankFrom = caughtAt;
                stack.shrink(taken);
                if (stack.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(stack);
                }
            } else if (entity.getBoundingBox().intersects(inside) && entity.getDeltaMovement().y <= 0.05) {
                // Cold water, or no room: the crucible will not take it - it bobs back up and out.
                entity.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.25, 0.35, (level.getRandom().nextDouble() - 0.5) * 0.25);
                level.playSound(null, worldPosition, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3F, 1.4F);
            }
        }
    }

    /** Whether this item is stone that soaks up essence (the blocks a drain valve turns into magic stone). */
    public static boolean soaks(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem block
                && block.getBlock().defaultBlockState().is(com.pockyl.hextale.registry.ModTags.SOAKS_ESSENCE);
    }

    /**
     * Lets up to {@code count} stones sink into the magic: each takes {@value #MAGIC_PER_STONE} mB of magic essence and
     * rises out as magic stone {@value #SOAK_TICKS} ticks later. Stones already soaking hold the next ones back.
     *
     * @param reached whether the stone has reached the liquid
     * @return how many sank
     */
    public int soak(ItemStack stack, int count, boolean reached) {
        int magic = essence.get(com.pockyl.hextale.rune.Essence.MAGIC);
        count = Math.min(count, magic / MAGIC_PER_STONE);
        if (!reached || soaking > 0 || drying() || count <= 0 || !soaks(stack)) {
            return 0;
        }
        essence = essence.minus(EssenceMix.pure(com.pockyl.hextale.rune.Essence.MAGIC, count * MAGIC_PER_STONE));
        soaking = count;
        soakedAt = (level == null ? 0 : level.getGameTime()) + SOAK_TICKS;
        // It sinks like anything dropped in, and the magic swirls in after it.
        sank = stack.copyWithCount(1);
        sankAt = level == null ? 0 : level.getGameTime();
        pouredColor = com.pockyl.hextale.rune.Essence.MAGIC.color();
        pouredAt = sankAt;
        playAt(SoundEvents.FISHING_BOBBER_SPLASH, 0.4F, 0.8F);
        playAt(ModSounds.CRUCIBLE_DISSOLVE.get(), 0.4F, 0.7F);
        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
            server.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.05, at.z, 10, 0.15, 0.02, 0.15, 0.05);
            server.sendParticles(new DustParticleOptions(com.pockyl.hextale.rune.Essence.MAGIC.color(), 1.2F), at.x, at.y + 0.08, at.z,
                    16, 0.2, 0.03, 0.2, 0);
        }
        changed();
        return count;
    }

    /** The soaked stones rise out of the liquid as magic stone, thrown up over the rim with a burst of the magic's light. */
    private void riseAsMagicStone(ServerLevel level) {
        int count = soaking;
        soaking = 0;
        Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface() + 0.15, 0.5);
        var random = level.getRandom();
        while (count > 0) {
            ItemStack stone = new ItemStack(com.pockyl.hextale.registry.ModItems.MAGIC_STONE.get(), Math.min(64, count));
            count -= stone.getCount();
            ItemEntity entity = new ItemEntity(level, at.x, at.y, at.z, stone);
            double angle = random.nextDouble() * Math.PI * 2;
            entity.setDeltaMovement(Math.cos(angle) * 0.12, 0.38, Math.sin(angle) * 0.12);
            entity.setPickUpDelay(10);
            thrownOut.put(entity.getUUID(), level.getGameTime());
            level.addFreshEntity(entity);
        }
        int magic = com.pockyl.hextale.rune.Essence.MAGIC.color();
        level.sendParticles(new DustParticleOptions(magic, 1.4F), at.x, at.y, at.z, 24, 0.22, 0.12, 0.22, 0);
        level.sendParticles(ParticleTypes.WITCH, at.x, at.y + 0.1, at.z, 12, 0.2, 0.2, 0.2, 0.05);
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 14, 0.2, 0.02, 0.2, 0.1);
        playAt(ModSounds.BLOCK_AWAKEN.get(), 0.6F, 1.2F);
        playAt(SoundEvents.GENERIC_SPLASH, 0.35F, 1.2F);
        changed();
    }

    /** Whether the water is boiling dry (sneak-use): it goes up in steam over {@link #BOIL_DRY_TICKS}. */
    public boolean drying() {
        return level != null && level.getGameTime() - dryingAt < BOIL_DRY_TICKS;
    }

    private void boilDry(ServerLevel level, long now) {
        float t = Math.min(1, (now - dryingAt + 1) / (float) BOIL_DRY_TICKS);
        water = Math.round(dryingFrom * (1 - t * t));
        Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.1, at.z, 3, 0.2, 0.05, 0.2, 0.03);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, at.x, at.y + 0.3, at.z, 4, 0.25, 0.2, 0.25, 0.02);
        if (now % 3 == 0) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y + 0.02, at.z, 6, 0.25, 0, 0.25, 0.02);
        }
        if (t >= 1) {
            dryingAt = -1000;
            empty();
        } else {
            setChanged();
            sync();
        }
    }

    /** Height of the water's surface in the block (the bottom of the bowl when empty). */
    public float surface() {
        return BOTTOM + (BRIM - BOTTOM) * Math.min(1, volume() / (float) CAPACITY);
    }

    public int water() {
        return water;
    }

    public float temperature() {
        return temperature;
    }

    public Phase phase() {
        return liquid() <= 0 ? Phase.COLD : Phase.of(temperature);
    }

    /** The water rolls: only now does it take anything in. */
    public boolean boiling() {
        return liquid() > 0 && temperature >= BOILING && heat() > 0;
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

    public float sankFrom() {
        return sankFrom;
    }

    // ---- Filling and emptying ------------------------------------------------------------------------------------

    /** Water and essence: both are liquid, so a crucible boiled down to its essence is not empty. */
    public int liquid() {
        return water + essence.total();
    }

    /** Everything in it: water, essence and the room the dissolved items take. */
    public int volume() {
        return water + essence.total() + ITEM_VOLUME * dissolvedCount();
    }

    /**
     * Pours water in; past the brim the mix spills over (so water dilutes what is in it). Refused only while it boils
     * dry, or when it is brim-full of plain water already.
     *
     * @return the water taken (all of {@code amount} or nothing)
     */
    public int fillWater(int amount, boolean simulate) {
        if (drying() || water >= CAPACITY && essence.isEmpty() && dissolved.isEmpty()) {
            return 0;
        }
        if (!simulate) {
            water += amount;
            spillOver();
            changed();
            playAt(SoundEvents.BUCKET_EMPTY, 0.8F, 1.0F);
        }
        return amount;
    }

    /**
     * Whatever rises past the brim spills over the rim: water and essence leave in proportion, the dissolved items stay.
     */
    private void spillOver() {
        int excess = volume() - CAPACITY;
        if (excess <= 0) {
            return;
        }
        int liquid = water + essence.total();
        int waterLoss = liquid <= 0 ? 0 : Math.round(excess * water / (float) liquid);
        water = Math.max(0, water - waterLoss);
        essence = essence.part(Math.max(0, essence.total() - (excess - waterLoss)));
        if (level instanceof ServerLevel server) {
            // Over the rim and down the sides, on every side.
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + BRIM + 0.05;
            double z = worldPosition.getZ() + 0.5;
            server.sendParticles(ParticleTypes.SPLASH, x, y, z, 24, 0.42, 0.02, 0.42, 0.08);
            server.sendParticles(ParticleTypes.FALLING_WATER, x, y - 0.1, z, 16, 0.48, 0.05, 0.48, 0);
            if (!essence.isEmpty()) {
                server.sendParticles(new DustParticleOptions(essence.color(), 1.0F), x, y, z, 10, 0.45, 0.04, 0.45, 0);
            }
        }
        playAt(SoundEvents.GENERIC_SPLASH, 0.35F, 1.3F);
    }

    /** Carried over from a water cauldron that woke into this crucible. */
    public void setWater(int water) {
        this.water = Mth.clamp(water, 0, CAPACITY);
        changed();
    }

    /**
     * Pours essence in, all of it; boiling water only. Past the brim the mix spills over.
     *
     * @return what was taken
     */
    public EssenceMix pour(EssenceMix offer) {
        if (!boiling() || offer.isEmpty()) {
            return EssenceMix.EMPTY;
        }
        EssenceMix taken = offer;
        essence = essence.plus(taken);
        spillOver();
        pouredColor = taken.color();
        pouredAt = level == null ? 0 : level.getGameTime();
        playAt(ModSounds.CRUCIBLE_POUR.get(), 1.0F, 0.9F + (level == null ? 0 : level.getRandom().nextFloat() * 0.2F));
        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
            server.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.05, at.z, 10, 0.12, 0.02, 0.12, 0.05);
            server.sendParticles(new DustParticleOptions(taken.color(), 1.2F), at.x, at.y + 0.1, at.z, 14, 0.18, 0.04, 0.18, 0);
        }
        changed();
        return taken;
    }

    /**
     * Dissolves up to {@code count} of the item in boiling water.
     *
     * @return how many were taken
     */
    public int dissolve(ItemStack stack, int count) {
        // Items take room but never crowd out the last of the liquid.
        count = Math.min(count, (CAPACITY - MIN_LIQUID - ITEM_VOLUME * dissolvedCount()) / ITEM_VOLUME);
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
        spillOver();
        sank = stack.copyWithCount(1);
        sankAt = level == null ? 0 : level.getGameTime();
        playAt(SoundEvents.FISHING_BOBBER_SPLASH, 0.35F, 1.0F + (level == null ? 0 : level.getRandom().nextFloat() * 0.3F));
        playAt(ModSounds.CRUCIBLE_DISSOLVE.get(), 0.25F, 0.9F + (level == null ? 0 : level.getRandom().nextFloat() * 0.2F));
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
        if (water + essence.total() < BUCKET) {
            return false;
        }
        empty();
        playAt(SoundEvents.BUCKET_FILL, 0.8F, 1.0F);
        return true;
    }

    /**
     * Boils the water dry at once (sneak-use): it goes up in a cloud of steam over a moment, taking the essence and
     * whatever dissolved in it along.
     */
    public void boilAway() {
        if (liquid() <= 0 || drying() || level == null) {
            return;
        }
        dryingAt = level.getGameTime();
        // Everything goes up as one cloud: the level sinks from where the liquid stood.
        dryingFrom = liquid();
        water = dryingFrom;
        essence = EssenceMix.EMPTY;
        dissolved.clear();
        playAt(SoundEvents.LAVA_EXTINGUISH, 0.8F, 0.75F + level.getRandom().nextFloat() * 0.1F);
        playAt(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.6F, 0.9F);
        changed();
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
        water = Mth.clamp(input.getIntOr("water", 0), 0, CAPACITY);
        temperature = Mth.clamp(input.getFloatOr("temperature", 0), 0, BOILING);
        essence = input.read("essence", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        dissolved.clear();
        dissolved.addAll(input.read("dissolved", ItemStack.CODEC.listOf()).orElse(List.of()));
        pouredAt = input.getLongOr("poured_at", -1000);
        pouredColor = input.getIntOr("poured_color", 0);
        sankAt = input.getLongOr("sank_at", -1000);
        sank = input.read("sank", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        sankFrom = input.getFloatOr("sank_from", 1);
        dryingAt = input.getLongOr("drying_at", -1000);
        dryingFrom = input.getIntOr("drying_from", 0);
        soaking = input.getIntOr("soaking", 0);
        soakedAt = input.getLongOr("soaked_at", 0);
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
        output.putFloat("sank_from", sankFrom);
        output.putLong("drying_at", dryingAt);
        output.putInt("drying_from", dryingFrom);
        if (soaking > 0) {
            output.putInt("soaking", soaking);
            output.putLong("soaked_at", soakedAt);
        }
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
