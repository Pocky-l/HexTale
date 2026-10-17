package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
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
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.PylonRune;
import com.pockyl.hextale.transport.PylonFlight;
import com.pockyl.hextale.transport.PylonIndex;
import com.pockyl.hextale.transport.PylonInsert;
import com.pockyl.hextale.transport.PylonKind;
import com.pockyl.hextale.transport.PylonLink;
import com.pockyl.hextale.transport.PylonLinking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 */
public class HexPylonBlockEntity extends BlockEntity {
    public static final int MAX_LINKS = 4;
    public static final int MAX_HOPS = 8;
    /** Flights shown per move: a stack of 64 shows a few items in the air, not 64. */
    public static final int SHOWN_FLIGHTS = 6;
    /** Longest sleep of an idle pylon, in ticks. */
    public static final int MAX_SLEEP = 80;
    /** Client: ticks between the tendrils of one move. */
    private static final int STAGGER_TICKS = 4;

    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private final List<PylonLink> links = new ArrayList<>();
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

    // ---- Links (changed by players) -------------------------------------------------------------------------------------

    public boolean linked(PylonLink link) {
        return links.stream().anyMatch(l -> l.pos().equals(link.pos()));
    }

    /** Adds the link, or takes away a link to the same block; false when there is no room for a new one. */
    public boolean toggleLink(PylonLink link) {
        if (unlink(link.pos())) {
            return true;
        }
        if (links.size() >= MAX_LINKS) {
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
        if (--pylon.wait > 0) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        List<PylonFlight> flights = new ArrayList<>();
        switch (pylon.kind()) {
            case GATHERING -> pylon.gather(server, flights);
            case SENDING -> pylon.send(server, flights);
            case SIPHON -> pylon.siphon(server, flights);
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
        List<Destination> routes = below ? List.of() : destinations(level);
        if (!below && routes.isEmpty()) {
            return;
        }
        int r = radius();
        List<ItemEntity> lying = level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition).inflate(r),
                item -> item.isAlive() && !item.hasPickUpDelay() && !item.getItem().isEmpty());
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
            int want = Math.min(budget, stack.getCount());
            int moved = 0;
            List<Vec3> path = new ArrayList<>();
            path.add(item.position().add(0, 0.25, 0));
            path.add(gem);
            if (below) {
                moved = PylonInsert.insert(level, belowPos, Direction.UP, resource, want, null);
                path.add(Vec3.atBottomCenterOf(worldPosition).add(0, 0.05, 0));
            } else {
                for (Destination route : routes) {
                    moved = PylonInsert.insert(level, route.pos(), route.face(), resource, want, null);
                    if (moved > 0) {
                        path.addAll(route.path());
                        break;
                    }
                }
            }
            if (moved <= 0) {
                continue;
            }
            budget -= moved;
            flights.add(new PylonFlight(stack.copyWithCount(1), path, true));
            stack.shrink(moved);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
        }
    }

    /** Takes what the container below gives out and throws it to the links, in their order. */
    void send(ServerLevel level, List<PylonFlight> flights) {
        if (links.isEmpty()) {
            return;
        }
        ResourceHandler<ItemResource> from = level.getCapability(Capabilities.Item.BLOCK, worldPosition.below(), Direction.DOWN);
        if (from == null) {
            return;
        }
        int budget = amount();
        Vec3 start = Vec3.atBottomCenterOf(worldPosition).add(0, 0.05, 0);
        for (Destination route : destinations(level)) {
            // Slot by slot of what the container gives out, each item where the target takes it (a furnace sorts ore and
            // fuel), up to four kinds of item per route while the budget lasts.
            int turns = 0;
            for (int slot = 0; slot < from.size() && budget > 0 && turns < 4; slot++) {
                ItemResource resource = from.getResource(slot);
                int available = from.getAmountAsInt(slot);
                if (resource.isEmpty() || available <= 0) {
                    continue;
                }
                // First find out how many the target takes (a trial that is thrown away), then move exactly that many.
                int moved;
                try (Transaction trial = Transaction.openRoot()) {
                    int extracted = from.extract(slot, resource, Math.min(budget, available), trial);
                    moved = extracted <= 0 ? 0 : PylonInsert.insert(level, route.pos(), route.face(), resource, extracted, trial);
                }
                if (moved <= 0) {
                    continue;
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    int taken = from.extract(slot, resource, moved, transaction);
                    if (taken != moved || PylonInsert.insert(level, route.pos(), route.face(), resource, taken, transaction) != taken) {
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
                flights.add(new PylonFlight(resource.toStack(1), path, false));
            }
            if (budget <= 0) {
                return;
            }
        }
    }

    /** mB a siphon pours per move: four times a sending pylon's items (Lift raises both). */
    public int essenceAmount() {
        return amount() * 4;
    }

    /**
     * Draws essence out of the jar under it and pours it to the links, in their order: each receiver (a jar, a separator,
     * a smelter...) takes what it can through the linked face, or any other face that lets it in.
     */
    void siphon(ServerLevel level, List<PylonFlight> flights) {
        if (links.isEmpty() || !(level.getBlockEntity(worldPosition.below()) instanceof EssenceTankBlockEntity jar) || jar.tank().isEmpty()) {
            return;
        }
        int budget = essenceAmount();
        Vec3 start = Vec3.atBottomCenterOf(worldPosition).add(0, -0.05, 0);
        for (Destination route : destinations(level, true)) {
            if (budget <= 0) {
                return;
            }
            if (!(level.getBlockEntity(route.pos()) instanceof EssenceNetwork.Receiver receiver) || route.pos().equals(worldPosition.below())) {
                continue;
            }
            EssenceMix offer = jar.tank().drain(budget, true);
            if (offer.isEmpty()) {
                return;
            }
            Direction face = receivingFace(receiver, route.face(), offer);
            if (face == null) {
                continue;
            }
            int taken = receiver.receive(face, offer, true);
            EssenceMix drawn = jar.tank().drain(taken, false);
            receiver.receive(face, drawn, false);
            jar.setChanged();
            budget -= drawn.total();
            List<Vec3> path = new ArrayList<>();
            path.add(start);
            path.add(gem());
            path.addAll(route.path());
            flights.add(PylonFlight.essence(path, drawn.color()));
        }
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

    /** A place items can go: a linked block and its face ({@link PylonInsert} picks the slots), and the way there after this pylon's gem. */
    record Destination(BlockPos pos, Direction face, List<Vec3> path) {
    }

    /** Every place this pylon reaches through its links, in link order (relays' links in theirs). */
    List<Destination> destinations(ServerLevel level) {
        return destinations(level, kind().essence());
    }

    /** The same, keeping only the places that take essence ({@code essence}) or items. */
    List<Destination> destinations(ServerLevel level, boolean essence) {
        List<Destination> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(worldPosition);
        collect(level, this, new ArrayList<>(), seen, 0, essence, out);
        return out;
    }

    private static void collect(ServerLevel level, HexPylonBlockEntity node, List<Vec3> path, Set<BlockPos> seen, int hops, boolean essence,
                                List<Destination> out) {
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
                collect(level, next, nextPath, seen, hops + 1, essence, out);
                seen.remove(link.pos());
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
            List<Vec3> full = new ArrayList<>(path);
            full.add(link.approach());
            full.add(link.entry());
            out.add(new Destination(link.pos(), link.face(), full));
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

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        links.clear();
        input.read("links", PylonLink.CODEC.listOf()).ifPresent(links::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        if (!links.isEmpty()) {
            output.store("links", PylonLink.CODEC.listOf(), List.copyOf(links));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
