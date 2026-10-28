package com.pockyl.hextale.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.block.HexFoundryBlock;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.List;

/**
 * The Hex Foundry: a 3 x 3 x 3 kiln of Fire Bricks with a hollow heart, an iron grate in the middle of its top and the
 * Foundry Hatch in the middle of its front, set alight by a pinch of hextech dust. The hatch becomes the controller:
 * it keeps its runes and its contents whether the kiln stands or falls apart. In the pattern the front faces south.
 */
public final class HexFoundry extends Multiblock {
    /** The pattern's front: the hatch looks this way before the structure is turned. */
    public static final Direction FRONT = Direction.SOUTH;

    HexFoundry() {
        super("hex_foundry", MultiblockPattern.builder()
                .layer("BBB", "BBB", "BBB")
                .layer("BBB", "B.B", "BHB")
                .layer("BBB", "BGB", "BBB")
                .where('B', MultiblockPattern.Key.block(ModBlocks.FIRE_BRICKS))
                .where('G', new MultiblockPattern.Key(state -> state.is(Blocks.IRON_BARS), () -> List.of(new ItemStack(Items.IRON_BARS))))
                .where('H', new MultiblockPattern.Key(state -> state.is(ModBlocks.HEX_FOUNDRY.get()) && !state.getValue(HexFoundryBlock.FORMED),
                        () -> List.of(new ItemStack(ModBlocks.HEX_FOUNDRY.get()))))
                .controller('H')
                .build());
    }

    @Override
    public ResearchTopic topic() {
        return ResearchTopics.HEX_FOUNDRY;
    }

    @Override
    public BlockState controller() {
        return ModBlocks.HEX_FOUNDRY.get().defaultBlockState().setValue(HexFoundryBlock.FORMED, true);
    }

    @Override
    public BlockState controller(MultiblockPattern.Placement placement) {
        return controller().setValue(HexFoundryBlock.FACING, placement.rotation().rotate(FRONT));
    }

    @Override
    public boolean sharesItems() {
        return true;
    }

    /** The bricks catch: soot and a lick of flame from every block, a whoosh of fire taking hold. */
    @Override
    public void showForming(ServerLevel level, List<BlockPos> cells, BlockPos controller) {
        for (BlockPos pos : cells) {
            level.sendParticles(com.pockyl.hextale.registry.ModParticles.vapour(com.pockyl.hextale.registry.ModParticles.SOOT),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.35, 0.35, 0.35, 0.01);
            level.sendParticles(com.pockyl.hextale.registry.ModParticles.flame(com.pockyl.hextale.registry.ModParticles.FIRE),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.4, 0.4, 0.4, 0.01);
        }
        level.playSound(null, controller, com.pockyl.hextale.registry.ModSounds.FORGE_CATCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public SoundEvent breakSound() {
        return SoundEvents.MUD_BRICKS_BREAK;
    }

    @Override
    public void onFormed(ServerPlayer player, BlockPos controller) {
        Quests.fire(player, Quests.HEX_FOUNDRY_LIT);
    }
}
