package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.HeatUser;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.StillRune;
import com.pockyl.hextale.runework.ToolEffects;

import org.jspecify.annotations.Nullable;

/**
 * The Chem Still: a copper pot still over a Chem Burner. Vials of raw shimmer poured into its boiler come round through
 * the swan neck and the cooling coil and drip into the receiver as refined shimmer - two raw vials make one refined. It
 * works only over a lit burner, and the burner burns only while the still has raw shimmer to boil and room for the
 * result. Hoppers feed raw vials from the top and the sides and take refined ones from the bottom.
 */
public class ChemStillBlockEntity extends BlockEntity implements WorldlyContainer, HeatUser {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    /** Raw vials one refined vial takes. */
    public static final int RAW_PER_REFINED = 2;
    /** Ticks over a lit burner for one refined vial. */
    public static final int REFINE_TICKS = 400;
    private static final int SYNC_TICKS = 10;
    private static final int[] TOP_AND_SIDES = {INPUT};
    private static final int[] BOTTOM = {OUTPUT};

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private boolean dirty;

    public ChemStillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEM_STILL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChemStillBlockEntity still) {
        still.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        boolean working = wantsHeat() && ChemBurnerBlockEntity.under(level, worldPosition) != null;
        if (working) {
            progress++;
            dirty = true;
            if (progress >= refineTicks()) {
                progress = 0;
                refine(level);
            }
        }
        if (working != state.getValue(ChemStillBlock.WORKING)) {
            level.setBlock(worldPosition, state.setValue(ChemStillBlock.WORKING, working), Block.UPDATE_ALL);
        }
        if (dirty && level.getGameTime() % SYNC_TICKS == 0) {
            dirty = false;
            changed();
        }
    }

    /** Raw shimmer to boil and room in the receiver. */
    @Override
    public boolean wantsHeat() {
        return isRaw(input) && input.getCount() >= RAW_PER_REFINED && output.getCount() < output.getMaxStackSize();
    }

    public int refineTicks() {
        int haste = strength(StillRune.HASTE);
        return haste > 0 ? Math.round(REFINE_TICKS * HolderRune.at(StillRune.FASTER, haste)) : REFINE_TICKS;
    }

    /** Two raw vials become one refined; Steam may give a second, Radiance experience, Purification a crystal. */
    private void refine(ServerLevel level) {
        input.shrink(RAW_PER_REFINED);
        Vec3 at = Vec3.atCenterOf(worldPosition);
        int made = 1;
        int steam = strength(StillRune.STEAM);
        if (steam > 0 && level.getRandom().nextFloat() < HolderRune.at(StillRune.SECOND_VIAL, steam)) {
            made++;
            ToolEffects.puff(level, at.add(0, 0.3, 0), StillRune.STEAM.rune(), 10, 0.3);
        }
        int room = output.isEmpty() ? 16 : output.getMaxStackSize() - output.getCount();
        made = Math.min(made, room);
        if (output.isEmpty()) {
            output = ChemVialItem.of(Brew.REFINED_SHIMMER, made);
        } else {
            output.grow(made);
        }
        int radiance = strength(StillRune.RADIANCE);
        if (radiance > 0) {
            ExperienceOrb.award(level, at.add(0, 0.5, 0), HolderRune.at(StillRune.EXPERIENCE, radiance));
            ToolEffects.puff(level, at.add(0, 0.4, 0), StillRune.RADIANCE.rune(), 6, 0.3);
        }
        int purification = strength(StillRune.PURIFICATION);
        if (purification > 0 && level.getRandom().nextFloat() < HolderRune.at(StillRune.CRYSTAL, purification)) {
            Containers.dropItemStack(level, at.x, at.y + 0.6, at.z, new ItemStack(ModItems.SYNTHETIC_CRYSTAL.get()));
            ToolEffects.puff(level, at.add(0, 0.6, 0), StillRune.PURIFICATION.rune(), 16, 0.35);
        }
        level.sendParticles(ModParticles.SHIMMER_MOTE.get(), at.x, at.y + 0.2, at.z, 10, 0.25, 0.2, 0.25, 0.01);
        level.playSound(null, worldPosition, ModSounds.CHEM_STILL_DRIP.get(), SoundSource.BLOCKS, 0.7F,
                0.95F + level.getRandom().nextFloat() * 0.1F);
        changed();
    }

    public static boolean isRaw(ItemStack stack) {
        return ChemVialItem.brew(stack) == Brew.RAW_SHIMMER;
    }

    public ItemStack input() {
        return input;
    }

    public ItemStack output() {
        return output;
    }

    /** Share of the way to the next refined vial. */
    public float progress() {
        return progress / (float) refineTicks();
    }

    /** Pours raw vials into the boiler, as many as fit; returns how many. */
    public int feed(ItemStack stack) {
        if (!isRaw(stack) || !input.isEmpty() && !ItemStack.isSameItemSameComponents(input, stack)) {
            return 0;
        }
        int taken = Math.min(stack.getCount(), stack.getMaxStackSize() - input.getCount());
        if (taken <= 0) {
            return 0;
        }
        if (input.isEmpty()) {
            input = stack.copyWithCount(taken);
        } else {
            input.grow(taken);
        }
        changed();
        return taken;
    }

    public ItemStack takeOutput() {
        ItemStack taken = output;
        output = ItemStack.EMPTY;
        changed();
        return taken;
    }

    /** One line: what the still is doing. */
    public Component status() {
        if (!isRaw(input) || input.getCount() < RAW_PER_REFINED) {
            return Component.translatable("block.hextale.chem_still.status.empty", input.getCount());
        }
        if (output.getCount() >= output.getMaxStackSize()) {
            return Component.translatable("block.hextale.chem_still.status.full");
        }
        if (ChemBurnerBlockEntity.under(level, worldPosition) == null) {
            return Component.translatable("block.hextale.chem_still.status.cold");
        }
        return Component.translatable("block.hextale.chem_still.status.working", Math.round(progress() * 100));
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public int strength(StillRune rune) {
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

    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : TOP_AND_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == INPUT && isRaw(stack);
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
        return input.isEmpty() && output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == INPUT ? input : output;
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
        if (slot == INPUT) {
            input = stack;
        } else {
            output = stack;
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == INPUT && isRaw(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        input = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.input = input.read("input", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        output = input.read("output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        progress = input.getIntOr("progress", 0);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!input.isEmpty()) {
            output.store("input", ItemStack.CODEC, input);
        }
        if (!this.output.isEmpty()) {
            output.store("output", ItemStack.CODEC, this.output);
        }
        output.putInt("progress", progress);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.STILL_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.STILL_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
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
