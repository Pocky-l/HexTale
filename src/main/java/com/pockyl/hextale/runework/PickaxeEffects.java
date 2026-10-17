package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.pockyl.hextale.runework.HextechPickaxe.Ability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What the hextech pickaxes do. Every value is chosen by the pickaxe's stars (1-3, index 0-2): a three-star pickaxe is
 * far stronger than a one-star one of the same kind. Extra blocks a pickaxe takes (area, vein, tunnel) wear it like
 * mined blocks and their drops go through its other abilities (smelting, the pocket) too.
 */
public final class PickaxeEffects {
    private static final float[] SMELT_BONUS = {0F, 0.3F, 0.6F};
    private static final float[] SMELT_XP = {0.5F, 1.0F, 1.5F};
    private static final int[] REVEAL_RADIUS = {8, 12, 16};
    private static final int[] REVEAL_SECONDS = {10, 15, 20};
    private static final int[] REVEAL_COOLDOWN = {200, 140, 80};
    private static final float[] TEMPER = {0.5F, 0.7F, 0.9F};
    private static final float[] HARD_SPEED = {1.5F, 2.0F, 3.0F};
    private static final int[] AREA_RADIUS = {1, 1, 2};
    private static final int[] AREA_DEPTH = {1, 2, 1};
    private static final int[] VEIN = {12, 24, 48};
    private static final float[] RICH = {0.15F, 0.3F, 0.5F};
    private static final float[] GEMS = {0.02F, 0.04F, 0.07F};
    private static final int[] TUNNEL_DEPTH = {6, 10, 16};
    private static final int[] TUNNEL_COOLDOWN = {80, 60, 40};
    private static final int[] QUAKE_RADIUS = {2, 3, 4};
    private static final int[] QUAKE_COOLDOWN = {100, 80, 60};
    private static final int[] MAGNET = {5, 8, 12};
    /** Gem odds out of 100 by stars: amethyst, lapis, emerald, diamond. */
    private static final int[][] GEM_TABLE = {{60, 25, 12, 3}, {45, 25, 20, 10}, {30, 25, 25, 20}};
    /** Blocks harder than this count as hard stone for a tempered pickaxe (deepslate is 3). */
    private static final float HARD = 2.9F;
    /** Tunnels take blocks up to this hardness (ores, deepslate; not obsidian). */
    private static final float TUNNEL_HARDNESS = 5.0F;

    /** Set while a pickaxe breaks extra blocks: their drops must not start another area or vein. */
    private static final ThreadLocal<Boolean> CHAINING = ThreadLocal.withInitial(() -> false);
    /** Ore markers of a prospector's sight, removed when their time is up. */
    private static final List<Expiring> EXPIRING = new ArrayList<>();

    private PickaxeEffects() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PickaxeEffects::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(PickaxeEffects::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(PickaxeEffects::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PickaxeEffects::onServerTick);
    }

    public static float temperChance(int stars) {
        return TEMPER[stars - 1];
    }

    // ---- Description ------------------------------------------------------------------------------------------------

    public static String percent(float value) {
        return Math.round(value * 100) + "%";
    }

    /** A multiplier as it reads: 1.5 -> "×1.5", 2.0 -> "×2". */
    public static String times(float value) {
        return "×" + (value == Math.round(value) ? String.valueOf(Math.round(value)) : String.valueOf(value));
    }

    /** One tooltip line per ability, with the numbers of these stars. */
    public static List<Component> describe(HextechPickaxe kind, int stars) {
        int i = stars - 1;
        List<Component> lines = new ArrayList<>();
        for (Ability ability : kind.abilities()) {
            String key = "item.hextale.hextech_pickaxe.ability." + ability.key();
            Component line = switch (ability) {
                case SMELT -> SMELT_BONUS[i] > 0 ? Component.translatable(key + ".bonus", percent(SMELT_BONUS[i]))
                        : Component.translatable(key);
                case REVEAL -> Component.translatable(key, REVEAL_RADIUS[i], REVEAL_SECONDS[i]);
                case TEMPER -> Component.translatable(key, percent(TEMPER[i]), times(HARD_SPEED[i]));
                case AREA -> Component.translatable(key, AREA_RADIUS[i] * 2 + 1, AREA_DEPTH[i]);
                case VEIN -> Component.translatable(key, VEIN[i]);
                case RICH_ORE -> Component.translatable(key, percent(RICH[i]));
                case GEMS -> Component.translatable(key, percent(GEMS[i]));
                case TUNNEL -> Component.translatable(key, TUNNEL_DEPTH[i]);
                case QUAKE -> Component.translatable(key, QUAKE_RADIUS[i]);
                case MAGNET -> Component.translatable(key, MAGNET[i]);
                case CLEAR_SIGHT, CACHE -> Component.translatable(key);
            };
            lines.add(Component.literal("• ").withStyle(ChatFormatting.DARK_AQUA).append(line.copy().withStyle(ChatFormatting.GRAY)));
        }
        return lines;
    }

