package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.ImpulsePlateBlock;
import com.pockyl.hextale.entity.GrappleHook;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.GrappleRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

/**
 * The Grapple Gauntlet: a leather glove with a hextech barrel. Press use and it throws its claw ({@link GrappleHook})
 * at what you look at; keep it pressed and you hang on a real rope: it only pulls when stretched, so you fall, swing
 * through the bottom and up the other side under gravity. Jump reels in, sneak lets out, and letting go of the button
 * frees you with all the speed you had - a fling; throw again mid-air to chain swings.
 *
 * <p>The rope works on the side that moves the wearer ({@link net.minecraft.world.entity.Entity#canSimulateMovement()}:
 * their own client) in {@link #rope}; the server only keeps the falls from hurting while the rope bears the weight and
 * tells the flying check that a hanging player is not cheating. Takes runes (Shift, Motor, Breath, Gravity,
 * Blades).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public class GrappleGauntletItem extends Item {
    /** Rest after letting go before the claw can be thrown again. */
    public static final int REST = 4;
    /** The fastest a rope moves its wearer, blocks a tick. */
    private static final double MAX_SPEED = 3.0;
    /** Blocks a tick the rope pays out while sneaking. */
    private static final double LET_OUT = 0.3;
    /** Air drag while swinging: almost none, so a swing keeps going (vanilla drag would stop it in one arc). */
    private static final AttributeModifier SWING = new AttributeModifier(Hextale.id("grapple_swing"), -0.92,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    /** Air drag after letting go mid-air, until the ground: lower than usual, so the fling carries. */
    private static final AttributeModifier FLING = new AttributeModifier(Hextale.id("grapple_fling"), -0.6,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    /** Longest fling. */
    private static final int FLING_TICKS = 80;
    /** Flings under way on each side: player to the tick theirs ends. */
    private static final Map<UUID, Long> CLIENT_FLINGS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SERVER_FLINGS = new ConcurrentHashMap<>();
    /** How the wearer works the rope this tick: +1 reel in, -1 let out, 0 hold (the client reads the keys). */
    public static ToIntFunction<Player> reelInput = player -> 0;
    /** Tells the server the rope's length (set by the client). */
    public static BiConsumer<GrappleHook, Double> reportLength = (hook, length) -> {
    };

    public GrappleGauntletItem(Properties properties) {
        super(properties);
    }

    /** Where the rope holds the wearer: a raised hand, a little over the head's middle. */
    public static Vec3 grip(Player player) {
        return player.position().add(0, player.getBbHeight() * 0.8, 0);
    }

    /** Whether {@code player} keeps a gauntlet clenched (use held). */
    public static boolean holding(Player player) {
        return player.isUsingItem() && player.getUseItem().getItem() instanceof GrappleGauntletItem;
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (level instanceof ServerLevel server) {
            GrappleHook old = GrappleHook.of(player);
            if (old != null) {
                old.discard();
            }
            PickaxeRunes runes = RuneHolder.GRAPPLE.runes(stack);
            Vec3 from = player.getEyePosition().add(0, -0.25, 0);
            GrappleHook hook = new GrappleHook(server, player, from, player.getLookAngle(),
                    GrappleRune.reach(runes.strength(GrappleRune.SHIFT.rune())), GrappleRune.reel(runes.strength(GrappleRune.MOTOR.rune())),
                    runes.strength(GrappleRune.BREATH.rune()) > 0, runes.strength(GrappleRune.GRAVITY.rune()),
                    runes.strength(GrappleRune.BLADES.rune()));
            server.addFreshEntity(hook);
            server.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.GRAPPLE_FIRE.get(), SoundSource.PLAYERS, 0.8F,
                    0.95F + player.getRandom().nextFloat() * 0.1F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Released (or the glove left the hand): the claw springs back, and whoever was swinging flies on. */
    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (!(entity instanceof Player player)) {
            return;
        }
        GrappleHook hook = GrappleHook.of(player);
        boolean wasHanging = hook != null && hook.attached();
        if (wasHanging && !player.onGround()) {
            flings(player.level()).put(player.getUUID(), player.level().getGameTime() + FLING_TICKS);
        }
        if (!player.level().isClientSide()) {
            if (hook != null) {
                if (wasHanging && hook.breath()) {
                    ImpulsePlateBlock.SOFT.put(player.getUUID(), player.level().getGameTime() + 200);
                }
                hook.retract();
            }
            player.getCooldowns().addCooldown(stack.getItem(), REST);
        }
    }

    private static Map<UUID, Long> flings(Level level) {
        return level.isClientSide() ? CLIENT_FLINGS : SERVER_FLINGS;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        GrappleHook hook = GrappleHook.of(player);
        boolean hanging = hook != null && hook.attached() && holding(player);
        if (com.pockyl.hextale.port.PortEntities.canSimulateMovement(player)) {
            if (hanging) {
                rope(player, hook, reelInput.applyAsInt(player));
            }
            drag(player, hanging);
        }
        if (hanging && !player.level().isClientSide()) {
            // The rope bears the weight: no flying kick while hanging, and a fall it catches does not hurt.
            if (player instanceof ServerPlayer server && server.connection != null) {
                server.connection.aboveGroundTickCount = 0;
            }
            double length = hook.length();
            if (length > 0 && grip(player).distanceTo(hook.position()) > length - 0.6) {
                player.resetFallDistance();
            }
        }
    }

    /**
     * One tick of the rope, before the wearer moves: if this tick's motion would carry the grip beyond the rope, the
     * motion is bent back onto the sphere of the rope round the claw. What is cut off is only the part along the rope,
     * so gravity turns into a swing and speed across the rope is kept. Reeling shortens the rope and so pulls in.
     */
    public static void rope(Player player, GrappleHook hook, int input) {
        Vec3 anchor = hook.position();
        Vec3 grip = grip(player);
        if (!hook.measured()) {
            hook.setLength(grip.distanceTo(anchor));
            reportLength.accept(hook, hook.length());
        }
        double length = hook.length();
        if (input > 0) {
            length -= hook.reelSpeed();
        } else if (input < 0) {
            length += LET_OUT;
        }
        if (Math.abs(length - hook.length()) > 1.0E-4) {
            double before = hook.length();
            hook.setLength(length);
            if (hook.length() != before) {
                reportLength.accept(hook, hook.length());
                if (player.level().getGameTime() % 4 == 0) {
                    player.level().playSound(player, player.getX(), player.getY() + 1, player.getZ(), ModSounds.GRAPPLE_REEL.get(),
                            SoundSource.PLAYERS, 0.4F, input > 0 ? 1.1F : 0.8F);
                }
            }
        }
        length = hook.length();
        Vec3 motion = player.getDeltaMovement();
        Vec3 out = grip.add(motion).subtract(anchor);
        double reach = out.length();
        if (reach > length && reach > 1.0E-4) {
            Vec3 onRope = anchor.add(out.scale(length / reach));
            Vec3 bent = onRope.subtract(grip);
            if (bent.length() > MAX_SPEED) {
                bent = bent.normalize().scale(MAX_SPEED);
            }
            player.setDeltaMovement(bent);
            player.hurtMarked = true;
        }
    }

    /**
     * Low air drag while swinging and, less so, during a fling after letting go mid-air (until the ground).
     * 1.21.1 has no air drag attribute: the motion is raised before the move by what the lower drag would have kept.
     */
    private static void drag(Player player, boolean hanging) {
        Map<UUID, Long> flings = flings(player.level());
        Long until = flings.get(player.getUUID());
        boolean flying = !hanging && until != null && until > player.level().getGameTime() && !player.onGround() && !player.isInWater();
        if (until != null && !flying) {
            flings.remove(player.getUUID());
        }
        double share = hanging && !player.onGround() ? 1 + SWING.amount() : flying ? 1 + FLING.amount() : 1;
        if (share < 1 && !player.isFallFlying()) {
            Vec3 motion = player.getDeltaMovement();
            double horizontal = (1 - 0.09 * share) / 0.91;
            double vertical = (1 - 0.02 * share) / 0.98;
            player.setDeltaMovement(motion.x * horizontal, motion.y * vertical, motion.z * horizontal);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SERVER_FLINGS.remove(event.getEntity().getUUID());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.GRAPPLE.runes(stack).count(), GrappleRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.grapple_gauntlet.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.grapple_gauntlet.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
