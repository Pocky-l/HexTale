package com.pockyl.hextale.entity;

import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.BiterBlastPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * A thrown Biter Grenade. The first creature it hits gets its jaw: it rides there, chattering faster and faster, and after
 * {@link #FUSE} ticks it bursts. A throw that hits nobody lands and hunts: it sits a moment working its jaw, then springs
 * at the nearest enemy it sees, up to {@link #HOPS} times. Touching a creature in a hop latches as usual; after the last
 * landing it swells and bursts where it is. With nothing to hunt it sits out the time the hops would have taken. The blast
 * only hurts: blocks stay whole and the thrower's pets are spared (the thrower is not). If the bitten creature dies first
 * the grenade falls off still armed and bursts where it lies.
 */
public class BiterGrenade extends ThrowableItemProjectile {
    /** Ticks between the bite and the blast. */
    public static final int FUSE = 40;
    public static final float RADIUS = 3.5F;
    /** Damage at the grenade, and at the very edge of the blast. */
    public static final float BLAST = 12.0F;
    public static final float EDGE = 4.0F;
    /** Hops a grenade makes after landing a throw that hit nobody. */
    public static final int HOPS = 3;
    /** Ticks it works its jaw on the ground before a hop; {@code biter_grenade_chatter} is as long. */
    public static final int WINDUP = 10;
    /** Ticks of the swell after the last landing. */
    public static final int SWELL = 10;
    /** Ticks it looks round in place of a hop it has no target for: about what the hop would have taken. */
    private static final int LOOK_ROUND = 24;
    /** Ticks an unarmed grenade lasts at the most, whatever it is doing: nothing falls or waits for ever. */
    private static final int LIFETIME = 160;
    /** How far it sees a target, blocks. */
    private static final double SIGHT = 10;
    /** Degrees a tick it turns while it looks round. */
    private static final float LOOK_TURN = 14;
    private static final float PUSH = 0.9F;
    private static final int GREEN = 0x8CE050;
    /** Sounds reach 16 blocks per 1.0 of volume, and past 1.0 volume only adds reach. */
    private static final float BLAST_VOLUME = 3.5F;
    private static final float BITE_VOLUME = 1.5F;
    /** Beyond this the thrower would not hear the bite: they get it anyway, as word of a hit. */
    private static final double BITE_HEARD = 20;
    /** From the ground up to the entity's place when it sits: the model's underside then touches the ground. */
    private static final double SIT_HEIGHT = 0.2;
    /**
     * The fuse values the jaw snaps shut on. The ticks of {@code biter_grenade_fuse} in hextale_sounds.py fall on the same
     * beats, so change both together.
     */
    private static final int[] BEATS = {32, 26, 21, 17, 14, 12, 10, 8, 6, 4, 2};
    private static final EntityDataAccessor<Integer> FUSE_LEFT = SynchedEntityData.defineId(BiterGrenade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(BiterGrenade.class, EntityDataSerializers.INT);
    /** Where it holds on, from the target's feet, in the target's own turn (it stays on the same flank as the body turns). */
    private static final EntityDataAccessor<Vector3fc> HOLD = SynchedEntityData.defineId(BiterGrenade.class, EntityDataSerializers.VECTOR3);
    /** Ticks left on the ground before it acts, or -1 in the air. */
    private static final EntityDataAccessor<Integer> SITTING = SynchedEntityData.defineId(BiterGrenade.class, EntityDataSerializers.INT);
    /** Where its mouth points while it sits. */
    private static final EntityDataAccessor<Vector3fc> LOOK = SynchedEntityData.defineId(BiterGrenade.class, EntityDataSerializers.VECTOR3);

    private int hops;
    private int age;
    private @Nullable LivingEntity prey;
    private float lookAngle;
    /** Client: ticks on the ground so far, and the turn drawn, eased towards {@link #LOOK}. */
    private int sat;
    private Vec3 shownLook = new Vec3(0, 0, -1);
    private Vec3 oldLook = shownLook;

    public BiterGrenade(EntityType<? extends BiterGrenade> type, Level level) {
        super(type, level);
    }

    public BiterGrenade(Level level, LivingEntity thrower, ItemStack stack) {
        super(ModEntities.BITER_GRENADE.get(), thrower, level, stack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUSE_LEFT, -1);
        builder.define(TARGET, -1);
        builder.define(HOLD, new Vector3f());
        builder.define(SITTING, -1);
        builder.define(LOOK, new Vector3f(0, 0, -1));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.BITER_GRENADE.get();
    }

    /** Heavier than a snowball: it arcs like a Chem Vial. */
    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    /** Ticks left to the blast, or -1 while it is not armed. */
    public int fuseLeft() {
        return entityData.get(FUSE_LEFT);
    }

    /** Ticks left on the ground before it acts, or -1 in the air. */
    public int sitting() {
        return entityData.get(SITTING);
    }

    /** Hops made so far. */
    public int hops() {
        return hops;
    }

    /** Client: ticks since it landed, for the squash the renderer draws. */
    public int sat() {
        return sat;
    }

    /** Client: where its mouth points on this frame while it sits. */
    public Vec3 shownLook(float partialTick) {
        return oldLook.lerp(shownLook, partialTick);
    }

    /** Whether the jaw snaps shut at this value of the fuse. */
    public static boolean beat(int left) {
        for (int beat : BEATS) {
            if (beat == left) {
                return true;
            }
        }
        return false;
    }

    /** The creature it is holding on to, or null. */
    @Nullable
    public LivingEntity target() {
        int id = entityData.get(TARGET);
        return id >= 0 && level().getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    /** Where it sits on {@code target} at this moment of the frame (the renderer draws it there, in step with the body). */
    public Vec3 heldAt(LivingEntity target, float partialTick) {
        Vector3fc hold = entityData.get(HOLD);
        float turn = Mth.rotLerp(partialTick, target.yBodyRotO, target.yBodyRot) * Mth.DEG_TO_RAD;
        return target.getPosition(partialTick).add(new Vec3(hold.x(), hold.y(), hold.z()).yRot(-turn));
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel level && fuseLeft() < 0) {
            age++;
            if (level.getFluidState(blockPosition()).is(FluidTags.LAVA)) {
                burst(level, null);
                return;
            }
            if (age >= LIFETIME) {
                swell();
            }
        }
        int left = fuseLeft();
        if (left < 0) {
            int sitting = sitting();
            if (sitting >= 0) {
                sit(sitting);
                return;
            }
            sat = 0;
            super.tick();
            if (level().isClientSide() && tickCount % 2 == 0) {
                level().addParticle(ModParticles.vapour(ModParticles.SMOKE), getX(), getY(), getZ(), 0, 0.01, 0);
            }
            return;
        }
        LivingEntity target = target();
        if (target != null) {
            setPos(heldAt(target, 1));
        } else {
            fall();
        }
        if (level().isClientSide()) {
            fuseEffects(left);
            return;
        }
        if (left == FUSE) {
            // One sound as long as the fuse: its ticks are the jaw's beats. It travels with the grenade.
            level().playSound(null, this, ModSounds.BITER_GRENADE_FUSE.get(), SoundSource.NEUTRAL, 0.8F, 1.0F);
        }
        if (left == 0 && level() instanceof ServerLevel level) {
            burst(level, target);
            return;
        }
        entityData.set(FUSE_LEFT, left - 1);
    }

    private void burst(ServerLevel level, @Nullable LivingEntity held) {
        blast(level, position(), this, getOwner(), held);
        discard();
    }

    /** Out of hops: it swells where it is, and the armed tick ends it. */
    private void swell() {
        entityData.set(SITTING, -1);
        entityData.set(FUSE_LEFT, SWELL);
        level().playSound(null, this, ModSounds.BITER_GRENADE_CHATTER.get(), SoundSource.NEUTRAL, 0.8F, 1.3F);
    }

    /** On the ground: it turns to what it hunts, or looks round, and acts when its time is up. */
    private void sit(int sitting) {
        setDeltaMovement(Vec3.ZERO);
        if (level().isClientSide()) {
            sat++;
            Vector3fc look = entityData.get(LOOK);
            oldLook = shownLook;
            shownLook = shownLook.lerp(new Vec3(look.x(), look.y(), look.z()), 0.35);
            if (sitting <= WINDUP && sitting % 3 == 0) {
                level().addParticle(ModParticles.glint(GREEN), getX(), getY() + 0.15, getZ(), 0, 0.02, 0);
            }
            return;
        }
        if (level().noCollision(this, getBoundingBox().move(0, -SIT_HEIGHT - 0.1, 0))) {
            // The ground went from under it: it falls on and the next landing counts.
            entityData.set(SITTING, -1);
            return;
        }
        if (sitting == 0 || tickCount % 4 == 0) {
            prey = prey();
        }
        if (prey != null) {
            look(prey.position().subtract(position()));
        } else {
            lookAngle += LOOK_TURN;
            look(Vec3.directionFromRotation(0, lookAngle));
        }
        if (sitting > 0) {
            // Something came along while it was looking round: it gets ready at once.
            int next = prey != null ? Math.min(sitting, WINDUP) : sitting;
            if (next == WINDUP) {
                level().playSound(null, this, ModSounds.BITER_GRENADE_CHATTER.get(), SoundSource.NEUTRAL, 0.5F, 0.9F + random.nextFloat() * 0.2F);
            }
            entityData.set(SITTING, next - 1);
        } else if (prey != null) {
            hop(prey);
        } else if (++hops >= HOPS) {
            swell();
        } else {
            entityData.set(SITTING, LOOK_ROUND);
        }
    }

    private void look(Vec3 way) {
        Vec3 flat = new Vec3(way.x, 0, way.z);
        if (flat.lengthSqr() > 1.0E-4) {
            flat = flat.normalize();
            entityData.set(LOOK, new Vector3f((float) flat.x, 0, (float) flat.z));
        }
    }

    /** The nearest thing it would hunt within sight, or null. */
    private @Nullable LivingEntity prey() {
        Entity owner = getOwner();
        LivingEntity nearest = null;
        double best = SIGHT * SIGHT;
        for (LivingEntity living : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SIGHT), living -> huntable(living, owner))) {
            double distance = living.distanceToSqr(this);
            if (distance < best && sees(living)) {
                best = distance;
                nearest = living;
            }
        }
        return nearest;
    }

    private boolean sees(LivingEntity living) {
        Vec3 from = position().add(0, 0.2, 0);
        return level().clip(new ClipContext(from, living.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    /**
     * What a landed grenade goes after: enemies, mobs angry at the thrower and players the thrower may harm. Never the
     * thrower, a pet, a villager or a passive animal. Stricter than what a throw may hit, which the thrower aimed themselves.
     */
    public static boolean huntable(LivingEntity living, @Nullable Entity owner) {
        if (!living.isAlive() || living == owner || spared(living, owner) || living instanceof AbstractVillager) {
            return false;
        }
        if (living instanceof Player player) {
            return owner instanceof Player thrower && !player.isSpectator() && !player.getAbilities().invulnerable && thrower.canHarmPlayer(player);
        }
        return living instanceof Enemy || living instanceof Mob mob && owner != null && mob.getTarget() == owner;
    }

    /** Springs at {@code prey}, aiming where it will be: thrown again, so it bites on touch and counts when it lands. */
    private void hop(LivingEntity prey) {
        hops++;
        Vec3 from = position();
        Vec3 body = prey.position().add(0, prey.getBbHeight() / 2, 0);
        int ticks = Mth.clamp((int) (8 + from.distanceTo(body) * 1.4), 10, 24);
        Vec3 running = prey instanceof ServerPlayer player ? player.getKnownMovement() : prey.getDeltaMovement();
        Vec3 aim = body.add(running.x * ticks * 0.8, 0, running.z * ticks * 0.8);
        // A flight loses 1% a tick, so a start of 1 carries this far; gravity then wants its fall made up.
        double carried = (1 - Math.pow(0.99, ticks)) / 0.01;
        double fall = getDefaultGravity() * ticks * ticks / 2;
        setDeltaMovement((aim.x - from.x) / carried, (aim.y - from.y + fall) / carried, (aim.z - from.z) / carried);
        entityData.set(SITTING, -1);
        needsSync = true;
        level().playSound(null, this, ModSounds.BITER_GRENADE_HOP.get(), SoundSource.NEUTRAL, 0.9F, 0.9F + random.nextFloat() * 0.2F);
    }

    /** Armed with nothing to hold on to: it drops to the ground and lies there. */
    private void fall() {
        if (!level().isClientSide()) {
            entityData.set(TARGET, -1);
        }
        applyGravity();
        setDeltaMovement(getDeltaMovement().scale(0.98));
        move(MoverType.SELF, getDeltaMovement());
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.5, 0, 0.5));
        }
    }

    private void fuseEffects(int left) {
        if (beat(left)) {
            level().addParticle(ModParticles.glint(GREEN), getX(), getY() + 0.1, getZ(), 0, 0, 0);
            level().addParticle(ModParticles.spark(GREEN), getX(), getY() + 0.1, getZ(),
                    (random.nextDouble() - 0.5) * 0.15, 0.08, (random.nextDouble() - 0.5) * 0.15);
        }
        if (tickCount % 4 == 0) {
            level().addParticle(ModParticles.wisp(GREEN), getX(), getY() + 0.1, getZ(), 0, 0.03, 0);
        }
    }

    /** It flies past what it would never bite, so a pet in the way does not stop a throw. */
    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && latchesOn(entity);
    }

    private boolean latchesOn(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (!(getOwner() instanceof Player thrower)) {
            return living instanceof Enemy;
        }
        return living instanceof Player other ? other != thrower && thrower.canHarmPlayer(other) : ToolEffects.mayStrike(living, thrower);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!level().isClientSide() && hit.getEntity() instanceof LivingEntity target) {
            latch(target);
        }
    }

    /** Bites into the flank it came from. The hit result only says who was hit, so the point comes from the last position. */
    private void latch(LivingEntity target) {
        AABB body = target.getBoundingBox();
        Vec3 at = new Vec3(Mth.clamp(xOld, body.minX, body.maxX), Mth.clamp(yOld, body.minY, body.maxY), Mth.clamp(zOld, body.minZ, body.maxZ));
        Vec3 hold = at.subtract(target.position()).yRot(target.yBodyRot * Mth.DEG_TO_RAD);
        entityData.set(HOLD, new Vector3f((float) hold.x, (float) hold.y, (float) hold.z));
        entityData.set(TARGET, target.getId());
        entityData.set(FUSE_LEFT, FUSE);
        entityData.set(SITTING, -1);
        setDeltaMovement(Vec3.ZERO);
        setPos(at);
        needsSync = true;
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.spark(GREEN), at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.15);
            level.sendParticles(ModParticles.drop(GREEN), at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.05);
            biteSound();
        }
    }

    /** With the grenade for those near it, and for the thrower at any distance: the bite is how a long throw reports a hit. */
    private void biteSound() {
        float pitch = 0.9F + random.nextFloat() * 0.2F;
        SoundEvent bite = ModSounds.BITER_GRENADE_BITE.get();
        level().playSound(null, this, bite, SoundSource.NEUTRAL, BITE_VOLUME, pitch);
        if (getOwner() instanceof ServerPlayer thrower && thrower.level() == level() && thrower.distanceToSqr(this) > BITE_HEARD * BITE_HEARD) {
            thrower.connection.send(new ClientboundSoundPacket(ModSounds.BITER_GRENADE_BITE, SoundSource.NEUTRAL,
                    thrower.getX(), thrower.getY(), thrower.getZ(), 0.6F, pitch, random.nextLong()));
        }
    }

    /** The ground starts the hunt; a wall or a ceiling only knocks it down, and it counts when it lands. */
    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        setPos(hit.getLocation().add(hit.getDirection().getUnitVec3().scale(SIT_HEIGHT)));
        level().playSound(null, this, ModSounds.BITER_GRENADE_CLATTER.get(), SoundSource.NEUTRAL, 0.7F, 0.9F + random.nextFloat() * 0.2F);
        needsSync = true;
        if (hit.getDirection() != Direction.UP) {
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y) * 0.5, 0);
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        if (hops >= HOPS) {
            swell();
        } else {
            entityData.set(SITTING, WINDUP);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fuse", fuseLeft());
        output.putInt("Sitting", sitting());
        output.putInt("Hops", hops);
        output.putInt("Age", age);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(FUSE_LEFT, input.getIntOr("Fuse", -1));
        entityData.set(SITTING, input.getIntOr("Sitting", -1));
        hops = input.getIntOr("Hops", 0);
        age = input.getIntOr("Age", 0);
    }

    /** Whether this creature is a pet of {@code owner}, which the blast spares. */
    public static boolean spared(LivingEntity victim, @Nullable Entity owner) {
        EntityReference<LivingEntity> master = victim instanceof OwnableEntity pet ? pet.getOwnerReference() : null;
        return owner != null && master != null && master.getUUID().equals(owner.getUUID());
    }

    /**
     * The blast at {@code centre}: it hurts and pushes what lives within {@link #RADIUS} and is not behind a wall, the most
     * right at the grenade, and never touches a block. {@code held} is the creature it bit, hit whatever stands between
     * them. What it looks like is the clients' business, from one word sent here. Returns who it hurt.
     */
    public static List<LivingEntity> blast(ServerLevel level, Vec3 centre, Entity source, @Nullable Entity owner, @Nullable LivingEntity held) {
        List<LivingEntity> hurt = new ArrayList<>();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(centre, RADIUS * 2, RADIUS * 2, RADIUS * 2))) {
            double distance = Math.sqrt(victim.getBoundingBox().distanceToSqr(centre));
            float seen = victim == held ? 1 : ServerExplosion.getSeenPercent(centre, victim);
            if (distance > RADIUS || seen <= 0 || spared(victim, owner)) {
                continue;
            }
            float closeness = 1 - (float) distance / RADIUS;
            if (victim.hurtServer(level, level.damageSources().explosion(source, owner), Mth.lerp(closeness, EDGE, BLAST) * seen)) {
                hurt.add(victim);
            }
            Vec3 away = victim.position().subtract(centre);
            double length = Math.max(0.5, away.length());
            victim.push(away.x / length * PUSH * closeness, 0.2 + 0.3 * closeness, away.z / length * PUSH * closeness);
            victim.needsSync = true;
            // needsSync reaches those watching; a player's own client moves them and needs this.
            victim.syncVelocity = victim instanceof Player;
        }
        BiterBlastPayload word = new BiterBlastPayload(centre);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(centre) < BiterBlastPayload.SEEN * BiterBlastPayload.SEEN) {
                ModNetwork.sendToPlayer(player, word);
            }
        }
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.BITER_GRENADE_BLAST.get(), SoundSource.NEUTRAL, BLAST_VOLUME,
                0.95F + level.getRandom().nextFloat() * 0.1F);
        return hurt;
    }
}
