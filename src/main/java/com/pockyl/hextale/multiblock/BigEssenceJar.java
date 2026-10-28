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
 * bottom's centre. It looks like the Essence Jar, four times as large.
 */
public final class BigEssenceJar extends Multiblock {
    /**
     * The jar's model is 16 pixels tall and drawn four times larger: its round body ends half way up the third layer, the
     * shoulders, neck and cork stand over the middle column, so the outer blocks of the top layer hold nothing.
     */
    private static final VoxelShape SHOULDER = slab(8);

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
        if (middle || offset.getY() < 2) {
            return Shapes.block();
        }
        return offset.getY() == 2 ? SHOULDER : Shapes.empty();
    }

    @Override
    public void onFormed(ServerPlayer player, BlockPos controller) {
        Quests.fire(player, Quests.BIG_ESSENCE_JAR_FORMED);
    }
}
