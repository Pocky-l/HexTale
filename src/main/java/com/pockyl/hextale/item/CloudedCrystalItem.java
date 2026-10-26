package com.pockyl.hextale.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;

import com.pockyl.hextale.block.VoidAnchorBlock;
import com.pockyl.hextale.block.VoidAnchorBlockEntity;

/** A hextech crystal clouded with the Void. Pressed to an anchor of a held breach while sneaking, it folds the breach. */
public class CloudedCrystalItem extends NotedItem {
    public CloudedCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isSecondaryUseActive()
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof VoidAnchorBlockEntity anchor) {
            return VoidAnchorBlock.fold(context.getItemInHand(), context.getLevel(), anchor, player);
        }
        return super.useOn(context);
    }
}
