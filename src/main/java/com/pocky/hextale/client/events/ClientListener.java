package com.pocky.hextale.client.events;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.client.render.entity.VoidlingEntityRenderer;
import com.pocky.hextale.common.register.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientListener {
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VOIDLING_ENTITY.get(), VoidlingEntityRenderer::new);
    }
}
