package com.pockyl.hextale.world;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModAttachments;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Asks the world's protection before the mod changes a block that is not its own device's state: a pickaxe's area
 * mining, a Dust Charge, a sawmill felling a tree, the blight taking the ground... Claim mods (FTB Chunks, Open Parties
 * and Claims) listen to NeoForge's {@link BreakBlockEvent} and {@link BlockEvent.EntityPlaceEvent}; posting them as the
 * player responsible lets them refuse. A refused change is simply not made.
 *
 * <p>Who is responsible:
 * <ul>
 * <li>a real player for what they do with a tool or a thrown gadget ({@link #actor(ServerLevel, Entity, UUID)});</li>
 * <li>for a placed device, the player who placed it, as a fake player with that player's UUID ({@link #owner(ServerLevel,
 * BlockEntity)}); every block entity of the mod remembers its placer in the attachment {@code hextale:placed_by}, and
 * a device placed before it existed is adopted by the first player who uses it;</li>
 * <li>for the world's own doings with no player (the blight, lightning) the fake player {@link #PROFILE} "[HexTale]",
 * which server owners can allow or deny by its UUID in their claim mod.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class WorldEdits {
    /** The name every fake player of the mod goes by. */
    public static final String NAME = "[HexTale]";
    /** The world's own doings: the blight, lightning, a device nobody is known to own. */
    public static final GameProfile PROFILE = new GameProfile(UUID.fromString("2030168d-9eab-3b8b-b353-18a58e583390"), NAME);
    /** Ticks a device remembers a refused spot before it asks again. */
    public static final int REMEMBER_REFUSAL = 1200;

    private WorldEdits() {
    }

    // ---- Who answers for a change ------------------------------------------------------------------------------------

    /** The fake player the world's own doings act as. */
    public static FakePlayer world(ServerLevel level) {
        return FakePlayerFactory.get(level, PROFILE);
    }

    /** A fake player acting for the player with {@code owner}'s UUID (the world's own when nobody is known). */
    public static FakePlayer owner(ServerLevel level, @Nullable UUID owner) {
        return owner == null || owner.equals(PROFILE.id()) ? world(level) : FakePlayerFactory.get(level, new GameProfile(owner, NAME));
    }

    /** The fake player a placed device acts as: its placer, or the world's own. */
    public static FakePlayer owner(ServerLevel level, @Nullable BlockEntity device) {
        return owner(level, device == null ? null : placer(device));
    }

    /**
     * Who answers for what a thrown thing does: the player who threw it when it is a player (even gone offline: a fake
     * player with their UUID), else the world's own.
     */
    public static Player actor(ServerLevel level, @Nullable Entity entity, @Nullable UUID fallback) {
        if (entity instanceof Player player) {
            return player;
        }
        return owner(level, entity == null ? fallback : null);
    }

    // ---- Placers of devices -------------------------------------------------------------------------------------------

    /** The player who placed the device (or adopted it), or null when nobody is known. */
    public static @Nullable UUID placer(BlockEntity device) {
        UUID placer = device.getData(ModAttachments.PLACED_BY);
        return placer.equals(ModAttachments.NOBODY) ? null : placer;
    }

    public static void setPlacer(BlockEntity device, UUID placer) {
        device.setData(ModAttachments.PLACED_BY, placer);
        device.setChanged();
    }

    /** Remembers who placed a block of the mod with a block entity (after every other listener had its say). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && event.getLevel() instanceof ServerLevel level) {
            adopt(level, event.getPos(), player, true);
        }
    }

    /** A device placed before placers were remembered is adopted by the first player allowed to use it. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel() instanceof ServerLevel level && !(event.getEntity() instanceof FakePlayer)) {
            adopt(level, event.getPos(), event.getEntity(), false);
        }
    }

    private static void adopt(ServerLevel level, BlockPos pos, Player player, boolean replace) {
        BlockEntity device = level.getBlockEntity(pos);
        if (device == null || !Hextale.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(device.getBlockState().getBlock()).getNamespace())) {
            return;
        }
        if (replace || placer(device) == null) {
            setPlacer(device, player.getUUID());
        }
    }

    // ---- Asking -------------------------------------------------------------------------------------------------------

    /** Whether {@code actor} may break (or replace) the block at {@code pos}. */
    public static boolean mayBreak(ServerLevel level, BlockPos pos, Player actor) {
        BlockState state = level.getBlockState(pos);
        if (!(actor instanceof FakePlayer) && actor instanceof ServerPlayer player
                && (player.blockActionRestricted(level, pos, player.gameMode()) || !level.mayInteract(player, pos))) {
            return false;
        }
        return !NeoForge.EVENT_BUS.post(new BreakBlockEvent(level, pos, state, actor)).isCanceled();
    }

    /** Whether {@code actor} may put {@code placed} into the spot at {@code pos} (asked before it is placed). */
    public static boolean mayPlace(ServerLevel level, BlockPos pos, BlockState placed, Player actor) {
        if (!(actor instanceof FakePlayer) && actor instanceof ServerPlayer player
                && (!player.mayBuild() || !level.mayInteract(player, pos))) {
            return false;
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        return !NeoForge.EVENT_BUS.post(new BlockEvent.EntityPlaceEvent(snapshot, level.getBlockState(pos.below()), actor)).isCanceled();
    }

    /**
     * Whether {@code actor} may turn the block at {@code pos} into {@code into}: a break when something solid goes (or
     * anything turns to air), a placement when something new appears.
     */
    public static boolean mayChange(ServerLevel level, BlockPos pos, BlockState into, Player actor) {
        BlockState old = level.getBlockState(pos);
        if (!old.isAir() && (into.isAir() || !old.canBeReplaced()) && !mayBreak(level, pos, actor)) {
            return false;
        }
        return into.isAir() || mayPlace(level, pos, into, actor);
    }

    // ---- Doing --------------------------------------------------------------------------------------------------------

    /** Sets {@code into} at {@code pos} if {@code actor} may; returns whether it did. */
    public static boolean change(ServerLevel level, BlockPos pos, BlockState into, int flags, Player actor) {
        if (!mayChange(level, pos, into, actor)) {
            return false;
        }
        level.setBlock(pos, into, flags);
        return true;
    }

    /** Destroys the block at {@code pos} (with its drops or not) if {@code actor} may; returns whether it did. */
    public static boolean destroy(ServerLevel level, BlockPos pos, boolean drops, Player actor) {
        return mayBreak(level, pos, actor) && level.destroyBlock(pos, drops, actor instanceof FakePlayer ? null : actor);
    }

    /**
     * Spots a device was refused lately, so it does not pick them again and again (a reaper's nearest crop behind a claim
     * border). Not saved: after a restart the device simply asks once more.
     */
    public static final class Refusals {
        private static final int MAX = 4096;
        private final Long2LongOpenHashMap until = new Long2LongOpenHashMap();

        /** Whether {@code pos} was refused within the last {@link #REMEMBER_REFUSAL} ticks. */
        public boolean refused(ServerLevel level, BlockPos pos) {
            long at = until.get(pos.asLong());
            if (at == 0) {
                return false;
            }
            if (at <= level.getGameTime()) {
                until.remove(pos.asLong());
                return false;
            }
            return true;
        }

        public void refuse(ServerLevel level, BlockPos pos) {
            if (until.size() >= MAX) {
                until.clear();
            }
            until.put(pos.asLong(), level.getGameTime() + REMEMBER_REFUSAL);
        }

        /** Whether {@code actor} may break the block at {@code pos}, remembering a refusal; refused spots are not asked again. */
        public boolean mayBreak(ServerLevel level, BlockPos pos, Player actor) {
            if (refused(level, pos)) {
                return false;
            }
            if (WorldEdits.mayBreak(level, pos, actor)) {
                return true;
            }
            refuse(level, pos);
            return false;
        }
    }
}
