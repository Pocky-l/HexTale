package com.pockyl.hextale.client.screen.book;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.pockyl.hextale.port.ItemStackTemplate;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.QuestDefinition;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The dev editor's window for one guide step (development clients only, opened by a right click on the map): its name,
 * goal and description in the language being played, its icon (an item id), its parents (ids, the first is where its
 * link comes from), its aura and whether it is a workstation; buttons add a device or an item step after it or remove it.
 * A plain vanilla-widget tool on purpose - players never see it. Saving writes the step's data file and the language
 * files through {@link GuideEditor}.
 */
final class GuideEditScreen extends Screen {
    private static final int TEXT = 0xFFE0E0E0;
    private static final int MUTED = 0xFF9A9AA8;
    private static final int WIDTH = 320;

    private final Quest quest;
    @Nullable
    private final Screen parent;
    private EditBox name;
    @Nullable
    private EditBox goal;
    private MultiLineEditBox description;
    private EditBox icon;
    private EditBox parents;
    private Quest.Aura aura;
    private boolean station;
    private String problem = "";
    private String initialName;
    private String initialGoal;
    private String initialDescription;
    private String initialIcon;
    private String initialParents;

    GuideEditScreen(Quest quest, @Nullable Screen parent) {
        super(Component.literal("Guide step " + quest.id()));
        this.quest = quest;
        // A container screen (the Research Table) would close its menu under us: the editor then returns to the world.
        this.parent = parent instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> ? null : parent;
        this.aura = quest.aura();
        this.station = quest.station();
    }

