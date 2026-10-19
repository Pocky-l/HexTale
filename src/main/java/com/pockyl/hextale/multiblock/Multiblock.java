package com.pockyl.hextale.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A structure built from plain blocks in the world that comes alive when the researcher touches it with the right item:
 * the blocks are kept (as {@link MultiblockPartBlock}s remembering what they were) and one of them becomes the
 * controller, a block of its own with a {@link MultiblockCore} entity that does the work and draws the whole thing.
 * Breaking any block of a formed structure takes it apart again. Register new structures in {@link Multiblocks}.
 */
public abstract class Multiblock {
    private final String id;
    private final MultiblockPattern pattern;
    private final Predicate<ItemStack> activator;
    private final List<Supplier<ItemStack>> activators;

    /**
     * @param activators the items that wake the structure (the first one is shown in the book)
     */
    @SafeVarargs
    protected Multiblock(String id, MultiblockPattern pattern, Supplier<? extends net.minecraft.world.level.ItemLike>... activators) {
        this.id = id;
        this.pattern = pattern;
        this.activators = java.util.Arrays.stream(activators).<Supplier<ItemStack>>map(item -> () -> new ItemStack(item.get())).toList();
        this.activator = stack -> {
            for (Supplier<? extends net.minecraft.world.level.ItemLike> item : activators) {
                if (stack.is(item.get().asItem())) {
                    return true;
                }
            }
            return false;
        };
    }

    public String id() {
        return id;
    }

    public MultiblockPattern pattern() {
        return pattern;
    }

    public boolean activatedBy(ItemStack stack) {
        return activator.test(stack);
    }

    /** The items that wake the structure, for the book. */
    public List<ItemStack> activators() {
        return activators.stream().map(Supplier::get).toList();
    }

    /** The block that takes the controller's cell once the structure is formed; its entity must be a {@link MultiblockCore}. */
    public abstract BlockState controller();

    /**
     * The solid part of the structure inside one of its blocks, by the block's offset from the controller (turned back
     * to the pattern's own orientation). A full block unless a structure says otherwise.
     */
    public VoxelShape partShape(BlockPos offset) {
        return Shapes.block();
    }

    /** What the player is told when the structure comes alive. */
    public Component formedMessage() {
        return Component.translatable("multiblock.hextale." + id + ".formed");
    }

    /** What must be researched before the structure can be woken, or null. */
    public com.pockyl.hextale.research.@org.jspecify.annotations.Nullable ResearchTopic topic() {
        return null;
    }

    /** Called once the structure is formed, on the server: quests, advancements. */
    public void onFormed(ServerPlayer player, BlockPos controller) {
    }

    /** A full-height block cut at this many pixels. */
    protected static VoxelShape slab(double height) {
        return Block.box(0, 0, 0, 16, height, 16);
    }
}
