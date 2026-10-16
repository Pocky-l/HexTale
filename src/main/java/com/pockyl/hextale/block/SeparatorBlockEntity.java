package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.essence.Essence;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.essence.Reagents;
import com.pockyl.hextale.menu.SeparatorMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.rune.SimpleRune;

import org.jspecify.annotations.Nullable;

/**
 * The Separator: a mixture goes into its vessel (from a flask or through pipes at the back and sides); a reagent binds
 * its own element and pulls it out, drop by drop, into a second vessel. What is left of the mixture - one element
 * fewer - leaves through the front once the reagent's element is gone from it; the drawn-out element, a concentrate,
 * leaves through the top. Both can also be poured into empty flasks. One reagent item binds {@value Reagents#BINDS}
 * drops.
 */
public class SeparatorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, EssenceNetwork.Receiver {
    public static final int POUR = 0;
    public static final int REAGENT = 1;
    public static final int SUPPLY = 2;
    public static final int MIX_OUT = 3;
    public static final int EXTRACT_OUT = 4;
    public static final int CAPACITY = 64;
    /** Ticks per separating step, and drops moved per step. */
    public static final int STEP_TICKS = 10;
    private static final int STEP_DROPS = 2;
    /** A partly filled flask is poured once nothing has come in for this long. */
    private static final int IDLE_TICKS = 40;
    private static final int[] TOP = {REAGENT};
    private static final int[] SIDES = {POUR, SUPPLY, REAGENT};
    private static final int[] BOTTOM = {POUR, MIX_OUT, EXTRACT_OUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private final EssenceTank mix = new EssenceTank(CAPACITY);
    private final EssenceTank extract = new EssenceTank(CAPACITY);
    /** Drops the reagent already used can still bind, and its element (-1 for none). */
    private int charge;
    private int chargeElement = -1;
    private int step;
    private long lastReceived = Long.MIN_VALUE / 2;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> charge;
                case 1 -> chargeElement;
                default -> step;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> charge = value;
                case 1 -> chargeElement = value;
                default -> step = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public SeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEPARATOR.get(), pos, state);
    }

    public EssenceTank mix() {
        return mix;
    }

    public EssenceTank extract() {
        return extract;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(SeparatorBlock.FACING) ? state.getValue(SeparatorBlock.FACING) : Direction.NORTH;
    }

    /** The element being drawn out now: the bound one while charge is left, else the reagent's. */
    @Nullable
    public SimpleRune separating() {
        if (charge > 0 && chargeElement >= 0) {
            return SimpleRune.values()[chargeElement];
        }
        return Reagents.element(items.get(REAGENT));
    }

    /** The mixture may leave: the element being separated is gone from it (or nothing is being separated). */
    public boolean mixReady() {
        SimpleRune element = separating();
        return element == null || mix.contents().get(element) == 0;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SeparatorBlockEntity separator) {
        boolean changed = separator.pourFlask();
        changed |= separator.separate();
        changed |= separator.fillFlasks();
        if (level.getGameTime() % EssenceTankBlockEntity.FLOW_EVERY == 0) {
            changed |= separator.pushToPipes();
        }
        if (changed) {
            separator.setChanged();
        }
    }

    private boolean pourFlask() {
        ItemStack flask = items.get(POUR);
        Essence inside = EssenceFlaskItem.contents(flask);
        if (!EssenceFlaskItem.isFlask(flask) || inside.isEmpty() || mix.space() <= 0) {
            return false;
        }
        int poured = mix.fill(inside, false);
        ItemStack after = EssenceFlaskItem.filled(inside.minus(inside.part(poured)));
        ItemStack supply = items.get(SUPPLY);
        if (EssenceFlaskItem.isEmptyFlask(after) && (supply.isEmpty() || EssenceFlaskItem.isEmptyFlask(supply)
                && supply.getCount() < supply.getMaxStackSize())) {
            // The emptied flask joins the empty ones, ready to carry the products away.
            if (supply.isEmpty()) {
                items.set(SUPPLY, after);
            } else {
                supply.grow(1);
            }
            items.set(POUR, ItemStack.EMPTY);
        } else {
            items.set(POUR, after);
        }
        lastReceived = level.getGameTime();
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 1.0F);
        return true;
    }

    /** Whether drops can still be drawn out: room for them and a reagent (or charge) for their element. */
    private boolean canSeparate() {
        SimpleRune element = separating();
        if (element == null || mix.contents().get(element) == 0 || extract.space() <= 0) {
            return false;
        }
        if (!extract.isEmpty() && extract.contents().pureElement() != element) {
            return false;
        }
        return charge > 0 || Reagents.element(items.get(REAGENT)) == element;
    }

    private boolean separate() {
        SimpleRune element = separating();
        if (element == null || mix.contents().get(element) == 0) {
            boolean had = step != 0;
            step = 0;
            return had;
        }
        SimpleRune held = extract.contents().pureElement();
        if (!extract.isEmpty() && held != element || extract.space() <= 0) {
            return false;
        }
        if (++step < STEP_TICKS) {
            return false;
        }
        step = 0;
        if (charge <= 0) {
            ItemStack reagent = items.get(REAGENT);
            if (Reagents.element(reagent) != element) {
                return false;
            }
            reagent.shrink(1);
            charge = Reagents.BINDS;
            chargeElement = element.ordinal();
        }
        int moved = Math.min(Math.min(STEP_DROPS, mix.contents().get(element)), Math.min(charge, extract.space()));
        Essence drawn = Essence.pure(element, moved);
        mix.remove(drawn);
        extract.fill(drawn, false);
        charge -= moved;
        if (charge <= 0) {
            chargeElement = -1;
        }
        level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.5F, 1.2F);
        return true;
    }

    private boolean fillFlasks() {
        boolean idle = level.getGameTime() - lastReceived > IDLE_TICKS && EssenceFlaskItem.contents(items.get(POUR)).isEmpty();
        boolean changed = false;
        if (mixReady() && (mix.amount() >= EssenceFlaskItem.CAPACITY || idle)) {
            changed |= fill(MIX_OUT, mix);
        }
        if (extract.amount() >= EssenceFlaskItem.CAPACITY || mixReady() || idle && !canSeparate()) {
            changed |= fill(EXTRACT_OUT, extract);
        }
        return changed;
    }

    private boolean fill(int slot, EssenceTank tank) {
        ItemStack supply = items.get(SUPPLY);
        if (tank.isEmpty() || !items.get(slot).isEmpty() || !EssenceFlaskItem.isEmptyFlask(supply)) {
            return false;
        }
        items.set(slot, EssenceFlaskItem.filled(tank.drain(EssenceFlaskItem.CAPACITY, false)));
        supply.shrink(1);
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.0F);
        return true;
    }

    private boolean pushToPipes() {
        boolean moved = false;
        if (mixReady()) {
            moved |= push(mix, facing());
        }
        moved |= push(extract, Direction.UP);
        return moved;
    }

    private boolean push(EssenceTank tank, Direction side) {
        if (tank.isEmpty()) {
            return false;
        }
        Essence offer = tank.drain(EssenceTankBlockEntity.FLOW, true);
        int moved = EssenceNetwork.push(level, worldPosition, side, offer);
        if (moved > 0) {
            tank.remove(offer.part(moved));
            return true;
        }
        return false;
    }

    /** Pipes bring the mixture in at the back and the sides. */
    @Override
    public int receive(Direction side, Essence offer, boolean simulate) {
        Direction front = facing();
        if (side == front || side.getAxis() == Direction.Axis.Y) {
            return 0;
        }
        int taken = mix.fill(offer, simulate);
        if (taken > 0 && !simulate) {
            lastReceived = level.getGameTime();
            setChanged();
        }
        return taken;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.separator");
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
        return new SeparatorMenu(containerId, inventory, this, data, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return 5;
    }

    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case POUR -> EssenceFlaskItem.isFlask(stack) && !EssenceFlaskItem.contents(stack).isEmpty();
            case REAGENT -> Reagents.element(stack) != null;
            case SUPPLY -> EssenceFlaskItem.isEmptyFlask(stack);
            default -> false;
        };
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
        return accepts(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == MIX_OUT || slot == EXTRACT_OUT || slot == POUR && EssenceFlaskItem.isEmptyFlask(stack);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        mix.load(input, "mix");
        extract.load(input, "extract");
        charge = input.getIntOr("charge", 0);
        chargeElement = input.getIntOr("charge_element", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        mix.save(output, "mix");
        extract.save(output, "extract");
        output.putInt("charge", charge);
        output.putInt("charge_element", chargeElement);
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
