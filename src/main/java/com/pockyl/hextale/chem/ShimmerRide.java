package com.pockyl.hextale.chem;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.chem.ShimmerMutations.Kind;
import com.pockyl.hextale.chem.ShimmerMutations.Mutation;
import com.pockyl.hextale.item.ShimmerInjectorItem;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModSounds;

import java.util.function.ToDoubleFunction;

/**
 * Riding mutated companions. A saddled Panther and a Raptor take their owner on an empty hand's click; sneaking keeps
 * the vanilla click, so they can still be told to sit. The vanilla classes get the rider through {@code CatMixin},
 * {@code ParrotMixin} and {@code AbstractHorseMixin}; everything they decide is here.
 *
 * <p>As with every mount the rider's client moves the animal, and the server only hears positions. A Raptor's stamina is
 * counted on the server from the rider's jump key and synced back. The floating kick is waived only for paid flaps: a
 * Raptor without wind glides down fast enough never to need it, and a client that climbs without paying meets the kick.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ShimmerRide {
    /** A rested Raptor's wind: ticks of flapping. */
    public static final int STAMINA = 240;
    /** Wind back on the ground, per {@value #REST_TICKS} ticks. */
    public static final int REST = 5;
    public static final int REST_TICKS = 5;
    /** Upward speed a flap gives in a tick, up to {@link #CLIMB} (blocks a tick). */
    public static final double LIFT = 0.16;
    public static final double CLIMB = 0.45;
    /** The fastest a gliding Raptor sinks (blocks a tick). */
    public static final double GLIDE = 0.12;
    /** Speed setting of a ridden Raptor, about 18 blocks a second in the air. */
    public static final float AIR_SPEED = 0.8F;
    public static final float GROUND_SPEED = 0.22F;
    public static final int FLAP_SOUND_TICKS = 9;
    /** What a leaning mount turns round, as a share of its height. */
    public static final float LEAN_PIVOT = 0.5F;
    /** Raptor in a glide, nose down in degrees: flat, as the model stands 28 degrees off upright. */
    public static final float GLIDE_PITCH = -60;
    /** Panther on a wall, nose up in degrees. */
    public static final float WALL_PITCH = 68;
    /** Rise of a Panther's middle at full lean, so that its hind paws stay on the ground (blocks at size 1). */
    public static final float WALL_LIFT = 0.15F;
    /** The seat over an animal's feet and behind its centre, at the middle of its body (blocks at size 1). */
    private static final float PANTHER_SEAT_UP = 0.52F;
    private static final float PANTHER_SEAT_BACK = 0.05F;
    /** A Raptor stands upright, so its seat is the rear of its body, which turns into the top in a glide. */
    private static final float RAPTOR_SEAT_UP = 0.35F;
    private static final float RAPTOR_SEAT_BACK = -0.015F;
    /** The lean of a mount as a client draws it, nose up in degrees. */
    private static ToDoubleFunction<Mob> lean = animal -> 0;

    private ShimmerRide() {
    }

    /** Clients draw the lean, so a client tells where it stands: the seat goes with the body. */
    public static void leanFrom(ToDoubleFunction<Mob> source) {
        lean = source;
    }

    /** Where a Panther or a Raptor carries its rider, from its feet; null for any other animal. */
    public static @Nullable Vec3 seat(Mob animal, EntityDimensions size, float scale) {
        ShimmerMutations.Mutation mutation = ShimmerMutations.of(animal);
        if (mutation == null || mutation.kind() != Kind.PANTHER && mutation.kind() != Kind.RAPTOR) {
            return null;
        }
        float pitch = animal.level().isClientSide() ? (float) lean.applyAsDouble(animal) : 0;
        return seat(mutation.kind(), size.height(), scale, pitch).yRot(-animal.getYRot() * Mth.DEG_TO_RAD);
    }

    /** The seat of an animal facing +Z, turned with its body as {@code ShimmerPose} turns the model. */
    public static Vec3 seat(Kind kind, float height, float scale, float pitch) {
        boolean panther = kind == Kind.PANTHER;
        float pivot = height * LEAN_PIVOT;
        float up = (panther ? PANTHER_SEAT_UP : RAPTOR_SEAT_UP) * scale - pivot;
        float back = (panther ? PANTHER_SEAT_BACK : RAPTOR_SEAT_BACK) * scale;
        float lift = pitch > 0 ? WALL_LIFT * scale * pitch / WALL_PITCH : 0;
        float sin = Mth.sin(pitch * Mth.DEG_TO_RAD);
        float cos = Mth.cos(pitch * Mth.DEG_TO_RAD);
        return new Vec3(0, pivot + lift + up * cos - back * sin, -(up * sin + back * cos));
    }

    public static boolean saddled(Mob animal) {
        return animal.getItemBySlot(EquipmentSlot.SADDLE).is(Items.SADDLE);
    }

    private static boolean hasSeat(Mob animal) {
        return ShimmerMutations.is(animal, Kind.RAPTOR) || ShimmerMutations.is(animal, Kind.PANTHER) && saddled(animal);
    }

    /** The player steering this animal, or null when it walks by itself. */
    public static @Nullable Player rider(Mob animal) {
        return animal.getFirstPassenger() instanceof Player player && hasSeat(animal) ? player : null;
    }

    /** The keys as a mount's input, by the rules of a horse. */
    public static Vec3 input(Player controller) {
        float forward = controller.zza;
        return new Vec3(controller.xxa * 0.5F, 0, forward <= 0 ? forward * 0.25F : forward);
    }

    public static float pantherSpeed(Mob panther) {
        return (float) panther.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    public static float raptorSpeed(Mob raptor) {
        return raptor.onGround() ? GROUND_SPEED : AIR_SPEED;
    }

    /** Stops what a server-side animal was walking to, so that it does not fight the rider. */
    public static void ridden(Mob animal) {
        if (!animal.level().isClientSide()) {
            animal.getNavigation().stop();
            animal.setNoGravity(false);
        }
    }

    /** The motion of a ridden Raptor before the tick's gravity. */
    public static Vec3 motion(Vec3 motion, boolean flap, boolean onGround) {
        if (flap) {
            return new Vec3(motion.x, Math.min(motion.y + LIFT, CLIMB), motion.z);
        }
        return onGround ? motion : new Vec3(motion.x, Math.max(motion.y, -GLIDE), motion.z);
    }

    /** One tick of a ridden Raptor on both sides: the client flies it, the server counts the wind. */
    public static void fly(Mob raptor, Player controller) {
        Mutation mutation = ShimmerMutations.of(raptor);
        if (mutation == null) {
            return;
        }
        ridden(raptor);
        if (raptor.canSimulateMovement()) {
            raptor.setDeltaMovement(motion(raptor.getDeltaMovement(), controller.isJumping() && mutation.stamina() > 0, raptor.onGround()));
        }
        if (controller instanceof ServerPlayer player && player.getLastClientInput().jump()) {
            flap(raptor, player, mutation);
        }
    }

    /** The server's side of a tick with the jump key down. Returns whether it was paid for. */
    public static boolean flap(Mob raptor, ServerPlayer rider, Mutation mutation) {
        if (mutation.stamina() <= 0) {
            return false;
        }
        raptor.setData(ModAttachments.SHIMMER_MUTATION, mutation.withStamina(mutation.stamina() - 1));
        raptor.resetFallDistance();
        if (rider.connection != null) {
            rider.connection.resetFlyingTicks();
        }
        if (raptor.tickCount % FLAP_SOUND_TICKS == 0) {
            raptor.level().playSound(null, raptor.getX(), raptor.getY(), raptor.getZ(), ModSounds.RAPTOR_FLAP.get(), SoundSource.NEUTRAL, 0.3F,
                    0.92F + raptor.getRandom().nextFloat() * 0.16F);
        }
        return true;
    }

    /** A Raptor gets its wind back only on the ground, with or without a rider. */
    public static void rest(Mob raptor, Mutation mutation) {
        if (raptor.onGround() && mutation.stamina() < STAMINA && raptor.tickCount % REST_TICKS == 0) {
            raptor.setData(ModAttachments.SHIMMER_MUTATION, mutation.withStamina(Math.min(STAMINA, mutation.stamina() + REST)));
        }
    }

    /** Whether this click on the animal is the owner getting on: an empty hand, not sneaking, and a free seat. */
    public static boolean mounts(Player player, Mob animal, ItemStack held) {
        return hasSeat(animal) && held.isEmpty() && !player.isSecondaryUseActive() && !animal.isVehicle() && ShimmerMutations.owns(player, animal);
    }

    /** Whether this click puts a saddle on a Panther. */
    public static boolean saddles(Player player, Mob animal, ItemStack held) {
        return held.is(Items.SADDLE) && ShimmerMutations.is(animal, Kind.PANTHER) && !saddled(animal) && ShimmerMutations.owns(player, animal);
    }

    /** Gets on; a sitting animal stands up for it. */
    public static void mount(Player player, Mob animal) {
        if (animal instanceof TamableAnimal tamable) {
            tamable.setOrderedToSit(false);
            tamable.setInSittingPose(false);
        }
        ridden(animal);
        player.startRiding(animal);
    }

    /** Puts the held saddle on; shears take it off again, as on any mob. */
    public static void saddle(Player player, Mob animal, ItemStack held) {
        animal.setItemSlot(EquipmentSlot.SADDLE, held.consumeAndReturn(1, player));
        animal.setGuaranteedDrop(EquipmentSlot.SADDLE);
        animal.level().playSound(null, animal.getX(), animal.getY(), animal.getZ(), SoundEvents.HORSE_SADDLE.value(), SoundSource.NEUTRAL, 0.5F, 1.0F);
    }

    /** The injector, the saddle and the seat come before the animal's own click, which would only make it sit. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Mob animal)) {
            return;
        }
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        InteractionResult result = InteractionResult.PASS;
        boolean server = !event.getLevel().isClientSide();
        if (held.getItem() instanceof ShimmerInjectorItem) {
            result = ShimmerInjectorItem.shoot(held, player, animal);
        } else if (saddles(player, animal, held)) {
            if (server) {
                saddle(player, animal, held);
            }
            result = InteractionResult.SUCCESS;
        } else if (event.getHand() == InteractionHand.MAIN_HAND && mounts(player, animal, held)) {
            if (server) {
                mount(player, animal);
            }
            result = InteractionResult.SUCCESS;
        }
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }
}
