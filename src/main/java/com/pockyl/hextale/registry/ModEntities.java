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

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
