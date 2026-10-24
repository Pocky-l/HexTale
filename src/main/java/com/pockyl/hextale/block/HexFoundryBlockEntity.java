package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.menu.HexFoundryMenu;
import com.pockyl.hextale.multiblock.HexFoundry;
import com.pockyl.hextale.multiblock.MultiblockCore;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.FoundryRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;

import org.jspecify.annotations.Nullable;

/**
 * The Foundry Hatch's contents, the heart of a formed Hex Foundry ({@link HexFoundry}). A big kiln is slow to wake and
 * then devours: whatever burns in it (furnace fuel in its fuel slot, else fire or magma essence drawn from jars around
 * it or piped into any of its bricks) raises its heat, and from a quarter of full heat up every one of its nine inputs
 * melts at once, faster the hotter it is - up to twice a furnace's speed in every slot. Idle, it cools slowly. Items
 * dropped onto its top fall in through the grate; hoppers feed the inputs from above, fuel from the sides and take the
 * ingots from below, through any of its bricks.
 */
public class HexFoundryBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, MultiblockCore, EssenceNetwork.Receiver, com.pockyl.hextale.port.Removal.SideEffects {
    public static final int INPUTS = 9;
    public static final int OUTPUT_START = INPUTS;
    public static final int OUTPUTS = 9;
    public static final int FUEL = INPUTS + OUTPUTS;
    public static final int SIZE = FUEL + 1;
    private static final int[] TOP = range(0, INPUTS);
    private static final int[] SIDES = {FUEL};
    private static final int[] BOTTOM = range(OUTPUT_START, OUTPUTS);

