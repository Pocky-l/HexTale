package com.pockyl.hextale.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.research.ResearchTopic;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModParticles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Every multiblock of the mod, and how they come alive and fall apart. A right click on any block of a finished pattern
 * with the structure's item forms it: the controller's cell gets the controller block, every other cell (the hollow
 * inside too) a {@link MultiblockPartBlock} remembering what it was. Removing any of those blocks puts every other block
 * back as it was.
 */
public final class Multiblocks {
    private static final Map<String, Multiblock> ALL = new LinkedHashMap<>();
    /** Controllers whose structures are being taken apart right now (their own removals must not start it again). */
    private static final Set<BlockPos> BREAKING = new HashSet<>();

    public static final BigEssenceJar BIG_ESSENCE_JAR = register(new BigEssenceJar());

    private Multiblocks() {
    }

    private static <T extends Multiblock> T register(T multiblock) {
        if (ALL.put(multiblock.id(), multiblock) != null) {
            throw new IllegalStateException("Two multiblocks named " + multiblock.id());
        }
        return multiblock;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Multiblocks::onRightClickBlock);
    }

    public static List<Multiblock> all() {
        return List.copyOf(ALL.values());
    }

    @Nullable
    public static Multiblock byId(String id) {
        return ALL.get(id);
    }

    /** The turn that undoes this one. */
    public static Rotation inverse(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
    }

    /** The structure a click with this item would form here, and where; empty when there is none. */
    public static Optional<Map.Entry<Multiblock, MultiblockPattern.Placement>> find(Level level, BlockPos clicked,
                                                                                    net.minecraft.world.item.ItemStack stack) {
        for (Multiblock multiblock : ALL.values()) {
            if (multiblock.activatedBy(stack)) {
                Optional<MultiblockPattern.Placement> placement = multiblock.pattern().find(level, clicked);
                if (placement.isPresent()) {
                    return Optional.of(Map.entry(multiblock, placement.get()));
                }
            }
        }
        return Optional.empty();
    }

    /** Both sides look for the pattern, so the hand swings at once; the server forms the structure. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        var found = find(level, event.getPos(), event.getItemStack());
        if (found.isEmpty()) {
            return;
        }
        ResearchTopic topic = found.get().getKey().topic();
        if (topic != null
                && !(level.isClientSide() ? com.pockyl.hextale.client.ClientKnowledge.get() : Research.of(event.getEntity())).researched(topic)) {
            // Not researched: the structure stays asleep and the click does what it would do anyway.
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendOverlayMessage(Component.translatable("research.hextale.locked",
                        Component.translatable(topic.translationKey()).withColor(topic.color())));
            }
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level instanceof ServerLevel serverLevel && event.getEntity() instanceof ServerPlayer player) {
            form(serverLevel, found.get().getKey(), found.get().getValue(), player);
        }
    }

    /** Turns the matched blocks into the working structure. */
    public static void form(ServerLevel level, Multiblock multiblock, MultiblockPattern.Placement placement, @Nullable ServerPlayer player) {
        MultiblockPattern pattern = multiblock.pattern();
        BlockPos controllerPos = pattern.controllerPos(placement);
        List<BlockPos> cells = new ArrayList<>();
        List<BlockState> originals = new ArrayList<>();
        for (MultiblockPattern.Cell cell : pattern.cells()) {
            BlockPos pos = placement.at(cell.x(), cell.y(), cell.z());
            cells.add(pos);
            originals.add(level.getBlockState(pos));
        }
        // The controller first, so parts find it when their shapes are asked for.
        BlockState original = level.getBlockState(controllerPos);
        level.setBlock(controllerPos, multiblock.controller(), Block.UPDATE_ALL);
        if (level.getBlockEntity(controllerPos) instanceof MultiblockCore core) {
            core.setFormed(new MultiblockCore.Formed(multiblock.id(), placement, original));
        }
        BlockState part = ModBlocks.MULTIBLOCK_PART.get().defaultBlockState();
        for (int i = 0; i < cells.size(); i++) {
            BlockPos pos = cells.get(i);
            if (pos.equals(controllerPos)) {
                continue;
            }
            level.setBlock(pos, part, Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity entity) {
                entity.bind(controllerPos, originals.get(i));
            }
        }
        for (BlockPos pos : cells) {
            level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
        }
        level.playSound(null, controllerPos, ModSounds.RUNE_FUSED.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        if (player != null) {
            player.sendOverlayMessage(multiblock.formedMessage());
            multiblock.onFormed(player, controllerPos);
        }
    }

    /**
     * Takes a formed structure apart: every block but {@code removed} (already gone) becomes what it was before. Called
     * when any of its blocks is removed.
     */
    public static void breakApart(ServerLevel level, BlockPos controllerPos, BlockPos removed) {
        BlockEntity entity = level.getBlockEntity(controllerPos);
        breakApart(level, controllerPos, removed, entity instanceof MultiblockCore core ? core.formed() : null);
    }

    /** The same, for a controller that is being removed itself and knows its structure. */
    public static void breakApart(ServerLevel level, BlockPos controllerPos, BlockPos removed, MultiblockCore.@Nullable Formed formed) {
        if (BREAKING.contains(controllerPos) || formed == null) {
            return;
        }
        Multiblock multiblock = byId(formed.multiblock());
        if (multiblock == null) {
            return;
        }
        BREAKING.add(controllerPos);
        try {
            for (MultiblockPattern.Cell cell : multiblock.pattern().cells()) {
                BlockPos pos = formed.placement().at(cell.x(), cell.y(), cell.z());
                if (pos.equals(removed)) {
                    continue;
                }
                if (pos.equals(controllerPos)) {
                    level.setBlock(pos, formed.original(), Block.UPDATE_ALL);
                } else if (level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part && part.controller().equals(controllerPos)) {
                    level.setBlock(pos, part.original(), Block.UPDATE_ALL);
                }
            }
        } finally {
            BREAKING.remove(controllerPos);
        }
        level.playSound(null, controllerPos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6F, 0.8F);
    }
}
