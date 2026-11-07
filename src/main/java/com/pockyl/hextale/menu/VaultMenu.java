package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;

import com.pockyl.hextale.block.HexTerminalBlockEntity;
import com.pockyl.hextale.block.HexVaultBlockEntity;
import com.pockyl.hextale.block.RiftGateBlock;
import com.pockyl.hextale.item.PocketTerminalItem;
import com.pockyl.hextale.network.EssenceFxPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.VaultViewPayload;
import com.pockyl.hextale.registry.ModMenus;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.storage.VaultContents;
import com.pockyl.hextale.storage.VaultRules;
import com.pockyl.hextale.storage.VaultStore;
import com.pockyl.hextale.storage.VaultToll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The one screen of a Hex Vault, whatever it is opened from: the vault itself, a Hex Terminal or a Pocket Terminal. Only the
 * player's inventory is made of slots. The grid is a list the server sends ({@link VaultViewPayload}, changes only after
 * the first time) and the client asks for items by name ({@code VaultActionPayload}), so the server alone decides what
 * moves: two players at one vault take from the same count and nothing is ever predicted on a client.
 *
 * <p>Taking at the vault is free. Through a terminal every take pays the toll ({@link VaultToll}); storing is free everywhere.
 */
public class VaultMenu extends AbstractContainerMenu implements VaultContents.Watcher {
    public static final int CELL = 18;
    /** The player's slots; the screen grows its panel round them to fit the window. */
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 16;
    /** How many entries one packet carries. */
    private static final int LINES_PER_PACKET = 256;
    /** How far from a vault or a terminal its screen stays open. */
    private static final float REACH = 9;

    /** Where the screen was opened from. */
    public enum Kind {
        VAULT, TERMINAL, POCKET
    }

    /** What a click on the grid asks for. */
    public enum Action {
        TAKE_STACK, TAKE_HALF, TAKE_TO_INVENTORY, TAKE_ONE, TAKE_ALL, PUT_CARRIED, PUT_ONE, RETURN_ONE, STORE_ALL, PIN
    }

    /** A line of the grid, as the client keeps it. */
    public record Line(ItemResource resource, long count, int weight) {
    }

    private final Player player;
    private final Kind kind;
    private final BlockPos pos;
    private final InteractionHand hand;
    // Server side.
    @Nullable
    private final VaultContents contents;
    @Nullable
    private final UUID vault;
    private final Set<VaultContents.Entry> touched = new LinkedHashSet<>();
    private boolean sentAll;
    private long sentUsed = -1;
    private long sentCapacity = -1;
    private int sentWaiting;
    private boolean sentDry;
    private boolean sentWorking;
    private List<ItemResource> sentPins = List.of();
    private VaultContents.@Nullable Refusal refusal;
    // Client side.
    private final Map<ItemResource, Line> lines = new LinkedHashMap<>();
    private long used;
    private long capacity;
    private int waiting;
    private boolean dry;
    private boolean working = true;
    private List<ItemResource> pins = List.of();
    private int note = -1;
    private int revision;

