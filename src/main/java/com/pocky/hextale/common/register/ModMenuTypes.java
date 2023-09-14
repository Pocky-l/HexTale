package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import com.pocky.hextale.common.menu.BaseCuttingMachineMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> REGISTRY_MENU = DeferredRegister.create(ForgeRegistries.MENU_TYPES, HexTaleMod.MODID);

    public static final RegistryObject<MenuType<BaseCuttingMachineMenu>> BASE_CUTTING_MACHINE = REGISTRY_MENU
            .register("base_cutting_machine_menu", () -> IForgeMenuType.create(BaseCuttingMachineMenu::new));

}
