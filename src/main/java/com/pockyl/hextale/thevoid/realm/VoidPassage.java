package com.pockyl.hextale.thevoid.realm;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidLayout;
import com.pockyl.hextale.thevoid.VoidShape;

/**
 * The way into the Void and out of it. A breach held by six anchors lets a player of our world walk through; on the
 * other side the same breach stands as a gate ({@link TheVoid#gate()}) at its place in the Void (our world's position
 * divided by {@link #SCALE}) on a ledge of chitin, and walking into a gate leads out to its breach.
 * <p>
 * The way out is always open: a gate cannot be closed or broken, it stays when its breach is gone (leading to where it
 * stood) and follows its breach when a Breach Heart moves it, and every player in the Void is checked now and then for a
 * gate to leave by - one is made beside them if none is known.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class VoidPassage {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Hextale.id("the_void"));
    /** Our world's blocks per block of the Void. */
    public static final int SCALE = 8;
    /** The height a gate's ledge stands at when there is no ground under it to stand on. */
    public static final int LEDGE_Y = 120;
    /** How much a player stepping through wears the anchors' crystals. */
    public static final float ENTER_STRAIN = 400;
    /** Ticks after a passage before the same player passes again. */
    public static final int REST_TICKS = 60;
    private static final int CHECK_TICKS = 40;
    /** Players who came out by a breach and have not stepped clear of it yet, by the breach's seed. */
    private static final Map<java.util.UUID, Integer> STILL_IN = new java.util.concurrent.ConcurrentHashMap<>();

    private VoidPassage() {
    }

    /** A gate: the breach of {@code seed} seen from inside, and where its breach stands (or stood) in our world. */
    public record Gate(int seed, Vec3 at, float yaw, String shape, float size, ResourceKey<Level> home, Vec3 homeAt, float homeYaw, boolean built) {
        static final Codec<Gate> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("seed").forGetter(Gate::seed),
                Vec3.CODEC.fieldOf("at").forGetter(Gate::at),
                Codec.FLOAT.fieldOf("yaw").forGetter(Gate::yaw),
                Codec.STRING.fieldOf("shape").forGetter(Gate::shape),
                Codec.FLOAT.fieldOf("size").forGetter(Gate::size),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("home").forGetter(Gate::home),
                Vec3.CODEC.fieldOf("home_at").forGetter(Gate::homeAt),
                Codec.FLOAT.fieldOf("home_yaw").forGetter(Gate::homeYaw),
                Codec.BOOL.optionalFieldOf("built", false).forGetter(Gate::built)
        ).apply(i, Gate::new));

        VoidShape voidShape() {
            return VoidShape.byId(shape);
        }

        Gate built(boolean built) {
            return new Gate(seed, at, yaw, shape, size, home, homeAt, homeYaw, built);
        }

        Gate home(ResourceKey<Level> home, Vec3 homeAt, float homeYaw) {
            return new Gate(seed, at, yaw, shape, size, home, homeAt, homeYaw, built);
        }
    }

    /** Where a player in the Void came in from: the gate they arrived by and the spot in our world they stepped off. */
    public record Return(int seed, ResourceKey<Level> home, Vec3 at, float yaw) {
        public static final Codec<Return> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("seed").forGetter(Return::seed),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("home").forGetter(Return::home),
                Vec3.CODEC.fieldOf("at").forGetter(Return::at),
                Codec.FLOAT.fieldOf("yaw").forGetter(Return::yaw)
        ).apply(i, Return::new));
        public static final Return NONE = new Return(0, Level.OVERWORLD, Vec3.ZERO, 0);

        public boolean none() {
            return this.equals(NONE);
        }

        /** Whether the spot they stepped in from is known (a gate given them later knows only its breach). */
        public boolean knowsSpot() {
            return !at.equals(Vec3.ZERO);
        }
    }

    /** Every gate there is, kept with the overworld. */
    public static final class Gates extends SavedData {
        public static final Codec<Gates> CODEC = Gate.CODEC.listOf().fieldOf("gates").codec()
                .xmap(Gates::new, gates -> List.copyOf(gates.bySeed.values()));
        public static final SavedDataType<Gates> TYPE = new SavedDataType<>(Hextale.id("void_gates"), Gates::new, CODEC);

        private final Map<Integer, Gate> bySeed = new HashMap<>();

        public Gates() {
        }

        private Gates(List<Gate> gates) {
            gates.forEach(gate -> bySeed.put(gate.seed(), gate));
        }

        public @Nullable Gate get(int seed) {
            return bySeed.get(seed);
        }

        public void put(Gate gate) {
            bySeed.put(gate.seed(), gate);
            setDirty();
        }

        public java.util.Collection<Gate> all() {
            return bySeed.values();
        }
    }

    public static Gates gates(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Gates.TYPE);
    }

    public static Return returnOf(ServerPlayer player) {
        return player.getData(ModAttachments.VOID_RETURN);
    }

    // ------------------------------------------------------------------ in

    /**
     * A player touched a held breach at {@code local} (its own frame): they are taken through to its gate. Returns false
     * when there is no Void to go to (the breach then throws them back as it always did).
     */
    public static boolean enter(ServerLevel level, TheVoid breach, ServerPlayer player, Vector3f local) {
        if (level.dimension().equals(LEVEL)) {
            return false;
        }
        ServerLevel realm = level.getServer().getLevel(LEVEL);
        if (realm == null) {
            return false;
        }
        Integer stillIn = STILL_IN.get(player.getUUID());
        if (player.isOnPortalCooldown() || stillIn != null && stillIn == breach.seed()) {
            // Just came out of it, and not yet stepped off: the breach lets them be.
            return true;
        }
        Gates gates = gates(level.getServer());
        Gate gate = gates.get(breach.seed());
        if (gate == null) {
            gate = newGate(breach, level);
            gates.put(gate);
        }
        gate = raise(realm, gate);
        gates.put(gate);
        // They will come back out on the side they went in from, a step off the breach.
        Vector3f side = breach.orientation().transform(new Vector3f(0, 0, local.z >= 0 ? 1 : -1));
        float off = breach.layout().halfThickness() + 2.2F;
        Vec3 back = new Vec3(player.getX() + side.x * off, player.getY(), player.getZ() + side.z * off);
        player.setData(ModAttachments.VOID_RETURN, new Return(gate.seed(), level.dimension(), back, Mth.wrapDegrees(player.getYRot() + 180)));
        breach.strainAnchors(level, ENTER_STRAIN);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.THE_VOID_EAT.get(), SoundSource.PLAYERS, 0.9F, 0.6F);
        // They arrive with the gate at their back, looking out into the Void.
        Vec3 arrival = arrival(gate);
        player.teleport(new TeleportTransition(realm, arrival, Vec3.ZERO, gate.yaw(), 0, TeleportTransition.DO_NOTHING));
        player.setPortalCooldown(REST_TICKS);
        player.resetFallDistance();
        realm.playSound(null, arrival.x, arrival.y, arrival.z, ModSounds.THE_VOID_OPEN.get(), SoundSource.PLAYERS, 0.8F, 0.7F);
        com.pockyl.hextale.quest.Quests.fire(player, com.pockyl.hextale.quest.Quests.VOID_ENTERED);
        return true;
    }

    private static Gate newGate(TheVoid breach, ServerLevel level) {
        VoidLayout layout = breach.layout();
        double x = Math.floor(breach.getX() / SCALE) + 0.5;
        double z = Math.floor(breach.getZ() / SCALE) + 0.5;
        Vec3 at = new Vec3(x, LEDGE_Y + 0.6 - layout.bottom(), z);
        return new Gate(breach.seed(), at, breach.getYRot(), breach.shape().id(), breach.size(), level.dimension(), breach.position(),
                breach.getYRot(), false);
    }

    /** Where one arrives at a gate: on its ledge, a few steps out of its front. */
    private static Vec3 arrival(Gate gate) {
        Vector3f front = TheVoid.orientation(gate.yaw(), gate.voidShape()).transform(new Vector3f(0, 0, 1));
        return new Vec3(gate.at().x + front.x * 2.6, ledgeY(gate), gate.at().z + front.z * 2.6);
    }

    /** The height one stands at in front of a gate (the gate's lowest cells hang a little above it). */
    private static int ledgeY(Gate gate) {
        VoidLayout layout = VoidLayout.of(gate.voidShape(), gate.size(), gate.seed());
        return Mth.floor(gate.at().y - 0.6 + layout.bottom() + 0.01);
    }

    /**
     * The ground a gate stands on: the top of the plate under (x, z), looked for from high above down to low, skipping
     * the ribs' crests; {@link #LEDGE_Y} when the column falls straight into the abyss.
     */
    private static int ground(ServerLevel realm, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 230, z);
        for (; pos.getY() > 30; pos.move(net.minecraft.core.Direction.DOWN)) {
            BlockState state = realm.getBlockState(pos);
            if ((state.is(ModBlocks.VOID_SOIL.get()) || state.is(ModBlocks.VOID_ROCK.get())) && realm.getBlockState(pos.above()).isAir()) {
                return pos.getY() + 1;
            }
        }
        return LEDGE_Y;
    }

    /**
     * Makes sure the gate stands in the Void: the first time, the place is cleared and a ledge of chitin laid under it;
     * every time, the gate itself is put back if it is not there.
     */
    static Gate raise(ServerLevel realm, Gate gate) {
        BlockPos column = BlockPos.containing(gate.at().x, LEDGE_Y, gate.at().z);
        realm.getChunk(column);
        VoidLayout layout = VoidLayout.of(gate.voidShape(), gate.size(), gate.seed());
        if (!gate.built()) {
            // Stand it on the plate below, if there is one: one arrives on the ground of the Void, not on a perch.
            int y = ground(realm, column.getX(), column.getZ());
            gate = new Gate(gate.seed(), new Vec3(gate.at().x, y + 0.6 - layout.bottom(), gate.at().z), gate.yaw(), gate.shape(),
                    gate.size(), gate.home(), gate.homeAt(), gate.homeYaw(), true);
            ledge(realm, new BlockPos(column.getX(), y, column.getZ()), layout);
        }
        if (find(realm, gate) == null) {
            TheVoid entity = TheVoid.gate(realm, gate.at(), gate.yaw(), gate.voidShape(), gate.size(), gate.seed());
            realm.addFreshEntity(entity);
        }
        return gate;
    }

    private static @Nullable TheVoid find(ServerLevel realm, Gate gate) {
        AABB around = new AABB(gate.at(), gate.at()).inflate(3);
        for (TheVoid found : realm.getEntitiesOfClass(TheVoid.class, around, TheVoid::gate)) {
            if (found.seed() == gate.seed()) {
                return found;
            }
        }
        return null;
    }

    /** A disc of chitin with a gold-lit lip and dark flesh beneath, and room cleared above it. */
    private static void ledge(ServerLevel realm, BlockPos centre, VoidLayout layout) {
        int ledgeY = centre.getY();
        int radius = Math.max(5, Mth.ceil(layout.reach()) + 3);
        int height = Mth.ceil(layout.height * layout.cell) + 4;
        BlockState chitin = ModBlocks.HUSK_CHITIN.get().defaultBlockState();
        BlockState ridge = ModBlocks.HUSK_CHITIN_RIDGE.get().defaultBlockState();
        BlockState flesh = ModBlocks.VOID_ROCK.get().defaultBlockState();
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                for (int dy = 0; dy <= height; dy++) {
                    if (d <= radius + 1 - dy * 0.15) {
                        pos.set(centre.getX() + dx, ledgeY + dy, centre.getZ() + dz);
                        if (!realm.getBlockState(pos).isAir()) {
                            realm.setBlock(pos, air, Block.UPDATE_CLIENTS);
                        }
                    }
                }
                if (d > radius + 0.5) {
                    continue;
                }
                // The ledge: a shallow bowl, thickest in the middle, its rim lit.
                pos.set(centre.getX() + dx, ledgeY - 1, centre.getZ() + dz);
                realm.setBlock(pos, d > radius - 0.8 ? ridge : chitin, Block.UPDATE_CLIENTS);
                int under = (int) Math.round((radius - d) * 0.6) + 1;
                for (int dy = 2; dy <= under + 1; dy++) {
                    pos.set(centre.getX() + dx, ledgeY - dy, centre.getZ() + dz);
                    realm.setBlock(pos, dy > under ? chitin : flesh, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /**
     * {@code /hextale void enter}: into the Void from where the player stands, without a breach - a gate is raised at the
     * place an eighth of the way, leading back to this spot.
     */
    public static void enterHere(ServerPlayer player) {
        if (inVoid(player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        ServerLevel realm = server.getLevel(LEVEL);
        if (realm == null) {
            return;
        }
        VoidShape shape = VoidShape.GATE;
        float size = 3.5F;
        int seed = player.getRandom().nextInt();
        VoidLayout layout = VoidLayout.of(shape, size, seed);
        Vec3 at = new Vec3(Math.floor(player.getX() / SCALE) + 0.5, LEDGE_Y + 0.6 - layout.bottom(), Math.floor(player.getZ() / SCALE) + 0.5);
        Gate gate = raise(realm, new Gate(seed, at, player.getYRot(), shape.id(), size, player.level().dimension(), player.position(),
                player.getYRot(), false));
        gates(server).put(gate);
        player.setData(ModAttachments.VOID_RETURN, new Return(seed, player.level().dimension(), player.position(), player.getYRot()));
        Vec3 arrival = arrival(gate);
        player.teleport(new TeleportTransition(realm, arrival, Vec3.ZERO, gate.yaw(), 0, TeleportTransition.DO_NOTHING));
        player.setPortalCooldown(REST_TICKS);
    }

    // ------------------------------------------------------------------ out

    /** A gate's turn: whoever of our world touches it is let out to its breach. */
    public static void gateTick(ServerLevel realm, TheVoid gate) {
        if (realm.getGameTime() % 2 != 0) {
            return;
        }
        VoidLayout layout = gate.layout();
        float reveal = gate.reveal(0);
        Quaternionf inverse = gate.orientation().conjugate(new Quaternionf());
        AABB area = new AABB(gate.position(), gate.position()).inflate(layout.reach() + 1.5);
        for (ServerPlayer player : realm.getEntitiesOfClass(ServerPlayer.class, area, p -> p.isAlive() && !p.isSpectator())) {
            Vec3 centre = player.getBoundingBox().getCenter();
            Vector3f local = inverse.transform(new Vector3f((float) (centre.x - gate.getX()), (float) (centre.y - gate.getY()),
                    (float) (centre.z - gate.getZ())));
            float halfW = player.getBbWidth() * 0.5F;
            if (Math.abs(local.z) > halfW + layout.halfThickness() || !layout.touches(local.x, local.y, halfW, player.getBbHeight() * 0.5F, reveal)) {
                continue;
            }
            leave(player, gate.seed());
        }
    }

    /** Out through the gate of {@code seed}: to the spot the player stepped in from, or in front of that gate's breach. */
    public static void leave(ServerPlayer player, int seed) {
        if (player.isOnPortalCooldown()) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        Gate gate = gates(server).get(seed);
        Return back = returnOf(player);
        ServerLevel home;
        Vec3 to;
        float yaw;
        if (!back.none() && back.seed() == seed && back.knowsSpot() && server.getLevel(back.home()) != null) {
            home = server.getLevel(back.home());
            to = back.at();
            yaw = back.yaw();
        } else if (gate != null && server.getLevel(gate.home()) != null) {
            home = server.getLevel(gate.home());
            Vector3f front = TheVoid.orientation(gate.homeYaw(), gate.voidShape()).transform(new Vector3f(0, 0, 1));
            float off = VoidLayout.of(gate.voidShape(), gate.size(), gate.seed()).halfThickness() + 1.6F;
            to = ground(home, gate.homeAt().add(front.x * off, 0, front.z * off));
            yaw = gate.homeYaw();
        } else {
            home = server.overworld();
            to = Vec3.atBottomCenterOf(home.getRespawnData().pos());
            yaw = 0;
        }
        out(player, home, to, yaw);
    }

    private static void out(ServerPlayer player, ServerLevel home, Vec3 to, float yaw) {
        Return back = returnOf(player);
        if (!back.none()) {
            STILL_IN.put(player.getUUID(), back.seed());
        }
        player.setData(ModAttachments.VOID_RETURN, Return.NONE);
        player.teleport(new TeleportTransition(home, to, Vec3.ZERO, yaw, player.getXRot(), TeleportTransition.DO_NOTHING));
        player.setPortalCooldown(REST_TICKS);
        player.resetFallDistance();
        home.playSound(null, to.x, to.y, to.z, ModSounds.THE_VOID_CLOSE.get(), SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    /** Standing ground at (x, z) in our world near {@code near}'s height: the column's surface. */
    private static Vec3 ground(ServerLevel level, Vec3 near) {
        BlockPos column = BlockPos.containing(near);
        level.getChunk(column);
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        return Vec3.atBottomCenterOf(top.getY() > near.y + 12 ? column : top);
    }

    /**
     * A Recall Charm used in the Void finds no trail to follow here: it pulls its bearer out to our world, to where they
     * stepped in (or the nearest gate's breach). Returns where they landed.
     */
    public static Vec3 recall(ServerPlayer player) {
        Return back = returnOf(player);
        int seed = !back.none() ? back.seed() : nearest(player).seed();
        leaveNow(player, seed);
        return player.position();
    }

    private static void leaveNow(ServerPlayer player, int seed) {
        player.setPortalCooldown(0);
        leave(player, seed);
    }

    /** The gate nearest to a player in the Void, made beside them when there is none at all. */
    private static Gate nearest(ServerPlayer player) {
        Gates gates = gates(player.level().getServer());
        Gate best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Gate gate : gates.all()) {
            double distance = gate.at().distanceToSqr(player.position());
            if (distance < bestDistance) {
                best = gate;
                bestDistance = distance;
            }
        }
        if (best == null) {
            best = scar(player);
        }
        return best;
    }

    /**
     * A gate made where none was: beside a player who found themselves in the Void with no known way out. It leads to
     * the world's spawn.
     */
    private static Gate scar(ServerPlayer player) {
        ServerLevel overworld = player.level().getServer().overworld();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        VoidShape shape = VoidShape.GATE;
        float size = 3;
        int seed = player.getRandom().nextInt();
        VoidLayout layout = VoidLayout.of(shape, size, seed);
        Vec3 at = player.position().add(look.scale(4)).add(0, 0.6 - layout.bottom(), 0);
        float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
        Gate gate = new Gate(seed, at, yaw, shape.id(), size, Level.OVERWORLD, Vec3.atBottomCenterOf(overworld.getRespawnData().pos()), 0, true);
        gates(player.level().getServer()).put(gate);
        player.level().addFreshEntity(TheVoid.gate(player.level(), at, yaw, shape, size, seed));
        return gate;
    }

    // ------------------------------------------------------------------ keeping it so

    /** A breach saw a player near it but not touching it: they have stepped clear and may go in again. */
    public static void clearOf(ServerPlayer player, int seed) {
        STILL_IN.remove(player.getUUID(), seed);
    }

    /** A held breach tells its gate where it stands now (a Breach Heart may have carried it elsewhere). */
    public static void noteBreach(ServerLevel level, TheVoid breach) {
        Gates gates = gates(level.getServer());
        Gate gate = gates.get(breach.seed());
        if (gate != null && (!gate.home().equals(level.dimension()) || gate.homeAt().distanceToSqr(breach.position()) > 1)) {
            gates.put(gate.home(level.dimension(), breach.position(), breach.getYRot()));
        }
    }

    /** Every player in the Void always has a gate to leave by. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.level().dimension().equals(LEVEL)
                || player.tickCount % CHECK_TICKS != 0) {
            return;
        }
        Return back = returnOf(player);
        Gates gates = gates(player.level().getServer());
        if (back.none() || gates.get(back.seed()) == null) {
            Gate gate = nearest(player);
            player.setData(ModAttachments.VOID_RETURN, new Return(gate.seed(), gate.home(), Vec3.ZERO, gate.homeYaw()));
            back = returnOf(player);
        }
        Gate gate = gates.get(back.seed());
        if (gate != null && player.level() instanceof ServerLevel realm && realm.isLoaded(BlockPos.containing(gate.at()))
                && find(realm, gate) == null) {
            // Its gate must stand: nothing may close the way out.
            realm.addFreshEntity(TheVoid.gate(realm, gate.at(), gate.yaw(), gate.voidShape(), gate.size(), gate.seed()));
        }
    }

    /** Whether an entity is in the Void. */
    public static boolean inVoid(Entity entity) {
        return entity.level().dimension().equals(LEVEL);
    }
}
