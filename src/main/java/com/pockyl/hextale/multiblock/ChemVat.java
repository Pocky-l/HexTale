package com.pockyl.hextale.multiblock;

import com.pockyl.hextale.block.ChemVatBlock;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The Chem Vat: a 3 x 3 x 3 tank of rusted plating over a Chem Burner, green glass in the middle of three sides, a rusted
 * grate on top and the Chem Vat Valve in the middle of its front. A vial of acid splashed on it seals the seams and makes
 * it one vessel (no hextech dust: chemtech holds together by its own means). The valve is the controller. In the pattern
 * the front faces south; layers go from the bottom up, rows from north to south.
 */
public final class ChemVat extends Multiblock {
    public static final Direction FRONT = Direction.SOUTH;

    ChemVat() {
        super("chem_vat", MultiblockPattern.builder()
                .layer("PPP", "PBP", "PPP")
                .layer("PGP", "G.G", "PVP")
                .layer("PPP", "P#P", "PPP")
                .where('P', new MultiblockPattern.Key(
                        state -> state.is(ModBlocks.RUSTED_PLATING.get()) || state.is(ModBlocks.RIVETED_RUSTED_PLATING.get()),
                        () -> List.of(new ItemStack(ModItems.RIVETED_RUSTED_PLATING.get()), new ItemStack(ModItems.RUSTED_PLATING.get()))))
                .where('B', MultiblockPattern.Key.block(ModBlocks.CHEM_BURNER))
                .where('G', MultiblockPattern.Key.block(ModBlocks.CHEM_GLASS))
                .where('#', MultiblockPattern.Key.block(ModBlocks.RUSTED_GRATE))
                .where('V', new MultiblockPattern.Key(state -> state.is(ModBlocks.CHEM_VAT.get()) && !state.getValue(ChemVatBlock.FORMED),
                        () -> List.of(new ItemStack(ModItems.CHEM_VAT.get()))))
                .controller('V')
                .build());
    }

    @Override
    public boolean activatedBy(ItemStack stack) {
        return ChemVialItem.brew(stack) == Brew.ACID;
    }

    @Override
    public List<ItemStack> activators() {
        return List.of(ChemVialItem.of(Brew.ACID));
    }

    @Override
    public ResearchTopic topic() {
        return ResearchTopics.CHEM_VAT;
    }

    @Override
    public BlockState controller() {
        return ModBlocks.CHEM_VAT.get().defaultBlockState().setValue(ChemVatBlock.FORMED, true);
    }

    @Override
    public BlockState controller(MultiblockPattern.Placement placement) {
        return controller().setValue(ChemVatBlock.FACING, placement.rotation().rotate(FRONT));
    }

    @Override
    public boolean sharesItems() {
        return true;
    }

    /** The acid bites into the seams: green fumes from every block, a hiss. */
    @Override
    public void showForming(ServerLevel level, List<BlockPos> cells, BlockPos controller) {
        for (BlockPos pos : cells) {
            level.sendParticles(ModParticles.vapour(Brew.ACID.color()), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    2, 0.35, 0.35, 0.35, 0.01);
        }
        level.playSound(null, controller, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    @Override
    public void onFormed(net.minecraft.server.level.ServerPlayer player, BlockPos controller) {
        com.pockyl.hextale.quest.Quests.fire(player, com.pockyl.hextale.quest.Quests.CHEM_VAT_FORMED);
    }

    @Override
    public SoundEvent breakSound() {
        return SoundEvents.COPPER_BREAK;
    }
}
