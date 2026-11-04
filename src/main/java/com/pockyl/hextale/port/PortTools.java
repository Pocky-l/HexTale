package com.pockyl.hextale.port;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.core.component.DataComponents;

/**
 * The tool shortcuts of 26.x {@code Item.Properties} ({@code pickaxe}, {@code axe}, {@code shovel}, {@code sword}) for
 * plain items: the tool component and the attribute modifiers a vanilla tool of that kind would have. The item class
 * must still return true from {@code hurtEnemy} (1.21.1 tools do) for {@code postHurtEnemy} to run.
 *
 * <p>port-of: net.minecraft.world.item.Item.Properties (tool shortcuts)
 */
public final class PortTools {
    private PortTools() {
    }

    public static Item.Properties pickaxe(Item.Properties properties, ToolMaterial material, float damage, float speed) {
        return properties.component(DataComponents.TOOL, material.createToolProperties(BlockTags.MINEABLE_WITH_PICKAXE))
                .attributes(DiggerItem.createAttributes(material, damage, speed));
    }

    public static Item.Properties axe(Item.Properties properties, ToolMaterial material, float damage, float speed) {
        return properties.component(DataComponents.TOOL, material.createToolProperties(BlockTags.MINEABLE_WITH_AXE))
                .attributes(DiggerItem.createAttributes(material, damage, speed));
    }

    public static Item.Properties shovel(Item.Properties properties, ToolMaterial material, float damage, float speed) {
        return properties.component(DataComponents.TOOL, material.createToolProperties(BlockTags.MINEABLE_WITH_SHOVEL))
                .attributes(DiggerItem.createAttributes(material, damage, speed));
    }

    public static Item.Properties sword(Item.Properties properties, ToolMaterial material, float damage, float speed) {
        return properties.component(DataComponents.TOOL, SwordItem.createToolProperties())
                .attributes(SwordItem.createAttributes(material, damage, speed));
    }
}
