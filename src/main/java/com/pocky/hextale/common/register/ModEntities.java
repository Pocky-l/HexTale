package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.world.entity.projectile.ThrownHextech;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

}
