package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.registry.ModBlockEntities;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Which vault a Hex Terminal is tied to. It keeps the vault's id, so the tie holds when the vault is moved. */
public class HexTerminalBlockEntity extends BlockEntity {
    @Nullable
    private UUID vault;

    public HexTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_TERMINAL.get(), pos, state);
    }

    @Nullable
    public UUID vault() {
        return vault;
    }

    public void tie(UUID id) {
        vault = id;
        setChanged();
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(HexTerminalBlock.LINKED, true), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        vault = input.read("vault", UUIDUtil.STRING_CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (vault != null) {
            output.store("vault", UUIDUtil.STRING_CODEC, vault);
        }
    }
}
