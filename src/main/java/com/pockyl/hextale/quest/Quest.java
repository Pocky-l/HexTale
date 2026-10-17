package com.pockyl.hextale.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.rune.ComplexRune;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * One step of the researcher's guide (see {@link Guide}): something to make or obtain (an {@link Kind#ITEM} quest),
 * a rune to study at the table ({@link Kind#RUNE}), or a device still being worked out ({@link Kind#WIP}, never done).
 * A step opens once all its parents are done; the first parent is the one it hangs from on the page. Texts: items and
 * WIP steps {@code quest.hextale.<id>}, {@code .goal}, {@code .desc}; rune steps use the rune's own keys.
 */
public final class Quest {
    public enum Kind {
        ITEM,
        RUNE,
        WIP
    }

    /** The effect a step wears in the book: important steps glow, storms crackle, curses smoke. */
    public enum Aura {
        NONE,
        /** A key step of the path: a larger frame with a slow golden halo. */
        GOLD,
        /** Lightning and raw hextech: electric arcs jumping around the frame. */
        STORM,
        /** Refined hextech: blue sparks orbiting the frame. */
        HEXTECH,
        /** Shadow and forbidden things: dark smoke creeping around the frame. */
        CURSED,
        /** Heat and smelting: embers rising from the frame. */
        FLAME,
        /** Life: green motes drifting up. */
        LIFE
    }

    private final String id;
    private final Kind kind;
    private final List<Quest> parents;
    private final List<Quest> children = new ArrayList<>();
    private final Supplier<ItemStack> icon;
    private final Predicate<ServerPlayer> goal;
    @Nullable
    private final QuestDisplay display;
    @Nullable
    private final ComplexRune rune;
    private final Aura aura;
    int index;
    int x;
    int y;

    Quest(String id, Kind kind, List<Quest> parents, Supplier<ItemStack> icon, Predicate<ServerPlayer> goal,
          @Nullable QuestDisplay display, @Nullable ComplexRune rune, Aura aura) {
        this.id = id;
        this.kind = kind;
        this.parents = List.copyOf(parents);
        this.icon = icon;
        this.goal = goal;
        this.display = display;
        this.rune = rune;
        this.aura = aura;
        for (Quest parent : parents) {
            parent.children.add(this);
        }
    }

    public String id() {
        return id;
    }

    public Kind kind() {
        return kind;
    }

    /** The steps that must be done first; the first is the one this step hangs from. */
    public List<Quest> parents() {
        return parents;
    }

    @Nullable
    public Quest parent() {
        return parents.isEmpty() ? null : parents.getFirst();
    }

    public List<Quest> children() {
        return children;
    }

    public ItemStack icon() {
        return icon.get();
    }

    /** Whether an item quest's goal is met now (rune and WIP steps are never checked this way). */
    public boolean isMet(ServerPlayer player) {
        return kind == Kind.ITEM && goal.test(player);
    }

    @Nullable
    public QuestDisplay display() {
        return display;
    }

    /** The rune a rune step teaches, else null. */
    @Nullable
    public ComplexRune rune() {
        return rune;
    }

    public Aura aura() {
        return aura;
    }

    /** Key steps are drawn larger. */
    public boolean important() {
        return aura == Aura.GOLD;
    }

    /** Position in the guide's order (parents always come first). */
    public int index() {
        return index;
    }

    /** Map position on the book's page, from {@link Guide}'s layout. */
    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public String translationKey() {
        return rune != null ? rune.translationKey() : "quest.hextale." + id;
    }

    @Override
    public String toString() {
        return id;
    }
}
