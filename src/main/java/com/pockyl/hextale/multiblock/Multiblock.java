package com.pockyl.hextale.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModItems;

import java.util.List;

/**
 * A structure built from plain blocks in the world that comes alive when the researcher sprinkles it with a pinch of
 * hextech dust: the blocks are kept (as {@link MultiblockPartBlock}s remembering what they were) and one of them becomes the
 * controller, a block of its own with a {@link MultiblockCore} entity that does the work and draws the whole thing.
 * Breaking any block of a formed structure takes it apart again. Register new structures in {@link Multiblocks}.
 */
public abstract class Multiblock {
    private final String id;
    private final MultiblockPattern pattern;

    protected Multiblock(String id, MultiblockPattern pattern) {
        this.id = id;
        this.pattern = pattern;
    }

    public String id() {
        return id;
    }

    public MultiblockPattern pattern() {
        return pattern;
    }

    /** Every structure wakes to a pinch of hextech dust (one is spent unless the researcher is in creative). */
    public boolean activatedBy(ItemStack stack) {
        return stack.is(ModItems.HEXTECH_DUST.get());
    }

    /** The items that wake the structure, for the book and JEI. */
    public List<ItemStack> activators() {
        return List.of(new ItemStack(ModItems.HEXTECH_DUST.get()));
    }

    /** The block that takes the controller's cell once the structure is formed; its entity must be a {@link MultiblockCore}. */
    public abstract BlockState controller();

    /** The controller's block for a structure found in this turn (a front that faces the way the structure does). */
    public BlockState controller(MultiblockPattern.Placement placement) {
        return controller();
    }

    /** A structure may take items through its blocks (hoppers, pipes of other mods): the controller's container. */
    public boolean sharesItems() {
        return false;
    }

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

    /** What the world shows when the structure comes alive: a glint in every block and a soft fused sound. */
    public void showForming(net.minecraft.server.level.ServerLevel level, List<BlockPos> cells, BlockPos controller) {
        for (BlockPos pos : cells) {
            level.sendParticles(com.pockyl.hextale.registry.ModParticles.glint(com.pockyl.hextale.registry.ModParticles.HEX_LIGHT),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
        }
        level.playSound(null, controller, com.pockyl.hextale.registry.ModSounds.RUNE_FUSED.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.0F);
    }

    /** The sound of the structure falling apart. */
    public net.minecraft.sounds.SoundEvent breakSound() {
        return net.minecraft.sounds.SoundEvents.GLASS_BREAK;
    }

    /** Called once the structure is formed, on the server: quests, advancements. */
    public void onFormed(ServerPlayer player, BlockPos controller) {
    }

    /** A full-height block cut at this many pixels. */
    protected static VoxelShape slab(double height) {
        return Block.box(0, 0, 0, 16, height, 16);
    }
}
