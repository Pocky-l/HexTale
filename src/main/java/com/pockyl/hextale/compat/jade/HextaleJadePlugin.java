package com.pockyl.hextale.compat.jade;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
        registration.registerBlockComponent(Machines.INSTANCE, EssenceTankBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, SeparatorBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, AlembicBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, DrainValveBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, PumpBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.HexFurnaceBlock.class);
        registration.registerBlockComponent(Machines.INSTANCE, com.pockyl.hextale.block.HexPylonBlock.class);
    }

    private enum Machines implements IBlockComponentProvider {
        INSTANCE;

        private static final Identifier UID = Hextale.id("essence_works");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockEntity entity = accessor.getBlockEntity();
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
                if ("no_essence".equals(blocked) || "no_fuel".equals(blocked)) {
                    tooltip.add(Component.translatable("gui.hextale.separator." + blocked).withStyle(ChatFormatting.RED));
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
        public Identifier getUid() {
            return UID;
        }
    }
}
