package com.pockyl.hextale.runework;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.world.WorldEdits;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * What the runes of the hextech axe, shovel and sword do ({@link ToolRune}), and the gifts every rune holder shares: a
 * shrugged-off point of wear (Mending), a burst of Haste, drops as with Fortune. Every proc shows as a puff of dust in
 * the rune's colour, so a lucky moment is seen.
 */
public final class ToolEffects {
    /** Set while a lightning arc strikes, so the arc's own hits start no new arcs or heals. */
    private static final ThreadLocal<Boolean> ARCING = ThreadLocal.withInitial(() -> false);
    /** Natural leaves a tree must touch before an axe takes it all (a log wall has none). */
    private static final int MIN_LEAVES = 4;
    private static final int MAX_LEAVES = 600;
    /** Blocks a lightning arc jumps over to the next creature. */
    private static final double ARC_REACH = 5;

    private ToolEffects() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ToolEffects::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(ToolEffects::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(ToolEffects::onDamaged);
        NeoForge.EVENT_BUS.addListener(ToolEffects::onDeath);
        NeoForge.EVENT_BUS.addListener(ToolEffects::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(ToolEffects::onExperience);
    }

    // ---- Shared -------------------------------------------------------------------------------------------------------

    /** The tool kind of a holder, or null for the pickaxe and the devices. */
    public static ToolRune.@Nullable Tool tool(RuneHolder holder) {
        return switch (holder) {
            case AXE -> ToolRune.Tool.AXE;
            case SHOVEL -> ToolRune.Tool.SHOVEL;
            case SWORD -> ToolRune.Tool.SWORD;
            default -> null;
        };
    }

    /** The strength a tool rune works with on this stack, 0 when the stack is not that tool or lacks the rune. */
    public static int strength(ItemStack stack, ToolRune.Tool tool, ToolRune rune) {
        RuneHolder holder = RuneHolder.of(stack);
        if (holder == null || tool(holder) != tool || !rune.on(tool)) {
            return 0;
        }
        return PickaxeRunes.of(stack).strength(rune.rune());
    }

    /**
     * Mending: each point of wear may be shrugged off and a point mended instead. Returns the wear left.
     */
    public static int mend(ItemStack stack, int amount, LivingEntity entity, int strength) {
        if (strength <= 0 || amount <= 0 || !(entity.level() instanceof ServerLevel level)) {
            return amount;
        }
        float chance = HolderRune.at(ToolRune.MEND, strength);
        int left = amount;
        boolean mended = false;
        for (int i = 0; i < amount; i++) {
            if (entity.getRandom().nextFloat() < chance) {
                left--;
                if (stack.getDamageValue() > 0) {
                    stack.setDamageValue(stack.getDamageValue() - 1);
                }
                mended = true;
            }
        }
        if (mended) {
            puff(level, entity.position().add(0, 1, 0), ToolRune.MENDING.rune(), 4, 0.4);
        }
        return left;
    }

    /** Haste: a chance of a burst of it while digging; stronger runes give Haste II. */
    public static void haste(ServerLevel level, Player player, ComplexRune rune, int strength) {
        if (strength <= 0 || level.getRandom().nextFloat() >= HolderRune.at(ToolRune.HASTE_CHANCE, strength)) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ToolRune.HASTE_TICKS, strength >= 3 ? 1 : 0, true, true));
        puff(level, player.position().add(0, 1, 0), rune, 10, 0.6);
        sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 1.4F);
    }

    /**
     * Gives (or renews) an effect without the flicker of a running-out one: only when it is missing or has less than
     * a third of {@code ticks} left.
     */
    public static boolean keep(LivingEntity entity, Holder<MobEffect> effect, int ticks, int amplifier) {
        MobEffectInstance now = entity.getEffect(effect);
        if (now != null && now.getAmplifier() >= amplifier && now.getDuration() > ticks / 3) {
            return false;
        }
        entity.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false, true));
        return true;
    }

    /** A copy of the tool with {@code extra} more levels of Fortune; null when it has Silk Touch (Fortune would not work). */
    public static @Nullable ItemStack withFortune(ServerLevel level, ItemStack tool, int extra) {
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> silk = enchantments.getOrThrow(Enchantments.SILK_TOUCH);
        if (EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0) {
            return null;
        }
        Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);
        ItemStack copy = tool.copy();
        copy.enchant(fortune, EnchantmentHelper.getItemEnchantmentLevel(fortune, tool) + extra);
        return copy;
    }

    /** The drops of a block event rolled again as if the tool had more Fortune. */
    public static void rerollWithFortune(BlockDropsEvent event, Entity breaker, int extra) {
        ServerLevel level = event.getLevel();
        ItemStack lucky = withFortune(level, event.getTool(), extra);
        if (lucky == null) {
            return;
        }
        List<ItemStack> drops = Block.getDrops(event.getState(), level, event.getPos(), event.getBlockEntity(), breaker, lucky);
        event.getDrops().clear();
        for (ItemStack drop : drops) {
            event.getDrops().add(drop(level, event.getPos(), drop));
        }
    }

    /** Glints and a ring of light in the rune's colour: the sign that a rune just worked (more glints = a bigger proc). */
    public static void puff(ServerLevel level, Vec3 at, ComplexRune rune, int count, double spread) {
        level.sendParticles(ModParticles.glint(rune.color()), at.x, at.y, at.z, count, spread, spread * 0.6, spread, 0.03);
        level.sendParticles(ModParticles.ring(rune.color()), at.x, at.y, at.z, 0, 0.4 + spread * 1.2, 0, 0, 1);
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch + level.getRandom().nextFloat() * 0.1F);
    }

    public static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        return entity;
    }

    // ---- Axe and shovel -------------------------------------------------------------------------------------------------

    private static void onBlockDrops(BlockDropsEvent event) {
        ItemStack stack = event.getTool();
        RuneHolder holder = RuneHolder.of(stack);
        ToolRune.Tool tool = holder == null ? null : tool(holder);
        if (tool == null || tool == ToolRune.Tool.SWORD || !(event.getBreaker() instanceof Player player)) {
            return;
        }
        PickaxeRunes runes = PickaxeRunes.of(stack);
        if (runes.isEmpty()) {
            return;
        }
        ServerLevel level = event.getLevel();
        BlockState state = event.getState();
        if (tool == ToolRune.Tool.AXE) {
            axe(event, level, player, stack, state, runes);
        } else {
            shovel(event, level, player, stack, state, runes);
        }
        if (stack.isCorrectToolForDrops(state) || state.is(tool == ToolRune.Tool.AXE ? BlockTags.MINEABLE_WITH_AXE : BlockTags.MINEABLE_WITH_SHOVEL)) {
            haste(level, player, ToolRune.HASTE.rune(), runes.strength(ToolRune.HASTE.rune()));
        }
    }

    private static void axe(BlockDropsEvent event, ServerLevel level, Player player, ItemStack axe, BlockState state, PickaxeRunes runes) {
        BlockPos pos = event.getPos();
        int pollen = runes.strength(ToolRune.POLLEN.rune());
        if (pollen > 0 && state.is(BlockTags.LEAVES)) {
            rerollWithFortune(event, player, HolderRune.at(ToolRune.LEAF_FORTUNE, pollen));
            if (level.getRandom().nextInt(4) == 0) {
                puff(level, Vec3.atCenterOf(pos), ToolRune.POLLEN.rune(), 4, 0.3);
            }
        }
        if (!state.is(BlockTags.LOGS) || PickaxeEffects.chaining()) {
            return;
        }
        int gravity = runes.strength(ToolRune.GRAVITY.rune());
        int growth = runes.strength(ToolRune.GROWTH.rune());
        List<BlockPos> feet = new ArrayList<>();
        if (isFoot(level, pos)) {
            feet.add(pos);
        }
        if (gravity > 0 && !player.isShiftKeyDown()) {
            fellTree(level, player, axe, pos, state, HolderRune.at(ToolRune.TREE, gravity), feet);
        }
        if (growth > 0) {
            for (BlockPos foot : feet) {
                replant(level, player, foot, state, growth);
            }
        }
    }

    /** Whether a log here stands on soil: a tree's foot. */
    private static boolean isFoot(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.DIRT);
    }

    /**
     * Gravity: the logs touching the cut one (and the natural leaves around them) come down as if cut by the axe - only a
     * real tree, one with natural leaves on it, never a log wall. Logs wear the axe, leaves do not.
     */
    private static void fellTree(ServerLevel level, Player player, ItemStack axe, BlockPos start, BlockState cut, int maxLogs, List<BlockPos> feet) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> logs = new ArrayList<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && logs.size() < maxLogs) {
            BlockPos at = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                BlockPos pos = next.immutable();
                if (logs.size() >= maxLogs || !seen.add(pos) || !level.getBlockState(pos).is(BlockTags.LOGS)) {
                    continue;
                }
                logs.add(pos);
                queue.add(pos);
            }
        }
        List<BlockPos> leaves = new ArrayList<>();
        Set<BlockPos> leafSeen = new HashSet<>();
        List<BlockPos> around = new ArrayList<>(logs);
        around.add(start);
        for (BlockPos log : around) {
            for (Direction side : Direction.values()) {
                BlockPos pos = log.relative(side);
                if (leaves.size() < MAX_LEAVES && leafSeen.add(pos) && natural(level.getBlockState(pos))) {
                    leaves.add(pos);
                }
            }
        }
        if (leaves.size() < MIN_LEAVES) {
            return;
        }
        // The rest of the crown: natural leaves touching those already found, a few steps out.
        ArrayDeque<BlockPos> crown = new ArrayDeque<>(leaves);
        while (!crown.isEmpty() && leaves.size() < MAX_LEAVES) {
            BlockPos at = crown.poll();
            for (Direction side : Direction.values()) {
                BlockPos pos = at.relative(side);
                if (leafSeen.add(pos) && natural(level.getBlockState(pos)) && pos.distManhattan(start) < 14) {
                    leaves.add(pos);
                    crown.add(pos);
                }
            }
        }
        logs.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        for (BlockPos log : logs) {
            if (isFoot(level, log)) {
                feet.add(log);
            }
        }
        PickaxeEffects.chain(() -> {
            for (BlockPos leaf : leaves) {
                if (!WorldEdits.mayBreak(level, leaf, player)) {
                    continue;
                }
                Block.dropResources(level.getBlockState(leaf), level, leaf, null, player, axe);
                level.destroyBlock(leaf, false, player);
            }
            for (BlockPos log : logs) {
                if (!PickaxeEffects.take(level, player, axe, log)) {
                    return;
                }
            }
        });
        puff(level, Vec3.atCenterOf(start).add(0, 1.5, 0), ToolRune.GRAVITY.rune(), 16, 1.2);
        sound(level, Vec3.atCenterOf(start), SoundEvents.WOOD_BREAK, 1.0F, 0.6F);
    }

    private static boolean natural(BlockState state) {
        return state.is(BlockTags.LEAVES) && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT);
    }

    /** Growth: a sapling of the tree goes back where its foot was - from the bag, or with a chance out of nothing. */
    private static void replant(ServerLevel level, Player player, BlockPos foot, BlockState log, int strength) {
        Item sapling = saplingOf(log);
        if (sapling == null || !(sapling instanceof BlockItem blockItem)) {
            return;
        }
        BlockState plant = blockItem.getBlock().defaultBlockState();
        // Planted on the next tick: the cut log may not be gone yet.
        level.getServer().execute(() -> {
            if (!level.getBlockState(foot).canBeReplaced() || !plant.canSurvive(level, foot)
                    || !WorldEdits.mayPlace(level, foot, plant, player)) {
                return;
            }
            int slot = player.getInventory().findSlotMatchingItem(new ItemStack(sapling));
            if (slot >= 0) {
                player.getInventory().removeItem(slot, 1);
            } else if (level.getRandom().nextFloat() >= HolderRune.at(ToolRune.FREE_SAPLING, strength)) {
                return;
            }
            level.setBlockAndUpdate(foot, plant);
            puff(level, Vec3.atCenterOf(foot), ToolRune.GROWTH.rune(), 8, 0.3);
            level.levelEvent(net.minecraft.world.level.block.LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, foot, 0);
        });
    }

    /** The sapling a log grows from: oak_log -> oak_sapling, crimson_stem -> crimson_fungus, mangrove -> propagule. */
    public static @Nullable Item saplingOf(BlockState log) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(log.getBlock());
        String path = id.getPath().replace("stripped_", "");
        String base = path.endsWith("_log") || path.endsWith("_wood") ? path.substring(0, path.lastIndexOf('_'))
                : path.endsWith("_stem") || path.endsWith("_hyphae") ? path.substring(0, path.lastIndexOf('_')) : null;
        if (base == null) {
            return null;
        }
        String[] tries = path.endsWith("_stem") || path.endsWith("_hyphae") ? new String[]{base + "_fungus"}
                : new String[]{base + "_sapling", base + "_propagule"};
        for (String name : tries) {
            ResourceLocation sapling = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), name);
            if (BuiltInRegistries.ITEM.containsKey(sapling)) {
                return BuiltInRegistries.ITEM.get(sapling);
            }
        }
        return null;
    }

    private static void shovel(BlockDropsEvent event, ServerLevel level, Player player, ItemStack shovel, BlockState state, PickaxeRunes runes) {
        if (!state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return;
        }
        BlockPos pos = event.getPos();
        RandomSource random = level.getRandom();
        int erosion = runes.strength(ToolRune.EROSION.rune());
        if (erosion > 0 && !PickaxeEffects.chaining() && !player.isShiftKeyDown()) {
            PickaxeEffects.area(event, player, shovel, 1, erosion >= 3 ? 2 : 1, s -> s.is(BlockTags.MINEABLE_WITH_SHOVEL));
        }
        int cache = runes.strength(ToolRune.CACHE.rune());
        if (cache > 0 && soil(state) && random.nextFloat() < HolderRune.at(ToolRune.TREASURE, cache)) {
            event.getDrops().add(drop(level, pos, treasure(level, random)));
            puff(level, Vec3.atCenterOf(pos), ToolRune.CACHE.rune(), 12, 0.35);
            level.sendParticles(ModParticles.glint(ModParticles.GOLD), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
            sound(level, Vec3.atCenterOf(pos), SoundEvents.BRUSH_GRAVEL_COMPLETED, 0.8F, 0.9F);
        }
        int fertility = runes.strength(ToolRune.FERTILITY.rune());
        if (fertility > 0 && state.is(BlockTags.DIRT) && random.nextFloat() < HolderRune.at(ToolRune.SEEDS, fertility)) {
            event.getDrops().add(drop(level, pos, seed(random)));
            puff(level, Vec3.atCenterOf(pos), ToolRune.FERTILITY.rune(), 8, 0.3);
        }
    }

    /** Ground a shovel may find a treasure in. */
    private static boolean soil(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL) || state.is(Blocks.MUD);
    }

    /** Cache: what the ground hides - bones and flint most often, rarely an emerald, a pottery sherd or a rune scroll. */
    private static ItemStack treasure(ServerLevel level, RandomSource random) {
        int roll = random.nextInt(100);
        if ((roll -= 22) < 0) {
            return new ItemStack(Items.BONE, 1 + random.nextInt(2));
        }
        if ((roll -= 18) < 0) {
            return new ItemStack(Items.FLINT, 1 + random.nextInt(2));
        }
        if ((roll -= 15) < 0) {
            return new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(4));
        }
        if ((roll -= 12) < 0) {
            return new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(4));
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(Items.CLAY_BALL, 2 + random.nextInt(3));
        }
        if ((roll -= 8) < 0) {
            return new ItemStack(Items.RAW_COPPER, 1 + random.nextInt(2));
        }
        if ((roll -= 6) < 0) {
            return new ItemStack(Items.EMERALD);
        }
        if ((roll -= 5) < 0) {
            return BuiltInRegistries.ITEM.getRandomElementOf(ItemTags.DECORATED_POT_SHERDS, random)
                    .map(item -> new ItemStack(item.value())).orElse(new ItemStack(Items.BRICK));
        }
        if ((roll -= 4) < 0) {
            return new ItemStack(Items.DIAMOND);
        }
        List<ComplexRune> first = ResearchBook.runes(1);
        return RuneScrollItem.of(first.get(random.nextInt(first.size())), 1, ModItems.RUNE_SCROLL.get());
    }

    /** Fertility: seeds, bulbs and saplings sleeping in the soil. */
    private static ItemStack seed(RandomSource random) {
        int roll = random.nextInt(100);
        if ((roll -= 35) < 0) {
            return new ItemStack(Items.WHEAT_SEEDS, 1 + random.nextInt(2));
        }
        if ((roll -= 13) < 0) {
            return new ItemStack(Items.BEETROOT_SEEDS);
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(Items.PUMPKIN_SEEDS);
        }
        if ((roll -= 10) < 0) {
            return new ItemStack(Items.MELON_SEEDS);
        }
        if ((roll -= 6) < 0) {
            return new ItemStack(random.nextBoolean() ? Items.POTATO : Items.CARROT);
        }
        if ((roll -= 15) < 0) {
            return BuiltInRegistries.ITEM.getRandomElementOf(ItemTags.SAPLINGS, random)
                    .map(item -> new ItemStack(item.value())).orElse(new ItemStack(Items.OAK_SAPLING));
        }
        if ((roll -= 6) < 0) {
            return new ItemStack(Items.SWEET_BERRIES);
        }
        if ((roll -= 3) < 0) {
            return new ItemStack(Items.TORCHFLOWER_SEEDS);
        }
        return new ItemStack(Items.PITCHER_POD);
    }

    // ---- Sword and axe in battle ------------------------------------------------------------------------------------------

    /** The weapon of a melee blow by a player (the item in their main hand), or empty. */
    private static ItemStack weapon(DamageSource source) {
        if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) {
            return ItemStack.EMPTY;
        }
        return player.getMainHandItem();
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (ARCING.get() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack weapon = weapon(event.getSource());
        if (weapon.isEmpty()) {
            return;
        }
        LivingEntity target = event.getEntity();
        int flash = strength(weapon, ToolRune.Tool.SWORD, ToolRune.FLASH);
        if (flash > 0 && target.getHealth() >= target.getMaxHealth()) {
            event.setAmount(event.getAmount() * (1 + HolderRune.at(ToolRune.FIRST_STRIKE, flash)));
            puff(level, target.position().add(0, target.getBbHeight() * 0.6, 0), ToolRune.FLASH.rune(), 10, 0.3);
            level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.2, 0.2, 0.2, 0.08);
        }
        int corrosion = strength(weapon, ToolRune.Tool.AXE, ToolRune.CORROSION);
        if (corrosion > 0 && level.getRandom().nextFloat() < HolderRune.at(ToolRune.CORRODE, corrosion)) {
            // The armour is eaten through: the blow lands as if it were thinner, and it wears.
            event.setAmount(event.getAmount() + 1 + target.getArmorValue() * 0.3F);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !target.getItemBySlot(slot).isEmpty()) {
                    target.getItemBySlot(slot).hurtAndBreak(2 + level.getRandom().nextInt(3), target, slot);
                }
            }
            level.sendParticles(ParticleTypes.SCRAPE, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 14, 0.3, 0.4, 0.3, 0.1);
            puff(level, target.position().add(0, target.getBbHeight() * 0.5, 0), ToolRune.CORROSION.rune(), 8, 0.3);
            sound(level, target.position(), SoundEvents.AXE_SCRAPE, 0.8F, 0.7F);
        }
    }

    private static void onDamaged(LivingDamageEvent.Post event) {
        if (ARCING.get() || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack weapon = weapon(event.getSource());
        if (weapon.isEmpty() || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        LivingEntity target = event.getEntity();
        RandomSource random = level.getRandom();
        int withering = strength(weapon, ToolRune.Tool.SWORD, ToolRune.WITHERING);
        if (withering > 0 && player.getHealth() < player.getMaxHealth() && random.nextFloat() < HolderRune.at(ToolRune.HEAL_CHANCE, withering)) {
            player.heal(HolderRune.at(ToolRune.HEAL, withering));
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
            puff(level, player.position().add(0, 1, 0), ToolRune.WITHERING.rune(), 10, 0.4);
            sound(level, player.position(), SoundEvents.SOUL_ESCAPE.value(), 0.8F, 0.8F);
        }
        int chain = strength(weapon, ToolRune.Tool.SWORD, ToolRune.CHAIN_LIGHTNING);
        if (chain > 0 && random.nextFloat() < HolderRune.at(ToolRune.ARC_CHANCE, chain)) {
            arc(level, player, target, HolderRune.at(ToolRune.ARC_TARGETS, chain), HolderRune.at(ToolRune.ARC_DAMAGE, chain));
        }
    }

    /** Chain Lightning: the blow jumps on to the nearest hostile creatures around, one after another. */
    private static void arc(ServerLevel level, Player player, LivingEntity first, int jumps, float damage) {
        List<LivingEntity> struck = new ArrayList<>();
        struck.add(first);
        LivingEntity from = first;
        ARCING.set(true);
        try {
            for (int i = 0; i < jumps; i++) {
                LivingEntity source = from;
                LivingEntity next = level.getEntitiesOfClass(LivingEntity.class, source.getBoundingBox().inflate(ARC_REACH),
                                e -> e.isAlive() && e != player && !struck.contains(e) && hostile(e, player)).stream()
                        .min((a, b) -> Double.compare(a.distanceToSqr(source), b.distanceToSqr(source))).orElse(null);
                if (next == null) {
                    break;
                }
                line(level, source.position().add(0, source.getBbHeight() * 0.5, 0), next.position().add(0, next.getBbHeight() * 0.5, 0));
                next.hurt(level.damageSources().playerAttack(player), damage);
                struck.add(next);
                from = next;
            }
        } finally {
            ARCING.set(false);
        }
        if (struck.size() > 1) {
            sound(level, first.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.7F);
        }
    }

    private static boolean hostile(LivingEntity entity, Player player) {
        return entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == player;
    }

    /** Sparks along the arc between two points. */
    private static void line(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int points = Math.max(4, (int) (step.length() * 4));
        for (int i = 0; i <= points; i++) {
            Vec3 at = from.add(step.scale(i / (double) points));
            level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
        }
        puff(level, to, ToolRune.CHAIN_LIGHTNING.rune(), 6, 0.2);
    }

    /** The sword the killing blow came from, or empty. */
    private static ItemStack killer(DamageSource source) {
        return source.getEntity() instanceof Player player ? player.getMainHandItem() : ItemStack.EMPTY;
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level) || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        int bellows = strength(killer(event.getSource()), ToolRune.Tool.SWORD, ToolRune.BELLOWS);
        if (bellows > 0 && level.getRandom().nextFloat() < HolderRune.at(ToolRune.RAGE, bellows)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, HolderRune.at(ToolRune.RAGE_TICKS, bellows), 0, true, true));
            level.sendParticles(ModParticles.flame(ModParticles.FIRE), player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.5, 0.4, 0.03);
            puff(level, player.position().add(0, 1, 0), ToolRune.BELLOWS.rune(), 10, 0.5);
            sound(level, player.position(), SoundEvents.BLAZE_SHOOT, 0.4F, 0.6F);
        }
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack sword = killer(event.getSource());
        LivingEntity dead = event.getEntity();
        RandomSource random = level.getRandom();
        int spoils = strength(sword, ToolRune.Tool.SWORD, ToolRune.SPOILS);
        if (spoils > 0) {
            int looting = HolderRune.at(ToolRune.LOOTING, spoils);
            boolean more = false;
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                // Like Looting: never the creature's gear.
                if (stack.isDamageableItem() || stack.isEnchanted() || stack.getMaxStackSize() == 1) {
                    continue;
                }
                int extra = random.nextInt(looting + 1);
                if (extra > 0) {
                    stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() + extra));
                    drop.setItem(stack);
                    more = true;
                }
            }
            if (more) {
                puff(level, dead.position().add(0, 0.5, 0), ToolRune.SPOILS.rune(), 8, 0.4);
            }
        }
        int engraving = strength(sword, ToolRune.Tool.SWORD, ToolRune.ENGRAVING);
        if (engraving > 0 && random.nextFloat() < HolderRune.at(ToolRune.HEAD, engraving)) {
            Item head = headOf(dead.getType());
            if (head != null && event.getDrops().stream().noneMatch(drop -> drop.getItem().is(head))) {
                event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY() + 0.5, dead.getZ(), new ItemStack(head)));
                puff(level, dead.position().add(0, 1, 0), ToolRune.ENGRAVING.rune(), 16, 0.5);
                level.sendParticles(ModParticles.glint(ModParticles.GOLD), dead.getX(), dead.getY() + 1, dead.getZ(), 10, 0.4, 0.4, 0.4, 0.05);
                sound(level, dead.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 0.6F);
            }
        }
    }

    /** The head a creature leaves to Engraving, or null for creatures without one. */
    public static @Nullable Item headOf(EntityType<?> type) {
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED || type == EntityType.ZOMBIE_VILLAGER) {
            return Items.ZOMBIE_HEAD;
        }
        if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.BOGGED) {
            return Items.SKELETON_SKULL;
        }
        if (type == EntityType.WITHER_SKELETON) {
            return Items.WITHER_SKELETON_SKULL;
        }
        if (type == EntityType.CREEPER) {
            return Items.CREEPER_HEAD;
        }
        if (type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE) {
            return Items.PIGLIN_HEAD;
        }
        return null;
    }

    private static void onExperience(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null) {
            return;
        }
        int radiance = strength(player.getMainHandItem(), ToolRune.Tool.SWORD, ToolRune.RADIANCE);
        if (radiance > 0 && event.getDroppedExperience() > 0) {
            event.setDroppedExperience(Math.round(event.getDroppedExperience() * HolderRune.at(ToolRune.EXPERIENCE, radiance)));
        }
    }
}
