package com.pockyl.hextale.client.screen.book;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import com.google.gson.reflect.TypeToken;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.client.ClientKnowledge;
import com.pockyl.hextale.client.render.PixelArt;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.QuestDefinition;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * The guide map's editor, only in a development client. Drag a step with the right mouse button to place it by hand; a
 * right click without dragging opens {@link GuideEditScreen} (name, goal, description, icon, parents, aura, workstation,
 * add a step after it, remove it); H shows every step, fog or not; a button above the hint hides the WIP steps. A changed step is written to its data file in the
 * mod's sources ({@code data/hextale/hextale_guide/<id>.json}), names and descriptions to the language files there, so
 * the next build ships them; until then they work at once (the guide is built again from the changed data, the texts
 * show through a language layered over the game's). Crossing links are reported after every change.
 */
final class GuideEditor {
    private static final int HINT = 0xFFB8B0D0;
    private static final int CLICK = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    /** Texts edited this session (key -> text, empty = removed), shown over the game's language until a rebuild. */
    private static final Map<String, String> TEXTS = new HashMap<>();

    private final StarBook book;
    private final GuidePage guide;
    @Nullable
    private Quest dragged;
    private double pressX;
    private double pressY;
    private boolean moved;
    /** Where on the step it was grabbed, in map pixels. */
    private float grabX;
    private float grabY;
    /** The WIP switch as drawn last (screen coordinates); none before the first frame. */
    private int wipLeft = Integer.MIN_VALUE;
    private int wipTop;
    private int wipRight;
    private int wipBottom;

    GuideEditor(StarBook book, GuidePage guide) {
        this.book = book;
        this.guide = guide;
    }

    static boolean enabled() {
        return !FMLEnvironment.isProduction();
    }

    boolean press(double mouseX, double mouseY) {
        if (!enabled()) {
            return false;
        }
        dragged = guide.questAt(mouseX, mouseY, ClientKnowledge.get());
        if (dragged != null) {
            pressX = mouseX;
            pressY = mouseY;
            moved = false;
            grabX = book.mapX(mouseX) - dragged.x();
            grabY = book.mapY(mouseY) - dragged.y();
        }
        return dragged != null;
    }

    boolean drag(double mouseX, double mouseY) {
        if (dragged == null) {
            return false;
        }
        moved |= Math.abs(mouseX - pressX) + Math.abs(mouseY - pressY) >= CLICK;
        if (moved) {
            Guide.client().preview(dragged, Math.round(book.mapX(mouseX) - grabX), Math.round(book.mapY(mouseY) - grabY));
        }
        return true;
    }

    boolean release(double mouseX, double mouseY) {
        if (dragged == null) {
            return false;
        }
        Quest quest = dragged;
        dragged = null;
        if (!moved) {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.gui.setScreen(new GuideEditScreen(quest, minecraft.gui.screen()));
            return true;
        }
        var pos = new QuestDefinition.Pos(Math.round(book.mapX(mouseX) - grabX), Math.round(book.mapY(mouseY) - grabY));
        Map<String, @Nullable QuestDefinition> changed = new LinkedHashMap<>();
        changed.put(quest.id(), quest.definition().withPos(Optional.of(pos)));
        report(quest.id() + " -> " + pos.x() + ", " + pos.y(), saveSteps(changed));
        return true;
    }

    boolean keyPressed(KeyEvent event) {
        if (!enabled() || event.key() != InputConstants.KEY_H) {
            return false;
        }
        guide.revealAll = !guide.revealAll;
        return true;
    }

    /**
     * A line in the sky's bottom right corner (screen coordinates) naming the editor's controls, and over it the switch
     * that takes the WIP steps off the map (lit while they are hidden).
     */
    void renderHint(GuiGraphicsExtractor graphics, int right, int bottom, int mouseX, int mouseY) {
        if (!enabled()) {
            return;
        }
        keepTexts();
        var font = Minecraft.getInstance().font;
        Component text = Component.literal("DEV: right-drag moves, right-click edits, H "
                + (guide.revealAll ? "brings the fog back" : "shows every step"));
        int w = font.width(text);
        PixelArt.fill(graphics, right - w - 6, bottom - 13, right, bottom, PixelArt.argb(0x120C08, 0.82F));

        Component label = Component.literal(GuidePage.hideWip ? "WIP steps: hidden" : "WIP steps: shown");
        wipRight = right;
        wipBottom = bottom - 16;
        wipLeft = wipRight - font.width(label) - 10;
        wipTop = wipBottom - 14;
        boolean hovered = overWipSwitch(mouseX, mouseY);
        int rim = PixelArt.argb(GuidePage.hideWip ? 0xF0C860 : hovered ? 0xB8A880 : 0x6A5A38, 1);
        PixelArt.fill(graphics, wipLeft, wipTop, wipRight, wipBottom, PixelArt.argb(GuidePage.hideWip ? 0x3A2C10 : hovered ? 0x2A2214 : 0x120C08, 0.9F));
        PixelArt.fill(graphics, wipLeft, wipTop, wipRight, wipTop + 1, rim);
        PixelArt.fill(graphics, wipLeft, wipBottom - 1, wipRight, wipBottom, rim);
        PixelArt.fill(graphics, wipLeft, wipTop, wipLeft + 1, wipBottom, rim);
        PixelArt.fill(graphics, wipRight - 1, wipTop, wipRight, wipBottom, rim);
        PixelArt.flush(graphics);
        graphics.text(font, text, right - w - 3, bottom - 11, HINT, false);
        graphics.text(font, label, wipLeft + 5, wipTop + 3, GuidePage.hideWip ? 0xFFF0D890 : HINT, false);
    }

    private boolean overWipSwitch(double mouseX, double mouseY) {
        return enabled() && wipLeft != Integer.MIN_VALUE && mouseX >= wipLeft && mouseX < wipRight && mouseY >= wipTop && mouseY < wipBottom;
    }

    /** A left click on the WIP switch hides or shows the WIP steps; returns whether it was one. */
    boolean clickWipSwitch(double mouseX, double mouseY) {
        if (!overWipSwitch(mouseX, mouseY)) {
            return false;
        }
        GuidePage.hideWip = !GuidePage.hideWip;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    /** Shows the outcome of a change on the action bar, with a warning when links cross now. */
    static void report(String what, boolean saved) {
        int crossings = Guide.client().crossings();
        String text = what + (saved ? "  saved" : "  NOT saved") + (crossings > 0 ? "  -  " + crossings + " link crossing(s)!" : "");
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(text).withColor(crossings > 0 || !saved ? 0xFF7060 : 0x8FE07A), true);
        }
    }

    // ---- Files --------------------------------------------------------------------------------------------------------

    /**
     * Puts changed guide steps into effect: writes each changed step's data file in the mod's sources (deletes a removed
     * one's) and builds the guide again on this client and, in single player, on its server, so the change works at
     * once. Returns whether every file was written.
     */
    static boolean saveSteps(Map<String, @Nullable QuestDefinition> changed) {
        Map<String, QuestDefinition> steps = new LinkedHashMap<>(Guide.client().definitions());
        boolean saved = true;
        for (var entry : changed.entrySet()) {
            Path file = stepFile(entry.getKey());
            try {
                if (entry.getValue() == null) {
                    steps.remove(entry.getKey());
                    Files.deleteIfExists(file);
                } else {
                    steps.put(entry.getKey(), entry.getValue());
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, format(QuestDefinition.CODEC.encodeStart(
                            RegistryOps.create(JsonOps.INSTANCE, Minecraft.getInstance().level.registryAccess()), entry.getValue()).getOrThrow()),
                            StandardCharsets.UTF_8);
                }
            } catch (IOException | RuntimeException e) {
                Hextale.LOGGER.warn("Could not save the guide step {}", file, e);
                saved = false;
            }
        }
        Guide.setClient(steps);
        if (Minecraft.getInstance().hasSingleplayerServer()) {
            Guide.setServer(steps);
        }
        return saved;
    }

    /** Where a step's data file lies in the mod's sources. */
    private static Path stepFile(String id) {
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? Hextale.MOD_ID : id.substring(0, colon);
        return sources().resolve("data/" + namespace + "/" + Guide.FOLDER + "/" + id.substring(colon + 1) + ".json");
    }

    /** Pretty JSON with the short lists (place, parents, ingredients) on one line and plain icons as ids, like the files. */
    private static String format(JsonElement json) {
        String text = GSON.toJson(json);
        Matcher lists = SHORT_LIST.matcher(text);
        StringBuilder out = new StringBuilder();
        while (lists.find()) {
            String items = String.join(", ", Arrays.stream(lists.group(2).split(",")).map(String::trim).toList());
            lists.appendReplacement(out, Matcher.quoteReplacement("\"" + lists.group(1) + "\": [" + items + "]"));
        }
        lists.appendTail(out);
        return ICON.matcher(out).replaceAll("\"icon\": $1") + "\n";
    }

    /** An icon that is a plain item is written as its id. */
    private static final Pattern ICON = Pattern.compile("\"icon\": \\{\\s*\"id\": (\"[^\"]+\")\\s*}");
    private static final Pattern SHORT_LIST = Pattern.compile("\"(pos|parents|ingredients)\": \\[\\s*([^\\]\\[{}]*?)\\s*]");

    /**
     * Writes texts to the language file of the language being played (English when it has none) and, for keys the other
     * file lacks, to that one too, so every language has the key; shows them at once. A null text removes the key.
     */
    static boolean saveTexts(Map<String, @Nullable String> texts) {
        String own = Minecraft.getInstance().getLanguageManager().getSelected().equals("ru_ru") ? "ru_ru" : "en_us";
        String other = own.equals("ru_ru") ? "en_us" : "ru_ru";
        boolean saved = writeLang(own, texts, true) & writeLang(other, texts, false);
        texts.forEach((key, text) -> TEXTS.put(key, text == null ? "" : text));
        Language current = Language.getInstance();
        Language.inject(new Layered(current instanceof Layered layered ? layered.base : current));
        return saved;
    }

    /** The text a key shows now: edited this session, else the game's language. */
    static String text(String key) {
        return Language.getInstance().getOrDefault(key, "");
    }

    private static boolean writeLang(String code, Map<String, @Nullable String> texts, boolean overwrite) {
        Path file = sources().resolve("assets/" + Hextale.MOD_ID + "/lang/" + code + ".json");
        try {
            Map<String, String> lang = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                    new TypeToken<LinkedHashMap<String, String>>() { }.getType());
            texts.forEach((key, text) -> {
                if (text == null) {
                    lang.remove(key);
                } else if (overwrite || !lang.containsKey(key)) {
                    lang.put(key, text);
                }
            });
            Files.writeString(file, GSON.toJson(lang) + "\n", StandardCharsets.UTF_8);
            return true;
        } catch (IOException | RuntimeException e) {
            Hextale.LOGGER.warn("Could not save texts to {}", file, e);
            return false;
        }
    }

    /** A resource reload brings a fresh language: lay the edited texts over it again. */
    private static void keepTexts() {
        if (!TEXTS.isEmpty() && !(Language.getInstance() instanceof Layered)) {
            Language.inject(new Layered(Language.getInstance()));
        }
    }

    /**
     * The mod's resources in its sources, found by walking up from the game folder (the mod's own run folder or the
     * workspace's playtest folder); the game folder itself when there are none.
     */
    private static Path sources() {
        for (Path dir = FMLPaths.GAMEDIR.get().toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve("src/main/java/com/pockyl/hextale"))) {
                return dir.resolve("src/main/resources");
            }
            Path workspace = dir.resolve("mods/hextale/src/main/resources");
            if (Files.isDirectory(workspace)) {
                return workspace;
            }
        }
        return FMLPaths.GAMEDIR.get();
    }

    /** The game's language with the texts edited this session over it. */
    private static final class Layered extends Language {
        private final Language base;

        Layered(Language base) {
            this.base = base;
        }

        @Override
        public String getOrDefault(String key, String fallback) {
            String text = TEXTS.get(key);
            return text != null && !text.isEmpty() ? text : base.getOrDefault(key, fallback);
        }

        @Override
        public boolean has(String key) {
            String text = TEXTS.get(key);
            return text != null ? !text.isEmpty() : base.has(key);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return base.isDefaultRightToLeft();
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return base.getVisualOrder(text);
        }

        @Override
        public Map<String, String> getLanguageData() {
            return base.getLanguageData();
        }

        @Override
        public net.minecraft.network.chat.@Nullable Component getComponent(String key) {
            return TEXTS.containsKey(key) ? null : base.getComponent(key);
        }
    }
}
