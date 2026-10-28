package com.pockyl.hextale.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;

import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModBlocks;

import java.util.List;

/**
 * The Big Essence Jar: a 3 x 3 x 4 jar built of glass with a hollow middle and a lid of wooden slabs, woken with a pinch
 * of hextech dust. It holds sixteen jars' worth of essence and works like a jar: bottles by hand, pipes in anywhere, out at the
 * bottom's centre. It looks like a big octagonal glass vat in a walnut tub under a wide walnut lid
 * ({@link com.pockyl.hextale.client.render.BigEssenceJarRenderer}).
 */
public final class BigEssenceJar extends Multiblock {
    /**
     * The vat's glass body runs 12 px into the third layer before its shoulders narrow in; the lid and its knob stand on
     * the middle column 9 px into the top layer, so the outer blocks of the top layer hold nothing.
     */
    private static final VoxelShape SHOULDER = slab(12);
    private static final VoxelShape LID = slab(9);

    BigEssenceJar() {
        super("big_essence_jar", MultiblockPattern.builder()
                .layer("GGG", "GCG", "GGG")
                .layer("GGG", "G.G", "GGG")
                .layer("GGG", "G.G", "GGG")
                .layer("SSS", "SSS", "SSS")
                .where('G', glass())
                .where('C', glass())
                .where('S', MultiblockPattern.Key.tag(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS))
                .controller('C')
                .build());
    }

    private static MultiblockPattern.Key glass() {
        return new MultiblockPattern.Key(state -> state.is(Tags.Blocks.GLASS_BLOCKS), () -> List.of(new ItemStack(Items.GLASS)));
    }

    @Override
    public com.pockyl.hextale.research.ResearchTopic topic() {
        return com.pockyl.hextale.research.ResearchTopics.BIG_ESSENCE_JAR;
    }

    @Override
    public BlockState controller() {
        return ModBlocks.BIG_ESSENCE_JAR.get().defaultBlockState();
    }

    @Override
    public VoxelShape partShape(BlockPos offset) {
        boolean middle = offset.getX() == 0 && offset.getZ() == 0;
        if (offset.getY() < 2 || middle && offset.getY() == 2) {
            return Shapes.block();
        }
        if (middle) {
            return LID;
        }
        return offset.getY() == 2 ? SHOULDER : Shapes.empty();
    }

    @Override
    public void onFormed(ServerPlayer player, BlockPos controller) {
        Quests.fire(player, Quests.BIG_ESSENCE_JAR_FORMED);
    }
}
