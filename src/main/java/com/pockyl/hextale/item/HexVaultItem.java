package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.storage.VaultMemory;

import java.util.function.Consumer;

/**
 * A Hex Vault taken off the ground: it remembers its store. On the ground it never despawns and resists fire and blasts
 * (its item properties), since everything the vault held waits for this one item.
 */
public class HexVaultItem extends BlockItem {
    public HexVaultItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** 26.x: the {@code damage_resistant} component with this tag. */
    @Override
    public boolean canBeHurtBy(ItemStack stack, net.minecraft.world.damagesource.DamageSource source) {
        return !source.is(com.pockyl.hextale.registry.ModTags.VAULT_SURVIVES) && super.canBeHurtBy(stack, source);
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return stack.has(ModDataComponents.VAULT_MEMORY.get()) ? Integer.MAX_VALUE : super.getEntityLifespan(stack, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        VaultMemory memory = stack.get(ModDataComponents.VAULT_MEMORY.get());
        if (memory != null && memory.kinds() > 0) {
            tooltip.accept(Component.translatable("block.hextale.hex_vault.holds", memory.kinds(), memory.used()).withStyle(ChatFormatting.AQUA));
        }
        tooltip.accept(Component.translatable("block.hextale.hex_vault.hint").withStyle(ChatFormatting.GRAY));
    }
}
