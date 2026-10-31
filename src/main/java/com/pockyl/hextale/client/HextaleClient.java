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
import com.pockyl.hextale.client.particle.EssenceBubbleParticle;
import com.pockyl.hextale.client.particle.EssenceDropParticle;
import com.pockyl.hextale.client.particle.EssenceWispParticle;
import com.pockyl.hextale.client.particle.FlowParticle;
import com.pockyl.hextale.client.particle.GlintParticle;
import com.pockyl.hextale.client.particle.HexFlameParticle;
import com.pockyl.hextale.client.particle.HexRingParticle;
import com.pockyl.hextale.client.particle.HexSparkParticle;
import com.pockyl.hextale.client.particle.MagicLeafParticle;
import com.pockyl.hextale.client.particle.RuneShardParticle;
import com.pockyl.hextale.client.particle.ShimmerMoteParticle;
import com.pockyl.hextale.client.particle.VapourParticle;
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
        com.pockyl.hextale.port.client.SpecialItemModels.register(
                com.pockyl.hextale.client.render.EssenceJarItemRenderer.ID, com.pockyl.hextale.client.render.EssenceJarItemRenderer.Unbaked.MAP_CODEC);
        ClientPayloads.register(new com.pockyl.hextale.port.RegisterClientPayloadHandlersEvent());
        com.pockyl.hextale.port.client.SpecialItemModels.register(
                com.pockyl.hextale.client.render.HexcoreItemRenderer.ID, com.pockyl.hextale.client.render.HexcoreItemRenderer.Unbaked.MAP_CODEC);
        com.pockyl.hextale.port.client.SpecialItemModels.register(
                com.pockyl.hextale.client.render.ToolAuraRenderer.ID, com.pockyl.hextale.client.render.ToolAuraRenderer.Unbaked.MAP_CODEC);
        com.pockyl.hextale.port.client.ItemTints.register(Hextale.id("essence"), EssenceTint.MAP_CODEC);
        com.pockyl.hextale.port.client.ItemTints.register(Hextale.id("rune"), RuneTint.MAP_CODEC);
        com.pockyl.hextale.port.client.ItemTints.register(Hextale.id("sticker"), com.pockyl.hextale.client.render.StickerTint.MAP_CODEC);
        com.pockyl.hextale.port.client.ItemTints.register(Hextale.id("brew"), com.pockyl.hextale.client.render.BrewTint.MAP_CODEC);
        com.pockyl.hextale.port.client.ItemTints.register(Hextale.id("pickaxe_runes"), com.pockyl.hextale.client.render.PickaxeRuneTint.MAP_CODEC);
        modBus.addListener(TableModels::register);
        modBus.addListener(com.pockyl.hextale.client.render.EssenceFluidModel::register);
        modBus.addListener((RegisterParticleProvidersEvent event) -> {
            event.registerSpriteSet(ModParticles.FLOW.get(), FlowParticle.Provider::new);
            event.registerSpecial(ModParticles.ITEM_DRAW.get(), new com.pockyl.hextale.client.particle.ItemDrawParticle.Provider());
            event.registerSpriteSet(ModParticles.VOID_MOTE.get(), com.pockyl.hextale.client.particle.VoidMoteParticle.Provider::new);
            event.registerSpriteSet(ModParticles.RUNE_SHARD.get(), RuneShardParticle.Provider::new);
            event.registerSpriteSet(ModParticles.ESSENCE_WISP.get(), EssenceWispParticle.Provider::new);
            event.registerSpriteSet(ModParticles.ESSENCE_DROP.get(), EssenceDropParticle.Provider::new);
            event.registerSpriteSet(ModParticles.ESSENCE_BUBBLE.get(), EssenceBubbleParticle.Provider::new);
            event.registerSpriteSet(ModParticles.VAPOUR.get(), VapourParticle.Provider::new);
            event.registerSpriteSet(ModParticles.THINNING_HAZE.get(), com.pockyl.hextale.client.particle.ThinningHazeParticle.Provider::new);
            event.registerSpriteSet(ModParticles.HEX_SPARK.get(), HexSparkParticle.Provider::new);
            event.registerSpriteSet(ModParticles.GLINT.get(), GlintParticle.Provider::new);
            event.registerSpriteSet(ModParticles.HEX_RING.get(), HexRingParticle.Provider::new);
            event.registerSpriteSet(ModParticles.HEX_FLAME.get(), HexFlameParticle.Provider::new);
            event.registerSpriteSet(ModParticles.MAGIC_LEAF.get(), MagicLeafParticle.Provider::new);
            event.registerSpriteSet(ModParticles.SHIMMER_MOTE.get(), ShimmerMoteParticle.Provider::new);
            event.registerSpriteSet(ModParticles.HEART_SHARD.get(), com.pockyl.hextale.client.particle.HeartShardParticle.Provider::new);
        });
        modBus.addListener(GogglesView::registerLayer);
        com.pockyl.hextale.compat.curios.CuriosClientCompat.register(modBus);
        modBus.addListener(com.pockyl.hextale.client.render.SkypackLayer::addLayers);
        modBus.addListener(com.pockyl.hextale.client.render.VoidMiteLayer::addLayers);
        modBus.addListener(com.pockyl.hextale.client.render.ShimmerVeinsLayer::addLayers);
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(
                com.pockyl.hextale.client.render.VoidMiteModel.LAYER, com.pockyl.hextale.client.render.VoidMiteModel::createBodyLayer));
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> event.registerLayerDefinition(
                com.pockyl.hextale.client.render.VoidlingModel.LAYER, com.pockyl.hextale.client.render.VoidlingModel::createBodyLayer));
        modBus.addListener(com.pockyl.hextale.client.render.ResearcherClothesClient::registerLayers);
        modBus.addListener(com.pockyl.hextale.client.render.ResearcherClothesClient::bake);
        modBus.addListener(com.pockyl.hextale.client.render.ResearcherClothesClient::registerExtensions);
        modBus.addListener(com.pockyl.hextale.client.render.BulwarkClient::registerLayers);
        modBus.addListener(com.pockyl.hextale.client.render.BulwarkClient::bake);
        modBus.addListener(com.pockyl.hextale.client.render.BulwarkClient::registerExtensions);
        modBus.addListener(com.pockyl.hextale.client.render.BulwarkHeartLayer::addLayers);
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) -> com.pockyl.hextale.client.render.BulwarkAuraLayer.addLayers(event));
        com.pockyl.hextale.port.client.SpecialItemModels.register(
                com.pockyl.hextale.client.render.BulwarkItemRenderer.ID, com.pockyl.hextale.client.render.BulwarkItemRenderer.Unbaked.MAP_CODEC);
        modBus.addListener(ShapeParticles::register);
        com.pockyl.hextale.port.client.SpecialItemModels.register(
                com.pockyl.hextale.client.render.ResearcherClothesItemRenderer.ID,
                com.pockyl.hextale.client.render.ResearcherClothesItemRenderer.Unbaked.MAP_CODEC);
        SkypackClient.register();
        com.pockyl.hextale.client.screen.book.AltarDiorama.register();
        com.pockyl.hextale.client.screen.book.CircleDiorama.register();
        com.pockyl.hextale.client.screen.MultiblockDiorama.register();
        com.pockyl.hextale.port.client.SubmitCustomGeometryEvent.register();

        modBus.addListener(com.pockyl.hextale.client.render.VoidPipelines::registerShaders);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) -> event.enqueueWork(IrisCompat::init));
        NeoForge.EVENT_BUS.addListener(com.pockyl.hextale.client.render.RuneFx::submit);
        NeoForge.EVENT_BUS.addListener(com.pockyl.hextale.client.render.EssenceFx::submit);
        TheVoidSounds.register();
        VoidAtmosphere.register(modBus);
        BreachCinematic.register(modBus);
        SilenceHush.register();
        ClientGrapple.register();
        LevitationHum.register();
        MachineLoops.register();
        WardRipple.register();
        com.pockyl.hextale.client.render.RuneEffects.register();
        com.pockyl.hextale.client.render.WeaponFx.register();
        WeaponInput.register();
        ShapeHits.register();
        ThinningAtmosphere.register(modBus);
        modBus.addListener(ThinningTint::register);
        ThinningTint.fadeWater();
        VoidHearts.register(modBus);
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
            // A pylon's runes, like a pickaxe's: name in its colour and stars.
            com.pockyl.hextale.runework.RuneHolder holder = com.pockyl.hextale.runework.RuneHolder.of(stack);
            if (holder != null && !holder.handTool()) {
                int line = 1;
                for (var entry : holder.runes(stack).entries()) {
                    event.getToolTip().add(line++, Component.translatable(entry.rune().translationKey()).withColor(entry.rune().color())
                            .append(Component.literal(" " + "★".repeat(entry.stars())).withStyle(ChatFormatting.GOLD)));
                }
            }
            if (stack.is(ModItems.HEX_SAWMILL.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.hex_sawmill.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.MIASMA_VENT.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.miasma_vent.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.XP_CONDENSER.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.xp_condenser.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.VOID_ANCHOR.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.void_anchor.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.HEXTECH_PURIFIER.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.hextech_purifier.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.ARC_SPIRE.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.arc_spire.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.HEX_FOUNDRY.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.hex_foundry.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.GREEDY_FUNNEL.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.greedy_funnel.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.WHIRL_REAPER.get())) {
                event.getToolTip().add(Component.translatable("block.hextale.whirl_reaper.hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.WARD_GLASS.get()) || stack.is(ModItems.VEIL_GLASS.get()) || stack.is(ModItems.FLOATING_STONE.get())) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                event.getToolTip().add(Component.translatable("block.hextale." + id + ".hint").withStyle(ChatFormatting.GRAY));
            }
            if (stack.is(ModItems.LEVITATION_PLATE.get()) || stack.is(ModItems.NULL_LANTERN.get()) || stack.is(ModItems.IMPULSE_PLATE.get())
                    || stack.is(ModItems.RIFT_GATE.get())
                    || stack.is(ModItems.WORLD_ANCHOR.get()) || stack.is(ModItems.DISINTEGRATOR.get())) {
                event.getToolTip().add(Component.translatable(stack.getItem().getDescriptionId() + ".hint").withStyle(ChatFormatting.GRAY));
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
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> com.pockyl.hextale.block.NullLanternBlockEntity.forget(true));
        ClientPylons.register();
        EssenceLookup.register();
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> ResearchBoards.warmUp());
    }

    /** A studied item shows its elements under its name. */
    private static void addElementsToTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
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
        event.register(ModMenus.LAPIDARY.get(), com.pockyl.hextale.client.screen.machine.LapidaryScreen::new);
        event.register(ModMenus.LAB_BENCH.get(), com.pockyl.hextale.client.screen.machine.LabBenchScreen::new);
        event.register(ModMenus.HEX_WORKBENCH.get(), com.pockyl.hextale.client.screen.machine.HexWorkbenchScreen::new);
        event.register(ModMenus.HEX_FURNACE.get(), com.pockyl.hextale.client.screen.machine.HexFurnaceScreen::new);
        event.register(ModMenus.HEX_FOUNDRY.get(), com.pockyl.hextale.client.screen.machine.HexFoundryScreen::new);
        event.register(ModMenus.ITEM_FILTER.get(), com.pockyl.hextale.client.screen.ItemFilterScreen::new);
        event.register(ModMenus.SEPARATOR.get(), SeparatorScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.THROWN_HEXTECH.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.DUST_CHARGE.get(),
                com.pockyl.hextale.client.render.DustChargeRenderer::new);
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.THROWN_DAGGER.get(),
                com.pockyl.hextale.client.render.ThrownDaggerRenderer::new);
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.THROWN_CHEM_VIAL.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.THE_VOID.get(),
                com.pockyl.hextale.client.render.TheVoidRenderer::new);
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.VOID_MITE.get(),
                com.pockyl.hextale.client.render.VoidMiteRenderer::new);
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.VOIDLING.get(),
                com.pockyl.hextale.client.render.VoidlingRenderer::new);
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.GRAPPLE_HOOK.get(),
                com.pockyl.hextale.client.render.GrappleHookRenderer::new);
        event.registerEntityRenderer(com.pockyl.hextale.registry.ModEntities.BREACH_CAMERA.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RESEARCH_TABLE.get(), ResearchTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ALEMBIC.get(), AlembicRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RIFT_GATE.get(), com.pockyl.hextale.client.render.RiftGateRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SEPARATOR.get(), SeparatorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ESSENCE_TANK.get(), EssenceTankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.BIG_ESSENCE_JAR.get(), com.pockyl.hextale.client.render.BigEssenceJarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEXTECH_ALTAR.get(), HextechAltarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RUNE_CIRCLE.get(), HextechAltarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LAPIDARY.get(), com.pockyl.hextale.client.render.LapidaryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WHIRL_REAPER.get(), com.pockyl.hextale.client.render.WhirlReaperRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEX_SAWMILL.get(), com.pockyl.hextale.client.render.HexSawmillRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEX_FOUNDRY.get(), com.pockyl.hextale.client.render.HexFoundryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GREEDY_FUNNEL.get(), com.pockyl.hextale.client.render.GreedyFunnelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ARC_SPIRE.get(), com.pockyl.hextale.client.render.ArcSpireRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEXTECH_PURIFIER.get(), com.pockyl.hextale.client.render.HextechPurifierRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.VOID_ANCHOR.get(), com.pockyl.hextale.client.render.VoidAnchorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.XP_CONDENSER.get(), com.pockyl.hextale.client.render.XpCondenserRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEX_WORKBENCH.get(), com.pockyl.hextale.client.render.HexWorkbenchRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.AXOLOTL_TANK.get(), com.pockyl.hextale.client.render.AxolotlTankRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CHEM_STILL.get(), com.pockyl.hextale.client.render.ChemStillRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CHEM_BOILER.get(), com.pockyl.hextale.client.render.ChemBoilerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEXTECH_LAMPPOST.get(), com.pockyl.hextale.client.render.HextechLamppostRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LAB_BENCH.get(), com.pockyl.hextale.client.render.LabBenchRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CRUCIBLE.get(), com.pockyl.hextale.client.render.CrucibleRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEX_PYLON.get(), com.pockyl.hextale.client.render.HexPylonRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL.get(), PedestalRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DISINTEGRATOR.get(), com.pockyl.hextale.client.render.DisintegratorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEX_WISP.get(), com.pockyl.hextale.client.render.HexWispRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.HEXCORE.get(), com.pockyl.hextale.client.render.HexcoreRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RUNE_STELE.get(), RuneSteleRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.IMPULSE_PLATE.get(), com.pockyl.hextale.client.render.ImpulsePlateRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LEVITATION_PLATE.get(), com.pockyl.hextale.client.render.LevitationPlateRenderer::new);
    }
}
