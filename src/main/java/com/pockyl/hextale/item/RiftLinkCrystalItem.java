package com.pockyl.hextale.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.RiftGateBlock;
import com.pockyl.hextale.block.RiftLinks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * A Rift Link Crystal: a hextech crystal that has been pressed to a Lesser Rift Gate and remembers it. Pressed to another
 * gate it ties the two with a new link (and is spent); a crystal whose gate is gone takes the new gate instead.
 */
public class RiftLinkCrystalItem extends Item {
    /** The link id to tie and the gate remembered. */
    public record Memory(UUID link, GlobalPos gate) {
        public static final Codec<Memory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("link").forGetter(Memory::link),
                GlobalPos.CODEC.fieldOf("gate").forGetter(Memory::gate)).apply(instance, Memory::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Memory> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
                Memory::link, GlobalPos.STREAM_CODEC, Memory::gate, Memory::new);
    }

    public RiftLinkCrystalItem(Properties properties) {
        super(properties);
    }

    /** A crystal remembering the gate at {@code gate}, to tie link {@code link}. */
    public static ItemStack remembering(UUID link, GlobalPos gate) {
        ItemStack stack = new ItemStack(ModItems.RIFT_LINK_CRYSTAL.get());
        stack.set(ModDataComponents.RIFT_LINK.get(), new Memory(link, gate));
        return stack;
    }

    /** Pressed to a gate: ties it with the gate remembered (sneaking or not - so it is the item's own use). */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof RiftGateBlock.Gate gate)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Memory memory = stack.get(ModDataComponents.RIFT_LINK.get());
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        GlobalPos here = GlobalPos.of(level.dimension(), context.getClickedPos());
        if (memory == null || memory.gate().equals(here)) {
            if (memory == null) {
                stack.set(ModDataComponents.RIFT_LINK.get(), new Memory(UUID.randomUUID(), here));
            }
            message(player, "block.hextale.rift_gate.same");
            return InteractionResult.SUCCESS;
        }
        ServerLevel there = server.getServer().getLevel(memory.gate().dimension());
        RiftGateBlock.Gate first = null;
        if (there != null) {
            there.getChunkAt(memory.gate().pos());
            first = there.getBlockEntity(memory.gate().pos()) instanceof RiftGateBlock.Gate found ? found : null;
        }
        if (first == null) {
            // The gate it remembered is gone: it remembers this one instead.
            stack.set(ModDataComponents.RIFT_LINK.get(), new Memory(UUID.randomUUID(), here));
            message(player, "block.hextale.rift_gate.lost");
            return InteractionResult.SUCCESS;
        }
        RiftLinks.of(server.getServer()).tie(memory.link(), memory.gate(), here);
        first.tie(memory.link());
        gate.tie(memory.link());
        if (player == null || !player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        message(player, "block.hextale.rift_gate.linked");
        for (RiftGateBlock.Gate end : new RiftGateBlock.Gate[] {first, gate}) {
            Level at = end.getLevel();
            if (at instanceof ServerLevel endLevel) {
                Vec3 centre = Vec3.atCenterOf(end.getBlockPos());
                endLevel.playSound(null, end.getBlockPos(), ModSounds.RIFT_LINK.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
                endLevel.sendParticles(ModParticles.ring(RiftGateBlock.RIFT), centre.x, centre.y, centre.z, 0, 0.9, 0, 0, 1);
                endLevel.sendParticles(ModParticles.glint(RiftGateBlock.RIFT), centre.x, centre.y, centre.z, 24, 0.3, 0.45, 0.3, 0.06);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static void message(Player player, String key) {
        if (player != null) {
            player.sendOverlayMessage(Component.translatable(key));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModDataComponents.RIFT_LINK.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Memory memory = stack.get(ModDataComponents.RIFT_LINK.get());
        if (memory != null) {
            tooltip.accept(Component.translatable("item.hextale.rift_link_crystal.gate", memory.gate().pos().getX(), memory.gate().pos().getY(),
                    memory.gate().pos().getZ()).withStyle(ChatFormatting.DARK_PURPLE));
        }
        tooltip.accept(Component.translatable("item.hextale.rift_link_crystal.hint").withStyle(ChatFormatting.GRAY));
    }
}
