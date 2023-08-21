package com.pocky.hextale.client.events;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.register.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRendererRegisterEvent {

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        EntityRenderers.register(ModEntities.HEXTECH.get(), ThrownItemRenderer::new);
    }
}
