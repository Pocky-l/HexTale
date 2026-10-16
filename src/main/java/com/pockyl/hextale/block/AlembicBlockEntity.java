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
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.ResearchTool;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The Alembic's flask: one stack of items waiting, the item being distilled, and nine small tanks of ink that drain
 * into a Research Table touching the alembic. Distilling needs heat below. Only items the alembic's owner (who placed it)
 * has studied go in: by hand they are checked against the hand's owner, from hoppers against the owner while online.
 */
public class AlembicBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    /** Ticks to distil one item. */
    public static final int DISTIL_TICKS = 80;
    /** Drops each of the alembic's own tanks holds. */
    public static final int TANK = 32;
    private static final int POUR_EVERY = 4;
    private static final int[] SLOTS = {0};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private final int[] tank = new int[SimpleRune.values().length];
    private int progress;
    @Nullable
    private UUID owner;
    /** Game time of the last drop poured into a table (clients show the drip). */
    private long pouredAt = Long.MIN_VALUE / 2;
    /** Client side: smooth progress between syncs. */
    public float clientProgress;

    public AlembicBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALEMBIC.get(), pos, state);
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
    }

    public int[] tank() {
        return tank;
    }

    public int progress() {
        return progress;
    }

    public long pouredAt() {
        return pouredAt;
    }

    public ItemStack input() {
        return items.get(0);
    }

    private boolean heated() {
        BlockState state = getBlockState();
        return state.hasProperty(AlembicBlock.LIT) && state.getValue(AlembicBlock.LIT);
    }

    private static boolean studied(Player player, ItemStack stack) {
        return player.getAbilities().instabuild || Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Nullable
    private ServerPlayer onlineOwner() {
        return owner == null || !(level instanceof ServerLevel serverLevel) ? null : serverLevel.getServer().getPlayerList().getPlayer(owner);
    }

    /** Puts the held items into the flask; says why when it cannot. */
    public boolean insert(ServerPlayer player, ItemStack held) {
        ItemElements elements = ResearchItems.of(held);
        if (elements == null) {
            return false;
        }
        if (!studied(player, held)) {
            player.sendOverlayMessage(Component.translatable("block.hextale.alembic.unstudied", ResearchText.item(held.getItem())));
            return false;
        }
        ItemStack inside = input();
        if (!inside.isEmpty() && !ItemStack.isSameItemSameComponents(inside, held)) {
            player.sendOverlayMessage(Component.translatable("block.hextale.alembic.busy", inside.getHoverName()));
            return false;
        }
        if (owner == null) {
            owner = player.getUUID();
        }
        int room = held.getMaxStackSize() - inside.getCount();
        int moved = Math.min(room, held.getCount());
        if (moved <= 0) {
            return false;
        }
        if (inside.isEmpty()) {
            items.set(0, held.copyWithCount(moved));
        } else {
            inside.grow(moved);
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(moved);
        }
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.2F);
        setChanged();
        return true;
    }

    /** Gives the waiting items back. */
    public void takeBack(Player player) {
        ItemStack inside = input();
        if (inside.isEmpty()) {
            return;
        }
        items.set(0, ItemStack.EMPTY);
        progress = 0;
        if (!player.getInventory().add(inside)) {
            player.drop(inside, false, Prediction.SERVER_ONLY);
        }
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity alembic) {
        boolean changed = alembic.distil();
        if (level.getGameTime() % POUR_EVERY == 0) {
            changed |= alembic.pour();
        }
        if (changed) {
            alembic.setChanged();
        }
    }

    /** Advances the item in the flask; a finished item turns into ink in the tanks. */
    private boolean distil() {
        ItemStack inside = input();
        ItemElements elements = ResearchItems.of(inside);
        if (!heated() || elements == null) {
            boolean had = progress != 0;
            progress = 0;
            return had;
        }
        int perPoint = ResearchTool.inkPerPoint(mortar());
        if (!fits(elements, perPoint)) {
            return false;
        }
        progress++;
        if (progress < DISTIL_TICKS) {
            return progress == 1;
        }
        elements.elements().forEach((element, strength) -> tank[element.ordinal()] += strength * perPoint);
        inside.shrink(1);
        progress = 0;
        level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.5F, 1.4F);
        return true;
    }

    private boolean fits(ItemElements elements, int perPoint) {
        for (Map.Entry<SimpleRune, Integer> entry : elements.elements().entrySet()) {
            if (tank[entry.getKey().ordinal()] + entry.getValue() * perPoint > TANK) {
                return false;
            }
        }
        return true;
    }

    /** The owner's mortar on the table the alembic pours into grinds items finer: more ink per item. */
    private int mortar() {
        ResearchTableBlockEntity table = table();
        ServerPlayer player = onlineOwner();
        return table == null || player == null ? 0 : table.toolLevel(player, ResearchTool.MORTAR);
    }

    /** A Research Table touching the alembic (any side, either half), or null. */
    @Nullable
    public ResearchTableBlockEntity table() {
        if (level == null) {
            return null;
        }
        for (Direction side : Direction.values()) {
            BlockPos at = worldPosition.relative(side);
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof ResearchTableBlock
                    && level.getBlockEntity(ResearchTableBlock.mainPos(state, at)) instanceof ResearchTableBlockEntity table) {
                return table;
            }
        }
        return null;
    }

    /** One drop of every ink the alembic holds runs into the table's vials, while they have room. */
    private boolean pour() {
        ResearchTableBlockEntity table = table();
        if (table == null) {
            return false;
        }
        boolean poured = false;
        for (int i = 0; i < tank.length; i++) {
            if (tank[i] > 0 && table.receiveInk(i, 1)) {
                tank[i]--;
                poured = true;
            }
        }
        if (poured) {
            pouredAt = level.getGameTime();
        }
        return poured;
    }

    /** Bubbles of the item's colours rise in a working flask; steam leaves the cap; ink drips from the spout. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, AlembicBlockEntity alembic) {
        RandomSource random = level.getRandom();
        ItemElements elements = ResearchItems.of(alembic.input());
        boolean working = state.getValue(AlembicBlock.LIT) && elements != null;
        alembic.clientProgress = working ? Math.min(1, alembic.clientProgress + 1.0F / DISTIL_TICKS) : 0;
        if (working && random.nextInt(3) == 0) {
            SimpleRune element = pick(elements, random);
            level.addParticle(new DustParticleOptions(element.color(), 0.5F), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.35,
                    pos.getY() + 0.35 + random.nextDouble() * 0.3, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.35, 0, 0.02, 0);
        }
        if (working && random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0.03, 0);
        }
        if (level.getGameTime() - alembic.pouredAt < 10 && random.nextInt(2) == 0) {
            Direction facing = state.getValue(AlembicBlock.FACING);
            Vec3 tip = Vec3.atCenterOf(pos).add(facing.getStepX() * 0.43, 0.15, facing.getStepZ() * 0.43);
            int colour = alembic.strongestInk();
            level.addParticle(new DustParticleOptions(colour, 0.7F), tip.x, tip.y, tip.z, 0, -0.05, 0);
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

    /** The colour of the ink the alembic holds most of (or of its item), for the flask and the drip. */
    public int strongestInk() {
        int best = -1;
        for (int i = 0; i < tank.length; i++) {
            if (tank[i] > 0 && (best < 0 || tank[i] > tank[best])) {
                best = i;
            }
        }
        if (best >= 0) {
            return SimpleRune.values()[best].color();
        }
        ItemElements elements = ResearchItems.of(input());
        return elements == null ? 0x7EC8E8 : elements.main().color();
    }

    /** All ink in the tanks, 0-1 of one full tank. */
    public float fill() {
        int total = 0;
        for (int drops : tank) {
            total += drops;
        }
        return Math.min(1, total / (float) TANK);
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
    protected @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return null;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    /** Hoppers feed items the owner has studied (while the owner is online) and of the kind already inside. */
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        ServerPlayer player = onlineOwner();
        ItemStack inside = input();
        return player != null && ResearchItems.of(stack) != null && studied(player, stack)
                && (inside.isEmpty() || ItemStack.isSameItemSameComponents(inside, stack));
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(1, ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(input, items);
        int[] saved = input.getIntArray("tank").orElse(new int[0]);
        for (int i = 0; i < tank.length; i++) {
            tank[i] = i < saved.length ? saved[i] : 0;
        }
        progress = input.getIntOr("progress", 0);
        clientProgress = progress / (float) DISTIL_TICKS;
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        pouredAt = input.getLongOr("poured_at", Long.MIN_VALUE / 2);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        net.minecraft.world.ContainerHelper.saveAllItems(output, items);
        output.putIntArray("tank", tank);
        output.putInt("progress", progress);
        if (owner != null) {
            output.store("owner", UUIDUtil.CODEC, owner);
        }
        output.putLong("poured_at", pouredAt);
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
