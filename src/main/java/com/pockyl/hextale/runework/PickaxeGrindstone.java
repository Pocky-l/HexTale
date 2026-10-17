package com.pockyl.hextale.runework;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GrindstoneEvent;

import com.pockyl.hextale.Hextale;

/**
 * A grindstone grinds the runes off a hextech pickaxe, as it grinds enchantments off other tools: a pickaxe with runes
 * alone in the grindstone comes out without them (its enchantments stay; grind it again for those).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class PickaxeGrindstone {
    private PickaxeGrindstone() {
    }

    @SubscribeEvent
    public static void onPlace(GrindstoneEvent.OnPlaceItem event) {
        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();
        ItemStack pickaxe = top.isEmpty() ? bottom : bottom.isEmpty() ? top : ItemStack.EMPTY;
        if (!HextechPickaxeItem.is(pickaxe) || PickaxeRunes.of(pickaxe).isEmpty()) {
            return;
        }
        ItemStack out = pickaxe.copy();
        PickaxeRunes.set(out, PickaxeRunes.EMPTY);
        event.setOutput(out);
        event.setXp(0);
    }
}
