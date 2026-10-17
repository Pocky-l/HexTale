package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.multiblock.MultiblockCore;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * The heart of a Big Essence Jar (the multiblock {@link com.pockyl.hextale.multiblock.BigEssenceJar}): an Essence Jar
 * sixteen times as large. Everything that works with jars - bottles, pipes, siphons, machines drawing from jars around
 * them - works with it as well.
 */
public class BigEssenceJarBlockEntity extends EssenceTankBlockEntity implements MultiblockCore {
    /** Millibuckets: sixteen jars. */
    public static final int CAPACITY = EssenceTankBlockEntity.CAPACITY * 16;

    @Nullable
    private Formed formed;

    public BigEssenceJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIG_ESSENCE_JAR.get(), pos, state, CAPACITY);
    }

    @Override
    public @Nullable Formed formed() {
        return formed;
    }

    @Override
    public void setFormed(Formed formed) {
        this.formed = formed;
        setChanged();
    }

    /** Broken: the essence is lost, the sticker comes off and the jar falls apart into its glass and slabs. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (sticker() != null && level != null && !level.isClientSide()) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    com.pockyl.hextale.item.EssenceStickerItem.of(com.pockyl.hextale.registry.ModItems.ESSENCE_STICKER.get(), sticker()));
        }
        if (level instanceof ServerLevel serverLevel) {
            Multiblocks.breakApart(serverLevel, pos, pos, formed);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        formed = input.read("formed", Formed.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (formed != null) {
            output.store("formed", Formed.CODEC, formed);
        }
    }
}
