package com.pockyl.hextale.runework;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.StaffFxPayload;
import com.pockyl.hextale.network.StaffFxPayload.Kind;
import com.pockyl.hextale.registry.ModDamageTypes;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModSounds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * What the Rift Staff does ({@link RiftStaffItem} is the item).
 *
 * <p>Casts. A left click asks the server ({@code StaffCastPayload}); a cast needs the staff's rhythm to have come round
 * ({@link RiftStaffItem#CAST_TICKS}). Two casts fly as rift bolts - real flights ticked here, hitting the first creature
 * or block on the way - and the third is the lance: everything along the line at once, and a burst where it ends. The
 * run is kept on the staff ({@code staff_charge}) and shows in its model: one prong lit per cast, all three before the
 * lance. The hits are the wielder's blows as far as the sword's runes go ({@link ToolEffects#strength}).
 *
 * <p>Ward. Held up, the staff blocks like a shield ({@code blocks_attacks}) but only as long as the ward holds: it
 * takes {@link #WARD_MAX} (more with Reinforcement), each blow spends what it stopped, and the blow that is too much
 * gets the rest through and breaks it (the staff cannot be raised for {@link #BROKEN_TICKS}). Lowered and left alone
 * for {@link #CALM_TICKS}, it heals. Every blow it stops adds a step to the run, so a guard is paid back with a lance.
 */
public final class RiftStaff {
    /** Casts before the lance: the run counts 0, 1, 2 and the cast on 2 is the lance. */
    public static final int LANCE_AFTER = 2;

    public static final float BOLT_DAMAGE = 9;
    /** Blocks a bolt flies per tick, and how long it may fly. */
    public static final double BOLT_SPEED = 1.8;
    public static final int BOLT_TICKS = 24;
    /** How far round a creature's box a bolt still catches it. */
    private static final double BOLT_CATCH = 0.3;

    public static final float LANCE_DAMAGE = 20;
    public static final double LANCE_RANGE = 32;
    /** How far from the lance's line a creature is still caught. */
    private static final double LANCE_CATCH = 0.6;
    public static final float BURST_RADIUS = 2.5F;
    public static final float BURST_DAMAGE = 8;

    /** The ward stops blows from within this many degrees of where the wielder looks. */
    public static final float WARD_ANGLE = 80;
    /** Damage the ward holds before it breaks. */
    public static final float WARD_MAX = 40;
    /** Ticks a lowered ward waits after its last blow before it heals, and what it heals per tick then. */
    public static final int CALM_TICKS = 40;
    public static final float HEAL_PER_TICK = 0.25F;
    /** Ticks a broken ward cannot be raised. */
    public static final int BROKEN_TICKS = 100;
    /** A stopped blow below this adds no step to the run (a cactus prick is no blow). */
    private static final float CHARGING_BLOW = 2;
    /** How fast a shot the Mirror sends back flies. */
    private static final double RETURN_SPEED = 1.6;

    private static final Map<ServerLevel, List<Bolt>> BOLTS = new WeakHashMap<>();
    private static final Map<ServerLevel, List<Returned>> RETURNS = new WeakHashMap<>();
    private static final Map<UUID, Long> LAST_CAST = new HashMap<>();
    private static final Map<UUID, Long> LAST_WARD_HIT = new HashMap<>();
    private static int nextBolt;

    private RiftStaff() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RiftStaff::onLevelTick);
        NeoForge.EVENT_BUS.addListener(RiftStaff::onShieldBlock);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            LAST_CAST.remove(event.getEntity().getUUID());
            LAST_WARD_HIT.remove(event.getEntity().getUUID());
        });
    }

    public static boolean is(ItemStack stack) {
        return stack.getItem() instanceof RiftStaffItem;
    }

    // ---- Casts ------------------------------------------------------------------------------------------------------

    /** A left click with the staff: a bolt, or the lance when the run is full. False when it is not ready. */
    public static boolean cast(ServerPlayer player) {
        ItemStack staff = player.getMainHandItem();
        if (!is(staff) || player.isUsingItem() || player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        long now = level.getGameTime();
        Long last = LAST_CAST.get(player.getUUID());
        // A little slack for the client's clock: the indicator it waits for runs on its own ticks.
        if (last != null && now - last < RiftStaffItem.CAST_TICKS - 3 && now >= last) {
            return false;
        }
        LAST_CAST.put(player.getUUID(), now);
        int run = staff.getOrDefault(ModDataComponents.STAFF_CHARGE.get(), 0);
        Vec3 muzzle = muzzle(player);
        Vec3 aim = aim(level, player, run >= LANCE_AFTER ? LANCE_RANGE : BOLT_SPEED * BOLT_TICKS);
        if (run >= LANCE_AFTER) {
            lance(level, player, muzzle, aim);
            staff.set(ModDataComponents.STAFF_CHARGE.get(), 0);
            staff.hurtAndBreak(2, player, InteractionHand.MAIN_HAND.asEquipmentSlot());
        } else {
            bolt(level, player, muzzle, aim, run + 1);
            setRun(level, player, staff, run + 1);
            staff.hurtAndBreak(1, player, InteractionHand.MAIN_HAND.asEquipmentSlot());
        }
        return true;
    }

    /** Sets the run, with the sign that the lance is ready when it fills. */
    private static void setRun(ServerLevel level, LivingEntity holder, ItemStack staff, int run) {
        int before = staff.getOrDefault(ModDataComponents.STAFF_CHARGE.get(), 0);
        int now = Math.min(LANCE_AFTER, run);
        staff.set(ModDataComponents.STAFF_CHARGE.get(), now);
        if (now == LANCE_AFTER && before < LANCE_AFTER) {
            StaffFxPayload.send(level, Kind.READY, holder.getId(), muzzle(holder), Vec3.ZERO, 0);
            play(level, holder.position(), ModSounds.STAFF_READY.get(), 0.3F, 1.0F);
        }
    }

    /** Where casts leave the staff: its head, a little to the side of the holding hand and below the eyes. */
    public static Vec3 muzzle(LivingEntity holder) {
        Vec3 look = holder.getLookAngle();
        Vec3 side = look.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        double hand = holder.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        return holder.getEyePosition().add(look.scale(0.9)).add(side.scale(0.32 * hand)).add(0, -0.28, 0);
    }

    /** Where the wielder aims: the first block along the look within {@code range}, else that far. */
    private static Vec3 aim(ServerLevel level, Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getLookAngle().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? far : hit.getLocation();
    }

    private static void bolt(ServerLevel level, ServerPlayer player, Vec3 muzzle, Vec3 aim, int run) {
        Vec3 way = aim.subtract(muzzle);
        Vec3 step = (way.lengthSqr() < 1.0E-4 ? player.getLookAngle() : way.normalize()).scale(BOLT_SPEED);
        Bolt bolt = new Bolt(++nextBolt, player.getUUID(), muzzle, step, BOLT_TICKS);
        BOLTS.computeIfAbsent(level, key -> new ArrayList<>()).add(bolt);
        StaffFxPayload.send(level, Kind.BOLT, bolt.id, muzzle, step, run);
        play(level, muzzle, ModSounds.STAFF_CAST.get(), 0.35F, 0.95F + level.getRandom().nextFloat() * 0.1F);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        List<Returned> returns = RETURNS.remove(level);
        if (returns != null) {
            returns.forEach(returned -> returned.send(level));
        }
        List<Bolt> bolts = BOLTS.get(level);
        if (bolts == null || bolts.isEmpty()) {
            return;
        }
        for (Iterator<Bolt> it = bolts.iterator(); it.hasNext(); ) {
            if (fly(level, it.next())) {
                it.remove();
            }
        }
    }

    /** One tick of a bolt's flight; true when it has landed or run out. */
    private static boolean fly(ServerLevel level, Bolt bolt) {
        Player caster = level.getPlayerByUUID(bolt.caster);
        if (caster == null || !level.isLoaded(net.minecraft.core.BlockPos.containing(bolt.pos))) {
            return true;
        }
        Vec3 from = bolt.pos;
        Vec3 to = from.add(bolt.step);
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (wall.getType() != HitResult.Type.MISS) {
            to = wall.getLocation();
        }
        LivingEntity struck = null;
        double nearest = Double.MAX_VALUE;
        Vec3 struckAt = to;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1),
                e -> e != caster && e.isAlive() && !e.isSpectator() && !caster.isPassengerOfSameVehicle(e))) {
            var hit = entity.getBoundingBox().inflate(BOLT_CATCH).clip(from, to);
            if (hit.isPresent() && hit.get().distanceToSqr(from) < nearest) {
                nearest = hit.get().distanceToSqr(from);
                struck = entity;
                struckAt = hit.get();
            }
        }
        if (struck != null) {
            struck.hurtServer(level, source(level, caster), BOLT_DAMAGE);
            land(level, bolt, struckAt, true);
            return true;
        }
        if (wall.getType() != HitResult.Type.MISS) {
            land(level, bolt, to, false);
            return true;
        }
        bolt.pos = to;
        if (--bolt.left <= 0) {
            land(level, bolt, to, false);
            return true;
        }
        return false;
    }

    private static void land(ServerLevel level, Bolt bolt, Vec3 at, boolean creature) {
        StaffFxPayload.send(level, Kind.LAND, bolt.id, at, bolt.step, creature ? 1 : 0);
        play(level, at, ModSounds.STAFF_IMPACT.get(), creature ? 0.35F : 0.25F, creature ? 1.0F : 1.2F);
    }

    /** The third cast: everything along the line, at once, and a burst where the line ends on a block. */
    private static void lance(ServerLevel level, ServerPlayer player, Vec3 muzzle, Vec3 aim) {
        Vec3 way = aim.subtract(muzzle);
        Vec3 dir = way.lengthSqr() < 1.0E-4 ? player.getLookAngle() : way.normalize();
        Vec3 end = muzzle.add(dir.scale(Math.max(1, way.length())));
        BlockHitResult wall = level.clip(new ClipContext(muzzle, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean onBlock = wall.getType() != HitResult.Type.MISS;
        if (onBlock) {
            end = wall.getLocation();
        }
        List<LivingEntity> struck = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(muzzle, end).inflate(LANCE_CATCH + 1),
                e -> e != player && e.isAlive() && !e.isSpectator() && !player.isPassengerOfSameVehicle(e))) {
            if (entity.getBoundingBox().inflate(LANCE_CATCH).clip(muzzle, end).isPresent()
                    || entity.getBoundingBox().inflate(LANCE_CATCH).contains(muzzle)) {
                struck.add(entity);
            }
        }
        DamageSource source = source(level, player);
        for (LivingEntity entity : struck) {
            // The lance lands in full even on a creature a bolt has just stung.
            entity.damageCooldownTime = 0;
            entity.hurtServer(level, source, LANCE_DAMAGE);
            entity.setDeltaMovement(entity.getDeltaMovement().add(dir.x * 0.5, 0.15, dir.z * 0.5));
            entity.needsSync = true;
        }
        float burst = 0;
        if (onBlock) {
            burst = BURST_RADIUS;
            Vec3 centre = end;
            List<LivingEntity> around = level.getEntitiesOfClass(LivingEntity.class, new AABB(centre, centre).inflate(BURST_RADIUS),
                    e -> !struck.contains(e) && ToolEffects.mayStrike(e, player) && e.distanceToSqr(centre) <= BURST_RADIUS * BURST_RADIUS);
            ToolEffects.quietly(() -> {
                for (LivingEntity entity : around) {
                    entity.hurtServer(level, source, BURST_DAMAGE);
                }
            });
        }
        StaffFxPayload.send(level, Kind.LANCE, player.getId(), muzzle, end, burst);
        play(level, muzzle, ModSounds.STAFF_LANCE.get(), 0.45F, 1.0F);
        if (burst > 0) {
            play(level, end, ModSounds.STAFF_IMPACT.get(), 0.45F, 0.7F);
        }
    }

    /** The staff's hit: the wielder's, so the sword's runes on it answer (see {@link ToolEffects#weapon}). */
    public static DamageSource source(ServerLevel level, Entity caster) {
        return ModDamageTypes.riftStaff(level, caster);
    }

    // ---- Ward -------------------------------------------------------------------------------------------------------

    public static float wardMax(ItemStack staff) {
        int more = ToolEffects.strength(staff, ToolRune.Tool.STAFF, ToolRune.REINFORCEMENT);
        return WARD_MAX * (1 + (more > 0 ? HolderRune.at(ToolRune.WARD_MORE, more) : 0));
    }

    public static float wardLeft(ItemStack staff) {
        return Math.max(0, wardMax(staff) - staff.getOrDefault(ModDataComponents.STAFF_WARD.get(), 0F));
    }

    /** The ward was raised: its sound. */
    static void raised(ServerLevel level, LivingEntity holder) {
        play(level, holder.position(), ModSounds.STAFF_WARD_UP.get(), 0.3F, 1.0F);
    }

    /** A lowered ward heals once it has been left alone long enough. */
    static void rest(ServerLevel level, LivingEntity holder, ItemStack staff) {
        float spent = staff.getOrDefault(ModDataComponents.STAFF_WARD.get(), 0F);
        if (spent <= 0 || holder.isUsingItem() && holder.getUseItem() == staff) {
            return;
        }
        long now = level.getGameTime();
        // Heal in steps of five ticks: the stack syncs to the client on every change.
        if (now % 5 != 0) {
            return;
        }
        Long hit = LAST_WARD_HIT.get(holder.getUUID());
        if (hit != null && now - hit < CALM_TICKS && now >= hit) {
            return;
        }
        float left = spent - HEAL_PER_TICK * 5;
        if (left <= 0) {
            staff.remove(ModDataComponents.STAFF_WARD.get());
        } else {
            staff.set(ModDataComponents.STAFF_WARD.get(), left);
        }
    }

    private static void onShieldBlock(LivingShieldBlockEvent event) {
        LivingEntity holder = event.getEntity();
        ItemStack staff = holder.getUseItem();
        if (!is(staff) || !event.getBlocked() || !(holder.level() instanceof ServerLevel level)) {
            return;
        }
        float blocked = event.getBlockedDamage();
        if (blocked <= 0) {
            return;
        }
        float left = wardLeft(staff);
        LAST_WARD_HIT.put(holder.getUUID(), level.getGameTime());
        DamageSource source = event.getDamageSource();
        Vec3 from = source.getSourcePosition();
        Vec3 face = wardPoint(holder, from);
        if (blocked < left) {
            staff.set(ModDataComponents.STAFF_WARD.get(), staff.getOrDefault(ModDataComponents.STAFF_WARD.get(), 0F) + blocked);
            StaffFxPayload.send(level, Kind.WARD_HIT, holder.getId(), face, Vec3.ZERO, wardLeft(staff) / wardMax(staff));
            if (blocked >= CHARGING_BLOW) {
                setRun(level, holder, staff, staff.getOrDefault(ModDataComponents.STAFF_CHARGE.get(), 0) + 1);
            }
            mirror(level, holder, staff, source, blocked, face);
            return;
        }
        // Too much: the ward takes what it still held, the rest gets through, and it breaks.
        event.setBlockedDamage(left);
        staff.set(ModDataComponents.STAFF_WARD.get(), wardMax(staff));
        StaffFxPayload.send(level, Kind.WARD_BREAK, holder.getId(), face, Vec3.ZERO, 0);
        play(level, holder.position(), ModSounds.STAFF_WARD_BREAK.get(), 0.45F, 1.0F);
        if (holder instanceof Player player) {
            player.getCooldowns().addCooldown(staff, BROKEN_TICKS);
        }
        holder.stopUsingItem();
        if (left > 0) {
            mirror(level, holder, staff, source, left, face);
        }
    }

    /** The spot on the ward a blow from {@code from} meets (the ward's middle when it came from nowhere). */
    public static Vec3 wardPoint(LivingEntity holder, @Nullable Vec3 from) {
        Vec3 eye = holder.getEyePosition();
        Vec3 centre = eye.add(holder.getLookAngle().scale(WARD_DISTANCE));
        if (from == null) {
            return centre;
        }
        Vec3 toward = from.subtract(eye);
        if (toward.lengthSqr() < 1.0E-4) {
            return centre;
        }
        Vec3 point = eye.add(toward.normalize().scale(WARD_DISTANCE));
        // Keep it on the ward's face.
        Vec3 off = point.subtract(centre);
        double reach = WARD_RADIUS;
        return off.lengthSqr() > reach * reach ? centre.add(off.normalize().scale(reach)) : point;
    }

    /** How far before the eyes the ward stands, and how far it reaches from its middle. */
    public static final double WARD_DISTANCE = 1.15;
    public static final double WARD_RADIUS = 0.95;

    /** Mirror: the stopped blow stings the striker back, a stopped shot flies back at the shooter. */
    private static void mirror(ServerLevel level, LivingEntity holder, ItemStack staff, DamageSource source, float blocked, Vec3 face) {
        int stars = ToolEffects.strength(staff, ToolRune.Tool.STAFF, ToolRune.MIRROR);
        if (stars <= 0) {
            return;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile shot && source.getEntity() instanceof LivingEntity shooter && shooter != holder) {
            RETURNS.computeIfAbsent(level, key -> new ArrayList<>()).add(new Returned(shot, shooter, holder));
            ToolEffects.puff(level, face, ToolRune.MIRROR.rune(), 6, 0.2);
            return;
        }
        if (direct instanceof LivingEntity striker && striker != holder && striker.isAlive()) {
            float back = blocked * HolderRune.at(ToolRune.MIRROR_SHARE, stars);
            ToolEffects.quietly(() -> striker.hurtServer(level, source(level, holder), back));
            StaffFxPayload.send(level, Kind.MIRROR, holder.getId(), face, striker.position().add(0, striker.getBbHeight() * 0.6, 0), 0);
            ToolEffects.puff(level, face, ToolRune.MIRROR.rune(), 6, 0.2);
        }
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** A bolt in flight. */
    private static final class Bolt {
        final int id;
        final UUID caster;
        final Vec3 step;
        Vec3 pos;
        int left;

        Bolt(int id, UUID caster, Vec3 pos, Vec3 step, int left) {
            this.id = id;
            this.caster = caster;
            this.pos = pos;
            this.step = step;
            this.left = left;
        }
    }

    /** A stopped shot to be sent back at its shooter after the game has bounced it off the ward. */
    private record Returned(Projectile shot, LivingEntity shooter, LivingEntity holder) {
        void send(ServerLevel level) {
            if (!shot.isAlive() || !shooter.isAlive() || shot.level() != level) {
                return;
            }
            Vec3 way = shooter.position().add(0, shooter.getBbHeight() * 0.6, 0).subtract(shot.position());
            if (way.lengthSqr() < 1.0E-4) {
                return;
            }
            shot.setOwner(holder);
            shot.setDeltaMovement(way.normalize().scale(RETURN_SPEED));
            shot.needsSync = true;
            StaffFxPayload.send(level, Kind.MIRROR, holder.getId(), shot.position(), shooter.position().add(0, shooter.getBbHeight() * 0.6, 0), 1);
        }
    }
}
