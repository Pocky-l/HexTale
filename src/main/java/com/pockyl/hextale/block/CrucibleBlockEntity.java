package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.HeatUser;
import com.pockyl.hextale.crafting.BrewRecipe;
import com.pockyl.hextale.crafting.CrucibleRecipe;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.runework.BurnerRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * The Crucible: a cauldron woken with hextech dust, the first vessel of chemtech. It holds water over a heat source;
 * the water warms by the heat's strength and cools without it. Only boiling water takes things in: bottles and jars of
 * essence pour into it, items dropped or put in sink and dissolve into their essences (lapis lazuli: 3 mB of gem, 3 of
 * magic, 1 of water). An item that is the catalyst of a {@link CrucibleRecipe} the water can pay for does not dissolve:
 * it takes the recipe's essences out of the water and rises as the result (stone in water holding 6 mB of magic comes
 * back as magic stone) - that happens in cold water too. It holds {@value #CAPACITY} mB of water and essence; more brims
 * over the rim and spills in proportion. The water never boils off by itself.
 * <p>
 * Chemtech starts here: a catalyst of a {@link BrewRecipe} sinking into water that boils hot enough and holds the brew's
 * essences takes them, and the water turns into doses of a brew (one kind at a time, up to {@value #MAX_DOSES}); a glass
 * bottle takes one dose as a Chem Vial. A Chem Burner below is the hottest heat (5); its runes may add a dose and give
 * experience for every brew.
 */
public class CrucibleBlockEntity extends BlockEntity implements HeatUser, com.pockyl.hextale.port.Removal.SideEffects {
    /** What it holds in all: water and essence. */
    public static final int CAPACITY = 1500;
    public static final int BUCKET = 1000;
    public static final int WATER_BOTTLE = 250;
    public static final float BOILING = 100;
    /** Degrees a tick per point of heat, and degrees a tick it cools without heat. */
    public static final float RISE = 0.1F;
    public static final float FALL = 0.15F;
    /** mB of essence an item's essence point dissolves into. */
    public static final int MB_PER_POINT = 1;
    /** Ticks a catalyst lies under the surface before it rises out as the recipe's result. */
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
    /** Doses of one brew the water holds at most. */
    public static final int MAX_DOSES = 12;

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
    /** The brew the water has turned into, and how many doses of it are left. */
    private @Nullable Brew brew;
    private int doses;
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
    /** Results of transmutations under way, and when they rise out of the water. */
    private final List<ItemStack> rising = new ArrayList<>();
    private long risesAt;
    /** Client only: when each essence last grew in the water, for the icons over the crucible. */
    private final long[] grewAt = new long[com.pockyl.hextale.rune.Essence.values().length];
    /** Results just thrown out: no crucible catches them again for a moment. */
    private final java.util.Map<java.util.UUID, Long> thrownOut = new java.util.HashMap<>();

    public CrucibleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    /**
     * How strongly the block under the crucible heats it: fire and campfires 2, their soul kin 3, magma 2, lava 4, a
     * burning Chem Burner 5.
     */
    public static int heat(BlockState below) {
        if (ChemBurnerBlock.isLit(below)) {
            return BrewRecipe.BURNER_HEAT;
        }
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
        if (!rising.isEmpty() && now >= risesAt) {
            rise(level);
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
            Player thrower = entity.getOwner() instanceof Player player ? player : null;
            int taken = take(stack, stack.getCount(), entity.getBoundingBox().intersects(inside) || boiling(), thrower);
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

    /**
     * Takes up to {@code count} of the item: a catalyst whose recipe the water can pay for transmutes (cold water too),
     * anything else with essences dissolves into them while the water boils. A catalyst the water cannot pay for yet is
     * not special: boiling water dissolves it like the rest.
     *
     * @param reached    whether the item has reached the liquid (boiling water draws items in from over it)
     * @param researcher who threw or put the item in (null: nobody): results that must be researched come only for them
     * @return how many were taken
     */
    public int take(ItemStack stack, int count, boolean reached, @Nullable Player researcher) {
        if (stack.isEmpty() || count <= 0 || drying() || liquid() <= 0) {
            return 0;
        }
        CrucibleRecipe recipe = CrucibleRecipe.find(stack, essence, researcher);
        if (recipe != null) {
            return reached ? transmute(recipe, stack, count) : 0;
        }
        ResearchTopic locked = CrucibleRecipe.lockedFor(stack, essence, researcher);
        if (boiling()) {
            BrewRecipe brewing = BrewRecipe.find(stack, essence, heat(), brew);
            if (brewing != null && brewing.mayBrew(researcher)) {
                // When the water already holds all the doses it can, the catalyst bobs back out rather than dissolve.
                return reached ? brew(brewing, stack, count) : 0;
            } else if (brewing != null) {
                locked = ResearchTopics.forBrew(brewing.brew());
            } else if (researcher instanceof ServerPlayer player) {
                hintBrew(stack, player);
            }
        }
        int dissolved = dissolve(stack, count);
        if (dissolved > 0 && locked != null && researcher instanceof ServerPlayer player) {
            // The brew was right, the researcher was not ready: the item only dissolved.
            player.displayClientMessage(Component.translatable("research.hextale.locked",
                    Component.translatable(locked.translationKey()).withColor(locked.color())), true);
        }
        return dissolved;
    }

    /**
     * The catalysts sink and the water turns into the brew: the recipe's essences are taken, the doses added (a Chem
     * Burner's Steam may add one more for each catalyst, its Radiance gives experience). No more than fit under
     * {@value #MAX_DOSES} doses are taken.
     */
    private int brew(BrewRecipe recipe, ItemStack stack, int count) {
        count = Math.min(count, Math.min(essence.timesHolds(recipe.essences()), (MAX_DOSES - doses) / recipe.doses()));
        if (count <= 0 || level == null) {
            return 0;
        }
        wear(recipe.essences().times(count), count);
        essence = essence.minus(recipe.essences().times(count));
        int made = recipe.doses() * count;
        ChemBurnerBlockEntity burner = ChemBurnerBlockEntity.under(level, worldPosition);
        Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
        if (burner != null && level instanceof ServerLevel server) {
            int steam = burner.strength(BurnerRune.STEAM);
            for (int i = 0; i < count && steam > 0; i++) {
                if (server.getRandom().nextFloat() < HolderRune.at(BurnerRune.MORE_DOSES, steam)) {
                    made++;
                    ToolEffects.puff(server, at.add(0, 0.2, 0), BurnerRune.STEAM.rune(), 10, 0.25);
                }
            }
            int radiance = burner.strength(BurnerRune.RADIANCE);
            if (radiance > 0) {
                net.minecraft.world.entity.ExperienceOrb.award(server, at.add(0, 0.4, 0), HolderRune.at(BurnerRune.EXPERIENCE, radiance) * count);
                ToolEffects.puff(server, at.add(0, 0.3, 0), BurnerRune.RADIANCE.rune(), 8, 0.25);
            }
        }
        brew = recipe.brew();
        doses = Math.min(MAX_DOSES, doses + made);
        long now = level.getGameTime();
        sank = stack.copyWithCount(1);
        sankAt = now;
        pouredColor = recipe.brew().color();
        pouredAt = now;
        playAt(ModSounds.CRUCIBLE_BREW.get(), 0.8F, 0.95F + level.getRandom().nextFloat() * 0.1F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ModParticles.wisp(recipe.brew().color()), at.x, at.y + 0.1, at.z, 10, 0.22, 0.05, 0.22, 0.01);
            server.sendParticles(ModParticles.bubble(recipe.brew().color()), at.x, at.y + 0.02, at.z, 14, 0.22, 0.01, 0.22, 0.01);
            server.sendParticles(ModParticles.glint(recipe.brew().color()), at.x, at.y + 0.3, at.z, 8, 0.2, 0.2, 0.2, 0.03);
        }
        changed();
        return count;
    }

    /** Why a catalyst of a brew only dissolved: the flame is too weak for it, or another brew is in the water. */
    private void hintBrew(ItemStack stack, ServerPlayer player) {
        BrewRecipe hot = BrewRecipe.find(stack, essence, BrewRecipe.BURNER_HEAT, brew);
        if (hot != null && hot.heat() > heat()) {
            player.displayClientMessage(Component.translatable("block.hextale.crucible.say.needs_heat",
                    Component.translatable("block.hextale.crucible.heat." + hot.heat())), true);
        } else if (brew != null && BrewRecipe.find(stack, essence, BrewRecipe.BURNER_HEAT, null) != null) {
            player.displayClientMessage(Component.translatable("block.hextale.crucible.say.other_brew",
                    Component.translatable(brew.translationKey()).withColor(brew.color())), true);
        }
    }

    /** The brew in the water (null: plain water), and its doses left. */
    public @Nullable Brew brew() {
        return brew;
    }

    public int doses() {
        return doses;
    }

    /** A glass bottle takes one dose: the Chem Vial, or empty when there is no brew. */
    public ItemStack bottle() {
        if (brew == null || doses <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack vial = ChemVialItem.of(brew);
        if (--doses <= 0) {
            brew = null;
            doses = 0;
        }
        playAt(SoundEvents.BOTTLE_FILL, 0.8F, 1.0F);
        changed();
        return vial;
    }

    /** It keeps its water hot while there is any. */
    @Override
    public boolean wantsHeat() {
        return liquid() > 0 && !drying();
    }

    /**
     * Brewing wears the land thin: per mB of essence a brew or a transmutation takes and per catalyst, up to double with
     * unknown essence in the water ({@link com.pockyl.hextale.thevoid.Thinning#BREWED}).
     */
    private void wear(EssenceMix taken, int catalysts) {
        if (level instanceof ServerLevel server) {
            float unknownShare = essence.total() == 0 ? 0 : essence.unknown() / (float) essence.total();
            com.pockyl.hextale.thevoid.Thinning.add(server, worldPosition, (taken.total() * com.pockyl.hextale.thevoid.Thinning.BREWED + catalysts * com.pockyl.hextale.thevoid.Thinning.BREW) * (1 + unknownShare),
                    com.pockyl.hextale.thevoid.Thinning.Source.BREWED);
        }
    }

    /** The catalysts sink, the recipe's essences swirl in after them, and the results rise out a moment later. */
    private int transmute(CrucibleRecipe recipe, ItemStack stack, int count) {
        count = Math.min(count, essence.timesHolds(recipe.essences()));
        if (count <= 0) {
            return 0;
        }
        wear(recipe.essences().times(count), count);
        essence = essence.minus(recipe.essences().times(count));
        ItemStack result = recipe.result().create();
        result.setCount(result.getCount() * count);
        boolean merged = false;
        for (ItemStack held : rising) {
            if (!merged && ItemStack.isSameItemSameComponents(held, result)) {
                held.grow(result.getCount());
                merged = true;
            }
        }
        if (!merged) {
            rising.add(result);
        }
        long now = level == null ? 0 : level.getGameTime();
        risesAt = now + SOAK_TICKS;
        sank = stack.copyWithCount(1);
        sankAt = now;
        pouredColor = recipe.essences().color();
        pouredAt = now;
        playAt(SoundEvents.FISHING_BOBBER_SPLASH, 0.4F, 0.8F);
        playAt(ModSounds.CRUCIBLE_DISSOLVE.get(), 0.4F, 0.7F);
        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
            server.sendParticles(ModParticles.drop(pouredColor), at.x, at.y + 0.05, at.z, 8, 0.12, 0.02, 0.12, 0.08);
            server.sendParticles(ModParticles.wisp(pouredColor), at.x, at.y + 0.08, at.z, 5, 0.2, 0.03, 0.2, 0.008);
        }
        changed();
        return count;
    }

    /** The transmuted items rise out of the liquid, thrown up over the rim with a burst of the essences' light. */
    private void rise(ServerLevel level) {
        Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface() + 0.15, 0.5);
        var random = level.getRandom();
        for (ItemStack result : rising) {
            while (!result.isEmpty()) {
                ItemStack part = result.split(result.getMaxStackSize());
                ItemEntity entity = new ItemEntity(level, at.x, at.y, at.z, part);
                double angle = random.nextDouble() * Math.PI * 2;
                entity.setDeltaMovement(Math.cos(angle) * 0.12, 0.38, Math.sin(angle) * 0.12);
                entity.setPickUpDelay(10);
                thrownOut.put(entity.getUUID(), level.getGameTime());
                level.addFreshEntity(entity);
            }
        }
        rising.clear();
        level.sendParticles(ModParticles.wisp(pouredColor), at.x, at.y, at.z, 10, 0.22, 0.12, 0.22, 0.01);
        level.sendParticles(ModParticles.glint(pouredColor), at.x, at.y + 0.1, at.z, 12, 0.2, 0.2, 0.2, 0.04);
        level.sendParticles(ModParticles.drop(pouredColor), at.x, at.y, at.z, 12, 0.2, 0.02, 0.2, 0.1);
        level.sendParticles(ModParticles.ring(pouredColor), at.x, at.y + 0.02, at.z, 0, 0.6, 1, 0, 1);
        playAt(ModSounds.BLOCK_AWAKEN.get(), 0.6F, 1.2F);
        playAt(SoundEvents.GENERIC_SPLASH, 0.35F, 1.2F);
        changed();
    }

    /** What the item dissolves into: its essences, {@value #MB_PER_POINT} mB a point (empty for an item without any). */
    public static EssenceMix essencesOf(ItemStack stack) {
        com.pockyl.hextale.research.ItemEssences essences = com.pockyl.hextale.research.ResearchItems.of(stack);
        return essences == null ? EssenceMix.EMPTY : EssenceMix.boiled(essences, MB_PER_POINT);
    }

    /** Whether the water is boiling dry (sneak-use): it goes up in steam over {@link #BOIL_DRY_TICKS}. */
    public boolean drying() {
        return level != null && level.getGameTime() - dryingAt < BOIL_DRY_TICKS;
    }

    private void boilDry(ServerLevel level, long now) {
        float t = Math.min(1, (now - dryingAt + 1) / (float) BOIL_DRY_TICKS);
        water = Math.round(dryingFrom * (1 - t * t));
        Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
        level.sendParticles(ModParticles.vapour(ModParticles.STEAM), at.x, at.y + 0.1, at.z, 4, 0.22, 0.05, 0.22, 0.02);
        if (now % 3 == 0) {
            level.sendParticles(ModParticles.bubble(liquidColor()), at.x, at.y + 0.02, at.z, 6, 0.25, 0, 0.25, 0.005);
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

    /** Broken while results still soak: they come out with the block instead of vanishing. */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        com.pockyl.hextale.port.Removal.vanilla(this, pos, state);
        if (level != null && !level.isClientSide()) {
            for (ItemStack result : rising) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, result);
            }
        }
        rising.clear();
    }

    /** Results of transmutations about to rise (a copy). */
    public List<ItemStack> rising() {
        return rising.stream().map(ItemStack::copy).toList();
    }

    /** Client: game time when this essence last grew in the water (long ago if never). */
    public long grewAt(com.pockyl.hextale.rune.Essence essence) {
        return grewAt[essence.ordinal()];
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

    /** Everything in it: water and essence. */
    public int volume() {
        return water + essence.total();
    }

    /**
     * Pours water in; past the brim the mix spills over (so water dilutes what is in it). Refused only while it boils
     * dry, or when it is brim-full of plain water already.
     *
     * @return the water taken (all of {@code amount} or nothing)
     */
    public int fillWater(int amount, boolean simulate) {
        if (drying() || water >= CAPACITY && essence.isEmpty()) {
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
     * Whatever rises past the brim spills over the rim: water and essence leave in proportion.
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
            server.sendParticles(ModParticles.drop(liquidColor()), x, y, z, 24, 0.42, 0.02, 0.42, 0.06);
            server.sendParticles(ModParticles.drop(liquidColor()), x, y - 0.1, z, 16, 0.48, 0.05, 0.48, 0);
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
            server.sendParticles(ModParticles.drop(taken.color()), at.x, at.y + 0.05, at.z, 8, 0.12, 0.02, 0.12, 0.07);
            server.sendParticles(ModParticles.wisp(taken.color()), at.x, at.y + 0.1, at.z, 6, 0.18, 0.04, 0.18, 0.008);
        }
        changed();
        return taken;
    }

    /**
     * Dissolves up to {@code count} of the item in boiling water into its essences. Items without essences are refused.
     *
     * @return how many were taken
     */
    public int dissolve(ItemStack stack, int count) {
        EssenceMix each = essencesOf(stack);
        if (!boiling() || stack.isEmpty() || count <= 0 || each.isEmpty()) {
            return 0;
        }
        essence = essence.plus(each.times(count));
        spillOver();
        sank = stack.copyWithCount(1);
        sankAt = level == null ? 0 : level.getGameTime();
        pouredColor = each.color();
        playAt(SoundEvents.FISHING_BOBBER_SPLASH, 0.35F, 1.0F + (level == null ? 0 : level.getRandom().nextFloat() * 0.3F));
        playAt(ModSounds.CRUCIBLE_DISSOLVE.get(), 0.25F, 0.9F + (level == null ? 0 : level.getRandom().nextFloat() * 0.2F));
        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atLowerCornerOf(worldPosition).add(0.5, surface(), 0.5);
            server.sendParticles(ModParticles.drop(liquidColor()), at.x, at.y + 0.05, at.z, 6, 0.15, 0.02, 0.15, 0.06);
            server.sendParticles(ModParticles.wisp(each.color()), at.x, at.y + 0.1, at.z, 4, 0.12, 0.05, 0.12, 0.008);
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
     * whatever is under way along.
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
        brew = null;
        doses = 0;
        rising.clear();
        playAt(SoundEvents.LAVA_EXTINGUISH, 0.8F, 0.75F + level.getRandom().nextFloat() * 0.1F);
        playAt(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.6F, 0.9F);
        changed();
    }

    /** Everything goes: water, essence (poured away, it wears the land), brew and soaking things. */
    private void empty() {
        if (level instanceof ServerLevel server && !essence.isEmpty()) {
            com.pockyl.hextale.thevoid.Thinning.poured(server, worldPosition, essence, essence.total(), false);
        }
        water = 0;
        temperature = 0;
        essence = EssenceMix.EMPTY;
        brew = null;
        doses = 0;
        rising.clear();
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
            level.addParticle(ModParticles.vapour(ModParticles.STEAM), x, y + 0.05, z, 0, 0.01 + random.nextDouble() * 0.015, 0);
        }
        if (phase == Phase.BOILING) {
            if (random.nextInt(2) == 0) {
                level.addParticle(ModParticles.bubble(crucible.liquidColor()), x, y + 0.01, z, 0, 0, 0);
            }
            if (random.nextInt(14) == 0) {
                level.addParticle(ModParticles.drop(crucible.liquidColor()), x, y + 0.05, z, (random.nextDouble() - 0.5) * 0.06, 0.12, (random.nextDouble() - 0.5) * 0.06);
            }
            if (random.nextInt(40) == 0) {
                level.playLocalSound(pos, ModSounds.CRUCIBLE_BOIL.get(), SoundSource.BLOCKS, 0.22F + random.nextFloat() * 0.08F,
                        0.9F + random.nextFloat() * 0.2F, false);
            }
        } else if (phase == Phase.SIMMERING && random.nextInt(6) == 0) {
            level.addParticle(ModParticles.bubble(crucible.liquidColor()), x, y + 0.01, z, 0, 0, 0);
        }
    }

    /** The colour the liquid shows: water, tinted by its essences and by a brew. */
    public int liquidColor() {
        int color = essence.isEmpty() ? ModParticles.WATER : mix(ModParticles.WATER, essence.color(), Math.min(1, essence.total() / 128F));
        return brew == null ? color : mix(color, brew.color(), 0.7F);
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }

    public static int steamColor() {
        return STEAM;
    }

    // ---- Saving -----------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        water = Mth.clamp(input.getIntOr("water", 0), 0, CAPACITY);
        temperature = Mth.clamp(input.getFloatOr("temperature", 0), 0, BOILING);
        EssenceMix before = essence;
        essence = input.read("essence", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        if (level != null && level.isClientSide()) {
            for (com.pockyl.hextale.rune.Essence e : com.pockyl.hextale.rune.Essence.values()) {
                if (essence.get(e) > before.get(e)) {
                    grewAt[e.ordinal()] = level.getGameTime();
                }
            }
        }
        brew = input.read("brew", Brew.CODEC).orElse(null);
        doses = brew == null ? 0 : Mth.clamp(input.getIntOr("doses", 0), 0, MAX_DOSES);
        rising.clear();
        rising.addAll(input.read("rising", ItemStack.CODEC.listOf()).orElse(List.of()));
        pouredAt = input.getLongOr("poured_at", -1000);
        pouredColor = input.getIntOr("poured_color", 0);
        sankAt = input.getLongOr("sank_at", -1000);
        sank = input.read("sank", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        sankFrom = input.getFloatOr("sank_from", 1);
        dryingAt = input.getLongOr("drying_at", -1000);
        dryingFrom = input.getIntOr("drying_from", 0);
        risesAt = input.getLongOr("rises_at", 0);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        output.putInt("water", water);
        output.putFloat("temperature", temperature);
        if (!essence.isEmpty()) {
            output.store("essence", EssenceMix.CODEC, essence);
        }
        if (brew != null) {
            output.store("brew", Brew.CODEC, brew);
            output.putInt("doses", doses);
        }
        if (!rising.isEmpty()) {
            output.store("rising", ItemStack.CODEC.listOf(), List.copyOf(rising));
            output.putLong("rises_at", risesAt);
        }
        // The last pour and the last item sunk, for the clients' animations (kept: they are small).
        output.putLong("poured_at", pouredAt);
        output.putInt("poured_color", pouredColor);
        output.putLong("sank_at", sankAt);
        output.putFloat("sank_from", sankFrom);
        output.putLong("drying_at", dryingAt);
        output.putInt("drying_from", dryingFrom);
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
