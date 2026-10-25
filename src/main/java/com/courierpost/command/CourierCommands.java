package com.courierpost.command;

import com.courierpost.CourierPost;
import com.courierpost.bird.BirdManager;
import com.courierpost.bird.CourierBird;
import com.courierpost.chat.Localization;
import com.courierpost.config.CourierConfig;
import com.courierpost.ui.ConfigDialog;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class CourierCommands {
    private static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggest(CourierConfig.KEYS, builder);

    private CourierCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("birdconfig")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .executes(CourierCommands::openDialog)
                .then(literal("reload").executes(CourierCommands::reload))
                .then(literal("toggle")
                        .then(argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .executes(CourierCommands::toggle)))
                .then(literal("edit")
                        .then(argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .executes(CourierCommands::edit)))
                .then(literal("apply")
                        .then(argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .then(argument("value", StringArgumentType.greedyString())
                                        .executes(CourierCommands::apply))))
                .then(literal("get")
                        .then(argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .executes(CourierCommands::get)))
                .then(literal("set")
                        .then(argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .then(argument("value", StringArgumentType.greedyString())
                                        .executes(CourierCommands::set)))));

        dispatcher.register(literal("birds")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .executes(context -> list(context, null)));

        dispatcher.register(literal("birdtrack")
                .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    return list(context, player);
                }));
    }

    private static int openDialog(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ConfigDialog.open(context.getSource().getPlayerOrException());
        return 1;
    }

    private static int toggle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, "key");
        if (!ConfigDialog.toggle(key)) {
            context.getSource().sendFailure(Localization.text(context.getSource(), "courierpost.command.config.unknown_key", key));
            return 0;
        }
        CourierPost.saveConfig();
        ConfigDialog.open(player);
        return 1;
    }

    private static int edit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, "key");
        if (!ConfigDialog.isEditable(key)) {
            context.getSource().sendFailure(Localization.text(context.getSource(), "courierpost.command.config.unknown_key", key));
            return 0;
        }
        ConfigDialog.edit(player, key);
        return 1;
    }

    private static int apply(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = StringArgumentType.getString(context, "key");
        CourierConfig config = CourierPost.config();
        if (config.get(key) == null) {
            context.getSource().sendFailure(Localization.text(context.getSource(), "courierpost.command.config.unknown_key", key));
            return 0;
        }
        String value = ConfigDialog.normalize(key, StringArgumentType.getString(context, "value").trim());
        if (!config.set(key, value)) {
            context.getSource().sendFailure(Localization.text(context.getSource(), "courierpost.command.config.invalid_value", value));
            return 0;
        }
        CourierPost.saveConfig();
        ConfigDialog.open(player);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        CourierPost.reloadConfig();
        context.getSource().sendSuccess(() -> Localization.text(context.getSource(), "courierpost.command.config.reloaded")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int get(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = CourierPost.config().get(key);
        if (value == null) {
            context.getSource().sendFailure(Localization.text(context.getSource(), "courierpost.command.config.unknown_key", key));
            return 0;
        }
        context.getSource().sendSuccess(() -> Localization.text(context.getSource(), "courierpost.command.config.get", key, value), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value").trim();
        CourierConfig config = CourierPost.config();
        if (config.get(key) == null) {
            context.getSource().sendFailure(Localization.text(context.getSource(), "courierpost.command.config.unknown_key", key));
            return 0;
        }
        if (!config.set(key, value)) {
            context.getSource().sendFailure(Localization.text(context.getSource(), "courierpost.command.config.invalid_value", value));
            return 0;
        }
        CourierPost.saveConfig();
        String applied = config.get(key);
        context.getSource().sendSuccess(() -> Localization.text(context.getSource(), "courierpost.command.config.set", key, applied)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context, ServerPlayer owner) {
        BirdManager manager = CourierPost.manager();
        List<CourierBird> birds = manager.birds().stream()
                .filter(bird -> owner == null || bird.senderId.equals(owner.getUUID()))
                .toList();

        if (birds.isEmpty()) {
            context.getSource().sendSuccess(() -> Localization.text(context.getSource(), "courierpost.command.birds.empty")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }

        context.getSource().sendSuccess(() -> Localization.text(context.getSource(), "courierpost.command.birds.header", birds.size())
                .withStyle(ChatFormatting.GOLD), false);

        for (CourierBird bird : birds) {
            ServerPlayer recipient = manager.server().getPlayerList().getPlayer(bird.recipientId);
            String recipientName = recipient == null ? "-" : recipient.getGameProfile().name();
            String dimension = bird.dimension.identifier().toString();
            String speed = String.format("%.1f", bird.lastSpeed);
            String distance = String.format("%.1f", bird.remainingDistance);
            context.getSource().sendSuccess(() -> Localization.text(context.getSource(), "courierpost.command.birds.entry",
                    bird.senderName,
                    recipientName,
                    dimension,
                    speed,
                    bird.elapsedSeconds(),
                    distance), false);
        }
        return birds.size();
    }
}
