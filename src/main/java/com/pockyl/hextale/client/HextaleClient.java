package com.pockyl.hextale.client;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.render.AlembicRenderer;
import com.pockyl.hextale.client.render.ResearchTableRenderer;
import com.pockyl.hextale.client.render.RuneScrollRenderer;
import com.pockyl.hextale.client.render.TableModels;
import com.pockyl.hextale.client.screen.ResearchTableScreen;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModMenus;
import com.pockyl.hextale.research.ElementsTooltip;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.RuneBoards;

@Mod(value = Hextale.MOD_ID, dist = Dist.CLIENT)
public final class HextaleClient {
    public HextaleClient(ModContainer container, IEventBus modBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(HextaleClient::registerScreens);
        modBus.addListener(HextaleClient::registerRenderers);
        modBus.addListener((RegisterSpecialModelRendererEvent event) ->
                event.register(RuneScrollRenderer.ID, RuneScrollRenderer.Unbaked.MAP_CODEC));
        modBus.addListener(ClientPayloads::register);
        modBus.addListener(TableModels::register);
        modBus.addListener((RegisterClientTooltipComponentFactoriesEvent event) ->
                event.register(ElementsTooltip.class, ClientElementsTooltip::new));
        NeoForge.EVENT_BUS.addListener(HextaleClient::addElementsToTooltip);
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientKnowledge.clear());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> RuneBoards.warmUp());
    }

    /** A studied item shows its elements under its name; in creative mode every item does. */
    private static void addElementsToTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        Minecraft minecraft = Minecraft.getInstance();
        boolean creative = minecraft.player != null && minecraft.player.getAbilities().instabuild;
        if (stack.isEmpty() || !creative && !ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return;
        }
        ItemElements elements = ResearchItems.of(stack);
        if (elements != null) {
            event.getTooltipElements().add(Math.min(1, event.getTooltipElements().size()), Either.right(new ElementsTooltip(elements)));
        }
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RESEARCH_TABLE.get(), ResearchTableScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RESEARCH_TABLE.get(), ResearchTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ALEMBIC.get(), AlembicRenderer::new);
    }
}
