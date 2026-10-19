package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

import com.mojang.serialization.Codec;
import com.pockyl.hextale.crafting.AlloyRecipe;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.menu.HexFurnaceMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Furnace's contents: up to three metals in its input slots melt together into an alloy ({@link AlloyRecipe}) on
 * ordinary furnace fuel; anything else a furnace smelts it smelts too, up to three of the same item at once. Alloys guarded by a research topic melt only when the furnace's owner (whoever placed or woke
 * it) has researched them; what the owner knows is remembered whenever they are about, so the furnace keeps working
 * while they are away. Hoppers feed the inputs from the top, fuel from the sides, and take the alloy from below.
 */
public class HexFurnaceBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUTS = AlloyRecipe.SLOTS;
    public static final int FUEL = 3;
    public static final int OUTPUT = 4;
    public static final int SIZE = 5;
    private static final int[] TOP = {0, 1, 2};
    private static final int[] SIDES = {FUEL};
    private static final int[] BOTTOM = {OUTPUT};
    private static final int REFRESH_TICKS = 100;
    private static final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> SMELTING = RecipeManager.createCheck(RecipeType.SMELTING);

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int litTime;
    private int litDuration;
    private int progress;
    private int total;
    private float experience;
    /** Why it waits, for the screen: "" while working or idle. */
    private String blocked = "";
    private @Nullable UUID owner;
    /** The owner's researched topics as last seen. */
    private Set<String> known = new HashSet<>();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> total;
                case 2 -> litTime;
                default -> litDuration;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> total = value;
                case 2 -> litTime = value;
                default -> litDuration = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public HexFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_FURNACE.get(), pos, state);
    }

    public String blocked() {
        return blocked;
    }

    public boolean burning() {
        return litTime > 0;
    }

    // ---- Owner and research ----------------------------------------------------------------------------------------------

    public void setOwner(Player player) {
        owner = player.getUUID();
        learnFrom(player);
    }

    /** Remembers what the owner has researched (the first player to use an ownerless furnace becomes its owner). */
    public void learnFrom(Player player) {
        if (owner == null) {
            owner = player.getUUID();
        }
        if (!owner.equals(player.getUUID())) {
            return;
        }
        RuneKnowledge knowledge = Research.of(player);
        Set<String> now = new HashSet<>();
        for (ResearchTopic topic : ResearchTopics.all()) {
            if (knowledge.researched(topic)) {
                now.add(topic.id());
            }
        }
        if (!now.equals(known)) {
            known = now;
            setChanged();
        }
    }

    /** Whether the owner may melt this alloy: free alloys always, researched ones once the owner knows them. */
    public boolean mayMake(ItemStackTemplate result) {
        ResearchTopic topic = ResearchTopics.of(result.item().value());
        return topic == null || known.contains(topic.id());
    }

    // ---- Ticking ------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, HexFurnaceBlockEntity furnace) {
        ServerLevel server = (ServerLevel) level;
        if (furnace.owner != null && Math.floorMod(level.getGameTime() + pos.asLong(), REFRESH_TICKS) == 0) {
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(furnace.owner);
            if (player != null) {
                furnace.learnFrom(player);
            }
        }
        boolean wasLit = furnace.litTime > 0;
        if (furnace.litTime > 0) {
            furnace.litTime--;
        }
        furnace.work(server);
        boolean lit = furnace.litTime > 0;
        if (wasLit != lit || state.getValue(HexFurnaceBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(HexFurnaceBlock.LIT, lit), Block.UPDATE_ALL);
            furnace.setChanged();
        }
    }

    private List<ItemStack> inputs() {
        return items.subList(0, INPUTS);
    }

    /** One melt: what comes out, how long it takes, its experience, and which input slots give how much. */
    private record Melt(ItemStack result, int time, float experience, int[] slots, int[] counts) {
    }

    /** What the inputs melt into now: an alloy, else plain smelting of one to three of the same item at once. */
    @Nullable
    private Melt melt(ServerLevel level) {
        Map.Entry<Identifier, AlloyRecipe> found = AlloyRecipe.find(inputs(), false);
        if (found != null) {
            AlloyRecipe recipe = found.getValue();
            int[] slots = recipe.assign(inputs());
            if (slots == null) {
                return null;
            }
            int[] counts = new int[slots.length];
            for (int i = 0; i < slots.length; i++) {
                counts[i] = recipe.inputs().get(i).count();
            }
            if (!mayMake(recipe.result())) {
                blocked = "locked";
                return null;
            }
            return new Melt(recipe.result().create(), recipe.time(), recipe.experience(), slots, counts);
        }
        return smelt(level);
    }

    /**
     * Plain smelting: every filled input holds the same item and it has a furnace recipe - all of them melt in one go
     * (three ores give three ingots in one furnace's time).
     */
    @Nullable
    private Melt smelt(ServerLevel level) {
        ItemStack first = ItemStack.EMPTY;
        int filled = 0;
        for (ItemStack stack : inputs()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!first.isEmpty() && !ItemStack.isSameItemSameComponents(first, stack)) {
                return null;
            }
            if (first.isEmpty()) {
                first = stack;
            }
            filled++;
        }
        if (first.isEmpty()) {
            return null;
        }
        SingleRecipeInput input = new SingleRecipeInput(first);
        RecipeHolder<? extends AbstractCookingRecipe> recipe = SMELTING.getRecipeFor(input, level).orElse(null);
        if (recipe == null) {
            return null;
        }
        ItemStack result = recipe.value().assemble(input);
        if (result.isEmpty()) {
            return null;
        }
        result.setCount(result.getCount() * filled);
        int[] slots = new int[filled];
        int[] counts = new int[filled];
        int n = 0;
        for (int slot = 0; slot < INPUTS; slot++) {
            if (!items.get(slot).isEmpty()) {
                slots[n] = slot;
                counts[n++] = 1;
            }
        }
        return new Melt(result, recipe.value().cookingTime(), recipe.value().experience() * filled, slots, counts);
    }

    private void work(ServerLevel level) {
        String before = blocked;
        blocked = "";
        Melt melt = melt(level);
        if (melt == null) {
            String reason = blocked;
            blocked = before;
            cool(reason);
            return;
        }
        ItemStack result = melt.result();
        ItemStack out = items.get(OUTPUT);
        if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, result) || out.getCount() + result.getCount() > out.getMaxStackSize())) {
            cool("output_full");
            return;
        }
        if (litTime <= 0 && !ignite(level)) {
            cool("no_fuel");
            return;
        }
        blocked = "";
        total = melt.time();
        progress++;
        if (progress < total) {
            return;
        }
        progress = 0;
        for (int i = 0; i < melt.slots().length; i++) {
            items.get(melt.slots()[i]).shrink(melt.counts()[i]);
        }
        if (out.isEmpty()) {
            items.set(OUTPUT, result);
        } else {
            out.grow(result.getCount());
        }
        experience += melt.experience();
        Vec3 mouth = Vec3.atCenterOf(worldPosition);
        level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.5F, 0.8F + level.getRandom().nextFloat() * 0.2F);
        setChanged();
    }

    /** Burns the next piece of fuel; false when there is none. */
    private boolean ignite(ServerLevel level) {
        ItemStack fuel = items.get(FUEL);
        int burn = fuel.isEmpty() ? 0
                : ResolvableInt.getFromItem(fuel, DataComponents.COOKING_FUEL, CookingFuel::burnTime, getLootContext(level, fuel), 0);
        if (burn <= 0) {
            return false;
        }
        litTime = burn;
        litDuration = burn;
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        fuel.shrink(1);
        if (fuel.isEmpty() && remainder != null) {
            items.set(FUEL, remainder.create());
        }
        setChanged();
        return true;
    }

    /** No alloy to make: the half-melted metal slowly cools back. */
    private void cool(String reason) {
        boolean changed = !blocked.equals(reason);
        blocked = reason;
        if (progress > 0) {
            progress = Math.max(0, progress - 2);
            changed = true;
        }
        if (changed) {
            setChanged();
        }
    }

    /** The experience of every alloy made since it was last taken out, as orbs at the player. */
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

    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.hex_furnace");
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
        return new HexFurnaceMenu(containerId, inventory, this, data, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    /** What each slot takes: anything into the inputs, only fuel into the fuel slot, nothing into the output. */
    public static boolean accepts(int slot, ItemStack stack) {
        return slot < INPUTS || slot == FUEL && stack.has(DataComponents.COOKING_FUEL);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    /** Hoppers fill an input that already holds the same item, else an empty one. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (slot >= INPUTS) {
            return accepts(slot, stack);
        }
        ItemStack here = items.get(slot);
        if (!here.isEmpty()) {
            return ItemStack.isSameItemSameComponents(here, stack);
        }
        for (int other = 0; other < INPUTS; other++) {
            if (ItemStack.isSameItemSameComponents(items.get(other), stack)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT;
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        litTime = input.getIntOr("lit_time", 0);
        litDuration = input.getIntOr("lit_duration", 0);
        progress = input.getIntOr("progress", 0);
        total = input.getIntOr("total", 0);
        experience = input.getFloatOr("experience", 0);
        blocked = input.getStringOr("blocked", "");
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        known = new HashSet<>(input.read("known", Codec.STRING.listOf()).orElse(List.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("lit_time", litTime);
        output.putInt("lit_duration", litDuration);
        output.putInt("progress", progress);
        output.putInt("total", total);
        output.putFloat("experience", experience);
        output.putString("blocked", blocked);
        if (owner != null) {
            output.store("owner", UUIDUtil.CODEC, owner);
        }
        output.store("known", Codec.STRING.listOf(), List.copyOf(known));
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
