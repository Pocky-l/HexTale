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

import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/**
 * The Big Essence Jar: a 3 x 3 x 4 jar built of glass with a hollow middle and a lid of wooden slabs, woken with a hextech
 * crystal. It holds sixteen jars' worth of essence and works like a jar: bottles by hand, pipes in anywhere, out at the
 * bottom's centre. It looks like the Essence Jar, four times as large.
 */
public final class BigEssenceJar extends Multiblock {
    /** The jar's lid ends 12 pixels into the top layer (the jar's model is 15 pixels tall, drawn four times larger). */
    private static final VoxelShape LID = slab(12);

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
                .build(), ModItems.HEXTECH_GEMSTONE, ModItems.UNSTABLE_HEXTECH);
    }

    private static MultiblockPattern.Key glass() {
        return new MultiblockPattern.Key(state -> state.is(Tags.Blocks.GLASS_BLOCKS), () -> List.of(new ItemStack(Items.GLASS)));
    }

    @Override
    public BlockState controller() {
        return ModBlocks.BIG_ESSENCE_JAR.get().defaultBlockState();
    }

    @Override
    public VoxelShape partShape(BlockPos offset) {
        return offset.getY() >= 3 ? LID : Shapes.block();
    }

    @Override
    public void onFormed(ServerPlayer player, BlockPos controller) {
        Quests.complete(player, Guide.BIG_ESSENCE_JAR);
    }
}
