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
                            com.pockyl.hextale.thevoid.VoidMite::new, MobCategory.MONSTER).noLootTable().sized(0.5F, 0.3F)
                    .eyeHeight(0.15F).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** A voidling: pushed out of a breach of the Void, runs at players, leaps and bites. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.pockyl.hextale.thevoid.Voidling>> VOIDLING =
            ENTITIES.register("voidling", id -> EntityType.Builder.<com.pockyl.hextale.thevoid.Voidling>of(
                            com.pockyl.hextale.thevoid.Voidling::new, MobCategory.MONSTER).noLootTable().sized(0.9F, 1.0F)
                    .eyeHeight(0.7F).clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

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
    }
}
