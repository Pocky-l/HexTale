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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.menu.SeparatorMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;

import org.jspecify.annotations.Nullable;

/**
 * The Separator splits a mixture in two. The mixture comes in (from a bottle, through pipes pushing into an inlet, or
 * drawn by an inlet out of the Essence Tanks piped to it) and is worked off {@value #STEP_DROPS} mB at a time: the
 * essence chosen on its screen goes into the concentrate vessel, all the others into the rest vessel, and every step that drew something out adds
 * {@value #UNKNOWN_PER_STEP} mB of dark unknown essence to the rest (so the rest is never a concentrate). Amounts are
 * millibuckets. Each side has a mode set in the screen or on the block: the
 * mixture comes in through inlets, the concentrate (the wanted product) and the rest (the by-product) leave through
 * their outlets; by default the mixture comes in at the back and the left, the concentrate leaves at the right and the
 * rest at the front. The top is the gas vent. Both products can be bottled. Drawing an essence out burns blaze powder:
 * one powder draws {@value #FUEL_BINDS} mB of whatever essence is chosen. Without a chosen essence or powder nothing is
 * split. The dark unknown essence can be chosen too: it is filtered out of a mixture into its own vessel, and then no
 * unknown essence is added to the rest - that is the way to clean a mixture.
 */