    /** Client side. */
    public VaultMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, null, null, Kind.values()[buffer.readVarInt()], buffer.readBlockPos(), buffer.readEnum(InteractionHand.class));
    }

    public VaultMenu(int containerId, Inventory inventory, @Nullable VaultContents contents, @Nullable UUID vault, Kind kind, BlockPos pos,
                     InteractionHand hand) {
        super(ModMenus.VAULT.get(), containerId);
        this.player = inventory.player;
        this.contents = contents;
        this.vault = vault;
        this.kind = kind;
        this.pos = pos;
        this.hand = hand;
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
        if (contents != null) {
            contents.watch(this);
        }
    }

    /** Opens the screen at the vault. */
    public static void open(ServerPlayer player, HexVaultBlockEntity vault) {
        open(player, vault.contents(), vault.id(), Kind.VAULT, vault.getBlockPos(), InteractionHand.MAIN_HAND, vault.getBlockState().getBlock().getName());
    }

    /** Opens the screen of vault {@code id} from a terminal, in the terminal's name; false when that vault is not standing. */
    public static boolean open(ServerPlayer player, UUID id, Kind kind, BlockPos pos, InteractionHand hand, Component title) {
        VaultContents contents = VaultStore.of(player.level().getServer()).find(id);
        if (contents == null || contents.home() == null || !contents.open()) {
            return false;
        }
        open(player, contents, id, kind, pos, hand, title);
        return true;
    }

    private static void open(ServerPlayer player, VaultContents contents, UUID id, Kind kind, BlockPos pos, InteractionHand hand, Component title) {
        player.openMenu(new SimpleMenuProvider((containerId, inventory, who) -> new VaultMenu(containerId, inventory, contents, id, kind, pos, hand), title),
                buffer -> {
                    buffer.writeVarInt(kind.ordinal());
                    buffer.writeBlockPos(pos);
                    buffer.writeEnum(hand);
                });
    }

    /** The translation key that says why an item stays out. */
    public static String refusalKey(VaultContents.Refusal refusal) {
        return switch (refusal) {
            case FULL -> "gui.hextale.vault.full";
            case TOO_MANY_KINDS -> "gui.hextale.vault.too_many_kinds";
            case CONTAINER -> "gui.hextale.vault.container";
            case ASLEEP -> "gui.hextale.vault.asleep";
        };
    }

    // ---- Server: moving items ------------------------------------------------------------------------------------------

    /** A click on the grid; {@code resource} is what was clicked (empty when a click only puts in). */
    public void act(ServerPlayer who, ItemResource resource, Action action) {
        if (contents == null || !stillValid(who)) {
            return;
        }
        switch (action) {
            case TAKE_STACK -> take(who, resource, resource.isEmpty() ? 0 : resource.getMaxStackSize(), false);
            case TAKE_HALF -> take(who, resource, resource.isEmpty() ? 0 : (int) Math.min((resource.getMaxStackSize() + 1) / 2, (contents.count(resource) + 1) / 2), false);
            case TAKE_TO_INVENTORY -> take(who, resource, resource.isEmpty() ? 0 : resource.getMaxStackSize(), true);
            case TAKE_ONE -> take(who, resource, 1, true);
            case TAKE_ALL -> takeAll(who, resource);
            case PUT_CARRIED -> put(who, getCarried().getCount());
            case PUT_ONE -> put(who, 1);
            case RETURN_ONE -> returnOne(who, resource);
            case STORE_ALL -> storeAll(who);
            case PIN -> {
                if (!resource.isEmpty()) {
                    contents.togglePin(resource);
                }
            }
        }
    }

    private void take(ServerPlayer who, ItemResource resource, int wanted, boolean toInventory) {
        if (move(who, resource, wanted, toInventory)) {
            taken(who, resource);
        }
    }

    /** Stack after stack into the pockets, each paying its own toll, so dry jars stop it after the first. */
    private void takeAll(ServerPlayer who, ItemResource resource) {
        if (resource.isEmpty()) {
            return;
        }
        boolean moved = false;
        while (move(who, resource, resource.getMaxStackSize(), true)) {
            moved = true;
        }
        if (moved) {
            taken(who, resource);
        }
    }

    private boolean move(ServerPlayer who, ItemResource resource, int wanted, boolean toInventory) {
        if (resource.isEmpty() || contents == null || contents.count(resource) <= 0) {
            return false;
        }
        ItemStack carried = getCarried();
        int room;
        if (toInventory) {
            room = room(who.getInventory(), resource);
        } else if (carried.isEmpty()) {
            room = resource.getMaxStackSize();
        } else {
            room = resource.matches(carried) ? carried.getMaxStackSize() - carried.getCount() : 0;
        }
        int amount = Math.min(wanted, room);
        // Room first: a take that cannot land costs nothing.
        if (amount <= 0 || !paid(who)) {
            return false;
        }
        ItemStack out = contents.take(resource, amount);
        if (out.isEmpty()) {
            return false;
        }
        int count = out.getCount();
        if (toInventory) {
            who.getInventory().add(out);
            if (!out.isEmpty()) {
                who.drop(out, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        } else if (carried.isEmpty()) {
            setCarried(out);
        } else {
            carried.grow(count);
            setCarried(carried);
        }
        return true;
    }

    /** How many of an item the player's pockets can take. */
    private static int room(Inventory inventory, ItemResource resource) {
        int room = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                room += resource.getMaxStackSize();
            } else if (resource.matches(stack)) {
                room += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
        }
        return room;
    }

    /** The toll of a distant take; the vault's own screen takes for free. */
    private boolean paid(ServerPlayer who) {
        if (kind == Kind.VAULT || contents == null) {
            return true;
        }
        ServerLevel level = who.level();
        BlockPos jars = kind == Kind.TERMINAL ? pos : who.blockPosition();
        Vec3 to = kind == Kind.TERMINAL ? Vec3.atCenterOf(pos) : EssenceFxPayload.hand(who);
        return VaultStore.of(level.getServer()).toll(who.getUUID()).pass(level, jars, to, contents.thrift());
    }

    private void put(ServerPlayer who, int amount) {
        ItemStack carried = getCarried();
        if (carried.isEmpty() || amount <= 0 || contents == null) {
            return;
        }
        ItemStack going = carried.copyWithCount(amount);
        int stored = contents.store(going);
        if (stored > 0) {
            carried.shrink(stored);
            setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            stored(who);
        } else {
            refusal = contents.refusal(ItemResource.of(carried));
        }
    }

    /** One of this item back from the pockets, the wheel's way down. */
    private void returnOne(ServerPlayer who, ItemResource resource) {
        if (resource.isEmpty() || contents == null) {
            return;
        }
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (resource.matches(stack) && slot.mayPickup(who)) {
                if (contents.store(stack.copyWithCount(1)) > 0) {
                    stack.shrink(1);
                    slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
                    stored(who);
                } else {
                    refusal = contents.refusal(resource);
                }
                return;
            }
        }
    }

    /** Everything in the pockets goes in, as far as it fits; the hotbar stays what the player is working with. */
    private void storeAll(ServerPlayer who) {
        if (contents == null) {
            return;
        }
        boolean moved = false;
        for (Slot slot : slots) {
            if (slot.getContainerSlot() < Inventory.getSelectionSize() || slot.getContainerSlot() >= Inventory.INVENTORY_SIZE
                    || !slot.hasItem() || !slot.mayPickup(who)) {
                continue;
            }
            ItemStack stack = slot.getItem();
            ItemResource resource = ItemResource.of(stack);
            if (contents.store(stack) > 0) {
                slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
                moved = true;
            } else {
                refusal = contents.refusal(resource);
            }
        }
        if (moved) {
            stored(who);
        }
    }

    /** Shift-click on the inventory: the stack goes in, as much as fits. The server's answer rewrites the slot on the client. */
    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (contents == null || !(who instanceof ServerPlayer server) || !slot.hasItem() || !slot.mayPickup(who)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemResource resource = ItemResource.of(stack);
        if (contents.store(stack) > 0) {
            slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
            stored(server);
        } else {
            refusal = contents.refusal(resource);
        }
        return ItemStack.EMPTY;
    }

    private void stored(ServerPlayer who) {
        HexVaultBlockEntity vault = here();
        if (vault != null) {
            EssenceFxPayload.pour(who.level(), EssenceFxPayload.hand(who), middle(vault), HexVaultBlockEntity.HEXTECH, 14);
        }
        who.level().playSound(null, soundAt(who), ModSounds.VAULT_STORE.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
    }

    private void taken(ServerPlayer who, ItemResource resource) {
        HexVaultBlockEntity vault = here();
        float[] carries = VaultRules.primals(resource);
        int strongest = 0;
        for (int i = 1; i < carries.length; i++) {
            if (carries[i] > carries[strongest]) {
                strongest = i;
            }
        }
        if (vault != null) {
            int colour = carries[strongest] > 0 ? Essence.PRIMALS.get(strongest).color() : HexVaultBlockEntity.HEXTECH;
            EssenceFxPayload.pour(who.level(), middle(vault), EssenceFxPayload.hand(who), colour, 14);
        }
        if (kind != Kind.VAULT) {
            Vec3 at = kind == Kind.TERMINAL ? Vec3.atCenterOf(pos) : EssenceFxPayload.hand(who);
            who.level().sendParticles(ModParticles.glint(RiftGateBlock.RIFT), at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.02);
        }
        who.level().playSound(null, soundAt(who), ModSounds.VAULT_TAKE.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
    }

    /** The vault itself, when the screen stands at it. */
    @Nullable
    private HexVaultBlockEntity here() {
        return kind == Kind.VAULT && player.level().getBlockEntity(pos) instanceof HexVaultBlockEntity vault ? vault : null;
    }

    private static Vec3 middle(HexVaultBlockEntity vault) {
        return Vec3.atLowerCornerOf(vault.getBlockPos()).add(0.5, 1.875, 0.5);
    }

    private BlockPos soundAt(ServerPlayer who) {
        return kind == Kind.POCKET ? who.blockPosition() : pos;
    }

    @Override
    public boolean stillValid(Player who) {
        if (contents == null) {
            return true;
        }
        if (contents.home() == null || kind != Kind.VAULT && !contents.open()) {
            return false;
        }
        return switch (kind) {
            case VAULT -> who.level().getBlockEntity(pos) instanceof HexVaultBlockEntity at && at.id().equals(vault)
                    && Container.stillValidBlockEntity(at, who, REACH);
            case TERMINAL -> who.level().getBlockEntity(pos) instanceof HexTerminalBlockEntity at && vault != null && vault.equals(at.vault())
                    && Container.stillValidBlockEntity(at, who, REACH);
            case POCKET -> vault != null && vault.equals(PocketTerminalItem.vault(who.getItemInHand(hand)));
        };
    }

    // ---- Server: telling the client ------------------------------------------------------------------------------------

    @Override
    public void changed(Collection<VaultContents.Entry> entries) {
        touched.addAll(entries);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (contents != null && player instanceof ServerPlayer server) {
            send(server);
        }
    }

    private void send(ServerPlayer server) {
        VaultToll toll = VaultStore.of(server.level().getServer()).toll(server.getUUID());
        int nowWaiting = kind == Kind.VAULT ? 0 : toll.waiting(server.level().getGameTime());
        boolean nowDry = kind != Kind.VAULT && toll.dry();
        int nowNote = refusal == null ? -1 : refusal.ordinal();
        refusal = null;
        long nowUsed = contents.used();
        long nowCapacity = contents.capacity();
        boolean nowWorking = contents.open();
        boolean pinsMoved = !sentPins.equals(contents.pins());
        // Ticks left go down by themselves on the client: only a new wait is news.
        boolean news = pinsMoved || nowWorking != sentWorking || nowUsed != sentUsed || nowCapacity != sentCapacity || nowDry != sentDry || nowWaiting > sentWaiting || nowNote >= 0;
        sentWaiting = nowWaiting;
        if (sentAll && touched.isEmpty() && !news) {
            return;
        }
        List<Line> changed = new ArrayList<>();
        if (!sentAll) {
            contents.entries().forEach(entry -> changed.add(new Line(entry.resource(), entry.count(), entry.weight())));
        } else {
            touched.forEach(entry -> changed.add(new Line(entry.resource(), entry.count(), entry.weight())));
        }
        boolean reset = !sentAll;
        touched.clear();
        sentAll = true;
        sentUsed = nowUsed;
        sentCapacity = nowCapacity;
        sentDry = nowDry;
        sentWorking = nowWorking;
        sentPins = List.copyOf(contents.pins());
        int from = 0;
        do {
            int to = Math.min(changed.size(), from + LINES_PER_PACKET);
            ModNetwork.sendToPlayer(server, new VaultViewPayload(containerId, reset && from == 0, List.copyOf(changed.subList(from, to)), nowUsed, nowCapacity,
                    nowWaiting, nowDry, nowNote, nowWorking, sentPins));
            from = to;
        } while (from < changed.size());
    }

    @Override
    public void removed(Player who) {
        super.removed(who);
        if (contents != null) {
            contents.unwatch(this);
        }
    }

    // ---- Client --------------------------------------------------------------------------------------------------------

    /** A view from the server. */
    public void accept(VaultViewPayload view) {
        if (view.reset()) {
            lines.clear();
        }
        for (Line line : view.lines()) {
            if (line.count() > 0) {
                lines.put(line.resource(), line);
            } else {
                lines.remove(line.resource());
            }
        }
        used = view.used();
        capacity = view.capacity();
        waiting = view.waiting();
        dry = view.dry();
        working = view.working();
        pins = view.pins();
        note = view.note();
        revision++;
    }

    public Collection<Line> lines() {
        return lines.values();
    }

    /** The line of an item, or null when none is held. */
    @Nullable
    public Line line(ItemResource resource) {
        return lines.get(resource);
    }

    public List<ItemResource> pins() {
        return pins;
    }

    /** Whether the vault takes items in; out of its building or before the trial it only gives them back. */
    public boolean working() {
        return working;
    }

    public long used() {
        return used;
    }

    public long capacity() {
        return capacity;
    }

    /** Ticks the server said a dry take has to wait, as of the last view; the screen counts them down. */
    public int waiting() {
        return waiting;
    }

    public boolean dry() {
        return dry;
    }

    /** A refusal to show (a {@link VaultContents.Refusal} ordinal), or -1; taken once. */
    public int takeNote() {
        int shown = note;
        note = -1;
        return shown;
    }

    /** Bumped on every view, so the screen knows when to sort again. */
    public int revision() {
        return revision;
    }

    public Kind kind() {
        return kind;
    }
}
