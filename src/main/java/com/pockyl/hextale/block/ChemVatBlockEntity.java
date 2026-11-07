package com.pockyl.hextale.block;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.CarboyReach;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.crafting.TemperingRecipe;
import com.pockyl.hextale.multiblock.MultiblockCore;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.thevoid.Thinning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The heart of a formed Chem Vat ({@link com.pockyl.hextale.multiblock.ChemVat}), kept by its valve. The bath holds doses
 * of acid and refined shimmer poured in as vials; the fire under it burns fuel vials (combustible mix, ember oil). Metal
 * dropped in (three input slots) tempers by a {@link TemperingRecipe} while the fire burns and the bath holds enough,
 * spending the recipe's doses, and the result waits in the valve. Hoppers feed vials and metal from the top and the
 * sides (through any block of the vat) and take the result from below. Things thrown onto the grate fall in.
 */
public class ChemVatBlockEntity extends BlockEntity implements WorldlyContainer, MultiblockCore, com.pockyl.hextale.port.Removal.SideEffects {
    public static final int INPUTS = TemperingRecipe.SLOTS;
    /** Fuel vials waiting to burn; reagent vials put here by a hopper are poured into the bath. */
    public static final int VIALS = INPUTS;
    public static final int OUTPUT = INPUTS + 1;
    public static final int SIZE = OUTPUT + 1;
    /** Doses of each reagent the bath holds. */
    public static final int MAX_DOSES = 16;
    private static final int SWALLOW_TICKS = 10;
    private static final int SYNC_TICKS = 10;
    private static final int[] TOP_AND_SIDES = {0, 1, 2, VIALS};
    private static final int[] BOTTOM = {OUTPUT};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int acid;
    private int shimmer;
    private int fuelTicks;
    private int progress;
    private int recipeTime;
    private @Nullable Formed formed;
    private final CarboyReach.Look bathLook = new CarboyReach.Look();
    private final CarboyReach.Look fireLook = new CarboyReach.Look();
    private boolean dirty;

