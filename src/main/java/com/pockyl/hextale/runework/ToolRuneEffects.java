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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * What every one of the 72 runes does on a hextech tool. Each rune fires at one or two moments - when a block is broken,
 * when an enemy is struck, when the tool is used (right click), while it is held, when an enemy falls, when the holder
 * is hurt. Runes cost nothing: right-click spells only make the tool rest a moment; a rune's strength is its stars (a
 * Lens before it adds one). Runes on one tool stack: a pickaxe with Drill, Smelting and Cache digs 3x3, smelts
 * and sends everything to the pocket. Nothing of this is ever written for the player: they find it out.
 */
public final class ToolRuneEffects {
    /** When a rune fires. */
    public enum Trigger {
        BREAK, HIT, USE, HOLD, KILL, HURT, WEAR, PASSIVE
    }

    private static final Map<String, Trigger[]> TRIGGERS = new java.util.HashMap<>();
    /** Set while a rune breaks extra blocks: their drops must not start another area or vein. */
    private static final ThreadLocal<Boolean> CHAINING = ThreadLocal.withInitial(() -> false);
    /** Blocks a Passage rune opened, put back after a while. */
    private static final List<Restore> RESTORES = new ArrayList<>();
    /** Things a rune called up that go away again (constructs, ore markers). */
    private static final List<Expiring> EXPIRING = new ArrayList<>();
    private static final int USE_COOLDOWN = 20;
    private static final int HOLD_PERIOD = 20;

    static {
        t("fire_water", Trigger.USE);
        t("fire_earth", Trigger.BREAK);
        t("fire_air", Trigger.USE);
        t("fire_light", Trigger.USE);
        t("fire_shadow", Trigger.HIT);
        t("fire_life", Trigger.HIT);
        t("fire_metal", Trigger.KILL);
        t("fire_spark", Trigger.USE);
        t("water_fire", Trigger.USE, Trigger.HOLD);
        t("water_earth", Trigger.BREAK);
        t("water_air", Trigger.USE);
        t("water_light", Trigger.PASSIVE);
        t("water_shadow", Trigger.HIT);
        t("water_life", Trigger.USE);
        t("water_metal", Trigger.HIT);
        t("water_spark", Trigger.HIT);
        t("earth_fire", Trigger.WEAR);
        t("earth_water", Trigger.USE);
        t("earth_air", Trigger.HIT);
        t("earth_light", Trigger.BREAK);
        t("earth_shadow", Trigger.BREAK);
        t("earth_life", Trigger.USE);
        t("earth_metal", Trigger.HOLD);
        t("earth_spark", Trigger.PASSIVE);
        t("air_fire", Trigger.HIT);
        t("air_water", Trigger.USE);
        t("air_earth", Trigger.USE);
        t("air_light", Trigger.USE);
        t("air_shadow", Trigger.HOLD);
        t("air_life", Trigger.HOLD);
        t("air_metal", Trigger.USE);
        t("air_spark", Trigger.HOLD);
        t("light_fire", Trigger.USE);
        t("light_water", Trigger.USE);
        t("light_earth", Trigger.USE);
        t("light_air", Trigger.USE);
        t("light_shadow", Trigger.USE);
        t("light_life", Trigger.HOLD);
        t("light_metal", Trigger.HIT);
        t("light_spark", Trigger.HOLD);
        t("shadow_fire", Trigger.HOLD);
        t("shadow_water", Trigger.USE);
        t("shadow_earth", Trigger.USE);
        t("shadow_air", Trigger.USE);
        t("shadow_light", Trigger.USE);
        t("shadow_life", Trigger.HIT);
        t("shadow_metal", Trigger.HIT);
        t("shadow_spark", Trigger.HURT);
        t("life_fire", Trigger.HOLD);
        t("life_water", Trigger.USE);
        t("life_earth", Trigger.USE);
        t("life_air", Trigger.BREAK);
        t("life_light", Trigger.HOLD);
        t("life_shadow", Trigger.HIT);
        t("life_metal", Trigger.HOLD);
        t("life_spark", Trigger.USE);
        t("metal_fire", Trigger.BREAK);
        t("metal_water", Trigger.USE);
        t("metal_earth", Trigger.BREAK);
        t("metal_air", Trigger.USE);
        t("metal_light", Trigger.HURT);
        t("metal_shadow", Trigger.HIT);
        t("metal_life", Trigger.USE);
        t("metal_spark", Trigger.HOLD);
        t("spark_fire", Trigger.HIT);
        t("spark_water", Trigger.HIT);
        t("spark_earth", Trigger.USE);
        t("spark_air", Trigger.HIT);
        t("spark_light", Trigger.USE);
        t("spark_shadow", Trigger.USE);
        t("spark_life", Trigger.HOLD);
        t("spark_metal", Trigger.HOLD);
    }

    private ToolRuneEffects() {
    }

