package com.courierpost.command;

import com.courierpost.chat.ChatHandler;
import com.courierpost.chat.Localization;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class WhisperCommands {
    private static final char SEPARATOR = ',';
    private static final String SPLIT_PATTERN = ",";

    private static final SuggestionProvider<CommandSourceStack> TARGETS = (context, builder) -> {
        String remaining = builder.getRemaining();
        if (remaining.indexOf(' ') >= 0) {
            return Suggestions.empty();
        }
        int comma = remaining.lastIndexOf(SEPARATOR);
        if (comma < 0) {
            return Suggestions.empty();
        }

        Set<String> chosen = new HashSet<>();
        for (String part : remaining.substring(0, comma).split(SPLIT_PATTERN)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                chosen.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }

        List<String> names = new ArrayList<>();
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            String name = player.getGameProfile().name();
            if (!chosen.contains(name.toLowerCase(Locale.ROOT))) {
                names.add(name);
            }
        }
        return SharedSuggestionProvider.suggest(names, builder.createOffset(builder.getStart() + comma + 1));
    };

    private WhisperCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> node = dispatcher.register(literal("msg")
                .then(argument("targets", EntityArgument.players())
                        .then(argument("message", MessageArgument.message())
                                .executes(WhisperCommands::bySelector)))
                .then(argument("line", StringArgumentType.greedyString())
                        .suggests(TARGETS)
                        .executes(WhisperCommands::byList)));

        dispatcher.register(literal("tell").redirect(node));
        dispatcher.register(literal("w").redirect(node));
    }

    private static int bySelector(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        MessageArgument.resolveChatMessage(context, "message", message ->
                ChatHandler.handleWhisper(sender, targets, message.signedContent()));
        return targets.size();
    }

    private static int byList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer sender = source.getPlayerOrException();
        String line = StringArgumentType.getString(context, "line");

        int space = line.indexOf(' ');
        if (space < 0) {
            throw new SimpleCommandExceptionType(
                    Localization.text(source, "courierpost.command.whisper.usage")).create();
        }

        String content = line.substring(space + 1).trim();
        if (content.isEmpty()) {
            throw new SimpleCommandExceptionType(
                    Localization.text(source, "courierpost.command.whisper.usage")).create();
        }

        List<ServerPlayer> targets = resolve(source, line.substring(0, space));
        ChatHandler.handleWhisper(sender, targets, content);
        return targets.size();
    }

    private static List<ServerPlayer> resolve(CommandSourceStack source, String raw) throws CommandSyntaxException {
        PlayerList list = source.getServer().getPlayerList();
        List<ServerPlayer> targets = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        for (String part : raw.split(SPLIT_PATTERN)) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            ServerPlayer player = list.getPlayerByName(name);
            if (player == null) {
                throw new SimpleCommandExceptionType(
                        Localization.text(source, "courierpost.command.whisper.unknown", name)).create();
            }
            if (seen.add(player.getUUID())) {
                targets.add(player);
            }
        }

        if (targets.isEmpty()) {
            throw new SimpleCommandExceptionType(
                    Localization.text(source, "courierpost.command.whisper.empty")).create();
        }
        return targets;
    }
}