    public ChemVatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEM_VAT.get(), pos, state);
    }

    // ---- Shape of the vat ---------------------------------------------------------------------------------------------

    /** The hollow middle of the vat (the bath), from the valve's facing. */
    public static BlockPos heart(BlockPos valve, Direction front) {
        return valve.relative(front.getOpposite());
    }

    public BlockPos heart() {
        return heart(worldPosition, getBlockState().getValue(ChemVatBlock.FACING));
    }

    public boolean isFormed() {
        return formed != null && getBlockState().getValue(ChemVatBlock.FORMED);
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

    /** Taken apart: it forgets the vat; the bath drains away (into the land's Thinning), contents stay in the valve. */
    @Override
    @SuppressWarnings("deprecation")
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (!state.getValue(ChemVatBlock.FORMED)) {
            formed = null;
            progress = 0;
            fuelTicks = 0;
            if (level instanceof ServerLevel server && acid + shimmer > 0) {
                Thinning.add(server, worldPosition, (acid + shimmer) * Thinning.BREW, Thinning.Source.POURED);
            }
            acid = 0;
            shimmer = 0;
        }
    }

    // ---- Ticking ------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChemVatBlockEntity vat) {
        if (vat.isFormed()) {
            vat.tick((ServerLevel) level, state);
        }
    }

    private void tick(ServerLevel level, BlockState state) {
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), SWALLOW_TICKS) == 0) {
            swallow(level);
            drainReagentVials();
        }
        TemperingRecipe.Found found = TemperingRecipe.find(inputs(), false);
        boolean ready = found != null && acid >= found.recipe().acid() && shimmer >= found.recipe().shimmer() && fits(found.recipe());
        if (found != null && !ready && fits(found.recipe()) && bathLook.due(level, worldPosition)) {
            drawBath(level, found.recipe());
        }
        if (ready && fuelTicks <= 0) {
            burnNextVial(level, state.getValue(ChemVatBlock.LIT));
        }
        boolean working = ready && fuelTicks > 0;
        if (working) {
            fuelTicks--;
            recipeTime = found.recipe().time();
            if (++progress >= recipeTime) {
                temper(level, found);
                progress = 0;
            }
            dirty = true;
        } else if (progress > 0 && found == null) {
            progress = 0;
            dirty = true;
        }
        if (working != state.getValue(ChemVatBlock.LIT)) {
            level.setBlock(worldPosition, state.setValue(ChemVatBlock.LIT, working), Block.UPDATE_ALL);
        }
        if (dirty && level.getGameTime() % SYNC_TICKS == 0) {
            dirty = false;
            sync();
        }
    }

    private List<ItemStack> inputs() {
        return items.subList(0, INPUTS);
    }

    private boolean fits(TemperingRecipe recipe) {
        ItemStack out = items.get(OUTPUT);
        ItemStack made = recipe.result().create();
        return out.isEmpty() || ItemStack.isSameItemSameComponents(out, made) && out.getCount() + made.getCount() <= out.getMaxStackSize();
    }

    /** The fire under the bath: a vial, else fuel from the carboys around (a burning fire reaches for it at once). */
    private void burnNextVial(ServerLevel level, boolean burning) {
        ItemStack vials = items.get(VIALS);
        Brew brew = ChemVialItem.brew(vials);
        if (isFuel(brew)) {
            fuelTicks = brew.burnTicks();
            vials.shrink(1);
        } else if (burning || fireLook.due(level, worldPosition)) {
            Brew drawn = fireLook.draw(level, heart(), Vec3.atCenterOf(heart()), CarboyReach.burns(ChemVatBlockEntity::isFuel), Comparator.comparingInt(Brew::burnTicks));
            if (drawn != null) {
                fuelTicks = drawn.burnTicks();
            }
        }
    }

    /** A bath too thin for the metal in the slots takes a dose of what it lacks from the carboys around. */
    private void drawBath(ServerLevel level, TemperingRecipe recipe) {
        Vec3 bath = Vec3.atCenterOf(heart());
        if (acid < recipe.acid() && bathLook.draw(level, heart(), bath, brew -> brew == Brew.ACID) != null) {
            pour(Brew.ACID);
            dirty = true;
        }
        if (shimmer < recipe.shimmer() && bathLook.draw(level, heart(), bath, brew -> brew == Brew.REFINED_SHIMMER) != null) {
            pour(Brew.REFINED_SHIMMER);
            dirty = true;
        }
    }

    private void temper(ServerLevel level, TemperingRecipe.Found found) {
        TemperingRecipe recipe = found.recipe();
        for (int i = 0; i < recipe.inputs().size(); i++) {
            items.get(found.slots()[i]).shrink(recipe.inputs().get(i).count());
        }
        acid -= recipe.acid();
        shimmer -= recipe.shimmer();
        ItemStack made = recipe.result().create();
        ItemStack out = items.get(OUTPUT);
        if (out.isEmpty()) {
            items.set(OUTPUT, made);
        } else {
            out.grow(made.getCount());
        }
        if (recipe.thinning() > 0) {
            Thinning.add(level, worldPosition, recipe.thinning(), Thinning.Source.BREWED);
        }
        Vec3 bath = Vec3.atCenterOf(heart());
        level.sendParticles(ModParticles.vapour(Brew.REFINED_SHIMMER.color()), bath.x, bath.y + 0.6, bath.z, 14, 0.3, 0.2, 0.3, 0.02);
        level.playSound(null, heart(), SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.7F);
        sync();
    }

    /** Things thrown onto the grate fall in: metal into the input slots, vials into the bath or under it. */
    private void swallow(ServerLevel level) {
        BlockPos grate = heart().above();
        AABB above = new AABB(grate).expandTowards(0, 1, 0);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, above, e -> e.isAlive() && accepts(e.getItem()))) {
            ItemStack stack = entity.getItem();
            int taken = insert(stack);
            if (taken > 0) {
                stack.shrink(taken);
                entity.setItem(stack);
                if (stack.isEmpty()) {
                    entity.discard();
                }
                level.playSound(null, grate, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.3F, 1.3F);
            }
        }
    }

    /** Acid and refined shimmer a hopper put among the vials go into the bath while it has room. */
    private void drainReagentVials() {
        ItemStack vials = items.get(VIALS);
        Brew brew = ChemVialItem.brew(vials);
        while (!vials.isEmpty() && isReagent(brew) && room(brew) > 0) {
            pour(brew);
            vials.shrink(1);
        }
    }

    // ---- Filling ------------------------------------------------------------------------------------------------------

    public static boolean isFuel(@Nullable Brew brew) {
        return brew == Brew.FUEL || brew == Brew.EMBER_OIL;
    }

    public static boolean isReagent(@Nullable Brew brew) {
        return brew == Brew.ACID || brew == Brew.REFINED_SHIMMER;
    }

    /** Whether the vat takes this at all: fuel or reagent vials, or metal some tempering asks for. */
    public boolean accepts(ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        if (brew != null) {
            return isFuel(brew) || isReagent(brew);
        }
        return TemperingRecipe.isInput(stack, level != null && level.isClientSide());
    }

    private int room(Brew brew) {
        return MAX_DOSES - (brew == Brew.ACID ? acid : shimmer);
    }

    private void pour(Brew brew) {
        if (brew == Brew.ACID) {
            acid++;
        } else {
            shimmer++;
        }
    }

    /** Takes as much of the stack as fits (reagents into the bath, fuel under it, metal into the slots); returns how many. */
    public int insert(ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        int taken = 0;
        if (isReagent(brew)) {
            taken = Math.min(stack.getCount(), room(brew));
            for (int i = 0; i < taken; i++) {
                pour(brew);
            }
        } else if (isFuel(brew)) {
            taken = merge(VIALS, stack);
        } else if (accepts(stack)) {
            for (int slot = 0; slot < INPUTS && taken < stack.getCount(); slot++) {
                ItemStack rest = stack.copyWithCount(stack.getCount() - taken);
                taken += merge(slot, rest);
            }
        }
        if (taken > 0) {
            sync();
        }
        return taken;
    }

    private int merge(int slot, ItemStack stack) {
        ItemStack held = items.get(slot);
        if (held.isEmpty()) {
            int n = Math.min(stack.getCount(), stack.getMaxStackSize());
            items.set(slot, stack.copyWithCount(n));
            return n;
        }
        if (!ItemStack.isSameItemSameComponents(held, stack)) {
            return 0;
        }
        int n = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
        held.grow(n);
        return n;
    }

    public ItemStack takeOutput() {
        ItemStack out = items.get(OUTPUT);
        items.set(OUTPUT, ItemStack.EMPTY);
        if (!out.isEmpty()) {
            sync();
        }
        return out;
    }

    // ---- Reading ------------------------------------------------------------------------------------------------------

    public int acid() {
        return acid;
    }

    public int shimmer() {
        return shimmer;
    }

    public boolean burning() {
        return getBlockState().getValue(ChemVatBlock.LIT);
    }

    /** Share of the way through the current tempering. */
    public float progress() {
        return recipeTime <= 0 ? 0 : progress / (float) recipeTime;
    }

    /** One line: what the vat is doing or what it lacks. */
    public Component status() {
        if (!isFormed()) {
            return Component.translatable("block.hextale.chem_vat.alone");
        }
        TemperingRecipe.Found found = TemperingRecipe.find(inputs(), level != null && level.isClientSide());
        if (found == null) {
            return Component.translatable("block.hextale.chem_vat.status.idle", acid, shimmer);
        }
        if (acid < found.recipe().acid() || shimmer < found.recipe().shimmer()) {
            return Component.translatable("block.hextale.chem_vat.status.bath", acid, found.recipe().acid(), shimmer, found.recipe().shimmer());
        }
        if (!fits(found.recipe())) {
            return Component.translatable("block.hextale.chem_vat.status.full");
        }
        if (fuelTicks <= 0 && !isFuel(ChemVialItem.brew(items.get(VIALS)))) {
            return Component.translatable("block.hextale.chem_vat.status.cold");
        }
        return Component.translatable("block.hextale.chem_vat.status.working", Math.round(progress() * 100));
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Broken: its contents spill out and the vat falls apart. */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        com.pockyl.hextale.port.Removal.vanilla(this, pos, state);
        if (level instanceof ServerLevel serverLevel) {
            Multiblocks.breakApart(serverLevel, pos, pos, formed);
        }
    }

    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : TOP_AND_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isFormed() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        if (slot == VIALS) {
            return isFuel(brew) || isReagent(brew);
        }
        return slot < INPUTS && brew == null && TemperingRecipe.isInput(stack, level != null && level.isClientSide());
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = ContainerHelper.removeItem(items, slot, count);
        sync();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        items.clear();
        ContainerHelper.loadAllItems(input.tag(), items, input.registries());
        acid = input.getIntOr("acid", 0);
        shimmer = input.getIntOr("shimmer", 0);
        fuelTicks = input.getIntOr("fuel", 0);
        progress = input.getIntOr("progress", 0);
        recipeTime = input.getIntOr("recipe_time", 0);
        formed = input.read("formed", Formed.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        ContainerHelper.saveAllItems(output.tag(), items, output.registries());
        output.putInt("acid", acid);
        output.putInt("shimmer", shimmer);
        output.putInt("fuel", fuelTicks);
        output.putInt("progress", progress);
        output.putInt("recipe_time", recipeTime);
        if (formed != null) {
            output.store("formed", Formed.CODEC, formed);
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