    static void chaining(boolean on) {
        CHAINING.set(on);
    }

    private static void t(String key, Trigger... triggers) {
        TRIGGERS.put(key, triggers);
    }

    /** When this rune fires on a tool (empty for a rune without a tool effect). */
    public static Trigger[] triggers(ComplexRune rune) {
        return TRIGGERS.getOrDefault(rune.key(), new Trigger[0]);
    }

    public static boolean has(ComplexRune rune, Trigger trigger) {
        for (Trigger t : triggers(rune)) {
            if (t == trigger) {
                return true;
            }
        }
        return false;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ToolRuneEffects::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(ToolRuneEffects::onHit);
        NeoForge.EVENT_BUS.addListener(ToolRuneEffects::onKill);
        NeoForge.EVENT_BUS.addListener(ToolRuneEffects::onHurt);
        NeoForge.EVENT_BUS.addListener(ToolRuneEffects::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ToolRuneEffects::onServerTick);
    }

    /** Engravings of the tool that fire at this moment, in slot order. */
    private static List<RuneSlots.Engraving> firing(ItemStack tool, Trigger trigger) {
        List<RuneSlots.Engraving> out = new ArrayList<>();
        if (HextechTools.isTool(tool)) {
            for (RuneSlots.Engraving engraving : HextechTools.runes(tool).engravings()) {
                if (has(engraving.rune(), trigger)) {
                    out.add(engraving);
                }
            }
        }
        return out;
    }

    private static int strength(ItemStack tool, RuneSlots.Engraving engraving) {
        return Math.min(4, HextechTools.strength(tool, engraving));
    }

    // ---- Break ------------------------------------------------------------------------------------------------------

