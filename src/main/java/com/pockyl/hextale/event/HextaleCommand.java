package com.pockyl.hextale.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.thevoid.Thinning;

import java.util.List;
import java.util.stream.Stream;

/** {@code /hextale runes list|learn <rune|all> [stars]|forget <rune|all>} {@code /hextale quests reset} and {@code /hextale void get|set|add|stage|breach|clear}: test helpers for operators. */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class HextaleCommand {
    private static final String ALL = "all";
    private static final DynamicCommandExceptionType UNKNOWN_RUNE =
            new DynamicCommandExceptionType(key -> Component.translatable("commands.hextale.runes.unknown", key));
    private static final SuggestionProvider<CommandSourceStack> RUNES = (context, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of(ALL), ComplexRunes.all().stream().map(ComplexRune::key)), builder);

    private HextaleCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(Hextale.MOD_ID)
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("runes")
                        .then(Commands.literal("list")
                                .executes(context -> list(context.getSource(), context.getSource().getPlayerOrException())))
                        .then(Commands.literal("learn")
                                .then(Commands.argument("rune", StringArgumentType.word()).suggests(RUNES)
                                        .executes(context -> learn(context.getSource(), StringArgumentType.getString(context, "rune"), 1))
                                        .then(Commands.argument("stars", IntegerArgumentType.integer(1, 3))
                                                .executes(context -> learn(context.getSource(), StringArgumentType.getString(context, "rune"),
                                                        IntegerArgumentType.getInteger(context, "stars"))))))
                        .then(Commands.literal("forget")
                                .then(Commands.argument("rune", StringArgumentType.word()).suggests(RUNES)
                                        .executes(context -> forget(context.getSource(), StringArgumentType.getString(context, "rune"))))))
                .then(Commands.literal("quests")
                        .then(Commands.literal("reset")
                                .executes(context -> resetQuests(context.getSource()))))
                .then(Commands.literal("void")
                        .then(Commands.literal("get")
                                .executes(context -> getThinning(context.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0, Thinning.MAX))
                                        .executes(context -> setThinning(context.getSource(), FloatArgumentType.getFloat(context, "value"), 0))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 16))
                                                .executes(context -> setThinning(context.getSource(), FloatArgumentType.getFloat(context, "value"),
                                                        IntegerArgumentType.getInteger(context, "radius"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(-Thinning.MAX, Thinning.MAX))
                                        .executes(context -> addThinning(context.getSource(), FloatArgumentType.getFloat(context, "value")))))
                        .then(Commands.literal("stage")
                                .then(Commands.argument("stage", IntegerArgumentType.integer(0, Thinning.Stage.values().length - 1))
                                        .executes(context -> setThinning(context.getSource(),
                                                Thinning.Stage.values()[IntegerArgumentType.getInteger(context, "stage")].from, 0))
                                        .then(Commands.argument("radius", IntegerArgumentType.integer(0, 16))
                                                .executes(context -> setThinning(context.getSource(),
                                                        Thinning.Stage.values()[IntegerArgumentType.getInteger(context, "stage")].from,
                                                        IntegerArgumentType.getInteger(context, "radius"))))))
                        .then(Commands.literal("breach")
                                .executes(context -> breach(context.getSource())))
                        .then(Commands.literal("clear")
                                .executes(context -> setThinning(context.getSource(), 0, 0))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(0, 16))
                                        .executes(context -> setThinning(context.getSource(), 0, IntegerArgumentType.getInteger(context, "radius")))))));
    }

    private static List<ComplexRune> runes(String key) throws CommandSyntaxException {
        if (ALL.equals(key)) {
            return ComplexRunes.all();
        }
        ComplexRune rune = ComplexRunes.byKey(key);
        if (rune == null) {
            throw UNKNOWN_RUNE.create(key);
        }
        return List.of(rune);
    }

    private static int list(CommandSourceStack source, ServerPlayer player) {
        RuneKnowledge knowledge = Research.of(player);
        List<Component> names = knowledge.runes().stream().map(HextaleCommand::name).toList();
        source.sendSuccess(() -> Component.translatable("commands.hextale.runes.list", player.getDisplayName(),
                knowledge.runeCount(), ComplexRunes.COUNT, ComponentUtils.formatList(names, Component.literal(", "))), false);
        return knowledge.runeCount();
    }

    private static int learn(CommandSourceStack source, String key, int stars) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int added = Research.learnAll(player, runes(key), stars);
        source.sendSuccess(() -> Component.translatable("commands.hextale.runes.learned", added, player.getDisplayName(),
                Research.of(player).runeCount(), ComplexRunes.COUNT), true);
        return added;
    }

    private static int forget(CommandSourceStack source, String key) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int removed = Research.forget(player, runes(key));
        source.sendSuccess(() -> Component.translatable("commands.hextale.runes.forgotten", removed, player.getDisplayName(),
                Research.of(player).runeCount(), ComplexRunes.COUNT), true);
        return removed;
    }

    /** Forgets the done quests; those whose goal is still met come back at the next check. */
    private static int resetQuests(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Research.of(player).forgetQuests();
        Research.sync(player);
        source.sendSuccess(() -> Component.translatable("commands.hextale.quests.reset", player.getDisplayName()), true);
        return 1;
    }

    private static int getThinning(CommandSourceStack source) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        float value = Thinning.get(source.getLevel(), pos);
        Thinning.Stage stage = Thinning.Stage.of(value);
        source.sendSuccess(() -> Component.translatable("commands.hextale.void.get", Math.round(value),
                Component.translatable(stage.key()), stage.ordinal()), false);
        return Math.round(value);
    }

    /**
     * Tears a breach open near the command's source at once, with its omen and the cinematic for the players near - on
     * open ground nearby when there is some, else on the ground ahead. Ignores how sick the land is and other breaches.
     */
    private static int breach(CommandSourceStack source) {
        net.minecraft.server.level.ServerLevel level = source.getLevel();
        BlockPos at = BlockPos.containing(source.getPosition());
        BlockPos ground = null;
        for (int ring = 0; ring <= 2 && ground == null; ring++) {
            for (int dx = -ring; dx <= ring && ground == null; dx++) {
                for (int dz = -ring; dz <= ring && ground == null; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        ground = com.pockyl.hextale.thevoid.Breaches.findGround(level,
                                new net.minecraft.world.level.ChunkPos((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz), level.getRandom());
                    }
                }
            }
        }
        if (ground == null) {
            net.minecraft.world.phys.Vec3 ahead = source.getPosition().add(
                    net.minecraft.world.phys.Vec3.directionFromRotation(0, source.getRotation().y).scale(10));
            int x = net.minecraft.util.Mth.floor(ahead.x);
            int z = net.minecraft.util.Mth.floor(ahead.z);
            ground = new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
        }
        BlockPos spot = ground;
        com.pockyl.hextale.thevoid.Breaches.open(level, spot, source.getPosition());
        source.sendSuccess(() -> Component.translatable("commands.hextale.void.breach", spot.getX(), spot.getY() + 1, spot.getZ()), true);
        return 1;
    }

    /** Sets the Thinning of the chunk the command runs in and of every loaded chunk within {@code radius} chunks. */
    private static int setThinning(CommandSourceStack source, float value, int radius) {
        int chunks = Thinning.setAround(source.getLevel(), BlockPos.containing(source.getPosition()), radius, value);
        source.sendSuccess(() -> Component.translatable("commands.hextale.void.set", chunks, Math.round(value),
                Component.translatable(Thinning.Stage.of(value).key())), true);
        return chunks;
    }

    private static int addThinning(CommandSourceStack source, float value) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        float now = Thinning.get(source.getLevel(), pos) + value;
        return setThinning(source, now, 0);
    }

    /** The rune's name in its category colour; hovering shows its key and effect. */
    private static Component name(ComplexRune rune) {
        MutableComponent hover = Component.literal(rune.key()).withStyle(ChatFormatting.GRAY).append("\n")
                .append(Component.translatable(rune.translationKey() + ".effect").withStyle(ChatFormatting.WHITE));
        return Component.translatable(rune.translationKey())
                .withStyle(style -> style.withColor(rune.color()).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }
}
