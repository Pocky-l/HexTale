package com.pocky.hextale.common.register;

import com.pocky.hextale.HexTaleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HexTaleMod.MODID);

    public static final RegistryObject<CreativeModeTab> HEXTALE_TAB = CREATIVE_MODE_TABS.register("hextale_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hextale.hextale_tab"))
                    .icon(ModItems.RAW_HEXTECH_CRYSTAL.get()::getDefaultInstance)
                    .displayItems((displayParameters, output) -> {
                        output.accept(ModItems.RAW_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.PROCESSED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.METICULOUSLY_PROCESSED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.EXPERTLY_PROCESSED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.PURIFIED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.METICULOUSLY_PURIFIED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.EXPERTLY_PURIFIED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.POLISHED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.METICULOUSLY_POLISHED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.EXPERTLY_POLISHED_HEXTECH_CRYSTAL.get());
                        output.accept(ModItems.FLAWLESS_HEXTECH_CRYSTAL.get());

                        output.accept(ModItems.SHIMMER.get());

                        output.accept(ModItems.HEXTECH_SWORD.get());
                        output.accept(ModItems.HEXTECH_PICKAXE.get());
                        output.accept(ModItems.HEXTECH_AXE.get());
                        output.accept(ModItems.HEXTECH_SHOVEL.get());

                        output.accept(ModItems.HEXTECH_HELMET.get());
                        output.accept(ModItems.HEXTECH_CHESTPLATE.get());
                        output.accept(ModItems.HEXTECH_LEGGINGS.get());
                        output.accept(ModItems.HEXTECH_BOOTS.get());

                        output.accept(ModBlocks.SUSPICIOUS_REMAINS_GRAVEL_ITEM.get());
                        output.accept(ModBlocks.BASE_CUTTING_MACHINE_ITEM.get());
                        output.accept(ModBlocks.ADVANCED_CUTTING_MACHINE_ITEM.get());
                        output.accept(ModBlocks.ELITE_CUTTING_MACHINE_ITEM.get());
                        output.accept(ModBlocks.MASTER_CUTTING_MACHINE_ITEM.get());
                    })
                    .build());

}
