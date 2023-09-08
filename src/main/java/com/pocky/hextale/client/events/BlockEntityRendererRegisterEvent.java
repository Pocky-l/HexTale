package com.pocky.hextale.client.events;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.client.render.block.SuspiciousRemainsGravelRenderer;
import com.pocky.hextale.common.register.ModBlockEntities;
import com.pocky.hextale.common.register.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockEntityRendererRegisterEvent {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SUSPICIOUS_REMAINS_GRAVEL_BLOCK.get(), context -> new SuspiciousRemainsGravelRenderer());
    }

    @SubscribeEvent
    public static void registerRenderers(final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SUSPICIOUS_REMAINS_GRAVEL.get(), RenderType.translucent());
    }
}
