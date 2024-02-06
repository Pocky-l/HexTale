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
                        output.accept(ModItems.FLAWLESS_HEXTECH_CRYSTAL.get());

                        output.accept(ModItems.HEXTECH_SHARD.get());
                        output.accept(ModBlocks.BUDDING_HEXTECH_ITEM.get());
                        output.accept(ModBlocks.HEXTECH_CLUSTER_ITEM.get());
                        output.accept(ModBlocks.LARGE_HEXTECH_BUD_ITEM.get());
                        output.accept(ModBlocks.MEDIUM_HEXTECH_BUD_ITEM.get());
                        output.accept(ModBlocks.SMALL_HEXTECH_BUD_ITEM.get());

                        output.accept(ModItems.SHIMMER.get());

                        output.accept(ModItems.HEX_GLASS.get());
                        output.accept(ModItems.MASTER_HEX_GLASS.get());
                        output.accept(ModItems.EXPERTLY_HEX_GLASS.get());
                        output.accept(ModItems.CLOUDY_LENS.get());
                        output.accept(ModItems.BASE_LENS.get());
                        output.accept(ModItems.MASTER_LENS.get());
                        output.accept(ModItems.EXPERTLY_LENS.get());

                        output.accept(ModItems.COPPER_DISK.get());
                        output.accept(ModItems.IRON_DISK.get());
                        output.accept(ModItems.GOLD_DISK.get());
                        output.accept(ModItems.DIAMOND_DISK.get());

                        output.accept(ModItems.CRYSTAL_SWORD.get());
                        output.accept(ModItems.CRYSTAL_PICKAXE.get());
                        output.accept(ModItems.CRYSTAL_AXE.get());
                        output.accept(ModItems.CRYSTAL_SHOVEL.get());
                        output.accept(ModItems.CRYSTAL_HAMMER.get());

                        output.accept(ModItems.HEXTECH_HELMET.get());
                        output.accept(ModItems.HEXTECH_CHESTPLATE.get());
                        output.accept(ModItems.HEXTECH_LEGGINGS.get());
                        output.accept(ModItems.HEXTECH_BOOTS.get());

                        output.accept(ModItems.HEX_JETPACK.get());
                        output.accept(ModItems.HEX_ELYTRA.get());

                        output.accept(ModBlocks.SUSPICIOUS_REMAINS_GRAVEL_ITEM.get());
                        output.accept(ModBlocks.BASE_CUTTING_MACHINE_ITEM.get());
                        output.accept(ModBlocks.ADVANCED_CUTTING_MACHINE_ITEM.get());
                        output.accept(ModBlocks.ELITE_CUTTING_MACHINE_ITEM.get());
                        output.accept(ModBlocks.MASTER_CUTTING_MACHINE_ITEM.get());

                        output.accept(ModBlocks.HEXCORE.get());

                        ModItems.EGGS.forEach((e) -> output.accept(e.get()));
                    })
                    .build());

}
