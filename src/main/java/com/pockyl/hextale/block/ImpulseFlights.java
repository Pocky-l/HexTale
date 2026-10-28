package com.pockyl.hextale.block;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Things in the air after an aimed Impulse Plate's throw. Near the target the fall is cushioned (where the thing moves
 * itself: a player's client) so the landing is soft and stays on the target; the server marks the landing with a
 * puff of light and a soft whump, and leaves a short trail behind the flyer.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ImpulseFlights {
    /** The fastest fall once the target is close below. */
    private static final double CUSHION = 0.35;
    private static final Map<UUID, Flight> CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Flight> SERVER = new ConcurrentHashMap<>();

    /** A throw towards {@code landing}, due there at game time {@code due}. */
    public record Flight(Vec3 landing, long start, long due) {
    }

    private ImpulseFlights() {
    }

    private static Map<UUID, Flight> flights(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    public static void start(Level level, Entity entity, Vec3 landing, long due) {
        flights(level).put(entity.getUUID(), new Flight(landing, level.getGameTime(), due));
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        Map<UUID, Flight> flights = flights(entity.level());
        if (flights.isEmpty()) {
            return;
        }
        Flight flight = flights.get(entity.getUUID());
        if (flight == null) {
            return;
        }
        long now = entity.level().getGameTime();
        Vec3 way = flight.landing().subtract(entity.position());
        double sideways = Math.sqrt(way.x * way.x + way.z * way.z);
        boolean landed = entity.onGround() && now > flight.start() + 3 && entity.getDeltaMovement().y <= 0.01;
        if (landed || now > flight.due() + 40 || entity.isRemoved() || entity.isInWater()) {
            flights.remove(entity.getUUID());
            if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(entity) && landed && sideways < 1.5) {
                // Stick the landing on the target.
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.2, 1, 0.2));
            }
            if (entity.level() instanceof ServerLevel server && landed && sideways < 2.5) {
                server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), entity.getX(), entity.getY() + 0.05, entity.getZ(), 0, 0.7, 1, 0, 1);
                server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), entity.getX(), entity.getY() + 0.1, entity.getZ(), 8, 0.3, 0.05, 0.3, 0.05);
                server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.IMPULSE_LAND.get(), SoundSource.NEUTRAL, 0.6F, 1.0F);
            }
            return;
        }
        Vec3 motion = entity.getDeltaMovement();
        if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(entity) && motion.y < -CUSHION && way.y > -3.5 && way.y < 0.5 && sideways < 2.5) {
            entity.setDeltaMovement(motion.x, Math.max(motion.y, -CUSHION), motion.z);
            entity.resetFallDistance();
        }
        if (entity.level() instanceof ServerLevel server && now % 2 == 0) {
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), entity.getX(), entity.getY() + entity.getBbHeight() * 0.5,
                    entity.getZ(), 1, 0.1, 0.1, 0.1, 0);
        }
    }
}
