package com.pockyl.hextale.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
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

import java.util.List;
import java.util.stream.Stream;

/** {@code /hextale runes list|learn <rune|all>|forget <rune|all>}: test helpers for operators. */
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
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("runes")
                        .then(Commands.literal("list")
                                .executes(context -> list(context.getSource(), context.getSource().getPlayerOrException())))
                        .then(Commands.literal("learn")
                                .then(Commands.argument("rune", StringArgumentType.word()).suggests(RUNES)
                                        .executes(context -> learn(context.getSource(), StringArgumentType.getString(context, "rune")))))
                        .then(Commands.literal("forget")
                                .then(Commands.argument("rune", StringArgumentType.word()).suggests(RUNES)
                                        .executes(context -> forget(context.getSource(), StringArgumentType.getString(context, "rune")))))));
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

    private static int learn(CommandSourceStack source, String key) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int added = Research.learnAll(player, runes(key));
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

    /** The rune's name in its category colour; hovering shows its key and effect. */
    private static Component name(ComplexRune rune) {
        MutableComponent hover = Component.literal(rune.key()).withStyle(ChatFormatting.GRAY).append("\n")
                .append(Component.translatable(rune.translationKey() + ".effect").withStyle(ChatFormatting.WHITE));
        return Component.translatable(rune.translationKey())
                .withStyle(style -> style.withColor(rune.color()).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }
}
