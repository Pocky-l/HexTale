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
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModItems;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What the runes on the researcher's clothes do (see {@link ClothesRune}). Chances and buffs show with a puff in the
 * rune's colour and a soft sound; the trousers' and boots' runes are steady attribute bonuses while worn.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class ClothesEffects {
    /** How often the robe's Mending works. */
    public static final int MEND_TICKS = 200;
    /** How often, while walking, the boots' Pollen may find a plant. */
    public static final int POLLEN_TICKS = 40;
    /** Ticks between two pulls of Magnet, and how fast things drift to the wearer (blocks per tick). */
    public static final int PULL_TICKS = 4;
    public static final double PULL_SPEED = 0.45;
    private static final int POLLEN_RADIUS = 3;
    private static final int DARK = 7;
    private static final Map<Player, Vec3> LAST_STEP = new WeakHashMap<>();

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
            ToolEffects.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.35F, 1.3F);
        }
        int purification = strength(player, ClothesRune.HOOD_PURIFICATION);
        if (purification > 0 && random.nextFloat() < HolderRune.at(ClothesRune.PURIFY, purification)) {
            for (var bad : java.util.List.of(MobEffects.POISON, MobEffects.HUNGER, MobEffects.WEAKNESS, MobEffects.NAUSEA, MobEffects.SLOWNESS)) {
                if (player.hasEffect(bad)) {
                    player.removeEffect(bad);
                    ToolEffects.puff(level, player.position().add(0, 1.6, 0), ClothesRune.HOOD_PURIFICATION.rune(), 10, 0.4);
                    ToolEffects.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4F, 1.6F);
                    break;
                }
            }
        }
        int mending = strength(player, ClothesRune.ROBE_MENDING);
        if (mending > 0 && player.tickCount % MEND_TICKS == 0 && mend(player, HolderRune.at(ClothesRune.MEND, mending))) {
            ToolEffects.puff(level, player.position().add(0, 1.1, 0), ClothesRune.ROBE_MENDING.rune(), 8, 0.4);
            ToolEffects.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.3F, 0.9F);
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
        entity.needsSync = true;
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

    /** Bone meal on one crop or sapling near the path. */
    private static void grow(ServerLevel level, BlockPos around, RandomSource random) {
        for (int i = 0; i < 8; i++) {
            BlockPos pos = around.offset(random.nextInt(POLLEN_RADIUS * 2 + 1) - POLLEN_RADIUS, random.nextInt(3) - 1,
                    random.nextInt(POLLEN_RADIUS * 2 + 1) - POLLEN_RADIUS);
            BlockState state = level.getBlockState(pos);
            if ((state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS)) && state.getBlock() instanceof BonemealableBlock plant
                    && plant.isValidBonemealTarget(level, pos, state)) {
                plant.performBonemeal(level, random, pos, state);
                level.levelEvent(1505, pos, 15);
                ToolEffects.puff(level, Vec3.atCenterOf(pos), ClothesRune.BOOTS_POLLEN.rune(), 8, 0.4);
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
        }
        int lift = strength(entity, ClothesRune.BOOTS_LIFT);
        if (lift > 0 && event.getSource().is(DamageTypeTags.IS_FALL) && entity.level() instanceof ServerLevel level) {
            event.setAmount(event.getAmount() * (1 - HolderRune.at(ClothesRune.CUSHION, lift)));
            ToolEffects.puff(level, entity.position().add(0, 0.2, 0), ClothesRune.BOOTS_LIFT.rune(), 10, 0.5);
            ToolEffects.sound(level, entity.position(), SoundEvents.BREEZE_IDLE_GROUND, 0.4F, 1.2F);
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.isAlive()) {
            return;
        }
        int healing = strength(entity, ClothesRune.ROBE_HEALING);
        if (healing > 0 && level.getRandom().nextFloat() < HolderRune.at(ClothesRune.REGEN_CHANCE, healing)) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
            ToolEffects.puff(level, entity.position().add(0, 1.1, 0), ClothesRune.ROBE_HEALING.rune(), 10, 0.45);
            ToolEffects.sound(level, entity.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4F, 1.5F);
        }
        int mirror = strength(entity, ClothesRune.ROBE_MIRROR);
        if (mirror > 0 && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != entity
                && level.getRandom().nextFloat() < HolderRune.at(ClothesRune.MIRROR_CHANCE, mirror)) {
            attacker.hurtServer(level, level.damageSources().thorns(entity), HolderRune.at(ClothesRune.MIRROR_DAMAGE, mirror));
            ToolEffects.puff(level, attacker.position().add(0, attacker.getBbHeight() / 2, 0), ClothesRune.ROBE_MIRROR.rune(), 10, 0.4);
            ToolEffects.sound(level, entity.position(), SoundEvents.AMETHYST_BLOCK_HIT, 0.6F, 1.4F);
        }
        int fireproofing = strength(entity, ClothesRune.ROBE_FIREPROOFING);
        if (fireproofing > 0 && event.getSource().is(DamageTypeTags.IS_FIRE)
                && ToolEffects.keep(entity, MobEffects.FIRE_RESISTANCE, HolderRune.at(ClothesRune.FIRE_SHIELD, fireproofing), 0)) {
            ToolEffects.puff(level, entity.position().add(0, 1.1, 0), ClothesRune.ROBE_FIREPROOFING.rune(), 12, 0.5);
            ToolEffects.sound(level, entity.position(), SoundEvents.FIRECHARGE_USE, 0.5F, 0.6F);
        }
    }

    /** Trousers and boots: steady bonuses while worn. */
    @SubscribeEvent
    public static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(ModItems.RESEARCHER_TROUSERS.get())) {
            PickaxeRunes runes = RuneHolder.TROUSERS.runes(stack);
            add(event, runes, ClothesRune.TROUSERS_HASTE, Attributes.MOVEMENT_SPEED, ClothesRune.SPEED,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE, EquipmentSlotGroup.LEGS);
            add(event, runes, ClothesRune.TROUSERS_WINDMILL, Attributes.JUMP_STRENGTH, ClothesRune.JUMP,
                    AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.LEGS);
            add(event, runes, ClothesRune.TROUSERS_GRAVITY, Attributes.KNOCKBACK_RESISTANCE, ClothesRune.STEADY,
                    AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.LEGS);
            add(event, runes, ClothesRune.TROUSERS_SHIFT, Attributes.STEP_HEIGHT, ClothesRune.STEP,
                    AttributeModifier.Operation.ADD_VALUE, EquipmentSlotGroup.LEGS);
        } else if (stack.is(ModItems.RESEARCHER_BOOTS.get())) {
            add(event, RuneHolder.BOOTS.runes(stack), ClothesRune.BOOTS_PUMP, Attributes.WATER_MOVEMENT_EFFICIENCY, ClothesRune.SWIM,
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
