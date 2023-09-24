package com.pocky.hextale.common.blocks;

import com.pocky.hextale.common.menu.AdvancedCuttingMachineMenu;
import com.pocky.hextale.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class AdvancedCuttingMachine extends AbstractCuttingMachine {

    private static final Component CONTAINER_TITLE = Component.translatable("container.advanced_cutting_machine");

    public AdvancedCuttingMachine(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState p_60563_, Level p_60564_, BlockPos p_60565_) {
        return new SimpleMenuProvider((p_57074_, p_57075_, p_57076_)
                -> new AdvancedCuttingMachineMenu(p_57074_, p_57075_, ContainerLevelAccess.create(p_60564_, p_60565_)), CONTAINER_TITLE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.ADVANCED_CUTTING_MACHINE.get().create(pos, state);
    }
}
