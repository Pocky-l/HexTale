package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.menu.HexWorkbenchMenu;
import com.pockyl.hextale.network.EssenceFxPayload;
import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.BulwarkHeart;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.runework.WorkbenchRune;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Hex Workbench's contents: the 3x3 grid (the items stay on the worktop when nobody works at it) and the crystal
 * socket. Every craft spends charge, and raw hextech in the socket gives it: an unstable orb {@value #UNSTABLE_CHARGE},
 * a crystal grown in shimmer {@value #SYNTHETIC_CHARGE}. Cut crystals and gemstones are never fuel (owner 2026-10-29: the
 * better grades go into devices, the best only into the best ones). Crystals in the socket soak
 * into the bench one by one while there is room under its {@link #capacity()}; a craft short of charge uses them up at
 * once. Charge and runes ({@link WorkbenchRune}) stay with the bench when it is picked up.
 * <p>
 * Every {@value #FEED_TICKS} ticks it feeds the heart of a Bulwark worn within {@value #REACH} blocks (two up or down), at
 * most {@value #FEED} points of the heart, on a thread of light; a point of charge is {@link #HEART_PER_CHARGE} points of
 * the heart, so a refill at the bench uses up as many orbs as one by hand. A comparator reads how full it is. Chain Lightning widens the reach,
 * speeds the feed and hands charge to the benches around that hold less. Sunlight Cell drinks a point of charge now and
 * then while the sky shows through to the bench by day in dry weather ({@link #sunlit}), drawn as a shaft of light.
 */
public class HexWorkbenchBlockEntity extends BaseContainerBlockEntity {
    public static final int GRID = 9;
    public static final int CRYSTAL = 9;
    public static final int SIZE = 10;
    public static final int UNSTABLE_CHARGE = 5;
    public static final int SYNTHETIC_CHARGE = 8;
    /** The most charge the bench soaks up from its socket by itself, without a Cache. */
    public static final int MAX_CHARGE = 60;
    /** Blocks it feeds across, two up or down whatever the reach. */
    public static final int REACH = 4;
    public static final int FEED_TICKS = 20;
    /** Charge handed to a bench or points to a heart per feed, without Chain Lightning. */
    public static final int FEED = 6;
    /** Points of a Bulwark heart one point of charge buys: an orb is worth the same at the bench and by hand. */
    public static final int HEART_PER_CHARGE = BulwarkHeart.PER_CRYSTAL / UNSTABLE_CHARGE;
    private static final int SOAK_TICKS = 10;
    private static final int HEXTECH = 0x7FD8FF;
    /** How high over its top the shaft of sunlight starts. */
    private static final float SUN_SHAFT = 2.5F;
    /** Every this many points drunk from the sun there is a soft sound (the light alone shows the rest). */
    private static final int SUN_SOUND_EVERY = 6;
    /** Block event: a craft was made (clients flash the worktop). */
    public static final int CRAFTED = 1;
    /** Block event: a crystal in the socket soaked into charge (clients draw it sinking into the worktop). */
    public static final int SOAKED = 2;
    /** How long the clients show a craft and a soak (ticks). */
    public static final int CRAFT_SHOWN_TICKS = 26;
    public static final int SOAK_SHOWN_TICKS = 24;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int charge;
    private int sunDrunk;
    /** Points of a heart fed but not paid for yet: charge goes in whole points, a feed may be less than one. */
    private int heartDebt;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** Bumped on every change, so open menus know when to look for a new result. */
    private int revision;
    /** Client side: game time of the last craft, for the flash. */
    public long craftedAt = -1000;

    public HexWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_WORKBENCH.get(), pos, state);
    }

    /** Charge a crystal gives, 0 for anything else. */
    public static int chargeOf(ItemStack stack) {
        if (stack.is(ModItems.UNSTABLE_HEXTECH.get())) {
            return UNSTABLE_CHARGE;
        }
        return stack.is(ModItems.SYNTHETIC_CRYSTAL.get()) ? SYNTHETIC_CHARGE : 0;
    }

    public int charge() {
        return charge;
    }

    public PickaxeRunes runes() {
        return runes;
    }

    /** The runes it works with (an altar burns them into its item; placing that item brings them here). */
    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }

    private int strength(WorkbenchRune rune) {
        return runes.strength(rune.rune());
    }

    /** The most charge it holds: {@value #MAX_CHARGE}, more with Cache. */
    public int capacity() {
        int cache = strength(WorkbenchRune.CACHE);
        return cache > 0 ? HolderRune.at(WorkbenchRune.POOL, cache) : MAX_CHARGE;
    }

    /** Blocks it feeds across: {@value #REACH}, more with Chain Lightning. */
    public int reach() {
        int chain = strength(WorkbenchRune.CHAIN_LIGHTNING);
        return REACH + (chain > 0 ? HolderRune.at(WorkbenchRune.REACH, chain) : 0);
    }

    /** Charge handed to each bench or heart per feed: {@value #FEED}, more with Chain Lightning. */
    public int feedRate() {
        int chain = strength(WorkbenchRune.CHAIN_LIGHTNING);
        return chain > 0 ? HolderRune.at(WorkbenchRune.FEED, chain) : FEED;
    }

    /** What a comparator reads: 0 when empty, 15 when full. */
    public int signal() {
        return charge <= 0 ? 0 : Math.min(15, 1 + Math.round(14.0F * charge / capacity()));
    }

    /** Takes charge from outside (another bench, a Crystal Battery) while there is room; returns what it took. */
    public int receive(int amount) {
        int taken = Math.max(0, Math.min(amount, capacity() - charge));
        if (taken > 0) {
            charge += taken;
            setChanged();
        }
        return taken;
    }

    public int revision() {
        return revision;
    }

    /** The charge left plus what the crystals in the socket would give. */
    public int chargeAvailable() {
        ItemStack crystal = items.get(CRYSTAL);
        return charge + chargeOf(crystal) * crystal.getCount();
    }

    /** Spends {@code cost} charge, using up crystals from the socket as needed; false (nothing spent) when short. */
    public boolean spend(int cost) {
        if (chargeAvailable() < cost) {
            return false;
        }
        ItemStack crystal = items.get(CRYSTAL);
        while (charge < cost) {
            charge += soaked(crystal);
            crystal.shrink(1);
        }
        charge -= cost;
        setChanged();
        return true;
    }

    public CraftingInput.Positioned input() {
        return CraftingInput.ofPositioned(3, 3, items.subList(0, GRID));
    }

    public List<ItemStack> grid() {
        return items.subList(0, GRID);
    }

    /** What a crystal gives as it soaks in: with Thrift now and then half as much again. */
    private int soaked(ItemStack crystal) {
        int value = chargeOf(crystal);
        int thrift = strength(WorkbenchRune.THRIFT);
        if (thrift <= 0 || level == null || level.getRandom().nextFloat() >= HolderRune.at(WorkbenchRune.BONUS, thrift)) {
            return value;
        }
        if (level instanceof ServerLevel server) {
            ToolEffects.puff(server, Vec3.atCenterOf(worldPosition).add(0, 0.6, 0), WorkbenchRune.THRIFT.rune(), 6, 0.3);
        }
        return value + (value + 1) / 2;
    }

    /** Whether the sun reaches the block: the sky shows through to it (glass lets it through, leaves do not), day, no rain. */
    public static boolean sunlit(Level level, BlockPos pos) {
        return level.isBrightOutside() && !level.isRaining() && level.canSeeSky(pos.above());
    }

    /** Sunlight Cell: a point of charge every {@link WorkbenchRune#SUN_TICKS} while sunlit. */
    private void bask(ServerLevel level) {
        int sun = strength(WorkbenchRune.SUNLIGHT_CELL);
        if (sun <= 0) {
            return;
        }
        int every = HolderRune.at(WorkbenchRune.SUN_TICKS, sun);
        if (level.getGameTime() % every != Math.floorMod(worldPosition.asLong(), every) || charge >= capacity() || !sunlit(level, worldPosition)) {
            return;
        }
        charge++;
        sunDrunk++;
        setChanged();
        Vec3 top = Vec3.atCenterOf(worldPosition).add(0, 0.5, 0);
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.SUNBEAM, top, top, WorkbenchRune.SUNLIGHT_CELL.rune().color(), SUN_SHAFT);
        if (sunDrunk % SUN_SOUND_EVERY == 1) {
            level.playSound(null, worldPosition, ModSounds.RUNE_BLOOM.get(), SoundSource.BLOCKS, 0.15F, 1.3F + level.getRandom().nextFloat() * 0.2F);
        }
    }

    /**
     * The workbenches within {@code reach} (two blocks up and down), nearest first: the block entities of the few chunks
     * the reach touches, never every block of it (up to 3125 lookups every second, as {@code JarReach} does it).
     */
    public static List<HexWorkbenchBlockEntity> near(Level level, BlockPos pos, int reach) {
        List<HexWorkbenchBlockEntity> benches = new ArrayList<>();
        for (int cx = (pos.getX() - reach) >> 4; cx <= (pos.getX() + reach) >> 4; cx++) {
            for (int cz = (pos.getZ() - reach) >> 4; cz <= (pos.getZ() + reach) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos at = entity.getBlockPos();
                    if (entity instanceof HexWorkbenchBlockEntity bench && !bench.isRemoved() && Math.abs(at.getX() - pos.getX()) <= reach
                            && Math.abs(at.getY() - pos.getY()) <= 2 && Math.abs(at.getZ() - pos.getZ()) <= reach) {
                        benches.add(bench);
                    }
                }
            }
        }
        benches.sort(Comparator.<HexWorkbenchBlockEntity>comparingDouble(bench -> bench.getBlockPos().distSqr(pos))
                .thenComparingLong(bench -> bench.getBlockPos().asLong()));
        return benches;
    }

    /**
     * Sends charge out: with Chain Lightning to the benches in reach that hold less (never past half the difference, so
     * two such benches level out instead of passing it back and forth), then to the Bulwark hearts worn in reach.
     */
    private void feed(ServerLevel level) {
        int before = charge;
        int reach = reach();
        int rate = feedRate();
        if (strength(WorkbenchRune.CHAIN_LIGHTNING) > 0) {
            for (HexWorkbenchBlockEntity other : near(level, worldPosition, reach)) {
                if (other != this) {
                    int given = other.receive(Math.min(rate, (charge - other.charge) / 2));
                    if (given > 0) {
                        charge -= given;
                        thread(level, Vec3.atCenterOf(other.worldPosition).add(0, 0.45, 0));
                    }
                }
            }
        }
        for (Player wearer : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(reach, 2, reach))) {
            if (charge <= 0) {
                break;
            }
            int given = BulwarkHeart.feed(wearer, Math.min(rate, charge * HEART_PER_CHARGE));
            if (given > 0) {
                heartDebt += given;
                charge -= heartDebt / HEART_PER_CHARGE;
                heartDebt %= HEART_PER_CHARGE;
                thread(level, wearer.position().add(0, wearer.getBbHeight() * 0.65, 0));
            }
        }
        if (charge != before) {
            setChanged();
        }
    }

    /** A thread of hextech light from the worktop, renewed every other feed so a steady charge reads as one steady stream. */
    private void thread(ServerLevel level, Vec3 to) {
        if (level.getGameTime() / FEED_TICKS % 2 == 0) {
            EssenceFxPayload.pour(level, Vec3.atCenterOf(worldPosition).add(0, 0.5, 0), to, HEXTECH, FEED_TICKS * 2 - 4);
        }
    }

    /** Clients hear of a working machine at most twice a second. */
    private final ClientSync clientSync = new ClientSync();

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, HexWorkbenchBlockEntity bench) {
        bench.clientSync.tick(bench);
        if (level instanceof ServerLevel server) {
            bench.bask(server);
            if (bench.charge > 0 && level.getGameTime() % FEED_TICKS == Math.floorMod(pos.asLong(), FEED_TICKS)) {
                bench.feed(server);
            }
        }
        // One crystal from the socket soaks into charge when there is room for it (every SOAK_TICKS ticks).
        ItemStack crystal = bench.items.get(CRYSTAL);
        int value = chargeOf(crystal);
        if (value <= 0 || bench.charge + value > bench.capacity() || level.getGameTime() % SOAK_TICKS != 0) {
            return;
        }
        bench.charge = Math.min(bench.capacity(), bench.charge + bench.soaked(crystal));
        crystal.shrink(1);
        level.playSound(null, pos, ModSounds.CRYSTAL_RESONATE.get(), SoundSource.BLOCKS, 0.6F, 1.1F);
        level.blockEvent(pos, state.getBlock(), SOAKED, 0);
        bench.setChanged();
    }

    /** Effects of a craft on the server: a soft spend sound. */
    public void crafted(ServerLevel level) {
        level.blockEvent(worldPosition, getBlockState().getBlock(), CRAFTED, 0);
        Vec3 top = Vec3.atLowerCornerOf(worldPosition).add(0.5, 1.02, 0.5);
        level.playSound(null, top.x, top.y, top.z, ModSounds.WORKBENCH_CRAFT.get(), SoundSource.BLOCKS, 0.7F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        level.sendParticles(com.pockyl.hextale.registry.ModParticles.glint(com.pockyl.hextale.registry.ModParticles.HEX_LIGHT),
                top.x, top.y + 0.25, top.z, 5, 0.15, 0.12, 0.15, 0.02);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        // A new craft or soak never restarts one still being shown (crafts in a row, a socket full of crystals
        // soaking every few ticks): the next one starts once the last has played out, so nothing jumps.
        if (id == SOAKED) {
            if (level != null && level.isClientSide() && (soakedAt < 0 || level.getGameTime() - soakedAt >= SOAK_SHOWN_TICKS)) {
                soakedAt = level.getGameTime();
            }
            return true;
        }
        if (id == CRAFTED && level != null && level.isClientSide()) {
            if (level.getGameTime() - craftedAt >= CRAFT_SHOWN_TICKS) {
                craftedAt = level.getGameTime();
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    public void setChanged() {
        revision++;
        super.setChanged();
        clientSync.request(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.hex_workbench");
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
        return new HexWorkbenchMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != CRYSTAL || chargeOf(stack) > 0;
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        charge = input.getIntOr("charge", 0);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        revision++;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("charge", charge);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.WORKBENCH_RUNES.get(), PickaxeRunes.EMPTY);
        charge = components.getOrDefault(ModDataComponents.WORKBENCH_CHARGE.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.WORKBENCH_RUNES.get(), runes);
        }
        if (charge > 0) {
            components.set(ModDataComponents.WORKBENCH_CHARGE.get(), charge);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("runes");
        output.discard("charge");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Client: whether a craft flashed within the last {@code ticks}. */
    public float flash(Level level, float partialTick, int ticks) {
        float age = level.getGameTime() - craftedAt + partialTick;
        return age < 0 || age > ticks ? 0 : 1 - age / ticks;
    }

    /** Client: ticks since a crystal soaked into charge (large when it never did). */
    public float sinceSoak(Level level, float partialTick) {
        return soakedAt < 0 ? Float.MAX_VALUE : level.getGameTime() - soakedAt + partialTick;
    }

    /** Client side: game time of the last soak. */
    private long soakedAt = -1;
}
