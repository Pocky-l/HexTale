package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.TankRune;
import com.pockyl.hextale.runework.ToolEffects;

import org.jspecify.annotations.Nullable;

/**
 * The Axolotl Tank: a rusted iron aquarium of chem glass where an axolotl lives well fed. The creature heals so fast it
 * sheds a little of itself as it goes - fed fish, it now and then leaves a lump of Regenerative Slime, the living half
 * of shimmer. Nothing is done to the animal: it swims, eats and sheds, and a bucket takes it out again whole (breaking the
 * tank gives the bucket back too). Fish come by hand or hopper; slime leaves through the bottom.
 */
public class AxolotlTankBlockEntity extends BlockEntity implements WorldlyContainer, com.pockyl.hextale.port.Removal.SideEffects {
    public static final int FOOD = 0;
    public static final int OUTPUT = 1;
    /** Ticks between two sheddings of a fed axolotl. */
    public static final int SHED_TICKS = 1800;
    /** Meals it keeps eaten ahead; a fish is one, a bucket of tropical fish four. */
    public static final int MAX_MEALS = 16;
    public static final int BUCKET_MEALS = 4;
    private static final int HEALING_EVERY = 80;
    private static final int[] TOP_AND_SIDES = {FOOD};
    private static final int[] BOTTOM = {OUTPUT};

    /** The axolotl as the bucket it came in (keeps its variant and name); empty when the tank has none. */
    private ItemStack axolotl = ItemStack.EMPTY;
    private ItemStack food = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int meals;
    private int progress;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;

