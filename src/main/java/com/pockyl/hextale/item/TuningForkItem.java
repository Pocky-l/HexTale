package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTags;
import com.pockyl.hextale.runework.ForkRune;
import com.pockyl.hextale.runework.PickaxeEffects;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * The Hextech Tuning Fork: held up and listened to (hold right click), it hums louder and higher the nearer what it
 * listens for, and a thread of light runs from it towards that. Untuned it hears hextech - dormant hextech, woken
 * crystals, budding amethyst ({@code hextale:resonant}); struck against a block (sneak + right click) it listens for
 * that block instead, and sneak + right click into the air untunes it. Its runes make it hear farther and more.
 */
public class TuningForkItem extends Item {
    /** Ticks between two listens while it is held up. */
    public static final int LISTEN_TICKS = 10;

    public TuningForkItem(Properties properties) {
        super(properties);
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            if (stack.has(ModDataComponents.TUNED_BLOCK.get())) {
                stack.remove(ModDataComponents.TUNED_BLOCK.get());
                player.displayClientMessage(Component.translatable("item.hextale.tuning_fork.untuned"), true);
                level.playSound(null, player.blockPosition(), ModSounds.LAMP_FLICKER.get(), SoundSource.PLAYERS, 0.6F, 0.7F);
            }
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    /** Sneak + right click on a block: the fork is struck against it and from now on listens for that block. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        context.getItemInHand().set(ModDataComponents.TUNED_BLOCK.get(), block);
        player.displayClientMessage(Component.translatable("item.hextale.tuning_fork.tuned", block.getName()), true);
        if (level instanceof ServerLevel server) {
            Vec3 at = context.getClickLocation();
            server.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y, at.z, 1, 0, 0, 0, 0);
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 8, 0.15, 0.15, 0.15, 0.05);
            server.playSound(null, pos, ModSounds.LAMP_FLICKER.get(), SoundSource.PLAYERS, 0.8F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        if (!(level instanceof ServerLevel server) || !(user instanceof ServerPlayer player) || ticksRemaining % LISTEN_TICKS != 0) {
            return;
        }
        listen(server, player, stack);
    }

    /** One listen: the nearest things it hears, a hum for the nearest and a thread of light to each. */
    public static List<BlockPos> listen(ServerLevel level, ServerPlayer player, ItemStack stack) {
        int reach = ForkRune.reach(strength(stack, ForkRune.SHIFT));
        int voices = strength(stack, ForkRune.SIGNAL) > 0 ? at(ForkRune.VOICES, strength(stack, ForkRune.SIGNAL)) : 1;
        Vec3 ear = player.getEyePosition();
        List<BlockPos> heard = hear(level, BlockPos.containing(ear), reach, listensFor(stack), voices, oreReach(stack, reach));
        if (heard.isEmpty()) {
            player.displayClientMessage(Component.translatable("item.hextale.tuning_fork.silence").withStyle(ChatFormatting.GRAY), true);
            return heard;
        }
        double nearest = Math.sqrt(heard.getFirst().distToCenterSqr(ear));
        float closeness = Mth.clamp(1.0F - (float) (nearest / reach), 0.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RITUAL_HUM.get(), SoundSource.PLAYERS,
                0.25F + 0.6F * closeness, 0.6F + 1.1F * closeness);
        int glow = strength(stack, ForkRune.INSIGHT);
        for (BlockPos pos : heard) {
            thread(level, ear, Vec3.atCenterOf(pos));
            if (glow > 0) {
                PickaxeEffects.glow(level, pos, level.getBlockState(pos), at(ForkRune.GLOW_TICKS, glow));
            }
        }
        int bars = 1 + Math.round(closeness * 4);
        player.displayClientMessage(Component.translatable("item.hextale.tuning_fork.echo",
                "▮".repeat(bars) + "▯".repeat(5 - bars), level.getBlockState(heard.getFirst()).getBlock().getName())
                .withColor(0x8FD6FF), true);
        return heard;
    }

    /**
     * The nearest blocks within {@code reach} the fork hears, nearest first, at most {@code voices}. Ores count too
     * within {@code oreReach} (0 = not at all). Walks only the chunk sections that may hold such a block.
     */
    public static List<BlockPos> hear(Level level, BlockPos ear, int reach, Predicate<BlockState> target, int voices, int oreReach) {
        Predicate<BlockState> ores = state -> state.is(Tags.Blocks.ORES);
        Predicate<BlockState> any = oreReach > 0 ? target.or(ores) : target;
        List<BlockPos> found = new ArrayList<>();
        int reachSqr = reach * reach;
        int oreSqr = oreReach * oreReach;
        for (int cx = SectionPos.blockToSectionCoord(ear.getX() - reach); cx <= SectionPos.blockToSectionCoord(ear.getX() + reach); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(ear.getZ() - reach); cz <= SectionPos.blockToSectionCoord(ear.getZ() + reach); cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (int sy = Math.max(level.getMinSection(), SectionPos.blockToSectionCoord(ear.getY() - reach));
                     sy <= Math.min((level.getMaxSection() - 1), SectionPos.blockToSectionCoord(ear.getY() + reach)); sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    if (section.hasOnlyAir() || !section.maybeHas(any)) {
                        continue;
                    }
                    for (int i = 0; i < 4096; i++) {
                        int x = i & 15;
                        int z = (i >> 4) & 15;
                        int y = i >> 8;
                        BlockState state = section.getBlockState(x, y, z);
                        if (!any.test(state)) {
                            continue;
                        }
                        BlockPos pos = new BlockPos(SectionPos.sectionToBlockCoord(cx) + x, SectionPos.sectionToBlockCoord(sy) + y,
                                SectionPos.sectionToBlockCoord(cz) + z);
                        double distance = pos.distSqr(ear);
                        if (distance <= reachSqr && (target.test(state) || distance <= oreSqr)) {
                            found.add(pos);
                        }
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(ear)));
        return found.size() > voices ? new ArrayList<>(found.subList(0, voices)) : found;
    }

    /** A short thread of light from the fork towards what it heard. */
    private static void thread(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        if (length < 0.5) {
            return;
        }
        Vec3 step = way.scale(1.0 / length);
        for (double d = 0.8; d < Math.min(3.5, length); d += 0.45) {
            Vec3 at = from.add(step.scale(d)).add(0, -0.25, 0);
            level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    /** What the fork listens for: the block it was struck against, else hextech. */
    public static Predicate<BlockState> listensFor(ItemStack stack) {
        Block tuned = stack.get(ModDataComponents.TUNED_BLOCK.get());
        TagKey<Block> resonant = ModTags.RESONANT;
        return tuned != null ? state -> state.is(tuned) : state -> state.is(resonant);
    }

    /** Within how many blocks an untuned fork hears ores too (Gemstone). */
    private static int oreReach(ItemStack stack, int reach) {
        int gemstone = strength(stack, ForkRune.GEMSTONE);
        return gemstone > 0 && !stack.has(ModDataComponents.TUNED_BLOCK.get()) ? Math.round(reach * at(ForkRune.ORE_SHARE, gemstone)) : 0;
    }

    private static int strength(ItemStack stack, ForkRune rune) {
        return RuneHolder.FORK.runes(stack).strength(rune.rune());
    }

    private static int at(int[] table, int strength) {
        return table[Mth.clamp(strength, 1, 4) - 1];
    }

    private static float at(float[] table, int strength) {
        return table[Mth.clamp(strength, 1, 4) - 1];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        @Nullable Block tuned = stack.get(ModDataComponents.TUNED_BLOCK.get());
        tooltip.accept((tuned != null ? Component.translatable("item.hextale.tuning_fork.tuned_to", tuned.getName())
                : Component.translatable("item.hextale.tuning_fork.untuned_line")).withColor(0x8FD6FF));
        tooltip.accept(Component.translatable("item.hextale.hextech_pickaxe.slots", RuneHolder.FORK.runes(stack).count(), ForkRune.SLOTS)
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.tuning_fork.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.tuning_fork.controls").withStyle(ChatFormatting.DARK_AQUA));
    }
}
