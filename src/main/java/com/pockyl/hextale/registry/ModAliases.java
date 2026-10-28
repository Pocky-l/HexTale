package com.pockyl.hextale.registry;

import com.pockyl.hextale.Hextale;

import java.util.List;

/**
 * Old ids that live on as aliases of their new names. Modpacks, quests and worlds refer to the mod's items and blocks by
 * id, so a released id never just disappears: when an item or block is renamed, its old id goes here and keeps resolving
 * to the new one (saved worlds, recipes, quest files). The list of released ids is {@code docs/released-ids.txt}; the game
 * test {@code releasedIdsStillResolve} fails when one of them is gone without an alias.
 */
public final class ModAliases {
    /** An old id and the one it became; {@code block} renames rename the block item too. */
    private record Rename(String from, String to, boolean block) {
    }

    private static final List<Rename> RENAMES = List.of();

    private ModAliases() {
    }

    /** Adds the aliases to the item and block registers; call before they are registered. */
    public static void register() {
        for (Rename rename : RENAMES) {
            ModItems.ITEMS.addAlias(Hextale.id(rename.from()), Hextale.id(rename.to()));
            if (rename.block()) {
                ModBlocks.BLOCKS.addAlias(Hextale.id(rename.from()), Hextale.id(rename.to()));
            }
        }
    }
}
