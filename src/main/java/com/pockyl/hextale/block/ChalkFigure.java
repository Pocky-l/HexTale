package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.ShackleEvents;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A binding figure drawn in Binding Chalk: chalk sigils joined into a closed line round at least one cell of floor. The rule
 * that keeps it fair:
 * <ul>
 *   <li>only a CLOSED figure binds (one line, all on one level, at most {@value #MAX_CELLS} cells round at most
 *   {@value #MAX_INSIDE} cells of floor), and only what stands inside it or on its line;</li>
 *   <li>it holds up to {@value #CAPACITY} creatures - not bosses, not tamed ones - which then skip their ticks entirely
 *   (the Rune Shackles' hold, {@link ShackleEvents}), so a held crowd costs the server nothing;</li>
 *   <li>a player who steps in is only rooted for {@value #ROOT} ticks and then cannot be rooted again for
 *   {@value #IMMUNE} ticks - long enough to walk out;</li>
 *   <li>holding is paid with shadow essence drawn from jars near the figure's first cell (1 mB holds one creature for
 *   {@value #HOLD_PER_MB} ticks); with no shadow at hand the chalk itself wears, and after {@value #WEAR} creature-ticks
 *   a cell smudges away and the figure opens - a field trap holds for a while, a farm needs jars.</li>
 * </ul>
 * The figure's first cell (lowest x, then z) does the holding on a scheduled tick every {@value #PERIOD} ticks.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ChalkFigure {
    public static final int MAX_CELLS = 64;
    public static final int MAX_INSIDE = 49;
    public static final int CAPACITY = 6;
    public static final int PERIOD = 5;
    public static final int HOLD_PER_MB = 600;
    private static final int DRAW_MB = 4;
    public static final int WEAR = 600;
    public static final int ROOT = 30;
    public static final int IMMUNE = 160;
    /** Creature-ticks a rooting costs. */
    private static final int ROOT_COST = 60;
    private static final AttributeModifier ROOT_SPEED = new AttributeModifier(Hextale.id("chalk_root"), -1,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final AttributeModifier ROOT_JUMP = new AttributeModifier(Hextale.id("chalk_root"), -1,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    /** Players rooted now, until when, and when they may be rooted again. */
    private static final Map<UUID, long[]> ROOTED = new ConcurrentHashMap<>();
    /** What each figure has paid for and how worn its chalk is, by level and first cell (not saved: a reload is a fresh start). */
    private static final Map<Level, Map<Long, Purse>> PURSES = new WeakHashMap<>();

    private ChalkFigure() {
    }

    /** The cells of a drawing and the floor they close round (empty when the line is open). */
    public record Figure(Set<BlockPos> cells, Set<BlockPos> inside, BlockPos first) {
        public boolean closed() {
            return !inside.isEmpty();
        }

        /** Whether the cell at {@code column} (any height on the figure's level) is in or on the figure. */
        public boolean covers(BlockPos column) {
            BlockPos flat = new BlockPos(column.getX(), first.getY(), column.getZ());
            return cells.contains(flat) || inside.contains(flat);
        }
    }

    private static final class Purse {
        int budget;
        int wear;
    }

    /** The drawing {@code start} belongs to: its joined cells and, if it closes, the floor inside. */
    public static Figure trace(Level level, BlockPos start) {
        Set<BlockPos> cells = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        open.add(start);
        cells.add(start);
        while (!open.isEmpty()) {
            BlockPos at = open.poll();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos next = at.relative(side);
                if (!cells.contains(next) && level.getBlockState(next).is(ModBlocks.CHALK_SIGIL.get())) {
                    if (cells.size() >= MAX_CELLS) {
                        return new Figure(cells, Set.of(), first(cells));
                    }
                    cells.add(next);
                    open.add(next);
                }
            }
        }
        BlockPos first = first(cells);
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos cell : cells) {
            minX = Math.min(minX, cell.getX());
            minZ = Math.min(minZ, cell.getZ());
            maxX = Math.max(maxX, cell.getX());
            maxZ = Math.max(maxZ, cell.getZ());
        }
        if (maxX - minX < 2 || maxZ - minZ < 2) {
            return new Figure(cells, Set.of(), first);
        }
        // The floor reachable from outside the drawing's box without crossing a line is outside; the rest is inside.
        int y = start.getY();
        Set<BlockPos> outside = new HashSet<>();
        Deque<BlockPos> flood = new ArrayDeque<>();
        BlockPos corner = new BlockPos(minX - 1, y, minZ - 1);
        outside.add(corner);
        flood.add(corner);
        while (!flood.isEmpty()) {
            BlockPos at = flood.poll();
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos next = at.relative(side);
                if (next.getX() < minX - 1 || next.getX() > maxX + 1 || next.getZ() < minZ - 1 || next.getZ() > maxZ + 1
                        || outside.contains(next) || cells.contains(next)) {
                    continue;
                }
                outside.add(next);
                flood.add(next);
            }
        }
        Set<BlockPos> inside = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos at = new BlockPos(x, y, z);
                if (!cells.contains(at) && !outside.contains(at)) {
                    inside.add(at);
                }
            }
        }
        return new Figure(cells, inside.size() > MAX_INSIDE ? Set.of() : inside, first);
    }

    private static BlockPos first(Set<BlockPos> cells) {
        return cells.stream().min(Comparator.<BlockPos>comparingInt(cell -> cell.getX()).thenComparingInt(cell -> cell.getZ())).orElseThrow();
    }

    /** Looks at the figure {@code pos} is part of: wakes or puts its cells to sleep, and lets its first cell hold. */
    public static void settle(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.CHALK_SIGIL.get())) {
            return;
        }
        Figure figure = trace(level, pos);
        boolean closed = figure.closed();
        boolean woke = false;
        for (BlockPos cell : figure.cells()) {
            BlockState state = level.getBlockState(cell);
            if (state.getValue(ChalkSigilBlock.ACTIVE) != closed) {
                woke |= closed;
                level.setBlock(cell, state.setValue(ChalkSigilBlock.ACTIVE, closed), Block.UPDATE_CLIENTS);
            }
        }
        Map<Long, Purse> purses = PURSES.computeIfAbsent(level, key -> new ConcurrentHashMap<>());
        if (!closed) {
            purses.remove(figure.first().asLong());
            return;
        }
        if (woke) {
            Vec3 middle = middle(figure);
            level.playSound(null, middle.x, middle.y, middle.z, ModSounds.CHALK_CLOSE.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            // Light runs round the line from its first cell, closes the loop, and the whole figure glows.
            com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.TRACE, middle,
                    alongTheLine(figure), ChalkSigilBlock.SHADOW, 1, false);
            Quests.fireNear(level, middle, 12, Quests.BINDING_FIGURE);
        }
        if (pos.equals(figure.first())) {
            hold(level, figure, purses.computeIfAbsent(pos.asLong(), key -> new Purse()));
            level.scheduleTick(pos, ModBlocks.CHALK_SIGIL.get(), PERIOD);
        } else if (!level.getBlockTicks().hasScheduledTick(figure.first(), ModBlocks.CHALK_SIGIL.get())) {
            level.scheduleTick(figure.first(), ModBlocks.CHALK_SIGIL.get(), 1);
        }
    }

    /**
     * The figure's cells in the order the line runs, from its first cell: each next one a neighbour not yet walked
     * (a branch's dead end jumps to the nearest cell left, which the drawing skips).
     */
    static List<BlockPos> alongTheLine(Figure figure) {
        Set<BlockPos> left = new HashSet<>(figure.cells());
        List<BlockPos> line = new ArrayList<>(left.size());
        BlockPos at = figure.first();
        while (at != null) {
            left.remove(at);
            line.add(at);
            BlockPos next = null;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (left.contains(at.relative(side))) {
                    next = at.relative(side);
                    break;
                }
            }
            if (next == null && !left.isEmpty()) {
                BlockPos from = at;
                next = left.stream().min(Comparator.comparingDouble(cell -> cell.distSqr(from))).orElse(null);
            }
            at = next;
        }
        return line;
    }

    private static Vec3 middle(Figure figure) {
        double x = 0;
        double z = 0;
        for (BlockPos cell : figure.cells()) {
            x += cell.getX() + 0.5;
            z += cell.getZ() + 0.5;
        }
        return new Vec3(x / figure.cells().size(), figure.first().getY() + 0.1, z / figure.cells().size());
    }

    /** Holds the creatures standing in the figure and roots the players who just stepped in. */
    private static void hold(ServerLevel level, Figure figure, Purse purse) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos cell : figure.cells()) {
            minX = Math.min(minX, cell.getX());
            minZ = Math.min(minZ, cell.getZ());
            maxX = Math.max(maxX, cell.getX());
            maxZ = Math.max(maxZ, cell.getZ());
        }
        int y = figure.first().getY();
        AABB box = new AABB(minX, y, minZ, maxX + 1, y + 2.5, maxZ + 1);
        long now = level.getGameTime();
        BlockPos first = figure.first();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box, mob -> holdable(mob) && figure.covers(mob.blockPosition()));
        mobs.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(middle(figure))));
        int held = 0;
        for (Mob mob : mobs) {
            if (held >= CAPACITY || !pay(level, figure, purse, PERIOD)) {
                break;
            }
            held++;
            boolean fresh = !mob.hasData(ModAttachments.SHACKLE);
            mob.setData(ModAttachments.SHACKLE, new ShackleEvents.Shackle(first.asLong(), now + PERIOD + 6));
            if (fresh) {
                mob.setDeltaMovement(Vec3.ZERO);
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), ModSounds.SHACKLE_BIND.get(), SoundSource.BLOCKS, 0.5F,
                        0.9F + level.getRandom().nextFloat() * 0.2F);
                level.sendParticles(ModParticles.runeShard(ChalkSigilBlock.SHADOW), mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                        12, mob.getBbWidth() * 0.4, mob.getBbHeight() * 0.3, mob.getBbWidth() * 0.4, 0.02);
            }
            feedback(level, mob);
        }
        for (Player player : level.getEntitiesOfClass(Player.class, box, player -> !player.isCreative() && !player.isSpectator()
                && figure.covers(player.blockPosition()))) {
            long[] times = ROOTED.get(player.getUUID());
            if (times != null && now < times[1]) {
                if (now < times[0]) {
                    feedback(level, player);
                }
                continue;
            }
            if (pay(level, figure, purse, ROOT_COST)) {
                root(player, now);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SHACKLE_BIND.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
                feedback(level, player);
            }
        }
    }

    /** Any creature but a boss and a player's own tamed one. */
    public static boolean holdable(Mob mob) {
        return mob.isAlive() && !mob.isDeadOrDying() && !mob.getType().is(Tags.EntityTypes.BOSSES)
                && !(mob instanceof TamableAnimal tame && tame.isTame());
    }

    /** A ring of shadow at the held one's feet and shards of runes rising round it. */
    private static void feedback(ServerLevel level, LivingEntity held) {
        level.sendParticles(ModParticles.ring(ChalkSigilBlock.SHADOW), held.getX(), held.getY() + 0.05, held.getZ(), 0,
                Math.max(0.6, held.getBbWidth() * 0.9), 1, 0, 1);
        level.sendParticles(ModParticles.runeShard(ChalkSigilBlock.SHADOW), held.getX(), held.getY() + 0.2, held.getZ(), 2,
                held.getBbWidth() * 0.4, 0.1, held.getBbWidth() * 0.4, 0.01);
    }

    /** Pays {@code cost} creature-ticks: from what was paid before, then shadow from a jar, then the chalk's own wear. */
    private static boolean pay(ServerLevel level, Figure figure, Purse purse, int cost) {
        if (purse.budget >= cost) {
            purse.budget -= cost;
            return true;
        }
        for (EssenceTankBlockEntity jar : JarReach.around(level, figure.first())) {
            int taken = Math.min(DRAW_MB, jar.tank().contents().get(Essence.SHADOW));
            if (taken <= 0) {
                continue;
            }
            jar.tank().remove(EssenceMix.pure(Essence.SHADOW, taken));
            jar.setChanged();
            purse.budget += taken * HOLD_PER_MB - cost;
            Vec3 mouth = JarReach.mouth(jar.getBlockPos());
            com.pockyl.hextale.network.EssenceFxPayload.pour(level, mouth, Vec3.atBottomCenterOf(figure.first()).add(0, 0.1, 0),
                    Essence.SHADOW.color(), 16);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.3F, 0.7F);
            return true;
        }
        purse.wear += cost;
        if (purse.wear >= WEAR) {
            // The chalk is spent: one cell of the line smudges away, and the figure opens.
            List<BlockPos> cells = new ArrayList<>(figure.cells());
            BlockPos smudged = cells.get(level.getRandom().nextInt(cells.size()));
            level.removeBlock(smudged, false);
            level.sendParticles(ParticleTypes.WHITE_ASH, smudged.getX() + 0.5, smudged.getY() + 0.1, smudged.getZ() + 0.5, 12, 0.3, 0.05, 0.3, 0.02);
            level.playSound(null, smudged, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.4F, 1.4F);
            purse.wear = 0;
        }
        return true;
    }

    /** Roots a player for {@link #ROOT} ticks: the attributes reach their client, which stops moving them. */
    private static void root(Player player, long now) {
        ROOTED.put(player.getUUID(), new long[] {now + ROOT, now + ROOT + IMMUNE});
        toggleRoot(player, true);
        player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
        player.hurtMarked = true;
    }

    private static void toggleRoot(Player player, boolean on) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        for (AttributeInstance attribute : new AttributeInstance[] {speed, jump}) {
            if (attribute == null) {
                continue;
            }
            AttributeModifier modifier = attribute == speed ? ROOT_SPEED : ROOT_JUMP;
            if (on && !attribute.hasModifier(modifier.id())) {
                attribute.addTransientModifier(modifier);
            } else if (!on && attribute.hasModifier(modifier.id())) {
                attribute.removeModifier(modifier.id());
            }
        }
    }

    /** Lets rooted players go when their time is up. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || ROOTED.isEmpty()) {
            return;
        }
        long[] times = ROOTED.get(player.getUUID());
        if (times == null) {
            return;
        }
        long now = player.level().getGameTime();
        if (now >= times[0]) {
            toggleRoot(player, false);
        }
        if (now >= times[1]) {
            ROOTED.remove(player.getUUID());
        }
    }
}
