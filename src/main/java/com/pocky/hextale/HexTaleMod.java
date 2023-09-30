package com.pocky.hextale;

import com.mojang.logging.LogUtils;
import com.pocky.hextale.client.events.BlockEntityRendererRegisterEvent;
import com.pocky.hextale.client.events.EntityRendererRegisterEvent;
import com.pocky.hextale.client.events.ScreenRegisterEvent;
import com.pocky.hextale.common.events.RightClickBlockEvent;
import com.pocky.hextale.common.events.points.PointProgressEvent;
import com.pocky.hextale.common.recipe.ModRecipeSerializer;
import com.pocky.hextale.common.register.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(HexTaleMod.MODID)
@Mod.EventBusSubscriber(modid = HexTaleMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HexTaleMod {

    public static final String MODID = "hextale";

    private static final Logger LOGGER = LogUtils.getLogger();

    public HexTaleMod() {
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);

        ModItems.ITEMS.register(modEventBus);

        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        ModCreativeTab.CREATIVE_MODE_TABS.register(modEventBus);

        ModMenuTypes.REGISTRY_MENU.register(modEventBus);

        ModRecipeTypes.REGISTRY_RECIPE.register(modEventBus);
        ModRecipeSerializer.REGISTRY_RECIPE_SERIALIZERS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(new EntityRendererRegisterEvent());
        MinecraftForge.EVENT_BUS.register(new BlockEntityRendererRegisterEvent());
        MinecraftForge.EVENT_BUS.register(new ScreenRegisterEvent());
        MinecraftForge.EVENT_BUS.register(new RightClickBlockEvent());

        MinecraftForge.EVENT_BUS.register(new PointProgressEvent());
        MinecraftForge.EVENT_BUS.register(this);
    }
}
