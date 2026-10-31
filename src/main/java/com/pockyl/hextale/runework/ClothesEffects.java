package com.pockyl.hextale.runework;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * What the runes on the researcher's clothes do (see {@link ClothesRune}). Chances and buffs show with a puff in the
 * rune's colour and a soft sound; the trousers' and boots' runes are steady attribute bonuses while worn.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ClothesEffects {
    /** Colours of the clothes' rune effects: pale light, clean water, life rising, steam. */
    private static final int PALE_LIGHT = 0xE8F4FF;
    private static final int CLEAN_WATER = 0x9FE6FF;
    private static final int LIFE = 0x8FE07A;
    private static final int STEAM = 0xDDE6EA;

    /** How often the robe's Mending works. */
    public static final int MEND_TICKS = 200;
    /** How often, while walking, the boots' Pollen may find a plant. */
    public static final int POLLEN_TICKS = 40;
    /** Ticks between two pulls of Magnet, and how fast things drift to the wearer (blocks per tick). */
    public static final int PULL_TICKS = 4;
    public static final double PULL_SPEED = 0.45;
    private static final int POLLEN_RADIUS = 3;
    private static final int DARK = 7;
    /** Blocks round the wearer in which Mirage makes the creatures hunting them lose the trail. */
    public static final int MIRAGE_REACH = 16;
    private static final Map<Player, Vec3> LAST_STEP = new WeakHashMap<>();
    /** Server: the game tick each Mirage-cloaked player stays out of the creatures' minds until (or until they strike). */
    private static final Map<UUID, Long> CLOAKED = new java.util.HashMap<>();

    private ClothesEffects() {
    }

    /** The strength of a rune on the piece worn in its slot (0 when absent). */
    public static int strength(LivingEntity entity, ClothesRune rune) {
        ItemStack stack = entity.getItemBySlot(slot(rune.piece()));
        RuneHolder holder = RuneHolder.of(stack);
        return holder == null || !holder.holderRunes().contains(rune) ? 0 : holder.runes(stack).strength(rune.rune());
    }

    public static EquipmentSlot slot(ClothesRune.Piece piece) {
        return switch (piece) {
            case HOOD -> EquipmentSlot.HEAD;
            case ROBE -> EquipmentSlot.CHEST;
            case TROUSERS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
        };
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getEntity();
        RandomSource random = level.getRandom();
        int breath = strength(player, ClothesRune.HOOD_BREATH);
        if (breath > 0 && player.isUnderWater() && player.getAirSupply() < player.getMaxAirSupply()
                && random.nextFloat() < HolderRune.at(ClothesRune.KEEP_AIR, breath)) {
            player.setAirSupply(player.getAirSupply() + 1);
            if (random.nextInt(30) == 0) {
                level.sendParticles(ModParticles.bubble(ModParticles.WATER), player.getX(), player.getEyeY(), player.getZ(), 4, 0.25, 0.15, 0.25, 0.04);
            }
        }
        int magnet = strength(player, ClothesRune.TROUSERS_MAGNET);
        if (magnet > 0 && player.tickCount % PULL_TICKS == 0 && !player.isShiftKeyDown() && !player.isSpectator()) {
            pull(level, player, HolderRune.at(ClothesRune.PULL_RADIUS, magnet));
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        int banishing = strength(player, ClothesRune.HOOD_BANISHING);
        if (banishing > 0 && level.getMaxLocalRawBrightness(player.blockPosition()) <= DARK
                && ToolEffects.keep(player, MobEffects.NIGHT_VISION, HolderRune.at(ClothesRune.NIGHT_SIGHT, banishing), 0)) {
            ToolEffects.puff(level, player.position().add(0, 1.7, 0), ClothesRune.HOOD_BANISHING.rune(), 8, 0.35);
            com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.RISE, player.getEyePosition(), PALE_LIGHT, 0.5F);
        }
        int purification = strength(player, ClothesRune.HOOD_PURIFICATION);
        if (purification > 0 && random.nextFloat() < HolderRune.at(ClothesRune.PURIFY, purification)) {
            for (var bad : java.util.List.of(MobEffects.POISON, MobEffects.HUNGER, MobEffects.WEAKNESS, MobEffects.CONFUSION, MobEffects.MOVEMENT_SLOWDOWN)) {
                if (player.hasEffect(bad)) {
                    player.removeEffect(bad);
                    ToolEffects.puff(level, player.position().add(0, 1.6, 0), ClothesRune.HOOD_PURIFICATION.rune(), 10, 0.4);
                    com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.RISE, player.position().add(0, 1.4, 0), CLEAN_WATER, 0.8F);
                    break;
                }
            }
        }
        int mending = strength(player, ClothesRune.ROBE_MENDING);
        if (mending > 0 && player.tickCount % MEND_TICKS == 0 && mend(player, HolderRune.at(ClothesRune.MEND, mending))) {
            ToolEffects.puff(level, player.position().add(0, 1.1, 0), ClothesRune.ROBE_MENDING.rune(), 8, 0.4);
            com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.RISE, player.position().add(0, 0.9, 0), LIFE, 0.6F);
        }
        int pollen = strength(player, ClothesRune.BOOTS_POLLEN);
        if (pollen > 0 && player.tickCount % POLLEN_TICKS == 0) {
            Vec3 last = LAST_STEP.put(player, player.position());
            if (last != null && last.distanceToSqr(player.position()) > 1 && random.nextFloat() < HolderRune.at(ClothesRune.POLLEN, pollen)) {
                grow(level, player.blockPosition(), random);
            }
        }
    }

    /** Magnet: the loose items and experience within {@code radius} drift to the wearer; returns how many things moved. */
    public static int pull(ServerLevel level, Player player, int radius) {
        Vec3 target = player.position().add(0, 0.6, 0);
        AABB reach = player.getBoundingBox().inflate(radius);
        int moved = 0;
        for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, reach,
                item -> item.isAlive() && !item.hasPickUpDelay())) {
            moved += drift(item, target);
        }
        for (net.minecraft.world.entity.ExperienceOrb orb : level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, reach,
                net.minecraft.world.entity.Entity::isAlive)) {
            moved += drift(orb, target);
        }
        if (moved > 0 && level.getRandom().nextInt(3) == 0) {
            ToolEffects.puff(level, player.position().add(0, 0.8, 0), ClothesRune.TROUSERS_MAGNET.rune(), 2, 0.3);
        }
        return moved;
    }

    private static int drift(net.minecraft.world.entity.Entity entity, Vec3 target) {
        Vec3 way = target.subtract(entity.position());
        double length = way.length();
        if (length < 0.3) {
            return 0;
        }
        entity.setDeltaMovement(way.scale(Math.min(PULL_SPEED, length) / length));
        entity.hurtMarked = true;
        return 1;
    }

    /** Mends every worn piece of the clothes a little; whether anything needed it. */
    private static boolean mend(Player player, int amount) {
        boolean mended = false;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ResearcherClothesItem && stack.isDamaged()) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - amount));
                mended = true;
            }
        }
        return mended;
    }

    /** Bone meal on the nearest crop or sapling by the path that can still grow. */
    static void grow(ServerLevel level, BlockPos around, RandomSource random) {
        java.util.List<BlockPos> near = new java.util.ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-POLLEN_RADIUS, -1, -POLLEN_RADIUS), around.offset(POLLEN_RADIUS, 1, POLLEN_RADIUS))) {
            near.add(pos.immutable());
        }
        // Nearest first; among equally near ones a random pick, so a field ripens all over and not row by row.
        java.util.Collections.shuffle(near, new java.util.Random(random.nextLong()));
        near.sort(java.util.Comparator.comparingDouble(pos -> pos.distSqr(around)));
        for (BlockPos pos : near) {
            BlockState state = level.getBlockState(pos);
            if ((state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS)) && state.getBlock() instanceof BonemealableBlock plant
                    && plant.isValidBonemealTarget(level, pos, state)) {
                plant.performBonemeal(level, random, pos, state);
                level.levelEvent(1505, pos, 15);
                ToolEffects.puff(level, Vec3.atCenterOf(pos), ClothesRune.BOOTS_POLLEN.rune(), 8, 0.4);
                com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.RISE, Vec3.atBottomCenterOf(pos), LIFE, 0.6F);
                ToolEffects.sound(level, Vec3.atCenterOf(pos), SoundEvents.BONE_MEAL_USE, 0.5F, 1.1F);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        int radiance = strength(player, ClothesRune.HOOD_RADIANCE);
        if (radiance == 0 || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        float bonus = event.getOrb().getValue() * HolderRune.at(ClothesRune.MORE_XP, radiance);
        int extra = (int) bonus + (level.getRandom().nextFloat() < bonus - (int) bonus ? 1 : 0);
        if (extra > 0) {
            player.giveExperiencePoints(extra);
            ToolEffects.puff(level, player.position().add(0, 1.7, 0), ClothesRune.HOOD_RADIANCE.rune(), 3, 0.25);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        int quenching = strength(entity, ClothesRune.BOOTS_QUENCHING);
        if (quenching > 0 && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR)) {
            event.setAmount(event.getAmount() * (1 - HolderRune.at(ClothesRune.COOL_FEET, quenching)));
            if (entity.level() instanceof ServerLevel server && server.getRandom().nextInt(3) == 0) {
                // The heat hisses off the boots as steam.
                com.pockyl.hextale.network.RuneEffectPayload.show(server, com.pockyl.hextale.network.RuneEffectPayload.Family.GUST, entity.position().add(0, 0.2, 0), STEAM, 0.6F);
            }
        }
        int lift = strength(entity, ClothesRune.BOOTS_LIFT);
        if (lift > 0 && event.getSource().is(DamageTypeTags.IS_FALL) && entity.level() instanceof ServerLevel level) {
            event.setAmount(event.getAmount() * (1 - HolderRune.at(ClothesRune.CUSHION, lift)));
            ToolEffects.puff(level, entity.position().add(0, 0.2, 0), ClothesRune.BOOTS_LIFT.rune(), 10, 0.5);
            com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.GUST, entity.position().add(0, 0.2, 0), 0, 0.8F);
            ToolEffects.sound(level, entity.position(), SoundEvents.BREEZE_IDLE_GROUND, 0.4F, 1.2F);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (event.getSource().getEntity() instanceof Player striker && !striker.level().isClientSide()) {
            // A Mirage holds only until the hidden one strikes.
            CLOAKED.remove(striker.getUUID());
        }
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
            return;
        }
        int spark = strength(entity, ClothesRune.ROBE_STATIC);
        if (spark > 0 && !ToolEffects.quiet() && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != entity
                && event.getSource().getDirectEntity() == attacker && attacker.isAlive() && attacker.distanceToSqr(entity) < 36
                && level.getRandom().nextFloat() < HolderRune.at(ClothesRune.STATIC_CHANCE, spark)) {
            spark(level, entity, attacker, spark);
        }
        int mirage = strength(entity, ClothesRune.HOOD_MIRAGE);
        if (mirage > 0 && entity instanceof Player player && event.getSource().getEntity() != null
                && player.getHealth() < player.getMaxHealth() * ClothesRune.MIRAGE_HEALTH) {
            ItemStack hood = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!player.getCooldowns().isOnCooldown(hood.getItem())) {
                mirage(level, player, hood, mirage);
            }
        }
        int healing = strength(entity, ClothesRune.ROBE_HEALING);
        if (healing > 0 && level.getRandom().nextFloat() < HolderRune.at(ClothesRune.REGEN_CHANCE, healing)) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
            ToolEffects.puff(level, entity.position().add(0, 1.1, 0), ClothesRune.ROBE_HEALING.rune(), 10, 0.45);
            com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.RISE, entity.position().add(0, 0.6, 0), LIFE, 0.9F);
        }
        int mirror = strength(entity, ClothesRune.ROBE_MIRROR);
        if (mirror > 0 && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != entity
                && level.getRandom().nextFloat() < HolderRune.at(ClothesRune.MIRROR_CHANCE, mirror)) {
            attacker.hurt(level.damageSources().thorns(entity), HolderRune.at(ClothesRune.MIRROR_DAMAGE, mirror));
            ToolEffects.puff(level, attacker.position().add(0, attacker.getBbHeight() / 2, 0), ClothesRune.ROBE_MIRROR.rune(), 10, 0.4);
            com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.ARCS, entity.position().add(0, entity.getBbHeight() * 0.6, 0),
                    java.util.List.of(net.minecraft.core.BlockPos.containing(attacker.position().add(0, attacker.getBbHeight() / 2, 0))),
                    ClothesRune.ROBE_MIRROR.rune().color(), 1);
        }
        int fireproofing = strength(entity, ClothesRune.ROBE_FIREPROOFING);
        if (fireproofing > 0 && event.getSource().is(DamageTypeTags.IS_FIRE)
                && ToolEffects.keep(entity, MobEffects.FIRE_RESISTANCE, HolderRune.at(ClothesRune.FIRE_SHIELD, fireproofing), 0)) {
            ToolEffects.puff(level, entity.position().add(0, 1.1, 0), ClothesRune.ROBE_FIREPROOFING.rune(), 12, 0.5);
            com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.RISE, entity.position().add(0, 0.3, 0), com.pockyl.hextale.registry.ModParticles.FIRE, 1);
        }
    }

    /**
     * Static: a spark jumps from the robe into whoever struck in close combat - it hurts and numbs them (Slowness IV for a
     * moment), drawn as a twitching bolt from the robe to them with a soft crackle.
     */
    public static void spark(ServerLevel level, LivingEntity wearer, LivingEntity attacker, int stars) {
        Vec3 from = wearer.position().add(0, wearer.getBbHeight() * 0.55, 0);
        Vec3 to = attacker.position().add(0, attacker.getBbHeight() * 0.55, 0);
        ToolEffects.quietly(() -> attacker.hurt(level.damageSources().thorns(wearer), HolderRune.at(ClothesRune.STATIC_DAMAGE, stars)));
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HolderRune.at(ClothesRune.STATIC_STUN, stars), 3, false, true, true));
        int color = ClothesRune.ROBE_STATIC.rune().color();
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.ZAP, from, to, color, 1);
        ToolEffects.puff(level, to, ClothesRune.ROBE_STATIC.rune(), 8, 0.3);
        level.playSound(null, to.x, to.y, to.z, ModSounds.RUNE_CRACKLE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.3F,
                1.2F + level.getRandom().nextFloat() * 0.2F);
    }

    /**
     * Mirage: the wearer fades (Invisibility), the creatures hunting them within {@link #MIRAGE_REACH} lose the trail and go
     * to look where they stood, and for the time unseen no creature can take them for a target again - until they strike.
     * A shimmering figure of light stays on the spot. The hood then rests for its cooldown.
     */
    public static void mirage(ServerLevel level, Player player, ItemStack hood, int stars) {
        int ticks = HolderRune.at(ClothesRune.MIRAGE_TICKS, stars);
        player.getCooldowns().addCooldown(hood.getItem(), HolderRune.at(ClothesRune.MIRAGE_COOLDOWN, stars));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        CLOAKED.put(player.getUUID(), level.getGameTime() + ticks);
        Vec3 spot = player.position();
        for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, player.getBoundingBox().inflate(MIRAGE_REACH),
                mob -> mob.getTarget() == player || mob.getBrain().checkMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET,
                        net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)
                        && mob.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)
                        .filter(target -> target == player).isPresent())) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
            mob.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.0);
        }
        int color = ClothesRune.HOOD_MIRAGE.rune().color();
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.MIRAGE, spot, spot, color, player.getBbHeight());
        ToolEffects.puff(level, spot.add(0, player.getBbHeight() * 0.6, 0), ClothesRune.HOOD_MIRAGE.rune(), 10, 0.4);
        level.playSound(null, spot.x, spot.y, spot.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, net.minecraft.sounds.SoundSource.PLAYERS, 0.3F,
                1.15F + level.getRandom().nextFloat() * 0.15F);
    }

    /** Whether a Mirage keeps the player out of the creatures' minds now. */
    public static boolean cloaked(Player player) {
        Long until = CLOAKED.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() >= until || !player.isAlive()) {
            CLOAKED.remove(player.getUUID());
            return false;
        }
        return true;
    }

    /** No creature takes a Mirage-cloaked player for its target. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player player && !player.level().isClientSide() && cloaked(player)) {
            event.setCanceled(true);
        }
    }

    /** Trousers and boots: steady bonuses while worn. */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        RuneHolder holder = RuneHolder.of(stack);
        if (holder == RuneHolder.TROUSERS) {
            PickaxeRunes runes = RuneHolder.TROUSERS.runes(stack);
            add(event, runes, ClothesRune.TROUSERS_HASTE, Attributes.MOVEMENT_SPEED, ClothesRune.SPEED,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE, EquipmentSlotGroup.LEGS);
            add(event, runes, ClothesRune.TROUSERS_WINDMILL, Attributes.JUMP_STRENGTH, ClothesRune.JUMP,
                    AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.LEGS);
            add(event, runes, ClothesRune.TROUSERS_GRAVITY, Attributes.KNOCKBACK_RESISTANCE, ClothesRune.STEADY,
                    AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.LEGS);
            add(event, runes, ClothesRune.TROUSERS_SHIFT, Attributes.STEP_HEIGHT, ClothesRune.STEP,
                    AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.LEGS);
        } else if (holder == RuneHolder.BOOTS) {
            add(event, RuneHolder.BOOTS.runes(stack), ClothesRune.BOOTS_WAVEWALK, Attributes.WATER_MOVEMENT_EFFICIENCY, ClothesRune.SWIM,
                    AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.FEET);
        }
    }

    private static void add(ItemAttributeModifierEvent event, PickaxeRunes runes, ClothesRune rune,
                            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, float[] table,
                            AttributeModifier.Operation operation, EquipmentSlotGroup slot) {
        int strength = runes.strength(rune.rune());
        if (strength > 0) {
            event.addModifier(attribute, new AttributeModifier(Hextale.id("clothes/" + rune.name().toLowerCase(java.util.Locale.ROOT)),
                    HolderRune.at(table, strength), operation), slot);
        }
    }
}