    public AxolotlTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AXOLOTL_TANK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AxolotlTankBlockEntity tank) {
        tank.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        if (axolotl.isEmpty()) {
            return;
        }
        if (meals <= 0 && !food.isEmpty()) {
            food.shrink(1);
            meals = 1;
            changed();
        }
        if (meals <= 0 || output.getCount() >= output.getMaxStackSize()) {
            return;
        }
        progress++;
        if (progress >= shedTicks()) {
            progress = 0;
            shed(level);
        }
        if (level.getGameTime() % HEALING_EVERY == 0) {
            heal(level);
        }
    }

    /** Ticks between sheddings; Haste makes it sooner. */
    public int shedTicks() {
        int haste = strength(TankRune.HASTE);
        return haste > 0 ? Math.round(SHED_TICKS * HolderRune.at(TankRune.SOONER, haste)) : SHED_TICKS;
    }

    /** The axolotl leaves a lump of slime: a meal is gone (unless the Dam spares it), maybe one lump more, experience. */
    private void shed(ServerLevel level) {
        Vec3 at = Vec3.atCenterOf(worldPosition);
        int made = 1;
        int spring = strength(TankRune.SPRING);
        if (spring > 0 && level.getRandom().nextFloat() < HolderRune.at(TankRune.MORE_SLIME, spring)) {
            made++;
            ToolEffects.puff(level, at, TankRune.SPRING.rune(), 10, 0.3);
        }
        int dam = strength(TankRune.DAM);
        if (dam > 0 && level.getRandom().nextFloat() < HolderRune.at(TankRune.MEAL_SPARED, dam)) {
            ToolEffects.puff(level, at, TankRune.DAM.rune(), 6, 0.3);
        } else {
            meals--;
        }
        int radiance = strength(TankRune.RADIANCE);
        if (radiance > 0) {
            ExperienceOrb.award(level, at.add(0, 0.6, 0), HolderRune.at(TankRune.EXPERIENCE, radiance));
            ToolEffects.puff(level, at.add(0, 0.4, 0), TankRune.RADIANCE.rune(), 6, 0.3);
        }
        ItemStack slime = new ItemStack(ModItems.REGENERATIVE_SLIME.get(), made);
        if (output.isEmpty()) {
            output = slime;
        } else {
            output.grow(Math.min(made, output.getMaxStackSize() - output.getCount()));
        }
        level.sendParticles(ModParticles.bubble(ModParticles.WATER), at.x, at.y - 0.1, at.z, 14, 0.2, 0.2, 0.2, 0.02);
        level.sendParticles(ModParticles.SHIMMER_MOTE.get(), at.x, at.y + 0.1, at.z, 6, 0.2, 0.15, 0.2, 0.005);
        level.playSound(null, worldPosition, ModSounds.AXOLOTL_TANK_SHED.get(), SoundSource.BLOCKS, 0.6F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        changed();
    }

    /** Healing: the players around a fed tank keep Regeneration. */
    private void heal(ServerLevel level) {
        int healing = strength(TankRune.HEALING);
        if (healing <= 0) {
            return;
        }
        AABB around = new AABB(worldPosition).inflate(TankRune.HEALING_RADIUS);
        for (Player player : level.getEntitiesOfClass(Player.class, around)) {
            if (ToolEffects.keep(player, MobEffects.REGENERATION, HolderRune.at(TankRune.HEALING_TICKS, healing), 0)) {
                ToolEffects.puff(level, player.position().add(0, 1, 0), TankRune.HEALING.rune(), 8, 0.4);
            }
        }
    }

    // ---- The axolotl --------------------------------------------------------------------------------------------------

    public boolean hasAxolotl() {
        return !axolotl.isEmpty();
    }

    /** Its colour, for the renderer. */
    public Axolotl.Variant variant() {
        // 1.21.1 keeps the bucket's axolotl in its entity data.
        return Axolotl.Variant.byId(axolotl.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, net.minecraft.world.item.component.CustomData.EMPTY)
                .copyTag().getInt(Axolotl.VARIANT_TAG));
    }

    /** Lets the axolotl of a bucket in; false when the tank already has one or the stack is no axolotl bucket. */
    public boolean letIn(ItemStack bucket) {
        if (!axolotl.isEmpty() || !bucket.is(Items.AXOLOTL_BUCKET)) {
            return false;
        }
        axolotl = bucket.copyWithCount(1);
        progress = 0;
        changed();
        return true;
    }

    /** Takes the axolotl out, back in its bucket (empty stack when there is none). */
    public ItemStack takeOut() {
        ItemStack taken = axolotl;
        axolotl = ItemStack.EMPTY;
        progress = 0;
        changed();
        return taken;
    }

    // ---- Food and slime -----------------------------------------------------------------------------------------------

    public static boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.FISHES);
    }

    public int meals() {
        return meals;
    }

    /** A bucket of tropical fish: four meals at once. False when the axolotl is full. */
    public boolean feedBucket() {
        if (meals + BUCKET_MEALS > MAX_MEALS) {
            return false;
        }
        meals += BUCKET_MEALS;
        changed();
        return true;
    }

    /** Puts fish into its food, as many as fit; returns how many. */
    public int feed(ItemStack stack) {
        if (!isFood(stack) || !food.isEmpty() && !ItemStack.isSameItemSameComponents(food, stack)) {
            return 0;
        }
        int taken = Math.min(stack.getCount(), stack.getMaxStackSize() - food.getCount());
        if (taken <= 0) {
            return 0;
        }
        if (food.isEmpty()) {
            food = stack.copyWithCount(taken);
        } else {
            food.grow(taken);
        }
        changed();
        return taken;
    }

    public ItemStack output() {
        return output;
    }

    public ItemStack takeOutput() {
        ItemStack taken = output;
        output = ItemStack.EMPTY;
        changed();
        return taken;
    }

    /** Share of the way to the next shedding (0 while it waits). */
    public float progress() {
        return progress / (float) shedTicks();
    }

    /** One line: what the tank is doing. */
    public Component status() {
        if (axolotl.isEmpty()) {
            return Component.translatable("block.hextale.axolotl_tank.status.empty");
        }
        if (meals <= 0 && food.isEmpty()) {
            return Component.translatable("block.hextale.axolotl_tank.status.hungry");
        }
        if (output.getCount() >= output.getMaxStackSize()) {
            return Component.translatable("block.hextale.axolotl_tank.status.full");
        }
        int seconds = Math.max(0, shedTicks() - progress) / 20;
        return Component.translatable("block.hextale.axolotl_tank.status.fed", meals + food.getCount(), seconds / 60,
                String.format("%02d", seconds % 60));
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public int strength(TankRune rune) {
        return runes.strength(rune.rune());
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        com.pockyl.hextale.port.Removal.vanilla(this, pos, state);
        if (level != null && !axolotl.isEmpty()) {
            // The axolotl leaves unharmed, in its bucket.
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, axolotl);
            axolotl = ItemStack.EMPTY;
        }
    }

    // ---- Container: fish in from the top and sides, slime out of the bottom --------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : TOP_AND_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == FOOD && isFood(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return food.isEmpty() && output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == FOOD ? food : output;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = getItem(slot).split(count);
        changed();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = getItem(slot);
        setItemQuietly(slot, ItemStack.EMPTY);
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        setItemQuietly(slot, stack);
        changed();
    }

    private void setItemQuietly(int slot, ItemStack stack) {
        if (slot == FOOD) {
            food = stack;
        } else {
            output = stack;
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == FOOD && isFood(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        food = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        axolotl = input.read("axolotl", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        food = input.read("food", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = input.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        meals = input.getIntOr("meals", 0);
        progress = input.getIntOr("progress", 0);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        if (!axolotl.isEmpty()) {
            output.store("axolotl", ItemStack.CODEC, axolotl);
        }
        if (!food.isEmpty()) {
            output.store("food", ItemStack.CODEC, food);
        }
        if (!this.output.isEmpty()) {
            output.store("output", ItemStack.CODEC, this.output);
        }
        output.putInt("meals", meals);
        output.putInt("progress", progress);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.TANK_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.TANK_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
        super.removeComponentsFromTag(output.tag());
        output.discard("runes");
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
