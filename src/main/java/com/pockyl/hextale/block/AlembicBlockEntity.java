package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.transport.Deliverer;
import com.pockyl.hextale.transport.WaterInlet;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.menu.AlembicMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The Alembic's contents: the items waiting to boil, glass bottles and the bottle it filled, the water in its flask and
 * the essence boiled so far. An item boils in {@value #WATER_PER_ITEM} mB of water for {@value #BOIL_TICKS} ticks over
 * heat into {@value #DROPS_PER_POINT} mB per point of each of its elements - all of them at once, a mixture. Water
 * comes in through pipes on any side (from a pump) or from a bucket; the essence goes into bottles and out through the
 * pipes on any side (water pipes never take it).
 * <p>
 * Any item with essences goes in, but only an item its boiler has studied gives its essences: anything else boils
 * into as much dark unknown essence. The boiler is the player who put the item in at the screen; for a hex pylon it is
 * the pylon's owner ({@link Deliverer}); for hoppers it is the alembic's owner (who placed it). Knowledge is read from
 * an online player only: anyone offline counts as nobody.
 */
public class AlembicBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, EssenceNetwork.WaterReceiver,
        com.pockyl.hextale.chem.HeatUser, WaterInlet.Vessel {
    public static final int INPUT = 0;
    public static final int FLASK_IN = 1;
    public static final int FLASK_OUT = 2;
    public static final int BOIL_TICKS = 80;
    /** Millibuckets of essence per point of each of the item's essences. */
    public static final int DROPS_PER_POINT = 1;
    public static final int CAPACITY = 64;
    /** Millibuckets of water the flask holds, and how much one boiled item takes. */
    public static final int WATER_CAPACITY = 2000;
    public static final int WATER_PER_ITEM = 250;
    private static final int[] TOP = {INPUT};
    private static final int[] SIDES = {INPUT, FLASK_IN};
    private static final int[] BOTTOM = {FLASK_OUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private final EssenceTank tank = new EssenceTank(CAPACITY);
    private int progress;
    private int water;
    @Nullable
    private UUID owner;
    /** Whether whoever put the current input in has studied it. */
    private boolean inputKnown;
    /** Client side: smooth boiling progress between syncs. */
    public float clientProgress;
    /** What other mods' pipes and tanks see: water goes in through the horizontal sides, nothing comes out. */
    private final WaterInlet waterInlet = new WaterInlet(this);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? progress : heated() ? 1 : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public AlembicBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALEMBIC.get(), pos, state);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    public EssenceTank tank() {
        return tank;
    }

    public int progress() {
        return progress;
    }

    /** Millibuckets of water in the flask. */
    public int water() {
        return water;
    }

    /** Pours in up to {@code amount} mB of water. */
    public int fillWater(int amount, boolean simulate) {
        int taken = Math.max(0, Math.min(amount, WATER_CAPACITY - water));
        if (taken > 0 && !simulate) {
            water += taken;
            setChanged();
        }
        return taken;
    }

    /** The water inlet for other mods' pipes and tanks. */
    public WaterInlet waterInlet() {
        return waterInlet;
    }

    @Override
    public int waterRoom() {
        return Math.max(0, WATER_CAPACITY - water);
    }

    @Override
    public void pipeWater(int amount) {
        fillWater(amount, false);
    }

    /** Pipes bring water in on any side. */
    @Override
    public int receiveWater(Direction side, int amount, boolean simulate) {
        if (!side.getAxis().isHorizontal()) {
            return 0;
        }
        return fillWater(amount, simulate);
    }

    public ItemStack input() {
        return items.get(INPUT);
    }

    /** A Chem Burner under it burns while there is something to boil and water to boil it in. */
    @Override
    public boolean wantsHeat() {
        return ResearchItems.of(input()) != null && water >= WATER_PER_ITEM;
    }

    private boolean heated() {
        BlockState state = getBlockState();
        return state.hasProperty(AlembicBlock.LIT) && state.getValue(AlembicBlock.LIT);
    }

    /** Whether this player may boil the item: studied. */
    public static boolean studied(Player player, ItemStack stack) {
        return Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** The player put the input in: it boils into its essences if they have studied it. */
    public void boiledBy(Player player) {
        inputKnown = studied(player, input());
        setChanged();
    }

    /** Whether the current input boils into its own essences rather than unknown essence. */
    public boolean inputKnown() {
        return inputKnown;
    }

    /**
     * A new kind of item arriving without a player at the screen is known only if whoever sent it has studied it: the
     * owner of the pylon that brought it, else (a hopper, say) the alembic's own owner.
     */
    @Override
    public void setItem(int slot, ItemStack stack, boolean insideTransaction) {
        // Item handlers (pylons, other mods' pipes) come in through this one; the plain setItem ends up here too.
        if (slot == INPUT && !stack.isEmpty() && !ItemStack.isSameItem(stack, items.get(INPUT))) {
            UUID sender = Deliverer.current();
            ServerPlayer player = online(sender != null ? sender : owner);
            inputKnown = player != null && studied(player, stack);
        }
        super.setItem(slot, stack, insideTransaction);
    }

    @Nullable
    private ServerPlayer online(@Nullable UUID player) {
        return player == null || !(level instanceof ServerLevel serverLevel) ? null : serverLevel.getServer().getPlayerList().getPlayer(player);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity alembic) {
        boolean changed = alembic.boil();
        changed |= alembic.fillFlask();
        if (level.getGameTime() % EssenceTankBlockEntity.FLOW_EVERY == 0) {
            changed |= alembic.pushToPipes();
        }
        if (changed) {
            alembic.setChanged();
        }
    }

    private boolean boil() {
        ItemStack inside = input();
        ItemEssences elements = ResearchItems.of(inside);
        if (!heated() || elements == null || water < WATER_PER_ITEM) {
            boolean had = progress != 0;
            progress = 0;
            return had;
        }
        EssenceMix boiled = EssenceMix.boiled(elements, DROPS_PER_POINT);
        if (!inputKnown) {
            boiled = EssenceMix.unknown(boiled.total());
        }
        if (tank.space() < boiled.total()) {
            return false;
        }
        progress++;
        if (progress < BOIL_TICKS) {
            return progress == 1;
        }
        tank.fill(boiled, false);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            com.pockyl.hextale.thevoid.Thinning.add(server, worldPosition, boiled.total() * (inputKnown ? com.pockyl.hextale.thevoid.Thinning.BOILED : com.pockyl.hextale.thevoid.Thinning.BOILED * 2),
                    com.pockyl.hextale.thevoid.Thinning.Source.BOILED);
        }
        inside.shrink(1);
        water -= WATER_PER_ITEM;
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.2F, 1.4F);
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            // The item is gone into the water: its essence rises off the surface as living drops, a glint or two over it.
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + 10.8 / 16;
            double z = worldPosition.getZ() + 0.5;
            server.sendParticles(com.pockyl.hextale.registry.ModParticles.wisp(boiled.color()), x, y, z, 5, 0.12, 0.02, 0.12, 0.01);
            server.sendParticles(com.pockyl.hextale.registry.ModParticles.glint(boiled.color()), x, y + 0.15, z, 3, 0.15, 0.08, 0.15, 0.01);
        }
        return true;
    }

    /** Fills a glass bottle once a bottle's worth is ready, or with the rest when nothing more is boiling. */
    private boolean fillFlask() {
        ItemStack empty = items.get(FLASK_IN);
        if (!EssenceFlaskItem.isEmptyFlask(empty) || !items.get(FLASK_OUT).isEmpty() || tank.isEmpty()) {
            return false;
        }
        boolean idle = progress == 0 && (input().isEmpty() || !heated());
        if (tank.amount() < EssenceFlaskItem.CAPACITY && !idle) {
            return false;
        }
        items.set(FLASK_OUT, EssenceFlaskItem.filled(tank.drain(EssenceFlaskItem.CAPACITY, false)));
        empty.shrink(1);
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.35F, 1.0F);
        return true;
    }

    /** Lets essence out into a pipe (or a vessel) on any side, the spout first, then the others in turn. */
    private boolean pushToPipes() {
        if (tank.isEmpty()) {
            return false;
        }
        Direction face = getBlockState().getValue(AlembicBlock.FACING);
        for (int i = 0; i < 4; i++) {
            EssenceMix offer = tank.drain(EssenceTankBlockEntity.FLOW, true);
            int moved = EssenceNetwork.push(level, worldPosition, face, offer);
            if (moved > 0) {
                tank.remove(offer.part(moved));
                return true;
            }
            face = face.getClockWise();
        }
        return false;
    }

    /** Client side: the boiling progress runs on smoothly between syncs (the renderer shows it as bubbles). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity alembic) {
        boolean working = state.getValue(AlembicBlock.LIT) && ResearchItems.of(alembic.input()) != null && alembic.water >= WATER_PER_ITEM;
        alembic.clientProgress = working ? Math.min(1, alembic.clientProgress + 1.0F / BOIL_TICKS) : 0;
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
        return Component.translatable("block.hextale.alembic");
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
        return new AlembicMenu(containerId, inventory, this, data, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    /** What each slot takes; whether the input is known decides only what it boils into. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> ResearchItems.of(stack) != null && !EssenceFlaskItem.isFlask(stack);
            case FLASK_IN -> EssenceFlaskItem.isEmptyFlask(stack);
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
        return slot == FLASK_OUT;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(3, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        tank.load(input, "essence");
        progress = input.getIntOr("progress", 0);
        water = input.getIntOr("water", 0);
        clientProgress = progress / (float) BOIL_TICKS;
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        inputKnown = input.getBooleanOr("input_known", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        tank.save(output, "essence");
        output.putInt("progress", progress);
        output.putInt("water", water);
        output.putBoolean("input_known", inputKnown);
        if (owner != null) {
            output.store("owner", UUIDUtil.CODEC, owner);
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
