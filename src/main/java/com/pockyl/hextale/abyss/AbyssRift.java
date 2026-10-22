package com.pockyl.hextale.abyss;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * A rift of the abyss: a tear in the world through which the depth shows. Only the look for now (no logic): it tears
 * open, breathes, sucks motes in, and closes when told to. Its shape is one of {@link RiftShape}, its jagged outline
 * comes from {@link #seed()} (the client builds the mesh, nothing but the numbers is synced). The entity stands at the
 * centre of the opening; {@link #orientation()} turns the shape's plane into the world.
 */
public class AbyssRift extends Entity {
    /** Ticks from the first crack to the fully open rift. */
    public static final int OPEN_TICKS = 40;
    /** Ticks the crack runs along the shape before it starts to part. */
    public static final int CRACK_TICKS = 10;
    /** Ticks from the order to close until the rift is gone. */
    public static final int CLOSE_TICKS = 26;

    public static final int VIOLET = 0x9A3CFF;
    public static final int MAGENTA = 0xE05CF0;
    public static final int PALE = 0xF4D2FF;
    public static final int DEEP = 0x34105A;
    public static final int COLD = 0x5FE8F0;

    private static final EntityDataAccessor<Integer> DATA_SEED = SynchedEntityData.defineId(AbyssRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHAPE = SynchedEntityData.defineId(AbyssRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(AbyssRift.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> DATA_BORN = SynchedEntityData.defineId(AbyssRift.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_CLOSE_AT = SynchedEntityData.defineId(AbyssRift.class, EntityDataSerializers.LONG);

    private boolean burstShown;

    public AbyssRift(EntityType<? extends AbyssRift> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A new rift at {@code centre}, facing {@code yaw}, opening from now on. */
    public static AbyssRift open(Level level, Vec3 centre, float yaw, RiftShape shape, float size, int seed) {
        AbyssRift rift = new AbyssRift(ModEntities.ABYSS_RIFT.get(), level);
        rift.setPos(centre);
        rift.setYRot(yaw);
        rift.entityData.set(DATA_SHAPE, shape.ordinal());
        rift.entityData.set(DATA_SIZE, Mth.clamp(size, 0.5F, 16));
        rift.entityData.set(DATA_SEED, seed);
        rift.entityData.set(DATA_BORN, level.getGameTime());
        return rift;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SEED, 0);
        builder.define(DATA_SHAPE, 0);
        builder.define(DATA_SIZE, 2.0F);
        builder.define(DATA_BORN, 0L);
        builder.define(DATA_CLOSE_AT, -1L);
    }

    public int seed() {
        return entityData.get(DATA_SEED);
    }

    public RiftShape shape() {
        return RiftShape.byOrdinal(entityData.get(DATA_SHAPE));
    }

    public float size() {
        return entityData.get(DATA_SIZE);
    }

    /** Ticks since the rift began to tear open, with the partial tick. */
    public float age(float partialTick) {
        return Math.max(0, level().getGameTime() - entityData.get(DATA_BORN)) + partialTick;
    }

    public boolean closing() {
        return entityData.get(DATA_CLOSE_AT) >= 0;
    }

    /** 0 while open, rising to 1 as it closes. */
    public float closeProgress(float partialTick) {
        long at = entityData.get(DATA_CLOSE_AT);
        if (at < 0) {
            return 0;
        }
        return Mth.clamp((level().getGameTime() - at + partialTick) / CLOSE_TICKS, 0, 1);
    }

    /** Starts closing it; the entity goes when the rift has shut. */
    public void close() {
        if (closing()) {
            return;
        }
        entityData.set(DATA_CLOSE_AT, level().getGameTime());
        level().playSound(null, getX(), getY(), getZ(), ModSounds.ABYSS_RIFT_CLOSE.get(), SoundSource.AMBIENT,
                0.6F + size() * 0.12F, 1.15F - Math.min(size(), 8) * 0.05F);
    }

    /**
     * How the shape's plane lies in the world: turned to the rift's yaw, a standing rift tilted a little by its seed, a
     * lying one laid flat. Local x = across the opening, y = along it, z = the plane's normal.
     */
    public Quaternionf orientation() {
        return orientation(getYRot(), shape(), seed());
    }

    public static Quaternionf orientation(float yaw, RiftShape shape, int seed) {
        Quaternionf q = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD);
        if (shape.lying()) {
            q.rotateX(Mth.HALF_PI);
        }
        float tilt = ((seed >>> 8 & 0xFF) / 255F - 0.5F) * (shape.lying() ? 0.6F : 0.7F);
        return q.rotateZ(tilt);
    }

    /** A rift-local point (blocks) in the world. */
    public Vec3 toWorld(Vector3f local) {
        Vector3f turned = orientation().transform(new Vector3f(local));
        return position().add(turned.x, turned.y, turned.z);
    }

    @Override
    public void tick() {
        // No physics, no fire, no fluids: a rift only hangs where it tore.
        if (level().isClientSide()) {
            clientEffects();
        } else if (closing() && level().getGameTime() - entityData.get(DATA_CLOSE_AT) >= CLOSE_TICKS) {
            discard();
        }
    }

    private void clientEffects() {
        RandomSource random = level().getRandom();
        RiftShape shape = shape();
        float size = size();
        float age = age(0);
        float close = closeProgress(0);
        Vector3f normal = orientation().transform(new Vector3f(0, 0, 1));

        if (age < OPEN_TICKS) {
            // The crack running out: sparks spit from the line, more while it parts.
            int sparks = age < CRACK_TICKS ? 2 : 1;
            for (int i = 0; i < sparks; i++) {
                Vec3 at = toWorld(shape.samplePoint(random, size, false).mul(Math.min(1, age / CRACK_TICKS)));
                level().addParticle(ModParticles.spark(random.nextBoolean() ? MAGENTA : PALE), at.x, at.y, at.z, 0, 0, 0);
            }
            if ((int) age == CRACK_TICKS) {
                Vec3 centre = position();
                level().addParticle(ModParticles.ring(MAGENTA), centre.x, centre.y, centre.z, size * 0.7, 0, 0);
            }
        }

        // The world drifting in: glowing motes and dark flakes spiral into the opening from all around it.
        float pull = closing() ? 2.5F : 1;
        float chance = Math.min(3, (0.35F + size * 0.22F) * pull);
        for (int n = 0; chance > 0; chance -= 1, n++) {
            if (random.nextFloat() >= chance) {
                break;
            }
            Vec3 target = toWorld(shape.samplePoint(random, size, false).mul(1 - close));
            float reach = (0.9F + random.nextFloat() * 1.4F) * (0.6F + size * 0.18F);
            Vector3f out = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
            // Mostly from in front of and behind the opening, a little from the sides.
            out.add(new Vector3f(normal).mul((random.nextBoolean() ? 1 : -1) * 0.7F)).normalize().mul(reach);
            int colour = switch (random.nextInt(5)) {
                case 0 -> DEEP;
                case 1 -> MAGENTA;
                case 2 -> PALE;
                default -> VIOLET;
            };
            level().addParticle(ModParticles.abyssMote(colour), target.x, target.y, target.z, out.x, out.y, out.z);
        }

        // Rare tongues of light licking off the torn edge.
        if (random.nextFloat() < 0.08F + size * 0.03F) {
            Vec3 at = toWorld(shape.samplePoint(random, size, true).mul(1 - close));
            level().addParticle(ModParticles.glint(random.nextInt(4) == 0 ? COLD : PALE), at.x, at.y, at.z, 0, 0, 0);
        }

        if (close > 0.8F && !burstShown) {
            // The last breath as it shuts: a ring pulled tight and a puff of dark.
            burstShown = true;
            Vec3 centre = position();
            level().addParticle(ModParticles.ring(VIOLET), centre.x, centre.y, centre.z, size * 0.5, 0, 0);
            for (int i = 0; i < 10; i++) {
                level().addParticle(ModParticles.vapour(DEEP), centre.x, centre.y, centre.z,
                        (random.nextFloat() - 0.5F) * 0.08, (random.nextFloat() - 0.5F) * 0.08, (random.nextFloat() - 0.5F) * 0.08);
            }
        }
    }

    /** The rift the player looks at within {@code reach} blocks, nearest first. */
    public static @Nullable AbyssRift aimedAt(Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB area = new AABB(eye, eye.add(look.scale(reach))).inflate(8);
        AbyssRift best = null;
        double bestAlong = reach;
        for (AbyssRift rift : player.level().getEntitiesOfClass(AbyssRift.class, area, rift -> !rift.closing())) {
            Vec3 to = rift.position().subtract(eye);
            double along = to.dot(look);
            if (along < 0 || along > bestAlong) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off <= rift.size() * 0.5 + 0.5) {
                best = rift;
                bestAlong = along;
            }
        }
        return best;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 64 + size() * 12;
        return distance < range * range;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        // A rift summoned by a command without numbers gets a random outline and tears open from now on.
        entityData.set(DATA_SEED, input.getIntOr("seed", level().getRandom().nextInt()));
        entityData.set(DATA_SHAPE, RiftShape.byId(input.getStringOr("shape", RiftShape.TEAR.id())).ordinal());
        entityData.set(DATA_SIZE, Mth.clamp(input.getFloatOr("size", 2), 0.5F, 16));
        entityData.set(DATA_BORN, input.getLongOr("born", level().getGameTime()));
        entityData.set(DATA_CLOSE_AT, input.getLongOr("close_at", -1));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("seed", seed());
        output.putString("shape", shape().id());
        output.putFloat("size", size());
        output.putLong("born", entityData.get(DATA_BORN));
        output.putLong("close_at", entityData.get(DATA_CLOSE_AT));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }
}
