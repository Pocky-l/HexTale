package com.pockyl.hextale.thevoid;

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
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * The Void breaking into the world: a blocky breach of glowing edges with a whirling depth behind it. Only the look for
 * now (no logic): it spreads open from its heart, breathes out dark smoke and draws the world's motes in, and closes when
 * told to. Its outline is a
 * {@link VoidLayout} built from the seed (nothing but the numbers is synced). The entity stands at the centre of the
 * breach; {@link #orientation()} turns the breach's plane into the world.
 */
public class TheVoid extends Entity {
    /** Ticks from the first cell to the whole breach. */
    public static final int OPEN_TICKS = 30;
    /** Ticks from the order to close until it is gone. */
    public static final int CLOSE_TICKS = 24;

    public static final int VIOLET = 0x9A3CFF;
    public static final int MAGENTA = 0xF23CE6;
    public static final int PINK = 0xFFB4F4;
    public static final int DEEP = 0x34105A;
    /** The smoke it breathes: nearly black, a little violet. */
    public static final int SMOKE = 0x140A1C;
    public static final int UMBER = 0x2A2014;

    private static final EntityDataAccessor<Integer> DATA_SEED = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHAPE = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> DATA_BORN = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> DATA_CLOSE_AT = SynchedEntityData.defineId(TheVoid.class, EntityDataSerializers.LONG);

    private boolean burstShown;

    public TheVoid(EntityType<? extends TheVoid> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A new breach at {@code centre}, facing {@code yaw}, opening from now on. */
    public static TheVoid open(Level level, Vec3 centre, float yaw, VoidShape shape, float size, int seed) {
        TheVoid breach = new TheVoid(ModEntities.THE_VOID.get(), level);
        breach.setPos(centre);
        breach.setYRot(yaw);
        breach.entityData.set(DATA_SHAPE, shape.ordinal());
        breach.entityData.set(DATA_SIZE, Mth.clamp(size, 0.5F, 16));
        breach.entityData.set(DATA_SEED, seed);
        breach.entityData.set(DATA_BORN, level.getGameTime());
        return breach;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SEED, 0);
        builder.define(DATA_SHAPE, 0);
        builder.define(DATA_SIZE, 3.0F);
        builder.define(DATA_BORN, 0L);
        builder.define(DATA_CLOSE_AT, -1L);
    }

    public int seed() {
        return entityData.get(DATA_SEED);
    }

    public VoidShape shape() {
        return VoidShape.byOrdinal(entityData.get(DATA_SHAPE));
    }

    public float size() {
        return entityData.get(DATA_SIZE);
    }

    public VoidLayout layout() {
        return VoidLayout.of(shape(), size(), seed());
    }

    /** Ticks since it began to break open, with the partial tick. */
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

    /** How much of the breach shows: 0 -> 1 while opening, back to 0 while closing. */
    public float reveal(float partialTick) {
        return Math.min(Mth.clamp(age(partialTick) / OPEN_TICKS, 0, 1), 1 - closeProgress(partialTick));
    }

    /** Starts closing it; the entity goes when the breach has shut. */
    public void close() {
        if (closing()) {
            return;
        }
        entityData.set(DATA_CLOSE_AT, level().getGameTime());
        level().playSound(null, getX(), getY(), getZ(), ModSounds.THE_VOID_CLOSE.get(), SoundSource.AMBIENT,
                0.6F + size() * 0.12F, 1.15F - Math.min(size(), 8) * 0.05F);
    }

    /**
     * How the breach's plane lies in the world: upright and turned to the yaw, or laid flat on the ground. Local x =
     * across the breach, y = along it, z = the plane's normal.
     */
    public Quaternionf orientation() {
        return orientation(getYRot(), shape());
    }

    public static Quaternionf orientation(float yaw, VoidShape shape) {
        Quaternionf q = new Quaternionf().rotationY(-yaw * Mth.DEG_TO_RAD);
        if (shape.lying()) {
            q.rotateX(Mth.HALF_PI);
        }
        return q;
    }

    /** A local point (blocks) in the world. */
    public Vec3 toWorld(Vector3f local) {
        Vector3f turned = orientation().transform(new Vector3f(local));
        return position().add(turned.x, turned.y, turned.z);
    }

    @Override
    public void tick() {
        // No physics, no fire, no fluids: the breach only hangs where it broke through.
        if (level().isClientSide()) {
            clientEffects();
        } else if (closing() && level().getGameTime() - entityData.get(DATA_CLOSE_AT) >= CLOSE_TICKS) {
            discard();
        }
    }

    private void clientEffects() {
        RandomSource random = level().getRandom();
        VoidLayout layout = layout();
        float size = size();
        float age = age(0);
        float reveal = reveal(0);
        Vector3f normal = orientation().transform(new Vector3f(0, 0, 1));

        if (age < OPEN_TICKS && !closing()) {
            // Breaking through: dark smoke bursts out of the cells as they appear.
            for (int i = 0; i < 3; i++) {
                Vec3 at = toWorld(layout.samplePoint(random, true).mul(Math.max(0.2F, reveal)));
                float push = (random.nextBoolean() ? 1 : -1) * 0.04F;
                level().addParticle(ModParticles.vapour(SMOKE), at.x, at.y, at.z, normal.x * push, normal.y * push, normal.z * push);
            }
            if ((int) age == 2) {
                Vec3 centre = position();
                level().addParticle(ModParticles.ring(MAGENTA), centre.x, centre.y, centre.z, size * 0.6, 0, 0);
            }
        }

        // The world drifting in: motes, mostly dark, spiral into the breach from in front of it and behind it.
        float pull = closing() ? 2.5F : 1;
        float chance = Math.min(3, (0.35F + size * 0.22F) * pull * reveal);
        for (; chance > 0; chance -= 1) {
            if (random.nextFloat() >= chance) {
                break;
            }
            Vec3 target = toWorld(layout.samplePoint(random, false));
            float reach = (0.9F + random.nextFloat() * 1.4F) * (0.6F + size * 0.18F);
            Vector3f out = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
            out.add(new Vector3f(normal).mul((random.nextBoolean() ? 1 : -1) * 0.7F)).normalize().mul(reach);
            int colour = switch (random.nextInt(7)) {
                case 0 -> MAGENTA;
                case 1 -> PINK;
                case 2 -> VIOLET;
                case 3 -> UMBER;
                default -> DEEP;
            };
            level().addParticle(ModParticles.voidMote(colour), target.x, target.y, target.z, out.x, out.y, out.z);
        }

        // It breathes out: dark smoke seeps from the edge and drifts away from it, slow and heavy.
        if (random.nextFloat() < (0.3F + size * 0.12F) * reveal) {
            Vector3f edge = layout.samplePoint(random, true);
            Vec3 at = toWorld(edge);
            Vector3f out = orientation().transform(new Vector3f(edge.x, edge.y, 0).normalize(0.025F));
            float side = (random.nextFloat() - 0.5F) * 0.03F;
            level().addParticle(ModParticles.vapour(random.nextInt(4) == 0 ? DEEP : SMOKE), at.x, at.y, at.z,
                    out.x + normal.x * side, out.y + normal.y * side, out.z + normal.z * side);
        }
        // Now and then a sickly spark of its light drifts off the edge.
        if (random.nextFloat() < (0.05F + size * 0.02F) * reveal) {
            Vec3 at = toWorld(layout.samplePoint(random, true));
            level().addParticle(ModParticles.glint(random.nextBoolean() ? MAGENTA : PINK), at.x, at.y, at.z, 0, 0, 0);
        }

        if (closeProgress(0) > 0.8F && !burstShown) {
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

    /** The breach the player looks at within {@code reach} blocks, nearest first. */
    public static @Nullable TheVoid aimedAt(Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB area = new AABB(eye, eye.add(look.scale(reach))).inflate(8);
        TheVoid best = null;
        double bestAlong = reach;
        for (TheVoid breach : player.level().getEntitiesOfClass(TheVoid.class, area, breach -> !breach.closing())) {
            Vec3 to = breach.position().subtract(eye);
            double along = to.dot(look);
            if (along < 0 || along > bestAlong) {
                continue;
            }
            double off = to.subtract(look.scale(along)).length();
            if (off <= breach.layout().reach() * 0.8 + 0.5) {
                best = breach;
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
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueInput input = ValueInput.of(portTag, registryAccess());
        // Summoned by a command without numbers: a random outline that breaks open from now on.
        entityData.set(DATA_SEED, input.getIntOr("seed", level().getRandom().nextInt()));
        entityData.set(DATA_SHAPE, VoidShape.byId(input.getStringOr("shape", VoidShape.GATE.id())).ordinal());
        entityData.set(DATA_SIZE, Mth.clamp(input.getFloatOr("size", 3), 0.5F, 16));
        entityData.set(DATA_BORN, input.getLongOr("born", level().getGameTime()));
        entityData.set(DATA_CLOSE_AT, input.getLongOr("close_at", -1));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, registryAccess());
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
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        return false;
    }
}
