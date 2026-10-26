package com.pockyl.hextale.quest;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.research.ResearchTopic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

/**
 * The researcher's guide: every step the mod asks of a researcher, built from data files
 * {@code data/<namespace>/hextale_guide/<id>.json} (one {@link QuestDefinition} each) loaded with the data packs, so a
 * data pack can add, change or remove steps. A guide is an immutable snapshot: the server builds one on every data
 * reload and sends the definitions to the clients, which build their own (the two live apart, like the mod's recipes).
 * Building links each step to its parents (steps whose parents are missing, or that wait for each other in a circle, are
 * left out with a warning), orders them parents first and lays out the map ({@link GuideLayout}).
 * {@code QuestGameTests.guideOrderIsLogical} checks that every mod item a step's recipe needs comes from a step before it.
 */
public final class Guide {
    /** The data folder of the guide's steps. */
    public static final String FOLDER = "hextale_guide";
    private static final Guide EMPTY = new Guide(Map.of());
    private static volatile Guide server = EMPTY;
    private static volatile Guide client = EMPTY;

    private final Map<String, QuestDefinition> definitions;
    private final List<Quest> all;
    private final Map<String, Quest> byId;

    private Guide(Map<String, QuestDefinition> definitions) {
        this.definitions = Collections.unmodifiableMap(new TreeMap<>(definitions));
        this.all = Collections.unmodifiableList(link(this.definitions));
        Map<String, Quest> index = new HashMap<>();
        all.forEach(quest -> index.put(quest.id(), quest));
        this.byId = Collections.unmodifiableMap(index);
        GuideLayout.lay(all);
    }

    // ---- The two sides ------------------------------------------------------------------------------------------------

    /** The guide the server loaded from the data packs. */
    public static Guide server() {
        return server;
    }

    /** The guide the server sent to this client (empty until it has). */
    public static Guide client() {
        return client;
    }

    public static Guide get(boolean clientSide) {
        return clientSide ? client : server;
    }

    public static void setServer(Map<String, QuestDefinition> definitions) {
        server = new Guide(definitions);
    }

    public static void setClient(Map<String, QuestDefinition> definitions) {
        client = new Guide(definitions);
    }

    /** Loads the steps on every data reload; item stacks in icons resolve through the reload's registries. */
    public static SimplePreparableReloadListener<Map<String, QuestDefinition>> loader(HolderLookup.Provider registries) {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Map<String, QuestDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<Identifier, QuestDefinition> found = new TreeMap<>();
                SimpleJsonResourceReloadListener.scanDirectory(manager, FileToIdConverter.json(FOLDER),
                        RegistryOps.create(JsonOps.INSTANCE, registries), QuestDefinition.CODEC, found);
                Map<String, QuestDefinition> steps = new LinkedHashMap<>();
                found.forEach((id, definition) -> steps.put(id(id), definition));
                return steps;
            }

