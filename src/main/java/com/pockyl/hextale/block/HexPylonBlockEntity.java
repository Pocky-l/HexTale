package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.network.PylonFlightsPayload;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.PylonRune;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.transport.ItemFilter;
import com.pockyl.hextale.transport.PylonFlight;
import com.pockyl.hextale.transport.PylonIndex;
import com.pockyl.hextale.transport.PylonInsert;
import com.pockyl.hextale.transport.PylonKind;
import com.pockyl.hextale.transport.PylonLink;
import com.pockyl.hextale.transport.PylonLinking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A hex pylon's state and work. The server moves items at once through the containers' item handlers (one transaction
 * per move, so sided inventories, modded machines and full containers behave); the clients only see the items fly
 * ({@link PylonFlight}, sent in one small payload per move).
 *
 * <p>Gathering: every interval, the items lying within the radius (nearest first, past their pickup delay) go into the
 * container below through its top - or, with no container below, to the links. Sending: what the container below gives
 * out through its bottom (a furnace's result, a chest's anything) goes to the links in order, each link through the face
 * it was made by; a link to a relay passes on through the relay's own links, up to {@value #MAX_HOPS} relays, each hop
 * within the range of the pylon it leaves. Furnaces are filled by {@link PylonInsert}: what they cook into the input,
 * fuel into the fuel slot. Nothing to do = the pylon sleeps longer each time (up to four intervals), so idle pylons cost
 * almost nothing.
 *
 * <p>Sorting: the matrix reads the container under it and sends each item to its home - a storage within the radius or
 * on a link that already holds that item, or a link whose relay filter names it; what has no home goes to a linked
 * machine that takes it (a furnace what it cooks or burns), and what nobody wants stays in the container below.
 */
public class HexPylonBlockEntity extends BlockEntity {
    public static final int MAX_LINKS = 4;
    /** A sorting matrix is a hub: it holds more links. */
    public static final int SORTING_LINKS = 8;
    /** Item kinds a sorting matrix sends per move, each to its own home. */
    public static final int SORT_KINDS = 8;
    /** Ticks between two looks of a sorting matrix for the storages around it. */
    private static final int SCAN_TICKS = 100;
    public static final int MAX_HOPS = 8;
    /** Flights shown per move: a stack of 64 shows a few items in the air, not 64. */
    public static final int SHOWN_FLIGHTS = 6;
    /** Longest sleep of an idle pylon, in ticks. */
    public static final int MAX_SLEEP = 80;
    /** Client: ticks between the tendrils of one move. */
    private static final int STAGGER_TICKS = 4;

    @Nullable
    private UUID owner;
    private String ownerName = "";
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private final List<PylonLink> links = new ArrayList<>();
    /** The Pylon Filter set into it (one item, given back when taken out or broken). */
    private ItemStack filterItem = ItemStack.EMPTY;
    /** The essence sticker on a siphon or relay: it carries only that essence. */
    private @Nullable Essence sticker;
    private int wait;
    private int idle;
    /** Server: the flights of the last move (what the clients were sent), for tests. */
    private List<PylonFlight> lastFlights = List.of();
    /** Client: flights in the air now. */
    public final List<PylonFlight.Shown> shown = new ArrayList<>();
    /** Client: game time of the last move seen, for the gem's flare. */
    public long movedAt = -1000;

