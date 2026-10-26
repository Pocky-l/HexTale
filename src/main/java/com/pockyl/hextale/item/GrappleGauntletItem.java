package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ImpulsePlateBlock;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.GrappleRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The Grapple Gauntlet: a leather glove with a hextech barrel. Used, it throws a tendril of light at the block looked at
 * (within reach) and draws its wearer to it. The pull runs on the side that moves the player (their client, or the
 * server for players it moves) - both sides start it from the same look; it ends near the hook, after
 * {@link #MAX_TICKS}, or when the player sneaks. Takes runes (Shift, Motor, Breath, Lens).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public class GrappleGauntletItem extends Item {
    /** Longest pull. */
    public static final int MAX_TICKS = 60;
    /** Rest after a throw. */
    public static final int REST = 20;
    /** Close enough to the hook to let go. */
    public static final double ARRIVED = 1.4;
    private static final Map<UUID, Pull> CLIENT_PULLS = new ConcurrentHashMap<>();
    private static final Map<UUID, Pull> SERVER_PULLS = new ConcurrentHashMap<>();

    /** A pull under way: towards {@code hook}, at {@code speed}, started on tick {@code start}. */
    public record Pull(Vec3 hook, float speed, long start, boolean breath) {
    }

    public GrappleGauntletItem(Properties properties) {
        super(properties);
    }

    private static Map<UUID, Pull> pulls(Level level) {
        return level.isClientSide() ? CLIENT_PULLS : SERVER_PULLS;
    }

    /** The block point the tendril catches within reach of {@code player}'s look, or null. */
    public static Vec3 aim(Player player, int reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        PickaxeRunes runes = RuneHolder.GRAPPLE.runes(stack);
        Vec3 hook = aim(player, GrappleRune.reach(runes.strength(GrappleRune.SHIFT.rune())));
        if (hook == null) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.hextale.grapple_gauntlet.nothing"));
            }
            return InteractionResult.FAIL;
        }
        start(player, hook, runes);
        if (level instanceof ServerLevel server) {
            Vec3 hand0 = player.getEyePosition().add(0, -0.3, 0);
            int steps = (int) Math.ceil(hand0.distanceTo(hook) * 2);
            for (int i = 1; i <= steps; i++) {
                Vec3 at = hand0.lerp(hook, i / (double) steps);
                server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0);
            }
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), hook.x, hook.y, hook.z, 0, 0.6, 0, 0, 1);
            server.playSound(null, player.blockPosition(), ModSounds.PYLON_SEND.get(), SoundSource.PLAYERS, 0.7F, 1.2F);
            player.getCooldowns().addCooldown(stack, REST);
        }
        return InteractionResult.SUCCESS;
    }

    /** Starts drawing {@code player} to {@code hook} on this side. */
    public static void start(Player player, Vec3 hook, PickaxeRunes runes) {
        pulls(player.level()).put(player.getUUID(), new Pull(hook, GrappleRune.speed(runes.strength(GrappleRune.MOTOR.rune())),
                player.level().getGameTime(), runes.strength(GrappleRune.BREATH.rune()) > 0));
    }

    public static boolean pulling(Player player) {
        return pulls(player.level()).containsKey(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        step(event.getEntity());
    }

    /** One tick of {@code player}'s pull, if any; returns whether it still goes on. */
    public static boolean step(Player player) {
        Map<UUID, Pull> pulls = pulls(player.level());
        Pull pull = pulls.get(player.getUUID());
        if (pull == null) {
            return false;
        }
        long now = player.level().getGameTime();
        Vec3 way = pull.hook().subtract(player.position().add(0, 0.9, 0));
        boolean done = way.length() < ARRIVED || now - pull.start() > MAX_TICKS || player.isShiftKeyDown() || player.isSpectator();
        if (done) {
            pulls.remove(player.getUUID());
            if (!player.level().isClientSide()) {
                if (pull.breath()) {
                    ImpulsePlateBlock.SOFT.put(player.getUUID(), now + 200);
                }
                player.level().playSound(null, player.blockPosition(), ModSounds.PYLON_ARRIVE.get(), SoundSource.PLAYERS, 0.5F, 1.3F);
            }
            return false;
        }
        player.resetFallDistance();
        if (player.canSimulateMovement()) {
            player.setDeltaMovement(way.normalize().scale(Math.min(pull.speed(), way.length())));
            player.needsSync = true;
        }
        if (player.level().isClientSide() && now % 2 == 0) {
            // The tendril between the glove and the hook, a few motes along it each other tick.
            Vec3 from = player.getEyePosition().add(0, -0.4, 0);
            for (int i = 0; i < 4; i++) {
                Vec3 at = from.lerp(pull.hook(), player.getRandom().nextDouble());
                player.level().addParticle(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 0, 0, 0);
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SERVER_PULLS.remove(event.getEntity().getUUID());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.GRAPPLE.runes(stack).count(), GrappleRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.grapple_gauntlet.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.grapple_gauntlet.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
