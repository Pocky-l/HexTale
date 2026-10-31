package com.pockyl.hextale.runework;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModSounds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * The weapon runes that do more than a blow: the Shockwave sent through the ground by a charged swing at it, the Blink
 * through the shadow to a creature's back, the Detonator's burst, the Whirlpool dragging creatures in and the
 * Whirlwind's sweeping circle. The server decides every hit; the clients draw each moment ({@link WeaponFxPayload}).
 * Hits made here start no rune procs of their own ({@link ToolEffects#quietly}), so runes never feed each other.
 */
public final class WeaponRunes {
    private static final int NO_GROUND = Integer.MIN_VALUE;
    /** Ticks between two blows that still count as one run for the Whirlwind. */
    private static final int COMBO_GAP = 40;
    private static final int COMBO_HITS = 3;
    /** A Whirlpool pulls this hard towards its middle, and turns this hard round it (blocks per tick). */
    private static final double PULL = 0.11;
    private static final double SWIRL = 0.07;

    private static final Map<ServerLevel, List<Wave>> WAVES = new WeakHashMap<>();
    private static final Map<ServerLevel, List<Vortex>> VORTICES = new WeakHashMap<>();
    /** Per player: blows in the current run and the tick of the last one. */
    private static final Map<UUID, long[]> COMBOS = new java.util.HashMap<>();
    /** Set while a Detonator burst hurts the creatures round it, so a creature it kills does not burst again. */
    private static final ThreadLocal<Boolean> BURSTING = ThreadLocal.withInitial(() -> false);

    private WeaponRunes() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WeaponRunes::onLevelTick);
        NeoForge.EVENT_BUS.addListener(WeaponRunes::onDamaged);
        NeoForge.EVENT_BUS.addListener(WeaponRunes::onDeath);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> COMBOS.remove(event.getEntity().getUUID()));
    }

    /** The strength of a weapon rune on the stack in the player's main hand, whatever weapon it is (0 = none). */
    static int strength(ItemStack stack, ToolRune rune) {
        RuneHolder holder = RuneHolder.of(stack);
        ToolRune.Tool tool = holder == null ? null : ToolEffects.tool(holder);
        return tool == null ? 0 : ToolEffects.strength(stack, tool, rune);
    }

    /** Whether the player can work a Shockwave now: the rune on the weapon in hand, rested, the swing charged. */
    public static boolean shockwaveReady(Player player) {
        ItemStack weapon = player.getMainHandItem();
        return strength(weapon, ToolRune.SHOCKWAVE) > 0 && !player.getCooldowns().isOnCooldown(weapon.getItem())
                && player.getAttackStrengthScale(0.5F) >= 0.85F && !player.isSpectator();
    }

    // ---- Shockwave --------------------------------------------------------------------------------------------------

    /** A wave on its way: the middles of its steps, who it has hit, how far it got. */
    private static final class Wave {
        final UUID owner;
        final List<Vec3> steps;
        final Vec3 dir;
        final int halfWidth;
        final float damage;
        final double toss;
        final Set<LivingEntity> hit = new HashSet<>();
        final List<BlockPos> ground;
        int age;

        Wave(UUID owner, List<Vec3> steps, List<BlockPos> ground, Vec3 dir, int halfWidth, float damage, double toss) {
            this.owner = owner;
            this.steps = steps;
            this.ground = ground;
            this.dir = dir;
            this.halfWidth = halfWidth;
            this.damage = damage;
            this.toss = toss;
        }
    }

    /**
     * A charged swing at the ground with the Shockwave rune: the wave runs off where the player looks, one block a tick,
     * along the ground (up and down a block, never over a gap or through a wall). Each block on its way heaves; every
     * creature it passes is hurt and tossed up. The weapon rests afterwards.
     */
    public static void shockwave(ServerPlayer player) {
        if (!shockwaveReady(player)) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        ItemStack weapon = player.getMainHandItem();
        int stars = strength(weapon, ToolRune.SHOCKWAVE);
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0, player.getYRot()) : dir.normalize();
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        int length = HolderRune.at(ToolRune.WAVE_LENGTH, stars);
        int half = HolderRune.at(ToolRune.WAVE_HALF_WIDTH, stars);
        int y = ground(level, Mth.floor(player.getX()), Mth.floor(player.getZ()), Mth.floor(player.getY() - 0.2));
        if (y == NO_GROUND) {
            y = Mth.floor(player.getY() - 0.2);
        }
        List<Vec3> steps = new ArrayList<>();
        List<BlockPos> blocks = new ArrayList<>();
        List<Integer> stepOf = new ArrayList<>();
        Set<BlockPos> seen = new LinkedHashSet<>();
        for (int i = 1; i <= length; i++) {
            Vec3 c = player.position().add(dir.scale(i + 0.2));
            int gy = ground(level, Mth.floor(c.x), Mth.floor(c.z), y);
            if (gy == NO_GROUND) {
                break;
            }
            y = gy;
            steps.add(new Vec3(c.x, gy + 1, c.z));
            for (int k = 0; k <= 2 * half; k++) {
                // The middle first, then the sides.
                int offset = k == 0 ? 0 : (k % 2 == 1 ? (k + 1) / 2 : -k / 2);
                Vec3 at = c.add(side.scale(offset));
                int sy = offset == 0 ? gy : ground(level, Mth.floor(at.x), Mth.floor(at.z), gy);
                BlockPos pos = new BlockPos(Mth.floor(at.x), sy, Mth.floor(at.z));
                if (sy != NO_GROUND && seen.add(pos)) {
                    blocks.add(pos);
                    stepOf.add(i - 1);
                }
            }
        }
        if (steps.isEmpty()) {
            level.playSound(null, player.blockPosition(), ModSounds.DEVICE_REFUSE.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
            return;
        }
        player.resetAttackStrengthTicker();
        player.getCooldowns().addCooldown(weapon.getItem(), HolderRune.at(ToolRune.WAVE_REST, stars));
        weapon.hurtAndBreak(2, player, EquipmentSlot.MAINHAND);
        WAVES.computeIfAbsent(level, key -> new ArrayList<>()).add(new Wave(player.getUUID(), steps, blocks, dir, half,
                HolderRune.at(ToolRune.WAVE_DAMAGE, stars), 0.42 + 0.08 * stars));
        Vec3 impact = player.position().add(dir.scale(0.8));
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.WAVE, impact, steps.getLast(), blocks, stepOf,
                ToolRune.SHOCKWAVE.rune().color(), stars);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 0.3F, 0.7F);
        level.playSound(null, impact.x, impact.y, impact.z, ModSounds.RUNE_CRUMBLE.get(), SoundSource.PLAYERS, 0.3F, 0.7F);
    }

    /**
     * The top of the ground in a column near {@code nearY} (a block up or down, two down): a block with a sturdy top
     * and room above it; {@link #NO_GROUND} over a gap or into a wall.
     */
    private static int ground(ServerLevel level, int x, int z, int nearY) {
        for (int dy : new int[]{0, 1, -1, -2}) {
            BlockPos pos = new BlockPos(x, nearY + dy, z);
            if (!level.isLoaded(pos)) {
                return NO_GROUND;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isFaceSturdy(level, pos, Direction.UP) && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return pos.getY();
            }
        }
        return NO_GROUND;
    }

    private static void tickWave(ServerLevel level, Wave wave, Player player) {
        int step = wave.age;
        Vec3 c = wave.steps.get(step);
        double reach = 0.9 + wave.halfWidth;
        AABB box = new AABB(c.x - reach, c.y - 0.6, c.z - reach, c.x + reach, c.y + 2.2, c.z + reach);
        List<LivingEntity> struck = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> ToolEffects.mayStrike(e, player) && !wave.hit.contains(e));
        ToolEffects.quietly(() -> {
            for (LivingEntity target : struck) {
                wave.hit.add(target);
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().playerAttack(player), wave.damage);
                target.setDeltaMovement(target.getDeltaMovement().multiply(0.3, 0, 0.3)
                        .add(wave.dir.x * 0.35, wave.toss, wave.dir.z * 0.35));
                target.hurtMarked = true;
            }
        });
        if (step % 3 == 0) {
            BlockPos under = BlockPos.containing(c.x, c.y - 1, c.z);
            BlockState state = level.getBlockState(under);
            level.playSound(null, under, state.getSoundType().getBreakSound(), SoundSource.PLAYERS, 0.18F, 0.6F + level.getRandom().nextFloat() * 0.2F);
        }
    }

    // ---- Blink ------------------------------------------------------------------------------------------------------

    /**
     * Right click with the Blink rune: the creature in sight (within the rune's reach, not behind a wall) is struck
     * from its back - the player steps through the shadow behind it, turned to it, and lands a blow stronger by the
     * rune's bonus. Returns false (nothing happens) when no creature is in sight or there is no room behind it.
     */
    public static boolean blink(ServerLevel level, Player player, ItemStack weapon) {
        int stars = strength(weapon, ToolRune.BLINK);
        if (stars <= 0) {
            return false;
        }
        double range = HolderRune.at(ToolRune.BLINK_RANGE, stars);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        LivingEntity target = null;
        double best = Double.MAX_VALUE;
        AABB around = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, around, e -> ToolEffects.mayStrike(e, player))) {
            var clip = candidate.getBoundingBox().inflate(0.35).clip(eye, end);
            if (clip.isPresent() && clip.get().distanceToSqr(eye) < best) {
                best = clip.get().distanceToSqr(eye);
                target = candidate;
            }
        }
        if (target == null) {
            return false;
        }
        Vec3 behind = target.position().subtract(player.position()).multiply(1, 0, 1);
        behind = behind.lengthSqr() < 1.0E-4 ? new Vec3(look.x, 0, look.z).normalize() : behind.normalize();
        Vec3 dest = null;
        double gap = target.getBbWidth() / 2 + player.getBbWidth() / 2 + 0.35;
        for (Vec3 way : new Vec3[]{behind, behind.yRot(Mth.HALF_PI), behind.yRot(-Mth.HALF_PI), behind.reverse()}) {
            Vec3 spot = target.position().add(way.scale(gap));
            if (level.noCollision(player, player.getBoundingBox().move(spot.subtract(player.position())))) {
                dest = spot;
                break;
            }
        }
        if (dest == null) {
            return false;
        }
        Vec3 from = player.position();
        Vec3 face = target.getEyePosition().subtract(dest.add(0, player.getEyeHeight(), 0));
        float yaw = (float) (Mth.atan2(face.z, face.x) * Mth.RAD_TO_DEG) - 90;
        float pitch = (float) -(Mth.atan2(face.y, face.horizontalDistance()) * Mth.RAD_TO_DEG);
        player.teleportTo(level, dest.x, dest.y, dest.z, Set.of(), yaw, pitch);
        player.fallDistance = 0;
        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (1 + HolderRune.at(ToolRune.BLINK_BONUS, stars));
        target.invulnerableTime = 0;
        // The blow from the back is a real blow of the weapon: the sword's other runes may answer it.
        target.hurt(level.damageSources().playerAttack(player), damage);
        target.setDeltaMovement(target.getDeltaMovement().add(-behind.x * 0.45, 0.25, -behind.z * 0.45));
        target.hurtMarked = true;
        player.swing(InteractionHand.MAIN_HAND, true);
        player.resetAttackStrengthTicker();
        player.getCooldowns().addCooldown(weapon.getItem(), HolderRune.at(ToolRune.BLINK_REST, stars));
        weapon.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.BLINK, from, dest, ToolRune.BLINK.rune().color(), player.getBbHeight());
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.2F, 0.55F);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.35F, 0.8F);
        level.playSound(null, dest.x, dest.y, dest.z, ModSounds.RUNE_PROC.get(), SoundSource.PLAYERS, 0.25F, 0.7F);
        return true;
    }

    // ---- Detonator, Whirlpool, Whirlwind --------------------------------------------------------------------------

    /** A Whirlpool turning at a spot: who opened it, its reach and how long it has left. */
    private static final class Vortex {
        final UUID owner;
        final Vec3 centre;
        final float radius;
        int left;

        Vortex(UUID owner, Vec3 centre, float radius, int left) {
            this.owner = owner;
            this.centre = centre;
            this.radius = radius;
            this.left = left;
        }
    }

    private static void onDamaged(LivingDamageEvent.Post event) {
        if (ToolEffects.quiet() || !(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack weapon = ToolEffects.weapon(event.getSource());
        if (weapon.isEmpty()) {
            return;
        }
        LivingEntity target = event.getEntity();
        int whirlpool = strength(weapon, ToolRune.WHIRLPOOL);
        if (whirlpool > 0 && level.getRandom().nextFloat() < HolderRune.at(ToolRune.VORTEX_CHANCE, whirlpool)
                && VORTICES.getOrDefault(level, List.of()).stream().noneMatch(v -> v.centre.distanceToSqr(target.position()) < 9)) {
            float radius = HolderRune.at(ToolRune.VORTEX_RADIUS, whirlpool);
            int ticks = HolderRune.at(ToolRune.VORTEX_TICKS, whirlpool);
            Vec3 centre = target.position();
            VORTICES.computeIfAbsent(level, key -> new ArrayList<>()).add(new Vortex(player.getUUID(), centre, radius, ticks));
            WeaponFxPayload.send(level, WeaponFxPayload.Kind.VORTEX, centre, new Vec3(ticks, 0, 0), ToolRune.WHIRLPOOL.rune().color(), radius);
            level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 0.3F, 0.6F);
            level.playSound(null, centre.x, centre.y, centre.z, ModSounds.RUNE_GUST.get(), SoundSource.PLAYERS, 0.25F, 0.6F);
        }
        int whirlwind = strength(weapon, ToolRune.WHIRLWIND);
        if (whirlwind > 0) {
            long now = level.getGameTime();
            long[] combo = COMBOS.computeIfAbsent(player.getUUID(), key -> new long[]{0, Long.MIN_VALUE / 2});
            combo[0] = now - combo[1] <= COMBO_GAP ? combo[0] + 1 : 1;
            combo[1] = now;
            if (combo[0] >= COMBO_HITS) {
                combo[0] = 0;
                sweep(level, player, target, whirlwind);
            }
        }
    }

    /** Whirlwind: the third blow in a row sweeps all round the player in a circle of light. */
    private static void sweep(ServerLevel level, Player player, LivingEntity struck, int stars) {
        float radius = HolderRune.at(ToolRune.SWEEP_RADIUS, stars);
        float damage = HolderRune.at(ToolRune.SWEEP_DAMAGE, stars);
        Vec3 centre = player.position();
        List<LivingEntity> around = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 1, radius),
                e -> e != struck && ToolEffects.mayStrike(e, player) && e.distanceToSqr(player) <= radius * radius);
        ToolEffects.quietly(() -> {
            for (LivingEntity target : around) {
                target.hurt(level.damageSources().playerAttack(player), damage);
                Vec3 away = target.position().subtract(centre).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                target.setDeltaMovement(target.getDeltaMovement().add(away.x * 0.55, 0.22, away.z * 0.55));
                target.hurtMarked = true;
            }
        });
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.SWEEP, centre.add(0, player.getBbHeight() * 0.45, 0),
                new Vec3(player.getYRot(), 0, 0), ToolRune.WHIRLWIND.rune().color(), radius);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4F, 0.75F);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.RUNE_GUST.get(), SoundSource.PLAYERS, 0.25F, 0.8F);
    }

    private static void onDeath(LivingDeathEvent event) {
        if (BURSTING.get() || !(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        int stars = strength(player.getMainHandItem(), ToolRune.DETONATOR);
        if (stars <= 0) {
            return;
        }
        LivingEntity dead = event.getEntity();
        Vec3 centre = dead.position().add(0, dead.getBbHeight() * 0.5, 0);
        float radius = HolderRune.at(ToolRune.BLAST_RADIUS, stars);
        float damage = HolderRune.at(ToolRune.BLAST_DAMAGE, stars);
        List<LivingEntity> around = level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(radius),
                e -> e != dead && ToolEffects.mayStrike(e, player) && e.distanceToSqr(centre) <= radius * radius);
        BURSTING.set(true);
        try {
            ToolEffects.quietly(() -> {
                for (LivingEntity target : around) {
                    // Full at the heart of the burst, half at its edge.
                    float share = 1 - 0.5F * (float) (Math.sqrt(target.distanceToSqr(centre)) / radius);
                    target.invulnerableTime = 0;
                    target.hurt(level.damageSources().playerAttack(player), damage * share);
                    Vec3 away = target.position().subtract(dead.position()).multiply(1, 0, 1);
                    away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                    target.setDeltaMovement(target.getDeltaMovement().add(away.x * 0.6, 0.3, away.z * 0.6));
                    target.hurtMarked = true;
                }
            });
        } finally {
            BURSTING.set(false);
        }
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.BURST, centre, centre, ToolRune.DETONATOR.rune().color(), radius);
        level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.18F, 1.4F);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.RUNE_KINDLE.get(), SoundSource.PLAYERS, 0.35F, 0.7F);
    }

    // ---- Ticking ----------------------------------------------------------------------------------------------------

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Wave> waves = WAVES.get(level);
        if (waves != null && !waves.isEmpty()) {
            for (Iterator<Wave> it = waves.iterator(); it.hasNext(); ) {
                Wave wave = it.next();
                Player player = level.getPlayerByUUID(wave.owner);
                if (player == null || wave.age >= wave.steps.size()) {
                    it.remove();
                    continue;
                }
                tickWave(level, wave, player);
                wave.age++;
            }
        }
        List<Vortex> vortices = VORTICES.get(level);
        if (vortices != null && !vortices.isEmpty()) {
            for (Iterator<Vortex> it = vortices.iterator(); it.hasNext(); ) {
                Vortex vortex = it.next();
                Player player = level.getPlayerByUUID(vortex.owner);
                if (player == null || --vortex.left < 0) {
                    it.remove();
                    continue;
                }
                pull(level, vortex, player);
            }
        }
    }

    /** Whirlpool: creatures round the spot are dragged in and turned about it, and slowed while they are. */
    private static void pull(ServerLevel level, Vortex vortex, Player player) {
        Vec3 c = vortex.centre;
        float r = vortex.radius;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 2, r),
                e -> ToolEffects.mayStrike(e, player))) {
            Vec3 in = c.subtract(entity.position()).multiply(1, 0, 1);
            double distance = in.length();
            if (distance > r || distance < 0.4) {
                continue;
            }
            Vec3 toward = in.scale(1 / distance);
            Vec3 round = new Vec3(-toward.z, 0, toward.x);
            double strength = 0.5 + 0.5 * (1 - distance / r);
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.6, 1, 0.6)
                    .add(toward.scale(PULL * strength)).add(round.scale(SWIRL * strength)));
            entity.hurtMarked = true;
            if (vortex.left % 10 == 0) {
                ToolEffects.keep(entity, MobEffects.MOVEMENT_SLOWDOWN, 30, 1);
            }
        }
    }

    /** For tests: whether a wave or a whirlpool is still under way in the level. */
    public static boolean busy(ServerLevel level) {
        return !WAVES.getOrDefault(level, List.of()).isEmpty() || !VORTICES.getOrDefault(level, List.of()).isEmpty();
    }
}
