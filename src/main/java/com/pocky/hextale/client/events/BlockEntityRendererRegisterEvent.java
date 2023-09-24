package com.pocky.hextale.client.events;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.client.render.block.CuttingMachineModel;
import com.pocky.hextale.client.render.block.CuttingMachineRenderer;
import com.pocky.hextale.client.render.block.SuspiciousRemainsGravelRenderer;
import com.pocky.hextale.common.register.ModBlockEntities;
import com.pocky.hextale.common.register.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockEntityRendererRegisterEvent {

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SUSPICIOUS_REMAINS_GRAVEL_BLOCK.get(), context -> new SuspiciousRemainsGravelRenderer());
        event.registerBlockEntityRenderer(ModBlockEntities.BASE_CUTTING_MACHINE.get(),
                context -> new CuttingMachineRenderer<>(new CuttingMachineModel<>("base_cutting_machine")));
        event.registerBlockEntityRenderer(ModBlockEntities.ADVANCED_CUTTING_MACHINE.get(),
                context -> new CuttingMachineRenderer<>(new CuttingMachineModel<>("advanced_cutting_machine")));
        event.registerBlockEntityRenderer(ModBlockEntities.ELITE_CUTTING_MACHINE.get(),
                context -> new CuttingMachineRenderer<>(new CuttingMachineModel<>("elite_cutting_machine")));
        event.registerBlockEntityRenderer(ModBlockEntities.MASTER_CUTTING_MACHINE.get(),
                context -> new CuttingMachineRenderer<>(new CuttingMachineModel<>("master_cutting_machine")));
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerRenderers(final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SUSPICIOUS_REMAINS_GRAVEL.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.BASE_CUTTING_MACHINE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.ADVANCED_CUTTING_MACHINE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.ELITE_CUTTING_MACHINE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.MASTER_CUTTING_MACHINE.get(), RenderType.translucent());
    }
}
