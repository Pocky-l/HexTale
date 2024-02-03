package com.pocky.hextale.common.events;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.register.ModEntities;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonListener {

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {

        AttributeSupplier.Builder genericAttribs = Monster.createMobAttributes()
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 8);

        event.put(ModEntities.VOIDLING_ENTITY.get(), genericAttribs.build());
    }
}
