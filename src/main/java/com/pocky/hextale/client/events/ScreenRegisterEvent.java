package com.pocky.hextale.client.events;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.client.screens.*;
import com.pocky.hextale.common.register.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ScreenRegisterEvent {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(ModMenuTypes.BASE_CUTTING_MACHINE.get(), BaseCuttingMachineScreen::new);
        MenuScreens.register(ModMenuTypes.ADVANCED_CUTTING_MACHINE.get(), AdvancedCuttingMachineScreen::new);
        MenuScreens.register(ModMenuTypes.ELITE_CUTTING_MACHINE.get(), EliteCuttingMachineScreen::new);
        MenuScreens.register(ModMenuTypes.MASTER_CUTTING_MACHINE.get(), MasterCuttingMachineScreen::new);
    }
}