public class SeparatorBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, EssenceNetwork.Receiver,
        com.pockyl.hextale.chem.HeatUser {
    public static final int POUR = 0;
    public static final int FUEL = 1;
    public static final int SUPPLY = 2;
    public static final int REST_OUT = 3;
    public static final int EXTRACT_OUT = 4;
    public static final int CAPACITY = 64;
    /** Millibuckets of the chosen essence one blaze powder draws out. */
    public static final int FUEL_BINDS = 32;
    /** Synced numbers: fuel left, the chosen essence, step, then the four side modes. */
    public static final int DATA_COUNT = 3 + SeparatorBlock.Side.values().length;
    /** Ticks per separating step, millibuckets of the mixture worked off per step, and of unknown essence each step
     * that drew something out adds to the rest. */
    public static final int STEP_TICKS = 10;
    /** The choice that draws out the dark unknown essence (after the essences' ordinals). */
    public static final int UNKNOWN_CHOICE = Essence.COUNT;
    private static final int STEP_DROPS = 4;
    public static final int UNKNOWN_PER_STEP = 1;
    private static final String UNKNOWN_KEY = "unknown";
    /** A partly filled bottle is filled once nothing has come in for this long. */
    private static final int IDLE_TICKS = 40;
    private static final int[] TOP = {FUEL};
    private static final int[] SIDES = {POUR, SUPPLY, FUEL};
    private static final int[] BOTTOM = {POUR, REST_OUT, EXTRACT_OUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private final EssenceTank mix = new EssenceTank(CAPACITY);
    private final EssenceTank extract = new EssenceTank(CAPACITY);
    private final EssenceTank rest = new EssenceTank(CAPACITY);
    /** Millibuckets the powder already burned can still draw out, and the essence to draw (-1 for none chosen). */
    private int charge;
    private int chosen = -1;
    private int step;
    private long lastReceived = Long.MIN_VALUE / 2;
    /** The mode of each face, in {@link SeparatorBlock.Side} order. */
    private final PortMode[] modes = defaultModes();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> charge;
                case 1 -> chosen;
                case 2 -> step;
                default -> modes[index - 3].ordinal();
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> charge = value;
                case 1 -> chosen = value;
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

    /** The next mode for a side: inlet, concentrate out, rest out, closed. The joint on the block and the pipes
     * follow. */
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

    /** The essence chosen to be drawn out, or null. */
    @Nullable
    public Essence separating() {
        return chosen >= 0 && chosen < Essence.COUNT ? Essence.values()[chosen] : null;
    }

    /** Whether the dark unknown essence is chosen to be drawn out. */
    public boolean separatingUnknown() {
        return chosen == UNKNOWN_CHOICE;
    }

    /** Chooses the essence to draw out (null: none, nothing is split). */
    public void choose(@Nullable Essence essence) {
        choose(essence == null ? -1 : essence.ordinal());
    }

    /** Chooses the dark unknown essence to draw out. */
    public void chooseUnknown() {
        choose(UNKNOWN_CHOICE);
    }

    private void choose(int now) {
        if (now != chosen) {
            chosen = now;
            step = 0;
            setChanged();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SeparatorBlockEntity separator) {
        boolean changed = separator.pourFlask();
        changed |= separator.separate();
        BlockState now = separator.getBlockState();
        boolean burning = separator.burning();
        if (now.getValue(SeparatorBlock.LIT) != burning) {
            level.setBlock(pos, now.setValue(SeparatorBlock.LIT, burning), Block.UPDATE_ALL);
        }
        changed |= separator.fillFlasks();
        if (level.getGameTime() % EssenceTankBlockEntity.FLOW_EVERY == 0) {
            changed |= separator.drawFromTanks();
            changed |= separator.pushToPipes();
        }
        if (changed) {
            separator.setChanged();
        }
    }

    private boolean pourFlask() {
        ItemStack flask = items.get(POUR);
        EssenceMix inside = EssenceFlaskItem.contents(flask);
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
    private EssenceMix portion() {
        return mix.contents().part(Math.min(STEP_DROPS, mix.amount()));
    }

    /** Why the separator cannot work now (a translation key suffix for the screen), or null when it can. */
    @Nullable
    public String blocked() {
        if (mix.isEmpty()) {
            return "empty";
        }
        Essence element = separating();
        boolean unknown = separatingUnknown();
        if (element == null && !unknown) {
            return "no_essence";
        }
        EssenceMix portion = portion();
        int drawn = drawnOut(portion);
        if (drawn > 0) {
            if (!burnerBelow() && charge + (isFuel(items.get(FUEL)) ? FUEL_BINDS : 0) < drawn) {
                return "no_fuel";
            }
            boolean other = !extract.isEmpty() && (unknown ? extract.contents().unknown() != extract.amount()
                    : extract.contents().pureEssence() != element);
            if (extract.space() < drawn || other) {
                return "extract_full";
            }
        }
        return rest.space() < portion.total() - drawn + stain(drawn) ? "rest_full" : null;
    }

    /** A burning Chem Burner under it heats the flask: no blaze powder is burned while it does. */
    private boolean burnerBelow() {
        return level != null && ChemBurnerBlock.isLit(level.getBlockState(worldPosition.below()));
    }

    /** A Chem Burner under it burns while an essence is drawn out of the mix. */
    @Override
    public boolean wantsHeat() {
        return !mix.isEmpty() && (separating() != null || separatingUnknown()) && drawnOut(portion()) > 0;
    }

    /** Whether the powder burns: the separator can work and the next portion gives something up. */
    public boolean burning() {
        return blocked() == null && drawnOut(portion()) > 0;
    }

    /** How much of the chosen essence (or of the unknown one) a portion gives up. */
    private int drawnOut(EssenceMix portion) {
        Essence element = separating();
        return separatingUnknown() ? portion.unknown() : element == null ? 0 : portion.get(element);
    }

    /** What the drawn-out portion leaves on top of the rest: unknown essence, except when the unknown is what is drawn. */
    private int stain(int drawn) {
        return drawn > 0 && !separatingUnknown() ? UNKNOWN_PER_STEP : 0;
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
        EssenceMix portion = portion();
        int drawn = drawnOut(portion);
        boolean burner = burnerBelow();
        if (!burner && drawn > charge) {
            items.get(FUEL).shrink(1);
            charge += FUEL_BINDS;
        }
        mix.remove(portion);
        EssenceMix out = separatingUnknown() ? EssenceMix.unknown(drawn) : EssenceMix.pure(separating(), drawn);
        if (drawn > 0) {
            extract.fill(out, false);
            if (!burner) {
                charge -= drawn;
            }
        }
        // What is left goes on as the rest, and drawing an essence out leaves a dark unknown essence on top of it.
        EssenceMix left = portion.minus(out);
        int stain = stain(drawn);
        rest.fill(stain > 0 ? left.plus(EssenceMix.unknown(stain)) : left, false);
        // A drop through the stopcock now and then: often enough to hear it work, rarely enough not to tire.
        if (drawn > 0 && level.getRandom().nextInt(3) == 0) {
            level.playSound(null, worldPosition, ModSounds.SEPARATOR_DRIP.get(), SoundSource.BLOCKS, 0.6F,
                    0.9F + level.getRandom().nextFloat() * 0.2F);
        }
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

    /** Each inlet draws the mixture from the Essence Tanks piped to it (or standing right at it). */
    private boolean drawFromTanks() {
        boolean drew = false;
        for (SeparatorBlock.Side side : SeparatorBlock.Side.values()) {
            if (modes[side.ordinal()] != PortMode.INLET || mix.space() <= 0) {
                continue;
            }
            EssenceMix drawn = EssenceNetwork.draw(level, worldPosition, side.face(facing()),
                    Math.min(EssenceTankBlockEntity.FLOW, mix.space()), this);
            if (!drawn.isEmpty()) {
                mix.fill(drawn, false);
                lastReceived = level.getGameTime();
                drew = true;
            }
        }
        return drew;
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
        EssenceMix offer = tank.drain(EssenceTankBlockEntity.FLOW, true);
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
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
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
            case FUEL -> isFuel(stack);
            case SUPPLY -> EssenceFlaskItem.isEmptyFlask(stack);
            default -> false;
        };
    }

    /** Separation always burns blaze powder. */
    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.BLAZE_POWDER);
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
            modes[i] = i < saved.length && saved[i] >= 0 && saved[i] < PortMode.values().length ? PortMode.values()[saved[i]] : defaults[i];
        }
        charge = input.getIntOr("charge", 0);
        String choice = input.getStringOr("chosen", "");
        Essence essence = Essence.byId(choice);
        chosen = UNKNOWN_KEY.equals(choice) ? UNKNOWN_CHOICE : essence == null ? -1 : essence.ordinal();
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
        Essence essence = separating();
        if (essence != null) {
            output.putString("chosen", essence.id());
        } else if (separatingUnknown()) {
            output.putString("chosen", UNKNOWN_KEY);
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
