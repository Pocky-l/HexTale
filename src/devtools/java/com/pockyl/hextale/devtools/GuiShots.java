package com.pockyl.hextale.devtools;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import com.pockyl.hextale.Hextale;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

/**
 * Development-only screenshot driver (run {@code guiShots}, never in the jar). When the game directory holds
 * {@code job.txt} naming a script in {@code tools/guishots/}, it opens (or creates) the flat creative world
 * "GuiShots", runs the script line by line - server commands, block and item use, mouse and keys driven through the
 * real input handlers, GUI scale changes - saves screenshots into {@code screenshots/} and quits.
 *
 * <p>Script lines ({@code #} comments): {@code cmd <command>}, {@code wait <ticks>}, {@code use <x> <y> <z> [face]},
 * {@code useitem}, {@code slot <0-8>}, {@code scale <n>}, {@code origin screen|zero|<x> <y>} (GUI coordinates below
 * are relative to it; {@code screen} = top left of a container screen's panel), {@code move <x> <y>},
 * {@code click <x> <y> [left|right|middle]}, {@code press}/{@code release <x> <y> [button]},
 * {@code drag <x1> <y1> <x2> <y2> [ticks] [hold|cont|held]} (hold: keep the button down at the end, cont: it is down
 * already, held: both), {@code scroll <x> <y> <amount>}, {@code key <name>} (e.g. escape, e),
 * {@code sneak on|off},
 * {@code close}, {@code shot <name>}, {@code quit}.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID, value = Dist.CLIENT)
public final class GuiShots {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORLD = "GuiShots";
    private static final int SETTLE_TICKS = 3;

    private static final Path TOOLS = System.getProperty("hextale.guishots") == null ? null
            : Path.of(System.getProperty("hextale.guishots"));
    private static List<String> script;
    private static int line;
    private static int wait;
    private static boolean worldRequested;
    private static int inWorldTicks;
    private static double originX;
    private static double originY;
    private static boolean originScreen;
    private static Drag drag;
    private static int shots;
    private static boolean finished;
    private static String pendingShot;
    private static final List<String> report = new ArrayList<>();

    private record Drag(double x1, double y1, double x2, double y2, int ticks, int button, int[] step, boolean pressed, boolean keep) {
    }

    private GuiShots() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (TOOLS == null || finished) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (script == null && !load(minecraft)) {
            return;
        }
        if (minecraft.level == null || minecraft.player == null) {
            if (!worldRequested && minecraft.gui.screen() instanceof TitleScreen) {
                worldRequested = true;
                minecraft.options.pauseOnLostFocus = false;
                openWorld(minecraft);
            }
            return;
        }
        if (inWorldTicks++ < 60) {
            return;
        }
        try {
            if (drag != null) {
                stepDrag(minecraft);
                return;
            }
            if (wait > 0) {
                wait--;
                return;
            }
            if (pendingShot != null) {
                String name = pendingShot;
                pendingShot = null;
                shot(minecraft, name);
                return;
            }
            while (line < script.size() && wait == 0 && drag == null && pendingShot == null) {
                String raw = script.get(line++).trim();
                if (raw.isEmpty() || raw.startsWith("#")) {
                    continue;
                }
                run(minecraft, raw);
            }
            if (line >= script.size() && wait == 0 && drag == null && pendingShot == null) {
                finish(minecraft, null);
            }
        } catch (Exception e) {
            LOGGER.error("[GuiShots] line {} failed", line, e);
            finish(minecraft, "line " + line + ": " + e);
        }
    }

    private static boolean load(Minecraft minecraft) {
        Path job = minecraft.gameDirectory.toPath().resolve("job.txt");
        if (!Files.isRegularFile(job)) {
            return false;
        }
        try {
            String name = Files.readString(job, StandardCharsets.UTF_8).trim();
            Path file = TOOLS.resolve(name);
            script = new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
            LOGGER.info("[GuiShots] running {} ({} lines)", file, script.size());
            return true;
        } catch (IOException e) {
            LOGGER.error("[GuiShots] cannot read the job", e);
            script = List.of();
            return false;
        }
    }

    private static void openWorld(Minecraft minecraft) {
        if (minecraft.getLevelSource().levelExists(WORLD)) {
            minecraft.createWorldOpenFlows().openWorld(WORLD, () -> minecraft.gui.setScreen(new TitleScreen()));
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true,
                WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1L, false, false),
                registries -> registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value()
                        .createWorldDimensions(), new TitleScreen());
    }

    private static void run(Minecraft minecraft, String raw) throws Exception {
        String[] a = raw.split("\\s+");
        String op = a[0].toLowerCase(Locale.ROOT);
        switch (op) {
            case "cmd" -> command(minecraft, raw.substring(3).trim());
            case "wait" -> wait = Integer.parseInt(a[1]);
            case "use" -> {
                BlockPos pos = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
                Direction face = a.length > 4 ? Direction.byName(a[4]) : Direction.UP;
                Vec3 hit = Vec3.atCenterOf(pos).relative(face, 0.5);
                minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false));
                wait = SETTLE_TICKS;
            }
            case "useitem" -> {
                minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
                wait = SETTLE_TICKS;
            }
            case "slot" -> minecraft.player.getInventory().setSelectedSlot(Integer.parseInt(a[1]));
            case "scale" -> {
                minecraft.options.guiScale().set(Integer.parseInt(a[1]));
                minecraft.resizeGui();
                wait = SETTLE_TICKS;
            }
            case "origin" -> {
                originScreen = a[1].equals("screen");
                originX = originScreen || a[1].equals("zero") ? 0 : Double.parseDouble(a[1]);
                originY = originScreen || a[1].equals("zero") ? 0 : Double.parseDouble(a[2]);
            }
            case "move" -> move(minecraft, x(minecraft, a[1]), y(minecraft, a[2]));
            case "click" -> {
                double x = x(minecraft, a[1]);
                double y = y(minecraft, a[2]);
                int button = button(a, 3);
                move(minecraft, x, y);
                button(minecraft, button, true);
                button(minecraft, button, false);
                wait = SETTLE_TICKS;
            }
            case "press", "release" -> {
                move(minecraft, x(minecraft, a[1]), y(minecraft, a[2]));
                button(minecraft, button(a, 3), op.equals("press"));
                wait = 1;
            }
            case "drag" -> {
                int ticks = a.length > 5 ? Integer.parseInt(a[5]) : 10;
                String mode = a.length > 6 ? a[6] : "";
                drag = new Drag(x(minecraft, a[1]), y(minecraft, a[2]), x(minecraft, a[3]), y(minecraft, a[4]), ticks,
                        InputConstants.MOUSE_BUTTON_LEFT, new int[] {-1}, mode.equals("cont") || mode.equals("held"),
                        mode.equals("hold") || mode.equals("held"));
            }
            case "scroll" -> {
                move(minecraft, x(minecraft, a[1]), y(minecraft, a[2]));
                minecraft.mouseHandler.onScroll(minecraft.getWindow().handle(), 0, Double.parseDouble(a[3]));
                wait = SETTLE_TICKS;
            }
            case "sneak" -> {
                minecraft.options.keyShift.setDown(a[1].equals("on"));
                wait = SETTLE_TICKS;
            }
            case "key" -> {
                int key = InputConstants.getKey("key.keyboard." + a[1].toLowerCase(Locale.ROOT)).getValue();
                KeyEvent keyEvent = new KeyEvent(key, 0, 0);
                minecraft.keyboardHandler.keyPress(minecraft.getWindow().handle(), InputConstants.PRESS, keyEvent);
                minecraft.keyboardHandler.keyPress(minecraft.getWindow().handle(), InputConstants.RELEASE, keyEvent);
                wait = SETTLE_TICKS;
            }
            case "close" -> {
                if (minecraft.player != null) {
                    minecraft.player.closeContainer();
                }
                minecraft.gui.setScreen(null);
                wait = SETTLE_TICKS;
            }
            case "shot" -> {
                // Chat lines, tutorial hints and toasts would cover the screen: clear them, render a frame, then shoot.
                minecraft.gui.hud.getChat().clearMessages(false);
                minecraft.gui.toastManager().clear();
                minecraft.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                minecraft.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                pendingShot = a[1];
                wait = 2;
            }
            case "quit" -> line = script.size();
            default -> throw new IllegalArgumentException("unknown step: " + raw);
        }
    }

    private static void command(Minecraft minecraft, String command) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            throw new IllegalStateException("no integrated server");
        }
        java.util.UUID id = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withEntity(player)
                        .withPosition(player.position()).withLevel(player.level()), command);
            }
        });
        wait = 2;
    }

    private static void stepDrag(Minecraft minecraft) {
        int i = ++drag.step()[0];
        double t = Math.min(1.0, i / (double) drag.ticks());
        double x = drag.x1() + (drag.x2() - drag.x1()) * t;
        double y = drag.y1() + (drag.y2() - drag.y1()) * t;
        if (i == 0) {
            move(minecraft, x, y);
            if (!drag.pressed()) {
                button(minecraft, drag.button(), true);
            }
            return;
        }
        double px = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        double py = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
        move(minecraft, x, y);
        Screen screen = minecraft.gui.screen();
        if (screen != null && !minecraft.isWindowActive()) {
            // An unfocused window ignores motion, so the drag is fed to the screen directly.
            screen.mouseMoved(x, y);
            screen.mouseDragged(new MouseButtonEvent(x, y, new MouseButtonInfo(drag.button(), 0)), x - px, y - py);
        }
        if (t >= 1.0) {
            if (!drag.keep()) {
                button(minecraft, drag.button(), false);
            }
            drag = null;
            wait = SETTLE_TICKS;
        }
    }

    private static void move(Minecraft minecraft, double guiX, double guiY) {
        Window window = minecraft.getWindow();
        double sx = window.getScreenWidth() / (double) window.getGuiScaledWidth();
        double sy = window.getScreenHeight() / (double) window.getGuiScaledHeight();
        double px = minecraft.mouseHandler.getScaledXPos(window) * sx;
        double py = minecraft.mouseHandler.getScaledYPos(window) * sy;
        minecraft.mouseHandler.onMove(window.handle(), guiX * sx, guiY * sy, guiX * sx - px, guiY * sy - py);
    }

    private static void button(Minecraft minecraft, int button, boolean pressed) {
        minecraft.mouseHandler.onButton(minecraft.getWindow().handle(), new MouseButtonInfo(button, 0),
                pressed ? InputConstants.PRESS : InputConstants.RELEASE);
    }

    private static int button(String[] a, int index) {
        if (a.length <= index) {
            return InputConstants.MOUSE_BUTTON_LEFT;
        }
        return switch (a[index]) {
            case "right" -> InputConstants.MOUSE_BUTTON_RIGHT;
            case "middle" -> InputConstants.MOUSE_BUTTON_MIDDLE;
            default -> InputConstants.MOUSE_BUTTON_LEFT;
        };
    }

    private static double x(Minecraft minecraft, String value) {
        return Double.parseDouble(value) + originX + (originScreen ? screenField(minecraft, "leftPos") : 0);
    }

    private static double y(Minecraft minecraft, String value) {
        return Double.parseDouble(value) + originY + (originScreen ? screenField(minecraft, "topPos") : 0);
    }

    private static int screenField(Minecraft minecraft, String name) {
        if (!(minecraft.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return 0;
        }
        try {
            Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    private static void shot(Minecraft minecraft, String name) {
        Window window = minecraft.getWindow();
        String file = name.endsWith(".png") ? name : name + ".png";
        Screen screen = minecraft.gui.screen();
        String info = file + " screen=" + (screen == null ? "none" : screen.getClass().getSimpleName()) + " gui="
                + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight() + " scale=" + window.getGuiScale()
                + (screen instanceof AbstractContainerScreen<?> ? " panel=" + screenField(minecraft, "leftPos") + ","
                + screenField(minecraft, "topPos") : "");
        report.add(info);
        shots++;
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.gameRenderer.mainRenderTarget(), 1,
                message -> LOGGER.info("[GuiShots] {}", info));
        wait = SETTLE_TICKS;
    }

    private static void finish(Minecraft minecraft, String error) {
        finished = true;
        Path dir = minecraft.gameDirectory.toPath().resolve("screenshots");
        try {
            Files.createDirectories(dir);
            List<String> lines = new ArrayList<>(report);
            lines.add(error == null ? "DONE " + shots + " shots" : "ERROR " + error);
            Files.write(dir.resolve("guishots-report.txt"), lines, StandardCharsets.UTF_8);
            Files.deleteIfExists(minecraft.gameDirectory.toPath().resolve("job.txt"));
        } catch (IOException e) {
            LOGGER.error("[GuiShots] cannot write the report", e);
        }
        // Give the screenshot writer threads a moment before the game closes.
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
            }
            minecraft.execute(minecraft::stop);
        }, "GuiShots-quit").start();
    }
}
