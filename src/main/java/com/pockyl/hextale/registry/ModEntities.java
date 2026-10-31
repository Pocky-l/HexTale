package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.entity.ThrownHextech;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Hextale.MOD_ID);

    /** Unstable hextech thrown like a snowball: it shatters into hextech dust where it lands. */
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownHextech>> THROWN_HEXTECH = ENTITIES.register("thrown_hextech",
            id -> EntityType.Builder.<ThrownHextech>of(ThrownHextech::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A thrown Dust Charge: it sticks where it lands, hisses, and cracks the rock around it. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.entity.DustCharge>> DUST_CHARGE = ENTITIES.register("dust_charge",
            id -> EntityType.Builder.<com.pockyl.hextale.entity.DustCharge>of(com.pockyl.hextale.entity.DustCharge::new, MobCategory.MISC)
                    .noLootTable().sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(5)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** The Grapple Gauntlet's claw on its rope of light. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.entity.GrappleHook>> GRAPPLE_HOOK = ENTITIES.register("grapple_hook",
            id -> EntityType.Builder.<com.pockyl.hextale.entity.GrappleHook>of(com.pockyl.hextale.entity.GrappleHook::new, MobCategory.MISC)
                    .noLootTable().noSave().sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A thrown Tendril Dagger: bites, then comes back along its tendril of light. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.entity.ThrownDagger>> THROWN_DAGGER = ENTITIES.register("thrown_dagger",
            id -> EntityType.Builder.<com.pockyl.hextale.entity.ThrownDagger>of(com.pockyl.hextale.entity.ThrownDagger::new, MobCategory.MISC)
                    .noLootTable().sized(0.4F, 0.4F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A Chem Vial thrown like a splash potion: it shatters and splashes its brew. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.entity.ThrownChemVial>> THROWN_CHEM_VIAL =
            ENTITIES.register("thrown_chem_vial", id -> EntityType.Builder.<com.pockyl.hextale.entity.ThrownChemVial>of(
                            com.pockyl.hextale.entity.ThrownChemVial::new, MobCategory.MISC).noLootTable().sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** The Void hanging in the world: a breach the depth shows through. Big and static, seen from afar. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.thevoid.TheVoid>> THE_VOID =
            ENTITIES.register("the_void", id -> EntityType.Builder.<com.pockyl.hextale.thevoid.TheVoid>of(
                            com.pockyl.hextale.thevoid.TheVoid::new, MobCategory.MISC).noLootTable().sized(0.5F, 0.5F)
                    .clientTrackingRange(10).updateInterval(4).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A Void Mite: crawls out of blighted land and latches onto players. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.thevoid.VoidMite>> VOID_MITE =
            ENTITIES.register("void_mite", id -> EntityType.Builder.<com.pockyl.hextale.thevoid.VoidMite>of(
                            com.pockyl.hextale.thevoid.VoidMite::new, MobCategory.MONSTER).sized(0.5F, 0.3F)
                    .eyeHeight(0.15F).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A voidling: pushed out of a breach of the Void, runs at players, leaps and bites. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.thevoid.Voidling>> VOIDLING =
            ENTITIES.register("voidling", id -> EntityType.Builder.<com.pockyl.hextale.thevoid.Voidling>of(
                            com.pockyl.hextale.thevoid.Voidling::new, MobCategory.MONSTER).sized(0.9F, 1.0F)
                    .eyeHeight(0.7F).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A Rib Crawler: hangs under the Void's ribs and drops on whoever passes below. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.thevoid.realm.RibCrawler>> RIB_CRAWLER =
            ENTITIES.register("rib_crawler", id -> EntityType.Builder.<com.pockyl.hextale.thevoid.realm.RibCrawler>of(
                            com.pockyl.hextale.thevoid.realm.RibCrawler::new, MobCategory.MONSTER).sized(1.3F, 0.55F)
                    .eyeHeight(0.4F).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A Pit Maw: a mouth lying flush in the ground of the Void that tears open under a step. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.thevoid.realm.PitMaw>> PIT_MAW =
            ENTITIES.register("pit_maw", id -> EntityType.Builder.<com.pockyl.hextale.thevoid.realm.PitMaw>of(
                            com.pockyl.hextale.thevoid.realm.PitMaw::new, MobCategory.MONSTER).sized(2.2F, 0.6F)
                    .eyeHeight(0.5F).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A Seer: a floating eye of the Void that marks players for its creatures. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.thevoid.realm.Seer>> SEER =
            ENTITIES.register("seer", id -> EntityType.Builder.<com.pockyl.hextale.thevoid.realm.Seer>of(
                            com.pockyl.hextale.thevoid.realm.Seer::new, MobCategory.MONSTER).sized(0.8F, 0.8F)
                    .eyeHeight(0.4F).clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** The eye a player watches a breach open through (client only, never in a level's entity list). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.thevoid.BreachCamera>> BREACH_CAMERA =
            ENTITIES.register("breach_camera", id -> EntityType.Builder.<com.pockyl.hextale.thevoid.BreachCamera>of(
                            com.pockyl.hextale.thevoid.BreachCamera::new, MobCategory.MISC).noLootTable().noSave().noSummon()
                    .sized(0.1F, 0.1F).eyeHeight(1.62F).clientTrackingRange(0).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(VOID_MITE.get(), com.pockyl.hextale.thevoid.VoidMite.createAttributes().build()));
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(VOIDLING.get(), com.pockyl.hextale.thevoid.Voidling.createAttributes().build()));
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) -> {
            event.put(RIB_CRAWLER.get(), com.pockyl.hextale.thevoid.realm.RibCrawler.createAttributes().build());
            event.put(PIT_MAW.get(), com.pockyl.hextale.thevoid.realm.PitMaw.createAttributes().build());
            event.put(SEER.get(), com.pockyl.hextale.thevoid.realm.Seer.createAttributes().build());
        });
        // In the Void its own creatures come out of the dark of their own accord (the Void's biomes list them).
        modBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) -> {
            event.register(VOIDLING.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::spawnsInTheVoid,
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(VOID_MITE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::spawnsInTheVoid,
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(RIB_CRAWLER.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::spawnsInTheVoid,
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(SEER.get(), net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModEntities::seerSpawns,
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
    }

    /** Seers drift into being over the Void's ground in any light. */
    private static boolean seerSpawns(EntityType<com.pockyl.hextale.thevoid.realm.Seer> type,
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getLevel().dimension().equals(com.pockyl.hextale.thevoid.realm.VoidPassage.LEVEL)
                && level.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL;
    }

    private static <T extends net.minecraft.world.entity.monster.Monster> boolean spawnsInTheVoid(EntityType<T> type,
            net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getLevel().dimension().equals(com.pockyl.hextale.thevoid.realm.VoidPassage.LEVEL)
                && net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
    }
}
