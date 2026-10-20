package com.pockyl.hextale.runework;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.effect.MobEffects;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * What the pickaxe runes do. Every value is chosen by a rune's strength - its stars, one more with a Lens (1-4, index
 * 0-3). Extra blocks a pickaxe takes (area, vein, tunnel) wear it like mined blocks and their drops go through its other
 * runes (smelting, the pocket) too. {@link #amount} tells the book and JEI the number behind each strength.
 */
public final class PickaxeEffects {
    private static final float[] SMELT_BONUS = {0F, 0.2F, 0.4F, 0.6F};
    private static final float[] SMELT_XP = {0.5F, 1.0F, 1.5F, 2.0F};
    private static final float[] TEMPER = {0.35F, 0.55F, 0.75F, 0.9F};
    private static final float[] HARD_SPEED = {1.25F, 1.5F, 2.0F, 2.5F};
    private static final int[] VEIN = {8, 16, 32, 64};
    private static final int[] REVEAL_RADIUS = {6, 10, 14, 18};
    private static final int[] REVEAL_SECONDS = {8, 12, 16, 20};
    private static final int[] REVEAL_COOLDOWN = {240, 180, 120, 80};
    private static final int[] AREA_RADIUS = {1, 1, 2, 2};
    private static final int[] AREA_DEPTH = {1, 2, 1, 2};
    private static final float[] RICH = {0.15F, 0.3F, 0.45F, 0.6F};
    private static final float[] GEMS = {0.015F, 0.03F, 0.05F, 0.07F};
    private static final float[] MOTOR = {1.3F, 1.6F, 2.0F, 2.5F};
    private static final int[] MAGNET = {4, 7, 10, 13};
    private static final int[] TUNNEL_DEPTH = {5, 8, 12, 16};
    private static final int[] TUNNEL_COOLDOWN = {100, 80, 60, 40};
    private static final int[] QUAKE_RADIUS = {2, 3, 4, 5};
    private static final int[] QUAKE_COOLDOWN = {120, 100, 80, 60};
    /** Gem odds out of 100 by strength: amethyst, lapis, emerald, diamond. */
    /** Ore experience is multiplied by 1 + this; stone gives a point now and then. */
    private static final float[] RADIANCE_ORE = {0.5F, 1.0F, 1.5F, 2.0F};
    private static final float[] RADIANCE_STONE = {0.03F, 0.05F, 0.07F, 0.09F};
    private static final float[] FEED = {0.02F, 0.035F, 0.05F, 0.065F};
    /** Air given back under water per block, in ticks. */
    private static final int[] BREATH_AIR = {30, 45, 60, 80};
    private static final int[] NIGHT_SIGHT = {300, 500, 800, 1200};
    private static final int[] FIRE_SHIELD = {120, 200, 300, 400};
    private static final float[] WISP = {0.10F, 0.20F, 0.30F, 0.40F};
    /** Light at the digging spot this dark or darker counts as "the dark". */
    private static final int DARK = 7;
    private static final int[][] GEM_TABLE = {{60, 25, 12, 3}, {50, 25, 17, 8}, {40, 25, 22, 13}, {30, 25, 25, 20}};
    /** Blocks harder than this count as hard stone for Tempering (deepslate is 3). */
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

    public static float temperChance(int strength) {
        return TEMPER[strength - 1];
    }

    /**
     * The number a rune works with at a strength (1-4), formatted for its {@link PickaxeRune#valueKey()} pattern:
     * a share, a radius, a block count or a factor.
     */
    public static String amount(PickaxeRune rune, int strength) {
        int i = Mth.clamp(strength, 1, 4) - 1;
        return switch (rune) {
            case SMELTING -> percent(SMELT_BONUS[i]);
            case TEMPERING -> percent(TEMPER[i]);
            case EROSION -> Integer.toString(VEIN[i]);
            case INSIGHT -> Integer.toString(REVEAL_RADIUS[i]);
            case LENS, CACHE -> "";
            case HASTE -> percent(ToolRune.HASTE_CHANCE[i]);
            case FERTILITY -> HolderRune.roman(i + 1);
            case RADIANCE -> "+" + percent(RADIANCE_ORE[i]);
            case BREATH -> percent(FEED[i]);
            case BANISHING -> HolderRune.seconds(NIGHT_SIGHT[i]);
            case FIREPROOFING -> HolderRune.seconds(FIRE_SHIELD[i]);
            case ETERNAL_FLAME -> percent(WISP[i]);
            case MENDING -> percent(ToolRune.MEND[i]);
            case DRILL -> {
                int side = AREA_RADIUS[i] * 2 + 1;
                yield side + "×" + side + (AREA_DEPTH[i] > 1 ? "×" + AREA_DEPTH[i] : "");
            }
            case FORGE -> percent(RICH[i]);
            case GEMSTONE -> percent(GEMS[i]);
            case MOTOR -> factor(MOTOR[i]);
            case MAGNET -> Integer.toString(MAGNET[i]);
            case PASSAGE -> Integer.toString(TUNNEL_DEPTH[i]);
            case QUAKE -> Integer.toString(QUAKE_RADIUS[i]);
        };
    }

    private static String percent(float share) {
        float value = share * 100;
        return (value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.1f", value)) + "%";
    }

    private static String factor(float value) {
        return value == Math.round(value) ? Integer.toString(Math.round(value)) : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** The pickaxe's right-click rune, or null (only one can be on a pickaxe). */
    public static PickaxeRune power(ItemStack tool) {
        if (!HextechPickaxeItem.is(tool)) {
            return null;
        }
        for (PickaxeRune rune : PickaxeRunes.of(tool).runes()) {
            if (rune.active()) {
                return rune;
            }
        }
        return null;
    }

    // ---- Digging ----------------------------------------------------------------------------------------------------

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        if (!HextechPickaxeItem.is(tool) || !tool.isCorrectToolForDrops(event.getState())) {
            return;
        }
        PickaxeRunes runes = PickaxeRunes.of(tool);
        float speed = event.getNewSpeed();
        int motor = runes.strength(PickaxeRune.MOTOR);
        if (motor > 0) {
            speed *= MOTOR[motor - 1];
        }
        int tempering = runes.strength(PickaxeRune.TEMPERING);
        if (tempering > 0) {
            BlockPos pos = event.getPosition().orElse(player.blockPosition());
            if (event.getState().getDestroySpeed(player.level(), pos) > HARD) {
                speed *= HARD_SPEED[tempering - 1];
            }
        }
        event.setNewSpeed(speed);
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!HextechPickaxeItem.is(tool) || !(event.getBreaker() instanceof Player player)) {
            return;
        }
        PickaxeRunes runes = PickaxeRunes.of(tool);
        if (runes.isEmpty()) {
            return;
        }
        ServerLevel level = event.getLevel();
        BlockState state = event.getState();
        boolean ore = state.is(Tags.Blocks.ORES);
        if (!CHAINING.get() && !player.isShiftKeyDown()) {
            int erosion = runes.strength(PickaxeRune.EROSION);
            int drill = runes.strength(PickaxeRune.DRILL);
            if (erosion > 0 && ore) {
                vein(event, player, tool, VEIN[erosion - 1]);
            } else if (drill > 0) {
                area(event, player, tool, AREA_RADIUS[drill - 1], AREA_DEPTH[drill - 1]);
            }
        }
        RandomSource random = level.getRandom();
        int fertility = runes.strength(PickaxeRune.FERTILITY);
        if (fertility > 0) {
            ToolEffects.rerollWithFortune(event, player, fertility);
        }
        int forge = runes.strength(PickaxeRune.FORGE);
        if (forge > 0 && ore) {
            List<ItemEntity> more = new ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                if (random.nextFloat() < RICH[forge - 1]) {
                    more.add(drop(level, event.getPos(), drop.getItem().copy()));
                }
            }
            event.getDrops().addAll(more);
        }
        int gemstone = runes.strength(PickaxeRune.GEMSTONE);
        if (gemstone > 0 && state.is(BlockTags.BASE_STONE_OVERWORLD) && random.nextFloat() < GEMS[gemstone - 1]) {
            event.getDrops().add(drop(level, event.getPos(), gem(random, GEM_TABLE[gemstone - 1])));
            burst(level, Vec3.atCenterOf(event.getPos()), ParticleTypes.WAX_ON, 8);
        }
        int smelting = runes.strength(PickaxeRune.SMELTING);
        if (smelting > 0) {
            smelt(event, ore, smelting - 1);
        }
        gifts(event, level, player, runes, ore);
        if (runes.strength(PickaxeRune.CACHE) > 0) {
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                ItemStack stack = it.next().getItem();
                player.getInventory().add(stack);
                if (stack.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    /** The passive gifts of digging: Radiance, Haste, Breath, Banishing, Fireproofing, Eternal Flame. */
    private static void gifts(BlockDropsEvent event, ServerLevel level, Player player, PickaxeRunes runes, boolean ore) {
        RandomSource random = level.getRandom();
        BlockPos pos = event.getPos();
        Vec3 at = Vec3.atCenterOf(pos);
        BlockState state = event.getState();
        int radiance = runes.strength(PickaxeRune.RADIANCE);
        if (radiance > 0) {
            if (ore && event.getDroppedExperience() > 0) {
                event.setDroppedExperience(Math.round(event.getDroppedExperience() * (1 + RADIANCE_ORE[radiance - 1])));
                ToolEffects.puff(level, at, PickaxeRune.RADIANCE.rune(), 5, 0.3);
            } else if (state.is(BlockTags.BASE_STONE_OVERWORLD) && random.nextFloat() < RADIANCE_STONE[radiance - 1]) {
                event.setDroppedExperience(event.getDroppedExperience() + 1 + random.nextInt(2));
                ToolEffects.puff(level, at, PickaxeRune.RADIANCE.rune(), 5, 0.3);
            }
        }
        ToolEffects.haste(level, player, PickaxeRune.HASTE.rune(), runes.strength(PickaxeRune.HASTE));
        int breath = runes.strength(PickaxeRune.BREATH);
        if (breath > 0) {
            if (player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply()) {
                player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + BREATH_AIR[breath - 1]));
                level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getEyeY(), player.getZ(), 6, 0.3, 0.2, 0.3, 0.05);
            }
            if (player.getFoodData().needsFood() && random.nextFloat() < FEED[breath - 1]) {
                player.getFoodData().eat(1, 0.5F);
                ToolEffects.puff(level, player.position().add(0, 1, 0), PickaxeRune.BREATH.rune(), 8, 0.4);
                ToolEffects.sound(level, player.position(), SoundEvents.BREEZE_IDLE_GROUND, 0.4F, 1.2F);
            }
        }
        boolean dark = level.getMaxLocalRawBrightness(player.blockPosition()) <= DARK;
        int banishing = runes.strength(PickaxeRune.BANISHING);
        if (banishing > 0 && dark && ToolEffects.keep(player, MobEffects.NIGHT_VISION, NIGHT_SIGHT[banishing - 1], 0)) {
            ToolEffects.puff(level, player.position().add(0, 1.4, 0), PickaxeRune.BANISHING.rune(), 10, 0.4);
        }
        int fireproofing = runes.strength(PickaxeRune.FIREPROOFING);
        if (fireproofing > 0 && nextToLava(level, pos) && ToolEffects.keep(player, MobEffects.FIRE_RESISTANCE, FIRE_SHIELD[fireproofing - 1], 0)) {
            level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 16, 0.4, 0.6, 0.4, 0.02);
            ToolEffects.puff(level, player.position().add(0, 1, 0), PickaxeRune.FIREPROOFING.rune(), 12, 0.5);
            ToolEffects.sound(level, player.position(), SoundEvents.FIRECHARGE_USE, 0.5F, 0.6F);
        }
        int flame = runes.strength(PickaxeRune.ETERNAL_FLAME);
        if (flame > 0 && !CHAINING.get() && level.getMaxLocalRawBrightness(pos) <= DARK && random.nextFloat() < WISP[flame - 1]) {
            // The block is gone by the time the drops fall; a wisp takes its place on the next tick, if nothing else does.
            level.getServer().execute(() -> {
                if (level.getBlockState(pos).isAir()) {
                    level.setBlockAndUpdate(pos, com.pockyl.hextale.registry.ModBlocks.HEX_WISP.get().defaultBlockState());
                    ToolEffects.puff(level, at, PickaxeRune.ETERNAL_FLAME.rune(), 10, 0.2);
                    ToolEffects.sound(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4F, 0.8F);
                }
            });
        }
    }

    private static boolean nextToLava(ServerLevel level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (level.getFluidState(pos.relative(side)).is(net.minecraft.tags.FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
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
        area(event, player, tool, radius, depth, state -> true);
    }

    /** {@link #area} over the blocks that pass {@code which} only (a shovel's soil). */
    static void area(BlockDropsEvent event, Player player, ItemStack tool, int radius, int depth,
                     java.util.function.Predicate<BlockState> which) {
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
                        if (which.test(state) && state.getDestroySpeed(level, at) <= hardness * 1.5F + 0.5F && !take(level, player, tool, at)) {
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

    /** Whether a tool is breaking extra blocks right now (their drops must not start another chain). */
    static boolean chaining() {
        return CHAINING.get();
    }

    static void chain(Runnable work) {
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
    static boolean take(ServerLevel level, Player player, ItemStack tool, BlockPos at) {
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

    /** The pickaxe's right-click rune; false when nothing happened (no cooldown then). */
    static boolean use(ServerLevel level, Player player, ItemStack tool, PickaxeRune power) {
        int i = PickaxeRunes.of(tool).strength(power) - 1;
        boolean done;
        int cooldown;
        switch (power) {
            case INSIGHT -> {
                done = reveal(level, player, REVEAL_RADIUS[i], REVEAL_SECONDS[i] * 20);
                cooldown = REVEAL_COOLDOWN[i];
            }
            case PASSAGE -> {
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
        if (!HextechPickaxeItem.is(tool)) {
            return;
        }
        int magnet = PickaxeRunes.of(tool).strength(PickaxeRune.MAGNET);
        if (magnet > 0) {
            double reach = MAGNET[magnet - 1];
            for (Entity loose : level.getEntities((Entity) null, player.getBoundingBox().inflate(reach),
                    e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
                Vec3 pull = player.position().add(0, 0.5, 0).subtract(loose.position()).normalize().scale(0.45);
                loose.setDeltaMovement(pull);
            }
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
