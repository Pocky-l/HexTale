package com.pockyl.hextale.client;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.client.render.AlembicRenderer;
import com.pockyl.hextale.client.render.EssenceTankRenderer;
import com.pockyl.hextale.client.render.EssenceTint;
import com.pockyl.hextale.client.render.FlowParticle;
import com.pockyl.hextale.client.render.HextechAltarRenderer;
import com.pockyl.hextale.client.render.PedestalRenderer;
import com.pockyl.hextale.client.render.RuneSteleRenderer;
import com.pockyl.hextale.client.render.ResearchTableRenderer;
import com.pockyl.hextale.client.render.RuneTint;
import com.pockyl.hextale.client.render.SeparatorRenderer;
import com.pockyl.hextale.client.render.TableModels;
import com.pockyl.hextale.client.screen.ResearchTableScreen;
import com.pockyl.hextale.client.screen.machine.AlembicScreen;
import com.pockyl.hextale.client.screen.machine.SeparatorScreen;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModMenus;
import com.pockyl.hextale.registry.ModParticles;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import com.pockyl.hextale.research.EssencesTooltip;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchBoards;

@Mod(value = Hextale.MOD_ID, dist = Dist.CLIENT)
public final class HextaleClient {
    public HextaleClient(ModContainer container, IEventBus modBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(HextaleClient::registerScreens);
        modBus.addListener(HextaleClient::registerRenderers);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) -> event.register(
                com.pockyl.hextale.client.render.EssenceJarItemRenderer.ID, com.pockyl.hextale.client.render.EssenceJarItemRenderer.Unbaked.MAP_CODEC));
        modBus.addListener(ClientPayloads::register);
        modBus.addListener((RegisterColorHandlersEvent.ItemTintSources event) -> event.register(Hextale.id("essence"), EssenceTint.MAP_CODEC));
        modBus.addListener((RegisterColorHandlersEvent.ItemTintSources event) -> event.register(Hextale.id("rune"), RuneTint.MAP_CODEC));
        modBus.addListener((RegisterColorHandlersEvent.ItemTintSources event) ->
                event.register(Hextale.id("sticker"), com.pockyl.hextale.client.render.StickerTint.MAP_CODEC));
        modBus.addListener((RegisterColorHandlersEvent.ItemTintSources event) ->
                event.register(Hextale.id("pickaxe_runes"), com.pockyl.hextale.client.render.PickaxeRuneTint.MAP_CODEC));
        modBus.addListener(TableModels::register);
        modBus.addListener((RegisterParticleProvidersEvent event) -> event.registerSpriteSet(ModParticles.FLOW.get(), FlowParticle.Provider::new));
        modBus.addListener(GogglesView::registerLayer);
        modBus.addListener(com.pockyl.hextale.client.screen.book.AltarDiorama::register);
        modBus.addListener(com.pockyl.hextale.client.screen.book.CircleDiorama::register);
        modBus.addListener(com.pockyl.hextale.client.render.HexGlow::registerPipelines);
        NeoForge.EVENT_BUS.addListener(GogglesView::tick);
        modBus.addListener((RegisterClientTooltipComponentFactoriesEvent event) ->
                event.register(EssencesTooltip.class, ClientEssencesTooltip::new));
        NeoForge.EVENT_BUS.addListener(HextaleClient::addElementsToTooltip);
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> {
            ItemStack stack = event.getItemStack();
            EssenceMix contents = stack.get(ModDataComponents.ESSENCE.get());
            if (stack.is(ModItems.CREATIVE_ESSENCE_TANK.get())) {
                event.getToolTip().add(1, Component.translatable("block.hextale.creative_essence_tank.all").withStyle(ChatFormatting.GRAY));
            } else if (contents != null && stack.is(ModItems.ESSENCE_TANK.get())) {
                event.getToolTip().add(1, contents.describe().append(Component.literal(" (" + contents.total() + "/"
                        + EssenceTankBlockEntity.CAPACITY + ")").withStyle(ChatFormatting.GRAY)));
            }
            // A smelter's or a pylon's runes, like a pickaxe's: name in its colour and stars.
            com.pockyl.hextale.runework.RuneHolder holder = com.pockyl.hextale.runework.RuneHolder.of(stack);
            if (holder != null && holder != com.pockyl.hextale.runework.RuneHolder.PICKAXE) {
                int line = 1;
                for (var entry : holder.runes(stack).entries()) {
                    event.getToolTip().add(line++, Component.translatable(entry.rune().translationKey()).withColor(entry.rune().color())
                            .append(Component.literal(" " + "★".repeat(entry.stars())).withStyle(ChatFormatting.GOLD)));
                }
            }
            // How a pipe's sides are set.
            if (stack.is(ModItems.BRASS_PIPE.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.brass_pipe.hint").withStyle(ChatFormatting.DARK_AQUA));
            }
            // How a pylon is set up: what it does, how to link it, how to untie it.
            if (ModItems.isPylon(stack)) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                event.getToolTip().add(Component.translatable("block.hextale." + id + ".hint").withStyle(ChatFormatting.GRAY));
                event.getToolTip().add(Component.translatable("block.hextale.hex_pylon.hint.link").withStyle(ChatFormatting.DARK_AQUA));
                event.getToolTip().add(Component.translatable("block.hextale.hex_pylon.hint.clear").withStyle(ChatFormatting.DARK_AQUA));
                event.getToolTip().add(Component.translatable("block.hextale.hex_pylon.hint.untie").withStyle(ChatFormatting.DARK_AQUA));
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ClientKnowledge.clear());
        ClientPylons.register();
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> ResearchBoards.warmUp());
    }

    /** A studied item shows its elements under its name; in creative mode every item does. */
    private static void addElementsToTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        Minecraft minecraft = Minecraft.getInstance();
        boolean creative = minecraft.player != null && minecraft.player.getAbilities().instabuild;
        if (stack.isEmpty() || !creative && !ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return;
        }
        ItemEssences elements = ResearchItems.of(stack);
        if (elements != null) {
            event.getTooltipElements().add(Math.min(1, event.getTooltipElements().size()), Either.right(new EssencesTooltip(elements)));
        }
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.RESEARCH_TABLE.get(), ResearchTableScreen::new);
        event.register(ModMenus.ALEMBIC.get(), AlembicScreen::new);
        event.register(ModMenus.HEX_SMELTER.get(), com.pockyl.hextale.client.screen.machine.HexSmelterScreen::new);
        event.register(ModMenus.ITEM_FILTER.get(), com.pockyl.hextale.client.screen.ItemFilterScreen::new);
        event.register(ModMenus.SEPARATOR.get(), SeparatorScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RESEARCH_TABLE.get(), ResearchTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ALEMBIC.get(), AlembicRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SEPARATOR.get(), SeparatorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ESSENCE_TANK.get(), EssenceTankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BIG_ESSENCE_JAR.get(), com.pockyl.hextale.client.render.BigEssenceJarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEXTECH_ALTAR.get(), HextechAltarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEX_SMELTER.get(), com.pockyl.hextale.client.render.HexSmelterRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEX_PYLON.get(), com.pockyl.hextale.client.render.HexPylonRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL.get(), PedestalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RUNE_STELE.get(), RuneSteleRenderer::new);
    }
}