    @Override
    protected void init() {
        int left = (width - WIDTH) / 2;
        int y = 30;
        initialName = GuideEditor.text(quest.translationKey());
        name = addRenderableWidget(new EditBox(font, left, y, WIDTH, 18, Component.literal("Name")));
        name.setMaxLength(200);
        name.setValue(initialName);
        y += 32;
        initialGoal = GuideEditor.text(quest.translationKey() + ".goal");
        if (quest.kind() == Quest.Kind.ITEM) {
            goal = addRenderableWidget(new EditBox(font, left, y, WIDTH, 18, Component.literal("Goal")));
            goal.setMaxLength(300);
            goal.setValue(initialGoal);
            y += 32;
        }
        initialDescription = GuideEditor.text(quest.translationKey() + ".desc");
        description = addRenderableWidget(MultiLineEditBox.builder().setX(left).setY(y).setShowDecorations(false).build(font, WIDTH, 64, Component.literal("Description")));
        description.setCharacterLimit(2000);
        description.setValue(initialDescription, true);
        y += 78;
        initialIcon = BuiltInRegistries.ITEM.getKey(quest.icon().getItem()).toString();
        icon = addRenderableWidget(new EditBox(font, left, y, WIDTH / 2 - 4, 18, Component.literal("Icon")));
        icon.setMaxLength(200);
        icon.setValue(initialIcon);
        initialParents = String.join(", ", quest.parents().stream().map(Quest::id).toList());
        parents = addRenderableWidget(new EditBox(font, left + WIDTH / 2 + 4, y, WIDTH / 2 - 4, 18, Component.literal("Parents")));
        parents.setMaxLength(500);
        parents.setValue(initialParents);
        y += 26;
        addRenderableWidget(Button.builder(auraLabel(), button -> {
            aura = Quest.Aura.values()[(aura.ordinal() + 1) % Quest.Aura.values().length];
            button.setMessage(auraLabel());
        }).bounds(left, y, WIDTH / 2 - 4, 20).build());
        addRenderableWidget(Button.builder(stationLabel(), button -> {
            station = !station;
            button.setMessage(stationLabel());
        }).bounds(left + WIDTH / 2 + 4, y, WIDTH / 2 - 4, 20).build());
        y += 26;
        int third = (WIDTH - 8) / 3;
        addRenderableWidget(Button.builder(Component.literal("+ device after it"), button -> addChild(Quest.Kind.WIP))
                .bounds(left, y, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+ item step after it"), button -> addChild(Quest.Kind.ITEM))
                .bounds(left + third + 4, y, third, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Remove"), button -> remove())
                .bounds(left + 2 * (third + 4), y, third, 20).build());
        y += 26;
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> save()).bounds(left, y, WIDTH / 2 - 4, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(left + WIDTH / 2 + 4, y, WIDTH / 2 - 4, 20).build());
        setInitialFocus(name);
    }

    private Component auraLabel() {
        return Component.literal("Aura: " + aura.name().toLowerCase(java.util.Locale.ROOT));
    }

    private Component stationLabel() {
        return Component.literal("Workstation: " + (station ? "yes" : "no"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int left = (width - WIDTH) / 2;
        graphics.text(font, Component.literal(quest.id() + "  (" + quest.kind().name().toLowerCase(java.util.Locale.ROOT) + ", "
                + quest.region().name().toLowerCase(java.util.Locale.ROOT) + ")  -  data/hextale/hextale_guide/" + quest.id() + ".json"),
                left, 8, TEXT, true);
        graphics.text(font, Component.literal("Name"), left, name.getY() - 10, MUTED, false);
        if (goal != null) {
            graphics.text(font, Component.literal("Goal (\"Goal: ...\" under the name)"), left, goal.getY() - 10, MUTED, false);
        }
        graphics.text(font, Component.literal("Description"), left, description.getY() - 10, MUTED, false);
        graphics.text(font, Component.literal("Icon (item id)"), left, icon.getY() - 10, MUTED, false);
        graphics.text(font, Component.literal("Parents (ids; the first draws the link)"), parents.getX(), parents.getY() - 10, MUTED, false);
        if (!problem.isEmpty()) {
            graphics.text(font, Component.literal(problem), left, height - 16, 0xFFFF7060, true);
        }
    }

    /** Applies every change: texts to the language files, the rest to the guide's edits file; back to the book. */
    private void save() {
        if (!apply()) {
            return;
        }
        onClose();
    }

    /** Applies every change to the step's data and texts; false (with the problem shown) when something is wrong. */
    private boolean apply() {
        QuestDefinition next = quest.definition();
        String iconId = icon.getValue().trim();
        if (!iconId.equals(initialIcon)) {
            ResourceLocation id = ResourceLocation.tryParse(iconId);
            if (id == null || BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
                problem = "No item " + iconId;
                return false;
            }
            next = next.withIcon(new ItemStackTemplate(BuiltInRegistries.ITEM.get(id)));
        }
        if (!parents.getValue().trim().equals(initialParents)) {
            List<String> ids = Arrays.stream(parents.getValue().split(",")).map(String::trim).filter(id -> !id.isEmpty()).toList();
            String wrong = checkParents(ids);
            if (wrong != null) {
                problem = wrong;
                return false;
            }
            next = next.withParents(ids);
        }
        next = next.withAura(aura).withStation(station);
        boolean saved = true;
        if (!next.equals(quest.definition())) {
            Map<String, @Nullable QuestDefinition> changed = new LinkedHashMap<>();
            changed.put(quest.id(), next);
            saved = GuideEditor.saveSteps(changed);
        }
        Map<String, @Nullable String> texts = new LinkedHashMap<>();
        if (!name.getValue().equals(initialName)) {
            texts.put(quest.translationKey(), name.getValue());
        }
        if (goal != null && !goal.getValue().equals(initialGoal)) {
            texts.put(quest.translationKey() + ".goal", goal.getValue());
        }
        if (!description.getValue().equals(initialDescription)) {
            texts.put(quest.translationKey() + ".desc", description.getValue());
        }
        if (!texts.isEmpty()) {
            saved &= GuideEditor.saveTexts(texts);
        }
        GuideEditor.report(quest.id() + " edited", saved);
        return true;
    }

    /** What is wrong with these parents, or null: they must exist, not wait for this step, and an item never waits for a WIP. */
    @Nullable
    private String checkParents(List<String> ids) {
        Guide guide = Guide.client();
        if (ids.isEmpty() && !quest.parents().isEmpty()) {
            return "A step needs at least one parent";
        }
        Set<Quest> after = guide.descendants(quest);
        for (String id : ids) {
            Quest parent = guide.byId(id);
            if (parent == null) {
                return "No step " + id;
            }
            if (parent == quest || after.contains(parent)) {
                return id + " waits for " + quest.id() + " - it cannot come first";
            }
            if (quest.kind() == Quest.Kind.ITEM && parent.kind() == Quest.Kind.WIP) {
                return "An item step cannot wait for a device still being worked out";
            }
        }
        return null;
    }

    /** Adds a step after this one (saving this one first): a book for an icon, next to it on the map, to be edited now. */
    private void addChild(Quest.Kind kind) {
        if (!apply()) {
            return;
        }
        Guide guide = Guide.client();
        String base = kind == Quest.Kind.WIP ? "wip_custom_" : "custom_";
        int n = 1;
        while (guide.byId(base + n) != null) {
            n++;
        }
        String id = base + n;
        Quest at = guide.byId(quest.id());
        QuestDefinition definition = new QuestDefinition(kind, quest.region(), quest.stage(), new ItemStackTemplate(Items.BOOK), List.of(quest.id()),
                Optional.empty(), Optional.empty(), Optional.empty(), Quest.Aura.NONE, false, false, Optional.empty(),
                Optional.of(freeSpot(guide, at != null ? at : quest)));
        Map<String, @Nullable QuestDefinition> changed = new LinkedHashMap<>();
        changed.put(id, definition);
        boolean saved = GuideEditor.saveSteps(changed);
        Quest child = Guide.client().byId(id);
        if (child == null) {
            problem = "The new step could not be built";
            return;
        }
        boolean russian = minecraft.getLanguageManager().getSelected().equals("ru_ru");
        Map<String, @Nullable String> texts = new LinkedHashMap<>();
        texts.put(child.translationKey(), kind == Quest.Kind.WIP ? (russian ? "Новое устройство" : "New device") : (russian ? "Новый шаг" : "New step"));
        texts.put(child.translationKey() + ".desc", "");
        if (kind == Quest.Kind.ITEM) {
            texts.put(child.translationKey() + ".goal", russian ? "получить предмет" : "obtain it");
        }
        saved &= GuideEditor.saveTexts(texts);
        GuideEditor.report(id + " added", saved);
        minecraft.gui.setScreen(new GuideEditScreen(child, parent));
    }

    /** A place next to a step that keeps clear of every other step. */
    private static QuestDefinition.Pos freeSpot(Guide guide, Quest near) {
        for (int ring = 0; ring < 20; ring++) {
            for (int turn = 0; turn < 12; turn++) {
                double angle = turn * Math.PI / 6;
                int x = (int) Math.round(near.x() + Math.cos(angle) * (60 + ring * 12));
                int y = (int) Math.round(near.y() + Math.sin(angle) * (60 + ring * 12));
                if (guide.all().stream().allMatch(other -> Math.hypot(other.x() - x, other.y() - y) >= 50)) {
                    return new QuestDefinition.Pos(x, y);
                }
            }
        }
        return new QuestDefinition.Pos(near.x() + 60, near.y());
    }

    /** Removes the step: its data file and its texts. Only a step nothing waits for. */
    private void remove() {
        if (!quest.children().isEmpty()) {
            problem = "Remove or move the steps after it first";
            return;
        }
        Map<String, @Nullable QuestDefinition> changed = new LinkedHashMap<>();
        changed.put(quest.id(), null);
        boolean saved = GuideEditor.saveSteps(changed);
        Map<String, @Nullable String> texts = new LinkedHashMap<>();
        texts.put(quest.translationKey(), null);
        texts.put(quest.translationKey() + ".desc", null);
        texts.put(quest.translationKey() + ".goal", null);
        saved &= GuideEditor.saveTexts(texts);
        GuideEditor.report(quest.id() + " removed", saved);
        onClose();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
