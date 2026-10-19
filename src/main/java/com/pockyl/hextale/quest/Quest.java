package com.pockyl.hextale.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.research.ResearchTopic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * One step of the researcher's guide (see {@link Guide}): something to make or obtain (an {@link Kind#ITEM} quest) or a
 * device still being worked out ({@link Kind#WIP}, never done). A step opens once all its parents are done; the first
 * parent is the one its main link comes from on the page. A step with a research {@link #topic()} must be researched
 * at the Research Table before its item can be made. Texts: {@code quest.hextale.<id>}, {@code .goal}, {@code .desc}.
 */
public final class Quest {
    public enum Kind {
        ITEM,
        WIP
    }

    /** The part of the ritual circle a step's 3D scene puts in focus. */
    public enum Scene {
        /** The whole circle at work: the altar draws the pedestals' items and the jars' essences. */
        ALTAR,
        /** The pedestals: their items rise and fly into the altar. */
        PEDESTAL,
        /** The steles: their runes stream into the altar. */
        STELE
    }

    /**
     * The effect a step wears in the book. Only a handful of steps wear one, the turning points of the path, so an effect
     * always means "this one matters".
     */
    public enum Aura {
        NONE,
        /** A key step of the path: a larger frame with a slow golden halo. */
        GOLD,
        /** Lightning and raw hextech: electric arcs jumping around the frame. */
        STORM,
        /** Refined hextech: blue sparks orbiting the frame. */
        HEXTECH
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
    private final ResearchTopic topic;
    private final Aura aura;
    private final Region region;
    /** A landmark is the heart of the guide: drawn large, with open sky around it. */
    boolean landmark;
    /** A workstation: drawn large in a cog ring, the steps that grow from it stand around it. */
    boolean station;
    @Nullable
    Scene scene;
    int index;
    int x;
    int y;

    Quest(String id, Kind kind, List<Quest> parents, Supplier<ItemStack> icon, Predicate<ServerPlayer> goal,
          @Nullable QuestDisplay display, @Nullable ResearchTopic topic, Aura aura, Region region) {
        this.id = id;
        this.kind = kind;
        this.parents = List.copyOf(parents);
        this.icon = icon;
        this.goal = goal;
        this.display = display;
        this.topic = topic;
        this.aura = aura;
        this.region = region;
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

    /** Whether an item quest's goal is met now (WIP steps are never checked this way). */
    public boolean isMet(ServerPlayer player) {
        return kind == Kind.ITEM && goal.test(player);
    }

    @Nullable
    public QuestDisplay display() {
        return display;
    }

    /** What must be researched before this step's item can be made, else null. */
    @Nullable
    public ResearchTopic topic() {
        return topic;
    }

    public Aura aura() {
        return aura;
    }

    /** The theme this step belongs to: where it sits in the book. */
    public Region region() {
        return region;
    }

    /** Key steps are drawn larger. */
    public boolean important() {
        return aura == Aura.GOLD;
    }

    /** The step the whole guide turns around: much larger, special, alone in its patch of sky. */
    public boolean landmark() {
        return landmark;
    }

    /** A workstation (bench, furnace, table, altar...): drawn large, its steps gather around it. */
    public boolean station() {
        return station;
    }

    /** The 3D scene of the ritual circle on the step's plate, or null. */
    @Nullable
    public Scene scene() {
        return scene;
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
        return "quest.hextale." + id;
    }

    @Override
    public String toString() {
        return id;
    }
}
