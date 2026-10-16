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
 * The Separator splits a mixture in two. The mixture comes in (from a bottle, or through pipes at the back and the
 * sides) and is worked off {@value #STEP_DROPS} drops at a time: the drops of the reagent's element go into the
 * concentrate vessel, all the others into the rest vessel. Each side has a mode set in the screen or on the block: the
 * mixture comes in through inlets, the concentrate (the wanted product) and the rest (the by-product) leave through
 * their outlets; by default the mixture comes in at the back and the left, the concentrate leaves at the right and the
 * rest at the front. The top is the gas vent. Both products can be bottled. Without a reagent nothing is split. One
 * reagent item binds {@value Reagents#BINDS} drops of its element.
 */
public class SeparatorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, EssenceNetwork.Receiver {
    public static final int POUR = 0;
    public static final int REAGENT = 1;
    public static final int SUPPLY = 2;
    public static final int REST_OUT = 3;
    public static final int EXTRACT_OUT = 4;
    public static final int CAPACITY = 64;
    /** Synced numbers: charge, its element, step, then the four side modes. */
    public static final int DATA_COUNT = 3 + SeparatorBlock.Side.values().length;
    /** Ticks per separating step, and drops of the mixture worked off per step. */
    public static final int STEP_TICKS = 10;
    private static final int STEP_DROPS = 4;
    /** A partly filled bottle is filled once nothing has come in for this long. */
    private static final int IDLE_TICKS = 40;
    private static final int[] TOP = {REAGENT};
    private static final int[] SIDES = {POUR, SUPPLY, REAGENT};
    private static final int[] BOTTOM = {POUR, REST_OUT, EXTRACT_OUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private final EssenceTank mix = new EssenceTank(CAPACITY);
    private final EssenceTank extract = new EssenceTank(CAPACITY);
    private final EssenceTank rest = new EssenceTank(CAPACITY);
    /** Drops the reagent already used can still bind, and its element (-1 for none). */
    private int charge;
    private int chargeElement = -1;
    private int step;
    private long lastReceived = Long.MIN_VALUE / 2;
    /** The mode of each face, in {@link SeparatorBlock.Side} order. */
    private final PortMode[] modes = defaultModes();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> charge;
                case 1 -> chargeElement;
                case 2 -> step;
                default -> modes[index - 3].ordinal();
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> charge = value;
                case 1 -> chargeElement = value;
                case 2 -> step = value;
                default -> modes[index - 3] = PortMode.values()[value];
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public SeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEPARATOR.get(), pos, state);
    }

    /** The mixture waiting to be split. */
    public EssenceTank mix() {
        return mix;
    }

    /** The drawn-out element: the concentrate. */
    public EssenceTank extract() {
        return extract;
    }

    /** Everything else of the mixture. */
    public EssenceTank rest() {
        return rest;
    }

    private static PortMode[] defaultModes() {
        PortMode[] modes = new PortMode[SeparatorBlock.Side.values().length];
        for (SeparatorBlock.Side side : SeparatorBlock.Side.values()) {
            modes[side.ordinal()] = side.defaultMode();
        }
        return modes;
    }

    public PortMode mode(SeparatorBlock.Side side) {
        return modes[side.ordinal()];
    }

    /** The mode of a face of the block (none for the bottom). */
    public PortMode mode(Direction face) {
        SeparatorBlock.Side side = SeparatorBlock.Side.of(face, facing());
        return side == null ? PortMode.NONE : modes[side.ordinal()];
    }

    /** The next mode for a side: inlet, concentrate out, rest out. The joint on the block follows. */
    public void cycle(SeparatorBlock.Side side) {
        modes[side.ordinal()] = modes[side.ordinal()].next();
        if (level != null) {
            BlockState now = getBlockState();
            BlockState looks = SeparatorBlock.withLooks(now, level, worldPosition);
            if (looks != now) {
                level.setBlock(worldPosition, looks, Block.UPDATE_ALL);
            }
        }
        setChanged();
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
        if (inside.isEmpty() || mix.space() <= 0) {
            return false;
        }
        int poured = mix.fill(inside, false);
        ItemStack after = EssenceFlaskItem.filled(inside.minus(inside.part(poured)));
        ItemStack supply = items.get(SUPPLY);
        if (EssenceFlaskItem.isEmptyFlask(after) && (supply.isEmpty() || EssenceFlaskItem.isEmptyFlask(supply)
                && supply.getCount() < supply.getMaxStackSize())) {
            // The emptied bottle joins the glass bottles, ready to carry the products away.
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

    /** The drops the next step works off. */
    private Essence portion() {
        return mix.contents().part(Math.min(STEP_DROPS, mix.amount()));
    }

    /** Why the separator cannot work now (a translation key suffix for the screen), or null when it can. */
    @Nullable
    public String blocked() {
        if (mix.isEmpty()) {
            return "empty";
        }
        SimpleRune element = separating();
        if (element == null) {
            return "no_reagent";
        }
        Essence portion = portion();
        int drawn = portion.get(element);
        if (drawn > 0) {
            int binds = charge + (Reagents.element(items.get(REAGENT)) == element ? Reagents.BINDS : 0);
            if (binds < drawn) {
                return "no_reagent";
            }
            if (extract.space() < drawn || !extract.isEmpty() && extract.contents().pureElement() != element) {
                return "extract_full";
            }
        }
        return rest.space() < portion.total() - drawn ? "rest_full" : null;
    }

    private boolean separate() {
        if (blocked() != null) {
            boolean had = step != 0;
            step = 0;
            return had;
        }
        if (++step < STEP_TICKS) {
            return false;
        }
        step = 0;
        SimpleRune element = separating();
        Essence portion = portion();
        int drawn = portion.get(element);
        if (drawn > charge) {
            items.get(REAGENT).shrink(1);
            charge += Reagents.BINDS;
            chargeElement = element.ordinal();
        }
        mix.remove(portion);
        if (drawn > 0) {
            extract.fill(Essence.pure(element, drawn), false);
            charge -= drawn;
            if (charge <= 0) {
                charge = 0;
                chargeElement = -1;
            }
        }
        rest.fill(portion.minus(Essence.pure(element, drawn)), false);
        level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.5F, 1.2F);
        return true;
    }

    /** Bottles a product once a bottle's worth is ready, or what is left once the mixture is used up. */
    private boolean fillFlasks() {
        boolean done = mix.isEmpty() && level.getGameTime() - lastReceived > IDLE_TICKS
                && EssenceFlaskItem.contents(items.get(POUR)).isEmpty();
        boolean changed = false;
        if (extract.amount() >= EssenceFlaskItem.CAPACITY || done) {
            changed |= fill(EXTRACT_OUT, extract);
        }
        if (rest.amount() >= EssenceFlaskItem.CAPACITY || done) {
            changed |= fill(REST_OUT, rest);
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

    /** The concentrate leaves through its outlets, the rest through theirs. */
    private boolean pushToPipes() {
        boolean moved = false;
        for (SeparatorBlock.Side side : SeparatorBlock.Side.values()) {
            PortMode mode = modes[side.ordinal()];
            if (mode == PortMode.PRODUCT) {
                moved |= push(extract, side.face(facing()));
            } else if (mode == PortMode.WASTE) {
                moved |= push(rest, side.face(facing()));
            }
        }
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

    @Override
    public boolean isOutlet(Direction face) {
        PortMode mode = mode(face);
        return mode == PortMode.PRODUCT || mode == PortMode.WASTE;
    }

    /** Pipes bring the mixture in through the inlets. */
    @Override
    public int receive(Direction side, Essence offer, boolean simulate) {
        if (mode(side) != PortMode.INLET) {
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
            case POUR -> !EssenceFlaskItem.contents(stack).isEmpty();
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
        return slot == REST_OUT || slot == EXTRACT_OUT || slot == POUR && EssenceFlaskItem.isEmptyFlask(stack);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(5, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        mix.load(input, "mix");
        extract.load(input, "extract");
        rest.load(input, "rest");
        int[] saved = input.getIntArray("ports").orElse(new int[0]);
        PortMode[] defaults = defaultModes();
        for (int i = 0; i < modes.length; i++) {
            modes[i] = i < saved.length && saved[i] > 0 && saved[i] < PortMode.values().length ? PortMode.values()[saved[i]] : defaults[i];
        }
        charge = input.getIntOr("charge", 0);
        chargeElement = input.getIntOr("charge_element", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        mix.save(output, "mix");
        extract.save(output, "extract");
        rest.save(output, "rest");
        int[] ports = new int[modes.length];
        for (int i = 0; i < modes.length; i++) {
            ports[i] = modes[i].ordinal();
        }
        output.putIntArray("ports", ports);
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
