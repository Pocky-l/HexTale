package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.util.RandomSource;
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

import com.pockyl.hextale.essence.Essence;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.menu.AlembicMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The Alembic's contents: the items waiting to boil, glass bottles and the bottle it filled, and the essence boiled so
 * far. An item boils for {@value #BOIL_TICKS} ticks over heat into {@value #DROPS_PER_POINT} drops per point of each of
 * its elements - all of them at once, a mixture. The essence goes into bottles and out of the spout (the front) into a
 * pipe.
 * <p>
 * Only items the researcher has studied go in: checked against the player at the screen, and against the alembic's
 * owner (who placed it) for hoppers, while the owner is online.
 */
public class AlembicBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUT = 0;
    public static final int FLASK_IN = 1;
    public static final int FLASK_OUT = 2;
    public static final int BOIL_TICKS = 80;
    public static final int DROPS_PER_POINT = 2;
    public static final int CAPACITY = 64;
    private static final int[] TOP = {INPUT};
    private static final int[] SIDES = {INPUT, FLASK_IN};
    private static final int[] BOTTOM = {FLASK_OUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private final EssenceTank tank = new EssenceTank(CAPACITY);
    private int progress;
    @Nullable
    private UUID owner;
    /** Client side: smooth boiling progress between syncs. */
    public float clientProgress;

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

    public ItemStack input() {
        return items.get(INPUT);
    }

    private boolean heated() {
        BlockState state = getBlockState();
        return state.hasProperty(AlembicBlock.LIT) && state.getValue(AlembicBlock.LIT);
    }

    /** Whether this player may boil the item: studied (or creative). */
    public static boolean studied(Player player, ItemStack stack) {
        return player.getAbilities().instabuild || Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Nullable
    private ServerPlayer onlineOwner() {
        return owner == null || !(level instanceof ServerLevel serverLevel) ? null : serverLevel.getServer().getPlayerList().getPlayer(owner);
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
        ItemElements elements = ResearchItems.of(inside);
        if (!heated() || elements == null) {
            boolean had = progress != 0;
            progress = 0;
            return had;
        }
        Essence boiled = Essence.boiled(elements, DROPS_PER_POINT);
        if (tank.space() < boiled.total()) {
            return false;
        }
        progress++;
        if (progress < BOIL_TICKS) {
            return progress == 1;
        }
        tank.fill(boiled, false);
        inside.shrink(1);
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5F, 1.4F);
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
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.0F);
        return true;
    }

    /** Lets essence out of the spout into a pipe (or a vessel) in front. */
    private boolean pushToPipes() {
        if (tank.isEmpty()) {
            return false;
        }
        Essence offer = tank.drain(EssenceTankBlockEntity.FLOW, true);
        int moved = EssenceNetwork.push(level, worldPosition, getBlockState().getValue(AlembicBlock.FACING), offer);
        if (moved > 0) {
            tank.remove(offer.part(moved));
            return true;
        }
        return false;
    }

    /** Bubbles of the item's colours rise in a working flask; steam leaves the cap. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity alembic) {
        RandomSource random = level.getRandom();
        ItemElements elements = ResearchItems.of(alembic.input());
        boolean working = state.getValue(AlembicBlock.LIT) && elements != null;
        alembic.clientProgress = working ? Math.min(1, alembic.clientProgress + 1.0F / BOIL_TICKS) : 0;
        if (working && random.nextInt(3) == 0) {
            SimpleRune element = pick(elements, random);
            level.addParticle(new DustParticleOptions(element.color(), 0.5F), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.35,
                    pos.getY() + 0.35 + random.nextDouble() * 0.3, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.35, 0, 0.02, 0);
        }
        if (working && random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0.03, 0);
        }
    }

    private static SimpleRune pick(ItemElements elements, RandomSource random) {
        int total = elements.elements().values().stream().mapToInt(Integer::intValue).sum();
        int roll = random.nextInt(Math.max(1, total));
        for (Map.Entry<SimpleRune, Integer> entry : elements.elements().entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        return elements.main();
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

    /** What each slot takes; studying is checked by the screen and for hoppers. */
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
        if (!accepts(slot, stack)) {
            return false;
        }
        if (slot != INPUT) {
            return true;
        }
        ServerPlayer player = onlineOwner();
        return player != null && studied(player, stack);
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
        clientProgress = progress / (float) BOIL_TICKS;
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        tank.save(output, "essence");
        output.putInt("progress", progress);
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
