package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.menu.RuneEngraverMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.runework.HextechTools;
import com.pockyl.hextale.runework.RuneSlots;
import com.pockyl.hextale.runework.ToolRunes;

import org.jspecify.annotations.Nullable;

/**
 * The Rune Engraver's contents: a hextech tool in the vise, a rune scroll and a bottle of essence. Engraving burns the
 * scroll into one of the tool's slots (the rune keeps the scroll's stars); grinding a rune off destroys it. A bottle
 * of essence put in pours straight into the tool's cell and leaves a glass bottle (or what did not fit).
 */
public class RuneEngraverBlockEntity extends BaseContainerBlockEntity {
    public static final int TOOL = 0;
    public static final int SCROLL = 1;
    public static final int BOTTLE = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    /** Client side: game time of the last engraving, for the renderer's flash. */
    private long engravedAt = -100;

    public RuneEngraverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_ENGRAVER.get(), pos, state);
    }

    public ItemStack tool() {
        return items.get(TOOL);
    }

    public long engravedAt() {
        return engravedAt;
    }

    /** Why a scroll cannot go into that slot of the tool now, or null when it can. */
    public static @Nullable String refusal(boolean creative, RuneKnowledge knowledge, ItemStack tool, ItemStack scroll, int slot) {
        if (!HextechTools.isTool(tool)) {
            return "no_tool";
        }
        ComplexRune rune = RuneScrollItem.rune(scroll);
        if (rune == null) {
            return "no_scroll";
        }
        if (!ToolRunes.worksOnTools(rune)) {
            return "not_for_tools";
        }
        RuneSlots runes = HextechTools.runes(tool);
        if (runes.get(slot) != null) {
            return "slot_taken";
        }
        if (runes.has(rune)) {
            return "already";
        }
        if (!creative && !knowledge.knows(rune)) {
            return "unknown";
        }
        return null;
    }

    /** Burns the scroll into the tool's slot; tells the player why not when it cannot. */
    public void engrave(ServerPlayer player, int slot) {
        ItemStack tool = items.get(TOOL);
        ItemStack scroll = items.get(SCROLL);
        String refusal = refusal(player.getAbilities().instabuild, Research.of(player), tool, scroll, slot);
        if (refusal != null) {
            player.sendOverlayMessage(Component.translatable("gui.hextale.engraver.refused." + refusal));
            return;
        }
        ComplexRune rune = RuneScrollItem.rune(scroll);
        HextechTools.setRunes(tool, HextechTools.runes(tool).with(new RuneSlots.Engraving(slot, rune, RuneScrollItem.stars(scroll))));
        if (!player.getAbilities().instabuild) {
            scroll.shrink(1);
        }
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + 1.05;
            double z = worldPosition.getZ() + 0.5;
            serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y + 0.3, z, 30, 0.3, 0.3, 0.3, 0.6);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 8, 0.2, 0.05, 0.2, 0.05);
            serverLevel.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);
            serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        }
    }

    /** Grinds the rune in that slot off the tool; the rune is lost. */
    public void grind(ServerPlayer player, int slot) {
        ItemStack tool = items.get(TOOL);
        if (!HextechTools.isTool(tool) || HextechTools.runes(tool).get(slot) == null) {
            return;
        }
        HextechTools.setRunes(tool, HextechTools.runes(tool).without(slot));
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.05, worldPosition.getZ() + 0.5,
                    12, 0.2, 0.05, 0.2, 0.2);
            serverLevel.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1 && level != null) {
            engravedAt = level.getGameTime();
            return true;
        }
        return super.triggerEvent(id, param);
    }

    /** A bottle of essence pours into the tool's cell; what does not fit stays in the bottle. */
    private void pourBottle() {
        ItemStack tool = items.get(TOOL);
        ItemStack bottle = items.get(BOTTLE);
        if (!HextechTools.isTool(tool) || !bottle.is(ModItems.ESSENCE_FLASK.get())) {
            return;
        }
        EssenceMix inside = EssenceFlaskItem.contents(bottle);
        int poured = HextechTools.fill(tool, inside);
        if (poured <= 0) {
            return;
        }
        items.set(BOTTLE, EssenceFlaskItem.filled(inside.minus(inside.part(poured))));
        if (level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7F, 1.0F);
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (level != null && !level.isClientSide()) {
            pourBottle();
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case TOOL -> HextechTools.isTool(stack);
            case SCROLL -> stack.is(ModItems.RUNE_SCROLL.get()) && RuneScrollItem.rune(stack) != null;
            case BOTTLE -> EssenceFlaskItem.isFlask(stack);
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.rune_engraver");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new RuneEngraverMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(3, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
