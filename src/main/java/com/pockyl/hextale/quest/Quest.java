package com.pockyl.hextale.quest;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import com.pockyl.hextale.port.ItemStackTemplate;

import com.pockyl.hextale.research.ResearchTopic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * One step of the researcher's guide as the book and the server use it: its {@link QuestDefinition} (read from the data)
 * linked to its parents and children, with its place in the guide's order and on the map ({@link Guide} builds it).
 * A step opens once all its parents are done; the first parent is the one its link comes from on the page.
 */
public final class Quest {
    public enum Kind implements StringRepresentable {
        /** Something to make or obtain. */
        ITEM,
        /** A device still being worked out: never done, it shows where the mod is heading. */
        WIP;

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The part of the game a step belongs to: the workshop of the first days, the ritual circle in the middle, the Hexcore
     * at the end. The book lays the stages out as bands and dresses each in its own metal.
     */
    public enum Stage implements StringRepresentable {
        /** Brass, wood and dust: from the diary to the Research Table and the ink. */
        EARLY,
        /** Crystal and hexsteel: from the Hextech Altar to the gemstone. */
        MID,
        /** The Hexcore and what only it can make. */
        LATE;

        public static final Codec<Stage> CODEC = StringRepresentable.fromEnum(Stage::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** The part of the ritual circle a step's 3D scene puts in focus. */
    public enum Scene implements StringRepresentable {
        /** The whole circle at work: the altar draws the pedestals' items and the jars' essences. */
        ALTAR,
        /** The pedestals: their items rise and fly into the altar. */
        PEDESTAL,
        /** The steles: their runes stream into the altar. */
        STELE;

        public static final Codec<Scene> CODEC = StringRepresentable.fromEnum(Scene::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * The effect a step wears in the book. Only a handful of steps wear one, the turning points of the path, so an effect
     * always means "this one matters".
     */
    public enum Aura implements StringRepresentable {
        NONE,
        /** A key step of the path: a larger frame with a slow golden halo. */
        GOLD,
        /** Lightning and raw hextech: electric arcs jumping around the frame. */
        STORM,
        /** Refined hextech: blue sparks orbiting the frame. */
        HEXTECH,
        /** The Void: a dark corona that swallows the light around the frame, motes drawn into it, angry flares. */
        VOID;

        public static final Codec<Aura> CODEC = StringRepresentable.fromEnum(Aura::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final String id;
    private final QuestDefinition definition;
    /** Made on first use: item components are not bound yet while data packs load. */
    @Nullable
    private List<ItemStack> icons;
    final List<Quest> parents = new ArrayList<>();
    final List<Quest> children = new ArrayList<>();
    int index;
    int x;
    int y;
    /** How far the link from the first parent bows out of the straight line (see {@link Guide#linkPoint}). */
    float bow;

    Quest(String id, QuestDefinition definition) {
        this.id = id;
        this.definition = definition;
    }

    /** The step's id: its data file's path ({@code namespace:path} outside the mod's own namespace). */
    public String id() {
        return id;
    }

    /** The data the step was built from. */
    public QuestDefinition definition() {
        return definition;
    }

    public Kind kind() {
        return definition.kind();
    }

    /** The steps that must be done first; the first is the one this step hangs from. */
    public List<Quest> parents() {
        return Collections.unmodifiableList(parents);
    }

    @Nullable
    public Quest parent() {
        return parents.isEmpty() ? null : parents.getFirst();
    }

    public List<Quest> children() {
        return Collections.unmodifiableList(children);
    }

    /** The step's sign: its first icon. */
    public ItemStack icon() {
        return icons().getFirst();
    }

    /** Every icon of the step; most have one, a step for a set of things one per thing. */
    public List<ItemStack> icons() {
        if (icons == null) {
            icons = definition.icons().stream().map(ItemStackTemplate::create).toList();
        }
        return icons;
    }

    /** How long each of several icons stays before the next one, in milliseconds. */
    public static final long ICON_TICK = 1400;

    /** The icon shown at this moment of the wall clock: several icons take turns. */
    public ItemStack icon(long now) {
        List<ItemStack> all = icons();
        return all.get((int) ((now / ICON_TICK + index) % all.size()));
    }

    /**
     * Whether an item step's goal is met now. The step that asks for a research is met only by researching it (reading
     * the notes): finding or being given the item teaches nothing. Other steps: their own goal, or by default holding
     * their icon's item. WIP steps never.
     */
    public boolean isMet(ServerPlayer player, Guide guide) {
        if (kind() != Kind.ITEM) {
            return false;
        }
        ResearchTopic topic = topic();
        if (topic != null && guide.forTopic(topic) == this) {
            return com.pockyl.hextale.knowledge.Research.of(player).researched(topic);
        }
        return definition.goal().map(goal -> goal.isMet(player))
                .orElseGet(() -> player.getInventory().contains(stack -> stack.is(icon().getItem())));
    }

    /** Whether this event (reported by the game) completes the step. */
    public boolean answers(String event) {
        return kind() == Kind.ITEM && definition.goal().map(goal -> goal.answers(event)).orElse(false);
    }

    /** The step's (first) picture, or null. */
    @Nullable
    public QuestDisplay display() {
        return definition.displays().isEmpty() ? null : definition.displays().getFirst();
    }

    /** Every picture of the step: one per icon when the step stands for a set of things. */
    public List<QuestDisplay> displays() {
        return definition.displays();
    }

    /** What must be researched before this step's item can be made, else null. */
    @Nullable
    public ResearchTopic topic() {
        return definition.research().orElse(null);
    }

    public Aura aura() {
        return definition.aura();
    }

    /** The theme this step belongs to: the haze around it in the book. */
    public Region region() {
        return definition.region();
    }

    /** The part of the game the step belongs to. */
    public Stage stage() {
        return definition.stage();
    }

    /** Key steps (a golden halo, or the Void's turning points) are drawn larger. */
    public boolean important() {
        return aura() == Aura.GOLD || aura() == Aura.VOID;
    }

    /** The step the whole guide turns around: much larger, special, alone in its patch of sky. */
    public boolean landmark() {
        return definition.landmark();
    }

    /** Stays in the fog until done: something that happens to the researcher, written down once it has. */
    public boolean secret() {
        return definition.secret();
    }

    /** A workstation (bench, furnace, table, altar...): drawn large, its steps gather around it. */
    public boolean station() {
        return definition.station();
    }

    /** The 3D scene of the ritual circle on the step's plate, or null. */
    @Nullable
    public Scene scene() {
        return definition.scene().orElse(null);
    }

    /** Position in the guide's order (parents always come first). */
    public int index() {
        return index;
    }

    /** Map position on the book's page, from the guide's layout. */
    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    /** The bow of the link from the first parent, map pixels to the side (from the guide's layout). */
    public float bow() {
        return bow;
    }

    public String translationKey() {
        int colon = id.indexOf(':');
        return colon < 0 ? "quest.hextale." + id : "quest." + id.substring(0, colon) + "." + id.substring(colon + 1);
    }

    @Override
    public String toString() {
        return id;
    }
}
