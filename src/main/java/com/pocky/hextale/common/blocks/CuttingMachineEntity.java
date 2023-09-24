package com.pocky.hextale.common.blocks;

import com.pocky.hextale.common.register.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class CuttingMachineEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    public CuttingMachineEntity(BlockEntityType<?> type, BlockPos p_155229_, BlockState p_155230_) {
        super(type, p_155229_, p_155230_);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static class Base extends CuttingMachineEntity {
        public Base(BlockPos p_155229_, BlockState p_155230_) {
            super(ModBlockEntities.BASE_CUTTING_MACHINE.get(), p_155229_, p_155230_);
        }
    }

    public static class Advanced extends CuttingMachineEntity {
        public Advanced(BlockPos p_155229_, BlockState p_155230_) {
            super(ModBlockEntities.ADVANCED_CUTTING_MACHINE.get(), p_155229_, p_155230_);
        }
    }

    public static class Elite extends CuttingMachineEntity {
        public Elite(BlockPos p_155229_, BlockState p_155230_) {
            super(ModBlockEntities.ELITE_CUTTING_MACHINE.get(), p_155229_, p_155230_);
        }
    }

    public static class Master extends CuttingMachineEntity {
        public Master(BlockPos p_155229_, BlockState p_155230_) {
            super(ModBlockEntities.MASTER_CUTTING_MACHINE.get(), p_155229_, p_155230_);
        }
    }
}
