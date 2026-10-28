package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.RecallRune;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

/**
 * The Recall Charm: a pocket watch with a hextech face. Used, it puts its holder back where they stood a few seconds
 * ago (same dimension), then rests. The server keeps every player's trail - one mark every {@link #MARK_TICKS} ticks,
 * the last {@link #KEPT} marks. Takes runes (Shift, Haste, Mending, Lens).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public class RecallCharmItem extends Item {
    public static final int MARK_TICKS = 10;
    /** Marks kept: enough for the longest reach back (24 seconds) and a little more. */
    public static final int KEPT = 60;
    private static final Map<UUID, Deque<Mark>> TRAILS = new ConcurrentHashMap<>();

    /** Where a player stood at a moment. */
    public record Mark(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch) {
    }

    public RecallCharmItem(Properties properties) {
        super(properties);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % MARK_TICKS == 0) {
            mark(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRAILS.remove(event.getEntity().getUUID());
    }

    /** Adds the player's current place to their trail. */
    public static void mark(Player player) {
        Deque<Mark> trail = TRAILS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        synchronized (trail) {
            trail.addLast(new Mark(player.level().dimension(), player.position(), player.getYRot(), player.getXRot()));
            while (trail.size() > KEPT) {
                trail.removeFirst();
            }
        }
    }

    /** Where the player stood {@code seconds} back (or the oldest mark), in this dimension; null when nothing is kept. */
    @Nullable
    public static Mark back(Player player, int seconds) {
        Deque<Mark> trail = TRAILS.get(player.getUUID());
        if (trail == null) {
            return null;
        }
        synchronized (trail) {
            int steps = seconds * 20 / MARK_TICKS;
            Mark found = null;
            int i = 0;
            for (var it = trail.descendingIterator(); it.hasNext() && i <= steps; i++) {
                Mark mark = it.next();
                if (!mark.dimension().equals(player.level().dimension())) {
                    break;
                }
                found = mark;
            }
            return found;
        }
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        var runes = RuneHolder.RECALL.runes(stack);
        Mark mark = back(player, RecallRune.seconds(runes.strength(RecallRune.SHIFT.rune())));
        if (mark == null) {
            player.displayClientMessage(Component.translatable("item.hextale.recall_charm.nowhere"), true);
            return InteractionResult.FAIL;
        }
        Vec3 from = player.position();
        serverPlayer.teleportTo(server, mark.pos().x, mark.pos().y, mark.pos().z, java.util.Set.of(), mark.yaw(), mark.pitch());
        player.resetFallDistance();
        int mending = runes.strength(RecallRune.MENDING.rune());
        if (mending > 0) {
            player.heal(RecallRune.MEND[Math.min(mending, 4) - 1]);
        }
        for (Vec3 at : new Vec3[] {from, mark.pos()}) {
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y + 0.1, at.z, 0, 1.2, 1, 0, 1);
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y + 1, at.z, 14, 0.3, 0.6, 0.3, 0.03);
        }
        server.playSound(null, mark.pos().x, mark.pos().y, mark.pos().z, ModSounds.SKYPACK_IGNITE.get(), SoundSource.PLAYERS, 0.7F, 0.7F);
        player.getCooldowns().addCooldown(stack.getItem(), RecallRune.rest(runes.strength(RecallRune.HASTE.rune())));
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.RECALL.runes(stack).count(), RecallRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.recall_charm.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.recall_charm.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