    /** Full heat. */
    public static final int MAX_HEAT = 1000;
    /** Below this nothing melts yet: the kiln is still waking. */
    public static final int MELT_HEAT = 250;
    /** Heat gained per tick of burning (Bellows multiplies it) and lost per tick of standing cold. */
    private static final int HEAT_RISE = 2;
    private static final int COOLING = 1;
    /** Melt speed (cooking ticks per tick) at {@link #MELT_HEAT} and at full heat. */
    private static final float SLOW = 0.5F;
    private static final float FAST = 2F;
    /** Fire is ticks of burning; essence gives this much per millibucket. */
    public static final int FIRE_PER_MB = 40;
    public static final int MAGMA_PER_MB = 60;
    /** Essence it holds as fire at most when pipes push it in. */
    public static final int FIRE_CAP = 4000;
    /** mB taken from a jar at a time. */
    public static final int DRAW_MB = 8;
    /** Ticks a jar's stream shows after a draw. */
    public static final int STREAM_TICKS = 40;
    private static final int SWALLOW_TICKS = 10;
    private static final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> SMELTING = RecipeManager.createCheck(RecipeType.SMELTING);

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    /** Cooking ticks done in each input, in hundredths. */
    private final int[] progress = new int[INPUTS];
    /** Cooking ticks each input needs (0 = nothing melting there). */
    private final int[] total = new int[INPUTS];
    private int heat;
    private int fire;
    /** The largest fire since it last ran out: the gauge's full mark. */
    private int fireTop;
    private float experience;
    /** Why it waits, for the screen and Jade: "" while working or idle. */
    private String blocked = "";
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    @Nullable
    private Formed formed;
    @Nullable
    private BlockPos drewFrom;
    private long drewAt = -1000;
    /** Heat as last sent to clients, in steps of 50. */
    private int sentHeat = -1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> heat;
                case 1 -> fireGauge();
                default -> total[index - 2] <= 0 ? 0 : Math.min(100, progress[index - 2] / total[index - 2]);
            };
        }

        @Override
        public void set(int index, int value) {
            // The client reads the numbers off the menu's own data.
        }

        @Override
        public int getCount() {
            return 2 + INPUTS;
        }
    };

    public HexFoundryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_FOUNDRY.get(), pos, state);
    }

    private static int[] range(int start, int count) {
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) {
            slots[i] = start + i;
        }
        return slots;
    }

    // ---- State --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }

    private int strength(FoundryRune rune) {
        return runes.strength(rune.rune());
    }

    public int heat() {
        return heat;
    }

    public int fire() {
        return fire;
    }

    /** Sets the heat at once (game tests). */
    public void setHeat(int heat) {
        this.heat = Mth.clamp(heat, 0, MAX_HEAT);
        setChanged();
    }

    /** 0-1000: the fire left against its full mark. */
    private int fireGauge() {
        return fire <= 0 ? 0 : Math.round(1000F * fire / Math.max(fire, fireTop));
    }

    public String blocked() {
        return blocked;
    }

    /** Whether it stands as a whole kiln (an unbuilt hatch does nothing). */
    public boolean isFormed() {
        return formed != null && getBlockState().getValue(HexFoundryBlock.FORMED);
    }

    @Override
    public @Nullable Formed formed() {
        return formed;
    }

    @Override
    public void setFormed(Formed formed) {
        this.formed = formed;
        sync();
    }

    /** Taken apart (the hatch became a plain hatch again): it forgets the kiln but keeps runes and contents. */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (!state.getValue(HexFoundryBlock.FORMED)) {
            formed = null;
            heat = 0;
        }
    }

    /** The jar it drew from lately, for the stream. */
    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** The cell in the middle of the kiln (its hollow heart), from the hatch's facing. */
    public static BlockPos heart(BlockPos hatch, Direction front) {
        return hatch.relative(front.getOpposite());
    }

    public BlockPos heart() {
        return heart(worldPosition, getBlockState().getValue(HexFoundryBlock.FACING));
    }

    /** The iron grate in the middle of its top, where things fall in and the heat breathes out. */
    public Vec3 grate() {
        return Vec3.atCenterOf(heart().above());
    }

    // ---- Ticking ------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, HexFoundryBlockEntity foundry) {
        if (!foundry.isFormed()) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        if (Math.floorMod(level.getGameTime() + pos.asLong(), SWALLOW_TICKS) == 0) {
            foundry.swallow(server);
        }
        boolean burning = foundry.work(server);
        if (state.getValue(HexFoundryBlock.LIT) != burning) {
            level.setBlock(pos, state.setValue(HexFoundryBlock.LIT, burning), Block.UPDATE_ALL);
        }
        if (foundry.heat / 50 != foundry.sentHeat) {
            foundry.sentHeat = foundry.heat / 50;
            foundry.sync();
        }
    }

    /** What the input in a slot melts into, or null. */
    @Nullable
    private RecipeHolder<SmeltingRecipe> recipe(ServerLevel level, ItemStack stack) {
        return stack.isEmpty() ? null : SMELTING.getRecipeFor(new SingleRecipeInput(stack), level).orElse(null);
    }

    /** One tick: burn, heat or cool, and melt every input a little. True while it burns. */
    private boolean work(ServerLevel level) {
        int active = 0;
        @SuppressWarnings("unchecked")
        RecipeHolder<SmeltingRecipe>[] recipes = new RecipeHolder[INPUTS];
        boolean full = false;
        for (int slot = 0; slot < INPUTS; slot++) {
            ItemStack stack = items.get(slot);
            RecipeHolder<SmeltingRecipe> recipe = recipe(level, stack);
            if (recipe == null) {
                progress[slot] = 0;
                total[slot] = 0;
                continue;
            }
            ItemStack result = recipe.value().assemble(new SingleRecipeInput(stack), level.registryAccess());
            if (result.isEmpty()) {
                continue;
            }
            if (!fits(result)) {
                full = true;
                continue;
            }
            recipes[slot] = recipe;
            total[slot] = recipe.value().getCookingTime();
            active++;
        }
        if (active == 0) {
            cool(full ? "output_full" : "");
            return false;
        }
        if (fire <= 0 && !kindle(level)) {
            cool("no_fire");
            return false;
        }
        blocked = "";
        // A big fire eats more the more it melts at once, still far less per piece than a furnace.
        int use = 1 + active / 2;
        int flame = strength(FoundryRune.ETERNAL_FLAME);
        if (flame == 0 || level.getRandom().nextFloat() >= HolderRune.at(FoundryRune.FREE_FIRE, flame)) {
            fire = Math.max(0, fire - use);
        }
        int bellows = strength(FoundryRune.BELLOWS);
        int rise = bellows == 0 ? HEAT_RISE : Math.round(HEAT_RISE * HolderRune.at(FoundryRune.HEATING, bellows));
        heat = Math.min(MAX_HEAT, heat + rise);
        if (heat >= MELT_HEAT) {
            int step = Math.round(speed() * 100);
            for (int slot = 0; slot < INPUTS; slot++) {
                if (recipes[slot] == null) {
                    continue;
                }
                progress[slot] += step;
                if (progress[slot] >= total[slot] * 100) {
                    progress[slot] = 0;
                    melt(level, slot, recipes[slot]);
                }
            }
        }
        setChanged();
        return true;
    }

    /** Cooking ticks per tick at the present heat. */
    public float speed() {
        if (heat < MELT_HEAT) {
            return 0;
        }
        float top = FAST;
        int tempering = strength(FoundryRune.TEMPERING);
        if (tempering > 0) {
            top *= HolderRune.at(FoundryRune.TOP_SPEED, tempering);
        }
        return Mth.lerp((heat - MELT_HEAT) / (float) (MAX_HEAT - MELT_HEAT), SLOW, top);
    }

    /** One piece in a slot melted: its result into the outputs, with whatever the runes add. */
    private void melt(ServerLevel level, int slot, RecipeHolder<SmeltingRecipe> recipe) {
        ItemStack input = items.get(slot);
        ItemStack result = recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess());
        boolean ore = input.is(Tags.Items.ORES) || input.is(Tags.Items.RAW_MATERIALS);
        boolean stony = input.is(Tags.Items.COBBLESTONES) || input.is(Tags.Items.STONES) || input.is(Tags.Items.SANDS)
                || input.is(Tags.Items.NETHERRACKS);
        input.shrink(1);
        Vec3 grate = grate();
        int forge = strength(FoundryRune.FORGE);
        if (ore && forge > 0 && level.getRandom().nextFloat() < HolderRune.at(FoundryRune.MORE_INGOTS, forge)) {
            ItemStack more = result.copyWithCount(result.getCount() + 1);
            if (fits(more)) {
                result = more;
                ToolEffects.puff(level, grate.add(0, 0.6, 0), FoundryRune.FORGE.rune(), 8, 0.35);
                level.sendParticles(ModParticles.glint(ModParticles.GOLD), grate.x, grate.y + 0.6, grate.z, 6, 0.3, 0.3, 0.3, 0.05);
            }
        }
        insert(result);
        int smelting = strength(FoundryRune.SMELTING);
        if (stony && smelting > 0 && level.getRandom().nextFloat() < HolderRune.at(FoundryRune.NUGGET, smelting)) {
            ItemStack nugget = new ItemStack(slag(level.getRandom().nextFloat()));
            if (fits(nugget)) {
                insert(nugget);
                ToolEffects.puff(level, grate.add(0, 0.6, 0), FoundryRune.SMELTING.rune(), 6, 0.3);
            }
        }
        int radiance = strength(FoundryRune.RADIANCE);
        experience += recipe.value().getExperience() * (radiance > 0 ? HolderRune.at(FoundryRune.EXPERIENCE, radiance) : 1);
        if (level.getRandom().nextInt(3) == 0) {
            level.playSound(null, grate.x, grate.y, grate.z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5F, 0.7F + level.getRandom().nextFloat() * 0.3F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, grate.x, grate.y + 0.5, grate.z, 1, 0.2, 0, 0.2, 0);
        }
    }

    /** The metal grains a stone melt leaves: mostly iron, then copper, rarely gold. */
    static Item slag(float roll) {
        return roll < 0.5F ? Items.IRON_NUGGET : roll < 0.85F ? Items.IRON_NUGGET : Items.GOLD_NUGGET;
    }

    /** Fuel from its slot, else fire or magma essence from the nearest jar that has some. */
    private boolean kindle(ServerLevel level) {
        ItemStack fuel = items.get(FUEL);
        int burn = fuel.isEmpty() ? 0
                : fuel.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
        if (burn > 0) {
            ItemStackTemplate remainder = com.pockyl.hextale.port.ItemStackTemplate.remainderOf(fuel);
            fuel.shrink(1);
            if (fuel.isEmpty() && remainder != null) {
                items.set(FUEL, remainder.create());
            }
            light(burn);
            return true;
        }
        return drawEssence(level);
    }

    private void light(int amount) {
        fire += amount;
        fireTop = fire;
        setChanged();
    }

    private boolean drawEssence(ServerLevel level) {
        BlockPos heart = heart();
        for (var jar : JarReach.around(level, heart.offset(-1, -1, -1), heart.offset(1, 1, 1))) {
            EssenceMix contents = jar.tank().contents();
            Essence kind = contents.get(Essence.MAGMA) > 0 ? Essence.MAGMA : contents.get(Essence.FIRE) > 0 ? Essence.FIRE : null;
            if (kind == null) {
                continue;
            }
            int taken = Math.min(DRAW_MB, contents.get(kind));
            jar.tank().remove(EssenceMix.pure(kind, taken));
            jar.setChanged();
            light(taken * (kind == Essence.MAGMA ? MAGMA_PER_MB : FIRE_PER_MB));
            drewFrom = jar.getBlockPos();
            drewAt = level.getGameTime();
            Vec3 mouth = JarReach.mouth(drewFrom);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 0.8F);
            sync();
            return true;
        }
        return false;
    }

    /** Fire or magma essence piped into any brick becomes fire, up to {@link #FIRE_CAP}. */
    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (!isFormed()) {
            return 0;
        }
        Essence kind = offer.pureEssence();
        if (kind != Essence.FIRE && kind != Essence.MAGMA) {
            return 0;
        }
        int per = kind == Essence.MAGMA ? MAGMA_PER_MB : FIRE_PER_MB;
        int taken = Math.min(offer.total(), (FIRE_CAP - fire) / per);
        if (taken <= 0) {
            return 0;
        }
        if (!simulate) {
            fire += taken * per;
            fireTop = Math.max(fireTop, fire);
            setChanged();
        }
        return taken;
    }

    /** Nothing to melt or nothing to burn: the heat slowly leaves the bricks. */
    private void cool(String reason) {
        boolean changed = !blocked.equals(reason);
        blocked = reason;
        if (heat > 0) {
            heat = Math.max(0, heat - COOLING);
            changed = true;
        }
        if (changed) {
            setChanged();
        }
    }

    /** Things dropped onto its top fall in through the grate, if they melt and there is room. */
    private void swallow(ServerLevel level) {
        BlockPos heart = heart();
        AABB top = new AABB(heart.getX() - 1, heart.getY() + 2, heart.getZ() - 1, heart.getX() + 2, heart.getY() + 2.75, heart.getZ() + 2);
        boolean took = false;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, top, ItemEntity::isAlive)) {
            ItemStack stack = entity.getItem();
            if (recipe(level, stack) == null) {
                continue;
            }
            int before = stack.getCount();
            ItemStack left = feed(stack.copy());
            if (left.getCount() != before) {
                took = true;
                if (left.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(left);
                }
            }
        }
        if (took) {
            Vec3 grate = grate();
            level.playSound(null, grate.x, grate.y, grate.z, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25F, 1.4F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, grate.x, grate.y + 0.55, grate.z, 4, 0.2, 0.05, 0.2, 0.02);
            setChanged();
        }
    }

    /** Puts what it can into the inputs (same item first, then empty slots); returns what is left. */
    public ItemStack feed(ItemStack stack) {
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            for (int slot = 0; slot < INPUTS && !stack.isEmpty(); slot++) {
                ItemStack here = items.get(slot);
                if (pass == 0 && !here.isEmpty() && ItemStack.isSameItemSameComponents(here, stack)) {
                    int move = Math.min(stack.getCount(), here.getMaxStackSize() - here.getCount());
                    here.grow(move);
                    stack.shrink(move);
                } else if (pass == 1 && here.isEmpty()) {
                    items.set(slot, stack.copy());
                    stack.setCount(0);
                }
            }
        }
        return stack;
    }

    /** Whether the outputs have room for the whole stack. */
    private boolean fits(ItemStack stack) {
        int room = 0;
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUTS; slot++) {
            ItemStack here = items.get(slot);
            if (here.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(here, stack)) {
                room += here.getMaxStackSize() - here.getCount();
                if (room >= stack.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void insert(ItemStack stack) {
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUTS && !stack.isEmpty(); slot++) {
            ItemStack here = items.get(slot);
            if (!here.isEmpty() && ItemStack.isSameItemSameComponents(here, stack)) {
                int move = Math.min(stack.getCount(), here.getMaxStackSize() - here.getCount());
                here.grow(move);
                stack.shrink(move);
            }
        }
        for (int slot = OUTPUT_START; slot < OUTPUT_START + OUTPUTS && !stack.isEmpty(); slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, stack.copy());
                stack.setCount(0);
            }
        }
    }

    /** The experience of every melt since it was last taken out, as orbs at the player. */
    public void awardExperience(Player player) {
        if (!(level instanceof ServerLevel server) || experience <= 0) {
            return;
        }
        int points = Mth.floor(experience);
        if (server.getRandom().nextFloat() < experience - points) {
            points++;
        }
        experience = 0;
        if (points > 0) {
            ExperienceOrb.award(server, player.position(), points);
        }
        setChanged();
    }

    /** Broken: its contents spill out (as from any container) and the kiln falls apart; the hatch keeps its runes. */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        com.pockyl.hextale.port.Removal.vanilla(this, pos, state);
        if (level instanceof ServerLevel serverLevel) {
            Multiblocks.breakApart(serverLevel, pos, pos, formed);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.hex_foundry");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new HexFoundryMenu(containerId, inventory, this, data, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    /** What each slot takes: anything into the inputs, only fuel into the fuel slot, nothing into the outputs. */
    public static boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS || slot == FUEL && (stack.getBurnTime(null) > 0);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return isFormed() && accepts(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot >= OUTPUT_START && slot < OUTPUT_START + OUTPUTS;
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.tag(), items, input.registries());
        int[] saved = input.getIntArray("progress").orElse(new int[0]);
        for (int i = 0; i < INPUTS; i++) {
            progress[i] = i < saved.length ? saved[i] : 0;
        }
        heat = input.getIntOr("heat", 0);
        fire = input.getIntOr("fire", 0);
        fireTop = input.getIntOr("fire_top", 0);
        experience = input.getFloatOr("experience", 0);
        blocked = input.getStringOr("blocked", "");
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        formed = input.read("formed", Formed.CODEC).orElse(null);
        drewFrom = input.read("drew_from", BlockPos.CODEC).orElse(null);
        drewAt = input.getLongOr("drew_at", -1000);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.FOUNDRY_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.FOUNDRY_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
        super.removeComponentsFromTag(output.tag());
        output.discard("runes");
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        ContainerHelper.saveAllItems(output.tag(), items, output.registries());
        output.putIntArray("progress", progress.clone());
        output.putInt("heat", heat);
        output.putInt("fire", fire);
        output.putInt("fire_top", fireTop);
        output.putFloat("experience", experience);
        output.putString("blocked", blocked);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        if (formed != null) {
            output.store("formed", Formed.CODEC, formed);
        }
        if (drewFrom != null) {
            output.store("drew_from", BlockPos.CODEC, drewFrom);
            output.putLong("drew_at", drewAt);
        }
    }

    /** Clients see the heat, the runes' colour, the stream and the structure, not the contents. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("heat", heat);
        tag.putString("blocked", blocked);
        if (drewFrom != null) {
            com.pockyl.hextale.port.ValueOutput.of(tag, registries).store("drew_from", BlockPos.CODEC, drewFrom);
            tag.putLong("drew_at", drewAt);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