    private static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!HextechTools.isTool(tool) || !(event.getBreaker() instanceof Player player)) {
            return;
        }
        ServerLevel level = event.getLevel();
        RandomSource random = level.getRandom();
        RuneSlots.Engraving cache = null;
        for (RuneSlots.Engraving engraving : firing(tool, Trigger.BREAK)) {
            int s = strength(tool, engraving);
            switch (engraving.rune().key()) {
                case "fire_earth" -> RuneFiring.smelt(event, tool, engraving);
                case "water_earth" -> {
                    if (!CHAINING.get()) {
                        RuneFiring.erode(event, player, tool, engraving);
                    }
                }
                case "metal_earth" -> {
                    if (!CHAINING.get()) {
                        drill(event, player, tool, engraving, s);
                    }
                }
                case "earth_light" -> {
                    // Gem: stone sometimes gives up a crystal.
                    if (event.getState().is(BlockTags.BASE_STONE_OVERWORLD) && random.nextFloat() < 0.02F * s) {
                        float roll = random.nextFloat();
                        ItemStack gem = new ItemStack(roll < 0.6F ? Items.AMETHYST_SHARD : roll < 0.85F ? Items.LAPIS_LAZULI
                                : roll < 0.97F ? Items.EMERALD : Items.DIAMOND);
                        event.getDrops().add(drop(level, event.getPos(), gem));
                        burst(level, Vec3.atCenterOf(event.getPos()), ParticleTypes.WAX_ON, 8);
                    }
                }
                case "metal_fire" -> {
                    // Forge: ores sometimes give twice.
                    if (event.getState().is(Tags.Blocks.ORES)) {
                        List<ItemEntity> more = new ArrayList<>();
                        for (ItemEntity drop : event.getDrops()) {
                            if (random.nextFloat() < 0.2F * s) {
                                more.add(drop(level, event.getPos(), drop.getItem().copy()));
                            }
                        }
                        event.getDrops().addAll(more);
                    }
                }
                case "life_air" -> {
                    // Pollen: a ripe crop plants itself again.
                    if (event.getState().getBlock() instanceof CropBlock crop && level.getBlockState(event.getPos()).isAir()) {
                        ItemStack seed = new ItemStack(crop.asItem());
                        for (ItemEntity drop : event.getDrops()) {
                            if (drop.getItem().is(seed.getItem())) {
                                drop.getItem().shrink(1);
                                break;
                            }
                        }
                        level.setBlockAndUpdate(event.getPos(), crop.defaultBlockState());
                        burst(level, Vec3.atCenterOf(event.getPos()), ParticleTypes.HAPPY_VILLAGER, 5);
                    }
                }
                case "earth_shadow" -> cache = engraving;
                default -> {
                }
            }
        }
        if (cache != null) {
            // Cache: the drops go straight into the pocket.
            for (Iterator<ItemEntity> it = event.getDrops().iterator(); it.hasNext(); ) {
                ItemEntity drop = it.next();
                ItemStack stack = drop.getItem();
                player.getInventory().add(stack);
                if (stack.isEmpty()) {
                    it.remove();
                }
            }
        }
    }

    /** Drill: the blocks around the broken one on the face it was struck from - 3x3, then deeper, then 5x5. */
    private static void drill(BlockDropsEvent event, Player player, ItemStack tool, RuneSlots.Engraving engraving, int s) {
        ServerLevel level = event.getLevel();
        HitResult hit = player.pick(6, 0, false);
        Direction face = hit instanceof BlockHitResult block ? block.getDirection() : Direction.UP;
        int radius = s >= 4 ? 2 : 1;
        int depth = s >= 3 ? 2 : 1;
        BlockPos origin = event.getPos();
        float hardness = event.getState().getDestroySpeed(level, origin);
        CHAINING.set(true);
        try {
            for (int d = 0; d < depth; d++) {
                for (int a = -radius; a <= radius; a++) {
                    for (int b = -radius; b <= radius; b++) {
                        BlockPos at = offset(origin, face, a, b, d);
                        if (at.equals(origin)) {
                            continue;
                        }
                        BlockState state = level.getBlockState(at);
                        float speed = state.getDestroySpeed(level, at);
                        if (state.isAir() || speed < 0 || speed > hardness * 1.5F + 0.5F || level.getBlockEntity(at) != null
                                || !tool.isCorrectToolForDrops(state) && state.requiresCorrectToolForDrops()) {
                            continue;
                        }
                        Block.dropResources(state, level, at, null, player, tool);
                        level.destroyBlock(at, false, player);
                    }
                }
            }
        } finally {
            CHAINING.set(false);
        }
    }

    private static BlockPos offset(BlockPos origin, Direction face, int a, int b, int depth) {
        Direction back = face.getOpposite();
        return switch (face.getAxis()) {
            case X -> origin.offset(back.getStepX() * depth, a, b);
            case Y -> origin.offset(a, back.getStepY() * depth, b);
            case Z -> origin.offset(a, b, back.getStepZ() * depth);
        };
    }

    // ---- Hit and kill -----------------------------------------------------------------------------------------------

    private static void onHit(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        LivingEntity target = event.getEntity();
        RandomSource random = level.getRandom();
        for (RuneSlots.Engraving engraving : firing(tool, Trigger.HIT)) {
            int s = strength(tool, engraving);
            String key = engraving.rune().key();
            if (!target.isAlive() && !key.equals("spark_air")) {
                continue;
            }
            switch (key) {
                case "fire_shadow" -> {
                    if (target.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
                        target.hurtServer(level, level.damageSources().magic(), 2 * s);
                        target.igniteForSeconds(3);
                        burst(level, target.position().add(0, 1, 0), ParticleTypes.SOUL_FIRE_FLAME, 10);
                    }
                }
                case "fire_life" -> target.igniteForSeconds(2 * s);
                case "water_shadow" -> {
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, s - 1));
                    target.setDeltaMovement(target.getDeltaMovement().add(0, -0.6, 0));
                    target.needsSync = true;
                    burst(level, target.position(), ParticleTypes.BUBBLE, 12);
                }
                case "water_metal" -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + 20 * s, s - 1));
                case "water_spark" -> {
                    if (target.isInWaterOrRain()) {
                        target.hurtServer(level, level.damageSources().lightningBolt(), 2 * s);
                        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4),
                                e -> e != player && e != target && e.isInWaterOrRain())) {
                            other.hurtServer(level, level.damageSources().lightningBolt(), s);
                            arc(level, target.position().add(0, 1, 0), other.position().add(0, 1, 0));
                        }
                    }
                }
                case "earth_air" -> {
                    if (!target.onGround()) {
                        target.hurtServer(level, level.damageSources().fall(), 2 * s);
                    }
                    target.setDeltaMovement(target.getDeltaMovement().x, -1.2, target.getDeltaMovement().z);
                    target.needsSync = true;
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
                }
                case "air_fire" -> {
                    if (target.isOnFire()) {
                        target.hurtServer(level, level.damageSources().onFire(), 2 * s);
                        target.igniteForSeconds(4);
                        knock(target, player, 0.6);
                        burst(level, target.position().add(0, 1, 0), ParticleTypes.FLAME, 12);
                    }
                }
                case "light_metal" -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100 * s, 0));
                case "shadow_life" -> {
                    target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40 + 20 * s, 0));
                    player.heal(s);
                }
                case "shadow_metal" -> {
                    for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                        ItemStack worn = target.getItemBySlot(slot);
                        if (worn.isDamageableItem()) {
                            worn.hurtAndBreak(5 * s, target, slot);
                        }
                    }
                }
                case "life_shadow" -> {
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40 * s, 2));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40 * s, 0));
                }
                case "metal_shadow" -> {
                    target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * s, 9));
                    target.setDeltaMovement(Vec3.ZERO);
                    target.needsSync = true;
                }
                case "spark_fire" -> {
                    if (target.getHealth() < target.getMaxHealth() * 0.3F) {
                        level.explode(player, target.getX(), target.getY() + 0.5, target.getZ(), 1.5F + 0.5F * s, Level.ExplosionInteraction.NONE);
                    }
                }
                case "spark_water" -> {
                    boolean wet = target.isInWaterOrRain();
                    target.hurtServer(level, level.damageSources().lightningBolt(), (1 + s) * (wet ? 2 : 1));
                    if (wet) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
                    }
                    burst(level, target.position().add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK, 12);
                }
                case "spark_air" -> {
                    int left = s;
                    for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5),
                            e -> e != player && e != target && e instanceof Enemy)) {
                        if (left-- <= 0) {
                            break;
                        }
                        other.hurtServer(level, level.damageSources().lightningBolt(), 2 + s);
                        arc(level, target.position().add(0, 1, 0), other.position().add(0, 1, 0));
                    }
                }
                default -> {
                }
            }
        }
    }

    private static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        for (RuneSlots.Engraving engraving : firing(tool, Trigger.KILL)) {
            int s = strength(tool, engraving);
            if (engraving.rune().key().equals("fire_metal")) {
                // Reforge: the fallen leave melted metal.
                LivingEntity dead = event.getEntity();
                level.addFreshEntity(drop(level, dead.blockPosition(), new ItemStack(Items.IRON_NUGGET, 1 + level.getRandom().nextInt(2 * s))));
                if (level.getRandom().nextFloat() < 0.1F * s) {
                    level.addFreshEntity(drop(level, dead.blockPosition(), new ItemStack(Items.GOLD_NUGGET, 1 + s)));
                }
            }
        }
    }

    private static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        for (RuneSlots.Engraving engraving : firing(tool, Trigger.HURT)) {
            int s = strength(tool, engraving);
            DamageSource source = event.getSource();
            switch (engraving.rune().key()) {
                case "metal_light" -> {
                    // Mirror: some arrows turn back on whoever shot them.
                    if (source.is(DamageTypeTags.IS_PROJECTILE) && level.getRandom().nextFloat() < 0.15F * s) {
                        if (source.getEntity() instanceof LivingEntity shooter) {
                            shooter.hurtServer(level, level.damageSources().magic(), event.getAmount());
                        }
                        event.setCanceled(true);
                        burst(level, player.position().add(0, 1, 0), ParticleTypes.END_ROD, 10);
                    }
                }
                case "shadow_spark" -> {
                    // Insulator: lightning finds no way in.
                    if (source.is(DamageTypes.LIGHTNING_BOLT)) {
                        event.setCanceled(true);
                    }
                }
                default -> {
                }
            }
        }
    }

    // ---- Held -------------------------------------------------------------------------------------------------------

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || player.tickCount % HOLD_PERIOD != 0) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        List<RuneSlots.Engraving> held = firing(tool, Trigger.HOLD);
        if (held.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (RuneSlots.Engraving engraving : held) {
            int s = strength(tool, engraving);
            switch (engraving.rune().key()) {
                case "water_fire" -> {
                    if (player.isOnFire()) {
                        player.clearFire();
                        burst(level, player.position().add(0, 1, 0), ParticleTypes.CLOUD, 8);
                    }
                }
                case "earth_metal" -> keep(player, MobEffects.RESISTANCE, s >= 3 ? 1 : 0);
                case "air_shadow" -> {
                    // Silence: enemies farther than a few steps forget the holder.
                                for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16),
                            m -> m.getTarget() == player && m.distanceTo(player) > 7 - s)) {
                        mob.setTarget(null);
                    }
                }
                case "air_life" -> {
                    if (player.isUnderWater()) {
                        keep(player, MobEffects.WATER_BREATHING, 0);
                    }
                }
                case "air_spark" -> {
                    // Windmill: high up or in a storm the wind carries the holder.
                    if (player.getY() >= 100 || level.isRainingAt(player.blockPosition())) {
                        keep(player, MobEffects.JUMP_BOOST, s - 1);
                        keep(player, MobEffects.SLOW_FALLING, 0);
                    }
                }
                case "light_life" -> {
                    // Photosynthesis: sunlight feeds the holder.
                    if (sunlit(level, player) && random.nextFloat() < 0.15F * s && player.getFoodData().needsFood()) {
                        player.getFoodData().eat(1, 0.3F);
                    }
                }
                case "light_spark" -> {
                    // Light-catcher: sunlight lends the holder strength.
                    if (sunlit(level, player)) {
                        keep(player, MobEffects.STRENGTH, s - 1);
                    }
                }
                case "shadow_fire" -> {
                    if (player.isOnFire() || player.isInLava()) {
                        keep(player, MobEffects.FIRE_RESISTANCE, 0);
                    }
                }
                case "life_fire" -> keep(player, MobEffects.FIRE_RESISTANCE, 0);
                case "life_light" -> keep(player, MobEffects.NIGHT_VISION, 0, 300);
                case "life_metal" -> {
                    if (tool.isDamaged()) {
                        tool.setDamageValue(Math.max(0, tool.getDamageValue() - s));
                    }
                }
                case "metal_spark" -> keep(player, MobEffects.HASTE, s - 1);
                case "spark_life" -> keep(player, MobEffects.SPEED, s - 1);
                case "spark_metal" -> {
                    // Magnet: drops and experience fly to the holder.
                    double reach = 4 + 2 * s;
                    for (Entity loose : level.getEntities((Entity) null, player.getBoundingBox().inflate(reach),
                            e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
                        Vec3 pull = player.position().add(0, 0.5, 0).subtract(loose.position()).normalize().scale(0.45);
                        loose.setDeltaMovement(pull);
                    }
                }
                default -> {
                }
            }
        }
    }

    /** Keeps a status effect on the holder while the tool is held. */
    private static void keep(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
        keep(player, effect, amplifier, 60);
    }

    private static void keep(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier,
                             int duration) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }

    private static boolean sunlit(ServerLevel level, Player player) {
        return level.isBrightOutside() && level.canSeeSky(player.blockPosition().above());
    }

    // ---- Use --------------------------------------------------------------------------------------------------------

    /** Whether the tool carries any rune that fires on use. */
    public static boolean usable(ItemStack tool) {
        return !firing(tool, Trigger.USE).isEmpty();
    }

    /**
     * Right click with the tool: every rune that fires on use does, at what the holder looks at.
     *
     * @return whether anything fired
     */
    public static boolean use(ServerLevel level, Player player, ItemStack tool) {
        List<RuneSlots.Engraving> runes = firing(tool, Trigger.USE);
        if (runes.isEmpty() || player.getCooldowns().isOnCooldown(tool)) {
            return false;
        }
        RandomSource random = level.getRandom();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(6)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockHitResult fluid = level.clip(new ClipContext(eye, eye.add(look.scale(6)), ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        boolean fired = false;
        for (RuneSlots.Engraving engraving : runes) {
            fired = true;
            int s = strength(tool, engraving);
            BlockPos hitPos = block.getType() == HitResult.Type.BLOCK ? block.getBlockPos() : null;
            BlockPos facePos = hitPos == null ? null : hitPos.relative(block.getDirection());
            switch (engraving.rune().key()) {
                case "fire_water" -> {
                    // Steam: a scalding gust shoves everything ahead.
                    for (LivingEntity e : cone(level, player, 3 + s)) {
                        e.hurtServer(level, level.damageSources().inFire(), 1 + s / 2F);
                        knock(e, player, 0.8 + 0.2 * s);
                    }
                    conePuff(level, player, ParticleTypes.CLOUD, 3 + s);
                    level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 0.6F);
                }
                case "fire_air" -> {
                    for (LivingEntity e : cone(level, player, 3 + s)) {
                        e.igniteForSeconds(3 + s);
                        e.hurtServer(level, level.damageSources().inFire(), 1);
                    }
                    conePuff(level, player, ParticleTypes.FLAME, 3 + s);
                    level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7F, 1.0F);
                }
                case "fire_light" -> {
                    // Eternal flame: a light that needs no fuel.
                    if (facePos != null && level.getBlockState(facePos).isAir()) {
                        level.setBlockAndUpdate(facePos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 11 + s));
                        burst(level, Vec3.atCenterOf(facePos), ParticleTypes.FLAME, 8);
                    }
                }
                case "fire_spark" -> {
                    Vec3 at = target(level, player, 16).getLocation();
                    level.explode(player, at.x, at.y, at.z, 1 + 0.5F * s, Level.ExplosionInteraction.NONE);
                }
                case "water_fire" -> {
                    if (hitPos != null) {
                        quench(level, hitPos, 1 + s);
                    }
                }
                case "water_air" -> {
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(4 + s), m -> m instanceof Enemy)) {
                        mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60 + 20 * s, 0));
                        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60 + 20 * s, 0));
                    }
                    level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 60, 3, 1, 3, 0.02);
                }
                case "water_life" -> {
                    player.heal(2 * s);
                    burst(level, player.position().add(0, 1, 0), ParticleTypes.HEART, 4);
                }
                case "earth_water" -> {
                    if (hitPos != null) {
                        replaceFluids(level, hitPos, 1 + s, Blocks.COBBLESTONE.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState());
                    }
                }
                case "earth_life" -> {
                    if (hitPos != null) {
                        for (BlockPos at : BlockPos.betweenClosed(hitPos.offset(-s, -1, -s), hitPos.offset(s, 1, s))) {
                            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, at.immutable());
                        }
                        burst(level, Vec3.atCenterOf(hitPos), ParticleTypes.HAPPY_VILLAGER, 12);
                    }
                }
                case "air_water" -> {
                    // Pump: a whole body of fluid drains away, the farther the stronger the rune.
                    if (fluid.getType() == HitResult.Type.BLOCK) {
                        pump(level, fluid.getBlockPos(), 16 * s);
                    }
                }
                case "air_earth" -> {
                    if (hitPos != null) {
                        shove(level, player, hitPos, block.getDirection().getOpposite());
                    }
                }
                case "air_light" -> {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60 + 20 * s, 0));
                    forget(level, player, 16);
                    burst(level, player.position().add(0, 1, 0), ParticleTypes.WITCH, 16);
                }
                case "air_metal" -> {
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.6 + 0.25 * s, player.getDeltaMovement().z);
                    player.needsSync = true;
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
                    burst(level, player.position(), ParticleTypes.CLOUD, 12);
                }
                case "light_fire" -> {
                    // Beam: a thin ray of heat.
                    HitResult hit = target(level, player, 16 + 4 * s);
                    line(level, eye, hit.getLocation(), ParticleTypes.END_ROD);
                    if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity e) {
                        e.hurtServer(level, level.damageSources().inFire(), 2 + s);
                        e.igniteForSeconds(2);
                    }
                }
                case "light_water" -> {
                    for (var effect : new ArrayList<>(player.getActiveEffects())) {
                        if (!effect.getEffect().value().isBeneficial()) {
                            player.removeEffect(effect.getEffect());
                        }
                    }
                    burst(level, player.position().add(0, 1, 0), ParticleTypes.END_ROD, 12);
                }
                case "light_earth" -> reveal(level, player, 4 + 4 * s);
                case "light_air" -> {
                    FireworkRocketEntity rocket = new FireworkRocketEntity(level, player.getX(), player.getY() + 1, player.getZ(),
                            new ItemStack(Items.FIREWORK_ROCKET));
                    level.addFreshEntity(rocket);
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16), e -> e != player)) {
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                    }
                }
                case "light_shadow" -> {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4 + s),
                            e -> e.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD))) {
                        e.hurtServer(level, level.damageSources().magic(), 3 * s);
                        knock(e, player, 0.8);
                    }
                    player.removeEffect(MobEffects.DARKNESS);
                    player.removeEffect(MobEffects.BLINDNESS);
                    level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, 2, 1, 2, 0.05);
                }
                case "shadow_water" -> {
                    if (hitPos != null) {
                        replaceFluids(level, hitPos, 1 + s, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState());
                    }
                }
                case "shadow_earth" -> {
                    if (hitPos != null) {
                        passage(level, hitPos, block.getDirection().getOpposite(), 2 + s);
                    }
                }
                case "shadow_air" -> player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100 * s, 0));
                case "shadow_light" -> {
                    // Eclipse: lights nearby go out (and drop).
                    for (BlockPos at : BlockPos.betweenClosed(player.blockPosition().offset(-3 - s, -2, -3 - s),
                            player.blockPosition().offset(3 + s, 3, 3 + s))) {
                        BlockState state = level.getBlockState(at);
                        if (state.getLightEmission() >= 10 && level.getBlockEntity(at) == null && state.getDestroySpeed(level, at) >= 0
                                && !state.is(Blocks.LAVA) && !state.is(Blocks.FIRE)) {
                            level.destroyBlock(at.immutable(), !state.is(Blocks.LIGHT), player);
                        }
                    }
                }
                case "life_water" -> {
                    if (facePos != null && level.getBlockState(facePos).canBeReplaced()) {
                        level.setBlockAndUpdate(facePos, Blocks.WATER.defaultBlockState());
                    }
                }
                case "life_earth" -> {
                    if (hitPos != null) {
                        for (BlockPos at : BlockPos.betweenClosed(hitPos.offset(-s, -1, -s), hitPos.offset(s, 1, s))) {
                            BlockState state = level.getBlockState(at);
                            if (state.isRandomlyTicking() && (state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS))) {
                                for (int i = 0; i < 5 * s; i++) {
                                    level.getBlockState(at).randomTick(level, at.immutable(), random);
                                }
                            }
                        }
                        burst(level, Vec3.atCenterOf(hitPos), ParticleTypes.HAPPY_VILLAGER, 12);
                    }
                }
                case "life_spark" -> {
                    for (Player other : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(4))) {
                        other.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, s - 1));
                    }
                }
                case "metal_water" -> {
                    // Pipes: a fluid flows into an empty bucket from afar.
                    if (fluid.getType() == HitResult.Type.BLOCK) {
                        bucket(level, player, fluid.getBlockPos());
                    }
                }
                case "metal_air" -> {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3 + s), e -> e != player)) {
                        knock(e, player, 1.0 + 0.3 * s);
                    }
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 1, player.getZ(), 8, 1.5, 0.2, 1.5, 0);
                }
                case "metal_life" -> construct(level, player, s);
                case "spark_earth" -> {
                    for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3 + s), e -> e != player && e.onGround())) {
                        e.hurtServer(level, level.damageSources().lightningBolt(), 2 + s);
                        e.setDeltaMovement(e.getDeltaMovement().add(0, 0.7, 0));
                        e.needsSync = true;
                    }
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.1, player.getZ(), 60, 3, 0.1, 3, 0.1);
                    level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4F, 1.6F);
                }
                case "spark_light" -> {
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(6 + s), m -> m instanceof Enemy)) {
                        mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        mob.setTarget(null);
                    }
                    level.sendParticles(net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFFFFF), player.getX(), player.getY() + 1.5, player.getZ(), 1, 0, 0, 0, 0);
                }
                case "spark_shadow" -> leap(level, player, 4 + 2 * s);
                default -> {
                }
            }
        }
        if (fired) {
            player.getCooldowns().addCooldown(tool, cooldown(tool));
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.4F, 1.4F);
        }
        return fired;
    }

    /** Ticks the tool rests after a right-click spell: Grounding leads the excess away, a quarter shorter per star. */
    private static int cooldown(ItemStack tool) {
        RuneSlots.Engraving grounding = HextechTools.runes(tool).find(ToolRunes.GROUNDING);
        return grounding == null ? USE_COOLDOWN : Math.max(5, USE_COOLDOWN - USE_COOLDOWN * HextechTools.strength(tool, grounding) / 4);
    }

    // ---- Use helpers ------------------------------------------------------------------------------------------------

    private static List<LivingEntity> cone(ServerLevel level, Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        return level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach), e -> {
            if (e == player) {
                return false;
            }
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            return to.length() <= reach && to.normalize().dot(look) > 0.7;
        });
    }

    private static void conePuff(ServerLevel level, Player player, ParticleOptions particle, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (int i = 0; i < 24; i++) {
            double d = 0.8 + level.getRandom().nextDouble() * reach;
            Vec3 at = eye.add(look.scale(d)).add((level.getRandom().nextDouble() - 0.5) * d * 0.5, (level.getRandom().nextDouble() - 0.5) * d * 0.5,
                    (level.getRandom().nextDouble() - 0.5) * d * 0.5);
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0.01);
        }
    }

    private static void knock(LivingEntity target, Entity from, double strength) {
        Vec3 away = target.position().subtract(from.position()).multiply(1, 0, 1).normalize().scale(strength);
        target.setDeltaMovement(target.getDeltaMovement().add(away.x, 0.3, away.z));
        target.needsSync = true;
    }

    /** What the holder looks at: the first living thing, else the block, within reach. */
    private static HitResult target(ServerLevel level, Player player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult entity = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, player, eye, stop,
                new AABB(eye, stop).inflate(1), e -> e instanceof LivingEntity && e != player && e.isPickable(), 0.3F);
        return entity != null ? entity : block;
    }

    private static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        int steps = (int) Math.max(2, from.distanceTo(to) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.lerp(to, i / (double) steps);
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }

    private static void arc(ServerLevel level, Vec3 from, Vec3 to) {
        line(level, from, to, ParticleTypes.ELECTRIC_SPARK);
    }

    private static void burst(ServerLevel level, Vec3 at, ParticleOptions particle, int count) {
        level.sendParticles(particle, at.x, at.y, at.z, count, 0.3, 0.3, 0.3, 0.02);
    }

    private static ItemEntity drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        return entity;
    }

    private static void quench(ServerLevel level, BlockPos centre, int radius) {
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(at);
            FluidState fluid = state.getFluidState();
            if (state.is(BlockTags.FIRE)) {
                level.removeBlock(at, false);
            } else if (fluid.is(FluidTags.LAVA)) {
                level.setBlockAndUpdate(at, fluid.isSource() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState());
                level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.01);
            }
        }
        level.playSound(null, centre, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    /** Replaces fluid blocks around: water with one block, lava with another. */
    private static void replaceFluids(ServerLevel level, BlockPos centre, int radius, BlockState forWater, BlockState forLava) {
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            FluidState fluid = level.getFluidState(at);
            if (fluid.isEmpty() || level.getBlockEntity(at) != null) {
                continue;
            }
            boolean isLava = fluid.is(FluidTags.LAVA);
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                level.setBlockAndUpdate(at, isLava ? forLava : forWater);
            }
        }
        level.playSound(null, centre, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7F, 1.0F);
    }

    private static void pump(ServerLevel level, BlockPos start, int max) {
        FluidState first = level.getFluidState(start);
        if (first.isEmpty()) {
            return;
        }
        boolean lava = first.is(FluidTags.LAVA);
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
        int taken = 0;
        while (!queue.isEmpty() && taken < max) {
            BlockPos at = queue.poll();
            if (!seen.add(at)) {
                continue;
            }
            FluidState fluid = level.getFluidState(at);
            if (fluid.isEmpty() || fluid.is(FluidTags.LAVA) != lava) {
                continue;
            }
            if (fluid.isSource() && level.getBlockState(at).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                taken++;
            }
            for (Direction d : Direction.values()) {
                queue.add(at.relative(d));
            }
        }
        level.playSound(null, start, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8F, 0.8F);
    }

    private static void shove(ServerLevel level, Player player, BlockPos pos, Direction away) {
        BlockState state = level.getBlockState(pos);
        BlockPos to = pos.relative(away);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null || !level.getBlockState(to).canBeReplaced()
                || !level.mayInteract(player, pos)) {
            return;
        }
        level.setBlockAndUpdate(to, state);
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    private static void forget(ServerLevel level, Player player, double reach) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(reach), m -> m.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    /** Insight: ores around glow through the stone for eight seconds (glowing block markers only the world shows). */
    private static void reveal(ServerLevel level, Player player, int radius) {
        BlockPos centre = player.blockPosition();
        int shown = 0;
        for (BlockPos at : BlockPos.betweenClosed(centre.offset(-radius, -radius, -radius), centre.offset(radius, radius, radius))) {
            if (shown >= 96) {
                break;
            }
            BlockState state = level.getBlockState(at);
            if (!state.is(Tags.Blocks.ORES)) {
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
            EXPIRING.add(new Expiring(level, marker, level.getGameTime() + 160));
            shown++;
        }
        level.playSound(null, centre, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    /** Passage: stone ahead opens for a few seconds and closes again. */
    private static void passage(ServerLevel level, BlockPos start, Direction into, int depth) {
        long until = level.getGameTime() + 100;
        for (int d = 0; d < depth; d++) {
            for (int up = 0; up < 2; up++) {
                BlockPos at = start.relative(into, d).above(up);
                BlockState state = level.getBlockState(at);
                if (state.isAir() || level.getBlockEntity(at) != null || state.getDestroySpeed(level, at) < 0 || !state.getFluidState().isEmpty()) {
                    continue;
                }
                RESTORES.add(new Restore(level, at.immutable(), state, until));
                level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.sendParticles(ParticleTypes.PORTAL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
            }
        }
        level.playSound(null, start, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.5F, 0.6F);
    }

    private static void bucket(ServerLevel level, Player player, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.isSource()) {
            return;
        }
        int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.BUCKET));
        if (slot < 0 || !(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) {
            return;
        }
        player.getInventory().getItem(slot).shrink(1);
        player.getInventory().placeItemBackInInventory(new ItemStack(fluid.is(FluidTags.LAVA) ? Items.LAVA_BUCKET : Items.WATER_BUCKET),
                net.minecraft.util.Prediction.SERVER_ONLY);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static void construct(ServerLevel level, Player player, int s) {
        IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            return;
        }
        golem.setPlayerCreated(true);
        Vec3 at = player.position().add(player.getLookAngle().multiply(2, 0, 2));
        golem.snapTo(at.x, player.getY(), at.z, player.getYRot(), 0);
        level.addFreshEntity(golem);
        EXPIRING.add(new Expiring(level, golem, level.getGameTime() + (15 + 5L * s) * 20));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, player.getY() + 1, at.z, 30, 0.5, 1, 0.5, 0.1);
    }

    /** Leap: a jump through space along the look, to the last place two blocks free. */
    private static void leap(ServerLevel level, Player player, int reach) {
        Vec3 look = player.getLookAngle();
        Vec3 best = null;
        for (int d = 1; d <= reach; d++) {
            Vec3 at = player.position().add(look.scale(d));
            BlockPos pos = BlockPos.containing(at);
            if (level.getBlockState(pos).isSuffocating(level, pos) || level.getBlockState(pos.above()).isSuffocating(level, pos.above())) {
                continue;
            }
            best = at;
        }
        if (best != null) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
            player.teleportTo(best.x, best.y, best.z);
            player.fallDistance = 0;
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    // ---- Things that go away again ----------------------------------------------------------------------------------

    private record Restore(ServerLevel level, BlockPos pos, BlockState state, long at) {
    }

    private record Expiring(ServerLevel level, Entity entity, long at) {
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Restore> it = RESTORES.iterator(); it.hasNext(); ) {
            Restore restore = it.next();
            if (restore.level.getGameTime() >= restore.at) {
                if (restore.level.getBlockState(restore.pos).isAir() && restore.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(restore.pos)).isEmpty()) {
                    restore.level.setBlockAndUpdate(restore.pos, restore.state);
                } else if (!restore.level.getBlockState(restore.pos).isAir()) {
                    it.remove();
                    continue;
                } else {
                    continue;
                }
                it.remove();
            }
        }
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

    /** Every rune of the codex has a tool effect (checked by a game test). */
    public static List<ComplexRune> withoutEffect() {
        return ComplexRunes.all().stream().filter(rune -> triggers(rune).length == 0).toList();
    }
}