    // ---- Digging ----------------------------------------------------------------------------------------------------

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        HextechPickaxe kind = HextechPickaxeItem.kind(tool);
        if (kind == null || !tool.isCorrectToolForDrops(event.getState())) {
            return;
        }
        int stars = HextechPickaxe.stars(tool);
        float speed = event.getNewSpeed() * HextechPickaxe.speed(stars);
        if (kind.has(Ability.TEMPER)) {
            BlockPos pos = event.getPosition().orElse(player.blockPosition());
            if (event.getState().getDestroySpeed(player.level(), pos) > HARD) {
                speed *= HARD_SPEED[stars - 1];
            }
        }
        event.setNewSpeed(speed);
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        HextechPickaxe kind = HextechPickaxeItem.kind(tool);
        if (kind == null || !(event.getBreaker() instanceof Player player)) {
            return;
        }
        ServerLevel level = event.getLevel();
        int i = HextechPickaxe.stars(tool) - 1;
        BlockState state = event.getState();
        boolean ore = state.is(Tags.Blocks.ORES);
        if (!CHAINING.get() && !player.isShiftKeyDown()) {
            if (kind.has(Ability.VEIN) && ore) {
                vein(event, player, tool, VEIN[i]);
            } else if (kind.has(Ability.AREA)) {
                area(event, player, tool, AREA_RADIUS[i], AREA_DEPTH[i]);
            }
        }
        RandomSource random = level.getRandom();
        if (kind.has(Ability.RICH_ORE) && ore) {
            List<ItemEntity> more = new ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                if (random.nextFloat() < RICH[i]) {
                    more.add(drop(level, event.getPos(), drop.getItem().copy()));
                }
            }
            event.getDrops().addAll(more);
        }
        if (kind.has(Ability.GEMS) && state.is(BlockTags.BASE_STONE_OVERWORLD) && random.nextFloat() < GEMS[i]) {
            event.getDrops().add(drop(level, event.getPos(), gem(random, GEM_TABLE[i])));
            burst(level, Vec3.atCenterOf(event.getPos()), ParticleTypes.WAX_ON, 8);
        }
        if (kind.has(Ability.SMELT)) {
            smelt(event, ore, i);
        }
        if (kind.has(Ability.CACHE)) {
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                ItemStack stack = it.next().getItem();
                player.getInventory().add(stack);
                if (stack.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    private static ItemStack gem(RandomSource random, int[] odds) {
        int roll = random.nextInt(100);
        if ((roll -= odds[0]) < 0) {
            return new ItemStack(Items.AMETHYST_SHARD, 1 + random.nextInt(2));
        }
        if ((roll -= odds[1]) < 0) {
            return new ItemStack(Items.LAPIS_LAZULI, 2 + random.nextInt(3));
        }
        return new ItemStack(roll < odds[2] ? Items.EMERALD : Items.DIAMOND);
    }

    /** Every drop with a furnace recipe comes out smelted; ores may give an extra piece; some of the furnace's experience. */
    private static void smelt(BlockDropsEvent event, boolean ore, int i) {
        ServerLevel level = event.getLevel();
        float experience = 0;
        int smelted = 0;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            SingleRecipeInput input = new SingleRecipeInput(stack);
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) {
                continue;
            }
            ItemStack result = recipe.get().value().assemble(input);
            if (result.isEmpty()) {
                continue;
            }
            int count = stack.getCount();
            int out = result.getCount() * count;
            if (ore && level.getRandom().nextFloat() < SMELT_BONUS[i]) {
                out++;
            }
            result.setCount(Math.min(result.getMaxStackSize(), out));
            drop.setItem(result);
            smelted += count;
            experience += recipe.get().value().experience() * count;
        }
        if (smelted <= 0) {
            return;
        }
        float share = experience * SMELT_XP[i];
        int xp = Mth.floor(share);
        if (level.getRandom().nextFloat() < share - xp) {
            xp++;
        }
        event.setDroppedExperience(event.getDroppedExperience() + xp);
        Vec3 at = Vec3.atCenterOf(event.getPos());
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 6, 0.25, 0.25, 0.25, 0.01);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.2, at.z, 3, 0.2, 0.1, 0.2, 0.01);
        level.playSound(null, event.getPos(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.2F, 1.6F + level.getRandom().nextFloat() * 0.3F);
    }

    /** The blocks around the broken one on the face it was struck from: 3x3, two deep, or 5x5. */
    private static void area(BlockDropsEvent event, Player player, ItemStack tool, int radius, int depth) {
        ServerLevel level = event.getLevel();
        HitResult hit = player.pick(6, 0, false);
        Direction face = hit instanceof BlockHitResult block ? block.getDirection() : Direction.UP;
        BlockPos origin = event.getPos();
        float hardness = event.getState().getDestroySpeed(level, origin);
        chain(() -> {
            for (int d = 0; d < depth; d++) {
                for (int a = -radius; a <= radius; a++) {
                    for (int b = -radius; b <= radius; b++) {
                        BlockPos at = offset(origin, face, a, b, d);
                        if (at.equals(origin)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(at);
                        if (state.getDestroySpeed(level, at) <= hardness * 1.5F + 0.5F && !take(level, player, tool, at)) {
                            return;
                        }
                    }
                }
            }
        });
    }

    /** The ore vein the broken block belonged to: the same ore touching it, nearest first. */
    private static void vein(BlockDropsEvent event, Player player, ItemStack tool, int size) {
        ServerLevel level = event.getLevel();
        Block ore = event.getState().getBlock();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(event.getPos());
        queue.add(event.getPos());
        chain(() -> {
            int taken = 0;
            while (!queue.isEmpty() && taken < size) {
                BlockPos at = queue.poll();
                for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                    if (taken >= size || !seen.add(next.immutable()) || !level.getBlockState(next).is(ore)) {
                        continue;
                    }
                    BlockPos pos = next.immutable();
                    if (!take(level, player, tool, pos)) {
                        return;
                    }
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.05);
                    taken++;
                    queue.add(pos);
                }
            }
        });
    }

    private static void chain(Runnable work) {
        CHAINING.set(true);
        try {
            work.run();
        } finally {
            CHAINING.set(false);
        }
    }

    /**
     * Breaks one extra block as the player with the pickaxe (drops go through the pickaxe's abilities) and wears the
     * pickaxe; false when the pickaxe broke. Blocks the pickaxe may not take are skipped.
     */
    private static boolean take(ServerLevel level, Player player, ItemStack tool, BlockPos at) {
        if (tool.isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(at);
        float speed = state.getDestroySpeed(level, at);
        if (state.isAir() || speed < 0 || level.getBlockEntity(at) != null || !state.getFluidState().isEmpty()
                || state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state) || !level.mayInteract(player, at)) {
            return true;
        }
        Block.dropResources(state, level, at, null, player, tool);
        level.destroyBlock(at, false, player);
        tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        return !tool.isEmpty();
    }

    private static BlockPos offset(BlockPos origin, Direction face, int a, int b, int depth) {
        Direction back = face.getOpposite();
        return switch (face.getAxis()) {
            case X -> origin.offset(back.getStepX() * depth, a, b);
            case Y -> origin.offset(a, back.getStepY() * depth, b);
            case Z -> origin.offset(a, b, back.getStepZ() * depth);
        };
    }

    // ---- Right click ------------------------------------------------------------------------------------------------

    /** The pickaxe's active ability; false when nothing happened (no cooldown then). */
    static boolean use(ServerLevel level, Player player, ItemStack tool, HextechPickaxe kind) {
        int i = HextechPickaxe.stars(tool) - 1;
        Ability active = kind.active();
        boolean done;
        int cooldown;
        switch (active) {
            case REVEAL -> {
                done = reveal(level, player, REVEAL_RADIUS[i], REVEAL_SECONDS[i] * 20);
                cooldown = REVEAL_COOLDOWN[i];
            }
            case TUNNEL -> {
                done = tunnel(level, player, tool, TUNNEL_DEPTH[i]);
                cooldown = TUNNEL_COOLDOWN[i];
            }
            case QUAKE -> {
                done = quake(level, player, tool, QUAKE_RADIUS[i]);
                cooldown = QUAKE_COOLDOWN[i];
            }
            default -> {
                return false;
            }
        }
        if (done) {
            player.getCooldowns().addCooldown(tool, cooldown);
        }
        return done;
    }

    /** Ores around glow through the stone for a while (glowing block markers). */
    private static boolean reveal(ServerLevel level, Player player, int radius, int ticks) {
        BlockPos centre = player.blockPosition();
        int shown = 0;
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            if (shown >= 160) {
                break;
            }
            BlockState state = level.getBlockState(at);
            if (!state.is(Tags.Blocks.ORES) || at.distSqr(centre) > radius * radius) {
                continue;
            }
            Entity marker = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
            if (marker == null) {
                break;
            }
            CompoundTag tag = new CompoundTag();
            tag.store("block_state", BlockState.CODEC, state);
            tag.putBoolean("Glowing", true);
            tag.putInt("glow_color_override", 0x7EF0FF);
            marker.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
            // A hair smaller than the block, so the outline hugs it.
            marker.snapTo(at.getX() + 0.01, at.getY() + 0.01, at.getZ() + 0.01, 0, 0);
            level.addFreshEntity(marker);
            EXPIRING.add(new Expiring(level, marker, level.getGameTime() + ticks));
            shown++;
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 24, 1.5, 1, 1.5, 0.05);
        level.playSound(null, centre, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.6F);
        return true;
    }

    /** Bores a 3x3 tunnel straight ahead (where the holder looks, the nearest of the six directions). */
    private static boolean tunnel(ServerLevel level, Player player, ItemStack tool, int depth) {
        Direction into = Direction.orderedByNearest(player)[0];
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(5)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        BlockPos start = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : BlockPos.containing(eye).relative(into);
        if (into.getAxis().isHorizontal() && hit.getType() != HitResult.Type.BLOCK) {
            start = start.below();
        }
        Direction face = into.getOpposite();
        int[] dug = {0};
        BlockPos origin = start;
        chain(() -> {
            for (int d = 0; d < depth; d++) {
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        BlockPos at = offset(origin, face, a, b, d);
                        BlockState state = level.getBlockState(at);
                        if (state.isAir() || state.getDestroySpeed(level, at) > TUNNEL_HARDNESS) {
                            continue;
                        }
                        if (!take(level, player, tool, at)) {
                            return;
                        }
                        if (level.getBlockState(at).isAir()) {
                            dug[0]++;
                            if (dug[0] % 3 == 0) {
                                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.05);
                            }
                        }
                    }
                }
            }
        });
        if (dug[0] == 0) {
            return false;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.6F);
        return true;
    }

    /** Shatters the rock around where the holder looks; ores and everything else stay standing. */
    private static boolean quake(ServerLevel level, Player player, ItemStack tool, int radius) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(6)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos centre = hit.getBlockPos();
        int shattered = 0;
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            if (at.distSqr(centre) > (radius + 0.5) * (radius + 0.5)) {
                continue;
            }
            BlockState state = level.getBlockState(at);
            if (!(state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(Tags.Blocks.COBBLESTONES))
                    || !level.mayInteract(player, at)) {
                continue;
            }
            level.destroyBlock(at, false, player);
            if (++shattered % 4 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.1);
            }
        }
        if (shattered == 0) {
            return false;
        }
        tool.hurtAndBreak(1 + shattered / 8, player, EquipmentSlot.MAINHAND);
        level.sendParticles(ParticleTypes.EXPLOSION, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5, 1, 0, 0, 0, 0);
        level.playSound(null, centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 0.5F);
        level.playSound(null, centre, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
        return true;
    }

    // ---- While held -------------------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 5 != 0) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        HextechPickaxe kind = HextechPickaxeItem.kind(tool);
        if (kind == null) {
            return;
        }
        int i = HextechPickaxe.stars(tool) - 1;
        if (kind.has(Ability.MAGNET)) {
            double reach = MAGNET[i];
            for (Entity loose : level.getEntities((Entity) null, player.getBoundingBox().inflate(reach),
                    e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
                Vec3 pull = player.position().add(0, 0.5, 0).subtract(loose.position()).normalize().scale(0.45);
                loose.setDeltaMovement(pull);
            }
        }
        if (kind.has(Ability.CLEAR_SIGHT) && player.tickCount % 20 == 0 && !level.canSeeSky(player.blockPosition().above())) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }
    }

    // ---- Things that go away again ----------------------------------------------------------------------------------

    private record Expiring(ServerLevel level, Entity entity, long at) {
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Expiring> it = EXPIRING.iterator(); it.hasNext(); ) {
            Expiring expiring = it.next();
            if (expiring.level.getGameTime() >= expiring.at || expiring.entity.isRemoved()) {
                if (!expiring.entity.isRemoved()) {
                    expiring.entity.discard();
                }
                it.remove();
            }
        }
    }

    // ---- Helpers ----------------------------------------------------------------------------------------------------

    private static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        return entity;
    }

    private static void burst(ServerLevel level, Vec3 at, ParticleOptions particle, int count) {
        level.sendParticles(particle, at.x, at.y, at.z, count, 0.3, 0.3, 0.3, 0.05);
    }
}
