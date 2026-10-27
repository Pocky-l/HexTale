package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * Aiming an Impulse Plate: right click the plate with an empty hand, then right click the block it should throw to
 * (the next right click on a block is taken by the plate, whatever is in hand). Right clicking the plate again while
 * aiming clears its target. Both sides keep the aim, so the client can draw the arc to the block under the crosshair
 * while the player chooses.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ImpulseAim {
    /** How long a player may take to choose. */
    private static final int TIMEOUT = 600;
    private static final Map<UUID, Aim> CLIENT = new ConcurrentHashMap<>();
    private static final Map<UUID, Aim> SERVER = new ConcurrentHashMap<>();

    /** A plate being aimed, until the given game time. */
    public record Aim(BlockPos plate, long until) {
    }

    private ImpulseAim() {
    }

    private static Map<UUID, Aim> aims(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }

    /** The plate {@code player} is aiming now, if any. */
    @Nullable
    public static BlockPos aiming(Player player) {
        Aim aim = aims(player.level()).get(player.getUUID());
        if (aim == null) {
            return null;
        }
        if (aim.until() < player.level().getGameTime() || aim.plate().distToCenterSqr(player.position()) > 96 * 96
                || !(player.level().getBlockEntity(aim.plate()) instanceof ImpulsePlateBlock.Plate)) {
            aims(player.level()).remove(player.getUUID());
            return null;
        }
        return aim.plate();
    }

    /** A right click on the plate with an empty hand: start choosing, or (while choosing) clear its target. */
    public static void toggle(Level level, BlockPos plate, Player player) {
        BlockPos current = aiming(player);
        if (plate.equals(current)) {
            aims(level).remove(player.getUUID());
            if (!level.isClientSide() && level.getBlockEntity(plate) instanceof ImpulsePlateBlock.Plate found) {
                found.setTarget(null);
                player.sendOverlayMessage(ImpulsePlateBlock.message("cleared"));
                level.playSound(null, plate, ModSounds.PYLON_UNLINK.get(), SoundSource.BLOCKS, 0.5F, 1.2F);
            }
            return;
        }
        aims(level).put(player.getUUID(), new Aim(plate, level.getGameTime() + TIMEOUT));
        if (!level.isClientSide()) {
            player.sendOverlayMessage(ImpulsePlateBlock.message("aim"));
            level.playSound(null, plate, ModSounds.LOUPE_LENS.get(), SoundSource.BLOCKS, 0.5F, 1.3F);
        }
    }

    /** While aiming, the next block right clicked becomes the target (it is not used or built on). */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        BlockPos plate = aiming(player);
        if (plate == null || event.getPos().equals(plate)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Level level = player.level();
        if (level.isClientSide() || !(level.getBlockEntity(plate) instanceof ImpulsePlateBlock.Plate found)) {
            if (level.isClientSide() && level.getBlockEntity(plate) instanceof ImpulsePlateBlock.Plate found && found.inRange(event.getPos())) {
                aims(level).remove(player.getUUID());
            }
            return;
        }
        BlockPos target = event.getPos();
        if (!found.inRange(target)) {
            player.sendOverlayMessage(ImpulsePlateBlock.message("too_far", found.range()));
            return;
        }
        aims(level).remove(player.getUUID());
        found.setTarget(target);
        player.sendOverlayMessage(ImpulsePlateBlock.message("aimed"));
        if (level instanceof ServerLevel server) {
            Vec3 landing = ImpulseArc.landing(level, target);
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), landing.x, landing.y + 0.05, landing.z, 0, 0.8, 1, 0, 1);
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), landing.x, landing.y + 0.2, landing.z, 10, 0.3, 0.1, 0.3, 0.05);
            server.playSound(null, target, ModSounds.PYLON_LINK.get(), SoundSource.BLOCKS, 0.6F, 1.3F);
            server.playSound(null, plate, ModSounds.PYLON_LINK.get(), SoundSource.BLOCKS, 0.4F, 1.5F);
        }
    }
}
