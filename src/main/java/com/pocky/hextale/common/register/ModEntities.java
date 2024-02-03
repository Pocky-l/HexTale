package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.entity.GeoVoidlingEntity;
import com.pocky.hextale.common.world.entity.projectile.ThrownHextech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HexTaleMod.MODID);

    public static final RegistryObject<EntityType<ThrownHextech>> HEXTECH = ENTITY_TYPES.register("hextech", () ->
            EntityType.Builder.<ThrownHextech>of(ThrownHextech::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(new ResourceLocation(HexTaleMod.MODID, "hextech").toString())
    );

    public static final RegistryObject<EntityType<GeoVoidlingEntity>> VOIDLING_ENTITY = registerMob("voidling", GeoVoidlingEntity::new,
            0.7f, 1.3f, 0x1F1F1F, 0x0D0D0D);

    public static <T extends Mob> RegistryObject<EntityType<T>> registerMob(String name, EntityType.EntityFactory<T> entity,
                                                                            float width, float height, int primaryEggColor, int secondaryEggColor) {
        RegistryObject<EntityType<T>> entityType = ENTITY_TYPES.register(name,
                () -> EntityType.Builder.of(entity, MobCategory.CREATURE).sized(width, height).build(name));

        RegistryObject<Item> spawnEgg = ModItems.ITEMS.register(name + "_spawn_egg",
                () -> new ForgeSpawnEggItem(entityType, primaryEggColor, secondaryEggColor, new Item.Properties()));

        ModItems.EGGS.add(spawnEgg);

        return entityType;
    }
}