            @Override
            protected void apply(Map<String, QuestDefinition> steps, ResourceManager manager, ProfilerFiller profiler) {
                setServer(steps);
            }
        };
    }

    /** A step's id from its file: the path in the mod's own namespace, {@code namespace:path} elsewhere. */
    public static String id(Identifier file) {
        return file.getNamespace().equals(Hextale.MOD_ID) ? file.getPath() : file.toString();
    }

    // ---- Building -----------------------------------------------------------------------------------------------------

    /**
     * Makes the steps and links them; returns them parents first (among the steps whose parents are all in, the one
     * with the smallest id comes next), so the order is the same wherever the guide is built.
     */
    private static List<Quest> link(Map<String, QuestDefinition> definitions) {
        Map<String, Quest> steps = new HashMap<>();
        definitions.forEach((id, definition) -> steps.put(id, new Quest(id, definition)));
        Set<String> broken = new HashSet<>();
        definitions.forEach((id, definition) -> {
            for (String parent : definition.parents()) {
                if (!steps.containsKey(parent)) {
                    Hextale.LOGGER.warn("Guide step {} waits for {}, which is not in the guide; left out", id, parent);
                    broken.add(id);
                }
            }
        });
        Map<String, Integer> waiting = new HashMap<>();
        Map<String, List<String>> after = new HashMap<>();
        definitions.forEach((id, definition) -> {
            waiting.put(id, definition.parents().size());
            definition.parents().forEach(parent -> after.computeIfAbsent(parent, key -> new ArrayList<>()).add(id));
        });
        PriorityQueue<String> ready = new PriorityQueue<>();
        waiting.forEach((id, count) -> {
            if (count == 0) {
                ready.add(id);
            }
        });
        List<Quest> ordered = new ArrayList<>();
        Set<String> out = new HashSet<>();
        while (!ready.isEmpty()) {
            String id = ready.poll();
            Quest quest = steps.get(id);
            boolean usable = !broken.contains(id) && quest.definition().parents().stream().allMatch(out::contains);
            if (usable) {
                for (String parent : quest.definition().parents()) {
                    Quest from = steps.get(parent);
                    quest.parents.add(from);
                    from.children.add(quest);
                }
                quest.index = ordered.size();
                ordered.add(quest);
                out.add(id);
            }
            for (String child : after.getOrDefault(id, List.of())) {
                if (waiting.merge(child, -1, Integer::sum) == 0) {
                    ready.add(child);
                }
            }
        }
        if (ordered.size() + broken.size() < definitions.size()) {
            definitions.keySet().stream().filter(id -> !out.contains(id) && !broken.contains(id))
                    .forEach(id -> Hextale.LOGGER.warn("Guide step {} is left out: a step it waits for is missing or waits for it in turn", id));
        }
        return ordered;
    }

    // ---- Queries ------------------------------------------------------------------------------------------------------

    /** Every step, parents before children. */
    public List<Quest> all() {
        return all;
    }

    @Nullable
    public Quest byId(String id) {
        return byId.get(id);
    }

    /** The data every step was built from, by id (what the server sends to the clients). */
    public Map<String, QuestDefinition> definitions() {
        return definitions;
    }

    /** The first step that asks for this research topic, or null. */
    @Nullable
    public Quest forTopic(ResearchTopic topic) {
        for (Quest quest : all) {
            if (quest.topic() == topic) {
                return quest;
            }
        }
        return null;
    }

    /** Whether the researcher may research this topic at the table: a step asking for it is open (or done). */
    public boolean canResearch(ResearchTopic topic, RuneKnowledge knowledge) {
        for (Quest quest : all) {
            if (quest.topic() == topic && visible(quest, knowledge)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves a step of this guide on the map while the book's dev editor drags it; the move itself is saved as data and
     * builds a new guide.
     */
    public void preview(Quest quest, int x, int y) {
        quest.x = x;
        quest.y = y;
    }

    /** Every step that waits for this one, through any number of steps. */
    public Set<Quest> descendants(Quest quest) {
        Set<Quest> out = new HashSet<>();
        List<Quest> todo = new ArrayList<>(quest.children());
        while (!todo.isEmpty()) {
            Quest next = todo.removeLast();
            if (out.add(next)) {
                todo.addAll(next.children());
            }
        }
        return out;
    }

    /** How many pairs of links cross (a hand-placed map can make them; the game tests then fail). */
    public int crossings() {
        return GuideLayout.crossings(all);
    }

    /** Bounds of the laid-out map (for the camera). */
    public int left() {
        return all.stream().mapToInt(Quest::x).min().orElse(0);
    }

    public int top() {
        return all.stream().mapToInt(Quest::y).min().orElse(0);
    }

    public int right() {
        return all.stream().mapToInt(Quest::x).max().orElse(0);
    }

    public int bottom() {
        return all.stream().mapToInt(Quest::y).max().orElse(0);
    }

    // ---- A step and a researcher ----------------------------------------------------------------------------------------

    /** An item step when its goal was met; WIP steps never. */
    public static boolean done(Quest quest, RuneKnowledge knowledge) {
        return quest.kind() == Quest.Kind.ITEM && knowledge.questDone(quest);
    }

    /** Every parent is done (a device still being worked out that another one grows from only has to be seen). */
    public static boolean open(Quest quest, RuneKnowledge knowledge) {
        for (Quest parent : quest.parents()) {
            if (parent.kind() == Quest.Kind.WIP ? !visible(parent, knowledge) : !done(parent, knowledge)) {
                return false;
            }
        }
        return true;
    }

    /** What the book shows: done steps and open ones (a secret one only once done). The rest stays in the fog. */
    public static boolean visible(Quest quest, RuneKnowledge knowledge) {
        return done(quest, knowledge) || open(quest, knowledge) && !quest.secret();
    }

    /**
     * One step beyond what the reader can do now: an item step that is not open yet, but every step it waits for can be
     * seen. The book shows it greyed out, as "not reachable yet"; everything further stays in the fog.
     */
    public static boolean ahead(Quest quest, RuneKnowledge knowledge) {
        if (quest.kind() != Quest.Kind.ITEM || quest.secret() || visible(quest, knowledge)) {
            return false;
        }
        for (Quest parent : quest.parents()) {
            if (!visible(parent, knowledge)) {
                return false;
            }
        }
        return true;
    }

    // ---- Map geometry shared with the book and the tests ----------------------------------------------------------------

    /** Points a link's curve is checked at by the tests. */
    public static final int LINK_SAMPLES = GuideLayout.LINK_SAMPLES;

    /** A point of the link from {@code from} to {@code to} at {@code t} (0..1), bowed {@code bow} map pixels to the side. */
    public static double[] linkPoint(Quest from, Quest to, double bow, double t) {
        return GuideLayout.linkPoint(from, to, bow, t);
    }

    /** Whether two segments cross (touching ends do not count). */
    public static boolean segmentsCross(double[] a0, double[] a1, double[] b0, double[] b1) {
        return GuideLayout.segmentsCross(a0, a1, b0, b1);
    }

    /** The distance from a point to a sampled curve. */
    public static double distanceToCurve(double x, double y, double[][] curve) {
        return GuideLayout.distanceToCurve(x, y, curve);
    }
}
