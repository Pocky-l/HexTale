package com.pockyl.hextale.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AlembicBlock;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.block.DrainValveBlock;
import com.pockyl.hextale.block.EssenceTankBlock;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.PortMode;
import com.pockyl.hextale.block.PumpBlock;
import com.pockyl.hextale.block.SeparatorBlock;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.essence.EssenceTank;

import java.util.Locale;

/**
 * Jade: what the essence works hold and do, read from the client's copy of their block entities (they sync it) - so
 * the tooltips work without Jade on the server.
 */
@WailaPlugin
public class HextaleJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // A device's stand-in in a cell its model spreads into reads as the device itself.
        registration.addRayTraceCallback((hit, accessor, original) -> {
            if (accessor instanceof BlockAccessor block && block.getBlock() instanceof com.pockyl.hextale.block.ShapePartBlock) {
                net.minecraft.core.BlockPos device = com.pockyl.hextale.block.ShapePartBlock.device(block.getBlockState(), block.getPosition());
                net.minecraft.world.level.block.state.BlockState state = block.getLevel().getBlockState(device);
                if (state.is(com.pockyl.hextale.block.ShapeParts.OWNERS)) {
                    return registration.blockAccessor().from(block).blockState(state)
                            .blockEntity(() -> block.getLevel().getBlockEntity(device)).hit(block.getHitResult().withPosition(device)).build();
                }
            }
            return accessor;
        });
        registration.registerBlockComponent(Machines.INSTANCE, EssenceTankBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, SeparatorBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, AlembicBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.CrucibleBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.ChemBurnerBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.AxolotlTankBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.ChemStillBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.PinkBrazierBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, DrainValveBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, PumpBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.HexFurnaceBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.HexPylonBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.WhirlReaperBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.HexSawmillBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.GreedyFunnelBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.HexFoundryBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.ArcSpireBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.HextechPurifierBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.VoidAnchorBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.XpCondenserBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.MiasmaVentBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.HexWorkbenchBlock.class);
        // A jar names its contents itself; Jade's own fluid bar would show a mixture as an empty tank.
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof BlockAccessor block && EssenceTankBlockEntity.at(block.getLevel(), block.getPosition()) != null) {
                box.getTooltip().remove(snownee.jade.api.JadeIds.UNIVERSAL_FLUID_STORAGE);
            }
        });
    }

    private enum Machines implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = Hextale.id("essence_works");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockEntity entity = accessor.getBlockEntity();
            if (entity == null && accessor.getBlock() instanceof com.pockyl.hextale.block.ArcSpireBlock) {
                // The upper half of a spire has no block entity of its own: tell about the base under it.
                entity = com.pockyl.hextale.block.ArcSpireBlock.spireAt(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState());
            }
            if (entity instanceof EssenceTankBlockEntity tank) {
                tooltip.add(contents(tank.tank()));
            } else if (entity instanceof SeparatorBlockEntity separator) {
                tooltip.add(Component.translatable("jade.hextale.mixture").append(": ").append(contents(separator.mix())));
                tooltip.add(Component.translatable("jade.hextale.concentrate").append(": ").append(contents(separator.extract())));
                tooltip.add(Component.translatable("jade.hextale.rest").append(": ").append(contents(separator.rest())));
                SeparatorBlock.Side side = SeparatorBlock.Side.of(accessor.getSide(), accessor.getBlockState().getValue(SeparatorBlock.FACING));
                if (side != null) {
                    PortMode mode = separator.mode(side);
                    tooltip.add(Component.translatable("gui.hextale.separator.side." + side.name().toLowerCase(Locale.ROOT))
                            .append(": ").append(Component.translatable("gui.hextale.port." + mode.getSerializedName()))
                            .withStyle(ChatFormatting.GRAY));
                }
                String blocked = separator.blocked();
                if ("no_essence".equals(blocked) || "no_fuel".equals(blocked) || "missing".equals(blocked)) {
                    tooltip.add(Component.translatable("gui.hextale.separator." + blocked).withStyle(ChatFormatting.RED));
                }
            } else if (entity instanceof com.pockyl.hextale.block.CrucibleBlockEntity crucible) {
                tooltip.add(com.pockyl.hextale.block.CrucibleBlock.status(crucible));
                if (!crucible.essence().isEmpty()) {
                    tooltip.add(crucible.essence().describe().withStyle(ChatFormatting.GRAY));
                }
            } else if (entity instanceof com.pockyl.hextale.block.AxolotlTankBlockEntity tank) {
                tooltip.add(tank.status());
            } else if (entity instanceof com.pockyl.hextale.block.ChemStillBlockEntity still) {
                tooltip.add(still.status());
            } else if (entity instanceof com.pockyl.hextale.block.PinkBrazierBlockEntity brazier) {
                tooltip.add(brazier.status());
            } else if (entity instanceof com.pockyl.hextale.block.ChemBurnerBlockEntity burner) {
                tooltip.add(burner.status());
                if (!burner.tank().isEmpty()) {
                    tooltip.add(burner.tank().describe().withStyle(ChatFormatting.GRAY));
                }
            } else if (entity instanceof AlembicBlockEntity alembic) {
                tooltip.add(Component.translatable("gui.hextale.alembic.water", alembic.water(), AlembicBlockEntity.WATER_CAPACITY));
                tooltip.add(Component.translatable("jade.hextale.essence").append(": ").append(contents(alembic.tank())));
                if (!accessor.getBlockState().getValue(AlembicBlock.LIT)) {
                    tooltip.add(Component.translatable("gui.hextale.alembic.cold").withStyle(ChatFormatting.RED));
                } else if (alembic.water() < AlembicBlockEntity.WATER_PER_ITEM) {
                    tooltip.add(Component.translatable("gui.hextale.alembic.no_water").withStyle(ChatFormatting.RED));
                } else if (alembic.progress() > 0) {
                    tooltip.add(Component.translatable("jade.hextale.boiling", alembic.progress() * 100 / AlembicBlockEntity.BOIL_TICKS));
                }
            } else if (entity instanceof com.pockyl.hextale.block.HexFurnaceBlockEntity furnace) {
                if (!furnace.blocked().isEmpty()) {
                    tooltip.add(Component.translatable("gui.hextale.hex_furnace." + furnace.blocked()).withStyle(ChatFormatting.RED));
                }
            } else if (entity instanceof com.pockyl.hextale.block.HexWorkbenchBlockEntity bench) {
                tooltip.add(Component.translatable("jade.hextale.charge", bench.charge(),
                        com.pockyl.hextale.block.HexWorkbenchBlockEntity.MAX_CHARGE).withStyle(ChatFormatting.AQUA));
            } else if (entity instanceof com.pockyl.hextale.block.HexFoundryBlockEntity foundry) {
                if (accessor.getBlockState().getValue(com.pockyl.hextale.block.HexFoundryBlock.FORMED)) {
                    tooltip.add(com.pockyl.hextale.block.HexFoundryBlock.status(foundry).copy().withStyle(ChatFormatting.GRAY));
                }
            } else if (entity instanceof com.pockyl.hextale.block.HexSawmillBlockEntity mill) {
                tooltip.add(com.pockyl.hextale.block.HexSawmillBlock.status(mill).copy().withStyle(ChatFormatting.GRAY));
            } else if (entity instanceof com.pockyl.hextale.block.MiasmaVentBlockEntity) {
                tooltip.add(com.pockyl.hextale.block.MiasmaVentBlock.status(accessor.getBlockState()).copy().withStyle(ChatFormatting.GRAY));
            } else if (entity instanceof com.pockyl.hextale.block.XpCondenserBlockEntity condenser) {
                tooltip.add(Component.translatable("block.hextale.xp_condenser.status", condenser.points(),
                        com.pockyl.hextale.block.XpCondenserBlockEntity.CAPACITY).withStyle(ChatFormatting.GRAY));
            } else if (entity instanceof com.pockyl.hextale.block.VoidAnchorBlockEntity anchor) {
                tooltip.add(com.pockyl.hextale.block.VoidAnchorBlock.status(anchor).copy().withStyle(ChatFormatting.GRAY));
            } else if (entity instanceof com.pockyl.hextale.block.HextechPurifierBlockEntity purifier) {
                tooltip.add(com.pockyl.hextale.block.HextechPurifierBlock.status(purifier).copy().withStyle(ChatFormatting.GRAY));
            } else if (entity instanceof com.pockyl.hextale.block.ArcSpireBlockEntity spire) {
                tooltip.add(com.pockyl.hextale.block.ArcSpireBlock.status(spire).copy().withStyle(ChatFormatting.GRAY));
            } else if (entity instanceof com.pockyl.hextale.block.GreedyFunnelBlockEntity funnel) {
                tooltip.add(com.pockyl.hextale.block.GreedyFunnelBlock.status(funnel).copy().withStyle(ChatFormatting.GRAY));
            } else if (entity instanceof com.pockyl.hextale.block.WhirlReaperBlockEntity reaper) {
                tooltip.add(com.pockyl.hextale.block.WhirlReaperBlock.status(reaper).copy().withStyle(ChatFormatting.GRAY));
            } else if (entity instanceof com.pockyl.hextale.block.HexPylonBlockEntity pylon) {
                tooltip.add(com.pockyl.hextale.block.HexPylonBlock.status(pylon).copy().withStyle(ChatFormatting.GRAY));
                for (var entry : pylon.runes().entries()) {
                    tooltip.add(Component.translatable(entry.rune().translationKey()).withColor(entry.rune().color())
                            .append(Component.literal(" " + "★".repeat(entry.stars())).withStyle(ChatFormatting.GOLD)));
                }
            } else if (accessor.getBlock() instanceof DrainValveBlock) {
                boolean open = accessor.getBlockState().getValue(DrainValveBlock.OPEN);
                tooltip.add(Component.translatable(open ? "block.hextale.drain_valve.open" : "block.hextale.drain_valve.shut")
                        .withStyle(open ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            } else if (accessor.getBlock() instanceof PumpBlock) {
                boolean wet = accessor.getBlockState().getValue(PumpBlock.WATERLOGGED);
                tooltip.add(Component.translatable(wet ? "jade.hextale.pump.working" : "jade.hextale.pump.dry")
                        .withStyle(wet ? ChatFormatting.AQUA : ChatFormatting.RED));
            }
        }

        private static Component contents(EssenceTank tank) {
            return tank.isEmpty() ? Component.translatable("block.hextale.essence.empty").withStyle(ChatFormatting.GRAY)
                    : tank.contents().describe().append(Component.literal(" (" + tank.amount() + "/" + tank.capacity() + ")")
                    .withStyle(ChatFormatting.GRAY));
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
