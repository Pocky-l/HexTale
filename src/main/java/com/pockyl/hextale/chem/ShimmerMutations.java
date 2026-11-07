package com.pockyl.hextale.chem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.Thinning;

import java.util.EnumSet;
import java.util.List;

/**
 * Shimmer in an animal's blood: a dose from the Shimmer Injector turns a companion into something bigger for
 * {@value #LASTS} ticks (five in-game days), and a new dose renews it. Only gifts, and they fade without harm; the price
 * is the vial and a little Thinning at the place of the shot.
 *
 * <p>The state is the attachment {@code hextale:shimmer_mutation}, synced so that every client draws the veins and the
 * rider's client knows the stamina. The gifts are saved attribute modifiers; size comes in over {@value #GROW_TICKS} ticks.
 * Riding lives in {@link ShimmerRide}. A mutation never ends under a rider: it waits until the rider is off.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ShimmerMutations {
    public static final int DAY = 24000;
    /** How long a dose lasts. */
    public static final int LASTS = 5 * DAY;
    /** Ticks a fresh mutation takes to reach its full size. */
    public static final int GROW_TICKS = 40;
    public static final String DOSED = "shimmer_mutation";
    /** A Hound's reach for a pounce, in blocks: inside the minimum it simply bites. */
    public static final double POUNCE_MIN = 3;
    public static final double POUNCE_MAX = 8;
    /** Ticks between two pounces. */
    public static final int POUNCE_REST = 60;
    /** Health a Hound regenerates per {@value #REGEN_TICKS} ticks. */
    public static final float REGEN = 1;
    public static final int REGEN_TICKS = 20;
    private static final Identifier MODIFIER = Hextale.id("shimmer_mutation");
    private static final List<Holder<Attribute>> CHANGED = List.of(Attributes.SCALE, Attributes.MAX_HEALTH, Attributes.ATTACK_KNOCKBACK,
            Attributes.MOVEMENT_SPEED, Attributes.JUMP_STRENGTH, Attributes.WATER_MOVEMENT_EFFICIENCY, Attributes.STEP_HEIGHT);

    /** What a companion turns into, by its size. */
    public enum Kind implements StringRepresentable {
        NONE(1), HOUND(1.4F), STEED(1.15F), PANTHER(2.4F), RAPTOR(2.6F);

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        public static final StreamCodec<ByteBuf, Kind> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Kind::ordinal);
        private final float scale;

        Kind(float scale) {
            this.scale = scale;
        }

        /** The size of the grown animal, 1 being its own. */
        public float scale() {
            return scale;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** What an animal turned into, the game time it fades at, and a Raptor's wind. */
    public record Mutation(Kind kind, long until, int stamina) {
        public static final Mutation NONE = new Mutation(Kind.NONE, 0, 0);
        public static final Codec<Mutation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Kind.CODEC.fieldOf("kind").forGetter(Mutation::kind),
                Codec.LONG.fieldOf("until").forGetter(Mutation::until),
                Codec.INT.optionalFieldOf("stamina", 0).forGetter(Mutation::stamina)).apply(instance, Mutation::new));
        public static final StreamCodec<ByteBuf, Mutation> STREAM_CODEC = StreamCodec.composite(
                Kind.STREAM_CODEC, Mutation::kind, ByteBufCodecs.VAR_LONG, Mutation::until, ByteBufCodecs.VAR_INT, Mutation::stamina, Mutation::new);

        public Mutation withStamina(int value) {
            return new Mutation(kind, until, value);
        }
    }

    private ShimmerMutations() {
    }

    /** The animal's mutation, or null. Both sides ask; it never creates the attachment. */
    public static @Nullable Mutation of(Entity entity) {
        return entity.getExistingDataOrNull(ModAttachments.SHIMMER_MUTATION);
    }

    public static boolean is(Entity entity, Kind kind) {
        Mutation mutation = of(entity);
        return mutation != null && mutation.kind() == kind;
    }

    /** What this kind of animal turns into; null for all others (llamas, undead horses and camel husks too). */
    public static @Nullable Kind kindFor(EntityType<?> type) {
        if (type == EntityTypes.WOLF) {
            return Kind.HOUND;
        }
        if (type == EntityTypes.HORSE || type == EntityTypes.DONKEY || type == EntityTypes.MULE || type == EntityTypes.CAMEL) {
            return Kind.STEED;
        }
        if (type == EntityTypes.CAT) {
            return Kind.PANTHER;
        }
        return type == EntityTypes.PARROT ? Kind.RAPTOR : null;
    }

    /** Whether the animal counts as this player's: tamed by them, or a tame horse nobody claimed. */
    public static boolean owns(Player player, Mob animal) {
        if (animal instanceof TamableAnimal tamable) {
            return tamable.isOwnedBy(player);
        }
        if (animal instanceof AbstractHorse horse && horse.isTamed()) {
            return horse.getOwnerReference() == null || player.equals(horse.getOwner());
        }
        return false;
    }

    /** A shot: the animal is changed, or renewed, from now on. The injector's rules are checked by the injector. */
    public static void dose(ServerPlayer player, Mob animal, Kind kind) {
        ServerLevel level = (ServerLevel) animal.level();
        Mutation before = of(animal);
        int stamina = before == null ? ShimmerRide.STAMINA : before.stamina();
        animal.setData(ModAttachments.SHIMMER_MUTATION, new Mutation(kind, level.getGameTime() + LASTS, kind == Kind.RAPTOR ? stamina : 0));
        animal.setPersistenceRequired();
        strengthen(animal, kind);
        animal.setHealth(animal.getMaxHealth());
        Thinning.add(level, animal.blockPosition(), Thinning.DOSED, Thinning.Source.DOSED);
        burst(level, animal, 24);
        level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), ModSounds.SHIMMER_RUSH.get(), SoundSource.NEUTRAL, 0.8F, 0.8F);
        Quests.fire(player, DOSED);
    }

    private static void strengthen(Mob animal, Kind kind) {
        switch (kind) {
            case HOUND -> {
                modify(animal, Attributes.MAX_HEALTH, 2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                modify(animal, Attributes.ATTACK_KNOCKBACK, 1.5, AttributeModifier.Operation.ADD_VALUE);
            }
            case STEED -> {
                modify(animal, Attributes.MOVEMENT_SPEED, 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                modify(animal, Attributes.JUMP_STRENGTH, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                modify(animal, Attributes.WATER_MOVEMENT_EFFICIENCY, 1, AttributeModifier.Operation.ADD_VALUE);
            }
            case PANTHER -> {
                modify(animal, Attributes.MAX_HEALTH, 2, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                modify(animal, Attributes.MOVEMENT_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                modify(animal, Attributes.JUMP_STRENGTH, 0.33, AttributeModifier.Operation.ADD_VALUE);
                modify(animal, Attributes.STEP_HEIGHT, 0.5, AttributeModifier.Operation.ADD_VALUE);
            }
            case RAPTOR -> modify(animal, Attributes.MAX_HEALTH, 3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case NONE -> {
            }
        }
    }

    private static void modify(LivingEntity animal, Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = animal.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(MODIFIER);
            instance.addPermanentModifier(new AttributeModifier(MODIFIER, amount, operation));
        }
    }

    /** Size follows the time since the dose. It never shrinks here, so a renewal does not make a grown animal small again. */
    private static void grow(Mob animal, Mutation mutation, long now) {
        AttributeInstance scale = animal.getAttribute(Attributes.SCALE);
        if (scale == null) {
            return;
        }
        double full = mutation.kind().scale() - 1;
        long age = now - (mutation.until() - LASTS);
        double amount = full * Math.clamp((age + 1) / (double) GROW_TICKS, 0, 1);
        AttributeModifier current = scale.getModifier(MODIFIER);
        if (current == null || current.amount() < amount) {
            modify(animal, Attributes.SCALE, amount, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    /** The mutation ends: every gift goes, nothing else, and a saddle only a Panther can wear comes off. */
    private static void fade(ServerLevel level, Mob animal, Mutation mutation) {
        for (Holder<Attribute> attribute : CHANGED) {
            AttributeInstance instance = animal.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(MODIFIER);
            }
        }
        if (mutation.kind() == Kind.PANTHER && ShimmerRide.saddled(animal)) {
            ItemStack saddle = animal.getItemBySlot(EquipmentSlot.SADDLE);
            animal.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
            animal.spawnAtLocation(level, saddle);
        }
        animal.removeData(ModAttachments.SHIMMER_MUTATION);
        burst(level, animal, 12);
        level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), ModSounds.SHIMMER_RUSH.get(), SoundSource.NEUTRAL, 0.5F, 0.6F);
    }

    private static void burst(ServerLevel level, Entity animal, int motes) {
        Vec3 at = animal.position().add(0, animal.getBbHeight() * 0.55, 0);
        double spread = animal.getBbWidth() * 0.5;
        level.sendParticles(ModParticles.SHIMMER_MOTE.get(), at.x, at.y, at.z, motes, spread, animal.getBbHeight() * 0.4, spread, 0.02);
    }

    /** One server tick of a mutated animal. */
    public static void tick(ServerLevel level, Mob animal, Mutation mutation) {
        long now = level.getGameTime();
        if (now >= mutation.until() && !animal.isVehicle()) {
            fade(level, animal, mutation);
            return;
        }
        grow(animal, mutation, now);
        if (mutation.kind() == Kind.HOUND && animal.tickCount % REGEN_TICKS == 0 && animal.getHealth() < animal.getMaxHealth()) {
            animal.heal(REGEN);
        } else if (mutation.kind() == Kind.RAPTOR) {
            ShimmerRide.rest(animal, mutation);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob animal && animal.level() instanceof ServerLevel level) {
            Mutation mutation = of(animal);
            if (mutation != null) {
                tick(level, animal, mutation);
            }
        }
    }

    /** Steeds, Panthers and Raptors land on their feet, and so does whoever sits on them. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        Mutation mutation = of(event.getEntity());
        if (mutation != null && mutation.kind() != Kind.HOUND) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Wolf wolf) {
            wolf.goalSelector.addGoal(3, new Pounce(wolf));
        }
    }

    /** Whether a Hound on the ground has a pounce at this target. */
    public static boolean canPounce(Mob hound, LivingEntity target) {
        double distance = hound.distanceTo(target);
        return is(hound, Kind.HOUND) && hound.onGround() && target.isAlive() && distance >= POUNCE_MIN && distance <= POUNCE_MAX
                && hound.getSensing().hasLineOfSight(target);
    }

    /** The speed that throws a Hound onto a spot this far away, air drag included. */
    public static Vec3 leapSpeed(Vec3 to) {
        double flat = Math.sqrt(to.x * to.x + to.z * to.z);
        int ticks = (int) Math.clamp(flat * 1.5, 6, 12);
        double speed = flat / ((1 - Math.pow(0.91, ticks)) / 0.09);
        double up = Math.clamp(0.04 * ticks + to.y / ticks, 0.2, 1.2);
        return flat < 1.0E-4 ? new Vec3(0, up, 0) : new Vec3(to.x / flat * speed, up, to.z / flat * speed);
    }

    private static final class Pounce extends Goal {
        private final Wolf wolf;
        private long ready;

        private Pounce(Wolf wolf) {
            this.wolf = wolf;
            setFlags(EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = wolf.getTarget();
            return target != null && !wolf.isInSittingPose() && wolf.level().getGameTime() >= ready && canPounce(wolf, target);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = wolf.getTarget();
            if (target == null || !(wolf.level() instanceof ServerLevel level)) {
                return;
            }
            ready = level.getGameTime() + POUNCE_REST;
            wolf.getNavigation().stop();
            wolf.getLookControl().setLookAt(target);
            wolf.setDeltaMovement(leapSpeed(target.position().subtract(wolf.position())));
            wolf.needsSync = true;
            burst(level, wolf, 6);
            level.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(), ModSounds.SHIMMER_RUSH.get(), SoundSource.NEUTRAL, 0.4F, 1.5F);
        }
    }
}