    public HexPylonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_PYLON.get(), pos, state);
    }

    public PylonKind kind() {
        return getBlockState().getBlock() instanceof HexPylonBlock pylon ? pylon.kind() : PylonKind.RELAY;
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public List<PylonLink> links() {
        return links;
    }

    public List<PylonFlight> lastFlights() {
        return lastFlights;
    }

    public Vec3 gem() {
        return kind().gem(worldPosition);
    }

    /** How deeply it sleeps: 0 after a move, up to 2 after looks that found nothing (it then looks four times rarer). */
    public int drowsiness() {
        return idle;
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    private int strength(PylonRune rune) {
        return runes.strength(rune.rune());
    }

    public int radius() {
        return PylonRune.radius(strength(PylonRune.SHIFT));
    }

    public int range() {
        return PylonRune.range(strength(PylonRune.SHIFT));
    }

    public int interval() {
        return PylonRune.interval(strength(PylonRune.MOTOR));
    }

    public int amount() {
        return PylonRune.amount(strength(PylonRune.LIFT));
    }

    /** How many links it holds: four, a sorting matrix eight. */
    public int maxLinks() {
        return kind() == PylonKind.SORTING ? SORTING_LINKS : MAX_LINKS;
    }

    // ---- Filter and sticker ---------------------------------------------------------------------------------------------

    /** The filter's settings, or null when it has none. */
    public @Nullable ItemFilter filter() {
        return filterItem.isEmpty() ? null : filterItem.getOrDefault(ModDataComponents.ITEM_FILTER.get(), ItemFilter.EMPTY);
    }

    public ItemStack filterItem() {
        return filterItem;
    }

    /** Sets a filter in (one item of {@code stack}); returns the one that was there (or empty). */
    public ItemStack setFilter(ItemStack stack) {
        ItemStack old = filterItem;
        filterItem = stack.copyWithCount(1);
        wake();
        changed();
        return old;
    }

    public ItemStack takeFilter() {
        ItemStack old = filterItem;
        filterItem = ItemStack.EMPTY;
        if (!old.isEmpty()) {
            wake();
            changed();
        }
        return old;
    }

    public @Nullable Essence sticker() {
        return sticker;
    }

    /** Sets the sticker; returns the essence of the one that was there. */
    public @Nullable Essence setSticker(@Nullable Essence essence) {
        Essence old = sticker;
        sticker = essence;
        wake();
        changed();
        return old;
    }

    // ---- Links (changed by players) -------------------------------------------------------------------------------------

    public boolean linked(PylonLink link) {
        return links.stream().anyMatch(l -> l.pos().equals(link.pos()));
    }

    /** Adds the link, or takes away a link to the same block; false when there is no room for a new one. */
    public boolean toggleLink(PylonLink link) {
        if (unlink(link.pos())) {
            return true;
        }
        if (links.size() >= maxLinks()) {
            return false;
        }
        links.add(link);
        if (level != null && !level.isClientSide()) {
            PylonIndex.add(level, worldPosition, link.pos());
        }
        wake();
        changed();
        return true;
    }

    /** Takes away the link to {@code target}; false when there was none. */
    public boolean unlink(BlockPos target) {
        if (!links.removeIf(l -> l.pos().equals(target))) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            PylonIndex.remove(level, worldPosition, target);
        }
        changed();
        return true;
    }

    /** Takes every link away; false when it had none. */
    public boolean clearLinks() {
        if (links.isEmpty()) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            PylonIndex.removeAll(level, worldPosition, links);
        }
        links.clear();
        changed();
        return true;
    }

    /** Server: a link to a block that no longer takes items (broken, replaced) goes away; false when it still holds. */
    public boolean dropIfBroken(PylonLink link) {
        if (level == null || !level.isLoaded(link.pos()) || PylonLinking.linkable(level, link.pos(), link.face())) {
            return false;
        }
        return unlink(link.pos());
    }

    /** Broken: the filter and the sticker drop with it. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            if (!filterItem.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), filterItem.copy());
            }
            for (Cargo c : cargo) {
                if (!c.item().isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), c.item().copy());
                }
            }
            cargo.clear();
            if (sticker != null) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        com.pockyl.hextale.item.EssenceStickerItem.of(com.pockyl.hextale.registry.ModItems.ESSENCE_STICKER.get(), sticker));
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            for (PylonLink link : links) {
                PylonIndex.add(level, worldPosition, link.pos());
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            PylonIndex.removeAll(level, worldPosition, links);
        }
        super.setRemoved();
    }

    /** Look again at once (a link changed, the container below changed). */
    public void wake() {
        wait = Math.min(wait, 2);
        idle = 0;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Work -----------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, HexPylonBlockEntity pylon) {
        ServerLevel server = (ServerLevel) level;
        pylon.deliver(server);
        if (--pylon.wait > 0) {
            return;
        }
        List<PylonFlight> flights = new ArrayList<>();
        switch (pylon.kind()) {
            case GATHERING -> pylon.gather(server, flights);
            case SENDING -> pylon.send(server, flights);
            case SIPHON -> pylon.siphon(server, flights);
            case SORTING -> pylon.sort(server, flights);
            case RELAY -> {
            }
        }
        int interval = pylon.interval();
        if (flights.isEmpty()) {
            pylon.idle = Math.min(pylon.idle + 1, 2);
            pylon.wait = Math.min(MAX_SLEEP, interval << pylon.idle);
        } else {
            pylon.idle = 0;
            pylon.wait = interval;
            pylon.lastFlights = List.copyOf(flights);
            // Only to players whose client has the mod's channel (fake players have none).
            PylonFlightsPayload payload = new PylonFlightsPayload(pos, PylonFlight.limit(flights, SHOWN_FLIGHTS));
            for (net.minecraft.server.level.ServerPlayer player : server.getChunkSource().chunkMap.getPlayers(ChunkPos.containing(pos), false)) {
                com.pockyl.hextale.network.ModNetwork.sendToPlayer(player, payload);
            }
        }
    }

    /** Pulls the items lying around into the container below (or to the links); every item moved adds a flight. */
    void gather(ServerLevel level, List<PylonFlight> flights) {
        BlockPos belowPos = worldPosition.below();
        boolean below = level.getCapability(Capabilities.Item.BLOCK, belowPos, Direction.UP) != null;
        List<Destination> routes = below ? List.of(new Destination(belowPos, Direction.UP,
                List.of(Vec3.atBottomCenterOf(worldPosition).add(0, 0.05, 0)), List.of())) : destinations(level);
        if (routes.isEmpty()) {
            return;
        }
        ItemFilter own = filter();
        int r = radius();
        List<ItemEntity> lying = level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition).inflate(r),
                item -> item.isAlive() && !item.hasPickUpDelay() && !item.getItem().isEmpty() && (own == null || own.passes(item.getItem())));
        if (lying.isEmpty()) {
            return;
        }
        Vec3 gem = gem();
        lying.sort(Comparator.comparingDouble(item -> item.distanceToSqr(gem)));
        int budget = amount();
        for (ItemEntity item : lying) {
            if (budget <= 0) {
                break;
            }
            ItemStack stack = item.getItem();
            ItemResource resource = ItemResource.of(stack);
            int moved = 0;
            List<Vec3> path = new ArrayList<>();
            path.add(item.position().add(0, 0.25, 0));
            path.add(gem);
            Destination to = null;
            for (Destination route : inTurn(routes)) {
                int want = Math.min(Math.min(budget, stack.getCount()), room(level, route, resource));
                if (want <= 0 || !route.passes(resource)) {
                    continue;
                }
                // How many it would take now (a trial that is thrown away); they arrive when the flight does, so what is
                // already flying there counts against the room.
                int way = onTheWay(route.pos(), resource);
                try (Transaction trial = Transaction.openRoot()) {
                    moved = Math.min(want, PylonInsert.insert(level, route.pos(), route.face(), resource, want + way, trial, owner) - way);
                }
                if (moved > 0) {
                    path.addAll(route.path());
                    to = route;
                    served(routes, route);
                    break;
                }
            }
            if (moved <= 0 || to == null) {
                continue;
            }
            budget -= moved;
            PylonFlight flight = new PylonFlight(stack.copyWithCount(1), path, true);
            ship(level, flights, flight, stack.copyWithCount(moved), EssenceMix.EMPTY, to.pos(), to.face());
            stack.shrink(moved);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
        }
    }

    /**
     * Takes what the container below gives out and throws it to one of its links per move, the links taking turns (a
     * link that takes nothing now is skipped), so every storage linked gets its share.
     */
    void send(ServerLevel level, List<PylonFlight> flights) {
        if (links.isEmpty()) {
            return;
        }
        ResourceHandler<ItemResource> from = level.getCapability(Capabilities.Item.BLOCK, worldPosition.below(), Direction.DOWN);
        if (from == null) {
            return;
        }
        ItemFilter own = filter();
        int budget = amount();
        Vec3 start = Vec3.atBottomCenterOf(worldPosition).add(0, 0.05, 0);
        List<Destination> routes = destinations(level);
        for (Destination route : inTurn(routes)) {
            // Slot by slot of what the container gives out, each item where the target takes it (a furnace sorts ore and
            // fuel), up to four kinds of item per route while the budget lasts.
            int turns = 0;
            for (int slot = 0; slot < from.size() && budget > 0 && turns < 4; slot++) {
                ItemResource resource = from.getResource(slot);
                int available = from.getAmountAsInt(slot);
                if (resource.isEmpty() || available <= 0 || own != null && !own.passes(resource) || !route.passes(resource)) {
                    continue;
                }
                int want = Math.min(Math.min(budget, available), room(level, route, resource));
                if (own != null) {
                    // Keep N in the container: only what is above it may go.
                    want = Math.min(want, own.sendable(resource, own.count(from, resource)));
                }
                if (want <= 0) {
                    continue;
                }
                // First find out how many the target takes (a trial that is thrown away), counting what is already flying
                // there, then move exactly that many. A target with room for less than a whole batch waits until it has
                // it: no single items flown to top up a nearly full slot.
                int way = onTheWay(route.pos(), resource);
                int moved;
                try (Transaction trial = Transaction.openRoot()) {
                    int extracted = from.extract(slot, resource, want, trial);
                    moved = extracted <= 0 ? 0
                            : Math.min(extracted, PylonInsert.insert(level, route.pos(), route.face(), resource, extracted + way, trial, owner) - way);
                }
                if (moved <= 0 || moved < want) {
                    continue;
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    if (from.extract(slot, resource, moved, transaction) != moved) {
                        continue;
                    }
                    transaction.commit();
                }
                turns++;
                budget -= moved;
                List<Vec3> path = new ArrayList<>();
                path.add(start);
                path.add(gem());
                path.addAll(route.path());
                ship(level, flights, new PylonFlight(resource.toStack(1), path, false), resource.toStack(moved), EssenceMix.EMPTY,
                        route.pos(), route.face());
            }
            if (turns > 0) {
                served(routes, route);
                return;
            }
        }
    }

    // ---- Sorting ------------------------------------------------------------------------------------------------------

    /** The storages around a sorting matrix (nearest first), found every {@value #SCAN_TICKS} ticks. */
    private List<BlockPos> storages = List.of();
    private long scannedAt = -SCAN_TICKS;

    /**
     * Sorts the container under the matrix: up to {@value #SORT_KINDS} kinds of item per move, each to its home
     * ({@link #homes}); a home with room for only a part takes that part, the rest waits for the next move or the next home.
     */
    void sort(ServerLevel level, List<PylonFlight> flights) {
        BlockPos belowPos = worldPosition.below();
        ResourceHandler<ItemResource> from = level.getCapability(Capabilities.Item.BLOCK, belowPos, Direction.DOWN);
        if (from == null) {
            return;
        }
        List<Destination> routes = sortingRoutes(level);
        if (routes.isEmpty()) {
            return;
        }
        ItemFilter own = filter();
        Map<BlockPos, Set<Item>> held = new HashMap<>();
        Set<Item> seen = new HashSet<>();
        Vec3 start = Vec3.atBottomCenterOf(worldPosition).add(0, 0.05, 0);
        int kinds = 0;
        for (int slot = 0; slot < from.size() && kinds < SORT_KINDS; slot++) {
            ItemResource resource = from.getResource(slot);
            int available = from.getAmountAsInt(slot);
            if (resource.isEmpty() || available <= 0 || own != null && !own.passes(resource) || !seen.add(resource.getItem())) {
                continue;
            }
            int budget = Math.min(amount(), available);
            if (own != null) {
                budget = Math.min(budget, own.sendable(resource, own.count(from, resource)));
            }
            if (budget <= 0) {
                continue;
            }
            for (Destination route : homes(level, routes, resource, held)) {
                int want = Math.min(budget, room(level, route, resource));
                if (want <= 0) {
                    continue;
                }
                int way = onTheWay(route.pos(), resource);
                int moved;
                try (Transaction trial = Transaction.openRoot()) {
                    int extracted = from.extract(slot, resource, want, trial);
                    moved = extracted <= 0 ? 0
                            : Math.min(extracted, PylonInsert.insert(level, route.pos(), route.face(), resource, extracted + way, trial, owner) - way);
                }
                if (moved <= 0) {
                    continue;
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    if (from.extract(slot, resource, moved, transaction) != moved) {
                        continue;
                    }
                    transaction.commit();
                }
                kinds++;
                List<Vec3> path = new ArrayList<>();
                path.add(start);
                path.add(gem());
                path.addAll(route.path());
                ship(level, flights, new PylonFlight(resource.toStack(1), path, false), resource.toStack(moved), EssenceMix.EMPTY,
                        route.pos(), route.face());
                break;
            }
        }
    }

    /**
     * Where an item may go, best first: its homes - a route whose relay filter names it, or a storage that already
     * holds it - then the linked machines (not plain storages) that take it; a furnace only what it cooks (fuel only as a
     * home: a furnace burning coal gets more coal). A plain storage that does not hold the item never gets it: a chest
     * becomes the home of an item by holding one.
     */
    private List<Destination> homes(ServerLevel level, List<Destination> routes, ItemResource resource, Map<BlockPos, Set<Item>> held) {
        List<Destination> homes = new ArrayList<>();
        List<Destination> machines = new ArrayList<>();
        for (Destination route : routes) {
            if (!route.passes(resource)) {
                continue;
            }
            if (route.lists(resource) || held.computeIfAbsent(route.pos(), pos -> itemsIn(level, pos, route.face())).contains(resource.getItem())) {
                homes.add(route);
            } else if (!isSortingStorage(level.getBlockState(route.pos()))
                    && (!PylonInsert.isFurnace(level, route.pos()) || PylonInsert.cooks(level, route.pos(), resource))) {
                machines.add(route);
            }
        }
        homes.addAll(machines);
        return homes;
    }

    /** The kinds of item a block holds, looked at through any face (a furnace's every slot), else through the link's. */
    private static Set<Item> itemsIn(ServerLevel level, BlockPos pos, Direction face) {
        ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, null);
        if (handler == null) {
            handler = level.getCapability(Capabilities.Item.BLOCK, pos, face);
        }
        Set<Item> items = new HashSet<>();
        if (handler != null) {
            for (int i = 0; i < handler.size(); i++) {
                ItemResource there = handler.getResource(i);
                if (!there.isEmpty() && handler.getAmountAsInt(i) > 0) {
                    items.add(there.getItem());
                }
            }
        }
        return items;
    }

    /** The matrix's routes: its links (through relays too), then the storages around it that no link already names. */
    private List<Destination> sortingRoutes(ServerLevel level) {
        List<Destination> routes = new ArrayList<>(destinations(level));
        routes.removeIf(route -> route.pos().equals(worldPosition.below()));
        Set<BlockPos> linked = new HashSet<>();
        for (Destination route : routes) {
            linked.add(route.pos());
        }
        long now = level.getGameTime();
        if (now - scannedAt >= SCAN_TICKS || now < scannedAt) {
            storages = sortingStorages(level, worldPosition, radius());
            scannedAt = now;
        }
        for (BlockPos pos : storages) {
            if (!linked.contains(pos) && level.getCapability(Capabilities.Item.BLOCK, pos, Direction.UP) != null) {
                PylonLink link = new PylonLink(pos, Direction.UP);
                routes.add(new Destination(pos, Direction.UP, List.of(link.approach(), link.entry()), List.of()));
            }
        }
        return routes;
    }

    /** Look for the storages around again at the next move (a storage was placed or broken nearby). */
    public void rescan() {
        scannedAt = -SCAN_TICKS;
        wake();
    }

    /**
     * The plain storages (chests, barrels, shulker boxes: block tag {@code hextale:sorting_storage}) within {@code radius}
     * of a sorting matrix at {@code pos}, nearest first - without the one under it and without any that carries a pylon
     * (that one feeds a pylon, it is no store). Both sides use it: the goggles show them.
     */
    public static List<BlockPos> sortingStorages(Level level, BlockPos pos, int radius) {
        List<BlockPos> out = new ArrayList<>();
        BlockPos below = pos.below();
        for (int cx = (pos.getX() - radius) >> 4; cx <= (pos.getX() + radius) >> 4; cx++) {
            for (int cz = (pos.getZ() - radius) >> 4; cz <= (pos.getZ() + radius) >> 4; cz++) {
                if (!(level.getChunkSource().getChunkNow(cx, cz) instanceof net.minecraft.world.level.chunk.LevelChunk chunk)) {
                    continue;
                }
                for (BlockPos at : chunk.getBlockEntities().keySet()) {
                    if (Math.abs(at.getX() - pos.getX()) <= radius && Math.abs(at.getY() - pos.getY()) <= radius
                            && Math.abs(at.getZ() - pos.getZ()) <= radius && !at.equals(below)
                            && isSortingStorage(level.getBlockState(at)) && !(level.getBlockEntity(at.above()) instanceof HexPylonBlockEntity)) {
                        out.add(at.immutable());
                    }
                }
            }
        }
        out.sort(Comparator.comparingDouble(at -> at.distSqr(pos)));
        return out;
    }

    public static boolean isSortingStorage(BlockState state) {
        return state.is(com.pockyl.hextale.registry.ModTags.SORTING_STORAGE);
    }

    /** Which route is served first next time: a pylon with several links takes them in turn. */
    private int nextRoute;

    /** The routes starting with the one whose turn it is. */
    private List<Destination> inTurn(List<Destination> routes) {
        if (routes.size() < 2) {
            return routes;
        }
        int first = Math.floorMod(nextRoute, routes.size());
        List<Destination> out = new ArrayList<>(routes.subList(first, routes.size()));
        out.addAll(routes.subList(0, first));
        return out;
    }

    /** The route got its share: the next one is first next time. */
    private void served(List<Destination> routes, Destination route) {
        nextRoute = routes.indexOf(route) + 1;
    }

    /** How many more of the item the route's target may take under the "fill up to N" rule of any filter on the way. */
    private int room(ServerLevel level, Destination route, ItemResource resource) {
        int room = Integer.MAX_VALUE;
        List<ItemFilter> filters = new ArrayList<>();
        if (filter() != null) {
            filters.add(filter());
        }
        for (HexPylonBlockEntity relay : route.through()) {
            if (relay.filter() != null) {
                filters.add(relay.filter());
            }
        }
        for (ItemFilter filter : filters) {
            if (filter.rule() == ItemFilter.Rule.LIMIT) {
                ResourceHandler<ItemResource> handler = PylonInsert.handler(level, route.pos(), route.face(), resource);
                room = Math.min(room, handler == null ? 0 : filter.room(resource, filter.count(handler, resource) + onTheWay(route.pos(), resource)));
            }
        }
        return room;
    }

    // ---- Cargo on its way --------------------------------------------------------------------------------------------

    /**
     * Something moved and still in the air: it left its source when the move began and arrives at {@code arriveAt}, the
     * game tick its flight lands on the clients (so nothing is in the target before the eye sees it get there).
     */
    public record Cargo(long arriveAt, ItemStack item, EssenceMix essence, BlockPos target, Direction face) {
        public static final Codec<Cargo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("arrive_at").forGetter(Cargo::arriveAt),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Cargo::item),
                EssenceMix.CODEC.optionalFieldOf("essence", EssenceMix.EMPTY).forGetter(Cargo::essence),
                BlockPos.CODEC.fieldOf("target").forGetter(Cargo::target),
                Direction.CODEC.fieldOf("face").forGetter(Cargo::face)
        ).apply(instance, Cargo::new));
    }

    private final List<Cargo> cargo = new ArrayList<>();

    public List<Cargo> cargo() {
        return cargo;
    }

    /** Adds the flight (shown to the clients) and its cargo, due when that flight lands - after the tendrils before it. */
    private void ship(ServerLevel level, List<PylonFlight> flights, PylonFlight flight, ItemStack item, EssenceMix essence, BlockPos target,
                      Direction face) {
        int index = Math.min(flights.size(), SHOWN_FLIGHTS - 1);
        flights.add(flight);
        cargo.add(new Cargo(level.getGameTime() + (long) index * STAGGER_TICKS + flight.duration(), item, essence, target, face));
        setChanged();
    }

    /** How many of the item are already on their way to {@code target}. */
    private int onTheWay(BlockPos target, ItemResource resource) {
        int total = 0;
        for (Cargo c : cargo) {
            if (c.target().equals(target) && !c.item().isEmpty() && resource.matches(c.item())) {
                total += c.item().getCount();
            }
        }
        return total;
    }

    /** mB of essence already on its way to {@code target}. */
    private int essenceOnTheWay(BlockPos target) {
        int total = 0;
        for (Cargo c : cargo) {
            if (c.target().equals(target)) {
                total += c.essence().total();
            }
        }
        return total;
    }

    /** Delivers the cargo whose flight has landed. */
    private void deliver(ServerLevel level) {
        if (cargo.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        List<Cargo> due = new ArrayList<>();
        cargo.removeIf(c -> {
            if (c.arriveAt() <= now) {
                due.add(c);
                return true;
            }
            return false;
        });
        for (Cargo c : due) {
            land(level, c);
        }
        if (!due.isEmpty()) {
            setChanged();
        }
    }

    /**
     * Puts landed cargo in. What the target no longer takes (it filled up meanwhile, or is gone) goes back to the source -
     * the container or jar under the pylon - and what that cannot take either drops by the pylon (essence evaporates).
     */
    private void land(ServerLevel level, Cargo c) {
        BlockPos source = worldPosition.below();
        if (!c.item().isEmpty()) {
            for (ItemStack stack : enrich(level, c.item(), c.target())) {
                ItemResource resource = ItemResource.of(stack);
                int rest = stack.getCount();
                if (level.isLoaded(c.target())) {
                    rest -= PylonInsert.insert(level, c.target(), c.face(), resource, rest, null, owner);
                }
                if (rest > 0 && level.getCapability(Capabilities.Item.BLOCK, source, Direction.UP) != null) {
                    rest -= PylonInsert.insert(level, source, Direction.UP, resource, rest, null, owner);
                }
                if (rest > 0) {
                    net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                            worldPosition.getZ() + 0.5, stack.copyWithCount(rest));
                }
            }
            return;
        }
        EssenceMix rest = c.essence();
        if (level.isLoaded(c.target()) && level.getBlockEntity(c.target()) instanceof EssenceNetwork.Receiver receiver) {
            int taken = receiver.receive(c.face(), rest, false);
            rest = rest.minus(rest.part(taken));
        }
        if (!rest.isEmpty() && level.getBlockEntity(source) instanceof EssenceTankBlockEntity jar && !jar.isCreative()) {
            jar.tank().fill(rest, false);
            jar.setChanged();
        }
    }

    /** Stone that Erosion grinds down, one step at a time. */
    private static final java.util.Map<net.minecraft.world.item.Item, net.minecraft.world.item.Item> GRIND = java.util.Map.of(
            net.minecraft.world.item.Items.STONE, net.minecraft.world.item.Items.COBBLESTONE,
            net.minecraft.world.item.Items.DEEPSLATE, net.minecraft.world.item.Items.COBBLED_DEEPSLATE,
            net.minecraft.world.item.Items.COBBLESTONE, net.minecraft.world.item.Items.GRAVEL,
            net.minecraft.world.item.Items.COBBLED_DEEPSLATE, net.minecraft.world.item.Items.GRAVEL,
            net.minecraft.world.item.Items.GRAVEL, net.minecraft.world.item.Items.SAND,
            net.minecraft.world.item.Items.SANDSTONE, net.minecraft.world.item.Items.SAND,
            net.minecraft.world.item.Items.RED_SANDSTONE, net.minecraft.world.item.Items.RED_SAND);

    /**
     * What the cargo arrives as: Smelting smelts each piece a furnace would with a chance, Erosion grinds each piece of
     * stone one step with a chance; Radiance may leave a little experience by the target. Each change flashes the rune's
     * light at the target. The stacks keep the cargo's order: changed pieces first, the rest after.
     */
    public List<ItemStack> enrich(ServerLevel level, ItemStack item, BlockPos target) {
        var random = level.getRandom();
        Vec3 at = Vec3.atCenterOf(target).add(0, 0.6, 0);
        List<ItemStack> out = new ArrayList<>();
        ItemStack rest = item.copy();
        int smelting = strength(PylonRune.SMELTING);
        if (smelting > 0) {
            var input = new net.minecraft.world.item.crafting.SingleRecipeInput(rest.copyWithCount(1));
            var recipe = level.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level);
            ItemStack result = recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
            int lucky = lucky(random, rest.getCount(), HolderRune.at(PylonRune.SMELT_CHANCE, smelting));
            if (!result.isEmpty() && lucky > 0) {
                rest.shrink(lucky);
                split(out, result.copyWithCount(1), result.getCount() * lucky);
                ToolEffects.puff(level, at, PylonRune.SMELTING.rune(), 5, 0.25);
                level.playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25F, 1.6F);
            }
        }
        int erosion = strength(PylonRune.EROSION);
        net.minecraft.world.item.Item ground = rest.isEmpty() ? null : GRIND.get(rest.getItem());
        if (erosion > 0 && ground != null) {
            int lucky = lucky(random, rest.getCount(), HolderRune.at(PylonRune.GRIND_CHANCE, erosion));
            if (lucky > 0) {
                rest.shrink(lucky);
                split(out, new ItemStack(ground), lucky);
                ToolEffects.puff(level, at, PylonRune.EROSION.rune(), 5, 0.25);
                level.playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.BRUSH_GRAVEL_COMPLETED, SoundSource.BLOCKS, 0.4F, 1.2F);
            }
        }
        if (!rest.isEmpty()) {
            out.add(rest);
        }
        int radiance = strength(PylonRune.RADIANCE);
        if (radiance > 0 && random.nextFloat() < HolderRune.at(PylonRune.EXPERIENCE_CHANCE, radiance)) {
            net.minecraft.world.entity.ExperienceOrb.award(level, at, 1 + random.nextInt(2));
            ToolEffects.puff(level, at, PylonRune.RADIANCE.rune(), 4, 0.2);
        }
        return out;
    }

    /** How many of {@code count} pieces win a roll of {@code chance} each. */
    private static int lucky(net.minecraft.util.RandomSource random, int count, float chance) {
        int won = 0;
        for (int i = 0; i < count; i++) {
            if (random.nextFloat() < chance) {
                won++;
            }
        }
        return won;
    }

    /** {@code count} of {@code one} into {@code out}, in stacks it can hold. */
    private static void split(List<ItemStack> out, ItemStack one, int count) {
        while (count > 0) {
            int part = Math.min(count, one.getMaxStackSize());
            out.add(one.copyWithCount(part));
            count -= part;
        }
    }

    /** mB a siphon pours per move: four times a sending pylon's items (Lift raises both). */
    public int essenceAmount() {
        return amount() * 4;
    }

    /**
     * Draws essence out of the jar under it and pours it to the links, in their order: each receiver (a jar, a separator,
     * a smelter...) takes what it can through the linked face, or any other face that lets it in. A mixture always travels
     * whole, in its own proportions - pylons never split essences (that is the separator's work). Stickers only let
     * through: when the siphon, a relay on the way or the target jar wears one, the way carries the jar's essence only
     * while it is that essence, pure (a way whose stickers disagree carries nothing). The one exception is a Creative
     * Essence Jar, which holds every essence: there a sticker picks the essence it gives.
     */
    void siphon(ServerLevel level, List<PylonFlight> flights) {
        // On a jar, or on the lid of a Big Essence Jar.
        EssenceTankBlockEntity jar = EssenceTankBlockEntity.at(level, worldPosition.below());
        if (links.isEmpty() || jar == null || jar.tank().isEmpty()) {
            return;
        }
        int budget = essenceAmount();
        Vec3 start = Vec3.atBottomCenterOf(worldPosition).add(0, -0.05, 0);
        List<Destination> routes = destinations(level, true);
        for (Destination route : inTurn(routes)) {
            BlockEntity target = level.getBlockEntity(route.pos());
            EssenceTankBlockEntity targetJar = EssenceTankBlockEntity.at(level, route.pos());
            if (!(target instanceof EssenceNetwork.Receiver receiver) || route.pos().equals(worldPosition.below()) || targetJar == jar) {
                continue;
            }
            Essence only = sticker;
            boolean clash = false;
            for (HexPylonBlockEntity relay : route.through()) {
                clash |= only != null && relay.sticker() != null && relay.sticker() != only;
                only = only == null ? relay.sticker() : only;
            }
            if (targetJar != null && targetJar.sticker() != null) {
                clash |= only != null && targetJar.sticker() != only;
                only = only == null ? targetJar.sticker() : only;
            }
            if (clash) {
                continue;
            }
            // A Creative Essence Jar holds every essence at once: there a sticker picks the one it gives.
            boolean pick = only != null && jar.isCreative();
            EssenceMix offer = pick ? EssenceMix.pure(only, Math.min(budget, jar.tank().contents().get(only))) : jar.tank().drain(budget, true);
            if (offer.isEmpty() || only != null && !pick && !pure(jar.tank().contents(), only)) {
                continue;
            }
            Direction face = receivingFace(receiver, route.face(), offer);
            if (face == null) {
                continue;
            }
            // What is already flying there counts against the room it has: ask the target about this mixture together
            // with what is on its way (the same mixture, repeated), then take that away again.
            int way = essenceOnTheWay(route.pos());
            EssenceMix asked = offer;
            while (asked.total() < offer.total() + way) {
                asked = asked.plus(offer);
            }
            int taken = Math.min(offer.total(), receiver.receive(face, asked, true) - way);
            if (taken <= 0) {
                continue;
            }
            EssenceMix drawn;
            if (pick) {
                drawn = EssenceMix.pure(only, taken);
                jar.tank().remove(drawn);
            } else {
                drawn = jar.tank().drain(taken, false);
            }
            jar.setChanged();
            budget -= drawn.total();
            List<Vec3> path = new ArrayList<>();
            path.add(start);
            path.add(gem());
            path.addAll(route.path());
            // Motes arc on their own: straight from the last gem into the top, without the turn above it.
            path.remove(path.size() - 2);
            ship(level, flights, PylonFlight.essence(path, drawn.color()), ItemStack.EMPTY, drawn, route.pos(), face);
            served(routes, route);
            return;
        }
    }

    /** Whether the mixture is this one essence and nothing else. */
    private static boolean pure(EssenceMix mix, Essence essence) {
        return mix.unknown() == 0 && mix.get(essence) == mix.total();
    }

    /** The face a receiver takes this essence through: the linked one first, then any side, then the top. */
    private static @Nullable Direction receivingFace(EssenceNetwork.Receiver receiver, Direction linked, EssenceMix offer) {
        if (receiver.receive(linked, offer, true) > 0) {
            return linked;
        }
        for (Direction face : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP}) {
            if (face != linked && receiver.receive(face, offer, true) > 0) {
                return face;
            }
        }
        return null;
    }

    /**
     * A place items can go: a linked block and its face ({@link PylonInsert} picks the slots), the way there after this
     * pylon's gem, and the relays on the way (their filters and stickers rule too).
     */
    record Destination(BlockPos pos, Direction face, List<Vec3> path, List<HexPylonBlockEntity> through) {
        boolean passes(ItemResource resource) {
            for (HexPylonBlockEntity relay : through) {
                ItemFilter filter = relay.filter();
                if (filter != null && !filter.passes(resource)) {
                    return false;
                }
            }
            return true;
        }

        /** Whether a relay's filter on the way names the item: for a sorting matrix that makes the way its home. */
        boolean lists(ItemResource resource) {
            for (HexPylonBlockEntity relay : through) {
                ItemFilter filter = relay.filter();
                if (filter != null && filter.lists(resource)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Every place this pylon reaches through its links, in link order (relays' links in theirs). */
    List<Destination> destinations(ServerLevel level) {
        return destinations(level, kind().essence());
    }

    /**
     * The same, keeping only the places that take essence ({@code essence}) or items, in link order (a relay's places
     * where the relay's link stands). Every relay is passed once, by the first way to it, and every place is reached once:
     * walking every possible way through a mesh of relays grows without bound.
     */
    List<Destination> destinations(ServerLevel level, boolean essence) {
        List<Destination> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(worldPosition);
        collect(level, this, new ArrayList<>(), new ArrayList<>(), seen, 0, essence, out);
        return out;
    }

    private static void collect(ServerLevel level, HexPylonBlockEntity node, List<Vec3> path, List<HexPylonBlockEntity> through,
                                Set<BlockPos> seen, int hops, boolean essence, List<Destination> out) {
        Vec3 gem = node.gem();
        double range = node.range() + 0.5;
        // Links to blocks that are gone go away (a broken block normally drops them at once, see PylonLinking).
        for (PylonLink link : List.copyOf(node.links)) {
            node.dropIfBroken(link);
        }
        for (PylonLink link : node.links) {
            if (!level.isLoaded(link.pos()) || seen.contains(link.pos())) {
                continue;
            }
            if (level.getBlockEntity(link.pos()) instanceof HexPylonBlockEntity next) {
                if (next.kind() != PylonKind.RELAY || hops >= MAX_HOPS || gem.distanceTo(next.gem()) > range) {
                    continue;
                }
                seen.add(link.pos());
                List<Vec3> nextPath = new ArrayList<>(path);
                nextPath.add(next.gem());
                List<HexPylonBlockEntity> nextThrough = new ArrayList<>(through);
                nextThrough.add(next);
                collect(level, next, nextPath, nextThrough, seen, hops + 1, essence, out);
                continue;
            }
            if (gem.distanceTo(Vec3.atCenterOf(link.pos())) > range) {
                continue;
            }
            boolean takes = essence ? level.getBlockEntity(link.pos()) instanceof EssenceNetwork.Receiver
                    : level.getCapability(Capabilities.Item.BLOCK, link.pos(), link.face()) != null;
            if (!takes) {
                continue;
            }
            seen.add(link.pos());
            List<Vec3> full = new ArrayList<>(path);
            full.add(link.approach());
            full.add(link.entry());
            out.add(new Destination(link.pos(), link.face(), full, List.copyOf(through)));
        }
    }

    /** Whether a link from this pylon could reach the block (gem to block centre within range). */
    public boolean reaches(BlockPos target) {
        Vec3 to = level != null && level.getBlockEntity(target) instanceof HexPylonBlockEntity other ? other.gem() : Vec3.atCenterOf(target);
        return gem().distanceTo(to) <= range() + 0.5;
    }

    // ---- Client ---------------------------------------------------------------------------------------------------------

    /** Client: flights arrived from the server. */
    public void show(List<PylonFlight> flights, long now) {
        // Each item gets its own tendril, a moment after the one before: many hands, not one jerk.
        for (int i = 0; i < flights.size(); i++) {
            shown.add(new PylonFlight.Shown(flights.get(i), now + i * STAGGER_TICKS));
        }
        movedAt = now;
    }

    // ---- Saving: the runes travel with the item -------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.PYLON_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.PYLON_RUNES.get(), runes);
        }
    }

    /**
     * The player who placed the pylon: it delivers on their behalf, so a machine that needs knowledge (the Alembic boils
     * only studied items) reads theirs.
     */
    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().name();
        setChanged();
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    /** The owner's name as it was when the pylon was placed, for the goggles; empty when nobody owns it. */
    public String ownerName() {
        return ownerName;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        ownerName = input.getStringOr("owner_name", "");
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        links.clear();
        input.read("links", PylonLink.CODEC.listOf()).ifPresent(links::addAll);
        filterItem = input.read("filter", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        sticker = Essence.byId(input.getStringOr("sticker", ""));
        cargo.clear();
        input.read("cargo", Cargo.CODEC.listOf()).ifPresent(cargo::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) {
            output.store("owner", UUIDUtil.CODEC, owner);
            output.putString("owner_name", ownerName);
        }
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        if (!links.isEmpty()) {
            output.store("links", PylonLink.CODEC.listOf(), List.copyOf(links));
        }
        if (!filterItem.isEmpty()) {
            output.store("filter", ItemStack.CODEC, filterItem);
        }
        if (sticker != null) {
            output.putString("sticker", sticker.id());
        }
        if (!cargo.isEmpty()) {
            output.store("cargo", Cargo.CODEC.listOf(), List.copyOf(cargo));
        }
    }

    /** The clients need the links, runes, filter and sticker - not the cargo in the air. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove("cargo");
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
