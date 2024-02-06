package com.pocky.hextale.client.events;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.client.render.block.*;
import com.pocky.hextale.common.register.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

        event.registerBlockEntityRenderer(ModBlockEntities.HEXCORE.get(), context -> new HexcoreRenderer());
    }
}
