package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
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
 * the last {@link #KEPT} marks. Takes runes (Shift, Haste, Mending).
 *
 * <p>Each return spends one of its {@value #WINDINGS} windings; pressed to a jar it drinks {@value #MB_PER_WINDING} mB
 * of rift essence per winding back ({@link #wind}). A new charm comes fully wound.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public class RecallCharmItem extends Item {
    public static final int MARK_TICKS = 10;
    /** Marks kept: enough for the longest reach back (24 seconds) and a little more. */
    public static final int KEPT = 60;
    private static final Map<UUID, Deque<Mark>> TRAILS = new ConcurrentHashMap<>();
    public static final int WINDINGS = 6;
    public static final int MB_PER_WINDING = 4;

    /** Where a player stood at a moment. */
    public record Mark(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch) {
    }

    public RecallCharmItem(Properties properties) {
        super(properties);
    }

    public static boolean is(ItemStack stack) {
        return stack.is(ModItems.RECALL_CHARM.get());
    }

    /** Windings left. */
    public static int windings(ItemStack stack) {
        return Math.max(0, WINDINGS - stack.getOrDefault(ModDataComponents.RECALL_SPENT.get(), 0));
    }

    public static void setWindings(ItemStack stack, int windings) {
        int spent = WINDINGS - Mth.clamp(windings, 0, WINDINGS);
        if (spent == 0) {
            stack.remove(ModDataComponents.RECALL_SPENT.get());
        } else {
            stack.set(ModDataComponents.RECALL_SPENT.get(), spent);
        }
    }

    /**
     * Winds the charm from a jar: {@value #MB_PER_WINDING} mB of rift essence per spent winding, as many as the jar can
     * give (a creative jar gives freely). Motes of rift essence flow from the lid to {@code to}. Returns the windings
     * put back (0: already wound, or no rift essence in the jar).
     */
    public static int wind(ServerLevel level, EssenceTankBlockEntity jar, ItemStack charm, Vec3 to) {
        int wound = Math.min(WINDINGS - windings(charm), jar.tank().contents().get(Essence.RIFT) / MB_PER_WINDING);
        if (wound <= 0) {
            return 0;
        }
        if (!jar.isCreative()) {
            jar.tank().remove(EssenceMix.pure(Essence.RIFT, wound * MB_PER_WINDING));
            jar.setChanged();
        }
        setWindings(charm, windings(charm) + wound);
        Vec3 mouth = JarReach.mouth(jar.getBlockPos());
        Vec3 offset = mouth.subtract(to);
        for (int i = 0; i < 4 + wound * 2; i++) {
            level.sendParticles(ModParticles.flow(Essence.RIFT.color()), to.x, to.y, to.z, 0, offset.x, offset.y, offset.z, 1);
        }
        level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 0.9F);
        return wound;
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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (windings(stack) <= 0 && !player.hasInfiniteMaterials()) {
            player.sendOverlayMessage(Component.translatable("item.hextale.recall_charm.unwound"));
            return InteractionResult.FAIL;
        }
        var runes = RuneHolder.RECALL.runes(stack);
        Mark mark = back(player, RecallRune.seconds(runes.strength(RecallRune.SHIFT.rune())));
        if (mark == null) {
            player.sendOverlayMessage(Component.translatable("item.hextale.recall_charm.nowhere"));
            return InteractionResult.FAIL;
        }
        Vec3 from = player.position();
        serverPlayer.teleportTo(server, mark.pos().x, mark.pos().y, mark.pos().z, java.util.Set.of(), mark.yaw(), mark.pitch(), true);
        player.resetFallDistance();
        int mending = runes.strength(RecallRune.MENDING.rune());
        if (mending > 0) {
            player.heal(RecallRune.MEND[Math.min(mending, 4) - 1]);
        }
        int fireproofing = runes.strength(RecallRune.FIREPROOFING.rune());
        if (fireproofing > 0) {
            player.clearFire();
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE,
                    com.pockyl.hextale.runework.HolderRune.at(RecallRune.FIRE_TICKS, fireproofing), 0, true, false, true));
            com.pockyl.hextale.runework.ToolEffects.puff(server, mark.pos().add(0, 1, 0), RecallRune.FIREPROOFING.rune(), 8, 0.4);
        }
        for (Vec3 at : new Vec3[] {from, mark.pos()}) {
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y + 0.1, at.z, 0, 1.2, 1, 0, 1);
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y + 1, at.z, 14, 0.3, 0.6, 0.3, 0.03);
        }
        server.playSound(null, mark.pos().x, mark.pos().y, mark.pos().z, ModSounds.SKYPACK_IGNITE.get(), SoundSource.PLAYERS, 0.7F, 0.7F);
        player.getCooldowns().addCooldown(stack, RecallRune.rest(runes.strength(RecallRune.HASTE.rune())));
        int dam = runes.strength(RecallRune.DAM.rune());
        if (dam > 0 && server.getRandom().nextFloat() < com.pockyl.hextale.runework.HolderRune.at(RecallRune.FREE, dam)) {
            // Dam: this turn back cost nothing.
            com.pockyl.hextale.runework.ToolEffects.puff(server, mark.pos().add(0, 1.2, 0), RecallRune.DAM.rune(), 6, 0.3);
            server.playSound(null, mark.pos().x, mark.pos().y, mark.pos().z, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
        } else if (!player.hasInfiniteMaterials()) {
            setWindings(stack, windings(stack) - 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return windings(stack) < WINDINGS;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * windings(stack) / WINDINGS);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Essence.RIFT.color();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.RECALL.runes(stack).count(), RecallRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.recall_charm.windings", windings(stack), WINDINGS).withColor(Essence.RIFT.color()));
        tooltip.accept(Component.translatable("item.hextale.recall_charm.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.recall_charm.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
