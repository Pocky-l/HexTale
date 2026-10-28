package com.pockyl.hextale.thevoid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModTags;

/**
 * Void Mites latched onto a player. Each one drinks away {@link #HEALTH_PER_MITE} of the player's maximum health and
 * stays until the player takes a potion of healing - drunk, splashed or a lingering cloud of it - which makes all of
 * them let go. Dying shakes them off too. The count lives on the player (saved, synced so the mites show on them).
 * Mites come out of blighted land around players at night or in the dark ({@link #spawnAround}).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class VoidMites {
    public static final double HEALTH_PER_MITE = 2;
    /** Never take the last heart: at most this many hold on at once. */
    public static final int MAX = 9;
    private static final ResourceLocation MODIFIER = Hextale.id("void_mites");
    private static final int MOTE = 0x2A1C34;
    /** Ticks between two looks for blighted ground to send mites out of, per player. */
    public static final int SPAWN_TICKS = 100;
    /** Mites around a player before no more come out. */
    public static final int CROWD = 6;

    private VoidMites() {
    }

    public static int count(Player player) {
        return player.getData(ModAttachments.VOID_MITES);
    }

    /** A mite latches on (false when no more fit, so it keeps crawling). */
    public static boolean latch(ServerPlayer player) {
        if (count(player) >= room(player)) {
            return false;
        }
        set(player, count(player) + 1);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.HOSTILE, 0.9F, 0.6F);
        level.sendParticles(ModParticles.voidMote(MOTE), player.getX(), player.getY() + 1, player.getZ(), 10, 0.3, 0.5, 0.3, 0);
        com.pockyl.hextale.quest.Quests.fire(player, com.pockyl.hextale.quest.Quests.VOID_MITE_LATCHED);
        return true;
    }

    /** How many can hold on: one heart is always left. */
    private static int room(Player player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        double base = health == null ? 20 : health.getValue() + count(player) * HEALTH_PER_MITE;
        return Math.min(MAX, (int) ((base - 2) / HEALTH_PER_MITE));
    }

    /** All of them let go and burst into dark motes. */
    public static void shakeOff(ServerPlayer player) {
        int mites = count(player);
        if (mites == 0) {
            return;
        }
        set(player, 0);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ModParticles.voidMote(MOTE), player.getX(), player.getY() + 1, player.getZ(), 12 * mites, 0.4, 0.6, 0.4, 0);
        level.sendParticles(ModParticles.glint(0xD0469E), player.getX(), player.getY() + 1, player.getZ(), 4 * mites, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.SCULK_BLOCK_BREAK, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    public static void set(ServerPlayer player, int mites) {
        player.setData(ModAttachments.VOID_MITES, Mth.clamp(mites, 0, MAX));
        com.pockyl.hextale.port.PortSync.syncData(player, ModAttachments.VOID_MITES);
        apply(player);
    }

    /** Puts the player's maximum health in step with the mites on them. */
    public static void apply(Player player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(MODIFIER);
        int mites = count(player);
        if (mites > 0) {
            health.addTransientModifier(new AttributeModifier(MODIFIER, -mites * HEALTH_PER_MITE, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static boolean heals(PotionContents contents) {
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().is(MobEffects.HEAL)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && heals(event.getItem().get(DataComponents.POTION_CONTENTS))) {
            shakeOff(player);
        }
    }

    /** A splash of healing reaches every player a splash potion would (lingering ones are caught by their cloud). */
    @SubscribeEvent
    public static void onSplash(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownPotion potion) || !(potion.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack item = potion.getItem();
        if (!heals(item.get(DataComponents.POTION_CONTENTS))) {
            return;
        }
        Vec3 at = event.getRayTraceResult().getLocation();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(at, at).inflate(4, 2, 4))) {
            shakeOff(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long time = player.level().getGameTime();
        if (count(player) > 0 && time % 10 == 0) {
            for (AreaEffectCloud cloud : player.level().getEntitiesOfClass(AreaEffectCloud.class, player.getBoundingBox())) {
                if (heals(cloud.potionContents)) {
                    shakeOff(player);
                    break;
                }
            }
        }
        if ((time + player.getId()) % SPAWN_TICKS == 0) {
            spawnAround(player);
        }
    }

    /** Out of blighted ground near the player, in the dark: one or two mites crawl up, unless enough are about. */
    static void spawnAround(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (player.isCreative() || player.isSpectator() || Thinning.get(level, player.blockPosition()) < Thinning.Stage.BLIGHT.from * 0.9F) {
            return;
        }
        if (level.getEntitiesOfClass(VoidMite.class, player.getBoundingBox().inflate(24)).size() >= CROWD) {
            return;
        }
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 8 + random.nextDouble() * 12;
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
        if (!level.getBlockState(ground).is(ModTags.VOID_GROUND) || Thinning.get(level, ground) < Thinning.Stage.BLIGHT.from) {
            return;
        }
        BlockPos at = ground.above();
        if (level.getMaxLocalRawBrightness(at) > 7 && level.isDay()) {
            return;
        }
        int count = 1 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            VoidMite mite = ModEntities.VOID_MITE.get().create(level);
            if (mite == null) {
                return;
            }
            mite.moveTo(at.getX() + 0.3 + random.nextDouble() * 0.4, at.getY(), at.getZ() + 0.3 + random.nextDouble() * 0.4,
                    random.nextFloat() * 360, 0);
            level.addFreshEntity(mite);
        }
        level.sendParticles(ModParticles.voidMote(MOTE), at.getX() + 0.5, at.getY() + 0.05, at.getZ() + 0.5, 10, 0.3, 0.05, 0.3, 0);
        level.playSound(null, at, SoundEvents.SCULK_BLOCK_STEP, SoundSource.HOSTILE, 0.8F, 0.6F);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        apply(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        apply(event.getEntity());
    }
}
